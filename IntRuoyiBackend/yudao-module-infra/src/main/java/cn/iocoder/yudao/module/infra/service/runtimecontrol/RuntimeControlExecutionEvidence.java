package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Parses the approved executor's structured rejection protocol, never human error wording. */
final class RuntimeControlExecutionEvidence {
    private static final String RESOURCE_ACQUIRE_REJECTED =
            "RELEASE_WORKFLOW_ZERO_WRITE_REJECTED=RESOURCE_ACQUIRE";

    private RuntimeControlExecutionEvidence() { }

    static boolean confirmedZeroWriteRejection(Path executorOutput) throws IOException {
        int proofs = 0;
        boolean acquired = false;
        try (var reader = Files.newBufferedReader(executorOutput, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (RESOURCE_ACQUIRE_REJECTED.equals(line)) {
                    proofs++;
                }
                if (line.startsWith("RUNTIME_RESOURCE_LEASE_TOKEN=") || line.startsWith("RUNTIME_LEASE_ACQUIRED")) {
                    acquired = true;
                }
            }
        }
        return proofs == 1 && !acquired;
    }
}
