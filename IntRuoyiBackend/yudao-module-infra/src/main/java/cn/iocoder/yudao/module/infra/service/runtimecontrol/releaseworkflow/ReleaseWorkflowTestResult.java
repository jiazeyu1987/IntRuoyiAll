package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import java.util.Locale;

/** Explicit manual test acceptance result for a release workflow. */
public enum ReleaseWorkflowTestResult {
    PASS,
    FAIL;

    public static ReleaseWorkflowTestResult fromApi(String value) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException("RELEASE_WORKFLOW_TEST_RESULT_INVALID");
        }
        try {
            return ReleaseWorkflowTestResult.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("RELEASE_WORKFLOW_TEST_RESULT_INVALID", ex);
        }
    }
}
