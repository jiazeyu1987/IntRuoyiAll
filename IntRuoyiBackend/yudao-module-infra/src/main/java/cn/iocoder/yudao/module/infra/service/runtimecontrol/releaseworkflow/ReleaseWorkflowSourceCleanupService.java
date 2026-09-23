package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Set;
import java.util.regex.Pattern;

/** Cleanup evidence is separate from the immutable deployment outcome. */
@Component
public final class ReleaseWorkflowSourceCleanupService {
    private final RuntimeControlProperties properties;
    private final ReleaseWorkflowWorktreeFactory factory;
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public ReleaseWorkflowSourceCleanupService(RuntimeControlProperties properties, ReleaseWorkflowWorktreeFactory factory) {
        this.properties = properties;
        this.factory = factory;
    }

    public CleanupStatus getStatus(String workflowId) {
        Path file = path(workflowId, ".json");
        if (!Files.exists(file)) return new CleanupStatus(workflowId, "PENDING", null, null);
        try {
            CleanupStatus status = mapper.readValue(file.toFile(), CleanupStatus.class);
            if (!workflowId.equals(status.workflowId()) || status.updatedAt() == null
                    || !Set.of("RUNNING", "SUCCEEDED", "FAILED").contains(status.status())) {
                throw new IllegalStateException("SOURCE_CLEANUP_STATUS_INVALID");
            }
            return status;
        } catch (IOException error) {
            throw new IllegalStateException("SOURCE_CLEANUP_STATUS_READ_FAILED", error);
        }
    }

    public CleanupStatus cleanup(ReleaseWorkflowRecord workflow) {
        if (workflow.state() != ReleaseWorkflowRecord.State.BACKUP_DEPLOYED || workflow.backupIntent() == null) {
            throw new IllegalStateException("SOURCE_CLEANUP_VERIFIED_DEPLOYMENT_REQUIRED");
        }
        Path lockPath = path(workflow.workflowId(), ".lock");
        try {
            Files.createDirectories(lockPath.getParent());
            try (FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
                FileLock lease;
                try { lease = channel.tryLock(); }
                catch (OverlappingFileLockException busy) { return getStatus(workflow.workflowId()); }
                if (lease == null) return getStatus(workflow.workflowId());
                try (lease) {
                    CleanupStatus current = getStatus(workflow.workflowId());
                    if ("SUCCEEDED".equals(current.status())) return current;
                    write(new CleanupStatus(workflow.workflowId(), "RUNNING", null, Instant.now()));
                    try {
                        factory.cleanup(workflow);
                        CleanupStatus success = new CleanupStatus(workflow.workflowId(), "SUCCEEDED", null, Instant.now());
                        write(success);
                        return success;
                    } catch (RuntimeException error) {
                        var match = Pattern.compile("\\b[A-Z][A-Z0-9_]{4,}\\b")
                                .matcher(String.valueOf(error.getMessage()));
                        String code = match.find() ? match.group() : "SOURCE_CLEANUP_FAILED";
                        write(new CleanupStatus(workflow.workflowId(), "FAILED", code, Instant.now()));
                        throw error;
                    }
                }
            }
        } catch (IOException error) {
            throw new IllegalStateException("SOURCE_CLEANUP_STATUS_WRITE_FAILED", error);
        }
    }

    private Path path(String workflowId, String suffix) {
        if (workflowId == null || !workflowId.matches("rw-backup-[a-z0-9]{8,32}")) {
            throw new IllegalArgumentException("SOURCE_CLEANUP_WORKFLOW_ID_INVALID");
        }
        return Path.of(properties.getStateDir()).toAbsolutePath().normalize()
                .resolve("source-cleanup").resolve(workflowId + suffix);
    }

    private void write(CleanupStatus status) {
        Path file = path(status.workflowId(), ".json");
        try {
            Path temporary = Files.createTempFile(file.getParent(), status.workflowId(), ".tmp");
            mapper.writeValue(temporary.toFile(), status);
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException error) {
            throw new IllegalStateException("SOURCE_CLEANUP_STATUS_WRITE_FAILED", error);
        }
    }

    public record CleanupStatus(String workflowId, String status, String errorCode, Instant updatedAt) {}
}
