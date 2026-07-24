#!/usr/bin/env python3
"""Generate strict English-name showroom legacy product-code backfill SQL.

The generator is intentionally conservative:
- it never writes to the database;
- it only maps product_* rows to empty-legacy INT-* candidates;
- English names must match after deterministic normalization;
- ambiguous, occupied, missing, or non-current candidates are reported as blockers.
"""

from __future__ import annotations

import argparse
import csv
import json
import re
import sys
import unicodedata
from dataclasses import dataclass
from pathlib import Path


if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")
if hasattr(sys.stderr, "reconfigure"):
    sys.stderr.reconfigure(encoding="utf-8")

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_INPUT = ROOT / (
    "doc/tasks/20260705-showroom-legacy-product-code-mapping/"
    "product-int-name-unmapped-candidate-audit.csv"
)
DEFAULT_SQL = ROOT / "sql/mysql/20260706_showroom_legacy_product_code_english_name_backfill.sql"
DEFAULT_REPORT = ROOT / (
    "doc/tasks/20260706-showroom-legacy-code-english-name-backfill/"
    "english-name-backfill-report.json"
)


@dataclass(frozen=True)
class CandidateRow:
    tenant_id: int
    product_code: str
    product_name_cn: str
    product_name_en: str
    candidate_int_code: str
    candidate_int_name_cn: str
    candidate_int_name_en: str
    candidate_legacy_product_code: str


@dataclass(frozen=True)
class Mapping:
    tenant_id: int
    product_code: str
    product_name_cn: str
    product_name_en: str
    target_int_code: str
    target_int_name_cn: str
    target_int_name_en: str
    resolution: str
    proximity_distance: int | None = None


MAX_PROXIMITY_DISTANCE = 1


def read_rows(path: Path) -> list[dict[str, str]]:
    with path.open("r", encoding="utf-8-sig", newline="") as file:
        return [
            {key: (value or "").strip() for key, value in row.items()}
            for row in csv.DictReader(file)
        ]


def normalize_english(value: str) -> str:
    normalized = unicodedata.normalize("NFKC", value or "").replace("\u00a0", " ")
    return re.sub(r"\s+", " ", normalized).strip().casefold()


def sql_literal(value: str) -> str:
    return "'" + value.replace("'", "''") + "'"


def require_product_code(value: str) -> bool:
    return bool(re.fullmatch(r"product_\d+", value or "", flags=re.IGNORECASE))


def require_int_code(value: str) -> bool:
    return bool(re.fullmatch(r"INT-\d+", value or "", flags=re.IGNORECASE))


def product_number(value: str) -> int | None:
    match = re.fullmatch(r"product_(\d+)", value or "", flags=re.IGNORECASE)
    return int(match.group(1)) if match else None


def int_number(value: str) -> int | None:
    match = re.fullmatch(r"INT-(\d+)", value or "", flags=re.IGNORECASE)
    return int(match.group(1)) if match else None


def to_candidate(row: dict[str, str]) -> CandidateRow:
    return CandidateRow(
        tenant_id=int(row.get("tenant_id", "0")),
        product_code=row.get("product_code", ""),
        product_name_cn=row.get("product_name_cn", ""),
        product_name_en=row.get("product_name_en", ""),
        candidate_int_code=row.get("candidate_int_code", ""),
        candidate_int_name_cn=row.get("candidate_int_name_cn", ""),
        candidate_int_name_en=row.get("candidate_int_name_en", ""),
        candidate_legacy_product_code=row.get("candidate_legacy_product_code", ""),
    )


def blocker(row: CandidateRow, reason: str, details: str = "") -> dict[str, str | int]:
    return {
        "tenant_id": row.tenant_id,
        "product_code": row.product_code,
        "product_name_cn": row.product_name_cn,
        "product_name_en": row.product_name_en,
        "reason": reason,
        "details": details,
    }


def build_mapping(source: CandidateRow, selected: CandidateRow, resolution: str,
                  proximity_distance: int | None = None) -> Mapping:
    return Mapping(
        tenant_id=source.tenant_id,
        product_code=source.product_code,
        product_name_cn=source.product_name_cn,
        product_name_en=source.product_name_en,
        target_int_code=selected.candidate_int_code.upper(),
        target_int_name_cn=selected.candidate_int_name_cn,
        target_int_name_en=selected.candidate_int_name_en,
        resolution=resolution,
        proximity_distance=proximity_distance,
    )


def select_mappings(rows: list[dict[str, str]]) -> tuple[list[Mapping], list[dict[str, str | int]]]:
    grouped: dict[tuple[int, str], list[CandidateRow]] = {}
    for raw in rows:
        candidate = to_candidate(raw)
        if not require_product_code(candidate.product_code):
            raise ValueError(f"product_code 非 product_*：{candidate.product_code}")
        grouped.setdefault((candidate.tenant_id, candidate.product_code.lower()), []).append(candidate)

    mappings: list[Mapping] = []
    blockers: list[dict[str, str | int]] = []
    used_targets: set[tuple[int, str]] = set()

    for key in sorted(grouped.keys()):
        candidates = grouped[key]
        source = candidates[0]
        product_name_en = normalize_english(source.product_name_en)
        if not product_name_en:
            blockers.append(blocker(source, "SOURCE_ENGLISH_NAME_BLANK"))
            continue

        non_int_candidates = [
            candidate for candidate in candidates
            if candidate.candidate_int_code and not require_int_code(candidate.candidate_int_code)
        ]
        if non_int_candidates:
            blockers.append(blocker(
                source,
                "CANDIDATE_CODE_NOT_CURRENT_INT",
                ",".join(candidate.candidate_int_code for candidate in non_int_candidates),
            ))
            continue

        occupied_matches = [
            candidate for candidate in candidates
            if require_int_code(candidate.candidate_int_code)
            and candidate.candidate_legacy_product_code
            and normalize_english(candidate.candidate_int_name_en) == product_name_en
        ]
        if occupied_matches:
            blockers.append(blocker(
                source,
                "CANDIDATE_LEGACY_CODE_ALREADY_SET",
                ",".join(
                    f"{candidate.candidate_int_code}:{candidate.candidate_legacy_product_code}"
                    for candidate in occupied_matches
                ),
            ))
            continue

        matched = [
            candidate for candidate in candidates
            if require_int_code(candidate.candidate_int_code)
            and not candidate.candidate_legacy_product_code
            and normalize_english(candidate.candidate_int_name_en) == product_name_en
        ]
        target_codes = sorted({candidate.candidate_int_code.upper() for candidate in matched})
        if not target_codes:
            blockers.append(blocker(source, "ENGLISH_NAME_MATCH_NOT_FOUND"))
            continue
        if len(target_codes) != 1:
            product_num = product_number(source.product_code)
            product_name_cn = normalize_english(source.product_name_cn)
            exact_name_matches = [
                candidate for candidate in matched
                if normalize_english(candidate.candidate_int_name_cn) == product_name_cn
                and int_number(candidate.candidate_int_code) is not None
            ]
            proximity_candidates = sorted(
                (
                    abs(int_number(candidate.candidate_int_code) - product_num),
                    int_number(candidate.candidate_int_code),
                    candidate,
                )
                for candidate in exact_name_matches
                if product_num is not None
            )
            exact_codes = sorted({candidate.candidate_int_code.upper() for candidate in exact_name_matches})
            if len(exact_codes) != len(target_codes):
                blockers.append(blocker(source, "ENGLISH_NAME_MATCH_NOT_UNIQUE", ",".join(target_codes)))
                continue
            if not proximity_candidates:
                blockers.append(blocker(source, "PROXIMITY_CANDIDATE_NUMBER_MISSING", ",".join(target_codes)))
                continue
            if len(proximity_candidates) > 1 and proximity_candidates[0][0] == proximity_candidates[1][0]:
                blockers.append(blocker(source, "PROXIMITY_NEAREST_DISTANCE_TIED", ",".join(target_codes)))
                continue
            best_distance, _, selected = proximity_candidates[0]
            if best_distance > MAX_PROXIMITY_DISTANCE:
                blockers.append(blocker(
                    source,
                    "PROXIMITY_NEAREST_DISTANCE_TOO_LARGE",
                    f"distance={best_distance}; candidates={','.join(target_codes)}",
                ))
                continue
            target_key = (source.tenant_id, selected.candidate_int_code.upper())
            if target_key in used_targets:
                blockers.append(blocker(source, "TARGET_INT_ALREADY_SELECTED", selected.candidate_int_code.upper()))
                continue
            used_targets.add(target_key)
            mappings.append(build_mapping(source, selected, "PROXIMITY_UNIQUE_NEAREST", best_distance))
            continue
        target_code = target_codes[0]
        target_key = (source.tenant_id, target_code)
        if target_key in used_targets:
            blockers.append(blocker(source, "TARGET_INT_ALREADY_SELECTED", target_code))
            continue
        selected = next(candidate for candidate in matched if candidate.candidate_int_code.upper() == target_code)
        used_targets.add(target_key)
        mappings.append(build_mapping(source, selected, "ENGLISH_NAME_UNIQUE"))

    return mappings, blockers


def build_sql(mappings: list[Mapping]) -> str:
    lines = [
        "-- 20260706 showroom legacy product code English-name backfill",
        "-- Generated from strict unique normalized English-name matches only.",
        "-- Safe guards: target legacy_product_code must still be NULL and product_* must not be occupied.",
        "START TRANSACTION;",
    ]
    for mapping in mappings:
        lines.extend([
            "",
            f"-- tenant_id={mapping.tenant_id} {mapping.product_code} -> {mapping.target_int_code}",
            f"-- product_name_en={mapping.product_name_en}",
            "UPDATE showroom_product",
            f"SET legacy_product_code = {sql_literal(mapping.product_code)}",
            f"WHERE tenant_id = {mapping.tenant_id}",
            f"AND product_code = {sql_literal(mapping.target_int_code)}",
            "AND deleted = 0",
            "AND legacy_product_code IS NULL",
            "AND NOT EXISTS (",
            "    SELECT 1 FROM (",
            "        SELECT id FROM showroom_product",
            f"        WHERE tenant_id = {mapping.tenant_id}",
            f"        AND legacy_product_code = {sql_literal(mapping.product_code)}",
            "        AND deleted = 0",
            "    ) occupied",
            ");",
        ])
    lines.append("COMMIT;")
    return "\n".join(lines) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", type=Path, default=DEFAULT_INPUT)
    parser.add_argument("--output-sql", type=Path, default=DEFAULT_SQL)
    parser.add_argument("--report", type=Path, default=DEFAULT_REPORT)
    args = parser.parse_args()

    args.report.parent.mkdir(parents=True, exist_ok=True)
    try:
        mappings, blockers = select_mappings(read_rows(args.input))
    except Exception as exception:  # fail fast with a readable report for malformed audit input
        args.report.write_text(json.dumps({
            "status": "BLOCKED",
            "reason": str(exception),
            "mapping_count": 0,
            "blocker_count": 1,
        }, ensure_ascii=False, indent=2), encoding="utf-8")
        print(f"BLOCKED: {exception}")
        return 2

    status = "PASS" if mappings and not blockers else "PASS_WITH_BLOCKERS" if mappings else "BLOCKED"
    payload = {
        "status": status,
        "input": str(args.input),
        "output_sql": str(args.output_sql),
        "mapping_count": len(mappings),
        "blocker_count": len(blockers),
        "mappings": [mapping.__dict__ for mapping in mappings],
        "blockers": blockers,
    }
    args.report.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    if not mappings:
        if args.output_sql.exists():
            args.output_sql.unlink()
        print(f"BLOCKED: no English-name mappings generated. Report: {args.report}")
        return 2

    args.output_sql.parent.mkdir(parents=True, exist_ok=True)
    args.output_sql.write_text(build_sql(mappings), encoding="utf-8")
    if blockers:
        print(
            f"PASS_WITH_BLOCKERS: generated {len(mappings)} English-name mapping SQL rows; "
            f"{len(blockers)} rows still need manual review. SQL: {args.output_sql}; Report: {args.report}"
        )
    else:
        print(f"PASS: generated {len(mappings)} English-name mapping SQL rows -> {args.output_sql}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
