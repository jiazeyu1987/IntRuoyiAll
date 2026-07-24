from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


def test_runtime_control_notify_seed_is_insert_only() -> None:
    sql_path = REPO_ROOT / "sql" / "mysql" / "20260527_infra_runtime_control_notify_template_seed.sql"
    text = sql_path.read_text(encoding="utf-8")
    normalized = text.upper()

    assert "RUNTIME_OPS_ALERT" in text
    assert "WHERE NOT EXISTS" in normalized
    assert "WHERE `code` = 'RUNTIME_OPS_ALERT'" in text
    assert "UPDATE `system_notify_template`" not in text
    assert "ON DUPLICATE KEY UPDATE" not in normalized


def test_runtime_control_notify_seed_exposes_required_template_params() -> None:
    sql_path = REPO_ROOT / "sql" / "mysql" / "20260527_infra_runtime_control_notify_template_seed.sql"
    text = sql_path.read_text(encoding="utf-8")

    for param in ["environment", "action", "severity", "title", "content"]:
        assert param in text
