package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeControlOperationStore;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeControlRestoreIsolationConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/** Inspect and recover only the persisted operation's exact remote resource owner. */
@Service
public class ReleaseWorkflowBackupRecoveryService {
    private final RuntimeControlProperties properties;
    private final ReleaseWorkflowStore store;
    private final RuntimeControlOperationStore operations;
    private final Runner runner;
    private final Completion completion;
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Autowired
    public ReleaseWorkflowBackupRecoveryService(RuntimeControlProperties properties, ReleaseWorkflowStore store,
            RuntimeControlOperationStore operations, ReleaseWorkflowOrchestrator orchestrator) {
        this(properties, store, operations, null, orchestrator::completeBackupRecovery);
    }

    public ReleaseWorkflowBackupRecoveryService(RuntimeControlProperties properties, ReleaseWorkflowStore store,
            RuntimeControlOperationStore operations, Runner runner, Completion completion) {
        this.properties = properties; this.store = store; this.operations = operations;
        this.runner = runner == null ? this::execute : runner; this.completion = completion;
    }

    public Preview inspect(String workflowId, long version, String actor) {
        requireActor(actor);
        requireNoPublicationDecision(workflowId);
        var workflow = requireRecoverable(workflowId, version);
        Binding binding = binding(workflow);
        String fingerprint = targetFingerprint();
        CommandResult result = runner.run(command(workflow, binding, "inspect", null));
        requireRecoverable(workflowId, version);
        if (!fingerprint.equals(targetFingerprint()) || !binding.equals(binding(workflow))) {
            throw new IllegalStateException("RECOVERY_INSPECTION_BINDING_CHANGED");
        }
        String digest = result.exitCode() == 0 ? optionalMarker(result.output(), "RECOVERY_PREVIEW_DIGEST", "[0-9a-f]{64}") : null;
        String runtime = result.exitCode() == 0 ? optionalMarker(result.output(), "RECOVERY_RUNTIME_VERSION", "[A-Za-z0-9._-]+") : null;
        boolean eligible = digest != null && runtime != null;
        List<String> blockers = eligible ? List.of() : blockers(result.output());
        Preview preview = new Preview("br-" + UUID.randomUUID(), workflowId, version, actor, fingerprint,
                binding.fingerprint(), digest, runtime, eligible, blockers,
                eligible ? "确认 PROD 后重新核对现场，证实零变更才解除隔离。"
                        : blockers.contains("RECOVERY_PUBLISH_RECEIPT_REQUIRES_ACK")
                        ? "该发布已有运行态验收收据，请继续发布 ACK 确认流程；保留当前环境隔离，不执行普通恢复。"
                        : "保持隔离；按报告检查迁移台账、配置、容器及旧恢复标记。部分写入须先执行经批准的数据恢复与兼容性验收，不能强制解锁。",
                Instant.now().plusSeconds(600));
        write(path(preview.previewId() + ".json"), preview);
        return preview;
    }

    public ReleaseWorkflowRecord recover(String workflowId, long version, String previewId, String actor, String confirm) {
        requireActor(actor);
        if (!"PROD".equals(confirm)) throw new IllegalArgumentException("RECOVERY_PROD_CONFIRM_REQUIRED");
        requireNoPublicationDecision(workflowId);
        if (previewId == null || !previewId.matches("br-[0-9a-f-]{36}")) throw new IllegalArgumentException("RECOVERY_PREVIEW_ID_INVALID");
        Preview preview = read(path(previewId + ".json"), Preview.class);
        if (!preview.actor().equals(actor) || !preview.workflowId().equals(workflowId)
                || preview.expectedStateVersion() != version || !preview.eligible()) {
            throw new IllegalStateException("RECOVERY_PREVIEW_BINDING_INVALID");
        }
        Path verified = path(workflowId + ".verified.json");
        if (Files.isRegularFile(verified)) {
            Preview evidence = read(verified, Preview.class);
            if (!evidence.equals(preview)) throw new IllegalStateException("RECOVERY_PREVIOUS_RESULT_BINDING_INVALID");
            var current = store.require(workflowId);
            if (current.state() == ReleaseWorkflowRecord.State.FAILED && current.zeroWriteEvidence()
                    && current.stateVersion() == version + 1) return current;
            requireRecoverable(workflowId, version);
            return completion.complete(workflowId, version, actor, preview.evidenceDigest());
        }
        if (preview.expiresAt().isBefore(Instant.now()) || !properties.getReleaseWorkflow().isProductionWriteEnabled()) {
            throw new IllegalStateException("RECOVERY_AUTHORIZATION_EXPIRED_OR_DISABLED");
        }
        var workflow = requireRecoverable(workflowId, version);
        Binding binding = binding(workflow);
        if (!preview.targetFingerprint().equals(targetFingerprint()) || !preview.operationFingerprint().equals(binding.fingerprint())) {
            throw new IllegalStateException("RECOVERY_TARGET_OR_OPERATION_CHANGED");
        }
        claim(workflowId, preview);
        // A permanent claim is made before dispatch. Missing verified output after a crash never retries remotely.
        CommandResult result = runner.run(command(workflow, binding, "recover", preview.evidenceDigest()));
        if (result.exitCode() != 0 || !Arrays.asList(result.output().split("\\R")).contains("RUNTIME_RESOURCE_RECOVERED")) {
            throw new IllegalStateException("RECOVERY_EXECUTION_UNVERIFIED: " + String.join(",", blockers(result.output())));
        }
        write(verified, preview);
        return completion.complete(workflowId, version, actor, preview.evidenceDigest());
    }

    private ReleaseWorkflowRecord requireRecoverable(String id, long version) {
        var workflow = store.require(id);
        if (!"backup".equals(workflow.targetEnvironment()) || workflow.state() != ReleaseWorkflowRecord.State.RECOVERY_REQUIRED
                || workflow.stateVersion() != version || workflow.operationId() == null) {
            throw new IllegalStateException("RECOVERY_WORKFLOW_STATE_INVALID");
        }
        return workflow;
    }

    private void requireNoPublicationDecision(String workflowId) {
        Path name = path(workflowId + ".decision.json").getFileName();
        Path decision = Path.of(properties.getStateDir()).resolve("backup-finalization").resolve(name);
        if (Files.exists(decision, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalStateException("RECOVERY_PUBLICATION_CONFIRMATION_REQUIRED");
        }
    }

    private Binding binding(ReleaseWorkflowRecord workflow) {
        var operation = operations.findById(workflow.operationId());
        if (operation == null || !"backup".equals(operation.getEnvironment()) || !"publish-backup".equals(operation.getAction())
                || Set.of("running", "queued", "pending").contains(String.valueOf(operation.getStatus()).toLowerCase(Locale.ROOT))
                || operation.getParameters() == null || !workflow.workflowId().equals(operation.getParameters().get("workflowId"))
                || !workflow.releaseTag().equals(operation.getParameters().get("releaseTag"))) {
            throw new IllegalStateException("RECOVERY_OPERATION_BINDING_INVALID");
        }
        String text = readText(operations.getOperationLogPath(workflow.operationId()));
        String token = requiredMarker(text, "RUNTIME_RESOURCE_LEASE_TOKEN", "[0-9a-f]{32}");
        String target = requiredMarker(text, "RUNTIME_RESOURCE_HOST", "[A-Za-z0-9.-]+");
        if (!target.equals(properties.requireBackupPublishTarget().getHost())) throw new IllegalStateException("RECOVERY_OPERATION_TARGET_MISMATCH");
        return new Binding(token, requiredMarker(text, "RUNTIME_RESOURCE_EXECUTOR_HOST", "[A-Za-z0-9._-]+"),
                requiredMarker(text, "RUNTIME_RESOURCE_EXECUTOR_PID", "[1-9][0-9]*"),
                requiredMarker(text, "RUNTIME_RESOURCE_EXECUTOR_IDENTITY", "[^\\s=]+"),
                ReleaseWorkflowBackupAuthorizationService.digest(text));
    }

    private List<String> command(ReleaseWorkflowRecord workflow, Binding binding, String mode, String digest) {
        var target = properties.requireBackupPublishTarget();
        List<String> args = new ArrayList<>(List.of("powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass",
                "-File", script().toString(), "-Mode", mode, "-Environment", "backup", "-ServerHost", target.getHost(),
                "-ServerUser", target.getServerUser(), "-RemoteAppDir", target.getRemoteAppDir(),
                "-ExpectedResourceLeaseToken", binding.token(), "-ExpectedReleaseTag", workflow.releaseTag(),
                "-ExpectedExecutorHost", binding.executorHost(), "-ExpectedExecutorPid", binding.executorPid(),
                "-ExpectedExecutorIdentity", binding.executorIdentity(), "-RestoreIsolationMarkerPath", markerPath()));
        if (digest != null) args.addAll(List.of("-ExpectedPreviewDigest", digest, "-ConfirmText", "PROD"));
        return List.copyOf(args);
    }

    private Path script() {
        String root = properties.getReleaseWorkflow().getMaintenanceRepoRoot();
        if (root == null || root.isBlank()) throw new IllegalStateException("RECOVERY_EXECUTOR_ROOT_REQUIRED");
        Path path = Path.of(root).resolve("ops/deploy/recover-runtime-resource-lease.ps1").toAbsolutePath().normalize();
        if (!Files.isRegularFile(path)) throw new IllegalStateException("RECOVERY_EXECUTOR_MISSING");
        return path;
    }

    private String markerPath() {
        return RuntimeControlRestoreIsolationConfig.resolve(properties);
    }

    private String targetFingerprint() {
        var target = properties.requireBackupPublishTarget();
        return ReleaseWorkflowBackupAuthorizationService.digest(String.join("\n", target.getHost(), target.getServerUser(),
                target.getRemoteAppDir(), markerPath(), readText(script()),
                properties.getBackupOps().getExecutionMode(),
                new TreeMap<>(target.getTargets()).entrySet().stream()
                        .map(entry -> entry.getKey() + ":" + entry.getValue().getPort() + ":" + entry.getValue().getUrl())
                        .collect(java.util.stream.Collectors.joining("|")),
                Boolean.toString(properties.getReleaseWorkflow().isProductionWriteEnabled())));
    }

    private CommandResult execute(List<String> command) {
        Process process = null;
        Thread reader = null;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        var readFailure = new java.util.concurrent.atomic.AtomicReference<IOException>();
        Path log = path("run-" + UUID.randomUUID() + ".log");
        try {
            Files.createDirectories(log.getParent());
            // Never hand the log file to Windows children/consoles as an inheritable handle.
            process = new ProcessBuilder(command).redirectErrorStream(true).start();
            Process child = process;
            reader = new Thread(() -> {
                try (var input = child.getInputStream()) {
                    byte[] buffer = new byte[8192]; int count;
                    while ((count = input.read(buffer)) != -1) {
                        if (output.size() + count > 8 * 1024 * 1024) throw new IOException("RECOVERY_OUTPUT_LIMIT_EXCEEDED");
                        output.write(buffer, 0, count);
                    }
                } catch (IOException ex) { readFailure.set(ex); }
            }, "backup-recovery-output-" + process.pid());
            reader.setDaemon(true); reader.start();
            if (!process.waitFor(120, TimeUnit.SECONDS)) {
                throw new IllegalStateException("RECOVERY_EXECUTOR_TIMEOUT_UNKNOWN: " + log.getFileName());
            }
            reader.join(10000);
            if (reader.isAlive()) throw new IllegalStateException("RECOVERY_EXECUTOR_OUTPUT_UNCONFIRMED");
            if (readFailure.get() != null) throw readFailure.get();
            String text = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(output.toByteArray())).toString();
            return new CommandResult(process.exitValue(), text);
        } catch (IOException ex) { throw new IllegalStateException("RECOVERY_EXECUTOR_IO_UNKNOWN", ex); }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("RECOVERY_EXECUTOR_INTERRUPTED_UNKNOWN", ex);
        } finally {
            boolean interrupted = Thread.interrupted();
            try {
                if (process != null) {
                    if (process.isAlive() || (reader != null && reader.isAlive())) stopOwnedProcessTree(process);
                    process.getOutputStream().close();
                    process.getInputStream().close();
                    process.getErrorStream().close();
                }
                if (reader != null) {
                    reader.join(10000);
                    if (reader.isAlive()) throw new IllegalStateException("RECOVERY_OUTPUT_READER_TERMINATION_UNCONFIRMED");
                }
                // The Java writer is opened only after the process/reader lifecycle has ended, then closed here.
                Files.write(log, output.toByteArray());
            } catch (IOException ex) { throw new IllegalStateException("RECOVERY_OUTPUT_CLOSE_FAILED", ex); }
            catch (InterruptedException ex) {
                interrupted = true;
                throw new IllegalStateException("RECOVERY_OUTPUT_CLOSE_INTERRUPTED", ex);
            } finally { if (interrupted) Thread.currentThread().interrupt(); }
        }
    }

    private static void stopOwnedProcessTree(Process process) {
        // Retain handles before terminating the parent; never enumerate unrelated processes by name.
        List<ProcessHandle> owned = new ArrayList<>(process.descendants().toList());
        owned.add(process.toHandle());
        process.destroyForcibly();
        owned.forEach(handle -> { if (handle.isAlive()) handle.destroyForcibly(); });
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (owned.stream().anyMatch(ProcessHandle::isAlive)) {
            if (System.nanoTime() >= deadline) throw new IllegalStateException("RECOVERY_EXECUTOR_TREE_TERMINATION_UNCONFIRMED");
            try { Thread.sleep(25); }
            catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("RECOVERY_EXECUTOR_TREE_TERMINATION_INTERRUPTED", ex);
            }
        }
        try {
            if (!process.waitFor(10, TimeUnit.SECONDS)) throw new IllegalStateException("RECOVERY_EXECUTOR_REAPER_UNCONFIRMED");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("RECOVERY_EXECUTOR_REAPER_INTERRUPTED", ex);
        }
    }

    private void claim(String workflowId, Preview preview) {
        try {
            Path claim = path(workflowId + ".claim"); Files.createDirectories(claim.getParent());
            try (FileChannel channel = FileChannel.open(claim, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                ByteBuffer data = ByteBuffer.wrap(mapper.writeValueAsBytes(preview));
                while (data.hasRemaining()) channel.write(data); channel.force(true);
            }
        } catch (FileAlreadyExistsException ex) { throw new IllegalStateException("RECOVERY_DISPATCH_ALREADY_CLAIMED_OR_UNKNOWN", ex); }
        catch (IOException ex) { throw new IllegalStateException("RECOVERY_CLAIM_IO_UNKNOWN", ex); }
    }

    private Path path(String name) {
        if (!name.matches("[A-Za-z0-9._-]+")) throw new IllegalArgumentException("RECOVERY_IDENTIFIER_INVALID");
        return Path.of(properties.getStateDir()).resolve("backup-recovery").resolve(name);
    }
    private void write(Path path, Object value) {
        try {
            Files.createDirectories(path.getParent()); Path temp = Files.createTempFile(path.getParent(), "recovery-", ".tmp");
            mapper.writeValue(temp.toFile(), value); Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ex) { throw new IllegalStateException("RECOVERY_EVIDENCE_WRITE_FAILED", ex); }
    }
    private <T> T read(Path path, Class<T> type) {
        try { return mapper.readValue(readText(path), type); }
        catch (IOException ex) { throw new IllegalStateException("RECOVERY_EVIDENCE_INVALID", ex); }
    }
    private static String readText(Path path) {
        try {
            if (!Files.isRegularFile(path) || Files.size(path) > 8 * 1024 * 1024) throw new IllegalStateException("RECOVERY_EVIDENCE_MISSING_OR_TOO_LARGE");
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException ex) { throw new IllegalStateException("RECOVERY_EVIDENCE_UNREADABLE", ex); }
    }
    private static void requireActor(String actor) {
        if (actor == null || actor.isBlank()) throw new IllegalArgumentException("RECOVERY_ACTOR_REQUIRED");
    }
    private static String requiredMarker(String text, String key, String expression) {
        String value = optionalMarker(text, key, expression);
        if (value == null) throw new IllegalStateException("RECOVERY_IDENTITY_MISSING_OR_AMBIGUOUS: " + key);
        return value;
    }
    private static String optionalMarker(String text, String key, String expression) {
        var matcher = Pattern.compile("(?:^|\\s)" + Pattern.quote(key) + "=(" + expression + ")(?=\\s|$)").matcher(text);
        Set<String> matches = new HashSet<>(); while (matcher.find()) matches.add(matcher.group(1));
        return matches.size() == 1 ? matches.iterator().next() : null;
    }
    private static List<String> blockers(String output) {
        var matcher = Pattern.compile("(?:RECOVERY|RUNTIME_RESOURCE)_[A-Z_]+").matcher(output);
        Set<String> codes = new LinkedHashSet<>(); while (matcher.find()) codes.add(matcher.group());
        codes.removeAll(Set.of("RECOVERY_PREVIEW_DIGEST", "RECOVERY_RUNTIME_VERSION", "RUNTIME_RESOURCE_RECOVERED"));
        return codes.isEmpty() ? List.of("RECOVERY_INSPECTION_FAILED") : List.copyOf(codes);
    }
    private record Binding(String token, String executorHost, String executorPid, String executorIdentity, String fingerprint) { }
    public record CommandResult(int exitCode, String output) { }
    @FunctionalInterface public interface Runner { CommandResult run(List<String> command); }
    @FunctionalInterface public interface Completion {
        ReleaseWorkflowRecord complete(String workflowId, long version, String actor, String proof);
    }
    public record Preview(String previewId, String workflowId, long expectedStateVersion, String actor,
                          String targetFingerprint, String operationFingerprint, String evidenceDigest,
                          String runtimeVersion, boolean eligible, List<String> blockers, String nextAction,
                          Instant expiresAt) { }
}
