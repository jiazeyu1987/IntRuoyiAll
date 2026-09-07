from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql" / "mysql" / "20260907_dcc_publication_followup.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing DCC publication follow-up migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_schema_has_release_metadata_and_no_historical_dml() -> None:
    sql = read_sql()
    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260903_dcc_controlled_file_related_file,20260906_dcc_new_file_lifecycle_p4; "
        "type=schema; riskLevel=medium"
    )
    lowered = sql.lower()
    assert "update `dcc_controlled_file`" not in lowered
    assert "insert into `dcc_controlled_file`" not in lowered
    assert "delete from `dcc_controlled_file`" not in lowered


def test_schema_uses_structured_detail_tables_and_business_unique_keys() -> None:
    sql = read_sql()
    for table in (
        "dcc_publication_followup_batch",
        "dcc_publication_visibility_rule_snapshot",
        "dcc_publication_visibility_user_snapshot",
        "dcc_publication_notification_candidate",
        "dcc_publication_notification_candidate_reason",
        "dcc_publication_relation_snapshot",
        "dcc_publication_relation_direction_snapshot",
    ):
        assert f"CREATE TABLE IF NOT EXISTS `{table}`" in sql
    assert "UNIQUE KEY `uk_dcc_pub_followup_file` (`tenant_id`, `published_controlled_file_id`, `deleted`)" in sql
    assert "UNIQUE KEY `uk_dcc_pub_candidate_user` (`tenant_id`, `batch_id`, `user_id`, `deleted`)" in sql
    assert "UNIQUE KEY `uk_dcc_pub_relation_master` (`tenant_id`, `batch_id`, `related_master_id`, `deleted`)" in sql
    assert "user_ids_json" not in sql.lower()
    assert "recipient_ids_json" not in sql.lower()


def test_schema_keeps_visibility_context_and_relation_direction_facts() -> None:
    sql = read_sql()
    for column in (
        "`dcc_project_code_id` BIGINT NULL",
        "`category_id` BIGINT NOT NULL",
        "`directory_id` BIGINT NULL",
        "`file_type_taxonomy_leaf_id` BIGINT NULL",
        "`source_type` VARCHAR(48) NOT NULL",
        "`source_rule_id` BIGINT NOT NULL",
        "`rule_snapshot_id` BIGINT NOT NULL",
        "`related_master_id` BIGINT NOT NULL",
        "`direction` VARCHAR(16) NOT NULL",
        "`source_relation_id` BIGINT NOT NULL",
    ):
        assert column in sql
