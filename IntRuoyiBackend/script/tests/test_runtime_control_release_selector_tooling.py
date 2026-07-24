from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
INT_RUOYI_ROOT = REPO_ROOT.parent
FRONTEND_RUNTIME_CONTROL = (
    INT_RUOYI_ROOT
    / "yudao-ui-admin-vue3"
    / "src"
    / "views"
    / "infra"
    / "runtime-control"
    / "index.vue"
)
FRONTEND_RUNTIME_API = (
    INT_RUOYI_ROOT / "yudao-ui-admin-vue3" / "src" / "api" / "infra" / "runtimeControl" / "index.ts"
)


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def test_publish_test_operation_uses_release_package_selector():
    vue_text = read_text(FRONTEND_RUNTIME_CONTROL)
    api_text = read_text(FRONTEND_RUNTIME_API)

    assert "operationUsesReleaseTagSelector" in vue_text
    assert "['publish-test', 'promote-prod', 'promote-backup'].includes(action)" in vue_text
    assert "选择 NAS 发布包编号" in vue_text
    assert "RuntimeControlApi.getRuntimeControlReleasePackages()" in vue_text
    assert "releasePackages.value" in vue_text
    assert "/infra/runtime-control/release-packages" in api_text


def test_publish_test_operation_defaults_reason_to_default_backup():
    vue_text = read_text(FRONTEND_RUNTIME_CONTROL)

    assert "DEFAULT_PUBLISH_TEST_REASON = '默认备份'" in vue_text
    assert "action.action === 'publish-test'" in vue_text
    assert "DEFAULT_PUBLISH_TEST_REASON" in vue_text


def test_promote_prod_operation_uses_release_selector_and_default_reason():
    vue_text = read_text(FRONTEND_RUNTIME_CONTROL)

    assert "DEFAULT_PROMOTE_PROD_REASON = '默认发布'" in vue_text
    assert "DEFAULT_PROMOTE_BACKUP_REASON = '默认发布'" in vue_text
    assert "action.action === 'promote-prod'" in vue_text
    assert "action.action === 'promote-backup'" in vue_text
    assert "DEFAULT_PROMOTE_PROD_REASON" in vue_text
    assert "DEFAULT_PROMOTE_BACKUP_REASON" in vue_text
    assert "['publish-test', 'promote-prod', 'promote-backup'].includes(action)" in vue_text


def test_promote_prod_release_selector_marks_test_usage_status():
    vue_text = read_text(FRONTEND_RUNTIME_CONTROL)

    assert "releasePackageUsageClass(item.releaseTag)" in vue_text
    assert "releasePackageUsageText(item.releaseTag)" in vue_text
    assert "current-test-release" in vue_text
    assert "used-test-release" in vue_text
    assert "testCurrentReleaseTag" in vue_text
    assert "testUsedReleaseTags" in vue_text
    assert "operation.action === 'publish-test'" in vue_text
    assert "operation.status === 'succeeded'" in vue_text


def test_runtime_status_header_displays_current_release_package_or_none():
    vue_text = read_text(FRONTEND_RUNTIME_CONTROL)
    api_text = read_text(FRONTEND_RUNTIME_API)

    assert "currentReleaseTag?: string" in api_text
    assert "当前发布包：" in vue_text
    assert "currentReleaseTagText(environment.key)" in vue_text
    assert "currentReleaseTagValue(environment) || '无'" in vue_text
