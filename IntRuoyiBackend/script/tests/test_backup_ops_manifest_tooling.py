from pathlib import Path
import json
import subprocess
import tempfile


def _backup_root() -> Path:
    return Path(__file__).resolve().parents[1] / "backup-ops"


def _run_powershell_script(script_text: str) -> subprocess.CompletedProcess[str]:
    with tempfile.NamedTemporaryFile(
        mode="w",
        suffix=".ps1",
        delete=False,
        encoding="utf-8-sig",
    ) as script_file:
        script_file.write(script_text)
        script_path = Path(script_file.name)

    try:
        return subprocess.run(
            [
                "powershell",
                "-NoProfile",
                "-ExecutionPolicy",
                "Bypass",
                "-File",
                str(script_path),
            ],
            capture_output=True,
            text=True,
            encoding="utf-8",
            errors="replace",
            check=False,
        )
    finally:
        script_path.unlink(missing_ok=True)


def _ps_literal(path: Path) -> str:
    return str(path).replace("'", "''")


def _write_dcc_backup_manifest(manifest_dir: Path, backup_id: str = "20260526-220000") -> None:
    manifest_dir.mkdir(parents=True, exist_ok=True)
    (manifest_dir / "dcc-backup-manifest.json").write_text(
        json.dumps(
            {
                "schemaVersion": "dcc-backup-manifest-v1",
                "backupId": backup_id,
                "targetEnvironment": "test",
                "targetHost": "172.30.30.58",
                "status": "success",
                "restoreVerified": False,
                "restoreRehearsal": {"status": "not-run"},
                "fullBaseline": {"restorePointId": "B1", "checksum": "sha256:" + "1" * 64},
                "incrementalChain": [],
                "restorePoints": [
                    {"id": "B1", "databaseRestorePointId": "B1", "objectInventoryRestorePointId": "B1"}
                ],
                "objectInventories": [{"restorePointId": "B1", "objects": []}],
                "databaseRecords": [],
                "dccEvents": [],
            },
            ensure_ascii=False,
            indent=2,
        )
        + "\n",
        encoding="utf-8",
    )


def test_manifest_model_carries_rehearsal_verification_state() -> None:
    text = (_backup_root() / "scripts" / "modules" / "Infra" / "ReportOps.psm1").read_text(encoding="utf-8")

    assert "rehearsalStatus" in text
    assert "lastRehearsedAt" in text


def test_restore_candidates_require_manifest_validation_and_no_longer_fallback_to_broken_manifest() -> None:
    text = (_backup_root() / "scripts" / "modules" / "Infra" / "DockerOps.psm1").read_text(encoding="utf-8")

    assert "mysqlDumpCreated" in text
    assert "objectBackupCreated" in text
    assert "checksumsGenerated" in text
    assert "pending-review" in text
    assert "Fallback to artifact metadata" not in text


def test_powershell_manifest_uses_runtime_env_ports(tmp_path: Path) -> None:
    root = _backup_root()
    report_module = root / "scripts" / "modules" / "Infra" / "ReportOps.psm1"
    file_module = root / "scripts" / "modules" / "Infra" / "FileOps.psm1"
    backup_root = tmp_path / "20260526-220000"
    deploy_path = backup_root / "deploy"
    manifest_path = backup_root / "manifest"
    mysql_path = backup_root / "mysql"
    objects_path = backup_root / "objects"
    deploy_path.mkdir(parents=True)
    manifest_path.mkdir(parents=True)
    mysql_path.mkdir(parents=True)
    objects_path.mkdir(parents=True)
    (deploy_path / "docker-compose.yml").write_text("services: {}\n", encoding="utf-8")
    (mysql_path / "ruoyi-vue-pro.sql.gz").write_bytes(b"dump")
    (objects_path / "manifest-object-inventory.json").write_text(
        '{"mode":"incremental-manifest","bucket":"yudao","objectStoreRoot":"/mnt/nas/Backup/BackupPackage/object-store","objects":[]}',
        encoding="utf-8",
    )
    (manifest_path / "checksums.txt").write_text("abc  deploy/runtime.env\n", encoding="utf-8")
    _write_dcc_backup_manifest(manifest_path)
    script = f"""
$ErrorActionPreference = 'Stop'
function Write-BackupOpsLog {{ param($Session, $Level, $Message) }}
Import-Module '{_ps_literal(report_module)}' -Force -DisableNameChecking
Import-Module '{_ps_literal(file_module)}' -Force -DisableNameChecking
[System.IO.File]::WriteAllText((Join-Path '{_ps_literal(deploy_path)}' 'runtime.env'), "IMAGE_TAG=release-current`nBACKEND_HOST_PORT=49123`nFRONTEND_HOST_PORT=18099`n", [System.Text.UTF8Encoding]::new($false))
$config = [pscustomobject]@{{
    environment = 'test'
    servers = [pscustomobject]@{{
        production = [pscustomobject]@{{
            host = '172.30.30.58'
            appDir = '/opt/intruoyi/runtime'
        }}
    }}
    backup = [pscustomobject]@{{
        mysqlDatabase = 'ruoyi-vue-pro'
        objectBucket = 'yudao'
    }}
}}
$workspace = [pscustomobject]@{{
    BackupId = '20260526-220000'
    BackupRoot = '{_ps_literal(backup_root)}'
    ImageTag = 'release-current'
    DeployPath = '{_ps_literal(deploy_path)}'
    ManifestPath = '{_ps_literal(manifest_path)}'
    MySqlPath = '{_ps_literal(mysql_path)}'
    ObjectsPath = '{_ps_literal(objects_path)}'
}}
$session = [pscustomobject]@{{ startedAt = [System.DateTimeOffset]::Parse('2026-05-26T22:00:00+08:00') }}
$path = New-BackupOpsManifest -Config $config -Workspace $workspace -BackupType 'manual' -LogSession $session
[System.IO.File]::ReadAllText($path, [System.Text.Encoding]::UTF8)
"""
    completed = _run_powershell_script(script)

    assert completed.returncode == 0, completed.stdout + completed.stderr
    manifest = json.loads(completed.stdout)
    assert manifest["deploy"]["backendPort"] == 49123
    assert manifest["deploy"]["frontendPort"] == 18099
    assert manifest["deploy"]["backendPort"] != 48081
    assert manifest["deploy"]["frontendPort"] != 8081


def test_powershell_manifest_fails_fast_when_runtime_port_missing(tmp_path: Path) -> None:
    root = _backup_root()
    report_module = root / "scripts" / "modules" / "Infra" / "ReportOps.psm1"
    file_module = root / "scripts" / "modules" / "Infra" / "FileOps.psm1"
    backup_root = tmp_path / "20260526-220000"
    deploy_path = backup_root / "deploy"
    manifest_path = backup_root / "manifest"
    mysql_path = backup_root / "mysql"
    objects_path = backup_root / "objects"
    deploy_path.mkdir(parents=True)
    manifest_path.mkdir(parents=True)
    mysql_path.mkdir(parents=True)
    objects_path.mkdir(parents=True)
    (deploy_path / "docker-compose.yml").write_text("services: {}\n", encoding="utf-8")
    (mysql_path / "ruoyi-vue-pro.sql.gz").write_bytes(b"dump")
    (objects_path / "manifest-object-inventory.json").write_text(
        '{"mode":"incremental-manifest","bucket":"yudao","objectStoreRoot":"/mnt/nas/Backup/BackupPackage/object-store","objects":[]}',
        encoding="utf-8",
    )
    (manifest_path / "checksums.txt").write_text("abc  deploy/runtime.env\n", encoding="utf-8")
    _write_dcc_backup_manifest(manifest_path)
    script = f"""
$ErrorActionPreference = 'Stop'
function Write-BackupOpsLog {{ param($Session, $Level, $Message) }}
Import-Module '{_ps_literal(report_module)}' -Force -DisableNameChecking
Import-Module '{_ps_literal(file_module)}' -Force -DisableNameChecking
[System.IO.File]::WriteAllText((Join-Path '{_ps_literal(deploy_path)}' 'runtime.env'), "IMAGE_TAG=release-current`nFRONTEND_HOST_PORT=18099`n", [System.Text.UTF8Encoding]::new($false))
$config = [pscustomobject]@{{
    environment = 'test'
    servers = [pscustomobject]@{{
        production = [pscustomobject]@{{
            host = '172.30.30.58'
            appDir = '/opt/intruoyi/runtime'
        }}
    }}
    backup = [pscustomobject]@{{
        objectBucket = 'yudao'
    }}
}}
$workspace = [pscustomobject]@{{
    BackupId = '20260526-220000'
    BackupRoot = '{_ps_literal(backup_root)}'
    ImageTag = 'release-current'
    DeployPath = '{_ps_literal(deploy_path)}'
    ManifestPath = '{_ps_literal(manifest_path)}'
    MySqlPath = '{_ps_literal(mysql_path)}'
    ObjectsPath = '{_ps_literal(objects_path)}'
}}
$session = [pscustomobject]@{{ startedAt = [System.DateTimeOffset]::Parse('2026-05-26T22:00:00+08:00') }}
try {{
    New-BackupOpsManifest -Config $config -Workspace $workspace -BackupType 'manual' -LogSession $session | Out-Null
    [pscustomobject]@{{
        succeeded = $true
        manifestExists = [System.IO.File]::Exists((Join-Path '{_ps_literal(manifest_path)}' 'manifest.json'))
    }} | ConvertTo-Json -Depth 8
}}
catch {{
    [pscustomobject]@{{
        succeeded = $false
        status = [string]$_.Exception.Data['BackupOpsStatus']
        code = [string]$_.Exception.Data['BackupOpsCode']
        message = $_.Exception.Message
        manifestExists = [System.IO.File]::Exists((Join-Path '{_ps_literal(manifest_path)}' 'manifest.json'))
    }} | ConvertTo-Json -Depth 8
}}
"""
    completed = _run_powershell_script(script)

    assert completed.returncode == 0, completed.stdout + completed.stderr
    payload = json.loads(completed.stdout)
    assert payload["succeeded"] is False
    assert payload["status"] == "blocked"
    assert "BACKEND_HOST_PORT" in payload["message"]
    assert payload["manifestExists"] is False
