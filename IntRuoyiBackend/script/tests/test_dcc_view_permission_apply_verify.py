import json
import subprocess
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / "script" / "dcc_view_permission_apply_verify.py"


def write_bundle(tmp_path):
    bundle = tmp_path / "bundle"
    inputs = bundle / "inputs"
    inputs.mkdir(parents=True)
    gate = bundle / "confirmation-gate-result.json"
    gate.write_text('{"ready": true}', encoding="utf-8")
    matrix_sql = bundle / "01-dcc-matrix-confirmed.sql"
    product_sql = bundle / "02-dcc-product-group-confirmed.sql"
    matrix_sql.write_text("START TRANSACTION;\nUPDATE dcc_controlled_file SET category_id = 1;\nINSERT INTO system_user_role (user_id) VALUES (1);\nCOMMIT;\n", encoding="utf-8")
    product_sql.write_text("START TRANSACTION;\nINSERT INTO dcc_product_visibility_group (tenant_id) VALUES (1);\nCOMMIT;\n", encoding="utf-8")
    classification = inputs / "matrix-file-classification.csv"
    roles = inputs / "matrix-role-members.csv"
    products = inputs / "product-group-bindings.csv"
    classification.write_text("file_id,tenant_id,manual_confirm_category_code\n100,1,DCC_FVM_DMR_001\n", encoding="utf-8")
    roles.write_text("role_code,candidate_user_id,manual_confirm\ndcc_matrix_qc_lead,200,确认\n", encoding="utf-8")
    products.write_text("tenant_id,group_name,dept_id,user_id,product_master_id,manual_confirm\n1,新品A组,136,200,300,确认\n", encoding="utf-8")
    manifest = {
        "ready": True,
        "gateResult": str(gate),
        "matrixSql": str(matrix_sql),
        "productGroupSql": str(product_sql),
        "inputFiles": {
            "matrixClassificationCsv": str(classification),
            "matrixRoleCsv": str(roles),
            "productGroupCsv": str(products),
        },
        "executionOrder": ["01-dcc-matrix-confirmed.sql", "02-dcc-product-group-confirmed.sql"],
    }
    (bundle / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False), encoding="utf-8")
    return bundle


def run_verify(args):
    return subprocess.run(
        ["python", "-X", "utf8", str(SCRIPT), *args],
        cwd=ROOT,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=False,
    )


def test_print_sql_builds_read_only_verification_query(tmp_path):
    bundle = write_bundle(tmp_path)
    cp = run_verify(["--bundle-dir", str(bundle), "--print-sql"])

    assert cp.returncode == 0, cp.stderr
    assert "missing_files" in cp.stdout
    assert "dcc_controlled_file" in cp.stdout
    assert "dcc_product_visibility_group_member" in cp.stdout
    assert "UPDATE " not in cp.stdout.upper()
    assert "INSERT " not in cp.stdout.upper()


def test_fails_without_mysql_command_when_not_printing_sql(tmp_path):
    bundle = write_bundle(tmp_path)
    cp = run_verify(["--bundle-dir", str(bundle)])

    assert cp.returncode != 0
    assert "--mysql-command is required" in cp.stderr


def test_fails_when_bundle_manifest_missing(tmp_path):
    cp = run_verify(["--bundle-dir", str(tmp_path / "missing"), "--print-sql"])

    assert cp.returncode != 0
    assert "manifest is missing" in cp.stderr
