from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


def test_mes_edhr_batch_signature_time_mysql_schema_contains_time_contract() -> None:
    schema_path = REPO_ROOT / "sql" / "mysql" / "20260608_edhr_batch_execution_schema.sql"
    schema = schema_path.read_text(encoding="utf-8")

    assert "`selected_signed_at` datetime DEFAULT NULL" in schema
    assert "`signature_display_at` datetime DEFAULT NULL" in schema
    assert "`signature_time_mode` varchar(32) NOT NULL DEFAULT 'SERVER_TIME'" in schema
    assert "`selected_time_zone` varchar(64) DEFAULT NULL" in schema
    assert "`selected_time_reason` varchar(500) DEFAULT NULL" in schema
    assert "`selected_time_policy_version` varchar(64) DEFAULT NULL" in schema
    assert "`selected_time_audit_hash` char(64) DEFAULT NULL" in schema


def test_mes_edhr_batch_signature_time_tail_migration_backfills_display_time() -> None:
    migration_path = REPO_ROOT / "sql" / "mysql" / "20260615_mes_edhr_tail_four_goals.sql"
    migration = migration_path.read_text(encoding="utf-8")

    assert "CALL ensure_mes_edhr_tail_goal_column(" in migration
    for table in (
        "mes_pro_batch_record_execution_signature",
        "mes_pro_edhr_batch_execution_signature",
    ):
        assert f"'{table}',\n  'selected_signed_at'," in migration
        assert f"'{table}',\n  'signature_display_at'," in migration
        assert f"'{table}',\n  'signature_time_mode'," in migration
        assert f"UPDATE `{table}`\nSET `signature_display_at` = `signed_at`" in migration
    assert "`signature_time_mode` = 'SERVER_TIME'" in migration
    assert "WHERE `signature_display_at` IS NULL" in migration


def test_mes_edhr_batch_signature_time_test_schema_matches_mysql_contract() -> None:
    schema_path = REPO_ROOT / "yudao-module-mes" / "src" / "test" / "resources" / "sql" / "create_tables.sql"
    schema = schema_path.read_text(encoding="utf-8")

    assert '"selected_signed_at" timestamp DEFAULT NULL' in schema
    assert '"signature_display_at" timestamp DEFAULT NULL' in schema
    assert '"signature_time_mode" varchar(32) NOT NULL DEFAULT \'SERVER_TIME\'' in schema
    assert '"selected_time_zone" varchar(64) DEFAULT NULL' in schema
    assert '"selected_time_reason" varchar(500) DEFAULT NULL' in schema
    assert '"selected_time_policy_version" varchar(64) DEFAULT NULL' in schema
    assert '"selected_time_audit_hash" char(64) DEFAULT NULL' in schema
