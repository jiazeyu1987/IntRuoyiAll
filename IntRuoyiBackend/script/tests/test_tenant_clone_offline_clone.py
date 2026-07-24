from __future__ import annotations

from pathlib import Path

from tenant_clone_fixtures import (
    JOB_CODE,
    PROFILE,
    SOURCE_TENANT_ID,
    TARGET_TENANT_ID,
    offline_clone_data_payload,
    parse_json_stdout,
    read_json,
    rows_for_tenant,
    run_tenant_clone,
    write_offline_clone_contract,
    write_offline_data_store,
    write_ready_job_store,
)


def test_create_job_is_idempotent_by_job_code_and_status_queries_same_job(tmp_path) -> None:
    contract = write_offline_clone_contract(tmp_path / "tenant-clone-contract.json")
    job_store = tmp_path / "job-store"

    first = run_tenant_clone(
        "create-job",
        "--source-tenant-id",
        str(SOURCE_TENANT_ID),
        "--target-tenant-id",
        str(TARGET_TENANT_ID),
        "--profile",
        PROFILE,
        "--mode",
        "clear_target_then_clone",
        "--contract",
        str(contract),
        "--job-code",
        JOB_CODE,
        "--job-store",
        str(job_store),
        "--offline",
    )
    second = run_tenant_clone(
        "create-job",
        "--source-tenant-id",
        str(SOURCE_TENANT_ID),
        "--target-tenant-id",
        str(TARGET_TENANT_ID),
        "--profile",
        PROFILE,
        "--mode",
        "clear_target_then_clone",
        "--contract",
        str(contract),
        "--job-code",
        JOB_CODE,
        "--job-store",
        str(job_store),
        "--offline",
    )
    status = run_tenant_clone(
        "status",
        "--job-code",
        JOB_CODE,
        "--job-store",
        str(job_store),
    )

    first_payload = parse_json_stdout(first)
    second_payload = parse_json_stdout(second)
    status_payload = parse_json_stdout(status)
    assert first.returncode == 0
    assert second.returncode == 0
    assert status.returncode == 0
    assert first_payload["success"] is True
    assert second_payload["success"] is True
    assert status_payload["success"] is True
    assert first_payload["jobCode"] == JOB_CODE
    assert second_payload["jobCode"] == JOB_CODE
    assert status_payload["jobCode"] == JOB_CODE
    assert first_payload["status"] == "READY"
    assert second_payload["status"] == "READY"
    assert status_payload["status"] == "READY"
    assert second_payload["jobId"] == first_payload["jobId"]
    assert status_payload["jobId"] == first_payload["jobId"]

    persisted_jobs = sorted(job_store.glob("*.json"))
    assert persisted_jobs == [job_store / f"{JOB_CODE}.json"]
    persisted = read_json(persisted_jobs[0])
    assert persisted["jobCode"] == JOB_CODE
    assert persisted["status"] == "READY"
    assert persisted["sourceTenantId"] == SOURCE_TENANT_ID
    assert persisted["targetTenantId"] == TARGET_TENANT_ID


def test_create_job_rejects_existing_job_code_with_different_request(tmp_path) -> None:
    contract = write_offline_clone_contract(tmp_path / "tenant-clone-contract.json")
    different_contract = write_offline_clone_contract(tmp_path / "tenant-clone-contract-v2.json")
    job_store = tmp_path / "job-store"

    created = run_tenant_clone(
        "create-job",
        "--source-tenant-id",
        str(SOURCE_TENANT_ID),
        "--target-tenant-id",
        str(TARGET_TENANT_ID),
        "--profile",
        PROFILE,
        "--mode",
        "clear_target_then_clone",
        "--contract",
        str(contract),
        "--job-code",
        JOB_CODE,
        "--job-store",
        str(job_store),
        "--offline",
    )
    created_payload = parse_json_stdout(created)
    assert created.returncode == 0
    assert created_payload["success"] is True

    persisted_path = job_store / f"{JOB_CODE}.json"
    persisted_before_conflicts = read_json(persisted_path)

    target_conflict = run_tenant_clone(
        "create-job",
        "--source-tenant-id",
        str(SOURCE_TENANT_ID),
        "--target-tenant-id",
        str(TARGET_TENANT_ID + 1),
        "--profile",
        PROFILE,
        "--mode",
        "clear_target_then_clone",
        "--contract",
        str(contract),
        "--job-code",
        JOB_CODE,
        "--job-store",
        str(job_store),
        "--offline",
    )
    target_payload = parse_json_stdout(target_conflict)
    assert target_conflict.returncode != 0
    assert target_payload["success"] is False
    assert target_payload["errorCode"] == "TENANT_CLONE_JOB_CODE_CONFLICT"
    assert target_payload["phase"] == "JOB_CREATE"
    assert target_payload["jobCode"] == JOB_CODE
    assert read_json(persisted_path) == persisted_before_conflicts

    contract_conflict = run_tenant_clone(
        "create-job",
        "--source-tenant-id",
        str(SOURCE_TENANT_ID),
        "--target-tenant-id",
        str(TARGET_TENANT_ID),
        "--profile",
        PROFILE,
        "--mode",
        "clear_target_then_clone",
        "--contract",
        str(different_contract),
        "--job-code",
        JOB_CODE,
        "--job-store",
        str(job_store),
        "--offline",
    )
    contract_payload = parse_json_stdout(contract_conflict)
    assert contract_conflict.returncode != 0
    assert contract_payload["success"] is False
    assert contract_payload["errorCode"] == "TENANT_CLONE_JOB_CODE_CONFLICT"
    assert contract_payload["phase"] == "JOB_CREATE"
    assert contract_payload["jobCode"] == JOB_CODE
    assert read_json(persisted_path) == persisted_before_conflicts


def test_execute_offline_json_store_clears_target_backs_up_and_clones_with_id_mapping(tmp_path) -> None:
    contract = write_offline_clone_contract(tmp_path / "tenant-clone-contract.json")
    data_store = write_offline_data_store(tmp_path / "offline-data-store.json")
    original_data = read_json(data_store)
    job_store = tmp_path / "job-store"
    write_ready_job_store(job_store)
    backup_dir = tmp_path / "backup"

    completed = run_tenant_clone(
        "execute",
        "--job-code",
        JOB_CODE,
        "--job-store",
        str(job_store),
        "--contract",
        str(contract),
        "--offline-data-store",
        str(data_store),
        "--backup-dir",
        str(backup_dir),
        "--confirm-clear-target",
    )

    payload = parse_json_stdout(completed)
    assert completed.returncode == 0
    assert payload["success"] is True
    assert payload["jobCode"] == JOB_CODE
    assert payload["status"] == "SUCCEEDED"
    assert payload["phase"] == "VERIFYING"
    assert payload["clearedRows"] == 2
    assert payload["clonedRows"] == 2
    assert Path(payload["backupIndexPath"]).is_file()
    assert Path(payload["idMapPath"]).is_file()

    backup_index = read_json(Path(payload["backupIndexPath"]))
    assert backup_index["jobCode"] == JOB_CODE
    assert {table["table"]: table["rows"] for table in backup_index["tables"]} == {
        "system_dept": 1,
        "system_users": 1,
    }
    backup_files = [backup_dir / table["file"] for table in backup_index["tables"]]
    assert all(path.is_file() for path in backup_files)

    updated_data = read_json(data_store)
    assert rows_for_tenant(updated_data, "system_dept", SOURCE_TENANT_ID) == rows_for_tenant(
        original_data, "system_dept", SOURCE_TENANT_ID
    )
    assert rows_for_tenant(updated_data, "system_users", SOURCE_TENANT_ID) == rows_for_tenant(
        original_data, "system_users", SOURCE_TENANT_ID
    )
    assert rows_for_tenant(updated_data, "system_dept", 999) == rows_for_tenant(
        original_data, "system_dept", 999
    )
    assert rows_for_tenant(updated_data, "system_users", 999) == rows_for_tenant(
        original_data, "system_users", 999
    )

    target_depts = rows_for_tenant(updated_data, "system_dept", TARGET_TENANT_ID)
    target_users = rows_for_tenant(updated_data, "system_users", TARGET_TENANT_ID)
    assert len(target_depts) == 1
    assert len(target_users) == 1
    assert target_depts[0]["code"] == "SRC-DEPT"
    assert target_users[0]["username"] == "source-user"
    assert target_depts[0]["id"] not in {1001, 2001}
    assert target_users[0]["id"] not in {5001, 6001}
    assert target_users[0]["dept_id"] == target_depts[0]["id"]

    id_map = read_json(Path(payload["idMapPath"]))
    assert {
        (row["table"], row["sourcePk"], row["targetPk"]) for row in id_map["mappings"]
    } == {
        ("system_dept", "1001", str(target_depts[0]["id"])),
        ("system_users", "5001", str(target_users[0]["id"])),
    }


def test_execute_offline_json_store_fails_when_child_reference_has_no_parent_id_mapping(tmp_path) -> None:
    contract = write_offline_clone_contract(tmp_path / "tenant-clone-contract.json")
    data_store = write_offline_data_store(tmp_path / "offline-data-store.json", missing_parent_reference=True)
    original_data = offline_clone_data_payload(missing_parent_reference=True)
    job_store = tmp_path / "job-store"
    write_ready_job_store(job_store)
    backup_dir = tmp_path / "backup"

    completed = run_tenant_clone(
        "execute",
        "--job-code",
        JOB_CODE,
        "--job-store",
        str(job_store),
        "--contract",
        str(contract),
        "--offline-data-store",
        str(data_store),
        "--backup-dir",
        str(backup_dir),
        "--confirm-clear-target",
    )

    payload = parse_json_stdout(completed)
    assert completed.returncode != 0
    assert payload["success"] is False
    assert payload.get("jobCode") == JOB_CODE
    assert payload["status"] == "FAILED"
    assert payload["phase"] == "REFERENCE_REWRITE"
    assert payload["errorCode"] == "TENANT_CLONE_MISSING_ID_MAPPING"
    assert payload["table"] == "system_users"
    assert payload["field"] == "dept_id"
    assert payload["missingSourcePk"] == "999999"
    assert read_json(data_store) == original_data


def test_rollback_offline_json_store_restores_backup_and_is_idempotent(tmp_path) -> None:
    contract = write_offline_clone_contract(tmp_path / "tenant-clone-contract.json")
    data_store = write_offline_data_store(tmp_path / "offline-data-store.json")
    original_data = read_json(data_store)
    job_store = tmp_path / "job-store"
    write_ready_job_store(job_store)
    backup_dir = tmp_path / "backup"

    execute_result = run_tenant_clone(
        "execute",
        "--job-code",
        JOB_CODE,
        "--job-store",
        str(job_store),
        "--contract",
        str(contract),
        "--offline-data-store",
        str(data_store),
        "--backup-dir",
        str(backup_dir),
        "--confirm-clear-target",
    )
    execute_payload = parse_json_stdout(execute_result)
    assert execute_result.returncode == 0
    assert execute_payload["status"] == "SUCCEEDED"
    assert read_json(data_store) != original_data

    first_rollback = run_tenant_clone(
        "rollback",
        "--job-code",
        JOB_CODE,
        "--job-store",
        str(job_store),
        "--offline-data-store",
        str(data_store),
        "--backup-index",
        execute_payload["backupIndexPath"],
        "--confirm-restore-target",
    )
    first_payload = parse_json_stdout(first_rollback)
    assert first_rollback.returncode == 0
    assert first_payload["success"] is True
    assert first_payload["status"] == "ROLLED_BACK"
    assert first_payload["restoredRows"] == 2
    assert read_json(data_store) == original_data

    second_rollback = run_tenant_clone(
        "rollback",
        "--job-code",
        JOB_CODE,
        "--job-store",
        str(job_store),
        "--offline-data-store",
        str(data_store),
        "--backup-index",
        execute_payload["backupIndexPath"],
        "--confirm-restore-target",
    )
    second_payload = parse_json_stdout(second_rollback)
    assert second_rollback.returncode == 0
    assert second_payload["success"] is True
    assert second_payload["status"] == "ROLLED_BACK"
    assert second_payload["restoredRows"] == 0
    assert read_json(data_store) == original_data
