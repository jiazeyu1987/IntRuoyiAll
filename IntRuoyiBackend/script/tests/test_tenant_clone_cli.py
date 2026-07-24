from __future__ import annotations

from tenant_clone_fixtures import (
    JOB_CODE,
    PROFILE,
    SOURCE_TENANT_ID,
    TARGET_TENANT_ID,
    parse_json_stdout,
    run_tenant_clone,
    write_schema_inventory_with_global_unique_index,
    write_target_counts,
    write_valid_contract,
)


def test_precheck_cli_returns_json_error_code_for_schema_unique_index_violation(tmp_path) -> None:
    contract = write_valid_contract(tmp_path / "tenant-clone-contract.json")
    schema_inventory = write_schema_inventory_with_global_unique_index(tmp_path / "schema-inventory.json")

    completed = run_tenant_clone(
        "precheck",
        "--source-tenant-id",
        str(SOURCE_TENANT_ID),
        "--target-tenant-id",
        str(TARGET_TENANT_ID),
        "--profile",
        PROFILE,
        "--contract",
        str(contract),
        "--schema-inventory",
        str(schema_inventory),
        "--offline",
    )

    payload = parse_json_stdout(completed)
    assert completed.returncode != 0
    assert payload["success"] is False
    assert payload["status"] == "PRECHECK_FAILED"
    assert payload["phase"] == "SCHEMA_CHECK"
    assert payload["errorCode"] == "TENANT_CLONE_SCHEMA_UNIQUE_INDEX_NOT_TENANT_SCOPED"
    assert payload["jobCode"] in (None, JOB_CODE)
    assert "dcc_file_category" in payload["message"]
    assert payload["counts"]["sourceTables"] == 2


def test_execute_cli_rejects_target_clear_without_confirm_flag(tmp_path) -> None:
    job_store = tmp_path / "jobs"
    job_store.mkdir()
    target_counts = write_target_counts(tmp_path / "target-counts.json", rows=2)

    completed = run_tenant_clone(
        "execute",
        "--job-code",
        JOB_CODE,
        "--job-store",
        str(job_store),
        "--target-counts",
        str(target_counts),
    )

    payload = parse_json_stdout(completed)
    assert completed.returncode != 0
    assert payload["success"] is False
    assert payload["jobCode"] == JOB_CODE
    assert payload["status"] in {"READY", "FAILED"}
    assert payload["phase"] == "CLEAR_TARGET_CONFIRMATION"
    assert payload["errorCode"] == "TENANT_CLONE_TARGET_NOT_EMPTY_CONFIRM_REQUIRED"
    assert "--confirm-clear-target" in payload["message"]


def test_cli_rejects_same_source_and_target_tenant_before_schema_checks(tmp_path) -> None:
    contract = write_valid_contract(tmp_path / "tenant-clone-contract.json")

    completed = run_tenant_clone(
        "precheck",
        "--source-tenant-id",
        str(SOURCE_TENANT_ID),
        "--target-tenant-id",
        str(SOURCE_TENANT_ID),
        "--profile",
        PROFILE,
        "--contract",
        str(contract),
        "--offline",
    )

    payload = parse_json_stdout(completed)
    assert completed.returncode != 0
    assert payload["success"] is False
    assert payload["phase"] == "TENANT_VALIDATE"
    assert payload["errorCode"] == "TENANT_CLONE_SAME_TENANT"


def test_schema_ddl_success_writes_json_stdout_without_runtime_warning_stderr() -> None:
    completed = run_tenant_clone("schema-ddl", "--name", "tenant-clone-job")

    payload = parse_json_stdout(completed)
    assert completed.returncode == 0
    assert completed.stderr == ""
    assert payload["success"] is True
    assert "CREATE TABLE infra_tenant_clone_job" in payload["ddl"]
