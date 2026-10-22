package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReleaseWorkflowGateOrderTest {

    @Test
    void requiredTestsPrecedeEveryExpensiveBuild() throws Exception {
        Class<?> contract = requireContract();
        @SuppressWarnings("unchecked")
        List<String> order = (List<String>) contract.getMethod("standardCommandOrder").invoke(null);

        assertEquals(List.of("backend-tests", "frontend-tests", "script-tests", "maven-build",
                "frontend-build", "docker-build"), order);
        int firstBuild = order.indexOf("maven-build");
        assertTrue(order.indexOf("backend-tests") < firstBuild);
        assertTrue(order.indexOf("frontend-tests") < firstBuild);
        assertTrue(order.indexOf("script-tests") < firstBuild);
    }

    @Test
    void anyFailedOrMissingTestBlocksBuildSteps() throws Exception {
        Method assertReady = requireContract().getMethod("assertReadyForBuild", Map.class);
        assertReady.invoke(null, Map.of("backend-tests", true, "frontend-tests", true, "script-tests", true));

        InvocationTargetException failed = assertThrows(InvocationTargetException.class,
                () -> assertReady.invoke(null,
                        Map.of("backend-tests", true, "frontend-tests", false, "script-tests", true)));
        assertTrue(failed.getCause().getMessage().contains("RELEASE_TEST_GATE_FAILED"));

        InvocationTargetException missing = assertThrows(InvocationTargetException.class,
                () -> assertReady.invoke(null, Map.of("backend-tests", true, "frontend-tests", true)));
        assertTrue(missing.getCause().getMessage().contains("RELEASE_TEST_GATE_MISSING"));
    }

    private static Class<?> requireContract() {
        try {
            return Class.forName(
                    "cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow.ReleaseWorkflowGateOrder");
        } catch (ClassNotFoundException exception) {
            throw new AssertionError("required P1 gate-order contract is missing", exception);
        }
    }
}
