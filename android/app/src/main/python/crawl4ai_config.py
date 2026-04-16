"""
crawl4ai_config.py
──────────────────
Helpers for converting between Kotlin config dicts (passed via JSON) and
crawl4ai Python config objects.  Used internally by crawl4ai_engine.py.
"""

from __future__ import annotations

from typing import Any


def build_crawler_run_config(config: dict) -> Any:
    """
    Build a CrawlerRunConfig from a plain dict produced by Kotlin.

    Expected dict shape (all fields optional):
    {
      "cache_mode": "enabled" | "bypass" | "disabled" | "write_only" | "read_only",
      "word_count_threshold": 10,
      "excluded_tags": [],
      "headers": {},
      "cookies": [],
      "timeout": 30000,
      "wait_until": "domcontentloaded",
      "screenshot": false,
      "pdf": false,
      "exclude_external_links": false,
      "content_filter": { "type": "pruning" | "bm25", ... },
      "extraction_strategy": { "type": "css" | "xpath" | "regex" | "llm", ... },
      "markdown_generator": { "content_filter": {...} }
    }
    """
    try:
        from crawl4ai.async_configs import CrawlerRunConfig, CacheMode
        from crawl4ai.markdown_generation_strategy import DefaultMarkdownGenerator

        cache_mode_map = {
            "bypass": CacheMode.BYPASS,
            "disabled": CacheMode.DISABLED,
            "enabled": CacheMode.ENABLED,
            "write_only": CacheMode.WRITE_ONLY,
            "read_only": CacheMode.READ_ONLY,
            "bypassed_hits": CacheMode.BYPASSED_HITS,
        }
        cache_mode = cache_mode_map.get(
            str(config.get("cache_mode", "enabled")).lower(),
            CacheMode.ENABLED,
        )

        # Markdown generator
        md_generator = None
        if md_cfg := config.get("markdown_generator"):
            content_filter = _build_content_filter(md_cfg.get("content_filter"))
            md_generator = DefaultMarkdownGenerator(content_filter=content_filter)

        # Extraction strategy
        extraction_strategy = None
        if ext_cfg := config.get("extraction_strategy"):
            from crawl4ai_engine import _build_extraction_strategy  # local
            extraction_strategy = _build_extraction_strategy(ext_cfg)

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
            markdown_generator=md_generator,
            extraction_strategy=extraction_strategy,
        )
    except Exception as exc:
        import logging
        logging.getLogger("crawl4ai.android").warning(
            "build_crawler_run_config failed: %s — using defaults", exc
        )
        try:
            from crawl4ai.async_configs import CrawlerRunConfig
            return CrawlerRunConfig()
        except Exception:
            return None


def build_browser_config(config: dict) -> Any:
    """
    Build a BrowserConfig.  On Android this is only used when a remote
    Docker Crawl4AI server is configured; local crawls use HTTP-only.
    """
    try:
        from crawl4ai.async_configs import BrowserConfig
        return BrowserConfig(
            headless=True,
            browser_type=config.get("browser_type", "chromium"),
            viewport_width=int(config.get("viewport_width", 1280)),
            viewport_height=int(config.get("viewport_height", 800)),
            proxy_server=config.get("proxy_server"),
        )
    except Exception:
        return None


def _build_content_filter(filter_cfg: dict | None) -> Any:
    """Return a content filter object or None."""
    if not filter_cfg:
        return None
    filter_type = filter_cfg.get("type", "pruning").lower()
    try:
        if filter_type == "bm25":
            from crawl4ai.content_filter_strategy import BM25ContentFilter
            return BM25ContentFilter(
                user_query=filter_cfg.get("query", ""),
                bm25_threshold=float(filter_cfg.get("threshold", 1.0)),
            )
        else:  # "pruning"
            from crawl4ai.content_filter_strategy import PruningContentFilter
            return PruningContentFilter(
                threshold=float(filter_cfg.get("threshold", 0.48)),
                threshold_type=filter_cfg.get("threshold_type", "fixed"),
            )
    except Exception:
        return None


def serialize_crawl_config(config: Any) -> dict:
    """
    Serialise a CrawlerRunConfig to a dict compatible with the Kotlin
    CrawlConfig domain model.
    """
    if config is None:
        return {}
    try:
        return config.to_serializable_dict()
    except Exception:
        return {}


def deserialize_crawl_config(data: dict) -> Any:
    """
    Reconstruct a CrawlerRunConfig from its serialised dict representation.
    Uses the from_serializable_dict mechanism already in crawl4ai.
    """
    try:
        from crawl4ai.async_configs import from_serializable_dict
        return from_serializable_dict(data)
    except Exception:
        return build_crawler_run_config(data)
