package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReleaseWorkflowLeaseTest {

    @TempDir
    Path tempDir;

    @Test
    void sameEnvironmentLeaseMustBeExclusiveAndRecoverAfterRelease() {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        ReleaseWorkflowLease lease = new ReleaseWorkflowLease(properties);

        Optional<ReleaseWorkflowLease.Handle> first = lease.tryAcquire("test", "rw-one", Duration.ofMinutes(5));
        Optional<ReleaseWorkflowLease.Handle> second = lease.tryAcquire("test", "rw-two", Duration.ofMinutes(5));
        assertTrue(first.isPresent());
        assertFalse(second.isPresent());

        first.orElseThrow().close();
        assertTrue(lease.tryAcquire("test", "rw-two", Duration.ofMinutes(5)).isPresent());
    }

    @Test
    void duplicateCreateMustReturnExistingActiveWorkflow() {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        ReleaseWorkflowService service = new ReleaseWorkflowService(properties);

        ReleaseWorkflowRecord first = service.create("operator", "same release", "approved-source");
        ReleaseWorkflowRecord second = service.create("operator", "same release", "approved-source");

        assertTrue(first.workflowId().equals(second.workflowId()));
        assertTrue(service.list().size() == 1);
    }
}
