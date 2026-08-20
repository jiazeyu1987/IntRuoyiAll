"""Build an auditable historical registration-certificate commit plan.

This module never writes the database or the source workbook.  It converts the
read-only preflight report into an explicit tenant sub-batch plan and fails
closed for facts that are not present in the approved source/evidence.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path

SCRIPT_ROOT = Path(__file__).resolve().parent
if str(SCRIPT_ROOT.parent) not in sys.path:
    sys.path.insert(0, str(SCRIPT_ROOT.parent))

from script.registration_certificate_migration_preflight import (
    DEFAULT_SOURCE_WORKBOOK,
    EXPECTED_SOURCE_SHA256,
    MIGRATION_ADMIN_PERMISSION,
    MIGRATION_PREFLIGHT_PERMISSION,
    build_preflight_report,
)


MIGRATION_COMMIT_PERMISSION = "dcc:registration-certificate:migration:commit"
TARGET_TENANT_ID = 1
TARGET_TENANT_NAME = "芋道源码"
NO_ATTACHMENT_POLICY = "NO_ATTACHMENT_POLICY"
MISSING_PROJECT_CODE = "MISSING_PROJECT_CODE"
MISSING_REGISTRANT_NAME = "MISSING_REGISTRANT_NAME"
UNRESOLVED_PRODUCTION_RELATION = "UNRESOLVED_PRODUCTION_RELATION"
MISSING_FORMAL_MAPPING = "MISSING_FORMAL_MAPPING"


class MigrationCommitPolicyError(RuntimeError):
    pass


class MigrationCommitPermissionError(PermissionError):
    pass


def assert_commit_permission(permissions):
    permission_set = set(permissions or ())
    if MIGRATION_COMMIT_PERMISSION not in permission_set and MIGRATION_ADMIN_PERMISSION not in permission_set:
        raise MigrationCommitPermissionError(
            "historical registration-certificate commit requires migration commit permission"
        )


def build_commit_plan(
    workbook_path=DEFAULT_SOURCE_WORKBOOK,
    *,
    permissions=None,
    expected_sha256=EXPECTED_SOURCE_SHA256,
    target_tenant_id=TARGET_TENANT_ID,
    target_tenant_name=TARGET_TENANT_NAME,
    allow_missing_project_code=True,
    import_attachments=False,
    formal_mapping=None,
):
    """Return an explicit commit plan; no persistence side effect is performed."""
    assert_commit_permission(permissions or ())
    if import_attachments:
        raise MigrationCommitPolicyError(
            "historical attachments are not approved in this batch; no attachment import is supported"
        )
    if target_tenant_id != TARGET_TENANT_ID or target_tenant_name != TARGET_TENANT_NAME:
        raise MigrationCommitPolicyError("historical source is approved only for the 芋道源码 tenant")
    if not allow_missing_project_code:
        raise MigrationCommitPolicyError("the approved policy requires missing project codes to be restricted records")

    report = build_preflight_report(
        workbook_path,
        permissions={MIGRATION_PREFLIGHT_PERMISSION},
        expected_sha256=expected_sha256,
    )
    mappings = formal_mapping or {}
    rows = [_build_row(row, mappings) for row in report["rows"]]
    blocked_rows = [row for row in rows if row["status"] == "BLOCKED"]
    ready_rows = [row for row in rows if row["status"] == "READY_FOR_COMMIT"]
    restricted_rows = [row for row in rows if row["restricted_reasons"]]

    return {
        "status": "READY" if not blocked_rows else "BLOCKED",
        "source_path": report["source_path"],
        "source_sha256": report["source_sha256"],
        "target_tenant_id": target_tenant_id,
        "target_tenant_name": target_tenant_name,
        "approval_date_policy": "EFFECTIVE_DATE",
        "attachment_policy": "DO_NOT_IMPORT",
        "files_to_import": 0,
        "fake_file_links": 0,
        "domestic_row_count": report["domestic_row_count"],
        "company_count": report["company_count"],
        "missing_project_code_count": report["missing_project_code_count"],
        "sheet_import_counts": report["sheet_import_counts"],
        "excluded_sheet_row_counts": report["excluded_sheet_row_counts"],
        "ready_row_count": len(ready_rows),
        "blocked_row_count": len(blocked_rows),
        "restricted_row_count": len(restricted_rows),
        "rows": rows,
    }


def _build_row(preflight_row, mappings):
    normalized = preflight_row["normalized"]
    source_row = preflight_row["source_row"]
    owner_name = normalized.get("owner_company_name")
    product_name = normalized.get("product_name")
    project_code = normalized.get("project_code")
    remark = normalized.get("remark") or ""

    production = _parse_production_relation(remark)
    restricted_reasons = [NO_ATTACHMENT_POLICY]
    blockers = []
    if project_code is None:
        restricted_reasons.append(MISSING_PROJECT_CODE)
    if production["status"] == "BLOCKED":
        blockers.append(UNRESOLVED_PRODUCTION_RELATION)
    if not normalized.get("certificate_no") or not normalized.get("first_obtained_date"):
        blockers.append("MISSING_CERTIFICATE_DATE_FACT")
    # The user approved the workbook as authoritative and confirmed that the
    # company column is the registrant for this historical source.  An explicit
    # per-certificate mapping may override it, but no other source is consulted.
    registrant_name = _lookup_text(mappings.get("registrant_names"), normalized.get("certificate_no")) or owner_name
    if registrant_name is None:
        blockers.append(MISSING_REGISTRANT_NAME)

    owner_company_id = _lookup(mappings.get("owner_company_ids"), owner_name)
    product_master_id = _lookup(mappings.get("product_master_ids"), product_name)
    project_code_id = _lookup(mappings.get("project_code_ids"), project_code) if project_code else None
    if owner_company_id is None or product_master_id is None or (project_code and project_code_id is None):
        blockers.append(MISSING_FORMAL_MAPPING)
    entrusted_ids = []
    for enterprise_name in production["entrusted_enterprises"]:
        enterprise_id = _lookup(mappings.get("entrusted_enterprise_ids"), enterprise_name)
        if enterprise_id is None:
            blockers.append(MISSING_FORMAL_MAPPING)
        else:
            entrusted_ids.append(enterprise_id)

    row = {
        "source_sheet": preflight_row["source_sheet"],
        "source_row": source_row,
        "owner_company_name": owner_name,
        "owner_company_id": owner_company_id,
        "project_code": project_code,
        "project_code_id": project_code_id,
        "product_name": product_name,
        "product_master_id": product_master_id,
        "certificate_no": normalized.get("certificate_no"),
        "registrant_name": registrant_name,
        "first_obtained_date": normalized.get("first_obtained_date"),
        "effective_date": normalized.get("effective_date"),
        "approval_date": normalized.get("effective_date"),
        "expiry_date": normalized.get("expiry_date"),
        "classification": normalized.get("classification"),
        "remark": remark or None,
        "production_relation": production["relation"],
        "self_production": production["self_production"],
        "entrusted_production": production["entrusted_production"],
        "entrusted_enterprises": production["entrusted_enterprises"],
        "entrusted_enterprise_ids": entrusted_ids,
        "restricted_reasons": sorted(set(restricted_reasons)),
        "blockers": sorted(set(blockers)),
        "status": "BLOCKED" if blockers else "READY_FOR_COMMIT",
        # Deliberately absent: business_file_id, infra_file_id, file_url.
    }
    return row


def _lookup(mapping, key):
    if not key or not mapping:
        return None
    value = mapping.get(key)
    return value if isinstance(value, int) and value > 0 else None


def _lookup_text(mapping, key):
    if not key or not mapping:
        return None
    value = mapping.get(key)
    if isinstance(value, str) and value.strip():
        return value.strip()
    return None


def _parse_production_relation(remark):
    has_entrusted = "委托" in remark
    has_self = "自行" in remark
    if not has_entrusted and not has_self:
        # The approved policy delegates production-relation normalization to
        # this importer.  A blank remark is treated as self-production only;
        # any explicit 委托 wording still requires a resolvable enterprise.
        return {
            "status": "RESOLVED",
            "relation": "SELF",
            "self_production": True,
            "entrusted_production": False,
            "entrusted_enterprises": [],
        }

    enterprises = []
    if has_entrusted:
        if "山东瑛泰" in remark:
            enterprises.append("山东瑛泰医疗器械有限公司")
        if "德瑞" in remark:
            enterprises.append("珠海德瑞医疗器械有限公司")
        if not enterprises:
            match = re.search(r"委托([^+，,。；;\s]+)生产", remark)
            if match and "有限公司" in match.group(1):
                enterprises.append(match.group(1))
            else:
                return {
                    "status": "BLOCKED",
                    "relation": None,
                    "self_production": None,
                    "entrusted_production": None,
                    "entrusted_enterprises": [],
                }
    relation = "BOTH" if has_entrusted and has_self else "ENTRUSTED" if has_entrusted else "SELF"
    return {
        "status": "RESOLVED",
        "relation": relation,
        "self_production": has_self,
        "entrusted_production": has_entrusted,
        "entrusted_enterprises": enterprises,
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--workbook", default=str(DEFAULT_SOURCE_WORKBOOK))
    parser.add_argument("--permission", action="append", default=[MIGRATION_COMMIT_PERMISSION])
    parser.add_argument("--expected-sha256", default=EXPECTED_SOURCE_SHA256)
    parser.add_argument("--formal-mapping-json")
    parser.add_argument("--output-json")
    args = parser.parse_args()
    formal_mapping = None
    if args.formal_mapping_json:
        formal_mapping = json.loads(Path(args.formal_mapping_json).read_text(encoding="utf-8"))
    plan = build_commit_plan(
        args.workbook,
        permissions=set(args.permission or ()),
        expected_sha256=args.expected_sha256 or None,
        formal_mapping=formal_mapping,
    )
    payload = json.dumps(plan, ensure_ascii=False, indent=2)
    if args.output_json:
        output_path = Path(args.output_json)
        output_path.parent.mkdir(parents=True, exist_ok=True)
        output_path.write_text(payload + "\n", encoding="utf-8")
    print(payload)


if __name__ == "__main__":
    main()
