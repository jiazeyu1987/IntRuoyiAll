from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260630_mes_pro_work_order_erp_snapshot_fields.sql"


def _sql_text() -> str:
    return SQL_PATH.read_text(encoding="utf-8")


def test_work_order_erp_snapshot_fields_sql_adds_expected_nullable_columns() -> None:
    sql = _sql_text()

    expected_columns = {
        "`workshop_name` varchar(128) DEFAULT NULL": "ERP production workshop",
        "`bom_version` varchar(128) DEFAULT NULL": "ERP BOM version",
        "`pick_mode` varchar(64) DEFAULT NULL": "ERP pick mode",
        "`auxiliary_code` varchar(128) DEFAULT NULL": "ERP auxiliary code",
        "`business_status` varchar(64) DEFAULT NULL": "ERP business status",
        "`drawing_number` varchar(128) DEFAULT NULL": "ERP drawing number",
        "`schedule_status` varchar(64) DEFAULT NULL": "ERP schedule status",
        "`planned_start_time` datetime DEFAULT NULL": "ERP planned start time",
        "`planned_end_time` datetime DEFAULT NULL": "ERP planned end time",
    }

    assert "mes_pro_work_order" in sql
    for column_definition in expected_columns:
        assert column_definition in sql


def test_work_order_erp_snapshot_fields_sql_is_idempotent_for_repeated_release_runs() -> None:
    sql = _sql_text()

    expected_guards = [
        "FROM information_schema.COLUMNS",
        "TABLE_NAME = 'mes_pro_work_order'",
        "COLUMN_NAME = 'workshop_name'",
        "COLUMN_NAME = 'bom_version'",
        "COLUMN_NAME = 'pick_mode'",
        "COLUMN_NAME = 'auxiliary_code'",
        "COLUMN_NAME = 'business_status'",
        "COLUMN_NAME = 'drawing_number'",
        "COLUMN_NAME = 'schedule_status'",
        "COLUMN_NAME = 'planned_start_time'",
        "COLUMN_NAME = 'planned_end_time'",
        "PREPARE mes_pro_work_order_erp_snapshot_workshop_name_stmt",
        "PREPARE mes_pro_work_order_erp_snapshot_bom_version_stmt",
        "PREPARE mes_pro_work_order_erp_snapshot_pick_mode_stmt",
        "PREPARE mes_pro_work_order_erp_snapshot_auxiliary_code_stmt",
        "PREPARE mes_pro_work_order_erp_snapshot_business_status_stmt",
        "PREPARE mes_pro_work_order_erp_snapshot_drawing_number_stmt",
        "PREPARE mes_pro_work_order_erp_snapshot_schedule_status_stmt",
        "PREPARE mes_pro_work_order_erp_snapshot_planned_start_time_stmt",
        "PREPARE mes_pro_work_order_erp_snapshot_planned_end_time_stmt",
    ]

    for fragment in expected_guards:
        assert fragment in sql

    assert "ALTER TABLE `mes_pro_work_order`\n  ADD COLUMN `workshop_name`" not in sql
    assert "ALTER TABLE `mes_pro_work_order`\r\n  ADD COLUMN `workshop_name`" not in sql


def test_work_order_erp_snapshot_fields_sql_keeps_existing_local_extensions() -> None:
    sql_lower = _sql_text().lower()

    forbidden_tokens = [
        "drop column",
        "drop table",
        "delete from",
        "truncate table",
        "rename column",
        "change column",
    ]

    for token in forbidden_tokens:
        assert token not in sql_lower
