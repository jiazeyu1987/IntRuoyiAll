from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
BACKEND_ROOT = REPO_ROOT / "yudao-module-infra" / "src" / "main" / "java" / "cn" / "iocoder" / "yudao" / "module" / "infra"
FRONTEND_ROOT = REPO_ROOT.parent / "yudao-ui-admin-vue3"


def read_backend(relative: str) -> str:
    return (BACKEND_ROOT / relative).read_text(encoding="utf-8")


def read_frontend(relative: str) -> str:
    return (FRONTEND_ROOT / relative).read_text(encoding="utf-8")


def test_backend_release_status_endpoint_is_readonly_snapshot() -> None:
    controller = read_backend("controller/admin/runtimecontrol/RuntimeControlController.java")
    service = read_backend("service/runtimecontrol/RuntimeControlService.java")
    impl = read_backend("service/runtimecontrol/RuntimeControlServiceImpl.java")
    vo = read_backend("controller/admin/runtimecontrol/vo/RuntimeControlReleaseStatusRespVO.java")

    assert '@GetMapping("/release-status")' in controller
    assert "getReleaseStatus()" in service
    assert "RuntimeControlReleaseStatusRespVO getReleaseStatus();" in service
    assert "respVO.setReleasePackages(packages)" in impl
    assert "respVO.setTargetStates(overview.getStatuses())" in impl
    assert "respVO.setRecentOperations" in impl
    assert "private List<RuntimeControlReleasePackageRespVO> releasePackages;" in vo
    assert "private Map<String, Map<String, RuntimeControlStatusRespVO>> targetStates;" in vo
    assert "private List<RuntimeControlOperationRespVO> recentOperations;" in vo


def test_frontend_release_status_panel_uses_canonical_api() -> None:
    api = read_frontend("src/api/infra/runtimeControl/index.ts")
    page = read_frontend("src/views/infra/runtime-control/index.vue")

    assert "export interface RuntimeControlReleaseStatusVO" in api
    assert "url: '/infra/runtime-control/release-status'" in api
    assert "const releaseStatus = ref<RuntimeControlApi.RuntimeControlReleaseStatusVO>()" in page
    assert "loadReleaseStatus()" in page
    assert "getRuntimeControlReleaseStatus()" in page
    assert "class=\"release-status-panel\"" in page
    assert "class=\"release-status-table\"" in page
    assert "发布状态" in page
