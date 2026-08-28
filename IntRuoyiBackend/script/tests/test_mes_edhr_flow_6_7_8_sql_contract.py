import re
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


def read_sql(name: str) -> str:
    path = REPO_ROOT / "sql" / "mysql" / name
    assert path.exists(), f"missing migration: {name}"
    return path.read_text(encoding="utf-8")


def compact(sql: str) -> str:
    return re.sub(r"\s+", " ", sql).strip().lower()


def test_flow6_batch_provisioning_record_schema_contract() -> None:
    sql = read_sql("20260826_mes_edhr_batch_provisioning_record.sql")
    flat = compact(sql)

    assert sql.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260822_mes_edhr_batch_traceability; type=schema; riskLevel=medium"
    )
    assert "add column provisioning_status varchar(48)" in flat
    assert "create table if not exists mes_pro_edhr_batch_provisioning_record (" in flat
    for required in [
        "batch_execution_id bigint not null",
        "entry_type varchar(48) not null",
        "entry_business_id varchar(180) not null",
        "source_credential_id varchar(128) not null",
        "source_snapshot_hash char(64) not null",
        "source_bundle_hash char(64) not null",
        "idempotency_key varchar(180) not null",
        "status varchar(48) not null",
        "mapping_event_id varchar(180) default null",
        "mapping_idempotency_key varchar(180) default null",
    ]:
        assert required in flat
    assert "unique key uk_mes_edhr_batch_provisioning_batch" in flat
    assert "unique key uk_mes_edhr_batch_provisioning_idempotency" in flat


def test_flow7_source_credential_and_transaction_are_string_contracts() -> None:
    credential_sql = read_sql("20260826_mes_edhr_batch_trace_source_credential.sql")
    transaction_sql = read_sql("20260826_mes_edhr_batch_trace_transaction_id.sql")

    assert "source_credential_id varchar(128) default null" in compact(credential_sql)
    assert "completion_transaction_id varchar(180) default null" in compact(transaction_sql)
    assert "modify column source_credential_id" in compact(credential_sql)
    assert "modify column completion_transaction_id" in compact(transaction_sql)
    assert "dependsOn=20260822_mes_edhr_batch_traceability" in credential_sql.splitlines()[0]
    assert "dependsOn=20260822_mes_edhr_batch_traceability" in transaction_sql.splitlines()[0]


def test_flow8_material_gate_receipt_schema_contract() -> None:
    sql = read_sql("20260826_mes_edhr_material_gate_receipt.sql")
    flat = compact(sql)

    assert sql.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260822_mes_edhr_release_final_state_trace; type=schema; riskLevel=medium"
    )
    assert "create table if not exists mes_pro_edhr_material_gate_receipt (" in flat
    for required in [
        "receipt_id varchar(128) not null",
        "batch_execution_id bigint not null",
        "gate_status varchar(32) not null",
        "material_type_keys_json varchar(512) not null",
        "manifest_hash char(64) not null",
        "source_snapshot_hash char(64) not null",
        "material_version_set_hash char(64) not null",
        "receipt_hash char(64) not null",
        "issued_by bigint not null",
        "audit_event_id varchar(128) not null",
        "version int not null",
    ]:
        assert required in flat
    assert "unique key uk_mes_edhr_material_gate_receipt_id" in flat
    assert "unique key uk_mes_edhr_material_gate_receipt_version" in flat


def test_flow8_material_task_source_witness_fails_fast() -> None:
    sql = read_sql("20260826_mes_edhr_material_task_source_witness.sql")
    flat = compact(sql)

    assert sql.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260707_mes_batch_record_extra_form_slots,20260822_mes_edhr_batch_traceability; "
        "type=schema; riskLevel=medium"
    )
    assert "signal sqlstate '45000'" in flat
    assert "missing mes_pro_edhr_batch_execution_task" in flat
    assert "add column material_source_snapshot_hash char(64) default null" in flat
    assert "after route_binding_snapshot_hash" in flat


def test_flow_6_7_8_migrations_are_idempotent_and_non_destructive() -> None:
    for name in [
        "20260826_mes_edhr_batch_provisioning_record.sql",
        "20260826_mes_edhr_batch_trace_source_credential.sql",
        "20260826_mes_edhr_batch_trace_transaction_id.sql",
        "20260826_mes_edhr_material_gate_receipt.sql",
        "20260826_mes_edhr_material_task_source_witness.sql",
    ]:
        upper_sql = read_sql(name).upper()
        assert (
            "CREATE TABLE IF NOT EXISTS" in upper_sql
            or "IF NOT EXISTS" in upper_sql
            or "IF EXISTS" in upper_sql
        )
        for forbidden in [r"\bDROP\s+TABLE\b", r"\bTRUNCATE\s+TABLE\b", r"\bDELETE\s+FROM\b", r"\bUPDATE\s+`?[A-Z0-9_]+`?\s+SET\b"]:
            assert re.search(forbidden, upper_sql) is None
