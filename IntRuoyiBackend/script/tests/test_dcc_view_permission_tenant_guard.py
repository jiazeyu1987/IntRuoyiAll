import json
import subprocess
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / "script" / "dcc_view_permission_tenant_guard.py"


def write_bundle(tmp_path, classification_tenant="122", product_tenant="122"):
    bundle = tmp_path / "bundle"
    inputs = bundle / "inputs"
    inputs.mkdir(parents=True)
    classification = inputs / "matrix-file-classification.csv"
    roles = inputs / "matrix-role-members.csv"
    product = inputs / "product-group-bindings.csv"
    classification.write_text(f"file_id,tenant_id,manual_confirm_category_code\n100,{classification_tenant},DCC_FVM_DMR_001\n", encoding="utf-8")
    roles.write_text("role_code,candidate_user_id,manual_confirm\ndcc_matrix_qc_lead,200,yes\n", encoding="utf-8")
    product.write_text(f"tenant_id,group_name,dept_id,user_id,product_master_id,manual_confirm\n{product_tenant},NPD,136,200,300,yes\n", encoding="utf-8")
    manifest = {
        "ready": True,
        "inputFiles": {
            "matrixClassificationCsv": str(classification),
            "matrixRoleCsv": str(roles),
            "productGroupCsv": str(product),
        },
    }
    (bundle / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False), encoding="utf-8")
    return bundle


def run_guard(bundle, tenant):
    output = Path(bundle).parent / "tenant-guard-result.json"
    return subprocess.run(
        [
            "python", "-X", "utf8", str(SCRIPT),
            "--bundle-dir", str(bundle),
            "--allowed-tenant-id", str(tenant),
            "--output-json", str(output),
        ],
        cwd=ROOT,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=False,
    )


def test_guard_accepts_matching_tenant(tmp_path):
    bundle = write_bundle(tmp_path, classification_tenant="122", product_tenant="122")
    cp = run_guard(bundle, 122)

    assert cp.returncode == 0, cp.stderr
    payload = json.loads(cp.stdout)
    assert payload["ready"] is True
    assert payload["actualTenantIds"] == ["122"]


def test_guard_rejects_non_matching_tenant(tmp_path):
    bundle = write_bundle(tmp_path, classification_tenant="122", product_tenant="1")
    cp = run_guard(bundle, 122)

    assert cp.returncode != 0
    assert "not allowed" in cp.stderr
    assert "allowed=122" in cp.stderr
    assert (tmp_path / "tenant-guard-result.json").exists()


def test_guard_fails_when_manifest_missing(tmp_path):
    cp = run_guard(tmp_path / "missing", 122)

    assert cp.returncode != 0
    assert "manifest is missing" in cp.stderr
