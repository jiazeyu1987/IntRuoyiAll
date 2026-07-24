from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
PUBLISH_SCRIPT = ROOT / "script" / "deploy" / "publish-int-ruoyi.ps1"


def test_backup_deploy_temporarily_skips_tested_gate_with_explicit_warning() -> None:
    text = PUBLISH_SCRIPT.read_text(encoding="utf-8")

    assert "function Should-RequireNasReleaseTestedForDeploy" in text
    assert "$Environment -eq 'prod'" in text
    assert "TEMPORARY_BACKUP_RECOVERY_GATE_DISABLED" in text
    assert "Backup deploy recovery gate temporarily disabled by operator request" in text
    assert "if (Should-RequireNasReleaseTestedForDeploy)" in text


def test_prod_deploy_still_keeps_tested_gate() -> None:
    text = PUBLISH_SCRIPT.read_text(encoding="utf-8")
    gate_body = text.split("function Should-RequireNasReleaseTestedForDeploy", 1)[1].split("function Copy-ReleasePackageFromNas", 1)[0]

    assert "$RequireTested" in gate_body
    assert "if ($Environment -eq 'prod') {\n        return $true\n    }" in gate_body
    assert "if ($Environment -eq 'backup') {" in gate_body
    backup_branch = gate_body.split("if ($Environment -eq 'backup') {", 1)[1].split("}", 1)[0]
    assert "return $false" in backup_branch
