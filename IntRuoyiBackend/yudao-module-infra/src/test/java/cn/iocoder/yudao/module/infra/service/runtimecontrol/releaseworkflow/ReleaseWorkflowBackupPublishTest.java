package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class ReleaseWorkflowBackupPublishTest {
    @TempDir Path directory;

    @Test void twoStoreInstancesCannotCommitTheSameVersion() throws Exception {
        var properties = RuntimeControlProperties.createDefaultForTests(directory);
        var first = new ReleaseWorkflowStore(properties);
        var second = new ReleaseWorkflowStore(properties);
        var service = new ReleaseWorkflowService(properties, first);
        var current = service.create("operator", "review release", "approved-source");
        var barrier = new java.util.concurrent.CyclicBarrier(2);
        var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            java.util.List<java.util.concurrent.Callable<Boolean>> calls = java.util.List.of(
                    () -> raceUpdate(first, current, barrier), () -> raceUpdate(second, current, barrier));
            var results = pool.invokeAll(calls);
            assertEquals(1, (results.get(0).get() ? 1 : 0) + (results.get(1).get() ? 1 : 0));
            assertEquals(1, first.require(current.workflowId()).stateVersion());
        } finally { pool.shutdownNow(); }
    }

    private boolean raceUpdate(ReleaseWorkflowStore store, ReleaseWorkflowRecord current,
                               java.util.concurrent.CyclicBarrier barrier) throws Exception {
        barrier.await();
        try {
            store.assignOperation(current, current.stateVersion(), java.util.UUID.randomUUID().toString(), "operator");
            return true;
        } catch (ReleaseWorkflowStore.CasConflictException expected) { return false; }
    }

    @Test void backupWorkflowHasDurableTargetAndDistinctTerminalState() {
        assertDoesNotThrow(() -> ReleaseWorkflowRecord.class.getMethod("backupIntent"));
        assertTrue(ReleaseWorkflowRecord.State.valueOf("BACKUP_DEPLOYED").isTerminal());
        assertTrue(ReleaseWorkflowRecord.State.valueOf("BACKUP_DEPLOYING").isWriteStage());
        var finalizing = assertDoesNotThrow(() -> ReleaseWorkflowRecord.State.valueOf("BACKUP_FINALIZING"));
        assertFalse(finalizing.isTerminal());
        assertTrue(finalizing.isWriteStage());
    }

    @Test void artifactBindingCannotBeReplacedAfterReload() {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(directory);
        ReleaseWorkflowService service = new ReleaseWorkflowService(properties);
        ReleaseWorkflowRecord workflow = service.create("operator", "review release", "approved-source");
        ReleaseWorkflowRecord bound = service.bindArtifacts(workflow.workflowId(), workflow.stateVersion(),
                "a".repeat(64), "b".repeat(64));
        ReleaseWorkflowService reloaded = new ReleaseWorkflowService(properties);
        assertThrows(IllegalStateException.class, () -> reloaded.bindArtifacts(bound.workflowId(), bound.stateVersion(),
                "c".repeat(64), "d".repeat(64)));
        assertEquals("a".repeat(64), reloaded.require(bound.workflowId()).packageDigest());
    }

    @Test void unknownWriteFailureMustRemainIsolated() {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(directory);
        ReleaseWorkflowService service = new ReleaseWorkflowService(properties);
        ReleaseWorkflowRecord workflow = service.create("operator", "review release", "approved-source");
        for (ReleaseWorkflowRecord.State state : new ReleaseWorkflowRecord.State[]{
                ReleaseWorkflowRecord.State.PREFLIGHTING, ReleaseWorkflowRecord.State.TESTING,
                ReleaseWorkflowRecord.State.BUILDING, ReleaseWorkflowRecord.State.READY,
                ReleaseWorkflowRecord.State.TEST_DEPLOYING}) {
            workflow = service.verifyAdvance(workflow.workflowId(), workflow.stateVersion(), state, state.name(), true, true);
        }
        ReleaseWorkflowRecord failed = service.verifyAdvance(workflow.workflowId(), workflow.stateVersion(),
                ReleaseWorkflowRecord.State.FAILED, "DEPLOY", false, false);
        assertEquals(ReleaseWorkflowRecord.State.RECOVERY_REQUIRED, failed.state());
        assertFalse(failed.state().isTerminal());
    }
}
