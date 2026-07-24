from pathlib import Path
import re


BASE_SCHEMA_PATH = Path(__file__).resolve().parents[2] / "sql" / "mysql" / "20260512_mes_schema.sql"


def test_mes_base_schema_contains_edhr_flow_tables():
    schema = BASE_SCHEMA_PATH.read_text(encoding="utf-8")

    for table_name in ["mes_pro_edhr_flow_event", "mes_pro_edhr_flow_intervention"]:
        block = find_create_block(schema, table_name)
        assert block is not None, f"Missing MES base schema table {table_name}"

    flow_event_block = find_create_block(schema, "mes_pro_edhr_flow_event")
    for column in [
        "business_object_type",
        "business_object_id",
        "business_object_code",
        "intervention_id",
        "flow_instance_id",
        "task_id",
        "node_key",
        "event_type",
        "from_status",
        "to_status",
        "actor_user_id",
        "target_user_id",
        "permission_code",
        "permission_decision",
        "reason",
        "signoff_evidence_hash",
        "integrity_check_result",
        "integrity_check_snapshot_json",
        "event_snapshot_json",
        "evidence_hash",
        "occurred_at",
        "tenant_id",
        "create_time",
        "update_time",
        "creator",
        "updater",
        "deleted",
    ]:
        assert re.search(r"`" + re.escape(column) + r"`\s+", flow_event_block, re.IGNORECASE)

    flow_intervention_block = find_create_block(schema, "mes_pro_edhr_flow_intervention")
    for column in [
        "intervention_code",
        "business_object_type",
        "business_object_id",
        "business_object_code",
        "flow_instance_id",
        "intervention_action",
        "intervention_status",
        "from_status",
        "to_status",
        "source_task_id",
        "target_task_id",
        "node_key",
        "target_user_id",
        "requested_by",
        "requested_at",
        "reason_category",
        "reason",
        "authorization_basis",
        "signoff_evidence_hash",
        "idempotency_key",
        "integrity_check_result",
        "integrity_check_snapshot_json",
        "evidence_hash",
        "tenant_id",
        "create_time",
        "update_time",
        "creator",
        "updater",
        "deleted",
    ]:
        assert re.search(r"`" + re.escape(column) + r"`\s+", flow_intervention_block, re.IGNORECASE)

    assert "idx_mes_pro_edhr_flow_event_object" in schema
    assert "idx_mes_pro_edhr_flow_event_instance" in schema
    assert "uk_mes_pro_edhr_flow_intervention_idempotency" in schema
    assert "idx_mes_pro_edhr_flow_intervention_object" in schema


def find_create_block(schema: str, table_name: str) -> str | None:
    match = re.search(
        r"CREATE\s+TABLE\s+IF\s+NOT\s+EXISTS\s+`?"
        + re.escape(table_name)
        + r"`?\s*\((.*?)\)\s*(?:ENGINE|;)",
        schema,
        re.IGNORECASE | re.DOTALL,
    )
    return match.group(1) if match else None
