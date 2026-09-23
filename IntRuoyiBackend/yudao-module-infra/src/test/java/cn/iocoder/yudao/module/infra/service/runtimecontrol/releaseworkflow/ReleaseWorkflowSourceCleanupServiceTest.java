package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Durable status tests; actual Git removal is independently tested by WorktreeFactoryTest. */
class ReleaseWorkflowSourceCleanupServiceTest {
    @TempDir Path temp;

    @Test void successSurvivesReloadAndDoesNotDeleteTwice() throws Exception {
        var factory = mock(ReleaseWorkflowWorktreeFactory.class);
        var properties = RuntimeControlProperties.createDefaultForTests(temp);
        var record = successfulRecord();
        Object service = service(properties, factory);
        assertEquals("PENDING", value(invoke(service, "getStatus", String.class, record.workflowId()), "status"));
        assertEquals("SUCCEEDED", value(invoke(service, "cleanup", ReleaseWorkflowRecord.class, record), "status"));
        Object reloaded = service(properties, factory);
        assertEquals("SUCCEEDED", value(invoke(reloaded, "getStatus", String.class, record.workflowId()), "status"));
        invoke(reloaded, "cleanup", ReleaseWorkflowRecord.class, record);
        verify(factory, times(1)).cleanup(record);
    }

    @Test void failedCleanupRemainsVisibleWithoutChangingDeploymentRecord() throws Exception {
        var factory = mock(ReleaseWorkflowWorktreeFactory.class);
        var record = successfulRecord();
        doThrow(new IllegalStateException("SOURCE_DIRTY")).when(factory).cleanup(record);
        var properties = RuntimeControlProperties.createDefaultForTests(temp);
        Object service = service(properties, factory);
        assertThrows(Exception.class, () -> invoke(service, "cleanup", ReleaseWorkflowRecord.class, record));
        Object status = invoke(service(properties, factory), "getStatus", String.class, record.workflowId());
        assertEquals("FAILED", value(status, "status"));
        assertEquals("SOURCE_DIRTY", value(status, "errorCode"));
        assertEquals(ReleaseWorkflowRecord.State.BACKUP_DEPLOYED, record.state());
    }

    @Test void concurrentServiceInstancesCannotCleanupSameSourcesTogether() throws Exception {
        var factory = mock(ReleaseWorkflowWorktreeFactory.class);
        var record = successfulRecord();
        var properties = RuntimeControlProperties.createDefaultForTests(temp);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        doAnswer(call -> { entered.countDown(); assertTrue(release.await(10, TimeUnit.SECONDS)); return null; })
                .when(factory).cleanup(record);
        Object first = service(properties, factory);
        Object second = service(properties, factory);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<Object> running = executor.submit(() -> invoke(first, "cleanup", ReleaseWorkflowRecord.class, record));
            assertTrue(entered.await(10, TimeUnit.SECONDS));
            assertEquals("RUNNING", value(invoke(second, "cleanup", ReleaseWorkflowRecord.class, record), "status"));
            release.countDown();
            assertEquals("SUCCEEDED", value(running.get(10, TimeUnit.SECONDS), "status"));
            verify(factory, times(1)).cleanup(record);
        } finally { release.countDown(); executor.shutdownNow(); }
    }

    private static Object service(RuntimeControlProperties properties, ReleaseWorkflowWorktreeFactory factory) throws Exception {
        Class<?> type;
        try { type = Class.forName(ReleaseWorkflowSourceCleanupServiceTest.class.getPackageName() + ".ReleaseWorkflowSourceCleanupService"); }
        catch (ClassNotFoundException error) { return fail("Durable source cleanup status is missing", error); }
        return type.getConstructor(RuntimeControlProperties.class, ReleaseWorkflowWorktreeFactory.class).newInstance(properties, factory);
    }

    private static Object invoke(Object service, String name, Class<?> argumentType, Object argument) throws Exception {
        try { return service.getClass().getMethod(name, argumentType).invoke(service, argument); }
        catch (InvocationTargetException error) {
            if (error.getCause() instanceof Exception cause) throw cause;
            throw error;
        }
    }
    private static Object value(Object status, String accessor) throws Exception {
        return status.getClass().getMethod(accessor).invoke(status);
    }
    private static ReleaseWorkflowRecord successfulRecord() {
        Instant now = Instant.now();
        return new ReleaseWorkflowRecord("rw-backup-abcdef123456", "release-review-abcdef123456", "app-release",
                "preset-app-release", "1", ReleaseWorkflowRecord.State.BACKUP_DEPLOYED, 9, 1, "op-abcdef123456",
                null, null, false, List.of("operation/verified"), "a".repeat(64), "b".repeat(64), null, null,
                now, now, now, false, "operator", "publish review", "approved-source", "a".repeat(40),
                "b".repeat(40), "b".repeat(40), new ReleaseWorkflowRecord.BackupIntent("ba-fixture", "bp-fixture",
                    "c".repeat(64), "request-fixture"));
    }
}
