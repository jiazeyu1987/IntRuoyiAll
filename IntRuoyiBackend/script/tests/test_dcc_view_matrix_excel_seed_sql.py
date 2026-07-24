from pathlib import Path
import runpy


ROOT = Path(__file__).resolve().parents[2]


def test_excel_view_matrix_seed_contract_script_passes():
    runpy.run_path(str(ROOT / "script" / "dcc_view_matrix_independent_seed_sql_test.py"), run_name="__main__")


def test_excel_view_matrix_test_tenant_prereq_contract_script_passes():
    runpy.run_path(str(ROOT / "script" / "dcc_view_matrix_test_tenant_prereq_sql_test.py"), run_name="__main__")
