import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
DRAFT_SQL = ROOT / "sql/mysql/20260705_showroom_legacy_product_code_auto_confirmable_draft.sql"


def test_showroom_legacy_product_code_auto_confirmable_draft_has_release_metadata():
    sql = DRAFT_SQL.read_text(encoding="utf-8")

    assert sql.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=; type=data; riskLevel=medium"
    )


def test_showroom_legacy_product_code_auto_confirmable_draft_is_review_only():
    sql = DRAFT_SQL.read_text(encoding="utf-8")
    executable_lines = [
        (line_number, line.strip())
        for line_number, line in enumerate(sql.splitlines(), start=1)
        if line.strip() and not line.strip().startswith("--")
    ]

    assert executable_lines == []
    assert "REVIEW ONLY: START TRANSACTION;" in sql
    assert "REVIEW ONLY: COMMIT;" in sql
    assert "must not be executed directly" in sql


def test_showroom_legacy_product_code_auto_confirmable_draft_has_confirmed_update_pairs():
    sql = DRAFT_SQL.read_text(encoding="utf-8")
    revision_update_pattern = re.compile(
        r"-- REVIEW ONLY: UPDATE showroom_product_revision r JOIN showroom_product p "
        r"ON p\.current_revision_id = r\.id SET r\.name_cn = '.+', r\.name_en = '.+' "
        r"WHERE p\.tenant_id = (1|122) AND p\.product_code = 'INT-\d+' "
        r"AND p\.deleted = 0 AND r\.deleted = 0;"
    )
    legacy_update_pattern = re.compile(
        r"-- REVIEW ONLY: UPDATE showroom_product SET legacy_product_code = 'product_\d+' "
        r"WHERE tenant_id = (1|122) AND product_code = 'INT-\d+' AND deleted = 0;"
    )
    revision_updates = [
        line.strip()
        for line in sql.splitlines()
        if line.strip().startswith("-- REVIEW ONLY: UPDATE showroom_product_revision")
    ]
    legacy_updates = [
        line.strip()
        for line in sql.splitlines()
        if line.strip().startswith("-- REVIEW ONLY: UPDATE showroom_product SET legacy_product_code")
    ]

    assert revision_updates
    assert len(revision_updates) == len(legacy_updates)
    assert all(revision_update_pattern.fullmatch(line) for line in revision_updates)
    assert all(legacy_update_pattern.fullmatch(line) for line in legacy_updates)
    assert any("tenant_id = 1" in line for line in legacy_updates)
    assert any("tenant_id = 122" in line for line in legacy_updates)
