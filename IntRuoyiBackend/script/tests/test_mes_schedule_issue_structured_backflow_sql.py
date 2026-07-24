from pathlib import Path


SQL_PATH = (
    Path(__file__).resolve().parents[2]
    / "sql"
    / "mysql"
    / "20260624_mes_schedule_issue_structured_backflow.sql"
)


def test_mes_schedule_issue_structured_backflow_sql_is_idempotent() -> None:
    sql = SQL_PATH.read_text(encoding="utf-8")

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260610_mes_schedule_order_p1; type=schema; riskLevel=medium\n"
    )
    assert "information_schema.columns" in sql
    assert "information_schema.statistics" in sql
    assert "PREPARE mes_schedule_issue_status_stmt FROM @mes_schedule_issue_status_sql;" in sql
    assert "PREPARE mes_schedule_issue_status_index_stmt FROM @mes_schedule_issue_status_index_sql;" in sql
    assert "PREPARE mes_schedule_issue_source_index_stmt FROM @mes_schedule_issue_source_index_sql;" in sql
    assert "ALTER TABLE `mes_pro_schedule_issue`\n    ADD COLUMN `status`" not in sql
    assert "ALTER TABLE `mes_pro_schedule_issue`\n    ADD KEY `idx_mes_pro_schedule_issue_status`" not in sql


if __name__ == "__main__":
    test_mes_schedule_issue_structured_backflow_sql_is_idempotent()
    print("PASS: MES schedule issue structured backflow SQL idempotency contract")
