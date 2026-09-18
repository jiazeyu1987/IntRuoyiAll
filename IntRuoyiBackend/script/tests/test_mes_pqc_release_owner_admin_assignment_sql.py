from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SQL = ROOT / "sql" / "mysql" / "20260918_mes_pqc_release_owner_admin_assignment.sql"


def read_sql() -> str:
    return SQL.read_text(encoding="utf-8")


def test_admin_assignment_is_precise_and_idempotent() -> None:
    sql = read_sql()
    assert "MES_PQC_RELEASE_OWNER" in sql
    assert "username = 'admin'" in sql
    assert "tenant_id = 1" in sql
    assert "INSERT INTO `system_user_role`" in sql
    assert "NOT EXISTS" in sql
    assert "ON DUPLICATE KEY UPDATE" not in sql
    assert "DELETE FROM `system_user_role`" not in sql


def test_release_owner_permission_is_not_qa_dispose_permission() -> None:
    sql = read_sql()
    assert "mes:pro-production-release:pqc-reject" in sql
    assert "mes:pro-edhr-nonconformance-review:dispose" not in sql
    assert "mes:pro-edhr-change:void" not in sql
