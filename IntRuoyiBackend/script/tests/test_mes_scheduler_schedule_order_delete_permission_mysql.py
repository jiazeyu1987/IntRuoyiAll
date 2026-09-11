import os
import subprocess
from pathlib import Path


BACKEND_ROOT = Path(__file__).resolve().parents[2]
MIGRATION_ID = "20260911_mes_scheduler_schedule_order_delete_permission"
MIGRATION_PATH = BACKEND_ROOT / "sql" / "mysql" / f"{MIGRATION_ID}.sql"
PREFLIGHT_PATH = (
    BACKEND_ROOT / "sql" / "mysql" / "target-preflight" / f"{MIGRATION_ID}.preflight.sql"
)
MYSQL_CONTAINER = "int-ruoyi-mysql"


def mysql(database: str | None, sql: str, expect_success: bool = True) -> subprocess.CompletedProcess[str]:
    command = 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" --default-character-set=utf8mb4 -N -B'
    if database:
        command += f" {database}"
    result = subprocess.run(
        ["docker", "exec", "-i", MYSQL_CONTAINER, "sh", "-lc", command],
        input=sql,
        text=True,
        encoding="utf-8",
        capture_output=True,
        check=False,
    )
    if expect_success:
        assert result.returncode == 0, result.stderr
    else:
        assert result.returncode != 0, result.stdout
    return result


def create_fixture(database: str) -> None:
    mysql(
        None,
        f"DROP DATABASE IF EXISTS `{database}`; CREATE DATABASE `{database}` "
        "CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;",
    )
    mysql(
        database,
        """
CREATE TABLE system_role (
  id bigint NOT NULL,
  name varchar(64) NOT NULL,
  code varchar(100) NOT NULL,
  status tinyint NOT NULL,
  deleted bit(1) NOT NULL,
  tenant_id bigint NOT NULL,
  PRIMARY KEY (id)
);
CREATE TABLE system_menu (
  id bigint NOT NULL,
  permission varchar(128) NOT NULL,
  type tinyint NOT NULL,
  parent_id bigint NOT NULL,
  status tinyint NOT NULL,
  deleted bit(1) NOT NULL,
  PRIMARY KEY (id)
);
CREATE TABLE system_role_menu (
  id bigint NOT NULL AUTO_INCREMENT,
  role_id bigint NOT NULL,
  menu_id bigint NOT NULL,
  creator varchar(64) DEFAULT '',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater varchar(64) DEFAULT '',
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted bit(1) NOT NULL DEFAULT b'0',
  tenant_id bigint NOT NULL,
  PRIMARY KEY (id)
);
INSERT INTO system_role (id, name, code, status, deleted, tenant_id) VALUES
  (101, 'Scheduler 1', 'mes_scheduler', 0, b'0', 1),
  (12201, 'Scheduler 122', 'mes_scheduler', 0, b'0', 122),
  (201, 'Other role', 'other_role', 0, b'0', 1);
INSERT INTO system_menu (id, permission, type, parent_id, status, deleted) VALUES
  (5586, 'mes:pro-schedule-order:delete', 3, 5580, 0, b'0');
INSERT INTO system_role_menu (id, role_id, menu_id, creator, updater, deleted, tenant_id) VALUES
  (11, 12201, 5586, 'legacy', 'legacy', b'1', 122),
  (12, 12201, 5586, 'legacy', 'legacy', b'1', 122),
  (21, 201, 5586, 'other', 'other', b'0', 1);
""",
    )


def scalar(database: str, sql: str) -> str:
    return mysql(database, sql).stdout.strip()


def test_mysql_migration_is_idempotent_isolated_and_reversible() -> None:
    database = f"codex_scheduler_delete_permission_{os.getpid()}"
    migration = MIGRATION_PATH.read_text(encoding="utf-8")
    preflight = PREFLIGHT_PATH.read_text(encoding="utf-8")
    marker = "mes-scheduler-schedule-order-delete-20260911"

    try:
        create_fixture(database)
        assert f"TARGET_PREFLIGHT_PASS:{MIGRATION_ID}" in scalar(database, preflight)

        mysql(database, migration)
        assert scalar(
            database,
            "SELECT GROUP_CONCAT(CONCAT(r.tenant_id, ':', COUNT_ACTIVE) ORDER BY r.tenant_id) "
            "FROM system_role r JOIN ("
            "SELECT role_id, tenant_id, SUM(deleted=b'0') COUNT_ACTIVE FROM system_role_menu "
            "WHERE menu_id=5586 GROUP BY role_id, tenant_id"
            ") x ON x.role_id=r.id AND x.tenant_id=r.tenant_id WHERE r.code='mes_scheduler';",
        ) == "1:1,122:1"
        assert scalar(
            database,
            "SELECT CONCAT(id, ':', CAST(deleted AS UNSIGNED), ':', updater) "
            "FROM system_role_menu WHERE role_id=12201 AND menu_id=5586 ORDER BY id;",
        ).splitlines() == [f"11:0:{marker}", "12:1:legacy"]
        assert scalar(
            database,
            "SELECT CONCAT(COUNT(*), ':', SUM(deleted=b'0')) FROM system_role_menu "
            "WHERE role_id=201 AND tenant_id=1 AND menu_id=5586;",
        ) == "1:1"

        first_row_count = scalar(database, "SELECT COUNT(*) FROM system_role_menu;")
        mysql(database, migration)
        assert scalar(database, "SELECT COUNT(*) FROM system_role_menu;") == first_row_count
        assert scalar(
            database,
            "SELECT COUNT(*) FROM system_role_menu rm JOIN system_role r ON r.id=rm.role_id "
            "AND r.tenant_id=rm.tenant_id WHERE r.code='mes_scheduler' AND rm.menu_id=5586 "
            "AND rm.deleted=b'0';",
        ) == "2"

        mysql(
            database,
            f"""
UPDATE system_role_menu rm
JOIN system_role r ON r.id=rm.role_id AND r.tenant_id=rm.tenant_id
JOIN system_menu m ON m.id=rm.menu_id
SET rm.deleted=b'1', rm.updater='rollback-{MIGRATION_ID}', rm.update_time=NOW()
WHERE r.code='mes_scheduler' AND r.status=0 AND r.deleted=b'0'
  AND m.permission='mes:pro-schedule-order:delete' AND m.status=0 AND m.deleted=b'0'
  AND rm.deleted=b'0'
  AND (rm.creator='{marker}' OR rm.updater='{marker}');
""",
        )
        assert scalar(
            database,
            "SELECT COUNT(*) FROM system_role_menu rm JOIN system_role r ON r.id=rm.role_id "
            "AND r.tenant_id=rm.tenant_id WHERE r.code='mes_scheduler' AND rm.menu_id=5586 "
            "AND rm.deleted=b'0';",
        ) == "0"
        assert scalar(
            database,
            "SELECT COUNT(*) FROM system_role_menu WHERE role_id=201 AND menu_id=5586 AND deleted=b'0';",
        ) == "1"

        create_fixture(database)
        mysql(
            database,
            "INSERT INTO system_role_menu (role_id, menu_id, creator, updater, deleted, tenant_id) "
            "VALUES (101,5586,'duplicate-a','duplicate-a',b'0',1),"
            "(101,5586,'duplicate-b','duplicate-b',b'0',1);",
        )
        assert f"TARGET_PREFLIGHT_BLOCKED:{MIGRATION_ID}" in scalar(database, preflight)
        failure = mysql(database, migration, expect_success=False)
        assert "Scheduler delete permission has duplicate active bindings before grant" in failure.stderr
        assert scalar(
            database,
            "SELECT COUNT(*) FROM system_role_menu WHERE role_id=101 AND menu_id=5586 AND deleted=b'0';",
        ) == "2"
    finally:
        mysql(None, f"DROP DATABASE IF EXISTS `{database}`;")
