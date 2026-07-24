import importlib.util
import json
from pathlib import Path


def _backup_root() -> Path:
    return Path(__file__).resolve().parents[1] / "backup-ops"


def _load_linux_module():
    module_path = _backup_root() / "linux" / "backup_ops_linux.py"
    spec = importlib.util.spec_from_file_location("backup_ops_linux", module_path)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module


def test_linux_runtime_entry_supports_rehearsal_mode() -> None:
    text = (_backup_root() / "linux" / "backup_ops_linux.py").read_text(encoding="utf-8")

    assert "rehearsal" in text


def test_linux_runtime_rehearsal_uses_isolated_runtime_and_validation_settings() -> None:
    config_text = (_backup_root() / "config" / "backup-ops.linux-local.example.json").read_text(encoding="utf-8")
    code_text = (_backup_root() / "linux" / "backup_ops_linux.py").read_text(encoding="utf-8")

    assert '"rehearsalRoot"' in config_text
    assert '"rehearsalBackendPort"' in config_text
    assert '"rehearsalFrontendPort"' in config_text
    assert '"bucket"' in config_text
    assert '"validation"' in config_text
    assert "admin-api/system/auth/login" in code_text
    assert "fileDownloadSample" in code_text or "sampleFilePath" in code_text


def test_linux_runtime_rehearsal_config_matches_allowed_runtime_root() -> None:
    config_files = [
        _backup_root() / "config" / "backup-ops.config.json",
        _backup_root() / "config" / "backup-ops.config.example.json",
        _backup_root() / "config" / "backup-ops.linux-local.example.json",
    ]

    for config_file in config_files:
        config = json.loads(config_file.read_text(encoding="utf-8"))
        rehearsal_root = config["servers"]["test"]["rehearsalRoot"]

        assert rehearsal_root.startswith("/backup/int-ruoyi/rehearsal/"), config_file


def test_rehearsal_mysql_wait_requires_docker_health_status() -> None:
    docker_ops_text = (_backup_root() / "scripts" / "modules" / "Infra" / "DockerOps.psm1").read_text(
        encoding="utf-8"
    )
    wait_start = docker_ops_text.index("function Wait-BackupOpsMySqlReady")
    wait_end = docker_ops_text.index("function Update-BackupOpsRehearsalFileMetadata")
    wait_body = docker_ops_text[wait_start:wait_end]

    assert "docker inspect --format" in wait_body
    assert ".State.Health.Status" in wait_body
    assert "healthy" in wait_body
    assert "Merge-BackupOpsDockerRequest" in wait_body
    assert "Merge-BackupOpsRequest" not in wait_body
    assert wait_body.index(".State.Health.Status") < wait_body.index("Test-BackupMySqlConnectivity")


def test_linux_runtime_rehearsal_uses_tcp_mysql_for_readiness_and_import() -> None:
    code_text = (_backup_root() / "linux" / "backup_ops_linux.py").read_text(encoding="utf-8")
    rehearsal_start = code_text.index("def rehearsal(")
    rehearsal_end = code_text.index("def main(")
    rehearsal_body = code_text[rehearsal_start:rehearsal_end]

    assert "docker inspect --format" in rehearsal_body
    assert "{{.State.Health.Status}}" in rehearsal_body
    assert 'health.stdout.strip() == "healthy"' in rehearsal_body
    assert "mysql -h127.0.0.1 -uroot" in rehearsal_body
    assert "docker exec -i {0} mysql -uroot" not in rehearsal_body
    assert "docker exec {0} mysql -uroot" not in rehearsal_body


def test_rehearsal_frontend_backend_start_does_not_start_compose_dependencies() -> None:
    docker_ops_text = (_backup_root() / "scripts" / "modules" / "Infra" / "DockerOps.psm1").read_text(
        encoding="utf-8"
    )
    start_start = docker_ops_text.index("function Start-BackupOpsFrontendBackend")
    start_end = docker_ops_text.index("function Test-BackupOpsRestoreValidation")
    start_body = docker_ops_text[start_start:start_end]

    assert "NoDeps = $true" in start_body
    assert "docker compose up -d backend frontend" not in start_body


def test_linux_runtime_rehearsal_writes_manifest_evidence_after_success(tmp_path: Path) -> None:
    module = _load_linux_module()
    backup_id = "20260526-203000"
    backup_root = tmp_path / backup_id
    manifest_dir = backup_root / "manifest"
    manifest_dir.mkdir(parents=True)
    (manifest_dir / "manifest.json").write_text(
        json.dumps(
            {
                "backupId": backup_id,
                "status": "success",
                "validation": {
                    "mysqlDumpCreated": True,
                    "objectBackupCreated": True,
                    "checksumsGenerated": True,
                    "rehearsalStatus": "unverified",
                    "lastRehearsedAt": None,
                },
            },
            ensure_ascii=False,
        ),
        encoding="utf-8",
    )

    verified_at = "2026-05-26T20:30:00"
    checks = {
        "backendHealth": "pass",
        "frontendHttp200": "pass",
        "loginReachable": "pass",
        "fileDownloadSample": "pass",
    }
    module.write_rehearsal_evidence(backup_root, backup_id, "PASSED", verified_at, checks)

    manifest = json.loads((manifest_dir / "manifest.json").read_text(encoding="utf-8"))
    report = json.loads((manifest_dir / "rehearsal-report.json").read_text(encoding="utf-8"))
    snapshot = (manifest_dir / "现场快照.md").read_text(encoding="utf-8")

    assert manifest["validation"]["rehearsalStatus"] == "PASSED"
    assert manifest["validation"]["lastRehearsedAt"] == verified_at
    assert manifest["validation"]["rehearsalChecks"] == checks
    assert report["status"] == "PASSED"
    assert report["verifiedAt"] == verified_at
    assert f"备份点: {backup_id}" in snapshot
    assert "fileDownloadSample: pass" in snapshot


def test_linux_runtime_rehearsal_evidence_rejects_unknown_status(tmp_path: Path) -> None:
    module = _load_linux_module()
    backup_root = tmp_path / "20260526-203000"
    manifest_dir = backup_root / "manifest"
    manifest_dir.mkdir(parents=True)
    (manifest_dir / "manifest.json").write_text('{"validation": {}}', encoding="utf-8")

    try:
        module.write_rehearsal_evidence(backup_root, "20260526-203000", "UNKNOWN", "2026-05-26T20:30:00", {})
    except module.BackupOpsError as exc:
        assert exc.code == "INTBK-7002"
        assert exc.status == "fail"
    else:
        raise AssertionError("unknown rehearsal status must fail fast")
