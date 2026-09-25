package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class RuntimeControlCommandExecutorImplTest {

    @TempDir
    private Path tempDir;

    @Test
    void threeArgumentRegistrationIsMandatoryAndLegacyOverloadProvidesEmptyBindings() throws Exception {
        Method boundRegistration = RuntimeControlCommandExecutor.class.getMethod(
                "registerOperation", String.class, Path.class, Map.class);
        Method legacyRegistration = RuntimeControlCommandExecutor.class.getMethod(
                "registerOperation", String.class, Path.class);
        assertTrue(Modifier.isAbstract(boundRegistration.getModifiers()),
                "implementations must explicitly support immutable security bindings");
        assertTrue(legacyRegistration.isDefault());

        AtomicReference<Object[]> delegatedArguments = new AtomicReference<>();
        RuntimeControlCommandExecutor executor = (RuntimeControlCommandExecutor) Proxy.newProxyInstance(
                RuntimeControlCommandExecutor.class.getClassLoader(),
                new Class<?>[]{RuntimeControlCommandExecutor.class},
                (proxy, method, arguments) -> {
                    if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method, arguments);
                    if ("registerOperation".equals(method.getName())) {
                        delegatedArguments.set(arguments);
                        return null;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });

        Path logPath = tempDir.resolve("legacy-registration.log");
        executor.registerOperation("op-legacy-12345678", logPath);

        assertTrue(delegatedArguments.get() != null);
        assertEquals(3, delegatedArguments.get().length);
        assertEquals("op-legacy-12345678", delegatedArguments.get()[0]);
        assertEquals(logPath, delegatedArguments.get()[1]);
        assertEquals(Map.of(), delegatedArguments.get()[2]);
    }

    @Test
    void silentRegisteredOperationHasLiveExecutorUntilCanceled() throws Exception {
        Method liveness = assertDoesNotThrow(() -> RuntimeControlCommandExecutorImpl.class
                .getMethod("isOperationExecutorAlive", String.class), "executor liveness must be independent of logs");
        RuntimeControlCommandExecutorImpl executor = executorWithProperties(propertiesWithRepoRoot(tempDir.toString()));
        Path script = writePowerShellScript("Start-Sleep -Seconds 30");
        Path log = tempDir.resolve("silent-operation.log");
        String operationId = "op-silent-12345678";
        assertFalse((Boolean) liveness.invoke(executor, operationId));
        executor.registerOperation(operationId, log);
        assertFalse((Boolean) liveness.invoke(executor, operationId), "queued is not proof of a living process");
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread worker = new Thread(() -> {
            try {
                executor.executeOperation(command(script), log);
            } catch (Throwable ex) {
                failure.set(ex);
            }
        });
        worker.start();
        try {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (!(Boolean) liveness.invoke(executor, operationId) && System.nanoTime() < deadline) {
                Thread.sleep(20);
            }
            assertTrue((Boolean) liveness.invoke(executor, operationId), String.valueOf(failure.get()));
            var lastWrite = Files.getLastModifiedTime(log);
            Thread.sleep(250);
            assertEquals(lastWrite, Files.getLastModifiedTime(log));
            assertTrue((Boolean) liveness.invoke(executor, operationId), "silent process remains alive");
            assertTrue(executor.cancelOperation(operationId));
            worker.join(10000);
            assertFalse(worker.isAlive());
            assertFalse((Boolean) liveness.invoke(executor, operationId));
        } finally {
            executor.cancelOperation(operationId);
            worker.interrupt();
            worker.join(10000);
        }
    }

    @Test
    void registeredImmutableBuildBindingsAreWrittenBeforeTheCommandLog() throws Exception {
        RuntimeControlCommandExecutorImpl executor = executorWithProperties(propertiesWithRepoRoot(tempDir.toString()));
        Path script = writePowerShellScript("Write-Output 'runtime-ok'");
        Path log = tempDir.resolve("bound-operation.log");
        String hostDigest = "a".repeat(64);
        List<String> commandLine = RuntimeControlCommandExecutorImpl.buildCommandLine(script);
        String commandDigest = cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow
                .ReleaseWorkflowCommandFingerprint.calculate("local", "intruoyi-backend", "<repoRoot>",
                        String.join(" ", commandLine));

        executor.registerOperation("op-bound-12345678", log, Map.of(
                "executorHostIdentitySha256", hostDigest,
                "releaseWorkflowCommandSha256", commandDigest));
        executor.executeOperation(command(script), log);

        String contents = Files.readString(log, StandardCharsets.UTF_8);
        assertTrue(contents.contains("executorHostIdentitySha256=" + hostDigest));
        assertTrue(contents.contains("releaseWorkflowCommandSha256=" + commandDigest));
        assertEquals(commandDigest, cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow
                .ReleaseWorkflowCommandFingerprint.calculate("local", "intruoyi-backend", "<repoRoot>",
                        contents.lines().filter(line -> line.startsWith("command="))
                                .map(line -> line.substring("command=".length())).findFirst().orElseThrow()));
    }

    @Test
    void executeForOutputShouldFailFastWhenRepoRootBlank() throws Exception {
        RuntimeControlProperties properties = propertiesWithRepoRoot("");
        RuntimeControlCommandExecutorImpl executor = executorWithProperties(properties);
        Path script = writePowerShellScript("Write-Output 'runtime-ok'");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> executor.executeForOutput(command(script), Duration.ofSeconds(5)));

        assertTrue(exception.getMessage().contains("repoRoot missing"),
                "blank repoRoot should be reported before ProcessBuilder receives an empty working directory");
        assertFalse(exception.getMessage().contains("CreateProcess error=123"),
                "blank repoRoot must not leak the Windows empty-directory CreateProcess error");
    }

    @Test
    void executeForOutputShouldUseConfiguredRepoRootAsWorkingDirectory() throws Exception {
        RuntimeControlProperties properties = propertiesWithRepoRoot(tempDir.toString());
        RuntimeControlCommandExecutorImpl executor = executorWithProperties(properties);
        Path script = writePowerShellScript("Write-Output ((Get-Location).Path)");

        String output = executor.executeForOutput(command(script), Duration.ofSeconds(5)).trim();

        assertEquals(tempDir.toAbsolutePath().normalize().toString(), output);
    }

    @Test
    void executeForOutputShouldUseExplicitWorkflowWorkingDirectoryWithoutLegacyRepoRoot() throws Exception {
        RuntimeControlProperties properties = propertiesWithRepoRoot(tempDir.resolve("missing-old-root").toString());
        RuntimeControlCommandExecutorImpl executor = executorWithProperties(properties);
        Path script = writePowerShellScript("Write-Output ((Get-Location).Path)");
        RuntimeControlCommand command = command(script);
        command.setWorkingDirectory(tempDir.toString());

        String output = executor.executeForOutput(command, Duration.ofSeconds(5)).trim();

        assertEquals(tempDir.toAbsolutePath().normalize().toString(), output);
    }

    @Test
    void queryStatusShouldUseConfiguredStatusCommandTimeout() throws Exception {
        RuntimeControlProperties properties = propertiesWithRepoRoot(tempDir.toString());
        properties.setStatusCommandTimeout(Duration.ofMillis(200));
        RuntimeControlCommandExecutorImpl executor = executorWithProperties(properties);
        Path script = writePowerShellScript("""
                Start-Sleep -Seconds 2
                Write-Output '{"status":"running","httpStatus":"HTTP 200","runtimeState":"running"}'
                """);

        RuntimeControlStatusResult status = executor.queryStatus(command(script));

        assertEquals("error", status.getStatus());
        assertEquals("ERROR", status.getHttpStatus());
        assertEquals("unknown", status.getRuntimeState());
        assertTrue(status.getBlockedReason().contains("Command timed out"));
    }

    @Test
    void queryStatusShouldTerminateTimedOutProcessTreeBeforeReturning() throws Exception {
        RuntimeControlProperties properties = propertiesWithRepoRoot(tempDir.toString());
        properties.setStatusCommandTimeout(Duration.ofSeconds(5));
        RuntimeControlCommandExecutorImpl executor = executorWithProperties(properties);
        Path parentPidFile = tempDir.resolve("timeout-parent.pid");
        Path childPidFile = tempDir.resolve("timeout-child.pid");
        Path script = writePowerShellScript("""
                [System.IO.File]::WriteAllText('%s', $PID.ToString())
                $startInfo = [System.Diagnostics.ProcessStartInfo]::new()
                $startInfo.FileName = 'powershell.exe'
                $startInfo.Arguments = '-NoProfile -Command "Start-Sleep -Seconds 30"'
                $startInfo.WorkingDirectory = (Get-Location).Path
                $startInfo.UseShellExecute = $false
                $startInfo.CreateNoWindow = $true
                $child = [System.Diagnostics.Process]::Start($startInfo)
                [System.IO.File]::WriteAllText('%s', $child.Id.ToString())
                Start-Sleep -Seconds 30
                """.formatted(toPowerShellLiteral(parentPidFile), toPowerShellLiteral(childPidFile)));

        RuntimeControlStatusResult status = executor.queryStatus(command(script));
        assertTrue(Files.isRegularFile(parentPidFile), "the timed-out command should publish its parent PID");
        assertTrue(Files.isRegularFile(childPidFile), "the timed-out command should publish its child PID");
        long parentPid = Long.parseLong(Files.readString(parentPidFile, StandardCharsets.UTF_8).trim());
        long childPid = Long.parseLong(Files.readString(childPidFile, StandardCharsets.UTF_8).trim());
        try {
            assertEquals("error", status.getStatus());
            assertTrue(status.getBlockedReason().contains("Command timed out"));
            assertFalse(ProcessHandle.of(parentPid).map(ProcessHandle::isAlive).orElse(false),
                    "the executor must await its timed-out parent process before returning");
            assertFalse(ProcessHandle.of(childPid).map(ProcessHandle::isAlive).orElse(false),
                    "the executor must terminate and await descendants before returning from a timeout");
        } finally {
            terminateTestProcess(childPid);
            terminateTestProcess(parentPid);
        }
        Files.delete(script);
        Files.delete(parentPidFile);
        Files.delete(childPidFile);
        deleteTreeWithRetry(tempDir);
    }

    @Test
    void executeForOutputShouldTerminateProcessWhenCallerIsInterrupted() throws Exception {
        RuntimeControlProperties properties = propertiesWithRepoRoot(tempDir.toString());
        RuntimeControlCommandExecutorImpl executor = executorWithProperties(properties);
        Path parentPidFile = tempDir.resolve("interrupted-parent.pid");
        Path script = writePowerShellScript("""
                [System.IO.File]::WriteAllText('%s', $PID.ToString())
                Start-Sleep -Seconds 30
                """.formatted(toPowerShellLiteral(parentPidFile)));
        AtomicReference<Throwable> thrown = new AtomicReference<>();
        Thread caller = new Thread(() -> {
            try {
                executor.executeForOutput(command(script), Duration.ofMinutes(1));
            } catch (Throwable ex) {
                thrown.set(ex);
            }
        }, "runtime-control-interrupted-caller");

        caller.start();
        awaitFile(parentPidFile);
        long parentPid = Long.parseLong(Files.readString(parentPidFile, StandardCharsets.UTF_8).trim());
        try {
            caller.interrupt();
            caller.join(TimeUnit.SECONDS.toMillis(15));

            assertFalse(caller.isAlive(), "the interrupted runtime-control call must complete its cleanup");
            assertTrue(caller.isInterrupted(), "the interrupted caller flag must be restored after cleanup");
            assertTrue(thrown.get() instanceof ServiceException);
            assertTrue(thrown.get().getMessage().contains("Command interrupted"));
            assertFalse(ProcessHandle.of(parentPid).map(ProcessHandle::isAlive).orElse(false),
                    "interrupting the caller must not leak the command process");
        } finally {
            caller.interrupt();
            terminateTestProcess(parentPid);
            caller.join(TimeUnit.SECONDS.toMillis(5));
        }
        Files.delete(script);
        Files.delete(parentPidFile);
        deleteTreeWithRetry(tempDir);
    }

    @Test
    void runCommandShouldTerminateTimedOutProcessTreeBeforeReturning() throws Exception {
        RuntimeControlCommandExecutorImpl executor = executorWithProperties(propertiesWithRepoRoot(tempDir.toString()));
        Path parentPidFile = tempDir.resolve("run-command-parent.pid");
        Path childPidFile = tempDir.resolve("run-command-child.pid");
        Path script = writePowerShellScript("""
                [System.IO.File]::WriteAllText('%s', $PID.ToString())
                $startInfo = [System.Diagnostics.ProcessStartInfo]::new()
                $startInfo.FileName = 'powershell.exe'
                $startInfo.Arguments = '-NoProfile -Command "Start-Sleep -Seconds 30"'
                $startInfo.WorkingDirectory = '%s'
                $startInfo.UseShellExecute = $false
                $startInfo.CreateNoWindow = $true
                $child = [System.Diagnostics.Process]::Start($startInfo)
                [System.IO.File]::WriteAllText('%s', $child.Id.ToString())
                Start-Sleep -Seconds 30
                """.formatted(toPowerShellLiteral(parentPidFile), toPowerShellLiteral(tempDir),
                toPowerShellLiteral(childPidFile)));
        Method runCommand = RuntimeControlCommandExecutorImpl.class
                .getDeclaredMethod("runCommand", List.class, Duration.class);
        runCommand.setAccessible(true);

        InvocationTargetException invocation = assertThrows(InvocationTargetException.class,
                () -> runCommand.invoke(executor, powerShellCommand(script), Duration.ofSeconds(5)));
        assertTrue(invocation.getCause() instanceof ServiceException);
        assertTrue(invocation.getCause().getMessage().contains("Command timed out"));
        assertTrue(Files.isRegularFile(parentPidFile));
        assertTrue(Files.isRegularFile(childPidFile));
        long parentPid = Long.parseLong(Files.readString(parentPidFile, StandardCharsets.UTF_8).trim());
        long childPid = Long.parseLong(Files.readString(childPidFile, StandardCharsets.UTF_8).trim());
        try {
            assertFalse(ProcessHandle.of(parentPid).map(ProcessHandle::isAlive).orElse(false));
            assertFalse(ProcessHandle.of(childPid).map(ProcessHandle::isAlive).orElse(false));
        } finally {
            terminateTestProcess(childPid);
            terminateTestProcess(parentPid);
        }
        Files.delete(script);
        Files.delete(parentPidFile);
        Files.delete(childPidFile);
        deleteTreeWithRetry(tempDir);
    }

    @Test
    void runCommandShouldTerminateProcessWhenCallerIsInterrupted() throws Exception {
        RuntimeControlCommandExecutorImpl executor = executorWithProperties(propertiesWithRepoRoot(tempDir.toString()));
        Path parentPidFile = tempDir.resolve("run-command-interrupted-parent.pid");
        Path script = writePowerShellScript("""
                [System.IO.File]::WriteAllText('%s', $PID.ToString())
                Start-Sleep -Seconds 30
                """.formatted(toPowerShellLiteral(parentPidFile)));
        Method runCommand = RuntimeControlCommandExecutorImpl.class
                .getDeclaredMethod("runCommand", List.class, Duration.class);
        runCommand.setAccessible(true);
        AtomicReference<Throwable> thrown = new AtomicReference<>();
        Thread caller = new Thread(() -> {
            try {
                runCommand.invoke(executor, powerShellCommand(script), Duration.ofMinutes(1));
            } catch (InvocationTargetException ex) {
                thrown.set(ex.getCause());
            } catch (Throwable ex) {
                thrown.set(ex);
            }
        }, "runtime-control-run-command-interrupted-caller");

        caller.start();
        awaitFile(parentPidFile);
        long parentPid = Long.parseLong(Files.readString(parentPidFile, StandardCharsets.UTF_8).trim());
        try {
            caller.interrupt();
            caller.join(TimeUnit.SECONDS.toMillis(15));

            assertFalse(caller.isAlive());
            assertTrue(caller.isInterrupted());
            assertTrue(thrown.get() instanceof ServiceException);
            assertTrue(thrown.get().getMessage().contains("Command interrupted"));
            assertFalse(ProcessHandle.of(parentPid).map(ProcessHandle::isAlive).orElse(false));
        } finally {
            caller.interrupt();
            terminateTestProcess(parentPid);
            caller.join(TimeUnit.SECONDS.toMillis(5));
        }
        Files.delete(script);
        Files.delete(parentPidFile);
        deleteTreeWithRetry(tempDir);
    }

    private RuntimeControlProperties propertiesWithRepoRoot(String repoRoot) {
        // These process tests do not exercise workflows or backup configuration.
        RuntimeControlProperties properties = new RuntimeControlProperties();
        properties.setStateDir(tempDir.resolve("state").toString());
        properties.setRepoRoot(repoRoot);
        return properties;
    }

    private RuntimeControlCommandExecutorImpl executorWithProperties(RuntimeControlProperties properties) throws Exception {
        RuntimeControlCommandExecutorImpl executor = new RuntimeControlCommandExecutorImpl();
        Field field = RuntimeControlCommandExecutorImpl.class.getDeclaredField("properties");
        field.setAccessible(true);
        field.set(executor, properties);
        return executor;
    }

    private RuntimeControlCommand command(Path script) {
        return new RuntimeControlCommand("local", "intruoyi-backend", script.toString(), List.of());
    }

    private Path writePowerShellScript(String scriptBody) throws Exception {
        Path script = tempDir.resolve("runtime-control-test.ps1");
        Files.writeString(script, scriptBody + System.lineSeparator(), StandardCharsets.UTF_8);
        return script;
    }

    private List<String> powerShellCommand(Path script) {
        return List.of("powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", script.toString());
    }

    private void awaitFile(Path path) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (!Files.isRegularFile(path)) {
            if (System.nanoTime() >= deadline) {
                throw new AssertionError("Timed out waiting for " + path);
            }
            Thread.sleep(20);
        }
    }

    private String toPowerShellLiteral(Path path) {
        return path.toAbsolutePath().normalize().toString().replace("'", "''");
    }

    private void terminateTestProcess(long pid) throws Exception {
        ProcessHandle process = ProcessHandle.of(pid).orElse(null);
        if (process == null || !process.isAlive()) {
            return;
        }
        process.destroyForcibly();
        process.onExit().get(5, TimeUnit.SECONDS);
    }

    private void deleteTreeWithRetry(Path root) throws Exception {
        Exception failure = null;
        for (int attempt = 0; attempt < 50; attempt++) {
            try {
                if (!Files.exists(root)) return;
                try (var paths = Files.walk(root)) {
                    paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ex) {
                            throw new java.io.UncheckedIOException(ex);
                        }
                    });
                }
                return;
            } catch (java.io.UncheckedIOException | IOException ex) {
                failure = ex;
                Thread.sleep(200);
            }
        }
        if (failure != null) throw failure;
    }
}
