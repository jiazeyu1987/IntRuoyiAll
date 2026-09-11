from pathlib import Path


BACKEND_ROOT = Path(__file__).resolve().parents[2]
MIGRATION_ID = "20260911_mes_scheduler_schedule_order_delete_permission"
MIGRATION_PATH = BACKEND_ROOT / "sql" / "mysql" / f"{MIGRATION_ID}.sql"
PREFLIGHT_PATH = (
    BACKEND_ROOT / "sql" / "mysql" / "target-preflight" / f"{MIGRATION_ID}.preflight.sql"
)


def read_text(path: Path) -> str:
    assert path.exists(), f"missing required SQL file: {path}"
    return path.read_text(encoding="utf-8")


def test_migration_declares_release_contract_and_exact_permission_scope() -> None:
    text = read_text(MIGRATION_PATH)

    assert text.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260624_mes_schedule_order_freeze_audit,20260629_mes_smart_scheduling_role_scope; "
        "type=permission; riskLevel=medium"
    )
    assert "`role`.`code` = 'mes_scheduler'" in text
    assert "`menu`.`permission` = 'mes:pro-schedule-order:delete'" in text
    assert "`role`.`status` = 0" in text
    assert "`menu`.`status` = 0" in text
    assert "`role`.`deleted` = b'0'" in text
    assert "`menu`.`deleted` = b'0'" in text


def test_migration_fails_fast_on_missing_or_ambiguous_role_and_menu() -> None:
    text = read_text(MIGRATION_PATH)

    for expected in [
        "Missing active mes_scheduler role",
        "Duplicate active mes_scheduler role in one tenant",
        "Missing or ambiguous schedule order delete permission menu",
    ]:
        assert expected in text
    assert "SIGNAL SQLSTATE '45000'" in text


def test_migration_reactivates_one_existing_binding_or_inserts_one_idempotently() -> None:
    text = read_text(MIGRATION_PATH)

    assert "MIN(`role_menu`.`id`) AS `keep_id`" in text
    assert "UPDATE `system_role_menu` AS `role_menu`" in text
    assert "`role_menu`.`id` = `existing`.`keep_id`" in text
    assert "SET `role_menu`.`deleted` = b'0'" in text
    assert "INSERT INTO `system_role_menu`" in text
    assert "WHERE NOT EXISTS" in text
    assert "Scheduler delete permission grant incomplete" in text
    assert "Scheduler delete permission grant has duplicate active bindings" in text


def test_migration_does_not_change_users_packages_or_other_roles() -> None:
    text = read_text(MIGRATION_PATH)
    upper = text.upper()

    for forbidden in [
        "SYSTEM_USER_ROLE",
        "SYSTEM_TENANT_PACKAGE",
        "DELETE FROM",
        "TRUNCATE TABLE",
        "UPDATE `SYSTEM_ROLE`",
        "UPDATE SYSTEM_ROLE",
        "UPDATE `SYSTEM_MENU`",
        "UPDATE SYSTEM_MENU",
    ]:
        assert forbidden not in upper


def test_target_preflight_is_read_only_and_checks_the_same_contract() -> None:
    text = read_text(PREFLIGHT_PATH)
    upper = text.upper()

    assert text.splitlines()[0] == (
        "-- release-target-preflight: "
        "migrationId=20260911_mes_scheduler_schedule_order_delete_permission; "
        "allowedEnvironments=test,prod,backup"
    )
    assert "TARGET_PREFLIGHT_PASS:20260911_mes_scheduler_schedule_order_delete_permission" in text
    assert "TARGET_PREFLIGHT_BLOCKED:20260911_mes_scheduler_schedule_order_delete_permission" in text
    assert "`role`.`code` = 'mes_scheduler'" in text
    assert "`menu`.`permission` = 'mes:pro-schedule-order:delete'" in text

    for forbidden in ["INSERT ", "UPDATE ", "DELETE ", "TRUNCATE ", "ALTER ", "DROP ", "CREATE "]:
        assert forbidden not in upper
