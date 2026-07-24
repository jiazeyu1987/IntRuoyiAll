from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
DEPLOY_ROOT = REPO_ROOT / "script" / "deploy"
COMPOSE_TEMPLATE = DEPLOY_ROOT / "int-ruoyi-test" / "docker-compose.yml"
PUBLISH_SCRIPT = DEPLOY_ROOT / "publish-int-ruoyi.ps1"


SMOKE_FRONTEND_DIR = "/opt/intruoyi/runtime/smoke/yudao-ui-admin-vue3"
SMOKE_NPM_WRAPPER = "/opt/intruoyi/runtime/smoke/bin/npm:/usr/local/bin/npm:ro"
SMOKE_SCRIPT_NAME = "e2e:mes:smart-scheduling-smoke"
SMOKE_NODE_IMAGE = "mcr.microsoft.com/playwright:v1.60.0-noble"
SMOKE_ROUTE_READY_PRODUCT_CODE = "YXN.069.001.1003"
SMOKE_ROUTE_READY_UNIT_NUMBER = "zhi"
SMOKE_LEGACY_ROUTE_MISSING_PRODUCT_CODE = "AW.106.03.08.1007"


def read_compose() -> str:
    return COMPOSE_TEMPLATE.read_text(encoding="utf-8")


def read_publish_script() -> str:
    return PUBLISH_SCRIPT.read_text(encoding="utf-8")


def test_backend_compose_template_exposes_scheduler_smoke_runtime_contract() -> None:
    compose = read_compose()

    assert "YUDAO_MES_SCHEDULER_WORKBENCH_SMOKE_TEST_FRONTEND_DIRECTORY" in compose
    assert "YUDAO_MES_SCHEDULER_WORKBENCH_SMOKE_TEST_SCRIPT_NAME" in compose
    assert "--yudao.mes.scheduler-workbench.smoke-test.frontend-directory=${YUDAO_MES_SCHEDULER_WORKBENCH_SMOKE_TEST_FRONTEND_DIRECTORY}" in compose
    assert "--yudao.mes.scheduler-workbench.smoke-test.script-name=${YUDAO_MES_SCHEDULER_WORKBENCH_SMOKE_TEST_SCRIPT_NAME}" in compose
    assert "MES_SMOKE_BASE_URL: ${MES_SMOKE_BASE_URL}" in compose
    assert "MES_SMOKE_NODE_IMAGE: ${MES_SMOKE_NODE_IMAGE}" in compose
    assert "MES_SMOKE_CAPACITY_MODE: ${MES_SMOKE_CAPACITY_MODE}" in compose
    assert "MES_SMOKE_PRODUCT_CODE: ${MES_SMOKE_PRODUCT_CODE}" in compose
    assert "MES_SMOKE_ERP_CREATOR_TENANT: ${MES_SMOKE_ERP_CREATOR_TENANT}" in compose
    assert "MES_SMOKE_SUPERVISOR_USERNAME: ${MES_SMOKE_SUPERVISOR_USERNAME}" in compose
    assert "MES_SMOKE_FEEDBACK_APPROVER_NAME: ${MES_SMOKE_FEEDBACK_APPROVER_NAME}" in compose
    assert SMOKE_NPM_WRAPPER in compose


def test_publish_script_writes_scheduler_smoke_runtime_env_defaults() -> None:
    script = read_publish_script()

    assert f"$schedulerSmokeFrontendDirectory = '{SMOKE_FRONTEND_DIR}'" in script
    assert f"$schedulerSmokeScriptName = '{SMOKE_SCRIPT_NAME}'" in script
    assert f"$schedulerSmokeNodeImage = '{SMOKE_NODE_IMAGE}'" in script
    assert 'Resolve-ExistingRuntimeEnvValue -Name \'MES_SMOKE_BASE_URL\' -DefaultValue "http://127.0.0.1:$FrontendPort"' in script
    assert 'Resolve-ExistingRuntimeEnvValue -Name \'MES_SMOKE_ARTIFACT_DIR\' -DefaultValue "$schedulerSmokeFrontendDirectory/output/artifacts"' in script
    assert "YUDAO_MES_SCHEDULER_WORKBENCH_SMOKE_TEST_FRONTEND_DIRECTORY=$effectiveSchedulerSmokeFrontendDirectory" in script
    assert "YUDAO_MES_SCHEDULER_WORKBENCH_SMOKE_TEST_SCRIPT_NAME=$effectiveSchedulerSmokeScriptName" in script
    assert "MES_SMOKE_BASE_URL=$effectiveMesSmokeBaseUrl" in script
    assert "MES_SMOKE_NODE_IMAGE=$effectiveMesSmokeNodeImage" in script
    assert "MES_SMOKE_ARTIFACT_DIR=$effectiveMesSmokeArtifactDir" in script
    assert "Resolve-ExistingRuntimeEnvValue -Name 'MES_SMOKE_CAPACITY_MODE' -DefaultValue 'PLANNED'" in script
    assert f"$defaultMesSmokeProductCode = '{SMOKE_ROUTE_READY_PRODUCT_CODE}'" in script
    assert f"$legacyRouteMissingMesSmokeProductCode = '{SMOKE_LEGACY_ROUTE_MISSING_PRODUCT_CODE}'" in script
    assert "Resolve-ExistingRuntimeEnvValue -Name 'MES_SMOKE_PRODUCT_CODE' -DefaultValue $defaultMesSmokeProductCode" in script
    assert "if ($effectiveMesSmokeProductCode -eq $legacyRouteMissingMesSmokeProductCode)" in script
    assert f"$defaultMesSmokeErpUnitNumber = '{SMOKE_ROUTE_READY_UNIT_NUMBER}'" in script
    assert "Resolve-ExistingRuntimeEnvValue -Name 'MES_SMOKE_ERP_UNIT_NUMBER' -DefaultValue $defaultMesSmokeErpUnitNumber" in script
    assert "if ($effectiveMesSmokeProductCode -eq $defaultMesSmokeProductCode -and $effectiveMesSmokeErpUnitNumber -eq 'PCS')" in script
    assert f"DefaultValue '{SMOKE_LEGACY_ROUTE_MISSING_PRODUCT_CODE}'" not in script
    assert "Resolve-ExistingRuntimeEnvValue -Name 'MES_SMOKE_BATCH_NUMBER' -DefaultValue 'TEST-SMOKE-BATCH'" in script
    assert "Resolve-ExistingRuntimeEnvValue -Name 'MES_SMOKE_DEFAULT_PASSWORD' -DefaultValue '111111'" in script
    assert "[System.Convert]::FromBase64String('6IqL6YGT5rqQ56CB')" in script
    assert "$effectiveMesSmokeErpCreatorTenant = $mesSmokeTenantName" in script
    assert "$effectiveMesSmokeErpCreatorUsername = 'messmokeerp'" in script
    assert "$effectiveMesSmokePlannerTenant = $mesSmokeTenantName" in script
    assert "Resolve-ExistingRuntimeEnvValue -Name 'MES_SMOKE_PLANNER_USERNAME' -DefaultValue 'zhaojie'" in script
    assert "$effectiveMesSmokeSupervisorTenant = $mesSmokeTenantName" in script
    assert "$effectiveMesSmokeSupervisorUsername = 'messmokesupervisor'" in script
    assert "$effectiveMesSmokeFeedbackApproverName = $effectiveMesSmokeSupervisorUsername" in script
    assert "MES_SMOKE_FEEDBACK_APPROVER_NAME=$effectiveMesSmokeFeedbackApproverName" in script
    assert "$effectiveMesSmokeNonApproverTenant = $mesSmokeTenantName" in script
    assert "$effectiveMesSmokeNonApproverUsername = 'messmokenonapprover'" in script
    assert "mes_smoke_erp_creator" not in script
    assert "mes_smoke_supervisor" not in script
    assert "mes_smoke_non_approver" not in script


def test_publish_script_prepares_scheduler_smoke_runner_before_backend_restart() -> None:
    script = read_publish_script()

    assert "function New-SchedulerSmokeRunnerPackage" in script
    assert "function Copy-SchedulerSmokeRunnerToServer" in script
    assert "function Assert-RemoteSchedulerSmokeRuntime" in script
    assert "smart-scheduling-smoke-real-flow.e2e.js" in script
    assert "smart-scheduling-smoke-real-flow-static.spec.js" in script
    assert '"xlsx": "0.18.5"' in script
    assert "mcr.microsoft.com/playwright:v1.60.0-noble" in script
    assert "docker run --rm --network host" in script
    assert "'MES_SMOKE_FEEDBACK_APPROVER_NAME'" in script
    assert "rm -rf '$remoteSchedulerSmokeRoot'" not in script
    assert "'$remoteSchedulerSmokeFrontendDir/input'" in script
    copy_call = "if ($publishBackend) { Copy-SchedulerSmokeRunnerToServer }"
    assert copy_call in script
    assert_call = "if ($publishBackend) { Assert-RemoteSchedulerSmokeRuntime }"
    assert assert_call in script
    assert script.index(copy_call) < script.index("docker compose up -d $runtimeServiceDependencyFlag$runtimeServicesArg")
    assert script.index(assert_call) > script.index("Wait-RemoteHttpOk -Url \"http://127.0.0.1:$FrontendPort/\"")
