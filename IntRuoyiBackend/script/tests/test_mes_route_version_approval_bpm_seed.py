from pathlib import Path
import re


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql" / "mysql" / "20260717_mes_route_version_approval_bpm_seed.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing MES route version approval BPM seed migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_route_version_approval_seed_declares_release_metadata() -> None:
    sql = read_sql()

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260714_bpm_category_seed_fix; type=seed; riskLevel=low"
    )
    assert "mes-route-version-approval-v1" in sql
    assert "工艺路线版本审批" in sql
    assert "START TRANSACTION;" in sql
    assert "COMMIT;" in sql


def test_route_version_approval_test_tenant_assigns_verified_e2e_user() -> None:
    sql = read_sql()

    assert "SET @route_version_admin_user_id_tenant_122" in sql
    assert "WHERE username = 'aoteman' AND tenant_id = 122" in sql
    test_tenant_bpmn = re.search(
        r"SET @route_version_approval_bpmn_tenant_122 = CONCAT\('(?P<bpmn>[\s\S]+?)</definitions>'\);",
        sql,
    )
    assert test_tenant_bpmn, "missing tenant 122 route approval BPMN"
    bpmn = test_tenant_bpmn.group("bpmn")

    assert "<flowable:candidateStrategy>30</flowable:candidateStrategy>" in bpmn
    assert (
        "<flowable:candidateParam>', @route_version_admin_user_id_tenant_122, '</flowable:candidateParam>"
        in bpmn
    )
    assert "@route_version_admin_role_id_tenant_122, '</flowable:candidateParam>" not in bpmn


def test_route_version_approval_seed_repairs_existing_role_based_test_bpmn() -> None:
    sql = read_sql()

    assert "rv-approval-bpmn-tenant-122" in sql
    assert "LOCATE('<flowable:candidateStrategy>10</flowable:candidateStrategy>', CONVERT(b.BYTES_ USING utf8mb4)) > 0" in sql
    assert "LOCATE(CAST(@route_version_admin_role_id_tenant_122 AS CHAR), CONVERT(b.BYTES_ USING utf8mb4)) > 0" in sql
