from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260919_mes_edhr_live_label_print_menu_removal.sql"
TARGET_MENU_IDS = [
    "900320",
    "900321",
    "900322",
    "900323",
    "900324",
    "900325",
    "900326",
    "900327",
    "900328",
    "900329",
    "900330",
    "900331",
]


def read_sql() -> str:
    assert SQL_PATH.exists(), "live eDHR label/print menu retirement SQL must be delivered"
    return SQL_PATH.read_text(encoding="utf-8")


def test_live_menu_retirement_targets_only_existing_label_and_print_task_menus() -> None:
    text = read_sql()

    assert (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260618_mes_edhr_label_print_queue; type=menu; riskLevel=low"
    ) in text
    for menu_id in TARGET_MENU_IDS:
        assert menu_id in text
    for absent_policy_menu_id in ["900338", "900339", "900340", "900341", "900342", "900343", "900344", "900345", "900346"]:
        assert absent_policy_menu_id not in text
    assert ") <> 12" in text
    assert "NOT IN (0, 12)" in text


def test_live_menu_retirement_soft_deletes_menu_role_and_package_assignments() -> None:
    text = read_sql()

    assert "UPDATE `system_menu`" in text
    assert "`deleted` = b'1'" in text
    assert "`visible` = b'0'" in text
    assert "`path` = ''" in text
    assert "`component` = ''" in text
    assert "`component_name` = ''" in text
    assert "UPDATE `system_role_menu`" in text
    assert "UPDATE `system_tenant_package`" in text
    assert "JSON_TABLE" in text
    assert "JSON_ARRAYAGG" in text
    assert "SIGNAL SQLSTATE '45000'" in text
    assert "JSON_VALID(`package`.`menu_ids`)" in text


def test_live_menu_retirement_forbids_destructive_or_silent_paths() -> None:
    text = read_sql().upper()

    for forbidden in [
        "DROP TABLE",
        "DELETE FROM `SYSTEM_MENU`",
        "DELETE FROM `SYSTEM_ROLE_MENU`",
        "DELETE FROM `SYSTEM_TENANT_PACKAGE`",
        "INSERT IGNORE",
        "ON DUPLICATE KEY UPDATE",
        "DEFAULT_SUCCESS",
    ]:
        assert forbidden not in text
