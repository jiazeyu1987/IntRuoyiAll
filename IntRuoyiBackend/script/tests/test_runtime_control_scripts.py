from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
DEPLOY_DIR = REPO_ROOT / "deploy"


def read_script(name: str) -> str:
    return (DEPLOY_DIR / name).read_text(encoding="utf-8")


def test_local_status_script_has_json_contract_and_required_components():
    script = read_script("show-int-ruoyi-local-status.ps1")

    assert "param(" in script
    assert "[switch]$Json" in script
    assert "intruoyi-frontend" in script
    assert "intruoyi-backend" in script
    assert "website-frontend" in script
    assert "ConvertTo-Json" in script
    assert "$statusMap" not in script


def test_local_status_script_defaults_to_int_main_without_global_worktree_pair_sync():
    script = read_script("show-int-ruoyi-local-status.ps1")
    port_map = read_script("worktree-port-map.ps1")

    assert "$EffectiveWorktreeName = if ([string]::IsNullOrWhiteSpace($WorktreeName)) { 'int_main' } else { $WorktreeName }" in script
    assert "Get-IntRuoyiWorktreePortContext -WorktreeName $EffectiveWorktreeName" in script
    assert "function New-IntRuoyiMainPortContext" in port_map
    assert "if ($WorktreeName -eq 'int_main')" in port_map


def test_local_restart_script_is_component_scoped_and_fail_fast():
    script = read_script("restart-int-ruoyi-local.ps1")

    assert "[ValidateSet('frontend', 'backend', 'full', 'website')]" in script
    assert "D:\\ProjectPackage\\Website\\run-website.bat" in script
    assert "Fail 'Missing npm" in script
    assert "Fail 'Missing java" in script
    assert "Fail 'Missing mvn" in script
    assert "Fail 'Missing pnpm" in script
    assert "$FrontendPort = [int]$PortContext.FrontendPort" in script
    assert "$BackendPort = [int]$PortContext.BackendPort" in script
    assert '"--server.port=$BackendPort"' in script
    assert "$backendLogDir = Join-Path $RuntimeDir 'logs'" in script
    assert "$backendLogFile = Join-Path $backendLogDir 'yudao-server.log'" in script
    assert "New-Item -ItemType Directory -Force -Path $backendLogDir" in script
    assert '"--logging.file.name=$backendLogFile"' in script
    assert '"--yudao.runtime-control.storage-guard.log-dir=$backendLogDir"' in script
    assert "-pl yudao-server -am -DskipTests package" in script
    assert "backend-runtime-control-$timestamp.jar" in script
    assert "frontend-runtime-control-$timestamp.out.log" in script
    assert "frontend-runtime-control-$timestamp.err.log" in script
    assert "`$env:VITE_PORT = '$FrontendPort'" in script
    assert "`$env:VITE_BASE_URL = 'http://127.0.0.1:$BackendPort'" in script
    assert "$RuntimeControlStateDir = Join-Path $RepoRoot 'runtime\\runtime-control'" in script
    assert '$RuntimeControlStateDir = Join-Path $RuntimeDir' not in script
    assert '--yudao.runtime-control.repo-root=$RepoRoot' in script
    assert '--yudao.runtime-control.state-dir=$RuntimeControlStateDir' in script
    assert "-EncodedCommand" in script
    assert "--spring.datasource.dynamic.datasource.master.url=jdbc:mysql://${LocalDockerRuntimeHost}:23306/ruoyi-vue-pro" in script
    assert "--spring.data.redis.port=26379" in script
    assert "OperationRecordPath" in script
    assert "Get-NetTCPConnection -LocalPort $Port -State Listen" in script
    assert "Get-CimInstance Win32_Process" in script
    assert "Stop-MatchingProcesses 'frontend' $FrontendDir" in script
    assert "Stop-MatchingProcesses 'backend' $RuntimeDir" in script
    assert "Required Docker container is not running: $Name" in script
    assert "20260523_dcc_nas_transfer_task.sql" in script
    assert "dcc_controlled_file_nas_transfer_task" in script
    assert "SELECT COUNT(*) FROM information_schema.tables" in script
    assert "Ensure-RequiredLocalMySqlSchema" in script
    assert "Invoke-LocalSqlScript" in script
    assert "Wait-WebsiteReadbackReady" in script
    assert "ShowroomPublicReleaseOrigin" not in script
    assert "showroom.release.public-website-origin" not in script
    assert "$ShowroomWebsiteReadbackOrigin = 'http://127.0.0.1:4173'" in script
    assert "Invoke-WebRequest -UseBasicParsing -Uri $WebsiteCurrentUrl" in script
    assert "did not become ready within" in script
    assert "`$env:WEBSITE_RUNTIME_MODE = 'preview'" in script
    assert "`$env:DCC_SIGNATURE_EVIDENCE_HMAC_SECRET = '$DccSignatureEvidenceHmacSecret'" in script
    assert "`$env:DCC_SIGNATURE_EVIDENCE_KEY_VERSION = '$DccSignatureEvidenceKeyVersion'" in script


def test_local_restart_script_defaults_to_int_main_without_global_worktree_pair_sync():
    script = read_script("restart-int-ruoyi-local.ps1")

    assert "$EffectiveWorktreeName = if ([string]::IsNullOrWhiteSpace($WorktreeName)) { 'int_main' } else { $WorktreeName }" in script
    assert "Get-IntRuoyiWorktreePortContext -WorktreeName $EffectiveWorktreeName -CurrentBackendRepoRoot $InitialRepoRoot" in script


def test_local_restart_backend_uses_java_argument_array_instead_of_powershell_line_continuation():
    script = read_script("restart-int-ruoyi-local.ps1")

    assert "$backendArgs = @(" in script
    assert "& java @backendArgs" in script
    assert "java -jar '$runtimeJar' `" not in script
    assert "--spring.datasource.dynamic.datasource.master.url=jdbc:mysql://${LocalDockerRuntimeHost}:23306/ruoyi-vue-pro?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&nullCatalogMeansCurrent=true" in script


def test_local_restart_backend_routes_mysql_and_redis_through_unshadowed_docker_loopback():
    script = read_script("restart-int-ruoyi-local.ps1")

    assert "$LocalDockerRuntimeHost = '127.0.0.2'" in script
    assert "function Assert-LocalDockerRuntimePortRoute" in script
    assert "Assert-LocalDockerRuntimePortRoute -Name 'MySQL' -Port 23306" in script
    assert "Assert-LocalDockerRuntimePortRoute -Name 'Redis' -Port 26379" in script
    assert "--spring.datasource.dynamic.datasource.master.url=jdbc:mysql://${LocalDockerRuntimeHost}:23306/ruoyi-vue-pro" in script
    assert "--spring.datasource.dynamic.datasource.slave.url=jdbc:mysql://${LocalDockerRuntimeHost}:23306/ruoyi-vue-pro" in script
    assert "--spring.datasource.dynamic.datasource.slave.username=root" in script
    assert "--spring.datasource.dynamic.datasource.slave.password=123456" in script
    assert "--spring.data.redis.host=$LocalDockerRuntimeHost" in script
    assert "LOCAL_DOCKER_PORT_SHADOWED" in script


def test_local_restart_backend_defaults_to_tokenless_codex_runner_mode():
    script = read_script("restart-int-ruoyi-local.ps1")

    assert "$CodexTestRunnerTokenFile" not in script
    assert "function Initialize-CodexTestRunnerToken" not in script
    assert "[Security.Cryptography.RandomNumberGenerator]::Create()" not in script
    assert "Codex Runner token file is empty" not in script

    backend_block = script[script.index("function Start-Backend"):script.index("function Start-Website")]
    assert "$runnerToken = Initialize-CodexTestRunnerToken" not in backend_block
    assert "'CODEX_TEST_RUNNER_TOKEN'," not in backend_block
    assert "Remove-Item -Path 'Env:\\CODEX_TEST_RUNNER_TOKEN' -ErrorAction SilentlyContinue" in backend_block
    assert backend_block.index(
        "Remove-Item -Path 'Env:\\CODEX_TEST_RUNNER_TOKEN' -ErrorAction SilentlyContinue"
    ) < backend_block.index("& java @backendArgs")


def test_local_restart_backend_protects_showroom_default_file_config_from_e2e_mutation():
    script = read_script("restart-int-ruoyi-local.ps1")

    assert "function Assert-LocalShowroomFileConfigProtected" in script
    assert "$ProtectedShowroomFileConfigId = 28" in script
    assert "$ProtectedShowroomBucket = 'yudao'" in script
    assert "$ProtectedShowroomEndpoint = 'http://127.0.0.1:9000'" in script
    assert "$ProtectedShowroomDomain = 'http://127.0.0.1:9000/yudao'" in script
    assert "SHOWROOM_FILE_CONFIG_PROTECTED" in script
    assert "SHOWROOM_MEDIA_URL_PROTECTED" in script
    assert "JSON_UNQUOTE(JSON_EXTRACT(config, '$.bucket'))" in script
    assert "JSON_UNQUOTE(JSON_EXTRACT(config, '$.endpoint'))" in script
    assert "JSON_UNQUOTE(JSON_EXTRACT(config, '$.domain'))" in script
    assert "path LIKE 'showroom/%'" in script
    assert "url NOT LIKE '$ProtectedShowroomDomain/%'" in script
    assert "Assert-LocalShowroomFileConfigProtected" in script[script.index("Ensure-RequiredLocalMySqlSchema"):]


def test_local_restart_backend_blocks_showroom_media_bucket_inconsistency_before_start():
    script = read_script("restart-int-ruoyi-local.ps1")

    assert "function Assert-LocalShowroomMediaBucketConsistency" in script
    assert "function Assert-LocalMinioObjectExists" in script
    assert "$LocalMinioContainer = 'docker-minio-1'" in script
    assert "$ShowroomMediaSampleObjects = @(" in script
    assert "showroom/product/cover/20260530/product-product_001-cover.png" in script
    assert "showroom/narration/20260522/company-1-zh-ruoxi.wav" in script
    assert "SELECT config FROM infra_file_config WHERE master = 1 AND deleted = 0 LIMIT 1" in script
    assert "Showroom media bucket consistency check failed" in script
    assert "test -f '$containerPath/xl.meta'" in script

    backend_block = script[script.index("function Start-Backend"):script.index("function Start-Website")]
    assert "Assert-LocalShowroomFileConfigProtected" in backend_block
    assert "Assert-LocalShowroomMediaBucketConsistency" in backend_block
    assert backend_block.index("Assert-LocalShowroomFileConfigProtected") < backend_block.index("Assert-LocalShowroomMediaBucketConsistency")
    assert backend_block.index("Assert-LocalShowroomMediaBucketConsistency") < backend_block.index("Stop-MatchingProcesses 'backend' $RuntimeDir")


def test_remote_scripts_support_json_and_website_component():
    status_script = read_script("show-int-ruoyi-remote-status.ps1")
    restart_script = read_script("restart-int-ruoyi-remote.ps1")

    assert "[switch]$Json" in status_script
    assert "website" in status_script
    assert "WEBSITE_HOST_PORT" in status_script
    assert "ConvertTo-Json" in status_script
    assert "Require-Command 'ssh'" in status_script
    assert "ConnectTimeout=5" in status_script
    assert "missing-docker" in status_script
    assert "$backendProbe = Probe-HttpStatus" in status_script

    assert "[ValidateSet('backend', 'frontend', 'full', 'website')]" in restart_script
    assert "docker compose restart website" in restart_script
    assert "OperationRecordPath" in restart_script


def test_remote_restart_blocks_showroom_media_bucket_inconsistency_before_backend_restart():
    script = read_script("restart-int-ruoyi-remote.ps1")

    assert "function Assert-RemoteShowroomMediaBucketConsistency" in script
    assert "$RemoteMysqlContainer = 'intruoyi-mysql'" in script
    assert "[string]$RemoteMinioContainer = ''" in script
    assert "Missing -RemoteMinioContainer; remote backend restart requires an explicit MinIO container." in script
    assert "$ShowroomMediaSampleObjects = @(" in script
    assert "showroom/product/cover/20260530/product-product_001-cover.png" in script
    assert "showroom/narration/20260522/company-1-zh-ruoxi.wav" in script
    assert "SELECT JSON_UNQUOTE(JSON_EXTRACT(config, CAST(0x242e6275636b6574 AS CHAR CHARACTER SET utf8mb4))) FROM infra_file_config WHERE master = 1 AND deleted = 0 LIMIT 1" in script
    assert "Showroom media bucket consistency check failed" in script
    assert "test -f '/data/`$bucket/`$object/xl.meta'" in script

    guard_condition = "if ($Component -eq 'backend' -or $Component -eq 'full')"
    guard_block_index = script.index(guard_condition, script.index("Info \"Restarting remote runtime"))
    restart_index = script.index("docker compose restart $serviceNames")
    assert script.index("Missing -RemoteMinioContainer", guard_block_index) < restart_index
    assert script.index("Assert-RemoteShowroomMediaBucketConsistency", guard_block_index) < restart_index


def test_remote_restart_quotes_mysql_bucket_sql_for_ssh_shell():
    script = read_script("restart-int-ruoyi-remote.ps1")

    assert "mysql -u$RemoteMysqlUser -N -B $RemoteMysqlDatabase -e 'SELECT JSON_UNQUOTE" in script
    assert "JSON_EXTRACT(config, CAST(0x242e6275636b6574 AS CHAR CHARACTER SET utf8mb4))" in script
    assert "-e \"SELECT COALESCE(JSON_UNQUOTE(JSON_EXTRACT(config," not in script


def test_remote_restart_bucket_sql_avoids_collation_sensitive_coalesce():
    script = read_script("restart-int-ruoyi-remote.ps1")

    assert "COALESCE(JSON_UNQUOTE(JSON_EXTRACT(config" not in script
    assert "SUBSTRING(CAST(0x20 AS CHAR), 1, 0)" not in script
    assert 'if [ "`$bucket" = "NULL" ]; then' in script
    assert "  bucket=''" in script


def test_remote_status_script_exposes_current_release_package_from_runtime_env():
    status_script = read_script("show-int-ruoyi-remote-status.ps1")

    assert "function Read-RemoteImageTag" in status_script
    assert "IMAGE_TAG" in status_script
    assert "$currentReleaseTag = Read-RemoteImageTag" in status_script
    assert "$currentReleaseTag = if ($Component -eq 'full')" not in status_script
    assert "currentReleaseTag = $currentReleaseTag" in status_script


def test_publish_script_supports_backup_server_with_production_grade_confirmation():
    publish_script = read_script("publish-int-ruoyi.ps1")

    assert "[ValidateSet('test', 'prod', 'backup')]" in publish_script
    assert "[string]$BackupServerHost = $env:RUNTIME_CONTROL_BACKUP_SERVER_HOST" in publish_script
    assert "ServerHost = $BackupServerHost" in publish_script
    assert "DisplayName = 'backup'" in publish_script
    assert "@('prod', 'backup') -contains $Environment" in publish_script
    assert "Explicit confirmation required for production-grade publish" in publish_script
    assert "backup-latest.json" in publish_script
    assert "backup-deployments" in publish_script
