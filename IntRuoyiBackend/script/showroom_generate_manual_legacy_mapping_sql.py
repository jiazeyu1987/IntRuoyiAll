#!/usr/bin/env python3
"""Generate reviewed showroom legacy product-code mapping SQL from the manual decision CSV.

The generator is intentionally strict:
- it never writes to the database;
- it only emits SQL for rows with a manual decision;
- each emitted row must make the final INT product name equal the reviewed product name;
- rows without manual decisions are kept as blockers in the JSON report.
"""

from __future__ import annotations

import argparse
import csv
import json
import re
import sys
from dataclasses import dataclass
from pathlib import Path


if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")
if hasattr(sys.stderr, "reconfigure"):
    sys.stderr.reconfigure(encoding="utf-8")

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_INPUT = ROOT / "doc/tasks/20260705-showroom-legacy-product-code-mapping/product-int-name-manual-decision-template-v2.csv"
DEFAULT_SQL = ROOT / "sql/mysql/20260705_showroom_legacy_product_code_manual_confirmed.sql"
DEFAULT_REPORT = ROOT / "doc/tasks/20260705-showroom-legacy-product-code-mapping/product-int-name-manual-confirmed-report.json"

VALID_ACTIONS = {
    "MAP_ONLY_NAMES_ALREADY_EQUAL",
    "RENAME_INT_TO_PRODUCT_NAME_AND_MAP",
}


@dataclass(frozen=True)
class ManualDecision:
    tenant_id: int
    product_code: str
    product_name_cn: str
    product_name_en: str
    target_int_code: str
    target_int_name_cn: str
    target_int_name_en: str
    final_name_cn: str
    final_name_en: str
    action: str
    notes: str


def read_rows(path: Path) -> list[dict[str, str]]:
    with path.open("r", encoding="utf-8-sig", newline="") as file:
        return [
            {key: (value or "").strip() for key, value in row.items()}
            for row in csv.DictReader(file)
        ]


def sql_literal(value: str) -> str:
    return "'" + value.replace("'", "''") + "'"


def require_product_code(value: str, row_number: int) -> None:
    if not re.fullmatch(r"product_\d+", value or "", flags=re.IGNORECASE):
        raise ValueError(f"第 {row_number} 行 product_code 非 product_*：{value}")


def require_int_code(value: str, row_number: int) -> None:
    if not re.fullmatch(r"INT-\d+", value or "", flags=re.IGNORECASE):
        raise ValueError(f"第 {row_number} 行 manual_decision_int_code 非 INT-*：{value}")


def parse_decisions(rows: list[dict[str, str]]) -> tuple[list[ManualDecision], list[dict[str, str]]]:
    decisions: list[ManualDecision] = []
    blockers: list[dict[str, str]] = []
    seen: set[tuple[int, str]] = set()

    for index, row in enumerate(rows, start=2):
        product_code = row.get("product_code", "")
        target_int_code = row.get("manual_decision_int_code", "")
        final_name_cn = row.get("manual_decision_final_name_cn", "")
        action = row.get("manual_decision_action", "")
        if not target_int_code and not final_name_cn and not action:
            blockers.append({
                "row": str(index),
                "tenant_id": row.get("tenant_id", ""),
                "product_code": product_code,
                "product_name_cn": row.get("product_name_cn", ""),
                "reason": "manual_decision_* 未填写",
            })
            continue
        require_product_code(product_code, index)
        require_int_code(target_int_code, index)
        if action not in VALID_ACTIONS:
            raise ValueError(f"第 {index} 行 manual_decision_action 必须是 {sorted(VALID_ACTIONS)}，实际={action}")
        tenant_id = int(row.get("tenant_id", "0"))
        key = (tenant_id, product_code.lower())
        if key in seen:
            raise ValueError(f"第 {index} 行重复确认同一租户 product_code：tenant_id={tenant_id}, product_code={product_code}")
        seen.add(key)

        product_name_cn = row.get("product_name_cn", "")
        product_name_en = row.get("product_name_en", "")
        target_name_cn = row.get("top_unassigned_candidate_name_cn", "") or row.get("same_x_int_name_cn", "")
        target_name_en = row.get("top_unassigned_candidate_name_en", "") or row.get("same_x_int_name_en", "")
        final_name_en = row.get("manual_decision_final_name_en", "") or product_name_en
        if not final_name_cn:
            raise ValueError(f"第 {index} 行 manual_decision_final_name_cn 不能为空")
        if final_name_cn != product_name_cn:
            raise ValueError(
                f"第 {index} 行最终中文名必须等于 product 中文名，避免映射后名字不一致："
                f"final={final_name_cn}, product={product_name_cn}"
            )
        if action == "MAP_ONLY_NAMES_ALREADY_EQUAL" and target_name_cn and target_name_cn != product_name_cn:
            raise ValueError(
                f"第 {index} 行选择 MAP_ONLY 但候选 INT 名称不等于 product 名称："
                f"int={target_name_cn}, product={product_name_cn}"
            )

        decisions.append(ManualDecision(
            tenant_id=tenant_id,
            product_code=product_code,
            product_name_cn=product_name_cn,
            product_name_en=product_name_en,
            target_int_code=target_int_code,
            target_int_name_cn=target_name_cn,
            target_int_name_en=target_name_en,
            final_name_cn=final_name_cn,
            final_name_en=final_name_en,
            action=action,
            notes=row.get("manual_decision_notes", ""),
        ))

    return decisions, blockers


def build_sql(decisions: list[ManualDecision]) -> str:
    lines = [
        "-- 20260705 showroom legacy product code manual confirmed mapping",
        "-- Generated from reviewed manual_decision_* rows only.",
        "-- This SQL keeps the final INT current product name equal to the confirmed product name.",
        "START TRANSACTION;",
    ]
    for decision in decisions:
        lines.extend([
            "",
            f"-- tenant_id={decision.tenant_id} {decision.product_code} -> {decision.target_int_code}",
            f"-- confirmed_name_cn={decision.final_name_cn}",
        ])
        if decision.action == "RENAME_INT_TO_PRODUCT_NAME_AND_MAP":
            lines.append(
                "UPDATE showroom_product_revision r "
                "JOIN showroom_product p ON p.current_revision_id = r.id "
                f"SET r.name_cn = {sql_literal(decision.final_name_cn)}, "
                f"r.name_en = {sql_literal(decision.final_name_en)} "
                f"WHERE p.tenant_id = {decision.tenant_id} "
                f"AND p.product_code = {sql_literal(decision.target_int_code)} "
                "AND p.deleted = 0 AND r.deleted = 0;"
            )
        lines.append(
            "UPDATE showroom_product "
            f"SET legacy_product_code = {sql_literal(decision.product_code)} "
            f"WHERE tenant_id = {decision.tenant_id} "
            f"AND product_code = {sql_literal(decision.target_int_code)} "
            "AND deleted = 0;"
        )
    lines.append("COMMIT;")
    return "\n".join(lines) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", type=Path, default=DEFAULT_INPUT)
    parser.add_argument("--output-sql", type=Path, default=DEFAULT_SQL)
    parser.add_argument("--report", type=Path, default=DEFAULT_REPORT)
    parser.add_argument("--allow-partial", action="store_true",
                        help="Generate SQL for filled decisions even when other rows remain blank.")
    args = parser.parse_args()

    rows = read_rows(args.input)
    try:
        decisions, blockers = parse_decisions(rows)
    except ValueError as exception:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(json.dumps({
            "status": "BLOCKED",
            "reason": str(exception),
            "decision_count": 0,
            "blocker_count": 1,
        }, ensure_ascii=False, indent=2), encoding="utf-8")
        print(f"BLOCKED: {exception}")
        return 2
    if blockers and not args.allow_partial:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(json.dumps({
            "status": "BLOCKED",
            "reason": "存在未填写 manual_decision_* 的行；如需部分生成请显式传 --allow-partial",
            "decision_count": len(decisions),
            "blocker_count": len(blockers),
            "blockers": blockers,
        }, ensure_ascii=False, indent=2), encoding="utf-8")
        print(f"BLOCKED: {len(blockers)} rows still need manual decisions. Report: {args.report}")
        return 2
    if not decisions:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(json.dumps({
            "status": "BLOCKED",
            "reason": "没有任何可生成 SQL 的 manual_decision 行",
            "decision_count": 0,
            "blocker_count": len(blockers),
            "blockers": blockers,
        }, ensure_ascii=False, indent=2), encoding="utf-8")
        print(f"BLOCKED: no confirmed manual decisions. Report: {args.report}")
        return 2

    args.output_sql.parent.mkdir(parents=True, exist_ok=True)
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.output_sql.write_text(build_sql(decisions), encoding="utf-8")
    args.report.write_text(json.dumps({
        "status": "PASS",
        "input": str(args.input),
        "output_sql": str(args.output_sql),
        "decision_count": len(decisions),
        "blocker_count": len(blockers),
        "allow_partial": args.allow_partial,
        "decisions": [decision.__dict__ for decision in decisions],
        "blockers": blockers,
    }, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"PASS: generated {len(decisions)} manual mapping SQL rows -> {args.output_sql}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
