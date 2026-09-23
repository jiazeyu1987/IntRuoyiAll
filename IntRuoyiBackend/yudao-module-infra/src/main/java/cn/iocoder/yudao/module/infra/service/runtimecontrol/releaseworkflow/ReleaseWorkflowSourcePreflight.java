package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/** The standard release suite requires this declared local Linux toolchain; it is never silently skipped. */
public final class ReleaseWorkflowSourcePreflight {
    private static final String READY = "RELEASE_RECEIPT_TOOLCHAIN_READY";
    private static final String CHECK = "import fcntl,json,os,shutil; assert hasattr(os,'O_DIRECTORY'); "
            + "assert all(shutil.which(tool) for tool in ('bash','flock','sha256sum')); print('" + READY + "')";
    private static final List<String> COMMAND = List.of("wsl.exe", "-d", "Ubuntu", "--exec", "python3", "-c", CHECK);
    private final Function<List<String>, ReleaseWorkflowLocalProcess.Result> runner;

    public ReleaseWorkflowSourcePreflight() {
        this(command -> ReleaseWorkflowLocalProcess.run(command, null, Map.of("WSL_UTF8", "1"), Duration.ofSeconds(45),
                "SOURCE_RECEIPT_TOOLCHAIN", false));
    }

    public ReleaseWorkflowSourcePreflight(Function<List<String>, ReleaseWorkflowLocalProcess.Result> runner) {
        this.runner = Objects.requireNonNull(runner, "runner");
    }

    public void verify() {
        try {
            var result = runner.apply(COMMAND);
            if (result == null || result.exitCode() != 0 || !READY.equals(result.output().trim())) {
                throw new IllegalStateException("SOURCE_RECEIPT_TOOLCHAIN_UNAVAILABLE");
            }
        } catch (RuntimeException error) {
            if ("SOURCE_RECEIPT_TOOLCHAIN_UNAVAILABLE".equals(error.getMessage())) throw error;
            throw new IllegalStateException("SOURCE_RECEIPT_TOOLCHAIN_UNAVAILABLE", error);
        }
    }
}
