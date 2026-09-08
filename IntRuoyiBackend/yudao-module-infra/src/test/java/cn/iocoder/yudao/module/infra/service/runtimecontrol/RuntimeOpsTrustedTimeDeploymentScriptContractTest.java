package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeOpsTrustedTimeDeploymentScriptContractTest {

    @Test
    void scriptShouldCollectRequiredReadonlyTimeEvidenceWithoutPrintingCredentials() throws Exception {
        String script = Files.readString(Path.of("..", "script", "deploy", "show-int-ruoyi-trusted-time.ps1"));
        String compose = Files.readString(Path.of("..", "script", "deploy", "int-ruoyi-test", "docker-compose.yml"));

        assertTrue(script.contains("chronyc tracking"));
        assertTrue(script.contains("chronyc sources -n"));
        assertTrue(script.contains("timedatectl status"));
        assertTrue(script.contains("date -u"));
        assertTrue(script.contains("UTC_TIMESTAMP(6)"));
        assertTrue(script.contains("docker exec intruoyi-mysql sh -c"));
        assertTrue(script.contains("MYSQL_PWD="));
        assertTrue(script.contains("BatchMode=yes"));
        assertTrue(script.contains("'-n'"));
        assertTrue(script.contains("System.Diagnostics.ProcessStartInfo"));
        assertTrue(script.contains("RedirectStandardOutput"));
        assertTrue(script.contains("RedirectStandardError"));
        assertTrue(script.contains("Resolve-TrustedTimeSshResult"));
        assertTrue(script.contains("ConvertTo-Json"));
        assertFalse(script.contains("2>&1"));
        assertFalse(script.contains(". ./.env"));
        assertFalse(script.contains("set -a"));
        assertFalse(script.contains(" -p"));
        assertFalse(script.contains("Write-Host $MYSQL_ROOT_PASSWORD"));
        assertFalse(script.contains("Write-Output $MYSQL_ROOT_PASSWORD"));
        assertTrue(compose.contains("INTRUOYI_TRUSTED_TIME_MAX_OFFSET_MILLIS: "
                + "${INTRUOYI_TRUSTED_TIME_MAX_OFFSET_MILLIS:?"));
        assertFalse(compose.contains("${INTRUOYI_TRUSTED_TIME_MAX_OFFSET_MILLIS:-"));
    }

    @Test
    void sshResultHelperShouldAllowOnlyKnownWindowsClosedSocketDiagnostic() throws Exception {
        ProcessResult result = invokeHelper("""
                Resolve-TrustedTimeSshResult -ExitCode 0 -StdOut '2026-09-07T10:00:00Z' -StdErr 'close - IO is still pending on closed socket. read:1, write:0, io:0000023A'
                """);

        assertEquals(0, result.exitCode());
        assertEquals("2026-09-07T10:00:00Z", result.stdout().trim());
        assertTrue(result.stderr().isBlank());
    }

    @Test
    void sshResultHelperShouldRejectUnknownStderrEvenWhenExitCodeIsZero() throws Exception {
        ProcessResult result = invokeHelper("""
                Resolve-TrustedTimeSshResult -ExitCode 0 -StdOut 'valid-output' -StdErr 'unexpected ssh warning'
                """);

        assertNotEquals(0, result.exitCode());
        assertTrue(result.stderr().contains("unexpected stderr"));
    }

    @Test
    void sshResultHelperShouldRejectEmptyStdout() throws Exception {
        ProcessResult result = invokeHelper("""
                Resolve-TrustedTimeSshResult -ExitCode 0 -StdOut '' -StdErr 'close - IO is still pending on closed socket. read:1, write:0, io:0000023A'
                """);

        assertNotEquals(0, result.exitCode());
        assertTrue(result.stderr().contains("empty stdout"));
    }

    @Test
    void sshResultHelperShouldRedactAllSupportedSecretAssignmentsCaseInsensitively() throws Exception {
        List<SecretCase> cases = List.of(
                new SecretCase("MYSQL_ROOT_PASSWORD=rootPlainSecret", List.of("rootPlainSecret")),
                new SecretCase("mysql_pwd='alphaBravo charlieDelta'", List.of("alphaBravo", "charlieDelta")),
                new SecretCase("PASSWORD=\"echoFoxtrot golfHotel\"", List.of("echoFoxtrot", "golfHotel")),
                new SecretCase("ToKeN=tokenPlainSecret", List.of("tokenPlainSecret")),
                new SecretCase("SECRET='indiaJuliet kiloLima'", List.of("indiaJuliet", "kiloLima")),
                new SecretCase("ACCESS_KEY=\"mikeNovember oscarPapa\"", List.of("mikeNovember", "oscarPapa")),
                new SecretCase("secret_key=quebecRomeoSecret", List.of("quebecRomeoSecret"))
        );

        for (SecretCase secretCase : cases) {
            ProcessResult result = invokeExitFailure(secretCase.diagnostic());
            assertNotEquals(0, result.exitCode(), secretCase.diagnostic());
            assertTrue(result.stderr().contains("exit code 255"), result.stderr());
            assertTrue(result.stderr().contains("<REDACTED>"), result.stderr());
            for (String fragment : secretCase.secretFragments()) {
                assertFalse(result.stderr().contains(fragment), result.stderr());
            }
        }
    }

    @Test
    void sshResultHelperShouldPreserveNonSecretDiagnostics() throws Exception {
        ProcessResult result = invokeExitFailure(
                "host=prod node=audit reason=connection_refused retry=disabled");

        assertNotEquals(0, result.exitCode());
        assertTrue(result.stderr().contains("host=prod"));
        assertTrue(result.stderr().contains("node=audit"));
        assertTrue(result.stderr().contains("reason=connection_refused"));
        assertTrue(result.stderr().contains("retry=disabled"));
    }

    private ProcessResult invokeExitFailure(String diagnostic) throws Exception {
        String encoded = Base64.getEncoder().encodeToString(diagnostic.getBytes(StandardCharsets.UTF_8));
        return invokeHelper("$stderr = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String('"
                + encoded + "')); Resolve-TrustedTimeSshResult -ExitCode 255 -StdOut '' -StdErr $stderr");
    }

    private ProcessResult invokeHelper(String statement) throws Exception {
        Path helper = Path.of("..", "script", "deploy", "resolve-trusted-time-ssh-result.ps1")
                .toAbsolutePath().normalize();
        String command = ". '" + helper.toString().replace("'", "''") + "'; " + statement;
        Process process = new ProcessBuilder("powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass",
                "-Command", command).start();
        String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();
        return new ProcessResult(exitCode, stdout, stderr);
    }

    private record ProcessResult(int exitCode, String stdout, String stderr) {
    }

    private record SecretCase(String diagnostic, List<String> secretFragments) {
    }
}
