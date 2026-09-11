package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReleaseWorkflowStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldPersistRecordAndJournalAndRejectCasConflict() {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        ReleaseWorkflowStore store = new ReleaseWorkflowStore(properties);
        ReleaseWorkflowRecord created = ReleaseWorkflowRecord.newWorkflow(
                "rw-store-test", "release-store-test-app", Instant.parse("2026-09-12T00:00:00Z"),
                "standard-app-release", "1");

        store.create(created);
        ReleaseWorkflowRecord saved = store.update(created, 0,
                ReleaseWorkflowRecord.State.PREFLIGHTING, "verifier:p1", "PREFLIGHT_OK",
                "PREFLIGHTING", true, List.of("evidence/preflight.json"));

        assertEquals(1, saved.stateVersion());
        assertEquals(ReleaseWorkflowRecord.State.PREFLIGHTING, store.require("rw-store-test").state());
        assertEquals(2, store.readJournal("rw-store-test").size());
        assertThrows(ReleaseWorkflowStore.CasConflictException.class,
                () -> store.update(created, 0, ReleaseWorkflowRecord.State.TESTING,
                        "verifier:p1", null, null, false, List.of()));

        ReleaseWorkflowStore reopened = new ReleaseWorkflowStore(properties);
        assertEquals(ReleaseWorkflowRecord.State.PREFLIGHTING, reopened.require("rw-store-test").state());
        assertEquals(2, reopened.readJournal("rw-store-test").size());
    }

    @Test
    void corruptedRecordMustBlockInsteadOfReturningDefault() throws Exception {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        ReleaseWorkflowStore store = new ReleaseWorkflowStore(properties);
        ReleaseWorkflowRecord created = ReleaseWorkflowRecord.newWorkflow(
                "rw-corrupt-test", "release-corrupt-test-app", Instant.now(),
                "standard-app-release", "1");
        store.create(created);
        Files.writeString(store.recordPath("rw-corrupt-test"), "{not-json", java.nio.charset.StandardCharsets.UTF_8);

        assertThrows(ReleaseWorkflowStore.CorruptStateException.class,
                () -> store.require("rw-corrupt-test"));
    }
}
