from pathlib import Path


BACKEND_ROOT = Path(__file__).resolve().parents[2]
BACKUP_OPS_ROOT = BACKEND_ROOT / "script" / "backup-ops"
REGISTRAR = BACKUP_OPS_ROOT / "actions" / "Register-BackupOpsScheduledTasks.ps1"
CONFIG_EXAMPLE = BACKUP_OPS_ROOT / "config" / "backup-ops.config.example.json"
SECRETS_EXAMPLE = BACKUP_OPS_ROOT / "config" / "backup-ops.secrets.example.json"
BACKUP_PLAN_SERVICE = (
    BACKEND_ROOT
    / "yudao-module-infra"
    / "src"
    / "main"
    / "java"
    / "cn"
    / "iocoder"
    / "yudao"
    / "module"
    / "infra"
    / "service"
    / "backupplan"
    / "BackupPlanServiceImpl.java"
)
RUNTIME_CONTROL_ACTION = (
    BACKEND_ROOT
    / "yudao-module-infra"
    / "src"
    / "main"
    / "java"
    / "cn"
    / "iocoder"
    / "yudao"
    / "module"
    / "infra"
    / "service"
    / "runtimecontrol"
    / "RuntimeControlOperationAction.java"
)
PUBLISH_SCRIPT = BACKEND_ROOT / "script" / "deploy" / "publish-int-ruoyi.ps1"


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def test_backend_script_directory_is_the_runtime_source_of_truth() -> None:
    service_source = read_text(BACKUP_PLAN_SERVICE)
    runtime_source = read_text(RUNTIME_CONTROL_ACTION)

    assert 'repoRoot.resolve("script/backup-ops/scripts/backup-ops.ps1")' in service_source
    assert (
        'repoRoot.resolve("script/backup-ops/actions/Register-BackupOpsScheduledTasks.ps1")'
        in service_source
    )
    assert '"script/backup-ops/scripts/backup-ops.ps1"' in runtime_source
    assert "IntRuoyiMaintance" not in service_source
    assert "IntRuoyiMaintance" not in runtime_source


def test_release_package_uses_backend_backup_ops_and_excludes_secrets() -> None:
    publish_source = read_text(PUBLISH_SCRIPT)

    assert 'Join-Path $backendRepo "script\\backup-ops\\$directoryName"' in publish_source
    assert "Join-Path $backendRepo 'script\\backup-ops\\config'" in publish_source
    assert "backup-ops.secrets.json" in publish_source
    assert "must not include backup-ops.secrets.json" in publish_source
    assert "IntRuoyiMaintance" not in publish_source


def test_registrar_requires_repository_environment_without_a_default() -> None:
    registrar_source = read_text(REGISTRAR)
    config_source = read_text(CONFIG_EXAMPLE)

    assert '"repositoryEnvironment"' in config_source
    assert "backup.repositoryEnvironment" in registrar_source
    assert "ValidateSet('test', 'backup')" in registrar_source
    assert "-RepositoryEnvironment $repositoryEnvironment" in registrar_source
    assert "-TargetEnvironment 'prod'" in registrar_source
    assert "'-NonInteractive'" in registrar_source


def test_registrar_fails_fast_when_repository_environment_is_missing_or_invalid() -> None:
    registrar_source = read_text(REGISTRAR)

    assert "backup.repositoryEnvironment is required" in registrar_source
    assert "Unsupported backup.repositoryEnvironment" in registrar_source


def test_registrar_uses_protected_authorization_and_never_passes_cleartext() -> None:
    registrar_source = read_text(REGISTRAR)
    secrets_source = read_text(SECRETS_EXAMPLE)

    assert '"productionBackupConfirmText"' in secrets_source
    assert "auth.productionBackupConfirmText" in registrar_source
    assert "productionAuthorizationProof" in registrar_source
    assert "masked" in registrar_source.lower()
    assert "-ProductionBackupConfirmText" not in registrar_source
    assert "-Password" not in registrar_source


def test_registrar_binds_acl_validation_to_the_s4u_limited_principal() -> None:
    registrar_source = read_text(REGISTRAR)
    secrets_source = read_text(SECRETS_EXAMPLE)

    assert '"taskPrincipal"' in secrets_source
    assert '"principalId"' in secrets_source
    assert "taskPrincipal.principalId" in registrar_source
    assert "Assert-BackupOpsBatchLogonRight -PrincipalId $principalId" in registrar_source
    assert "Assert-BackupOpsPrincipalAclIdentity -PrincipalId $principalId" in registrar_source
    assert "Assert-BackupOpsSecretsAcl" in registrar_source
    assert "-RejectOrdinaryUserWrite" in registrar_source
    assert "New-ScheduledTaskPrincipal" in registrar_source
    assert "-LogonType S4U" in registrar_source
    assert "-RunLevel Limited" in registrar_source
    assert "-Principal $principal" in registrar_source
    assert "-RunLevel Highest" not in registrar_source


def test_registrar_fails_fast_when_principal_id_is_missing() -> None:
    registrar_source = read_text(REGISTRAR)

    assert "taskPrincipal.principalId is required" in registrar_source
    assert "IsNullOrWhiteSpace($principalId)" in registrar_source
