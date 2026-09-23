package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlActionReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOperationRespVO;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import cn.iocoder.yudao.module.infra.service.file.NasBrowserService;
import cn.iocoder.yudao.module.infra.service.file.NasSettingsService;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RuntimeControlGeneratedBackupIdentityTest {
    @TempDir Path tempDir;

    @Test
    void generatedAuthorizedWorkflowIdPassesRealRuntimeBindingAndReplaysClaim() throws Exception {
        var properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        properties.getReleaseWorkflow().setProductionWriteEnabled(true);
        var authorization = new ReleaseWorkflowBackupAuthorizationService(properties);
        var preview = authorization.preview("operator", "review publication", "approved-source", "request-12345678");
        var grant = authorization.authorize(preview.previewId(), "operator", "PROD");
        var store = new ReleaseWorkflowStore(properties);
        var created = new ReleaseWorkflowService(properties, store).createBackup(grant);
        assertTrue(created.workflowId().matches("rw-backup-[0-9a-f]{32}"));
        var building = store.update(created, created.stateVersion(), ReleaseWorkflowRecord.State.PREFLIGHTING,
                "verifier:unit", null, null, false, List.of(), true);
        building = store.assignOperation(building, building.stateVersion(), "op-build-12345678", "verifier:unit");
        var request = new RuntimeControlActionReqVO();
        request.setAction("build-release");
        request.setTargetEnvironment("backup");
        request.setProdConfirmText("PROD");
        request.setReason(building.reason());
        request.setPublishScope(building.publishScope());
        request.setIncludeOnlyOffice(false);
        request.setIncludeShowroomBuildPackage(false);
        request.setReleaseTag(building.releaseTag());
        request.setReleaseWorkflowId(building.workflowId());
        request.setReleaseWorkflowExpectedStateVersion(building.stateVersion());
        request.setPreassignedOperationId(building.operationId());
        request.setReleaseAuthorizationId(grant.authorizationId());
        request.setSourceSelectionId(building.sourceSelectionId());
        request.setExpectedMaintenanceCommit(building.maintenanceCommit());
        request.setExpectedApplicationCommit(building.applicationCommit());
        request.setExpectedFrontendCommit(building.frontendCommit());
        var operation = new RuntimeControlOperationRespVO();
        operation.setOperationId(building.operationId());
        operation.setAction("build-release");
        operation.setEnvironment("backup");
        operation.setRequestedBy("operator");
        operation.setReason(building.reason());
        operation.setStatus("running");
        var parameters = new LinkedHashMap<>(RuntimeControlOperationAction.BUILD_RELEASE.safeParameters(request));
        parameters.put("workflowId", building.workflowId());
        parameters.put("authorizationId", grant.authorizationId());
        parameters.put("sourceSelectionId", building.sourceSelectionId());
        parameters.put("maintenanceCommit", building.maintenanceCommit());
        parameters.put("applicationCommit", building.applicationCommit());
        parameters.put("frontendCommit", building.frontendCommit());
        parameters.put("packageDigest", null);
        parameters.put("manifestDigest", null);
        operation.setParameters(parameters);
        var operations = new RuntimeControlOperationStore(properties);
        assertTrue(operations.createIfAbsent(operation));
        var executor = mock(RuntimeControlCommandExecutor.class);
        var nas = mock(NasBrowserService.class);
        var runtime = new RuntimeControlServiceImpl(properties, executor, operations,
                mock(RuntimeOpsResponsibilityService.class), mock(RuntimeOpsCandidateService.class),
                mock(RuntimeControlReleasePackageConfigService.class), mock(NasSettingsService.class), nas);
        assertEquals(operation, runtime.executeAction(request, "operator"));
        var recheck = RuntimeControlServiceImpl.class.getDeclaredMethod("validateIndependentBackupBinding",
                RuntimeControlOperationAction.class, RuntimeControlActionReqVO.class, String.class, boolean.class);
        recheck.setAccessible(true);
        for (var state : List.of(ReleaseWorkflowRecord.State.TESTING, ReleaseWorkflowRecord.State.BUILDING)) {
            building = store.update(building, building.stateVersion(), state, "verifier:unit",
                    null, null, false, List.of(), false);
            assertDoesNotThrow(() -> recheck.invoke(runtime, RuntimeControlOperationAction.BUILD_RELEASE,
                    request, "operator", false));
        }
        store.update(building, building.stateVersion(), ReleaseWorkflowRecord.State.READY,
                "verifier:unit", null, null, false, List.of(), false);
        assertThrows(java.lang.reflect.InvocationTargetException.class, () -> recheck.invoke(runtime,
                RuntimeControlOperationAction.BUILD_RELEASE, request, "operator", false));
        verifyNoInteractions(executor, nas);
    }
}
