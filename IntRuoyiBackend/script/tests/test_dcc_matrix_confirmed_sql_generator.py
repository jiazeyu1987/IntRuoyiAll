import csv
import subprocess
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / "script" / "dcc_matrix_confirmed_sql_generator.py"


CLASSIFICATION_FIELDS = [
    "file_id", "tenant_id", "deleted", "manual_confirm_category_code",
]
ROLE_FIELDS = [
    "role_code", "candidate_user_id", "manual_confirm",
]


def write_csv(path, fields, rows):
    with path.open("w", encoding="utf-8-sig", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=fields)
        writer.writeheader()
        writer.writerows(rows)


def run_generator(tmp_path, classification_rows, role_rows):
    classification = tmp_path / "classification.csv"
    roles = tmp_path / "roles.csv"
    output = tmp_path / "confirmed.sql"
    write_csv(classification, CLASSIFICATION_FIELDS, classification_rows)
    write_csv(roles, ROLE_FIELDS, role_rows)
    cp = subprocess.run(
        [
            "python", "-X", "utf8", str(SCRIPT),
            "--classification-csv", str(classification),
            "--role-csv", str(roles),
            "--output-sql", str(output),
        ],
        cwd=ROOT,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=False,
    )
    return cp, output


def test_generator_fails_when_nothing_is_confirmed(tmp_path):
    cp, output = run_generator(
        tmp_path,
        [{"file_id": "100", "tenant_id": "1", "deleted": "0", "manual_confirm_category_code": ""}],
        [{"role_code": "dcc_matrix_qc_lead", "candidate_user_id": "200", "manual_confirm": ""}],
    )

    assert cp.returncode != 0
    assert "No confirmed rows" in cp.stderr
    assert not output.exists()


def test_generator_rejects_deleted_file_selection(tmp_path):
    cp, output = run_generator(
        tmp_path,
        [{"file_id": "100", "tenant_id": "1", "deleted": "1", "manual_confirm_category_code": "DCC_FVM_DMR_001"}],
        [],
    )

    assert cp.returncode != 0
    assert "deleted!=0" in cp.stderr
    assert not output.exists()


def test_generator_writes_transaction_sql_for_confirmed_rows(tmp_path):
    cp, output = run_generator(
        tmp_path,
        [{"file_id": "100", "tenant_id": "1", "deleted": "0", "manual_confirm_category_code": "DCC_FVM_DMR_001"}],
        [{"role_code": "dcc_matrix_qc_lead", "candidate_user_id": "200", "manual_confirm": "是"}],
    )

    assert cp.returncode == 0, cp.stderr
    sql = output.read_text(encoding="utf-8")
    assert "START TRANSACTION;" in sql
    assert "tmp_dcc_confirmed_precheck_error" in sql
    assert "SIGNAL SQLSTATE '45000'" not in sql
    assert "UPDATE dcc_controlled_file" in sql
    assert "INSERT INTO system_user_role" in sql
    assert "DCC_FVM_DMR_001" in sql
    assert "dcc_matrix_qc_lead" in sql
    assert "can_download" not in sql
    assert "dcc_directory_access_rule" not in sql
