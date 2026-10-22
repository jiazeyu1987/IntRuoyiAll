import re
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


def read_text(relative_path: str) -> str:
    path = REPO_ROOT / relative_path
    assert path.exists(), f"missing file: {relative_path}"
    return path.read_text(encoding="utf-8")


def compact(sql: str) -> str:
    return re.sub(r"\s+", " ", sql).strip().lower()


def test_traceability_capacity_migration_widens_status_and_failure_reason() -> None:
    sql = read_text("sql/mysql/20260917_mes_edhr_batch_traceability_capacity.sql")
    flat = compact(sql)

    assert sql.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260822_mes_edhr_batch_traceability; type=schema; riskLevel=medium"
    )
    assert "modify column batch_provision_status varchar(32) not null" in flat
    assert "modify column reason longtext default null" in flat
    assert "information_schema.columns" in flat
    assert "signal sqlstate '45000'" in flat
    assert "missing batch_provision_status" in flat
    assert "missing reason" in flat
    assert "drop table" not in flat
    assert "truncate table" not in flat
    assert re.search(r"\bdelete\s+from\b", flat) is None
    assert re.search(r"\bupdate\s+[a-z0-9_`]+\s+set\b", flat) is None


def test_traceability_base_and_h2_schema_match_capacity_contract() -> None:
    base_sql = compact(read_text("sql/mysql/20260822_mes_edhr_batch_traceability.sql"))
    h2_sql = compact(read_text("yudao-module-mes/src/test/resources/sql/create_tables.sql"))

    assert "batch_provision_status varchar(32) not null" in base_sql
    assert "reason longtext default null" in base_sql
    assert '"batch_provision_status" varchar(32) not null' in h2_sql
    assert '"reason" clob default null' in h2_sql


def test_local_backend_restart_probes_and_applies_capacity_migration() -> None:
    restart_script = compact(read_text("script/deploy/restart-int-ruoyi-local.ps1"))

    assert "20260917_mes_edhr_batch_traceability_capacity.sql" in restart_script
    assert "table_name = 'mes_pro_edhr_batch_execution_origin'" in restart_script
    assert "character_maximum_length >= 32" in restart_script
    assert "table_name = 'mes_pro_edhr_batch_trace_outbox_event'" in restart_script
    assert "data_type = 'longtext'" in restart_script
