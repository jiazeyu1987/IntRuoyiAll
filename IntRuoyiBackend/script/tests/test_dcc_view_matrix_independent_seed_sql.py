from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
MIGRATION = ROOT / "sql" / "mysql" / "20260624_dcc_view_matrix_independent_seed.sql"
PREREQ_MIGRATION = ROOT / "sql" / "mysql" / "20260624_dcc_view_matrix_test_tenant_prereq.sql"


def read_sql() -> str:
    return MIGRATION.read_text(encoding="utf-8")


def test_view_matrix_seed_declares_release_contract():
    sql = read_sql()

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260623_dcc_view_matrix_independent_source; type=seed; riskLevel=low\n"
    )


def test_view_matrix_seed_matches_current_test_tenant_org_mapping():
    sql = read_sql()

    assert "('市场 / 业务+跟单及以上', '市场', '业务+跟单及以上', '●', 'ALL_MEMBERS', 'DEPT', '瑛泰医疗/市场营销中心'" in sql
    assert "('市场 / 注册', '市场', '注册', '●', 'ALL_MEMBERS', 'DEPT', '瑛泰医疗/注册服务中心'" in sql
    assert "('注册', '注册', NULL, '●', 'ALL_MEMBERS', 'DEPT', '瑛泰医疗/注册服务中心/注册部'" in sql
    assert "'注册服务中心/注册部'" not in sql
    assert "'市场营销中心/市场服务部'" not in sql
    assert "'顶级部门/注册服务中心'" not in sql


def test_view_matrix_seed_resolves_subjects_by_full_department_path():
    sql = read_sql()

    assert "parent_path_name varchar(64) NULL" in sql
    assert "grand_parent_path_name varchar(64) NULL" in sql
    assert "SUBSTRING_INDEX(subject_lookup_name, '/', 1)" in sql
    assert "parent_dept.name = subject.parent_path_name" in sql
    assert "parent_dept.parent_id = grand_dept.id" in sql
    assert "parent_dept.name = subject.parent_dept_name" not in sql


def test_view_matrix_test_tenant_prereq_declares_release_contract():
    sql = PREREQ_MIGRATION.read_text(encoding="utf-8")

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test; "
        "dependsOn=20260623_dcc_view_matrix_independent_source; type=seed; riskLevel=low\n"
    )
    assert "VIEW_MATRIX_TEST_TENANT_ONLY" in sql
    assert "@dcc_view_matrix_test_tenant_id := 122;" in sql


def test_view_matrix_test_tenant_prereq_matches_prod_org_topology():
    sql = PREREQ_MIGRATION.read_text(encoding="utf-8")

    assert "('regsvc', 'ytyl', '瑛泰医疗', '注册服务中心', 1040)" in sql
    assert "('regsvc', 'ROOT', '顶级部门', '注册服务中心', 1040)" not in sql


def test_view_matrix_test_tenant_prereq_resolves_parent_by_plan_key():
    sql = PREREQ_MIGRATION.read_text(encoding="utf-8")

    assert "CREATE TEMPORARY TABLE tmp_dcc_view_matrix_test_parent_plan AS" in sql
    assert "CREATE TEMPORARY TABLE tmp_dcc_view_matrix_test_grand_plan AS" in sql
    assert "parent_resolved.dept_id AS parent_dept_id" in sql
    assert "ON parent_plan.dept_key = plan.parent_key" in sql
    assert "ON parent_resolved.dept_key = plan.dept_key" in sql
    assert "dept.parent_id = parent_resolved.dept_id" in sql
    assert "OR (plan.parent_key <> 'ROOT' AND parent_dept.name = plan.parent_name)" not in sql
