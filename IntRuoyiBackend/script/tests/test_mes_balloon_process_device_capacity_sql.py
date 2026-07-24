from pathlib import Path
import re


SQL_PATH = Path("sql/mysql/20260708_mes_balloon_process_device_capacity.sql")


def read_sql() -> str:
    assert SQL_PATH.exists(), "balloon process/device capacity migration must exist"
    return SQL_PATH.read_text(encoding="utf-8")


def executable_sql(sql: str) -> str:
    return "\n".join(line for line in sql.splitlines() if not re.match(r"^\s*--", line)).upper()


def test_balloon_capacity_migration_adds_required_columns_and_indexes():
    sql = read_sql()

    for token in [
        "release-migration:",
        "intruoyi_add_balloon_process_device_capacity_columns",
        "mes_pro_process",
        "product_name",
        "manual_shift_capacity",
        "mes_dv_machinery_process",
        "process_code",
        "idx_mes_dv_machinery_process_process_id",
    ]:
        assert token in sql, f"migration must include token: {token}"


def test_balloon_capacity_migration_seeds_authoritative_counts_for_tenant_1_only():
    sql = read_sql()

    assert "SET @target_tenant_id = 1" in sql
    assert "SET @process_seed_count = 49" in sql
    assert "SET @machinery_seed_count = 31" in sql
    assert "SET @machinery_process_seed_count = 83" in sql
    assert "tenant_id = @target_tenant_id" in sql
    assert "tenant_id = 122" not in sql


def test_balloon_capacity_migration_contains_manual_and_machine_examples():
    sql = read_sql()

    for token in [
        "球囊扩张导管",
        "棘突球囊扩张导管",
        "Z2630",
        "吹球囊成型",
        "A03190",
        "球囊成型机",
        "Z5200",
        "穿显影环",
        "740.000000",
        "Z760",
        "包套装管",
        "5440.000000",
    ]:
        assert token in sql, f"migration must include Excel-derived token: {token}"


def test_balloon_capacity_migration_is_non_destructive():
    upper_sql = executable_sql(read_sql())

    for forbidden in [
        "TRUNCATE",
        "DROP TABLE",
        "DROP COLUMN",
        "DELETE FROM MES_DV_MACHINERY",
        "DELETE FROM MES_PRO_PROCESS WHERE",
        "DELETE FROM MES_DV_MACHINERY_PROCESS WHERE",
    ]:
        assert forbidden not in upper_sql


def test_balloon_capacity_migration_cleans_legacy_process_rows_without_codes():
    sql = read_sql()

    for token in [
        "tmp_balloon_machinery_process_legacy_keep",
        "mp.`line_name` = seed.`product_name`",
        "mp.`machinery_code` = seed.`machinery_code`",
        "mp.`source_row_no` = seed.`source_row_no`",
        "mp.`process_name` = seed.`process_name`",
        "mp.`process_code` = legacy_keep.`process_code`",
        "mp.`id` <> legacy_keep.`keep_id`",
    ]:
        assert token in sql, f"migration must clean legacy machinery process rows: {token}"


def test_balloon_capacity_migration_removes_manual_capacity_from_machinery_detail():
    sql = read_sql()

    for token in [
        "seed.`manual_shift_capacity` IS NOT NULL",
        "mp.`machinery_code` = '/'",
        "SET mp.`deleted` = b'1'",
    ]:
        assert token in sql, f"manual capacity rows must not remain as machinery details: {token}"


def test_balloon_route_process_migration_seeds_two_routes_and_expected_counts():
    sql = read_sql()

    for token in [
        "tmp_balloon_route_seed",
        "ROUTE-BALLOON-CATHETER",
        "球囊扩张导管工艺路线",
        "ROUTE-SCORING-BALLOON-CATHETER",
        "棘突球囊扩张导管工艺路线",
        "SET @balloon_route_process_count = 23",
        "SET @scoring_balloon_route_process_count = 26",
        "mes_pro_route_process",
    ]:
        assert token in sql, f"migration must include route process token: {token}"


def test_balloon_route_process_migration_is_fail_fast_and_process_id_based():
    upper_sql = executable_sql(read_sql())

    for token in [
        "SIGNAL SQLSTATE '45000'",
        "MISSING BALLOON ROUTE PROCESS MASTER DATA",
        "BALLOON ROUTE PROCESS SORT CONFLICT",
        "BALLOON ROUTE PROCESS ID CONFLICT",
        "PROCESS.`PRODUCT_NAME` = SEED.`PRODUCT_NAME`",
        "PROCESS.`CODE` = SEED.`PROCESS_CODE`",
        "PROCESS.`NAME` = SEED.`PROCESS_NAME`",
        "`PROCESS_ID`",
        "`NEXT_PROCESS_ID`",
    ]:
        assert token in upper_sql, f"migration must fail fast or link by process id: {token}"
