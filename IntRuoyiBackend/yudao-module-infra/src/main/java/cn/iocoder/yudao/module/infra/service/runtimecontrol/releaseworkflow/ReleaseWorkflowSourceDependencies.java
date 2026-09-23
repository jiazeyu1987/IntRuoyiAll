package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.Duration;
import java.util.regex.Pattern;

/** Restores the approved dependency graph in a fresh source worktree before build preflight. */
@Component
public final class ReleaseWorkflowSourceDependencies {
    private final RuntimeControlProperties properties;

    public ReleaseWorkflowSourceDependencies(RuntimeControlProperties properties) {
        this.properties = properties;
    }

    public void prepare(ReleaseWorkflowWorktreeFactory.FrozenWorktrees frozen) {
        String version = properties.getReleaseWorkflow().getPnpmVersion();
        if (version == null || !version.matches("[0-9]+\\.[0-9]+\\.[0-9]+")) {
            throw new IllegalStateException("DEPENDENCY_TOOLCHAIN_VERSION_REQUIRED");
        }
        Path frontend = frozen.frontendRoot();
        Map<String, String> inputs = inputDigests(frontend);
        assertClean(frozen.applicationRoot(), "SOURCE_DIRTY");
        Result actual = corepack(frontend, "--version");
        if (actual.exitCode != 0 || !version.equals(actual.output.trim())) {
            throw new IllegalStateException("DEPENDENCY_TOOLCHAIN_MISMATCH: approved pnpm=" + version);
        }
        Result install = corepack(frontend, "install", "--frozen-lockfile");
        if (install.exitCode != 0) {
            var code = Pattern.compile("\\b(?:ERR_PNPM_[A-Z0-9_]+|ELIFECYCLE)\\b").matcher(install.output);
            throw new IllegalStateException("DEPENDENCY_INSTALL_FAILED: exit=" + install.exitCode
                    + (code.find() ? ", " + code.group() : ""));
        }
        Map<String, String> after = inputDigests(frontend);
        if (!inputs.equals(after)) {
            var changed = new java.util.TreeSet<>(inputs.keySet());
            changed.addAll(after.keySet());
            changed.removeIf(file -> java.util.Objects.equals(inputs.get(file), after.get(file)));
            throw new IllegalStateException("DEPENDENCY_INPUT_DRIFT: " + String.join(",", changed));
        }
        assertClean(frozen.applicationRoot(), "DEPENDENCY_SOURCE_DRIFT");
        // The managed build preflight separately verifies required binaries and ignored-builds.
    }

    private static Map<String, String> inputDigests(Path frontend) {
        Map<String, String> result = new LinkedHashMap<>();
        try {
            for (String file : List.of("package.json", "pnpm-lock.yaml", "pnpm-workspace.yaml")) {
                Path input = frontend.resolve(file);
                if (!Files.isRegularFile(input)) {
                    if (file.equals("pnpm-workspace.yaml") && !Files.exists(input)) continue;
                    throw new IllegalStateException("DEPENDENCY_INPUT_MISSING: " + file);
                }
                if (Files.isSymbolicLink(input)) throw new IllegalStateException("DEPENDENCY_INPUT_SYMLINK");
                result.put(file, HexFormat.of().formatHex(
                        MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(input))));
            }
        } catch (IOException | java.security.NoSuchAlgorithmException error) {
            throw new IllegalStateException("DEPENDENCY_INPUT_INSPECTION_FAILED", error);
        }
        return result;
    }

    private static void assertClean(Path root, String code) {
        Result result = run(root, List.of("git", "status", "--porcelain", "--untracked-files=all"));
        if (result.exitCode != 0 || !result.output.isBlank()) throw new IllegalStateException(code);
    }

    private static Result corepack(Path directory, String... arguments) {
        // The release executor runs on Windows. All shell arguments are fixed tokens, never user input.
        var command = new java.util.ArrayList<>(List.of("cmd.exe", "/d", "/c", "corepack.cmd", "pnpm"));
        command.addAll(List.of(arguments));
        return run(directory, command);
    }

    private static Result run(Path directory, List<String> command) {
        var result = ReleaseWorkflowLocalProcess.run(command, directory,
                Map.of("COREPACK_ENABLE_NETWORK", "0", "COREPACK_ENABLE_AUTO_PIN", "0", "CI", "true"),
                Duration.ofMinutes(30), "DEPENDENCY_PREPARATION");
        return new Result(result.exitCode(), result.output());
    }

    private record Result(int exitCode, String output) {}
}
