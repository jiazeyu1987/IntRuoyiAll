from __future__ import annotations

from tenant_clone_fixtures import parse_json_stdout, run_tenant_clone, write_schema_inventory_with_global_unique_index


def test_schema_precheck_reports_non_primary_unique_index_without_tenant_id(tmp_path) -> None:
    schema_inventory = write_schema_inventory_with_global_unique_index(tmp_path / "schema-inventory.json")

    completed = run_tenant_clone(
        "check-schema",
        "--schema-inventory",
        str(schema_inventory),
        "--tenant-field",
        "tenant_id",
    )

    payload = parse_json_stdout(completed)
    assert completed.returncode != 0
    assert payload["success"] is False
    assert payload["phase"] == "SCHEMA_CHECK"
    assert payload["errorCode"] == "TENANT_CLONE_SCHEMA_UNIQUE_INDEX_NOT_TENANT_SCOPED"
    assert payload["violations"] == [
        {
            "table": "dcc_file_category",
            "index": "uk_dcc_file_category_code",
            "columns": ["code"],
            "requiredTenantField": "tenant_id",
        }
    ]


def test_job_and_id_map_ddl_contains_auditable_required_columns_and_unique_keys() -> None:
    completed = run_tenant_clone("schema-ddl", "--name", "tenant-clone-job")

    payload = parse_json_stdout(completed)
    assert completed.returncode == 0
    assert payload["success"] is True
    ddl = payload["ddl"]
    assert "CREATE TABLE infra_tenant_clone_job" in ddl
    assert "job_code VARCHAR(64) NOT NULL" in ddl
    assert "source_tenant_id BIGINT NOT NULL" in ddl
    assert "target_tenant_id BIGINT NOT NULL" in ddl
    assert "request_payload JSON NOT NULL" in ddl
    assert "precheck_report JSON NULL" in ddl
    assert "error_code VARCHAR(64) NULL" in ddl
    assert "UNIQUE KEY uk_tenant_clone_job_code (job_code)" in ddl
    assert "KEY idx_tenant_clone_job_target (target_tenant_id, status)" in ddl
    assert "CREATE TABLE infra_tenant_clone_id_map" in ddl
    assert "job_id BIGINT NOT NULL" in ddl
    assert "table_name VARCHAR(128) NOT NULL" in ddl
    assert "source_pk VARCHAR(128) NOT NULL" in ddl
    assert "target_pk VARCHAR(128) NOT NULL" in ddl
    assert "UNIQUE KEY uk_clone_id_map (job_id, table_name, source_pk)" in ddl
    assert "UNIQUE KEY uk_clone_target_id_map (job_id, table_name, target_pk)" in ddl
    for status in [
        "CREATED",
        "PRECHECKING",
        "PRECHECK_FAILED",
        "READY",
        "LOCKED",
        "BACKING_UP",
        "CLEARING_TARGET",
        "CLONING",
        "VERIFYING",
        "SUCCEEDED",
        "FAILED",
        "ROLLING_BACK",
        "ROLLED_BACK",
    ]:
        assert status in payload["allowedStatuses"]
