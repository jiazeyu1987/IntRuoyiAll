package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReleaseWorkflowRecoveryTest {

    @TempDir
    Path tempDir;

    @Test
    void persistedReadyWorkflowRemainsReadyAfterRestart() {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        ReleaseWorkflowService first = new ReleaseWorkflowService(properties);
        ReleaseWorkflowRecord workflow = first.create("routine release", "approved-source");
        workflow = first.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                ReleaseWorkflowRecord.State.PREFLIGHTING, "PREFLIGHTING", true, false);
        workflow = first.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                ReleaseWorkflowRecord.State.TESTING, "TESTING", true, false);
        workflow = first.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                ReleaseWorkflowRecord.State.BUILDING, "BUILDING", true, false);
        workflow = first.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                ReleaseWorkflowRecord.State.READY, "READY", true, false);

        ReleaseWorkflowService restarted = new ReleaseWorkflowService(properties);
        assertEquals(ReleaseWorkflowRecord.State.READY,
                restarted.require(workflow.workflowId()).state());
        assertEquals(workflow.stateVersion(), restarted.require(workflow.workflowId()).stateVersion());
    }

    @Test
    void staleWriteStageRequiresRecoveryAndDoesNotAutoResume() {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        properties.getReleaseWorkflow().setHeartbeatTimeout(Duration.ofSeconds(1));
        ReleaseWorkflowService service = new ReleaseWorkflowService(properties);
        ReleaseWorkflowRecord workflow = service.create("routine release", "approved-source");
        workflow = service.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                ReleaseWorkflowRecord.State.PREFLIGHTING, "PREFLIGHTING", true, false);
        workflow = service.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                ReleaseWorkflowRecord.State.TESTING, "TESTING", true, false);
        workflow = service.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                ReleaseWorkflowRecord.State.BUILDING, "BUILDING", true, false);
        service.overrideHeartbeatForTest(workflow.workflowId(), Instant.now().minusSeconds(10));

        ReleaseWorkflowService restarted = new ReleaseWorkflowService(properties);
        assertEquals(ReleaseWorkflowRecord.State.RECOVERY_REQUIRED,
                restarted.recoverStaleWorkflows(Instant.now()).get(0).state());
    }

    @Test
    void cancelDuringWriteStageMustRequireRecovery() {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        ReleaseWorkflowService service = new ReleaseWorkflowService(properties);
        ReleaseWorkflowRecord workflow = service.create("routine release", "approved-source");
        workflow = service.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                ReleaseWorkflowRecord.State.PREFLIGHTING, "PREFLIGHTING", true, false);
        workflow = service.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                ReleaseWorkflowRecord.State.TESTING, "TESTING", true, false);
        workflow = service.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                ReleaseWorkflowRecord.State.BUILDING, "BUILDING", true, false);

        assertEquals(ReleaseWorkflowRecord.State.RECOVERY_REQUIRED,
                service.cancel(workflow.workflowId()).state());
    }
}
