#!/usr/bin/env python3
"""Read-only gate for P0 BDD/TDD evidence completeness."""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any


DEFAULT_TASK_DIR = (
    Path(__file__).resolve().parents[3]
    / "doc"
    / "tasks"
    / "20260803-p0-production-execution-loop-implementation"
)

REQUIRED_EVIDENCE_FILES = ["execution-log.md"]
OPTIONAL_EVIDENCE_FILES = [
    "backend-api-evidence.md",
    "verification-report.md",
    "task.md",
]
EVIDENCE_FILES = [*REQUIRED_EVIDENCE_FILES, *OPTIONAL_EVIDENCE_FILES]

ORIGINAL_M2_RED_REQUIRED_TOKENS = [
    "RED:",
    "MesP0TeamLeaderReviewSignatureServiceTest",
    "FAIL",
]

ORIGINAL_M2_RED_REASON_TOKENS = [
    "无签名仍可复核",
    "无签名仍可确认分配",
    "无签名仍可复核或确认分配",
    "班组长复核尚未要求电子签名",
]

SNAPSHOT_RED_TOKENS = [
    "reviewSignatureSnapshotJson",
    "签名快照",
    "空签名快照",
    "非 JSON",
    "MalformedReviewSignatureSnapshot",
    "MissingReviewSignatureSnapshot",
]

M2_EVIDENCE_GAP_TOKENS = [
    "EVIDENCE-GAP",
    "M2",
    "原始 RED",
]

M2_EVIDENCE_RESOLVED_TOKENS = [
    "EVIDENCE-RESOLVED",
    "M2",
    "原始 RED",
]


def read_evidence_files(task_dir: Path) -> tuple[list[dict[str, Any]], list[dict[str, str]]]:
    loaded: list[dict[str, Any]] = []
    missing: list[dict[str, str]] = []
    for relative_path in EVIDENCE_FILES:
        path = task_dir / relative_path
        if not path.exists() and relative_path in REQUIRED_EVIDENCE_FILES:
            missing.append(
                {
                    "code": "P0_TDD_EVIDENCE_FILE_MISSING",
                    "file": relative_path,
                    "message": f"Required P0 evidence file is missing: {relative_path}",
                }
            )
            continue
        if not path.exists():
            continue
        loaded.append(
            {
                "file": relative_path,
                "lines": path.read_text(encoding="utf-8").splitlines(),
            }
        )
    return loaded, missing


def contains_all(line: str, tokens: list[str]) -> bool:
    return all(token in line for token in tokens)


def contains_any(line: str, tokens: list[str]) -> bool:
    return any(token in line for token in tokens)


def collect_matching_lines(files: list[dict[str, Any]], predicate) -> list[dict[str, Any]]:
    matches: list[dict[str, Any]] = []
    for file_item in files:
        for index, line in enumerate(file_item["lines"], start=1):
            if predicate(line):
                matches.append(
                    {
                        "file": file_item["file"],
                        "line": index,
                        "text": line.strip(),
                    }
                )
    return matches


def is_original_m2_red(line: str) -> bool:
    return (
        contains_all(line, ORIGINAL_M2_RED_REQUIRED_TOKENS)
        and contains_any(line, ORIGINAL_M2_RED_REASON_TOKENS)
        and not contains_any(line, SNAPSHOT_RED_TOKENS)
    )


def is_m2_snapshot_red(line: str) -> bool:
    return (
        "RED:" in line
        and "MesP0TeamLeaderReviewSignatureServiceTest" in line
        and "FAIL" in line
        and contains_any(line, SNAPSHOT_RED_TOKENS)
    )


def is_m2_evidence_gap(line: str) -> bool:
    stripped = line.strip()
    if stripped.startswith("- EVIDENCE-GAP:"):
        return contains_all(stripped, M2_EVIDENCE_GAP_TOKENS)
    if stripped.startswith("- SEARCH-BLOCKED:"):
        return contains_all(stripped, M2_EVIDENCE_GAP_TOKENS)
    return False


def is_m2_evidence_resolved(line: str) -> bool:
    stripped = line.strip()
    if stripped.startswith("- EVIDENCE-RESOLVED:"):
        return contains_all(stripped, M2_EVIDENCE_RESOLVED_TOKENS)
    return False


def build_contract_payload() -> dict[str, Any]:
    return {
        "requiredFiles": REQUIRED_EVIDENCE_FILES,
        "optionalFiles": OPTIONAL_EVIDENCE_FILES,
        "m2OriginalRedRequiredTokens": ORIGINAL_M2_RED_REQUIRED_TOKENS,
        "m2OriginalRedReasonTokens": ORIGINAL_M2_RED_REASON_TOKENS,
        "m2SnapshotRedIgnoredTokens": SNAPSHOT_RED_TOKENS,
        "m2EvidenceGapTokens": M2_EVIDENCE_GAP_TOKENS,
        "m2EvidenceResolvedTokens": M2_EVIDENCE_RESOLVED_TOKENS,
    }


def evaluate(task_dir: Path) -> dict[str, Any]:
    files, blockers = read_evidence_files(task_dir)
    original_red_matches = collect_matching_lines(files, is_original_m2_red)
    snapshot_red_matches = collect_matching_lines(files, is_m2_snapshot_red)
    evidence_gap_matches = collect_matching_lines(files, is_m2_evidence_gap)
    evidence_resolved_matches = collect_matching_lines(files, is_m2_evidence_resolved)

    if not original_red_matches:
        blockers.append(
            {
                "code": "P0_TDD_EVIDENCE_GAP",
                "message": (
                    "Missing original M2 RED evidence for unsigned team-leader review/allocation; "
                    "snapshot fail-fast RED cannot substitute for the original behavior RED."
                ),
            }
        )
    if evidence_gap_matches and not evidence_resolved_matches:
        blockers.append(
            {
                "code": "P0_TDD_EVIDENCE_GAP_MARKER_PRESENT",
                "message": "M2 EVIDENCE-GAP marker is still present and must block M6 completion.",
                "matches": evidence_gap_matches,
            }
        )

    return {
        "status": "PASS" if not blockers else "BLOCKED",
        "taskDir": str(task_dir),
        "checkedFiles": [item["file"] for item in files],
        "blockers": blockers,
        "m2OriginalRed": {
            "found": bool(original_red_matches),
            "matches": original_red_matches,
        },
        "m2SnapshotRed": {
            "found": bool(snapshot_red_matches),
            "matches": snapshot_red_matches,
            "ignoredAsOriginalEvidence": True,
        },
        "m2EvidenceGap": {
            "found": bool(evidence_gap_matches),
            "matches": evidence_gap_matches,
            "resolved": bool(evidence_resolved_matches),
        },
        "m2EvidenceResolved": {
            "found": bool(evidence_resolved_matches),
            "matches": evidence_resolved_matches,
        },
        **build_contract_payload(),
    }


def emit(payload: dict[str, Any]) -> None:
    print(json.dumps(payload, ensure_ascii=False, indent=2, sort_keys=True))


def main() -> int:
    parser = argparse.ArgumentParser(description="Verify P0 TDD evidence completeness")
    parser.add_argument("--task-dir", default=str(DEFAULT_TASK_DIR))
    parser.add_argument("--print-contract", action="store_true")
    args = parser.parse_args()

    if args.print_contract:
        emit({"status": "PASS", **build_contract_payload()})
        return 0

    payload = evaluate(Path(args.task_dir).resolve())
    emit(payload)
    return 0 if payload["status"] == "PASS" else 2


if __name__ == "__main__":
    sys.exit(main())
