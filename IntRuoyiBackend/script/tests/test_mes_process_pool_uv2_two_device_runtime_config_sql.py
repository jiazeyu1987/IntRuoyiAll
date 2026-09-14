from pathlib import Path


BACKEND_ROOT = Path(__file__).resolve().parents[2]
MIGRATION_PATH = (
    BACKEND_ROOT
    / "sql"
    / "mysql"
    / "20260811_mes_process_pool_uv2_two_device_runtime_config.sql"
)
PREFLIGHT_PATH = (
    BACKEND_ROOT
    / "sql"
    / "mysql"
    / "target-preflight"
    / "20260811_mes_process_pool_uv2_two_device_runtime_config.preflight.sql"
)


def read_text(path: Path) -> str:
    assert path.exists(), f"SQL file must exist: {path}"
    return path.read_text(encoding="utf-8")


def compact_sql(text: str) -> str:
    return " ".join(text.split())


def test_uv2_runtime_config_migration_noops_when_target_has_no_source_binding() -> None:
    sql = read_text(MIGRATION_PATH)
    compact = compact_sql(sql)

    assert "UV curing II requires an enabled formal A05075 source binding" not in sql
    assert "v_source_binding_count" in sql
    assert "IF v_source_binding_count = 0 THEN" not in compact


def test_uv2_runtime_config_target_preflight_matches_noop_and_conflict_contract() -> None:
    sql = read_text(PREFLIGHT_PATH)
    compact = compact_sql(sql)

    assert "source_binding_count" in sql
    assert "source_binding_count FROM source_bindings) >= 0" in compact
    assert "missing_target_device_count FROM missing_target_devices) = 0" in compact
    assert "missing_source_rule_count FROM missing_source_rules) = 0" in compact
    assert "conflicting_metering_valid_count FROM conflicting_metering_valid) = 0" in compact
