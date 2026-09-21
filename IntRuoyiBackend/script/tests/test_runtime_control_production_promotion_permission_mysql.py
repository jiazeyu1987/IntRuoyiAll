from __future__ import annotations

import re
import subprocess
import uuid
from pathlib import Path


MIGRATION = Path(__file__).resolve().parents[2] / "sql/mysql/20260919_runtime_control_production_promotion_permission.sql"
PREFLIGHT = Path(__file__).resolve().parents[2] / "sql/mysql/target-preflight/20260919_runtime_control_production_promotion_permission.preflight.sql"


def mysql(sql: str, database: str | None = None) -> subprocess.CompletedProcess[str]:
    command = [
        "docker", "exec", "-i", "int-ruoyi-mysql", "sh", "-lc",
        'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot --default-character-set=utf8mb4 '
        "--batch --skip-column-names" + (f" {database}" if database else ""),
    ]
    return subprocess.run(command, input=sql, text=True, encoding="utf-8", capture_output=True, check=False)


def create_database() -> str:
    database = f"release_promotion_perm_{uuid.uuid4().hex[:12]}"
    assert re.fullmatch(r"release_promotion_perm_[0-9a-f]{12}", database)
    result = mysql(f"CREATE DATABASE `{database}` CHARACTER SET utf8mb4;")
    assert result.returncode == 0, result.stderr
    setup = mysql("""
CREATE TABLE system_menu (
  id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY, name varchar(50) NOT NULL,
  permission varchar(100) NOT NULL DEFAULT '', type tinyint NOT NULL,
  sort int NOT NULL DEFAULT 0, parent_id bigint NOT NULL DEFAULT 0,
  path varchar(200), icon varchar(100), component varchar(255), component_name varchar(255),
  status tinyint NOT NULL DEFAULT 0, visible bit(1) NOT NULL DEFAULT b'1',
  keep_alive bit(1) NOT NULL DEFAULT b'1', always_show bit(1) NOT NULL DEFAULT b'1',
  creator varchar(64), create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater varchar(64), update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted bit(1) NOT NULL DEFAULT b'0'
) ENGINE=InnoDB;
INSERT INTO system_menu(id,name,permission,type,sort,parent_id,path,component)
VALUES (900104,'Existing unrelated menu','mes:pro-puhui-schedule:query',2,1,900120,'other','mes/other');
""", database)
    assert setup.returncode == 0, setup.stderr
    return database


def drop_database(database: str) -> None:
    assert re.fullmatch(r"release_promotion_perm_[0-9a-f]{12}", database)
    result = mysql(f"DROP DATABASE `{database}`;")
    assert result.returncode == 0, result.stderr


def query(database: str, sql: str) -> str:
    result = mysql(sql, database)
    assert result.returncode == 0, result.stderr
    return result.stdout.strip()


def add_parent(database: str, parent_id: int) -> None:
    result = mysql(f"""
INSERT INTO system_menu(id,name,permission,type,sort,parent_id,path,component)
VALUES ({parent_id},'Runtime control','infra:runtime-control:query',2,4,2740,
        'runtime-control','infra/runtime-control/index');
""", database)
    assert result.returncode == 0, result.stderr


def test_permission_uses_identity_without_overwriting_colliding_id_and_is_idempotent() -> None:
    database = create_database()
    try:
        add_parent(database, 980115)
        assert query(database, PREFLIGHT.read_text(encoding="utf-8")) == "TARGET_PREFLIGHT_PASS:20260919_runtime_control_production_promotion_permission"
        for _ in range(2):
            result = mysql(MIGRATION.read_text(encoding="utf-8"), database)
            assert result.returncode == 0, result.stderr
            assert query(database, PREFLIGHT.read_text(encoding="utf-8")) == "TARGET_PREFLIGHT_PASS:20260919_runtime_control_production_promotion_permission"
        assert query(database, "SELECT permission FROM system_menu WHERE id=900104;") == "mes:pro-puhui-schedule:query"
        assert query(database, "SELECT COUNT(*),MIN(parent_id) FROM system_menu WHERE deleted=b'0' AND type=3 AND permission='infra:runtime-control:promote-prod';") == "1\t980115"
    finally:
        drop_database(database)


def test_permission_fails_without_exactly_one_parent() -> None:
    for parents in ((), (980115, 980117)):
        database = create_database()
        try:
            for parent_id in parents:
                add_parent(database, parent_id)
            assert query(database, PREFLIGHT.read_text(encoding="utf-8")) == "TARGET_PREFLIGHT_BLOCKED:20260919_runtime_control_production_promotion_permission"
            result = mysql(MIGRATION.read_text(encoding="utf-8"), database)
            assert result.returncode != 0
            assert "RELEASE_PROMOTION_PARENT_AMBIGUOUS" in result.stderr
            assert query(database, "SELECT COUNT(*) FROM system_menu WHERE permission='infra:runtime-control:promote-prod';") == "0"
        finally:
            drop_database(database)


def test_permission_rejects_wrong_parent_without_creating_another_grant() -> None:
    database = create_database()
    try:
        add_parent(database, 980115)
        setup = mysql("""
INSERT INTO system_menu(name,permission,type,sort,parent_id,path,component)
VALUES ('Wrong parent','infra:runtime-control:promote-prod',3,4,1234,'','');
""", database)
        assert setup.returncode == 0, setup.stderr
        assert query(database, PREFLIGHT.read_text(encoding="utf-8")) == "TARGET_PREFLIGHT_BLOCKED:20260919_runtime_control_production_promotion_permission"

        result = mysql(MIGRATION.read_text(encoding="utf-8"), database)

        assert result.returncode != 0
        assert "RELEASE_PROMOTION_PERMISSION_CONFLICT" in result.stderr
        assert query(database, "SELECT COUNT(*),MIN(parent_id) FROM system_menu WHERE permission='infra:runtime-control:promote-prod';") == "1\t1234"
    finally:
        drop_database(database)


def test_permission_insert_failure_rolls_back_without_touching_existing_menu() -> None:
    database = create_database()
    try:
        add_parent(database, 980115)
        setup = mysql("""
CREATE TRIGGER fail_promotion_insert BEFORE INSERT ON system_menu
FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='forced permission insert failure';
""", database)
        assert setup.returncode == 0, setup.stderr

        result = mysql(MIGRATION.read_text(encoding="utf-8"), database)

        assert result.returncode != 0
        assert "forced permission insert failure" in result.stderr
        assert query(database, "SELECT COUNT(*) FROM system_menu WHERE permission='infra:runtime-control:promote-prod';") == "0"
        assert query(database, "SELECT permission FROM system_menu WHERE id=900104;") == "mes:pro-puhui-schedule:query"
    finally:
        drop_database(database)
