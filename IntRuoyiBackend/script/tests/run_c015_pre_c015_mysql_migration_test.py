from __future__ import annotations

import os
import re
import sys
import uuid
from pathlib import Path

import pymysql
from pymysql.connections import Connection


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_ROOT = REPO_ROOT / "sql" / "mysql"
PREFIX = "20260814_mes_c015_route_dcc_qa_reconciliation_"
DATABASE_PREFIX = "c015_round4_"
PRE_C015_TABLE_SOURCES = {
    "mes_md_item": SQL_ROOT / "20260512_mes_schema.sql",
    "mes_pro_route_product": SQL_ROOT / "20260512_mes_base_schema.sql",
    "dcc_project_code": SQL_ROOT / "20260513_dcc_base_schema.sql",
    "mes_pro_route_dcc_project_binding": SQL_ROOT / "20260812_mes_pqc_dcc_qa_c00_schema.sql",
}


def required_env(name: str) -> str:
    value = os.environ.get(name, "").strip()
    if not value:
        raise RuntimeError(f"missing required environment variable: {name}")
    return value


def execute_script(connection: Connection, path: Path) -> None:
    if not path.is_file():
        raise RuntimeError(f"missing migration file: {path}")
    delimiter = ";"
    buffer: list[str] = []
    with path.open("r", encoding="utf-8") as source:
        for raw_line in source:
            stripped = raw_line.strip()
            if stripped.upper().startswith("DELIMITER "):
                if buffer:
                    raise RuntimeError(f"unexpected buffered SQL before DELIMITER in {path.name}")
                delimiter = stripped.split(maxsplit=1)[1]
                continue
            if not stripped or stripped.startswith("--"):
                continue
            buffer.append(raw_line.rstrip("\r\n"))
            if stripped.endswith(delimiter):
                statement = "\n".join(buffer)
                statement = statement[: statement.rfind(delimiter)].strip()
                buffer.clear()
                if statement:
                    with connection.cursor() as cursor:
                        cursor.execute(statement)
                        while cursor.nextset():
                            pass
    if buffer:
        raise RuntimeError(f"unterminated SQL statement in {path.name}")


def execute_statements(connection: Connection, statements: list[str]) -> None:
    with connection.cursor() as cursor:
        for statement in statements:
            cursor.execute(statement)


def create_repository_table(connection: Connection, table_name: str) -> None:
    source_path = PRE_C015_TABLE_SOURCES[table_name]
    source = source_path.read_text(encoding="utf-8")
    pattern = re.compile(
        rf"CREATE TABLE IF NOT EXISTS `{re.escape(table_name)}`\s*\([\s\S]*?\)\s*ENGINE=.*?;",
        re.IGNORECASE,
    )
    matches = pattern.findall(source)
    if len(matches) != 1:
        raise RuntimeError(
            f"expected one repository CREATE TABLE for {table_name} in {source_path.name}, found {len(matches)}"
        )
    with connection.cursor() as cursor:
        cursor.execute(matches[0])


def scalar(connection: Connection, statement: str) -> int:
    with connection.cursor() as cursor:
        cursor.execute(statement)
        row = cursor.fetchone()
    return int(row[0])


def blocker_scopes(connection: Connection) -> set[str]:
    with connection.cursor() as cursor:
        cursor.execute("SELECT blocker_scope FROM c015_reconciliation_blocker_report")
        return {str(row[0]) for row in cursor.fetchall()}


def create_pre_c015_fixture(connection: Connection) -> None:
    for table_name in PRE_C015_TABLE_SOURCES:
        create_repository_table(connection, table_name)
    execute_statements(
        connection,
        [
            "CREATE TABLE mes_pro_route (id BIGINT NOT NULL, tenant_id BIGINT NOT NULL, deleted BIT(1) NOT NULL DEFAULT b'0', PRIMARY KEY (id)) ENGINE=InnoDB",
            "CREATE TABLE mes_pro_route_version (id BIGINT NOT NULL, route_id BIGINT NOT NULL, route_snapshot_json LONGTEXT DEFAULT NULL, tenant_id BIGINT NOT NULL, deleted BIT(1) NOT NULL DEFAULT b'0', PRIMARY KEY (id)) ENGINE=InnoDB",
            "CREATE TABLE mes_qa_inspection_regulation (id BIGINT NOT NULL, dcc_project_code_id BIGINT DEFAULT NULL, owner_module VARCHAR(32) DEFAULT NULL, tenant_id BIGINT NOT NULL, deleted BIT(1) NOT NULL DEFAULT b'0', PRIMARY KEY (id), UNIQUE KEY uk_mes_qa_regulation_dcc_project (tenant_id, dcc_project_code_id, deleted)) ENGINE=InnoDB",
            "CREATE TABLE mes_qa_inspection_regulation_version (id BIGINT NOT NULL, regulation_id BIGINT NOT NULL, lifecycle_status VARCHAR(32) NOT NULL, inspection_type_rules_json LONGTEXT DEFAULT NULL, tenant_id BIGINT NOT NULL, deleted BIT(1) NOT NULL DEFAULT b'0', PRIMARY KEY (id)) ENGINE=InnoDB",
            "CREATE TABLE mes_qa_inspection_regulation_process (id BIGINT NOT NULL, regulation_version_id BIGINT NOT NULL, tenant_id BIGINT NOT NULL, deleted BIT(1) NOT NULL DEFAULT b'0', PRIMARY KEY (id)) ENGINE=InnoDB",
            "CREATE TABLE mes_qa_inspection_regulation_item (id BIGINT NOT NULL, regulation_version_id BIGINT NOT NULL, qa_process_id BIGINT NOT NULL, inspection_type VARCHAR(32) NOT NULL, tenant_id BIGINT NOT NULL, deleted BIT(1) NOT NULL DEFAULT b'0', PRIMARY KEY (id)) ENGINE=InnoDB",
            "CREATE TABLE mes_pro_process_pool_active_order (id BIGINT NOT NULL, work_order_id BIGINT DEFAULT NULL, route_id BIGINT DEFAULT NULL, route_version_id BIGINT DEFAULT NULL, dcc_project_code_id BIGINT DEFAULT NULL, qa_regulation_id BIGINT DEFAULT NULL, qa_regulation_version_id BIGINT DEFAULT NULL, erp_fixed_quantity_snapshot DECIMAL(18,6) DEFAULT NULL, active_status VARCHAR(32) DEFAULT NULL, tenant_id BIGINT NOT NULL, deleted BIT(1) NOT NULL DEFAULT b'0', PRIMARY KEY (id)) ENGINE=InnoDB",
            "CREATE TABLE mes_pro_process_pool_active_order_process_snapshot (id BIGINT NOT NULL, active_order_id BIGINT NOT NULL, work_order_id BIGINT NOT NULL, route_id BIGINT NOT NULL, route_version_id BIGINT NOT NULL, route_process_id BIGINT NOT NULL, process_id BIGINT NOT NULL, erp_fixed_quantity_snapshot DECIMAL(18,6) DEFAULT NULL, production_quantity_factor_snapshot DECIMAL(18,6) DEFAULT NULL, planned_quantity_snapshot DECIMAL(18,6) DEFAULT NULL, tenant_id BIGINT NOT NULL, deleted BIT(1) NOT NULL DEFAULT b'0', PRIMARY KEY (id)) ENGINE=InnoDB",
            "CREATE TABLE mes_pqc_inspection_task (id BIGINT NOT NULL, active_order_id BIGINT DEFAULT NULL, work_order_id BIGINT DEFAULT NULL, route_id BIGINT DEFAULT NULL, route_version_id BIGINT DEFAULT NULL, route_process_id BIGINT DEFAULT NULL, process_id BIGINT DEFAULT NULL, qa_process_id BIGINT DEFAULT NULL, regulation_version_id BIGINT DEFAULT NULL, inspection_type VARCHAR(32) DEFAULT NULL, inspection_rule_key VARCHAR(128) DEFAULT NULL, shift_code VARCHAR(32) DEFAULT NULL, round_no INT DEFAULT NULL, tenant_id BIGINT NOT NULL, deleted BIT(1) NOT NULL DEFAULT b'0', PRIMARY KEY (id)) ENGINE=InnoDB",
            "INSERT INTO mes_md_item (id, tenant_id, deleted) VALUES (101, 1, b'0')",
            "INSERT INTO mes_pro_route_product (id, route_id, item_id, tenant_id, deleted) VALUES (201, 301, 101, 1, b'0')",
            "INSERT INTO dcc_project_code (id, product_master_id, project_name, project_code, status, tenant_id, deleted) VALUES (401, 501, 'C015 fixture', 'C015-FIXTURE', 'ENABLE', 1, 0)",
            "INSERT INTO mes_pro_route_dcc_project_binding (id, route_id, dcc_project_code_id, version, tenant_id, deleted) VALUES (601, 301, 401, 1, 1, b'0')",
        ],
    )


def run() -> None:
    host = required_env("C015_MYSQL_HOST")
    port = int(required_env("C015_MYSQL_PORT"))
    user = required_env("C015_MYSQL_USER")
    password = required_env("C015_MYSQL_PASSWORD")
    database_name = DATABASE_PREFIX + uuid.uuid4().hex[:12]
    if not re.fullmatch(r"c015_round4_[a-f0-9]{12}", database_name):
        raise RuntimeError("unsafe temporary database name")

    admin = pymysql.connect(host=host, port=port, user=user, password=password, charset="utf8mb4", autocommit=True)
    try:
        with admin.cursor() as cursor:
            cursor.execute(f"CREATE DATABASE `{database_name}` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
        connection = pymysql.connect(
            host=host,
            port=port,
            user=user,
            password=password,
            database=database_name,
            charset="utf8mb4",
            autocommit=True,
        )
        try:
            create_pre_c015_fixture(connection)
            if scalar(connection, "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'mes_md_item' AND column_name = 'product_master_id'") != 0:
                raise AssertionError("fixture is not pre-C015: product_master_id already exists")

            execute_script(connection, SQL_ROOT / f"{PREFIX}bootstrap.sql")
            if scalar(connection, "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'mes_md_item' AND index_name = 'idx_mes_md_item_product_master'") != 3:
                raise AssertionError("bootstrap did not create the canonical three-column product-master index")

            try:
                execute_script(connection, SQL_ROOT / f"{PREFIX}preflight.sql")
            except pymysql.MySQLError as error:
                if error.args[0] != 1644 or "approved ID-based manifest" not in str(error):
                    raise
            else:
                raise AssertionError("preflight must block when the approved product-master mapping is absent")

            execute_statements(
                connection,
                ["UPDATE mes_md_item SET product_master_id = 501 WHERE id = 101 AND tenant_id = 1 AND deleted = b'0'"],
            )
            execute_script(connection, SQL_ROOT / f"{PREFIX}preflight.sql")

            execute_statements(
                connection,
                [
                    "INSERT INTO mes_pro_route (id, tenant_id, deleted) VALUES (301, 1, b'0')",
                    "INSERT INTO mes_pro_route_version (id, route_id, route_snapshot_json, tenant_id, deleted) VALUES (801, 301, '{\"configSnapshots\":{\"flowGraph\":{\"nodes\":[]}}}', 1, b'0')",
                    "INSERT INTO mes_qa_inspection_regulation (id, dcc_project_code_id, owner_module, tenant_id, deleted) VALUES (901, 401, 'MES_QA', 1, b'0')",
                    "INSERT INTO mes_qa_inspection_regulation_version (id, regulation_id, lifecycle_status, inspection_type_rules_json, tenant_id, deleted) VALUES (902, 901, 'RETIRED', '[{\"key\":\"FIRST\",\"inspectionType\":\"FIRST\",\"required\":true},{\"key\":\"PATROL_AM\",\"inspectionType\":\"PATROL\",\"required\":true},{\"key\":\"PATROL_PM\",\"inspectionType\":\"PATROL\",\"required\":true},{\"key\":\"FINAL\",\"inspectionType\":\"FINAL\",\"required\":true}]', 1, b'0')",
                    "INSERT INTO mes_pro_process_pool_active_order (id, work_order_id, route_id, route_version_id, dcc_project_code_id, qa_regulation_id, qa_regulation_version_id, erp_fixed_quantity_snapshot, active_status, tenant_id, deleted) VALUES (1001, 1101, 301, 801, 401, 901, 902, 200.000000, 'REMOVED', 1, b'0')",
                ],
            )
            try:
                execute_script(connection, SQL_ROOT / f"{PREFIX}preflight.sql")
            except pymysql.MySQLError as error:
                expected = {
                    "removed_active_order_route_identity",
                    "removed_active_order_process_snapshot_identity",
                    "removed_active_order_pqc_task_identity",
                }
                if error.args[0] != 1644 or not expected.issubset(blocker_scopes(connection)):
                    raise
            else:
                raise AssertionError("preflight must block incomplete REMOVED frozen history")
            execute_statements(
                connection,
                [
                    "DELETE FROM mes_pro_process_pool_active_order WHERE id = 1001",
                    "DELETE FROM mes_qa_inspection_regulation_version WHERE id = 902",
                    "DELETE FROM mes_qa_inspection_regulation WHERE id = 901",
                    "DELETE FROM mes_pro_route_version WHERE id = 801",
                    "DELETE FROM mes_pro_route WHERE id = 301",
                ],
            )
            execute_script(connection, SQL_ROOT / f"{PREFIX}preflight.sql")
            execute_script(connection, SQL_ROOT / f"{PREFIX}backfill.sql")
            execute_script(connection, SQL_ROOT / f"{PREFIX}schema.sql")
            execute_script(connection, SQL_ROOT / f"{PREFIX}postflight.sql")
            execute_script(connection, SQL_ROOT / f"{PREFIX}bootstrap.sql")
            execute_script(connection, SQL_ROOT / f"{PREFIX}preflight.sql")
            execute_script(connection, SQL_ROOT / f"{PREFIX}backfill.sql")
            execute_script(connection, SQL_ROOT / f"{PREFIX}schema.sql")
            execute_script(connection, SQL_ROOT / f"{PREFIX}postflight.sql")
            print("PASS: C015 pre-C015 MySQL blockers, staged enforcement and idempotent rerun")
        finally:
            connection.close()
    finally:
        with admin.cursor() as cursor:
            cursor.execute(f"DROP DATABASE IF EXISTS `{database_name}`")
            cursor.execute(
                "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name = %s",
                (database_name,),
            )
            if int(cursor.fetchone()[0]) != 0:
                raise AssertionError(f"temporary database cleanup failed: {database_name}")
        admin.close()


if __name__ == "__main__":
    try:
        run()
    except Exception as error:
        print(f"FAIL: {error}", file=sys.stderr)
        raise
