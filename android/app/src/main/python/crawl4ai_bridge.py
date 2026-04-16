"""
crawl4ai_bridge.py
──────────────────
Entry-point called from Kotlin via Chaquopy.

Every public function:
• Accepts plain Python types (str, dict, list) so that Chaquopy's default
  type-conversion rules apply — no custom Java/Kotlin imports needed.
• Returns a JSON string so the Kotlin side can deserialise with kotlinx.
• Wraps all exceptions and surfaces them as JSON error objects so that the
  Kotlin caller always gets a predictable response shape.

Android constraints
───────────────────
• Playwright is NOT available on Android — all crawling uses HTTPCrawlerConfig
  (aiohttp / httpx under the hood).
• JavaScript rendering is handled by the Android WebView on the Kotlin side
  when explicitly requested.
• asyncio must be run with asyncio.run() or an explicit event loop because
  Chaquopy calls are made from Kotlin threads.
"""

from __future__ import annotations

import asyncio
import json
import traceback
from typing import Any

# Lazy-initialise the engine so the first call bears the startup cost.
_engine: "CrawlEngine | None" = None  # noqa: F821


def _get_engine() -> "CrawlEngine":  # noqa: F821
    global _engine
    if _engine is None:
        from crawl4ai_engine import CrawlEngine  # local module in /python
        _engine = CrawlEngine()
    return _engine


def _json_error(msg: str, detail: str = "") -> str:
    return json.dumps({"success": False, "error": msg, "detail": detail})


def _run(coro: Any) -> Any:
    """Run an async coroutine from a synchronous (Kotlin/Chaquopy) context."""
    try:
        loop = asyncio.get_event_loop()
        if loop.is_running():
            # If there is already a running loop (rare on Android), use a new
            # thread-bound loop to avoid nest_asyncio complications.
            import concurrent.futures
            with concurrent.futures.ThreadPoolExecutor(max_workers=1) as pool:
                future = pool.submit(asyncio.run, coro)
                return future.result()
        return loop.run_until_complete(coro)
    except RuntimeError:
        return asyncio.run(coro)


# ── Public API — called from Kotlin ───────────────────────────────────────


def get_version() -> str:
    """Return the crawl4ai version string."""
    try:
        from crawl4ai import __version__
        return json.dumps({"success": True, "version": __version__})
    except Exception as exc:
        return _json_error("Failed to get version", str(exc))


def init_engine() -> str:
    """Pre-warm the engine (NLTK data, import chains, etc.)."""
    try:
        engine = _get_engine()
        result = _run(engine.init())
        return json.dumps({"success": True, "ready": result})
    except Exception as exc:
        return _json_error("Engine init failed", traceback.format_exc())


def crawl_url(url: str, config_json: str = "{}") -> str:
    """
    Crawl a single URL.

    Parameters
    ----------
    url         : Target URL string
    config_json : JSON-serialised CrawlConfig (Kotlin domain model)

    Returns
    -------
    JSON string of CrawlResponse (Kotlin domain model shape)
    """
    try:
        config = json.loads(config_json)
        result = _run(_get_engine().crawl_url(url, config))
        return json.dumps(result)
    except Exception as exc:
        return _json_error(f"crawl_url failed for {url}", traceback.format_exc())


def crawl_urls(urls_json: str, config_json: str = "{}") -> str:
    """
    Crawl multiple URLs concurrently.

    Parameters
    ----------
    urls_json   : JSON array of URL strings
    config_json : JSON-serialised CrawlConfig

    Returns
    -------
    JSON array of CrawlResponse objects
    """
    try:
        urls = json.loads(urls_json)
        config = json.loads(config_json)
        results = _run(_get_engine().crawl_urls(urls, config))
        return json.dumps(results)
    except Exception as exc:
        return _json_error("crawl_urls failed", traceback.format_exc())


def deep_crawl(
    start_url: str,
    strategy_json: str = "{}",
    config_json: str = "{}",
) -> str:
    """
    Execute a deep (multi-level) crawl.

    Returns a JSON array of CrawlResponse objects (one per visited page).
    For large crawls this can be memory-intensive; prefer deep_crawl_stream()
    when the Kotlin side can process pages as they arrive.
    """
    try:
        strategy = json.loads(strategy_json)
        config = json.loads(config_json)
        results = _run(_get_engine().deep_crawl(start_url, strategy, config))
        return json.dumps(results)
    except Exception as exc:
        return _json_error("deep_crawl failed", traceback.format_exc())


def extract_content(html: str, strategy_json: str = "{}") -> str:
    """
    Run an extraction strategy against raw HTML.

    Parameters
    ----------
    html          : Raw HTML string
    strategy_json : JSON describing the strategy type and its parameters

    Returns
    -------
    JSON object with {"success": true, "data": <extracted>, "strategy": <name>}
    """
    try:
        strategy_config = json.loads(strategy_json)
        result = _run(_get_engine().extract_content(html, strategy_config))
        return json.dumps(result)
    except Exception as exc:
        return _json_error("extract_content failed", traceback.format_exc())


def generate_markdown(html: str, config_json: str = "{}") -> str:
    """
    Convert HTML to Markdown using DefaultMarkdownGenerator.

    Returns
    -------
    JSON with {raw_markdown, markdown_with_citations, references_markdown,
               fit_markdown}
    """
    try:
        config = json.loads(config_json)
        result = _run(_get_engine().generate_markdown(html, config))
        return json.dumps(result)
    except Exception as exc:
        return _json_error("generate_markdown failed", traceback.format_exc())


def filter_content(html: str, filter_json: str = "{}") -> str:
    """
    Apply a content filter (Pruning or BM25) to raw HTML.

    Returns
    -------
    JSON with {"success": true, "filtered_html": "..."}
    """
    try:
        filter_config = json.loads(filter_json)
        result = _run(_get_engine().filter_content(html, filter_config))
        return json.dumps(result)
    except Exception as exc:
        return _json_error("filter_content failed", traceback.format_exc())


def chunk_text(text: str, strategy_json: str = "{}") -> str:
    """
    Split text into chunks using the specified chunking strategy.

    Returns
    -------
    JSON with {"success": true, "chunks": ["chunk1", "chunk2", ...]}
    """
    try:
        strategy_config = json.loads(strategy_json)
        result = _run(_get_engine().chunk_text(text, strategy_config))
        return json.dumps(result)
    except Exception as exc:
        return _json_error("chunk_text failed", traceback.format_exc())


def seed_urls(start_url: str, config_json: str = "{}") -> str:
    """
    Discover URLs reachable from start_url using AsyncUrlSeeder.

    Returns
    -------
    JSON with {"success": true, "urls": ["url1", "url2", ...]}
    """
    try:
        config = json.loads(config_json)
        result = _run(_get_engine().seed_urls(start_url, config))
        return json.dumps(result)
    except Exception as exc:
        return _json_error("seed_urls failed", traceback.format_exc())


def score_urls(urls_json: str, scorer_json: str = "{}") -> str:
    """
    Score a list of URLs using the specified scorer configuration.

    Returns
    -------
    JSON with {"success": true, "scored": [{"url": "...", "score": 0.9}, ...]}
    """
    try:
        urls = json.loads(urls_json)
        scorer_config = json.loads(scorer_json)
        result = _run(_get_engine().score_urls(urls, scorer_config))
        return json.dumps(result)
    except Exception as exc:
        return _json_error("score_urls failed", traceback.format_exc())


def filter_urls(urls_json: str, filter_json: str = "{}") -> str:
    """
    Apply URL filters (pattern, domain, content-type) to a list of URLs.

    Returns
    -------
    JSON with {"success": true, "passed": [...], "rejected": [...]}
    """
    try:
        urls = json.loads(urls_json)
        filter_config = json.loads(filter_json)
        result = _run(_get_engine().filter_urls(urls, filter_config))
        return json.dumps(result)
    except Exception as exc:
        return _json_error("filter_urls failed", traceback.format_exc())


def get_cache_stats() -> str:
    """
    Return cache statistics from the SQLite-based cache.

    Returns
    -------
    JSON with {"success": true, "total_entries": N, "size_bytes": M, ...}
    """
    try:
        result = _run(_get_engine().get_cache_stats())
        return json.dumps(result)
    except Exception as exc:
        return _json_error("get_cache_stats failed", traceback.format_exc())


def clear_cache(url: str = "") -> str:
    """
    Clear cached data. If url is empty, clear everything.

    Returns
    -------
    JSON with {"success": true, "cleared": N}
    """
    try:
        result = _run(_get_engine().clear_cache(url if url else None))
        return json.dumps(result)
    except Exception as exc:
        return _json_error("clear_cache failed", traceback.format_exc())


def process_html(html: str, url: str = "", config_json: str = "{}") -> str:
    """
    Run the full crawl4ai post-processing pipeline (scraping → markdown →
    extraction) against an HTML string that was already fetched by the
    Android WebView (used for JS-rendered pages).

    Returns a CrawlResponse JSON.
    """
    try:
        config = json.loads(config_json)
        result = _run(_get_engine().process_html(html, url, config))
        return json.dumps(result)
    except Exception as exc:
        return _json_error("process_html failed", traceback.format_exc())
