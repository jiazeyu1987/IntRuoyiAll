package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeControlBackupPublicationReceipt;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeControlService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** The durable acceptance decision precedes every remote ACK; no method here deploys application data. */
@Service
public class ReleaseWorkflowBackupFinalizationService {
    private final RuntimeControlProperties properties;
    private final ReleaseWorkflowService workflows;
    private final RuntimeControlService runtime;
    private final ObjectMapper mapper = new ObjectMapper();

    public ReleaseWorkflowBackupFinalizationService(RuntimeControlProperties properties,
            ReleaseWorkflowService workflows, RuntimeControlService runtime) {
        this.properties = properties;
        this.workflows = workflows;
        this.runtime = runtime;
    }

    /** Empty means no receipt exists, never successful deployment or permission to release an owner. */
    public Optional<ReleaseWorkflowRecord> reconcile(String workflowId) {
        ReleaseWorkflowRecord workflow = workflows.require(workflowId);
        requireCandidate(workflow);
        if (workflow.state() == ReleaseWorkflowRecord.State.BACKUP_DEPLOYED) return Optional.of(workflow);
        String leaseName = "ack-" + ReleaseWorkflowBackupAuthorizationService.digest(workflowId).substring(0, 16);
        try (var lease = workflows.acquireEnvironmentLease(leaseName, workflowId)) {
            if (!lease.acquired()) return Optional.of(workflows.require(workflowId));
            workflow = workflows.require(workflowId);
            requireCandidate(workflow);
            if (workflow.state() == ReleaseWorkflowRecord.State.BACKUP_DEPLOYED) return Optional.of(workflow);
            Path decisionPath = decisionPath(workflowId);
            AcceptedDecision decision;
            if (Files.exists(decisionPath)) {
                decision = read(decisionPath, AcceptedDecision.class);
                decision.verifyFor(workflow);
            } else {
                if (workflow.state() == ReleaseWorkflowRecord.State.BACKUP_FINALIZING) {
                    throw new IllegalStateException("RELEASE_WORKFLOW_ACCEPTANCE_DECISION_MISSING");
                }
                var receipt = runtime.inspectBackupReceipt(workflow);
                if (receipt.isEmpty()) return Optional.empty();
                receipt.get().verifyFor(workflow);
                decision = AcceptedDecision.accept(workflow, receipt.get());
                publishImmutable(decisionPath, decision);
            }
            // Read from disk again, validate the complete identity, then derive the ACK digest from those exact bytes.
            byte[] committed = readBytes(decisionPath);
            AcceptedDecision verified = decode(committed, AcceptedDecision.class);
            verified.verifyFor(workflow);
            if (!verified.equals(decision)) throw new IllegalStateException("RELEASE_WORKFLOW_ACCEPTANCE_DECISION_CHANGED");
            String decisionDigest = RuntimeControlBackupPublicationReceipt.sha256(committed);
            if (workflow.state() != ReleaseWorkflowRecord.State.BACKUP_FINALIZING) {
                workflow = workflows.verifyAdvance(workflowId, workflow.stateVersion(),
                        ReleaseWorkflowRecord.State.BACKUP_FINALIZING, "BACKUP_FINALIZING", true, false,
                        List.of(decisionPath.toString(), "receipt-sha256:" + decision.receipt().receiptDigest()));
            }
            try {
                Path confirmationPath = confirmationPath(workflowId);
                RuntimeControlBackupPublicationReceipt confirmed;
                if (Files.exists(confirmationPath)) {
                    // This file is written only after a successful ACK response, not from an inspect result.
                    confirmed = read(confirmationPath, RuntimeControlBackupPublicationReceipt.class);
                } else {
                    confirmed = runtime.acknowledgeBackupReceipt(workflow, decision.receipt(), decisionDigest);
                    verifyConfirmation(workflow, decision, decisionDigest, confirmed);
                    publishImmutable(confirmationPath, confirmed);
                }
                confirmed = read(confirmationPath, RuntimeControlBackupPublicationReceipt.class);
                verifyConfirmation(workflow, decision, decisionDigest, confirmed);
                runtime.completeBackupConfirmation(workflow, confirmed);
                ReleaseWorkflowRecord current = workflows.require(workflowId);
                decision.verifyFor(current);
                if (current.state() == ReleaseWorkflowRecord.State.BACKUP_DEPLOYED) return Optional.of(current);
                return Optional.of(workflows.verifyAdvance(workflowId, current.stateVersion(),
                        ReleaseWorkflowRecord.State.BACKUP_DEPLOYED, "BACKUP_DEPLOYED", true, false,
                        List.of(decisionPath.toString(), confirmationPath.toString())));
            } catch (RuntimeException error) {
                ReleaseWorkflowRecord current = workflows.require(workflowId);
                if (current.state() == ReleaseWorkflowRecord.State.BACKUP_FINALIZING) {
                    var matcher = java.util.regex.Pattern.compile("^[A-Z][A-Z0-9_]{2,100}")
                            .matcher(String.valueOf(error.getMessage()));
                    String code = matcher.find() ? matcher.group() : "ACK_RESULT_UNCONFIRMED";
                    return Optional.of(workflows.verifyAdvance(workflowId, current.stateVersion(),
                            ReleaseWorkflowRecord.State.BACKUP_FINALIZING, "BACKUP_FINALIZING", false, false,
                            List.of(decisionPath.toString(), "failure:" + code)));
                }
                throw error;
            }
        }
    }

    public boolean hasAcceptedDecision(String workflowId) {
        Path path = decisionPath(workflowId);
        if (!Files.exists(path)) return false;
        read(path, AcceptedDecision.class).verifyFor(workflows.require(workflowId));
        return true;
    }

    private static void verifyConfirmation(ReleaseWorkflowRecord workflow, AcceptedDecision decision,
            String decisionDigest, RuntimeControlBackupPublicationReceipt receipt) {
        if (receipt == null) throw new IllegalStateException("RELEASE_WORKFLOW_ACK_RESPONSE_MISSING");
        receipt.verifyFor(workflow);
        if (!"CONFIRMED".equals(receipt.state()) || !decision.receipt().sameImmutableReceipt(receipt)
                || !decisionDigest.equals(receipt.confirmationDecisionDigest())) {
            throw new IllegalStateException("RELEASE_WORKFLOW_ACK_CONFIRMATION_MISMATCH");
        }
    }

    private static void requireCandidate(ReleaseWorkflowRecord workflow) {
        if (workflow.backupIntent() == null || workflow.operationId() == null
                || !(workflow.state() == ReleaseWorkflowRecord.State.BACKUP_DEPLOYING
                    || workflow.state() == ReleaseWorkflowRecord.State.BACKUP_FINALIZING
                    || workflow.state() == ReleaseWorkflowRecord.State.RECOVERY_REQUIRED
                    || workflow.state() == ReleaseWorkflowRecord.State.BACKUP_DEPLOYED)) {
            throw new IllegalStateException("RELEASE_WORKFLOW_FINALIZATION_STATE_INVALID");
        }
    }

    private Path decisionPath(String id) { return path(id, ".decision.json"); }
    private Path confirmationPath(String id) { return path(id, ".confirmation.json"); }
    private Path path(String id, String suffix) {
        if (id == null || !id.matches("rw-backup-[a-z0-9]{8,32}")) throw new IllegalArgumentException("RELEASE_WORKFLOW_ID_INVALID");
        return Path.of(properties.getStateDir()).toAbsolutePath().normalize().resolve("backup-finalization").resolve(id + suffix);
    }

    /** The caller holds the per-workflow OS lease. Existing decisions are immutable, including on replay. */
    private void publishImmutable(Path path, Object value) {
        try {
            byte[] bytes = mapper.writeValueAsBytes(value);
            if (Files.exists(path)) {
                if (!Arrays.equals(bytes, readBytes(path))) throw new IllegalStateException("RELEASE_WORKFLOW_FINALIZATION_EVIDENCE_CONFLICT");
                return;
            }
            Files.createDirectories(path.getParent());
            Path temporary = Files.createTempFile(path.getParent(), path.getFileName().toString(), ".tmp");
            try (var channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                var buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE);
            if (!Arrays.equals(bytes, readBytes(path))) throw new IllegalStateException("RELEASE_WORKFLOW_FINALIZATION_EVIDENCE_READBACK_FAILED");
        } catch (IOException error) {
            throw new IllegalStateException("RELEASE_WORKFLOW_FINALIZATION_EVIDENCE_WRITE_FAILED", error);
        }
    }

    private byte[] readBytes(Path path) {
        try {
            if (!Files.isRegularFile(path) || Files.size(path) > 1024 * 1024) throw new IllegalStateException("RELEASE_WORKFLOW_FINALIZATION_EVIDENCE_INVALID");
            return Files.readAllBytes(path);
        } catch (IOException error) { throw new IllegalStateException("RELEASE_WORKFLOW_FINALIZATION_EVIDENCE_READ_FAILED", error); }
    }
    private <T> T read(Path path, Class<T> type) { return decode(readBytes(path), type); }
    private <T> T decode(byte[] bytes, Class<T> type) {
        try { return mapper.readValue(bytes, type); }
        catch (IOException error) { throw new IllegalStateException("RELEASE_WORKFLOW_FINALIZATION_EVIDENCE_INVALID", error); }
    }

    public record AcceptedDecision(int schemaVersion, String decision, String workflowId, String operationId,
            String releaseTag, String sourceSelectionId, String maintenanceCommit, String applicationCommit,
            String frontendCommit, RuntimeControlBackupPublicationReceipt receipt) {
        static AcceptedDecision accept(ReleaseWorkflowRecord workflow, RuntimeControlBackupPublicationReceipt receipt) {
            return new AcceptedDecision(1, "ACCEPTED", workflow.workflowId(), workflow.operationId(), workflow.releaseTag(),
                    workflow.sourceSelectionId(), workflow.maintenanceCommit(), workflow.applicationCommit(),
                    workflow.frontendCommit(), receipt.awaitingConfirmation());
        }
        void verifyFor(ReleaseWorkflowRecord workflow) {
            if (schemaVersion != 1 || !"ACCEPTED".equals(decision) || !Objects.equals(workflowId, workflow.workflowId())
                    || !Objects.equals(operationId, workflow.operationId()) || !Objects.equals(releaseTag, workflow.releaseTag())
                    || !Objects.equals(sourceSelectionId, workflow.sourceSelectionId())
                    || !Objects.equals(maintenanceCommit, workflow.maintenanceCommit())
                    || !Objects.equals(applicationCommit, workflow.applicationCommit())
                    || !Objects.equals(frontendCommit, workflow.frontendCommit()) || receipt == null) {
                throw new IllegalStateException("RELEASE_WORKFLOW_ACCEPTANCE_DECISION_BINDING_INVALID");
            }
            receipt.verifyFor(workflow);
        }
    }
}
