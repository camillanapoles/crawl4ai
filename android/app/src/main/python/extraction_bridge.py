"""
extraction_bridge.py
────────────────────
Convenient wrappers for the crawl4ai extraction strategies exposed to
Kotlin via crawl4ai_bridge.py.

Each function:
• Takes plain Python types (the caller passes JSON; the bridge parses it).
• Returns a dict (JSON-serialisable).
"""

from __future__ import annotations

from typing import Any


def run_css_extraction(html: str, schema: dict) -> dict:
    """
    Apply JsonCssExtractionStrategy to an HTML string.

    schema example:
    {
      "name": "Product",
      "baseSelector": "div.product",
      "fields": [
        {"name": "title",  "selector": "h1",      "type": "text"},
        {"name": "price",  "selector": ".price",  "type": "text"},
        {"name": "image",  "selector": "img",     "type": "attribute", "attribute": "src"}
      ]
    }
    """
    try:
        from crawl4ai.extraction_strategy import JsonCssExtractionStrategy
        strategy = JsonCssExtractionStrategy(schema=schema)
        result = strategy.extract(html, url="")
        return {"success": True, "data": result, "strategy": "css"}
    except Exception as exc:
        import traceback
        return {"success": False, "error": str(exc), "detail": traceback.format_exc()}


def run_xpath_extraction(html: str, schema: dict) -> dict:
    """
    Apply JsonXPathExtractionStrategy to an HTML string.

    schema example:
    {
      "name": "Article",
      "baseXPath": "//article",
      "fields": [
        {"name": "title", "xpath": ".//h1/text()", "type": "text"}
      ]
    }
    """
    try:
        from crawl4ai.extraction_strategy import JsonXPathExtractionStrategy
        strategy = JsonXPathExtractionStrategy(schema=schema)
        result = strategy.extract(html, url="")
        return {"success": True, "data": result, "strategy": "xpath"}
    except Exception as exc:
        import traceback
        return {"success": False, "error": str(exc), "detail": traceback.format_exc()}


def run_regex_extraction(text: str, pattern: str, schema: dict | None = None) -> dict:
    """
    Apply RegexExtractionStrategy to text (markdown or raw HTML).

    pattern should include named groups for field extraction, e.g.:
    r'Price:\\s*(?P<price>[\\d\\.]+)'
    """
    try:
        from crawl4ai.extraction_strategy import RegexExtractionStrategy
        strategy = RegexExtractionStrategy(
            pattern=pattern,
            schema=schema or {},
        )
        result = strategy.extract(text, url="")
        return {"success": True, "data": result, "strategy": "regex"}
    except Exception as exc:
        import traceback
        return {"success": False, "error": str(exc), "detail": traceback.format_exc()}


def run_llm_extraction(
    text: str,
    provider: str,
    api_key: str,
    schema: dict | None = None,
    instruction: str = "",
    api_base: str | None = None,
) -> dict:
    """
    Apply LLMExtractionStrategy to text.

    Requires an internet connection and a valid API key.
    """
    try:
        from crawl4ai.extraction_strategy import LLMExtractionStrategy
        from crawl4ai.async_configs import LLMConfig

        llm_config = LLMConfig(
            provider=provider,
            api_token=api_key,
            api_base=api_base,
        )
        strategy = LLMExtractionStrategy(
            llm_config=llm_config,
            schema=schema or {},
            instruction=instruction,
            extraction_type="schema" if schema else "block",
        )

        # LLM extraction is async internally; use asyncio.run
        import asyncio
        result = asyncio.run(_async_llm_extract(strategy, text))
        return {"success": True, "data": result, "strategy": "llm"}
    except Exception as exc:
        import traceback
        return {"success": False, "error": str(exc), "detail": traceback.format_exc()}


async def _async_llm_extract(strategy: Any, text: str) -> Any:
    return await strategy.aextract(text, url="")


def get_available_strategies() -> dict:
    """Return metadata about all available extraction strategies."""
    strategies = []

    strategy_defs = [
        {
            "id": "css",
            "name": "CSS Selectors",
            "description": "Extract structured data using CSS selector schemas. "
                           "Fast, no network required.",
            "requires_api_key": False,
            "input_format": "html",
        },
        {
            "id": "xpath",
            "name": "XPath",
            "description": "Extract data using XPath expressions. "
                           "Powerful for deeply nested structures.",
            "requires_api_key": False,
            "input_format": "html",
        },
        {
            "id": "regex",
            "name": "Regular Expressions",
            "description": "Extract data using regex patterns with named groups.",
            "requires_api_key": False,
            "input_format": "text",
        },
        {
            "id": "llm",
            "name": "LLM (AI) Extraction",
            "description": "Use a large language model to extract structured data. "
                           "Requires an API key and internet connection.",
            "requires_api_key": True,
            "input_format": "markdown",
            "supported_providers": [
                "openai/gpt-4o",
                "openai/gpt-4o-mini",
                "anthropic/claude-3-5-sonnet-20241022",
                "anthropic/claude-3-haiku-20240307",
                "gemini/gemini-2.0-flash",
                "groq/llama-3.1-70b-versatile",
                "ollama/llama3",
            ],
        },
    ]
    strategies.extend(strategy_defs)
    return {"success": True, "strategies": strategies}
