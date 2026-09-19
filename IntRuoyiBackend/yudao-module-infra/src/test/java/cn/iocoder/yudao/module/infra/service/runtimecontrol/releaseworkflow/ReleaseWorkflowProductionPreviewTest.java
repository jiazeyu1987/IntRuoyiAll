package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
