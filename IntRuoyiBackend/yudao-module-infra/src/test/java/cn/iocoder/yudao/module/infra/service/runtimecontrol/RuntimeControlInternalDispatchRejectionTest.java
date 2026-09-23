package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlActionReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOperationRespVO;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import cn.iocoder.yudao.module.infra.service.file.NasBrowserService;
import cn.iocoder.yudao.module.infra.service.file.NasSettingsService;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RuntimeControlInternalDispatchRejectionTest {
    @TempDir Path tempDir;

    @Test
    void trustedDispatchRecordsPreclaimConfigRejectionWithoutLaunchingExecutor() throws Exception {
        var dispatch = assertDoesNotThrow(() -> RuntimeControlService.class.getMethod("dispatchWorkflowAction",
                RuntimeControlActionReqVO.class, String.class));
        var fixture = fixture();
        assertThrows(InvocationTargetException.class,
                () -> dispatch.invoke(fixture.runtime(), fixture.request(), "operator"));
        var rejected = fixture.operations().findById(fixture.request().getPreassignedOperationId());
        assertNotNull(rejected);
        assertEquals("blocked", rejected.getStatus());
        assertEquals(Boolean.TRUE, rejected.getZeroWriteEvidence());
        verifyNoInteractions(fixture.executor());
    }

    @Test
    void trustedRejectionCannotReplaceAnExistingDispatchClaimWithZeroWrite() throws Exception {
        var dispatch = assertDoesNotThrow(() -> RuntimeControlService.class.getMethod("dispatchWorkflowAction",
                RuntimeControlActionReqVO.class, String.class));
        var fixture = fixture();
        var existing = new RuntimeControlOperationRespVO();
        existing.setOperationId(fixture.request().getPreassignedOperationId());
        existing.setStatus("running");
        existing.setZeroWriteEvidence(false);
        assertTrue(fixture.operations().createIfAbsent(existing));
        Path path = fixture.operations().getOperationPath(existing.getOperationId());
        byte[] before = Files.readAllBytes(path);
        assertThrows(InvocationTargetException.class,
                () -> dispatch.invoke(fixture.runtime(), fixture.request(), "operator"));
        assertArrayEquals(before, Files.readAllBytes(path));
        assertEquals(Boolean.FALSE, fixture.operations().findById(existing.getOperationId()).getZeroWriteEvidence());
        verifyNoInteractions(fixture.executor());
    }

    private Fixture fixture() {
        var properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        properties.getReleaseWorkflow().setProductionWriteEnabled(true);
        var authorization = new ReleaseWorkflowBackupAuthorizationService(properties);
        var preview = authorization.preview("operator", "review publication", "approved-source", "request-12345678");
        var grant = authorization.authorize(preview.previewId(), "operator", "PROD");
        var store = new ReleaseWorkflowStore(properties);
        var record = new ReleaseWorkflowService(properties, store).createBackup(grant);
        record = store.update(record, record.stateVersion(), ReleaseWorkflowRecord.State.PREFLIGHTING,
                "verifier:unit", null, null, false, List.of(), true);
        record = store.assignOperation(record, record.stateVersion(), "op-internal-12345678", "verifier:unit");
        var request = new RuntimeControlActionReqVO();
        request.setAction("build-release");
        request.setTargetEnvironment("backup");
        request.setProdConfirmText("PROD");
        request.setReason(record.reason());
        request.setReleaseWorkflowId(record.workflowId());
        request.setReleaseWorkflowExpectedStateVersion(record.stateVersion());
        request.setPreassignedOperationId(record.operationId());
        request.setReleaseTag(record.releaseTag());
        request.setSourceSelectionId(record.sourceSelectionId());
        request.setReleaseAuthorizationId(grant.authorizationId());
        request.setExpectedMaintenanceCommit(record.maintenanceCommit());
        request.setExpectedApplicationCommit(record.applicationCommit());
        request.setExpectedFrontendCommit(record.frontendCommit());
        request.setPublishScope("app-release");
        request.setIncludeOnlyOffice(false);
        request.setIncludeShowroomBuildPackage(false);
        var operations = new RuntimeControlOperationStore(properties);
        var executor = mock(RuntimeControlCommandExecutor.class);
        var runtime = new RuntimeControlServiceImpl(properties, executor, operations,
                mock(RuntimeOpsResponsibilityService.class), mock(RuntimeOpsCandidateService.class),
                mock(RuntimeControlReleasePackageConfigService.class), mock(NasSettingsService.class), mock(NasBrowserService.class));
        properties.getEnvironments().get("backup").setRemoteDataDiskDevice("/unapproved-device");
        return new Fixture(runtime, operations, executor, request);
    }

    private record Fixture(RuntimeControlServiceImpl runtime, RuntimeControlOperationStore operations,
                           RuntimeControlCommandExecutor executor, RuntimeControlActionReqVO request) { }
}
