import csv
import subprocess
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / "script" / "dcc_product_group_confirmed_sql_generator.py"


FIELDS = [
    "tenant_id",
    "group_code",
    "group_name",
    "dept_id",
    "user_id",
    "product_master_id",
    "manual_confirm",
]


def write_csv(path, rows):
    with path.open("w", encoding="utf-8-sig", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=FIELDS)
        writer.writeheader()
        writer.writerows(rows)


def run_generator(tmp_path, rows):
    input_csv = tmp_path / "product-groups.csv"
    output_sql = tmp_path / "product-groups.sql"
    write_csv(input_csv, rows)
    cp = subprocess.run(
        [
            "python", "-X", "utf8", str(SCRIPT),
            "--input-csv", str(input_csv),
            "--output-sql", str(output_sql),
        ],
        cwd=ROOT,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=False,
    )
    return cp, output_sql


def test_generator_fails_when_no_rows_are_confirmed(tmp_path):
    cp, output = run_generator(
        tmp_path,
        [{
            "tenant_id": "1",
            "group_code": "np-alpha",
            "group_name": "新品A组",
            "dept_id": "136",
            "user_id": "200",
            "product_master_id": "300",
            "manual_confirm": "",
        }],
    )

    assert cp.returncode != 0
    assert "No confirmed product group rows" in cp.stderr
    assert not output.exists()


def test_generator_rejects_invalid_ids(tmp_path):
    cp, output = run_generator(
        tmp_path,
        [{
            "tenant_id": "1",
            "group_code": "np-alpha",
            "group_name": "新品A组",
            "dept_id": "abc",
            "user_id": "200",
            "product_master_id": "300",
            "manual_confirm": "是",
        }],
    )

    assert cp.returncode != 0
    assert "dept_id must be a positive integer" in cp.stderr
    assert not output.exists()


def test_generator_writes_fail_fast_transaction_sql(tmp_path):
    cp, output = run_generator(
        tmp_path,
        [{
            "tenant_id": "1",
            "group_code": "np-alpha",
            "group_name": "新品A组",
            "dept_id": "136",
            "user_id": "200",
            "product_master_id": "300",
            "manual_confirm": "确认",
        }],
    )

    assert cp.returncode == 0, cp.stderr
    sql = output.read_text(encoding="utf-8")
    assert "START TRANSACTION;" in sql
    assert "tmp_dcc_product_group_precheck_error" in sql
    assert "SIGNAL SQLSTATE '45000'" not in sql
    assert "dcc_product_visibility_group" in sql
    assert "dcc_product_visibility_group_member" in sql
    assert "dcc_product_visibility_group_product" in sql
    assert "system_dept" in sql
    assert "system_users" in sql
    assert "mdm_product" in sql
    assert "np-alpha" in sql
    assert "dcc_file_category_permission_rule" not in sql
    assert "system_user_role" not in sql
