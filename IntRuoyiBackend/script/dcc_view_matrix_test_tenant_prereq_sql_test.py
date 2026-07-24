from __future__ import annotations

import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SQL_PATH = ROOT / "script" / "dcc_view_matrix_test_tenant_prereq_20260624.sql"


def main() -> None:
    if not SQL_PATH.exists():
        raise AssertionError(f"missing test tenant prerequisite SQL: {SQL_PATH}")
    sql = SQL_PATH.read_text(encoding="utf-8")

    assert "VIEW_MATRIX_TEST_TENANT_ONLY" in sql
    assert "@dcc_view_matrix_test_tenant_id <> 122" in sql
    assert "VIEW_MATRIX_TEST_PREREQ_AOTEMAN_MISSING" in sql
    assert "VIEW_MATRIX_TEST_PREREQ_DEPT_RESOLUTION_FAILED" in sql
    assert "VIEW_MATRIX_TEST_PREREQ_USER_RESOLUTION_FAILED" in sql
    assert "VIEW_MATRIX_TEST_PREREQ_LEADER_RESOLUTION_FAILED" in sql
    assert "tenant_id = @dcc_view_matrix_test_tenant_id" in sql
    assert "CREATE TEMPORARY TABLE tmp_dcc_view_matrix_test_parent_plan AS" in sql
    assert "CREATE TEMPORARY TABLE tmp_dcc_view_matrix_test_grand_plan AS" in sql
    assert "parent_resolved.dept_id AS parent_dept_id" in sql
    assert "ON parent_plan.dept_key = plan.parent_key" in sql
    assert "ON parent_resolved.dept_key = plan.dept_key" in sql
    assert "dept.parent_id = parent_resolved.dept_id" in sql
    assert "OR (plan.parent_key <> 'ROOT' AND parent_dept.name = plan.parent_name)" not in sql
    assert re.search(r"tenant_id\s*=\s*1\b", sql) is None

    expected_departments = [
        "瑛泰医疗",
        "质量体系中心",
        "QA",
        "QC",
        "QMS",
        "研发创新中心",
        "新品开发部",
        "设备开发部",
        "供应链中心",
        "包装设计组",
        "生产计划",
        "生产采购",
        "市场营销中心",
        "注册服务中心",
        "注册部",
        "检测中心",
        "生产制造中心",
    ]
    for department in expected_departments:
        assert department in sql

    unexpected_departments = [
        "市场服务部",
        "生产一车间",
    ]
    for department in unexpected_departments:
        assert department not in sql

    expected_users = [
        "dccmatrixqa",
        "dccmatrixqc",
        "dccmatrixqms",
        "dccmatrixpackaging",
        "dccmatrixmarket",
        "dccmatrixregsvc",
        "dccmatrixnewproduct",
        "dccmatrixinspection",
        "dccmatrixregdept",
        "dccmatrixproduction",
        "dccmatrixplan",
        "dccmatrixpurchase",
        "dccmatrixequipment",
        "dccmatrixqualitylead",
        "dccmatrixrdlead",
        "dccmatrixsupplylead",
    ]
    for username in expected_users:
        assert username in sql

    assert "dccmatrixworkshop" not in sql

    print("dcc view matrix test tenant prerequisite SQL contract PASS")


if __name__ == "__main__":
    main()
