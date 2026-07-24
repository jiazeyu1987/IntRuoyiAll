from pathlib import Path
import re


REPO_ROOT = Path(__file__).resolve().parents[2]
MIGRATION_PATH = REPO_ROOT / "sql" / "mysql" / "20260528_dcc_controlled_file_protection.sql"
BASE_SCHEMA_PATH = REPO_ROOT / "sql" / "mysql" / "20260513_dcc_base_schema.sql"
H2_SCHEMA_PATH = REPO_ROOT / "yudao-module-dcc" / "src" / "test" / "resources" / "sql" / "create_tables.sql"
H2_CLEAN_PATH = REPO_ROOT / "yudao-module-dcc" / "src" / "test" / "resources" / "sql" / "clean.sql"

BASE_COLUMNS = ["tenant_id", "create_time", "update_time", "creator", "updater", "deleted"]
PROTECTION_TABLE_COLUMNS = {
    "dcc_controlled_file_access_event": [
        "access_event_code",
        "controlled_file_id",
        "file_version_no",
        "user_id",
        "access_type",
        "purpose",
        "result",
        "failure_code",
        "failure_reason",
        "source_ip",
        "user_agent",
        "request_id",
        "occurred_at",
    ],
    "dcc_controlled_file_watermark_trace": [
        "trace_code",
        "access_event_id",
        "access_event_code",
        "controlled_file_id",
        "file_number",
        "file_version_no",
        "user_id",
        "user_identifier",
        "user_display_name",
        "dept_id",
        "dept_name",
        "tenant_name",
        "privacy_mode",
        "watermark_payload_json",
        "issued_at",
        "expires_at",
    ],
    "dcc_controlled_file_upload_policy": [
        "policy_code",
        "scope_type",
        "category_id",
        "purpose",
        "max_bytes",
        "enabled",
        "priority",
        "policy_version",
        "effective_from",
        "effective_to",
        "change_reason",
    ],
    "dcc_controlled_file_temporary_file": [
        "upload_ticket",
        "session_id",
        "purpose",
        "uploader_id",
        "original_file_name",
        "content_type",
        "file_size",
        "file_sha256",
        "storage_file_id",
        "status",
        "expire_time",
        "bound_controlled_file_id",
        "bound_time",
        "cleanup_status",
        "cleanup_reason",
        "cleanup_time",
        "request_id",
    ],
    "dcc_controlled_file_download_record": [
        "download_request_id",
        "access_event_id",
        "access_event_code",
        "controlled_file_id",
        "file_version_no",
        "user_id",
        "policy_version",
        "encryption_status",
        "encryption_policy_version",
        "artifact_id",
        "cipher_file_ref",
        "plain_sha256",
        "cipher_sha256",
        "failure_code",
        "failure_reason",
        "requested_at",
        "encrypted_at",
        "returned_at",
    ],
}
ACCESS_LOG_EXTENSION_COLUMNS = [
    "access_event_id",
    "access_event_code",
    "watermark_trace_code",
    "file_version_no",
    "purpose",
    "request_id",
    "user_agent",
    "failure_code",
]
REQUIRED_INDEXES = [
    "uk_dcc_protection_access_event_code",
    "idx_dcc_protection_access_event_file",
    "idx_dcc_protection_access_event_user_time",
    "uk_dcc_protection_watermark_trace_code",
    "idx_dcc_protection_watermark_event",
    "uk_dcc_protection_upload_policy_code",
    "uk_dcc_protection_upload_policy_scope",
    "uk_dcc_protection_upload_ticket",
    "idx_dcc_protection_temp_session",
    "uk_dcc_protection_download_request",
    "idx_dcc_protection_download_event",
]


def test_runtime_migration_declares_protection_foundation_without_fallback_defaults() -> None:
    migration = MIGRATION_PATH.read_text(encoding="utf-8")

    assert_schema_is_non_destructive(migration)
    assert "CREATE PROCEDURE ensure_dcc_column" in migration
    assert "CREATE PROCEDURE ensure_dcc_index" in migration
    for table_name, columns in PROTECTION_TABLE_COLUMNS.items():
        block = find_create_block(migration, table_name)
        assert block is not None, f"Missing runtime table {table_name}"
        assert_columns(block, table_name, columns + BASE_COLUMNS)
    for column in ACCESS_LOG_EXTENSION_COLUMNS:
        assert f"'{column}'" in migration
        assert f"ADD COLUMN `{column}`" in migration
    for index_name in REQUIRED_INDEXES:
        assert f"`{index_name}`" in migration

    upload_policy = find_create_block(migration, "dcc_controlled_file_upload_policy")
    assert re.search(r"`max_bytes`\s+bigint\s+NOT\s+NULL(?!\s+DEFAULT)", upload_policy, re.IGNORECASE)
    assert "INSERT INTO `dcc_controlled_file_upload_policy`" not in migration


def test_fresh_base_schema_contains_protection_tables_and_audit_extensions() -> None:
    base_schema = BASE_SCHEMA_PATH.read_text(encoding="utf-8")

    assert_schema_is_non_destructive(base_schema)
    for table_name, columns in PROTECTION_TABLE_COLUMNS.items():
        block = find_create_block(base_schema, table_name)
        assert block is not None, f"Missing base schema table {table_name}"
        assert_columns(block, table_name, columns + BASE_COLUMNS)
    access_log_block = find_create_block(base_schema, "dcc_controlled_file_access_log")
    assert access_log_block is not None
    assert_columns(access_log_block, "dcc_controlled_file_access_log", ACCESS_LOG_EXTENSION_COLUMNS)


def test_h2_schema_and_clean_are_aligned_with_runtime_foundation() -> None:
    h2_schema = H2_SCHEMA_PATH.read_text(encoding="utf-8")
    h2_clean = H2_CLEAN_PATH.read_text(encoding="utf-8")

    for table_name, columns in PROTECTION_TABLE_COLUMNS.items():
        block = find_create_block(h2_schema, table_name)
        assert block is not None, f"Missing H2 table {table_name}"
        assert_columns(block, table_name, columns + BASE_COLUMNS)
        assert f"DELETE FROM `{table_name}`;" in h2_clean
    access_log_block = find_create_block(h2_schema, "dcc_controlled_file_access_log")
    assert access_log_block is not None
    assert_columns(access_log_block, "dcc_controlled_file_access_log", ACCESS_LOG_EXTENSION_COLUMNS)


def assert_schema_is_non_destructive(schema: str) -> None:
    assert re.search(r"\b(DROP\s+TABLE|TRUNCATE\s+TABLE)\b", schema, re.IGNORECASE) is None
    assert re.search(r"\bDELETE\s+FROM\s+`?dcc_", schema, re.IGNORECASE) is None


def assert_columns(create_block: str, table_name: str, columns: list[str]) -> None:
    for column in columns:
        assert re.search(r"`" + re.escape(column) + r"`\s+", create_block, re.IGNORECASE), (
            f"Missing column {table_name}.{column}"
        )


def find_create_block(schema: str, table_name: str) -> str | None:
    match = re.search(
        r"CREATE\s+TABLE\s+IF\s+NOT\s+EXISTS\s+`?"
        + re.escape(table_name)
        + r"`?\s*\((.*?)\)\s*(?:ENGINE|;)",
        schema,
        re.IGNORECASE | re.DOTALL,
    )
    return match.group(1) if match else None
