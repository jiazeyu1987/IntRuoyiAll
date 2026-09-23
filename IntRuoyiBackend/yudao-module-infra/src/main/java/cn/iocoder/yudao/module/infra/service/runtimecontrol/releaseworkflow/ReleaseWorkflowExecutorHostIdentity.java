package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/** Provides a stable, non-secret Windows host identity digest for build recovery binding. */
public final class ReleaseWorkflowExecutorHostIdentity {
    private static final Pattern MACHINE_GUID = Pattern.compile(
            "(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    private ReleaseWorkflowExecutorHostIdentity() { }

    public static String currentDigest() {
        String systemRoot = System.getenv("SystemRoot");
        if (systemRoot == null || systemRoot.isBlank()) {
            throw new IllegalStateException("BUILD_EXECUTOR_HOST_IDENTITY_UNAVAILABLE");
        }
        String executable = Path.of(systemRoot, "System32", "WindowsPowerShell", "v1.0", "powershell.exe").toString();
        var result = ReleaseWorkflowLocalProcess.run(List.of(executable, "-NoProfile", "-NonInteractive", "-Command",
                        "$ErrorActionPreference='Stop'; $value=(Get-ItemProperty -LiteralPath 'HKLM:\\SOFTWARE\\Microsoft\\Cryptography' -Name MachineGuid).MachineGuid; [Console]::Write($value)"),
                null, Map.of(), Duration.ofSeconds(15), "BUILD_EXECUTOR_HOST_IDENTITY_QUERY");
        String machineGuid = result.output().trim();
        if (result.exitCode() != 0 || !MACHINE_GUID.matcher(machineGuid).matches()) {
            throw new IllegalStateException("BUILD_EXECUTOR_HOST_IDENTITY_UNAVAILABLE");
        }
        return ReleaseWorkflowBackupAuthorizationService.digest("windows-machine-guid:" + machineGuid.toLowerCase(Locale.ROOT));
    }
}
