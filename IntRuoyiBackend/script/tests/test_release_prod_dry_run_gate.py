import pytest

from script.release.release_prod_dry_run_gate import ProdDryRunGateError, validate_prod_dry_run_evidence


PUBLISH_SCRIPT = (
    __import__("pathlib").Path(__file__).resolve().parents[2] / "script" / "deploy" / "publish-int-ruoyi.ps1"
)


def evidence(**overrides: object) -> dict[str, object]:
    payload: dict[str, object] = {
        "status": "passed",
        "targetEnvironment": "prod",
        "releaseTag": "20260613",
        "mode": "preflight-release",
        "writeActions": [],
    }
    payload.update(overrides)
    return payload


def test_prod_deploy_requires_passed_dry_run_evidence() -> None:
    assert validate_prod_dry_run_evidence(evidence(), release_tag="20260613")["status"] == "passed"


@pytest.mark.parametrize(
    "payload, reason",
    [
        ({}, "missing prod dry-run evidence"),
        (evidence(status="blocked"), "must be passed"),
        (evidence(targetEnvironment="test"), "targetEnvironment must be prod"),
        (evidence(releaseTag="other"), "releaseTag does not match"),
        (evidence(writeActions=["sql"]), "must be read-only"),
    ],
)
def test_prod_deploy_blocks_invalid_dry_run_evidence(payload: dict[str, object], reason: str) -> None:
    with pytest.raises(ProdDryRunGateError, match=reason):
        validate_prod_dry_run_evidence(payload, release_tag="20260613")


def test_publish_script_requires_prod_dry_run_evidence_before_release_sql() -> None:
    text = PUBLISH_SCRIPT.read_text(encoding="utf-8")
    deploy_start = text.index("if ($Mode -eq 'deploy-release') {")
    deploy_end = text.index("Set-PublishRuntimeDefaultsForTarget", deploy_start)
    deploy_block = text[deploy_start:deploy_end]

    assert "[string]$ProdDryRunEvidencePath = ''" in text
    assert "function Assert-ProdDryRunEvidence" in text
    assert "Production deploy-release requires -ProdDryRunEvidencePath" in text
    assert "Production dry-run evidence status must be passed" in text
    assert "Production dry-run evidence targetEnvironment must be prod" in text
    assert "Production dry-run evidence releaseTag does not match deploy-release" in text
    assert "Production dry-run evidence must be read-only and include empty writeActions" in text
    assert deploy_block.index("Copy-ReleasePackageFromNas -PackageTag $ReleaseTag") < deploy_block.index(
        "Assert-ProdDryRunEvidence"
    ) < deploy_block.index("$requiredDatabaseSqlScripts = Get-ReleasePackageDatabaseSqlScripts")
