from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


def test_signature_governance_menu_seed_is_fail_closed_and_grants_all_permissions() -> None:
    sql_path = REPO_ROOT / "sql" / "mysql" / "20260528_signature_governance_menu.sql"
    text = sql_path.read_text(encoding="utf-8")

    required_snippets = [
        "SIGNAL SQLSTATE '45000'",
        "Missing prerequisite menu 6815 dcc:controlled-file:signature:manage",
        "signature-governance:policy:query",
        "signature-governance:policy:manage",
        "signature-governance:retention:query",
        "signature-governance:retention:manage",
        "signature-governance:periodic-review:query",
        "signature-governance:periodic-review:manage",
        "signature-governance:csv-package:query",
        "signature-governance:csv-package:manage",
        "FROM `system_role_menu` src",
        "src.`menu_id` = @signature_governance_parent_menu_id",
        "existing.`tenant_id` = src.`tenant_id`",
        "NOT EXISTS",
    ]

    for snippet in required_snippets:
        assert snippet in text


def test_signature_governance_menu_seed_does_not_grant_global_tenant_defaults() -> None:
    sql_path = REPO_ROOT / "sql" / "mysql" / "20260528_signature_governance_menu.sql"
    text = sql_path.read_text(encoding="utf-8")

    assert "VALUES (900210" not in text
    assert "tenant_id`) VALUES" not in text
