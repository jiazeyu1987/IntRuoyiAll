#!/usr/bin/env python3
"""Read-only completion gate for the P0 production execution loop."""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Any
from urllib.parse import urlparse

SCRIPT_DIR = Path(__file__).resolve().parent
WORKSPACE_ROOT = Path(__file__).resolve().parents[3]
DEFAULT_TASK_DIR = WORKSPACE_ROOT / "doc" / "tasks" / "20260803-p0-production-execution-loop-implementation"
DEFAULT_RUNTIME_VERIFIER = SCRIPT_DIR / "verify_p0_runtime_migration.py"
GENERATED_AT_MAX_EVIDENCE_MTIME_SKEW = timedelta(hours=6)
REAL_E2E_RESULT_RELATIVE_PATH = (
    Path("IntRuoyiFronted") / "test-results" / "p0-production-execution-loop-real" / "result.json"
)

REQUIRED_CLOSURE_ANSWERS = [
    "who",
    "device",
    "process",
    "quantity",
    "quality",
    "signature",
    "workOrder",
    "review",
    "batchRecord",
]

REQUIRED_TARGET_REQUEST_LABELS = [
    "FRONTLINE_SUBMIT_ENDPOINT",
    "PQC_SUBMIT_ENDPOINT",
    "TEAM_LEADER_REVIEW_ENDPOINT",
    "TEAM_LEADER_ALLOCATION_CONFIRM_ENDPOINT",
    "PRODUCTION_EXECUTION_TRACE_ENDPOINT",
]
REQUIRED_TARGET_REQUEST_ENDPOINTS = {
    "FRONTLINE_SUBMIT_ENDPOINT": "/mes/pro/feedback/frontline/submit",
    "PQC_SUBMIT_ENDPOINT": "/mes/pro/feedback/frontline/device-account/pqc/submit",
    "TEAM_LEADER_REVIEW_ENDPOINT": "/mes/pro/process-pool/team-leader/submission/review",
    "TEAM_LEADER_ALLOCATION_CONFIRM_ENDPOINT": "/mes/pro/process-pool/team-leader/submission/allocation/confirm",
    "PRODUCTION_EXECUTION_TRACE_ENDPOINT": "/mes/pro/process-pool/team-leader/production-execution/trace",
}
REQUIRED_TARGET_REQUEST_METHODS = {
    "FRONTLINE_SUBMIT_ENDPOINT": "POST",
    "PQC_SUBMIT_ENDPOINT": "POST",
    "TEAM_LEADER_REVIEW_ENDPOINT": "POST",
    "TEAM_LEADER_ALLOCATION_CONFIRM_ENDPOINT": "POST",
    "PRODUCTION_EXECUTION_TRACE_ENDPOINT": "GET",
}
REQUIRED_TARGET_RESPONSE_IDENTITIES = {
    "FRONTLINE_SUBMIT_ENDPOINT": {
        "field": "processPoolEventId",
        "mustMatchProcessPoolEventId": True,
    },
    "PQC_SUBMIT_ENDPOINT": {
        "field": "pqcEventId",
        "mustMatchProcessPoolEventId": False,
    },
    "TEAM_LEADER_REVIEW_ENDPOINT": {
        "field": "reviewId",
        "mustMatchProcessPoolEventId": False,
    },
    "TEAM_LEADER_ALLOCATION_CONFIRM_ENDPOINT": {
        "field": "reviewId",
        "mustMatchProcessPoolEventId": False,
    },
    "PRODUCTION_EXECUTION_TRACE_ENDPOINT": {
        "field": "processPoolEventId",
        "mustMatchProcessPoolEventId": True,
    },
}
REQUIRED_IDEMPOTENCY_RESULT_FIELDS = {
    "Submit Idempotency Key Configured": "submitIdempotencyKey",
    "PQC Idempotency Key Configured": "pqcIdempotencyKey",
    "Confirm Idempotency Key Configured": "confirmIdempotencyKey",
}
REQUIRED_DUPLICATE_RESULT_FIELDS = {
    "Duplicate Production Submit Verified": "duplicateProductionSubmitVerified",
    "Duplicate PQC Submit Verified": "duplicatePqcSubmitVerified",
    "Duplicate FIFO Confirm Rejected": "duplicateConfirmRejected",
}
REQUIRED_RUNTIME_MIGRATION_COUNT_FIELDS = {
    "Required Columns": {
        "resultKey": "requiredColumns",
        "blockerCode": "P0_COMPLETION_REAL_E2E_RESULT_RUNTIME_MIGRATION_COLUMNS_MISMATCH",
    },
    "Required Indexes": {
        "resultKey": "requiredIndexes",
        "blockerCode": "P0_COMPLETION_REAL_E2E_RESULT_RUNTIME_MIGRATION_INDEXES_MISMATCH",
    },
    "Historical Checks": {
        "resultKey": "historicalChecks",
        "blockerCode": "P0_COMPLETION_REAL_E2E_RESULT_RUNTIME_MIGRATION_HISTORICAL_CHECKS_MISMATCH",
    },
}
REQUIRED_BROWSER_ROUTE_SKELETON = [
    "/login",
    "/mes/pro/process-pool/team-leader",
    "/mes/pro/feedback/edhr-batch-production-fill",
    "/mes/pro/feedback/edhr-batch-pqc-fill",
    "/mes/pro/process-pool/timeline",
]

ALLOWED_RUNTIME_URL_PAIRS = {
    "http://127.0.0.1:8092": "http://127.0.0.1:48092",
    "http://localhost:8092": "http://127.0.0.1:48092",
    "http://127.0.0.1:8081": "http://127.0.0.1:48081",
    "http://localhost:8081": "http://127.0.0.1:48081",
}

sys.path.insert(0, str(SCRIPT_DIR))
from verify_p0_tdd_evidence_gate import evaluate as evaluate_tdd_evidence  # noqa: E402


def markdown_value(text: str, label: str) -> str | None:
    pattern = re.compile(rf"^- {re.escape(label)}:\s+`([^`]*)`", re.MULTILINE)
    match = pattern.search(text)
    return match.group(1) if match else None


def markdown_section(text: str, heading: str) -> str:
    marker = f"## {heading}"
    start = text.find(marker)
    if start < 0:
        return ""
    next_heading = text.find("\n## ", start + len(marker))
    return text[start:] if next_heading < 0 else text[start:next_heading]


def as_positive_int(value: str | None) -> int | None:
    if value is None or not value.isdigit():
        return None
    parsed = int(value)
    return parsed if parsed > 0 else None


def as_int(value: str | None) -> int | None:
    if value is None or not re.fullmatch(r"-?\d+", value):
        return None
    return int(value)


def as_positive_int_value(value: Any) -> int | None:
    if isinstance(value, bool) or value is None:
        return None
    if isinstance(value, int):
        return value if value > 0 else None
    return as_positive_int(str(value))


def as_int_value(value: Any) -> int | None:
    if isinstance(value, bool) or value is None:
        return None
    if isinstance(value, int):
        return value
    return as_int(str(value))


def has_result_evidence_value(value: Any) -> bool:
    if isinstance(value, bool):
        return value
    if value is None:
        return False
    if isinstance(value, str):
        return value.strip() not in ["", "--", "null", "None", "MISSING"]
    return bool(value)


def find_result_target_requests(target_requests: list[Any], label: str, endpoint: str) -> list[dict[str, Any]]:
    matches: list[dict[str, Any]] = []
    for item in target_requests:
        if not isinstance(item, dict):
            continue
        if str(item.get("label") or "") != label:
            continue
        url = str(item.get("url") or "")
        if normalize_url_path(url) == endpoint:
            matches.append(item)
    return matches


def normalize_url_path(value: str) -> str:
    if value.startswith("http://") or value.startswith("https://"):
        return urlparse(value).path.rstrip("/") or "/"
    return value.split("?", 1)[0].split("#", 1)[0].rstrip("/") or "/"


def is_expected_result_target_request(item: Any) -> bool:
    if not isinstance(item, dict):
        return False
    label = str(item.get("label") or "")
    expected_endpoint = REQUIRED_TARGET_REQUEST_ENDPOINTS.get(label)
    if expected_endpoint is None:
        return False
    return normalize_url_path(str(item.get("url") or "")) == expected_endpoint


def count_result_diagnostic_items(diagnostics: Any, key: str) -> int | None:
    if not isinstance(diagnostics, dict):
        return None
    items = diagnostics.get(key)
    if not isinstance(items, list):
        return None
    return len(items)


def count_result_list_items(payload: Any, key: str) -> int | None:
    if not isinstance(payload, dict):
        return None
    items = payload.get(key)
    if not isinstance(items, list):
        return None
    return len(items)


def count_closure_source_ids(source_ids: Any) -> int | None:
    if isinstance(source_ids, dict):
        return len(
            [
                value
                for value in source_ids.values()
                if value is not None and str(value).strip() not in ["", "--", "null", "None"]
            ]
        )
    if isinstance(source_ids, list):
        return len(
            [
                value
                for value in source_ids
                if value is not None and str(value).strip() not in ["", "--", "null", "None"]
            ]
        )
    return None


def normalize_browser_route_step(step: Any) -> str | None:
    if isinstance(step, dict):
        raw = step.get("route") or step.get("url") or step.get("currentUrl")
    else:
        raw = step
    if raw is None:
        return None
    value = str(raw).strip()
    if value in ["", "--", "null", "None"]:
        return None
    return normalize_url_path(value)


def extract_browser_route_paths(route_steps: Any) -> list[str]:
    if not isinstance(route_steps, list):
        return []
    routes: list[str] = []
    for step in route_steps:
        route = normalize_browser_route_step(step)
        if route is not None:
            routes.append(route)
    return routes


def has_formal_markdown_value(value: str | None) -> bool:
    return value is not None and value.strip() not in ["", "--", "null", "None"]


def is_allowed_runtime_url_pair(frontend_url: str | None, backend_url: str | None) -> bool:
    if not has_formal_markdown_value(frontend_url) or not has_formal_markdown_value(backend_url):
        return False
    return ALLOWED_RUNTIME_URL_PAIRS.get(frontend_url or "") == backend_url


def is_browser_preflight_under_frontend(browser_preflight: str | None, frontend_url: str | None) -> bool:
    if not has_formal_markdown_value(browser_preflight) or not has_formal_markdown_value(frontend_url):
        return False
    frontend_base = (frontend_url or "").rstrip("/")
    browser_url = browser_preflight or ""
    return browser_url == frontend_base or any(
        browser_url.startswith(f"{frontend_base}{separator}") for separator in ["/", "?", "#"]
    )


def is_target_request_under_backend(
    target_request_url: str | None,
    backend_url: str | None,
    endpoint: str,
) -> bool:
    if not has_formal_markdown_value(target_request_url) or not has_formal_markdown_value(backend_url):
        return False
    expected_url = f"{(backend_url or '').rstrip('/')}{endpoint}"
    actual_url = target_request_url or ""
    return actual_url == expected_url or actual_url.startswith(f"{expected_url}?")


def parse_iso_utc_datetime(value: str | None) -> datetime | None:
    if not has_formal_markdown_value(value) or value is None:
        return None
    if not value.endswith("Z"):
        return None
    try:
        parsed = datetime.fromisoformat(value.replace("Z", "+00:00"))
    except ValueError:
        return None
    return parsed.astimezone(timezone.utc)


def parse_iso_utc_timestamp(value: str | None) -> str | None:
    parsed = parse_iso_utc_datetime(value)
    return value if parsed is not None else None


def evaluate_generated_at_freshness(parsed_generated_at: datetime, evidence_path: Path) -> tuple[bool, bool, str]:
    evidence_mtime = datetime.fromtimestamp(evidence_path.stat().st_mtime, tz=timezone.utc)
    return (
        evidence_mtime - parsed_generated_at > GENERATED_AT_MAX_EVIDENCE_MTIME_SKEW,
        parsed_generated_at - evidence_mtime > GENERATED_AT_MAX_EVIDENCE_MTIME_SKEW,
        evidence_mtime.isoformat(timespec="seconds").replace("+00:00", "Z"),
    )


def resolve_existing_evidence_path(value: str, task_dir: Path) -> Path | None:
    path = Path(value)
    if path.is_absolute():
        return path if path.exists() else None

    candidates = [task_dir / path, WORKSPACE_ROOT / path]
    if len(task_dir.parents) >= 3:
        candidates.append(task_dir.parents[2] / path)
    for candidate in candidates:
        if candidate.exists():
            return candidate
    return None


def resolve_real_e2e_result_path(task_dir: Path) -> Path | None:
    task_root = task_dir.parents[2] if len(task_dir.parents) >= 3 else WORKSPACE_ROOT
    candidate = task_root / REAL_E2E_RESULT_RELATIVE_PATH
    if candidate.exists():
        return candidate
    return None


def evaluate_migration_policy_evidence(path: Path) -> tuple[str, list[str]]:
    text = path.read_text(encoding="utf-8")
    markers: list[str] = []
    if re.search(r"\bPASS\b", text) is None:
        markers.append("PASS_MISSING")
    if re.search(r"\b(BLOCKED|FAIL|FAILED)\b", text):
        markers.append("BLOCKED_OR_FAIL_MARKER_PRESENT")
    return ("PASS" if not markers else "BLOCKED", markers)


def parse_batch_record_binding(text: str) -> dict[str, int | None]:
    match = re.search(
        r"^- Batch Record Binding:\s+report=`([^`]*)`,\s+definition=`([^`]*)`,\s+version=`([^`]*)`",
        text,
        re.MULTILINE,
    )
    if not match:
        return {"report": None, "definition": None, "version": None}
    return {
        "report": as_positive_int(match.group(1)),
        "definition": as_positive_int(match.group(2)),
        "version": as_positive_int(match.group(3)),
    }


def add_blocker(blockers: list[dict[str, Any]], code: str, message: str, **extra: Any) -> None:
    item = {"code": code, "message": message}
    item.update(extra)
    blockers.append(item)


def evaluate_task_status(task_dir: Path) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    blockers: list[dict[str, Any]] = []
    path = task_dir / "task.md"
    if not path.exists():
        return {"status": "MISSING"}, [
            {
                "code": "P0_COMPLETION_TASK_DOC_MISSING",
                "message": "task.md is required before P0 completion can be claimed.",
            }
        ]
    text = path.read_text(encoding="utf-8")
    current_status_match = re.search(r"## Current Status\s+([\s\S]*?)(?:\n## |\Z)", text)
    current_status = current_status_match.group(1).strip() if current_status_match else ""
    current_status_line = next((line.strip() for line in current_status.splitlines() if line.strip()), "")
    current_status_token_match = re.match(r"^([A-Za-z_]+)", current_status_line)
    current_status_token = current_status_token_match.group(1) if current_status_token_match else ""
    if current_status_token not in ["ready_for_closeout", "completed"]:
        add_blocker(
            blockers,
            "P0_COMPLETION_TASK_STATUS_NOT_READY",
            "Task status must be ready_for_closeout or completed before completion gate can PASS.",
            parsedStatus=current_status_token or "MISSING",
            currentStatus=current_status[:240],
        )
    return {"status": current_status or "UNKNOWN", "parsedStatus": current_status_token or "UNKNOWN"}, blockers


def evaluate_real_e2e(task_dir: Path) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    blockers: list[dict[str, Any]] = []
    path = task_dir / "p0-real-e2e-evidence.md"
    if not path.exists():
        return {"status": "MISSING"}, [
            {
                "code": "P0_COMPLETION_REAL_E2E_EVIDENCE_MISSING",
                "message": "p0-real-e2e-evidence.md is required before P0 completion can PASS.",
            }
        ]
    text = path.read_text(encoding="utf-8")
    top_status = markdown_value(text, "Status")
    if top_status != "PASS":
        add_blocker(
            blockers,
            "P0_COMPLETION_REAL_E2E_NOT_PASS",
            "Real P0 Playwright E2E evidence must have top-level Status PASS.",
            actual=top_status or "MISSING",
        )
    generated_at = markdown_value(text, "Generated At")
    parsed_generated_at_dt = parse_iso_utc_datetime(generated_at)
    parsed_generated_at = generated_at if parsed_generated_at_dt is not None else None
    if parsed_generated_at is None:
        add_blocker(
            blockers,
            "P0_COMPLETION_GENERATED_AT_MISSING",
            "Real E2E evidence must include a valid ISO UTC Generated At timestamp from this run.",
            actual=generated_at or "MISSING",
        )
    else:
        stale_generated_at, future_generated_at, evidence_mtime = evaluate_generated_at_freshness(
            parsed_generated_at_dt,
            path,
        )
        if stale_generated_at:
            add_blocker(
                blockers,
                "P0_COMPLETION_GENERATED_AT_STALE",
                "Real E2E evidence Generated At is too old for the current evidence file.",
                actual=generated_at,
                evidenceFileModifiedAt=evidence_mtime,
            )
        if future_generated_at:
            add_blocker(
                blockers,
                "P0_COMPLETION_GENERATED_AT_FUTURE",
                "Real E2E evidence Generated At is too far in the future for the current evidence file.",
                actual=generated_at,
                evidenceFileModifiedAt=evidence_mtime,
            )
    completion_metadata: dict[str, str | None] = {}
    for label, code in [
        ("Frontend", "P0_COMPLETION_FRONTEND_URL_MISSING"),
        ("Backend", "P0_COMPLETION_BACKEND_URL_MISSING"),
        ("Tenant", "P0_COMPLETION_TENANT_MISSING"),
        ("User", "P0_COMPLETION_USER_MISSING"),
    ]:
        actual = markdown_value(text, label)
        completion_metadata[label] = actual
        if not has_formal_markdown_value(actual):
            add_blocker(blockers, code, f"{label} must be present in real E2E evidence.", actual=actual or "MISSING")
    frontend_url = completion_metadata["Frontend"]
    backend_url = completion_metadata["Backend"]
    if has_formal_markdown_value(frontend_url) and has_formal_markdown_value(backend_url):
        if not is_allowed_runtime_url_pair(frontend_url, backend_url):
            add_blocker(
                blockers,
                "P0_COMPLETION_RUNTIME_URL_PAIR_INVALID",
                "Frontend and Backend URLs must be one approved runtime pair before P0 completion.",
                frontend=frontend_url,
                backend=backend_url,
                allowedPairs=ALLOWED_RUNTIME_URL_PAIRS,
            )

    run_id = markdown_value(text, "Run ID")
    data_prefix = markdown_value(text, "Data Prefix")
    if not has_formal_markdown_value(run_id):
        add_blocker(
            blockers,
            "P0_COMPLETION_RUN_ID_MISSING",
            "Real E2E evidence must contain this run's P0_RUN_ID.",
            actual=run_id or "MISSING",
        )
    elif data_prefix != f"P0-EXEC-{run_id}":
        add_blocker(
            blockers,
            "P0_COMPLETION_DATA_PREFIX_MISMATCH",
            "Real E2E evidence Data Prefix must equal P0-EXEC-<Run ID>.",
            runId=run_id,
            dataPrefix=data_prefix or "MISSING",
        )

    device_account_id = as_positive_int(markdown_value(text, "Device Account ID"))
    if device_account_id is None:
        add_blocker(
            blockers,
            "P0_COMPLETION_DEVICE_ACCOUNT_MISSING",
            "Real E2E evidence must contain a positive device account ID.",
        )

    binding = parse_batch_record_binding(text)
    if any(value is None for value in binding.values()):
        add_blocker(
            blockers,
            "P0_COMPLETION_BATCH_RECORD_BINDING_MISSING",
            "Real E2E evidence must contain formal batch-record report, definition, and version IDs.",
            binding=binding,
        )

    schema_migration_id = markdown_value(text, "Schema Migration ID")
    if not has_formal_markdown_value(schema_migration_id):
        add_blocker(
            blockers,
            "P0_COMPLETION_SCHEMA_MIGRATION_ID_MISSING",
            "Real E2E evidence must include the applied schema migration ID.",
            actual=schema_migration_id or "MISSING",
        )

    migration_policy_evidence = markdown_value(text, "Migration Policy Evidence")
    migration_policy_evidence_status = "MISSING"
    if not has_formal_markdown_value(migration_policy_evidence):
        add_blocker(
            blockers,
            "P0_COMPLETION_MIGRATION_POLICY_EVIDENCE_MISSING",
            "Real E2E evidence must include a release migration policy evidence path.",
            actual=migration_policy_evidence or "MISSING",
        )
    else:
        migration_policy_evidence_path = resolve_existing_evidence_path(migration_policy_evidence, task_dir)
        if migration_policy_evidence_path is None:
            add_blocker(
                blockers,
                "P0_COMPLETION_MIGRATION_POLICY_EVIDENCE_MISSING",
                "Migration policy evidence path must exist before P0 completion.",
                path=migration_policy_evidence,
            )
        else:
            migration_policy_evidence_status, migration_policy_markers = evaluate_migration_policy_evidence(
                migration_policy_evidence_path,
            )
            if migration_policy_evidence_status != "PASS":
                add_blocker(
                    blockers,
                    "P0_COMPLETION_MIGRATION_POLICY_EVIDENCE_NOT_PASS",
                    "Migration policy evidence must contain an explicit PASS and no BLOCKED/FAIL markers.",
                    path=migration_policy_evidence,
                    markers=migration_policy_markers,
                )

    idempotency_evidence: dict[str, bool] = {}
    for label, code in [
        ("Submit Idempotency Key Configured", "P0_COMPLETION_SUBMIT_IDEMPOTENCY_KEY_MISSING"),
        ("PQC Idempotency Key Configured", "P0_COMPLETION_PQC_IDEMPOTENCY_KEY_MISSING"),
        ("Confirm Idempotency Key Configured", "P0_COMPLETION_CONFIRM_IDEMPOTENCY_KEY_MISSING"),
    ]:
        actual = markdown_value(text, label)
        idempotency_evidence[label] = actual == "true"
        if actual != "true":
            add_blocker(blockers, code, f"{label} must be true in real E2E evidence.", actual=actual or "MISSING")

    process_pool_event_id = as_positive_int(markdown_value(text, "processPoolEventId"))
    if process_pool_event_id is None:
        add_blocker(
            blockers,
            "P0_COMPLETION_PROCESS_POOL_EVENT_MISSING",
            "Real E2E evidence must contain a fresh positive processPoolEventId.",
        )
    duplicate_evidence: dict[str, bool] = {}
    for label, code in [
        ("Duplicate Production Submit Verified", "P0_COMPLETION_DUPLICATE_PRODUCTION_NOT_VERIFIED"),
        ("Duplicate PQC Submit Verified", "P0_COMPLETION_DUPLICATE_PQC_NOT_VERIFIED"),
        ("Duplicate FIFO Confirm Rejected", "P0_COMPLETION_DUPLICATE_FIFO_NOT_REJECTED"),
    ]:
        actual = markdown_value(text, label)
        duplicate_evidence[label] = actual == "true"
        if actual != "true":
            add_blocker(blockers, code, f"{label} must be true in real E2E evidence.", actual=actual or "MISSING")

    route_steps = as_positive_int(markdown_value(text, "Route Preflight Steps"))
    browser_preflight = markdown_value(text, "Browser Preflight")
    if route_steps is None or browser_preflight in [None, "--", ""]:
        add_blocker(
            blockers,
            "P0_COMPLETION_BROWSER_PATH_NOT_PROVEN",
            "Real E2E evidence must prove browser route steps and a real browser preflight URL.",
            browserPreflight=browser_preflight or "MISSING",
            routePreflightSteps=markdown_value(text, "Route Preflight Steps") or "MISSING",
        )
    elif has_formal_markdown_value(frontend_url) and not is_browser_preflight_under_frontend(
        browser_preflight,
        frontend_url,
    ):
        add_blocker(
            blockers,
            "P0_COMPLETION_BROWSER_PREFLIGHT_URL_MISMATCH",
            "Browser Preflight URL must be the same frontend runtime recorded by the real E2E evidence.",
            browserPreflight=browser_preflight,
            frontendUrl=frontend_url,
        )

    target_request_hits: dict[str, bool] = {}
    target_request_urls: dict[str, str] = {}
    target_request_methods: dict[str, str] = {}
    target_request_http_statuses: dict[str, int | str] = {}
    target_request_business_codes: dict[str, int | str] = {}
    target_response_identities: dict[str, int | str] = {}
    for label in REQUIRED_TARGET_REQUEST_LABELS:
        actual = markdown_value(text, f"Target Request {label} Hit")
        target_request_hits[label] = actual == "true"
        if actual != "true":
            add_blocker(
                blockers,
                "P0_COMPLETION_TARGET_REQUEST_NOT_PROVEN",
                "Every target browser/API boundary must be observed in real E2E evidence.",
                targetRequest=label,
                actual=actual or "MISSING",
            )
        target_request_url = markdown_value(text, f"Target Request {label} URL")
        target_request_urls[label] = target_request_url or "MISSING"
        endpoint = REQUIRED_TARGET_REQUEST_ENDPOINTS[label]
        if actual == "true" and not is_target_request_under_backend(target_request_url, backend_url, endpoint):
            add_blocker(
                blockers,
                "P0_COMPLETION_TARGET_REQUEST_URL_MISMATCH",
                "Target request URL must use the same backend runtime recorded by the real E2E evidence.",
                targetRequest=label,
                targetRequestUrl=target_request_url or "MISSING",
                backendUrl=backend_url or "MISSING",
                expectedEndpoint=endpoint,
            )
        target_request_method = markdown_value(text, f"Target Request {label} Method")
        target_request_methods[label] = target_request_method or "MISSING"
        expected_method = REQUIRED_TARGET_REQUEST_METHODS[label]
        if actual == "true" and (target_request_method or "").upper() != expected_method:
            add_blocker(
                blockers,
                "P0_COMPLETION_TARGET_REQUEST_METHOD_MISMATCH",
                "Target request HTTP method must match the formal P0 write/read boundary.",
                targetRequest=label,
                targetRequestMethod=target_request_method or "MISSING",
                expectedMethod=expected_method,
            )
        target_request_http_status = as_positive_int(markdown_value(text, f"Target Request {label} HTTP Status"))
        target_request_http_statuses[label] = target_request_http_status or "MISSING"
        if actual == "true" and (target_request_http_status is None or not 200 <= target_request_http_status < 300):
            add_blocker(
                blockers,
                "P0_COMPLETION_TARGET_REQUEST_HTTP_STATUS_NOT_OK",
                "Target request HTTP status must be 2xx before P0 completion.",
                targetRequest=label,
                targetRequestHttpStatus=target_request_http_status or "MISSING",
            )
        target_request_business_code = as_int(markdown_value(text, f"Target Request {label} Business Code"))
        target_request_business_codes[label] = (
            target_request_business_code if target_request_business_code is not None else "MISSING"
        )
        if actual == "true" and target_request_business_code != 0:
            add_blocker(
                blockers,
                "P0_COMPLETION_TARGET_REQUEST_BUSINESS_CODE_NOT_OK",
                "Target request business code must be 0 before P0 completion.",
                targetRequest=label,
                targetRequestBusinessCode=(
                    target_request_business_code if target_request_business_code is not None else "MISSING"
                ),
            )
        response_identity = REQUIRED_TARGET_RESPONSE_IDENTITIES[label]
        response_identity_field = response_identity["field"]
        response_identity_value = as_positive_int(
            markdown_value(text, f"Target Response {label} {response_identity_field}")
        )
        target_response_identities[label] = response_identity_value or "MISSING"
        if actual == "true" and response_identity_value is None:
            add_blocker(
                blockers,
                "P0_COMPLETION_TARGET_RESPONSE_ID_MISSING",
                "Target response identity must be captured from the actual CommonResult data.",
                targetRequest=label,
                targetResponseField=response_identity_field,
            )
        if (
            actual == "true"
            and response_identity["mustMatchProcessPoolEventId"]
            and response_identity_value is not None
            and process_pool_event_id is not None
            and response_identity_value != process_pool_event_id
        ):
            add_blocker(
                blockers,
                "P0_COMPLETION_TARGET_RESPONSE_PROCESS_POOL_EVENT_MISMATCH",
                "Target response processPoolEventId must match the fresh root processPoolEventId from this run.",
                targetRequest=label,
                targetResponseField=response_identity_field,
                targetResponseValue=response_identity_value,
                processPoolEventId=process_pool_event_id,
            )

    browser_diagnostics: dict[str, int | None] = {}
    for label in ["Browser Page Errors", "Browser Console Errors", "Target Request Failures"]:
        actual = markdown_value(text, label)
        parsed = as_positive_int(actual)
        if actual == "0":
            parsed = 0
        browser_diagnostics[label] = parsed
        if actual is None:
            add_blocker(
                blockers,
                "P0_COMPLETION_BROWSER_DIAGNOSTICS_MISSING",
                "Real E2E evidence must report browser page errors, console errors, and target request failures.",
                diagnostic=label,
            )
        elif parsed != 0:
            add_blocker(
                blockers,
                "P0_COMPLETION_BROWSER_DIAGNOSTICS_NOT_CLEAN",
                "Browser diagnostics must be clean before P0 completion.",
                diagnostic=label,
                actual=actual,
            )

    closure_section = markdown_section(text, "Closure Evidence Packet")
    if "CLOSURE_EVIDENCE_MISSING_SOURCE" in closure_section or re.search(r"- Status:\s+`BLOCKED`", closure_section):
        add_blocker(
            blockers,
            "P0_COMPLETION_CLOSURE_EVIDENCE_BLOCKED",
            "Closure evidence packet is still blocked or missing formal sources.",
        )
    closure_process_pool_event_id = as_positive_int(markdown_value(closure_section, "processPoolEventId"))
    closure_complete = markdown_value(closure_section, "complete")
    closure_same_source_checks = as_positive_int(markdown_value(closure_section, "sameSourceChecks"))
    closure_blockers = as_int(markdown_value(closure_section, "blockers"))
    if process_pool_event_id is not None and closure_process_pool_event_id != process_pool_event_id:
        add_blocker(
            blockers,
            "P0_COMPLETION_CLOSURE_PROCESS_POOL_EVENT_MISMATCH",
            "Closure evidence packet processPoolEventId must match the fresh root processPoolEventId.",
            closureProcessPoolEventId=closure_process_pool_event_id or "MISSING",
            processPoolEventId=process_pool_event_id,
        )
    if closure_complete != "true":
        add_blocker(
            blockers,
            "P0_COMPLETION_CLOSURE_NOT_COMPLETE",
            "Closure evidence packet must explicitly report complete=true before P0 completion.",
            closureComplete=closure_complete or "MISSING",
        )
    if closure_same_source_checks is None or closure_same_source_checks < len(REQUIRED_CLOSURE_ANSWERS):
        add_blocker(
            blockers,
            "P0_COMPLETION_CLOSURE_SAME_SOURCE_CHECKS_INCOMPLETE",
            "Closure evidence packet must include at least one same-source check per required audit answer.",
            closureSameSourceChecks=closure_same_source_checks or "MISSING",
            requiredSameSourceChecks=len(REQUIRED_CLOSURE_ANSWERS),
        )
    if closure_blockers != 0:
        add_blocker(
            blockers,
            "P0_COMPLETION_CLOSURE_BLOCKERS_PRESENT",
            "Closure evidence packet blockers must be zero before P0 completion.",
            closureBlockers=closure_blockers if closure_blockers is not None else "MISSING",
        )
    closure_issue_lines = re.findall(r"^- Closure Issue:\s+(.+)$", closure_section, re.MULTILINE)
    if closure_issue_lines:
        add_blocker(
            blockers,
            "P0_COMPLETION_CLOSURE_ISSUE_PRESENT",
            "Closure evidence packet must not contain unresolved Closure Issue lines.",
            closureIssues=closure_issue_lines,
        )
    closure_answers: dict[str, dict[str, Any]] = {}
    for answer_key in REQUIRED_CLOSURE_ANSWERS:
        match = re.search(
            rf"answers\.{re.escape(answer_key)}:\s+sourceIds=`(\d+)`,\s+sameSource=`true`,\s+readOnlyVerificationEntries=`(\d+)`",
            closure_section,
        )
        if match is None:
            closure_answers[answer_key] = {
                "sourceIds": None,
                "sameSource": None,
                "readOnlyVerificationEntries": None,
            }
            add_blocker(
                blockers,
                "P0_COMPLETION_CLOSURE_ANSWER_INCOMPLETE",
                "Every closure evidence answer must have sourceIds, sameSource=true, and read-only verification entries.",
                answer=answer_key,
            )
            continue
        source_count = int(match.group(1))
        verification_count = int(match.group(2))
        closure_answers[answer_key] = {
            "sourceIds": source_count,
            "sameSource": True,
            "readOnlyVerificationEntries": verification_count,
        }
        if source_count <= 0 or verification_count <= 0:
            add_blocker(
                blockers,
                "P0_COMPLETION_CLOSURE_ANSWER_INCOMPLETE",
                "Every closure evidence answer must have sourceIds, sameSource=true, and read-only verification entries.",
                answer=answer_key,
            )

    runtime_section = markdown_section(text, "Runtime Migration")
    runtime_evidence_status = markdown_value(runtime_section, "Status")
    runtime_evidence_counts: dict[str, int | None] = {}
    for markdown_label in REQUIRED_RUNTIME_MIGRATION_COUNT_FIELDS:
        parsed_count = as_int(markdown_value(runtime_section, markdown_label))
        runtime_evidence_counts[markdown_label] = parsed_count if parsed_count is not None and parsed_count >= 0 else None
    if runtime_evidence_status != "PASS":
        add_blocker(
            blockers,
            "P0_COMPLETION_RUNTIME_EVIDENCE_NOT_PASS",
            "Runtime Migration section in real E2E evidence must be PASS.",
            actual=runtime_evidence_status or "MISSING",
        )
    elif any(count is None for count in runtime_evidence_counts.values()):
        add_blocker(
            blockers,
            "P0_COMPLETION_RUNTIME_EVIDENCE_COUNTS_MISSING",
            "Runtime Migration section must include numeric Required Columns, Required Indexes, and Historical Checks counts.",
            runtimeMigrationCounts=runtime_evidence_counts,
        )

    real_e2e_result: dict[str, Any] = {"path": "MISSING", "status": "MISSING"}
    result_path = resolve_real_e2e_result_path(task_dir)
    if result_path is None:
        add_blocker(
            blockers,
            "P0_COMPLETION_REAL_E2E_RESULT_MISSING",
            "Real E2E result.json is required and must match the Markdown evidence before P0 completion.",
            expectedPath=str(REAL_E2E_RESULT_RELATIVE_PATH),
        )
    else:
        real_e2e_result["path"] = str(result_path)
        try:
            result_data = json.loads(result_path.read_text(encoding="utf-8"))
        except json.JSONDecodeError as exc:
            result_data = None
            add_blocker(
                blockers,
                "P0_COMPLETION_REAL_E2E_RESULT_INVALID_JSON",
                "Real E2E result.json must be valid JSON.",
                path=str(result_path),
                error=str(exc),
            )
        if isinstance(result_data, dict):
            result_status = str(result_data.get("status") or "MISSING")
            real_e2e_result["status"] = result_status
            if result_status != (top_status or "MISSING"):
                add_blocker(
                    blockers,
                    "P0_COMPLETION_REAL_E2E_RESULT_STATUS_MISMATCH",
                    "Real E2E result.json status must match p0-real-e2e-evidence.md status.",
                    markdownStatus=top_status or "MISSING",
                    resultStatus=result_status,
                )
            result_generated_at = str(result_data.get("generatedAt") or "MISSING")
            real_e2e_result["generatedAt"] = result_generated_at
            if result_generated_at != (parsed_generated_at or "MISSING"):
                add_blocker(
                    blockers,
                    "P0_COMPLETION_REAL_E2E_RESULT_GENERATED_AT_MISMATCH",
                    "Real E2E result.json generatedAt must match p0-real-e2e-evidence.md Generated At.",
                    markdownGeneratedAt=parsed_generated_at or "MISSING",
                    resultGeneratedAt=result_generated_at,
                )
            result_frontend_url = str(result_data.get("frontendUrl") or "MISSING")
            real_e2e_result["frontendUrl"] = result_frontend_url
            result_backend_url = str(result_data.get("backendUrl") or "MISSING")
            real_e2e_result["backendUrl"] = result_backend_url
            if top_status == "PASS" and result_frontend_url != (frontend_url or "MISSING"):
                add_blocker(
                    blockers,
                    "P0_COMPLETION_REAL_E2E_RESULT_FRONTEND_URL_MISMATCH",
                    "Real E2E result.json frontendUrl must match p0-real-e2e-evidence.md Frontend.",
                    markdownFrontendUrl=frontend_url or "MISSING",
                    resultFrontendUrl=result_frontend_url,
                )
            if top_status == "PASS" and result_backend_url != (backend_url or "MISSING"):
                add_blocker(
                    blockers,
                    "P0_COMPLETION_REAL_E2E_RESULT_BACKEND_URL_MISMATCH",
                    "Real E2E result.json backendUrl must match p0-real-e2e-evidence.md Backend.",
                    markdownBackendUrl=backend_url or "MISSING",
                    resultBackendUrl=result_backend_url,
                )
            if top_status == "PASS":
                result_metadata_summary: dict[str, Any] = {}
                result_metadata_string_fields = [
                    (
                        "Tenant",
                        "tenant",
                        completion_metadata.get("Tenant") or "MISSING",
                        "P0_COMPLETION_REAL_E2E_RESULT_TENANT_MISMATCH",
                    ),
                    (
                        "User",
                        "username",
                        completion_metadata.get("User") or "MISSING",
                        "P0_COMPLETION_REAL_E2E_RESULT_USER_MISMATCH",
                    ),
                    (
                        "Run ID",
                        "runId",
                        run_id or "MISSING",
                        "P0_COMPLETION_REAL_E2E_RESULT_RUN_ID_MISMATCH",
                    ),
                    (
                        "Data Prefix",
                        "dataPrefix",
                        data_prefix or "MISSING",
                        "P0_COMPLETION_REAL_E2E_RESULT_DATA_PREFIX_MISMATCH",
                    ),
                    (
                        "Schema Migration ID",
                        "schemaMigrationId",
                        schema_migration_id or "MISSING",
                        "P0_COMPLETION_REAL_E2E_RESULT_SCHEMA_MIGRATION_ID_MISMATCH",
                    ),
                    (
                        "Migration Policy Evidence",
                        "migrationPolicyEvidence",
                        migration_policy_evidence or "MISSING",
                        "P0_COMPLETION_REAL_E2E_RESULT_MIGRATION_POLICY_EVIDENCE_MISMATCH",
                    ),
                ]
                for markdown_label, result_key, markdown_expected, blocker_code in result_metadata_string_fields:
                    result_actual = str(result_data.get(result_key) or "MISSING")
                    result_metadata_summary[markdown_label] = result_actual
                    if result_actual != markdown_expected:
                        add_blocker(
                            blockers,
                            blocker_code,
                            "Real E2E result.json run metadata must match the Markdown evidence.",
                            metadataField=markdown_label,
                            markdownValue=markdown_expected,
                            resultValue=result_actual,
                        )
                result_device_account_id = as_positive_int_value(result_data.get("deviceAccountId"))
                result_metadata_summary["Device Account ID"] = result_device_account_id or "MISSING"
                if result_device_account_id != device_account_id:
                    add_blocker(
                        blockers,
                        "P0_COMPLETION_REAL_E2E_RESULT_DEVICE_ACCOUNT_ID_MISMATCH",
                        "Real E2E result.json deviceAccountId must match the Markdown evidence.",
                        markdownDeviceAccountId=device_account_id or "MISSING",
                        resultDeviceAccountId=result_device_account_id or "MISSING",
                    )
                result_binding = {
                    "report": as_positive_int_value(result_data.get("batchRecordReportId")),
                    "definition": as_positive_int_value(result_data.get("batchRecordDefinitionId")),
                    "version": as_positive_int_value(result_data.get("batchRecordVersionId")),
                }
                result_metadata_summary["Batch Record Binding"] = result_binding
                if result_binding != binding:
                    add_blocker(
                        blockers,
                        "P0_COMPLETION_REAL_E2E_RESULT_BATCH_RECORD_BINDING_MISMATCH",
                        "Real E2E result.json batch-record binding IDs must match the Markdown evidence.",
                        markdownBinding=binding,
                        resultBinding=result_binding,
                    )
                real_e2e_result["runMetadata"] = result_metadata_summary
                result_idempotency_summary: dict[str, bool] = {}
                for label, result_key in REQUIRED_IDEMPOTENCY_RESULT_FIELDS.items():
                    result_configured = has_result_evidence_value(result_data.get(result_key))
                    result_idempotency_summary[label] = result_configured
                    if result_configured != idempotency_evidence.get(label):
                        add_blocker(
                            blockers,
                            "P0_COMPLETION_REAL_E2E_RESULT_IDEMPOTENCY_EVIDENCE_MISMATCH",
                            "Real E2E result.json idempotency evidence must match the Markdown evidence.",
                            idempotencyEvidence=label,
                            markdownConfigured=idempotency_evidence.get(label, "MISSING"),
                            resultConfigured=result_configured,
                        )
                real_e2e_result["idempotencyEvidence"] = result_idempotency_summary
                result_duplicate_summary: dict[str, bool] = {}
                for label, result_key in REQUIRED_DUPLICATE_RESULT_FIELDS.items():
                    result_verified = result_data.get(result_key) is True
                    result_duplicate_summary[label] = result_verified
                    if result_verified != duplicate_evidence.get(label):
                        add_blocker(
                            blockers,
                            "P0_COMPLETION_REAL_E2E_RESULT_DUPLICATE_EVIDENCE_MISMATCH",
                            "Real E2E result.json duplicate-action evidence must match the Markdown evidence.",
                            duplicateEvidence=label,
                            markdownVerified=duplicate_evidence.get(label, "MISSING"),
                            resultVerified=result_verified,
                        )
                real_e2e_result["duplicateEvidence"] = result_duplicate_summary
                result_target_request_evidence_flushed = result_data.get("targetRequestEvidenceFlushed") is True
                real_e2e_result["targetRequestEvidenceFlushed"] = result_target_request_evidence_flushed
                if not result_target_request_evidence_flushed:
                    add_blocker(
                        blockers,
                        "P0_COMPLETION_REAL_E2E_RESULT_TARGET_REQUEST_EVIDENCE_NOT_FLUSHED",
                        "Real E2E result.json must prove target request business-code parsing was flushed before evidence write.",
                    )
                result_target_requests_raw = result_data.get("targetRequests")
                result_target_requests = (
                    result_target_requests_raw if isinstance(result_target_requests_raw, list) else []
                )
                real_e2e_result["targetRequestCount"] = len(result_target_requests)
                result_target_request_summary: dict[str, dict[str, Any] | str] = {}
                if not isinstance(result_target_requests_raw, list):
                    add_blocker(
                        blockers,
                        "P0_COMPLETION_REAL_E2E_RESULT_TARGET_REQUESTS_MISSING",
                        "Real E2E result.json targetRequests must be present before P0 completion.",
                    )
                elif len(result_target_requests) != len(REQUIRED_TARGET_REQUEST_LABELS):
                    add_blocker(
                        blockers,
                        "P0_COMPLETION_REAL_E2E_RESULT_TARGET_REQUEST_COUNT_MISMATCH",
                        "Real E2E result.json targetRequests must contain exactly the five canonical P0 target request boundaries.",
                        expectedCount=len(REQUIRED_TARGET_REQUEST_LABELS),
                        observedCount=len(result_target_requests),
                    )
                target_request_missing_label_indexes: set[int] = set()
                for index, item in enumerate(result_target_requests):
                    if not isinstance(item, dict):
                        add_blocker(
                            blockers,
                            "P0_COMPLETION_REAL_E2E_RESULT_TARGET_REQUEST_OBJECT_MISSING",
                            "Real E2E result.json targetRequests entries must be JSON objects before P0 completion.",
                            targetRequestIndex=index,
                            observedType=type(item).__name__,
                            observedValue=str(item),
                        )
                        continue
                    if isinstance(item, dict) and not has_result_evidence_value(item.get("label")):
                        target_request_missing_label_indexes.add(index)
                        add_blocker(
                            blockers,
                            "P0_COMPLETION_REAL_E2E_RESULT_TARGET_REQUEST_LABEL_MISSING",
                            "Real E2E result.json target request label must be present before P0 completion.",
                            targetRequestIndex=index,
                            observedLabel=str(item.get("label") or "MISSING"),
                            observedUrl=str(item.get("url") or "MISSING"),
                        )
                        continue
                    if is_expected_result_target_request(item):
                        continue
                    add_blocker(
                        blockers,
                        "P0_COMPLETION_REAL_E2E_RESULT_TARGET_REQUEST_UNEXPECTED",
                        "Real E2E result.json targetRequests must contain only the five canonical P0 target request boundaries.",
                        targetRequestIndex=index,
                        observedLabel=(
                            str(item.get("label") or "MISSING")
                            if isinstance(item, dict)
                            else "MISSING"
                        ),
                        observedUrl=(
                            str(item.get("url") or "MISSING")
                            if isinstance(item, dict)
                            else "MISSING"
                        ),
                    )
                for label in REQUIRED_TARGET_REQUEST_LABELS:
                    endpoint = REQUIRED_TARGET_REQUEST_ENDPOINTS[label]
                    result_target_request_matches = find_result_target_requests(
                        result_target_requests,
                        label,
                        endpoint,
                    )
                    endpoint_matches = [
                        item
                        for item in result_target_requests
                        if isinstance(item, dict)
                        and normalize_url_path(str(item.get("url") or "")) == endpoint
                    ]
                    label_matches = [
                        item
                        for item in result_target_requests
                        if isinstance(item, dict) and str(item.get("label") or "") == label
                    ]
                    if len(result_target_request_matches) > 1 or len(endpoint_matches) > 1 or len(label_matches) > 1:
                        add_blocker(
                            blockers,
                            "P0_COMPLETION_REAL_E2E_RESULT_TARGET_REQUEST_DUPLICATE",
                            "Real E2E result.json must contain one canonical target request per required label and endpoint.",
                            targetRequest=label,
                            expectedEndpoint=endpoint,
                            labelMatchCount=len(label_matches),
                            endpointMatchCount=len(endpoint_matches),
                            boundaryMatchCount=len(result_target_request_matches),
                            observedLabels=[
                                str(item.get("label") or "MISSING")
                                for item in endpoint_matches
                            ],
                        )
                    result_target_request = (
                        result_target_request_matches[0]
                        if result_target_request_matches
                        else None
                    )
                    if result_target_request is None:
                        result_target_request_summary[label] = "MISSING"
                        label_match_urls = [
                            item.get("url")
                            for item in result_target_requests
                            if isinstance(item, dict)
                            and str(item.get("label") or "") == label
                        ]
                        if any(not has_result_evidence_value(url) for url in label_match_urls):
                            add_blocker(
                                blockers,
                                "P0_COMPLETION_REAL_E2E_RESULT_TARGET_REQUEST_URL_MISSING",
                                "Real E2E result.json target request URL must be present before P0 completion.",
                                targetRequest=label,
                                observedUrls=[
                                    str(url) if url is not None else "MISSING"
                                    for url in label_match_urls
                                ],
                            )
                            continue
                        endpoint_label_matches = [
                            (index, item.get("label"))
                            for index, item in enumerate(result_target_requests)
                            if isinstance(item, dict)
                            and normalize_url_path(str(item.get("url") or "")) == endpoint
                        ]
                        if any(
                            not has_result_evidence_value(observed_label)
                            and index not in target_request_missing_label_indexes
                            for index, observed_label in endpoint_label_matches
                        ):
                            add_blocker(
                                blockers,
                                "P0_COMPLETION_REAL_E2E_RESULT_TARGET_REQUEST_LABEL_MISSING",
                                "Real E2E result.json target request label must be present before P0 completion.",
                                targetRequest=label,
                                expectedEndpoint=endpoint,
                                observedLabels=[
                                    str(observed_label) if observed_label is not None else "MISSING"
                                    for _, observed_label in endpoint_label_matches
                                ],
                            )
                            continue
                        endpoint_match_labels = [
                            str(observed_label or "MISSING")
                            for _, observed_label in endpoint_label_matches
                        ]
                        if endpoint_match_labels:
                            add_blocker(
                                blockers,
                                "P0_COMPLETION_REAL_E2E_RESULT_TARGET_REQUEST_LABEL_MISMATCH",
                                "Real E2E result.json target request label must match the expected endpoint boundary.",
                                targetRequest=label,
                                expectedEndpoint=endpoint,
                                observedLabels=endpoint_match_labels,
                            )
                            continue
                        add_blocker(
                            blockers,
                            "P0_COMPLETION_REAL_E2E_RESULT_TARGET_REQUEST_MISSING",
                            "Real E2E result.json must contain each target request observed by the browser.",
                            targetRequest=label,
                            expectedEndpoint=endpoint,
                        )
                        continue
                    result_request_url = str(result_target_request.get("url") or "MISSING")
                    result_request_method_raw = result_target_request.get("method")
                    result_request_method = str(result_request_method_raw or "MISSING").upper()
                    result_request_http_status = as_positive_int_value(result_target_request.get("httpStatus"))
                    result_request_business_code = as_int_value(result_target_request.get("businessCode"))
                    result_target_request_summary[label] = {
                        "url": result_request_url,
                        "method": result_request_method,
                        "httpStatus": result_request_http_status or "MISSING",
                        "businessCode": (
                            result_request_business_code
                            if result_request_business_code is not None
                            else "MISSING"
                        ),
                    }
                    if result_request_url != target_request_urls.get(label):
                        add_blocker(
                            blockers,
                            "P0_COMPLETION_REAL_E2E_RESULT_TARGET_REQUEST_URL_MISMATCH",
                            "Real E2E result.json target request URL must match the Markdown evidence.",
                            targetRequest=label,
                            markdownUrl=target_request_urls.get(label, "MISSING"),
                            resultUrl=result_request_url,
                        )
                    if not has_result_evidence_value(result_request_method_raw):
                        add_blocker(
                            blockers,
                            "P0_COMPLETION_REAL_E2E_RESULT_TARGET_REQUEST_METHOD_MISSING",
                            "Real E2E result.json target request method must be present before P0 completion.",
                            targetRequest=label,
                            resultMethod=str(result_target_request.get("method", "MISSING")),
                        )
                    if result_request_method != str(target_request_methods.get(label) or "MISSING").upper():
                        add_blocker(
                            blockers,
                            "P0_COMPLETION_REAL_E2E_RESULT_TARGET_REQUEST_METHOD_MISMATCH",
                            "Real E2E result.json target request method must match the Markdown evidence.",
                            targetRequest=label,
                            markdownMethod=target_request_methods.get(label, "MISSING"),
                            resultMethod=result_request_method,
                        )
                    if result_request_http_status is None:
                        add_blocker(
                            blockers,
                            "P0_COMPLETION_REAL_E2E_RESULT_TARGET_REQUEST_HTTP_STATUS_MISSING",
                            "Real E2E result.json target request HTTP status must be present and numeric before P0 completion.",
                            targetRequest=label,
                            resultHttpStatus=str(result_target_request.get("httpStatus", "MISSING")),
                        )
                    if result_request_http_status != target_request_http_statuses.get(label):
                        add_blocker(
                            blockers,
                            "P0_COMPLETION_REAL_E2E_RESULT_TARGET_REQUEST_HTTP_STATUS_MISMATCH",
                            "Real E2E result.json target request HTTP status must match the Markdown evidence.",
                            targetRequest=label,
                            markdownHttpStatus=target_request_http_statuses.get(label, "MISSING"),
                            resultHttpStatus=result_request_http_status or "MISSING",
                        )
                    if result_request_business_code is None:
                        add_blocker(
                            blockers,
                            "P0_COMPLETION_REAL_E2E_RESULT_TARGET_REQUEST_BUSINESS_CODE_MISSING",
                            "Real E2E result.json target request business code must be present and numeric before P0 completion.",
                            targetRequest=label,
                            resultBusinessCode=str(result_target_request.get("businessCode", "MISSING")),
                        )
                    if result_request_business_code != target_request_business_codes.get(label):
                        add_blocker(
                            blockers,
                            "P0_COMPLETION_REAL_E2E_RESULT_TARGET_REQUEST_BUSINESS_CODE_MISMATCH",
                            "Real E2E result.json target request business code must match the Markdown evidence.",
                            targetRequest=label,
                            markdownBusinessCode=target_request_business_codes.get(label, "MISSING"),
                            resultBusinessCode=(
                                result_request_business_code
                                if result_request_business_code is not None
                                else "MISSING"
                            ),
                        )
                real_e2e_result["targetRequests"] = result_target_request_summary
                result_target_response_raw = result_data.get("targetResponseIdentities")
                result_target_response_summary: dict[str, dict[str, Any] | str] = {}
                if not isinstance(result_target_response_raw, dict):
                    add_blocker(
                        blockers,
                        "P0_COMPLETION_REAL_E2E_RESULT_TARGET_RESPONSE_IDENTITIES_MISSING",
                        "Real E2E result.json targetResponseIdentities must be present before P0 completion.",
                    )
                    result_target_response_raw = {}
                else:
                    if len(result_target_response_raw) != len(REQUIRED_TARGET_REQUEST_LABELS):
                        add_blocker(
                            blockers,
                            "P0_COMPLETION_REAL_E2E_RESULT_TARGET_RESPONSE_COUNT_MISMATCH",
                            "Real E2E result.json targetResponseIdentities must contain exactly five canonical P0 target response identities.",
                            observedCount=len(result_target_response_raw),
                            expectedCount=len(REQUIRED_TARGET_REQUEST_LABELS),
                            expectedLabels=REQUIRED_TARGET_REQUEST_LABELS,
                        )
                    unexpected_response_identity_keys = sorted(
                        key
                        for key in result_target_response_raw.keys()
                        if str(key) not in REQUIRED_TARGET_REQUEST_LABELS
                    )
                    if unexpected_response_identity_keys:
                        add_blocker(
                            blockers,
                            "P0_COMPLETION_REAL_E2E_RESULT_TARGET_RESPONSE_UNEXPECTED",
                            "Real E2E result.json targetResponseIdentities must contain only the five canonical P0 target response identities.",
                            observedLabels=unexpected_response_identity_keys,
                            expectedLabels=REQUIRED_TARGET_REQUEST_LABELS,
                        )
                if isinstance(result_target_requests_raw, list) and isinstance(result_target_response_raw, dict):
                    observed_target_request_labels = sorted(
                        {
                            str(item.get("label") or "MISSING")
                            for item in result_target_requests
                            if isinstance(item, dict)
                        }
                    )
                    observed_target_response_labels = sorted(
                        {str(label) for label in result_target_response_raw.keys()}
                    )
                    real_e2e_result["targetResponseRequestLabelSet"] = {
                        "targetRequests": observed_target_request_labels,
                        "targetResponseIdentities": observed_target_response_labels,
                    }
                    if set(observed_target_request_labels) != set(observed_target_response_labels):
                        add_blocker(
                            blockers,
                            "P0_COMPLETION_REAL_E2E_RESULT_TARGET_RESPONSE_REQUEST_SET_MISMATCH",
                            "Real E2E result.json targetResponseIdentities keys must match the observed targetRequests labels from the same result artifact.",
                            observedTargetRequestLabels=observed_target_request_labels,
                            observedTargetResponseIdentityLabels=observed_target_response_labels,
                            missingObservedTargetRequests=sorted(
                                set(observed_target_response_labels) - set(observed_target_request_labels)
                            ),
                            responseIdentitiesWithoutObservedRequest=sorted(
                                set(observed_target_request_labels) - set(observed_target_response_labels)
                            ),
                        )
                for label in REQUIRED_TARGET_REQUEST_LABELS:
                    expected_identity = REQUIRED_TARGET_RESPONSE_IDENTITIES[label]
                    expected_field = expected_identity["field"]
                    result_identity = result_target_response_raw.get(label)
                    if not isinstance(result_identity, dict):
                        result_target_response_summary[label] = "MISSING"
                        if label in result_target_response_raw:
                            add_blocker(
                                blockers,
                                "P0_COMPLETION_REAL_E2E_RESULT_TARGET_RESPONSE_OBJECT_MISSING",
                                "Real E2E result.json targetResponseIdentities entries must be JSON objects before P0 completion.",
                                targetRequest=label,
                                observedType=type(result_identity).__name__,
                                observedValue=str(result_identity),
                            )
                            continue
                        add_blocker(
                            blockers,
                            "P0_COMPLETION_REAL_E2E_RESULT_TARGET_RESPONSE_IDENTITY_MISSING",
                            "Real E2E result.json must contain each target response identity.",
                            targetRequest=label,
                            targetResponseField=expected_field,
                        )
                        continue
                    result_identity_field_raw = result_identity.get("field")
                    result_identity_value_raw = result_identity.get("value")
                    result_identity_source_label_raw = result_identity.get("sourceRequestLabel")
                    result_identity_field = str(result_identity_field_raw or "MISSING")
                    result_identity_value = as_positive_int_value(result_identity_value_raw)
                    result_identity_source_label = str(result_identity_source_label_raw or "MISSING")
                    result_target_response_summary[label] = {
                        "field": result_identity_field,
                        "value": result_identity_value or "MISSING",
                        "sourceRequestLabel": result_identity_source_label,
                    }
                    if not has_result_evidence_value(result_identity_source_label_raw):
                        add_blocker(
                            blockers,
                            "P0_COMPLETION_REAL_E2E_RESULT_TARGET_RESPONSE_SOURCE_REQUEST_LABEL_MISSING",
                            "Real E2E result.json target response identity source request label must be present before P0 completion.",
                            targetRequest=label,
                            expectedSourceRequestLabel=label,
                            resultSourceRequestLabel=str(result_identity.get("sourceRequestLabel", "MISSING")),
                        )
                    if result_identity_source_label != label:
                        add_blocker(
                            blockers,
                            "P0_COMPLETION_REAL_E2E_RESULT_TARGET_RESPONSE_REQUEST_LABEL_MISMATCH",
                            "Real E2E result.json target response identity must declare the matching canonical target request label as its source.",
                            targetRequest=label,
                            resultSourceRequestLabel=result_identity_source_label,
                        )
                    if not has_result_evidence_value(result_identity_field_raw):
                        add_blocker(
                            blockers,
                            "P0_COMPLETION_REAL_E2E_RESULT_TARGET_RESPONSE_FIELD_MISSING",
                            "Real E2E result.json target response field must be present before P0 completion.",
                            targetRequest=label,
                            expectedField=expected_field,
                            resultField=str(result_identity.get("field", "MISSING")),
                        )
                    if result_identity_field != expected_field:
                        add_blocker(
                            blockers,
                            "P0_COMPLETION_REAL_E2E_RESULT_TARGET_RESPONSE_FIELD_MISMATCH",
                            "Real E2E result.json target response field must match the Markdown evidence.",
                            targetRequest=label,
                            markdownField=expected_field,
                            resultField=result_identity_field,
                        )
                    if result_identity_value is None:
                        add_blocker(
                            blockers,
                            "P0_COMPLETION_REAL_E2E_RESULT_TARGET_RESPONSE_VALUE_MISSING",
                            "Real E2E result.json target response value must be present as a positive integer before P0 completion.",
                            targetRequest=label,
                            targetResponseField=expected_field,
                            resultValue=str(result_identity.get("value", "MISSING")),
                        )
                    if result_identity_value != target_response_identities.get(label):
                        add_blocker(
                            blockers,
                            "P0_COMPLETION_REAL_E2E_RESULT_TARGET_RESPONSE_VALUE_MISMATCH",
                            "Real E2E result.json target response value must match the Markdown evidence.",
                            targetRequest=label,
                            targetResponseField=expected_field,
                            markdownValue=target_response_identities.get(label, "MISSING"),
                            resultValue=result_identity_value or "MISSING",
                        )
                real_e2e_result["targetResponseIdentities"] = result_target_response_summary
                result_browser_preflight_raw = result_data.get("browserPreflight")
                result_browser_preflight_summary: dict[str, Any] = {}
                if not isinstance(result_browser_preflight_raw, dict):
                    add_blocker(
                        blockers,
                        "P0_COMPLETION_REAL_E2E_RESULT_BROWSER_PREFLIGHT_MISSING",
                        "Real E2E result.json browserPreflight must be present before P0 completion.",
                    )
                else:
                    result_browser_preflight_url = str(
                        result_browser_preflight_raw.get("currentUrl") or "MISSING"
                    )
                    result_browser_preflight_route_steps_raw = result_browser_preflight_raw.get("routeSteps")
                    result_browser_preflight_route_steps = (
                        len(result_browser_preflight_route_steps_raw)
                        if isinstance(result_browser_preflight_route_steps_raw, list)
                        else None
                    )
                    result_browser_preflight_routes = extract_browser_route_paths(
                        result_browser_preflight_route_steps_raw
                    )
                    result_browser_preflight_summary = {
                        "currentUrl": result_browser_preflight_url,
                        "routeSteps": (
                            result_browser_preflight_route_steps
                            if result_browser_preflight_route_steps is not None
                            else "MISSING"
                        ),
                        "routes": result_browser_preflight_routes,
                    }
                    if result_browser_preflight_url != (browser_preflight or "MISSING"):
                        add_blocker(
                            blockers,
                            "P0_COMPLETION_REAL_E2E_RESULT_BROWSER_PREFLIGHT_URL_MISMATCH",
                            "Real E2E result.json browserPreflight.currentUrl must match the Markdown evidence.",
                            markdownBrowserPreflight=browser_preflight or "MISSING",
                            resultBrowserPreflight=result_browser_preflight_url,
                        )
                    if result_browser_preflight_route_steps != route_steps:
                        add_blocker(
                            blockers,
                            "P0_COMPLETION_REAL_E2E_RESULT_BROWSER_PREFLIGHT_ROUTE_STEPS_MISMATCH",
                            "Real E2E result.json browserPreflight.routeSteps count must match the Markdown evidence.",
                            markdownRoutePreflightSteps=route_steps or "MISSING",
                            resultRoutePreflightSteps=(
                                result_browser_preflight_route_steps
                                if result_browser_preflight_route_steps is not None
                                else "MISSING"
                            ),
                        )
                    missing_browser_routes = [
                        route
                        for route in REQUIRED_BROWSER_ROUTE_SKELETON
                        if route not in result_browser_preflight_routes
                    ]
                    if missing_browser_routes:
                        add_blocker(
                            blockers,
                            "P0_COMPLETION_REAL_E2E_RESULT_BROWSER_PREFLIGHT_ROUTE_MISSING",
                            "Real E2E result.json browserPreflight.routeSteps must include the P0 page route skeleton.",
                            missingRoutes=missing_browser_routes,
                            actualRoutes=result_browser_preflight_routes,
                        )
                real_e2e_result["browserPreflight"] = result_browser_preflight_summary or "MISSING"
                result_diagnostics_raw = result_data.get("browserDiagnostics")
                result_diagnostic_summary: dict[str, int | str] = {}
                result_diagnostic_keys = {
                    "Browser Page Errors": "pageErrors",
                    "Browser Console Errors": "consoleErrors",
                    "Target Request Failures": "targetRequestFailures",
                }
                for markdown_label, result_key in result_diagnostic_keys.items():
                    result_count = count_result_diagnostic_items(result_diagnostics_raw, result_key)
                    result_diagnostic_summary[markdown_label] = result_count if result_count is not None else "MISSING"
                    if result_count != browser_diagnostics.get(markdown_label):
                        add_blocker(
                            blockers,
                            "P0_COMPLETION_REAL_E2E_RESULT_BROWSER_DIAGNOSTICS_MISMATCH",
                            "Real E2E result.json browserDiagnostics counts must match the Markdown evidence.",
                            diagnostic=markdown_label,
                            markdownCount=browser_diagnostics.get(markdown_label, "MISSING"),
                            resultCount=result_count if result_count is not None else "MISSING",
                        )
                real_e2e_result["browserDiagnostics"] = result_diagnostic_summary
                result_runtime_migration_raw = result_data.get("runtimeMigration")
                result_runtime_migration_summary: dict[str, Any] = {}
                if not isinstance(result_runtime_migration_raw, dict):
                    add_blocker(
                        blockers,
                        "P0_COMPLETION_REAL_E2E_RESULT_RUNTIME_MIGRATION_MISSING",
                        "Real E2E result.json runtimeMigration must be present before P0 completion.",
                    )
                else:
                    result_runtime_status = str(result_runtime_migration_raw.get("status") or "MISSING")
                    result_runtime_migration_summary["status"] = result_runtime_status
                    if result_runtime_status != (runtime_evidence_status or "MISSING"):
                        add_blocker(
                            blockers,
                            "P0_COMPLETION_REAL_E2E_RESULT_RUNTIME_MIGRATION_STATUS_MISMATCH",
                            "Real E2E result.json runtimeMigration.status must match the Markdown Runtime Migration status.",
                            markdownStatus=runtime_evidence_status or "MISSING",
                            resultStatus=result_runtime_status,
                        )
                    result_runtime_blockers = result_runtime_migration_raw.get("blockers")
                    result_runtime_blocker_count = (
                        len(result_runtime_blockers) if isinstance(result_runtime_blockers, list) else None
                    )
                    result_runtime_migration_summary["blockerCount"] = (
                        result_runtime_blocker_count if result_runtime_blocker_count is not None else "MISSING"
                    )
                    if result_runtime_blocker_count != 0:
                        add_blocker(
                            blockers,
                            "P0_COMPLETION_REAL_E2E_RESULT_RUNTIME_MIGRATION_BLOCKERS_PRESENT",
                            "Real E2E result.json runtimeMigration.blockers must be an empty list before P0 completion.",
                            resultBlockerCount=(
                                result_runtime_blocker_count
                                if result_runtime_blocker_count is not None
                                else "MISSING"
                            ),
                        )
                    for markdown_label, field_contract in REQUIRED_RUNTIME_MIGRATION_COUNT_FIELDS.items():
                        result_key = str(field_contract["resultKey"])
                        result_count = count_result_list_items(result_runtime_migration_raw, result_key)
                        result_runtime_migration_summary[markdown_label] = (
                            result_count if result_count is not None else "MISSING"
                        )
                        if result_count != runtime_evidence_counts.get(markdown_label):
                            add_blocker(
                                blockers,
                                str(field_contract["blockerCode"]),
                                "Real E2E result.json runtimeMigration counts must match the Markdown Runtime Migration evidence.",
                                runtimeMigrationEvidence=markdown_label,
                                markdownCount=runtime_evidence_counts.get(markdown_label, "MISSING"),
                                resultCount=result_count if result_count is not None else "MISSING",
                            )
                real_e2e_result["runtimeMigration"] = result_runtime_migration_summary or "MISSING"
                result_process_pool_event_id = as_positive_int_value(result_data.get("processPoolEventId"))
                real_e2e_result["processPoolEventId"] = result_process_pool_event_id or "MISSING"
                if result_process_pool_event_id != process_pool_event_id:
                    add_blocker(
                        blockers,
                        "P0_COMPLETION_REAL_E2E_RESULT_PROCESS_POOL_EVENT_MISMATCH",
                        "Real E2E result.json processPoolEventId must match the Markdown evidence root event.",
                        markdownProcessPoolEventId=process_pool_event_id or "MISSING",
                        resultProcessPoolEventId=result_process_pool_event_id or "MISSING",
                    )
                closure_result = result_data.get("closureEvidence")
                closure_result = closure_result if isinstance(closure_result, dict) else {}
                result_closure_process_pool_event_id = as_positive_int_value(
                    closure_result.get("processPoolEventId")
                )
                real_e2e_result["closureProcessPoolEventId"] = (
                    result_closure_process_pool_event_id or "MISSING"
                )
                if result_closure_process_pool_event_id != closure_process_pool_event_id:
                    add_blocker(
                        blockers,
                        "P0_COMPLETION_REAL_E2E_RESULT_CLOSURE_EVENT_MISMATCH",
                        "Real E2E result.json closureEvidence.processPoolEventId must match the Markdown closure packet.",
                        markdownClosureProcessPoolEventId=closure_process_pool_event_id or "MISSING",
                        resultClosureProcessPoolEventId=result_closure_process_pool_event_id or "MISSING",
                    )
                result_closure_complete = closure_result.get("complete")
                real_e2e_result["closureComplete"] = result_closure_complete
                if result_closure_complete is not True:
                    add_blocker(
                        blockers,
                        "P0_COMPLETION_REAL_E2E_RESULT_CLOSURE_NOT_COMPLETE",
                        "Real E2E result.json closureEvidence.complete must be true before P0 completion.",
                        resultClosureComplete=result_closure_complete,
                    )
                result_closure_same_source_check_count = count_result_list_items(
                    closure_result,
                    "sameSourceChecks",
                )
                real_e2e_result["closureSameSourceChecks"] = (
                    result_closure_same_source_check_count
                    if result_closure_same_source_check_count is not None
                    else "MISSING"
                )
                if result_closure_same_source_check_count != closure_same_source_checks:
                    add_blocker(
                        blockers,
                        "P0_COMPLETION_REAL_E2E_RESULT_CLOSURE_SAME_SOURCE_CHECKS_MISMATCH",
                        "Real E2E result.json closureEvidence.sameSourceChecks count must match the Markdown closure packet.",
                        markdownSameSourceChecks=closure_same_source_checks or "MISSING",
                        resultSameSourceChecks=(
                            result_closure_same_source_check_count
                            if result_closure_same_source_check_count is not None
                            else "MISSING"
                        ),
                    )
                result_closure_same_source_checks_raw = closure_result.get("sameSourceChecks")
                result_closure_same_source_checks = (
                    result_closure_same_source_checks_raw
                    if isinstance(result_closure_same_source_checks_raw, list)
                    else []
                )
                failed_closure_same_source_checks = [
                    check
                    for check in result_closure_same_source_checks
                    if not isinstance(check, dict) or check.get("passed") is not True
                ]
                if failed_closure_same_source_checks:
                    add_blocker(
                        blockers,
                        "P0_COMPLETION_REAL_E2E_RESULT_CLOSURE_SAME_SOURCE_CHECK_FAILED",
                        "Real E2E result.json closureEvidence.sameSourceChecks must all pass before P0 completion.",
                        failedSameSourceChecks=failed_closure_same_source_checks,
                    )
                result_closure_blocker_count = count_result_list_items(closure_result, "blockers")
                real_e2e_result["closureBlockers"] = (
                    result_closure_blocker_count if result_closure_blocker_count is not None else "MISSING"
                )
                if result_closure_blocker_count != closure_blockers:
                    add_blocker(
                        blockers,
                        "P0_COMPLETION_REAL_E2E_RESULT_CLOSURE_BLOCKERS_MISMATCH",
                        "Real E2E result.json closureEvidence.blockers count must match the Markdown closure packet.",
                        markdownBlockers=closure_blockers if closure_blockers is not None else "MISSING",
                        resultBlockers=(
                            result_closure_blocker_count
                            if result_closure_blocker_count is not None
                            else "MISSING"
                        ),
                    )
                result_closure_answers_raw = closure_result.get("answers")
                result_closure_answers = (
                    result_closure_answers_raw if isinstance(result_closure_answers_raw, dict) else {}
                )
                result_closure_answer_summary: dict[str, dict[str, Any] | str] = {}
                for answer_key in REQUIRED_CLOSURE_ANSWERS:
                    expected_answer = closure_answers.get(answer_key, {})
                    result_answer_raw = result_closure_answers.get(answer_key)
                    if not isinstance(result_answer_raw, dict):
                        result_closure_answer_summary[answer_key] = "MISSING"
                        add_blocker(
                            blockers,
                            "P0_COMPLETION_REAL_E2E_RESULT_CLOSURE_ANSWER_MISMATCH",
                            "Real E2E result.json closureEvidence.answers must match the Markdown closure answer evidence.",
                            answer=answer_key,
                            markdownAnswer=expected_answer,
                            resultAnswer="MISSING",
                        )
                        continue
                    result_source_count = count_closure_source_ids(result_answer_raw.get("sourceIds"))
                    result_same_source = result_answer_raw.get("sameSource") is True
                    result_verification_count = count_result_list_items(
                        result_answer_raw,
                        "readOnlyVerificationEntries",
                    )
                    result_answer_blockers = result_answer_raw.get("blockers")
                    result_answer_blocker_count = (
                        len(result_answer_blockers) if isinstance(result_answer_blockers, list) else 0
                    )
                    result_closure_answer_summary[answer_key] = {
                        "sourceIds": result_source_count if result_source_count is not None else "MISSING",
                        "sameSource": result_same_source,
                        "readOnlyVerificationEntries": (
                            result_verification_count
                            if result_verification_count is not None
                            else "MISSING"
                        ),
                        "blockers": result_answer_blocker_count,
                    }
                    if (
                        result_source_count != expected_answer.get("sourceIds")
                        or result_same_source != expected_answer.get("sameSource")
                        or result_verification_count != expected_answer.get("readOnlyVerificationEntries")
                    ):
                        add_blocker(
                            blockers,
                            "P0_COMPLETION_REAL_E2E_RESULT_CLOSURE_ANSWER_MISMATCH",
                            "Real E2E result.json closureEvidence.answers must match the Markdown closure answer evidence.",
                            answer=answer_key,
                            markdownAnswer=expected_answer,
                            resultAnswer=result_closure_answer_summary[answer_key],
                        )
                    if result_answer_blocker_count > 0:
                        add_blocker(
                            blockers,
                            "P0_COMPLETION_REAL_E2E_RESULT_CLOSURE_ANSWER_BLOCKERS_PRESENT",
                            "Real E2E result.json closureEvidence.answers must not retain answer-level blockers before P0 completion.",
                            answer=answer_key,
                            resultAnswerBlockers=result_answer_blockers,
                        )
                real_e2e_result["closureAnswers"] = result_closure_answer_summary
                result_closure_issues = result_data.get("closureEvidenceIssues")
                result_closure_issues = result_closure_issues if isinstance(result_closure_issues, list) else []
                real_e2e_result["closureIssues"] = result_closure_issues
                if result_closure_issues:
                    add_blocker(
                        blockers,
                        "P0_COMPLETION_REAL_E2E_RESULT_CLOSURE_ISSUES_PRESENT",
                        "Real E2E result.json closureEvidenceIssues must be empty before P0 completion.",
                        resultClosureIssues=result_closure_issues,
                    )
        elif result_path is not None:
            add_blocker(
                blockers,
                "P0_COMPLETION_REAL_E2E_RESULT_INVALID_SHAPE",
                "Real E2E result.json must contain a JSON object.",
                path=str(result_path),
            )

    return {
        "status": top_status or "MISSING",
        "generatedAt": parsed_generated_at or "MISSING",
        "frontendUrl": frontend_url or "MISSING",
        "backendUrl": backend_url or "MISSING",
        "runId": run_id or "MISSING",
        "dataPrefix": data_prefix or "MISSING",
        "processPoolEventId": process_pool_event_id,
        "batchRecordBinding": binding,
        "schemaMigrationId": schema_migration_id or "MISSING",
        "migrationPolicyEvidenceStatus": migration_policy_evidence_status,
        "idempotencyEvidence": idempotency_evidence,
        "duplicateEvidence": duplicate_evidence,
        "browserPreflightUrl": browser_preflight or "MISSING",
        "targetRequestHits": target_request_hits,
        "targetRequestUrls": target_request_urls,
        "targetRequestMethods": target_request_methods,
        "targetRequestHttpStatuses": target_request_http_statuses,
        "targetRequestBusinessCodes": target_request_business_codes,
        "targetResponseIdentities": target_response_identities,
        "browserDiagnostics": browser_diagnostics,
        "closureEvidence": {
            "processPoolEventId": closure_process_pool_event_id,
            "complete": closure_complete,
            "sameSourceChecks": closure_same_source_checks,
            "blockers": closure_blockers,
            "issues": closure_issue_lines,
        },
        "runtimeEvidenceStatus": runtime_evidence_status or "MISSING",
        "resultJson": real_e2e_result,
    }, blockers


def run_runtime_verifier(runtime_verifier: Path) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    if not runtime_verifier.exists():
        return {"status": "MISSING"}, [
            {
                "code": "P0_COMPLETION_RUNTIME_VERIFIER_MISSING",
                "message": f"Runtime verifier is missing: {runtime_verifier}",
            }
        ]
    result = subprocess.run(
        [sys.executable, "-X", "utf8", str(runtime_verifier)],
        text=True,
        capture_output=True,
        encoding="utf-8",
        errors="replace",
    )
    try:
        payload = json.loads(result.stdout or "{}")
    except json.JSONDecodeError as exc:
        return {"status": "FAIL", "returnCode": result.returncode}, [
            {
                "code": "P0_COMPLETION_RUNTIME_VERIFIER_OUTPUT_INVALID",
                "message": f"Runtime verifier did not return JSON: {exc}",
            }
        ]
    if result.returncode != 0 or payload.get("status") != "PASS":
        return payload, [
            {
                "code": "P0_COMPLETION_RUNTIME_MIGRATION_NOT_PASS",
                "message": "Runtime migration verifier must return PASS before P0 completion.",
                "returnCode": result.returncode,
                "runtimeBlockers": payload.get("blockers", []),
            }
        ]
    return payload, []


def evaluate_completion(task_dir: Path, runtime_verifier: Path) -> dict[str, Any]:
    blockers: list[dict[str, Any]] = []

    task_status, task_blockers = evaluate_task_status(task_dir)
    blockers.extend(task_blockers)

    tdd_payload = evaluate_tdd_evidence(task_dir)
    if tdd_payload.get("status") != "PASS":
        blockers.extend(tdd_payload.get("blockers", []))

    real_e2e, real_e2e_blockers = evaluate_real_e2e(task_dir)
    blockers.extend(real_e2e_blockers)

    runtime_payload, runtime_blockers = run_runtime_verifier(runtime_verifier)
    blockers.extend(runtime_blockers)

    return {
        "status": "PASS" if not blockers else "BLOCKED",
        "taskDir": str(task_dir),
        "taskStatus": task_status,
        "tddEvidence": {
            "status": tdd_payload.get("status"),
            "m2OriginalRedFound": tdd_payload.get("m2OriginalRed", {}).get("found"),
            "m2SnapshotRedFound": tdd_payload.get("m2SnapshotRed", {}).get("found"),
        },
        "realE2e": real_e2e,
        "runtimeMigration": {
            "status": runtime_payload.get("status"),
            "blockerCount": len(runtime_payload.get("blockers", []) or []),
        },
        "blockers": blockers,
        "requiredClosureAnswers": REQUIRED_CLOSURE_ANSWERS,
        "requiredTargetRequestLabels": REQUIRED_TARGET_REQUEST_LABELS,
        "requiredTargetRequestMethods": REQUIRED_TARGET_REQUEST_METHODS,
        "requiredTargetResponseIdentities": REQUIRED_TARGET_RESPONSE_IDENTITIES,
    }


def emit(payload: dict[str, Any]) -> None:
    print(json.dumps(payload, ensure_ascii=False, indent=2, sort_keys=True))


def main() -> int:
    parser = argparse.ArgumentParser(description="Verify P0 production execution completion gate")
    parser.add_argument("--task-dir", default=str(DEFAULT_TASK_DIR))
    parser.add_argument("--runtime-verifier", default=str(DEFAULT_RUNTIME_VERIFIER))
    parser.add_argument("--print-contract", action="store_true")
    args = parser.parse_args()

    if args.print_contract:
        emit(
            {
                "status": "PASS",
                "requiredClosureAnswers": REQUIRED_CLOSURE_ANSWERS,
                "requiredTaskStatuses": ["ready_for_closeout", "completed"],
                "requiredRealE2eStatus": "PASS",
                "requiredRuntimeMigrationStatus": "PASS",
                "requiredTargetRequestLabels": REQUIRED_TARGET_REQUEST_LABELS,
                "requiredTargetRequestMethods": REQUIRED_TARGET_REQUEST_METHODS,
                "requiredTargetResponseIdentities": REQUIRED_TARGET_RESPONSE_IDENTITIES,
            }
        )
        return 0

    payload = evaluate_completion(Path(args.task_dir).resolve(), Path(args.runtime_verifier).resolve())
    emit(payload)
    return 0 if payload["status"] == "PASS" else 2


if __name__ == "__main__":
    sys.exit(main())
