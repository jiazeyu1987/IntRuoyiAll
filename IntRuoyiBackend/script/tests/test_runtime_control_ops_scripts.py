from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
RUNTIME_CONTROL_DIR = (
    REPO_ROOT
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
)


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def test_runtime_control_ops_are_whitelisted_to_existing_scripts():
    action_source = read_text(RUNTIME_CONTROL_DIR / "RuntimeControlOperationAction.java")

    assert "build-release" in action_source
    assert "构建发布包" in action_source
    assert "publish-test" in action_source
    assert "部署发布包到测试服" in action_source
    assert action_source.count('"script/deploy/publish-int-ruoyi.ps1"') == 5
    assert "script/deploy/publish-int-ruoyi-to-test.ps1" not in action_source
    assert "mark-release-tested" in action_source
    assert "标记测试通过" in action_source
    assert "promote-prod" in action_source
    assert "上线已验证发布包" in action_source
    assert "script/deploy/promote-int-ruoyi-test-to-prod.ps1" not in action_source
    assert "buildReleaseArguments" in action_source
    assert "deployReleaseArguments" in action_source
    assert "markReleaseTestedArguments" in action_source
    assert 'args.add("-Environment")' in action_source
    assert 'args.add("-ConfirmText")' in action_source
    assert 'args.add("-RequireTested")' in action_source
    assert 'addOptionalArgument(args, "-RemoteMinioContainer", environment.getRemoteMinioContainer())' in action_source
    assert "environment.getRemoteMinioContainer()" in action_source
    assert "requiresNasReleaseRepository" in action_source
    assert "requiresReleaseTag" in action_source
    assert "publishScope" in action_source
    assert "code-only" in action_source
    assert "with-data" in action_source
    assert "-SkipDatabaseSync" in action_source
    assert "-SkipMinioSync" in action_source
    assert "backup-now" in action_source
    assert "script/backup-ops/scripts/backup-ops.ps1" in action_source
    assert "rollback-app" in action_source
    assert "restore-data" in action_source
    assert "-NonInteractive" in action_source
    assert "apply-test-db-sql" in action_source
    assert "测试服数据库快应用" in action_source
    assert "script/deploy/apply-test-db-sql.ps1" in action_source
    assert "applyTestDbSqlArguments" in action_source
    assert 'args.add("-SqlPath")' in action_source
    assert 'appendApplyTestDbSqlTargetArguments(args, properties)' in action_source


def test_test_db_quick_apply_script_has_fail_fast_test_server_gates():
    script_path = REPO_ROOT / "script" / "deploy" / "apply-test-db-sql.ps1"
    script_text = read_text(script_path)

    assert "[string]$ExpectedServerHost = '172.30.30.58'" in script_text
    assert "$ServerHost -ne $ExpectedServerHost" in script_text
    assert "SqlPath must point to a .sql file" in script_text
    assert "SQL file is empty" in script_text
    assert "system_tenant" in script_text
    assert "docker exec -i" in script_text
    assert "--default-character-set=utf8mb4" in script_text
    assert "ruoyi-vue-pro" in script_text
    assert "/actuator/health" in script_text
    assert "mysqldump" not in script_text.lower()
    assert "mc mirror" not in script_text.lower()
    assert "DROP DATABASE" not in script_text
    assert "172.30.30.57" not in script_text
    assert "172.30.30.59" not in script_text


def test_runtime_control_ops_support_linux_local_backup_ops():
    action_source = read_text(RUNTIME_CONTROL_DIR / "RuntimeControlOperationAction.java")
    executor_source = read_text(RUNTIME_CONTROL_DIR / "RuntimeControlCommandExecutorImpl.java")
    properties_source = read_text(
        REPO_ROOT
        / "yudao-module-infra"
        / "src"
        / "main"
        / "java"
        / "cn"
        / "iocoder"
        / "yudao"
        / "module"
        / "infra"
        / "framework"
        / "runtimecontrol"
        / "config"
        / "RuntimeControlProperties.java"
    )

    assert "linux-local" in properties_source
    assert "backup-ops-linux.sh" in properties_source
    assert "backup-ops.linux-local.runtime.json" in properties_source
    assert "resolveScriptPath" in action_source
    assert "--mode" in action_source
    assert "--config" in action_source
    assert "--selected-backup-id" in action_source
    assert "--selected-image-tag" in action_source
    assert "rollback-app" in action_source
    assert "--target-environment" in action_source
    assert 'endsWith(".sh")' in executor_source
    assert "bash" in executor_source
    assert "executeDetachedOperation" in executor_source
    assert "docker" in executor_source
    assert "run" in executor_source
    assert "-d" in executor_source
    assert "--network" in executor_source
    assert "host" in executor_source
    assert "markOperationStatus" in executor_source


def test_test_server_compose_mounts_linux_backup_ops_runtime_prerequisites():
    compose_text = read_text(REPO_ROOT / "script" / "deploy" / "int-ruoyi-test" / "docker-compose.yml")
    dockerfile_text = read_text(REPO_ROOT / "script" / "deploy" / "int-ruoyi-test" / "Dockerfile.backend")
    base_dockerfile_text = read_text(REPO_ROOT / "script" / "deploy" / "int-ruoyi-test" / "Dockerfile.backend-base")
    application_local_text = read_text(REPO_ROOT / "yudao-server" / "src" / "main" / "resources" / "application-local.yaml")

    assert "--yudao.runtime-control.repo-root=/opt/intruoyi/ops/backup-ops/linux-native" in compose_text
    assert "--yudao.runtime-control.state-dir=/opt/intruoyi/ops/runtime-control" in compose_text
    assert "--yudao.runtime-control.backup-ops.execution-mode=linux-local" in compose_text
    assert "/var/run/docker.sock:/var/run/docker.sock" in compose_text
    assert "/opt/intruoyi/runtime:/opt/intruoyi/runtime" in compose_text
    assert "/opt/intruoyi/ops:/opt/intruoyi/ops" in compose_text
    assert "/backup:/backup" in compose_text
    assert "--yudao.runtime-control.release-package.backend-runtime-base-mode=${RUNTIME_CONTROL_BACKEND_RUNTIME_BASE_MODE:-}" in compose_text
    assert "--yudao.runtime-control.release-package.backend-runtime-base-tar-path=${RUNTIME_CONTROL_BACKEND_RUNTIME_BASE_TAR:-}" in compose_text
    assert "--yudao.runtime-control.release-package.backend-runtime-base-tar-sha256=${RUNTIME_CONTROL_BACKEND_RUNTIME_BASE_TAR_SHA256:-}" in compose_text
    assert "--yudao.runtime-control.release-package.backend-runtime-base-image=${RUNTIME_CONTROL_BACKEND_RUNTIME_BASE_IMAGE:-}" in compose_text
    assert "--yudao.runtime-control.release-package.backend-runtime-base-digest=${RUNTIME_CONTROL_BACKEND_RUNTIME_BASE_DIGEST:-}" in compose_text
    assert "--yudao.runtime-control.release-package.backend-runtime-base-version=${RUNTIME_CONTROL_BACKEND_RUNTIME_BASE_VERSION:-}" in compose_text
    assert "--yudao.runtime-control.environments.test.host=${RUNTIME_CONTROL_TEST_SERVER_HOST:-}" in compose_text
    assert "--yudao.runtime-control.environments.prod.host=${RUNTIME_CONTROL_PROD_SERVER_HOST:-}" in compose_text
    assert "--yudao.runtime-control.environments.backup.host=${RUNTIME_CONTROL_BACKUP_SERVER_HOST:-}" in compose_text
    assert "backend-runtime-base-mode: ${INTRUOYI_BACKEND_RUNTIME_BASE_MODE:}" in application_local_text
    assert "backend-runtime-base-tar-path: ${INTRUOYI_BACKEND_RUNTIME_BASE_TAR:}" in application_local_text
    assert "backend-runtime-base-tar-sha256: ${INTRUOYI_BACKEND_RUNTIME_BASE_TAR_SHA256:}" in application_local_text
    assert "backend-runtime-base-image: ${INTRUOYI_BACKEND_RUNTIME_BASE_IMAGE:}" in application_local_text
    assert "backend-runtime-base-digest: ${INTRUOYI_BACKEND_RUNTIME_BASE_DIGEST:}" in application_local_text
    assert "backend-runtime-base-version: ${INTRUOYI_BACKEND_RUNTIME_BASE_VERSION:}" in application_local_text
    assert "host: ${INTRUOYI_RUNTIME_CONTROL_TEST_HOST:" in application_local_text
    assert "host: ${INTRUOYI_RUNTIME_CONTROL_PROD_HOST:" in application_local_text
    assert "host: ${INTRUOYI_RUNTIME_CONTROL_BACKUP_HOST:" in application_local_text
    assert "python3" not in dockerfile_text
    assert "docker.io" not in dockerfile_text
    assert "docker-compose-v2" not in dockerfile_text
    assert "python3" in base_dockerfile_text
    assert "docker.io" in base_dockerfile_text
    assert "docker-compose-v2" in base_dockerfile_text


def test_runtime_control_ops_menu_permission_is_declared():
    menu_sql = read_text(REPO_ROOT / "sql" / "mysql" / "20260523_infra_runtime_control_menu.sql")

    assert "infra:runtime-control:operate" in menu_sql
    assert "运行控制台运维操作" in menu_sql


def test_runtime_control_ops_menu_reuses_existing_runtime_control_parent():
    menu_sql = read_text(REPO_ROOT / "sql" / "mysql" / "20260523_infra_runtime_control_menu.sql")

    assert "@runtime_control_menu_id" in menu_sql
    assert "path` = 'runtime-control'" in menu_sql
    assert "component` = 'infra/runtime-control/index'" in menu_sql
    assert "parent_id`, `path`" in menu_sql
    assert "@runtime_control_menu_id, '', '', '', ''" in menu_sql
