import json
import subprocess
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / "script" / "dcc_view_permission_sql_bundle_verify.py"


def write_bundle(tmp_path, matrix_sql, product_sql):
    bundle = tmp_path / "bundle"
    inputs = bundle / "inputs"
    inputs.mkdir(parents=True)
    gate = bundle / "confirmation-gate-result.json"
    gate.write_text('{"ready": true}', encoding="utf-8")
    matrix = bundle / "01-dcc-matrix-confirmed.sql"
    product = bundle / "02-dcc-product-group-confirmed.sql"
    matrix.write_text(matrix_sql, encoding="utf-8")
    product.write_text(product_sql, encoding="utf-8")
    for name in ("matrix-file-classification.csv", "matrix-role-members.csv", "product-group-bindings.csv"):
        (inputs / name).write_text("header\n", encoding="utf-8")
    manifest = {
        "ready": True,
        "gateResult": str(gate),
        "matrixSql": str(matrix),
        "productGroupSql": str(product),
        "inputFiles": {
            "matrixClassificationCsv": str(inputs / "matrix-file-classification.csv"),
            "matrixRoleCsv": str(inputs / "matrix-role-members.csv"),
            "productGroupCsv": str(inputs / "product-group-bindings.csv"),
        },
        "executionOrder": ["01-dcc-matrix-confirmed.sql", "02-dcc-product-group-confirmed.sql"],
    }
    (bundle / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False), encoding="utf-8")
    return bundle


def run_verify(bundle):
    return subprocess.run(
        ["python", "-X", "utf8", str(SCRIPT), "--bundle-dir", str(bundle)],
        cwd=ROOT,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=False,
    )


def test_verify_accepts_valid_bundle(tmp_path):
    bundle = write_bundle(
        tmp_path,
        "START TRANSACTION;\nINSERT INTO tmp_dcc_confirmed_role_member (role_code) VALUES ('x');\nUPDATE dcc_controlled_file SET category_id = 1;\nINSERT INTO system_user_role (user_id) VALUES (1);\nCOMMIT;\n",
        "START TRANSACTION;\nINSERT INTO dcc_product_visibility_group (tenant_id) VALUES (1);\nINSERT INTO dcc_product_visibility_group_member (tenant_id) VALUES (1);\nINSERT INTO dcc_product_visibility_group_product (tenant_id) VALUES (1);\nCOMMIT;\n",
    )

    cp = run_verify(bundle)

    assert cp.returncode == 0, cp.stderr
    payload = json.loads(cp.stdout)
    assert payload["ready"] is True
    assert len(payload["checkedSqlFiles"]) == 2


def test_verify_rejects_download_permission_change(tmp_path):
    bundle = write_bundle(
        tmp_path,
        "START TRANSACTION;\nUPDATE dcc_controlled_file SET can_download = 1;\nCOMMIT;\n",
        "START TRANSACTION;\nINSERT INTO dcc_product_visibility_group (tenant_id) VALUES (1);\nCOMMIT;\n",
    )

    cp = run_verify(bundle)

    assert cp.returncode != 0
    assert "Forbidden token" in cp.stderr
    assert "can_download" in cp.stderr


def test_verify_rejects_unauthorized_write_table(tmp_path):
    bundle = write_bundle(
        tmp_path,
        "START TRANSACTION;\nUPDATE system_users SET status = 0;\nCOMMIT;\n",
        "START TRANSACTION;\nINSERT INTO dcc_product_visibility_group (tenant_id) VALUES (1);\nCOMMIT;\n",
    )

    cp = run_verify(bundle)

    assert cp.returncode != 0
    assert "Unauthorized write table" in cp.stderr
    assert "system_users" in cp.stderr


def test_verify_fails_when_manifest_is_missing(tmp_path):
    cp = run_verify(tmp_path / "missing-bundle")

    assert cp.returncode != 0
    assert "manifest is missing" in cp.stderr
