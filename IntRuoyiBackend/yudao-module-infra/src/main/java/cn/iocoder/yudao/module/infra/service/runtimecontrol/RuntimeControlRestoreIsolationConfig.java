package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/** Shares the existing restore writer's marker location, without guessing a NAS root. */
public final class RuntimeControlRestoreIsolationConfig {
    private RuntimeControlRestoreIsolationConfig() { }

    public static String resolve(RuntimeControlProperties properties) {
        String configured = properties.getBackupOps().getConfigPath();
        if (configured == null || configured.isBlank()) {
            throw new IllegalStateException("RESTORE_ISOLATION_CONFIG_REQUIRED: backupOps.configPath");
        }
        Path config = Path.of(configured);
        if (!config.isAbsolute()) {
            if (properties.getRepoRoot() == null || properties.getRepoRoot().isBlank()) {
                throw new IllegalStateException("RESTORE_ISOLATION_CONFIG_REQUIRED: repoRoot");
            }
            config = Path.of(properties.getRepoRoot()).resolve(config);
        }
        config = config.toAbsolutePath().normalize();
        if (!Files.isRegularFile(config)) {
            throw new IllegalStateException("RESTORE_ISOLATION_CONFIG_REQUIRED: " + config);
        }
        try (var reader = Files.newBufferedReader(config, StandardCharsets.UTF_8)) {
            var document = new ObjectMapper().readTree(reader);
            if (document == null || !document.isObject()) {
                throw new IllegalStateException("RESTORE_ISOLATION_CONFIG_INVALID");
            }
            var node = document.path("servers").path("test").path("backupPointsRoot");
            if (!node.isTextual()) {
                throw new IllegalStateException("RESTORE_ISOLATION_ROOT_REQUIRED: servers.test.backupPointsRoot");
            }
            String root = node.textValue();
            if (!root.matches("/[A-Za-z0-9_.-]+(?:/[A-Za-z0-9_.-]+)*/?")
                    || Arrays.stream(root.split("/")).anyMatch(part -> part.equals(".") || part.equals(".."))) {
                throw new IllegalStateException("RESTORE_ISOLATION_ROOT_INVALID");
            }
            if (root.endsWith("/")) {
                root = root.substring(0, root.length() - 1);
            }
            return root + "/.restore-stage/restore-isolation.json";
        } catch (IOException ex) {
            // Do not echo malformed JSON: the controlled configuration can contain credentials.
            throw new IllegalStateException("RESTORE_ISOLATION_CONFIG_UNREADABLE: " + ex.getClass().getSimpleName());
        }
    }
}
