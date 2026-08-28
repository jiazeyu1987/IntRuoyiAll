from pathlib import Path
import re


ROOT = Path(__file__).resolve().parents[2]
BASE_SCHEMA = ROOT / "sql" / "mysql" / "20260513_dcc_base_schema.sql"
REPAIR_SCHEMA = ROOT / "sql" / "mysql" / "20260515_dcc_runtime_schema_repair.sql"
MIGRATION = ROOT / "sql" / "mysql" / "20260618_dcc_project_code_recognition_link.sql"
TEST_SCHEMA = ROOT / "yudao-module-dcc" / "src" / "test" / "resources" / "sql" / "create_tables.sql"

PROJECT_CODE_LINK_COLUMNS = [
    "dcc_project_code_id",
    "project_code_recognition_type",
    "project_code_recognition_text",
    "project_code_recognized_by",
    "project_code_recognized_time",
]
LEDGER_MIGRATION = ROOT / "sql" / "mysql" / "20260629_dcc_controlled_file_recognition_record.sql"
CLAIM_MIGRATION = ROOT / "sql" / "mysql" / "20260629_dcc_controlled_file_recognition_claim.sql"
FILE_TYPE_LEVEL_MIGRATION = ROOT / "sql" / "mysql" / "20260702_dcc_recognition_file_type_levels.sql"
ALIAS_MAPPING_MIGRATION = ROOT / "sql" / "mysql" / "20260703_dcc_project_code_alias_mapping.sql"
TRACEABLE_FAILURE_MIGRATION = ROOT / "sql" / "mysql" / "20260706_dcc_recognition_traceable_failure_messages.sql"
STRUCTURED_FAILURE_MIGRATION = ROOT / "sql" / "mysql" / "20260710_dcc_recognition_structured_failure.sql"
EXISTING_RECORD_POLICY_MIGRATION = ROOT / "sql" / "mysql" / "20260706_dcc_batch_recognition_existing_record_policy.sql"
SIGNATURE_IMAGE_MIGRATION = ROOT / "sql" / "mysql" / "20260706_dcc_signature_image_evidence_chain.sql"
LEDGER_TABLE_COLUMNS = [
    "controlled_file_id",
    "recognition_scope",
    "recognition_method",
    "recognition_version",
    "status",
    "matched_project_code_id",
    "matched_project_alias_id",
    "matched_project_alias_text",
    "matched_project_alias_source",
    "recognized_product_code",
    "recognized_product_name",
    "match_type",
    "match_text",
    "failure_message",
    "recognized_by",
    "recognized_time",
    "source_file_id",
    "tenant_id",
]
CLAIM_TABLE_COLUMNS = [
    "controlled_file_id",
    "recognition_scope",
    "claimed_by",
    "claim_task_id",
    "claimed_at",
    "tenant_id",
]
FILE_TYPE_LEVEL_COLUMNS = [
    "file_type_level1",
    "file_type_level2",
    "file_type_level3",
    "file_type_level4",
    "file_type_level5",
]
ALIAS_MAPPING_COLUMNS = [
    "project_code_id",
    "alias_text",
    "normalized_alias_text",
    "alias_source",
    "status",
    "active",
    "tenant_id",
]
SIGNATURE_IMAGE_TABLE_COLUMNS = [
    "user_id",
    "version_no",
    "file_id",
    "file_url",
    "storage_path",
    "file_name",
    "content_type",
    "file_size",
    "sha256",
    "image_status",
    "active",
    "uploaded_by",
    "uploaded_at",
    "referenced_count",
    "tenant_id",
]
SIGNATURE_IMAGE_SNAPSHOT_COLUMNS = [
    "signature_image_id",
    "signature_image_version_no",
    "signature_image_file_id",
    "signature_image_file_url",
    "signature_image_sha256",
    "signature_image_content_type",
    "signature_image_file_size",
    "signature_image_status_snapshot",
    "signature_image_verified_status",
]


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def assert_non_destructive(sql: str) -> None:
    assert not re.search(r"\b(DROP\s+TABLE|TRUNCATE\s+TABLE)\b", sql, re.I)
    assert not re.search(r"\bDELETE\s+FROM\s+`?dcc_", sql, re.I)


def test_dcc_project_code_recognition_migration_is_idempotent_and_tenant_scoped():
    assert MIGRATION.exists(), "project-code recognition link migration must exist"
    sql = read(MIGRATION)

    assert_non_destructive(sql)
    assert "CREATE PROCEDURE ensure_dcc_column" in sql
    assert "CREATE PROCEDURE ensure_dcc_index" in sql
    for column in PROJECT_CODE_LINK_COLUMNS:
        assert re.search(
            rf"ensure_dcc_column\s*\(\s*'dcc_controlled_file'\s*,\s*'{column}'",
            sql,
            re.I | re.S,
        ), f"missing idempotent column patch for {column}"
    assert re.search(
        r"ensure_dcc_index\s*\(\s*'dcc_controlled_file'\s*,\s*'idx_dcc_controlled_file_project_code'",
        sql,
        re.I | re.S,
    )
    assert "`tenant_id`, `dcc_project_code_id`" in sql


def test_dcc_base_and_test_schema_cover_project_code_recognition_columns():
    base_schema = read(BASE_SCHEMA)
    test_schema = read(TEST_SCHEMA)
    for schema in (base_schema, test_schema):
        for column in PROJECT_CODE_LINK_COLUMNS:
            assert re.search(rf"`{column}`\s+", schema, re.I), f"missing {column}"
        assert "idx_dcc_controlled_file_project_code" in schema


def test_dcc_file_type_levels_are_in_all_runtime_schema_paths():
    migration = read(FILE_TYPE_LEVEL_MIGRATION)
    repair_schema = read(REPAIR_SCHEMA)
    base_schema = read(BASE_SCHEMA)
    test_schema = read(TEST_SCHEMA)

    assert_non_destructive(migration)
    for column in FILE_TYPE_LEVEL_COLUMNS:
        for table in ["dcc_controlled_file", "dcc_controlled_file_recognition_record"]:
            assert re.search(
                rf"ensure_dcc_recognition_file_type_column\s*\(\s*'{table}'\s*,\s*'{column}'",
                migration,
                re.I | re.S,
            ), f"missing idempotent migration patch for {table}.{column}"
        assert re.search(rf"`{column}`\s+", base_schema, re.I), f"base schema missing {column}"
        assert re.search(rf"`{column}`\s+", test_schema, re.I), f"test schema missing {column}"
        assert re.search(
            rf"ensure_dcc_column\s*\(\s*'dcc_controlled_file'\s*,\s*'{column}'",
            repair_schema,
            re.I | re.S,
        ), f"runtime repair schema missing dcc_controlled_file.{column}"
        assert re.search(
            rf"ensure_dcc_column\s*\(\s*'dcc_controlled_file_recognition_record'\s*,\s*'{column}'",
            repair_schema,
            re.I | re.S,
        ), f"runtime repair schema missing dcc_controlled_file_recognition_record.{column}"
    assert "idx_dcc_controlled_file_type_level" in migration
    assert "idx_dcc_controlled_file_type_level" in base_schema
    assert "idx_dcc_controlled_file_type_level" in test_schema


def test_dcc_runtime_repair_file_number_backfill_is_length_safe():
    repair_schema = read(REPAIR_SCHEMA)

    unsafe_title_backfill = (
        "`file_number` = COALESCE(NULLIF(`file_number`, ''), "
        "NULLIF(`title`, ''), CONCAT('DCC-FILE-', `id`))"
    )
    assert unsafe_title_backfill not in repair_schema
    assert "CHAR_LENGTH(NULLIF(`file_number`, '')) <= 64" in repair_schema
    assert "CHAR_LENGTH(NULLIF(`title`, '')) <= 64" in repair_schema
    assert "CONCAT('DCC-FILE-', `id`)" in repair_schema


def test_dcc_recognition_ledger_migration_is_idempotent_and_non_destructive():
    assert LEDGER_MIGRATION.exists(), "recognition ledger migration must exist"
    sql = read(LEDGER_MIGRATION)

    assert_non_destructive(sql)
    assert "CREATE TABLE IF NOT EXISTS `dcc_controlled_file_recognition_record`" in sql
    for column in LEDGER_TABLE_COLUMNS:
        assert re.search(rf"`{column}`\s+", sql, re.I), f"missing ledger column {column}"
    assert "uk_dcc_file_recognition_record_biz" in sql
    assert "`controlled_file_id`, `recognition_scope`, `recognition_method`, `recognition_version`" in sql


def test_dcc_project_alias_mapping_migration_is_idempotent_and_tracked_in_schemas():
    assert ALIAS_MAPPING_MIGRATION.exists(), "project alias mapping migration must exist"
    migration = read(ALIAS_MAPPING_MIGRATION)
    base_schema = read(BASE_SCHEMA)
    test_schema = read(TEST_SCHEMA)

    assert_non_destructive(migration)
    assert "CREATE TABLE IF NOT EXISTS `dcc_project_code_alias_mapping`" in migration
    assert "ensure_dcc_alias_record_column" in migration
    for column in ALIAS_MAPPING_COLUMNS:
        for schema in [migration, base_schema, test_schema]:
            assert re.search(rf"`{column}`\s+", schema, re.I), f"missing alias mapping column {column}"
    for column in ["matched_project_alias_id", "matched_project_alias_text", "matched_project_alias_source"]:
        assert re.search(rf"`{column}`\s+", migration, re.I), f"missing migration ledger alias column {column}"
        assert re.search(rf"`{column}`\s+", test_schema, re.I), f"missing test ledger alias column {column}"


def test_dcc_test_schema_and_batch_task_schema_cover_recognition_version_contract():
    test_schema = read(TEST_SCHEMA)
    batch_task_schema = read(ROOT / "sql" / "mysql" / "20260623_dcc_browser_batch_recognition_task.sql")

    assert re.search(r"`recognition_version_snapshot`\s+", batch_task_schema, re.I)
    assert re.search(r"`recognition_version_snapshot`\s+", test_schema, re.I)
    assert "dcc_controlled_file_recognition_record" in test_schema


def test_dcc_recognition_failure_message_columns_are_traceable_length():
    migration = read(TRACEABLE_FAILURE_MIGRATION)
    test_schema = read(TEST_SCHEMA)

    assert_non_destructive(migration)
    assert "MODIFY COLUMN `last_failure_message` varchar(2048)" in migration
    assert "MODIFY COLUMN `failure_message` varchar(2048)" in migration
    assert re.search(r"`last_failure_message`\s+varchar\(2048\)", test_schema, re.I)
    assert re.search(r"`failure_message`\s+varchar\(2048\)", test_schema, re.I)


def test_dcc_recognition_structured_failure_columns_are_in_all_schema_paths():
    assert STRUCTURED_FAILURE_MIGRATION.exists(), "structured failure migration must exist"
    migration = read(STRUCTURED_FAILURE_MIGRATION)
    ledger_schema = read(LEDGER_MIGRATION)
    repair_schema = read(REPAIR_SCHEMA)
    test_schema = read(TEST_SCHEMA)

    assert_non_destructive(migration)
    for column in ["failure_stage", "failure_code"]:
        assert re.search(rf"`{column}`\s+varchar\(64\)", migration, re.I), f"migration missing {column}"
        assert re.search(rf"`{column}`\s+varchar\(64\)", ledger_schema, re.I), f"ledger schema missing {column}"
        assert re.search(rf"`{column}`\s+varchar\(64\)", repair_schema, re.I), f"repair schema missing {column}"
        assert re.search(rf"`{column}`\s+varchar\(64\)", test_schema, re.I), f"test schema missing {column}"
    assert "INFORMATION_SCHEMA.COLUMNS" in migration


def test_dcc_batch_recognition_existing_record_policy_is_in_schema_paths():
    migration = read(EXISTING_RECORD_POLICY_MIGRATION)
    test_schema = read(TEST_SCHEMA)

    assert_non_destructive(migration)
    assert "INFORMATION_SCHEMA.COLUMNS" in migration
    assert re.search(r"`existing_record_policy`\s+varchar\(32\)\s+NOT NULL", migration, re.I)
    assert re.search(r"`existing_record_policy`\s+varchar\(32\)\s+NOT NULL", test_schema, re.I)
    assert "'RETRY_FAILED'" in migration
    assert "'OVERWRITE_ALL'" in migration


def test_dcc_signature_image_evidence_chain_is_in_all_schema_paths():
    assert SIGNATURE_IMAGE_MIGRATION.exists(), "signature image migration must exist"
    migration = read(SIGNATURE_IMAGE_MIGRATION)
    base_schema = read(BASE_SCHEMA)
    test_schema = read(TEST_SCHEMA)

    assert_non_destructive(migration)
    assert "CREATE TABLE IF NOT EXISTS `dcc_electronic_signature_image`" in migration
    assert "CREATE PROCEDURE ensure_dcc_column" in migration
    assert "DROP PROCEDURE IF EXISTS ensure_dcc_column" in migration
    for column in SIGNATURE_IMAGE_TABLE_COLUMNS:
        for schema in [migration, base_schema, test_schema]:
            assert re.search(
                rf"`{column}`\s+",
                schema,
                re.I,
            ), f"missing signature image column {column}"

    for column in SIGNATURE_IMAGE_SNAPSHOT_COLUMNS:
        assert re.search(
            rf"ensure_dcc_column\s*\(\s*'dcc_controlled_file_signature'\s*,\s*'{column}'",
            migration,
            re.I | re.S,
        ), f"missing idempotent signature snapshot migration patch for {column}"
        for schema in [base_schema, test_schema]:
            assert re.search(
                rf"`{column}`\s+",
                schema,
                re.I,
            ), f"schema missing signature snapshot column {column}"

    assert "uk_dcc_signature_image_user_version" in migration
    assert "idx_dcc_signature_image_user_active" in migration
    assert "`tenant_id`, `user_id`, `version_no`, `deleted`" in migration
    assert re.search(r"`sha256`\s+varchar\(128\)\s+NOT NULL", migration, re.I)
    assert re.search(r"`signature_image_sha256`\s+varchar\(128\)", base_schema, re.I)


def test_dcc_recognition_claim_migration_is_idempotent_and_non_destructive():
    assert CLAIM_MIGRATION.exists(), "recognition claim migration must exist"
    sql = read(CLAIM_MIGRATION)

    assert_non_destructive(sql)
    assert "CREATE TABLE IF NOT EXISTS `dcc_controlled_file_recognition_claim`" in sql
    for column in CLAIM_TABLE_COLUMNS:
        assert re.search(rf"`{column}`\s+", sql, re.I), f"missing claim column {column}"
    assert "uk_dcc_file_recognition_claim_scope" in sql
    assert "`controlled_file_id`, `recognition_scope`" in sql
