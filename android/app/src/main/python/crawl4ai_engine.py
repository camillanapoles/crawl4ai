"""
crawl4ai_engine.py
──────────────────
Core async engine that wraps crawl4ai in a way that is compatible with the
Android / Chaquopy constraints:

  1. Playwright is NOT available → use HTTPCrawlerConfig for all HTTP fetching.
  2. The engine is long-lived (singleton pattern in crawl4ai_bridge.py).
  3. All methods are async; callers use asyncio.run() or _run() helper.
  4. Results are returned as plain dicts (JSON-serialisable) so that the
     bridge can json.dumps() them without extra conversion.

Module layout
─────────────
CrawlEngine
  ├── init()                 — download NLTK data, warm browser-less crawlers
  ├── crawl_url()            — single URL via aiohttp/httpx
  ├── crawl_urls()           — many URLs concurrently
  ├── deep_crawl()           — BFS / DFS / BFF multi-level crawl
  ├── extract_content()      — CSS / XPath / Regex / LLM extraction
  ├── generate_markdown()    — DefaultMarkdownGenerator
  ├── filter_content()       — Pruning / BM25 content filter
  ├── chunk_text()           — RegexChunking / NLP sentence chunking
  ├── seed_urls()            — AsyncUrlSeeder
  ├── score_urls()           — URL scorers
  ├── filter_urls()          — URL filters
  ├── process_html()         — Full pipeline from raw HTML (WebView output)
  ├── get_cache_stats()      — SQLite cache statistics
  └── clear_cache()          — Cache purge
"""

from __future__ import annotations

import asyncio
import logging
import os
import time
from typing import Any

logger = logging.getLogger("crawl4ai.android")


# ── Helpers ───────────────────────────────────────────────────────────────

def _ok(**kwargs: Any) -> dict:
    return {"success": True, **kwargs}


def _err(msg: str, detail: str = "") -> dict:
    return {"success": False, "error": msg, "detail": detail}


# ── Engine ────────────────────────────────────────────────────────────────

class CrawlEngine:
    """Async engine wrapping crawl4ai for Android / Chaquopy."""

    def __init__(self) -> None:
        self._crawler: Any = None          # AsyncWebCrawler instance
        self._cache_db_path: str = ""      # Set during init()
        self._ready: bool = False

    # ── Lifecycle ──────────────────────────────────────────────────────────

    async def init(self) -> bool:
        """
        Initialise the engine:
        - Locate / create the SQLite cache directory (Android's files dir
          is available via the CRAWL4AI_CACHE_DIR env var set by Kotlin).
        - Download required NLTK data packages.
        - Instantiate AsyncWebCrawler with HTTP-only strategy.
        """
        if self._ready:
            return True

        # Determine cache directory (set by Kotlin before calling init_engine)
        cache_dir = os.environ.get("CRAWL4AI_CACHE_DIR", "/data/local/tmp/crawl4ai")
        os.makedirs(cache_dir, exist_ok=True)
        self._cache_db_path = os.path.join(cache_dir, "crawl4ai_cache.db")

        # Set crawl4ai home so it writes data to the Android-writable directory
        os.environ.setdefault("CRAWL4AI_HOME", cache_dir)

        # Download NLTK tokenizers (required by NlpSentenceChunking)
        try:
            import nltk
            nltk_data_dir = os.path.join(cache_dir, "nltk_data")
            os.makedirs(nltk_data_dir, exist_ok=True)
            nltk.data.path.append(nltk_data_dir)
            # Download only the small packages needed
            for pkg in ("punkt", "punkt_tab", "stopwords"):
                try:
                    nltk.download(pkg, download_dir=nltk_data_dir, quiet=True)
                except Exception:
                    pass
        except ImportError:
            pass

        # Build a minimal AsyncWebCrawler that uses HTTP only (no Playwright)
        try:
            from crawl4ai import AsyncWebCrawler
            from crawl4ai.async_configs import BrowserConfig, CrawlerRunConfig
            from crawl4ai.crawlers import AsyncHTTPCrawlerStrategy

            # HTTPCrawlerStrategy — no Chromium needed
            http_strategy = AsyncHTTPCrawlerStrategy()
            self._crawler = AsyncWebCrawler(crawler_strategy=http_strategy)
            await self._crawler.start()
        except Exception as exc:
            logger.warning("Could not initialise AsyncWebCrawler: %s", exc)
            # We still continue — individual methods can fall back to aiohttp
            self._crawler = None

        self._ready = True
        return True

    # ── Single URL crawl ───────────────────────────────────────────────────

    async def crawl_url(self, url: str, config: dict) -> dict:
        await self.init()
        try:
            run_config = _build_run_config(config)

            if self._crawler is not None:
                result = await self._crawler.arun(url=url, config=run_config)
                return _crawl_result_to_dict(result)

            # Fallback: raw aiohttp fetch + post-processing
            return await _fallback_http_crawl(url, config)
        except Exception as exc:
            import traceback
            return _err(str(exc), traceback.format_exc())

    # ── Multi-URL crawl ────────────────────────────────────────────────────

    async def crawl_urls(self, urls: list[str], config: dict) -> list[dict]:
        await self.init()
        results = []
        if self._crawler is not None:
            try:
                from crawl4ai.async_configs import CrawlerRunConfig
                run_config = _build_run_config(config)
                async for result in await self._crawler.arun_many(
                    urls=urls, config=run_config
                ):
                    results.append(_crawl_result_to_dict(result))
                return results
            except Exception:
                pass

        # Fallback: sequential
        tasks = [self.crawl_url(url, config) for url in urls]
        return list(await asyncio.gather(*tasks, return_exceptions=False))

    # ── Deep crawl ─────────────────────────────────────────────────────────

    async def deep_crawl(
        self,
        start_url: str,
        strategy_cfg: dict,
        config: dict,
    ) -> list[dict]:
        await self.init()
        try:
            strategy = _build_deep_crawl_strategy(strategy_cfg)
            run_config = _build_run_config(config)
            run_config.deep_crawl_strategy = strategy

            results = []
            if self._crawler is not None:
                async for result in await self._crawler.arun(
                    url=start_url, config=run_config
                ):
                    results.append(_crawl_result_to_dict(result))
            return results
        except Exception as exc:
            import traceback
            return [_err(str(exc), traceback.format_exc())]

    # ── Content extraction ─────────────────────────────────────────────────

    async def extract_content(self, html: str, strategy_cfg: dict) -> dict:
        await self.init()
        try:
            strategy = _build_extraction_strategy(strategy_cfg)
            if strategy is None:
                return _err("Unknown extraction strategy", str(strategy_cfg))

            # Extraction strategies can work on markdown or HTML
            input_format = strategy_cfg.get("input_format", "html")
            if input_format == "markdown":
                # Convert HTML to markdown first
                md_result = await self.generate_markdown(html, {})
                content = md_result.get("raw_markdown", "")
            else:
                content = html

            extracted = strategy.extract(content, url=strategy_cfg.get("url", ""))
            return _ok(data=extracted, strategy=strategy_cfg.get("type", "unknown"))
        except Exception as exc:
            import traceback
            return _err(str(exc), traceback.format_exc())

    # ── Markdown generation ────────────────────────────────────────────────

    async def generate_markdown(self, html: str, config: dict) -> dict:
        await self.init()
        try:
            from crawl4ai.markdown_generation_strategy import DefaultMarkdownGenerator
            from crawl4ai.content_filter_strategy import PruningContentFilter, BM25ContentFilter

            content_filter = None
            filter_type = config.get("content_filter")
            if filter_type == "pruning":
                threshold = float(config.get("pruning_threshold", 0.48))
                content_filter = PruningContentFilter(
                    threshold=threshold,
                    threshold_type=config.get("threshold_type", "fixed"),
                )
            elif filter_type == "bm25":
                query = config.get("bm25_query", "")
                if query:
                    content_filter = BM25ContentFilter(user_query=query)

            generator = DefaultMarkdownGenerator(content_filter=content_filter)
            base_url = config.get("base_url", "")

            result = generator.generate_markdown(
                cleaned_html=html,
                base_url=base_url,
                html2text_options=config.get("html2text_options", {}),
            )
            return _ok(
                raw_markdown=result.raw_markdown,
                markdown_with_citations=result.markdown_with_citations,
                references_markdown=result.references_markdown,
                fit_markdown=result.fit_markdown or "",
                fit_html=result.fit_html or "",
            )
        except Exception as exc:
            import traceback
            return _err(str(exc), traceback.format_exc())

    # ── Content filtering ─────────────────────────────────────────────────

    async def filter_content(self, html: str, filter_cfg: dict) -> dict:
        await self.init()
        try:
            from crawl4ai.content_filter_strategy import PruningContentFilter, BM25ContentFilter

            filter_type = filter_cfg.get("type", "pruning")
            if filter_type == "bm25":
                query = filter_cfg.get("query", "")
                cf = BM25ContentFilter(user_query=query)
            else:
                threshold = float(filter_cfg.get("threshold", 0.48))
                cf = PruningContentFilter(threshold=threshold)

            filtered_html = cf.filter_content(html)
            return _ok(filtered_html=filtered_html)
        except Exception as exc:
            import traceback
            return _err(str(exc), traceback.format_exc())

    # ── Text chunking ──────────────────────────────────────────────────────

    async def chunk_text(self, text: str, strategy_cfg: dict) -> dict:
        await self.init()
        try:
            from crawl4ai.chunking_strategy import (
                RegexChunking,
                NlpSentenceChunking,
                FixedLengthWordChunking,
                SlidingWindowChunking,
                IdentityChunking,
            )

            strategy_type = strategy_cfg.get("type", "regex")
            if strategy_type == "nlp_sentence":
                chunker = NlpSentenceChunking()
            elif strategy_type == "fixed_length":
                chunker = FixedLengthWordChunking(
                    chunk_size=int(strategy_cfg.get("chunk_size", 500))
                )
            elif strategy_type == "sliding_window":
                chunker = SlidingWindowChunking(
                    window_size=int(strategy_cfg.get("window_size", 500)),
                    step=int(strategy_cfg.get("step", 250)),
                )
            elif strategy_type == "identity":
                chunker = IdentityChunking()
            else:  # "regex" (default)
                patterns = strategy_cfg.get("patterns", [r"\n\n"])
                chunker = RegexChunking(patterns=patterns)

            chunks = chunker.chunk(text)
            return _ok(chunks=chunks, count=len(chunks))
        except Exception as exc:
            import traceback
            return _err(str(exc), traceback.format_exc())

    # ── URL seeding ────────────────────────────────────────────────────────

    async def seed_urls(self, start_url: str, config: dict) -> dict:
        await self.init()
        try:
            from crawl4ai.async_url_seeder import AsyncUrlSeeder

            seeder = AsyncUrlSeeder()
            urls = await seeder.seed(
                url=start_url,
                max_depth=int(config.get("max_depth", 2)),
                max_urls=int(config.get("max_urls", 100)),
                patterns=config.get("patterns", []),
                respect_robots=bool(config.get("respect_robots", True)),
            )
            return _ok(urls=list(urls), count=len(urls))
        except Exception as exc:
            import traceback
            return _err(str(exc), traceback.format_exc())

    # ── URL scoring ────────────────────────────────────────────────────────

    async def score_urls(self, urls: list[str], scorer_cfg: dict) -> dict:
        await self.init()
        try:
            scorer = _build_url_scorer(scorer_cfg)
            scored = []
            for url in urls:
                score = scorer.score(url) if scorer else 0.5
                scored.append({"url": url, "score": score})
            scored.sort(key=lambda x: x["score"], reverse=True)
            return _ok(scored=scored)
        except Exception as exc:
            import traceback
            return _err(str(exc), traceback.format_exc())

    # ── URL filtering ──────────────────────────────────────────────────────

    async def filter_urls(self, urls: list[str], filter_cfg: dict) -> dict:
        await self.init()
        try:
            from crawl4ai.deep_crawling.filters import (
                URLPatternFilter,
                DomainFilter,
                FilterChain,
            )

            filters = []
            if patterns := filter_cfg.get("url_patterns"):
                filters.append(URLPatternFilter(patterns=patterns))
            if domains := filter_cfg.get("allowed_domains"):
                filters.append(DomainFilter(allowed_domains=domains))
            if blocked := filter_cfg.get("blocked_domains"):
                filters.append(
                    DomainFilter(
                        allowed_domains=[],
                        blocked_domains=blocked,
                    )
                )

            if not filters:
                return _ok(passed=urls, rejected=[])

            chain = FilterChain(filters=filters)
            passed, rejected = [], []
            for url in urls:
                if await chain.apply(url):
                    passed.append(url)
                else:
                    rejected.append(url)
            return _ok(passed=passed, rejected=rejected)
        except Exception as exc:
            import traceback
            return _err(str(exc), traceback.format_exc())

    # ── Process pre-fetched HTML (from Android WebView) ───────────────────

    async def process_html(self, html: str, url: str, config: dict) -> dict:
        """
        Run the full crawl4ai post-processing pipeline on HTML that was already
        fetched (e.g. by Android WebView for JS-rendered pages).
        """
        await self.init()
        try:
            # 1. Scrape media & links
            from crawl4ai.content_scraping_strategy import LXMLWebScrapingStrategy
            scraper = LXMLWebScrapingStrategy()
            scrape_result = scraper.scrap(url=url, html=html)

            # 2. Generate markdown
            md_result_dict = await self.generate_markdown(html, config)

            # 3. Optional extraction
            extracted = None
            if extraction_cfg := config.get("extraction_strategy"):
                ext_result = await self.extract_content(html, extraction_cfg)
                extracted = ext_result.get("data")

            return _ok(
                url=url,
                html=html,
                markdown=md_result_dict,
                media=scrape_result.media if scrape_result else {},
                links=scrape_result.links if scrape_result else {},
                extracted_content=extracted,
                success=True,
            )
        except Exception as exc:
            import traceback
            return _err(str(exc), traceback.format_exc())

    # ── Cache management ───────────────────────────────────────────────────

    async def get_cache_stats(self) -> dict:
        await self.init()
        try:
            import aiosqlite, os
            db_path = self._cache_db_path
            if not os.path.exists(db_path):
                return _ok(total_entries=0, size_bytes=0, size_mb=0.0)

            size_bytes = os.path.getsize(db_path)
            async with aiosqlite.connect(db_path) as db:
                async with db.execute("SELECT COUNT(*) FROM crawl_cache") as cur:
                    row = await cur.fetchone()
                    total = row[0] if row else 0
            return _ok(
                total_entries=total,
                size_bytes=size_bytes,
                size_mb=round(size_bytes / (1024 * 1024), 2),
            )
        except Exception:
            return _ok(total_entries=0, size_bytes=0, size_mb=0.0)

    async def clear_cache(self, url: str | None = None) -> dict:
        await self.init()
        try:
            import aiosqlite, os
            db_path = self._cache_db_path
            if not os.path.exists(db_path):
                return _ok(cleared=0)

            async with aiosqlite.connect(db_path) as db:
                if url:
                    await db.execute(
                        "DELETE FROM crawl_cache WHERE url = ?", (url,)
                    )
                else:
                    await db.execute("DELETE FROM crawl_cache")
                cleared = db.total_changes
                await db.commit()
            return _ok(cleared=cleared)
        except Exception as exc:
            import traceback
            return _err(str(exc), traceback.format_exc())


# ── Private helpers ───────────────────────────────────────────────────────

def _build_run_config(config: dict) -> Any:
    """Convert a plain config dict into a CrawlerRunConfig."""
    try:
        from crawl4ai.async_configs import CrawlerRunConfig, CacheMode

        cache_mode_map = {
            "bypass": CacheMode.BYPASS,
            "disabled": CacheMode.DISABLED,
            "enabled": CacheMode.ENABLED,
            "write_only": CacheMode.WRITE_ONLY,
            "read_only": CacheMode.READ_ONLY,
        }
        cache_mode = cache_mode_map.get(
            str(config.get("cache_mode", "enabled")).lower(),
            CacheMode.ENABLED,
        )

        return CrawlerRunConfig(
            cache_mode=cache_mode,
            word_count_threshold=int(config.get("word_count_threshold", 10)),
            excluded_tags=config.get("excluded_tags", []),
            headers=config.get("headers", {}),
            cookies=config.get("cookies", []),
            timeout=int(config.get("timeout", 30000)),
            wait_until=config.get("wait_until", "domcontentloaded"),
            screenshot=bool(config.get("screenshot", False)),
            pdf=bool(config.get("pdf", False)),
            exclude_external_links=bool(config.get("exclude_external_links", False)),
        )
    except Exception:
        try:
            from crawl4ai.async_configs import CrawlerRunConfig
            return CrawlerRunConfig()
        except Exception:
            return None


def _build_deep_crawl_strategy(strategy_cfg: dict) -> Any:
    """Build a deep crawl strategy from config dict."""
    strategy_type = strategy_cfg.get("type", "bfs").lower()
    max_depth = int(strategy_cfg.get("max_depth", 3))
    max_pages = int(strategy_cfg.get("max_pages", 50))

    try:
        if strategy_type == "dfs":
            from crawl4ai.deep_crawling import DFSDeepCrawlStrategy
            return DFSDeepCrawlStrategy(max_depth=max_depth, max_pages=max_pages)
        elif strategy_type in ("bff", "best_first", "best-first"):
            from crawl4ai.deep_crawling import BestFirstCrawlingStrategy
            scorer = _build_url_scorer(strategy_cfg.get("scorer", {}))
            return BestFirstCrawlingStrategy(
                max_depth=max_depth,
                max_pages=max_pages,
                url_scorer=scorer,
            )
        else:  # "bfs" (default)
            from crawl4ai.deep_crawling import BFSDeepCrawlStrategy
            return BFSDeepCrawlStrategy(max_depth=max_depth, max_pages=max_pages)
    except Exception:
        return None


def _build_extraction_strategy(strategy_cfg: dict) -> Any:
    """Build an extraction strategy from config dict."""
    strategy_type = strategy_cfg.get("type", "").lower()
    try:
        if strategy_type == "css":
            from crawl4ai.extraction_strategy import JsonCssExtractionStrategy
            return JsonCssExtractionStrategy(
                schema=strategy_cfg.get("schema", {}),
            )
        elif strategy_type == "xpath":
            from crawl4ai.extraction_strategy import JsonXPathExtractionStrategy
            return JsonXPathExtractionStrategy(
                schema=strategy_cfg.get("schema", {}),
            )
        elif strategy_type == "regex":
            from crawl4ai.extraction_strategy import RegexExtractionStrategy
            return RegexExtractionStrategy(
                pattern=strategy_cfg.get("pattern", ""),
                schema=strategy_cfg.get("schema", {}),
            )
        elif strategy_type == "llm":
            from crawl4ai.extraction_strategy import LLMExtractionStrategy
            from crawl4ai.async_configs import LLMConfig
            llm_config = LLMConfig(
                provider=strategy_cfg.get("provider", "openai/gpt-4o"),
                api_token=strategy_cfg.get("api_key", ""),
                api_base=strategy_cfg.get("api_base", None),
            )
            return LLMExtractionStrategy(
                llm_config=llm_config,
                schema=strategy_cfg.get("schema", {}),
                extraction_type=strategy_cfg.get("extraction_type", "schema"),
                instruction=strategy_cfg.get("instruction", ""),
            )
        return None
    except Exception:
        return None


def _build_url_scorer(scorer_cfg: dict) -> Any:
    """Build a URL scorer from config dict."""
    scorer_type = scorer_cfg.get("type", "keyword").lower() if scorer_cfg else "keyword"
    try:
        if scorer_type == "composite":
            from crawl4ai.deep_crawling.scorers import CompositeScorer
            return CompositeScorer()
        elif scorer_type == "domain_authority":
            from crawl4ai.deep_crawling.scorers import DomainAuthorityScorer
            return DomainAuthorityScorer()
        elif scorer_type == "path_depth":
            from crawl4ai.deep_crawling.scorers import PathDepthScorer
            return PathDepthScorer()
        else:  # "keyword" (default)
            from crawl4ai.deep_crawling.scorers import KeywordRelevanceScorer
            keywords = scorer_cfg.get("keywords", []) if scorer_cfg else []
            return KeywordRelevanceScorer(keywords=keywords)
    except Exception:
        return None


def _crawl_result_to_dict(result: Any) -> dict:
    """Convert a CrawlResult object to a plain JSON-serialisable dict."""
    if result is None:
        return _err("No result returned")
    try:
        md = result.markdown
        markdown_dict = {}
        if md:
            if hasattr(md, "raw_markdown"):
                markdown_dict = {
                    "raw_markdown": md.raw_markdown or "",
                    "markdown_with_citations": md.markdown_with_citations or "",
                    "references_markdown": md.references_markdown or "",
                    "fit_markdown": md.fit_markdown or "",
                }
            else:
                markdown_dict = {"raw_markdown": str(md)}

        return {
            "success": result.success,
            "url": result.url or "",
            "html": result.html or "",
            "cleaned_html": result.cleaned_html or "",
            "markdown": markdown_dict,
            "extracted_content": result.extracted_content or "",
            "media": result.media or {},
            "links": result.links or {},
            "tables": result.tables or [],
            "metadata": result.metadata or {},
            "status_code": result.status_code,
            "response_headers": result.response_headers or {},
            "error_message": result.error_message or "",
            "cache_status": result.cache_status or "miss",
            "head_fingerprint": result.head_fingerprint or "",
        }
    except Exception as exc:
        return _err(f"Result serialisation failed: {exc}")


async def _fallback_http_crawl(url: str, config: dict) -> dict:
    """
    Minimal HTTP crawl using aiohttp when the full AsyncWebCrawler is
    unavailable (e.g. during first-time initialisation).
    """
    import aiohttp
    from crawl4ai.content_scraping_strategy import LXMLWebScrapingStrategy
    from crawl4ai.markdown_generation_strategy import DefaultMarkdownGenerator

    timeout_s = int(config.get("timeout", 30000)) / 1000
    headers = {
        "User-Agent": (
            "Mozilla/5.0 (Linux; Android 15) "
            "AppleWebKit/537.36 (KHTML, like Gecko) "
            "Chrome/131.0.0.0 Mobile Safari/537.36 Crawl4AI/1.0"
        ),
        **config.get("headers", {}),
    }

    async with aiohttp.ClientSession(headers=headers) as session:
        async with session.get(
            url,
            timeout=aiohttp.ClientTimeout(total=timeout_s),
            allow_redirects=True,
        ) as resp:
            html = await resp.text()
            status = resp.status

    scraper = LXMLWebScrapingStrategy()
    scrape_result = scraper.scrap(url=url, html=html)

    generator = DefaultMarkdownGenerator()
    md_result = generator.generate_markdown(cleaned_html=html, base_url=url)

    return {
        "success": True,
        "url": url,
        "html": html,
        "cleaned_html": html,
        "markdown": {
            "raw_markdown": md_result.raw_markdown or "",
            "markdown_with_citations": md_result.markdown_with_citations or "",
            "references_markdown": md_result.references_markdown or "",
            "fit_markdown": "",
        },
        "extracted_content": "",
        "media": scrape_result.media if scrape_result else {},
        "links": scrape_result.links if scrape_result else {},
        "tables": [],
        "metadata": {},
        "status_code": status,
        "response_headers": {},
        "error_message": "",
        "cache_status": "miss",
        "head_fingerprint": "",
    }
