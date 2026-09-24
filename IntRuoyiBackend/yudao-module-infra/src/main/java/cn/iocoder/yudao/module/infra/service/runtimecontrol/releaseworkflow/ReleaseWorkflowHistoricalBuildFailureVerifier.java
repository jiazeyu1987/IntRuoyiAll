package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOperationRespVO;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeControlOperationStore;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeControlService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Verifies only legacy build failures that are durably proven to have stopped
 * before the build/NAS write boundary.  This verifier intentionally does not
 * certify zero writes and does not perform any remote operation.
 */
final class ReleaseWorkflowHistoricalBuildFailureVerifier {
    private static final Set<ReleaseWorkflowRecord.State> PRE_BUILD_STATES = Set.of(
            ReleaseWorkflowRecord.State.SOURCE_FREEZING,
            ReleaseWorkflowRecord.State.PREFLIGHTING,
            ReleaseWorkflowRecord.State.TESTING,
            ReleaseWorkflowRecord.State.RECOVERY_REQUIRED);
    private static final Pattern FAILURE_EVENT = Pattern.compile(
            "(?m)^\\[FAIL\\] Command failed with exit code [1-9][0-9]*(?:: .*?)?\\s*$");

    private final RuntimeControlProperties properties;
    private final RuntimeControlOperationStore operations;
    private final RuntimeControlService runtime;
    private final ReleaseWorkflowWorktreeFactory factory;
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    ReleaseWorkflowHistoricalBuildFailureVerifier(RuntimeControlProperties properties,
            RuntimeControlOperationStore operations, RuntimeControlService runtime,
            ReleaseWorkflowWorktreeFactory factory) {
        this.properties = properties;
        this.operations = operations;
        this.runtime = runtime;
        this.factory = factory;
    }

    ReleaseWorkflowBuildFailureRecovery.Proof inspect(ReleaseWorkflowRecord workflow, String actor) {
        try {
            return verify(workflow, actor);
        } catch (RuntimeException | IOException ex) {
            String code = ex.getMessage();
            if (code == null || !code.matches("[A-Z][A-Z0-9_]+")) {
                code = "BUILD_RECOVERY_HISTORICAL_EVIDENCE_INVALID";
            }
            return new ReleaseWorkflowBuildFailureRecovery.Proof(null, false, List.of(code));
        }
    }

    private ReleaseWorkflowBuildFailureRecovery.Proof verify(ReleaseWorkflowRecord workflow, String actor)
            throws IOException {
        require(workflow != null && workflow.backupIntent() != null
                        && workflow.state() == ReleaseWorkflowRecord.State.RECOVERY_REQUIRED
                        && "backup".equals(workflow.targetEnvironment())
                        && "app-release".equals(workflow.publishScope())
                        && actor != null && actor.equals(workflow.requestedBy())
                        && workflow.packageDigest() == null && workflow.manifestDigest() == null
                        && workflow.testOperationId() == null && workflow.testOperationEvidencePath() == null,
                "BUILD_RECOVERY_HISTORICAL_WORKFLOW_BINDING_INVALID");

        RuntimeControlOperationRespVO operation = operations.findById(workflow.operationId());
        require(operation != null && "build-release".equals(operation.getAction())
                        && "failed".equals(operation.getStatus()) && "backup".equals(operation.getEnvironment())
                        && actor.equals(operation.getRequestedBy()) && operation.getRequestedAt() != null
                        && !Boolean.TRUE.equals(operation.getZeroWriteEvidence())
                        && operation.getBackupReceiptDigest() == null
                        && operation.getBackupReceiptEvidencePath() == null
                        && operation.getBackupConfirmationState() == null,
                "BUILD_RECOVERY_HISTORICAL_OPERATION_BINDING_INVALID");

        Map<String, String> parameters = operation.getParameters();
        Map<String, String> expected = Map.of(
                "workflowId", workflow.workflowId(),
                "releaseTag", workflow.releaseTag(),
                "publishScope", "app-release",
                "authorizationId", workflow.backupIntent().authorizationId(),
                "sourceSelectionId", workflow.sourceSelectionId(),
                "maintenanceCommit", workflow.maintenanceCommit(),
                "applicationCommit", workflow.applicationCommit(),
                "frontendCommit", workflow.frontendCommit());
        require(parameters != null && expected.entrySet().stream()
                        .allMatch(entry -> entry.getValue().equals(parameters.get(entry.getKey())))
                        && parameters.get("packageDigest") == null
                        && parameters.get("manifestDigest") == null,
                "BUILD_RECOVERY_HISTORICAL_SOURCE_BINDING_INVALID");

        List<ReleaseWorkflowEvent> journal = new ReleaseWorkflowStore(properties).readJournal(workflow.workflowId());
        require(!journal.isEmpty() && journal.stream().allMatch(event -> PRE_BUILD_STATES.contains(event.toState())),
                "BUILD_RECOVERY_HISTORICAL_STATE_AFTER_BUILD");
        require(journal.stream().noneMatch(ReleaseWorkflowHistoricalBuildFailureVerifier::hasBuildOrNasBoundary),
                "BUILD_RECOVERY_NAS_WRITE_BOUNDARY");
        require(journal.stream().anyMatch(event -> event.toState() == ReleaseWorkflowRecord.State.TESTING)
                        && journal.stream().anyMatch(event -> event.toState() == ReleaseWorkflowRecord.State.RECOVERY_REQUIRED
                        && event.errorCode() != null),
                "BUILD_RECOVERY_HISTORICAL_FAILURE_EVENT_MISSING");

        Path log = operations.getOperationLogPath(operation.getOperationId()).toAbsolutePath().normalize();
        require(operation.getResultLogPath() != null
                        && log.toString().equals(Path.of(operation.getResultLogPath()).toAbsolutePath().normalize().toString())
                        && Files.isRegularFile(log, LinkOption.NOFOLLOW_LINKS) && Files.size(log) <= 8 * 1024 * 1024,
                "BUILD_RECOVERY_HISTORICAL_LOG_BINDING_INVALID");
        String logText = Files.readString(log, StandardCharsets.UTF_8);
        require(logText.lines().findFirst().orElse("").equals("# Runtime Control Operation")
                        && FAILURE_EVENT.matcher(logText).find()
                        && operation.getSummary() != null
                        && operation.getSummary().contains("exitCode=")
                        && operation.getSummary().contains(log.toString()),
                "BUILD_RECOVERY_HISTORICAL_FAILURE_EVENT_INVALID");
        List<String> commandHeaders = logText.lines().filter(line -> line.startsWith("command=")).toList();
        require(commandHeaders.size() == 1, "BUILD_RECOVERY_COMMAND_AMBIGUOUS");
        Map<String, String> command = ReleaseWorkflowBuildFailureRecovery.command(commandHeaders.get(0).substring(8));
        ReleaseWorkflowWorktreeFactory.FrozenWorktrees frozen = factory.requirePrepared(workflow);
        var authorization = readAuthorization(workflow, actor);
        var target = properties.requireBackupPublishTarget();
        var required = requiredCommandBindings(workflow, frozen, authorization, target);
        require(required.entrySet().stream().allMatch(entry -> entry.getValue().equals(command.get(entry.getKey())))
                        && command.containsKey("-SkipDatabaseSync") && command.containsKey("-SkipMinioSync"),
                "BUILD_RECOVERY_COMMAND_BINDING_INVALID");

        require(!runtime.isOperationExecutorAlive(operation.getOperationId()),
                "BUILD_EXECUTOR_TERMINATION_UNPROVEN");
        require(!Files.exists(operations.backupReceiptPath(operation.getOperationId()), LinkOption.NOFOLLOW_LINKS)
                        && !Files.exists(Path.of(properties.getStateDir()).resolve("backup-finalization")
                        .resolve(workflow.workflowId() + ".decision.json"), LinkOption.NOFOLLOW_LINKS),
                "BUILD_RECOVERY_PUBLICATION_EVIDENCE_PRESENT");
        rejectOtherOperationEvidence(workflow, operation);

        String digest = ReleaseWorkflowBackupAuthorizationService.digest(String.join("\n",
                "historical-pre-building-v1", workflow.workflowId(), workflow.releaseTag(),
                operation.getOperationId(), actor, new TreeMap<>(expected).toString(),
                ReleaseWorkflowBackupAuthorizationService.digest(logText),
                ReleaseWorkflowBackupAuthorizationService.digest(journal.toString()),
                operation.getSummary()));
        return new ReleaseWorkflowBuildFailureRecovery.Proof(digest, true, List.of());
    }

    private ReleaseWorkflowBackupAuthorizationService.Preview readAuthorization(
            ReleaseWorkflowRecord workflow, String actor) throws IOException {
        String authorizationId = workflow.backupIntent().authorizationId();
        require(authorizationId.matches("[A-Za-z0-9-]+"), "BUILD_RECOVERY_AUTHORIZATION_INVALID");
        Path path = Path.of(properties.getStateDir()).resolve("backup-publish-authorizations")
                .resolve(authorizationId + ".json");
        require(Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) && Files.size(path) <= 1024 * 1024,
                "BUILD_RECOVERY_AUTHORIZATION_MISSING");
        String text = Files.readString(path, StandardCharsets.UTF_8);
        var grant = mapper.readValue(text, ReleaseWorkflowBackupAuthorizationService.Grant.class);
        var preview = grant.preview();
        var target = properties.requireBackupPublishTarget();
        require(grant.authorizationId().equals(authorizationId) && preview != null
                        && actor.equals(preview.actor()) && workflow.backupIntent().previewId().equals(preview.previewId())
                        && workflow.backupIntent().targetFingerprint().equals(preview.targetFingerprint())
                        && workflow.sourceSelectionId().equals(preview.sourceSelectionId())
                        && workflow.maintenanceCommit().equals(preview.maintenanceCommit())
                        && workflow.applicationCommit().equals(preview.applicationCommit())
                        && workflow.frontendCommit().equals(preview.frontendCommit())
                        && "backup".equals(preview.targetEnvironment()) && "app-release".equals(preview.publishScope())
                        && target.getHost().equals(preview.targetHost())
                        && target.getRemoteAppDir().equals(preview.remoteAppDir())
                        && target.getRemoteDataRoot().equals(preview.remoteDataRoot())
                        && target.getRemoteReleaseRoot().equals(preview.remoteReleaseRoot())
                        && target.getRemoteDataDiskMount().equals(preview.remoteDataDiskMount())
                        && target.getRemoteDataDiskDevice().equals(preview.remoteDataDiskDevice()),
                "BUILD_RECOVERY_AUTHORIZATION_BINDING_INVALID");
        return preview;
    }

    private Map<String, String> requiredCommandBindings(ReleaseWorkflowRecord workflow,
            ReleaseWorkflowWorktreeFactory.FrozenWorktrees frozen,
            ReleaseWorkflowBackupAuthorizationService.Preview authorization,
            RuntimeControlProperties.Environment target) {
        Path script = frozen.maintenanceRoot().resolve(properties.getReleaseWorkflow().getPublishScriptPath());
        var required = new LinkedHashMap<String, String>();
        required.put("-File", script.toString());
        required.put("-Mode", "build-release");
        required.put("-DeployIntent", "independent-backup");
        required.put("-Environment", "backup");
        required.put("-Component", "intruoyi");
        required.put("-PublishScope", "app-release");
        required.put("-ConfirmText", "PROD");
        required.put("-ReleaseWorkflowId", workflow.workflowId());
        required.put("-ReleaseTag", workflow.releaseTag());
        required.put("-AuthorizationId", workflow.backupIntent().authorizationId());
        required.put("-SourceSelectionId", workflow.sourceSelectionId());
        required.put("-ExpectedMaintenanceCommit", workflow.maintenanceCommit());
        required.put("-ExpectedApplicationCommit", workflow.applicationCommit());
        required.put("-ExpectedFrontendCommit", workflow.frontendCommit());
        required.put("-BackendRepoRoot", frozen.backendRoot().toString());
        required.put("-FrontendRepoRoot", frozen.frontendRoot().toString());
        required.put("-ServerHost", authorization.targetHost());
        required.put("-BackupServerHost", authorization.targetHost());
        required.put("-ServerUser", target.getServerUser());
        required.put("-RemoteAppDir", authorization.remoteAppDir());
        required.put("-RemoteDataRoot", authorization.remoteDataRoot());
        required.put("-RemoteReleaseRoot", authorization.remoteReleaseRoot());
        required.put("-RemoteDataDiskMount", authorization.remoteDataDiskMount());
        required.put("-RemoteDataDiskDevice", authorization.remoteDataDiskDevice());
        required.put("-RemoteMinioContainer", target.getRemoteMinioContainer());
        return required;
    }

    private void rejectOtherOperationEvidence(ReleaseWorkflowRecord workflow,
            RuntimeControlOperationRespVO current) throws IOException {
        try (var files = Files.list(operations.getStateDir())) {
            for (Path file : files.filter(path -> path.getFileName().toString().endsWith(".json")).toList()) {
                String id = file.getFileName().toString().replaceFirst("\\.json$", "");
                RuntimeControlOperationRespVO other = operations.findById(id);
                require(other != null, "BUILD_RECOVERY_OPERATION_HISTORY_INVALID");
                if (id.equals(current.getOperationId())) {
                    continue;
                }
                Map<String, String> parameters = other.getParameters();
                require(parameters == null || (!workflow.workflowId().equals(parameters.get("workflowId"))
                                && !workflow.releaseTag().equals(parameters.get("releaseTag"))),
                        "BUILD_RECOVERY_OTHER_OPERATION_PRESENT");
            }
        }
    }

    private static boolean hasBuildOrNasBoundary(ReleaseWorkflowEvent event) {
        if (event.toState() == ReleaseWorkflowRecord.State.BUILDING
                || event.fromState() == ReleaseWorkflowRecord.State.BUILDING
                || "BUILDING".equals(event.failedStage())) {
            return true;
        }
        String evidence = String.valueOf(event.errorCode()) + " " + String.valueOf(event.failedStage())
                + " " + event.evidenceRefs() + " " + event.details();
        return evidence.matches("(?is).*\\b(?:NAS|REMOTE)[_-]?(?:WRITE|PUBLISH|UPLOAD)\\b.*");
    }

    private static void require(boolean condition, String code) {
        if (!condition) {
            throw new IllegalStateException(code);
        }
    }
}
