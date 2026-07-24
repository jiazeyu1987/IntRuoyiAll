from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
MIGRATION = REPO_ROOT / "sql" / "mysql" / "20260614_mes_edhr_work_task_candidate_pool.sql"


def test_candidate_pool_migration_adds_required_columns_idempotently() -> None:
    sql = MIGRATION.read_text(encoding="utf-8")

    assert "CREATE PROCEDURE ensure_mes_edhr_candidate_pool_column" in sql
    assert sql.count("CALL ensure_mes_edhr_candidate_pool_column(") == 5
    assert "`mes_pro_edhr_work_task_assignment_rule`" in sql
    assert "`mes_pro_edhr_work_task`" in sql

    for column in [
        "`candidate_source_type`",
        "`candidate_source_id`",
        "`candidate_user_snapshot`",
    ]:
        assert column in sql


def test_candidate_pool_migration_backfills_existing_user_assignments() -> None:
    sql = MIGRATION.read_text(encoding="utf-8")

    assert "SET `candidate_source_type` = 'USER'," in sql
    assert "`candidate_source_id` = `assignee_user_id`" in sql
    assert "`candidate_user_snapshot` = CAST(`assignee_user_id` AS CHAR)" in sql
    assert "DROP PROCEDURE IF EXISTS ensure_mes_edhr_candidate_pool_column;" in sql
