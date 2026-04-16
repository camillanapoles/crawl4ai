"""
deep_crawl_bridge.py
────────────────────
Android-compatible wrappers for crawl4ai deep-crawling.

Because deep crawls can take seconds to minutes and produce many results,
this module provides both:
  • batch mode — collects all results, returns as JSON list
  • streaming mode — yields results one-by-one via a Python generator,
    which Kotlin reads by calling next_page() repeatedly until exhausted.
"""

from __future__ import annotations

import asyncio
import json
from typing import Any, Generator


# ── Streaming state (per-session) ─────────────────────────────────────────
# Maps session_id → async generator + event loop
_sessions: dict[str, dict] = {}


# ── Batch deep crawl ──────────────────────────────────────────────────────

def batch_deep_crawl(
    start_url: str,
    strategy_type: str = "bfs",
    max_depth: int = 3,
    max_pages: int = 50,
    config_json: str = "{}",
    scorer_json: str = "{}",
    url_patterns: list[str] | None = None,
    allowed_domains: list[str] | None = None,
) -> str:
    """
    Run a deep crawl and return ALL results as a JSON array.

    Parameters
    ----------
    start_url       : Seed URL
    strategy_type   : "bfs" | "dfs" | "bff"
    max_depth       : Maximum crawl depth
    max_pages       : Maximum pages to visit
    config_json     : JSON-serialised CrawlConfig
    scorer_json     : JSON scorer config (only used by "bff" strategy)
    url_patterns    : Optional list of fnmatch URL patterns to include
    allowed_domains : Optional list of allowed domains

    Returns
    -------
    JSON string: {"success": true, "results": [...], "total": N}
    """
    try:
        config = json.loads(config_json)
        scorer_cfg = json.loads(scorer_json)
        result = asyncio.run(
            _async_batch_deep_crawl(
                start_url,
                strategy_type,
                max_depth,
                max_pages,
                config,
                scorer_cfg,
                url_patterns or [],
                allowed_domains or [],
            )
        )
        return json.dumps(result)
    except Exception as exc:
        import traceback
        return json.dumps({
            "success": False,
            "error": str(exc),
            "detail": traceback.format_exc(),
        })


async def _async_batch_deep_crawl(
    start_url: str,
    strategy_type: str,
    max_depth: int,
    max_pages: int,
    config: dict,
    scorer_cfg: dict,
    url_patterns: list[str],
    allowed_domains: list[str],
) -> dict:
    from crawl4ai_engine import (
        CrawlEngine,
        _build_deep_crawl_strategy,
        _build_url_scorer,
        _crawl_result_to_dict,
        _build_run_config,
    )

    strategy_cfg = {
        "type": strategy_type,
        "max_depth": max_depth,
        "max_pages": max_pages,
        "scorer": scorer_cfg,
    }

    engine = CrawlEngine()
    await engine.init()

    run_config = _build_run_config(config)
    strategy = _build_deep_crawl_strategy(strategy_cfg)
    if strategy is None:
        return {"success": False, "error": "Failed to build deep crawl strategy"}

    # Apply URL filters if provided
    if url_patterns or allowed_domains:
        try:
            from crawl4ai.deep_crawling.filters import URLPatternFilter, DomainFilter, FilterChain
            filters = []
            if url_patterns:
                filters.append(URLPatternFilter(patterns=url_patterns))
            if allowed_domains:
                filters.append(DomainFilter(allowed_domains=allowed_domains))
            strategy.filter_chain = FilterChain(filters=filters)
        except Exception:
            pass

    run_config.deep_crawl_strategy = strategy

    results = []
    if engine._crawler is not None:
        async for result in await engine._crawler.arun(
            url=start_url, config=run_config
        ):
            results.append(_crawl_result_to_dict(result))

    return {
        "success": True,
        "results": results,
        "total": len(results),
        "strategy": strategy_type,
        "max_depth": max_depth,
    }


# ── Streaming deep crawl — page by page ───────────────────────────────────

def start_streaming_session(
    session_id: str,
    start_url: str,
    strategy_type: str = "bfs",
    max_depth: int = 3,
    max_pages: int = 50,
    config_json: str = "{}",
) -> str:
    """
    Initialise a streaming deep crawl session.

    The Kotlin side calls next_page(session_id) in a loop until
    has_more_pages(session_id) returns false.

    Returns
    -------
    JSON: {"success": true, "session_id": "..."}
    """
    try:
        if session_id in _sessions:
            return json.dumps({"success": False, "error": "Session already exists"})

        config = json.loads(config_json)
        _sessions[session_id] = {
            "iterator": _stream_generator(
                session_id, start_url, strategy_type, max_depth, max_pages, config
            ),
            "done": False,
            "pages_yielded": 0,
        }
        return json.dumps({"success": True, "session_id": session_id})
    except Exception as exc:
        import traceback
        return json.dumps({
            "success": False,
            "error": str(exc),
            "detail": traceback.format_exc(),
        })


def next_page(session_id: str) -> str:
    """
    Return the next crawled page from the streaming session.

    Returns
    -------
    JSON: {"success": true, "page": {...}, "done": false}
          or {"success": true, "page": null, "done": true} when finished.
    """
    session = _sessions.get(session_id)
    if not session:
        return json.dumps({"success": False, "error": "Session not found"})

    try:
        page = next(session["iterator"], None)
        if page is None:
            session["done"] = True
            return json.dumps({"success": True, "page": None, "done": True})
        session["pages_yielded"] += 1
        return json.dumps({"success": True, "page": page, "done": False})
    except StopIteration:
        session["done"] = True
        return json.dumps({"success": True, "page": None, "done": True})
    except Exception as exc:
        import traceback
        return json.dumps({
            "success": False,
            "error": str(exc),
            "detail": traceback.format_exc(),
        })


def close_session(session_id: str) -> str:
    """Release resources for a streaming session."""
    _sessions.pop(session_id, None)
    return json.dumps({"success": True})


def session_status(session_id: str) -> str:
    session = _sessions.get(session_id)
    if not session:
        return json.dumps({"exists": False})
    return json.dumps({
        "exists": True,
        "done": session["done"],
        "pages_yielded": session["pages_yielded"],
    })


def _stream_generator(
    session_id: str,
    start_url: str,
    strategy_type: str,
    max_depth: int,
    max_pages: int,
    config: dict,
) -> Generator[dict, None, None]:
    """Sync generator that internally drives an asyncio event loop."""
    import queue
    import threading

    result_queue: queue.Queue[dict | None] = queue.Queue()

    def run_async():
        asyncio.run(
            _async_stream_producer(
                start_url, strategy_type, max_depth, max_pages, config, result_queue
            )
        )

    thread = threading.Thread(target=run_async, daemon=True)
    thread.start()

    while True:
        item = result_queue.get()
        if item is None:
            break
        yield item

    thread.join(timeout=5)


async def _async_stream_producer(
    start_url: str,
    strategy_type: str,
    max_depth: int,
    max_pages: int,
    config: dict,
    result_queue: Any,
) -> None:
    from crawl4ai_engine import (
        CrawlEngine,
        _build_deep_crawl_strategy,
        _crawl_result_to_dict,
        _build_run_config,
    )

    engine = CrawlEngine()
    await engine.init()

    strategy_cfg = {
        "type": strategy_type,
        "max_depth": max_depth,
        "max_pages": max_pages,
    }
    strategy = _build_deep_crawl_strategy(strategy_cfg)
    run_config = _build_run_config(config)
    if strategy and run_config:
        run_config.deep_crawl_strategy = strategy

    try:
        if engine._crawler:
            async for result in await engine._crawler.arun(
                url=start_url, config=run_config
            ):
                result_queue.put(_crawl_result_to_dict(result))
    finally:
        result_queue.put(None)  # signal completion
