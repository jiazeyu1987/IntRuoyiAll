from pathlib import Path


BACKEND_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = BACKEND_ROOT / "sql" / "mysql" / "20260818_mes_pressure_pump_same_name_item_convergence.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "pressure pump same-name item convergence migration must exist"
    return SQL_PATH.read_text(encoding="utf-8")


def normalized_sql() -> str:
    return " ".join(read_sql().split())


def test_pressure_pump_convergence_is_release_managed_and_transactional() -> None:
    sql = read_sql()
    normalized = normalized_sql().upper()

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260814_mes_c015_route_dcc_qa_reconciliation_schema; "
        "type=data; riskLevel=medium\n"
    )
    assert "START TRANSACTION" in normalized
    assert "COMMIT" in normalized
    assert "ROLLBACK" in normalized
    assert "SIGNAL SQLSTATE '45000'" in normalized


def test_pressure_pump_convergence_targets_exact_formal_ids() -> None:
    sql = read_sql()

    for token in [
        "v_target_tenant_id bigint DEFAULT 1",
        "v_target_route_id bigint DEFAULT 922119",
        "v_target_item_id bigint DEFAULT 902101",
        "v_target_item_code varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci",
        "DEFAULT 'AW.107.02.01.1009'",
        "v_target_route_code varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci",
        "DEFAULT 'RT000028'",
        "v_expected_product_master_id bigint DEFAULT 11",
        "v_expected_dcc_project_code_id bigint DEFAULT 147",
    ]:
        assert token in sql, f"migration must lock exact target identity: {token}"

    assert "item.name" not in sql.lower(), "migration must not identify target by Chinese display name"


def test_pressure_pump_convergence_is_idempotent_and_non_destructive() -> None:
    normalized = normalized_sql().upper()

    for token in [
        "SELECT COUNT(1)",
        "MES_PRO_ROUTE_PRODUCT",
        "WHERE `TENANT_ID` = V_TARGET_TENANT_ID",
        "AND `ROUTE_ID` = V_TARGET_ROUTE_ID",
        "AND `ITEM_ID` = V_TARGET_ITEM_ID",
        "INSERT INTO `MES_PRO_ROUTE_PRODUCT`",
        "V_EXISTING_ROUTE_PRODUCT_COUNT = 0",
    ]:
        assert token in normalized, f"migration must be guarded/idempotent: {token}"

    for forbidden in [
        "TRUNCATE",
        "DELETE FROM",
        "DROP TABLE",
        "DROP COLUMN",
        "UPDATE `MES_PRO_WORK_ORDER`",
        "UPDATE `MES_MD_ITEM`",
    ]:
        assert forbidden not in normalized


def test_pressure_pump_convergence_fails_fast_on_drift() -> None:
    normalized = normalized_sql().upper()

    for token in [
        "PRESSURE PUMP CONVERGENCE FAILED: TARGET ITEM IDENTITY DRIFTED",
        "PRESSURE PUMP CONVERGENCE FAILED: TARGET ROUTE IDENTITY DRIFTED",
        "PRESSURE PUMP CONVERGENCE FAILED: ROUTE DCC PRODUCT MASTER DRIFTED",
        "PRESSURE PUMP CONVERGENCE FAILED: DUPLICATE ROUTE PRODUCT BINDING",
        "PRESSURE PUMP CONVERGENCE FAILED: INSERT INCOMPLETE",
    ]:
        assert token in normalized, f"migration must fail fast with token: {token}"
