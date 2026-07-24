import subprocess
from pathlib import Path

from openpyxl import Workbook


ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / "script" / "dcc_product_group_workbook_to_sql.py"

HEADERS = [
    "tenant_id",
    "group_code",
    "group_name",
    "dept_id",
    "dept_name",
    "user_id",
    "username",
    "nickname",
    "product_master_id",
    "product_code",
    "dcc_product_code",
    "product_name",
    "candidate_source",
    "manual_confirm",
    "confirm_note",
]


def write_workbook(path, rows):
    wb = Workbook()
    ws = wb.active
    ws.title = "候选明细"
    ws.append(HEADERS)
    for row in rows:
        ws.append([row.get(header, "") for header in HEADERS])
    wb.save(path)


def run_to_sql(tmp_path, rows):
    workbook = tmp_path / "confirm.xlsx"
    output_sql = tmp_path / "confirmed.sql"
    write_workbook(workbook, rows)
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
    cp, output_sql = run_to_sql(tmp_path, [{
        "tenant_id": 1,
        "group_code": "np-alpha",
        "group_name": "新品A组",
        "dept_id": 136,
        "user_id": 200,
        "product_master_id": 300,
        "manual_confirm": "",
    }])

    assert cp.returncode != 0
    assert "No confirmed rows" in cp.stderr
    assert not output_sql.exists()


def test_to_sql_generates_transaction_sql_for_confirmed_workbook(tmp_path):
    cp, output_sql = run_to_sql(tmp_path, [{
        "tenant_id": 1,
        "group_code": "np-alpha",
        "group_name": "新品A组",
        "dept_id": 136,
        "user_id": 200,
        "product_master_id": 300,
        "manual_confirm": "确认",
    }])

    assert cp.returncode == 0, cp.stderr
    assert "confirmed_rows=1" in cp.stdout
    sql = output_sql.read_text(encoding="utf-8")
    assert "START TRANSACTION;" in sql
    assert "DCC_PRODUCT_GROUP_CONFIRMED_PRECHECK_FAILED" in sql
    assert "dcc_product_visibility_group_member" in sql
    assert "dcc_product_visibility_group_product" in sql
