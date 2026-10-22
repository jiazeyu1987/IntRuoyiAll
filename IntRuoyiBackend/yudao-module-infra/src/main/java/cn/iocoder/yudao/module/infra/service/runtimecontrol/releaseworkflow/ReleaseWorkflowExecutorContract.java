package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class ReleaseWorkflowExecutorContract {

    private ReleaseWorkflowExecutorContract() {
    }

    public static VerifiedExecutor verify(RuntimeControlProperties properties) {
        RuntimeControlProperties.ReleaseWorkflow config = properties.getReleaseWorkflow();
        config.validate();
        Path root = Path.of(config.getMaintenanceRepoRoot()).toAbsolutePath().normalize();
        Path script = root.resolve(config.getPublishScriptPath()).normalize();
        if (!script.startsWith(root) || Files.isSymbolicLink(root) || Files.isSymbolicLink(script)
                || !Files.isDirectory(root) || !Files.isRegularFile(script)) {
            throw new IllegalArgumentException("releaseWorkflow.publishScriptPath");
        }
        try {
            String digest = HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(script)));
            if (!digest.equalsIgnoreCase(config.getExpectedPublishScriptSha256())) {
                throw new IllegalArgumentException(
                        "RELEASE_EXECUTOR_DIGEST_MISMATCH: expected published maintenance executor");
            }
            return new VerifiedExecutor(root, script, digest);
        } catch (IOException | NoSuchAlgorithmException ex) {
            throw new IllegalStateException("releaseWorkflow.publishScriptSha256", ex);
        }
    }

    public record VerifiedExecutor(Path maintenanceRoot, Path script, String sha256) {
    }
}
