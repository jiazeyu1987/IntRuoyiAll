package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeControlCommandExecutorImplTest {

    @TempDir
    private Path tempDir;

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

    private RuntimeControlProperties propertiesWithRepoRoot(String repoRoot) {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(tempDir.resolve("state"));
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
}
