package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

/** Server-owned review publication authorization contract. */
public class ReleaseWorkflowBackupAuthorizationService {
    private final RuntimeControlProperties properties;
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    public ReleaseWorkflowBackupAuthorizationService(RuntimeControlProperties properties) { this.properties = properties; }

    public static String targetFingerprint(RuntimeControlProperties properties) {
        var target = properties.requireBackupPublishTarget();
        var source = properties.getReleaseWorkflow();
        source.validate();
        source.validateApprovedCommits();
        return digest(String.join("\n", "backup", target.getHost(), target.getServerUser(), target.getRemoteAppDir(),
                target.getRemoteDataRoot(), target.getRemoteReleaseRoot(), target.getRemoteDataDiskMount(),
                target.getRemoteDataDiskDevice(), target.getRemoteMinioContainer(), source.getPresetId(),
                source.getPresetVersion(), source.getApprovedSourceSelectionId(), source.getApprovedMaintenanceCommit(),
                source.getApprovedApplicationCommit(), source.getApprovedFrontendCommit(),
                source.getExpectedPublishScriptSha256(), source.getMaintenanceRepoRoot(), source.getApplicationRepoRoot(),
                Objects.requireNonNull(source.getWorktreeRoot(), "RELEASE_WORKFLOW_WORKTREE_ROOT_REQUIRED"), "app-release",
                Boolean.toString(target.isAccessEnabled()), Boolean.toString(source.isProductionWriteEnabled()),
                properties.getBackupOps().getExecutionMode(),
                Objects.requireNonNull(source.getPnpmVersion(), "RELEASE_WORKFLOW_PNPM_VERSION_REQUIRED"),
                cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeControlRestoreIsolationConfig.resolve(properties),
                new TreeMap<>(target.getTargets()).entrySet().stream()
                        .map(entry -> entry.getKey() + ":" + entry.getValue().getPort() + ":" + entry.getValue().getUrl())
                        .collect(java.util.stream.Collectors.joining("|"))));
    }

    public synchronized Preview preview(String actor, String reason, String sourceSelectionId, String idempotencyKey) {
        if (actor == null || actor.isBlank() || reason == null || reason.isBlank() || reason.length() > 120
                || !reason.equals(reason.trim()) || reason.matches("(?is).*(password|token|secret)\\s*[:=].+")) {
            throw new IllegalArgumentException("RELEASE_WORKFLOW_REQUEST_INVALID");
        }
        if (idempotencyKey == null || !idempotencyKey.matches("[A-Za-z0-9][A-Za-z0-9._:-]{7,127}")) {
            throw new IllegalArgumentException("RELEASE_WORKFLOW_IDEMPOTENCY_KEY_INVALID");
        }
        var source = properties.getReleaseWorkflow();
        if (!source.getApprovedSourceSelectionId().equals(sourceSelectionId)) {
            throw new IllegalArgumentException("RELEASE_WORKFLOW_SOURCE_SELECTION_NOT_APPROVED");
        }
        var target = properties.requireBackupPublishTarget();
        Preview preview = new Preview("bp-" + UUID.randomUUID(), actor, reason, sourceSelectionId, idempotencyKey,
                targetFingerprint(properties), source.getApprovedMaintenanceCommit(), source.getApprovedApplicationCommit(),
                source.getApprovedFrontendCommit(), source.getPresetId(), source.getPresetVersion(),
                "backup", "172.30.30.59", "app-release", Instant.now().plusSeconds(600),
                "审查服务器", target.getRemoteAppDir(), target.getRemoteDataRoot(), target.getRemoteReleaseRoot(),
                target.getRemoteDataDiskMount(), target.getRemoteDataDiskDevice(),
                Map.of("frontend", 8081, "backend", 48081, "showroom", 8083));
        write(preview.previewId(), preview);
        return preview;
    }

    public synchronized Grant authorize(String previewId, String actor, String confirmation) {
        requireProd(confirmation);
        Preview preview = requirePreview(previewId, actor, true);
        Grant grant = new Grant("ba-" + UUID.randomUUID(), preview, Instant.now());
        write(grant.authorizationId(), grant);
        return grant;
    }

    public Grant requireGrant(String grantId, String previewId, String actor, String idempotencyKey,
                              String confirmation, boolean executionStarted) {
        requireProd(confirmation);
        Grant grant = read(grantId, Grant.class);
        Preview preview = grant.preview();
        if (!preview.previewId().equals(previewId) || !preview.actor().equals(actor)
                || !preview.idempotencyKey().equals(idempotencyKey)
                || (!executionStarted && preview.expiresAt().isBefore(Instant.now()))
                || !preview.targetFingerprint().equals(targetFingerprint(properties))) {
            throw new IllegalStateException("RELEASE_WORKFLOW_BACKUP_AUTHORIZATION_BINDING_INVALID");
        }
        return grant;
    }

    public void requireWorkflowBinding(ReleaseWorkflowRecord workflow) {
        var intent = Objects.requireNonNull(workflow.backupIntent(), "RELEASE_WORKFLOW_BACKUP_INTENT_MISSING");
        Grant grant = requireGrant(intent.authorizationId(), intent.previewId(), workflow.requestedBy(),
                intent.idempotencyKey(), "PROD", true);
        Preview preview = grant.preview();
        if (!preview.maintenanceCommit().equals(workflow.maintenanceCommit())
                || !preview.applicationCommit().equals(workflow.applicationCommit())
                || !preview.frontendCommit().equals(workflow.frontendCommit())
                || !preview.sourceSelectionId().equals(workflow.sourceSelectionId())
                || !preview.reason().equals(workflow.reason())) {
            throw new IllegalStateException("RELEASE_WORKFLOW_BACKUP_SOURCE_BINDING_INVALID");
        }
    }

    private Preview requirePreview(String id, String actor, boolean checkExpiry) {
        Preview preview = read(id, Preview.class);
        if (!preview.actor().equals(actor) || (checkExpiry && preview.expiresAt().isBefore(Instant.now()))
                || !preview.targetFingerprint().equals(targetFingerprint(properties))) {
            throw new IllegalStateException("RELEASE_WORKFLOW_BACKUP_PREVIEW_STALE");
        }
        return preview;
    }
    private static void requireProd(String confirmation) {
        if (!"PROD".equals(confirmation)) throw new IllegalArgumentException("RELEASE_WORKFLOW_PROD_CONFIRM_REQUIRED");
    }
    private Path path(String id) {
        if (id == null || !id.matches("b[pa]-[0-9a-f-]{36}")) throw new IllegalArgumentException("RELEASE_WORKFLOW_AUTH_ID_INVALID");
        return Path.of(properties.getStateDir()).resolve("backup-publish-authorizations").resolve(id + ".json");
    }
    private void write(String id, Object record) {
        try {
            Path target = path(id);
            Files.createDirectories(target.getParent());
            Path temporary = Files.createTempFile(target.getParent(), id, ".tmp");
            mapper.writeValue(temporary.toFile(), record);
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.io.IOException e) { throw new IllegalStateException("RELEASE_WORKFLOW_AUTH_STORE_WRITE_FAILED", e); }
    }
    private <T> T read(String id, Class<T> type) {
        try { return mapper.readValue(path(id).toFile(), type); }
        catch (java.io.IOException e) { throw new IllegalStateException("RELEASE_WORKFLOW_AUTH_STORE_READ_FAILED", e); }
    }
    public static String digest(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    public record Preview(String previewId, String actor, String reason, String sourceSelectionId,
                          String idempotencyKey, String targetFingerprint, String maintenanceCommit,
                          String applicationCommit, String frontendCommit, String presetId, String presetVersion,
                          String targetEnvironment, String targetHost, String publishScope, Instant expiresAt,
                          String targetLabel, String remoteAppDir, String remoteDataRoot, String remoteReleaseRoot,
                          String remoteDataDiskMount, String remoteDataDiskDevice, Map<String, Integer> targetPorts) { }
    public record Grant(String authorizationId, Preview preview, Instant authorizedAt) { }
}
