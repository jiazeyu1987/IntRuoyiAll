package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class RuntimeControlRestoreIsolationConfigTest {
    @TempDir Path tempDir;

    @Test
    void usesTheActualRestoreWritersTestRootFromControlledRelativeConfig() throws Exception {
        var properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        properties.setRepoRoot(tempDir.toString());
        properties.getBackupOps().setConfigPath("backup.json");
        Files.writeString(tempDir.resolve("backup.json"),
                "{\"servers\":{\"test\":{\"backupPointsRoot\":\"/controlled/points/\"},"
                        + "\"backup\":{\"backupPointsRoot\":\"/not-the-restore-writer\"}}}", StandardCharsets.UTF_8);
        assertEquals("/controlled/points/.restore-stage/restore-isolation.json",
                RuntimeControlRestoreIsolationConfig.resolve(properties));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/", "relative/path", "/valid/../elsewhere", "/valid/./points", "/valid//points"})
    void refusesMissingBroadOrTraversingRemoteRoots(String root) throws Exception {
        var properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        Path config = tempDir.resolve("invalid-backup.json");
        properties.getBackupOps().setConfigPath(config.toString());
        Files.writeString(config, "{\"servers\":{\"test\":{\"backupPointsRoot\":\"" + root + "\"}}}",
                StandardCharsets.UTF_8);
        assertThrows(IllegalStateException.class, () -> RuntimeControlRestoreIsolationConfig.resolve(properties));
    }

    @Test
    void missingOrMalformedConfigurationNeverUsesDefaultNasRoot() throws Exception {
        var properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        Path config = tempDir.resolve("missing.json");
        properties.getBackupOps().setConfigPath(config.toString());
        assertThrows(IllegalStateException.class, () -> RuntimeControlRestoreIsolationConfig.resolve(properties));
        Files.writeString(config, "{}", StandardCharsets.UTF_8);
        assertThrows(IllegalStateException.class, () -> RuntimeControlRestoreIsolationConfig.resolve(properties));
        Files.writeString(config, "", StandardCharsets.UTF_8);
        assertThrows(IllegalStateException.class, () -> RuntimeControlRestoreIsolationConfig.resolve(properties));
    }
}
