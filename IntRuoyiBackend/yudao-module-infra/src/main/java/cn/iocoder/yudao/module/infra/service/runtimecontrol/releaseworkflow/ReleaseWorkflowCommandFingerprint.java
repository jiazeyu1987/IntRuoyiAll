package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import java.util.List;

/** Fingerprints the complete local execution context and exact command line for recovery verification. */
public final class ReleaseWorkflowCommandFingerprint {
    private ReleaseWorkflowCommandFingerprint() { }

    public static String calculate(String environment, String component, String workingDirectory, String commandLine) {
        List<String> fields = List.of(environment, component, workingDirectory, commandLine);
        if (fields.stream().anyMatch(value -> value.isBlank() || value.contains("\n") || value.contains("\r"))) {
            throw new IllegalArgumentException("BUILD_RECOVERY_COMMAND_FINGERPRINT_INPUT_INVALID");
        }
        String canonical = fields.stream().map(value -> value.length() + ":" + value)
                .collect(java.util.stream.Collectors.joining("\n"));
        return ReleaseWorkflowBackupAuthorizationService.digest(canonical);
    }
}
