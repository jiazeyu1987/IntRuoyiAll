package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Atomic file-backed workflow and append-only event journal store. */
@Component
public class ReleaseWorkflowStore {

    private final RuntimeControlProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public ReleaseWorkflowStore(RuntimeControlProperties properties) {
        this.properties = properties;
    }

    public synchronized void create(ReleaseWorkflowRecord record) {
        Objects.requireNonNull(record, "record");
        Path path = recordPath(record.workflowId());
        try {
            Files.createDirectories(workflowDir());
            if (Files.exists(path)) {
                throw new DuplicateWorkflowException(record.workflowId());
            }
            ReleaseWorkflowEvent created = new ReleaseWorkflowEvent(
                    1, record.workflowId(), null, record.state(), record.stateVersion(),
                    "server", null, null, false, List.of(), Map.of(), record.createdAt());
            writeAtomic(path, new WorkflowEnvelope(record, List.of(created)));
        } catch (DuplicateWorkflowException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new StoreException("RELEASE_WORKFLOW_STORE_WRITE_FAILED", ex);
        }
    }

    public synchronized ReleaseWorkflowRecord require(String workflowId) {
        return readEnvelope(workflowId).record();
    }

    public synchronized List<ReleaseWorkflowRecord> list() {
        try {
            if (!Files.isDirectory(workflowDir())) {
                return List.of();
            }
            try (var stream = Files.list(workflowDir())) {
                return stream.filter(path -> path.getFileName().toString().endsWith(".json"))
                        .filter(path -> !path.getFileName().toString().endsWith(".tmp"))
                        .sorted(Comparator.comparingLong(this::modifiedAt).reversed())
                        .map(path -> path.getFileName().toString())
                        .map(name -> name.substring(0, name.length() - ".json".length()))
                        .map(this::require)
                        .toList();
            }
        } catch (IOException ex) {
            throw new StoreException("RELEASE_WORKFLOW_STORE_READ_FAILED", ex);
        }
    }

    public synchronized ReleaseWorkflowRecord update(ReleaseWorkflowRecord expected,
                                                      long expectedStateVersion,
                                                      ReleaseWorkflowRecord.State targetState,
                                                      String actor,
                                                      String errorCode,
                                                      String failedStage,
                                                      boolean retryable,
                                                      List<String> evidenceRefs) {
        return update(expected, expectedStateVersion, targetState, actor, errorCode, failedStage,
                retryable, evidenceRefs, expected.zeroWriteEvidence());
    }

    public synchronized ReleaseWorkflowRecord update(ReleaseWorkflowRecord expected,
                                                      long expectedStateVersion,
                                                      ReleaseWorkflowRecord.State targetState,
                                                      String actor,
                                                      String errorCode,
                                                      String failedStage,
                                                      boolean retryable,
                                                      List<String> evidenceRefs,
                                                      boolean zeroWriteEvidence) {
        return update(expected, expectedStateVersion, targetState, actor, errorCode, failedStage,
                retryable, evidenceRefs, zeroWriteEvidence, Map.of());
    }

    public synchronized ReleaseWorkflowRecord update(ReleaseWorkflowRecord expected,
                                                      long expectedStateVersion,
                                                      ReleaseWorkflowRecord.State targetState,
                                                      String actor,
                                                      String errorCode,
                                                      String failedStage,
                                                      boolean retryable,
                                                      List<String> evidenceRefs,
                                                      boolean zeroWriteEvidence,
                                                      Map<String, String> details) {
        WorkflowEnvelope envelope = readEnvelope(expected.workflowId());
        ReleaseWorkflowRecord current = requireVersion(expected, expectedStateVersion, envelope.record());
        if (current.state().isTerminal()) {
            throw new TerminalWorkflowException(current.workflowId(), current.state());
        }
        Instant now = Instant.now();
        ReleaseWorkflowRecord updated = new ReleaseWorkflowRecord(
                current.workflowId(), current.releaseTag(), current.publishScope(), current.presetId(),
                current.presetVersion(), targetState, current.stateVersion() + 1, current.attempt(),
                current.operationId(), errorCode, failedStage, retryable,
                evidenceRefs == null ? List.of() : evidenceRefs, current.packageDigest(), current.manifestDigest(),
                current.testOperationId(), current.testOperationEvidencePath(),
                current.createdAt(), now, now,
                zeroWriteEvidence, current.requestedBy(), current.reason(), current.sourceSelectionId(),
                current.maintenanceCommit(), current.applicationCommit(), current.frontendCommit());
        return persistEvent(envelope, updated, current.state(), targetState, actor, errorCode,
                failedStage, retryable, updated.evidenceRefs(), details, now);
    }

    public synchronized ReleaseWorkflowRecord assignOperation(ReleaseWorkflowRecord expected,
                                                               long expectedStateVersion,
                                                               String operationId,
                                                               String actor) {
        WorkflowEnvelope envelope = readEnvelope(expected.workflowId());
        ReleaseWorkflowRecord current = requireVersion(expected, expectedStateVersion, envelope.record());
        if (current.state().isTerminal()) {
            throw new TerminalWorkflowException(current.workflowId(), current.state());
        }
        if (operationId == null || !operationId.matches("(?:op-)?[a-z0-9-]{8,64}")) {
            throw new IllegalArgumentException("RELEASE_WORKFLOW_OPERATION_ID_INVALID");
        }
        Instant now = Instant.now();
        ReleaseWorkflowRecord updated = new ReleaseWorkflowRecord(
                current.workflowId(), current.releaseTag(), current.publishScope(), current.presetId(),
                current.presetVersion(), current.state(), current.stateVersion() + 1, current.attempt(),
                operationId, current.errorCode(), current.failedStage(), current.retryable(),
                current.evidenceRefs(), current.packageDigest(), current.manifestDigest(),
                current.testOperationId(), current.testOperationEvidencePath(),
                current.createdAt(), now, now, current.zeroWriteEvidence(),
                current.requestedBy(), current.reason(), current.sourceSelectionId(),
                current.maintenanceCommit(), current.applicationCommit(), current.frontendCommit());
        return persistEvent(envelope, updated, current.state(), current.state(), actor, null,
                null, false, List.of(), Map.of(), now);
    }

    public synchronized ReleaseWorkflowRecord touch(ReleaseWorkflowRecord expected, Instant heartbeatAt,
                                                     String actor) {
        WorkflowEnvelope envelope = readEnvelope(expected.workflowId());
        ReleaseWorkflowRecord current = requireVersion(expected, expected.stateVersion(), envelope.record());
        if (current.state().isTerminal()) {
            throw new TerminalWorkflowException(current.workflowId(), current.state());
        }
        Instant now = Instant.now();
        ReleaseWorkflowRecord updated = new ReleaseWorkflowRecord(
                current.workflowId(), current.releaseTag(), current.publishScope(), current.presetId(),
                current.presetVersion(), current.state(), current.stateVersion() + 1, current.attempt(),
                current.operationId(), current.errorCode(), current.failedStage(), current.retryable(),
                current.evidenceRefs(), current.packageDigest(), current.manifestDigest(),
                current.testOperationId(), current.testOperationEvidencePath(),
                current.createdAt(), now, heartbeatAt,
                current.zeroWriteEvidence(), current.requestedBy(), current.reason(), current.sourceSelectionId(),
                current.maintenanceCommit(), current.applicationCommit(), current.frontendCommit());
        return persistEvent(envelope, updated, current.state(), current.state(), actor, null,
                null, false, List.of(), Map.of(), now);
    }

    public synchronized ReleaseWorkflowRecord bindArtifacts(ReleaseWorkflowRecord expected,
                                                             String packageDigest,
                                                             String manifestDigest,
                                                             String actor) {
        WorkflowEnvelope envelope = readEnvelope(expected.workflowId());
        ReleaseWorkflowRecord current = requireVersion(expected, expected.stateVersion(), envelope.record());
        if (current.state().isTerminal()) {
            throw new TerminalWorkflowException(current.workflowId(), current.state());
        }
        Instant now = Instant.now();
        ReleaseWorkflowRecord updated = new ReleaseWorkflowRecord(
                current.workflowId(), current.releaseTag(), current.publishScope(), current.presetId(),
                current.presetVersion(), current.state(), current.stateVersion() + 1, current.attempt(),
                current.operationId(), current.errorCode(), current.failedStage(), current.retryable(),
                current.evidenceRefs(), packageDigest, manifestDigest,
                current.testOperationId(), current.testOperationEvidencePath(), current.createdAt(), now, now,
                current.zeroWriteEvidence(), current.requestedBy(), current.reason(), current.sourceSelectionId(),
                current.maintenanceCommit(), current.applicationCommit(), current.frontendCommit());
        return persistEvent(envelope, updated, current.state(), current.state(), actor, null,
                null, false, List.of("manifest.json"), Map.of(), now);
    }

    public synchronized ReleaseWorkflowRecord bindTestOperation(ReleaseWorkflowRecord expected,
                                                                 String testOperationId,
                                                                 String testOperationEvidencePath,
                                                                 String actor) {
        WorkflowEnvelope envelope = readEnvelope(expected.workflowId());
        ReleaseWorkflowRecord current = requireVersion(expected, expected.stateVersion(), envelope.record());
        Instant now = Instant.now();
        ReleaseWorkflowRecord updated = new ReleaseWorkflowRecord(
                current.workflowId(), current.releaseTag(), current.publishScope(), current.presetId(),
                current.presetVersion(), current.state(), current.stateVersion() + 1, current.attempt(),
                current.operationId(), current.errorCode(), current.failedStage(), current.retryable(),
                current.evidenceRefs(), current.packageDigest(), current.manifestDigest(),
                testOperationId, testOperationEvidencePath, current.createdAt(), now, now,
                current.zeroWriteEvidence(), current.requestedBy(), current.reason(), current.sourceSelectionId(),
                current.maintenanceCommit(), current.applicationCommit(), current.frontendCommit());
        return persistEvent(envelope, updated, current.state(), current.state(), actor, null,
                null, false, List.of(testOperationEvidencePath), Map.of(), now);
    }

    public synchronized List<ReleaseWorkflowEvent> readJournal(String workflowId) {
        return readEnvelope(workflowId).events();
    }

    public Path recordPath(String workflowId) {
        return workflowDir().resolve(safeWorkflowId(workflowId) + ".json");
    }

    private ReleaseWorkflowRecord persistEvent(WorkflowEnvelope envelope,
                                               ReleaseWorkflowRecord updated,
                                               ReleaseWorkflowRecord.State from,
                                               ReleaseWorkflowRecord.State to,
                                               String actor,
                                               String errorCode,
                                               String failedStage,
                                               boolean retryable,
                                               List<String> evidenceRefs,
                                               Map<String, String> details,
                                               Instant occurredAt) {
        List<ReleaseWorkflowEvent> events = new ArrayList<>(envelope.events());
        events.add(new ReleaseWorkflowEvent(events.size() + 1L, updated.workflowId(), from, to,
                updated.stateVersion(), actor, errorCode, failedStage, retryable, evidenceRefs,
                details == null ? Map.of() : Map.copyOf(details), occurredAt));
        try {
            writeAtomic(recordPath(updated.workflowId()), new WorkflowEnvelope(updated, events));
            return updated;
        } catch (IOException ex) {
            throw new StoreException("RELEASE_WORKFLOW_STORE_WRITE_FAILED", ex);
        }
    }

    private WorkflowEnvelope readEnvelope(String workflowId) {
        Path path = recordPath(workflowId);
        if (!Files.isRegularFile(path)) {
            throw new WorkflowNotFoundException(workflowId);
        }
        try {
            WorkflowEnvelope envelope = objectMapper.readValue(path.toFile(), WorkflowEnvelope.class);
            validateEnvelope(workflowId, envelope);
            return envelope;
        } catch (IOException | RuntimeException ex) {
            if (ex instanceof WorkflowNotFoundException notFound) {
                throw notFound;
            }
            if (ex instanceof CorruptStateException corrupt) {
                throw corrupt;
            }
            throw new CorruptStateException(workflowId, ex);
        }
    }

    private static void validateEnvelope(String workflowId, WorkflowEnvelope envelope) {
        if (envelope == null || envelope.record() == null || envelope.events() == null
                || envelope.events().isEmpty()) {
            throw new CorruptStateException(workflowId, new IllegalStateException("workflow envelope incomplete"));
        }
        List<ReleaseWorkflowEvent> events = envelope.events();
        for (int i = 0; i < events.size(); i++) {
            ReleaseWorkflowEvent event = events.get(i);
            if (event.sequence() != i + 1L || !workflowId.equals(event.workflowId())) {
                throw new CorruptStateException(workflowId, new IllegalStateException("event journal sequence invalid"));
            }
        }
        ReleaseWorkflowEvent tail = events.get(events.size() - 1);
        if (tail.stateVersion() != envelope.record().stateVersion()
                || tail.toState() != envelope.record().state()) {
            throw new CorruptStateException(workflowId, new IllegalStateException("record and journal diverged"));
        }
    }

    private static ReleaseWorkflowRecord requireVersion(ReleaseWorkflowRecord expected,
                                                        long expectedStateVersion,
                                                        ReleaseWorkflowRecord current) {
        if (current.stateVersion() != expectedStateVersion
                || current.stateVersion() != expected.stateVersion()) {
            throw new CasConflictException(expected.workflowId(), expectedStateVersion, current.stateVersion());
        }
        return current;
    }

    private Path workflowDir() {
        return Path.of(properties.getStateDir()).normalize().resolve("release-workflows");
    }

    private void writeAtomic(Path path, Object value) throws IOException {
        Files.createDirectories(path.getParent());
        Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), value);
        Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private long modifiedAt(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException ex) {
            throw new StoreException("RELEASE_WORKFLOW_STORE_READ_FAILED", ex);
        }
    }

    private static String safeWorkflowId(String workflowId) {
        if (workflowId == null || !workflowId.matches("rw-[a-z0-9-]{8,64}")) {
            throw new IllegalArgumentException("RELEASE_WORKFLOW_ID_INVALID");
        }
        return workflowId.toLowerCase(Locale.ROOT);
    }

    public record WorkflowEnvelope(ReleaseWorkflowRecord record, List<ReleaseWorkflowEvent> events) {
        @JsonCreator
        public WorkflowEnvelope(@JsonProperty("record") ReleaseWorkflowRecord record,
                                @JsonProperty("events") List<ReleaseWorkflowEvent> events) {
            this.record = record;
            this.events = events == null ? List.of() : List.copyOf(events);
        }
    }

    public static class StoreException extends RuntimeException {
        public StoreException(String message, Throwable cause) { super(message, cause); }
    }

    public static class CorruptStateException extends StoreException {
        public CorruptStateException(String workflowId, Throwable cause) {
            super("RELEASE_WORKFLOW_STATE_CORRUPT: " + workflowId, cause);
        }
    }

    public static class WorkflowNotFoundException extends StoreException {
        public WorkflowNotFoundException(String workflowId) {
            super("RELEASE_WORKFLOW_NOT_FOUND: " + workflowId, null);
        }
    }

    public static class DuplicateWorkflowException extends StoreException {
        public DuplicateWorkflowException(String workflowId) {
            super("RELEASE_WORKFLOW_DUPLICATE: " + workflowId, null);
        }
    }

    public static class CasConflictException extends StoreException {
        public CasConflictException(String workflowId, long expected, long actual) {
            super("RELEASE_WORKFLOW_CAS_CONFLICT: " + workflowId + ": expected=" + expected + ", actual=" + actual, null);
        }
    }

    public static class TerminalWorkflowException extends StoreException {
        public TerminalWorkflowException(String workflowId, ReleaseWorkflowRecord.State state) {
            super("RELEASE_WORKFLOW_TERMINAL: " + workflowId + ": " + state, null);
        }
    }
}
