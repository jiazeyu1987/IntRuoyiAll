package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReleaseWorkflowSourceBindingTest {

    @TempDir
    Path tempDir;

    @Test
    void workflowPersistsApprovedExpectedCommits() {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        ReleaseWorkflowService service = new ReleaseWorkflowService(properties);

        ReleaseWorkflowRecord workflow = service.create("operator", "bind approved source", "approved-source");

        assertEquals("a".repeat(40), workflow.maintenanceCommit());
        assertEquals("b".repeat(40), workflow.applicationCommit());
        assertEquals("b".repeat(40), workflow.frontendCommit());
        ReleaseWorkflowService restarted = new ReleaseWorkflowService(properties);
        assertEquals(workflow, restarted.require(workflow.workflowId()));
    }

    @Test
    void missingApprovedCommitBlocksWorkflowCreation() {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        properties.getReleaseWorkflow().setApprovedApplicationCommit("");
        ReleaseWorkflowService service = new ReleaseWorkflowService(properties);

        assertThrows(IllegalArgumentException.class,
                () -> service.create("operator", "missing approved source", "approved-source"));
        assertEquals(0, service.list().size());
    }
}
