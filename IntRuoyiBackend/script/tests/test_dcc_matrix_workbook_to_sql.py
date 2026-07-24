import subprocess
from pathlib import Path

from openpyxl import Workbook


ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / "script" / "dcc_matrix_workbook_to_sql.py"

CLASSIFICATION_HEADERS = [
    "title",
    "deleted",
    "file_id",
    "file_name",
    "tenant_id",
    "manual_confirm_category_code",
]

ROLE_HEADERS = [
    "matrix_department",
    "role_name",
    "role_code",
    "candidate_user_id",
    "manual_confirm",
]


def write_workbook(path, classification_rows, role_rows, include_role_sheet=True):
    wb = Workbook()
    ws = wb.active
    ws.title = "文件归类待确认"
    ws.append(CLASSIFICATION_HEADERS)
    for row in classification_rows:
        ws.append([row.get(header, "") for header in CLASSIFICATION_HEADERS])
    if include_role_sheet:
        role_ws = wb.create_sheet("主管角色候选")
        role_ws.append(ROLE_HEADERS)
        for row in role_rows:
            role_ws.append([row.get(header, "") for header in ROLE_HEADERS])
    wb.save(path)


def run_to_sql(tmp_path, classification_rows, role_rows, include_role_sheet=True):
    workbook = tmp_path / "matrix-confirm.xlsx"
    output_sql = tmp_path / "matrix-confirmed.sql"
    write_workbook(workbook, classification_rows, role_rows, include_role_sheet=include_role_sheet)
    cp = subprocess.run(
        [
            "python", "-X", "utf8", str(SCRIPT),
            "--input-xlsx", str(workbook),
            "--output-sql", str(output_sql),
        ],
        cwd=ROOT,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=False,
    )
    return cp, output_sql


def test_to_sql_fails_when_workbook_has_no_confirmed_rows(tmp_path):
    cp, output_sql = run_to_sql(
        tmp_path,
        [{
            "deleted": "0",
            "file_id": "100",
            "tenant_id": "1",
            "manual_confirm_category_code": "",
        }],
        [{
            "role_code": "dcc_matrix_qc_lead",
            "candidate_user_id": "200",
            "manual_confirm": "",
        }],
    )

    assert cp.returncode != 0
    assert "No confirmed rows" in cp.stderr
    assert not output_sql.exists()


def test_to_sql_rejects_missing_role_sheet(tmp_path):
    cp, output_sql = run_to_sql(
        tmp_path,
        [{
            "deleted": "0",
            "file_id": "100",
            "tenant_id": "1",
            "manual_confirm_category_code": "DCC_FVM_DMR_001",
        }],
        [],
        include_role_sheet=False,
    )

    assert cp.returncode != 0
    assert "Workbook missing required sheet" in cp.stderr
    assert not output_sql.exists()


def test_to_sql_generates_transaction_sql_for_confirmed_workbook(tmp_path):
    cp, output_sql = run_to_sql(
        tmp_path,
        [{
            "deleted": "0",
            "file_id": "100",
            "tenant_id": "1",
            "manual_confirm_category_code": "DCC_FVM_DMR_001",
        }],
        [{
            "role_code": "dcc_matrix_qc_lead",
            "candidate_user_id": "200",
            "manual_confirm": "确认",
        }],
    )

    assert cp.returncode == 0, cp.stderr
    assert "classification_rows=1" in cp.stdout
    assert "role_rows=1" in cp.stdout
    sql = output_sql.read_text(encoding="utf-8")
    assert "START TRANSACTION;" in sql
    assert "DCC_MATRIX_CONFIRMED_FILE_PRECHECK_FAILED" in sql
    assert "DCC_MATRIX_CONFIRMED_ROLE_MEMBER_PRECHECK_FAILED" in sql
    assert "UPDATE dcc_controlled_file" in sql
    assert "INSERT INTO system_user_role" in sql
    assert "can_download" not in sql
    assert "dcc_directory_access_rule" not in sql
