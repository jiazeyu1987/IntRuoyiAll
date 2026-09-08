from __future__ import annotations

import json
import subprocess
import tempfile
from pathlib import Path


BACKEND_ROOT = Path(__file__).resolve().parents[2]
BACKUP_ROOT = BACKEND_ROOT / "script" / "backup-ops"


def _run_powershell(script: str) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        ["powershell", "-NoProfile", "-Command", script],
        cwd=BACKEND_ROOT,
        text=True,
        encoding="utf-8",
        capture_output=True,
        check=False,
    )


def test_payload_checksums_cover_all_local_recovery_files() -> None:
    module = BACKUP_ROOT / "scripts" / "modules" / "Infra" / "FileOps.psm1"
    with tempfile.TemporaryDirectory() as temp_dir:
        root = Path(temp_dir) / "20260907-010000"
        paths = [
            "deploy/docker-compose.yml",
            "deploy/runtime.env",
            "deploy/image-tag.txt",
            "manifest/dcc-backup-manifest.json",
            "mysql/ruoyi-vue-pro.sql.gz.evidence.json",
            "mysql/binlog-segment-manifest.json",
            "mysql/binlog/mysql-bin.000001.sql",
            "objects/manifest-object-inventory.json",
        ]
        for relative in paths:
            target = root / relative
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(relative, encoding="utf-8")

        script = f"""
$ErrorActionPreference = 'Stop'
function Write-BackupOpsLog {{ param($Session, $Message, $Level) }}
Import-Module '{module}' -Force -DisableNameChecking
$workspace = [pscustomobject]@{{
  BackupRoot = '{root}'
  ManifestPath = '{root / 'manifest'}'
}}
$result = New-BackupOpsChecksums -Config ([pscustomobject]@{{}}) -Workspace $workspace -LogSession ([pscustomobject]@{{}})
$result | ConvertTo-Json -Depth 5 -Compress
"""
        result = _run_powershell(script)

        assert result.returncode == 0, result.stderr + result.stdout
        payload = json.loads(result.stdout.strip().splitlines()[-1])
        checksum_text = Path(payload["path"]).read_text(encoding="utf-8").replace("\\", "/")
        for relative in paths:
            assert relative in checksum_text


def test_mysql_restore_verifies_dump_before_resetting_database() -> None:
    source = (BACKUP_ROOT / "scripts" / "modules" / "Infra" / "MySqlOps.psm1").read_text(encoding="utf-8")
    spec = source.split("function New-BackupMySqlRestoreCommandSpec", 1)[1].split(
        "function Test-BackupMySqlConnectivity", 1
    )[0]
    restore = source.split("function Import-BackupMySqlDump", 1)[1].split(
        "function Export-BackupOpsMySqlDump", 1
    )[0]

    assert "gzip -t" in spec
    assert "sha256sum" in spec
    verify_index = restore.find("$restoreSpec.integrityCommand")
    reset_index = restore.find("$restoreSpec.resetCommand")
    assert verify_index >= 0
    assert verify_index < reset_index
    assert "'--events'" in source


def test_object_restore_verifies_repository_payload_before_mirror() -> None:
    source = (BACKUP_ROOT / "scripts" / "modules" / "Infra" / "ObjectOps.psm1").read_text(encoding="utf-8")
    restore = source.split("function Import-BackupObjectInventoryFromRemoteNas", 1)[1].split(
        "function Import-BackupObjectSnapshot", 1
    )[0]

    verify_index = restore.find("sha256sum")
    mirror_index = restore.find("mc mirror")
    assert verify_index >= 0
    assert verify_index < mirror_index


def test_launcher_requires_explicit_full_or_incremental_kind() -> None:
    source = (BACKUP_ROOT / "scripts" / "backup-ops.ps1").read_text(encoding="utf-8")

    assert "[ValidateSet('FULL', 'INCREMENTAL')]" in source
    assert "[string]$BackupKind" in source
    assert "Invoke-BackupNowUseCase -Config $Config -BackupKind $BackupKind" in source
    assert "Invoke-BackupScheduledUseCase -Config $Config -BackupKind $BackupKind" in source


def test_backup_use_cases_dispatch_mysql_by_kind_without_fallback() -> None:
    for name in ("BackupNow.psm1", "BackupScheduled.psm1"):
        source = (BACKUP_ROOT / "scripts" / "modules" / "UseCases" / name).read_text(encoding="utf-8")
        assert "[ValidateSet('FULL', 'INCREMENTAL')]" in source
        assert "if ($BackupKind -eq 'FULL')" in source
        assert "Export-BackupOpsMySqlDump" in source
        assert "Export-BackupOpsMySqlBinlogIncrement" in source
        assert "Backup-BackupOpsObjectBucket" in source
        assert "-BackupKind $BackupKind" in source


def test_manifest_records_explicit_chain_identity() -> None:
    source = (BACKUP_ROOT / "scripts" / "modules" / "Infra" / "FileOps.psm1").read_text(encoding="utf-8")
    manifest = source.split("function New-BackupOpsManifest", 1)[1].split(
        "function Import-BackupOpsDccManifestDependencies", 1
    )[0]

    for marker in (
        "[ValidateSet('FULL', 'INCREMENTAL')]",
        "$BackupKind",
        "backupKind",
        "baseBackupId",
        "parentBackupId",
        "mysqlEvidence",
    ):
        assert marker in manifest


def test_full_manifest_records_source_repository_and_chain_proof() -> None:
    file_module = BACKUP_ROOT / "scripts" / "modules" / "Infra" / "FileOps.psm1"
    report_module = BACKUP_ROOT / "scripts" / "modules" / "Infra" / "ReportOps.psm1"
    with tempfile.TemporaryDirectory() as temp_dir:
        root = Path(temp_dir) / "20260907-010000"
        deploy = root / "deploy"
        manifest = root / "manifest"
        mysql = root / "mysql"
        objects = root / "objects"
        for directory in (deploy, manifest, mysql, objects):
            directory.mkdir(parents=True)
        (deploy / "runtime.env").write_text(
            "BACKEND_HOST_PORT=48081\nFRONTEND_HOST_PORT=8081\n", encoding="utf-8"
        )
        (deploy / "docker-compose.yml").write_text("services: {}\n", encoding="utf-8")
        (mysql / "ruoyi-vue-pro.sql.gz").write_bytes(b"dump")
        (mysql / "full-dump-manifest.json").write_text(
            json.dumps(
                {
                    "schemaVersion": "mysql-full-dump-v1",
                    "status": "exported",
                    "endPosition": {"file": "mysql-bin.000001", "position": 100},
                }
            ),
            encoding="utf-8",
        )
        (objects / "manifest-object-inventory.json").write_text(
            json.dumps(
                    {
                        "mode": "incremental-manifest",
                        "backupKind": "FULL",
                        "parentBackupId": "",
                        "bucket": "yudao",
                    "objectStoreRoot": "/mnt/nas/Backup/BackupPackage/object-store",
                    "objects": [],
                }
            ),
            encoding="utf-8",
        )
        (manifest / "dcc-backup-manifest.json").write_text("{}", encoding="utf-8")
        (manifest / "checksums.txt").write_text("abc  deploy/runtime.env\n", encoding="utf-8")
        script = f"""
$ErrorActionPreference = 'Stop'
function Write-BackupOpsLog {{ param($Session, $Message, $Level) }}
Import-Module '{report_module}' -Force -DisableNameChecking
Import-Module '{file_module}' -Force -DisableNameChecking
$config = [pscustomobject]@{{
  environment = 'production'
  servers = [pscustomobject]@{{
    production = [pscustomobject]@{{ host = '172.30.30.57'; appDir = '/opt/intruoyi/runtime' }}
    test = [pscustomobject]@{{ host = '172.30.30.58' }}
  }}
  backup = [pscustomobject]@{{
    repositoryEnvironment = 'test'; mysqlDatabase = 'ruoyi-vue-pro'; objectBucket = 'yudao'
  }}
}}
$workspace = [pscustomobject]@{{
  BackupId = '20260907-010000'; BackupRoot = '{root}'; ImageTag = 'release-v1'
  DeployPath = '{deploy}'; ManifestPath = '{manifest}'; MySqlPath = '{mysql}'; ObjectsPath = '{objects}'
}}
$path = New-BackupOpsManifest -Config $config -Workspace $workspace -BackupType 'manual' -BackupKind FULL -LogSession ([pscustomobject]@{{ startedAt = [System.DateTimeOffset]::Now }})
([System.IO.File]::ReadAllText($path, [System.Text.Encoding]::UTF8) | ConvertFrom-Json) | ConvertTo-Json -Depth 10 -Compress
"""
        result = _run_powershell(script)

        assert result.returncode == 0, result.stderr + result.stdout
        payload = json.loads(result.stdout.strip().splitlines()[-1])
        assert payload["backupKind"] == "FULL"
        assert payload["baseBackupId"] == "20260907-010000"
        assert payload["parentBackupId"] is None
        assert payload["sourceEnvironment"] == "production"
        assert payload["sourceHost"] == "172.30.30.57"
        assert payload["repositoryEnvironment"] == "test"
        assert payload["repositoryHost"] == "172.30.30.58"


def test_scheduler_registers_separate_full_and_incremental_tasks() -> None:
    source = (BACKUP_ROOT / "actions" / "Register-BackupOpsScheduledTasks.ps1").read_text(encoding="utf-8")

    assert "IntRuoyi Backup Full" in source
    assert "IntRuoyi Backup Incremental" in source
    assert "-BackupKind FULL" in source
    assert "-BackupKind INCREMENTAL" in source
    assert "backup.fullSchedule" in source
    assert "backup.incrementalSchedule" in source
    assert "[string]$RepositoryEnvironment" in source
    assert "RepositoryEnvironment does not match backup.repositoryEnvironment" in source
    assert "-Weekly -DaysOfWeek $incrementalDays" in source
    assert "Sunday" not in source.split("function Resolve-BackupOpsIncrementalTrigger", 1)[1].split(
        "function Resolve-BackupOpsTaskPrincipalId", 1
    )[0]
    assert "Disable-ScheduledTask" in source
    assert "Scheduled task registration failed; all backup tasks were disabled" in source


def test_backup_and_rehearsal_modes_share_a_nonblocking_global_mutex() -> None:
    source = (BACKUP_ROOT / "scripts" / "backup-ops.ps1").read_text(encoding="utf-8")

    assert "Global\\IntRuoyi-BackupOps" in source
    assert "@('backup-now', 'backup-scheduled', 'rehearsal', 'restore-data')" in source
    assert "WaitOne(0)" in source
    assert "Another backup or rehearsal operation is already running" in source
    assert "ReleaseMutex" in source


def test_remote_retention_deletes_only_whole_full_chains() -> None:
    source = (BACKUP_ROOT / "scripts" / "modules" / "Infra" / "FileOps.psm1").read_text(encoding="utf-8")
    retention = source.split("function New-BackupOpsRemoteRetentionPythonScript", 1)[1].split(
        "function Get-BackupOpsWorkspaceRoot", 1
    )[0]

    for marker in ("KEEP_LAST_FULL_CHAINS", "MINIMUM_RECOVERABLE_CHAINS", "baseBackupId", "recoverable_chain_ids"):
        assert marker in retention
    assert "delete_chains" in retention
    assert "for chain_id in delete_chains" in retention


def test_runtime_metadata_is_redacted_and_rehearsal_uses_current_test_secrets() -> None:
    file_source = (BACKUP_ROOT / "scripts" / "modules" / "Infra" / "FileOps.psm1").read_text(encoding="utf-8")
    docker_source = (BACKUP_ROOT / "scripts" / "modules" / "Infra" / "DockerOps.psm1").read_text(encoding="utf-8")
    save = file_source.split("function Save-BackupOpsDeployMetadata", 1)[1].split(
        "function Read-BackupOpsObjectInventoryMarker", 1
    )[0]
    initialize = docker_source.split("function Initialize-BackupOpsRehearsalRuntime", 1)[1].split(
        "function Wait-BackupOpsMySqlReady", 1
    )[0]

    assert "ConvertTo-BackupOpsRedactedRuntimeEnv" in save
    assert "raw-runtime-env-" in save
    assert "<redacted>" in file_source
    assert "servers', 'test', 'runtimeDir" in initialize
    assert "/deploy/runtime.env" not in initialize


def test_binlog_increment_exports_continuous_parent_segment() -> None:
    module_path = BACKUP_ROOT / "scripts" / "modules" / "Infra" / "MySqlOps.psm1"
    with tempfile.TemporaryDirectory() as temp_dir:
        mysql_path = Path(temp_dir) / "mysql"
        mysql_path.mkdir()
        script = f"""
$ErrorActionPreference = 'Stop'
$script:commands = @()
function Write-BackupOpsLog {{ param($Session, $Message, $Level) }}
function Invoke-BackupSshCommand {{
    param($Request)
    $script:commands += [string]$Request.Command
    $command = [string]$Request.Command
    if ($command -like 'cat*runtime/.env*') {{
        return [pscustomobject]@{{ output = 'MYSQL_ROOT_PASSWORD=test-password' }}
    }}
    if ($command -like 'find*') {{
        return [pscustomobject]@{{ output = '/mnt/nas/Backup/BackupPackage/20260907-000000' }}
    }}
    if ($command -like 'cat*manifest/manifest.json*') {{
            return [pscustomobject]@{{ output = '{{"status":"success","backupKind":"FULL","backupId":"20260907-000000","baseBackupId":"20260907-000000","sourceEnvironment":"production","sourceHost":"172.30.30.57","repositoryEnvironment":"test","repositoryHost":"172.30.30.58","recoverySet":{{"status":"COMPLETE"}},"mysqlEvidence":{{"schemaVersion":"mysql-full-dump-v1","dumpPath":"mysql/ruoyi-vue-pro.sql.gz","sha256":"{('b' * 64)}","endPosition":{{"file":"mysql-bin.000001","position":100}}}}}}' }}
    }}
    if ($command -like '*SELECT @@global.log_bin*') {{
        return [pscustomobject]@{{ output = "1$([char]9)ROW$([char]9)604800$([Environment]::NewLine)mysql-bin.000001$([char]9)200" }}
    }}
    if ($command -like '*SHOW BINARY LOGS*') {{
        return [pscustomobject]@{{ output = "mysql-bin.000001$([char]9)300" }}
    }}
    if ($command -like '*mysqlbinlog*') {{
        return [pscustomobject]@{{ output = "$(('a' * 64))$([char]9)123" }}
    }}
    if ($command -like '*sha256sum -c*' -or $command -like '*gzip -t*') {{
        return [pscustomobject]@{{ output = 'VERIFIED' }}
    }}
    throw "Unexpected command: $command"
}}
Import-Module '{module_path}' -Force -DisableNameChecking
$config = [pscustomobject]@{{
    environment = 'production'
    servers = [pscustomobject]@{{
        production = [pscustomobject]@{{ host = '172.30.30.57'; appDir = '/opt/intruoyi/runtime' }}
        test = [pscustomobject]@{{ host = '172.30.30.58'; backupPointsRoot = '/mnt/nas/Backup/BackupPackage' }}
    }}
    containers = [pscustomobject]@{{ mysql = 'intruoyi-mysql' }}
    backup = [pscustomobject]@{{ mysqlDatabase = 'ruoyi-vue-pro' }}
    ssh = [pscustomobject]@{{ user = 'ops'; port = 22 }}
    auth = [pscustomobject]@{{ sshKeyPath = 'D:\\keys\\ops'; knownHostsPath = '' }}
}}
$workspace = [pscustomobject]@{{ BackupId = '20260907-010000'; MySqlPath = '{mysql_path}' }}
$evidence = Export-BackupOpsMySqlBinlogIncrement -Config $config -Workspace $workspace -LogSession ([pscustomobject]@{{}})
$evidence | ConvertTo-Json -Depth 8 -Compress
"""
        result = _run_powershell(script)

        assert result.returncode == 0, result.stderr + result.stdout
        payload = json.loads(result.stdout.strip().splitlines()[-1])
        assert payload["baseBackupId"] == "20260907-000000"
        assert payload["parentBackupId"] == "20260907-000000"
        assert payload["startPosition"] == {"file": "mysql-bin.000001", "position": 100}
        assert payload["endPosition"] == {"file": "mysql-bin.000001", "position": 200}
        assert payload["segments"][0]["startPosition"] == 100
        assert payload["segments"][0]["stopPosition"] == 200
        assert payload["segments"][0]["sha256"] == "a" * 64
        assert (mysql_path / "binlog-segment-manifest.json").is_file()


def test_backup_use_cases_restart_services_in_finally() -> None:
    for name in ("BackupNow.psm1", "BackupScheduled.psm1"):
        source = (BACKUP_ROOT / "scripts" / "modules" / "UseCases" / name).read_text(encoding="utf-8")
        stop_index = source.find("Stop-BackupOpsFrontendBackend")
        quiesced_index = source.find("Assert-BackupOpsWriteWindowQuiesced")
        mysql_index = min(
            index
            for index in (
                source.find("Export-BackupOpsMySqlDump"),
                source.find("Export-BackupOpsMySqlBinlogIncrement"),
            )
            if index >= 0
        )
        object_index = source.find("Backup-BackupOpsObjectBucket")
        finally_index = source.find("finally", object_index)
        start_index = source.find("Start-BackupOpsFrontendBackend", finally_index)
        health_index = source.find("Test-BackupOpsFrontendBackendHealth", start_index)

        assert "$servicesStopped = $false" in source
        assert 0 <= stop_index < quiesced_index < mysql_index < object_index < finally_index < start_index < health_index


def test_dcc_snapshot_is_captured_inside_the_same_write_window() -> None:
    for name in ("BackupNow.psm1", "BackupScheduled.psm1"):
        source = (BACKUP_ROOT / "scripts" / "modules" / "UseCases" / name).read_text(encoding="utf-8")
        stop_index = source.find("Stop-BackupOpsFrontendBackend")
        dcc_index = source.find("New-BackupOpsDccBackupManifest")
        finally_index = source.find("finally", dcc_index)
        start_index = source.find("Start-BackupOpsFrontendBackend", finally_index)

        assert 0 <= stop_index < dcc_index < finally_index < start_index


def test_incremental_object_and_dcc_use_the_mysql_direct_parent() -> None:
    object_source = (BACKUP_ROOT / "scripts" / "modules" / "Infra" / "ObjectOps.psm1").read_text(
        encoding="utf-8"
    )
    file_source = (BACKUP_ROOT / "scripts" / "modules" / "Infra" / "FileOps.psm1").read_text(encoding="utf-8")

    assert "ExpectedParentBackupId" in object_source
    assert "ExpectedParentBackupId" in file_source
    assert "parentBackupId = $ExpectedParentBackupId" in object_source
    assert "Object inventory parentBackupId does not match MySQL parentBackupId" in file_source
    assert "DCC manifest previousBackupId does not match MySQL parentBackupId" in file_source
    assert "dccPreviousManifestPath" not in file_source.split(
        "function Resolve-BackupOpsPreviousDccBackupManifestPath", 1
    )[1].split("function Get-BackupOpsDccMySqlRootPassword", 1)[0]


def test_production_confirmation_uses_only_the_protected_secret() -> None:
    source = (BACKUP_ROOT / "scripts" / "backup-ops.ps1").read_text(encoding="utf-8")
    confirmation = source.split("function Assert-BackupOpsProductionBackupConfirmation", 1)[1].split(
        "function Resolve-BackupOpsRepositoryEnvironment", 1
    )[0]

    assert "auth.productionBackupConfirmText is required" in confirmation
    assert "$resolvedConfirmText = $expectedConfirmText" in confirmation
    assert '$expectedConfirmText = "PROD-BACKUP-$productionHost"' not in confirmation


def test_rehearsal_does_not_swallow_reset_or_image_probe_failures() -> None:
    source = (BACKUP_ROOT / "scripts" / "modules" / "Infra" / "DockerOps.psm1").read_text(encoding="utf-8")
    image_probe = source.split("function Ensure-BackupOpsRehearsalImageAvailable", 1)[1].split(
        "function Initialize-BackupOpsRehearsalRuntime", 1
    )[0]
    restore = source.split("function Restore-BackupOpsRehearsalRuntime", 1)[1].split(
        "function Test-BackupOpsRehearsalValidation", 1
    )[0]

    assert "catch {" not in image_probe
    assert "MISSING" in image_probe
    assert "&& echo READY" not in image_probe
    assert "command -v docker" in image_probe
    assert "docker compose down -v --remove-orphans || true" not in restore


def test_binlog_export_is_database_scoped_and_parent_environment_bound() -> None:
    source = (BACKUP_ROOT / "scripts" / "modules" / "Infra" / "MySqlOps.psm1").read_text(encoding="utf-8")
    incremental = source.split("function Export-BackupOpsMySqlBinlogIncrement", 1)[1].split(
        "function Get-BackupOpsRemoteBackupManifest", 1
    )[0]
    parent = source.split("function Get-BackupOpsPreviousMySqlChainPoint", 1)[1].split(
        "function Write-BackupOpsMySqlEvidenceJson", 1
    )[0]

    assert "--database={" in incremental
    assert "Assert-BackupOpsExistingMySqlChainIntegrity" in incremental
    assert "sourceEnvironment" in parent
    assert "repositoryEnvironment" in parent


def test_rehearsal_restores_full_baseline_before_increment_chain() -> None:
    source = (BACKUP_ROOT / "scripts" / "modules" / "Infra" / "DockerOps.psm1").read_text(encoding="utf-8")
    candidate = source.split("function Get-BackupOpsRehearsalCandidate", 1)[1].split(
        "function Set-BackupOpsRehearsalVerificationState", 1
    )[0]
    restore = source.split("function Restore-BackupOpsRehearsalRuntime", 1)[1].split(
        "function Test-BackupOpsRehearsalValidation", 1
    )[0]

    assert "baseBackupId" in candidate
    assert "backupKind" in candidate
    assert "mysqlDumpPath" not in candidate.split("try {", 1)[0]
    import_index = restore.find("Import-BackupOpsMySqlDump")
    replay_index = restore.find("Replay-BackupOpsMySqlBinlogChain")
    assert 0 <= import_index < replay_index
    assert "-BackupId $candidate.baseBackupId" in restore
    for marker in ("repositoryEnvironment", "repositoryHost", "sourceEnvironment", "sourceHost"):
        assert marker in candidate


def test_binlog_replay_validates_complete_parent_chain() -> None:
    source = (BACKUP_ROOT / "scripts" / "modules" / "Infra" / "MySqlOps.psm1").read_text(encoding="utf-8")

    assert "function Replay-BackupOpsMySqlBinlogChain" in source
    replay = source.split("function Import-BackupOpsMySqlBinlogSegment", 1)[1].split(
        "function Export-BackupOpsMySqlDump", 1
    )[0]
    for marker in (
        "parentBackupId",
        "baseBackupId",
        "gzip -t",
        "sha256sum",
        "Import-BackupOpsMySqlBinlogSegment",
        "replayStatus",
    ):
        assert marker in replay
    assert "[ValidateSet('INTBK-3001', 'INTBK-3002', 'INTBK-3003', 'INTBK-7001')]" in source


def test_incremental_does_not_skip_a_broken_latest_parent() -> None:
    source = (BACKUP_ROOT / "scripts" / "modules" / "Infra" / "MySqlOps.psm1").read_text(encoding="utf-8")
    parent_lookup = source.split("function Get-BackupOpsPreviousMySqlChainPoint", 1)[1].split(
        "function Write-BackupOpsMySqlEvidenceJson", 1
    )[0]

    assert "foreach ($path" not in parent_lookup
    assert "latest successful backup point" in parent_lookup


def test_full_and_incremental_object_snapshots_have_explicit_baseline_semantics() -> None:
    object_source = (BACKUP_ROOT / "scripts" / "modules" / "Infra" / "ObjectOps.psm1").read_text(
        encoding="utf-8"
    )
    use_case_sources = [
        (BACKUP_ROOT / "scripts" / "modules" / "UseCases" / name).read_text(encoding="utf-8")
        for name in ("BackupNow.psm1", "BackupScheduled.psm1")
    ]

    assert "[ValidateSet('FULL', 'INCREMENTAL')]" in object_source
    assert "BackupKind = $BackupKind" in object_source
    assert "if ($backupKind -eq 'FULL') { @() }" in object_source
    for source in use_case_sources:
        assert "Backup-BackupOpsObjectBucket -Config $Config -Workspace $workspace -BackupKind $BackupKind" in source
    assert "$previousIdentity = if ($previous.PSObject.Properties['etag']" in object_source
    assert "$currentIdentity = if ($current.PSObject.Properties['etag']" in object_source
    assert "$previousIdentity -eq $currentIdentity" in object_source
    assert "$item.repositoryKey = [string]$hashByRepositoryKey[$repositoryKey]" in object_source
    assert 'mv "$incoming" "/object-store/$actual"' in object_source
    assert "$validationCommand" in object_source
    assert "':/object-store:ro'" in object_source


def test_full_dcc_manifest_does_not_reuse_a_previous_restore_point() -> None:
    source = (BACKUP_ROOT / "scripts" / "modules" / "Infra" / "FileOps.psm1").read_text(encoding="utf-8")
    dcc = source.split("function New-BackupOpsDccBackupManifest", 1)[1].split(
        "function New-BackupOpsChecksums", 1
    )[0]

    assert "[ValidateSet('FULL', 'INCREMENTAL')]" in dcc
    assert "if ($BackupKind -eq 'INCREMENTAL')" in dcc
    assert "DCC INCREMENTAL expected parent backup ID is invalid" in source


def test_rehearsal_writes_success_evidence_before_marking_recoverable() -> None:
    source = (BACKUP_ROOT / "scripts" / "modules" / "UseCases" / "Rehearsal.psm1").read_text(
        encoding="utf-8"
    )
    use_case = source.split("function Invoke-RehearsalUseCase", 1)[1]
    write_index = use_case.find("Write-BackupOpsRehearsalEvidence")
    state_index = use_case.find("Set-BackupOpsRehearsalVerificationState")

    assert 0 <= write_index < state_index


def test_manifest_separates_backup_source_from_repository_proof() -> None:
    source = (BACKUP_ROOT / "scripts" / "modules" / "Infra" / "FileOps.psm1").read_text(encoding="utf-8")
    manifest = source.split("function New-BackupOpsManifest", 1)[1].split(
        "function Import-BackupOpsDccManifestDependencies", 1
    )[0]

    for marker in ("sourceEnvironment", "sourceHost", "repositoryEnvironment", "repositoryHost"):
        assert marker in manifest


def test_remote_backup_point_is_published_by_atomic_rename() -> None:
    source = (BACKUP_ROOT / "scripts" / "modules" / "Infra" / "FileOps.psm1").read_text(encoding="utf-8")
    sync = source.split("function Sync-BackupOpsBackupToTestServer", 1)[1].split(
        "function Invoke-BackupOpsLocalRetention", 1
    )[0]

    assert ".creating" in sync
    assert "test ! -e" in sync
    assert "mv --" in sync
    manifest_upload = sync.find("Send-BackupFileOverSsh")
    promote = sync.rfind("mv --")
    assert 0 <= manifest_upload < promote


def test_rehearsal_prevalidates_all_mysql_and_object_payloads_before_runtime_reset() -> None:
    source = (BACKUP_ROOT / "scripts" / "modules" / "Infra" / "DockerOps.psm1").read_text(encoding="utf-8")
    restore = source.split("function Restore-BackupOpsRehearsalRuntime", 1)[1].split(
        "function Test-BackupOpsRehearsalValidation", 1
    )[0]

    dump_verify = restore.find("Test-BackupOpsMySqlDumpIntegrity")
    chain_verify = restore.find("Test-BackupOpsMySqlRestoreChainIntegrity")
    object_verify = restore.find("Test-BackupOpsObjectBucketIntegrity")
    reset = restore.find("docker compose down")
    assert 0 <= dump_verify < chain_verify < object_verify < reset
