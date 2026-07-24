from pathlib import Path


DEPLOY_ROOT = Path(__file__).resolve().parents[1] / "deploy"
PUBLISH_SCRIPT = "publish-int-ruoyi.ps1"
LEGACY_PUBLISH_ENTRYPOINTS = [
    "publish-int-ruoyi-to-test.ps1",
    "promote-int-ruoyi-test-to-prod.ps1",
    "publish-int-ruoyi-direct-to-prod.bat",
]

REQUIRED_EDHR_ENV = [
    "EDHR_S3_ENDPOINT",
    "EDHR_S3_BUCKET",
    "EDHR_S3_REGION",
    "EDHR_S3_ACCESS_KEY",
    "EDHR_S3_SECRET_KEY",
    "EDHR_S3_RETENTION_MODE",
    "EDHR_S3_RETAIN_UNTIL_DAYS",
    "EDHR_S3_REQUIRE_LEGAL_HOLD",
]


def _read_deploy_file(name: str) -> str:
    return (DEPLOY_ROOT / name).read_text(encoding="utf-8")


def _read_publish_script() -> str:
    return _read_deploy_file(PUBLISH_SCRIPT)


def test_unified_publish_runtime_requires_edhr_protected_storage_env() -> None:
    script = _read_publish_script()
    compose = _read_deploy_file("int-ruoyi-test/docker-compose.yml")

    assert "Invoke-EdhrStorageRetentionVerifier -BackendRepo $backendRepo" in script
    assert "Assert-EdhrProtectedStorageConfig -Context 'target publish environment'" in script
    assert "Set-EdhrStorageVerifierEnvironment" in script
    assert "New-EdhrProtectedStoragePostImportSql" in script
    assert "infra_file_config.id=28 is protected from publish-time mutation" in script

    for key in REQUIRED_EDHR_ENV:
        assert f"$env:{key}" in script, f"publish script must read {key} from operator environment"
        assert f"{key}=$" in script, f"publish script must write {key} to remote runtime .env"
        assert f"{key}: ${{{key}}}" in compose, f"backend runtime must receive {key}"

    assert 'Fail "Missing $key for $Context; eDHR protected storage/Object Lock is fail-fast' in script
    assert "--edhr.s3" not in compose
    assert "UPDATE infra_file_config" not in script.split("function New-EdhrProtectedStoragePostImportSql", 1)[1].split(
        "function New-ShowroomFileStoragePostImportSql", 1
    )[0]
    assert "EDHR_S3_SECRET_KEY=changeme" not in script
    assert "EDHR_S3_SECRET_KEY=test" not in script
    assert "EDHR_S3_SECRET_KEY=dummy" not in script
    assert "EDHR_S3_SECRET_KEY=password" not in script


def test_unified_publish_does_not_allow_skip_minio_to_bypass_edhr_gate() -> None:
    script = _read_publish_script()

    gate = script.index("Assert-EdhrProtectedStorageConfig -Context 'target publish environment'")
    verifier = script.index("Invoke-EdhrStorageRetentionVerifier -BackendRepo $backendRepo")
    build = script.index("Info 'Building backend jar'")
    minio_branch = script.index("if (-not $SkipMinioSync) {")
    db_reset = script.index("Info 'Resetting remote MySQL container and data directory before import'")

    assert gate < build
    assert verifier < build
    assert gate < minio_branch
    assert verifier < minio_branch
    assert gate < db_reset
    assert verifier < db_reset


def test_unified_publish_handles_test_and_prod_with_the_same_edhr_gate() -> None:
    script = _read_publish_script()

    assert "[ValidateSet('test', 'prod', 'backup')]" in script
    assert "[string]$Environment = 'test'" in script
    assert "[string]$ConfirmText = ''" in script
    assert "@('prod', 'backup') -contains $Environment" in script
    assert "Explicit confirmation required for production-grade publish" in script

    for key in REQUIRED_EDHR_ENV:
        assert f"$env:{key}" in script
        assert f"{key}=$" in script


def test_release_runtime_env_can_bind_edhr_storage_per_target_environment() -> None:
    script = _read_publish_script()

    assert "Resolve-TargetPublishRuntimeValue" in script
    assert "Get-TargetSpecificEdhrEnvName" in script
    assert "TargetEnvironment" in script
    assert "EDHR_S3_TEST_ENDPOINT" in script
    assert "EDHR_S3_BACKUP_ENDPOINT" in script
    assert "EDHR_S3_PROD_ENDPOINT" in script

    for suffix in [
        "ENDPOINT",
        "BUCKET",
        "REGION",
        "ACCESS_KEY",
        "SECRET_KEY",
        "RETENTION_MODE",
        "RETAIN_UNTIL_DAYS",
        "REQUIRE_LEGAL_HOLD",
    ]:
        assert f"EDHR_S3_TEST_{suffix}" in script
        assert f"EDHR_S3_BACKUP_{suffix}" in script
        assert f"EDHR_S3_PROD_{suffix}" in script

    runtime_env_writer = script.split("function New-ReleaseRuntimeEnvContent", 1)[1].split(
        "function Write-ReleaseRuntimeEnvPackage", 1
    )[0]
    assert "Resolve-TargetPublishRuntimeValue" in runtime_env_writer
    assert "-TargetEnvironment $TargetEnvironment" in runtime_env_writer

    defaults = script.split("function Set-PublishRuntimeDefaultsForTarget", 1)[1].split(
        "function Read-ReleaseRuntimeEnvFile", 1
    )[0]
    assert "Resolve-TargetPublishRuntimeValue" in defaults
    assert "-TargetEnvironment $TargetEnvironment" in defaults


def test_intruoyi_deploy_release_does_not_prepare_website_runtime_tree() -> None:
    script = _read_publish_script()
    prepare_tree = script.split("function Prepare-RemoteReleaseTree", 1)[1].split(
        "function Get-RequiredDatabaseSqlFileName", 1
    )[0]

    assert "if ($publishWebsite)" in prepare_tree
    assert "if ($publishBackend)" in prepare_tree
    assert "$remoteWebsiteStagingDir" not in prepare_tree.split("if ($publishWebsite)", 1)[0]
    assert "$remoteWebsitePreviousDir" not in prepare_tree.split("if ($publishWebsite)", 1)[0]


def test_deploy_release_verifies_remote_env_image_tag_after_copy() -> None:
    script = _read_publish_script()
    copy_block = script.split("Info \"Copying compose and environment files", 1)[1].split(
        "if ($publishBackend -or $publishFrontend) {\n    Copy-ToServer -LocalPath $imageTar", 1
    )[0]

    assert "function Assert-RemoteRuntimeEnvImageTag" in script
    assert "IMAGE_TAG=$packageDirectoryName" in script
    assert "Assert-RemoteRuntimeEnvImageTag" in copy_block


def test_intruoyi_deploy_release_does_not_cleanup_website_previous() -> None:
    script = _read_publish_script()
    cleanup = script.split("Info 'Cleaning remote release temp files'", 1)[1].split(
        "Write-Host ''", 1
    )[0]

    assert "if ($publishWebsite)" in cleanup
    assert "$remoteWebsitePreviousDir" not in cleanup.split("if ($publishWebsite)", 1)[0]


def test_release_deploy_without_onlyoffice_does_not_start_or_wait_onlyoffice_dependency() -> None:
    script = _read_publish_script()
    runtime_start = script.split("$runtimeServices = @()", 1)[1].split(
        "Info 'Waiting for remote HTTP readiness'", 1
    )[0]
    readiness = script.split("Info 'Waiting for remote HTTP readiness'", 1)[1].split(
        "if ($publishWebsite)", 1
    )[0]

    assert "$runtimeServiceDependencyFlag = if ($IncludeOnlyOffice) { '' } else { '--no-deps ' }" in runtime_start
    assert "docker compose up -d $runtimeServiceDependencyFlag$runtimeServicesArg" in runtime_start
    assert "if ($IncludeOnlyOffice)" in readiness
    assert "Test-RemoteComposeService -Services $remoteComposeServices -ServiceName 'onlyoffice'" not in readiness


def test_code_only_intruoyi_deploy_does_not_require_showroom_media_content_readback() -> None:
    script = _read_publish_script()
    readiness = script.split("Info 'Waiting for remote HTTP readiness'", 1)[1].split(
        "if ($Mode -eq 'deploy-release'", 1
    )[0]

    assert "$verifyShowroomMediaContent = $publishWebsite -or (-not $SkipMinioSync)" in readiness
    assert "if ($publishFrontend -and $verifyShowroomMediaContent)" in readiness
    assert "code-only deploy without Website or MinIO data sync" in readiness


def test_legacy_publish_entrypoints_are_removed_so_they_cannot_bypass_edhr_or_dcc_gates() -> None:
    for name in LEGACY_PUBLISH_ENTRYPOINTS:
        assert not (DEPLOY_ROOT / name).exists(), f"legacy publish entrypoint must not exist: {name}"
