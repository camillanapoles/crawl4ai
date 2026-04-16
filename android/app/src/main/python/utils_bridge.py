"""
utils_bridge.py
───────────────
Utility functions exposed to Kotlin via Chaquopy / crawl4ai_bridge.py.

Covers URL validation, user-agent generation, metadata extraction,
HTML cleaning, and other small helpers that do not deserve their own module.
"""

from __future__ import annotations

import json
import re
from typing import Any


def validate_url(url: str) -> str:
    """
    Check whether a string is a valid HTTP/HTTPS URL.

    Returns
    -------
    JSON: {"valid": bool, "reason": "..."}
    """
    url = url.strip()
    pattern = re.compile(
        r"^https?://"
        r"(?:(?:[A-Z0-9](?:[A-Z0-9-]{0,61}[A-Z0-9])?\.)+[A-Z]{2,6}\.?|"
        r"localhost|"
        r"\d{1,3}(?:\.\d{1,3}){3})"
        r"(?::\d+)?"
        r"(?:/?|[/?]\S+)$",
        re.IGNORECASE,
    )
    if not url:
        return json.dumps({"valid": False, "reason": "Empty URL"})
    if not pattern.match(url):
        return json.dumps({"valid": False, "reason": "Not a valid HTTP/HTTPS URL"})
    return json.dumps({"valid": True, "reason": ""})


def normalize_url(url: str) -> str:
    """
    Normalise a URL (strip trailing slashes, lowercase scheme/host, etc.).

    Returns
    -------
    JSON: {"normalized": "https://example.com/path"}
    """
    try:
        from urllib.parse import urlparse, urlunparse
        parsed = urlparse(url.strip())
        normalised = urlunparse(
            parsed._replace(
                scheme=parsed.scheme.lower(),
                netloc=parsed.netloc.lower(),
                path=parsed.path.rstrip("/") or "/",
            )
        )
        return json.dumps({"success": True, "normalized": normalised})
    except Exception as exc:
        return json.dumps({"success": False, "error": str(exc)})


def extract_base_domain(url: str) -> str:
    """
    Extract the base domain from a URL.

    Returns
    -------
    JSON: {"domain": "example.com"}
    """
    try:
        from urllib.parse import urlparse
        parsed = urlparse(url)
        return json.dumps({"domain": parsed.netloc.lower()})
    except Exception as exc:
        return json.dumps({"domain": "", "error": str(exc)})


def is_external_url(url: str, base_url: str) -> str:
    """
    Return whether url belongs to a different domain than base_url.

    Returns
    -------
    JSON: {"external": bool}
    """
    try:
        from urllib.parse import urlparse
        url_domain = urlparse(url).netloc.lower()
        base_domain = urlparse(base_url).netloc.lower()
        return json.dumps({"external": url_domain != base_domain})
    except Exception:
        return json.dumps({"external": True})


def generate_user_agent(device: str = "mobile") -> str:
    """
    Generate a realistic User-Agent string.

    Parameters
    ----------
    device : "mobile" | "desktop" | "tablet"

    Returns
    -------
    JSON: {"user_agent": "Mozilla/5.0 ..."}
    """
    try:
        from fake_useragent import UserAgent
        ua = UserAgent()
        if device == "desktop":
            agent = ua.chrome
        elif device == "tablet":
            agent = ua.safari
        else:
            agent = ua.random
        return json.dumps({"user_agent": agent})
    except Exception:
        # Fallback hardcoded Android UA
        ua_str = (
            "Mozilla/5.0 (Linux; Android 15; Pixel 8) "
            "AppleWebKit/537.36 (KHTML, like Gecko) "
            "Chrome/131.0.0.0 Mobile Safari/537.36"
        )
        return json.dumps({"user_agent": ua_str})


def extract_html_metadata(html: str, url: str = "") -> str:
    """
    Extract Open Graph, Twitter Card, and JSON-LD metadata from an HTML string.

    Returns
    -------
    JSON: {"title": "...", "description": "...", "og": {...}, "json_ld": [...]}
    """
    try:
        from bs4 import BeautifulSoup
        soup = BeautifulSoup(html, "lxml")

        # Title
        title_tag = soup.find("title")
        title = title_tag.get_text(strip=True) if title_tag else ""

        # Meta description
        desc_tag = soup.find("meta", attrs={"name": "description"})
        description = desc_tag.get("content", "") if desc_tag else ""

        # Open Graph
        og: dict[str, str] = {}
        for tag in soup.find_all("meta", attrs={"property": re.compile(r"^og:")}):
            prop = tag.get("property", "")[3:]  # strip "og:"
            og[prop] = tag.get("content", "")

        # Twitter Card
        twitter: dict[str, str] = {}
        for tag in soup.find_all("meta", attrs={"name": re.compile(r"^twitter:")}):
            name = tag.get("name", "")[8:]  # strip "twitter:"
            twitter[name] = tag.get("content", "")

        # JSON-LD
        json_ld: list[Any] = []
        for script in soup.find_all("script", type="application/ld+json"):
            try:
                data = json.loads(script.string or "")
                json_ld.append(data)
            except Exception:
                pass

        return json.dumps({
            "success": True,
            "title": title,
            "description": description,
            "og": og,
            "twitter": twitter,
            "json_ld": json_ld,
        })
    except Exception as exc:
        import traceback
        return json.dumps({"success": False, "error": str(exc), "detail": traceback.format_exc()})


def clean_html(html: str) -> str:
    """
    Remove scripts, styles, comments, and other noise from HTML.

    Returns
    -------
    JSON: {"cleaned": "..."}
    """
    try:
        from bs4 import BeautifulSoup, Comment
        soup = BeautifulSoup(html, "lxml")

        # Remove scripts, styles, and noscript
        for tag in soup.find_all(["script", "style", "noscript"]):
            tag.decompose()

        # Remove HTML comments
        for comment in soup.find_all(string=lambda text: isinstance(text, Comment)):
            comment.extract()

        cleaned = str(soup)
        return json.dumps({"success": True, "cleaned": cleaned})
    except Exception as exc:
        import traceback
        return json.dumps({"success": False, "error": str(exc), "detail": traceback.format_exc()})


def html_to_text(html: str) -> str:
    """
    Convert HTML to plain text (without Markdown formatting).

    Returns
    -------
    JSON: {"text": "..."}
    """
    try:
        from bs4 import BeautifulSoup
        soup = BeautifulSoup(html, "lxml")
        text = soup.get_text(separator="\n", strip=True)
        return json.dumps({"success": True, "text": text})
    except Exception as exc:
        return json.dumps({"success": False, "error": str(exc)})


def estimate_token_count(text: str, model: str = "gpt-4o") -> str:
    """
    Estimate the number of tokens in a text string.
    Uses a simple heuristic (≈ 4 chars per token) when tiktoken is unavailable.

    Returns
    -------
    JSON: {"tokens": N, "method": "heuristic" | "tiktoken"}
    """
    try:
        import tiktoken
        enc = tiktoken.encoding_for_model(model)
        tokens = len(enc.encode(text))
        return json.dumps({"tokens": tokens, "method": "tiktoken"})
    except Exception:
        # Heuristic fallback
        tokens = max(1, len(text) // 4)
        return json.dumps({"tokens": tokens, "method": "heuristic"})


def get_crawl4ai_info() -> str:
    """
    Return version and capability information about the crawl4ai installation.

    Returns
    -------
    JSON: {"version": "...", "capabilities": [...]}
    """
    info: dict[str, Any] = {
        "success": True,
        "version": "unknown",
        "python_version": "",
        "capabilities": [],
    }

    try:
        import sys
        info["python_version"] = sys.version

        from crawl4ai import __version__
        info["version"] = __version__
    except Exception:
        pass

    capabilities = []

    # Check HTTP crawling
    try:
        from crawl4ai.crawlers import AsyncHTTPCrawlerStrategy  # noqa: F401
        capabilities.append("http_crawl")
    except Exception:
        pass

    # Check LXML scraping
    try:
        from crawl4ai.content_scraping_strategy import LXMLWebScrapingStrategy  # noqa: F401
        capabilities.append("lxml_scraping")
    except Exception:
        pass

    # Check Markdown generation
    try:
        from crawl4ai.markdown_generation_strategy import DefaultMarkdownGenerator  # noqa: F401
        capabilities.append("markdown_generation")
    except Exception:
        pass

    # Check deep crawling
    try:
        from crawl4ai.deep_crawling import BFSDeepCrawlStrategy  # noqa: F401
        capabilities.append("deep_crawl_bfs")
    except Exception:
        pass

    # Check extraction strategies
    try:
        from crawl4ai.extraction_strategy import (  # noqa: F401
            JsonCssExtractionStrategy,
            JsonXPathExtractionStrategy,
            RegexExtractionStrategy,
        )
        capabilities.extend(["extraction_css", "extraction_xpath", "extraction_regex"])
    except Exception:
        pass

    # Check LLM extraction (optional)
    try:
        from crawl4ai.extraction_strategy import LLMExtractionStrategy  # noqa: F401
        capabilities.append("extraction_llm")
    except Exception:
        pass

    # Check content filtering
    try:
        from crawl4ai.content_filter_strategy import PruningContentFilter, BM25ContentFilter  # noqa: F401
        capabilities.extend(["filter_pruning", "filter_bm25"])
    except Exception:
        pass

    # Check chunking
    try:
        from crawl4ai.chunking_strategy import RegexChunking, NlpSentenceChunking  # noqa: F401
        capabilities.extend(["chunking_regex", "chunking_nlp"])
    except Exception:
        pass

    info["capabilities"] = capabilities
    return json.dumps(info)
