package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOperationRespVO;
import cn.iocoder.yudao.module.infra.service.file.NasBrowserService;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeControlOperationStore;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeControlService;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.regex.Pattern;

/** Retires a failed build; never certifies zero writes or recovers a deployed environment. */
final class ReleaseWorkflowBuildFailureRecovery {
    private final RuntimeControlProperties properties;
    private final RuntimeControlOperationStore operations;
    private final RuntimeControlService runtime;
    private final ReleaseWorkflowWorktreeFactory factory;
    private final BootVerifier boot;
    private final HostIdentityProvider hostIdentity;
    private final NasBoundaryInspector nasBoundaryInspector;
    private final ReleaseWorkflowHistoricalBuildFailureVerifier historicalVerifier;

    ReleaseWorkflowBuildFailureRecovery(RuntimeControlProperties properties, RuntimeControlOperationStore operations,
            RuntimeControlService runtime, ReleaseWorkflowWorktreeFactory factory, BootVerifier boot) {
        this(properties, operations, runtime, factory, boot, ReleaseWorkflowExecutorHostIdentity::currentDigest,
                workflowId -> NasBoundaryEvidence.unavailable());
    }

    ReleaseWorkflowBuildFailureRecovery(RuntimeControlProperties properties, RuntimeControlOperationStore operations,
            RuntimeControlService runtime, ReleaseWorkflowWorktreeFactory factory, BootVerifier boot,
            HostIdentityProvider hostIdentity) {
        this(properties, operations, runtime, factory, boot, hostIdentity,
                workflowId -> NasBoundaryEvidence.unavailable());
    }

    ReleaseWorkflowBuildFailureRecovery(RuntimeControlProperties properties, RuntimeControlOperationStore operations,
            RuntimeControlService runtime, ReleaseWorkflowWorktreeFactory factory, BootVerifier boot,
            HostIdentityProvider hostIdentity, NasBoundaryInspector nasBoundaryInspector) {
        this.properties = properties; this.operations = operations; this.runtime = runtime;
        this.factory = factory; this.boot = boot; this.hostIdentity = Objects.requireNonNull(hostIdentity);
        this.nasBoundaryInspector = Objects.requireNonNull(nasBoundaryInspector);
        this.historicalVerifier = new ReleaseWorkflowHistoricalBuildFailureVerifier(properties, operations, runtime, factory);
    }

    static NasBoundaryEvidence inspectNasReleaseBoundary(RuntimeControlProperties properties,
            NasBrowserService nasBrowserService, String releaseTag) {
        try {
            String root = properties.getReleasePackage().getNasReleaseRoot();
            var listing = nasBrowserService.listFiles(root);
            String entries = listing.getItems().stream()
                    .map(item -> String.valueOf(item.getPath()) + "|" + String.valueOf(item.getName()))
                    .sorted().collect(java.util.stream.Collectors.joining("\n"));
            boolean absent = listing.getItems().stream().noneMatch(item -> {
                String name = String.valueOf(item.getName());
                String path = String.valueOf(item.getPath());
                return name.equals(releaseTag) || path.equals(root + "/" + releaseTag)
                        || name.startsWith("." + releaseTag + ".staging-")
                        || path.contains("/." + releaseTag + ".staging-");
            });
            return new NasBoundaryEvidence(absent, absent,
                    ReleaseWorkflowBackupAuthorizationService.digest(root + "\n" + entries));
        } catch (RuntimeException ex) {
            return NasBoundaryEvidence.unavailable();
        }
    }

    Proof inspect(ReleaseWorkflowRecord workflow, String actor) {
        Proof exact;
        try { return verify(workflow, actor); }
        catch (RuntimeException | java.io.IOException ex) {
            String code = ex.getMessage();
            if (code == null || !code.matches("[A-Z][A-Z0-9_]+")) code = "BUILD_RECOVERY_EVIDENCE_INVALID";
            exact = new Proof(null, false, List.of(code));
        }
        if (!isHistoricalSchemaCandidate(workflow)) return exact;
        Proof historical = historicalVerifier.inspect(workflow, actor);
        return historical.eligible() ? historical : exact;
    }

    private boolean isHistoricalSchemaCandidate(ReleaseWorkflowRecord workflow) {
        try {
            if (workflow == null || workflow.operationId() == null) return false;
            var operation = operations.findById(workflow.operationId());
            if (operation == null || !"build-release".equals(operation.getAction())) return false;
            var parameters = operation.getParameters();
            if (parameters == null || parameters.containsKey("executorHostIdentitySha256")
                    || parameters.containsKey("releaseWorkflowCommandSha256")) return false;
            Path log = operations.getOperationLogPath(operation.getOperationId());
            if (!Files.isRegularFile(log, LinkOption.NOFOLLOW_LINKS) || Files.size(log) > 8 * 1024 * 1024) {
                return false;
            }
            String text = Files.readString(log, java.nio.charset.StandardCharsets.UTF_8);
            return text.lines().noneMatch(line -> line.startsWith("executorHostIdentitySha256=")
                    || line.startsWith("releaseWorkflowCommandSha256="));
        } catch (RuntimeException | java.io.IOException ignored) {
            return false;
        }
    }

    private Proof verify(ReleaseWorkflowRecord w, String actor) throws java.io.IOException {
        require(w.backupIntent() != null && "backup".equals(w.targetEnvironment()) && "app-release".equals(w.publishScope())
                && actor != null && actor.equals(w.requestedBy()) && w.packageDigest() == null && w.manifestDigest() == null,
                "BUILD_RECOVERY_WORKFLOW_BINDING_INVALID");
        var op = operations.findById(w.operationId());
        require(op != null && "build-release".equals(op.getAction()) && "backup".equals(op.getEnvironment())
                && "failed".equals(op.getStatus()) && actor.equals(op.getRequestedBy()) && op.getRequestedAt() != null
                && op.getBackupReceiptDigest() == null && op.getBackupReceiptEvidencePath() == null
                && op.getBackupConfirmationState() == null, "BUILD_RECOVERY_OPERATION_BINDING_INVALID");
        var expected = Map.of("workflowId", w.workflowId(), "releaseTag", w.releaseTag(), "publishScope", "app-release",
                "authorizationId", w.backupIntent().authorizationId(), "sourceSelectionId", w.sourceSelectionId(),
                "maintenanceCommit", w.maintenanceCommit(), "applicationCommit", w.applicationCommit(), "frontendCommit", w.frontendCommit());
        require(op.getParameters() != null && expected.entrySet().stream().allMatch(e -> e.getValue().equals(op.getParameters().get(e.getKey())))
                && op.getParameters().get("packageDigest") == null && op.getParameters().get("manifestDigest") == null,
                "BUILD_RECOVERY_SOURCE_BINDING_INVALID");
        var journal = new ReleaseWorkflowStore(properties).readJournal(w.workflowId());
        boolean enteredBuilding = w.state() == ReleaseWorkflowRecord.State.BUILDING
                || journal.stream().anyMatch(e -> e.toState() == ReleaseWorkflowRecord.State.BUILDING
                || "BUILDING".equals(e.failedStage()));
        String nasBoundaryDigest = null;
        if (enteredBuilding) {
            require(isVerifiablePrePackageFailure(w, op, journal), "BUILD_RECOVERY_NAS_WRITE_BOUNDARY");
            NasBoundaryEvidence evidence = nasBoundaryInspector.inspect(w.releaseTag());
            require(evidence.packageAbsent() && evidence.stagingAbsent(), "BUILD_RECOVERY_NAS_EVIDENCE_UNAVAILABLE");
            nasBoundaryDigest = evidence.digest();
        }
        require(!journal.isEmpty() && journal.stream().allMatch(e -> e.toState() == null
                || Set.of(ReleaseWorkflowRecord.State.SOURCE_FREEZING, ReleaseWorkflowRecord.State.PREFLIGHTING,
                ReleaseWorkflowRecord.State.TESTING, ReleaseWorkflowRecord.State.RECOVERY_REQUIRED).contains(e.toState())
                || (enteredBuilding && e.toState() == ReleaseWorkflowRecord.State.BUILDING)),
                "BUILD_RECOVERY_DEPLOYMENT_HISTORY_PRESENT");
        try (var files = Files.list(operations.getStateDir())) {
            for (Path file : files.filter(p -> p.getFileName().toString().endsWith(".json")).toList()) {
                String id = file.getFileName().toString().replaceFirst("\\.json$", "");
                var other = operations.findById(id);
                require(other != null, "BUILD_RECOVERY_OPERATION_HISTORY_INVALID");
                if (id.equals(w.operationId())) continue;
                var params = other.getParameters();
                require(params == null || (!w.workflowId().equals(params.get("workflowId"))
                        && !w.releaseTag().equals(params.get("releaseTag"))), "BUILD_RECOVERY_OTHER_OPERATION_PRESENT");
            }
        }
        Path state = Path.of(properties.getStateDir());
        require(!Files.exists(state.resolve("backup-finalization").resolve(w.workflowId() + ".decision.json"), LinkOption.NOFOLLOW_LINKS)
                && !Files.exists(state.resolve("backup-receipts").resolve(w.operationId() + ".json"), LinkOption.NOFOLLOW_LINKS),
                "BUILD_RECOVERY_PUBLICATION_EVIDENCE_PRESENT");
        var frozen = factory.requirePrepared(w);
        require(w.backupIntent().authorizationId().matches("[A-Za-z0-9-]+"), "BUILD_RECOVERY_AUTHORIZATION_INVALID");
        Path grantPath = state.resolve("backup-publish-authorizations").resolve(w.backupIntent().authorizationId() + ".json");
        require(Files.isRegularFile(grantPath) && Files.size(grantPath) <= 1024 * 1024, "BUILD_RECOVERY_AUTHORIZATION_MISSING");
        String grantText = Files.readString(grantPath, java.nio.charset.StandardCharsets.UTF_8);
        var grant = new com.fasterxml.jackson.databind.ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                .readValue(grantText, ReleaseWorkflowBackupAuthorizationService.Grant.class);
        var authorized = grant.preview();
        var target = properties.requireBackupPublishTarget();
        require(grant.authorizationId().equals(w.backupIntent().authorizationId()) && authorized != null
                && actor.equals(authorized.actor()) && w.backupIntent().previewId().equals(authorized.previewId())
                && w.backupIntent().targetFingerprint().equals(authorized.targetFingerprint())
                && w.sourceSelectionId().equals(authorized.sourceSelectionId())
                && w.maintenanceCommit().equals(authorized.maintenanceCommit()) && w.applicationCommit().equals(authorized.applicationCommit())
                && w.frontendCommit().equals(authorized.frontendCommit()) && "backup".equals(authorized.targetEnvironment())
                && "app-release".equals(authorized.publishScope()) && target.getHost().equals(authorized.targetHost())
                && target.getRemoteAppDir().equals(authorized.remoteAppDir()) && target.getRemoteDataRoot().equals(authorized.remoteDataRoot())
                && target.getRemoteReleaseRoot().equals(authorized.remoteReleaseRoot())
                && target.getRemoteDataDiskMount().equals(authorized.remoteDataDiskMount())
                && target.getRemoteDataDiskDevice().equals(authorized.remoteDataDiskDevice()), "BUILD_RECOVERY_AUTHORIZATION_BINDING_INVALID");
        Path log = operations.getOperationLogPath(w.operationId());
        require(Files.isRegularFile(log) && Files.size(log) <= 8 * 1024 * 1024, "BUILD_RECOVERY_LOG_INVALID");
        String text = Files.readString(log, java.nio.charset.StandardCharsets.UTF_8);
        String operationIdentity = op.getParameters().get("executorHostIdentitySha256");
        String operationCommandDigest = op.getParameters().get("releaseWorkflowCommandSha256");
        List<String> identityMarkers = text.lines().filter(line -> line.startsWith("executorHostIdentitySha256=")).toList();
        List<String> commandDigestMarkers = text.lines().filter(line -> line.startsWith("releaseWorkflowCommandSha256=")).toList();
        require(validDigest(operationIdentity) && validDigest(operationCommandDigest)
                        && identityMarkers.size() == 1 && commandDigestMarkers.size() == 1
                        && operationIdentity.equals(identityMarkers.get(0).substring("executorHostIdentitySha256=".length()))
                        && operationCommandDigest.equals(commandDigestMarkers.get(0).substring("releaseWorkflowCommandSha256=".length()))
                        && operationIdentity.equals(hostIdentity.currentDigest()),
                "BUILD_EXECUTOR_HOST_IDENTITY_INVALID");
        List<String> headers = text.lines().filter(line -> line.startsWith("command=")).toList();
        require(headers.size() == 1, "BUILD_RECOVERY_COMMAND_AMBIGUOUS");
        String commandLine = headers.get(0).substring(8);
        String environment = headerValue(text, "environment=");
        String component = headerValue(text, "component=");
        String workingDirectory = headerValue(text, "workingDirectory=");
        require(operationCommandDigest.equals(ReleaseWorkflowCommandFingerprint.calculate(
                        environment, component, workingDirectory, commandLine)),
                "BUILD_RECOVERY_COMMAND_FINGERPRINT_INVALID");
        Map<String, String> command = command(commandLine);
        var required = new LinkedHashMap<String, String>();
        required.put("-File", frozen.maintenanceRoot().resolve(properties.getReleaseWorkflow().getPublishScriptPath()).toString());
        required.put("-Mode", "build-release"); required.put("-DeployIntent", "independent-backup");
        required.put("-Environment", "backup"); required.put("-Component", "intruoyi");
        required.put("-PublishScope", "app-release"); required.put("-ConfirmText", "PROD");
        required.put("-ReleaseWorkflowId", w.workflowId()); required.put("-ReleaseTag", w.releaseTag());
        required.put("-AuthorizationId", w.backupIntent().authorizationId()); required.put("-SourceSelectionId", w.sourceSelectionId());
        required.put("-ExpectedMaintenanceCommit", w.maintenanceCommit()); required.put("-ExpectedApplicationCommit", w.applicationCommit());
        required.put("-ExpectedFrontendCommit", w.frontendCommit()); required.put("-BackendRepoRoot", frozen.backendRoot().toString());
        required.put("-FrontendRepoRoot", frozen.frontendRoot().toString());
        required.put("-SkipDatabaseSync", ""); required.put("-SkipMinioSync", "");
        required.put("-ServerHost", authorized.targetHost()); required.put("-BackupServerHost", authorized.targetHost());
        required.put("-ServerUser", target.getServerUser()); required.put("-RemoteAppDir", authorized.remoteAppDir());
        required.put("-RemoteDataRoot", authorized.remoteDataRoot()); required.put("-RemoteReleaseRoot", authorized.remoteReleaseRoot());
        required.put("-RemoteDataDiskMount", authorized.remoteDataDiskMount()); required.put("-RemoteDataDiskDevice", authorized.remoteDataDiskDevice());
        required.put("-RemoteMinioContainer", target.getRemoteMinioContainer());
        require(required.entrySet().stream().allMatch(e -> e.getValue().equals(command.get(e.getKey()))), "BUILD_RECOVERY_COMMAND_BINDING_INVALID");
        Instant bootTime = boot.lastBoot();
        require(bootTime != null && bootTime.isAfter(op.getRequestedAt().atZone(ZoneId.systemDefault()).toInstant())
                && !bootTime.isAfter(Instant.now()) && !runtime.isOperationExecutorAlive(w.operationId()),
                "BUILD_EXECUTOR_TERMINATION_UNPROVEN");
        String digest = ReleaseWorkflowBackupAuthorizationService.digest(String.join("\n", w.workflowId(), w.releaseTag(),
                w.operationId(), actor, new TreeMap<>(expected).toString(), frozen.publishScriptSha256(),
                op.getRequestedAt().toString(), bootTime.toString(), ReleaseWorkflowBackupAuthorizationService.digest(grantText),
                ReleaseWorkflowBackupAuthorizationService.digest(text), String.valueOf(nasBoundaryDigest)));
        return new Proof(digest, true, List.of());
    }

    private boolean isVerifiablePrePackageFailure(ReleaseWorkflowRecord workflow,
            RuntimeControlOperationRespVO operation, List<ReleaseWorkflowEvent> journal) throws java.io.IOException {
        if (journal.stream().noneMatch(event -> event.toState() == ReleaseWorkflowRecord.State.RECOVERY_REQUIRED
                && "BUILDING".equals(event.failedStage()))) return false;
        Path log = operations.getOperationLogPath(operation.getOperationId());
        if (!Files.isRegularFile(log) || operation.getSummary() == null
                || !operation.getSummary().contains("exitCode=1")) return false;
        String text = Files.readString(log, java.nio.charset.StandardCharsets.UTF_8);
        return text.contains("backend build is blocked before package generation")
                && text.lines().filter(line -> line.startsWith("[") || line.startsWith("RELEASE_"))
                .noneMatch(line -> line.matches("(?i).*\\b(?:NAS|REMOTE|UPLOAD|PUBLISH)\\b.*"));
    }

    static Map<String, String> command(String source) {
        var matcher = Pattern.compile("\"[^\"]*\"|'[^']*'|[^\\s]+").matcher(source);
        List<String> tokens = new ArrayList<>();
        while (matcher.find()) {
            String token = matcher.group();
            tokens.add(token.startsWith("\"") || token.startsWith("'") ? token.substring(1, token.length() - 1) : token);
        }
        require(!tokens.isEmpty() && "powershell.exe".equals(tokens.remove(0)), "BUILD_RECOVERY_COMMAND_INVALID");
        Set<String> switches = Set.of("-NoProfile", "-SkipDatabaseSync", "-SkipMinioSync");
        Set<String> allowed = Set.of("-ExecutionPolicy", "-File", "-Mode", "-ReleaseTag", "-ExpectedMaintenanceCommit",
                "-ExpectedApplicationCommit", "-ExpectedFrontendCommit", "-SourceSelectionId", "-Component", "-PublishScope",
                "-DeployIntent", "-ReleaseWorkflowId", "-AuthorizationId", "-Environment", "-ConfirmText", "-ServerHost",
                "-ServerUser", "-RemoteAppDir", "-RemoteReleaseRoot", "-RemoteDataRoot", "-RemoteDataDiskMount",
                "-RemoteDataDiskDevice", "-RemoteMinioContainer", "-TestServerHost", "-BackupServerHost", "-BackendRepoRoot",
                "-FrontendRepoRoot", "-RestoreIsolationMarkerPath", "-NasConfigPath", "-NasServer", "-NasShare", "-NasReleaseRoot",
                "-BackendRuntimeBaseMode", "-BackendRuntimeBaseTarPath", "-BackendRuntimeBaseTarSha256", "-BackendRuntimeBaseImage",
                "-BackendRuntimeBaseDigest", "-BackendRuntimeBaseVersion");
        Map<String, String> result = new HashMap<>();
        for (int i = 0; i < tokens.size(); i++) {
            String key = tokens.get(i);
            require(!result.containsKey(key) && (switches.contains(key) || allowed.contains(key)), "BUILD_RECOVERY_COMMAND_INVALID");
            if (switches.contains(key)) result.put(key, "");
            else { require(++i < tokens.size() && !tokens.get(i).startsWith("-"), "BUILD_RECOVERY_COMMAND_INVALID"); result.put(key, tokens.get(i)); }
        }
        require("Bypass".equals(result.get("-ExecutionPolicy")) && result.containsKey("-NoProfile"), "BUILD_RECOVERY_COMMAND_INVALID");
        return result;
    }

    static Instant windowsLastBoot() {
        String systemRoot = System.getenv("SystemRoot");
        require(systemRoot != null, "BUILD_BOOT_QUERY_UNAVAILABLE");
        String executable = Path.of(systemRoot, "System32", "WindowsPowerShell", "v1.0", "powershell.exe").toString();
        var result = ReleaseWorkflowLocalProcess.run(List.of(executable, "-NoProfile", "-NonInteractive", "-Command",
                "$ErrorActionPreference='Stop'; $boot=(Get-CimInstance Win32_OperatingSystem).LastBootUpTime; [Console]::Write(([DateTimeOffset]$boot.ToUniversalTime()).ToUnixTimeMilliseconds())"),
                null, Map.of(), Duration.ofSeconds(20), "BUILD_BOOT_QUERY");
        require(result.exitCode() == 0 && result.output().trim().matches("[0-9]{13}"), "BUILD_BOOT_QUERY_FAILED");
        return Instant.ofEpochMilli(Long.parseLong(result.output().trim()));
    }

    private static void require(boolean condition, String code) { if (!condition) throw new IllegalStateException(code); }
    private static boolean validDigest(String value) { return value != null && value.matches("[0-9a-f]{64}"); }
    private static String headerValue(String text, String prefix) {
        List<String> matches = text.lines().filter(line -> line.startsWith(prefix)).toList();
        require(matches.size() == 1, "BUILD_RECOVERY_COMMAND_CONTEXT_INVALID");
        return matches.get(0).substring(prefix.length());
    }
    @FunctionalInterface interface BootVerifier { Instant lastBoot(); }
    @FunctionalInterface interface HostIdentityProvider { String currentDigest(); }
    @FunctionalInterface interface NasBoundaryInspector { NasBoundaryEvidence inspect(String releaseTag); }
    record NasBoundaryEvidence(boolean packageAbsent, boolean stagingAbsent, String digest) {
        static NasBoundaryEvidence unavailable() { return new NasBoundaryEvidence(false, false, null); }
    }
    record Proof(String digest, boolean eligible, List<String> blockers) { }
}
