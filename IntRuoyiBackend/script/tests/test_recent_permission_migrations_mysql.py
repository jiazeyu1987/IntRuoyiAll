from __future__ import annotations

import subprocess
import uuid
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
LABEL_MIGRATION = REPO_ROOT / "sql/mysql/20260918_mes_edhr_label_print_menu_removal.sql"
LABEL_PREFLIGHT = REPO_ROOT / "sql/mysql/target-preflight/20260918_mes_edhr_label_print_menu_removal.preflight.sql"
MANAGEMENT_MIGRATION = REPO_ROOT / "sql/mysql/20260918_mes_management_representative_admin_permission.sql"
LABEL_MENU_IDS = (
    900320, 900321, 900322, 900323, 900324, 900325, 900326,
    900327, 900328, 900329, 900330, 900331, 900338, 900339,
    900340, 900341, 900342, 900343, 900344, 900345, 900346,
)


def _mysql(sql: str, database: str | None = None) -> subprocess.CompletedProcess[str]:
    command = [
        "docker", "exec", "-i", "int-ruoyi-mysql", "sh", "-lc",
        "MYSQL_PWD=\"$MYSQL_ROOT_PASSWORD\" exec mysql -uroot "
        "--default-character-set=utf8mb4 --batch --skip-column-names"
        + (f" {database}" if database else ""),
    ]
    return subprocess.run(command, input=sql, text=True, capture_output=True, check=False)


def _database(prefix: str) -> str:
    return f"{prefix}_{uuid.uuid4().hex[:12]}"


def _create_database(database: str) -> None:
    result = _mysql(
        f"CREATE DATABASE `{database}` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    )
    assert result.returncode == 0, result.stderr


def _drop_database(database: str) -> None:
    result = _mysql(f"DROP DATABASE IF EXISTS `{database}`;")
    assert result.returncode == 0, result.stderr


def _query(database: str, sql: str) -> str:
    result = _mysql(sql, database)
    assert result.returncode == 0, result.stderr
    return result.stdout.strip()


def _label_schema(menu_ids: str) -> str:
    menu_values = ",\n".join(
        f"({menu_id}, 0, 0, '/menu', 'Component', 'Component{menu_id}', 1, 1, 1, 'seed', NOW())"
        for menu_id in LABEL_MENU_IDS
    )
    return f"""
CREATE TABLE system_menu (
  id bigint PRIMARY KEY, status int NOT NULL, deleted bit(1) NOT NULL,
  path varchar(255), component varchar(255), component_name varchar(255),
  visible bit(1), keep_alive bit(1), always_show bit(1),
  updater varchar(64), update_time datetime
) ENGINE=InnoDB;
CREATE TABLE system_role_menu (
  id bigint PRIMARY KEY AUTO_INCREMENT, role_id bigint NOT NULL, menu_id bigint NOT NULL,
  deleted bit(1) NOT NULL, updater varchar(64), update_time datetime
) ENGINE=InnoDB;
CREATE TABLE system_tenant_package (
  id bigint PRIMARY KEY, menu_ids text, deleted bit(1) NOT NULL,
  updater varchar(64), update_time datetime
) ENGINE=InnoDB;
INSERT INTO system_menu VALUES
  (900220, 0, 0, '/edhr', 'Edhr', 'Edhr', 1, 1, 1, 'seed', NOW()),
  {menu_values};
INSERT INTO system_role_menu(role_id, menu_id, deleted, updater, update_time)
VALUES (1, 900320, b'0', 'seed', NOW());
INSERT INTO system_tenant_package VALUES (1, '{menu_ids}', b'0', 'seed', NOW());
"""


def test_template_category_migration_requires_target_preflight() -> None:
    migration = REPO_ROOT / "sql/mysql/20260526_dcc_other_template_category.sql"
    header = migration.read_text(encoding="utf-8").splitlines()[0]

    assert "requiresTargetPreflight=true" in header


def test_registration_certificate_reminder_schema_requires_target_preflight() -> None:
    migration = REPO_ROOT / "sql/mysql/20260818_dcc_registration_certificate_reminder.sql"
    header = migration.read_text(encoding="utf-8").splitlines()[0]

    assert "requiresTargetPreflight=true" in header


def test_label_preflight_blocks_empty_menu_ids_like_migration() -> None:
    database = _database("label_preflight")
    try:
        _create_database(database)
        setup = _mysql(_label_schema(""), database)
        assert setup.returncode == 0, setup.stderr

        result = _mysql(LABEL_PREFLIGHT.read_text(encoding="utf-8"), database)

        assert result.returncode == 0, result.stderr
        assert result.stdout.strip() == "TARGET_PREFLIGHT_BLOCKED:20260918_mes_edhr_label_print_menu_removal"
    finally:
        _drop_database(database)


def test_label_migration_rolls_back_all_writes_when_late_package_update_fails() -> None:
    database = _database("label_rollback")
    try:
        _create_database(database)
        setup = _mysql(
            _label_schema("[900320]")
            + """
CREATE TRIGGER fail_label_package_update BEFORE UPDATE ON system_tenant_package
FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='forced label package failure';
""",
            database,
        )
        assert setup.returncode == 0, setup.stderr

        result = _mysql(LABEL_MIGRATION.read_text(encoding="utf-8"), database)

        assert result.returncode != 0
        assert "forced label package failure" in result.stderr
        assert _query(database, "SELECT COUNT(*) FROM system_menu WHERE deleted=b'0';") == "22"
        assert _query(database, "SELECT COUNT(*) FROM system_role_menu WHERE deleted=b'0';") == "1"
        assert _query(database, "SELECT menu_ids FROM system_tenant_package WHERE id=1;") == "[900320]"
    finally:
        _drop_database(database)


def test_management_migration_rolls_back_menu_grants_when_user_role_insert_fails() -> None:
    database = _database("management_rollback")
    try:
        _create_database(database)
        setup = _mysql(
            """
CREATE TABLE system_tenant (id bigint PRIMARY KEY, deleted bit(1) NOT NULL) ENGINE=InnoDB;
CREATE TABLE system_users (
  id bigint PRIMARY KEY, tenant_id bigint NOT NULL, username varchar(64),
  status int NOT NULL, deleted bit(1) NOT NULL
) ENGINE=InnoDB;
CREATE TABLE system_role (
  id bigint PRIMARY KEY, tenant_id bigint NOT NULL, code varchar(128),
  status int NOT NULL, deleted bit(1) NOT NULL
) ENGINE=InnoDB;
CREATE TABLE system_menu (
  id bigint PRIMARY KEY, permission varchar(128), status int NOT NULL, deleted bit(1) NOT NULL
) ENGINE=InnoDB;
CREATE TABLE system_role_menu (
  id bigint PRIMARY KEY AUTO_INCREMENT, role_id bigint, menu_id bigint,
  creator varchar(64), create_time datetime, updater varchar(64), update_time datetime,
  deleted bit(1), tenant_id bigint
) ENGINE=InnoDB;
CREATE TABLE system_user_role (
  id bigint PRIMARY KEY AUTO_INCREMENT, user_id bigint, role_id bigint,
  creator varchar(64), create_time datetime, updater varchar(64), update_time datetime,
  deleted bit(1), tenant_id bigint
) ENGINE=InnoDB;
INSERT INTO system_tenant VALUES (1, b'0');
INSERT INTO system_users VALUES (1, 1, 'admin', 0, b'0');
INSERT INTO system_role VALUES (910218, 1, 'MES_MANAGEMENT_REPRESENTATIVE', 0, b'0');
INSERT INTO system_menu VALUES
  (1, 'mes:pro-edhr-release:query', 0, b'0'),
  (2, 'mes:pro-edhr-release:approve', 0, b'0');
CREATE TRIGGER fail_management_user_role_insert BEFORE INSERT ON system_user_role
FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='forced user role failure';
""",
            database,
        )
        assert setup.returncode == 0, setup.stderr

        result = _mysql(MANAGEMENT_MIGRATION.read_text(encoding="utf-8"), database)

        assert result.returncode != 0
        assert "forced user role failure" in result.stderr
        assert _query(database, "SELECT COUNT(*) FROM system_role_menu;") == "0"
        assert _query(database, "SELECT COUNT(*) FROM system_user_role;") == "0"
    finally:
        _drop_database(database)
