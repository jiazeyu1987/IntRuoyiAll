import base64
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import textwrap


DEPLOY_ROOT = Path(__file__).resolve().parents[1] / "deploy"
REPO_ROOT = Path(__file__).resolve().parents[2]
WORKSPACE_ROOT = REPO_ROOT.parent
PUBLISH_SCRIPT = DEPLOY_ROOT / "publish-int-ruoyi.ps1"


def _extract_nginx_block(text: str, marker: str) -> str:
    start = text.index(marker)
    open_brace = text.index("{", start)
    depth = 0
    for index in range(open_brace, len(text)):
        char = text[index]
        if char == "{":
            depth += 1
        elif char == "}":
            depth -= 1
            if depth == 0:
                return text[open_brace : index + 1]
    raise AssertionError(f"Unable to find complete nginx block for {marker}")


def read_publish_script() -> str:
    return PUBLISH_SCRIPT.read_text(encoding="utf-8")


def _extract_function_body(text: str, function_name: str, next_function_name: str) -> str:
    return text.split(f"function {function_name}", 1)[1].split(f"function {next_function_name}", 1)[0]


def _extract_powershell_function(text: str, function_name: str) -> str:
    start = text.index(f"function {function_name}")
    open_brace = text.index("{", start)
    depth = 0
    for index in range(open_brace, len(text)):
        char = text[index]
        if char == "{":
            depth += 1
        elif char == "}":
            depth -= 1
            if depth == 0:
                return text[start : index + 1]
    raise AssertionError(f"Unable to find complete PowerShell function {function_name}")


def _invoke_release_package_directory_name(release_tag: str) -> subprocess.CompletedProcess[str]:
    function_text = _extract_powershell_function(read_publish_script(), "ConvertTo-ReleasePackageDirectoryName")
    command = textwrap.dedent(
        f"""
        $ErrorActionPreference = 'Stop'
        [Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
        function Fail([string]$Message) {{
            throw $Message
        }}
        {function_text}
        try {{
            $releaseTag = @'
{release_tag}
'@
            $result = ConvertTo-ReleasePackageDirectoryName -ReleaseTagValue $releaseTag
            Write-Output $result
        }} catch {{
            Write-Output $_.Exception.Message
            exit 1
        }}
        """
    )
    encoded = base64.b64encode(command.encode("utf-16le")).decode("ascii")
    return subprocess.run(
        ["powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-EncodedCommand", encoded],
        cwd=REPO_ROOT,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        check=False,
    )


def _invoke_codex_summary_validator(
    response: object,
    facts: list[dict[str, str]],
) -> subprocess.CompletedProcess[str]:
    function_text = _extract_powershell_function(
        read_publish_script(),
        "ConvertTo-ValidatedReleaseCodexSummaryItems",
    )
    response_json = json.dumps(response, ensure_ascii=False)
    facts_json = json.dumps(facts, ensure_ascii=False)
    command = textwrap.dedent(
        f"""
        $ErrorActionPreference = 'Stop'
        [Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
        function Fail([string]$Message) {{
            throw $Message
        }}
        {function_text}
        try {{
            $response = @'
{response_json}
'@ | ConvertFrom-Json
            $facts = @'
{facts_json}
'@ | ConvertFrom-Json
            $items = @(ConvertTo-ValidatedReleaseCodexSummaryItems -Response $response -Facts @($facts) -MaxItems 10)
            $items | ConvertTo-Json -Compress
        }} catch {{
            Write-Output $_.Exception.Message
            exit 1
        }}
        """
    )
    encoded = base64.b64encode(command.encode("utf-16le")).decode("ascii")
    return subprocess.run(
        ["powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-EncodedCommand", encoded],
        cwd=REPO_ROOT,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        check=False,
    )


def test_only_one_publish_script_entrypoint_remains() -> None:
    publish_like = sorted(
        path.name
        for path in DEPLOY_ROOT.iterdir()
        if path.is_file() and ("publish" in path.name.lower() or "promote" in path.name.lower())
    )

    assert publish_like == ["publish-int-ruoyi.ps1"]
    assert not (WORKSPACE_ROOT / "publish-int-ruoyi-to-test.bat").exists()


def test_unified_publish_script_targets_test_and_production_with_explicit_confirmation() -> None:
    text = read_publish_script()

    assert "[ValidateSet('direct', 'build-release', 'deploy-release', 'mark-tested')]" in text
    assert "[string]$Mode = 'direct'" in text
    assert "[string]$ReleaseTag = ''" in text
    assert "[string]$NasConfigPath = ''" in text
    assert "[string]$NasReleaseRoot = 'Backup/ReleasePackage'" in text
    assert "[string]$NasServer = '172.30.30.4'" in text
    assert "[string]$NasShare = ''" in text
    assert "[switch]$RequireTested" in text
    assert "[string]$SelectedRecoverySetCandidateId = ''" in text
    assert "[string]$RecoverySetManifestHash = ''" in text
    assert "[ValidateSet('test', 'prod', 'backup')]" in text
    assert "[string]$Environment = 'test'" in text
    assert "[string]$ConfirmText = ''" in text
    assert "Resolve-PublishTarget" in text
    assert "@('prod', 'backup') -contains $Environment" in text
    assert "Explicit confirmation required for production-grade publish" in text
    assert "Current local workspace" not in text
    assert "promote-int-ruoyi-test-to-prod" not in text
    assert "FRONTEND-ONLY PROD" not in text


def test_publish_script_uses_configured_target_hosts_instead_of_hardcoded_environment_ips() -> None:
    text = read_publish_script()

    assert "[string]$TestServerHost = $env:RUNTIME_CONTROL_TEST_SERVER_HOST" in text
    assert "[string]$ProdServerHost = $env:RUNTIME_CONTROL_PROD_SERVER_HOST" in text
    assert "[string]$BackupServerHost = $env:RUNTIME_CONTROL_BACKUP_SERVER_HOST" in text
    assert "-TestServerHost" in text
    assert "-ProdServerHost" in text
    assert "-BackupServerHost" in text
    assert "Missing -ServerHost" in text
    assert "Missing $ArgumentName" in text
    assert "-ArgumentName '-TestServerHost'" in text
    assert "-ArgumentName '-ProdServerHost'" in text
    assert "-ArgumentName '-BackupServerHost'" in text
    assert not re.search(r"172\.30\.30\.(57|58|59)", text)


def test_release_change_set_uses_codex_plain_language_summary_from_previous_git_diff() -> None:
    text = read_publish_script()

    assert "function Get-PreviousReleaseManifestForGitChanges" in text
    assert "function New-ReleaseGitChangeItems" in text
    assert "function Get-ReleaseGitChangeFacts" in text
    assert "function Invoke-ReleaseCodexSummary" in text
    assert re.search(r"& git -C \$repoPath log", text)
    assert "--numstat" in text
    assert "$previousCommit..$currentCommit" in text
    assert "summaryGenerator = 'codex'" in text
    assert "--output-schema" in text
    assert "--output-last-message" in text
    assert "ConvertFrom-Json" in text
    assert "plain-language" in text
    assert r"[\u4e00-\u9fff]" in text
    assert "previousReleaseTag" in text
    assert "gitChanges = @($gitChangeSummary.items)" in text
    assert "items = @($gitChangeSummary.items)" in text
    assert "changes = @($gitChangeSummary.items)" in text
    assert "[{0}] {1} {2}" not in text
    assert "--pretty=format:%cI%x09%h%x09%s" not in text


def test_release_change_set_fails_fast_without_codex_or_valid_plain_language_output() -> None:
    text = read_publish_script()

    assert "Codex CLI is required to generate release change summary" in text
    assert "Codex CLI failed" in text
    assert "Codex CLI timed out after $TimeoutSeconds seconds" in text
    assert "Codex summary output must be valid JSON" in text
    assert "Codex summary must contain between 1 and 10 items" in text
    assert "Codex summary item must be plain Chinese" in text
    assert "Codex summary must not expose raw commit identifiers" in text
    assert "Do not fall back to raw Git subjects or hashes" in text


def test_codex_summary_validator_accepts_plain_chinese_items_and_caps_at_ten() -> None:
    result = _invoke_codex_summary_validator(
        {"items": ["新增批次执行页面的填写人配置", "修复审批提交后状态显示不正确"]},
        [{"subject": "feat: add filler configuration"}, {"subject": "fix: approval status"}],
    )

    assert result.returncode == 0, result.stdout + result.stderr
    assert "新增批次执行页面的填写人配置" in result.stdout


def test_codex_summary_validator_rejects_raw_hashes_and_non_chinese_items() -> None:
    hash_result = _invoke_codex_summary_validator(
        {"items": ["修复功能 abcdef1234567"]},
        [{"subject": "fix: release summary"}],
    )
    english_result = _invoke_codex_summary_validator(
        {"items": ["Fix the release summary"]},
        [{"subject": "fix: release summary"}],
    )

    assert hash_result.returncode != 0
    assert "raw commit identifiers" in hash_result.stdout
    assert english_result.returncode != 0
    assert "plain Chinese" in english_result.stdout


def test_codex_summary_validator_rejects_more_than_ten_items() -> None:
    result = _invoke_codex_summary_validator(
        {"items": [f"第{i}项版本变化说明" for i in range(11)]},
        [{"subject": "feat: many changes"}],
    )

    assert result.returncode != 0
    assert "between 1 and 10 items" in result.stdout


def test_release_info_json_is_written_before_frontend_docker_context() -> None:
    text = read_publish_script()

    assert "function Write-FrontendReleaseInfo" in text
    assert "'release-info.json'" in text
    assert "[System.IO.File]::WriteAllText($releaseInfoPath" in text
    assert text.index("Write-FrontendReleaseInfo -PackageTag $ReleaseTag") < text.index("Info 'Preparing Docker build context from current worktree artifacts'")


def test_smart_release_report_only_switch_and_env_are_explicit_without_changing_mode_enum() -> None:
    text = read_publish_script()
    param_block = text[text.index("param(") : text.index("$ErrorActionPreference")]

    assert "[ValidateSet('direct', 'build-release', 'deploy-release', 'mark-tested')]" in param_block
    assert "[switch]$EnableSmartReleaseReport" in param_block
    assert param_block.count("[switch]$EnableSmartReleaseReport") == 1
    assert "[string]$SmartReleaseBaselineManifestPath = $env:INTRUOYI_SMART_RELEASE_BASELINE_MANIFEST_PATH" in param_block
    assert "[string]$SmartReleaseLocalDatabaseConfigPath = $env:INTRUOYI_SMART_RELEASE_LOCAL_DATABASE_CONFIG_PATH" in param_block
    assert "[string]$SmartReleaseDataOwnershipRegistryPath = $env:INTRUOYI_SMART_RELEASE_DATA_OWNERSHIP_REGISTRY_PATH" in param_block
    assert "[string]$SmartReleaseTargetConfigPath = $env:INTRUOYI_SMART_RELEASE_TARGET_CONFIG_PATH" in param_block
    assert "INTRUOYI_SMART_RELEASE_REPORT_ONLY" in text
    assert "Invalid INTRUOYI_SMART_RELEASE_REPORT_ONLY" in text


def test_build_release_smart_report_runs_validation_and_intake_after_manifest_before_nas_upload() -> None:
    text = read_publish_script()
    build_report_body = _extract_powershell_function(text, "Invoke-SmartReleaseBuildReportOnly")
    write_manifest_index = text.index("Write-ReleaseManifest -PackageTag $ReleaseTag")
    smart_report_index = text.index("Invoke-SmartReleaseBuildReportOnly -PackagePath $releaseDir")
    nas_upload_index = text.index("Copy-ReleasePackageToNas -PackageTag $ReleaseTag")

    assert write_manifest_index < smart_report_index < nas_upload_index
    assert "validate-release-manifest.ps1" in build_report_body
    assert "run-release-intake.ps1" in build_report_body
    assert "smart-release-report" in text
    assert "manifest-validation-result.json" in build_report_body
    assert "intake" in build_report_body
    assert "-PackagePath', $PackagePath" in build_report_body
    assert "-Mode', 'report-only'" in build_report_body
    assert "-OutputPath', $manifestValidationOutputPath" in build_report_body
    assert "-OutputDir', $intakeOutputDir" in build_report_body


def test_build_release_writes_frontend_release_info_before_docker_context() -> None:
    text = read_publish_script()

    assert "function Write-FrontendReleaseInfo" in text
    release_info_body = _extract_powershell_function(text, "Write-FrontendReleaseInfo")
    assert "release-info.json" in release_info_body
    assert "New-ReleaseSourceRepoManifestEntries" in release_info_body
    assert "releaseTag = $PackageTag" in release_info_body
    assert "publishScope = if ($SkipDatabaseSync -and $SkipMinioSync) { 'code-only' } else { 'with-data' }" in release_info_body
    assert "includeOnlyOffice = [bool]$IncludeOnlyOffice" in release_info_body
    assert "[System.IO.File]::WriteAllText($releaseInfoPath, $releaseInfoJson, [System.Text.UTF8Encoding]::new($false))" in release_info_body

    write_release_info_index = text.index("Write-FrontendReleaseInfo -PackageTag $ReleaseTag")
    docker_context_index = text.index("New-ReleaseDockerBuildContext `")
    frontend_build_index = text.index("Invoke-FrontendViteBuild -FrontendDir $frontendDir")
    assert frontend_build_index < write_release_info_index < docker_context_index


def test_smart_release_report_only_keeps_legacy_publish_flow_unhooked_when_disabled() -> None:
    text = read_publish_script()

    assert "$smartReleaseReportOnlyEnabled = Resolve-SmartReleaseReportOnlyEnabled" in text
    assert "if ($smartReleaseReportOnlyEnabled -and $Mode -eq 'build-release') {" in text
    assert "if ($smartReleaseReportOnlyEnabled -and $Mode -eq 'deploy-release') {" in text
    assert "Invoke-SmartReleaseBuildReportOnly -PackagePath $releaseDir" in text
    assert "Invoke-SmartReleaseDeployPrecheckReportOnly -PackagePath $releaseDir" in text


def test_deploy_release_smart_report_precheck_stops_before_ssh_scp_or_remote_commands() -> None:
    text = read_publish_script()
    deploy_report_body = _extract_powershell_function(text, "Invoke-SmartReleaseDeployPrecheckReportOnly")
    deploy_start = text.index("Invoke-SmartReleaseDeployPrecheckReportOnly -PackagePath $releaseDir")
    require_ssh_index = text.index("Require-Command 'ssh'")
    check_ssh_index = text.index('Info "Checking SSH access to $ServerHost"')

    assert deploy_start < require_ssh_index < check_ssh_index
    assert "run-deploy-precheck-report.ps1" in deploy_report_body
    assert "Smart Release deploy report-only completed; real deploy was not executed." in deploy_report_body
    assert "exit 2" in deploy_report_body
    assert "ssh" not in deploy_report_body.lower()
    assert "scp" not in deploy_report_body.lower()
    assert "docker pull" not in deploy_report_body.lower()
    assert "apt-get" not in deploy_report_body.lower()


def test_smart_release_deploy_precheck_uses_stable_package_report_path_and_target_config() -> None:
    text = read_publish_script()
    deploy_report_body = _extract_powershell_function(text, "Invoke-SmartReleaseDeployPrecheckReportOnly")

    assert "Missing $ArgumentName; Smart Release report-only requires explicit local input files." in text
    assert "smart-release-report" in text
    assert "deploy-precheck-result.json" in deploy_report_body
    assert "-PackagePath', $PackagePath" in deploy_report_body
    assert "-Environment', $Environment" in deploy_report_body
    assert "-TargetConfigPath', $targetConfigPath" in deploy_report_body
    assert "-Mode', 'report-only'" in deploy_report_body
    assert "-OutputPath', $deployPrecheckOutputPath" in deploy_report_body


def test_smart_release_report_only_does_not_add_docker_pull_or_apt_get_fallback() -> None:
    text = read_publish_script()
    smart_build_body = _extract_powershell_function(text, "Invoke-SmartReleaseBuildReportOnly")
    smart_deploy_body = _extract_powershell_function(text, "Invoke-SmartReleaseDeployPrecheckReportOnly")
    smart_report_body = smart_build_body + "\n" + smart_deploy_body

    assert "docker pull" not in smart_report_body.lower()
    assert "apt-get" not in smart_report_body.lower()
    assert "172.30.30.57" not in smart_report_body
    assert "172.30.30.58" not in smart_report_body
    assert "172.30.30.59" not in smart_report_body


def test_publish_script_stores_release_packages_in_nas_without_logging_password() -> None:
    text = read_publish_script()

    assert "function Read-NasReleaseConfig" in text
    assert "function Copy-ReleasePackageToNas" in text
    assert "function Copy-ReleasePackageFromNas" in text
    assert "function Mark-NasReleaseTested" in text
    assert "function Assert-NasReleaseTested" in text
    assert "ConvertFrom-Json" in text
    assert "NasConfigPath" in text
    assert "password" in text
    assert "Write-Host $nasConfig.password" not in text
    assert "function Connect-NasReleaseShare" in text
    assert "function Disconnect-NasReleaseShare" in text
    assert "New-PSDrive" not in text
    assert "Remove-PSDrive -Name $MountInfo.Name -Scope Script" not in text
    assert "net use " in text
    assert "DisplayCommand" in text


def test_publish_script_disconnects_nas_mapping_idempotently_for_missing_mapping_only() -> None:
    text = read_publish_script()
    disconnect_body = text.split("function Disconnect-NasReleaseShare", 1)[1].split(
        "function Invoke-NasReleaseShareDisconnect", 1
    )[0]
    guarded_body = text.split("function Invoke-NasReleaseShareDisconnect", 1)[1].split(
        "function Get-NasReleasePackagePath", 1
    )[0]

    assert "Invoke-NasReleaseShareDisconnect -Root" in disconnect_body
    assert "Invoke-CheckedShell -Command $disconnectCommand" not in disconnect_body
    assert "Invoke-ProcessCapture -FilePath 'net.exe' -ArgumentList @('use', $Root, '/delete', '/y')" in guarded_body
    assert "$result.ExitCode -eq 2 -and $cleanOutput -match 'NET HELPMSG 2250'" in guarded_body
    assert "NAS release repository mapping already absent; cleanup completed" in guarded_body
    assert "Fail \"Shell command failed with exit code $($result.ExitCode): $displayCommand`n$cleanOutput\"" in guarded_body


def test_release_package_mode_builds_once_and_deploys_without_rebuild() -> None:
    text = read_publish_script()

    assert "$Mode -eq 'build-release'" in text
    assert "$Mode -eq 'deploy-release'" in text
    assert "$Mode -eq 'mark-tested'" in text
    assert "Copy-ReleasePackageToNas" in text
    assert "Copy-ReleasePackageFromNas" in text
    assert "Release package built" in text
    assert "Deploying release package" in text
    assert "Release package marked as tested" in text
    assert "VITE_BASE_URL = ''" in text
    assert "release-manifest.json" in text
    assert "tested.json" in text
    assert "Write-NasReleaseDeploymentHistory" in text
    assert "prod-latest.json" in text
    assert "prod-deployments" in text
    assert "backup-latest.json" in text
    assert "backup-deployments" in text
    assert "Exporting release images" in text
    assert "'save'," in text
    assert "docker load -i '$remoteImageTar'" in text


def test_build_release_copy_to_nas_does_not_require_test_conclusion() -> None:
    text = read_publish_script()
    copy_body = text.split("function Copy-ReleasePackageToNas", 1)[1].split("function Copy-ReleasePackageFromNas", 1)[0]
    mark_tested_body = text.split("function Mark-NasReleaseTested", 1)[1].split("function Assert-NasReleaseTested", 1)[0]

    assert "TestConclusion is required when marking a release package as tested." not in copy_body
    assert "TestConclusion is required when marking a release package as tested." in mark_tested_body
    assert "SelectedRecoverySetCandidateId is required when marking a release package as tested." in mark_tested_body
    assert "RecoverySetManifestHash is required when marking a release package as tested." in mark_tested_body
    assert "recoverySet = [ordered]@{" in mark_tested_body


def test_mark_tested_generates_formal_rollback_compatibility_evidence() -> None:
    text = read_publish_script()
    mark_tested_body = text.split("function Mark-NasReleaseTested", 1)[1].split("function Assert-NasReleaseTested", 1)[0]
    evidence_body = text.split("function New-RollbackCompatibilityEvidence", 1)[1].split(
        "function Write-NasRollbackCompatibilityEvidence", 1
    )[0]

    assert "RecoverySetProgramVersion is required when marking rollback compatibility evidence." in mark_tested_body
    assert "RecoverySetRedisPolicy is required when marking rollback compatibility evidence." in mark_tested_body
    assert "Write-NasRollbackCompatibilityEvidence" in mark_tested_body
    assert "rollback-compatibility.json" in mark_tested_body
    assert "status = 'COMPATIBLE'" in evidence_body
    assert "packageDirectoryName = $PackageDirectoryName" in evidence_body
    assert "checkedAt = $CheckedAt" in evidence_body
    assert "summary = \"release package $PackageDirectoryName tested with recovery set $RecoverySetId; app-only rollback compatibility confirmed\"" in evidence_body
    assert "recoverySetManifestHash = $RecoverySetManifestHash.ToLowerInvariant()" in evidence_body


def test_mark_tested_blocks_rollback_compatibility_without_code_only_matching_recovery_set() -> None:
    text = read_publish_script()
    evidence_body = text.split("function New-RollbackCompatibilityEvidence", 1)[1].split(
        "function Write-NasRollbackCompatibilityEvidence", 1
    )[0]

    assert "$Manifest.publishScope -ne 'code-only'" in evidence_body
    assert "$RecoverySetProgramVersion -ne $PackageDirectoryName" in evidence_body
    assert "status = 'BLOCKED'" in evidence_body
    assert "app-only rollback blocked:" in evidence_body


def test_publish_script_uses_path_safe_directory_for_human_release_tag() -> None:
    text = read_publish_script()

    assert "function ConvertTo-ReleasePackageDirectoryName" in text
    assert '.Replace(\' \', \'_\').Replace(\':\', \'-\')' in text
    assert "'^[A-Za-z0-9][A-Za-z0-9_.-]{0,127}$'" in text
    assert "$expectedPackageDirectoryName = ConvertTo-ReleasePackageDirectoryName -ReleaseTagValue $ReleaseTag" in text
    assert "$Tag = $expectedPackageDirectoryName" in text
    assert "$packageDirectoryName = $Tag" in text
    assert "$releaseDir = Join-Path $localTempRoot $packageDirectoryName" in text
    assert "$imageTar = Join-Path $releaseDir \"intruoyi-images_$packageDirectoryName.tar\"" in text
    assert "'-t', \"intruoyi-backend:$packageDirectoryName\"" in text
    assert "'-t', \"intruoyi-frontend:$packageDirectoryName\"" in text
    assert "IMAGE_TAG=$packageDirectoryName" in text
    assert "packageDirectoryName = $packageDirectoryName" in text
    assert "Get-NasReleasePackagePath -NasRoot $mountInfo.Root -PackageTag $PackageTag" in text
    assert "$packageDirectoryName = ConvertTo-ReleasePackageDirectoryName -ReleaseTagValue $PackageTag" in text
    assert "NAS release path: $NasReleaseRoot/$packageDirectoryName" in text


def test_publish_script_encodes_chinese_release_tag_to_ascii_directory_name() -> None:
    release_tag = "26-06-04 21:22:26 QMS已导入"
    expected_hash = hashlib.sha256(release_tag.encode("utf-8")).hexdigest()[:12]
    expected_directory_name = f"26-06-04_21-22-26_QMS-u{expected_hash}"

    completed = _invoke_release_package_directory_name(release_tag)

    assert completed.returncode == 0, completed.stdout + completed.stderr
    assert completed.stdout.strip() == expected_directory_name


def test_publish_script_blocks_corrupted_release_tag_encoding() -> None:
    completed = _invoke_release_package_directory_name("26-06-04 21:22:26 QMS�ѵ���")

    assert completed.returncode != 0
    output = completed.stdout + completed.stderr
    assert "ReleaseTag contains Unicode replacement characters" in output
    assert "command invocation encoding is corrupted" in output


def test_publish_script_writes_local_release_packages_to_cachedata_by_default() -> None:
    text = read_publish_script()
    param_block = text[text.index("param(") : text.index("$ErrorActionPreference")]

    assert "[string]$LocalCacheRoot = $env:INTRUOYI_LOCAL_CACHE_ROOT" in param_block
    assert "$defaultLocalCacheRoot = 'E:\\Int\\CacheData\\IntRuoyi'" in text
    assert "function Resolve-LocalCacheRoot" in text
    assert "$resolvedLocalCacheRoot = Resolve-LocalCacheRoot $LocalCacheRoot" in text
    assert "$localTempRoot = Join-Path $resolvedLocalCacheRoot 'publish-int-ruoyi'" in text
    assert "Join-Path $backendRepo 'tmp\\publish-int-ruoyi'" not in text


def test_publish_script_defaults_showroom_repo_to_project_website_baseline() -> None:
    text = read_publish_script()

    assert "$defaultWebsiteRepo = 'D:\\ProjectPackage\\Website'" in text
    assert "Join-Path $workspaceRoot '..\\..\\Website'" not in text
    assert "$websiteRepo = (Resolve-Path $WebsiteRepo).Path" in text


def test_publish_script_reads_runtime_credentials_instead_of_hardcoding() -> None:
    text = read_publish_script()

    assert "Get-LocalContainerEnvValue" in text
    assert "Get-RemoteContainerEnvValue" in text
    assert "MYSQL_ROOT_PASSWORD=123456" not in text
    assert "infini_rag_flow" not in text
    assert "rag_flow" not in text
    assert "DisplayCommand" in text
    assert "Invoke-CheckedShell -Command $minioSyncCommand -DisplayCommand" in text


def test_publish_script_uses_target_remote_minio_profile_without_global_ragflow_default() -> None:
    text = read_publish_script()
    param_block = text[text.index("param(") : text.index("$ErrorActionPreference")]
    prod_target = text[text.index("'prod' {") : text.index("'backup' {")]
    backup_target = text[text.index("'backup' {") : text.index("default {", text.index("'backup' {"))]

    assert "[string]$RemoteMinioContainer = ''" in param_block
    assert "RemoteMinioContainer = 'ragflow_compose-minio-1'" in prod_target
    assert "RemoteMinioContainer = 'intruoyi-minio'" in backup_target
    assert "$RemoteMinioContainer = $publishTarget.RemoteMinioContainer" in text
    assert "RemoteMinioContainer = 'ragflow_compose-minio-1'" not in backup_target


def test_deploy_release_uses_manifest_scope_and_fails_fast_for_incomplete_with_data_packages() -> None:
    text = read_publish_script()
    deploy_release_info = text.index('Info "Deploying release package: $ReleaseTag"')
    deploy_release_start = text.rindex("if ($Mode -eq 'deploy-release') {", 0, deploy_release_info)
    deploy_release_block = text[deploy_release_start : text.index("Set-PublishRuntimeDefaultsForTarget", deploy_release_start)]

    assert "function Read-ReleaseManifest" in text
    assert "function Resolve-DeployReleasePackageScope" in text
    assert "release-manifest.json missing publishScope" in text
    assert "Invalid release package publishScope" in text
    assert "with-data release package is missing MySQL dump" in text
    assert "with-data release package is missing MinIO snapshot" in text
    assert "$releasePublishScope = Resolve-DeployReleasePackageScope" in deploy_release_block
    assert "if ($releasePublishScope -eq 'code-only')" in deploy_release_block
    assert "elseif ($releasePublishScope -eq 'with-data')" in deploy_release_block


def test_with_data_release_package_requires_dcc_object_inventory() -> None:
    text = read_publish_script()
    deploy_artifact_body = _extract_powershell_function(text, "Assert-DeployReleasePackageArtifactsForScope")
    local_mysql_body = _extract_powershell_function(text, "Invoke-LocalMySqlRaw")

    assert "function Write-DccObjectInventoryForReleasePackage" in text
    assert "function Get-DccReleaseObjectReferenceRows" in text
    assert "function Invoke-ProcessCaptureWithInput" in text
    assert "Invoke-ProcessCaptureWithInput" in local_mysql_body
    assert "'-i'," in local_mysql_body
    assert 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql' in local_mysql_body
    assert "'-e'," not in local_mysql_body
    assert "manifest\\dcc-object-inventory.json" in text
    assert "DCC_OBJECT_SNAPSHOT_MISSING" in text
    assert "DCC_OBJECT_INVENTORY_FILE_MISSING" in deploy_artifact_body
    assert "DCC_OBJECT_INVENTORY_HASH_MISMATCH" in deploy_artifact_body
    assert "DCC object inventory created" in text
    build_minio_index = text.index("if ($publishBackend -and $Mode -eq 'build-release' -and -not $SkipMinioSync)")
    runtime_env_index = text.index("Write-ReleaseRuntimeEnvPackage", build_minio_index)
    assert "Write-DccObjectInventoryForReleasePackage" in text[
        build_minio_index:runtime_env_index
    ]
    assert "with-data release package is missing DCC object inventory" in deploy_artifact_body


def test_release_package_manifest_controls_onlyoffice_inclusion_without_fallback() -> None:
    text = read_publish_script()
    param_block = text[text.index("param(") : text.index("$ErrorActionPreference")]
    deploy_release_info = text.index('Info "Deploying release package: $ReleaseTag"')
    deploy_release_start = text.rindex("if ($Mode -eq 'deploy-release') {", 0, deploy_release_info)
    deploy_release_block = text[deploy_release_start : text.index("Set-PublishRuntimeDefaultsForTarget", deploy_release_start)]

    assert "[switch]$IncludeOnlyOffice" in param_block
    assert "function Resolve-DeployReleasePackageOnlyOfficeIncluded" in text
    assert "release-manifest.json missing onlyOfficeIncluded; rebuild the release package" in text
    assert "Invalid release package onlyOfficeIncluded" in text
    assert "onlyOfficeIncluded = [bool]$IncludeOnlyOffice" in text
    assert "$releaseOnlyOfficeIncluded = Resolve-DeployReleasePackageOnlyOfficeIncluded" in deploy_release_block
    assert "$IncludeOnlyOffice = $releaseOnlyOfficeIncluded" in deploy_release_block


def test_release_package_manifest_controls_component_scope_without_fallback() -> None:
    text = read_publish_script()
    param_block = text[text.index("param(") : text.index("$ErrorActionPreference")]
    deploy_release_info = text.index('Info "Deploying release package: $ReleaseTag"')
    deploy_release_start = text.rindex("if ($Mode -eq 'deploy-release') {", 0, deploy_release_info)
    deploy_release_block = text[deploy_release_start : text.index("Set-PublishRuntimeDefaultsForTarget", deploy_release_start)]

    assert "[ValidateSet('full', 'intruoyi', 'backend', 'frontend', 'website')]" in param_block
    assert "$componentExplicit = $PSBoundParameters.ContainsKey('Component')" in text
    assert "function Resolve-DeployReleasePackageComponent" in text
    assert "release-manifest.json missing component; rebuild the release package or pass -Component explicitly" in text
    assert "Invalid release package component" in text
    assert "component = $Component" in text
    assert "includeShowroomBuildPackage = [bool]$publishWebsite" in text
    assert "$Component = Resolve-DeployReleasePackageComponent -ComponentExplicit $componentExplicit" in deploy_release_block
    assert "$publishBackend = $Component -in @('full', 'intruoyi', 'backend')" in text
    assert "$publishFrontend = $Component -in @('full', 'intruoyi', 'frontend')" in text
    assert "$publishWebsite = $Component -in @('full', 'website')" in text


def test_frontend_only_manifest_does_not_require_database_sql_package() -> None:
    text = read_publish_script()
    manifest_body = _extract_powershell_function(text, "Write-ReleaseManifestV1")

    assert "if ($publishBackend) {" in manifest_body
    assert "$requiredSqlEntries = New-ReleaseRequiredSqlManifestEntries" in manifest_body
    assert "$schemaDigest = Get-ReleaseSchemaDigest -RequiredSqlEntries $requiredSqlEntries" in manifest_body
    assert "$schemaVersion = 'schema-digest-' + $schemaDigest.Substring('sha256:'.Length, 16)" in manifest_body
    assert "$schemaDigest = ConvertTo-ReleaseStringSha256Digest -Value 'schema:not-included'" in manifest_body
    assert "$schemaVersion = 'not-included'" in manifest_body
    assert "requiresDatabaseMigrationPrecheck = [bool]$publishBackend" in manifest_body
    assert "requiresRequiredDataPrecheck = [bool]$publishBackend" in manifest_body


def test_publish_required_sql_contract_includes_showroom_canvas_dependency_before_award_schema() -> None:
    text = read_publish_script()
    roots_body = _extract_powershell_function(text, "Get-ReleaseDatabaseSqlRoots")

    assert "20260606_showroom_hall_product_canvas_layout.sql" in roots_body
    assert roots_body.index("20260606_showroom_hall_product_canvas_layout.sql") < roots_body.index(
        "20260613_showroom_award_and_hall_item_schema.sql"
    )


def test_publish_script_resolves_paired_frontend_worktree_before_failing() -> None:
    text = read_publish_script()
    path_resolution_block = text[text.index("$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path") : text.index("$defaultWebsiteRepo = 'D:\\ProjectPackage\\Website'")]

    current_frontend = "$currentFrontendDir = Join-Path $workspaceRoot 'IntRuoyiFronted'"
    legacy_frontend = "$legacyFrontendDir = Join-Path $workspaceRoot 'yudao-ui-admin-vue3'"
    frontend_selection = "$frontendDir = if (Test-Path -LiteralPath $currentFrontendDir) { $currentFrontendDir } else { $legacyFrontendDir }"

    assert current_frontend in path_resolution_block
    assert legacy_frontend in path_resolution_block
    assert frontend_selection in path_resolution_block
    assert path_resolution_block.index(current_frontend) < path_resolution_block.index(legacy_frontend)
    assert path_resolution_block.index(legacy_frontend) < path_resolution_block.index(frontend_selection)
    assert "if (-not (Test-Path -LiteralPath $frontendDir)) {" in path_resolution_block
    assert "$worktreePortMapPath = Join-Path $scriptDir 'worktree-port-map.ps1'" in path_resolution_block
    assert ". $worktreePortMapPath" in path_resolution_block
    assert "Get-IntRuoyiWorktreePortContext -CurrentBackendRepoRoot $backendRepo" in path_resolution_block
    assert "$frontendDir = $worktreePortContext.FrontendPath" in path_resolution_block


def test_release_artifact_component_name_uses_explicit_branching_for_image_tar() -> None:
    text = read_publish_script()
    component_block = _extract_function_body(text, "Resolve-ReleaseArtifactComponentName", "New-ReleaseArtifactManifestEntries")

    assert "if ($RelativePath.StartsWith('intruoyi-images_')) {" in component_block
    assert "if ($publishBackend) {" in component_block
    assert "return 'backend'" in component_block
    assert "return 'admin-frontend'" in component_block
    assert "return if ($publishBackend)" not in component_block


def test_linux_target_runtime_env_files_are_written_with_lf_helper() -> None:
    text = read_publish_script()

    assert "function Write-Utf8LfNoBomFile" in text
    assert '$normalized = ($Content -replace "`r`n", "`n" -replace "`r", "`n")' in text
    assert "Write-Utf8LfNoBomFile -Path (Join-Path $runtimeEnvDir $targetRuntimeEnvFileName) -Content $content" in text
    assert "Write-Utf8LfNoBomFile -Path $remoteEnvLocal -Content $remoteEnvContent" in text


def test_release_images_use_worktree_staging_build_context() -> None:
    text = read_publish_script()

    assert "function New-ReleaseDockerBuildContext" in text
    assert "$dockerBuildContextRoot = Join-Path $releaseDir 'docker-build-context'" in text
    assert "if ($publishBackend -or $publishFrontend) {" in text
    assert "Preparing Docker build context from current worktree artifacts" in text
    assert "-FrontendRepoRoot $frontendDir" in text
    assert "-BackendRepoRoot $backendRepo" in text
    assert "Copy-Item -LiteralPath $BackendJarPath" in text
    assert "Copy-Item -LiteralPath $frontendDistSource -Destination $frontendRepoTarget -Recurse -Force" in text
    assert "'-f', $backendDockerfile," in text
    assert "'-f', $frontendDockerfile," in text
    assert "$dockerBuildContextRoot" in text
    assert "'-f', $backendDockerfile,\n    $workspaceRoot" not in text
    assert "'-f', $frontendDockerfile,\n    $workspaceRoot" not in text


def test_deploy_release_reads_remote_minio_credentials_for_showroom_file_rebind() -> None:
    text = read_publish_script()
    credential_block = text[text.index("$remoteMinioAccessKey = ''") : text.index("Info 'Checking local Docker daemon'")]

    assert "$requiresRemoteMinioCredentials = $publishBackend -and $Mode -ne 'build-release'" in credential_block
    assert "$requiresRemoteMinioCredentials = $publishBackend -and $Mode -ne 'build-release' -and -not $SkipMinioSync" not in credential_block
    assert "if ($requiresRemoteMinioCredentials) {" in credential_block
    assert "if ($requiresRemoteMinioCredentials -and [string]::IsNullOrWhiteSpace($RemoteMinioContainer))" in credential_block
    assert "Missing -RemoteMinioContainer" in credential_block
    assert "if ($requiresRemoteMinioCredentials -and (" in credential_block


def test_publish_script_synchronizes_database_and_minio_without_silent_skip() -> None:
    text = read_publish_script()

    assert "DROP DATABASE IF EXISTS `ruoyi-vue-pro`;" in text
    assert "CREATE DATABASE `ruoyi-vue-pro` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;" in text
    assert "docker rm -f intruoyi-mysql 2>/dev/null || true" in text
    assert "rm -rf '$RemoteAppDir/data/mysql' && mkdir -p '$RemoteAppDir/data/mysql'" in text
    assert "docker exec intruoyi-mysql mysqladmin -uroot -p$mySqlRootPassword ping --silent" in text
    assert "mc mirror --overwrite --disable-multipart --retry src/yudao dst/yudao" in text
    assert "--skip-errors" not in text
    assert "http://${ServerHost}:9000" in text
    assert "http://$ServerHost:9000" not in text


def test_publish_script_verifies_protected_showroom_file_storage_after_restore() -> None:
    text = read_publish_script()
    function_body = _extract_function_body(text, "New-ShowroomFileStoragePostImportSql", "Copy-ToServer")

    assert "function New-ShowroomFileStoragePostImportSql" in text
    assert "function Assert-RemoteFileStorageConfigRebound" in text
    assert "function Assert-RemoteBackendContainerMinioReachable" in text
    assert "function Get-RemoteShowroomSmokeImageAsset" in text
    assert "function Assert-RemoteShowroomSmokeImageContent" in text
    assert "SHOWROOM_FILE_CONFIG_UNBOUND" in text
    assert "SHOWROOM_FILE_CONFIG_28_PROTECTED" in text
    assert "SHOWROOM_FILE_STORAGE_ENDPOINT_UNREACHABLE" in text
    assert "SHOWROOM_FILE_CONTENT_SMOKE_FAILED" in text
    assert "JSON_SET(config" not in function_body
    assert "UPDATE infra_file_config" not in function_body
    assert "UPDATE infra_file" not in function_body
    assert "config LIKE '%127.0.0.1:9000%'" in function_body
    assert "url LIKE '%127.0.0.1:9000%'" in function_body
    assert "path LIKE 'showroom/%'" in function_body
    assert "Wait-HttpContentTypeOk -Url $smokeImageUrl -ExpectedContentType 'image/'" in text
    assert "http://host.docker.internal:9000/minio/health/live" in text

    assert "New-ShowroomFileStoragePostImportSql -TargetServerHost $ServerHost" in text
    assert "Regenerating target-bound post-import SQL for deploy-release" in text
    assert "if ($Mode -eq 'deploy-release' -and -not $SkipDatabaseSync)" not in text
    assert "if ($Mode -eq 'deploy-release')" in text[
        text.index("Regenerating target-bound post-import SQL for deploy-release") - 120:
        text.index("Regenerating target-bound post-import SQL for deploy-release")
    ]
    assert "Assert-RemoteFileStorageConfigRebound" in text[text.index("Invoke-RequiredDatabaseSqlScripts"):]
    assert "Assert-RemoteBackendContainerMinioReachable" in text[text.index("Wait-RemoteHttpOk -Url \"http://127.0.0.1:$BackendPort/actuator/health\""):]
    assert "Assert-RemoteShowroomSmokeImageContent" in text[text.index("Wait-HttpContentTypeOk -Url \"http://${ServerHost}:$FrontendPort/pdfjs/pdf.worker.mjs\""):]


def test_deploy_release_applies_showroom_file_config_rebind_for_code_only_packages() -> None:
    text = read_publish_script()

    assert "Regenerating target-bound post-import SQL for deploy-release" in text
    assert "if ($Mode -eq 'deploy-release' -and -not $SkipDatabaseSync)" not in text
    assert "if ($Mode -eq 'deploy-release')" in text[
        text.index("Regenerating target-bound post-import SQL for deploy-release") - 120:
        text.index("Regenerating target-bound post-import SQL for deploy-release")
    ]
    assert "if (-not (Test-Path -LiteralPath $postImportSqlLocal))" in text
    assert "Copy-ToServer -LocalPath $postImportSqlLocal -RemotePath $remotePostImportSql" in text
    assert "cat '$remotePostImportSql' | docker exec -i intruoyi-mysql mysql -uroot -p$mySqlRootPassword -D ruoyi-vue-pro" in text

    rebind_apply_idx = text.index("cat '$remotePostImportSql' | docker exec -i intruoyi-mysql")
    required_sql_idx = text.rindex("Invoke-RequiredDatabaseSqlScripts")
    verify_idx = text.rindex("Assert-RemoteFileStorageConfigRebound")

    assert rebind_apply_idx < required_sql_idx < verify_idx


def test_showroom_file_config_rebind_sql_keeps_mysql_delimiter_literal() -> None:
    text = read_publish_script()
    function_body = text.split("function New-ShowroomFileStoragePostImportSql", 1)[1].split(
        "function Copy-ToServer", 1
    )[0]

    assert "DELIMITER `$`$" in function_body
    assert "END `$`$" in function_body
    assert "DELIMITER $$" not in function_body
    assert "END $$" not in function_body


def test_publish_script_does_not_mutate_fixed_showroom_file_config_28() -> None:
    text = read_publish_script()
    edhr_body = _extract_function_body(
        text, "New-EdhrProtectedStoragePostImportSql", "New-ShowroomFileStoragePostImportSql"
    )
    showroom_body = _extract_function_body(
        text, "New-ShowroomFileStoragePostImportSql", "Copy-ToServer"
    )
    protected_sql_body = "\n".join([edhr_body, showroom_body])

    assert "SHOWROOM_FILE_CONFIG_28_PROTECTED" in text
    assert "infra_file_config.id=28 is protected from publish-time mutation" in text
    assert not re.search(r"UPDATE\s+infra_file_config\b", protected_sql_body, re.IGNORECASE)
    assert not re.search(r"INSERT\s+INTO\s+infra_file_config\b", protected_sql_body, re.IGNORECASE)
    assert not re.search(r"ON\s+DUPLICATE\s+KEY\s+UPDATE", protected_sql_body, re.IGNORECASE)
    assert not re.search(r"UPDATE\s+infra_file\b", showroom_body, re.IGNORECASE)
    assert "path LIKE 'showroom/%'" in showroom_body

    verify_body = _extract_function_body(text, "Assert-RemoteFileStorageConfigRebound", "Assert-RemoteBackendContainerMinioReachable")
    assert "path LIKE 'showroom/%'" in verify_body
    assert "SHOWROOM_FILE_CONFIG_28_PROTECTED" in verify_body


def test_publish_script_packages_and_applies_all_versioned_release_sql_for_all_deploys() -> None:
    text = read_publish_script()

    assert "function Get-ReleaseDatabaseSqlSortKey" in text
    assert "'20260611_mes_edhr_work_task_flow.sql' { return '20260611_000_mes_edhr_work_task_flow.sql' }" in text
    assert "'20260611_mes_edhr_multi_signature_approval.sql' { return '20260611_010_mes_edhr_multi_signature_approval.sql' }" in text
    assert "'20260611_mes_edhr_rejection_revision_flow.sql' { return '20260611_020_mes_edhr_rejection_revision_flow.sql' }" in text
    assert "'20260611_mes_smart_scheduling_tabs.sql' { return '20260611_100_mes_smart_scheduling_tabs.sql' }" in text
    assert "'20260611_mes_scheduler_workbench_smart_scheduling_tab.sql' { return '20260611_110_mes_scheduler_workbench_smart_scheduling_tab.sql' }" in text
    assert "'20260611_mes_smart_scheduling_extra_tabs.sql' { return '20260611_120_mes_smart_scheduling_extra_tabs.sql' }" in text
    assert "function Get-ReleaseDatabaseSqlRoots" in text
    assert "RelativePath = 'sql/mysql'" in text
    assert "RelativePath = 'sql/showroom'" in text
    assert "20260606_showroom_hall_product_canvas_layout.sql" in text
    assert "20260605_showroom_product_revision_attachment_schema.sql" in text
    assert "20260613_showroom_award_and_hall_item_schema.sql" in text
    assert "20260615_showroom_hall_canvas_background.sql" in text
    assert "function Get-ReleaseDatabaseSqlScripts" in text
    assert "Get-ChildItem -LiteralPath $sqlRoot -Filter '*.sql' -File" in text
    assert "$_.Name -match '^20\\d{6}_.+\\.sql$'" in text
    assert "Read-ReleaseMigrationMetadata -SqlPath $file.FullName" in text
    assert "Sort-Object @{ Expression = { Get-ReleaseDatabaseSqlSortKey -FileName ([System.IO.Path]::GetFileName(([string]$_.Path).Replace('/', [System.IO.Path]::DirectorySeparatorChar))) } }, Path" in text
    assert "Path = ($relativeRoot + '/' + $file.Name)" in text
    assert "$requiredDatabaseSqlScripts = Get-ReleaseDatabaseSqlScripts" in text
    assert "ruoyi-vue-pro.sql" not in text[text.index("function Get-ReleaseDatabaseSqlScripts"):text.index("$requiredSqlLocalDir")]
    assert "quartz.sql" not in text[text.index("function Get-ReleaseDatabaseSqlScripts"):text.index("$requiredSqlLocalDir")]
    assert "Environments = @('test', 'prod', 'backup')" in text
    assert "function Get-RequiredDatabaseSqlEntriesForEnvironment" in text
    assert "No required database SQL scripts are allowed for environment" in text

    assert "$requiredSqlLocalDir = Join-Path $releaseDir 'required-sql'" in text
    assert '$remoteRequiredSqlDir = "$remoteReleaseDir/required-sql"' in text
    assert "function Copy-RequiredDatabaseSqlScripts" in text
    assert "function Assert-RequiredDatabaseSqlScriptsInRelease" in text
    assert "function Copy-RequiredDatabaseSqlScriptsToServer" in text
    assert "function Invoke-RequiredDatabaseSqlScripts" in text
    assert "Required database SQL script missing" in text
    assert "Release package required SQL missing" in text
    assert "Copy-RequiredDatabaseSqlScripts" in text
    assert "Assert-RequiredDatabaseSqlScriptsInRelease" in text
    assert "Copy-RequiredDatabaseSqlScriptsToServer" in text
    assert "Applying required database SQL scripts" in text
    assert "test -f '$remoteSqlPath' && echo REQUIRED_SQL_EXISTS" in text
    assert "Get-RequiredDatabaseSqlEntriesForEnvironment -TargetEnvironment $Environment" in text
    assert "docker exec -i intruoyi-mysql mysql -uroot -p$mySqlRootPassword --default-character-set=utf8mb4 ruoyi-vue-pro" in text
    assert "allowedEnvironments = @($metadata.allowedEnvironments)" in text
    assert "$cleanupTargets += $remoteRequiredSqlDir" in text
    assert 'Invoke-SshCommand "rm -rf $cleanupArgs && mkdir -p $mkdirArgs"' in text

    import_idx = text.index("Replacing remote MySQL database")
    required_sql_idx = text.rindex("Invoke-RequiredDatabaseSqlScripts")
    backend_idx = text.index("Switching Website runtime directory")

    assert import_idx < required_sql_idx < backend_idx


def test_publish_script_creates_remote_required_sql_dir_before_uploading_files() -> None:
    text = read_publish_script()

    assert "function Prepare-RemoteReleaseTree" in text
    assert "$remoteRequiredSqlDir" in text[text.index("function Prepare-RemoteReleaseTree"):text.index("function Get-RequiredDatabaseSqlFileName")]
    assert "rm -rf '$remoteRequiredSqlDir' && mkdir -p '$remoteRequiredSqlDir'" not in text
    assert "foreach ($entry in $requiredDatabaseSqlScripts)" in text
    assert "$relativePath = [string]$entry.Path" in text
    assert "$packageSqlPath = Join-Path $requiredSqlLocalDir $fileName" in text
    assert "Copy-ToServer -LocalPath $packageSqlPath -RemotePath \"$remoteRequiredSqlDir/$fileName\"" in text
    assert "Copy-ToServer -LocalPath $requiredSqlLocalDir -RemotePath $remoteReleaseDir -Recursive" not in text


def test_deploy_release_uses_required_sql_from_release_manifest_not_current_worktree() -> None:
    text = read_publish_script()
    deploy_start = text.index("if ($Mode -eq 'deploy-release') {")
    deploy_end = text.index("Set-PublishRuntimeDefaultsForTarget -TargetServerHost", deploy_start)
    deploy_block = text[deploy_start:deploy_end]

    assert "function Get-ReleasePackageDatabaseSqlScripts" in text
    assert "$requiredDatabaseSqlScripts = Get-ReleasePackageDatabaseSqlScripts" in deploy_block
    assert "manifest.json missing in release package for required SQL" in text
    assert "$manifest.requiredSql" in text
    assert "$manifest.database.requiredDataSets" in text
    assert "$manifest.database.schemaMigrations" in text
    assert "Environments = @($allowedEnvironments)" in text
    assert "sourcePath missing in release package required SQL entry" in text
    assert "required SQL entry allowedEnvironments is empty" in text
    assert "Sort-Object @{ Expression = { Get-ReleaseDatabaseSqlSortKey -FileName ([System.IO.Path]::GetFileName([string]$_.sourcePath)) } }, sourcePath" in text
    assert "Release package contains no required SQL entries" in text
    assert deploy_block.index("Copy-ReleasePackageFromNas -PackageTag $ReleaseTag") < deploy_block.index(
        "$requiredDatabaseSqlScripts = Get-ReleasePackageDatabaseSqlScripts"
    ) < deploy_block.index("Assert-RequiredDatabaseSqlScriptsInRelease")


def test_build_release_packages_all_required_sql_across_environments() -> None:
    text = read_publish_script()
    copy_start = text.index("function Copy-RequiredDatabaseSqlScripts")
    copy_end = text.index("function Assert-RequiredDatabaseSqlScriptsInRelease", copy_start)
    copy_block = text[copy_start:copy_end]

    assert "foreach ($entry in $requiredDatabaseSqlScripts)" in copy_block
    assert "Get-RequiredDatabaseSqlEntriesForEnvironment -TargetEnvironment $Environment" not in copy_block


def test_deploy_release_executes_only_preflight_apply_migrations() -> None:
    text = read_publish_script()
    invoke_start = text.index("function Invoke-RequiredDatabaseSqlScripts")
    invoke_end = text.index("if ($Mode -eq 'mark-tested')", invoke_start)
    invoke_block = text[invoke_start:invoke_end]

    assert "function Read-ReleasePreflightPlan" in text
    assert "preflight-plan.json missing in release package; run preflight-release before deploy-release" in text
    assert "preflight-plan.json status must be passed before deploy-release" in text
    assert "preflight-plan.json contains blocked migration" in text
    assert "SKIP_ENV_NOT_ALLOWED" in text
    assert "function Get-ReleasePreflightApplyItems" in text
    assert "function Invoke-ReleaseMigrationStateUpdate" in text
    assert "INSERT INTO infra_release_migration" in text
    assert "SKIPPED_ALREADY_APPLIED" in invoke_block
    assert "$preflightApplyItems = @(Get-ReleasePreflightApplyItems -PreflightPlan $preflightPlan -PublishScope $releasePublishScope)" in invoke_block
    assert "$applyItems = Sort-RequiredDatabaseSqlApplyItems -Items $preflightApplyItems -TargetEnvironment $Environment" in invoke_block
    assert "Get-RequiredDatabaseSqlEntriesForEnvironment -TargetEnvironment $Environment" not in invoke_block
    assert "Invoke-ReleaseMigrationStateUpdate -Item $item -Status 'RUNNING'" in invoke_block
    assert "Invoke-ReleaseMigrationStateUpdate -Item $item -Status 'APPLIED'" in invoke_block
    assert "Invoke-ReleaseMigrationStateUpdate -Item $item -Status 'FAILED' -ErrorMessage $_.Exception.Message" in invoke_block


def test_deploy_release_injects_test_tenant_context_for_dcc_view_matrix_seed() -> None:
    text = read_publish_script()
    helper_start = text.index("function Get-RequiredDatabaseSqlSessionPreamble")
    helper_end = text.index("function Invoke-RequiredDatabaseSqlScripts", helper_start)
    helper_block = text[helper_start:helper_end]
    invoke_start = text.index("function Invoke-RequiredDatabaseSqlScripts")
    invoke_end = text.index("if ($Mode -eq 'mark-tested')", invoke_start)
    invoke_block = text[invoke_start:invoke_end]

    assert "function Get-RequiredDatabaseSqlSessionPreamble" in text
    assert "'20260624_dcc_view_matrix_independent_seed'" in helper_block
    assert "'SET @dcc_view_matrix_seed_tenant_id := 122;'" in helper_block
    assert "$sessionPreamble = Get-RequiredDatabaseSqlSessionPreamble -Item $item" in invoke_block
    assert "if ([string]::IsNullOrWhiteSpace($sessionPreamble))" in invoke_block
    assert "cat <<'SQL'" in invoke_block
    assert "cat '$remoteSqlPath'" in invoke_block
    assert "} | docker exec -i intruoyi-mysql mysql -uroot -p$mySqlRootPassword --default-character-set=utf8mb4 ruoyi-vue-pro" in invoke_block


def test_publish_script_packages_formal_dcc_view_matrix_test_tenant_prereq_sql() -> None:
    sql_path = REPO_ROOT / "sql" / "mysql" / "20260624_dcc_view_matrix_test_tenant_prereq.sql"

    assert sql_path.exists()


def test_deploy_release_executes_dcc_view_matrix_test_tenant_prereq_before_seed_on_test() -> None:
    text = read_publish_script()
    helper_start = text.index("function Get-RequiredDatabaseSqlSessionPreamble")
    helper_end = text.index("function Invoke-RequiredDatabaseSqlScripts", helper_start)
    helper_block = text[helper_start:helper_end]
    invoke_start = text.index("function Invoke-RequiredDatabaseSqlScripts")
    invoke_end = text.index("if ($Mode -eq 'mark-tested')", invoke_start)
    invoke_block = text[invoke_start:invoke_end]

    assert "function Sort-RequiredDatabaseSqlApplyItems" in text
    assert "'20260624_dcc_view_matrix_test_tenant_prereq' = 10" in helper_block
    assert "'20260624_dcc_view_matrix_independent_seed' = 20" in helper_block
    assert "$preflightApplyItems = @(Get-ReleasePreflightApplyItems -PreflightPlan $preflightPlan -PublishScope $releasePublishScope)" in invoke_block
    assert "$applyItems = Sort-RequiredDatabaseSqlApplyItems -Items $preflightApplyItems -TargetEnvironment $Environment" in invoke_block


def test_deploy_release_handles_empty_code_only_apply_queue_before_sorting() -> None:
    text = read_publish_script()
    invoke_start = text.index("function Invoke-RequiredDatabaseSqlScripts")
    invoke_end = text.index("if ($Mode -eq 'mark-tested')", invoke_start)
    invoke_block = text[invoke_start:invoke_end]

    assert "$preflightApplyItems = @(Get-ReleasePreflightApplyItems" in invoke_block
    assert "-Items (Get-ReleasePreflightApplyItems" not in invoke_block
    assert "[AllowEmptyCollection()]" in _extract_powershell_function(text, "Sort-RequiredDatabaseSqlApplyItems")


def test_deploy_release_preserves_preflight_dependency_order_for_non_priority_required_sql() -> None:
    text = read_publish_script()
    helper_start = text.index("function Sort-RequiredDatabaseSqlApplyItems")
    helper_end = text.index("function Invoke-RequiredDatabaseSqlScripts", helper_start)
    helper_block = text[helper_start:helper_end]

    assert "function Sort-RequiredDatabaseSqlApplyItems" in text
    assert "'20260624_dcc_view_matrix_test_tenant_prereq' = 10" in helper_block
    assert "'20260624_dcc_view_matrix_independent_seed' = 20" in helper_block
    assert "OriginalOrder" in helper_block
    assert "return 1000 + [int]$_.OriginalOrder" in helper_block
    assert "@{ Expression = { [int]$_.OriginalOrder } }" in helper_block


def test_deploy_release_generates_target_bound_preflight_plan_before_required_sql() -> None:
    text = read_publish_script()

    assert "function Write-ReleasePreflightPlan" in text
    assert "preflight-target-state-$Environment.json" in text
    assert "--target-environment', $Environment" in text
    assert "JSON_OBJECTAGG(" in text
    assert "script\\release\\release_preflight_plan.py" in text

    acquire_idx = text.rindex("Invoke-ReleaseOperationLockAcquire")
    write_plan_idx = text.rindex("Write-ReleasePreflightPlan")
    migration_idx = text.rindex("Invoke-RequiredDatabaseSqlScripts")

    assert acquire_idx < write_plan_idx < migration_idx


def test_deploy_release_acquires_release_operation_lock_before_migrations() -> None:
    text = read_publish_script()

    assert "function Invoke-ReleaseOperationLockAcquire" in text
    assert "function Invoke-ReleaseOperationLockRelease" in text
    assert "infra_release_operation_lock" in text
    assert "LOCK_ACQUIRED" in text
    assert "LOCK_RELEASED" in text

    acquire_idx = text.rindex("Invoke-ReleaseOperationLockAcquire")
    migration_idx = text.rindex("Invoke-RequiredDatabaseSqlScripts")
    release_idx = text.rindex("Invoke-ReleaseOperationLockRelease -Status 'APPLIED'")
    readiness_idx = text.rindex("Info 'Waiting for remote HTTP readiness'")

    assert acquire_idx < migration_idx < readiness_idx < release_idx


def test_publish_script_syncs_runtime_control_ops_scripts_without_secrets() -> None:
    text = read_publish_script()

    assert "$opsRuntimeLocalDir = Join-Path $releaseDir 'ops-runtime'" in text
    assert "$remoteOpsRuntimeDir = '/opt/intruoyi/ops/backup-ops/linux-native'" in text
    assert "function Copy-ReleaseOpsRuntimePackage" in text
    assert "script\\backup-ops\\$directoryName" in text
    assert "script\\deploy\\manage-int-ruoyi-remote-root-disk.ps1" in text
    assert "Where-Object { $_.Name -ne 'backup-ops.secrets.json' }" in text
    assert "Runtime-control ops package must not include backup-ops.secrets.json" in text
    assert "mkdir -p '$remoteOpsRuntimeDir/linux' '$remoteOpsRuntimeDir/scripts' '$remoteOpsRuntimeDir/actions'" in text
    assert "test -f '$remoteOpsRuntimeDir/linux/backup-ops-linux.sh'" in text
    assert "test -f '$remoteOpsRuntimeDir/script/deploy/manage-int-ruoyi-remote-root-disk.ps1'" in text
    assert "Copy-ReleaseOpsRuntimePackage" in text[text.index("if ($publishBackend) {\n    Copy-RequiredDatabaseSqlScriptsToServer"):text.index('Info "Reading remote compose services')]


def test_publish_runtime_requires_dcc_signature_evidence_secret() -> None:
    text = read_publish_script()
    compose = (DEPLOY_ROOT / "int-ruoyi-test" / "docker-compose.yml").read_text(encoding="utf-8")

    assert "[string]$DccSignatureEvidenceHmacSecret = $env:DCC_SIGNATURE_EVIDENCE_HMAC_SECRET" in text
    assert "[string]$DccSignatureEvidenceKeyVersion = $env:DCC_SIGNATURE_EVIDENCE_KEY_VERSION" in text
    assert "Missing DCC_SIGNATURE_EVIDENCE_HMAC_SECRET" in text
    assert "Missing DCC_SIGNATURE_EVIDENCE_KEY_VERSION" in text
    assert "DCC_SIGNATURE_EVIDENCE_HMAC_SECRET=$DccSignatureEvidenceHmacSecret" in text
    assert "DCC_SIGNATURE_EVIDENCE_KEY_VERSION=$DccSignatureEvidenceKeyVersion" in text
    assert "--dcc.signature.evidence.hmac-secret=${DCC_SIGNATURE_EVIDENCE_HMAC_SECRET}" in compose
    assert "--dcc.signature.evidence.key-version=${DCC_SIGNATURE_EVIDENCE_KEY_VERSION}" in compose


def test_publish_runtime_requires_dcc_viewer_token_onlyoffice_and_download_encryption_configuration() -> None:
    text = read_publish_script()
    compose = (DEPLOY_ROOT / "int-ruoyi-test" / "docker-compose.yml").read_text(encoding="utf-8")
    status = (DEPLOY_ROOT / "show-int-ruoyi-remote-status.ps1").read_text(encoding="utf-8")

    assert "[string]$DccViewerTokenHmacSecret = $env:DCC_VIEWER_TOKEN_HMAC_SECRET" in text
    assert "[string]$DccOnlyOfficeJwtSecret = $env:DCC_ONLYOFFICE_JWT_SECRET" in text
    assert "[string]$DccOnlyOfficeBaseUrl = $env:DCC_ONLYOFFICE_BASE_URL" in text
    assert "[string]$DccOnlyOfficePublicFileBaseUrl = $env:DCC_ONLYOFFICE_PUBLIC_FILE_BASE_URL" in text
    assert "[string]$DccDownloadEncryptionPolicyVersion = $env:DCC_DOWNLOAD_ENCRYPTION_POLICY_VERSION" in text
    assert "[string]$DccDownloadEncryptionKeyId = $env:DCC_DOWNLOAD_ENCRYPTION_KEY_ID" in text
    assert "[string]$DccDownloadEncryptionBase64Key = $env:DCC_DOWNLOAD_ENCRYPTION_BASE64_KEY" in text
    assert "[string]$DccDownloadEncryptionArtifactDirectory = $env:DCC_DOWNLOAD_ENCRYPTION_ARTIFACT_DIRECTORY" in text
    assert "Missing DCC_VIEWER_TOKEN_HMAC_SECRET" in text
    assert "Missing DCC_ONLYOFFICE_JWT_SECRET" in text
    assert "Missing DCC_ONLYOFFICE_BASE_URL" in text
    assert "Missing DCC_ONLYOFFICE_PUBLIC_FILE_BASE_URL" in text
    assert "Missing DCC_DOWNLOAD_ENCRYPTION_POLICY_VERSION" in text
    assert "Missing DCC_DOWNLOAD_ENCRYPTION_KEY_ID" in text
    assert "Missing DCC_DOWNLOAD_ENCRYPTION_BASE64_KEY" in text
    assert "Missing DCC_DOWNLOAD_ENCRYPTION_ARTIFACT_DIRECTORY" in text
    assert "DCC_VIEWER_TOKEN_HMAC_SECRET=$DccViewerTokenHmacSecret" in text
    assert "DCC_ONLYOFFICE_JWT_SECRET=$DccOnlyOfficeJwtSecret" in text
    assert "DCC_ONLYOFFICE_BASE_URL=$DccOnlyOfficeBaseUrl" in text
    assert "DCC_ONLYOFFICE_PUBLIC_FILE_BASE_URL=$DccOnlyOfficePublicFileBaseUrl" in text
    assert "DCC_DOWNLOAD_ENCRYPTION_BASE64_KEY=$DccDownloadEncryptionBase64Key" in text
    assert "--yudao.dcc.viewer-token.hmac-secret=${DCC_VIEWER_TOKEN_HMAC_SECRET}" in compose
    assert "--yudao.dcc.preview.onlyoffice.base-url=${DCC_ONLYOFFICE_BASE_URL}" in compose
    assert "--yudao.dcc.preview.onlyoffice.jwt-secret=${DCC_ONLYOFFICE_JWT_SECRET}" in compose
    assert "--yudao.dcc.preview.onlyoffice.public-file-base-url=${DCC_ONLYOFFICE_PUBLIC_FILE_BASE_URL}" in compose
    assert "--yudao.dcc.download.encryption.base64-key=${DCC_DOWNLOAD_ENCRYPTION_BASE64_KEY}" in compose
    assert "OnlyOffice" in status


def test_publish_runtime_preserves_dcc_project_code_codex_configuration() -> None:
    text = read_publish_script()
    compose = (DEPLOY_ROOT / "int-ruoyi-test" / "docker-compose.yml").read_text(encoding="utf-8")

    assert "[string]$DccProjectCodeCodexCliCommand = $env:DCC_PROJECT_CODE_CODEX_CLI_COMMAND" in text
    assert "[string]$DccProjectCodeCodexHome = $env:DCC_PROJECT_CODE_CODEX_HOME" in text
    assert "DCC_PROJECT_CODE_CODEX_CLI_COMMAND=$DccProjectCodeCodexCliCommand" in text
    assert "DCC_PROJECT_CODE_CODEX_HOME=$DccProjectCodeCodexHome" in text
    assert "/opt/intruoyi/runtime/tools/codex" in text
    assert "/opt/intruoyi/runtime/backend-codex-home" in text
    assert "DCC_PROJECT_CODE_CODEX_HOME: ${DCC_PROJECT_CODE_CODEX_HOME}" in compose
    assert "CODEX_HOME: ${DCC_PROJECT_CODE_CODEX_HOME}" in compose
    assert "--yudao.dcc.project-code-recognition.codex-cli-command=${DCC_PROJECT_CODE_CODEX_CLI_COMMAND}" in compose
    assert "- /opt/intruoyi/runtime/tools/codex:/opt/intruoyi/runtime/tools/codex:ro" in compose
    assert "- /opt/intruoyi/runtime/backend-codex-home:/opt/intruoyi/runtime/backend-codex-home" in compose


def test_release_package_embeds_runtime_env_for_all_targets() -> None:
    text = read_publish_script()

    assert "function Write-ReleaseRuntimeEnvPackage" in text
    assert "function Apply-ReleaseRuntimeEnvPackage" in text
    assert "function Test-RemoteFileExists" in text
    assert text.index("function Test-RemoteFileExists") < text.index("function Get-RemoteRuntimeEnvMap")
    assert "runtime-env" in text
    assert "test.env" in text
    assert "prod.env" in text
    assert "backup.env" in text
    assert "DCC_HARDCODED_VIEWER_TOKEN_HMAC_SECRET" in text
    assert "DCC_HARDCODED_ONLYOFFICE_JWT_SECRET" in text
    assert "DCC_HARDCODED_SIGNATURE_EVIDENCE_HMAC_SECRET" in text
    assert "Write-ReleaseRuntimeEnvPackage" in text
    assert "New-ReleaseRuntimeEnvContent -TargetEnvironment $targetEnvironment -TargetServerHost $targetServerHost" in text
    assert "Apply-ReleaseRuntimeEnvPackage -TargetEnvironment $Environment" in text
    assert '-HardcodedValue "http://${TargetServerHost}:$OnlyOfficeHostPort"' in text
    assert '-HardcodedValue "http://backend:48081"' in text
    assert "DCC_ONLYOFFICE_BASE_URL=$resolvedDccOnlyOfficeBaseUrl" in text
    assert "DCC_ONLYOFFICE_PUBLIC_FILE_BASE_URL=$resolvedDccOnlyOfficePublicFileBaseUrl" in text
    assert "DCC_DOWNLOAD_ENCRYPTION_BASE64_KEY=$resolvedDccDownloadEncryptionBase64Key" in text


def test_onlyoffice_public_file_base_url_uses_compose_backend_service() -> None:
    text = read_publish_script()

    assert '-HardcodedValue "http://backend:48081"' in text
    assert '-HardcodedValue "http://host.docker.internal:$BackendPort"' not in text


def test_release_runtime_env_onlyoffice_public_file_url_uses_backend_service() -> None:
    text = read_publish_script()

    assert '$resolvedDccOnlyOfficePublicFileBaseUrl = "http://backend:48081"' in text
    assert '$resolvedDccOnlyOfficePublicFileBaseUrl = "http://host.docker.internal:$BackendPort"' not in text


def test_deploy_checks_onlyoffice_container_can_reach_public_file_base_url() -> None:
    text = read_publish_script()

    assert "function Assert-RemoteOnlyOfficePublicFileBaseUrlReachable" in text
    assert "DCC_ONLYOFFICE_PUBLIC_FILE_BASE_URL must not use host.docker.internal" in text
    assert "$healthUrlLiteral = ConvertTo-ShellSingleQuotedLiteral -Value $healthUrl" in text
    assert "docker exec intruoyi-onlyoffice curl -fsS --connect-timeout 5 $healthUrlLiteral" in text
    assert "docker exec intruoyi-onlyoffice sh -lc" not in _extract_powershell_function(
        text, "Assert-RemoteOnlyOfficePublicFileBaseUrlReachable"
    )
    assert "/actuator/health" in text
    readiness_start = text.index('if ($IncludeOnlyOffice) {\n    Wait-RemoteHttpOk -Url "http://127.0.0.1:$OnlyOfficeHostPort/healthcheck"')
    readiness_block = text[readiness_start:text.index("if ($publishWebsite)", readiness_start)]
    assert readiness_block.index('Wait-RemoteHttpOk -Url "http://127.0.0.1:$OnlyOfficeHostPort/healthcheck"') < readiness_block.index(
        "Assert-RemoteOnlyOfficePublicFileBaseUrlReachable"
    )


def test_publish_onlyoffice_runtime_uses_real_image_and_health_gates() -> None:
    text = read_publish_script()
    compact = re.sub(r"\s+", " ", text)

    assert "$onlyOfficeImage = 'onlyoffice/documentserver:latest'" in text
    assert "ONLYOFFICE_HOST_PORT=$OnlyOfficeHostPort" in text
    assert "if ($publishBackend -and $IncludeOnlyOffice) {" in text
    assert "Invoke-CheckedCommand -FilePath 'docker' -ArgumentList @( 'image', 'inspect', $onlyOfficeImage )" in compact
    assert 'if ($publishBackend -and $IncludeOnlyOffice) { $releaseImages += $onlyOfficeImage }' in text
    assert "Invoke-CheckedCommand -FilePath 'docker' -ArgumentList (@('save', '-o', $imageTar) + $releaseImages)" in text
    assert 'Wait-RemoteHttpOk -Url "http://127.0.0.1:$OnlyOfficeHostPort/healthcheck"' in text
    assert 'Wait-HttpOk -Url "http://${ServerHost}:$OnlyOfficeHostPort/healthcheck"' in text
    assert 'Write-Host "OnlyOffice health: http://${ServerHost}:$OnlyOfficeHostPort/healthcheck"' in text


def test_publish_without_onlyoffice_skips_onlyoffice_deploy_operations() -> None:
    text = read_publish_script()

    assert "$runtimeServices = @()" in text
    assert "if ($IncludeOnlyOffice) { $runtimeServices += 'onlyoffice' }" in text
    assert "if ($publishBackend) { $runtimeServices += 'backend' }" in text
    assert "if ($runtimeServices.Count -gt 0)" in text
    assert "docker compose up -d $runtimeServiceDependencyFlag$runtimeServicesArg" in text
    assert "Assert-RemoteComposeService -Services $remoteComposeServices -ServiceName 'onlyoffice'" in text
    assert "if ($publishBackend -and $IncludeOnlyOffice) {" in text
    assert 'Wait-RemoteHttpOk -Url "http://127.0.0.1:$OnlyOfficeHostPort/healthcheck"' in text


def test_publish_script_builds_backend_frontend_and_website_runtime() -> None:
    text = read_publish_script()

    assert "[ValidateSet('full', 'intruoyi', 'backend', 'frontend', 'website')]" in text
    assert "[string]$Component = 'full'" in text
    assert "JAVA_OPTS=-Xms1g -Xmx2g -Djava.security.egd=file:/dev/./urandom" in text
    assert "$env:NODE_OPTIONS = '--max-old-space-size=8192'" in text
    assert "$env:VITE_BASE_URL = \"http://${ServerHost}:$BackendPort\"" in text
    assert "$publishBackend = $Component -in @('full', 'intruoyi', 'backend')" in text
    assert "$publishFrontend = $Component -in @('full', 'intruoyi', 'frontend')" in text
    assert "$publishWebsite = $Component -in @('full', 'website')" in text
    assert "Invoke-FrontendViteBuild -FrontendDir $frontendDir" in text
    assert "Assert-FrontendBuildStaticAssetContract -FrontendDir $frontendDir" in text
    assert "node_modules\\vite\\bin\\vite.js" in text
    assert "Building Website static assets" in text
    assert "Invoke-CheckedCommand -FilePath 'npm' -ArgumentList @('run', 'build', '--', '--outDir', $websiteDistSource, '--emptyOutDir') -WorkingDirectory $websiteRepo" in text
    assert "website.nginx.conf" in text
    assert "$runtimeServices += 'frontend'" in text
    assert "docker compose up -d --force-recreate website" in text


def test_publish_script_frontend_build_reuses_test_build_entrypoint() -> None:
    text = read_publish_script()

    assert "$env:NODE_OPTIONS = '--max-old-space-size=8192'" in text
    assert "Invoke-CheckedShell -Command 'pnpm build:test'" in text
    assert "Invoke-CheckedCommand -FilePath 'node' -ArgumentList @($viteCli, 'build', '--mode', 'test')" not in text


def test_publish_script_writes_manifest_v1_release_contract() -> None:
    text = read_publish_script()

    assert "function Write-ReleaseManifestV1" in text
    assert "function Write-ReleaseResourceReferenceManifest" in text
    assert "function New-ReleaseRequiredSqlManifestEntries" in text
    assert "function New-ReleaseBuildModuleManifestEntries" in text
    assert "schemaVersion = $schemaVersion" in text
    assert "schemaDigest = $schemaDigest" in text
    assert "migrationPlan = $migrationPlan" in text
    assert "requiredSql = $requiredSqlEntries" in text
    assert "buildModules = $buildModules" in text
    assert "compatibilityMatrix = @(" in text
    assert "operationEvidencePolicy = [ordered]@{" in text
    assert "resourceReferenceManifest = 'resources/resource-reference-manifest.json'" in text
    assert "Join-Path $releaseDir 'manifest.json'" in text
    assert "Write-ReleaseManifestV1 -PackageTag $PackageTag -LegacyArtifacts $files" in text


def test_frontend_nginx_allows_large_showroom_product_import_requests() -> None:
    nginx = (DEPLOY_ROOT / "int-ruoyi-test" / "nginx.conf").read_text(encoding="utf-8")

    assert "client_max_body_size 0;" in nginx
    assert nginx.index("client_max_body_size 0;") < nginx.index("location /admin-api/")


def test_publish_script_uses_release_repo_server_and_share_overrides() -> None:
    text = read_publish_script()

    assert "$config.server = $NasServer" in text
    assert "$config.share = $NasShare" in text


def test_publish_script_builds_website_dist_inside_release_package() -> None:
    text = read_publish_script()

    assert "$websiteDistSource = Join-Path $websiteRuntimeLocal 'dist'" in text
    assert "Invoke-CheckedCommand -FilePath 'npm' -ArgumentList @('run', 'build', '--', '--outDir', $websiteDistSource, '--emptyOutDir') -WorkingDirectory $websiteRepo" in text
    assert "Join-Path $websiteRepo 'dist'" not in text
    assert "Copy-Item -LiteralPath $websiteDistSource -Destination $websiteRuntimeLocal -Recurse -Force" not in text


def test_publish_script_stages_website_runtime_before_switching_remote_directory() -> None:
    text = read_publish_script()

    assert '$remoteWebsiteStagingDir = "$remoteReleaseDir/website"' in text
    assert '$remoteWebsitePreviousDir = "$RemoteAppDir/website.previous"' in text
    assert "function Prepare-RemoteReleaseTree" in text
    assert '$remoteRequiredSqlDir = "$remoteReleaseDir/required-sql"' in text
    assert "rm -rf $cleanupArgs && mkdir -p $mkdirArgs" in text
    assert "Copy-ToServer -LocalPath $websiteRuntimeLocal -RemotePath $remoteReleaseDir -Recursive" in text
    assert "test -f '$remoteWebsiteStagingDir/dist/index.html' && test -f '$remoteWebsiteStagingDir/nginx.conf'" in text
    assert "mv '$remoteWebsiteStagingDir' '$remoteWebsiteDir'" in text
    assert "docker compose up -d --force-recreate website" in text
    assert "rm -rf '$remoteWebsiteDir'" not in text
    assert "Copy-ToServer -LocalPath $websiteRuntimeLocal -RemotePath $RemoteAppDir -Recursive" not in text
    assert "Invoke-SshCommand \"rm -rf '$remoteRequiredSqlDir' && mkdir -p '$remoteRequiredSqlDir'\"" not in text


def test_publish_backend_component_skips_frontend_and_website_paths() -> None:
    text = read_publish_script()

    assert "if ($publishFrontend) {" in text
    assert "if ($publishWebsite) {" in text
    assert "if ($publishBackend) {" in text
    assert 'if ($publishFrontend) { Wait-HttpOk -Url "http://${ServerHost}:$FrontendPort/" -TimeoutSeconds 180 }' in text
    assert 'if ($publishWebsite) { Wait-HttpOk -Url "http://${ServerHost}:$WebsiteHostPort/" -TimeoutSeconds 180 }' in text
    assert 'if ($publishBackend) { Wait-HttpOk -Url "http://${ServerHost}:$BackendPort/actuator/health" -TimeoutSeconds 180 }' in text
    assert 'if ($publishWebsite) { Assert-PublicWebsiteScopedReleaseCurrent }' in text
    assert 'if ($publishWebsite) { Invoke-SshCommand "cd \'$RemoteAppDir\' && docker compose up -d --force-recreate website" }' in text
    assert "if ($publishFrontend) { $runtimeServices += 'frontend' }" in text


def test_publish_website_component_does_not_require_dcc_backend_runtime_secrets() -> None:
    text = read_publish_script()

    assert "if ($publishBackend -and $Mode -ne 'build-release' -and [string]::IsNullOrWhiteSpace($DccSignatureEvidenceHmacSecret))" in text
    assert "if ($publishBackend -and $Mode -ne 'build-release' -and [string]::IsNullOrWhiteSpace($DccSignatureEvidenceKeyVersion))" in text
    assert "if ($publishBackend -and $Mode -ne 'build-release' -and ([string]::IsNullOrWhiteSpace($DccViewerTokenHmacSecret)" in text
    assert "if ($publishBackend -and $IncludeOnlyOffice -and $Mode -ne 'build-release' -and [string]::IsNullOrWhiteSpace($DccOnlyOfficeJwtSecret))" in text
    assert "if ($publishBackend -and $Mode -ne 'build-release' -and [string]::IsNullOrWhiteSpace($DccDownloadEncryptionPolicyVersion))" in text


def test_publish_website_component_deploy_skips_backend_required_sql_package_gate() -> None:
    text = read_publish_script()

    assert "if ($publishBackend) {\n        Assert-RequiredDatabaseSqlScriptsInRelease\n    }" in text


def test_publish_script_accepts_explicit_website_repo_for_worktree_publish() -> None:
    text = read_publish_script()

    assert "[string]$WebsiteRepo = $env:INT_RUOYI_WEBSITE_REPO" in text
    assert "$defaultWebsiteRepo = 'D:\\ProjectPackage\\Website'" in text
    assert "if ([string]::IsNullOrWhiteSpace($WebsiteRepo))" in text
    assert "$websiteRepo = (Resolve-Path $WebsiteRepo).Path" in text


def test_publish_script_normalizes_multiline_ssh_commands_to_lf() -> None:
    text = read_publish_script()

    assert '$normalizedCommand = $Command -replace "`r`n", "`n" -replace "`r", "`n"' in text
    assert "$normalizedCommand" in text[text.index("function Invoke-SshCommand"):text.index("function Invoke-SshCapture")]
    assert "$normalizedCommand" in text[text.index("function Invoke-SshCapture"):text.index("function Get-LocalContainerEnvValue")]


def test_publish_script_uses_background_safe_fail_fast_ssh_options() -> None:
    text = read_publish_script()

    assert "function New-SshArgumentList" in text
    assert "function New-ScpArgumentList" in text
    assert "'-n'" in text[text.index("function New-SshArgumentList"):text.index("function New-ScpArgumentList")]
    assert "ConnectTimeout=10" in text
    assert "ConnectionAttempts=1" in text
    assert "ServerAliveInterval=10" in text
    assert "ServerAliveCountMax=3" in text
    assert "New-SshArgumentList -Command $normalizedCommand" in text
    assert "New-ScpArgumentList -LocalPath $LocalPath -RemotePath $RemotePath -Recursive:$Recursive" in text


def test_publish_script_places_release_and_runtime_data_on_remote_vdb_disk() -> None:
    text = read_publish_script()

    assert "[string]$RemoteReleaseRoot = '/var/lib/docker/intruoyi-releases'" in text
    assert "[string]$RemoteDataRoot = '/var/lib/docker/intruoyi-data/runtime-data'" in text
    assert "[string]$RemoteDataDiskMount = '/var/lib/docker'" in text
    assert "[string]$RemoteDataDiskDevice = '/dev/vdb'" in text
    assert '$remoteReleaseDir = "$RemoteReleaseRoot/$packageDirectoryName"' in text
    assert "function Assert-RemoteRuntimeDataOnDataDisk" in text
    assert "findmnt -n -o SOURCE --target '$RemoteDataDiskMount'" in text
    assert "Expected $RemoteDataDiskMount to be mounted from $RemoteDataDiskDevice" in text
    assert "mount --bind '$RemoteDataRoot' '$RemoteAppDir/data'" in text
    assert "df -P '$RemoteAppDir/data'" in text
    assert "mkdir -p '$RemoteAppDir/data/mysql' '$RemoteAppDir/data/redis' '$RemoteAppDir/data/backend-logs' '$RemoteAppDir/data/minio'" in text


def test_publish_dockerfiles_point_at_current_workspace_artifacts() -> None:
    deploy_root = DEPLOY_ROOT / "int-ruoyi-test"
    backend_dockerfile = (deploy_root / "Dockerfile.backend").read_text(encoding="utf-8")
    backend_base_dockerfile = (deploy_root / "Dockerfile.backend-base").read_text(encoding="utf-8")
    frontend_dockerfile = (deploy_root / "Dockerfile.frontend").read_text(encoding="utf-8")

    assert "COPY ruoyi-vue-pro/yudao-server/target/yudao-server-exec.jar app.jar" in backend_dockerfile
    assert "ARG BACKEND_RUNTIME_BASE_IMAGE" in backend_dockerfile
    assert "FROM ${BACKEND_RUNTIME_BASE_IMAGE}" in backend_dockerfile
    assert "apt-get" not in backend_dockerfile
    assert "docker.io" not in backend_dockerfile
    assert "docker-compose-v2" not in backend_dockerfile
    assert "FROM maven:" not in backend_dockerfile
    assert 'CMD ["sh", "-c", "exec java ${JAVA_OPTS} -jar app.jar ${ARGS}"]' in backend_dockerfile
    assert "FROM eclipse-temurin:21-jre-noble" in backend_base_dockerfile
    assert "ARG APT_MIRROR=http://mirrors.aliyun.com/ubuntu" in backend_base_dockerfile
    assert "security.ubuntu.com/ubuntu#${APT_MIRROR}" in backend_base_dockerfile
    assert "archive.ubuntu.com/ubuntu#${APT_MIRROR}" in backend_base_dockerfile
    assert "Acquire::Retries=5" in backend_base_dockerfile
    assert "python3 docker.io docker-compose-v2" in backend_base_dockerfile
    assert "COPY yudao-ui-admin-vue3/dist-intruoyi-test/ /usr/share/nginx/html/" in frontend_dockerfile
    assert "COPY ruoyi-vue-pro/script/deploy/int-ruoyi-test/nginx.conf" in frontend_dockerfile


def test_publish_script_loads_and_verifies_internal_backend_runtime_base_image() -> None:
    text = read_publish_script()
    backend_build_info_index = text.index("Info 'Building backend image'")
    backend_build_block_start = text.rindex("if ($publishBackend) {", 0, backend_build_info_index)
    build_block = text[
        backend_build_block_start:
        text.index("if ($publishFrontend) {", backend_build_info_index)
    ]

    assert "[ValidateSet('offline-tar')]" in text
    assert "[string]$BackendRuntimeBaseMode = $env:INTRUOYI_BACKEND_RUNTIME_BASE_MODE" in text
    assert "[string]$BackendRuntimeBaseTarPath = $env:INTRUOYI_BACKEND_RUNTIME_BASE_TAR" in text
    assert "[string]$BackendRuntimeBaseTarSha256 = $env:INTRUOYI_BACKEND_RUNTIME_BASE_TAR_SHA256" in text
    assert "[string]$BackendRuntimeBaseImage = $env:INTRUOYI_BACKEND_RUNTIME_BASE_IMAGE" in text
    assert "[string]$BackendRuntimeBaseDigest = $env:INTRUOYI_BACKEND_RUNTIME_BASE_DIGEST" in text
    assert "[string]$BackendRuntimeBaseVersion = $env:INTRUOYI_BACKEND_RUNTIME_BASE_VERSION" in text
    assert "function Resolve-BackendRuntimeBaseConfig" in text
    assert "function Assert-BackendRuntimeBaseImageAvailable" in text
    assert "Missing BackendRuntimeBaseMode" in text
    assert "Missing BackendRuntimeBaseTarPath" in text
    assert "Get-FileHash -Algorithm SHA256 -LiteralPath $config.TarPath" in text
    assert "Backend runtime base tar sha256 mismatch" in text
    assert "Invoke-CheckedCommand -FilePath 'docker' -ArgumentList @('load', '-i', $config.TarPath)" in text
    assert "Invoke-ProcessCapture -FilePath 'docker' -ArgumentList @('image', 'inspect', $config.Image" in text
    assert "Backend runtime base image id mismatch" in text
    assert "backendRuntimeBaseImage = $backendRuntimeBaseConfig.Image" in text
    assert "backendRuntimeBaseTarSha256 = $backendRuntimeBaseConfig.TarSha256" in text
    assert "RUNTIME_CONTROL_BACKEND_RUNTIME_BASE_MODE=$BackendRuntimeBaseMode" in text
    assert "RUNTIME_CONTROL_BACKEND_RUNTIME_BASE_TAR=$BackendRuntimeBaseTarPath" in text
    assert "RUNTIME_CONTROL_BACKEND_RUNTIME_BASE_TAR_SHA256=$BackendRuntimeBaseTarSha256" in text
    assert "RUNTIME_CONTROL_BACKEND_RUNTIME_BASE_IMAGE=$BackendRuntimeBaseImage" in text
    assert "RUNTIME_CONTROL_BACKEND_RUNTIME_BASE_DIGEST=$BackendRuntimeBaseDigest" in text
    assert "RUNTIME_CONTROL_BACKEND_RUNTIME_BASE_VERSION=$BackendRuntimeBaseVersion" in text
    assert "'--build-arg', \"BACKEND_RUNTIME_BASE_IMAGE=$($backendRuntimeBaseConfig.Image)\"" in build_block
    assert text.index("Assert-BackendRuntimeBaseImageAvailable -Config $backendRuntimeBaseConfig") < text.index(
        "Invoke-CheckedCommand -FilePath 'mvn'"
    )


def test_publish_script_fails_fast_when_backend_target_jar_is_locked_before_maven_clean() -> None:
    text = read_publish_script()

    assert "$backendJar = Join-Path $backendRepo 'yudao-server\\target\\yudao-server-exec.jar'" in text
    assert "function Assert-BackendJarAvailableForMavenClean" in text
    assert "Backend jar is locked before Maven clean" in text
    assert "restart-ruoyi-backend.bat" in text
    assert text.index("Assert-BackendJarAvailableForMavenClean -JarPath $backendJar") < text.index(
        "Checking local Docker daemon"
    )
    assert text.index("Assert-BackendJarAvailableForMavenClean -JarPath $backendJar") < text.index(
        "Invoke-CheckedCommand -FilePath 'mvn'"
    )


def test_build_release_backend_e2e_fails_fast_without_internal_backend_runtime_base_config(tmp_path: Path) -> None:
    env = os.environ.copy()
    for key in (
        "INTRUOYI_BACKEND_RUNTIME_BASE_MODE",
        "INTRUOYI_BACKEND_RUNTIME_BASE_TAR",
        "INTRUOYI_BACKEND_RUNTIME_BASE_TAR_SHA256",
        "INTRUOYI_BACKEND_RUNTIME_BASE_IMAGE",
        "INTRUOYI_BACKEND_RUNTIME_BASE_DIGEST",
        "INTRUOYI_BACKEND_RUNTIME_BASE_VERSION",
    ):
        env.pop(key, None)
    env["INTRUOYI_LOCAL_CACHE_ROOT"] = str(tmp_path / "cache")
    env["INT_RUOYI_WEBSITE_REPO"] = str(REPO_ROOT)
    env["RUNTIME_CONTROL_TEST_SERVER_HOST"] = "192.0.2.58"
    env["RUNTIME_CONTROL_PROD_SERVER_HOST"] = "192.0.2.57"
    env["RUNTIME_CONTROL_BACKUP_SERVER_HOST"] = "192.0.2.59"

    completed = subprocess.run(
        [
            "powershell.exe",
            "-NoProfile",
            "-ExecutionPolicy",
            "Bypass",
            "-File",
            str(PUBLISH_SCRIPT),
            "-Mode",
            "build-release",
            "-Component",
            "backend",
            "-ReleaseTag",
            "20260604_e2e_missing_base",
            "-SkipDatabaseSync",
            "-SkipMinioSync",
        ],
        cwd=REPO_ROOT,
        env=env,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        check=False,
        timeout=30,
    )
    output = f"{completed.stdout}\n{completed.stderr}"

    assert completed.returncode != 0
    assert "[FAIL] Missing BackendRuntimeBaseMode" in output
    assert "Checking local Docker daemon" not in output
    assert "Run: mvn" not in output
    assert "Building backend image" not in output
    assert "docker build" not in output


def test_publish_script_does_not_preflight_dockerhub_base_image_metadata() -> None:
    text = read_publish_script()

    assert "function Get-DockerfileBaseImages" not in text
    assert "function Assert-DockerBaseImageMetadataAvailable" not in text
    assert "@('manifest', 'inspect', $image)" not in text
    assert "DOCKERHUB_PREFLIGHT_FAILED" not in text
    assert "Cannot read Docker base image metadata" not in text
    assert "auth.docker.io / registry-1.docker.io" not in text


def test_publish_compose_uses_isolated_runtime_names_ports_and_dcc_config() -> None:
    compose = (DEPLOY_ROOT / "int-ruoyi-test" / "docker-compose.yml").read_text(encoding="utf-8")

    assert "name: intruoyi-runtime" in compose
    assert "container_name: intruoyi-backend" in compose
    assert "container_name: intruoyi-frontend" in compose
    assert "container_name: intruoyi-website" in compose
    assert "container_name: intruoyi-mysql" in compose
    assert "container_name: intruoyi-redis" in compose
    assert "container_name: intruoyi-onlyoffice" in compose
    assert "${BACKEND_HOST_PORT}:48081" in compose
    assert "${FRONTEND_HOST_PORT}:80" in compose
    assert "${WEBSITE_HOST_PORT}:80" in compose
    assert "${ONLYOFFICE_HOST_PORT}:80" in compose
    assert "./website/dist:/usr/share/nginx/html:ro" in compose
    assert "./website/nginx.conf:/etc/nginx/conf.d/default.conf:ro" in compose
    assert "host.docker.internal:host-gateway" in compose
    assert "onlyoffice/documentserver:latest" in compose
    assert "JWT_ENABLED: \"false\"" in compose
    assert "JWT_SECRET:" not in compose
    assert "condition: service_healthy" in compose
    assert "--yudao.dcc.viewer-token.hmac-secret=${DCC_VIEWER_TOKEN_HMAC_SECRET}" in compose
    assert "--yudao.dcc.download.encryption.artifact-directory=${DCC_DOWNLOAD_ENCRYPTION_ARTIFACT_DIRECTORY}" in compose
    assert "--yudao.dcc.project-code-recognition.codex-cli-command=${DCC_PROJECT_CODE_CODEX_CLI_COMMAND}" in compose
    assert "DCC_PROJECT_CODE_CODEX_HOME: ${DCC_PROJECT_CODE_CODEX_HOME}" in compose
    assert "CODEX_HOME: ${DCC_PROJECT_CODE_CODEX_HOME}" in compose


def test_publish_compose_keeps_test_login_captcha_consistent_with_frontend_build() -> None:
    compose = (DEPLOY_ROOT / "int-ruoyi-test" / "docker-compose.yml").read_text(encoding="utf-8")

    assert "--yudao.captcha.enable=false" in compose


def test_publish_frontend_nginx_proxies_admin_file_assets_to_backend() -> None:
    text = (DEPLOY_ROOT / "int-ruoyi-test" / "nginx.conf").read_text(encoding="utf-8")
    file_block = _extract_nginx_block(text, "location /admin-api/infra/file/")
    admin_block = _extract_nginx_block(text, "location /admin-api/")

    assert "location /admin-api/" in text
    assert "location /admin-api/infra/file/" in text
    assert "proxy_pass http://backend:48081;" in text
    assert "proxy_read_timeout 300s;" in file_block
    assert "proxy_send_timeout 300s;" in file_block
    assert "proxy_read_timeout 300s;" in admin_block
    assert "proxy_send_timeout 300s;" in admin_block
    assert "try_files $uri $uri/ /index.html;" in text


def test_publish_frontend_nginx_serves_pdf_worker_modules_as_javascript() -> None:
    text = (DEPLOY_ROOT / "int-ruoyi-test" / "nginx.conf").read_text(encoding="utf-8")
    pdfjs_location = _extract_nginx_block(text, "location ^~ /pdfjs/")

    assert "application/javascript" in text
    assert re.search(r"\bmjs\b", text)
    assert "application/javascript mjs;" in pdfjs_location
    assert "default_type application/javascript;" in pdfjs_location
    assert "location ^~ /assets/" in text
    assert "location ^~ /admin-ui-vue3/assets/" in text
    assert "try_files $uri =404;" in text


def test_publish_script_verifies_frontend_pdf_worker_mime_after_deploy() -> None:
    text = read_publish_script()

    assert "function Wait-HttpContentTypeOk" in text
    assert "[string]$ExpectedContentType" in text
    assert "'Cache-Control' = 'no-cache'" in text
    assert "expected $ExpectedContentType but got $contentType" in text
    assert 'Wait-HttpContentTypeOk -Url "http://${ServerHost}:$FrontendPort/pdfjs/pdf.worker.mjs" -ExpectedContentType \'application/javascript\'' in text
    assert text.index('Wait-HttpOk -Url "http://${ServerHost}:$FrontendPort/"') < text.index(
        'Wait-HttpContentTypeOk -Url "http://${ServerHost}:$FrontendPort/pdfjs/pdf.worker.mjs"'
    )


def test_publish_website_nginx_config_supports_scoped_showroom_media_routes() -> None:
    text = (DEPLOY_ROOT / "int-ruoyi-test" / "website.nginx.conf").read_text(encoding="utf-8")

    assert "listen 80;" in text
    assert "root /usr/share/nginx/html;" in text
    assert "location /showroom/sites/" in text
    assert "location /showroom/release/" in text
    assert "location /showroom/assets/" in text
    assert "location /showroom/display/" in text
    assert "location /admin-api/" in text
    assert "proxy_pass http://__BACKEND_ORIGIN__;" in text
    assert "try_files $uri $uri/ /index.html;" in text


def test_publish_website_nginx_config_forces_no_store_for_spa_entry() -> None:
    text = (DEPLOY_ROOT / "int-ruoyi-test" / "website.nginx.conf").read_text(encoding="utf-8")

    root_entry = _extract_nginx_block(text, "location = /")
    index_entry = _extract_nginx_block(text, "location = /index.html")

    for block in (root_entry, index_entry):
        assert 'add_header Cache-Control "no-store, no-cache, must-revalidate, max-age=0" always;' in block
        assert 'add_header Pragma "no-cache" always;' in block
        assert 'add_header Expires "0" always;' in block

    assert "try_files $uri $uri/ /index.html;" in text


def test_publish_website_nginx_config_caches_hashed_assets_with_immutable() -> None:
    text = (DEPLOY_ROOT / "int-ruoyi-test" / "website.nginx.conf").read_text(encoding="utf-8")

    assets = _extract_nginx_block(text, "location /assets/")

    assert 'add_header Cache-Control "public, max-age=31536000, immutable" always;' in assets
    assert "try_files $uri =404;" in assets


def test_publish_script_smoke_checks_public_website_scoped_current_release() -> None:
    text = read_publish_script()

    assert "[string]$ShowroomSiteKey = 'yingtai-showroom'" in text
    assert "[string]$ShowroomStage = 'TEST'" in text
    assert "function Assert-PublicWebsiteScopedReleaseCurrent" in text
    assert "$backendCurrentUrl = \"http://${ServerHost}:$BackendPort/showroom/sites/$ShowroomSiteKey/stages/$ShowroomStage/release/current\"" in text
    assert "$websiteCurrentUrl = \"http://${ServerHost}:$WebsiteHostPort/showroom/sites/$ShowroomSiteKey/stages/$ShowroomStage/release/current\"" in text
    assert "SHOWROOM_WEBSITE_CURRENT_READBACK_FAILED" in text
    assert "Assert-PublicWebsiteScopedReleaseCurrent" in text[text.index("Wait-HttpOk -Url \"http://${ServerHost}:$WebsiteHostPort/showroom\""):]


def test_publish_script_readback_checks_website_entry_bundle_and_headers_after_deploy() -> None:
    text = read_publish_script()

    assert "function Assert-PublicWebsiteEntryReadback" in text
    assert "$websiteEntryUrl = \"http://${ServerHost}:$WebsiteHostPort/\"" in text
    assert "SHOWROOM_WEBSITE_ENTRY_READBACK_FAILED" in text
    assert "no-store" in text
    assert "public, max-age=31536000, immutable" in text
    assert "yingtai-showroom" in text
    assert "TEST" in text
    assert "3221225472" in text
    assert "1073741824" in text
    assert "Assert-PublicWebsiteEntryReadback" in text[text.index("Wait-HttpOk -Url \"http://${ServerHost}:$WebsiteHostPort/\""):]
    assert text.index("Assert-PublicWebsiteEntryReadback") < text.index("Assert-PublicWebsiteScopedReleaseCurrent")


def test_unified_ops_bat_routes_publish_to_single_powershell_script() -> None:
    text = (REPO_ROOT / "运维工具.bat").read_text(encoding="utf-8")

    assert 'set "PUBLISH_PS1=%ROOT%script\\deploy\\publish-int-ruoyi.ps1"' in text
    assert "publish-int-ruoyi-to-test.bat" not in text
    assert "publish-int-ruoyi-to-prod.bat" not in text
    assert "publish-int-ruoyi-direct-to-prod.bat" not in text
    assert "promote test release" not in text.lower()
    assert "prod-direct" not in text
    assert 'powershell -NoProfile -ExecutionPolicy Bypass -File "%PUBLISH_PS1%" -Environment test' in text
    assert 'powershell -NoProfile -ExecutionPolicy Bypass -File "%PUBLISH_PS1%" -Environment prod -ConfirmText' in text
    assert "Type PROD to continue:" in text


def test_unified_ops_bat_keeps_restart_and_status_routes() -> None:
    text = (REPO_ROOT / "运维工具.bat").read_text(encoding="utf-8")

    assert 'set "TEST_RESTART_BAT=%ROOT%script\\deploy\\restart-int-ruoyi-to-test.bat"' in text
    assert 'set "PROD_RESTART_BAT=%ROOT%script\\deploy\\restart-int-ruoyi-to-prod.bat"' in text
    assert 'set "TEST_STATUS_BAT=%ROOT%script\\deploy\\show-int-ruoyi-test-status.bat"' in text
    assert 'set "PROD_STATUS_BAT=%ROOT%script\\deploy\\show-int-ruoyi-prod-status.bat"' in text
    assert 'if /i "%~1"=="test-restart"' in text
    assert 'if /i "%~1"=="prod-restart"' in text
    assert 'if /i "%~1"=="test-status"' in text
    assert 'if /i "%~1"=="prod-status"' in text
    assert "1. Publish" in text
    assert "2. Restart" in text
    assert "3. Status" in text


def test_restart_and_status_scripts_check_remote_runtime_data_disk() -> None:
    restart_text = (DEPLOY_ROOT / "restart-int-ruoyi-remote.ps1").read_text(encoding="utf-8")
    status_text = (DEPLOY_ROOT / "show-int-ruoyi-remote-status.ps1").read_text(encoding="utf-8")

    for text in (restart_text, status_text):
        assert "[string]$RemoteDataRoot = '/var/lib/docker/intruoyi-data/runtime-data'" in text
        assert "[string]$RemoteDataDiskMount = '/var/lib/docker'" in text
        assert "[string]$RemoteDataDiskDevice = '/dev/vdb'" in text
        assert "[string]$RemoteMinioContainer" in text
        assert "findmnt -n -o SOURCE --target '$RemoteDataDiskMount'" in text
        assert "df -P '$RemoteAppDir/data'" in text

    assert "Assert-RemoteRuntimeDataOnDataDisk" in restart_text
    assert "Runtime data directory:" in status_text
    assert "dataDiskState" in status_text


def test_status_script_reports_onlyoffice_and_frontend_pdf_worker_health() -> None:
    status_text = (DEPLOY_ROOT / "show-int-ruoyi-remote-status.ps1").read_text(encoding="utf-8")

    assert "[ValidateSet('backend', 'frontend', 'full', 'website', 'onlyoffice')]" in status_text
    assert "[int]$OnlyOfficeHostPort = 8080" in status_text
    assert "'onlyoffice' {" in status_text
    assert "http://${ServerHost}:$OnlyOfficeHostPort/healthcheck" in status_text
    assert "onlyoffice=$(Get-ContainerRuntimeState -Name 'onlyoffice')" in status_text
    assert "OnlyOffice=$onlyOfficeProbe" in status_text
    assert 'Write-Host "OnlyOffice status: $onlyOfficeProbe"' in status_text
    assert "function Probe-HttpContentType" in status_text
    assert "function Probe-FrontendPdfWorker" in status_text
    assert "pdfjs/pdf.worker.mjs" in status_text
    assert "ExpectedContentType 'application/javascript'" in status_text
    assert "$pdfWorkerProbe -like 'HTTP 2* application/javascript*'" in status_text
    assert "Frontend PDF.js worker: $pdfWorkerProbe" in status_text


def test_restart_bat_wrappers_target_test_and_production() -> None:
    test_text = (DEPLOY_ROOT / "restart-int-ruoyi-to-test.bat").read_text(encoding="utf-8")
    prod_text = (DEPLOY_ROOT / "restart-int-ruoyi-to-prod.bat").read_text(encoding="utf-8")

    assert 'set "PS1=%SCRIPT_DIR%restart-int-ruoyi-remote.ps1"' in test_text
    assert 'set "SERVER_HOST=172.30.30.58"' in test_text
    assert 'set "PS1=%SCRIPT_DIR%restart-int-ruoyi-remote.ps1"' in prod_text
    assert 'set "SERVER_HOST=172.30.30.57"' in prod_text
    assert 'if /i "%~1"=="cancel"' in prod_text


def test_status_bat_wrappers_target_test_and_production() -> None:
    test_text = (DEPLOY_ROOT / "show-int-ruoyi-test-status.bat").read_text(encoding="utf-8")
    prod_text = (DEPLOY_ROOT / "show-int-ruoyi-prod-status.bat").read_text(encoding="utf-8")

    assert 'set "PS1=%SCRIPT_DIR%show-int-ruoyi-remote-status.ps1"' in test_text
    assert 'set "SERVER_HOST=172.30.30.58"' in test_text
    assert 'set "PS1=%SCRIPT_DIR%show-int-ruoyi-remote-status.ps1"' in prod_text
    assert 'set "SERVER_HOST=172.30.30.57"' in prod_text
