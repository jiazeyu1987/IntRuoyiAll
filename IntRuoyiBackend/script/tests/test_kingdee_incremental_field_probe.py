from pathlib import Path
import json
import os
import subprocess
import sys


REPO_ROOT = Path(__file__).resolve().parents[2]
SCRIPT = REPO_ROOT / "script" / "erp" / "kingdee_incremental_field_probe.py"


def _run_probe(*args: str, env: dict[str, str] | None = None) -> subprocess.CompletedProcess[str]:
    run_env = os.environ.copy()
    run_env.pop("PRODUCTION_PLAN_ERP_K3CLOUD_BASE_URL", None)
    run_env.pop("PRODUCTION_PLAN_ERP_K3CLOUD_ACCT_ID", None)
    run_env.pop("PRODUCTION_PLAN_ERP_K3CLOUD_PASSWORD", None)
    if env:
        run_env.update(env)
    return subprocess.run(
        [sys.executable, str(SCRIPT), *args],
        cwd=REPO_ROOT,
        env=run_env,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        check=False,
    )


def test_probe_prints_plan_for_all_required_form_ids_without_credentials() -> None:
    result = _run_probe("--print-plan")

    assert result.returncode == 0
    plan = json.loads(result.stdout)
    form_ids = {item["form_id"] for item in plan["objects"]}
    assert form_ids == {
        "BD_MATERIAL",
        "STK_Inventory",
        "PUR_PurchaseOrder",
        "SAL_SaleOrder",
        "PRD_MO",
        "ENG_BOM",
    }
    for item in plan["objects"]:
        assert "FModifyDate" in item["field_keys"]
        assert item["source_key_fields"]
        assert item["status_fields"]


def test_probe_fails_fast_when_required_credentials_are_missing() -> None:
    result = _run_probe()

    assert result.returncode == 2
    assert "missing required environment variables" in result.stderr
    assert "PRODUCTION_PLAN_ERP_K3CLOUD_BASE_URL" in result.stderr
    assert "PRODUCTION_PLAN_ERP_K3CLOUD_ACCT_ID" in result.stderr
    assert "PRODUCTION_PLAN_ERP_K3CLOUD_PASSWORD" in result.stderr
    assert "贾泽宇" not in result.stderr


def test_probe_never_prints_secret_values_when_configuration_is_incomplete() -> None:
    result = _run_probe(
        env={
            "PRODUCTION_PLAN_ERP_K3CLOUD_BASE_URL": "https://kingdee.invalid",
            "PRODUCTION_PLAN_ERP_K3CLOUD_PASSWORD": "super-secret-value",
        }
    )

    assert result.returncode == 2
    assert "super-secret-value" not in result.stdout
    assert "super-secret-value" not in result.stderr
    assert "PRODUCTION_PLAN_ERP_K3CLOUD_ACCT_ID" in result.stderr
