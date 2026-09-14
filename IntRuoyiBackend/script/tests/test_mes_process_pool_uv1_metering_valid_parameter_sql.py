from pathlib import Path


BACKEND_ROOT = Path(__file__).resolve().parents[2]
MIGRATION_PATH = (
    BACKEND_ROOT
    / "sql"
    / "mysql"
    / "20260811_mes_process_pool_uv1_metering_valid_parameter.sql"
)
PREFLIGHT_PATH = (
    BACKEND_ROOT
    / "sql"
    / "mysql"
    / "target-preflight"
    / "20260811_mes_process_pool_uv1_metering_valid_parameter.preflight.sql"
)


def read_text(path: Path) -> str:
    assert path.exists(), f"SQL file must exist: {path}"
    return path.read_text(encoding="utf-8")


def compact_sql(text: str) -> str:
    return " ".join(text.split())


def test_uv1_metering_valid_migration_noops_when_target_has_no_source_configuration() -> None:
    sql = read_text(MIGRATION_PATH)
    compact = compact_sql(sql)

    assert "Missing enabled A05075 UV curing I source configuration" not in sql
    assert "v_source_scope_count" in sql
    assert "IF v_source_scope_count = 0 THEN" not in compact


def test_uv1_metering_valid_target_preflight_matches_noop_and_conflict_contract() -> None:
    sql = read_text(PREFLIGHT_PATH)
    compact = compact_sql(sql)

    assert "source_scope_count" in sql
    assert "source_scope_count FROM source_scopes) >= 0" in compact
    assert "legacy_metering_count FROM legacy_metering) = 0" in compact
    assert "conflicting_a05059_device_count FROM conflicting_a05059_device) = 0" in compact
    assert "disabled_a05059_binding_count FROM disabled_a05059_binding) = 0" in compact
    assert "conflicting_a05059_rule_count FROM conflicting_a05059_rule) = 0" in compact
    assert "conflicting_metering_valid_count FROM conflicting_metering_valid) = 0" in compact
