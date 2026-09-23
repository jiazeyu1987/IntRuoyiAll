package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import org.junit.jupiter.api.Test;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import static org.junit.jupiter.api.Assertions.*;

class ReleaseWorkflowSourcePreflightTest {
    @Test void verifiesActualLocalLinuxReceiptToolchain() throws Exception {
        invoke(subject(null));
    }

    @Test void preservesUtf8DiagnosticsSeparatelyFromCapabilityOutput() {
        var result = ReleaseWorkflowLocalProcess.run(List.of("wsl.exe", "-d", "Ubuntu", "--exec", "bash", "-lc",
                        "printf RELEASE_RECEIPT_TOOLCHAIN_READY; printf diagnostic-marker >&2"),
                null, Map.of("WSL_UTF8", "1"), Duration.ofSeconds(45), "SOURCE_RECEIPT_TOOLCHAIN", false);
        assertEquals(0, result.exitCode());
        assertEquals("RELEASE_RECEIPT_TOOLCHAIN_READY", result.output());
        assertTrue(result.diagnostics().contains("diagnostic-marker"));
    }

    @Test void failedToolchainNeverSelectsAnotherDistribution() throws Exception {
        AtomicReference<List<String>> command = new AtomicReference<>();
        Object subject = subject(args -> {
            command.set(List.copyOf(args));
            return new ReleaseWorkflowLocalProcess.Result(1, "distribution unavailable");
        });
        Exception error = assertThrows(Exception.class, () -> invoke(subject));
        assertTrue(error.getMessage().contains("SOURCE_RECEIPT_TOOLCHAIN_UNAVAILABLE"));
        assertEquals(List.of("wsl.exe", "-d", "Ubuntu", "--exec", "python3", "-c"), command.get().subList(0, 6));
    }

    @Test void exitZeroWithoutVerifiedCapabilitiesDoesNotPass() throws Exception {
        Object subject = subject(args -> new ReleaseWorkflowLocalProcess.Result(0, "tool started"));
        assertThrows(Exception.class, () -> invoke(subject));
    }

    private static Object subject(Function<List<String>, ReleaseWorkflowLocalProcess.Result> runner) throws Exception {
        Class<?> type;
        try { type = Class.forName(ReleaseWorkflowSourcePreflightTest.class.getPackageName() + ".ReleaseWorkflowSourcePreflight"); }
        catch (ClassNotFoundException error) { return fail("Early receipt toolchain preflight is missing", error); }
        return runner == null ? type.getConstructor().newInstance() : type.getConstructor(Function.class).newInstance(runner);
    }

    private static void invoke(Object subject) throws Exception {
        try { subject.getClass().getMethod("verify").invoke(subject); }
        catch (InvocationTargetException error) {
            if (error.getCause() instanceof Exception cause) throw cause;
            throw error;
        }
    }
}
