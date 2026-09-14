from pathlib import Path


BACKEND_ROOT = Path(__file__).resolve().parents[2]
MIGRATION_PATH = (
    BACKEND_ROOT
    / "sql"
    / "mysql"
    / "20260811_mes_process_pool_cleaning_process_parameter_data.sql"
)
PREFLIGHT_PATH = (
    BACKEND_ROOT
    / "sql"
    / "mysql"
    / "target-preflight"
    / "20260811_mes_process_pool_cleaning_process_parameter_data.preflight.sql"
)


def read_text(path: Path) -> str:
    assert path.exists(), f"SQL file must exist: {path}"
    return path.read_text(encoding="utf-8")


def compact_sql(text: str) -> str:
    return " ".join(text.split())


def test_cleaning_process_parameter_migration_noops_when_target_has_no_rules() -> None:
    sql = read_text(MIGRATION_PATH)
    compact = compact_sql(sql)

    assert "No active cleaning process ultrasonic cleaner parameter rules found" not in sql
    assert "v_cleaning_rule_candidate_count" in sql
    assert "SELECT COUNT(*) INTO v_cleaning_rule_candidate_count" in compact
    assert "IF v_cleaning_rule_candidate_count > 0 AND EXISTS" in compact


def test_cleaning_process_parameter_target_preflight_accepts_empty_rule_baseline() -> None:
    sql = read_text(PREFLIGHT_PATH)
    compact = compact_sql(sql)

    assert "candidate_rule_count" in sql
    assert "candidate_rule_count = 0" in compact
    assert "duplicate_target_count = 0" in compact
    assert "incomplete_target_count = 0" in compact
