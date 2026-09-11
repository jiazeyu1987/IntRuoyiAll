package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import java.util.List;
import java.util.Map;

public final class ReleaseWorkflowGateOrder {
    private static final List<String> REQUIRED_TESTS = List.of(
            "backend-tests", "frontend-tests", "script-tests");
    private static final List<String> STANDARD_ORDER = List.of(
            "backend-tests", "frontend-tests", "script-tests",
            "maven-build", "frontend-build", "docker-build");

    private ReleaseWorkflowGateOrder() {
    }

    public static List<String> standardCommandOrder() {
        return STANDARD_ORDER;
    }

    public static void assertReadyForBuild(Map<String, Boolean> testResults) {
        if (testResults == null) {
            throw new IllegalStateException("RELEASE_TEST_GATE_MISSING: all");
        }
        for (String requiredTest : REQUIRED_TESTS) {
            if (!testResults.containsKey(requiredTest)) {
                throw new IllegalStateException("RELEASE_TEST_GATE_MISSING: " + requiredTest);
            }
            if (!Boolean.TRUE.equals(testResults.get(requiredTest))) {
                throw new IllegalStateException("RELEASE_TEST_GATE_FAILED: " + requiredTest);
            }
        }
    }
}
