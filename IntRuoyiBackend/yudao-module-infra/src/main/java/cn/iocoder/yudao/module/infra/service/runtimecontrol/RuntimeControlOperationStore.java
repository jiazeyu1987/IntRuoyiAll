package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOperationRespVO;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.FileAlreadyExistsException;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.RUNTIME_CONTROL_OPERATION_STORE_FAILED;

@Component
public class RuntimeControlOperationStore {

    private static final Object[] OPERATION_LOCKS = java.util.stream.IntStream.range(0, 64)
            .mapToObj(index -> new Object()).toArray();

    private final RuntimeControlProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public RuntimeControlOperationStore(RuntimeControlProperties properties) {
        this.properties = properties;
    }

    public Path getOperationPath(String operationId) {
        return stateDir().resolve(operationId + ".json");
    }

    public Path getOperationLogPath(String operationId) {
        return stateDir().resolve("logs").resolve(operationId + ".log");
    }

    public void initializeLog(Path logPath) {
        try {
            Files.createDirectories(logPath.getParent());
            if (!Files.exists(logPath)) {
                Files.createFile(logPath);
            }
        } catch (IOException ex) {
            throw exception(RUNTIME_CONTROL_OPERATION_STORE_FAILED, ex.getMessage());
        }
    }

    public void save(RuntimeControlOperationRespVO operation) {
        try {
            Files.createDirectories(stateDir());
            Path operationPath = getOperationPath(operation.getOperationId());
            Path tmpPath = stateDir().resolve(operation.getOperationId() + ".json.tmp");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(tmpPath.toFile(), operation);
            Files.move(tmpPath, operationPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ex) {
            throw exception(RUNTIME_CONTROL_OPERATION_STORE_FAILED, ex.getMessage());
        }
    }

    /**
     * Permanent dispatch claim, exclusive even across store instances and JVMs.
     * A crash/IO failure after creation deliberately leaves the journal in place:
     * an unreadable claim requires recovery and must never trigger redispatch.
     */
    public boolean createIfAbsent(RuntimeControlOperationRespVO operation) {
        try {
            Files.createDirectories(stateDir());
            byte[] bytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(operation);
            try (FileChannel channel = FileChannel.open(getOperationPath(operation.getOperationId()),
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
                channel.force(true);
            } catch (FileAlreadyExistsException ex) {
                return false;
            }
            return true;
        } catch (IOException ex) {
            throw exception(RUNTIME_CONTROL_OPERATION_STORE_FAILED, ex.getMessage());
        }
    }

    public List<RuntimeControlOperationRespVO> listLatest(int limit) {
        try {
            if (!Files.isDirectory(stateDir())) {
                return List.of();
            }
            return Files.list(stateDir())
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing((Path path) -> path.toFile().lastModified()).reversed())
                    .limit(limit)
                    .map(this::read)
                    .toList();
        } catch (IOException ex) {
            throw exception(RUNTIME_CONTROL_OPERATION_STORE_FAILED, ex.getMessage());
        }
    }

    public RuntimeControlOperationRespVO findLatest(String environment, String component) {
        return listLatest(200).stream()
                .filter(operation -> environment.equals(operation.getEnvironment())
                        && component.equals(operation.getComponent()))
                .findFirst()
                .orElse(null);
    }

    /** Isolation decisions must not use the bounded, presentation-only history window. */
    public RuntimeControlOperationRespVO findUnknownOperation(String environment) {
        if (!Files.isDirectory(stateDir())) {
            return null;
        }
        try (var records = Files.list(stateDir())) {
            return records.filter(path -> path.getFileName().toString().endsWith(".json"))
                    .map(this::read)
                    .filter(operation -> environment.equals(operation.getEnvironment()))
                    .filter(operation -> "unknown".equals(operation.getStatus()))
                    .findFirst().orElse(null);
        } catch (IOException ex) {
            throw exception(RUNTIME_CONTROL_OPERATION_STORE_FAILED, ex.getMessage());
        }
    }

    public RuntimeControlOperationRespVO findById(String operationId) {
        Path path = getOperationPath(operationId);
        return Files.isRegularFile(path) ? read(path) : null;
    }

    public Path getStateDir() {
        return stateDir();
    }

    public void updateStatus(String operationId, String status, String summary) {
        withOperationLock(operationId, () -> {
            RuntimeControlOperationRespVO operation = requireOperation(operationId);
            requireOrdinaryStatusUpdate(operation, status);
            operation.setStatus(status);
            operation.setSummary(summary);
            save(operation);
            return null;
        });
    }

    public void updateResult(String operationId, String status, String summary, boolean zeroWriteEvidence) {
        withOperationLock(operationId, () -> {
            RuntimeControlOperationRespVO operation = requireOperation(operationId);
            requireOrdinaryStatusUpdate(operation, status);
            operation.setStatus(status);
            operation.setSummary(summary);
            operation.setZeroWriteEvidence(zeroWriteEvidence);
            save(operation);
            return null;
        });
    }

    public Path backupReceiptPath(String operationId) {
        requireSafeOperationId(operationId);
        return stateDir().resolve("backup-receipts").resolve(operationId + ".json");
    }

    public Optional<RuntimeControlBackupPublicationReceipt> findBackupReceipt(String operationId) {
        Path path = backupReceiptPath(operationId);
        if (!Files.exists(path)) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(path.toFile(), RuntimeControlBackupPublicationReceipt.class));
        } catch (IOException ex) {
            throw new IllegalStateException("BACKUP_RECEIPT_STORE_READ_FAILED", ex);
        }
    }

    public RuntimeControlBackupPublicationReceipt archiveBackupReceipt(String operationId,
            RuntimeControlBackupPublicationReceipt receipt, String executionError) {
        return withOperationLock(operationId, () -> {
            RuntimeControlOperationRespVO operation = requireOperation(operationId);
            if (!"publish-backup".equals(operation.getAction()) || !"backup".equals(operation.getEnvironment())
                    || receipt.binding() == null || !operationId.equals(receipt.binding().operationId())) {
                throw new IllegalArgumentException("BACKUP_RECEIPT_OPERATION_MISMATCH");
            }
            RuntimeControlBackupPublicationReceipt previous = findBackupReceipt(operationId).orElse(null);
            if (previous != null && (!previous.sameImmutableReceipt(receipt)
                    || "CONFIRMED".equals(previous.state()) && "CONFIRMED".equals(receipt.state())
                    && !java.util.Objects.equals(previous.confirmationDecisionDigest(), receipt.confirmationDecisionDigest()))) {
                throw new IllegalStateException("BACKUP_RECEIPT_IMMUTABLE_BINDING_DRIFT");
            }
            // A delayed worker may report AWAITING after the finalizer already archived CONFIRMED.
            RuntimeControlBackupPublicationReceipt effective = previous != null && "CONFIRMED".equals(previous.state())
                    ? previous : receipt;
            writeDurableJson(backupReceiptPath(operationId), effective);
            operation.setBackupReceiptDigest(effective.receiptDigest());
            operation.setBackupReceiptEvidencePath(backupReceiptPath(operationId).toAbsolutePath().normalize().toString());
            operation.setBackupConfirmationState(effective.state());
            operation.setZeroWriteEvidence(false);
            if (executionError != null) operation.setExecutionError(executionError);
            if (!"succeeded".equals(operation.getStatus())) {
                if (executionError != null && !"CONFIRMED".equals(effective.state())) {
                    operation.setStatus("failed"); operation.setSummary(executionError);
                } else if (!"failed".equals(operation.getStatus()) && !"unknown".equals(operation.getStatus())
                        && !"canceled".equals(operation.getStatus()) || "CONFIRMED".equals(effective.state())) {
                    operation.setStatus("awaiting-confirmation");
                    operation.setSummary("审查服务器运行态验收凭据已保留，等待正式确认");
                }
            } else if (executionError != null) {
                operation.setSummary(backupOutcomeSummary(operation, "审查服务器已确认发布"));
            }
            writeDurableJson(getOperationPath(operationId), operation);
            return effective;
        });
    }

    public void recordLocalCleanupError(String operationId, String error) {
        withOperationLock(operationId, () -> {
            RuntimeControlOperationRespVO operation = requireOperation(operationId);
            operation.setLocalCleanupError(error);
            operation.setSummary(backupOutcomeSummary(operation,
                    "succeeded".equals(operation.getStatus()) ? "审查服务器已确认发布" : "发布状态未改变"));
            writeDurableJson(getOperationPath(operationId), operation);
            return null;
        });
    }

    void completeBackupPublication(String operationId, RuntimeControlBackupPublicationReceipt confirmed) {
        withOperationLock(operationId, () -> {
            RuntimeControlOperationRespVO operation = requireOperation(operationId);
            RuntimeControlBackupPublicationReceipt stored = findBackupReceipt(operationId)
                    .orElseThrow(() -> new IllegalStateException("BACKUP_CONFIRMED_RECEIPT_REQUIRED"));
            if (!"CONFIRMED".equals(confirmed.state()) || !stored.equals(confirmed)
                    || !"CONFIRMED".equals(operation.getBackupConfirmationState())) {
                throw new IllegalStateException("BACKUP_CONFIRMED_RECEIPT_MISMATCH");
            }
            operation.setStatus("succeeded");
            operation.setSummary(backupOutcomeSummary(operation, "审查服务器已确认发布"));
            operation.setZeroWriteEvidence(false);
            writeDurableJson(getOperationPath(operationId), operation);
            return null;
        });
    }

    private RuntimeControlOperationRespVO requireOperation(String operationId) {
        RuntimeControlOperationRespVO operation = findById(operationId);
        if (operation == null) throw exception(RUNTIME_CONTROL_OPERATION_STORE_FAILED, "operation not found: " + operationId);
        return operation;
    }

    private String backupOutcomeSummary(RuntimeControlOperationRespVO operation, String base) {
        return base + (operation.getExecutionError() == null ? "" : "；执行器后续错误：" + operation.getExecutionError())
                + (operation.getLocalCleanupError() == null ? "" : "；本地配置清理失败：" + operation.getLocalCleanupError());
    }

    private void requireOrdinaryStatusUpdate(RuntimeControlOperationRespVO operation, String status) {
        if ("publish-backup".equals(operation.getAction()) && "succeeded".equals(status)) {
            throw new IllegalStateException("BACKUP_FINALIZATION_API_REQUIRED");
        }
        if ("CONFIRMED".equals(operation.getBackupConfirmationState()) && "succeeded".equals(operation.getStatus())
                && !"succeeded".equals(status)) {
            throw new IllegalStateException("BACKUP_CONFIRMED_OPERATION_CANNOT_REGRESS");
        }
    }

    private void writeDurableJson(Path path, Object value) {
        try {
            Files.createDirectories(path.getParent());
            Path pending = Files.createTempFile(path.getParent(), path.getFileName().toString(), ".tmp");
            byte[] bytes = objectMapper.writeValueAsBytes(value);
            try (FileChannel file = FileChannel.open(pending, StandardOpenOption.WRITE)) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) file.write(buffer);
                file.force(true);
            }
            Files.move(pending, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("BACKUP_PUBLICATION_EVIDENCE_WRITE_FAILED", ex);
        }
    }

    private <T> T withOperationLock(String operationId, Supplier<T> work) {
        requireSafeOperationId(operationId);
        Path lockPath = stateDir().toAbsolutePath().normalize().resolve(".operation-locks").resolve(operationId + ".lock");
        synchronized (OPERATION_LOCKS[lockPath.hashCode() & 63]) {
            try {
                Files.createDirectories(lockPath.getParent());
                try (FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                     var lock = channel.lock()) {
                    return work.get();
                }
            } catch (IOException ex) {
                throw exception(RUNTIME_CONTROL_OPERATION_STORE_FAILED, ex.getMessage());
            }
        }
    }

    private static void requireSafeOperationId(String operationId) {
        if (operationId == null || !operationId.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,127}")) {
            throw new IllegalArgumentException("OPERATION_ID_INVALID");
        }
    }

    private RuntimeControlOperationRespVO read(Path path) {
        try {
            return objectMapper.readValue(path.toFile(), RuntimeControlOperationRespVO.class);
        } catch (IOException ex) {
            throw exception(RUNTIME_CONTROL_OPERATION_STORE_FAILED, ex.getMessage());
        }
    }

    private Path stateDir() {
        return Path.of(properties.getStateDir()).normalize();
    }
}
