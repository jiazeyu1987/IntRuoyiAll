from pathlib import Path
from http.server import BaseHTTPRequestHandler, HTTPServer
import base64
import json
import re
import subprocess
import tempfile
import threading


def _backup_root() -> Path:
    return Path(__file__).resolve().parents[1] / "backup-ops"


def _run_backup_ops_mode(mode: str, *extra_args: str) -> subprocess.CompletedProcess[str]:
    root = _backup_root()
    script = root / "scripts" / "backup-ops.ps1"
    config_example = root / "config" / "backup-ops.config.example.json"
    with tempfile.TemporaryDirectory() as temp_dir:
        config = Path(temp_dir) / "backup-ops.config.test.json"
        secrets = Path(temp_dir) / "backup-ops.secrets.test.json"
        config_object = json.loads(config_example.read_text(encoding="utf-8"))
        config_object.setdefault("rehearsal", {})
        config_object["rehearsal"].setdefault("tenantName", "rehearsal-tenant")
        config_object["rehearsal"].setdefault("username", "rehearsal-admin")
        config_object["rehearsal"].setdefault("password", "not-a-real-password")
        config.write_text(
            json.dumps(config_object, ensure_ascii=False, indent=2),
            encoding="utf-8",
        )
        secrets.write_text(
            json.dumps(
                {
                    "ssh": {"user": "root", "port": 22},
                    "auth": {
                        "sshKeyPath": str(Path(temp_dir) / "missing_id_rsa"),
                        "knownHostsPath": str(Path(temp_dir) / "missing_known_hosts"),
                    },
                    "rehearsal": {
                        "tenantName": "rehearsal-tenant",
                        "username": "rehearsal-admin",
                        "password": "not-a-real-password",
                    },
                },
                ensure_ascii=False,
                indent=2,
            ),
            encoding="utf-8",
        )

        return subprocess.run(
            [
                "powershell",
                "-NoProfile",
                "-ExecutionPolicy",
                "Bypass",
                "-File",
                str(script),
                "-Mode",
                mode,
                "-ConfigPath",
                str(config),
                "-SecretsPath",
                str(secrets),
                *extra_args,
            ],
            capture_output=True,
            text=True,
            encoding="utf-8",
            errors="replace",
            check=False,
        )


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


def _valid_dcc_backup_manifest(
    backup_id: str = "20260520-010203",
    *,
    target_environment: str = "test",
    target_host: str = "172.30.30.58",
) -> dict[str, object]:
    return {
        "schemaVersion": "dcc-backup-manifest-v1",
        "backupId": backup_id,
        "targetEnvironment": target_environment,
        "targetHost": target_host,
        "status": "success",
        "chainStatus": "COMPLETE",
        "backupMode": "full",
        "baselineBackupId": backup_id,
        "baselineRestorePointId": "B1",
        "previousBackupId": "",
        "previousRestorePointId": "",
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
    }


def _invalid_deleted_dcc_backup_manifest(backup_id: str = "20260520-010203") -> dict[str, object]:
    manifest = _valid_dcc_backup_manifest(backup_id)
    manifest["incrementalChain"] = [
        {"from": "B1", "to": "B2", "checksum": "sha256:" + "2" * 64}
    ]
    manifest["restorePoints"] = [
        {"id": "B1", "databaseRestorePointId": "B1", "objectInventoryRestorePointId": "B1"},
        {"id": "B2", "databaseRestorePointId": "B2", "objectInventoryRestorePointId": "B2"},
    ]
    manifest["objectInventories"] = [
        {"restorePointId": "B1", "objects": []},
        {
            "restorePointId": "B2",
            "objects": [
                {
                    "fileKey": "DCC-B",
                    "state": "deleted",
                    "contentHash": "sha256:" + "b" * 64,
                    "storedHash": "sha256:" + "b" * 64,
                    "present": False,
                }
            ],
        },
    ]
    manifest["databaseRecords"] = [
        {"restorePointId": "B2", "fileKey": "DCC-B", "state": "deleted"}
    ]
    manifest["dccEvents"] = []
    return manifest


def _dcc_backup_manifest_json(manifest: dict[str, object] | None = None) -> str:
    return json.dumps(manifest or _valid_dcc_backup_manifest(), ensure_ascii=False)


def _write_dcc_backup_manifest(
    manifest_dir: Path,
    backup_id: str = "20260520-010203",
    *,
    target_environment: str = "test",
    target_host: str = "172.30.30.58",
) -> None:
    manifest_dir.mkdir(parents=True, exist_ok=True)
    (manifest_dir / "dcc-backup-manifest.json").write_text(
        json.dumps(
            _valid_dcc_backup_manifest(
                backup_id,
                target_environment=target_environment,
                target_host=target_host,
            ),
            ensure_ascii=False,
            indent=2,
        )
        + "\n",
        encoding="utf-8",
    )


def _latest_report_after_run(pattern: str, before: set[Path]) -> Path:
    reports_root = Path(r"D:\IntRuoyi-BackupOps\logs")
    after = set(reports_root.rglob(pattern))
    new_reports = sorted(after - before)
    if new_reports:
        return max(new_reports, key=lambda path: path.stat().st_mtime_ns)

    matches = sorted(after)
    assert matches, f"expected a report matching {pattern!r} to be emitted"
    return max(matches, key=lambda path: path.stat().st_mtime_ns)


def test_backup_ops_launcher_and_actions_exist() -> None:
    root = _backup_root()

    assert (root / "00-备份恢复控制台.bat").exists()
    assert (root / "actions" / "01-立即备份.bat").exists()
    assert (root / "actions" / "02-回滚应用版本.bat").exists()
    assert (root / "actions" / "03-恢复数据.bat").exists()


def test_backup_ops_powershell_entry_and_module_layout_exist() -> None:
    root = _backup_root()
    scripts_root = root / "scripts"
    modules_root = scripts_root / "modules"

    assert (scripts_root / "backup-ops.ps1").exists()

    expected_modules = [
        modules_root / "Core" / "Config.psm1",
        modules_root / "Core" / "Logging.psm1",
        modules_root / "Core" / "ResultModel.psm1",
        modules_root / "Core" / "Validation.psm1",
        modules_root / "Infra" / "SshOps.psm1",
        modules_root / "Infra" / "DockerOps.psm1",
        modules_root / "Infra" / "MySqlOps.psm1",
        modules_root / "Infra" / "ObjectOps.psm1",
        modules_root / "Infra" / "FileOps.psm1",
        modules_root / "Infra" / "ReportOps.psm1",
        modules_root / "Infra" / "NotifyOps.psm1",
        modules_root / "UseCases" / "BackupNow.psm1",
        modules_root / "UseCases" / "BackupScheduled.psm1",
        modules_root / "UseCases" / "RollbackApp.psm1",
        modules_root / "UseCases" / "RestoreData.psm1",
        modules_root / "UseCases" / "Rehearsal.psm1",
        modules_root / "Ui" / "ConsoleMenu.psm1",
        modules_root / "Ui" / "ConsolePrompt.psm1",
    ]

    missing = [str(path) for path in expected_modules if not path.exists()]
    assert not missing, "missing backup ops modules: " + ", ".join(missing)


def test_backup_ops_example_configs_exist_without_inline_secrets() -> None:
    root = _backup_root()
    config_path = root / "config" / "backup-ops.config.example.json"
    secrets_path = root / "config" / "backup-ops.secrets.example.json"

    assert config_path.exists()
    assert secrets_path.exists()

    config_text = config_path.read_text(encoding="utf-8")
    secrets_text = secrets_path.read_text(encoding="utf-8")

    assert '"environment": "production"' in config_text
    assert '"production"' in config_text
    assert '"test"' in config_text
    assert '"mysql": "intruoyi-mysql"' in config_text
    assert '"objectBucket": "yudao"' in config_text
    assert '"keepDaysRemote": 30' in config_text
    assert '"keepDaysLocal": 3' in config_text
    assert '"backupRoot": "/mnt/nas/Backup/BackupPackage"' in config_text
    assert '"backupPointsRoot": "/mnt/nas/Backup/BackupPackage"' in config_text
    assert "/mnt/nas/int-ruoyi/backups" not in config_text

    assert '"sshKeyPath"' in secrets_text
    assert "MYSQL_ROOT_PASSWORD" not in secrets_text
    assert "MINIO_ROOT_PASSWORD" not in secrets_text
    assert "123456" not in secrets_text


def test_backup_ops_config_declares_release_package_root_for_rollback() -> None:
    config = json.loads((_backup_root() / "config" / "backup-ops.config.example.json").read_text(encoding="utf-8"))

    assert config["servers"]["test"]["releasePackagesRoot"] == "/mnt/nas/Backup/ReleasePackage"
    assert config["servers"]["backup"]["releasePackagesRoot"] == "/mnt/nas/Backup/ReleasePackage"


def test_mysql_restore_command_spec_streams_remote_backup_package_dump() -> None:
    root = _backup_root()
    mysql_module = root / "scripts" / "modules" / "Infra" / "MySqlOps.psm1"
    remote_dump_path = "/mnt/nas/Backup/BackupPackage/20260608-031706/mysql/ruoyi-vue-pro.sql.gz"

    script = f"""
$ErrorActionPreference = 'Stop'
Import-Module '{mysql_module}' -Force -DisableNameChecking
$spec = New-BackupMySqlRestoreCommandSpec -Request @{{
    ContainerName = 'intruoyi-mysql'
    DatabaseName = 'ruoyi-vue-pro'
    RemoteDumpPath = '{remote_dump_path}'
    RootPassword = 'secret'
}}
$spec | ConvertTo-Json -Depth 8
"""
    completed = _run_powershell_script(script)
    assert completed.returncode == 0, completed.stdout + completed.stderr
    spec = json.loads(completed.stdout)

    assert spec["remoteDumpPath"] == remote_dump_path
    assert spec["importCommand"].startswith("gzip -dc ")
    assert "/mnt/nas/Backup/BackupPackage/" in spec["importCommand"]
    assert "/opt/intruoyi/ops/backup/tmp" not in spec["importCommand"]
    assert "remoteStageDir" not in spec


def test_mysql_restore_does_not_download_or_upload_dump_to_runtime_tmp() -> None:
    mysql_ops = (_backup_root() / "scripts" / "modules" / "Infra" / "MySqlOps.psm1").read_text(encoding="utf-8")
    import_wrapper = mysql_ops.split("function Import-BackupOpsMySqlDump", 1)[1].split("Export-ModuleMember", 1)[0]
    import_impl = mysql_ops.split("function Import-BackupMySqlDump", 1)[1].split("function Export-BackupOpsMySqlDump", 1)[0]

    assert "Receive-BackupFileOverSsh" not in import_wrapper
    assert "Send-BackupFileOverSsh" not in import_impl
    assert "RemoteDumpPath = $testDumpPath" in import_wrapper
    assert "$productionSshRequest.Host -ne [string]$testSshRequest.Host" in import_wrapper


def test_mysql_dump_command_spec_writes_directly_to_remote_backup_package() -> None:
    root = _backup_root()
    mysql_module = root / "scripts" / "modules" / "Infra" / "MySqlOps.psm1"
    remote_dump_path = "/mnt/nas/Backup/BackupPackage/20260608-121507/mysql/ruoyi-vue-pro.sql.gz"

    script = f"""
$ErrorActionPreference = 'Stop'
Import-Module '{mysql_module}' -Force -DisableNameChecking
$spec = New-BackupMySqlDumpCommandSpec -Request @{{
    ContainerName = 'intruoyi-mysql'
    DatabaseName = 'ruoyi-vue-pro'
    RemoteDumpPath = '{remote_dump_path}'
    RootPassword = 'secret'
}}
$spec | ConvertTo-Json -Depth 8
"""
    completed = _run_powershell_script(script)
    assert completed.returncode == 0, completed.stdout + completed.stderr
    spec = json.loads(completed.stdout)

    assert spec["remoteDumpPath"] == remote_dump_path
    assert spec["remoteCommand"].startswith("bash -lc ")
    assert "mkdir -p " in spec["remoteCommand"]
    assert "/mnt/nas/Backup/BackupPackage/20260608-121507/mysql" in spec["remoteCommand"]
    assert "mysqldump" in spec["remoteCommand"]
    assert "> " in spec["remoteCommand"]
    assert remote_dump_path in spec["remoteCommand"]
    assert "test -s " in spec["remoteCommand"]
    assert spec["commandPreview"].endswith(f"> {remote_dump_path}")


def test_backup_mysql_export_records_remote_nas_dump_path_for_test_target() -> None:
    mysql_ops = (_backup_root() / "scripts" / "modules" / "Infra" / "MySqlOps.psm1").read_text(encoding="utf-8")
    export_wrapper = mysql_ops.split("function Export-BackupOpsMySqlDump", 1)[1].split(
        "function Import-BackupOpsMySqlDump", 1
    )[0]

    assert "Assert-BackupOpsMySqlBackupSourceHost" in mysql_ops
    assert "'production' { return '172.30.30.57' }" in mysql_ops
    assert "servers', 'test', 'backupPointsRoot'" in export_wrapper
    assert "RemoteDumpPath = $remoteDumpPath" in export_wrapper
    assert "Workspace | Add-Member" in export_wrapper
    assert "RemoteMySqlDumpPath" in export_wrapper
    assert "Assert-BackupOpsMySqlBackupSourceHost -Environment $targetEnvironment -Host ([string]$sshRequest.Host)" in export_wrapper
    assert "Assert-BackupOpsMySqlBackupRepositoryHost -Host ([string]$testSshRequest.Host)" in export_wrapper
    assert "OutputPath = $outputPath" not in export_wrapper


def test_mysql_dump_and_restore_use_explicit_long_ssh_timeouts() -> None:
    mysql_ops = (_backup_root() / "scripts" / "modules" / "Infra" / "MySqlOps.psm1").read_text(encoding="utf-8")
    export_dump = mysql_ops.split("function Export-BackupMySqlDump", 1)[1].split(
        "function Import-BackupMySqlDump", 1
    )[0]
    import_dump = mysql_ops.split("function Import-BackupMySqlDump", 1)[1].split(
        "function Export-BackupOpsMySqlDump", 1
    )[0]

    assert "TimeoutSeconds = 7200" in export_dump
    assert "Command = $restoreSpec.importCommand" in import_dump
    assert "TimeoutSeconds = 7200" in import_dump


def test_restore_helpers_hard_reject_production_server_ip() -> None:
    mysql_ops = (_backup_root() / "scripts" / "modules" / "Infra" / "MySqlOps.psm1").read_text(encoding="utf-8")
    object_ops = (_backup_root() / "scripts" / "modules" / "Infra" / "ObjectOps.psm1").read_text(encoding="utf-8")

    assert "172.30.30.57" in mysql_ops
    assert "172.30.30.58" in mysql_ops
    assert "MySQL 恢复${Label}禁止指向正式服务器 172.30.30.57" in mysql_ops
    assert "恢复目标只能是测试服务器 172.30.30.58" in mysql_ops
    assert "172.30.30.57" in object_ops
    assert "172.30.30.58" in object_ops
    assert "对象恢复${Label}禁止指向正式服务器 172.30.30.57" in object_ops
    assert "恢复目标只能是测试服务器 172.30.30.58" in object_ops


def test_rollback_tag_scan_requires_complete_compatibility_evidence_contract() -> None:
    docker_ops = (_backup_root() / "scripts" / "modules" / "Infra" / "DockerOps.psm1").read_text(encoding="utf-8")
    rollback_body = docker_ops.split("function Get-BackupOpsRollbackTags", 1)[1].split(
        "function Save-BackupOpsRuntimeEnvBackup", 1
    )[0]

    assert "rollback-compatibility.json is missing packageDirectoryName" in rollback_body
    assert "rollback-compatibility.json is missing checkedAt" in rollback_body
    assert "rollback-compatibility.json is missing summary" in rollback_body
    assert "rollback-compatibility packageDirectoryName differs" in rollback_body


def test_backup_ops_manifest_declares_complete_recovery_set() -> None:
    root = _backup_root()
    report_module = root / "scripts" / "modules" / "Infra" / "ReportOps.psm1"
    file_module = root / "scripts" / "modules" / "Infra" / "FileOps.psm1"

    with tempfile.TemporaryDirectory() as temp_dir:
        backup_root = Path(temp_dir) / "20260520-010203"
        deploy = backup_root / "deploy"
        manifest = backup_root / "manifest"
        mysql = backup_root / "mysql"
        objects = backup_root / "objects"
        for directory in (deploy, manifest, mysql, objects):
            directory.mkdir(parents=True)
        (deploy / "image-tag.txt").write_text("release-v2\n", encoding="utf-8")
        (deploy / "runtime.env").write_text(
            "IMAGE_TAG=release-v2\nBACKEND_HOST_PORT=48081\nFRONTEND_HOST_PORT=8081\n",
            encoding="utf-8",
        )
        (deploy / "docker-compose.yml").write_text("services: {}\n", encoding="utf-8")
        (mysql / "ruoyi-vue-pro.sql.gz").write_bytes(b"dump")
        (manifest / "checksums.txt").write_text("abc  deploy/runtime.env\n", encoding="utf-8")
        _write_dcc_backup_manifest(manifest)
        (objects / "manifest-object-inventory.json").write_text(
            json.dumps(
                {
                    "mode": "incremental-manifest",
                    "bucket": "yudao",
                    "objectStoreRoot": "/mnt/nas/Backup/BackupPackage/object-store",
                    "stats": {
                        "addedCount": 1,
                        "modifiedCount": 0,
                        "deletedCount": 0,
                        "reusedCount": 0,
                    },
                    "objects": [
                        {
                            "path": "dcc/A.txt",
                            "sha256": "sha-a",
                            "status": "active",
                            "repositoryKey": "sha-a",
                        }
                    ],
                },
                ensure_ascii=False,
            ),
            encoding="utf-8",
        )

        script = f"""
$ErrorActionPreference = 'Stop'
function Write-BackupOpsLog {{ param($Session, $Level, $Message) }}
Import-Module '{report_module}' -Force -DisableNameChecking
Import-Module '{file_module}' -Force -DisableNameChecking
$config = [pscustomobject]@{{
    environment = 'test'
    servers = [pscustomobject]@{{
        production = [pscustomobject]@{{ host = '172.30.30.58'; appDir = '/opt/intruoyi/runtime' }}
    }}
    backup = [pscustomobject]@{{
        mysqlDatabase = 'ruoyi-vue-pro'
        objectBucket = 'yudao'
        keepDaysRemote = 30
        keepLastPoints = 5
        maxNasUsedPercent = 90
    }}
}}
$workspace = [pscustomobject]@{{
    BackupId = '20260520-010203'
    BackupRoot = '{backup_root}'
    ImageTag = 'release-v2'
    DeployPath = '{deploy}'
    ManifestPath = '{manifest}'
    MySqlPath = '{mysql}'
    ObjectsPath = '{backup_root / "objects"}'
}}
$logSession = [pscustomobject]@{{ startedAt = [System.DateTimeOffset]::Now }}
$path = New-BackupOpsManifest -Config $config -Workspace $workspace -BackupType 'manual' -Status 'success' -Validation @{{
    mysqlDumpCreated = $true
    objectBackupCreated = $true
    checksumsGenerated = $true
    syncedToTestServer = $true
}} -OperatorName 'tester' -LogSession $logSession
[System.IO.File]::ReadAllText($path, [System.Text.UTF8Encoding]::new($false))
"""
        completed = _run_powershell_script(script)
        assert completed.returncode == 0, completed.stdout + completed.stderr
        payload = json.loads(completed.stdout)

    recovery_set = payload["recoverySet"]
    assert payload["schemaVersion"] == "v2"
    assert payload["targetEnvironment"] == "test"
    assert payload["targetHost"] == "172.30.30.58"
    assert payload["imageTag"] == "release-v2"
    assert recovery_set["id"] == "20260520-010203"
    assert recovery_set["status"] == "COMPLETE"
    assert recovery_set["program"]["imageTag"] == "release-v2"
    assert recovery_set["mysql"]["dumpPath"] == "mysql/ruoyi-vue-pro.sql.gz"
    assert recovery_set["minio"]["snapshotPath"] == "objects/manifest-object-inventory.json"
    assert recovery_set["businessFiles"]["snapshotPath"] == "objects/manifest-object-inventory.json"
    assert recovery_set["dcc"]["manifestPath"] == "manifest/dcc-backup-manifest.json"
    assert recovery_set["redis"]["policy"] == "CLEAR_AND_REBUILD"
    assert recovery_set["configuration"]["manifestPath"] == "deploy/runtime.env"
    assert recovery_set["checksums"]["sha256"]
    assert payload["artifacts"]["dccBackupManifest"] == "manifest/dcc-backup-manifest.json"
    assert payload["backupStrategy"]["mode"] == "incremental-manifest"
    assert payload["backupStrategy"]["mysqlBaseline"] == "full-dump"
    assert payload["backupStrategy"]["mysqlIncrementalPlan"]["binlog"]["status"] == "requires-prerequisite"
    assert payload["backupStrategy"]["mysqlIncrementalPlan"]["xtrabackup"]["status"] == "requires-prerequisite"
    assert "silent full dump fallback" in payload["backupStrategy"]["mysqlIncrementalPlan"]["noFallbackRule"]
    assert payload["retentionPolicy"]["keepDays"] == 30
    assert payload["retentionPolicy"]["keepLast"] == 5
    assert payload["retentionPolicy"]["maxNasUsedPercent"] == 90
    assert payload["objectDeltaStats"]["addedCount"] == 1
    assert payload["objects"][0]["repositoryKey"] == "sha-a"


def test_backup_ops_manifest_blocks_mysql_incremental_request_without_prerequisites() -> None:
    root = _backup_root()
    report_module = root / "scripts" / "modules" / "Infra" / "ReportOps.psm1"
    file_module = root / "scripts" / "modules" / "Infra" / "FileOps.psm1"

    with tempfile.TemporaryDirectory() as temp_dir:
        backup_root = Path(temp_dir) / "20260520-010203"
        deploy = backup_root / "deploy"
        manifest = backup_root / "manifest"
        mysql = backup_root / "mysql"
        objects = backup_root / "objects"
        for directory in (deploy, manifest, mysql, objects):
            directory.mkdir(parents=True)
        (deploy / "image-tag.txt").write_text("release-v2\n", encoding="utf-8")
        (deploy / "runtime.env").write_text(
            "IMAGE_TAG=release-v2\nBACKEND_HOST_PORT=48081\nFRONTEND_HOST_PORT=8081\n",
            encoding="utf-8",
        )
        (deploy / "docker-compose.yml").write_text("services: {}\n", encoding="utf-8")
        (mysql / "ruoyi-vue-pro.sql.gz").write_bytes(b"dump")
        (manifest / "checksums.txt").write_text("abc  deploy/runtime.env\n", encoding="utf-8")
        _write_dcc_backup_manifest(manifest)
        (objects / "manifest-object-inventory.json").write_text(
            json.dumps(
                {
                    "mode": "incremental-manifest",
                    "bucket": "yudao",
                    "objectStoreRoot": "/mnt/nas/Backup/BackupPackage/object-store",
                    "objects": [],
                },
                ensure_ascii=False,
            ),
            encoding="utf-8",
        )

        script = f"""
$ErrorActionPreference = 'Stop'
function Write-BackupOpsLog {{ param($Session, $Level, $Message) }}
Import-Module '{report_module}' -Force -DisableNameChecking
Import-Module '{file_module}' -Force -DisableNameChecking
$config = [pscustomobject]@{{
    environment = 'test'
    servers = [pscustomobject]@{{
        production = [pscustomobject]@{{ host = '172.30.30.58'; appDir = '/opt/intruoyi/runtime' }}
    }}
    backup = [pscustomobject]@{{
        mysqlDatabase = 'ruoyi-vue-pro'
        objectBucket = 'yudao'
        mysqlBackupMode = 'binlog-incremental'
    }}
}}
$workspace = [pscustomobject]@{{
    BackupId = '20260520-010203'
    BackupRoot = '{backup_root}'
    ImageTag = 'release-v2'
    DeployPath = '{deploy}'
    ManifestPath = '{manifest}'
    MySqlPath = '{mysql}'
    ObjectsPath = '{backup_root / "objects"}'
}}
$logSession = [pscustomobject]@{{ startedAt = [System.DateTimeOffset]::Now }}
New-BackupOpsManifest -Config $config -Workspace $workspace -BackupType 'manual' -Status 'success' -Validation @{{
    mysqlDumpCreated = $true
    objectBackupCreated = $true
    checksumsGenerated = $true
    syncedToTestServer = $true
}} -OperatorName 'tester' -LogSession $logSession
"""
        completed = _run_powershell_script(script)

    output = completed.stdout + completed.stderr
    assert completed.returncode != 0, output
    assert "mysqlBackupMode=binlog-incremental" in output
    assert "No silent full dump fallback" in output


def test_backup_ops_manifest_blocks_success_without_dcc_backup_manifest() -> None:
    root = _backup_root()
    report_module = root / "scripts" / "modules" / "Infra" / "ReportOps.psm1"
    file_module = root / "scripts" / "modules" / "Infra" / "FileOps.psm1"

    with tempfile.TemporaryDirectory() as temp_dir:
        backup_root = Path(temp_dir) / "20260520-010203"
        deploy = backup_root / "deploy"
        manifest = backup_root / "manifest"
        mysql = backup_root / "mysql"
        objects = backup_root / "objects"
        for directory in (deploy, manifest, mysql, objects):
            directory.mkdir(parents=True)
        (deploy / "runtime.env").write_text(
            "IMAGE_TAG=release-v2\nBACKEND_HOST_PORT=48081\nFRONTEND_HOST_PORT=8081\n",
            encoding="utf-8",
        )
        (deploy / "docker-compose.yml").write_text("services: {}\n", encoding="utf-8")
        (mysql / "ruoyi-vue-pro.sql.gz").write_bytes(b"dump")
        (manifest / "checksums.txt").write_text("abc  deploy/runtime.env\n", encoding="utf-8")
        (objects / "manifest-object-inventory.json").write_text(
            json.dumps(
                {
                    "mode": "incremental-manifest",
                    "bucket": "yudao",
                    "objectStoreRoot": "/mnt/nas/Backup/BackupPackage/object-store",
                    "objects": [],
                },
                ensure_ascii=False,
            ),
            encoding="utf-8",
        )

        script = f"""
$ErrorActionPreference = 'Stop'
function Write-BackupOpsLog {{ param($Session, $Level, $Message) }}
Import-Module '{report_module}' -Force -DisableNameChecking
Import-Module '{file_module}' -Force -DisableNameChecking
$config = [pscustomobject]@{{
    environment = 'test'
    servers = [pscustomobject]@{{
        production = [pscustomobject]@{{ host = '172.30.30.58'; appDir = '/opt/intruoyi/runtime' }}
    }}
    backup = [pscustomobject]@{{
        mysqlDatabase = 'ruoyi-vue-pro'
        objectBucket = 'yudao'
    }}
}}
$workspace = [pscustomobject]@{{
    BackupId = '20260520-010203'
    BackupRoot = '{backup_root}'
    ImageTag = 'release-v2'
    DeployPath = '{deploy}'
    ManifestPath = '{manifest}'
    MySqlPath = '{mysql}'
    ObjectsPath = '{objects}'
}}
$logSession = [pscustomobject]@{{ startedAt = [System.DateTimeOffset]::Now }}
try {{
    New-BackupOpsManifest -Config $config -Workspace $workspace -BackupType 'manual' -Status 'success' -Validation @{{
        mysqlDumpCreated = $true
        objectBackupCreated = $true
        checksumsGenerated = $true
        syncedToTestServer = $true
    }} -OperatorName 'tester' -LogSession $logSession | Out-Null
    [pscustomobject]@{{
        succeeded = $true
        manifestExists = [System.IO.File]::Exists((Join-Path '{manifest}' 'manifest.json'))
    }} | ConvertTo-Json -Depth 8
}} catch {{
    [pscustomobject]@{{
        succeeded = $false
        status = [string]$_.Exception.Data['BackupOpsStatus']
        code = [string]$_.Exception.Data['BackupOpsCode']
        message = $_.Exception.Message
        manifestExists = [System.IO.File]::Exists((Join-Path '{manifest}' 'manifest.json'))
    }} | ConvertTo-Json -Depth 8
}}
"""
        completed = _run_powershell_script(script)
        assert completed.returncode == 0, completed.stdout + completed.stderr
        payload = json.loads(completed.stdout)

    assert payload["succeeded"] is False
    assert payload["status"] == "blocked"
    assert payload["code"] == "INTBK-6001"
    assert "dcc-backup-manifest.json" in payload["message"]
    assert payload["manifestExists"] is False


def test_backup_ops_manifest_accepts_remote_mysql_dump_proof() -> None:
    root = _backup_root()
    report_module = root / "scripts" / "modules" / "Infra" / "ReportOps.psm1"
    file_module = root / "scripts" / "modules" / "Infra" / "FileOps.psm1"
    remote_dump_path = "/mnt/nas/Backup/BackupPackage/20260520-010203/mysql/ruoyi-vue-pro.sql.gz"

    with tempfile.TemporaryDirectory() as temp_dir:
        backup_root = Path(temp_dir) / "20260520-010203"
        deploy = backup_root / "deploy"
        manifest = backup_root / "manifest"
        mysql = backup_root / "mysql"
        objects = backup_root / "objects"
        for directory in (deploy, manifest, mysql, objects):
            directory.mkdir(parents=True)
        (deploy / "runtime.env").write_text(
            "IMAGE_TAG=release-v2\nBACKEND_HOST_PORT=48081\nFRONTEND_HOST_PORT=8081\n",
            encoding="utf-8",
        )
        (deploy / "docker-compose.yml").write_text("services: {}\n", encoding="utf-8")
        (manifest / "checksums.txt").write_text("abc  deploy/runtime.env\n", encoding="utf-8")
        _write_dcc_backup_manifest(manifest)
        (objects / "manifest-object-inventory.json").write_text(
            json.dumps(
                {
                    "mode": "incremental-manifest",
                    "bucket": "yudao",
                    "objectStoreRoot": "/mnt/nas/Backup/BackupPackage/object-store",
                    "objects": [],
                },
                ensure_ascii=False,
            ),
            encoding="utf-8",
        )

        script = f"""
$ErrorActionPreference = 'Stop'
$script:commands = @()
function Invoke-BackupSshCommand {{
    param($Request)
    $script:commands += $Request.Command
    if ($Request.Command -match 'test -s') {{
        [pscustomobject]@{{ output = '' }}
    }} else {{
        [pscustomobject]@{{ output = '' }}
    }}
}}
function Write-BackupOpsLog {{ param($Session, $Level, $Message) }}
Import-Module '{report_module}' -Force -DisableNameChecking
Import-Module '{file_module}' -Force -DisableNameChecking
$config = [pscustomobject]@{{
    environment = 'test'
    servers = [pscustomobject]@{{
        production = [pscustomobject]@{{ host = '172.30.30.58'; appDir = '/opt/intruoyi/runtime' }}
        test = [pscustomobject]@{{ host = '172.30.30.58'; backupPointsRoot = '/mnt/nas/Backup/BackupPackage' }}
    }}
    ssh = [pscustomobject]@{{ user = 'root'; port = 22 }}
    auth = [pscustomobject]@{{ sshKeyPath = 'D:/missing_id_rsa'; knownHostsPath = 'D:/missing_known_hosts' }}
    backup = [pscustomobject]@{{
        mysqlDatabase = 'ruoyi-vue-pro'
        objectBucket = 'yudao'
    }}
}}
$workspace = [pscustomobject]@{{
    BackupId = '20260520-010203'
    BackupRoot = '{backup_root}'
    ImageTag = 'release-v2'
    DeployPath = '{deploy}'
    ManifestPath = '{manifest}'
    MySqlPath = '{mysql}'
    ObjectsPath = '{objects}'
    RemoteMySqlDumpPath = '{remote_dump_path}'
}}
$logSession = [pscustomobject]@{{ startedAt = [System.DateTimeOffset]::Now }}
$path = New-BackupOpsManifest -Config $config -Workspace $workspace -BackupType 'manual' -Status 'success' -Validation @{{
    mysqlDumpCreated = $true
    objectBackupCreated = $true
    checksumsGenerated = $true
    syncedToTestServer = $true
}} -OperatorName 'tester' -LogSession $logSession
[pscustomobject]@{{
    manifest = [System.IO.File]::ReadAllText($path, [System.Text.UTF8Encoding]::new($false)) | ConvertFrom-Json
    commands = $script:commands
}} | ConvertTo-Json -Depth 8
"""
        completed = _run_powershell_script(script)
        assert completed.returncode == 0, completed.stdout + completed.stderr
        payload = json.loads(completed.stdout)

    assert payload["manifest"]["recoverySet"]["status"] == "COMPLETE"
    assert any("test -s" in command and remote_dump_path in command for command in payload["commands"])


def test_backup_ops_manifest_accepts_production_target_proof() -> None:
    root = _backup_root()
    report_module = root / "scripts" / "modules" / "Infra" / "ReportOps.psm1"
    file_module = root / "scripts" / "modules" / "Infra" / "FileOps.psm1"

    with tempfile.TemporaryDirectory() as temp_dir:
        backup_root = Path(temp_dir) / "20260520-010203"
        deploy = backup_root / "deploy"
        manifest = backup_root / "manifest"
        mysql = backup_root / "mysql"
        objects = backup_root / "objects"
        for directory in (deploy, manifest, mysql, objects):
            directory.mkdir(parents=True)
        (deploy / "runtime.env").write_text(
            "IMAGE_TAG=release-v2\nBACKEND_HOST_PORT=48081\nFRONTEND_HOST_PORT=8081\n",
            encoding="utf-8",
        )
        (deploy / "docker-compose.yml").write_text("services: {}\n", encoding="utf-8")
        (mysql / "ruoyi-vue-pro.sql.gz").write_bytes(b"dump")
        (manifest / "checksums.txt").write_text("abc  deploy/runtime.env\n", encoding="utf-8")
        _write_dcc_backup_manifest(
            manifest,
            target_environment="production",
            target_host="172.30.30.57",
        )
        (objects / "manifest-object-inventory.json").write_text(
            json.dumps(
                {
                    "mode": "incremental-manifest",
                    "bucket": "yudao",
                    "objectStoreRoot": "/mnt/nas/Backup/BackupPackage/object-store",
                    "objects": [],
                },
                ensure_ascii=False,
            ),
            encoding="utf-8",
        )

        script = f"""
$ErrorActionPreference = 'Stop'
function Write-BackupOpsLog {{ param($Session, $Level, $Message) }}
Import-Module '{report_module}' -Force -DisableNameChecking
Import-Module '{file_module}' -Force -DisableNameChecking
$config = [pscustomobject]@{{
    environment = 'production'
    servers = [pscustomobject]@{{
        production = [pscustomobject]@{{ host = '172.30.30.57'; appDir = '/opt/intruoyi/runtime' }}
    }}
    backup = [pscustomobject]@{{
        mysqlDatabase = 'ruoyi-vue-pro'
        objectBucket = 'yudao'
    }}
}}
$workspace = [pscustomobject]@{{
    BackupId = '20260520-010203'
    BackupRoot = '{backup_root}'
    ImageTag = 'release-v2'
    DeployPath = '{deploy}'
    ManifestPath = '{manifest}'
    MySqlPath = '{mysql}'
    ObjectsPath = '{objects}'
}}
$logSession = [pscustomobject]@{{ startedAt = [System.DateTimeOffset]::Now }}
$path = New-BackupOpsManifest -Config $config -Workspace $workspace -BackupType 'manual' -Status 'success' -Validation @{{
    mysqlDumpCreated = $true
    objectBackupCreated = $true
    checksumsGenerated = $true
    syncedToTestServer = $true
}} -OperatorName 'tester' -LogSession $logSession
[System.IO.File]::ReadAllText($path, [System.Text.UTF8Encoding]::new($false))
"""
        completed = _run_powershell_script(script)
        assert completed.returncode == 0, completed.stdout + completed.stderr
        payload = json.loads(completed.stdout)

    assert payload["targetEnvironment"] == "production"
    assert payload["targetHost"] == "172.30.30.57"
    assert payload["recoverySet"]["status"] == "COMPLETE"


def test_backup_ops_manifest_accepts_incremental_object_inventory_marker() -> None:
    root = _backup_root()
    report_module = root / "scripts" / "modules" / "Infra" / "ReportOps.psm1"
    file_module = root / "scripts" / "modules" / "Infra" / "FileOps.psm1"

    with tempfile.TemporaryDirectory() as temp_dir:
        backup_root = Path(temp_dir) / "20260520-010203"
        deploy = backup_root / "deploy"
        manifest = backup_root / "manifest"
        mysql = backup_root / "mysql"
        objects = backup_root / "objects"
        for directory in (deploy, manifest, mysql, objects):
            directory.mkdir(parents=True)
        (deploy / "runtime.env").write_text(
            "IMAGE_TAG=release-v2\nBACKEND_HOST_PORT=48081\nFRONTEND_HOST_PORT=8081\n",
            encoding="utf-8",
        )
        (deploy / "docker-compose.yml").write_text("services: {}\n", encoding="utf-8")
        (mysql / "ruoyi-vue-pro.sql.gz").write_bytes(b"dump")
        (manifest / "checksums.txt").write_text("abc  deploy/runtime.env\n", encoding="utf-8")
        _write_dcc_backup_manifest(manifest)
        (objects / "manifest-object-inventory.json").write_text(
            json.dumps(
                {
                    "mode": "incremental-manifest",
                    "bucket": "yudao",
                    "objectStoreRoot": "/mnt/nas/Backup/BackupPackage/object-store",
                    "stats": {
                        "addedCount": 0,
                        "modifiedCount": 1,
                        "deletedCount": 0,
                        "reusedCount": 2,
                    },
                    "objects": [
                        {
                            "path": "dcc/B.txt",
                            "sha256": "sha-b",
                            "status": "active",
                            "repositoryKey": "sha-b",
                        }
                    ],
                },
                ensure_ascii=False,
            ),
            encoding="utf-8",
        )

        script = f"""
$ErrorActionPreference = 'Stop'
function Write-BackupOpsLog {{ param($Session, $Level, $Message) }}
Import-Module '{report_module}' -Force -DisableNameChecking
Import-Module '{file_module}' -Force -DisableNameChecking
$config = [pscustomobject]@{{
    environment = 'test'
    servers = [pscustomobject]@{{
        production = [pscustomobject]@{{ host = '172.30.30.58'; appDir = '/opt/intruoyi/runtime' }}
    }}
    backup = [pscustomobject]@{{
        mysqlDatabase = 'ruoyi-vue-pro'
        objectBucket = 'yudao'
    }}
}}
$workspace = [pscustomobject]@{{
    BackupId = '20260520-010203'
    BackupRoot = '{backup_root}'
    ImageTag = 'release-v2'
    DeployPath = '{deploy}'
    ManifestPath = '{manifest}'
    MySqlPath = '{mysql}'
    ObjectsPath = '{objects}'
}}
$logSession = [pscustomobject]@{{ startedAt = [System.DateTimeOffset]::Now }}
$path = New-BackupOpsManifest -Config $config -Workspace $workspace -BackupType 'manual' -Status 'success' -Validation @{{
    mysqlDumpCreated = $true
    objectBackupCreated = $true
    checksumsGenerated = $true
    syncedToTestServer = $true
}} -OperatorName 'tester' -LogSession $logSession
[System.IO.File]::ReadAllText($path, [System.Text.UTF8Encoding]::new($false))
"""
        completed = _run_powershell_script(script)
        assert completed.returncode == 0, completed.stdout + completed.stderr
        payload = json.loads(completed.stdout)

    recovery_set = payload["recoverySet"]
    assert payload["targetEnvironment"] == "test"
    assert payload["targetHost"] == "172.30.30.58"
    assert payload["imageTag"] == "release-v2"
    assert recovery_set["status"] == "COMPLETE"
    assert recovery_set["minio"]["snapshotPath"] == "objects/manifest-object-inventory.json"
    assert recovery_set["businessFiles"]["snapshotPath"] == "objects/manifest-object-inventory.json"
    assert payload["objectDeltaStats"]["modifiedCount"] == 1
    assert payload["objectDeltaStats"]["reusedCount"] == 2


def test_backup_workspace_id_uses_ymd_hms_hyphen_format() -> None:
    root = _backup_root()
    file_module = root / "scripts" / "modules" / "Infra" / "FileOps.psm1"

    with tempfile.TemporaryDirectory() as temp_dir:
        config_path = Path(temp_dir) / "config" / "backup-ops.config.test.json"
        local_workspace_root = Path(temp_dir) / "backup-workspace"
        config_path.parent.mkdir()
        script = f"""
$ErrorActionPreference = 'Stop'
Import-Module '{file_module}' -Force -DisableNameChecking
$config = [pscustomobject]@{{
    backup = [pscustomobject]@{{
        localWorkspaceRoot = '{local_workspace_root}'
    }}
    paths = [pscustomobject]@{{
        configPath = '{config_path}'
    }}
}}
$workspace = New-BackupOpsBackupWorkspace -Config $config -Action 'backup-now' -BackupType 'manual'
$workspace | ConvertTo-Json -Depth 8
"""
        completed = _run_powershell_script(script)
        assert completed.returncode == 0, completed.stdout + completed.stderr
        workspace = json.loads(completed.stdout)

    assert re.fullmatch(r"\d{8}-\d{6}", workspace["BackupId"])
    assert "_" not in workspace["BackupId"]
    assert workspace["MySqlPath"].endswith(f"{workspace['BackupId']}\\mysql")
    assert workspace["ObjectsPath"].endswith(f"{workspace['BackupId']}\\objects")
    assert workspace["DeployPath"].endswith(f"{workspace['BackupId']}\\deploy")
    assert workspace["ManifestPath"].endswith(f"{workspace['BackupId']}\\manifest")


def test_local_retention_only_targets_hyphenated_backup_directories() -> None:
    root = _backup_root()
    file_module = root / "scripts" / "modules" / "Infra" / "FileOps.psm1"

    with tempfile.TemporaryDirectory() as temp_dir:
        script = f"""
$ErrorActionPreference = 'Stop'
Import-Module '{file_module}' -Force -DisableNameChecking
$root = '{temp_dir}'
$oldHyphen = Join-Path $root '20260520-010203'
$oldUnderscore = Join-Path $root '20260520_010203'
$notes = Join-Path $root 'notes'
New-Item -ItemType Directory -Path $oldHyphen, $oldUnderscore, $notes -Force | Out-Null
$oldTime = (Get-Date).AddDays(-10)
[System.IO.Directory]::GetLastWriteTime($oldHyphen) | Out-Null
[System.IO.Directory]::SetLastWriteTime($oldHyphen, $oldTime)
[System.IO.Directory]::SetLastWriteTime($oldUnderscore, $oldTime)
[System.IO.Directory]::SetLastWriteTime($notes, $oldTime)
$result = Remove-ExpiredBackupDirectories -Request @{{
    RootPath = $root
    KeepDays = 3
}}
$result | ConvertTo-Json -Depth 8
"""
        completed = _run_powershell_script(script)
        assert completed.returncode == 0, completed.stdout + completed.stderr
        result = json.loads(completed.stdout)

    assert result["status"] == "planned"
    assert result["count"] == 1
    assert Path(result["targets"][0]).name == "20260520-010203"


def test_remote_retention_filters_hyphenated_backup_point_names() -> None:
    root = _backup_root()
    file_module = root / "scripts" / "modules" / "Infra" / "FileOps.psm1"
    script = f"""
$ErrorActionPreference = 'Stop'
$script:commands = @()
function Invoke-BackupSshCommand {{
    param($Request)
    $script:commands += $Request.Command
    [pscustomobject]@{{ output = (@{{
        operation = 'remote-cleanup'
        status = 'success'
        code = 'INTBK-0000'
        action = 'delete'
        rootPath = '/mnt/nas/Backup/BackupPackage'
        keepDays = 30
        keepLast = 5
        maxNasUsedPercent = 90
        deletedBackupPoints = @()
        deletedObjectBlobs = @()
        retainedBackupPoints = @()
        before = @{{ backupPointCount = 0; objectBlobCount = 0; objectStoreBytes = 0 }}
        after = @{{ backupPointCount = 0; objectBlobCount = 0; objectStoreBytes = 0 }}
        capacityBefore = @{{ usedPercent = 10 }}
        capacityAfter = @{{ usedPercent = 10 }}
    }} | ConvertTo-Json -Depth 8) }}
}}
function Write-BackupOpsLog {{ param($Session, $Level, $Message) }}
Import-Module '{file_module}' -Force -DisableNameChecking
$config = [pscustomobject]@{{
    servers = [pscustomobject]@{{
        test = [pscustomobject]@{{
            host = '172.30.30.58'
            backupPointsRoot = '/mnt/nas/Backup/BackupPackage'
        }}
    }}
    ssh = [pscustomobject]@{{
        user = 'root'
        port = 22
    }}
    auth = [pscustomobject]@{{
        sshKeyPath = 'D:/missing_id_rsa'
        knownHostsPath = 'D:/missing_known_hosts'
    }}
    backup = [pscustomobject]@{{
        keepDaysRemote = 30
        keepLastPoints = 5
        maxNasUsedPercent = 90
    }}
}}
$session = [pscustomobject]@{{}}
$result = Invoke-BackupOpsRemoteRetention -Config $config -LogSession $session
[pscustomobject]@{{
    result = $result
    commands = $script:commands
}} | ConvertTo-Json -Depth 8
"""
    completed = _run_powershell_script(script)
    assert completed.returncode == 0, completed.stdout + completed.stderr
    payload = json.loads(completed.stdout)
    command = payload["commands"][0]

    assert payload["result"]["rootPath"] == "/mnt/nas/Backup/BackupPackage"
    assert payload["result"]["keepDays"] == 30
    assert payload["result"]["keepLast"] == 5
    assert payload["result"]["maxNasUsedPercent"] == 90
    assert "export BACKUP_ROOT=/mnt/nas/Backup/BackupPackage" in command
    assert "export KEEP_DAYS=30" in command
    assert "export KEEP_LAST_POINTS=5" in command
    assert "export MAX_NAS_USED_PERCENT=90" in command
    assert "manifest/manifest.json" in command
    assert "object-store" in command
    assert "repositoryKey" in command
    assert "ReleasePackage" not in command
    assert "rm -rf" not in command


def test_remote_retention_rejects_non_backup_package_root_before_ssh() -> None:
    root = _backup_root()
    file_module = root / "scripts" / "modules" / "Infra" / "FileOps.psm1"
    script = f"""
$ErrorActionPreference = 'Stop'
$script:sshCalled = $false
function Invoke-BackupSshCommand {{
    param($Request)
    $script:sshCalled = $true
    throw 'SSH must not be called for an unsafe retention root'
}}
function Write-BackupOpsLog {{ param($Session, $Level, $Message) }}
Import-Module '{file_module}' -Force -DisableNameChecking
$config = [pscustomobject]@{{
    servers = [pscustomobject]@{{
        test = [pscustomobject]@{{
            host = '172.30.30.58'
            backupPointsRoot = '/mnt/nas/Backup/ReleasePackage'
        }}
    }}
    ssh = [pscustomobject]@{{
        user = 'root'
        port = 22
    }}
    auth = [pscustomobject]@{{
        sshKeyPath = 'D:/missing_id_rsa'
        knownHostsPath = 'D:/missing_known_hosts'
    }}
    backup = [pscustomobject]@{{
        keepDaysRemote = 30
        keepLastPoints = 5
        maxNasUsedPercent = 90
    }}
}}
$message = ''
try {{
    Invoke-BackupOpsRemoteRetention -Config $config -LogSession ([pscustomobject]@{{}}) | Out-Null
    throw 'Expected unsafe root failure'
}} catch {{
    $message = $_.Exception.Message
}}
[pscustomobject]@{{
    message = $message
    sshCalled = $script:sshCalled
}} | ConvertTo-Json -Depth 8
"""
    completed = _run_powershell_script(script)
    assert completed.returncode == 0, completed.stdout + completed.stderr
    payload = json.loads(completed.stdout)

    assert payload["sshCalled"] is False
    assert "/mnt/nas/Backup/BackupPackage" in payload["message"]
    assert "ReleasePackage" in payload["message"]


def test_sync_backup_to_test_server_targets_nas_backup_root() -> None:
    root = _backup_root()
    file_module = root / "scripts" / "modules" / "Infra" / "FileOps.psm1"

    with tempfile.TemporaryDirectory() as temp_dir:
        workspace_root = Path(temp_dir) / "20260520-010203"
        deploy = workspace_root / "deploy"
        manifest = workspace_root / "manifest"
        mysql = workspace_root / "mysql"
        objects = workspace_root / "objects"
        for directory in (deploy, manifest, mysql, objects):
            directory.mkdir(parents=True)
        (deploy / "image-tag.txt").write_text("release-test\n", encoding="utf-8")
        (manifest / "manifest.json").write_text("{}", encoding="utf-8")
        (manifest / "checksums.txt").write_text("abc  deploy/runtime.env\n", encoding="utf-8")
        (mysql / "dump.sql").write_text("-- dump\n", encoding="utf-8")
        (objects / "manifest-object-inventory.json").write_text(
            json.dumps(
                {
                    "mode": "incremental-manifest",
                    "bucket": "yudao",
                    "objectStoreRoot": "/mnt/nas/Backup/BackupPackage/object-store",
                    "stats": {
                        "addedCount": 1,
                        "modifiedCount": 0,
                        "deletedCount": 0,
                        "reusedCount": 0,
                    },
                    "objects": [
                        {
                            "path": "dcc/A.txt",
                            "sha256": "sha-a",
                            "status": "active",
                            "repositoryKey": "sha-a",
                        }
                    ],
                },
                ensure_ascii=False,
            ),
            encoding="utf-8",
        )

        script = f"""
$ErrorActionPreference = 'Stop'
$script:commands = @()
$script:uploads = @()
function Invoke-BackupSshCommand {{
    param($Request)
    $script:commands += [pscustomobject]@{{
        command = $Request.Command
        timeoutSeconds = $Request.TimeoutSeconds
    }}
    [pscustomobject]@{{ output = '' }}
}}
function Send-BackupFileOverSsh {{
    param($Request)
    $script:uploads += [pscustomobject]@{{
        localPath = $Request.LocalPath
        remotePath = $Request.RemotePath
        timeoutSeconds = $Request.TimeoutSeconds
    }}
    [pscustomobject]@{{ status = 'success' }}
}}
function Write-BackupOpsLog {{ param($Session, $Level, $Message) }}
Import-Module '{file_module}' -Force -DisableNameChecking
$config = [pscustomobject]@{{
    servers = [pscustomobject]@{{
        test = [pscustomobject]@{{
            host = '172.30.30.58'
            backupPointsRoot = '/mnt/nas/Backup/BackupPackage'
        }}
    }}
    ssh = [pscustomobject]@{{
        user = 'root'
        port = 22
    }}
    auth = [pscustomobject]@{{
        sshKeyPath = 'D:/missing_id_rsa'
        knownHostsPath = 'D:/missing_known_hosts'
    }}
}}
$workspace = [pscustomobject]@{{
    BackupId = '20260520-010203'
    BackupRoot = '{workspace_root}'
    DeployPath = '{deploy}'
    ManifestPath = '{manifest}'
    MySqlPath = '{mysql}'
    ObjectsPath = '{objects}'
}}
$session = [pscustomobject]@{{}}
$result = Sync-BackupOpsBackupToTestServer -Config $config -Workspace $workspace -LogSession $session
[pscustomobject]@{{
    result = $result
    commands = $script:commands
    uploads = $script:uploads
}} | ConvertTo-Json -Depth 8
"""
        completed = _run_powershell_script(script)
        assert completed.returncode == 0, completed.stdout + completed.stderr
        payload = json.loads(completed.stdout)

    assert payload["result"]["remoteRoot"] == "/mnt/nas/Backup/BackupPackage/20260520-010203"
    assert any("'/mnt/nas/Backup/BackupPackage/20260520-010203'" in command["command"] for command in payload["commands"])
    assert payload["commands"], "expected remote mkdir commands to be issued"
    assert all(command["timeoutSeconds"] == 60 for command in payload["commands"])
    upload_paths = {upload["remotePath"] for upload in payload["uploads"]}
    assert "/mnt/nas/Backup/BackupPackage/20260520-010203/deploy/" in upload_paths
    assert "/mnt/nas/Backup/BackupPackage/20260520-010203/manifest/" in upload_paths
    assert "/mnt/nas/Backup/BackupPackage/20260520-010203/mysql/" in upload_paths
    assert "/mnt/nas/Backup/BackupPackage/20260520-010203/objects/manifest-object-inventory.json" in upload_paths
    assert any(upload["localPath"].endswith("checksums.txt") for upload in payload["uploads"])
    assert not any(upload["localPath"].endswith("manifest.json") for upload in payload["uploads"])
    metadata_uploads = [upload for upload in payload["uploads"] if not upload["remotePath"].endswith("/mysql/")]
    assert metadata_uploads, "expected deploy, manifest, and object inventory uploads"
    assert all(upload["timeoutSeconds"] == 300 for upload in metadata_uploads)
    mysql_uploads = [upload for upload in payload["uploads"] if upload["remotePath"].endswith("/mysql/")]
    assert mysql_uploads and all(upload["timeoutSeconds"] == 7200 for upload in mysql_uploads)


def test_sync_manifest_to_test_server_upload_is_bounded() -> None:
    root = _backup_root()
    file_module = root / "scripts" / "modules" / "Infra" / "FileOps.psm1"

    with tempfile.TemporaryDirectory() as temp_dir:
        workspace_root = Path(temp_dir) / "20260520-010203"
        manifest = workspace_root / "manifest"
        manifest.mkdir(parents=True)
        (manifest / "manifest.json").write_text("{}", encoding="utf-8")

        script = f"""
$ErrorActionPreference = 'Stop'
$script:uploads = @()
function Invoke-BackupSshCommand {{
    param($Request)
    [pscustomobject]@{{ output = '' }}
}}
function Send-BackupFileOverSsh {{
    param($Request)
    $script:uploads += [pscustomobject]@{{
        localPath = $Request.LocalPath
        remotePath = $Request.RemotePath
        timeoutSeconds = $Request.TimeoutSeconds
    }}
    [pscustomobject]@{{ status = 'success' }}
}}
function Write-BackupOpsLog {{ param($Session, $Level, $Message) }}
Import-Module '{file_module}' -Force -DisableNameChecking
$config = [pscustomobject]@{{
    servers = [pscustomobject]@{{
        test = [pscustomobject]@{{
            host = '172.30.30.58'
            backupPointsRoot = '/mnt/nas/Backup/BackupPackage'
        }}
    }}
    ssh = [pscustomobject]@{{
        user = 'root'
        port = 22
    }}
    auth = [pscustomobject]@{{
        sshKeyPath = 'D:/missing_id_rsa'
        knownHostsPath = 'D:/missing_known_hosts'
    }}
}}
$workspace = [pscustomobject]@{{
    BackupId = '20260520-010203'
    ManifestPath = '{manifest}'
}}
$session = [pscustomobject]@{{}}
$result = Sync-BackupOpsManifestToTestServer -Config $config -Workspace $workspace -LogSession $session
[pscustomobject]@{{
    result = $result
    uploads = $script:uploads
}} | ConvertTo-Json -Depth 8
"""
        completed = _run_powershell_script(script)
        assert completed.returncode == 0, completed.stdout + completed.stderr
        payload = json.loads(completed.stdout)

    assert payload["result"]["remoteRoot"] == "/mnt/nas/Backup/BackupPackage/20260520-010203/manifest/"
    assert payload["uploads"] == [
        {
            "localPath": str(manifest / "manifest.json"),
            "remotePath": "/mnt/nas/Backup/BackupPackage/20260520-010203/manifest/",
            "timeoutSeconds": 300,
        }
    ]


def test_restore_candidate_scan_accepts_remote_nas_manifest_inventory() -> None:
    root = _backup_root()
    docker_module = root / "scripts" / "modules" / "Infra" / "DockerOps.psm1"
    manifest = {
        "schemaVersion": "v2",
        "backupId": "20260606-155715",
        "targetEnvironment": "test",
        "targetHost": "172.30.30.58",
        "status": "success",
        "deploy": {"imageTag": "20260606_ui_code_only_onlyoffice_A_1138"},
        "backupStrategy": {
            "mode": "incremental-manifest",
            "mysqlBackupMode": "full-dump-baseline",
            "mysqlBaseline": "full-dump",
        },
        "recoverySet": {
            "id": "20260606-155715",
            "status": "COMPLETE",
            "program": {"imageTag": "20260606_ui_code_only_onlyoffice_A_1138"},
            "mysql": {"dumpPath": "mysql/ruoyi-vue-pro.sql.gz"},
            "minio": {"bucket": "yudao", "snapshotPath": "objects/manifest-object-inventory.json"},
            "businessFiles": {"snapshotPath": "objects/manifest-object-inventory.json"},
            "dcc": {"manifestPath": "manifest/dcc-backup-manifest.json"},
            "redis": {"policy": "CLEAR_AND_REBUILD"},
            "configuration": {"manifestPath": "deploy/runtime.env", "composePath": "deploy/docker-compose.yml"},
            "checksums": {"path": "manifest/checksums.txt", "sha256": "abc"},
        },
        "validation": {
            "mysqlDumpCreated": True,
            "objectBackupCreated": True,
            "checksumsGenerated": True,
            "rehearsalStatus": "PASSED",
            "lastRehearsedAt": "2026-06-09T06:20:00+08:00",
        },
    }
    manifest_json = json.dumps(manifest, ensure_ascii=False)
    dcc_manifest_json = _dcc_backup_manifest_json()
    script = f"""
$ErrorActionPreference = 'Stop'
$script:commands = @()
$script:manifestJson = @'
{manifest_json}
'@
$script:dccManifestJson = @'
{dcc_manifest_json}
'@
function Write-BackupOpsLog {{ param($Session, $Level, $Message) }}
function Invoke-BackupSshCommand {{
    param($Request)
    $script:commands += $Request.Command
    $command = [string]$Request.Command
    if ($command.StartsWith('find ')) {{
        return [pscustomobject]@{{ output = '/mnt/nas/Backup/BackupPackage/20260606-155715' }}
    }}
    if ($command.StartsWith('test -f ')) {{
        if ($command -match 'mysql/ruoyi-vue-pro\\.sql\\.gz|deploy/image-tag\\.txt|manifest/manifest\\.json|manifest/checksums\\.txt|manifest/dcc-backup-manifest\\.json|objects/manifest-object-inventory\\.json') {{
            return [pscustomobject]@{{ output = 'EXISTS' }}
        }}
        throw [System.InvalidOperationException]::new('missing file')
    }}
    if ($command.StartsWith('test -d ')) {{
        if ($command -match 'objects/yudao') {{
            throw [System.InvalidOperationException]::new('legacy object directory is absent')
        }}
        return [pscustomobject]@{{ output = 'EXISTS' }}
    }}
    if ($command -match 'cat .*deploy/image-tag\\.txt') {{
        return [pscustomobject]@{{ output = '20260606_ui_code_only_onlyoffice_A_1138' }}
    }}
    if ($command -match 'cat .*manifest/manifest\\.json') {{
        return [pscustomobject]@{{ output = $script:manifestJson }}
    }}
    if ($command -match 'cat .*manifest/dcc-backup-manifest\\.json') {{
        return [pscustomobject]@{{ output = $script:dccManifestJson }}
    }}
    throw [System.InvalidOperationException]::new("unexpected command: $command")
}}
Import-Module '{docker_module}' -Force -DisableNameChecking
$config = [pscustomobject]@{{
    servers = [pscustomobject]@{{
        test = [pscustomobject]@{{
            host = '172.30.30.58'
            backupPointsRoot = '/mnt/nas/Backup/BackupPackage'
        }}
    }}
    backup = [pscustomobject]@{{
        mysqlDatabase = 'ruoyi-vue-pro'
        objectBucket = 'yudao'
    }}
    ssh = [pscustomobject]@{{
        user = 'root'
        port = 22
    }}
    auth = [pscustomobject]@{{
        sshKeyPath = 'D:/missing_id_rsa'
        knownHostsPath = 'D:/missing_known_hosts'
    }}
}}
$session = [pscustomobject]@{{}}
$candidates = Get-BackupOpsRestoreCandidates -Config $config -LogSession $session
[pscustomobject]@{{
    candidates = $candidates
    commands = $script:commands
}} | ConvertTo-Json -Depth 10
"""
    completed = _run_powershell_script(script)
    assert completed.returncode == 0, completed.stdout + completed.stderr
    payload = json.loads(completed.stdout)
    candidates = payload["candidates"]
    if isinstance(candidates, dict):
        candidates = [candidates]

    assert len(candidates) == 1
    assert candidates[0]["backupId"] == "20260606-155715"
    assert candidates[0]["checksumsSha256"] == "abc"
    assert not any("test -d '/mnt/nas/Backup/BackupPackage/20260606-155715/objects/yudao'" in command for command in payload["commands"])
    assert any("test -f '/mnt/nas/Backup/BackupPackage/20260606-155715/objects/manifest-object-inventory.json'" in command for command in payload["commands"])


def test_restore_candidate_scan_skips_invalid_dcc_backup_chain() -> None:
    root = _backup_root()
    docker_module = root / "scripts" / "modules" / "Infra" / "DockerOps.psm1"
    manifest = {
        "schemaVersion": "v2",
        "backupId": "20260606-155715",
        "targetEnvironment": "test",
        "targetHost": "172.30.30.58",
        "status": "success",
        "deploy": {"imageTag": "20260606_ui_code_only_onlyoffice_A_1138"},
        "backupStrategy": {
            "mode": "incremental-manifest",
            "mysqlBackupMode": "full-dump-baseline",
            "mysqlBaseline": "full-dump",
        },
        "recoverySet": {
            "id": "20260606-155715",
            "status": "COMPLETE",
            "program": {"imageTag": "20260606_ui_code_only_onlyoffice_A_1138"},
            "mysql": {"dumpPath": "mysql/ruoyi-vue-pro.sql.gz"},
            "minio": {"bucket": "yudao", "snapshotPath": "objects/manifest-object-inventory.json"},
            "businessFiles": {"snapshotPath": "objects/manifest-object-inventory.json"},
            "dcc": {"manifestPath": "manifest/dcc-backup-manifest.json"},
            "redis": {"policy": "CLEAR_AND_REBUILD"},
            "configuration": {"manifestPath": "deploy/runtime.env", "composePath": "deploy/docker-compose.yml"},
            "checksums": {"path": "manifest/checksums.txt", "sha256": "abc"},
        },
        "validation": {
            "mysqlDumpCreated": True,
            "objectBackupCreated": True,
            "checksumsGenerated": True,
            "rehearsalStatus": "PASSED",
            "lastRehearsedAt": "2026-06-09T06:20:00+08:00",
        },
    }
    manifest_json = json.dumps(manifest, ensure_ascii=False)
    dcc_manifest_json = _dcc_backup_manifest_json(_invalid_deleted_dcc_backup_manifest("20260606-155715"))
    script = f"""
$ErrorActionPreference = 'Stop'
$script:logs = @()
$script:manifestJson = @'
{manifest_json}
'@
$script:dccManifestJson = @'
{dcc_manifest_json}
'@
function Write-BackupOpsLog {{
    param($Session, [string]$Level = 'INFO', [string]$Message)
    $script:logs += [pscustomobject]@{{ level = $Level; message = $Message }}
}}
function Invoke-BackupSshCommand {{
    param($Request)
    $command = [string]$Request.Command
    if ($command.StartsWith('find ')) {{
        return [pscustomobject]@{{ output = '/mnt/nas/Backup/BackupPackage/20260606-155715' }}
    }}
    if ($command.StartsWith('test -f ')) {{
        if ($command -match 'mysql/ruoyi-vue-pro\\.sql\\.gz|deploy/image-tag\\.txt|manifest/manifest\\.json|manifest/checksums\\.txt|manifest/dcc-backup-manifest\\.json|objects/manifest-object-inventory\\.json') {{
            return [pscustomobject]@{{ output = 'EXISTS' }}
        }}
        throw [System.InvalidOperationException]::new('missing file')
    }}
    if ($command -match 'cat .*deploy/image-tag\\.txt') {{
        return [pscustomobject]@{{ output = '20260606_ui_code_only_onlyoffice_A_1138' }}
    }}
    if ($command -match 'cat .*manifest/manifest\\.json') {{
        return [pscustomobject]@{{ output = $script:manifestJson }}
    }}
    if ($command -match 'cat .*manifest/dcc-backup-manifest\\.json') {{
        return [pscustomobject]@{{ output = $script:dccManifestJson }}
    }}
    throw [System.InvalidOperationException]::new("unexpected command: $command")
}}
Import-Module '{docker_module}' -Force -DisableNameChecking
$config = [pscustomobject]@{{
    servers = [pscustomobject]@{{
        test = [pscustomobject]@{{
            host = '172.30.30.58'
            backupPointsRoot = '/mnt/nas/Backup/BackupPackage'
        }}
    }}
    backup = [pscustomobject]@{{
        mysqlDatabase = 'ruoyi-vue-pro'
        objectBucket = 'yudao'
    }}
    ssh = [pscustomobject]@{{
        user = 'root'
        port = 22
    }}
    auth = [pscustomobject]@{{
        sshKeyPath = 'D:/missing_id_rsa'
        knownHostsPath = 'D:/missing_known_hosts'
    }}
}}
$session = [pscustomobject]@{{}}
$candidates = Get-BackupOpsRestoreCandidates -Config $config -LogSession $session
[pscustomobject]@{{
    candidates = @($candidates)
    logs = $script:logs
}} | ConvertTo-Json -Depth 10
"""
    completed = _run_powershell_script(script)
    assert completed.returncode == 0, completed.stdout + completed.stderr
    payload = json.loads(completed.stdout)

    assert payload["candidates"] == []
    assert any("dcc_delete_event_missing" in item["message"] for item in payload["logs"])


def test_restore_candidate_scan_skips_unverified_recovery_set() -> None:
    root = _backup_root()
    docker_module = root / "scripts" / "modules" / "Infra" / "DockerOps.psm1"
    manifest = {
        "schemaVersion": "v2",
        "backupId": "20260606-155715",
        "targetEnvironment": "test",
        "targetHost": "172.30.30.58",
        "status": "success",
        "deploy": {"imageTag": "20260606_ui_code_only_onlyoffice_A_1138"},
        "backupStrategy": {
            "mode": "incremental-manifest",
            "mysqlBackupMode": "full-dump-baseline",
            "mysqlBaseline": "full-dump",
        },
        "recoverySet": {
            "id": "20260606-155715",
            "status": "COMPLETE",
            "program": {"imageTag": "20260606_ui_code_only_onlyoffice_A_1138"},
            "mysql": {"dumpPath": "mysql/ruoyi-vue-pro.sql.gz"},
            "minio": {"bucket": "yudao", "snapshotPath": "objects/manifest-object-inventory.json"},
            "businessFiles": {"snapshotPath": "objects/manifest-object-inventory.json"},
            "dcc": {"manifestPath": "manifest/dcc-backup-manifest.json"},
            "redis": {"policy": "CLEAR_AND_REBUILD"},
            "configuration": {"manifestPath": "deploy/runtime.env", "composePath": "deploy/docker-compose.yml"},
            "checksums": {"path": "manifest/checksums.txt", "sha256": "abc"},
        },
        "validation": {
            "mysqlDumpCreated": True,
            "objectBackupCreated": True,
            "checksumsGenerated": True,
            "rehearsalStatus": "unverified",
            "lastRehearsedAt": None,
        },
    }
    manifest_json = json.dumps(manifest, ensure_ascii=False)
    script = f"""
$ErrorActionPreference = 'Stop'
$script:logs = @()
$script:manifestJson = @'
{manifest_json}
'@
function Write-BackupOpsLog {{
    param($Session, [string]$Level = 'INFO', [string]$Message)
    $script:logs += [pscustomobject]@{{ level = $Level; message = $Message }}
}}
function Invoke-BackupSshCommand {{
    param($Request)
    $command = [string]$Request.Command
    if ($command.StartsWith('find ')) {{
        return [pscustomobject]@{{ output = '/mnt/nas/Backup/BackupPackage/20260606-155715' }}
    }}
    if ($command.StartsWith('test -f ')) {{
        if ($command -match 'mysql/ruoyi-vue-pro\\.sql\\.gz|deploy/image-tag\\.txt|manifest/manifest\\.json|manifest/checksums\\.txt|manifest/dcc-backup-manifest\\.json|objects/manifest-object-inventory\\.json') {{
            return [pscustomobject]@{{ output = 'EXISTS' }}
        }}
        throw [System.InvalidOperationException]::new('missing file')
    }}
    if ($command -match 'cat .*deploy/image-tag\\.txt') {{
        return [pscustomobject]@{{ output = '20260606_ui_code_only_onlyoffice_A_1138' }}
    }}
    if ($command -match 'cat .*manifest/manifest\\.json') {{
        return [pscustomobject]@{{ output = $script:manifestJson }}
    }}
    if ($command -match 'cat .*manifest/dcc-backup-manifest\\.json') {{
        return [pscustomobject]@{{ output = $script:dccManifestJson }}
    }}
    throw [System.InvalidOperationException]::new("unexpected command: $command")
}}
$module = Import-Module '{docker_module}' -Force -DisableNameChecking -PassThru
$config = [pscustomobject]@{{
    servers = [pscustomobject]@{{
        test = [pscustomobject]@{{
            host = '172.30.30.58'
            backupPointsRoot = '/mnt/nas/Backup/BackupPackage'
        }}
    }}
    backup = [pscustomobject]@{{
        mysqlDatabase = 'ruoyi-vue-pro'
        objectBucket = 'yudao'
    }}
    ssh = [pscustomobject]@{{
        user = 'root'
        port = 22
    }}
    auth = [pscustomobject]@{{
        sshKeyPath = 'D:/missing_id_rsa'
        knownHostsPath = 'D:/missing_known_hosts'
    }}
}}
$session = [pscustomobject]@{{}}
$candidates = Get-BackupOpsRestoreCandidates -Config $config -LogSession $session
[pscustomobject]@{{
    candidates = @($candidates)
    logs = $script:logs
}} | ConvertTo-Json -Depth 10
"""
    completed = _run_powershell_script(script)
    assert completed.returncode == 0, completed.stdout + completed.stderr
    payload = json.loads(completed.stdout)

    assert payload["candidates"] == []
    assert any("rehearsalStatus" in item["message"] for item in payload["logs"])


def test_restore_candidate_scan_skips_manifest_without_backup_strategy() -> None:
    root = _backup_root()
    docker_module = root / "scripts" / "modules" / "Infra" / "DockerOps.psm1"
    manifest = {
        "schemaVersion": "v2",
        "backupId": "20260606-155715",
        "targetEnvironment": "test",
        "targetHost": "172.30.30.58",
        "status": "success",
        "deploy": {"imageTag": "20260606_ui_code_only_onlyoffice_A_1138"},
        "recoverySet": {
            "id": "20260606-155715",
            "status": "COMPLETE",
            "program": {"imageTag": "20260606_ui_code_only_onlyoffice_A_1138"},
            "mysql": {"dumpPath": "mysql/ruoyi-vue-pro.sql.gz"},
            "minio": {"bucket": "yudao", "snapshotPath": "objects/manifest-object-inventory.json"},
            "businessFiles": {"snapshotPath": "objects/manifest-object-inventory.json"},
            "dcc": {"manifestPath": "manifest/dcc-backup-manifest.json"},
            "redis": {"policy": "CLEAR_AND_REBUILD"},
            "configuration": {"manifestPath": "deploy/runtime.env", "composePath": "deploy/docker-compose.yml"},
            "checksums": {"path": "manifest/checksums.txt", "sha256": "abc"},
        },
        "validation": {
            "mysqlDumpCreated": True,
            "objectBackupCreated": True,
            "checksumsGenerated": True,
            "rehearsalStatus": "PASSED",
            "lastRehearsedAt": "2026-06-09T06:20:00+08:00",
        },
    }
    manifest_json = json.dumps(manifest, ensure_ascii=False)
    script = f"""
$ErrorActionPreference = 'Stop'
$script:logs = @()
$script:manifestJson = @'
{manifest_json}
'@
function Write-BackupOpsLog {{
    param($Session, [string]$Level = 'INFO', [string]$Message)
    $script:logs += [pscustomobject]@{{ level = $Level; message = $Message }}
}}
function Invoke-BackupSshCommand {{
    param($Request)
    $command = [string]$Request.Command
    if ($command.StartsWith('find ')) {{
        return [pscustomobject]@{{ output = '/mnt/nas/Backup/BackupPackage/20260606-155715' }}
    }}
    if ($command.StartsWith('test -f ')) {{
        if ($command -match 'mysql/ruoyi-vue-pro\\.sql\\.gz|deploy/image-tag\\.txt|manifest/manifest\\.json|manifest/checksums\\.txt|manifest/dcc-backup-manifest\\.json|objects/manifest-object-inventory\\.json') {{
            return [pscustomobject]@{{ output = 'EXISTS' }}
        }}
        throw [System.InvalidOperationException]::new('missing file')
    }}
    if ($command -match 'cat .*deploy/image-tag\\.txt') {{
        return [pscustomobject]@{{ output = '20260606_ui_code_only_onlyoffice_A_1138' }}
    }}
    if ($command -match 'cat .*manifest/manifest\\.json') {{
        return [pscustomobject]@{{ output = $script:manifestJson }}
    }}
    if ($command -match 'cat .*manifest/dcc-backup-manifest\\.json') {{
        return [pscustomobject]@{{ output = $script:dccManifestJson }}
    }}
    throw [System.InvalidOperationException]::new("unexpected command: $command")
}}
$module = Import-Module '{docker_module}' -Force -DisableNameChecking -PassThru
$config = [pscustomobject]@{{
    servers = [pscustomobject]@{{
        test = [pscustomobject]@{{
            host = '172.30.30.58'
            backupPointsRoot = '/mnt/nas/Backup/BackupPackage'
        }}
    }}
    backup = [pscustomobject]@{{
        mysqlDatabase = 'ruoyi-vue-pro'
        objectBucket = 'yudao'
    }}
    ssh = [pscustomobject]@{{
        user = 'root'
        port = 22
    }}
    auth = [pscustomobject]@{{
        sshKeyPath = 'D:/missing_id_rsa'
        knownHostsPath = 'D:/missing_known_hosts'
    }}
}}
$session = [pscustomobject]@{{}}
$candidates = Get-BackupOpsRestoreCandidates -Config $config -LogSession $session
[pscustomobject]@{{
    candidates = @($candidates)
    logs = $script:logs
}} | ConvertTo-Json -Depth 10
"""
    completed = _run_powershell_script(script)
    assert completed.returncode == 0, completed.stdout + completed.stderr
    payload = json.loads(completed.stdout)

    assert payload["candidates"] == []
    assert any("backupStrategy" in item["message"] for item in payload["logs"])


def test_restore_candidate_scan_skips_manifest_without_dcc_backup_manifest() -> None:
    root = _backup_root()
    docker_module = root / "scripts" / "modules" / "Infra" / "DockerOps.psm1"
    manifest = {
        "schemaVersion": "v2",
        "backupId": "20260606-155715",
        "targetEnvironment": "test",
        "targetHost": "172.30.30.58",
        "status": "success",
        "deploy": {"imageTag": "20260606_ui_code_only_onlyoffice_A_1138"},
        "backupStrategy": {
            "mode": "incremental-manifest",
            "mysqlBackupMode": "full-dump-baseline",
            "mysqlBaseline": "full-dump",
        },
        "recoverySet": {
            "id": "20260606-155715",
            "status": "COMPLETE",
            "program": {"imageTag": "20260606_ui_code_only_onlyoffice_A_1138"},
            "mysql": {"dumpPath": "mysql/ruoyi-vue-pro.sql.gz"},
            "minio": {"bucket": "yudao", "snapshotPath": "objects/manifest-object-inventory.json"},
            "businessFiles": {"snapshotPath": "objects/manifest-object-inventory.json"},
            "redis": {"policy": "CLEAR_AND_REBUILD"},
            "configuration": {"manifestPath": "deploy/runtime.env", "composePath": "deploy/docker-compose.yml"},
            "checksums": {"path": "manifest/checksums.txt", "sha256": "abc"},
        },
        "validation": {
            "mysqlDumpCreated": True,
            "objectBackupCreated": True,
            "checksumsGenerated": True,
            "rehearsalStatus": "PASSED",
            "lastRehearsedAt": "2026-06-09T06:20:00+08:00",
        },
    }
    manifest_json = json.dumps(manifest, ensure_ascii=False)
    script = f"""
$ErrorActionPreference = 'Stop'
$script:logs = @()
$script:manifestJson = @'
{manifest_json}
'@
function Write-BackupOpsLog {{
    param($Session, [string]$Level = 'INFO', [string]$Message)
    $script:logs += [pscustomobject]@{{ level = $Level; message = $Message }}
}}
function Invoke-BackupSshCommand {{
    param($Request)
    $command = [string]$Request.Command
    if ($command.StartsWith('find ')) {{
        return [pscustomobject]@{{ output = '/mnt/nas/Backup/BackupPackage/20260606-155715' }}
    }}
    if ($command.StartsWith('test -f ')) {{
        if ($command -match 'manifest/dcc-backup-manifest\\.json') {{
            return [pscustomobject]@{{ output = 'EXISTS' }}
        }}
        if ($command -match 'mysql/ruoyi-vue-pro\\.sql\\.gz|deploy/image-tag\\.txt|manifest/manifest\\.json|manifest/checksums\\.txt|objects/manifest-object-inventory\\.json') {{
            return [pscustomobject]@{{ output = 'EXISTS' }}
        }}
        throw [System.InvalidOperationException]::new('missing file')
    }}
    if ($command -match 'cat .*deploy/image-tag\\.txt') {{
        return [pscustomobject]@{{ output = '20260606_ui_code_only_onlyoffice_A_1138' }}
    }}
    if ($command -match 'cat .*manifest/manifest\\.json') {{
        return [pscustomobject]@{{ output = $script:manifestJson }}
    }}
    throw [System.InvalidOperationException]::new("unexpected command: $command")
}}
$module = Import-Module '{docker_module}' -Force -DisableNameChecking -PassThru
$config = [pscustomobject]@{{
    servers = [pscustomobject]@{{
        test = [pscustomobject]@{{
            host = '172.30.30.58'
            backupPointsRoot = '/mnt/nas/Backup/BackupPackage'
        }}
    }}
    backup = [pscustomobject]@{{
        mysqlDatabase = 'ruoyi-vue-pro'
        objectBucket = 'yudao'
    }}
    ssh = [pscustomobject]@{{
        user = 'root'
        port = 22
    }}
    auth = [pscustomobject]@{{
        sshKeyPath = 'D:/missing_id_rsa'
        knownHostsPath = 'D:/missing_known_hosts'
    }}
}}
$session = [pscustomobject]@{{}}
$candidates = Get-BackupOpsRestoreCandidates -Config $config -LogSession $session
[pscustomobject]@{{
    candidates = @($candidates)
    logs = $script:logs
}} | ConvertTo-Json -Depth 10
"""
    completed = _run_powershell_script(script)
    assert completed.returncode == 0, completed.stdout + completed.stderr
    payload = json.loads(completed.stdout)

    assert payload["candidates"] == []
    assert any("recoverySet.dcc.manifestPath" in item["message"] for item in payload["logs"])


def test_restore_candidate_scan_skips_manifest_without_test_target_proof() -> None:
    root = _backup_root()
    docker_module = root / "scripts" / "modules" / "Infra" / "DockerOps.psm1"
    manifest = {
        "schemaVersion": "v2",
        "backupId": "20260606-155715",
        "status": "success",
        "deploy": {"imageTag": "20260606_ui_code_only_onlyoffice_A_1138"},
        "backupStrategy": {
            "mode": "incremental-manifest",
            "mysqlBackupMode": "full-dump-baseline",
            "mysqlBaseline": "full-dump",
        },
        "recoverySet": {
            "id": "20260606-155715",
            "status": "COMPLETE",
            "program": {"imageTag": "20260606_ui_code_only_onlyoffice_A_1138"},
            "mysql": {"dumpPath": "mysql/ruoyi-vue-pro.sql.gz"},
            "minio": {"bucket": "yudao", "snapshotPath": "objects/manifest-object-inventory.json"},
            "businessFiles": {"snapshotPath": "objects/manifest-object-inventory.json"},
            "dcc": {"manifestPath": "manifest/dcc-backup-manifest.json"},
            "redis": {"policy": "CLEAR_AND_REBUILD"},
            "configuration": {"manifestPath": "deploy/runtime.env", "composePath": "deploy/docker-compose.yml"},
            "checksums": {"path": "manifest/checksums.txt", "sha256": "abc"},
        },
        "validation": {
            "mysqlDumpCreated": True,
            "objectBackupCreated": True,
            "checksumsGenerated": True,
            "rehearsalStatus": "PASSED",
            "lastRehearsedAt": "2026-06-09T06:20:00+08:00",
        },
    }
    manifest_json = json.dumps(manifest, ensure_ascii=False)
    dcc_manifest_json = _dcc_backup_manifest_json()
    script = f"""
$ErrorActionPreference = 'Stop'
$script:commands = @()
$script:manifestJson = @'
{manifest_json}
'@
$script:dccManifestJson = @'
{dcc_manifest_json}
'@
function Write-BackupOpsLog {{ param($Session, $Level, $Message) }}
function Invoke-BackupSshCommand {{
    param($Request)
    $script:commands += $Request.Command
    $command = [string]$Request.Command
    if ($command.StartsWith('find ')) {{
        return [pscustomobject]@{{ output = '/mnt/nas/Backup/BackupPackage/20260606-155715' }}
    }}
    if ($command.StartsWith('test -f ')) {{
        if ($command -match 'mysql/ruoyi-vue-pro\\.sql\\.gz|deploy/image-tag\\.txt|manifest/manifest\\.json|manifest/checksums\\.txt|manifest/dcc-backup-manifest\\.json|objects/manifest-object-inventory\\.json') {{
            return [pscustomobject]@{{ output = 'EXISTS' }}
        }}
        throw [System.InvalidOperationException]::new('missing file')
    }}
    if ($command -match 'cat .*deploy/image-tag\\.txt') {{
        return [pscustomobject]@{{ output = '20260606_ui_code_only_onlyoffice_A_1138' }}
    }}
    if ($command -match 'cat .*manifest/manifest\\.json') {{
        return [pscustomobject]@{{ output = $script:manifestJson }}
    }}
    throw [System.InvalidOperationException]::new("unexpected command: $command")
}}
Import-Module '{docker_module}' -Force -DisableNameChecking
$config = [pscustomobject]@{{
    servers = [pscustomobject]@{{
        test = [pscustomobject]@{{
            host = '172.30.30.58'
            backupPointsRoot = '/mnt/nas/Backup/BackupPackage'
        }}
    }}
    backup = [pscustomobject]@{{
        mysqlDatabase = 'ruoyi-vue-pro'
        objectBucket = 'yudao'
    }}
    ssh = [pscustomobject]@{{
        user = 'root'
        port = 22
    }}
    auth = [pscustomobject]@{{
        sshKeyPath = 'D:/missing_id_rsa'
        knownHostsPath = 'D:/missing_known_hosts'
    }}
}}
$session = [pscustomobject]@{{}}
$candidates = Get-BackupOpsRestoreCandidates -Config $config -LogSession $session
[pscustomobject]@{{
    candidates = @($candidates)
    commands = $script:commands
}} | ConvertTo-Json -Depth 10
"""
    completed = _run_powershell_script(script)
    assert completed.returncode == 0, completed.stdout + completed.stderr
    payload = json.loads(completed.stdout)

    assert payload["candidates"] == []


def test_restore_candidate_scan_limits_remote_probe_to_selected_backup_id() -> None:
    root = _backup_root()
    docker_module = root / "scripts" / "modules" / "Infra" / "DockerOps.psm1"
    manifest = {
        "schemaVersion": "v2",
        "backupId": "20260606-181538",
        "targetEnvironment": "test",
        "targetHost": "172.30.30.58",
        "status": "success",
        "deploy": {"imageTag": "20260606_ui_code_only_onlyoffice_A_1815"},
        "backupStrategy": {
            "mode": "incremental-manifest",
            "mysqlBackupMode": "full-dump-baseline",
            "mysqlBaseline": "full-dump",
        },
        "recoverySet": {
            "id": "20260606-181538",
            "status": "COMPLETE",
            "program": {"imageTag": "20260606_ui_code_only_onlyoffice_A_1815"},
            "mysql": {"dumpPath": "mysql/ruoyi-vue-pro.sql.gz"},
            "minio": {"bucket": "yudao", "snapshotPath": "objects/manifest-object-inventory.json"},
            "businessFiles": {"snapshotPath": "objects/manifest-object-inventory.json"},
            "dcc": {"manifestPath": "manifest/dcc-backup-manifest.json"},
            "redis": {"policy": "CLEAR_AND_REBUILD"},
            "configuration": {"manifestPath": "deploy/runtime.env", "composePath": "deploy/docker-compose.yml"},
            "checksums": {"path": "manifest/checksums.txt", "sha256": "def"},
        },
        "validation": {
            "mysqlDumpCreated": True,
            "objectBackupCreated": True,
            "checksumsGenerated": True,
            "rehearsalStatus": "PASSED",
            "lastRehearsedAt": "2026-06-09T06:20:00+08:00",
        },
    }
    manifest_json = json.dumps(manifest, ensure_ascii=False)
    dcc_manifest_json = _dcc_backup_manifest_json()
    script = f"""
$ErrorActionPreference = 'Stop'
$script:commands = @()
$script:manifestJson = @'
{manifest_json}
'@
$script:dccManifestJson = @'
{dcc_manifest_json}
'@
function Write-BackupOpsLog {{ param($Session, $Level, $Message) }}
function Invoke-BackupSshCommand {{
    param($Request)
    $script:commands += $Request.Command
    $command = [string]$Request.Command
    if ($command -match '20260606-135859') {{
        throw [System.InvalidOperationException]::new('unselected backup point must not be probed')
    }}
    if ($command.StartsWith('find ')) {{
        return [pscustomobject]@{{ output = "/mnt/nas/Backup/BackupPackage/20260606-135859`n/mnt/nas/Backup/BackupPackage/20260606-181538" }}
    }}
    if ($command.StartsWith('test -f ')) {{
        if ($command -match '20260606-181538' -and $command -match 'mysql/ruoyi-vue-pro\\.sql\\.gz|deploy/image-tag\\.txt|manifest/manifest\\.json|manifest/checksums\\.txt|manifest/dcc-backup-manifest\\.json|objects/manifest-object-inventory\\.json') {{
            return [pscustomobject]@{{ output = 'EXISTS' }}
        }}
        throw [System.InvalidOperationException]::new('missing file')
    }}
    if ($command -match 'cat .*20260606-181538.*/deploy/image-tag\\.txt') {{
        return [pscustomobject]@{{ output = '20260606_ui_code_only_onlyoffice_A_1815' }}
    }}
    if ($command -match 'cat .*20260606-181538.*/manifest/manifest\\.json') {{
        return [pscustomobject]@{{ output = $script:manifestJson }}
    }}
    if ($command -match 'cat .*20260606-181538.*/manifest/dcc-backup-manifest\\.json') {{
        return [pscustomobject]@{{ output = $script:dccManifestJson }}
    }}
    throw [System.InvalidOperationException]::new("unexpected command: $command")
}}
Import-Module '{docker_module}' -Force -DisableNameChecking
$config = [pscustomobject]@{{
    servers = [pscustomobject]@{{
        test = [pscustomobject]@{{
            host = '172.30.30.58'
            backupPointsRoot = '/mnt/nas/Backup/BackupPackage'
        }}
    }}
    backup = [pscustomobject]@{{
        mysqlDatabase = 'ruoyi-vue-pro'
        objectBucket = 'yudao'
    }}
    ssh = [pscustomobject]@{{
        user = 'root'
        port = 22
    }}
    auth = [pscustomobject]@{{
        sshKeyPath = 'D:/missing_id_rsa'
        knownHostsPath = 'D:/missing_known_hosts'
    }}
}}
$session = [pscustomobject]@{{}}
$candidates = Get-BackupOpsRestoreCandidates -Config $config -LogSession $session -SelectedBackupId '20260606-181538'
[pscustomobject]@{{
    candidates = $candidates
    commands = $script:commands
}} | ConvertTo-Json -Depth 10
"""
    completed = _run_powershell_script(script)
    assert completed.returncode == 0, completed.stdout + completed.stderr
    payload = json.loads(completed.stdout)
    candidates = payload["candidates"]
    if isinstance(candidates, dict):
        candidates = [candidates]

    assert len(candidates) == 1
    assert candidates[0]["backupId"] == "20260606-181538"
    assert candidates[0]["checksumsSha256"] == "def"
    assert not any(command.startswith("find ") for command in payload["commands"])
    assert not any("20260606-135859" in command for command in payload["commands"])


def _run_rehearsal_candidate_fixture(
    manifest: dict,
    dcc_manifest: dict[str, object] | None = None,
) -> subprocess.CompletedProcess[str]:
    root = _backup_root()
    docker_module = root / "scripts" / "modules" / "Infra" / "DockerOps.psm1"
    backup_id = manifest.get("backupId", "20260606-181538")
    manifest_json = json.dumps(manifest, ensure_ascii=False)
    dcc_manifest_json = _dcc_backup_manifest_json(dcc_manifest)
    script = f"""
$ErrorActionPreference = 'Stop'
$script:commands = @()
$script:manifestJson = @'
{manifest_json}
'@
$script:dccManifestJson = @'
{dcc_manifest_json}
'@
function Write-BackupOpsLog {{ param($Session, $Level, $Message) }}
function Invoke-BackupSshCommand {{
    param($Request)
    $script:commands += $Request.Command
    $command = [string]$Request.Command
    if ($command.StartsWith('test -f ')) {{
        if ($command -match 'mysql/ruoyi-vue-pro\\.sql\\.gz|deploy/image-tag\\.txt|manifest/manifest\\.json|manifest/checksums\\.txt|manifest/dcc-backup-manifest\\.json|objects/manifest-object-inventory\\.json') {{
            return [pscustomobject]@{{ output = 'EXISTS' }}
        }}
        throw [System.InvalidOperationException]::new('missing file')
    }}
    if ($command.StartsWith('test -d ') -and $command -match 'objects/yudao') {{
        throw [System.InvalidOperationException]::new('legacy object bucket directory is absent')
    }}
    if ($command -match 'cat .*deploy/image-tag\\.txt') {{
        return [pscustomobject]@{{ output = '20260606_ui_code_only_onlyoffice_A_1815' }}
    }}
    if ($command -match 'cat .*manifest/manifest\\.json') {{
        return [pscustomobject]@{{ output = $script:manifestJson }}
    }}
    if ($command -match 'cat .*manifest/dcc-backup-manifest\\.json') {{
        return [pscustomobject]@{{ output = $script:dccManifestJson }}
    }}
    throw [System.InvalidOperationException]::new("unexpected command: $command")
}}
$module = Import-Module '{docker_module}' -Force -DisableNameChecking -PassThru
$config = [pscustomobject]@{{
    servers = [pscustomobject]@{{
        test = [pscustomobject]@{{
            host = '172.30.30.58'
            backupPointsRoot = '/mnt/nas/Backup/BackupPackage'
        }}
    }}
    backup = [pscustomobject]@{{
        mysqlDatabase = 'ruoyi-vue-pro'
        objectBucket = 'yudao'
    }}
    ssh = [pscustomobject]@{{
        user = 'root'
        port = 22
    }}
    auth = [pscustomobject]@{{
        sshKeyPath = 'D:/missing_id_rsa'
        knownHostsPath = 'D:/missing_known_hosts'
    }}
}}
$session = [pscustomobject]@{{}}
$candidate = Get-BackupOpsRehearsalCandidate -Config $config -BackupId '{backup_id}' -LogSession $session
[pscustomobject]@{{
    candidate = $candidate
    commands = $script:commands
}} | ConvertTo-Json -Depth 10
"""
    return _run_powershell_script(script)


def test_rehearsal_candidate_accepts_inventory_only_object_backup() -> None:
    manifest = {
        "schemaVersion": "v2",
        "backupId": "20260606-181538",
        "targetEnvironment": "test",
        "targetHost": "172.30.30.58",
        "status": "success",
        "deploy": {"imageTag": "20260606_ui_code_only_onlyoffice_A_1815"},
        "backupStrategy": {
            "mode": "incremental-manifest",
            "mysqlBackupMode": "full-dump-baseline",
            "mysqlBaseline": "full-dump",
        },
        "recoverySet": {
            "id": "20260606-181538",
            "status": "COMPLETE",
            "program": {"imageTag": "20260606_ui_code_only_onlyoffice_A_1815"},
            "mysql": {"dumpPath": "mysql/ruoyi-vue-pro.sql.gz"},
            "minio": {"bucket": "yudao", "snapshotPath": "objects/manifest-object-inventory.json"},
            "businessFiles": {"snapshotPath": "objects/manifest-object-inventory.json"},
            "dcc": {"manifestPath": "manifest/dcc-backup-manifest.json"},
            "redis": {"policy": "CLEAR_AND_REBUILD"},
            "configuration": {"manifestPath": "deploy/runtime.env", "composePath": "deploy/docker-compose.yml"},
            "checksums": {"path": "manifest/checksums.txt", "sha256": "def"},
        },
        "validation": {
            "mysqlDumpCreated": True,
            "objectBackupCreated": True,
            "checksumsGenerated": True,
            "rehearsalStatus": "unverified",
            "lastRehearsedAt": None,
        },
    }
    completed = _run_rehearsal_candidate_fixture(manifest)
    assert completed.returncode == 0, completed.stdout + completed.stderr
    payload = json.loads(completed.stdout)

    assert payload["candidate"]["backupId"] == "20260606-181538"
    assert payload["candidate"]["imageTag"] == "20260606_ui_code_only_onlyoffice_A_1815"
    assert any("objects/manifest-object-inventory.json" in command for command in payload["commands"])
    assert not any("objects/yudao" in command for command in payload["commands"])


def test_rehearsal_candidate_blocks_invalid_dcc_backup_chain() -> None:
    manifest = {
        "schemaVersion": "v2",
        "backupId": "20260606-181538",
        "targetEnvironment": "test",
        "targetHost": "172.30.30.58",
        "status": "success",
        "deploy": {"imageTag": "20260606_ui_code_only_onlyoffice_A_1815"},
        "backupStrategy": {
            "mode": "incremental-manifest",
            "mysqlBackupMode": "full-dump-baseline",
            "mysqlBaseline": "full-dump",
        },
        "recoverySet": {
            "id": "20260606-181538",
            "status": "COMPLETE",
            "program": {"imageTag": "20260606_ui_code_only_onlyoffice_A_1815"},
            "mysql": {"dumpPath": "mysql/ruoyi-vue-pro.sql.gz"},
            "minio": {"bucket": "yudao", "snapshotPath": "objects/manifest-object-inventory.json"},
            "businessFiles": {"snapshotPath": "objects/manifest-object-inventory.json"},
            "dcc": {"manifestPath": "manifest/dcc-backup-manifest.json"},
            "redis": {"policy": "CLEAR_AND_REBUILD"},
            "configuration": {"manifestPath": "deploy/runtime.env", "composePath": "deploy/docker-compose.yml"},
            "checksums": {"path": "manifest/checksums.txt", "sha256": "def"},
        },
        "validation": {
            "mysqlDumpCreated": True,
            "objectBackupCreated": True,
            "checksumsGenerated": True,
            "rehearsalStatus": "unverified",
            "lastRehearsedAt": None,
        },
    }
    completed = _run_rehearsal_candidate_fixture(
        manifest,
        dcc_manifest=_invalid_deleted_dcc_backup_manifest("20260606-181538"),
    )

    assert completed.returncode != 0
    output = completed.stdout + completed.stderr
    assert "dcc_delete_event_missing" in output


def test_rehearsal_candidate_blocks_manifest_without_backup_strategy() -> None:
    manifest = {
        "schemaVersion": "v2",
        "backupId": "20260606-181538",
        "targetEnvironment": "test",
        "targetHost": "172.30.30.58",
        "status": "success",
        "deploy": {"imageTag": "20260606_ui_code_only_onlyoffice_A_1815"},
        "recoverySet": {
            "id": "20260606-181538",
            "status": "COMPLETE",
            "program": {"imageTag": "20260606_ui_code_only_onlyoffice_A_1815"},
            "mysql": {"dumpPath": "mysql/ruoyi-vue-pro.sql.gz"},
            "minio": {"bucket": "yudao", "snapshotPath": "objects/manifest-object-inventory.json"},
            "businessFiles": {"snapshotPath": "objects/manifest-object-inventory.json"},
            "dcc": {"manifestPath": "manifest/dcc-backup-manifest.json"},
            "redis": {"policy": "CLEAR_AND_REBUILD"},
            "configuration": {"manifestPath": "deploy/runtime.env", "composePath": "deploy/docker-compose.yml"},
            "checksums": {"path": "manifest/checksums.txt", "sha256": "def"},
        },
        "validation": {
            "mysqlDumpCreated": True,
            "objectBackupCreated": True,
            "checksumsGenerated": True,
            "rehearsalStatus": "unverified",
            "lastRehearsedAt": None,
        },
    }
    completed = _run_rehearsal_candidate_fixture(manifest)

    assert completed.returncode != 0
    assert "backupStrategy" in (completed.stdout + completed.stderr)


def test_rehearsal_candidate_blocks_manifest_without_dcc_backup_manifest() -> None:
    manifest = {
        "schemaVersion": "v2",
        "backupId": "20260606-181538",
        "targetEnvironment": "test",
        "targetHost": "172.30.30.58",
        "status": "success",
        "deploy": {"imageTag": "20260606_ui_code_only_onlyoffice_A_1815"},
        "backupStrategy": {
            "mode": "incremental-manifest",
            "mysqlBackupMode": "full-dump-baseline",
            "mysqlBaseline": "full-dump",
        },
        "recoverySet": {
            "id": "20260606-181538",
            "status": "COMPLETE",
            "program": {"imageTag": "20260606_ui_code_only_onlyoffice_A_1815"},
            "mysql": {"dumpPath": "mysql/ruoyi-vue-pro.sql.gz"},
            "minio": {"bucket": "yudao", "snapshotPath": "objects/manifest-object-inventory.json"},
            "businessFiles": {"snapshotPath": "objects/manifest-object-inventory.json"},
            "redis": {"policy": "CLEAR_AND_REBUILD"},
            "configuration": {"manifestPath": "deploy/runtime.env", "composePath": "deploy/docker-compose.yml"},
            "checksums": {"path": "manifest/checksums.txt", "sha256": "def"},
        },
        "validation": {
            "mysqlDumpCreated": True,
            "objectBackupCreated": True,
            "checksumsGenerated": True,
            "rehearsalStatus": "unverified",
            "lastRehearsedAt": None,
        },
    }
    completed = _run_rehearsal_candidate_fixture(manifest)

    assert completed.returncode != 0
    assert "recoverySet.dcc.manifestPath" in (completed.stdout + completed.stderr)


def test_rehearsal_candidate_blocks_manifest_without_target_proof() -> None:
    manifest = {
        "schemaVersion": "v2",
        "backupId": "20260606-181538",
        "status": "success",
        "deploy": {"imageTag": "20260606_ui_code_only_onlyoffice_A_1815"},
        "backupStrategy": {
            "mode": "incremental-manifest",
            "mysqlBackupMode": "full-dump-baseline",
            "mysqlBaseline": "full-dump",
        },
        "recoverySet": {
            "id": "20260606-181538",
            "status": "COMPLETE",
            "program": {"imageTag": "20260606_ui_code_only_onlyoffice_A_1815"},
            "checksums": {"path": "manifest/checksums.txt", "sha256": "def"},
        },
        "validation": {
            "mysqlDumpCreated": True,
            "objectBackupCreated": True,
            "checksumsGenerated": True,
        },
    }
    completed = _run_rehearsal_candidate_fixture(manifest)

    assert completed.returncode != 0
    assert "target" in (completed.stdout + completed.stderr)
    assert "172.30.30.58" in (completed.stdout + completed.stderr)


def test_rehearsal_candidate_blocks_manifest_without_complete_recovery_set() -> None:
    manifest = {
        "schemaVersion": "v2",
        "backupId": "20260606-181538",
        "targetEnvironment": "test",
        "targetHost": "172.30.30.58",
        "status": "success",
        "deploy": {"imageTag": "20260606_ui_code_only_onlyoffice_A_1815"},
        "backupStrategy": {
            "mode": "incremental-manifest",
            "mysqlBackupMode": "full-dump-baseline",
            "mysqlBaseline": "full-dump",
        },
        "recoverySet": {
            "id": "20260606-181538",
            "status": "PARTIAL",
            "program": {"imageTag": "20260606_ui_code_only_onlyoffice_A_1815"},
            "checksums": {"path": "manifest/checksums.txt", "sha256": ""},
        },
        "validation": {
            "mysqlDumpCreated": True,
            "objectBackupCreated": True,
            "checksumsGenerated": True,
        },
    }
    completed = _run_rehearsal_candidate_fixture(manifest)

    assert completed.returncode != 0
    assert "recoverySet" in (completed.stdout + completed.stderr)
    assert "COMPLETE" in (completed.stdout + completed.stderr)


def test_backup_ssh_short_reads_are_bounded_by_native_process_timeout() -> None:
    root = _backup_root()
    ssh_ops_text = (root / "scripts" / "modules" / "Infra" / "SshOps.psm1").read_text(encoding="utf-8")
    docker_ops_text = (root / "scripts" / "modules" / "Infra" / "DockerOps.psm1").read_text(encoding="utf-8")

    assert "[int]$TimeoutSeconds = 0" in ssh_ops_text
    assert "WaitForExit($TimeoutSeconds * 1000)" in ssh_ops_text
    assert "Stop-Process -Id $process.Id -Force" in ssh_ops_text
    assert "$process.Refresh()" in ssh_ops_text
    assert "'ConnectTimeout=10'" in ssh_ops_text
    assert "$script:BackupOpsDefaultSshTimeoutSeconds = 300" in ssh_ops_text
    assert "$script:BackupOpsDefaultScpTimeoutSeconds = 300" in ssh_ops_text
    assert "else { $script:BackupOpsDefaultSshTimeoutSeconds }" in ssh_ops_text
    assert "else { $script:BackupOpsDefaultScpTimeoutSeconds }" in ssh_ops_text
    assert "$arguments += '-n'" in ssh_ops_text
    assert "TimeoutSeconds = 60" in docker_ops_text


def test_backup_native_process_drains_large_stdout_before_waiting_for_timeout() -> None:
    root = _backup_root()
    ssh_module = root / "scripts" / "modules" / "Infra" / "SshOps.psm1"
    child_script = "$text = 'x' * 200000; [Console]::Out.Write($text)"

    script = f"""
$ErrorActionPreference = 'Stop'
Import-Module '{ssh_module}' -Force -DisableNameChecking
$module = Get-Module | Where-Object {{ $_.Path -eq '{ssh_module}' }} | Select-Object -First 1
$childScript = @'
{child_script}
'@
$result = & $module {{
    param($ChildScript)
    Invoke-BackupNativeProcess -FilePath 'powershell' -ArgumentList @('-NoProfile', '-Command', $ChildScript) -TimeoutSeconds 5
}} $childScript
[pscustomobject]@{{
    exitCode = $result.ExitCode
    stdoutLength = $result.StdOut.Length
    stderr = $result.StdErr
}} | ConvertTo-Json -Depth 4
"""
    completed = _run_powershell_script(script)
    assert completed.returncode == 0, completed.stdout + completed.stderr
    payload = json.loads(completed.stdout)

    assert payload["exitCode"] == 0
    assert payload["stdoutLength"] == 200000
    assert "timed out" not in payload["stderr"]


def test_remote_nas_mount_is_checked_before_backup_now_exports_mysql() -> None:
    root = _backup_root()
    backup_now_text = (root / "scripts" / "modules" / "UseCases" / "BackupNow.psm1").read_text(encoding="utf-8")

    assert "Assert-BackupOpsRemoteNasMounted" in backup_now_text
    assert backup_now_text.index("Assert-BackupOpsRemoteNasMounted") < backup_now_text.index("Export-BackupOpsMySqlDump")


def test_backup_use_cases_require_dcc_manifest_before_checksums_and_sync() -> None:
    root = _backup_root()
    backup_now_text = (root / "scripts" / "modules" / "UseCases" / "BackupNow.psm1").read_text(encoding="utf-8")
    backup_scheduled_text = (root / "scripts" / "modules" / "UseCases" / "BackupScheduled.psm1").read_text(
        encoding="utf-8"
    )
    file_ops_text = (root / "scripts" / "modules" / "Infra" / "FileOps.psm1").read_text(encoding="utf-8")

    assert "function New-BackupOpsDccBackupManifest" in file_ops_text
    assert "function Assert-BackupOpsDccBackupManifestReady" in file_ops_text
    export_section = file_ops_text.split("Export-ModuleMember", 1)[1]
    assert "New-BackupOpsDccBackupManifest" in export_section
    assert "Assert-BackupOpsDccBackupManifestReady" in export_section

    for use_case_text in (backup_now_text, backup_scheduled_text):
        assert "New-BackupOpsDccBackupManifest" in use_case_text
        assert "Assert-BackupOpsDccBackupManifestReady" in use_case_text
        assert use_case_text.index("Backup-BackupOpsObjectBucket") < use_case_text.index(
            "New-BackupOpsDccBackupManifest"
        )
        assert use_case_text.index("New-BackupOpsDccBackupManifest") < use_case_text.index(
            "Assert-BackupOpsDccBackupManifestReady"
        )
        assert use_case_text.index("Assert-BackupOpsDccBackupManifestReady") < use_case_text.index(
            "New-BackupOpsChecksums"
        )
        assert use_case_text.index("Assert-BackupOpsDccBackupManifestReady") < use_case_text.index(
            "Sync-BackupOpsBackupToTestServer"
        )


def test_new_backup_ops_dcc_backup_manifest_generates_from_query_fixture_and_object_inventory() -> None:
    root = _backup_root()
    file_module = root / "scripts" / "modules" / "Infra" / "FileOps.psm1"

    with tempfile.TemporaryDirectory() as temp_dir:
        temp_root = Path(temp_dir)
        backup_root = temp_root / "20260609-030000"
        manifest_dir = backup_root / "manifest"
        objects_dir = backup_root / "objects"
        query_path = temp_root / "dcc-query-result.json"
        objects_dir.mkdir(parents=True)
        manifest_dir.mkdir(parents=True)
        query_path.write_text(
            json.dumps(
                {
                    "schemaVersion": "dcc-database-query-v1",
                    "rows": [
                        {
                            "controlledFileId": 100,
                            "tenantId": 122,
                            "fileNumber": "DCC-100",
                            "versionNo": "V1.0",
                            "status": "PUBLISHED",
                            "updatedAt": "2026-06-09T09:00:00+08:00",
                            "objectRole": "published",
                            "objectFileId": 9100,
                            "objectPath": "dcc/published/a.pdf",
                            "objectSha256": "sha256:" + "a" * 64,
                            "permissionDigest": "permission-v1",
                        }
                    ],
                },
                ensure_ascii=False,
                indent=2,
            )
            + "\n",
            encoding="utf-8",
        )
        (objects_dir / "manifest-object-inventory.json").write_text(
            json.dumps(
                {
                    "mode": "incremental-manifest",
                    "bucket": "yudao",
                    "objectStoreRoot": "/mnt/nas/Backup/BackupPackage/object-store",
                    "stats": {"addedCount": 1, "modifiedCount": 0, "deletedCount": 0, "reusedCount": 0},
                    "objects": [
                        {
                            "path": "dcc/published/a.pdf",
                            "sha256": "sha256:" + "a" * 64,
                            "repositoryKey": "sha256:" + "a" * 64,
                            "size": 123,
                            "lastModified": "2026-06-09T09:00:00+08:00",
                            "status": "active",
                        }
                    ],
                },
                ensure_ascii=False,
                indent=2,
            )
            + "\n",
            encoding="utf-8",
        )
        script = f"""
$ErrorActionPreference = 'Stop'
Import-Module '{file_module}' -Force -DisableNameChecking
$config = [pscustomobject]@{{
    environment = 'test'
    servers = [pscustomobject]@{{
        production = [pscustomobject]@{{
            host = '172.30.30.58'
            appDir = '/opt/intruoyi/runtime'
        }}
    }}
    backup = [pscustomobject]@{{
        dccTenantId = 122
        dccSnapshotQueryResultPath = '{query_path}'
    }}
}}
$workspace = [pscustomobject]@{{
    BackupId = '20260609-030000'
    BackupRoot = '{backup_root}'
    ObjectsPath = '{objects_dir}'
    ManifestPath = '{manifest_dir}'
    DeployPath = '{backup_root / "deploy"}'
}}
$logSession = [pscustomobject]@{{ messages = @() }}
function Write-BackupOpsLog {{
    param($Session, $Level, $Message)
    $Session.messages += $Message
}}
$path = New-BackupOpsDccBackupManifest -Config $config -Workspace $workspace -LogSession $logSession
$manifest = [System.IO.File]::ReadAllText($path, [System.Text.Encoding]::UTF8) | ConvertFrom-Json
[pscustomobject]@{{
    path = $path
    status = $manifest.status
    targetEnvironment = $manifest.targetEnvironment
    targetHost = $manifest.targetHost
    recordCount = @($manifest.databaseRecords).Count
    firstFileKey = $manifest.databaseRecords[0].fileKey
    snapshotExists = Test-Path -LiteralPath (Join-Path '{manifest_dir}' 'dcc-database-snapshot.json')
}} | ConvertTo-Json -Depth 8
"""
        completed = _run_powershell_script(script)
        assert completed.returncode == 0, completed.stdout + completed.stderr
        payload = json.loads(completed.stdout)

    assert payload["path"].endswith("manifest\\dcc-backup-manifest.json")
    assert payload["status"] == "success"
    assert payload["targetEnvironment"] == "test"
    assert payload["targetHost"] == "172.30.30.58"
    assert payload["recordCount"] == 1
    assert payload["firstFileKey"] == "controlled-file:100"
    assert payload["snapshotExists"] is True


def test_new_backup_ops_dcc_backup_manifest_skips_blocked_auto_previous_manifest() -> None:
    root = _backup_root()
    file_module = root / "scripts" / "modules" / "Infra" / "FileOps.psm1"

    with tempfile.TemporaryDirectory() as temp_dir:
        temp_root = Path(temp_dir)
        backup_root = temp_root / "20260609-030000"
        manifest_dir = backup_root / "manifest"
        objects_dir = backup_root / "objects"
        query_path = temp_root / "dcc-query-result.json"
        blocked_previous_dir = temp_root / "20260609-020000" / "manifest"
        valid_previous_dir = temp_root / "20260609-010000" / "manifest"
        objects_dir.mkdir(parents=True)
        manifest_dir.mkdir(parents=True)
        blocked_previous_dir.mkdir(parents=True)
        valid_previous_dir.mkdir(parents=True)
        (blocked_previous_dir / "dcc-backup-manifest.json").write_text(
            json.dumps(
                {
                    "status": "blocked",
                    "backupId": "20260609-020000",
                    "restorePointId": "20260609-020000",
                    "errors": [{"code": "previous_restore_point_missing"}],
                },
                ensure_ascii=False,
                indent=2,
            )
            + "\n",
            encoding="utf-8",
        )
        (valid_previous_dir / "dcc-backup-manifest.json").write_text(
            json.dumps(_valid_dcc_backup_manifest("20260609-010000"), ensure_ascii=False, indent=2)
            + "\n",
            encoding="utf-8",
        )
        query_path.write_text(
            json.dumps(
                {
                    "schemaVersion": "dcc-database-query-v1",
                    "rows": [
                        {
                            "controlledFileId": 100,
                            "tenantId": 122,
                            "fileNumber": "DCC-100",
                            "versionNo": "V1.0",
                            "status": "PUBLISHED",
                            "updatedAt": "2026-06-09T09:00:00+08:00",
                            "objectRole": "published",
                            "objectFileId": 9100,
                            "objectPath": "dcc/published/a.pdf",
                            "objectSha256": "sha256:" + "a" * 64,
                            "permissionDigest": "permission-v1",
                        }
                    ],
                },
                ensure_ascii=False,
                indent=2,
            )
            + "\n",
            encoding="utf-8",
        )
        (objects_dir / "manifest-object-inventory.json").write_text(
            json.dumps(
                {
                    "mode": "incremental-manifest",
                    "bucket": "yudao",
                    "objectStoreRoot": "/mnt/nas/Backup/BackupPackage/object-store",
                    "stats": {"addedCount": 1, "modifiedCount": 0, "deletedCount": 0, "reusedCount": 0},
                    "objects": [
                        {
                            "path": "dcc/published/a.pdf",
                            "sha256": "sha256:" + "a" * 64,
                            "repositoryKey": "sha256:" + "a" * 64,
                            "size": 123,
                            "lastModified": "2026-06-09T09:00:00+08:00",
                            "status": "active",
                        }
                    ],
                },
                ensure_ascii=False,
                indent=2,
            )
            + "\n",
            encoding="utf-8",
        )
        script = f"""
$ErrorActionPreference = 'Stop'
Import-Module '{file_module}' -Force -DisableNameChecking
$config = [pscustomobject]@{{
    environment = 'test'
    servers = [pscustomobject]@{{
        production = [pscustomobject]@{{
            host = '172.30.30.58'
            appDir = '/opt/intruoyi/runtime'
        }}
    }}
    backup = [pscustomobject]@{{
        dccTenantId = 122
        dccSnapshotQueryResultPath = '{query_path}'
        localWorkspaceRoot = '{temp_root}'
    }}
}}
$workspace = [pscustomobject]@{{
    BackupId = '20260609-030000'
    BackupRoot = '{backup_root}'
    ObjectsPath = '{objects_dir}'
    ManifestPath = '{manifest_dir}'
    DeployPath = '{backup_root / "deploy"}'
}}
$logSession = [pscustomobject]@{{ messages = @() }}
function Write-BackupOpsLog {{
    param($Session, $Level, $Message)
    $Session.messages += $Message
}}
$path = New-BackupOpsDccBackupManifest -Config $config -Workspace $workspace -LogSession $logSession
$manifest = [System.IO.File]::ReadAllText($path, [System.Text.Encoding]::UTF8) | ConvertFrom-Json
[pscustomobject]@{{
    status = $manifest.status
    fullBaseline = $manifest.fullBaseline.restorePointId
    lastIncrementFrom = $manifest.incrementalChain[-1].from
    lastIncrementTo = $manifest.incrementalChain[-1].to
}} | ConvertTo-Json -Depth 8
"""
        completed = _run_powershell_script(script)
        assert completed.returncode == 0, completed.stdout + completed.stderr
        payload = json.loads(completed.stdout)

    assert payload["status"] == "success"
    assert payload["fullBaseline"] == "B1"
    assert payload["lastIncrementFrom"] == "B1"
    assert payload["lastIncrementTo"] == "20260609-030000"


def test_dcc_chain_plan_restore_replays_baseline_incremental_states() -> None:
    root = _backup_root()
    validator_module = root / "scripts" / "modules" / "Core" / "DccBackupChainValidator.psm1"
    hash_a = "sha256:" + "a" * 64
    hash_b1 = "sha256:" + "b" * 64
    hash_b2 = "sha256:" + "c" * 64
    hash_preview = "sha256:" + "d" * 64
    manifest = {
        "schemaVersion": "dcc-backup-manifest-v1",
        "backupId": "B5",
        "targetEnvironment": "test",
        "targetHost": "172.30.30.58",
        "status": "success",
        "chainStatus": "COMPLETE",
        "backupMode": "incremental",
        "baselineBackupId": "B1",
        "baselineRestorePointId": "B1",
        "previousBackupId": "B4",
        "previousRestorePointId": "B4",
        "restoreVerified": False,
        "restoreRehearsal": {"status": "not-run"},
        "fullBaseline": {"restorePointId": "B1", "checksum": hash_a},
        "incrementalChain": [
            {"from": "B1", "to": "B3", "checksum": hash_b1},
            {"from": "B3", "to": "B4", "checksum": hash_b2},
            {"from": "B4", "to": "B5", "checksum": hash_preview},
        ],
        "restorePoints": [
            {"id": "B1", "databaseRestorePointId": "B1", "objectInventoryRestorePointId": "B1"},
            {"id": "B3", "databaseRestorePointId": "B3", "objectInventoryRestorePointId": "B3"},
            {"id": "B4", "databaseRestorePointId": "B4", "objectInventoryRestorePointId": "B4"},
            {"id": "B5", "databaseRestorePointId": "B5", "objectInventoryRestorePointId": "B5"},
        ],
        "objectInventories": [
            {"restorePointId": "B1", "objects": [{"fileKey": "DCC-A", "objectRole": "source", "state": "active", "contentHash": hash_a, "storedHash": hash_a, "present": True}]},
            {"restorePointId": "B3", "objects": [
                {"fileKey": "DCC-A", "objectRole": "source", "state": "active", "contentHash": hash_a, "storedHash": hash_a, "present": True},
                {"fileKey": "DCC-B", "objectRole": "source", "state": "active", "contentHash": hash_b1, "storedHash": hash_b1, "present": True},
                {"fileKey": "DCC-B", "objectRole": "preview", "state": "active", "contentHash": hash_preview, "storedHash": hash_preview, "present": True},
            ]},
            {"restorePointId": "B4", "objects": [
                {"fileKey": "DCC-A", "objectRole": "source", "state": "active", "contentHash": hash_a, "storedHash": hash_a, "present": True},
                {"fileKey": "DCC-B", "objectRole": "source", "state": "active", "contentHash": hash_b2, "storedHash": hash_b2, "present": True},
                {"fileKey": "DCC-B", "objectRole": "preview", "state": "active", "contentHash": hash_preview, "storedHash": hash_preview, "present": True},
            ]},
            {"restorePointId": "B5", "objects": [
                {"fileKey": "DCC-A", "objectRole": "source", "state": "active", "contentHash": hash_a, "storedHash": hash_a, "present": True},
                {"fileKey": "DCC-B", "objectRole": "source", "state": "deleted", "contentHash": hash_b2, "storedHash": hash_b2, "present": False},
                {"fileKey": "DCC-C", "objectRole": "source", "state": "voided", "contentHash": hash_preview, "storedHash": hash_preview, "present": True},
            ]},
        ],
        "databaseRecords": [
            {"restorePointId": "B1", "fileKey": "DCC-A", "state": "active", "versionNo": "V1.0", "permissionDigest": "perm-a"},
            {"restorePointId": "B3", "fileKey": "DCC-B", "state": "active", "versionNo": "V1.0", "permissionDigest": "perm-b1"},
            {"restorePointId": "B4", "fileKey": "DCC-B", "state": "active", "versionNo": "V2.0", "permissionDigest": "perm-b2", "permissionChanged": True},
            {"restorePointId": "B5", "fileKey": "DCC-B", "state": "deleted", "versionNo": "V2.0", "permissionDigest": "perm-b2"},
            {"restorePointId": "B5", "fileKey": "DCC-C", "state": "voided", "versionNo": "V1.0", "permissionDigest": "perm-c"},
        ],
        "dccEvents": [
            {"restorePointId": "B4", "fileKey": "DCC-B", "eventType": "permission_change"},
            {"restorePointId": "B5", "fileKey": "DCC-B", "eventType": "delete"},
            {"restorePointId": "B5", "fileKey": "DCC-C", "eventType": "void"},
        ],
    }
    with tempfile.TemporaryDirectory() as temp_dir:
        temp_root = Path(temp_dir)
        manifest_path = temp_root / "dcc-backup-manifest.json"
        output_path = temp_root / "plan.json"
        manifest_path.write_text(json.dumps(manifest, ensure_ascii=False), encoding="utf-8")
        script = f"""
$ErrorActionPreference = 'Stop'
Import-Module '{validator_module}' -Force -DisableNameChecking
$null = Invoke-DccBackupChainValidation -Mode plan-restore -BackupManifestPath '{manifest_path}' -RestorePoint 'B4' -OutputPath '{output_path}'
$payloadB4 = [System.IO.File]::ReadAllText('{output_path}', [System.Text.Encoding]::UTF8) | ConvertFrom-Json
$null = Invoke-DccBackupChainValidation -Mode plan-restore -BackupManifestPath '{manifest_path}' -RestorePoint 'B5' -OutputPath '{output_path}'
$payloadB5 = [System.IO.File]::ReadAllText('{output_path}', [System.Text.Encoding]::UTF8) | ConvertFrom-Json
[pscustomobject]@{{
    b4Status = $payloadB4.status
    b4Segments = @($payloadB4.restorePlan.segments).Count
    b4FileB = ($payloadB4.restorePlan.finalFiles | Where-Object {{ $_.fileKey -eq 'DCC-B' }})
    b5Status = $payloadB5.status
    b5Segments = @($payloadB5.restorePlan.segments).Count
    b5FileB = ($payloadB5.restorePlan.finalFiles | Where-Object {{ $_.fileKey -eq 'DCC-B' }})
    b5FileC = ($payloadB5.restorePlan.finalFiles | Where-Object {{ $_.fileKey -eq 'DCC-C' }})
}} | ConvertTo-Json -Depth 12
"""
        completed = _run_powershell_script(script)
        assert completed.returncode == 0, completed.stdout + completed.stderr
        payload = json.loads(completed.stdout)

    assert payload["b4Status"] == "passed"
    assert payload["b4Segments"] == 2
    assert payload["b4FileB"]["versionNo"] == "V2.0"
    assert payload["b4FileB"]["permissionDigest"] == "perm-b2"
    assert sorted(item["objectRole"] for item in payload["b4FileB"]["objects"]) == ["preview", "source"]
    assert payload["b5Status"] == "passed"
    assert payload["b5Segments"] == 3
    assert payload["b5FileB"]["state"] == "deleted"
    assert payload["b5FileB"]["objects"] == []
    assert payload["b5FileC"]["state"] == "voided"


def test_new_backup_ops_dcc_backup_manifest_queries_test_mysql_when_no_query_fixture() -> None:
    root = _backup_root()
    file_module = root / "scripts" / "modules" / "Infra" / "FileOps.psm1"

    with tempfile.TemporaryDirectory() as temp_dir:
        temp_root = Path(temp_dir)
        backup_root = temp_root / "20260609-031500"
        manifest_dir = backup_root / "manifest"
        objects_dir = backup_root / "objects"
        objects_dir.mkdir(parents=True)
        manifest_dir.mkdir(parents=True)
        (objects_dir / "manifest-object-inventory.json").write_text(
            json.dumps(
                {
                    "mode": "incremental-manifest",
                    "bucket": "yudao",
                    "objectStoreRoot": "/mnt/nas/Backup/BackupPackage/object-store",
                    "stats": {"addedCount": 1, "modifiedCount": 0, "deletedCount": 0, "reusedCount": 0},
                    "objects": [
                        {
                            "path": "dcc/published/remote.pdf",
                            "sha256": "sha256:" + "b" * 64,
                            "repositoryKey": "sha256:" + "b" * 64,
                            "size": 456,
                            "lastModified": "2026-06-09T09:15:00+08:00",
                            "status": "active",
                        }
                    ],
                },
                ensure_ascii=False,
                indent=2,
            )
            + "\n",
            encoding="utf-8",
        )
        tsv = (
            "controlledFileId\ttenantId\tfileNumber\tversionNo\tstatus\tupdatedAt\tobjectRole\tobjectFileId\tobjectPath\tobjectSha256\tpermissionDigest\n"
            + "101\t122\tDCC-101\tV1.0\tPUBLISHED\t2026-06-09T09:15:00+08:00\tpublished\t9101\tdcc/published/remote.pdf\tsha256:"
            + "b" * 64
            + "\tpermission-remote\n"
        )
        script = f"""
$ErrorActionPreference = 'Stop'
$script:commands = @()
$script:queryOutput = @'
{tsv.rstrip()}
'@
function Invoke-BackupSshCommand {{
    param($Request)
    $script:commands += $Request.Command
    if ($Request.Command -like '*cat*runtime/.env*') {{
        return [pscustomobject]@{{ output = 'MYSQL_ROOT_PASSWORD=secret' }}
    }}
    return [pscustomobject]@{{ output = $script:queryOutput }}
}}
function Write-BackupOpsLog {{
    param($Session, $Level, $Message)
    $Session.messages += $Message
}}
Import-Module '{file_module}' -Force -DisableNameChecking
$config = [pscustomobject]@{{
    environment = 'test'
    servers = [pscustomobject]@{{
        production = [pscustomobject]@{{
            host = '172.30.30.58'
            appDir = '/opt/intruoyi/runtime'
        }}
    }}
    ssh = [pscustomobject]@{{
        user = 'root'
        port = 22
    }}
    auth = [pscustomobject]@{{
        sshKeyPath = 'D:/test_id_rsa'
        knownHostsPath = 'D:/test_known_hosts'
    }}
    containers = [pscustomobject]@{{
        mysql = 'intruoyi-mysql'
    }}
    backup = [pscustomobject]@{{
        dccTenantId = 122
        mysqlDatabase = 'ruoyi-vue-pro'
    }}
}}
$workspace = [pscustomobject]@{{
    BackupId = '20260609-031500'
    BackupRoot = '{backup_root}'
    ObjectsPath = '{objects_dir}'
    ManifestPath = '{manifest_dir}'
    DeployPath = '{backup_root / "deploy"}'
}}
$logSession = [pscustomobject]@{{ messages = @() }}
$path = New-BackupOpsDccBackupManifest -Config $config -Workspace $workspace -LogSession $logSession
$manifest = [System.IO.File]::ReadAllText($path, [System.Text.Encoding]::UTF8) | ConvertFrom-Json
[pscustomobject]@{{
    status = $manifest.status
    recordCount = @($manifest.databaseRecords).Count
    firstFileKey = $manifest.databaseRecords[0].fileKey
    queryFileExists = Test-Path -LiteralPath (Join-Path '{manifest_dir}' 'dcc-database-query.tsv')
    commandText = ($script:commands -join "`n")
}} | ConvertTo-Json -Depth 8
"""
        completed = _run_powershell_script(script)
        assert completed.returncode == 0, completed.stdout + completed.stderr
        payload = json.loads(completed.stdout)

    assert payload["status"] == "success"
    assert payload["recordCount"] == 1
    assert payload["firstFileKey"] == "controlled-file:101"
    assert payload["queryFileExists"] is True
    assert "docker exec" in payload["commandText"]
    assert "mysql --batch --raw --default-character-set=utf8mb4" in payload["commandText"]
    assert "dcc_controlled_file" in payload["commandText"]


def test_remote_nas_mount_check_requires_mnt_nas_mountpoint() -> None:
    root = _backup_root()
    file_module = root / "scripts" / "modules" / "Infra" / "FileOps.psm1"
    script = f"""
$ErrorActionPreference = 'Stop'
$script:commands = @()
function Invoke-BackupSshCommand {{
    param($Request)
    $script:commands += $Request.Command
    [pscustomobject]@{{ output = '' }}
}}
function Write-BackupOpsLog {{ param($Session, $Level, $Message) }}
Import-Module '{file_module}' -Force -DisableNameChecking
$config = [pscustomobject]@{{
    servers = [pscustomobject]@{{
        test = [pscustomobject]@{{
            host = '172.30.30.58'
            backupPointsRoot = '/mnt/nas/Backup/BackupPackage'
        }}
    }}
    ssh = [pscustomobject]@{{
        user = 'root'
        port = 22
    }}
    auth = [pscustomobject]@{{
        sshKeyPath = 'D:/missing_id_rsa'
        knownHostsPath = 'D:/missing_known_hosts'
    }}
}}
$session = [pscustomobject]@{{}}
$result = Assert-BackupOpsRemoteNasMounted -Config $config -LogSession $session
[pscustomobject]@{{
    result = $result
    commands = $script:commands
}} | ConvertTo-Json -Depth 8
"""
    completed = _run_powershell_script(script)
    assert completed.returncode == 0, completed.stdout + completed.stderr
    payload = json.loads(completed.stdout)
    command = payload["commands"][0]

    assert payload["result"]["mountRoot"] == "/mnt/nas"
    assert payload["result"]["backupPointsRoot"] == "/mnt/nas/Backup/BackupPackage"
    assert "mountpoint -q '/mnt/nas'" in command
    assert "mkdir -p '/mnt/nas/Backup/BackupPackage'" in command
    assert "test -w '/mnt/nas/Backup/BackupPackage'" in command


def test_backup_console_launcher_routes_to_powershell_entry() -> None:
    text = (_backup_root() / "00-备份恢复控制台.bat").read_text(encoding="utf-8")

    assert 'set "PS1=%SCRIPT_DIR%scripts\\backup-ops.ps1"' in text
    assert "IntRuoyi 备份恢复控制台" in text
    assert "1. 立即备份" in text
    assert "2. 回滚应用版本" in text
    assert "3. 恢复数据" in text
    assert "9. 查看最近日志目录" in text
    assert 'if "%CHOICE%"=="1"' in text
    assert 'if "%CHOICE%"=="2"' in text
    assert 'if "%CHOICE%"=="3"' in text
    assert 'if "%CHOICE%"=="9"' in text
    assert 'if "%CHOICE%"=="0"' in text
    assert '-Mode "backup-now" -TargetEnvironment "test"' in text
    assert '-Mode "restore-data" -TargetEnvironment "test"' in text


def test_action_wrappers_route_modes_to_backup_ops_entry() -> None:
    root = _backup_root() / "actions"
    files_to_modes = {
        "01-立即备份.bat": "backup-now",
        "02-回滚应用版本.bat": "rollback-app",
        "03-恢复数据.bat": "restore-data",
    }

    for filename, mode in files_to_modes.items():
        text = (root / filename).read_text(encoding="utf-8")
        assert "backup-ops.ps1" in text
        assert f'-Mode "{mode}"' in text or f"-Mode '{mode}'" in text

    restore_text = (root / "03-恢复数据.bat").read_text(encoding="utf-8")
    backup_now_text = (root / "01-立即备份.bat").read_text(encoding="utf-8")
    assert '-Mode "backup-now" -TargetEnvironment "test"' in backup_now_text
    assert '-Mode "restore-data" -TargetEnvironment "test"' in restore_text


def test_backup_ops_entry_declares_supported_modes_and_exit_codes() -> None:
    text = (_backup_root() / "scripts" / "backup-ops.ps1").read_text(encoding="utf-8")

    assert "backup-now" in text
    assert "backup-scheduled" in text
    assert "rollback-app" in text
    assert "restore-data" in text
    assert "rehearsal" in text
    assert "exit 0" in text.lower() or "Exit 0" in text
    assert "exit 1" in text.lower() or "Exit 1" in text
    assert "exit 2" in text.lower() or "Exit 2" in text


def test_backup_ops_launcher_projects_backup_now_and_restore_data_target_environment_explicitly() -> None:
    text = (_backup_root() / "scripts" / "backup-ops.ps1").read_text(encoding="utf-8")
    config_text = (_backup_root() / "config" / "backup-ops.config.example.json").read_text(encoding="utf-8")

    assert "[string]$TargetEnvironment = 'prod'" in text
    assert "[ValidateSet('prod', 'test', 'backup')]" in text
    assert "[string]$ProductionBackupConfirmText" in text
    assert "Resolve-BackupOpsTargetEnvironmentConfig" in text
    assert "Assert-BackupOpsProductionBackupConfirmation" in text
    assert "PROD-BACKUP-172.30.30.57" in text
    assert "Production backup confirmation is required" in text
    assert "$supportedTestTargetModes = @('backup-now', 'backup-scheduled', 'rollback-app', 'restore-data')" in text
    assert "TargetEnvironment test/backup is only supported for backup-now, backup-scheduled, rollback-app and restore-data" in text
    assert "@('test', 'backup')" in text
    assert 'servers.$TargetEnvironment.runtimeDir' in text
    assert 'servers.$TargetEnvironment.tmpRoot' in text
    assert "$clone.servers.production.host = $targetHost" in text
    assert "$clone.servers.production.appDir = $targetRuntimeDir" in text
    assert "$clone.servers.production.tmpRoot = $targetTmpRoot" in text
    assert 'servers.$TargetEnvironment.minioContainer' in text
    assert "$clone.containers.minio = $targetMinioContainer" in text
    assert "restore-data only supports TargetEnvironment test or backup" in text
    assert '"runtimeDir": "/opt/intruoyi/runtime"' in config_text
    assert '"tmpRoot": "/opt/intruoyi/runtime/data/backup-ops/tmp"' in config_text
    assert '"minioContainer": "ragflow_compose-minio-1"' in config_text
    assert '"backup": {' in config_text
    assert '"host": "172.30.30.59"' in config_text
    assert '"minioContainer": "intruoyi-minio"' in config_text


def test_backup_ops_launcher_blocks_prod_backup_without_explicit_confirmation() -> None:
    result = _run_backup_ops_mode("backup-now", "-TargetEnvironment", "prod", "-NonInteractive")

    assert result.returncode == 2, result.stdout + result.stderr
    assert "Production backup confirmation is required" in result.stdout
    assert "PROD-BACKUP-172.30.30.57" in result.stdout


def test_backup_ops_launcher_allows_prod_backup_with_exact_confirmation() -> None:
    root = _backup_root()
    entry_script = root / "scripts" / "backup-ops.ps1"
    config_path = root / "config" / "backup-ops.config.example.json"
    secrets_path = root / "config" / "backup-ops.secrets.example.json"

    script = f"""
$ErrorActionPreference = 'Stop'
function Import-Module {{ param($Name, [switch]$Force, [switch]$DisableNameChecking) }}
function Assert-BackupOpsSupportedMode {{ param($Mode) }}
function Show-BackupOpsOperationResult {{ param($Result) }}
function Import-BackupOpsConfiguration {{
    param($ConfigPath, $SecretsPath)
    return [System.IO.File]::ReadAllText($ConfigPath, [System.Text.UTF8Encoding]::new($false)) | ConvertFrom-Json
}}
function Invoke-BackupNowUseCase {{
    param($Config, $OperatorName, [switch]$NonInteractive)
    if ($Config.environment -ne 'production') {{
        throw "Expected production environment, got $($Config.environment)"
    }}
    if ($Config.servers.production.host -ne '172.30.30.57') {{
        throw "Expected production host 172.30.30.57, got $($Config.servers.production.host)"
    }}
    return [pscustomobject]@{{
        action = 'backup-now'
        status = 'success'
        code = 'INTBK-0000'
        message = 'captured'
        startedAt = (Get-Date).ToString('o')
        completedAt = (Get-Date).ToString('o')
        logPath = $null
        reportPath = $null
        context = @{{}}
    }}
}}
. '{entry_script}' -Mode 'backup-now' -ConfigPath '{config_path}' -SecretsPath '{secrets_path}' -TargetEnvironment 'prod' -ProductionBackupConfirmText 'PROD-BACKUP-172.30.30.57' -NonInteractive
"""
    completed = subprocess.run(
        [
            "powershell",
            "-NoProfile",
            "-ExecutionPolicy",
            "Bypass",
            "-Command",
            script,
        ],
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        check=False,
    )

    assert completed.returncode == 0, completed.stdout + completed.stderr


def test_backup_ops_launcher_projects_backup_minio_container_for_backup_target() -> None:
    root = _backup_root()
    entry_script = root / "scripts" / "backup-ops.ps1"
    config_path = root / "config" / "backup-ops.config.example.json"
    secrets_path = root / "config" / "backup-ops.secrets.example.json"

    script = f"""
$ErrorActionPreference = 'Stop'
$script:resolvedConfig = $null
function Import-Module {{ param($Name, [switch]$Force, [switch]$DisableNameChecking) }}
function Assert-BackupOpsSupportedMode {{ param($Mode) }}
function Show-BackupOpsOperationResult {{ param($Result) }}
function Import-BackupOpsConfiguration {{
    param($ConfigPath, $SecretsPath)
    $config = [System.IO.File]::ReadAllText($ConfigPath, [System.Text.UTF8Encoding]::new($false)) | ConvertFrom-Json
    $config.containers.minio = 'ragflow_compose-minio-1'
    if (-not $config.servers.backup.PSObject.Properties['minioContainer']) {{
        Add-Member -InputObject $config.servers.backup -MemberType NoteProperty -Name 'minioContainer' -Value 'intruoyi-minio'
    }}
    return $config
}}
function Invoke-RestoreDataUseCase {{
    param($Config, $SelectedBackupId, $OperatorName, [switch]$NonInteractive)
    $script:resolvedConfig = $Config
    return [pscustomobject]@{{
        action = 'restore-data'
        status = 'success'
        code = 'INTBK-0000'
        message = 'captured'
        startedAt = (Get-Date).ToString('o')
        completedAt = (Get-Date).ToString('o')
        logPath = $null
        reportPath = $null
        context = @{{}}
    }}
}}
. '{entry_script}' -Mode 'restore-data' -ConfigPath '{config_path}' -SecretsPath '{secrets_path}' -TargetEnvironment 'backup' -SelectedBackupId '20260604-182827' -NonInteractive
if ($script:resolvedConfig.containers.minio -ne 'intruoyi-minio') {{
    throw "Expected backup target MinIO container intruoyi-minio, got $($script:resolvedConfig.containers.minio)"
}}
"""
    completed = subprocess.run(
        [
            "powershell",
            "-NoProfile",
            "-ExecutionPolicy",
            "Bypass",
            "-Command",
            script,
        ],
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        check=False,
    )

    assert completed.returncode == 0, completed.stdout + completed.stderr


def test_backup_ops_restore_data_forbids_production_target() -> None:
    text = (_backup_root() / "scripts" / "backup-ops.ps1").read_text(encoding="utf-8")
    restore_use_case = (_backup_root() / "scripts" / "modules" / "UseCases" / "RestoreData.psm1").read_text(encoding="utf-8")

    assert "$Mode -eq 'restore-data' -and $TargetEnvironment -eq 'prod'" in text
    assert "restore-data only supports TargetEnvironment test or backup" in text
    assert "Assert-RestoreDataTargetEnvironment" in restore_use_case
    assert "'test'" in restore_use_case
    assert "'backup'" in restore_use_case


def test_backup_ops_minio_client_image_is_explicit_and_quay_hosted() -> None:
    root = _backup_root()
    config_module_text = (root / "scripts" / "modules" / "Core" / "Config.psm1").read_text(encoding="utf-8")
    object_ops_text = (root / "scripts" / "modules" / "Infra" / "ObjectOps.psm1").read_text(encoding="utf-8")
    validation_text = (root / "scripts" / "modules" / "Core" / "Validation.psm1").read_text(encoding="utf-8")
    config_text = (root / "config" / "backup-ops.config.json").read_text(encoding="utf-8")
    example_text = (root / "config" / "backup-ops.config.example.json").read_text(encoding="utf-8")

    assert '"tools"' in config_text
    assert '"minioClientImage": "quay.io/minio/mc:latest"' in config_text
    assert '"archiveImage": "alpine:3.20"' in config_text
    assert '"minioClientImage": "quay.io/minio/mc:latest"' in example_text
    assert '"archiveImage": "alpine:3.20"' in example_text
    assert "tools         = $ConfigObject.tools" in config_module_text
    assert "@('tools', 'minioClientImage')" in validation_text
    assert "@('tools', 'archiveImage')" in validation_text
    assert "Get-BackupOpsMinioClientImage" in object_ops_text
    assert "$arguments += @($Image, '-c', $Command)" in object_ops_text
    assert "minio/mc" not in object_ops_text


def test_backup_ops_stores_incremental_objects_in_remote_object_store() -> None:
    root = _backup_root()
    object_ops_text = (root / "scripts" / "modules" / "Infra" / "ObjectOps.psm1").read_text(encoding="utf-8")
    file_ops_text = (root / "scripts" / "modules" / "Infra" / "FileOps.psm1").read_text(encoding="utf-8")

    assert "Export-BackupObjectSnapshotToRemoteNas" in object_ops_text
    assert "manifest-object-inventory.json" in object_ops_text
    assert "servers', 'test', 'backupPointsRoot'" in object_ops_text
    assert "docker run --rm --entrypoint /bin/sh" in object_ops_text
    assert "mc ls --recursive --json" in object_ops_text
    assert "mc cp " in object_ops_text
    assert "manifest-object-copy.sh" in object_ops_text
    assert "Write-BackupOpsUtf8LfFile" in object_ops_text
    assert "cut -f1" in object_ops_text
    assert "cut -f2-" in object_ops_text
    assert "tr -d" not in object_ops_text
    assert "printf ''%s\\n''" not in object_ops_text
    assert "printf '%s\\n'" not in object_ops_text
    assert "printf ''%s''" in object_ops_text
    assert "printf '%s'" in object_ops_text
    assert "printf ''\\t''" not in object_ops_text
    assert "printf ''\\r''" not in object_ops_text
    assert "printf ''\\011''" not in object_ops_text
    assert "printf ''\\015''" not in object_ops_text
    assert 'repo=${repo%"$cr"}' not in object_ops_text
    assert 'rel=${rel%"$cr"}' not in object_ops_text
    assert 'IFS="$tab"' not in object_ops_text
    assert "objectStoreRoot" in object_ops_text
    assert "ConvertFrom-BackupOpsRemoteObjectMetadataJson" in object_ops_text
    assert "New-BackupOpsObjectCopyPlan" in object_ops_text
    assert "etag" in object_ops_text
    assert "/backup/' + $bucket" not in object_ops_text
    assert "mc mirror --retry --max-workers 1 --overwrite" not in object_ops_text
    assert "sha256sum" not in object_ops_text
    assert "manifest-object-inventory.json" in file_ops_text
    assert "对象增量清单不存在" in file_ops_text
    assert "objectStoreRoot" in file_ops_text
    backup_bucket_section = object_ops_text[
        object_ops_text.index("function Backup-BackupOpsObjectBucket"):
        object_ops_text.index("function Restore-BackupOpsObjectBucket")
    ]
    assert "Test-BackupObjectAccess" not in backup_bucket_section


def test_incremental_object_copy_plan_excludes_reused_and_deleted_objects() -> None:
    root = _backup_root()
    object_module = root / "scripts" / "modules" / "Infra" / "ObjectOps.psm1"
    script = f"""
$ErrorActionPreference = 'Stop'
Import-Module '{object_module}' -Force -DisableNameChecking
$current = @(
    [pscustomobject]@{{ path = 'dcc/A.txt'; sha256 = 'sha-a2'; size = 20; lastModified = '2026-06-08T01:00:00Z'; status = 'active'; repositoryKey = 'repo-a2' }},
    [pscustomobject]@{{ path = 'dcc/B.txt'; sha256 = 'sha-b1'; size = 10; lastModified = '2026-06-08T01:00:00Z'; status = 'active'; repositoryKey = 'repo-b1' }},
    [pscustomobject]@{{ path = 'dcc/D.txt'; sha256 = 'sha-d1'; size = 30; lastModified = '2026-06-08T01:00:00Z'; status = 'active'; repositoryKey = 'repo-d1' }}
)
$previous = @(
    [pscustomobject]@{{ path = 'dcc/A.txt'; sha256 = 'sha-a1'; size = 19; lastModified = '2026-06-07T01:00:00Z'; status = 'active'; repositoryKey = 'repo-a1' }},
    [pscustomobject]@{{ path = 'dcc/C.txt'; sha256 = 'sha-c1'; size = 9; lastModified = '2026-06-07T01:00:00Z'; status = 'active'; repositoryKey = 'repo-c1' }},
    [pscustomobject]@{{ path = 'dcc/D.txt'; sha256 = 'sha-d1'; size = 30; lastModified = '2026-06-07T01:00:00Z'; status = 'active'; repositoryKey = 'repo-d1' }}
)
$inventory = Merge-BackupOpsObjectInventory -CurrentObjects $current -PreviousObjects $previous
$plan = New-BackupOpsObjectCopyPlan -Inventory $inventory
[pscustomobject]@{{
    inventory = $inventory
    plan = $plan
}} | ConvertTo-Json -Depth 8
"""
    completed = _run_powershell_script(script)
    assert completed.returncode == 0, completed.stdout + completed.stderr
    payload = json.loads(completed.stdout)

    objects_by_path = {item["path"]: item for item in payload["inventory"]["objects"]}
    assert payload["inventory"]["stats"]["addedCount"] == 1
    assert payload["inventory"]["stats"]["modifiedCount"] == 1
    assert payload["inventory"]["stats"]["deletedCount"] == 1
    assert payload["inventory"]["stats"]["reusedCount"] == 1
    assert objects_by_path["dcc/A.txt"]["changeType"] == "modified"
    assert objects_by_path["dcc/B.txt"]["changeType"] == "added"
    assert objects_by_path["dcc/C.txt"]["status"] == "deleted"
    assert objects_by_path["dcc/C.txt"]["changeType"] == "deleted"
    assert objects_by_path["dcc/D.txt"]["changeType"] == "reused"
    assert [item["repositoryKey"] for item in payload["plan"]] == ["repo-a2", "repo-b1"]


def test_backup_ops_manifest_carries_incremental_object_inventory_and_stats() -> None:
    root = _backup_root()
    report_module = (root / "scripts" / "modules" / "Infra" / "ReportOps.psm1").read_text(encoding="utf-8")

    assert "backupStrategy" in report_module
    assert "retentionPolicy" in report_module
    assert "objectDeltaStats" in report_module
    assert "objects" in report_module
    assert "incremental-manifest" in report_module
    assert "objects/manifest-object-inventory.json" in report_module
    assert "objects/objects-yudao.tar" not in report_module


def test_sync_backup_to_test_server_no_longer_requires_remote_object_archive_marker() -> None:
    root = _backup_root()
    file_module = (root / "scripts" / "modules" / "Infra" / "FileOps.psm1").read_text(encoding="utf-8")

    assert "remote-object-backup.json" not in file_module.split("function Sync-BackupOpsBackupToTestServer", 1)[1].split(
        "function Sync-BackupOpsManifestToTestServer", 1
    )[0]
    assert "objects-yudao.tar" not in file_module.split("function Sync-BackupOpsBackupToTestServer", 1)[1].split(
        "function Sync-BackupOpsManifestToTestServer", 1
    )[0]
    assert "object-store" in file_module
    assert "manifest-object-inventory.json" in file_module


def test_restore_object_bucket_uses_manifest_inventory_not_remote_tar_archive() -> None:
    root = _backup_root()
    object_ops_text = (root / "scripts" / "modules" / "Infra" / "ObjectOps.psm1").read_text(encoding="utf-8")

    restore_section = object_ops_text[
        object_ops_text.index("function Restore-BackupOpsObjectBucket"):
    ]
    assert "objects-yudao.tar" not in restore_section
    assert "Import-BackupObjectSnapshotFromRemoteNas" not in restore_section
    assert "manifest-object-inventory.json" in restore_section
    assert "repositoryKey" in restore_section


def test_restore_object_bucket_stages_under_backup_package_not_runtime_tmp() -> None:
    root = _backup_root()
    object_ops_text = (root / "scripts" / "modules" / "Infra" / "ObjectOps.psm1").read_text(encoding="utf-8")
    restore_section = object_ops_text[
        object_ops_text.index("function Restore-BackupOpsObjectBucket"):
        object_ops_text.index("Export-ModuleMember", object_ops_text.index("function Restore-BackupOpsObjectBucket"))
    ]

    assert "servers', 'production', 'tmpRoot'" not in restore_section
    assert "Assert-BackupOpsObjectBackupPackageRoot" in restore_section
    assert ".restore-stage" in restore_section
    assert "/mnt/nas/Backup/BackupPackage" in object_ops_text


def test_remote_nas_object_backup_plan_uses_incremental_object_store() -> None:
    root = _backup_root()
    object_module = root / "scripts" / "modules" / "Infra" / "ObjectOps.psm1"
    script = f"""
$ErrorActionPreference = 'Stop'
Import-Module '{object_module}' -Force -DisableNameChecking
$plan = Export-BackupObjectSnapshotToRemoteNas -PlanOnly -Request @{{
    Bucket = 'yudao'
    TargetPath = 'D:/tmp/objects'
    RemotePath = '/mnt/nas/Backup/BackupPackage/20260606-135859/objects'
    SourcePath = 'test'
    Mode = 'snapshot'
    Endpoint = 'http://172.30.30.58:9000'
    AccessKey = 'access'
    SecretKey = 'secret'
    ClientImage = 'quay.io/minio/mc:latest'
    ArchiveImage = 'alpine:3.20'
    SshRequest = @{{
        Host = '172.30.30.58'
        User = 'root'
        KeyPath = 'D:/missing_id_rsa'
        Port = 22
        KnownHostsPath = 'D:/missing_known_hosts'
    }}
    EnvironmentLabel = '测试环境'
}}
$plan | ConvertTo-Json -Depth 8
"""
    completed = _run_powershell_script(script)
    assert completed.returncode == 0, completed.stdout + completed.stderr
    payload = json.loads(completed.stdout)

    assert payload["objectStoreRoot"] == "/mnt/nas/Backup/BackupPackage/object-store"
    assert "mc ls --recursive --json" in payload["command"]
    assert "mc cp " in payload["command"]
    assert "manifest-object-copy-plan.tsv" in payload["command"]
    assert "/backup/yudao" not in payload["command"]


def test_remote_nas_object_backup_short_ssh_steps_are_bounded() -> None:
    root = _backup_root()
    object_module = root / "scripts" / "modules" / "Infra" / "ObjectOps.psm1"
    metadata_output = "\n".join(
        [
            json.dumps(
                {
                    "type": "file",
                    "key": "dcc/A.txt",
                    "etag": "repo-a",
                    "size": 10,
                    "lastModified": "2026-06-08T01:00:00Z",
                }
            ),
            json.dumps(
                {
                    "type": "file",
                    "key": "dcc/B.txt",
                    "etag": "repo-b",
                    "size": 20,
                    "lastModified": "2026-06-08T01:01:00Z",
                }
            ),
        ]
    )
    previous_manifest = json.dumps(
        {
            "objects": [
                {
                    "path": "dcc/A.txt",
                    "sha256": "repo-a",
                    "repositoryKey": "repo-a",
                    "status": "active",
                    "size": 10,
                    "lastModified": "2026-06-07T01:00:00Z",
                }
            ]
        },
        ensure_ascii=False,
    )

    with tempfile.TemporaryDirectory() as temp_dir:
        target_path = Path(temp_dir) / "objects"
        script = f"""
$ErrorActionPreference = 'Stop'
$script:commands = @()
$script:uploads = @()
$script:metadata = @'
{metadata_output}
'@
$script:previousManifest = @'
{previous_manifest}
'@
function Invoke-BackupSshCommand {{
    param($Request)
    $script:commands += [pscustomobject]@{{
        command = $Request.Command
        timeoutSeconds = $Request.TimeoutSeconds
    }}
    if ([string]$Request.Command -like 'docker run*mc ls*') {{
        return [pscustomobject]@{{ output = $script:metadata }}
    }}
    if ([string]$Request.Command -like 'find *') {{
        return [pscustomobject]@{{ output = '/mnt/nas/Backup/BackupPackage/20260520-010203' }}
    }}
    if ([string]$Request.Command -like 'cat *manifest/manifest.json*') {{
        return [pscustomobject]@{{ output = $script:previousManifest }}
    }}
    [pscustomobject]@{{ output = '' }}
}}
function Send-BackupFileOverSsh {{
    param($Request)
    $bytes = [System.IO.File]::ReadAllBytes([string]$Request.LocalPath)
    $script:uploads += [pscustomobject]@{{
        localPath = $Request.LocalPath
        remotePath = $Request.RemotePath
        timeoutSeconds = $Request.TimeoutSeconds
        base64 = [Convert]::ToBase64String($bytes)
    }}
    [pscustomobject]@{{ status = 'success' }}
}}
Import-Module '{object_module}' -Force -DisableNameChecking
$result = Export-BackupObjectSnapshotToRemoteNas -Request @{{
    Bucket = 'yudao'
    TargetPath = '{target_path}'
    RemotePath = '/mnt/nas/Backup/BackupPackage/20260521-010203/objects'
    SourcePath = 'test'
    Mode = 'snapshot'
    Endpoint = 'http://172.30.30.58:9000'
    AccessKey = 'access'
    SecretKey = 'secret'
    ClientImage = 'quay.io/minio/mc:latest'
    ArchiveImage = 'alpine:3.20'
    SshRequest = @{{
        Host = '172.30.30.58'
        User = 'root'
        KeyPath = 'D:/missing_id_rsa'
        Port = 22
        KnownHostsPath = 'D:/missing_known_hosts'
    }}
    EnvironmentLabel = '测试环境'
}}
[pscustomobject]@{{
    result = $result
    commands = $script:commands
    uploads = $script:uploads
}} | ConvertTo-Json -Depth 10
"""
        completed = _run_powershell_script(script)
        assert completed.returncode == 0, completed.stdout + completed.stderr
        payload = json.loads(completed.stdout)

    commands = payload["commands"]
    assert payload["result"]["stats"]["addedCount"] == 1
    metadata_commands = [item for item in commands if "mc ls" in item["command"]]
    assert metadata_commands and all(item["timeoutSeconds"] == 300 for item in metadata_commands)
    previous_manifest_lists = [item for item in commands if item["command"].startswith("find ")]
    assert previous_manifest_lists and all(item["timeoutSeconds"] == 300 for item in previous_manifest_lists)
    previous_manifest_reads = [item for item in commands if "manifest/manifest.json" in item["command"]]
    assert previous_manifest_reads and all(item["timeoutSeconds"] == 60 for item in previous_manifest_reads)
    mkdir_commands = [item for item in commands if item["command"].startswith("mkdir -p ")]
    assert mkdir_commands and all(item["timeoutSeconds"] == 60 for item in mkdir_commands)
    copy_commands = [item for item in commands if "manifest-object-copy.sh" in item["command"]]
    assert copy_commands and all(item["timeoutSeconds"] == 7200 for item in copy_commands)
    assert payload["uploads"] and all(upload["timeoutSeconds"] == 300 for upload in payload["uploads"])
    copy_plan_uploads = [
        upload for upload in payload["uploads"] if str(upload["remotePath"]).endswith("manifest-object-copy-plan.tsv")
    ]
    assert copy_plan_uploads
    copy_plan_bytes = base64.b64decode(copy_plan_uploads[0]["base64"])
    assert copy_plan_bytes == b"repo-b\tdcc/B.txt\n"
    assert b"\r" not in copy_plan_bytes
    copy_script_uploads = [
        upload for upload in payload["uploads"] if str(upload["remotePath"]).endswith("manifest-object-copy.sh")
    ]
    assert copy_script_uploads
    copy_script_bytes = base64.b64decode(copy_script_uploads[0]["base64"])
    assert copy_script_bytes.startswith(b"set -eu\n")
    assert b"\r" not in copy_script_bytes


def test_remote_manifest_restore_mounts_object_store_readonly() -> None:
    root = _backup_root()
    object_module = root / "scripts" / "modules" / "Infra" / "ObjectOps.psm1"
    inventory = json.dumps(
        {
            "mode": "incremental-manifest",
            "bucket": "yudao",
            "objectStoreRoot": "/mnt/nas/Backup/BackupPackage/object-store",
            "objects": [
                {
                    "path": "dcc/A.txt",
                    "repositoryKey": "repo-a",
                    "status": "active",
                }
            ],
        },
        ensure_ascii=False,
    )
    script = f"""
$ErrorActionPreference = 'Stop'
$script:inventory = @'
{inventory}
'@
function Invoke-BackupSshCommand {{
    param($Request)
    if ([string]$Request.Command -like 'cat *manifest-object-inventory.json*') {{
        return [pscustomobject]@{{ output = $script:inventory }}
    }}
    [pscustomobject]@{{ output = '' }}
}}
Import-Module '{object_module}' -Force -DisableNameChecking
$plan = Import-BackupObjectInventoryFromRemoteNas -PlanOnly -Request @{{
    Bucket = 'yudao'
    RemoteInventoryPath = '/mnt/nas/Backup/BackupPackage/20260608-031706/objects/manifest-object-inventory.json'
    Endpoint = 'http://172.30.30.58:9000'
    AccessKey = 'access'
    SecretKey = 'secret'
    ClientImage = 'quay.io/minio/mc:latest'
    RemoteTempRoot = '/mnt/nas/Backup/BackupPackage/.restore-stage/20260608-031706/objects'
    SshRequest = @{{
        Host = '172.30.30.58'
        User = 'root'
        KeyPath = 'D:/missing_id_rsa'
        Port = 22
        KnownHostsPath = 'D:/missing_known_hosts'
    }}
    EnvironmentLabel = '测试环境'
}}
$plan | ConvertTo-Json -Depth 8
"""
    completed = _run_powershell_script(script)
    assert completed.returncode == 0, completed.stdout + completed.stderr
    payload = json.loads(completed.stdout)

    assert "/mnt/nas/Backup/BackupPackage/object-store:/object-store:ro" in payload["command"]
    assert 'cp "/object-store/$sha" "$dest"' in payload["command"]
    assert 'cp "/mnt/nas/Backup/BackupPackage/object-store/$sha"' not in payload["command"]


def test_remote_manifest_restore_ssh_steps_are_bounded() -> None:
    root = _backup_root()
    object_module = root / "scripts" / "modules" / "Infra" / "ObjectOps.psm1"
    inventory = json.dumps(
        {
            "mode": "incremental-manifest",
            "bucket": "yudao",
            "objectStoreRoot": "/mnt/nas/Backup/BackupPackage/object-store",
            "objects": [
                {
                    "path": "dcc/A.txt",
                    "repositoryKey": "repo-a",
                    "status": "active",
                }
            ],
        },
        ensure_ascii=False,
    )
    script = f"""
$ErrorActionPreference = 'Stop'
$script:commands = @()
$script:uploads = @()
$script:inventory = @'
{inventory}
'@
function Invoke-BackupSshCommand {{
    param($Request)
    $script:commands += [pscustomobject]@{{
        command = $Request.Command
        timeoutSeconds = $Request.TimeoutSeconds
    }}
    if ([string]$Request.Command -like 'cat *manifest-object-inventory.json*') {{
        return [pscustomobject]@{{ output = $script:inventory }}
    }}
    [pscustomobject]@{{ output = '' }}
}}
function Send-BackupFileOverSsh {{
    param($Request)
    $bytes = [System.IO.File]::ReadAllBytes([string]$Request.LocalPath)
    $script:uploads += [pscustomobject]@{{
        localPath = $Request.LocalPath
        remotePath = $Request.RemotePath
        timeoutSeconds = $Request.TimeoutSeconds
        base64 = [Convert]::ToBase64String($bytes)
    }}
    [pscustomobject]@{{ output = '' }}
}}
Import-Module '{object_module}' -Force -DisableNameChecking
$result = Import-BackupObjectInventoryFromRemoteNas -Request @{{
    Bucket = 'yudao'
    RemoteInventoryPath = '/mnt/nas/Backup/BackupPackage/20260608-031706/objects/manifest-object-inventory.json'
    Endpoint = 'http://172.30.30.58:9000'
    AccessKey = 'access'
    SecretKey = 'secret'
    ClientImage = 'quay.io/minio/mc:latest'
    RemoteTempRoot = '/mnt/nas/Backup/BackupPackage/.restore-stage/20260608-031706/objects'
    SshRequest = @{{
        Host = '172.30.30.58'
        User = 'root'
        KeyPath = 'D:/missing_id_rsa'
        Port = 22
        KnownHostsPath = 'D:/missing_known_hosts'
    }}
    EnvironmentLabel = '测试环境'
}}
[pscustomobject]@{{
    result = $result
    commands = $script:commands
    uploads = $script:uploads
}} | ConvertTo-Json -Depth 10
"""
    completed = _run_powershell_script(script)
    assert completed.returncode == 0, completed.stdout + completed.stderr
    payload = json.loads(completed.stdout)

    assert payload["result"]["status"] == "success"
    inventory_reads = [item for item in payload["commands"] if "manifest-object-inventory.json" in item["command"]]
    assert inventory_reads and all(item["timeoutSeconds"] == 60 for item in inventory_reads)
    mkdir_commands = [item for item in payload["commands"] if item["command"].startswith("mkdir -p ")]
    assert mkdir_commands and all(item["timeoutSeconds"] == 60 for item in mkdir_commands)
    restore_commands = [item for item in payload["commands"] if "mc mirror --overwrite --remove" in item["command"]]
    assert restore_commands and all(item["timeoutSeconds"] == 7200 for item in restore_commands)
    assert payload["uploads"] and all(upload["timeoutSeconds"] == 300 for upload in payload["uploads"])
    restore_plan_uploads = [
        upload for upload in payload["uploads"] if str(upload["remotePath"]).endswith("restore-object-plan.tsv")
    ]
    assert restore_plan_uploads
    restore_plan_bytes = base64.b64decode(restore_plan_uploads[0]["base64"])
    assert restore_plan_bytes == b"repo-a\tdcc/A.txt\n"
    assert b"\r" not in restore_plan_bytes


def test_rehearsal_object_restore_uses_incremental_inventory_restore() -> None:
    root = _backup_root()
    docker_module = root / "scripts" / "modules" / "Infra" / "DockerOps.psm1"
    script = f"""
$ErrorActionPreference = 'Stop'
$script:restoreRequests = @()
$script:commands = @()
function Write-BackupOpsLog {{ param($Session, $Level, $Message) }}
function Restore-BackupOpsObjectBucket {{
    param($Config, $BackupId, $LogSession)
    $script:restoreRequests += [pscustomobject]@{{
        backupId = $BackupId
        environment = $Config.environment
        targetHost = $Config.servers.production.host
        appDir = $Config.servers.production.appDir
        objectBucket = $Config.backup.objectBucket
        sourceRoot = $Config.servers.test.backupPointsRoot
    }}
    [pscustomobject]@{{ status = 'success'; backupId = $BackupId }}
}}
function Invoke-BackupSshCommand {{
    param($Request)
    $script:commands += [string]$Request.Command
    if ([string]$Request.Command -like 'docker inspect*') {{
        return [pscustomobject]@{{ output = "MINIO_ROOT_USER=access`nMINIO_ROOT_PASSWORD=secret" }}
    }}
    if ([string]$Request.Command -like 'test -d *objects/yudao*') {{
        throw [System.InvalidOperationException]::new('legacy object bucket directory must not be required')
    }}
    [pscustomobject]@{{ output = '' }}
}}
$module = Import-Module '{docker_module}' -Force -DisableNameChecking -PassThru
$config = [pscustomobject]@{{
    environment = 'test'
    servers = [pscustomobject]@{{
        production = [pscustomobject]@{{
            host = '172.30.30.57'
            appDir = '/opt/intruoyi/runtime'
            tmpRoot = '/opt/intruoyi/tmp'
        }}
        test = [pscustomobject]@{{
            host = '172.30.30.58'
            backupPointsRoot = '/mnt/nas/Backup/BackupPackage'
        }}
    }}
    backup = [pscustomobject]@{{
        objectBucket = 'yudao'
    }}
    rehearsal = [pscustomobject]@{{
        bucket = 'yudao-rehearsal'
    }}
    ssh = [pscustomobject]@{{
        user = 'root'
        port = 22
    }}
    auth = [pscustomobject]@{{
        sshKeyPath = 'D:/missing_id_rsa'
        knownHostsPath = 'D:/missing_known_hosts'
    }}
    containers = [pscustomobject]@{{
        mysql = 'intruoyi-mysql'
        redis = 'intruoyi-redis'
        backend = 'intruoyi-backend'
        frontend = 'intruoyi-frontend'
        minio = 'intruoyi-minio'
    }}
    tools = [pscustomobject]@{{
        minioClientImage = 'quay.io/minio/mc:latest'
    }}
}}
$metadata = [pscustomobject]@{{
    BackupId = '20260606-181538'
    RuntimeRoot = '/backup/int-ruoyi/rehearsal/current'
    Bucket = 'yudao-rehearsal'
    ContainerMap = [ordered]@{{
        'intruoyi-mysql' = 'rehearsal-mysql'
        'intruoyi-redis' = 'rehearsal-redis'
        'intruoyi-backend' = 'rehearsal-backend'
        'intruoyi-frontend' = 'rehearsal-frontend'
    }}
}}
$session = [pscustomobject]@{{}}
$boundRestore = $module.NewBoundScriptBlock({{
    param($Config, $Metadata, $Session)
    Restore-BackupOpsRehearsalObjectBucket -Config $Config -Metadata $Metadata -LogSession $Session
}})
$result = & $boundRestore $config $metadata $session
[pscustomobject]@{{
    result = $result
    restoreRequests = $script:restoreRequests
    commands = $script:commands
}} | ConvertTo-Json -Depth 10
"""
    completed = _run_powershell_script(script)
    assert completed.returncode == 0, completed.stdout + completed.stderr
    payload = json.loads(completed.stdout)
    restore_requests = payload["restoreRequests"]
    if isinstance(restore_requests, dict):
        restore_requests = [restore_requests]

    assert payload["result"]["status"] == "success"
    assert len(restore_requests) == 1
    assert restore_requests[0]["backupId"] == "20260606-181538"
    assert restore_requests[0]["environment"] == "rehearsal"
    assert restore_requests[0]["targetHost"] == "172.30.30.58"
    assert restore_requests[0]["appDir"] == "/backup/int-ruoyi/rehearsal/current"
    assert restore_requests[0]["objectBucket"] == "yudao-rehearsal"
    assert not any("objects/yudao" in command for command in payload["commands"])


def test_remote_manifest_restore_rejects_runtime_tmp_stage_root() -> None:
    root = _backup_root()
    object_module = root / "scripts" / "modules" / "Infra" / "ObjectOps.psm1"
    inventory = json.dumps(
        {
            "mode": "incremental-manifest",
            "bucket": "yudao",
            "objectStoreRoot": "/mnt/nas/Backup/BackupPackage/object-store",
            "objects": [],
        },
        ensure_ascii=False,
    )
    script = f"""
$ErrorActionPreference = 'Stop'
$script:inventory = @'
{inventory}
'@
function Invoke-BackupSshCommand {{
    param($Request)
    if ([string]$Request.Command -like 'cat *manifest-object-inventory.json*') {{
        return [pscustomobject]@{{ output = $script:inventory }}
    }}
    [pscustomobject]@{{ output = '' }}
}}
Import-Module '{object_module}' -Force -DisableNameChecking
$message = ''
try {{
    Import-BackupObjectInventoryFromRemoteNas -PlanOnly -Request @{{
        Bucket = 'yudao'
        RemoteInventoryPath = '/mnt/nas/Backup/BackupPackage/20260608-031706/objects/manifest-object-inventory.json'
        Endpoint = 'http://172.30.30.58:9000'
        AccessKey = 'access'
        SecretKey = 'secret'
        ClientImage = 'quay.io/minio/mc:latest'
        RemoteTempRoot = '/opt/intruoyi/ops/backup/tmp/20260608-031706/objects'
        SshRequest = @{{ Host = '172.30.30.58'; User = 'root'; KeyPath = 'D:/missing_id_rsa'; Port = 22; KnownHostsPath = 'D:/missing_known_hosts' }}
        EnvironmentLabel = '测试环境'
    }} | Out-Null
    throw 'Expected runtime tmp stage root to be rejected'
}} catch {{
    $message = $_.Exception.Message
}}
$message
"""
    completed = _run_powershell_script(script)
    assert completed.returncode == 0, completed.stdout + completed.stderr

    assert "/mnt/nas/Backup/BackupPackage/.restore-stage" in completed.stdout
    assert "/opt/intruoyi/ops/backup/tmp" in completed.stdout


def test_backup_ops_docker_invocation_preserves_shell_command_argument() -> None:
    root = _backup_root()
    object_ops_text = (root / "scripts" / "modules" / "Infra" / "ObjectOps.psm1").read_text(encoding="utf-8")

    assert "function ConvertTo-BackupOpsProcessArgument" in object_ops_text
    local_process_section = object_ops_text[
        object_ops_text.index("function Invoke-BackupOpsLocalProcessCapture"):
        object_ops_text.index("function Invoke-BackupOpsMcShell")
    ]
    assert "UseShellExecute = $false" in local_process_section
    assert "RedirectStandardOutput = $true" in local_process_section
    assert "RedirectStandardError = $true" in local_process_section
    assert "ConvertTo-BackupOpsProcessArgument" in local_process_section
    assert "& $FilePath @ArgumentList" not in local_process_section
    assert "Start-Process" not in local_process_section


def test_backup_ops_docker_volume_cleanup_retries_without_masking_backup_result() -> None:
    root = _backup_root()
    object_ops_text = (root / "scripts" / "modules" / "Infra" / "ObjectOps.psm1").read_text(encoding="utf-8")

    assert "Remove-BackupOpsDockerVolumeBestEffort" in object_ops_text
    assert "@('volume', 'rm', '-f', $VolumeName)" in object_ops_text
    assert "Start-Sleep -Seconds 2" in object_ops_text
    assert "Write-Warning \"Docker volume cleanup failed: $VolumeName" in object_ops_text


def test_backup_ops_uses_explicit_local_workspace_root_outside_repo() -> None:
    root = _backup_root()
    file_ops_text = (root / "scripts" / "modules" / "Infra" / "FileOps.psm1").read_text(encoding="utf-8")
    validation_text = (root / "scripts" / "modules" / "Core" / "Validation.psm1").read_text(encoding="utf-8")
    config_text = (root / "config" / "backup-ops.config.json").read_text(encoding="utf-8")
    example_text = (root / "config" / "backup-ops.config.example.json").read_text(encoding="utf-8")

    assert '"localWorkspaceRoot": "D:\\\\IntRuoyi-BackupOps\\\\tmp"' in config_text
    assert '"localWorkspaceRoot": "D:\\\\IntRuoyi-BackupOps\\\\tmp"' in example_text
    assert "@('backup', 'localWorkspaceRoot')" in validation_text
    assert "Get-BackupOpsRequiredFileConfigValue -Config $Config -Path @('backup', 'localWorkspaceRoot')" in file_ops_text
    assert "Join-Path $rootDir 'tmp'" not in file_ops_text


def test_backup_ops_entry_loads_config_logging_and_result_modules() -> None:
    text = (_backup_root() / "scripts" / "backup-ops.ps1").read_text(encoding="utf-8")

    assert "Config.psm1" in text
    assert "Logging.psm1" in text
    assert "ResultModel.psm1" in text
    assert "Validation.psm1" in text


def test_result_model_and_validation_modules_define_shared_contracts() -> None:
    root = _backup_root() / "scripts" / "modules" / "Core"
    result_text = (root / "ResultModel.psm1").read_text(encoding="utf-8")
    validation_text = (root / "Validation.psm1").read_text(encoding="utf-8")

    assert "INTBK-0000" in result_text
    assert "status" in result_text
    assert "action" in result_text
    assert "logPath" in result_text
    assert "reportPath" in result_text

    assert "INTBK-1001" in validation_text
    assert "INTBK-1002" in validation_text
    assert "INTBK-1003" in validation_text
    assert "INTBK-1004" in validation_text


def test_report_module_knows_manifest_checksums_and_dual_report_outputs() -> None:
    text = (
        _backup_root() / "scripts" / "modules" / "Infra" / "ReportOps.psm1"
    ).read_text(encoding="utf-8")

    assert "manifest.json" in text
    assert "checksums.txt" in text
    assert "backup-report.json" in text
    assert "backup-report.md" in text
    assert "reportType" in text
    assert "summary" in text


def test_backup_ops_example_invocation_blocks_instead_of_false_success() -> None:
    reports_root = Path(r"D:\IntRuoyi-BackupOps\logs")
    existing_md_reports = set(reports_root.rglob("*_backup-now_blocked.report.md"))
    existing_json_reports = set(reports_root.rglob("*_backup-now_blocked.report.json"))
    result = _run_backup_ops_mode("backup-now", "-NonInteractive")

    assert result.returncode == 2, result.stdout + result.stderr
    assert "操作未执行：需人工介入" in result.stdout
    assert "结果代码：INTBK-" in result.stdout
    assert "原因：" in result.stdout
    assert "建议动作：" in result.stdout
    assert "Phase-1 skeleton only." not in result.stdout

    report_path = _latest_report_after_run(
        "*_backup-now_blocked.report.md",
        existing_md_reports,
    )
    report_text = report_path.read_text(encoding="utf-8")
    report_json_path = _latest_report_after_run(
        "*_backup-now_blocked.report.json",
        existing_json_reports,
    )
    report_json = json.loads(report_json_path.read_text(encoding="utf-8"))

    assert "# 备份报告" in report_text
    assert "- 结果: `blocked`" in report_text
    assert "- 结束时间: `" in report_text
    assert "原因：" in report_text
    assert "建议动作：" in report_text
    assert "Phase-1 skeleton only." not in report_text

    assert report_json["status"] == "blocked"
    assert "原因：" in report_json["summary"]
    assert "建议动作：" in report_json["summary"]


def test_rollback_app_example_blocks_instead_of_null_candidate_crash() -> None:
    reports_root = Path(r"D:\IntRuoyi-BackupOps\logs")
    existing_md_reports = set(reports_root.rglob("*_rollback-app_blocked.report.md"))
    existing_json_reports = set(reports_root.rglob("*_rollback-app_blocked.report.json"))
    result = _run_backup_ops_mode(
        "rollback-app",
        "-SelectedImageTag",
        "release-test",
        "-NonInteractive",
    )

    assert result.returncode == 2, result.stdout + result.stderr
    assert "需人工介入" in result.stdout
    assert "Cannot bind argument to parameter 'Candidates'" not in result.stdout
    assert "Cannot bind argument to parameter 'Candidates'" not in result.stderr
    assert "结果代码：INTBK-" in result.stdout
    assert "原因：" in result.stdout
    assert "建议动作：" in result.stdout

    report_path = _latest_report_after_run(
        "*_rollback-app_blocked.report.md",
        existing_md_reports,
    )
    report_text = report_path.read_text(encoding="utf-8")
    report_json_path = _latest_report_after_run(
        "*_rollback-app_blocked.report.json",
        existing_json_reports,
    )
    report_json = json.loads(report_json_path.read_text(encoding="utf-8"))

    assert "- 结果: `blocked`" in report_text
    assert "原因：" in report_text
    assert "建议动作：" in report_text
    assert "Phase-1 skeleton only." not in report_text

    assert report_json["status"] == "blocked"
    if "imageTag" in report_json:
        assert report_json["imageTag"] == "release-test"
    assert "原因：" in report_json["summary"]
    assert "建议动作：" in report_json["summary"]


def test_restore_data_example_blocks_instead_of_null_candidate_crash() -> None:
    reports_root = Path(r"D:\IntRuoyi-BackupOps\logs")
    existing_reports = set(reports_root.rglob("*_restore-data_blocked.report.md"))
    result = _run_backup_ops_mode(
        "restore-data",
        "-TargetEnvironment",
        "test",
        "-SelectedBackupId",
        "20260520-123000",
        "-NonInteractive",
    )

    assert result.returncode == 2, result.stdout + result.stderr
    assert "需人工介入" in result.stdout
    assert "Cannot bind argument to parameter 'Candidates'" not in result.stdout
    assert "Cannot bind argument to parameter 'Candidates'" not in result.stderr

    report_path = _latest_report_after_run(
        "*_restore-data_blocked.report.md",
        existing_reports,
    )
    report_text = report_path.read_text(encoding="utf-8")

    assert "# 数据恢复报告" in report_text
    assert "蹇収" not in report_text
    assert "原因：" in report_text
    assert "建议动作：" in report_text


def test_rollback_app_without_candidates_blocks_with_clear_message() -> None:
    reports_root = Path(r"D:\IntRuoyi-BackupOps\logs")
    existing_md_reports = set(reports_root.rglob("*_rollback-app_blocked.report.md"))
    existing_json_reports = set(reports_root.rglob("*_rollback-app_blocked.report.json"))
    result = _run_backup_ops_mode("rollback-app", "-NonInteractive")
    expected_reason = "原因：当前未找到任何可回滚的 IMAGE_TAG 候选。"
    expected_action = "建议动作：请先完成一次备份或同步备份元数据，确认存在可回滚 IMAGE_TAG 后再重试。"

    assert result.returncode == 2, result.stdout + result.stderr
    assert expected_reason in result.stdout or "原因：" in result.stdout
    assert expected_action in result.stdout or "建议动作：" in result.stdout
    assert "Cannot bind argument to parameter 'Candidates'" not in result.stdout
    assert "null-valued expression" not in result.stdout

    report_path = _latest_report_after_run(
        "*_rollback-app_blocked.report.md",
        existing_md_reports,
    )
    report_text = report_path.read_text(encoding="utf-8")
    report_json_path = _latest_report_after_run(
        "*_rollback-app_blocked.report.json",
        existing_json_reports,
    )
    report_json = json.loads(report_json_path.read_text(encoding="utf-8"))

    assert "原因：" in report_text
    assert "建议动作：" in report_text
    assert "原因：" in report_json["summary"]
    assert "建议动作：" in report_json["summary"]


def test_restore_data_without_candidates_blocks_with_clear_message() -> None:
    reports_root = Path(r"D:\IntRuoyi-BackupOps\logs")
    existing_md_reports = set(reports_root.rglob("*_restore-data_blocked.report.md"))
    existing_json_reports = set(reports_root.rglob("*_restore-data_blocked.report.json"))
    result = _run_backup_ops_mode("restore-data", "-TargetEnvironment", "test", "-NonInteractive")
    expected_reason = "原因：当前未找到任何可用恢复点候选。"
    expected_action = "建议动作：请先完成一次可恢复备份或同步备份元数据，确认存在恢复点后再重试。"

    assert result.returncode == 2, result.stdout + result.stderr
    assert expected_reason in result.stdout or "原因：" in result.stdout
    assert expected_action in result.stdout or "建议动作：" in result.stdout
    assert "Cannot bind argument to parameter 'Candidates'" not in result.stdout
    assert "property 'backupId' cannot be found" not in result.stdout

    report_path = _latest_report_after_run(
        "*_restore-data_blocked.report.md",
        existing_md_reports,
    )
    report_text = report_path.read_text(encoding="utf-8")
    report_json_path = _latest_report_after_run(
        "*_restore-data_blocked.report.json",
        existing_json_reports,
    )
    report_json = json.loads(report_json_path.read_text(encoding="utf-8"))

    assert "原因：" in report_text
    assert "建议动作：" in report_text
    assert "原因：" in report_json["summary"]
    assert "建议动作：" in report_json["summary"]


def test_notify_module_disabled_status_is_visible_in_log_and_result() -> None:
    root = _backup_root()
    logging_module = root / "scripts" / "modules" / "Core" / "Logging.psm1"
    notify_module = root / "scripts" / "modules" / "Infra" / "NotifyOps.psm1"

    with tempfile.TemporaryDirectory() as temp_dir:
        log_root = Path(temp_dir) / "logs"
        script = f"""
$ErrorActionPreference = 'Stop'
Import-Module '{logging_module}' -Force -DisableNameChecking
Import-Module '{notify_module}' -Force -DisableNameChecking
$session = New-BackupOpsLogSession -Action 'backup-now' -LogRoot '{log_root}' -OperatorName 'tester' -Mode 'backup-now'
$result = Send-BackupNotification -Request @{{
    Action = 'backup-now'
    Status = 'success'
    Summary = 'backup completed and synced'
    Enabled = $false
    Channel = 'webhook'
    BackupId = '20260520-010203'
    ImageTag = '20260520_000001'
    LogPath = [string]$session.logPath
    ReportPath = 'D:/IntRuoyi-BackupOps/logs/backup.report.md'
    LogSession = $session
}}
[pscustomobject]@{{
    result = $result
    logPath = [string]$session.logPath
}} | ConvertTo-Json -Depth 8
"""
        completed = _run_powershell_script(script)
        assert completed.returncode == 0, completed.stdout + completed.stderr
        payload = json.loads(completed.stdout)
        result = payload["result"]
        log_text = Path(payload["logPath"]).read_text(encoding="utf-8")

        assert result["status"] == "disabled"
        assert result["channel"] == "webhook"
        assert result["summary"].find("备份点: 20260520-010203") >= 0
        assert result["summary"].find("IMAGE_TAG: 20260520_000001") >= 0
        assert "通知未发送" in result["message"]
        assert "通知未发送" in log_text
        assert "disabled" in log_text


def test_notify_module_pending_status_is_visible_in_log_and_result() -> None:
    root = _backup_root()
    logging_module = root / "scripts" / "modules" / "Core" / "Logging.psm1"
    notify_module = root / "scripts" / "modules" / "Infra" / "NotifyOps.psm1"

    with tempfile.TemporaryDirectory() as temp_dir:
        log_root = Path(temp_dir) / "logs"
        script = f"""
$ErrorActionPreference = 'Stop'
Import-Module '{logging_module}' -Force -DisableNameChecking
Import-Module '{notify_module}' -Force -DisableNameChecking
$session = New-BackupOpsLogSession -Action 'restore-data' -LogRoot '{log_root}' -OperatorName 'tester' -Mode 'restore-data'
$result = Send-BackupNotification -Request @{{
    Action = 'restore-data'
    Status = 'success'
    Summary = 'restore completed and validated'
    Enabled = $true
    Channel = 'pending'
    RestorePoint = '20260520-010203'
    ImageTag = '20260520_000001'
    LogPath = [string]$session.logPath
    LogSession = $session
}}
[pscustomobject]@{{
    result = $result
    logPath = [string]$session.logPath
}} | ConvertTo-Json -Depth 8
"""
        completed = _run_powershell_script(script)
        assert completed.returncode == 0, completed.stdout + completed.stderr
        payload = json.loads(completed.stdout)
        result = payload["result"]
        log_text = Path(payload["logPath"]).read_text(encoding="utf-8")

        assert result["status"] == "pending"
        assert result["summary"].find("恢复点: 20260520-010203") >= 0
        assert "通知未发送" in result["message"]
        assert "pending" in log_text


def test_notify_wrapper_fails_fast_when_webhook_enabled_without_url() -> None:
    root = _backup_root()
    logging_module = root / "scripts" / "modules" / "Core" / "Logging.psm1"
    notify_module = root / "scripts" / "modules" / "Infra" / "NotifyOps.psm1"

    with tempfile.TemporaryDirectory() as temp_dir:
        log_root = Path(temp_dir) / "logs"
        script = f"""
$ErrorActionPreference = 'Stop'
Import-Module '{logging_module}' -Force -DisableNameChecking
Import-Module '{notify_module}' -Force -DisableNameChecking
$config = [pscustomobject]@{{
    notify = [pscustomobject]@{{
        enabled = $true
        channel = 'webhook'
    }}
}}
$session = New-BackupOpsLogSession -Action 'rollback-app' -LogRoot '{log_root}' -OperatorName 'tester' -Mode 'rollback-app'
$context = @{{
    imageTag = '20260520_000001'
}}
Send-BackupOpsNotification -Config $config -Action 'rollback-app' -Status 'success' -Summary 'rollback completed and restarted' -Context $context -LogSession $session
"""
        completed = _run_powershell_script(script)
        assert completed.returncode != 0
        assert "webhook" in (completed.stdout + completed.stderr).lower()
        assert "url" in (completed.stdout + completed.stderr).lower()


def test_notify_module_can_send_webhook_with_operation_context() -> None:
    root = _backup_root()
    logging_module = root / "scripts" / "modules" / "Core" / "Logging.psm1"
    notify_module = root / "scripts" / "modules" / "Infra" / "NotifyOps.psm1"

    received: dict[str, object] = {}

    class Handler(BaseHTTPRequestHandler):
        def do_POST(self) -> None:  # noqa: N802
            length = int(self.headers["Content-Length"])
            body = self.rfile.read(length).decode("utf-8")
            received["path"] = self.path
            received["headers"] = dict(self.headers.items())
            received["body"] = json.loads(body)
            self.send_response(200)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.end_headers()
            self.wfile.write(b'{"ok":true}')

        def log_message(self, format: str, *args: object) -> None:
            return

    server = HTTPServer(("127.0.0.1", 0), Handler)
    thread = threading.Thread(target=server.serve_forever, daemon=True)
    thread.start()
    try:
        with tempfile.TemporaryDirectory() as temp_dir:
            log_root = Path(temp_dir) / "logs"
            webhook_url = f"http://127.0.0.1:{server.server_port}/notify"
            script = f"""
$ErrorActionPreference = 'Stop'
Import-Module '{logging_module}' -Force -DisableNameChecking
Import-Module '{notify_module}' -Force -DisableNameChecking
$config = [pscustomobject]@{{
    notify = [pscustomobject]@{{
        enabled = $true
        channel = 'webhook'
        webhook = [pscustomobject]@{{
            url = '{webhook_url}'
            timeoutSeconds = 5
        }}
    }}
}}
$session = New-BackupOpsLogSession -Action 'restore-data' -LogRoot '{log_root}' -OperatorName 'tester' -Mode 'restore-data'
$context = @{{
    backupId = '20260520-010203'
    restorePoint = '20260520-010203'
    imageTag = '20260520_000001'
    preRestoreSnapshotId = '20260520_020304_pre-restore'
}}
$result = Send-BackupOpsNotification -Config $config -Action 'restore-data' -Status 'success' -Summary 'restore completed and validated' -Context $context -LogSession $session
$result | ConvertTo-Json -Depth 8
"""
            completed = _run_powershell_script(script)
            assert completed.returncode == 0, completed.stdout + completed.stderr
            result = json.loads(completed.stdout)
    finally:
        server.shutdown()
        server.server_close()
        thread.join(timeout=5)

    assert result["status"] == "sent"
    assert result["channel"] == "webhook"
    assert received["path"] == "/notify"
    body = received["body"]
    assert body["action"] == "restore-data"
    assert body["status"] == "success"
    assert body["backupId"] == "20260520-010203"
    assert body["restorePoint"] == "20260520-010203"
    assert body["imageTag"] == "20260520_000001"
    assert body["preRestoreSnapshotId"] == "20260520_020304_pre-restore"
    assert "恢复点: 20260520-010203" in body["summary"]
    assert "IMAGE_TAG: 20260520_000001" in body["summary"]


def test_backup_now_failure_path_invokes_notification_with_context() -> None:
    root = _backup_root()
    module_path = root / "scripts" / "modules" / "UseCases" / "BackupNow.psm1"
    script = f"""
$ErrorActionPreference = 'Stop'
$script:notifications = @()
function Start-BackupOpsLogSession {{ param($Config, $Action, $OperatorName, $StartedAt) [pscustomobject]@{{ LogPath = 'log'; ReportPath = 'report' }} }}
function Show-BackupOpsBanner {{ }}
function Show-BackupOpsProgress {{ }}
function Assert-BackupOpsRemoteNasMounted {{ }}
function New-BackupOpsBackupWorkspace {{ param($Config, $Action, $BackupType) [pscustomobject]@{{ BackupId = '20260520-010203'; ImageTag = 'unknown'; MySqlPath = 'mysql'; ObjectsPath = 'objects'; DeployPath = 'deploy'; ManifestPath = 'manifest' }} }}
function Get-BackupOpsCurrentImageTag {{ param($Config, $LogSession) '20260520_000001' }}
function Export-BackupOpsMySqlDump {{
    $ex = [System.InvalidOperationException]::new('dump failed')
    $ex.Data['BackupOpsCode'] = 'INTBK-3001'
    $ex.Data['BackupOpsStatus'] = 'fail'
    throw $ex
}}
function Invoke-BackupOpsNotificationCapture {{
    param($Config, $Action, $Status, $Summary, $Context, $LogSession)
    $script:notifications += [pscustomobject]@{{ action = $Action; status = $Status; summary = $Summary; backupId = $Context['backupId']; imageTag = $Context['imageTag'] }}
    [pscustomobject]@{{ status = 'sent'; channel = 'webhook'; message = 'sent'; summary = $Summary }}
}}
function Set-BackupOpsNotificationContext {{ param($Context, $NotificationResult) $Context['notificationStatus'] = $NotificationResult.status }}
function Get-BackupOpsNotificationOutcomeMessage {{ param($NotificationResult) ' notified' }}
function Complete-BackupOpsOutcome {{
    param($Config, $Action, $Status, $Code, $Message, $StartedAt, $CompletedAt, $Context, $LogSession)
    [pscustomobject]@{{ status = $Status; code = $Code; message = $Message; context = $Context; notifications = @($script:notifications) }}
}}
Import-Module '{module_path}' -Force -DisableNameChecking
$result = Invoke-BackupNowUseCase -Config ([pscustomobject]@{{}}) -NonInteractive
$result | ConvertTo-Json -Depth 8
"""
    completed = _run_powershell_script(script)
    assert completed.returncode == 0, completed.stdout + completed.stderr
    payload = json.loads(completed.stdout)

    assert payload["status"] == "fail"
    assert payload["code"] == "INTBK-3001"
    assert payload["context"]["backupId"] == "20260520-010203"
    assert payload["context"]["imageTag"] == "20260520_000001"
    assert payload["context"]["notificationStatus"] == "sent"
    assert payload["notifications"][0]["action"] == "backup-now"
    assert payload["notifications"][0]["status"] == "fail"


def test_rollback_blocked_path_invokes_notification_module() -> None:
    root = _backup_root()
    module_path = root / "scripts" / "modules" / "UseCases" / "RollbackApp.psm1"
    script = f"""
$ErrorActionPreference = 'Stop'
$script:notifications = @()
function Start-BackupOpsLogSession {{ param($Config, $Action, $OperatorName, $StartedAt) [pscustomobject]@{{ LogPath = 'log'; ReportPath = 'report' }} }}
function Show-BackupOpsBanner {{ }}
function Show-BackupOpsProgress {{ }}
function Get-BackupOpsRollbackTags {{ param($Config, $LogSession) @() }}
function Select-BackupOpsImageTag {{ param($Candidates, $SelectedImageTag) $null }}
function Invoke-BackupOpsNotificationCapture {{
    param($Config, $Action, $Status, $Summary, $Context, $LogSession)
    $script:notifications += [pscustomobject]@{{ action = $Action; status = $Status; summary = $Summary }}
    [pscustomobject]@{{ status = 'sent'; channel = 'webhook'; message = 'sent'; summary = $Summary }}
}}
function Set-BackupOpsNotificationContext {{ param($Context, $NotificationResult) $Context['notificationStatus'] = $NotificationResult.status }}
function Get-BackupOpsNotificationOutcomeMessage {{ param($NotificationResult) ' notified' }}
function Complete-BackupOpsOutcome {{
    param($Config, $Action, $Status, $Code, $Message, $StartedAt, $CompletedAt, $Context, $LogSession)
    [pscustomobject]@{{ status = $Status; code = $Code; message = $Message; context = $Context; notifications = @($script:notifications) }}
}}
Import-Module '{module_path}' -Force -DisableNameChecking
$result = Invoke-RollbackAppUseCase -Config ([pscustomobject]@{{}}) -NonInteractive
$result | ConvertTo-Json -Depth 8
"""
    completed = _run_powershell_script(script)
    assert completed.returncode == 0, completed.stdout + completed.stderr
    payload = json.loads(completed.stdout)

    assert payload["status"] == "blocked"
    assert payload["context"]["notificationStatus"] == "sent"
    assert payload["notifications"][0]["action"] == "rollback-app"
    assert payload["notifications"][0]["status"] == "blocked"


def test_rollback_blocks_selected_tag_without_compatibility_candidate_before_env_backup() -> None:
    root = _backup_root()
    module_path = root / "scripts" / "modules" / "UseCases" / "RollbackApp.psm1"
    script = f"""
$ErrorActionPreference = 'Stop'
$script:notifications = @()
function Start-BackupOpsLogSession {{ param($Config, $Action, $OperatorName, $StartedAt) [pscustomobject]@{{ LogPath = 'log'; ReportPath = 'report' }} }}
function Show-BackupOpsBanner {{ }}
function Show-BackupOpsProgress {{ }}
function Get-BackupOpsRollbackTags {{ param($Config, $LogSession) @('release-compatible') }}
function Select-BackupOpsImageTag {{ param($Candidates, $SelectedImageTag) $SelectedImageTag }}
function Save-BackupOpsRuntimeEnvBackup {{ throw [System.InvalidOperationException]::new('must block before env backup') }}
function Invoke-BackupOpsNotificationCapture {{
    param($Config, $Action, $Status, $Summary, $Context, $LogSession)
    $script:notifications += [pscustomobject]@{{ action = $Action; status = $Status; summary = $Summary }}
    [pscustomobject]@{{ status = 'sent'; channel = 'webhook'; message = 'sent'; summary = $Summary }}
}}
function Set-BackupOpsNotificationContext {{ param($Context, $NotificationResult) $Context['notificationStatus'] = $NotificationResult.status }}
function Get-BackupOpsNotificationOutcomeMessage {{ param($NotificationResult) ' notified' }}
function Complete-BackupOpsOutcome {{
    param($Config, $Action, $Status, $Code, $Message, $StartedAt, $CompletedAt, $Context, $LogSession)
    [pscustomobject]@{{ status = $Status; code = $Code; message = $Message; context = $Context; notifications = @($script:notifications) }}
}}
Import-Module '{module_path}' -Force -DisableNameChecking
$result = Invoke-RollbackAppUseCase -Config ([pscustomobject]@{{}}) -SelectedImageTag 'release-old' -NonInteractive
$result | ConvertTo-Json -Depth 8
"""
    completed = _run_powershell_script(script)
    assert completed.returncode == 0, completed.stdout + completed.stderr
    payload = json.loads(completed.stdout)

    assert payload["status"] == "blocked"
    assert "compat" in payload["message"].lower() or "兼容" in payload["message"]
    assert payload["context"]["notificationStatus"] == "sent"


def test_restore_data_sends_start_notification_before_mutating_steps() -> None:
    root = _backup_root()
    module_path = root / "scripts" / "modules" / "UseCases" / "RestoreData.psm1"
    script = f"""
$ErrorActionPreference = 'Stop'
$script:events = @()
function Start-BackupOpsLogSession {{ param($Config, $Action, $OperatorName, $StartedAt) [pscustomobject]@{{ LogPath = 'log'; ReportPath = 'report' }} }}
function Show-BackupOpsBanner {{ }}
function Show-BackupOpsProgress {{ }}
function Get-BackupOpsRestoreCandidates {{ param($Config, $LogSession, $SelectedBackupId) @([pscustomobject]@{{ backupId = '20260520-010203'; imageTag = '20260520_000001'; redisPolicy = 'CLEAR_AND_REBUILD'; configurationManifestPath = 'deploy/runtime.env'; configurationComposePath = 'deploy/docker-compose.yml'; checksumsSha256 = 'abc'; rehearsalStatus = 'PASSED'; lastRehearsedAt = '2026-06-09T06:20:00+08:00'; backupStrategyMode = 'incremental-manifest'; mysqlBackupMode = 'full-dump-baseline'; dccBackupManifestPath = 'manifest/dcc-backup-manifest.json' }}) }}
function Get-BackupOpsCurrentImageTag {{ param($Config, $LogSession) '20260520_000001' }}
function Select-BackupOpsRestorePoint {{ param($Candidates, $SelectedBackupId) '20260520-010203' }}
function Send-BackupOpsNotification {{
    param($Config, $Action, $Status, $Summary, $Context, $LogSession)
    $script:events += ('notify:' + $Status)
    [pscustomobject]@{{ status = if ($Status -eq 'started') {{ 'sent' }} else {{ 'disabled' }}; channel = 'webhook'; message = 'ok'; summary = $Summary }}
}}
function New-BackupOpsPreRestoreSnapshot {{ param($Config, $BackupId, $LogSession) [pscustomobject]@{{ SnapshotId = 'snap-1' }} }}
function Stop-BackupOpsFrontendBackend {{ param($Config, $LogSession) $script:events += 'stop' }}
function Import-BackupOpsMySqlDump {{ }}
function Restore-BackupOpsObjectBucket {{ }}
function Restore-BackupOpsDependentAssets {{ }}
function Start-BackupOpsFrontendBackend {{ }}
function Test-BackupOpsRestoreValidation {{ }}
function Publish-BackupOpsReport {{ param($Config, $Action, $Status, $StartedAt, $CompletedAt, $Summary, $Context, $LogSession) [pscustomobject]@{{ LogPath = 'log-final'; ReportPath = 'report-final' }} }}
function Set-BackupOpsNotificationContext {{ param($Context, $NotificationResult) $Context['notificationStatus'] = $NotificationResult.status }}
function Get-BackupOpsNotificationOutcomeMessage {{ param($NotificationResult) ' disabled' }}
function New-BackupOpsResult {{
    param($Action, $Status, $Code, $Message, $StartedAt, $CompletedAt, $LogPath, $ReportPath, $Context)
    [pscustomobject]@{{ status = $Status; code = $Code; message = $Message; context = $Context; events = @($script:events) }}
}}
Import-Module '{module_path}' -Force -DisableNameChecking
$result = Invoke-RestoreDataUseCase -Config ([pscustomobject]@{{ environment = 'test' }}) -SelectedBackupId '20260520-010203' -NonInteractive
$result | ConvertTo-Json -Depth 8
"""
    completed = _run_powershell_script(script)
    assert completed.returncode == 0, completed.stdout + completed.stderr
    payload = json.loads(completed.stdout)

    assert payload["status"] == "success"
    assert payload["events"].index("notify:started") < payload["events"].index("stop")
    assert payload["context"]["startNotification"]["status"] == "sent"


def test_restore_data_blocks_unverified_candidate_before_start_notification() -> None:
    root = _backup_root()
    module_path = root / "scripts" / "modules" / "UseCases" / "RestoreData.psm1"
    script = f"""
$ErrorActionPreference = 'Stop'
$script:events = @()
function Start-BackupOpsLogSession {{ param($Config, $Action, $OperatorName, $StartedAt) [pscustomobject]@{{ LogPath = 'log'; ReportPath = 'report' }} }}
function Show-BackupOpsBanner {{ }}
function Show-BackupOpsProgress {{ }}
function Get-BackupOpsRestoreCandidates {{ param($Config, $LogSession, $SelectedBackupId) @([pscustomobject]@{{ backupId = '20260520-010203'; imageTag = 'release-current'; redisPolicy = 'CLEAR_AND_REBUILD'; configurationManifestPath = 'deploy/runtime.env'; configurationComposePath = 'deploy/docker-compose.yml'; checksumsSha256 = 'abc'; rehearsalStatus = 'unverified'; lastRehearsedAt = '' }}) }}
function Get-BackupOpsCurrentImageTag {{ param($Config, $LogSession) 'release-current' }}
function Select-BackupOpsRestorePoint {{ param($Candidates, $SelectedBackupId) '20260520-010203' }}
function Send-BackupOpsNotification {{ param($Config, $Action, $Status, $Summary, $Context, $LogSession) $script:events += ('notify:' + $Status); [pscustomobject]@{{ status = 'sent'; channel = 'webhook'; message = 'sent'; summary = $Summary }} }}
function New-BackupOpsPreRestoreSnapshot {{ throw [System.InvalidOperationException]::new('must block before pre-restore snapshot') }}
function Invoke-BackupOpsNotificationCapture {{
    param($Config, $Action, $Status, $Summary, $Context, $LogSession)
    $script:events += ('capture:' + $Status)
    [pscustomobject]@{{ status = 'sent'; channel = 'webhook'; message = 'sent'; summary = $Summary }}
}}
function Set-BackupOpsNotificationContext {{ param($Context, $NotificationResult) $Context['notificationStatus'] = $NotificationResult.status }}
function Get-BackupOpsNotificationOutcomeMessage {{ param($NotificationResult) ' notified' }}
function Complete-BackupOpsOutcome {{
    param($Config, $Action, $Status, $Code, $Message, $StartedAt, $CompletedAt, $Context, $LogSession)
    [pscustomobject]@{{ status = $Status; code = $Code; message = $Message; context = $Context; events = @($script:events) }}
}}
Import-Module '{module_path}' -Force -DisableNameChecking
$result = Invoke-RestoreDataUseCase -Config ([pscustomobject]@{{ environment = 'test' }}) -SelectedBackupId '20260520-010203' -NonInteractive
$result | ConvertTo-Json -Depth 8
"""
    completed = _run_powershell_script(script)
    assert completed.returncode == 0, completed.stdout + completed.stderr
    payload = json.loads(completed.stdout)

    assert payload["status"] == "blocked"
    assert payload["code"] == "INTBK-3002"
    assert "rehearsalStatus" in payload["message"]
    assert "PASSED" in payload["message"]
    assert "notify:started" not in payload["events"]
    assert payload["events"] == ["capture:blocked"]


def test_restore_data_blocks_candidate_without_backup_strategy_before_start_notification() -> None:
    root = _backup_root()
    module_path = root / "scripts" / "modules" / "UseCases" / "RestoreData.psm1"
    script = f"""
$ErrorActionPreference = 'Stop'
$script:events = @()
function Start-BackupOpsLogSession {{ param($Config, $Action, $OperatorName, $StartedAt) [pscustomobject]@{{ LogPath = 'log'; ReportPath = 'report' }} }}
function Show-BackupOpsBanner {{ }}
function Show-BackupOpsProgress {{ }}
function Get-BackupOpsRestoreCandidates {{ param($Config, $LogSession, $SelectedBackupId) @([pscustomobject]@{{ backupId = '20260520-010203'; imageTag = 'release-current'; redisPolicy = 'CLEAR_AND_REBUILD'; configurationManifestPath = 'deploy/runtime.env'; configurationComposePath = 'deploy/docker-compose.yml'; checksumsSha256 = 'abc'; rehearsalStatus = 'PASSED'; lastRehearsedAt = '2026-06-09T06:20:00+08:00' }}) }}
function Get-BackupOpsCurrentImageTag {{ param($Config, $LogSession) 'release-current' }}
function Select-BackupOpsRestorePoint {{ param($Candidates, $SelectedBackupId) '20260520-010203' }}
function Send-BackupOpsNotification {{ param($Config, $Action, $Status, $Summary, $Context, $LogSession) $script:events += ('notify:' + $Status); [pscustomobject]@{{ status = 'sent'; channel = 'webhook'; message = 'sent'; summary = $Summary }} }}
function New-BackupOpsPreRestoreSnapshot {{ throw [System.InvalidOperationException]::new('must block before pre-restore snapshot') }}
function Invoke-BackupOpsNotificationCapture {{
    param($Config, $Action, $Status, $Summary, $Context, $LogSession)
    $script:events += ('capture:' + $Status)
    [pscustomobject]@{{ status = 'sent'; channel = 'webhook'; message = 'sent'; summary = $Summary }}
}}
function Set-BackupOpsNotificationContext {{ param($Context, $NotificationResult) $Context['notificationStatus'] = $NotificationResult.status }}
function Get-BackupOpsNotificationOutcomeMessage {{ param($NotificationResult) ' notified' }}
function Complete-BackupOpsOutcome {{
    param($Config, $Action, $Status, $Code, $Message, $StartedAt, $CompletedAt, $Context, $LogSession)
    [pscustomobject]@{{ status = $Status; code = $Code; message = $Message; context = $Context; events = @($script:events) }}
}}
Import-Module '{module_path}' -Force -DisableNameChecking
$result = Invoke-RestoreDataUseCase -Config ([pscustomobject]@{{ environment = 'test' }}) -SelectedBackupId '20260520-010203' -NonInteractive
$result | ConvertTo-Json -Depth 8
"""
    completed = _run_powershell_script(script)
    assert completed.returncode == 0, completed.stdout + completed.stderr
    payload = json.loads(completed.stdout)

    assert payload["status"] == "blocked"
    assert payload["code"] == "INTBK-3002"
    assert "backupStrategy.mode" in payload["message"]
    assert "notify:started" not in payload["events"]
    assert payload["events"] == ["capture:blocked"]


def test_restore_data_blocks_candidate_without_dcc_manifest_before_start_notification() -> None:
    root = _backup_root()
    module_path = root / "scripts" / "modules" / "UseCases" / "RestoreData.psm1"
    script = f"""
$ErrorActionPreference = 'Stop'
$script:events = @()
function Start-BackupOpsLogSession {{ param($Config, $Action, $OperatorName, $StartedAt) [pscustomobject]@{{ LogPath = 'log'; ReportPath = 'report' }} }}
function Show-BackupOpsBanner {{ }}
function Show-BackupOpsProgress {{ }}
function Get-BackupOpsRestoreCandidates {{ param($Config, $LogSession, $SelectedBackupId) @([pscustomobject]@{{ backupId = '20260520-010203'; imageTag = 'release-current'; redisPolicy = 'CLEAR_AND_REBUILD'; configurationManifestPath = 'deploy/runtime.env'; configurationComposePath = 'deploy/docker-compose.yml'; checksumsSha256 = 'abc'; rehearsalStatus = 'PASSED'; lastRehearsedAt = '2026-06-09T06:20:00+08:00'; backupStrategyMode = 'incremental-manifest'; mysqlBackupMode = 'full-dump-baseline' }}) }}
function Get-BackupOpsCurrentImageTag {{ param($Config, $LogSession) 'release-current' }}
function Select-BackupOpsRestorePoint {{ param($Candidates, $SelectedBackupId) '20260520-010203' }}
function Send-BackupOpsNotification {{ param($Config, $Action, $Status, $Summary, $Context, $LogSession) $script:events += ('notify:' + $Status); [pscustomobject]@{{ status = 'sent'; channel = 'webhook'; message = 'sent'; summary = $Summary }} }}
function New-BackupOpsPreRestoreSnapshot {{ throw [System.InvalidOperationException]::new('must block before pre-restore snapshot') }}
function Invoke-BackupOpsNotificationCapture {{
    param($Config, $Action, $Status, $Summary, $Context, $LogSession)
    $script:events += ('capture:' + $Status)
    [pscustomobject]@{{ status = 'sent'; channel = 'webhook'; message = 'sent'; summary = $Summary }}
}}
function Set-BackupOpsNotificationContext {{ param($Context, $NotificationResult) $Context['notificationStatus'] = $NotificationResult.status }}
function Get-BackupOpsNotificationOutcomeMessage {{ param($NotificationResult) ' notified' }}
function Complete-BackupOpsOutcome {{
    param($Config, $Action, $Status, $Code, $Message, $StartedAt, $CompletedAt, $Context, $LogSession)
    [pscustomobject]@{{ status = $Status; code = $Code; message = $Message; context = $Context; events = @($script:events) }}
}}
Import-Module '{module_path}' -Force -DisableNameChecking
$result = Invoke-RestoreDataUseCase -Config ([pscustomobject]@{{ environment = 'test' }}) -SelectedBackupId '20260520-010203' -NonInteractive
$result | ConvertTo-Json -Depth 8
"""
    completed = _run_powershell_script(script)
    assert completed.returncode == 0, completed.stdout + completed.stderr
    payload = json.loads(completed.stdout)

    assert payload["status"] == "blocked"
    assert payload["code"] == "INTBK-3002"
    assert "recoverySet.dcc.manifestPath" in payload["message"]
    assert "notify:started" not in payload["events"]
    assert payload["events"] == ["capture:blocked"]


def test_restore_data_blocks_program_image_mismatch_before_snapshot() -> None:
    root = _backup_root()
    module_path = root / "scripts" / "modules" / "UseCases" / "RestoreData.psm1"
    script = f"""
$ErrorActionPreference = 'Stop'
$script:events = @()
function Start-BackupOpsLogSession {{ param($Config, $Action, $OperatorName, $StartedAt) [pscustomobject]@{{ LogPath = 'log'; ReportPath = 'report' }} }}
function Show-BackupOpsBanner {{ }}
function Show-BackupOpsProgress {{ }}
function Get-BackupOpsRestoreCandidates {{ param($Config, $LogSession, $SelectedBackupId) @([pscustomobject]@{{ backupId = '20260520-010203'; imageTag = 'release-from-backup'; redisPolicy = 'CLEAR_AND_REBUILD'; configurationManifestPath = 'deploy/runtime.env'; configurationComposePath = 'deploy/docker-compose.yml'; checksumsSha256 = 'abc'; rehearsalStatus = 'PASSED'; lastRehearsedAt = '2026-06-09T06:20:00+08:00'; backupStrategyMode = 'incremental-manifest'; mysqlBackupMode = 'full-dump-baseline'; dccBackupManifestPath = 'manifest/dcc-backup-manifest.json' }}) }}
function Get-BackupOpsCurrentImageTag {{ param($Config, $LogSession) 'release-current' }}
function Select-BackupOpsRestorePoint {{ param($Candidates, $SelectedBackupId) '20260520-010203' }}
function Send-BackupOpsNotification {{ param($Config, $Action, $Status, $Summary, $Context, $LogSession) $script:events += ('notify:' + $Status); [pscustomobject]@{{ status = 'sent'; channel = 'webhook'; message = 'sent'; summary = $Summary }} }}
function New-BackupOpsPreRestoreSnapshot {{ throw [System.InvalidOperationException]::new('must block before pre-restore snapshot') }}
function Invoke-BackupOpsNotificationCapture {{
    param($Config, $Action, $Status, $Summary, $Context, $LogSession)
    $script:events += ('capture:' + $Status)
    [pscustomobject]@{{ status = 'sent'; channel = 'webhook'; message = 'sent'; summary = $Summary }}
}}
function Set-BackupOpsNotificationContext {{ param($Context, $NotificationResult) $Context['notificationStatus'] = $NotificationResult.status }}
function Get-BackupOpsNotificationOutcomeMessage {{ param($NotificationResult) ' notified' }}
function Complete-BackupOpsOutcome {{
    param($Config, $Action, $Status, $Code, $Message, $StartedAt, $CompletedAt, $Context, $LogSession)
    [pscustomobject]@{{ status = $Status; code = $Code; message = $Message; context = $Context; events = @($script:events) }}
}}
Import-Module '{module_path}' -Force -DisableNameChecking
$result = Invoke-RestoreDataUseCase -Config ([pscustomobject]@{{ environment = 'test' }}) -SelectedBackupId '20260520-010203' -NonInteractive
$result | ConvertTo-Json -Depth 8
"""
    completed = _run_powershell_script(script)
    assert completed.returncode == 0, completed.stdout + completed.stderr
    payload = json.loads(completed.stdout)

    assert payload["status"] == "blocked"
    assert payload["code"] == "INTBK-3002"
    assert "recoverySet.program.imageTag" in payload["message"]
    assert "release-from-backup" in payload["message"]
    assert "release-current" in payload["message"]
    assert "notify:started" not in payload["events"]
    assert payload["events"] == ["capture:blocked"]


def test_restore_data_blocks_missing_recovery_scope_before_start_notification() -> None:
    root = _backup_root()
    module_path = root / "scripts" / "modules" / "UseCases" / "RestoreData.psm1"
    script = f"""
$ErrorActionPreference = 'Stop'
$script:events = @()
function Start-BackupOpsLogSession {{ param($Config, $Action, $OperatorName, $StartedAt) [pscustomobject]@{{ LogPath = 'log'; ReportPath = 'report' }} }}
function Show-BackupOpsBanner {{ }}
function Show-BackupOpsProgress {{ }}
function Get-BackupOpsRestoreCandidates {{ param($Config, $LogSession, $SelectedBackupId) @([pscustomobject]@{{ backupId = '20260520-010203'; imageTag = 'release-current'; redisPolicy = ''; configurationManifestPath = 'deploy/runtime.env'; configurationComposePath = 'deploy/docker-compose.yml'; checksumsSha256 = 'abc'; rehearsalStatus = 'PASSED'; lastRehearsedAt = '2026-06-09T06:20:00+08:00'; backupStrategyMode = 'incremental-manifest'; mysqlBackupMode = 'full-dump-baseline'; dccBackupManifestPath = 'manifest/dcc-backup-manifest.json' }}) }}
function Get-BackupOpsCurrentImageTag {{ param($Config, $LogSession) 'release-current' }}
function Select-BackupOpsRestorePoint {{ param($Candidates, $SelectedBackupId) '20260520-010203' }}
function Send-BackupOpsNotification {{ param($Config, $Action, $Status, $Summary, $Context, $LogSession) $script:events += ('notify:' + $Status); [pscustomobject]@{{ status = 'sent'; channel = 'webhook'; message = 'sent'; summary = $Summary }} }}
function New-BackupOpsPreRestoreSnapshot {{ throw [System.InvalidOperationException]::new('must block before pre-restore snapshot') }}
function Invoke-BackupOpsNotificationCapture {{
    param($Config, $Action, $Status, $Summary, $Context, $LogSession)
    $script:events += ('capture:' + $Status)
    [pscustomobject]@{{ status = 'sent'; channel = 'webhook'; message = 'sent'; summary = $Summary }}
}}
function Set-BackupOpsNotificationContext {{ param($Context, $NotificationResult) $Context['notificationStatus'] = $NotificationResult.status }}
function Get-BackupOpsNotificationOutcomeMessage {{ param($NotificationResult) ' notified' }}
function Complete-BackupOpsOutcome {{
    param($Config, $Action, $Status, $Code, $Message, $StartedAt, $CompletedAt, $Context, $LogSession)
    [pscustomobject]@{{ status = $Status; code = $Code; message = $Message; context = $Context; events = @($script:events) }}
}}
Import-Module '{module_path}' -Force -DisableNameChecking
$result = Invoke-RestoreDataUseCase -Config ([pscustomobject]@{{ environment = 'test' }}) -SelectedBackupId '20260520-010203' -NonInteractive
$result | ConvertTo-Json -Depth 8
"""
    completed = _run_powershell_script(script)
    assert completed.returncode == 0, completed.stdout + completed.stderr
    payload = json.loads(completed.stdout)

    assert payload["status"] == "blocked"
    assert payload["code"] == "INTBK-3002"
    assert "recoverySet.redis.policy" in payload["message"]
    assert "notify:started" not in payload["events"]
    assert payload["events"] == ["capture:blocked"]


def test_backup_scheduled_records_cleanup_success_and_notifies() -> None:
    root = _backup_root()
    module_path = root / "scripts" / "modules" / "UseCases" / "BackupScheduled.psm1"
    script = f"""
$ErrorActionPreference = 'Stop'
$script:notifications = @()
function Start-BackupOpsLogSession {{ param($Config, $Action, $OperatorName, $StartedAt) [pscustomobject]@{{ LogPath = 'log'; ReportPath = 'report' }} }}
function Show-BackupOpsProgress {{ }}
function Assert-BackupOpsRemoteNasMounted {{ }}
function New-BackupOpsBackupWorkspace {{ param($Config, $Action, $BackupType) [pscustomobject]@{{ BackupId = '20260520-010203'; ImageTag = 'unknown'; MySqlPath = 'mysql'; ObjectsPath = 'objects'; DeployPath = 'deploy'; ManifestPath = 'manifest' }} }}
function Get-BackupOpsCurrentImageTag {{ param($Config, $LogSession) '20260520_000001' }}
function Save-BackupOpsDeployMetadata {{ }}
function Export-BackupOpsMySqlDump {{ }}
function Backup-BackupOpsObjectBucket {{ }}
function New-BackupOpsDccBackupManifest {{ }}
function Assert-BackupOpsDccBackupManifestReady {{ }}
function New-BackupOpsChecksums {{ }}
function New-BackupOpsManifest {{ }}
function Sync-BackupOpsBackupToTestServer {{ }}
function Sync-BackupOpsManifestToTestServer {{ }}
function Invoke-BackupOpsLocalRetention {{ 'local cleaned' }}
function Invoke-BackupOpsRemoteRetention {{ 'remote cleaned' }}
function Publish-BackupOpsReport {{ param($Config, $Action, $Status, $StartedAt, $CompletedAt, $Summary, $Context, $LogSession) [pscustomobject]@{{ LogPath = 'log-final'; ReportPath = 'report-final' }} }}
function Send-BackupOpsNotification {{
    param($Config, $Action, $Status, $Summary, $Context, $LogSession)
    $script:notifications += [pscustomobject]@{{ action = $Action; status = $Status; summary = $Summary; cleanup = $Context['cleanup'] }}
    [pscustomobject]@{{ status = 'sent'; channel = 'webhook'; message = 'sent'; summary = $Summary }}
}}
function Invoke-BackupOpsNotificationCapture {{
    param($Config, $Action, $Status, $Summary, $Context, $LogSession)
    $script:notifications += [pscustomobject]@{{ action = $Action; status = $Status; summary = $Summary; cleanup = $Context['cleanup'] }}
    [pscustomobject]@{{ status = 'sent'; channel = 'webhook'; message = 'sent'; summary = $Summary }}
}}
function Set-BackupOpsNotificationContext {{ param($Context, $NotificationResult) $Context['notificationStatus'] = $NotificationResult.status }}
function Get-BackupOpsNotificationOutcomeMessage {{ param($NotificationResult) ' notified' }}
function New-BackupOpsResult {{
    param($Action, $Status, $Code, $Message, $StartedAt, $CompletedAt, $LogPath, $ReportPath, $Context)
    [pscustomobject]@{{ status = $Status; code = $Code; message = $Message; context = $Context; notifications = @($script:notifications) }}
}}
Import-Module '{module_path}' -Force -DisableNameChecking
$result = Invoke-BackupScheduledUseCase -Config ([pscustomobject]@{{}}) -OperatorName 'scheduler'
$result | ConvertTo-Json -Depth 10
"""
    completed = _run_powershell_script(script)
    assert completed.returncode == 0, completed.stdout + completed.stderr
    payload = json.loads(completed.stdout)

    assert payload["status"] == "success"
    assert payload["context"]["cleanup"]["localRetention"]["status"] == "success"
    assert payload["context"]["cleanup"]["remoteRetention"]["status"] == "success"
    assert "正式机临时副本=success" in payload["notifications"][0]["summary"]
    assert "测试机过期备份=success" in payload["notifications"][0]["summary"]


def test_backup_scheduled_cleanup_failure_is_captured_before_failure_notification() -> None:
    root = _backup_root()
    module_path = root / "scripts" / "modules" / "UseCases" / "BackupScheduled.psm1"
    script = f"""
$ErrorActionPreference = 'Stop'
$script:notifications = @()
function Start-BackupOpsLogSession {{ param($Config, $Action, $OperatorName, $StartedAt) [pscustomobject]@{{ LogPath = 'log'; ReportPath = 'report' }} }}
function Show-BackupOpsProgress {{ }}
function Assert-BackupOpsRemoteNasMounted {{ }}
function New-BackupOpsBackupWorkspace {{ param($Config, $Action, $BackupType) [pscustomobject]@{{ BackupId = '20260520-010203'; ImageTag = 'unknown'; MySqlPath = 'mysql'; ObjectsPath = 'objects'; DeployPath = 'deploy'; ManifestPath = 'manifest' }} }}
function Get-BackupOpsCurrentImageTag {{ param($Config, $LogSession) '20260520_000001' }}
function Save-BackupOpsDeployMetadata {{ }}
function Export-BackupOpsMySqlDump {{ }}
function Backup-BackupOpsObjectBucket {{ }}
function New-BackupOpsDccBackupManifest {{ }}
function Assert-BackupOpsDccBackupManifestReady {{ }}
function New-BackupOpsChecksums {{ }}
function New-BackupOpsManifest {{ }}
function Sync-BackupOpsBackupToTestServer {{ }}
function Sync-BackupOpsManifestToTestServer {{ }}
function Invoke-BackupOpsLocalRetention {{ throw [System.InvalidOperationException]::new('local retention failed') }}
function Invoke-BackupOpsNotificationCapture {{
    param($Config, $Action, $Status, $Summary, $Context, $LogSession)
    $script:notifications += [pscustomobject]@{{ action = $Action; status = $Status; summary = $Summary; cleanup = $Context['cleanup'] }}
    [pscustomobject]@{{ status = 'sent'; channel = 'webhook'; message = 'sent'; summary = $Summary }}
}}
function Set-BackupOpsNotificationContext {{ param($Context, $NotificationResult) $Context['notificationStatus'] = $NotificationResult.status }}
function Get-BackupOpsNotificationOutcomeMessage {{ param($NotificationResult) ' notified' }}
function Complete-BackupOpsOutcome {{
    param($Config, $Action, $Status, $Code, $Message, $StartedAt, $CompletedAt, $Context, $LogSession)
    [pscustomobject]@{{ status = $Status; code = $Code; message = $Message; context = $Context; notifications = @($script:notifications) }}
}}
Import-Module '{module_path}' -Force -DisableNameChecking
$result = Invoke-BackupScheduledUseCase -Config ([pscustomobject]@{{}}) -OperatorName 'scheduler'
$result | ConvertTo-Json -Depth 10
"""
    completed = _run_powershell_script(script)
    assert completed.returncode == 0, completed.stdout + completed.stderr
    payload = json.loads(completed.stdout)

    assert payload["status"] == "fail"
    assert payload["context"]["cleanup"]["localRetention"]["status"] == "fail"
    assert payload["notifications"][0]["status"] == "fail"


def test_backup_now_can_run_twice_without_log_name_collision() -> None:
    first = _run_backup_ops_mode("backup-now", "-NonInteractive")
    second = _run_backup_ops_mode("backup-now", "-NonInteractive")

    assert first.returncode == 2, first.stdout + first.stderr
    assert second.returncode == 2, second.stdout + second.stderr
    assert "Cannot move item because the item at" not in second.stdout
