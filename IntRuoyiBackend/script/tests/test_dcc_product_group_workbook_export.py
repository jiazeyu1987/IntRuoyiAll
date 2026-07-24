import csv
import subprocess
from pathlib import Path

from openpyxl import Workbook


ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / "script" / "dcc_product_group_workbook_export.py"

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


def write_workbook(path, rows, sheet_name="候选明细", headers=HEADERS):
    wb = Workbook()
    ws = wb.active
    ws.title = sheet_name
    ws.append(headers)
    for row in rows:
        ws.append([row.get(header, "") for header in headers])
    wb.save(path)


def run_export(tmp_path, rows, sheet_name="候选明细", headers=HEADERS):
    workbook = tmp_path / "confirm.xlsx"
    output = tmp_path / "confirmed.csv"
    write_workbook(workbook, rows, sheet_name=sheet_name, headers=headers)
    cp = subprocess.run(
        [
            "python", "-X", "utf8", str(SCRIPT),
            "--input-xlsx", str(workbook),
            "--output-csv", str(output),
        ],
        cwd=ROOT,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=False,
    )
    return cp, output


def read_csv(path):
    with path.open(encoding="utf-8-sig", newline="") as f:
        return list(csv.DictReader(f))


def test_export_fails_when_script_missing_or_no_confirmed_rows(tmp_path):
    cp, output = run_export(tmp_path, [{
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
    assert not output.exists()


def test_export_rejects_missing_required_header(tmp_path):
    headers = [header for header in HEADERS if header != "product_master_id"]
    cp, output = run_export(tmp_path, [], headers=headers)

    assert cp.returncode != 0
    assert "Missing required columns" in cp.stderr
    assert not output.exists()


def test_export_writes_only_confirmed_rows(tmp_path):
    cp, output = run_export(tmp_path, [
        {
            "tenant_id": 1,
            "group_code": "np-alpha",
            "group_name": "新品A组",
            "dept_id": 136,
            "user_id": 200,
            "product_master_id": 300,
            "manual_confirm": "确认",
        },
        {
            "tenant_id": 1,
            "group_code": "np-beta",
            "group_name": "新品B组",
            "dept_id": 136,
            "user_id": 201,
            "product_master_id": 301,
            "manual_confirm": "",
        },
    ])

    assert cp.returncode == 0, cp.stderr
    rows = read_csv(output)
    assert rows == [{
        "tenant_id": "1",
        "group_code": "np-alpha",
        "group_name": "新品A组",
        "dept_id": "136",
        "user_id": "200",
        "product_master_id": "300",
        "manual_confirm": "确认",
    }]
