package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReleaseWorkflowWorktreeFactoryTest {
    @TempDir Path temp;

    @ParameterizedTest
    @ValueSource(strings = {"rw-abcdef001234", "rw-backup-abcdef001234567890abcdef00123456"})
    void freezesOnlyApprovedCommitsAndResumesSameRegisteredWorktrees(String workflowId) throws Exception {
        Fixture fixture = fixture(workflowId);
        Files.writeString(fixture.application.resolve("IntRuoyiBackend/source.txt"), "uncommitted edit");
        Files.writeString(fixture.application.resolve("not-committed.txt"), "must not ship");
        Object first = prepare(fixture, fixture.workflow);
        Path app = path(first, "applicationRoot");
        Path maintenance = path(first, "maintenanceRoot");
        assertEquals("approved", Files.readString(app.resolve("IntRuoyiBackend/source.txt")));
        assertFalse(Files.exists(app.resolve("not-committed.txt")));
        assertEquals(fixture.workflow.applicationCommit(), git(app, "rev-parse", "HEAD"));
        assertEquals(fixture.workflow.maintenanceCommit(), git(maintenance, "rev-parse", "HEAD"));
        assertEquals("", git(app, "status", "--porcelain"));
        assertEquals(app.resolve("IntRuoyiBackend"), path(first, "backendRoot"));
        assertEquals(app.resolve("IntRuoyiFronted"), path(first, "frontendRoot"));
        assertEquals(fixture.properties.getReleaseWorkflow().getExpectedPublishScriptSha256(),
                first.getClass().getMethod("publishScriptSha256").invoke(first));
        String registrations = git(fixture.application, "worktree", "list", "--porcelain");
        Object resumed = prepare(fixture, fixture.workflow);
        assertEquals(app, path(resumed, "applicationRoot"));
        assertEquals(registrations, git(fixture.application, "worktree", "list", "--porcelain"));
        assertEquals("uncommitted edit", Files.readString(fixture.application.resolve("IntRuoyiBackend/source.txt")));
    }

    @Test
    void refusesDirtyPreparedTreeInsteadOfResettingIt() throws Exception {
        Fixture fixture = fixture();
        Path app = path(prepare(fixture, fixture.workflow), "applicationRoot");
        Files.writeString(app.resolve("IntRuoyiBackend/source.txt"), "unexpected writer");
        Exception error = assertThrows(Exception.class, () -> prepare(fixture, fixture.workflow));
        assertTrue(error.getMessage().contains("SOURCE_DIRTY"), error.toString());
        assertEquals("unexpected writer", Files.readString(app.resolve("IntRuoyiBackend/source.txt")));
    }

    @Test
    void refusesUnregisteredOccupiedDestinationWithoutOverwriting() throws Exception {
        Fixture fixture = fixture();
        Path occupied = temp.resolve("worktrees").resolve(fixture.workflow.workflowId()).resolve("application");
        Files.createDirectories(occupied);
        Files.writeString(occupied.resolve("owned-by-someone-else.txt"), "keep");
        assertThrows(Exception.class, () -> prepare(fixture, fixture.workflow));
        assertEquals("keep", Files.readString(occupied.resolve("owned-by-someone-else.txt")));
    }

    @Test
    void refusesExecutorDigestDrift() throws Exception {
        Fixture fixture = fixture();
        fixture.properties.getReleaseWorkflow().setExpectedPublishScriptSha256("0".repeat(64));
        Exception error = assertThrows(Exception.class, () -> prepare(fixture, fixture.workflow));
        assertTrue(error.getMessage().contains("DIGEST"), error.toString());
    }

    @Test
    void refusesDifferentFrontendCommitBeforePreparingSources() throws Exception {
        Fixture fixture = fixture();
        ReleaseWorkflowRecord invalid = ReleaseWorkflowRecord.newWorkflow("rw-abcdef001234", "release-test-app",
                Instant.now(), "preset-app-release", "1", "approved-source",
                fixture.workflow.maintenanceCommit(), fixture.workflow.applicationCommit(), "a".repeat(40));
        Exception error = assertThrows(Exception.class, () -> prepare(fixture, invalid));
        assertTrue(error.getMessage().contains("SOURCE"), error.toString());
        assertFalse(Files.exists(temp.resolve("worktrees").resolve(invalid.workflowId()).resolve("application")));
    }

    @Test void removesOnlyVerifiedSuccessfulWorktreesAndKeepsArchivedSourceBinding() throws Exception {
        Fixture fixture = fixture("rw-backup-abcdef001234567890abcdef00123456");
        Object frozen = prepare(fixture, fixture.workflow);
        Path app = path(frozen, "applicationRoot");
        Path maintenance = path(frozen, "maintenanceRoot");
        ReleaseWorkflowRecord success = terminal(fixture.workflow, ReleaseWorkflowRecord.State.BACKUP_DEPLOYED);
        cleanup(fixture, success);
        assertFalse(Files.exists(app));
        assertFalse(Files.exists(maintenance));
        assertTrue(Files.isDirectory(fixture.application));
        assertFalse(git(fixture.application, "worktree", "list", "--porcelain").contains(app.toString().replace('\\', '/')));
        Path receipt = temp.resolve("state/release-workflows/worktree-archives")
                .resolve(fixture.workflow.workflowId() + ".txt");
        assertTrue(Files.readString(receipt).contains(fixture.workflow.applicationCommit()));
        cleanup(fixture, success);
    }

    @Test void cleanupRejectsRecoveryStateAndDirtySourcesWithoutDeletingEitherTree() throws Exception {
        Fixture fixture = fixture("rw-backup-abcdef001234567890abcdef00123456");
        Object frozen = prepare(fixture, fixture.workflow);
        Path app = path(frozen, "applicationRoot");
        Path maintenance = path(frozen, "maintenanceRoot");
        assertThrows(Exception.class, () -> cleanup(fixture,
                terminal(fixture.workflow, ReleaseWorkflowRecord.State.RECOVERY_REQUIRED)));
        Files.writeString(app.resolve("IntRuoyiBackend/source.txt"), "preserve unexpected edit");
        assertThrows(Exception.class, () -> cleanup(fixture,
                terminal(fixture.workflow, ReleaseWorkflowRecord.State.BACKUP_DEPLOYED)));
        assertTrue(Files.isDirectory(app));
        assertTrue(Files.isDirectory(maintenance));
        assertEquals("preserve unexpected edit", Files.readString(app.resolve("IntRuoyiBackend/source.txt")));
    }

    @Test void requirePreparedNeverCreatesMissingSourceTrees() throws Exception {
        Fixture fixture = fixture();
        String registeredBefore = git(fixture.application, "worktree", "list", "--porcelain");
        assertThrows(Exception.class, () -> requirePrepared(fixture));
        assertFalse(Files.exists(temp.resolve("worktrees")));
        assertEquals(registeredBefore, git(fixture.application, "worktree", "list", "--porcelain"));
    }

    @Test void requirePreparedReadsExistingBindingAndNeverRestoresMissingFiles() throws Exception {
        Fixture fixture = fixture();
        Object prepared = prepare(fixture, fixture.workflow);
        String registeredBefore = git(fixture.application, "worktree", "list", "--porcelain");
        Object verified = requirePrepared(fixture);
        assertEquals(path(prepared, "applicationRoot"), path(verified, "applicationRoot"));
        Path script = path(prepared, "maintenanceRoot").resolve("ops/deploy/publish-int-ruoyi.ps1");
        Files.delete(script);
        assertThrows(Exception.class, () -> requirePrepared(fixture));
        assertFalse(Files.exists(script));
        assertEquals(registeredBefore, git(fixture.application, "worktree", "list", "--porcelain"));
    }

    private static Object requirePrepared(Fixture fixture) throws Exception {
        Class<?> type = Class.forName(ReleaseWorkflowWorktreeFactoryTest.class.getPackageName()
                + ".ReleaseWorkflowWorktreeFactory");
        Object factory = type.getConstructor(RuntimeControlProperties.class).newInstance(fixture.properties);
        java.lang.reflect.Method method;
        try { method = type.getMethod("requirePrepared", ReleaseWorkflowRecord.class); }
        catch (NoSuchMethodException error) { return fail("Read-only frozen source verification is missing", error); }
        try { return method.invoke(factory, fixture.workflow); }
        catch (InvocationTargetException error) {
            if (error.getCause() instanceof Exception cause) throw cause;
            throw error;
        }
    }

    private static ReleaseWorkflowRecord terminal(ReleaseWorkflowRecord source, ReleaseWorkflowRecord.State state) {
        return new ReleaseWorkflowRecord(source.workflowId(), source.releaseTag(), source.publishScope(),
                source.presetId(), source.presetVersion(), state, source.stateVersion() + 1, 1,
                "op-abcdef123456", null, null, false, List.of("operation/verified"),
                "a".repeat(64), "b".repeat(64), null, null,
                source.createdAt(), Instant.now(), Instant.now(), false, source.requestedBy(), source.reason(),
                source.sourceSelectionId(), source.maintenanceCommit(), source.applicationCommit(), source.frontendCommit(),
                source.backupIntent());
    }

    private static void cleanup(Fixture fixture, ReleaseWorkflowRecord workflow) throws Exception {
        Class<?> type = Class.forName(ReleaseWorkflowWorktreeFactoryTest.class.getPackageName()
                + ".ReleaseWorkflowWorktreeFactory");
        Object factory = type.getConstructor(RuntimeControlProperties.class).newInstance(fixture.properties);
        java.lang.reflect.Method method;
        try {
            method = type.getMethod("cleanup", ReleaseWorkflowRecord.class);
        } catch (NoSuchMethodException error) {
            fail("Verified workflow source cleanup is missing", error);
            return;
        }
        try { method.invoke(factory, workflow); }
        catch (InvocationTargetException error) {
            if (error.getCause() instanceof Exception cause) throw cause;
            throw error;
        }
    }

    private Fixture fixture() throws Exception {
        return fixture("rw-abcdef001234");
    }

    private Fixture fixture(String workflowId) throws Exception {
        Path maintenance = Files.createDirectories(temp.resolve("maintenance-source"));
        Path application = Files.createDirectories(temp.resolve("application-source"));
        Files.createDirectories(maintenance.resolve("ops/deploy"));
        byte[] executor = "Write-Output 'managed executor'\n".getBytes(StandardCharsets.UTF_8);
        Files.write(maintenance.resolve("ops/deploy/publish-int-ruoyi.ps1"), executor);
        Files.createDirectories(application.resolve("IntRuoyiBackend"));
        Files.createDirectories(application.resolve("IntRuoyiFronted"));
        Files.writeString(application.resolve("IntRuoyiBackend/source.txt"), "approved");
        Files.writeString(application.resolve("IntRuoyiFronted/source.txt"), "approved frontend");
        String maintenanceCommit = initialize(maintenance);
        String applicationCommit = initialize(application);
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(temp.resolve("state"));
        properties.getReleaseWorkflow().setMaintenanceRepoRoot(maintenance.toString());
        properties.getReleaseWorkflow().setApplicationRepoRoot(application.toString());
        properties.getReleaseWorkflow().setApprovedMaintenanceCommit(maintenanceCommit);
        properties.getReleaseWorkflow().setApprovedApplicationCommit(applicationCommit);
        properties.getReleaseWorkflow().setApprovedFrontendCommit(applicationCommit);
        properties.getReleaseWorkflow().setExpectedPublishScriptSha256(
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(executor)));
        // Reflection keeps the pre-implementation RED an executed assertion, not a compilation failure.
        try {
            properties.getReleaseWorkflow().getClass().getMethod("setWorktreeRoot", String.class)
                    .invoke(properties.getReleaseWorkflow(), temp.resolve("worktrees").toString());
        } catch (NoSuchMethodException error) {
            fail("Approved release worktree root configuration is missing", error);
        }
        Instant now = Instant.now();
        ReleaseWorkflowRecord.BackupIntent intent = workflowId.startsWith("rw-backup-")
                ? new ReleaseWorkflowRecord.BackupIntent("ba-factory-test", "bp-factory-test",
                    "a".repeat(64), "factory-request-key") : null;
        ReleaseWorkflowRecord workflow = new ReleaseWorkflowRecord(workflowId, "release-test-app",
                ReleaseWorkflowContract.PUBLISH_SCOPE, "preset-app-release", "1",
                ReleaseWorkflowRecord.State.SOURCE_FREEZING, 0, 1, null, null, null, false, List.of(),
                null, null, null, null, now, now, now, false, "factory-test", "freeze approved source",
                "approved-source", maintenanceCommit, applicationCommit, applicationCommit, intent);
        return new Fixture(properties, workflow, maintenance, application);
    }

    private static String initialize(Path root) throws Exception {
        git(root, "init");
        git(root, "config", "core.autocrlf", "false");
        git(root, "config", "user.name", "release-factory-test");
        git(root, "config", "user.email", "release-factory-test@example.invalid");
        git(root, "add", ".");
        git(root, "-c", "commit.gpgsign=false", "commit", "-m", "approved source fixture");
        return git(root, "rev-parse", "HEAD");
    }

    private static String git(Path root, String... arguments) throws Exception {
        List<String> command = new ArrayList<>(List.of("git", "-C", root.toString()));
        command.addAll(List.of(arguments));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        assertEquals(0, process.waitFor(), output);
        return output;
    }

    private static Object prepare(Fixture fixture, ReleaseWorkflowRecord workflow) throws Exception {
        Class<?> type;
        try {
            type = Class.forName(ReleaseWorkflowWorktreeFactoryTest.class.getPackageName()
                    + ".ReleaseWorkflowWorktreeFactory");
        } catch (ClassNotFoundException error) {
            return fail("Server-owned release worktree factory is missing", error);
        }
        Object factory = type.getConstructor(RuntimeControlProperties.class).newInstance(fixture.properties);
        try {
            return type.getMethod("prepare", ReleaseWorkflowRecord.class).invoke(factory, workflow);
        } catch (InvocationTargetException error) {
            if (error.getCause() instanceof Exception cause) throw cause;
            throw error;
        }
    }

    private static Path path(Object result, String accessor) throws Exception {
        return (Path) result.getClass().getMethod(accessor).invoke(result);
    }

    private record Fixture(RuntimeControlProperties properties, ReleaseWorkflowRecord workflow,
                           Path maintenance, Path application) {}
}
