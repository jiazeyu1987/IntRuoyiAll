package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class RuntimeControlLocalConfigContractTest {

    @Test
    void localStorageGuardLogDirShouldFollowSpringUserHomeLogRoot() throws IOException {
        String localConfig = Files.readString(resolveRepoFile(Path.of(
                "yudao-server", "src", "main", "resources", "application-local.yaml"
        )), StandardCharsets.UTF_8);

        assertTrue(localConfig.contains("name: ${INTRUOYI_BACKEND_LOG_FILE:${INTRUOYI_RUNTIME_LOG_DIR:../output/runtime/${INTRUOYI_RUNTIME_PROFILE:int_main}/logs}/${spring.application.name}.log"));
        assertTrue(localConfig.contains("storage-guard:"));
        assertTrue(localConfig.contains("log-dir: ${INTRUOYI_RUNTIME_CONTROL_LOG_DIR:${INTRUOYI_RUNTIME_LOG_DIR:../output/runtime/${INTRUOYI_RUNTIME_PROFILE:int_main}/logs}}"));
        assertFalse(localConfig.contains("log-dir: ${INTRUOYI_RUNTIME_CONTROL_LOG_DIR:E:/Int/CacheData/IntRuoyi/runtime}"));
    }

    private static Path resolveRepoFile(Path relativePath) {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(relativePath);
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        fail("Missing repository file: " + relativePath);
        return relativePath;
    }

}
