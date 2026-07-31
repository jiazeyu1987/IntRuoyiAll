package cn.iocoder.yudao.server;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalRuntimeLoggingConfigTest {

    private static final String RUNTIME_LOG_DIR = "../output/runtime/${INTRUOYI_RUNTIME_PROFILE:int_main}/logs";
    private static final String RUNTIME_LOG_FILE = "${INTRUOYI_BACKEND_LOG_FILE:${INTRUOYI_RUNTIME_LOG_DIR:"
            + RUNTIME_LOG_DIR + "}/${spring.application.name}.log}";
    private static final Pattern MAPPER_DEBUG_PATTERN = Pattern.compile(
            "(?m)^\\s+cn\\.iocoder\\.yudao\\.module\\.[\\w-]+\\.dal\\.mysql:\\s*debug\\s*$");

    private final Path projectDir = findProjectDir();

    @Test
    void localProfileShouldWriteApplicationLogToRuntimeSpecificDirectoryByDefault() throws IOException {
        String content = readLocalProfile();

        assertTrue(content.contains("name: " + RUNTIME_LOG_FILE),
                "local profile must default backend application logs to the runtime-specific output directory");
        assertTrue(content.contains("log-dir: ${INTRUOYI_RUNTIME_CONTROL_LOG_DIR:${INTRUOYI_RUNTIME_LOG_DIR:"
                        + RUNTIME_LOG_DIR + "}}"),
                "runtime-control storage guard log-dir must follow the same runtime log directory by default");
        assertFalse(content.contains("name: ${user.home}/logs/${spring.application.name}.log"),
                "local profile must not default backend application logs to the shared user home log file");
        assertFalse(content.contains("log-dir: ${INTRUOYI_RUNTIME_CONTROL_LOG_DIR:${user.home}/logs}"),
                "runtime-control storage guard must not default to the shared user home log directory");
    }

    @Test
    void localProfileShouldNotEnableMapperDebugLoggingByDefault() throws IOException {
        String content = readLocalProfile();

        assertFalse(MAPPER_DEBUG_PATTERN.matcher(content).find(),
                "local profile must not enable self-owned MyBatis mapper DEBUG logging by default");
    }

    private String readLocalProfile() throws IOException {
        return Files.readString(projectDir.resolve(
                "yudao-server/src/main/resources/application-local.yaml"), StandardCharsets.UTF_8);
    }

    private static Path findProjectDir() {
        Path currentDir = Path.of(System.getProperty("user.dir"));
        return "yudao-server".equals(currentDir.getFileName().toString()) ? currentDir.getParent() : currentDir;
    }

}
