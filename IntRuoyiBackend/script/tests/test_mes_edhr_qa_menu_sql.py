from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260804_mes_edhr_qa_menu.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing eDHR QA menu SQL migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_edhr_qa_menu_declares_release_metadata_and_fail_fast_guards() -> None:
    text = read_sql()
    upper_text = text.upper()

    assert "release-migration: allowedEnvironments=test,backup,prod" in text
    assert "dependsOn=20260715_mes_edhr_template_config_menu_removal" in text
    assert "ensure_mes_edhr_qa_menu" in text
    assert "SET NAMES utf8mb4;" in text
    assert "SIGNAL SQLSTATE '45000'" in text
    assert "Invalid system_tenant_package.menu_ids JSON" in text
    assert "Missing eDHR parent menu 900220" in text
    assert "Missing retained eDHR visible menu rows; cannot insert QA menu" in text
    assert "system_menu id 900434 is already used by another active menu" in text
    assert "system_menu id 900435 is already used by another active menu" in text
    assert "QA menu route already exists on a different menu id" in text
    assert "PQC leader menu route already exists on a different menu id" in text

    for forbidden in [
        "DELETE FROM `SYSTEM_MENU`",
        "DELETE FROM `SYSTEM_ROLE_MENU`",
        "DELETE FROM `SYSTEM_TENANT_PACKAGE`",
        "TRUNCATE TABLE",
        "DROP TABLE",
    ]:
        assert forbidden not in upper_text


def test_edhr_qa_menu_inserts_between_batch_record_form_and_batch_execution() -> None:
    text = read_sql()

    expected_children = [
        (
            900365,
            "批记录表单",
            "mes:pro-batch-record-template:query",
            0,
            "/mes/pro/batch-record-form-list",
            "mes/pro/batchrecordformlist/index",
            "MesProBatchRecordFormList",
        ),
        (
            900434,
            "QA",
            "mes:pro-process-pool-team-leader:query",
            1,
            "/mes/pro/process-pool/qa-regulation",
            "mes/pro/processpool/QaRegulationPage",
            "MesProProcessPoolQaRegulation",
        ),
        (
            900033,
            "批次执行",
            "mes:pro-edhr-batch-execution:query",
            3,
            "/mes/pro/feedback/edhr-batch-execution",
            "mes/pro/edhr-batch/BatchExecutionListPage",
            "MesProEdhrBatchExecutionListPage",
        ),
        (
            900435,
            "PQC组长",
            "mes:pro-process-pool-team-leader:query",
            2,
            "/mes/pro/process-pool/pqc-leader",
            "mes/pro/processpool/PqcLeaderWorkbenchPage",
            "MesProProcessPoolPqcLeaderWorkbench",
        ),
        (
            900025,
            "表单追溯",
            "mes:pro-batch-record-execution:track",
            4,
            "/mes/pro/feedback/edhr-form-trace",
            "mes/pro/edhr/FormTracePage",
            "MesProFeedbackEdhrFormTrace",
        ),
        (
            900432,
            "表单日志",
            "mes:pro-edhr-form-fill-log:query",
            5,
            "/mes/pro/feedback/edhr-form-fill-log",
            "mes/pro/edhr/FormFillLogPage",
            "MesProEdhrFormFillLogPage",
        ),
    ]

    for menu_id, name, permission, sort, menu_path, component, component_name in expected_children:
        assert f"SELECT {menu_id} AS `id`, '{name}' AS `name`" in text
        assert f"'{permission}' AS `permission`, {sort} AS `sort`" in text
        assert f"'{menu_path}' AS `path`" in text
        assert f"'{component}' AS `component`" in text
        assert f"'{component_name}' AS `component_name`" in text

    assert "tmp_mes_edhr_qa_visible_order" in text
    assert "COUNT(*) FROM `tmp_mes_edhr_qa_visible_order`) <> 6" in text
    assert "`parent_id` = 900220" in text
    assert "`visible` = b'1'" in text


def test_edhr_qa_menu_is_bound_to_tenant_packages_and_admin_roles() -> None:
    text = read_sql()

    for required in [
        "tmp_mes_edhr_qa_target_packages",
        "tmp_mes_edhr_qa_package_menu_ids",
        "tmp_mes_edhr_qa_target_roles",
        "system_tenant_package",
        "system_role_menu",
        "system_role",
        "system_tenant",
        "JSON_VALID",
        "JSON_TABLE",
        "JSON_ARRAYAGG",
        "CAST('900220' AS JSON)",
        "CAST('900365' AS JSON)",
        "CAST('900033' AS JSON)",
        "900434",
        "900435",
        "'tenant_admin'",
        "'super_admin'",
        "INSERT INTO `system_role_menu`",
        "QA or PQC leader menu is not bound to any admin role",
        "QA or PQC leader menu is missing from target tenant packages",
    ]:
        assert required in text
