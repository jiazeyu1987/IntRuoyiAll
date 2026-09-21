package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReleaseWorkflowProductionPreviewTest {

    @TempDir
    Path tempDir;

    @Test
    void previewBlocksUntilTestedAndProductionWriteIsEnabled() {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        properties.getReleaseWorkflow().setProductionWriteEnabled(false);
        ReleaseWorkflowService workflowService = new ReleaseWorkflowService(properties);
        ReleaseWorkflowProductionPreviewService previewService =
                new ReleaseWorkflowProductionPreviewService(properties);
        ReleaseWorkflowRecord workflow = workflowService.create("preview", "approved-source");

        var preview = previewService.create(workflow, workflow.stateVersion());

        assertFalse(preview.isEligible());
        assertEquals(2, preview.getBlockers().stream()
                .filter(item -> item.startsWith("WORKFLOW_TESTED") || item.startsWith("PRODUCTION_WRITE_ENABLED"))
                .count());
    }

    @Test
    void staleStateVersionIsRejectedBeforePreviewPersistence() {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        ReleaseWorkflowService workflowService = new ReleaseWorkflowService(properties);
        ReleaseWorkflowProductionPreviewService previewService =
                new ReleaseWorkflowProductionPreviewService(properties);
        ReleaseWorkflowRecord workflow = workflowService.create("preview-stale", "approved-source");

        assertThrows(IllegalStateException.class,
                () -> previewService.create(workflow, workflow.stateVersion() + 1));
    }

    @Test
    void productionTargetChangeInvalidatesPreviouslyEligiblePreview() {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        properties.getEnvironments().get("prod").setAccessEnabled(true);
        properties.getReleaseWorkflow().setProductionWriteEnabled(true);
        ReleaseWorkflowService workflowService = new ReleaseWorkflowService(properties);
        ReleaseWorkflowProductionPreviewService previewService =
                new ReleaseWorkflowProductionPreviewService(properties);
        ReleaseWorkflowRecord workflow = workflowService.create("preview-target-binding", "approved-source");
        workflow = workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                ReleaseWorkflowRecord.State.PREFLIGHTING, "PREFLIGHTING", true, true);
        workflow = workflowService.bindArtifacts(workflow.workflowId(), workflow.stateVersion(),
                "a".repeat(64), "b".repeat(64));
        workflow = workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                ReleaseWorkflowRecord.State.TESTING, "TESTING", true, true);
        workflow = workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                ReleaseWorkflowRecord.State.BUILDING, "BUILDING", true, true);
        workflow = workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                ReleaseWorkflowRecord.State.READY, "READY", true, true);
        workflow = workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                ReleaseWorkflowRecord.State.TEST_DEPLOYING, "TEST_DEPLOYING", true, false);
        workflow = workflowService.bindTestOperation(workflow.workflowId(), workflow.stateVersion(),
                "op-test-12345678", "test/operation.json");
        workflow = workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                ReleaseWorkflowRecord.State.TEST_DEPLOYED, "TEST_DEPLOYED", true, false);
        workflow = workflowService.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                ReleaseWorkflowRecord.State.TESTED, "TESTED", true, false);
        var preview = previewService.create(workflow, workflow.stateVersion());
        assertTrue(preview.isEligible());

        properties.getEnvironments().get("prod").setHost("198.51.100.57");
        ReleaseWorkflowRecord tested = workflow;
        assertThrows(IllegalStateException.class,
                () -> previewService.requireBinding(preview.getPreviewId(), tested, tested.stateVersion()));
    }

    @Test
    void productionPreviewBlocksWhenEnvironmentAccessIsDisabled() {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        properties.getReleaseWorkflow().setProductionWriteEnabled(true);
        ReleaseWorkflowService workflowService = new ReleaseWorkflowService(properties);
        ReleaseWorkflowProductionPreviewService previewService =
                new ReleaseWorkflowProductionPreviewService(properties);
        ReleaseWorkflowRecord workflow = workflowService.create("preview-target-disabled", "approved-source");

        var preview = previewService.create(workflow, workflow.stateVersion());

        assertTrue(preview.getBlockers().stream().anyMatch(item -> item.startsWith("PRODUCTION_ACCESS_ENABLED")));
    }
}
