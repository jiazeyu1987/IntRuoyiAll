from pathlib import Path


def _backup_root() -> Path:
    return Path(__file__).resolve().parents[1] / "backup-ops"


def test_rehearsal_runtime_config_declares_isolated_ports_and_weekly_schedule() -> None:
    config_text = (_backup_root() / "config" / "backup-ops.config.json").read_text(encoding="utf-8")

    assert '"rehearsalBackendPort"' in config_text
    assert '"rehearsalFrontendPort"' in config_text
    assert '"rehearsal"' in config_text
    assert '"schedule"' in config_text
    assert '"validation"' in config_text


def test_rehearsal_runtime_no_longer_uses_blocked_stub_and_validates_login_and_sample_file() -> None:
    docker_text = (_backup_root() / "scripts" / "modules" / "Infra" / "DockerOps.psm1").read_text(encoding="utf-8")
    usecase_text = (_backup_root() / "scripts" / "modules" / "UseCases" / "Rehearsal.psm1").read_text(encoding="utf-8")

    assert "恢复演练的数据面恢复尚未完成对象恢复编排接线" not in docker_text
    assert "/admin-api/system/auth/login" in docker_text
    assert "sampleFilePath" in docker_text
    assert "登录可达" in usecase_text or "login" in usecase_text
    assert "文件抽样" in usecase_text
