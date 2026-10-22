package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReleaseWorkflowSecurityTest {

    @TempDir
    Path tempDir;

    @Test
    void clientCannotAdvanceWorkflowOrSetSuccess() {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        ReleaseWorkflowService service = new ReleaseWorkflowService(properties);
        ReleaseWorkflowRecord workflow = service.create("routine release", "approved-source");

        assertThrows(ReleaseWorkflowService.VerifierRequiredException.class,
                () -> service.advanceFromClient(workflow.workflowId(), workflow.stateVersion(),
                        ReleaseWorkflowRecord.State.COMPLETED));
        assertEquals(ReleaseWorkflowRecord.State.SOURCE_FREEZING,
                service.require(workflow.workflowId()).state());
    }

    @Test
    void clientOwnedPresetAndScopeAreServerConstants() {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        ReleaseWorkflowService service = new ReleaseWorkflowService(properties);
        ReleaseWorkflowRecord workflow = service.create("routine release", "approved-source");

        assertEquals(ReleaseWorkflowContract.PUBLISH_SCOPE, workflow.publishScope());
        assertEquals(properties.getReleaseWorkflow().getPresetId(), workflow.presetId());
        assertEquals(properties.getReleaseWorkflow().getPresetVersion(), workflow.presetVersion());
    }

    @Test
    void invalidPresetBlocksBeforeDispatch() {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        properties.getReleaseWorkflow().setPresetId("../unsafe");
        ReleaseWorkflowService service = new ReleaseWorkflowService(properties);

        assertThrows(IllegalArgumentException.class,
                () -> service.create("routine release", "approved-source"));
        assertEquals(0, service.list().size());
    }

    @Test
    void secretLookingReasonMustBeRejectedBeforePersistence() {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        ReleaseWorkflowService service = new ReleaseWorkflowService(properties);

        assertThrows(IllegalArgumentException.class,
                () -> service.create("token=do-not-store", "approved-source"));
        assertEquals(0, service.list().size());
    }
}
