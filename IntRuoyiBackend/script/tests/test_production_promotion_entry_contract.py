from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
INFRA_JAVA = ROOT / "yudao-module-infra/src/main/java/cn/iocoder/yudao/module/infra"
CONTROLLER = INFRA_JAVA / "controller/admin/runtimecontrol/RuntimeControlController.java"
SERVICE = INFRA_JAVA / "service/runtimecontrol/RuntimeControlServiceImpl.java"
PROPERTIES = INFRA_JAVA / "framework/runtimecontrol/config/RuntimeControlProperties.java"
FRONTEND = ROOT.parent / "IntRuoyiFronted/src/views/infra/runtime-control/index.vue"


def test_only_workflow_can_enter_production_promotion() -> None:
    controller = CONTROLLER.read_text(encoding="utf-8")
    service = SERVICE.read_text(encoding="utf-8")
    frontend = FRONTEND.read_text(encoding="utf-8")

    for route in (
        '/release-workflows/{workflowId}/production-preview',
        '/release-workflows/{workflowId}/production-authorization',
        '/release-workflows/{workflowId}/promote-prod',
    ):
        assert route in controller
    assert "rejectLegacyProductionAction(reqVO)" in controller
    assert "action.requiresReleaseWorkflowContext()" in service
    assert "previewRuntimeControlReleaseWorkflowProduction" in frontend
    assert "authorizeRuntimeControlReleaseWorkflowProduction" in frontend
    assert "promoteRuntimeControlReleaseWorkflowProduction" in frontend


def test_workflow_uses_one_configured_maintenance_executor() -> None:
    properties = PROPERTIES.read_text(encoding="utf-8")
    service = SERVICE.read_text(encoding="utf-8")

    assert "getMaintenanceRepoRoot()" in service
    assert "getPublishScriptPath()" in service
    assert "getExpectedPublishScriptSha256()" in service
    assert "publishScriptPath" in properties
    assert "expectedPublishScriptSha256" in properties
    assert "script/deploy/publish-int-ruoyi.ps1" not in service
