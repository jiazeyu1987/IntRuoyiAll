from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
MIGRATION = ROOT / "sql" / "mysql" / "20260718_mes_route_version_single_open_candidate.sql"


def read_migration_sql() -> str:
    assert MIGRATION.exists(), (
        "missing migration for single open route candidate: "
        "sql/mysql/20260718_mes_route_version_single_open_candidate.sql"
    )
    return MIGRATION.read_text(encoding="utf-8")


def test_single_open_candidate_migration_exists():
    read_migration_sql()


def test_single_open_candidate_migration_blocks_existing_conflicts():
    sql = read_migration_sql()
    assert "HAVING COUNT(*) > 1" in sql
    assert "SIGNAL SQLSTATE '45000'" in sql
    assert "DRAFT" in sql
    assert "PENDING_APPROVAL" in sql
    assert "READY_TO_PUBLISH" in sql


def test_single_open_candidate_unique_index_contract():
    sql = read_migration_sql()
    assert "open_candidate_route_id" in sql
    assert "GENERATED ALWAYS AS" in sql
    assert "uk_mes_route_version_open_candidate" in sql
    assert "tenant_id" in sql
    assert "route_id" in sql or "open_candidate_route_id" in sql
