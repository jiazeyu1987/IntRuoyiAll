package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.time.Duration;
import java.util.Map;

/** Creates only this workflow's registered, immutable source worktrees. Never resets an occupied path. */
@Component
public final class ReleaseWorkflowWorktreeFactory {
    private final RuntimeControlProperties properties;

    public ReleaseWorkflowWorktreeFactory(RuntimeControlProperties properties) {
        this.properties = properties;
    }

    public FrozenWorktrees prepare(ReleaseWorkflowRecord workflow) {
        RuntimeControlProperties.ReleaseWorkflow config = properties.getReleaseWorkflow();
        config.validate();
        if (!workflow.workflowId().matches("rw-(?:backup-)?[a-z0-9]{8,32}")
                || !workflow.applicationCommit().equalsIgnoreCase(workflow.frontendCommit())
                || !workflow.maintenanceCommit().equalsIgnoreCase(config.getApprovedMaintenanceCommit())
                || !workflow.applicationCommit().equalsIgnoreCase(config.getApprovedApplicationCommit())
                || !workflow.frontendCommit().equalsIgnoreCase(config.getApprovedFrontendCommit())) {
            throw new IllegalStateException("SOURCE_APPROVED_TUPLE_MISMATCH");
        }
        Path root = controlledPath(config.getWorktreeRoot());
        Path maintenanceSource = controlledPath(config.getMaintenanceRepoRoot());
        Path applicationSource = controlledPath(config.getApplicationRepoRoot());
        if (overlaps(root, maintenanceSource) || overlaps(root, applicationSource)
                || overlaps(maintenanceSource, applicationSource)) {
            throw new IllegalStateException("SOURCE_WORKTREE_ROOT_OVERLAP");
        }
        verifySource(maintenanceSource, workflow.maintenanceCommit());
        verifySource(applicationSource, workflow.applicationCommit());
        Path workflowRoot = root.resolve(workflow.workflowId());
        Path maintenance = workflowRoot.resolve("maintenance");
        Path application = workflowRoot.resolve("application");
        try {
            rejectRedirectedAncestors(root);
            rejectRedirectedAncestors(workflowRoot);
            Files.createDirectories(root.resolve(".locks"));
            rejectRedirectedAncestors(root.resolve(".locks"));
            Path lockPath = root.resolve(".locks").resolve(workflow.workflowId() + ".lock");
            rejectRedirectedAncestors(lockPath);
            try (FileChannel channel = FileChannel.open(lockPath,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                 FileLock lock = acquire(channel)) {
                Files.createDirectories(workflowRoot);
                String binding = binding(workflow, maintenanceSource, applicationSource);
                Path owner = workflowRoot.resolve("source-binding.txt");
                rejectRedirectedAncestors(owner);
                if (Files.exists(owner)) {
                    if (!Files.readString(owner, StandardCharsets.UTF_8).equals(binding)) {
                        throw new IllegalStateException("SOURCE_WORKTREE_BINDING_DRIFT");
                    }
                } else {
                    if (Files.exists(maintenance) || Files.exists(application)) {
                        throw new IllegalStateException("SOURCE_WORKTREE_DESTINATION_OCCUPIED");
                    }
                    Files.writeString(owner, binding, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
                }
                prepareOne(maintenanceSource, maintenance, workflow.maintenanceCommit());
                prepareOne(applicationSource, application, workflow.applicationCommit());
                return verifiedLayout(maintenance, application);
            }
        } catch (IOException | NoSuchAlgorithmException error) {
            throw new IllegalStateException("SOURCE_WORKTREE_PREPARATION_FAILED", error);
        }
    }

    /** ACK recovery validates the original sources; it must never rebuild missing evidence. */
    public FrozenWorktrees requirePrepared(ReleaseWorkflowRecord workflow) {
        var config = properties.getReleaseWorkflow();
        config.validate();
        if (!workflow.workflowId().matches("rw-(?:backup-)?[a-z0-9]{8,32}")
                || !workflow.applicationCommit().equalsIgnoreCase(workflow.frontendCommit())) {
            throw new IllegalStateException("SOURCE_APPROVED_TUPLE_MISMATCH");
        }
        Path root = controlledPath(config.getWorktreeRoot());
        Path maintenanceSource = controlledPath(config.getMaintenanceRepoRoot());
        Path applicationSource = controlledPath(config.getApplicationRepoRoot());
        if (overlaps(root, maintenanceSource) || overlaps(root, applicationSource)
                || overlaps(maintenanceSource, applicationSource)) {
            throw new IllegalStateException("SOURCE_WORKTREE_ROOT_OVERLAP");
        }
        Path workflowRoot = root.resolve(workflow.workflowId());
        Path owner = workflowRoot.resolve("source-binding.txt");
        Path lockPath = root.resolve(".locks").resolve(workflow.workflowId() + ".lock");
        Path maintenance = workflowRoot.resolve("maintenance");
        Path application = workflowRoot.resolve("application");
        try {
            rejectRedirectedAncestors(owner);
            rejectRedirectedAncestors(lockPath);
            if (!Files.isRegularFile(owner) || !Files.isRegularFile(lockPath)) {
                throw new IllegalStateException("SOURCE_PREPARED_EVIDENCE_MISSING");
            }
            // No CREATE option: a missing preparation lock is evidence loss, not a new preparation request.
            try (FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.WRITE);
                 FileLock lock = acquire(channel)) {
                if (!Files.readString(owner, StandardCharsets.UTF_8)
                        .equals(binding(workflow, maintenanceSource, applicationSource))) {
                    throw new IllegalStateException("SOURCE_WORKTREE_BINDING_DRIFT");
                }
                verifyPrepared(maintenanceSource, maintenance, workflow.maintenanceCommit());
                verifyPrepared(applicationSource, application, workflow.applicationCommit());
                return verifiedLayout(maintenance, application);
            }
        } catch (IOException | NoSuchAlgorithmException error) {
            throw new IllegalStateException("SOURCE_PREPARED_VERIFICATION_FAILED", error);
        }
    }

    private String binding(ReleaseWorkflowRecord workflow, Path maintenanceSource, Path applicationSource) {
        return String.join("\n", "release-worktree-v1", workflow.workflowId(), workflow.releaseTag(),
                maintenanceSource.toString(), applicationSource.toString(), workflow.maintenanceCommit(),
                workflow.applicationCommit(), workflow.frontendCommit(),
                properties.getReleaseWorkflow().getExpectedPublishScriptSha256().toLowerCase()) + "\n";
    }

    private FrozenWorktrees verifiedLayout(Path maintenance, Path application) throws IOException, NoSuchAlgorithmException {
        Path backend = application.resolve("IntRuoyiBackend");
        Path frontend = application.resolve("IntRuoyiFronted");
        Path script = maintenance.resolve(properties.getReleaseWorkflow().getPublishScriptPath());
        rejectRedirectedAncestors(backend);
        rejectRedirectedAncestors(frontend);
        rejectRedirectedAncestors(script);
        if (!Files.isDirectory(backend) || !Files.isDirectory(frontend) || !Files.isRegularFile(script)) {
            throw new IllegalStateException("SOURCE_ROLE_PATH_MISSING");
        }
        String sha256 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(script)));
        if (!sha256.equalsIgnoreCase(properties.getReleaseWorkflow().getExpectedPublishScriptSha256())) {
            throw new IllegalStateException("RELEASE_EXECUTOR_DIGEST_MISMATCH");
        }
        return new FrozenWorktrees(maintenance, application, backend, frontend, sha256);
    }

    private static FileLock acquire(FileChannel channel) throws IOException {
        try {
            FileLock lock = channel.tryLock();
            if (lock == null) throw new IllegalStateException("SOURCE_PREPARATION_BUSY");
            return lock;
        } catch (OverlappingFileLockException error) {
            throw new IllegalStateException("SOURCE_PREPARATION_BUSY", error);
        }
    }

    /** Called only after verified deployment; failures retain sources and surface as cleanup errors. */
    public void cleanup(ReleaseWorkflowRecord workflow) {
        if (workflow.state() != ReleaseWorkflowRecord.State.BACKUP_DEPLOYED || workflow.backupIntent() == null
                || workflow.packageDigest() == null || workflow.manifestDigest() == null
                || workflow.evidenceRefs().isEmpty()
                || !workflow.workflowId().matches("rw-backup-[a-z0-9]{8,32}")) {
            throw new IllegalStateException("SOURCE_CLEANUP_VERIFIED_DEPLOYMENT_REQUIRED");
        }
        var config = properties.getReleaseWorkflow();
        Path root = controlledPath(config.getWorktreeRoot());
        Path maintenanceSource = controlledPath(config.getMaintenanceRepoRoot());
        Path applicationSource = controlledPath(config.getApplicationRepoRoot());
        if (overlaps(root, maintenanceSource) || overlaps(root, applicationSource)) {
            throw new IllegalStateException("SOURCE_WORKTREE_ROOT_OVERLAP");
        }
        Path workflowRoot = root.resolve(workflow.workflowId());
        Path maintenance = workflowRoot.resolve("maintenance");
        Path application = workflowRoot.resolve("application");
        Path receipt = controlledPath(properties.getStateDir()).resolve("release-workflows/worktree-archives")
                .resolve(workflow.workflowId() + ".txt");
        String binding = binding(workflow, maintenanceSource, applicationSource);
        try {
            rejectRedirectedAncestors(workflowRoot);
            rejectRedirectedAncestors(receipt);
            Path lockPath = root.resolve(".locks").resolve(workflow.workflowId() + ".lock");
            rejectRedirectedAncestors(lockPath);
            try (FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.WRITE);
                 FileLock lock = acquire(channel)) {
                boolean archived = Files.isRegularFile(receipt)
                        && Files.readString(receipt, StandardCharsets.UTF_8).equals(binding);
                if (!Files.exists(workflowRoot)) {
                    if (!archived || registered(maintenanceSource, maintenance) || registered(applicationSource, application)) {
                        throw new IllegalStateException("SOURCE_CLEANUP_EVIDENCE_MISSING");
                    }
                    return;
                }
                Path owner = workflowRoot.resolve("source-binding.txt");
                rejectRedirectedAncestors(owner);
                if (!Files.isRegularFile(owner) || !Files.readString(owner, StandardCharsets.UTF_8).equals(binding)) {
                    throw new IllegalStateException("SOURCE_WORKTREE_BINDING_DRIFT");
                }
                try (var entries = Files.list(workflowRoot)) {
                    if (entries.anyMatch(path -> !List.of("maintenance", "application", "source-binding.txt")
                            .contains(path.getFileName().toString()))) {
                        throw new IllegalStateException("SOURCE_CLEANUP_UNOWNED_FILES");
                    }
                }
                for (var pair : List.of(new Path[]{maintenanceSource, maintenance}, new Path[]{applicationSource, application})) {
                    if (Files.exists(pair[1])) {
                        verifyPrepared(pair[0], pair[1], pair[1].equals(maintenance)
                                ? workflow.maintenanceCommit() : workflow.applicationCommit());
                    } else if (!archived || registered(pair[0], pair[1])) {
                        throw new IllegalStateException("SOURCE_CLEANUP_REGISTRATION_DRIFT");
                    }
                }
                if (!archived) {
                    Files.createDirectories(receipt.getParent());
                    Files.writeString(receipt, binding, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
                }
                if (Files.exists(maintenance)) git(maintenanceSource, "worktree", "remove", "--force", maintenance.toString());
                if (Files.exists(application)) git(applicationSource, "worktree", "remove", "--force", application.toString());
                if (Files.exists(maintenance) || Files.exists(application)
                        || registered(maintenanceSource, maintenance) || registered(applicationSource, application)) {
                    throw new IllegalStateException("SOURCE_CLEANUP_INCOMPLETE");
                }
                Files.delete(owner);
                Files.delete(workflowRoot);
            }
        } catch (IOException error) {
            throw new IllegalStateException("SOURCE_CLEANUP_FAILED", error);
        }
    }

    private static void verifySource(Path source, String commit) {
        if (!Files.isDirectory(source)
                || !Path.of(git(source, "rev-parse", "--show-toplevel")).toAbsolutePath().normalize().equals(source)
                || !"commit".equals(git(source, "cat-file", "-t", commit))) {
            throw new IllegalStateException("SOURCE_REPOSITORY_OR_COMMIT_INVALID");
        }
    }

    private static void prepareOne(Path source, Path target, String commit) throws IOException {
        rejectRedirectedAncestors(target);
        if (!Files.exists(target)) {
            git(source, "-c", "core.longpaths=true", "worktree", "add", "--detach", target.toString(), commit);
        }
        verifyPrepared(source, target, commit);
    }

    private static void verifyPrepared(Path source, Path target, String commit) throws IOException {
        rejectRedirectedAncestors(target);
        if (!Files.isRegularFile(target.resolve(".git"), LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalStateException("SOURCE_WORKTREE_NOT_REGISTERED");
        }
        Path actualRoot = Path.of(git(target, "rev-parse", "--show-toplevel")).toAbsolutePath().normalize();
        Path sourceCommon = Path.of(git(source, "rev-parse", "--path-format=absolute", "--git-common-dir"))
                .toAbsolutePath().normalize();
        Path targetCommon = Path.of(git(target, "rev-parse", "--path-format=absolute", "--git-common-dir"))
                .toAbsolutePath().normalize();
        if (!registered(source, target) || !target.equals(actualRoot) || !sourceCommon.equals(targetCommon)) {
            throw new IllegalStateException("SOURCE_WORKTREE_NOT_REGISTERED");
        }
        if (!commit.equalsIgnoreCase(git(target, "rev-parse", "HEAD"))) {
            throw new IllegalStateException("SOURCE_COMMIT_DRIFT");
        }
        if (!git(target, "--no-optional-locks", "status", "--porcelain", "--untracked-files=all").isBlank()) {
            throw new IllegalStateException("SOURCE_DIRTY");
        }
    }

    private static boolean registered(Path source, Path target) {
        String registration = "worktree " + target.toString().replace('\\', '/');
        return git(source, "-c", "core.quotePath=false", "worktree", "list", "--porcelain")
                .lines().anyMatch(registration::equalsIgnoreCase);
    }

    private static Path controlledPath(String value) {
        if (value == null || value.isBlank() || value.contains("\n") || value.contains("\r")) {
            throw new IllegalStateException("SOURCE_WORKTREE_CONFIGURATION_MISSING");
        }
        Path path = Path.of(value);
        if (!path.isAbsolute() || path.getParent() == null) {
            throw new IllegalStateException("SOURCE_WORKTREE_PATH_INVALID");
        }
        path = path.normalize();
        try {
            rejectRedirectedAncestors(path);
        } catch (IOException error) {
            throw new IllegalStateException("SOURCE_WORKTREE_PATH_INSPECTION_FAILED", error);
        }
        return path;
    }

    private static boolean overlaps(Path left, Path right) {
        return left.startsWith(right) || right.startsWith(left);
    }

    private static void rejectRedirectedAncestors(Path path) throws IOException {
        for (Path part = path; part != null; part = part.getParent()) {
            if (Files.exists(part, LinkOption.NOFOLLOW_LINKS)
                    && (Files.isSymbolicLink(part) || !part.toRealPath().equals(part.toAbsolutePath().normalize()))) {
                throw new IllegalStateException("SOURCE_WORKTREE_REDIRECTED_PATH");
            }
        }
    }

    private static String git(Path root, String... arguments) {
        List<String> command = new ArrayList<>(List.of("git", "-C", root.toString()));
        command.addAll(List.of(arguments));
        var result = ReleaseWorkflowLocalProcess.run(command, null, Map.of(), Duration.ofMinutes(20), "SOURCE_GIT");
        if (result.exitCode() != 0) {
            throw new IllegalStateException("SOURCE_GIT_FAILED: " + arguments[0] + ", exit=" + result.exitCode());
        }
        return result.output().trim();
    }

    public record FrozenWorktrees(Path maintenanceRoot, Path applicationRoot, Path backendRoot,
                                  Path frontendRoot, String publishScriptSha256) {}
}
