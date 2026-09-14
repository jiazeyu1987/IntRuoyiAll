from pathlib import Path


BACKEND_ROOT = Path(__file__).resolve().parents[2]
SQL_ROOT = BACKEND_ROOT / "sql" / "mysql"
PREFLIGHT_ROOT = SQL_ROOT / "target-preflight"

MIGRATION_IDS = (
    "20260812_mes_process_pool_b04091_cleaning_temperature_label",
    "20260812_mes_process_pool_b09353_cleaning_temperature_label",
)


def read_text(path: Path) -> str:
    assert path.exists(), f"SQL file must exist: {path}"
    return path.read_text(encoding="utf-8")


def compact_sql(text: str) -> str:
    return " ".join(text.split())


def test_cleaning_temperature_label_migrations_noop_when_target_has_no_active_rule() -> None:
    for migration_id in MIGRATION_IDS:
        sql = read_text(SQL_ROOT / f"{migration_id}.sql")
        compact = compact_sql(sql)
        suffix = migration_id.removeprefix("20260812_mes_process_pool_")

        assert "No active" not in sql
        assert f"v_{suffix}_candidate_count" in sql
        assert f"SELECT COUNT(*) INTO v_{suffix}_candidate_count" in compact
        assert "No active" not in sql


def test_cleaning_temperature_label_target_preflights_accept_empty_rule_baseline() -> None:
    for migration_id in MIGRATION_IDS:
        sql = read_text(PREFLIGHT_ROOT / f"{migration_id}.preflight.sql")
        compact = compact_sql(sql)

        assert "candidate_rule_count" in sql
        assert "process_count" not in sql
        assert "(SELECT candidate_rule_count FROM candidate_rules) >= 0" in compact
