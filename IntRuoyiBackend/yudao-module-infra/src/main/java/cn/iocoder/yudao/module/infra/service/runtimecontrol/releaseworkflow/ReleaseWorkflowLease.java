package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/** Cross-process lease using an OS file lock in the same state directory. */
public final class ReleaseWorkflowLease {

    private final RuntimeControlProperties properties;

    public ReleaseWorkflowLease(RuntimeControlProperties properties) {
        this.properties = properties;
    }

    public Optional<Handle> tryAcquire(String environment, String workflowId, Duration ttl) {
        if (environment == null || !environment.matches("[a-z][a-z0-9-]{0,31}")) {
            throw new IllegalArgumentException("RELEASE_WORKFLOW_ENVIRONMENT_INVALID");
        }
        if (workflowId == null || workflowId.isBlank() || ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("RELEASE_WORKFLOW_LEASE_ARGUMENT_INVALID");
        }
        Path lockPath = Path.of(properties.getStateDir()).normalize()
                .resolve("release-workflows").resolve("locks").resolve(environment + ".lock");
        try {
            Files.createDirectories(lockPath.getParent());
            FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.READ,
                    StandardOpenOption.WRITE);
            FileLock fileLock;
            try {
                fileLock = channel.tryLock();
            } catch (OverlappingFileLockException ex) {
                channel.close();
                return Optional.empty();
            }
            if (fileLock == null) {
                channel.close();
                return Optional.empty();
            }
            String metadata = "workflowId=" + workflowId + "\nacquiredAt=" + Instant.now()
                    + "\nttlSeconds=" + ttl.toSeconds() + "\n";
            channel.truncate(0);
            channel.write(ByteBuffer.wrap(metadata.getBytes(StandardCharsets.UTF_8)));
            channel.force(true);
            return Optional.of(new Handle(environment, workflowId, channel, fileLock));
        } catch (IOException ex) {
            throw new LeaseException("RELEASE_WORKFLOW_LEASE_IO_FAILED: " + environment, ex);
        }
    }

    public static final class Handle implements AutoCloseable {
        private final String environment;
        private final String workflowId;
        private final FileChannel channel;
        private final FileLock lock;
        private boolean closed;

        private Handle(String environment, String workflowId, FileChannel channel, FileLock lock) {
            this.environment = environment;
            this.workflowId = workflowId;
            this.channel = channel;
            this.lock = lock;
        }

        public String environment() { return environment; }

        public String workflowId() { return workflowId; }

        @Override
        public synchronized void close() {
            if (closed) {
                return;
            }
            try {
                lock.release();
                channel.close();
                closed = true;
            } catch (IOException ex) {
                throw new LeaseException("RELEASE_WORKFLOW_LEASE_RELEASE_FAILED: " + environment, ex);
            }
        }
    }

    public static class LeaseException extends RuntimeException {
        public LeaseException(String message, Throwable cause) { super(message, cause); }
    }
}
