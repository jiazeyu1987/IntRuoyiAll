#!/usr/bin/env python3
"""Generate protected SQL for current empty showroom legacy product codes.

This script consumes the current-db recognition report and applies the final
safe-selection gate:
- same-number-only rows are never auto-confirmed unless names also match;
- strict CN/EN matches may resolve duplicate product_* candidates by nearest
  INT number;
- far or ambiguous matches stay as blockers;
- generated SQL is guarded and is not executed by this script.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
import unicodedata
from dataclasses import dataclass, asdict
from pathlib import Path
from typing import Any


if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")
if hasattr(sys.stderr, "reconfigure"):
    sys.stderr.reconfigure(encoding="utf-8")


ROOT = Path(__file__).resolve().parents[1]
TASK_DIR = ROOT / "doc/tasks/20260706-showroom-empty-legacy-current-db-recognition"
DEFAULT_INPUT_REPORT = TASK_DIR / "current-empty-legacy-recognition-report.json"
DEFAULT_OUTPUT_REPORT = TASK_DIR / "current-empty-legacy-sql-report.json"
DEFAULT_OUTPUT_SQL = ROOT / "sql/mysql/20260706_showroom_current_empty_legacy_product_code_backfill.sql"

MAX_SAFE_DISTANCE = 1


@dataclass(frozen=True)
class Recognition:
    tenant_id: int
    target_product_code: str
    target_name_cn: str
    target_name_en: str
    recognized_product_code: str
    recognized_name_cn: str
    recognized_name_en: str
    rule: str
    distance: int | None
    source_row_no: int | None

    @property
    def old_key(self) -> tuple[int, str]:
        return self.tenant_id, self.recognized_product_code.lower()

    @property
    def target_key(self) -> tuple[int, str]:
        return self.tenant_id, self.target_product_code.upper()

    @property
    def cn_equal(self) -> bool:
        return bool(normalize_text(self.target_name_cn)) and (
            normalize_text(self.target_name_cn) == normalize_text(self.recognized_name_cn)
        )

    @property
    def en_equal(self) -> bool:
        return bool(normalize_text(self.target_name_en)) and (
            normalize_text(self.target_name_en) == normalize_text(self.recognized_name_en)
        )

    @property
    def strong_name_score(self) -> int:
        return int(self.cn_equal) + int(self.en_equal)


@dataclass(frozen=True)
class Mapping:
    tenant_id: int
    recognized_product_code: str
    target_product_code: str
    target_name_cn: str
    target_name_en: str
    rule: str
    distance: int | None
    source_row_no: int | None
    resolution: str


def normalize_text(value: str) -> str:
    normalized = unicodedata.normalize("NFKC", value or "").replace("\u00a0", " ")
    return re.sub(r"\s+", " ", normalized).strip().casefold()


def sql_literal(value: str) -> str:
    return "'" + value.replace("'", "''") + "'"


def require_product_code(value: str) -> bool:
    return bool(re.fullmatch(r"product_\d+", value or "", flags=re.IGNORECASE))


def require_int_code(value: str) -> bool:
    return bool(re.fullmatch(r"INT-\d+", value or "", flags=re.IGNORECASE))


def as_int(value: Any) -> int | None:
    if value in ("", None):
        return None
    return int(value)


def load_recognitions(path: Path) -> tuple[dict[str, Any], list[Recognition], list[dict[str, Any]]]:
    payload = json.loads(path.read_text(encoding="utf-8"))
    recognitions: list[Recognition] = []
    for item in payload.get("recognitions", []):
        recognition = Recognition(
            tenant_id=int(item["tenant_id"]),
            target_product_code=str(item.get("target_product_code", "")).upper(),
            target_name_cn=str(item.get("target_name_cn", "")),
            target_name_en=str(item.get("target_name_en", "")),
            recognized_product_code=str(item.get("recognized_product_code", "")),
            recognized_name_cn=str(item.get("recognized_name_cn", "")),
            recognized_name_en=str(item.get("recognized_name_en", "")),
            rule=str(item.get("rule", "")),
            distance=as_int(item.get("distance")),
            source_row_no=as_int(item.get("source_row_no")),
        )
        if not require_int_code(recognition.target_product_code):
            raise ValueError(f"目标产品编码不是当前 INT-*：{recognition.target_product_code}")
        if not require_product_code(recognition.recognized_product_code):
            raise ValueError(f"识别旧编号不是 product_*：{recognition.recognized_product_code}")
        recognitions.append(recognition)
    return payload, recognitions, list(payload.get("blocked", []))


def make_mapping(recognition: Recognition, resolution: str) -> Mapping:
    return Mapping(
        tenant_id=recognition.tenant_id,
        recognized_product_code=recognition.recognized_product_code.lower(),
        target_product_code=recognition.target_product_code.upper(),
        target_name_cn=recognition.target_name_cn,
        target_name_en=recognition.target_name_en,
        rule=recognition.rule,
        distance=recognition.distance,
        source_row_no=recognition.source_row_no,
        resolution=resolution,
    )


def blocker(recognition: Recognition, reason: str, details: str = "") -> dict[str, Any]:
    return {
        "tenant_id": recognition.tenant_id,
        "recognized_product_code": recognition.recognized_product_code,
        "target_product_code": recognition.target_product_code,
        "target_name_cn": recognition.target_name_cn,
        "target_name_en": recognition.target_name_en,
        "recognized_name_cn": recognition.recognized_name_cn,
        "recognized_name_en": recognition.recognized_name_en,
        "rule": recognition.rule,
        "distance": recognition.distance,
        "reason": reason,
        "details": details,
    }


def safe_single_candidate(recognition: Recognition) -> tuple[Mapping | None, dict[str, Any] | None]:
    if recognition.strong_name_score == 0:
        if recognition.rule == "SAME_NUMBER_ONLY_REVIEW":
            return None, blocker(recognition, "SAME_NUMBER_ONLY_NAME_MISMATCH")
        return None, blocker(recognition, "NAME_MATCH_NOT_CONFIRMED")
    if recognition.distance is None:
        return None, blocker(recognition, "MATCH_DISTANCE_MISSING")
    if recognition.distance > MAX_SAFE_DISTANCE:
        return None, blocker(recognition, "MATCH_DISTANCE_TOO_LARGE")
    if recognition.rule == "CN_UNIQUE" and recognition.strong_name_score < 2:
        return None, blocker(recognition, "CN_ONLY_REQUIRES_MANUAL_REVIEW")
    resolution = "STRICT_CN_EN" if recognition.strong_name_score == 2 else "STRICT_EN"
    return make_mapping(recognition, resolution), None


def select_mappings(recognitions: list[Recognition], inherited_blockers: list[dict[str, Any]]) -> tuple[list[Mapping], list[dict[str, Any]]]:
    blockers: list[dict[str, Any]] = list(inherited_blockers)
    grouped: dict[tuple[int, str], list[Recognition]] = {}
    for recognition in recognitions:
        grouped.setdefault(recognition.old_key, []).append(recognition)

    mappings: list[Mapping] = []
    used_targets: set[tuple[int, str]] = set()

    for key in sorted(grouped.keys()):
        candidates = sorted(
            grouped[key],
            key=lambda item: (
                -(item.strong_name_score),
                item.distance if item.distance is not None else 10_000,
                item.target_product_code,
            ),
        )
        safe_candidates: list[Recognition] = []
        candidate_blockers: list[dict[str, Any]] = []
        for candidate in candidates:
            mapping, block = safe_single_candidate(candidate)
            if mapping is not None:
                safe_candidates.append(candidate)
            elif block is not None:
                candidate_blockers.append(block)

        if not safe_candidates:
            blockers.extend(candidate_blockers)
            continue

        strongest_score = max(candidate.strong_name_score for candidate in safe_candidates)
        strongest = [candidate for candidate in safe_candidates if candidate.strong_name_score == strongest_score]
        nearest_distance = min(candidate.distance or 0 for candidate in strongest)
        nearest = [candidate for candidate in strongest if candidate.distance == nearest_distance]
        if len(nearest) != 1:
            blockers.append(blocker(candidates[0], "OLD_CODE_MATCH_NOT_UNIQUE", ",".join(c.target_product_code for c in nearest)))
            continue

        selected = nearest[0]
        if selected.target_key in used_targets:
            blockers.append(blocker(selected, "TARGET_ALREADY_SELECTED"))
            continue
        used_targets.add(selected.target_key)
        resolution = (
            "CONFLICT_STRONG_UNIQUE_NEAREST"
            if len(candidates) > 1
            else "STRICT_CN_EN" if selected.strong_name_score == 2
            else "STRICT_EN"
        )
        mappings.append(make_mapping(selected, resolution))

    return mappings, blockers


def build_sql(mappings: list[Mapping]) -> str:
    lines = [
        "-- 20260706 showroom current empty legacy product code backfill",
        "-- Generated from current-db empty legacy recognition with strict name/proximity gates.",
        "-- Safe guards: target legacy_product_code must still be NULL and product_* must not be occupied.",
        "START TRANSACTION;",
    ]
    for mapping in sorted(mappings, key=lambda item: (item.tenant_id, item.target_product_code)):
        lines.extend([
            "",
            f"-- tenant_id={mapping.tenant_id} {mapping.recognized_product_code} -> {mapping.target_product_code}",
            f"-- resolution={mapping.resolution}; rule={mapping.rule}; distance={mapping.distance}",
            "UPDATE showroom_product",
            f"SET legacy_product_code = {sql_literal(mapping.recognized_product_code)}",
            f"WHERE tenant_id = {mapping.tenant_id}",
            f"AND product_code = {sql_literal(mapping.target_product_code)}",
            "AND deleted = 0",
            "AND legacy_product_code IS NULL",
            "AND NOT EXISTS (",
            "    SELECT 1 FROM (",
            "        SELECT id FROM showroom_product",
            f"        WHERE tenant_id = {mapping.tenant_id}",
            f"        AND legacy_product_code = {sql_literal(mapping.recognized_product_code)}",
            "        AND deleted = 0",
            "    ) occupied",
            ");",
        ])
    lines.append("COMMIT;")
    return "\n".join(lines) + "\n"


def write_report(path: Path, source_payload: dict[str, Any], mappings: list[Mapping], blockers: list[dict[str, Any]], output_sql: Path) -> str:
    status = "PASS" if mappings and not blockers else "PASS_WITH_BLOCKERS" if mappings else "BLOCKED"
    payload = {
        "status": status,
        "input_report": str(source_payload.get("database", "")),
        "source_workbook": str(source_payload.get("workbook", "")),
        "output_sql": str(output_sql),
        "mapping_count": len(mappings),
        "blocker_count": len(blockers),
        "mappings": [asdict(mapping) for mapping in sorted(mappings, key=lambda item: (item.tenant_id, item.target_product_code))],
        "blockers": blockers,
    }
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    return status


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input-report", type=Path, default=DEFAULT_INPUT_REPORT)
    parser.add_argument("--output-sql", type=Path, default=DEFAULT_OUTPUT_SQL)
    parser.add_argument("--output-report", type=Path, default=DEFAULT_OUTPUT_REPORT)
    args = parser.parse_args()

    try:
        source_payload, recognitions, inherited_blockers = load_recognitions(args.input_report)
        mappings, blockers = select_mappings(recognitions, inherited_blockers)
    except Exception as exception:
        args.output_report.parent.mkdir(parents=True, exist_ok=True)
        args.output_report.write_text(json.dumps({
            "status": "BLOCKED",
            "reason": str(exception),
            "mapping_count": 0,
            "blocker_count": 1,
        }, ensure_ascii=False, indent=2), encoding="utf-8")
        print(f"BLOCKED: {exception}")
        return 2

    status = write_report(args.output_report, source_payload, mappings, blockers, args.output_sql)
    if not mappings:
        if args.output_sql.exists():
            args.output_sql.unlink()
        print(f"BLOCKED: no safe current empty legacy mappings generated. Report: {args.output_report}")
        return 2

    args.output_sql.parent.mkdir(parents=True, exist_ok=True)
    args.output_sql.write_text(build_sql(mappings), encoding="utf-8")
    if blockers:
        print(
            f"PASS_WITH_BLOCKERS: generated {len(mappings)} protected SQL rows; "
            f"{len(blockers)} rows remain blocked. SQL: {args.output_sql}; Report: {args.output_report}"
        )
    else:
        print(f"PASS: generated {len(mappings)} protected SQL rows -> {args.output_sql}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
