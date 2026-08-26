import sys
from pathlib import Path

import pytest

SCRIPT_ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(SCRIPT_ROOT))

from registration_certificate_master_data_sync import (  # noqa: E402
    MasterDataSyncError,
    build_master_data_plan,
    stable_code,
)


def preflight(rows):
    return {"source_sha256": "D42162DC354E8976CED450FA8A2BB00A2AB6099EDDF19AB907FEC3366EF94FF4", "rows": rows}


def row(source_row, owner="甲公司", product="产品A", project_code=None, remark=None):
    return {"source_row": source_row, "normalized": {
        "owner_company_name": owner, "product_name": product, "project_code": project_code,
        "remark": remark, "certificate_no": f"CERT-{source_row}", "first_obtained_date": "2020-01-01",
        "effective_date": "2020-01-01", "expiry_date": "2030-01-01", "classification": "III",
    }}


def test_stable_codes_are_deterministic_and_source_bound():
    assert stable_code("ENT", "甲公司") == stable_code("ENT", "甲公司")
    assert stable_code("PROD", "甲产品") != stable_code("PROD", "乙产品")
    assert stable_code("PROJ", "AC") != stable_code("PROJ", "QDJ")


def test_missing_and_ambiguous_products_get_canonical_create_actions():
    plan = build_master_data_plan(
        preflight([row(2), row(3, product="重复产品")]),
        {"enterprises": [], "products": [
            {"id": 10, "name_cn": "重复产品", "status": "ENABLE", "tenant_id": 1, "deleted": 0,
             "product_code": "LEGACY-A"},
            {"id": 11, "name_cn": "重复产品", "status": "ENABLE", "tenant_id": 1, "deleted": 0,
             "product_code": "LEGACY-B"},
        ], "projects": []},
    )
    actions = {x["name_cn"]: x for x in plan["product_actions"]}
    assert actions["产品A"]["action"] == "CREATE"
    assert actions["重复产品"]["action"] == "CREATE"
    assert actions["重复产品"]["product_code"].startswith("HIST-REG-PROD-")


def test_mysql_zero_bit_deleted_value_reuses_existing_product():
    plan = build_master_data_plan(
        preflight([row(2, product="已有产品")]),
        {"enterprises": [], "products": [
            {"id": 10, "name_cn": "已有产品", "status": "ENABLE", "tenant_id": 1,
             "deleted": b"\x00", "product_code": "PMD-10"},
        ], "projects": []},
    )
    assert plan["product_actions"] == [{
        "action": "REUSE", "id": 10, "name_cn": "已有产品", "product_code": "PMD-10",
    }]


def test_existing_canonical_product_code_wins_over_legacy_name_duplicates():
    canonical = stable_code("PROD", "重复产品")
    plan = build_master_data_plan(
        preflight([row(2, product="重复产品")]),
        {"enterprises": [], "products": [
            {"id": 10, "name_cn": "重复产品", "status": "ENABLE", "tenant_id": 1,
             "deleted": 0, "product_code": "LEGACY-A"},
            {"id": 11, "name_cn": "重复产品", "status": "ENABLE", "tenant_id": 1,
             "deleted": 0, "product_code": canonical},
        ], "projects": []},
    )
    assert plan["product_actions"] == [{
        "action": "REUSE", "id": 11, "name_cn": "重复产品", "product_code": canonical,
    }]


def test_duplicate_project_code_is_blocked_and_never_deleted():
    plan = build_master_data_plan(
        preflight([row(2, project_code="AC")]),
        {"enterprises": [], "products": [], "projects": [
            {"id": 20, "project_code": "AC", "status": "ENABLE", "tenant_id": 1, "deleted": 0},
            {"id": 21, "project_code": "AC", "status": "ENABLE", "tenant_id": 1, "deleted": 0},
        ]},
    )
    assert plan["status"] == "READY_WITH_BLOCKED_ROWS"
    assert plan["project_actions"] == []
    assert plan["project_conflicts"] == [{
        "project_code": "AC", "candidate_ids": [20, 21],
        "reason": "MULTIPLE_ENABLED_TENANT_PROJECT_CODES",
    }]
    assert plan["row_mappings"][0]["blockers"] == ["PROJECT_CODE_MAPPING_CONFLICT"]


def test_source_hash_mismatch_fails_closed():
    with pytest.raises(MasterDataSyncError):
        build_master_data_plan({"source_sha256": "wrong", "rows": []}, {"enterprises": [], "products": [], "projects": []})
