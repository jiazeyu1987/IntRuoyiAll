package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-side one-time production authorization binding. */
@Service
public class ReleaseWorkflowAuthorizationService {

    private static final ConcurrentHashMap<String, Object> JVM_GRANT_LOCKS = new ConcurrentHashMap<>();

    private final RuntimeControlProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public ReleaseWorkflowAuthorizationService(RuntimeControlProperties properties) {
        this.properties = properties;
    }

    public synchronized ReleaseAuthorizationGrant issue(String workflowId, String releaseTag,
                                                         String packageDigest, String manifestDigest,
                                                         String presetId, String presetVersion,
                                                         String approver) {
        Instant issuedAt = Instant.now();
        return issue(workflowId, releaseTag, packageDigest, manifestDigest, presetId, presetVersion,
                approver, issuedAt, issuedAt.plus(Duration.ofMinutes(15)));
    }

    public synchronized ReleaseAuthorizationGrant issue(String workflowId, String releaseTag,
                                                         String packageDigest, String manifestDigest,
                                                         String presetId, String presetVersion,
                                                         String approver, Instant issuedAt,
                                                         Instant validUntil) {
        properties.getReleaseWorkflow().validate();
        if (!properties.getReleaseWorkflow().getPresetId().equals(presetId)
                || !properties.getReleaseWorkflow().getPresetVersion().equals(presetVersion)) {
            throw new AuthorizationException("AUTHORIZATION_PRESET_MISMATCH");
        }
        ReleaseAuthorizationGrant grant = new ReleaseAuthorizationGrant(
                "grant-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16),
                workflowId, releaseTag, packageDigest, manifestDigest, "prod", presetId, presetVersion,
                ReleaseWorkflowContract.PUBLISH_SCOPE, approver, issuedAt, validUntil,
                "nonce-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24), null, null);
        persist(grant);
        return grant;
    }

    public synchronized ReleaseAuthorizationGrant require(String grantId) {
        try {
            Path path = grantPath(grantId);
            if (!Files.isRegularFile(path)) {
                throw new AuthorizationException("AUTHORIZATION_NOT_FOUND");
            }
            return objectMapper.readValue(path.toFile(), ReleaseAuthorizationGrant.class);
        } catch (AuthorizationException ex) {
            throw ex;
        } catch (IOException | RuntimeException ex) {
            throw new AuthorizationException("AUTHORIZATION_STATE_CORRUPT", ex);
        }
    }

    public synchronized Validation preview(String grantId, WorkflowTuple workflow, Instant now) {
        ReleaseAuthorizationGrant grant = require(grantId);
        return validate(grant, workflow, now, false, null);
    }

    public synchronized Validation previewExecution(String grantId, WorkflowTuple workflow,
                                                     String prodConfirmText, Instant now) {
        ReleaseAuthorizationGrant grant = require(grantId);
        return validate(grant, workflow, now, true, prodConfirmText);
    }

    public synchronized ReleaseAuthorizationGrant execute(String grantId, WorkflowTuple workflow,
                                                           String prodConfirmText, Instant now) {
        Object jvmLock = JVM_GRANT_LOCKS.computeIfAbsent(grantId, ignored -> new Object());
        synchronized (jvmLock) {
            Path lockPath = grantPath(grantId).resolveSibling(grantId + ".lock");
            try {
                Files.createDirectories(authorizationDir());
                try (FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE); FileLock ignored = channel.lock()) {
                    ReleaseAuthorizationGrant grant = require(grantId);
                    Validation validation = validate(grant, workflow, now, true, prodConfirmText);
                    if (!validation.valid()) {
                        throw new AuthorizationException(validation.errorCode());
                    }
                    ReleaseAuthorizationGrant consumed = new ReleaseAuthorizationGrant(
                            grant.grantId(), grant.workflowId(), grant.releaseTag(), grant.packageDigest(),
                            grant.manifestDigest(), grant.targetEnvironment(), grant.presetId(), grant.presetVersion(),
                            grant.approvedScope(), grant.approver(), grant.issuedAt(), grant.validUntil(), grant.nonce(),
                            grant.revokedAt(), now);
                    persist(consumed);
                    return consumed;
                }
            } catch (AuthorizationException ex) {
                throw ex;
            } catch (IOException ex) {
                throw new AuthorizationException("AUTHORIZATION_CAS_LOCK_FAILED", ex);
            }
        }
    }

    public synchronized ReleaseAuthorizationGrant revoke(String grantId, Instant now) {
        ReleaseAuthorizationGrant grant = require(grantId);
        if (grant.consumedAt() != null) {
            throw new AuthorizationException("AUTHORIZATION_ALREADY_CONSUMED");
        }
        ReleaseAuthorizationGrant revoked = new ReleaseAuthorizationGrant(
                grant.grantId(), grant.workflowId(), grant.releaseTag(), grant.packageDigest(),
                grant.manifestDigest(), grant.targetEnvironment(), grant.presetId(), grant.presetVersion(),
                grant.approvedScope(), grant.approver(), grant.issuedAt(), grant.validUntil(), grant.nonce(),
                now, grant.consumedAt());
        persist(revoked);
        return revoked;
    }

    private Validation validate(ReleaseAuthorizationGrant grant, WorkflowTuple workflow, Instant now,
                                boolean execute, String prodConfirmText) {
        if (!grant.workflowId().equals(workflow.workflowId())
                || !grant.releaseTag().equals(workflow.releaseTag())
                || !grant.packageDigest().equals(workflow.packageDigest())
                || !grant.manifestDigest().equals(workflow.manifestDigest())
                || !grant.presetId().equals(workflow.presetId())
                || !grant.presetVersion().equals(workflow.presetVersion())
                || !grant.approvedScope().equals(workflow.approvedScope())) {
            return invalid("AUTHORIZATION_BINDING_MISMATCH");
        }
        if (!"prod".equals(workflow.targetEnvironment()) || !"prod".equals(grant.targetEnvironment())) {
            return invalid("AUTHORIZATION_TARGET_INVALID");
        }
        if (!ReleaseWorkflowContract.PUBLISH_SCOPE.equals(grant.approvedScope())) {
            return invalid("AUTHORIZATION_SCOPE_INVALID");
        }
        if (workflow.state() != ReleaseWorkflowRecord.State.TESTED) {
            return invalid("WORKFLOW_NOT_TESTED");
        }
        if (grant.revokedAt() != null) {
            return invalid("AUTHORIZATION_REVOKED");
        }
        if (grant.consumedAt() != null) {
            return invalid("AUTHORIZATION_ALREADY_CONSUMED");
        }
        if (now.isBefore(grant.issuedAt()) || !now.isBefore(grant.validUntil())) {
            return invalid("AUTHORIZATION_EXPIRED");
        }
        if (execute) {
            if (!properties.getReleaseWorkflow().isProductionWriteEnabled()) {
                return invalid("PRODUCTION_WRITE_DISABLED");
            }
            if (!"PROD".equals(prodConfirmText)) {
                return invalid("PROD_CONFIRM_REQUIRED");
            }
        }
        return new Validation(true, null);
    }

    private static Validation invalid(String code) {
        return new Validation(false, code);
    }

    private void persist(ReleaseAuthorizationGrant grant) {
        try {
            Files.createDirectories(authorizationDir());
            writeAtomic(grantPath(grant.grantId()), grant);
        } catch (IOException ex) {
            throw new AuthorizationException("AUTHORIZATION_STORE_WRITE_FAILED", ex);
        }
    }

    private Path grantPath(String grantId) {
        if (grantId == null || !grantId.matches("grant-[a-z0-9]{16}")) {
            throw new AuthorizationException("AUTHORIZATION_ID_INVALID");
        }
        return authorizationDir().resolve(grantId + ".json");
    }

    private Path authorizationDir() {
        return Path.of(properties.getStateDir()).normalize().resolve("release-authorizations");
    }

    private void writeAtomic(Path path, Object value) throws IOException {
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(temporary.toFile(), value);
        Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    public record Validation(boolean valid, String errorCode) { }

    public record WorkflowTuple(String workflowId, String releaseTag, String packageDigest,
                                String manifestDigest, String targetEnvironment, String presetId,
                                String presetVersion, String approvedScope,
                                ReleaseWorkflowRecord.State state) { }

    public static class AuthorizationException extends RuntimeException {
        public AuthorizationException(String message) { super(message); }
        public AuthorizationException(String message, Throwable cause) { super(message, cause); }
    }
}
