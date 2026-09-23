package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.io.CleanupMode;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

/** Runs the installed, approved pnpm against small real packages without external dependencies. */
class ReleaseWorkflowSourceDependenciesTest {
    @TempDir(cleanup = CleanupMode.ON_SUCCESS) Path temp;

    @Test void restoresFrozenDependenciesWithRealPackageManager() throws Exception {
        var fixture = fixture(",\"scripts\":{\"postinstall\":\"node confirm.cjs\"}");
        Files.writeString(fixture.frozen.frontendRoot().resolve("confirm.cjs"),
                "const fs=require('node:fs');fs.mkdirSync('node_modules',{recursive:true});"
                + "fs.writeFileSync('node_modules/install-ran.txt', 'actual lifecycle')", StandardCharsets.UTF_8);
        byte[] before = Files.readAllBytes(fixture.frozen.frontendRoot().resolve("pnpm-lock.yaml"));
        prepare(fixture);
        assertArrayEquals(before, Files.readAllBytes(fixture.frozen.frontendRoot().resolve("pnpm-lock.yaml")));
        assertEquals("actual lifecycle", Files.readString(fixture.frozen.frontendRoot().resolve("node_modules/install-ran.txt")));
    }

    @Test void propagatesRealInstallScriptFailure() throws Exception {
        var fixture = fixture(",\"scripts\":{\"postinstall\":\"node -e \\\"process.exit(23)\\\"\"}");
        Exception error = assertThrows(Exception.class, () -> prepare(fixture));
        assertTrue(error.getMessage().contains("DEPENDENCY_INSTALL_FAILED"), error.toString());
    }

    @Test void refusesSourceMutationByCommittedInstallScript() throws Exception {
        var fixture = fixture(",\"scripts\":{\"postinstall\":\"node mutate.cjs\"}");
        Files.writeString(fixture.frozen.frontendRoot().resolve("mutate.cjs"),
                "require('node:fs').appendFileSync('package.json', '\\n ')", StandardCharsets.UTF_8);
        Exception error = assertThrows(Exception.class, () -> prepare(fixture));
        assertTrue(error.getMessage().contains("DEPENDENCY_INPUT_DRIFT"), error.toString());
    }

    @Test void blocksUnapprovedToolchainBeforeInstalling() throws Exception {
        var fixture = fixture("");
        fixture.properties.getReleaseWorkflow().getClass().getMethod("setPnpmVersion", String.class)
                .invoke(fixture.properties.getReleaseWorkflow(), "0.0.1");
        Exception error = assertThrows(Exception.class, () -> prepare(fixture));
        assertTrue(error.getMessage().contains("DEPENDENCY_TOOLCHAIN_MISMATCH"), error.toString());
        assertFalse(Files.exists(fixture.frozen.frontendRoot().resolve("node_modules")));
    }

    @Test void interruptedInstallStopsItsOwnedLifecycleProcess() throws Exception {
        var fixture = fixture(",\"scripts\":{\"postinstall\":\"node hold.cjs\"}");
        Path pidFile = fixture.frozen.frontendRoot().resolve("node_modules/hold.pid");
        Files.writeString(fixture.frozen.frontendRoot().resolve("hold.cjs"),
                "const fs=require('node:fs');fs.mkdirSync('node_modules',{recursive:true});"
                + "fs.writeFileSync('node_modules/hold.pid',String(process.pid));setInterval(()=>{},1000)",
                StandardCharsets.UTF_8);
        CountDownLatch finished = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> running = executor.submit(() -> {
            try { prepare(fixture); }
            catch (Throwable error) { failure.set(error); }
            finally { finished.countDown(); }
        });
        Long childPid = null;
        try {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
            while (!Files.exists(pidFile) && System.nanoTime() < deadline && !running.isDone()) Thread.sleep(25);
            assertTrue(Files.exists(pidFile), "Real pnpm lifecycle did not start: " + failure.get());
            childPid = Long.parseLong(Files.readString(pidFile).trim());
            running.cancel(true);
            assertTrue(finished.await(10, TimeUnit.SECONDS), "Interrupted dependency preparation did not terminate");
            assertFalse(ProcessHandle.of(childPid).map(ProcessHandle::isAlive).orElse(false), "Owned install script survived cancellation");
            assertNotNull(failure.get());
            assertTrue(failure.get().getMessage().contains("DEPENDENCY_PREPARATION_INTERRUPTED"), failure.get().toString());
        } finally {
            running.cancel(true);
            if (childPid != null) ProcessHandle.of(childPid).filter(ProcessHandle::isAlive).ifPresent(ProcessHandle::destroyForcibly);
            executor.shutdownNow();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private Fixture fixture(String extraJson) throws Exception {
        Path frontend = Files.createDirectories(temp.resolve("application/IntRuoyiFronted"));
        Path backend = Files.createDirectories(temp.resolve("application/IntRuoyiBackend"));
        Path maintenance = Files.createDirectories(temp.resolve("maintenance"));
        Files.writeString(temp.resolve("application/.gitignore"), "node_modules/\n", StandardCharsets.UTF_8);
        Files.writeString(frontend.resolve("package.json"),
                "{\"name\":\"release-dependencies-real-test\",\"version\":\"1.0.0\",\"private\":true,"
                + "\"packageManager\":\"pnpm@10.25.0\"" + extraJson + "}\n", StandardCharsets.UTF_8);
        Files.writeString(frontend.resolve("pnpm-lock.yaml"),
                "lockfileVersion: '9.0'\n\nsettings:\n  autoInstallPeers: true\n  excludeLinksFromLockfile: false\n\nimporters:\n\n  .: {}\n",
                StandardCharsets.UTF_8);
        var properties = RuntimeControlProperties.createDefaultForTests(temp.resolve("state"));
        try {
            properties.getReleaseWorkflow().getClass().getMethod("setPnpmVersion", String.class)
                    .invoke(properties.getReleaseWorkflow(), "10.25.0");
        } catch (NoSuchMethodException error) {
            fail("Approved dependency toolchain version is missing", error);
        }
        return new Fixture(properties, new ReleaseWorkflowWorktreeFactory.FrozenWorktrees(
                maintenance, temp.resolve("application"), backend, frontend, "a".repeat(64)));
    }

    private static void prepare(Fixture fixture) throws Exception {
        Path repository = fixture.frozen.applicationRoot();
        git(repository, "init");
        git(repository, "config", "core.autocrlf", "false");
        git(repository, "config", "user.name", "dependency-fixture");
        git(repository, "config", "user.email", "dependency-fixture@example.invalid");
        git(repository, "add", ".");
        git(repository, "-c", "commit.gpgsign=false", "commit", "-m", "frozen dependency fixture");
        Class<?> type;
        try {
            type = Class.forName(ReleaseWorkflowSourceDependenciesTest.class.getPackageName()
                    + ".ReleaseWorkflowSourceDependencies");
        } catch (ClassNotFoundException error) {
            fail("Automatic frozen dependency preparation is missing", error);
            return;
        }
        Object subject = type.getConstructor(RuntimeControlProperties.class).newInstance(fixture.properties);
        try {
            type.getMethod("prepare", ReleaseWorkflowWorktreeFactory.FrozenWorktrees.class)
                    .invoke(subject, fixture.frozen);
        } catch (InvocationTargetException error) {
            if (error.getCause() instanceof Exception cause) throw cause;
            throw error;
        }
    }

    private static void git(Path root, String... args) throws Exception {
        List<String> command = new ArrayList<>(List.of("git", "-C", root.toString()));
        command.addAll(List.of(args));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, process.waitFor(), output);
    }

    private record Fixture(RuntimeControlProperties properties, ReleaseWorkflowWorktreeFactory.FrozenWorktrees frozen) {}
}
