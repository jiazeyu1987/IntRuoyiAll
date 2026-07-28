package cn.iocoder.yudao.server;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeControlLocalConfigTest {

    private static final String RUNTIME_LOG_DIR = "../output/runtime/${INTRUOYI_RUNTIME_PROFILE:int_main}/logs";
    private static final String RUNTIME_LOG_FILE = "${INTRUOYI_BACKEND_LOG_FILE:${INTRUOYI_RUNTIME_LOG_DIR:"
            + RUNTIME_LOG_DIR + "}/${spring.application.name}.log}";

    private final Path projectDir = findProjectDir();

    @Test
    void localRuntimeControlLogDirShouldMatchSpringLogRoot() throws IOException {
        String content = Files.readString(projectDir.resolve(
                "yudao-server/src/main/resources/application-local.yaml"), StandardCharsets.UTF_8);

        assertTrue(content.contains("name: " + RUNTIME_LOG_FILE),
                "application-local.yaml must write Spring logs to the runtime-specific output directory");
        assertTrue(content.contains("storage-guard:"),
                "application-local.yaml must configure runtime-control.storage-guard");
        assertTrue(content.contains("log-dir: ${INTRUOYI_RUNTIME_CONTROL_LOG_DIR:${INTRUOYI_RUNTIME_LOG_DIR:"
                        + RUNTIME_LOG_DIR + "}}"),
                "runtime-control.storage-guard.log-dir must follow the local runtime Spring log root");
    }

    @Test
    void localShowroomReleaseReadbackOriginShouldFollowLocalServerPort() throws IOException {
        String content = Files.readString(projectDir.resolve(
                "yudao-server/src/main/resources/application-local.yaml"), StandardCharsets.UTF_8);

        assertTrue(content.contains("showroom:"),
                "application-local.yaml must configure showroom release settings");
        assertTrue(content.contains("public-website-origin: \"${SHOWROOM_RELEASE_PUBLIC_WEBSITE_ORIGIN:http://127.0.0.1:${server.port}}\""),
                "local showroom release readback origin must default to the local backend server port");
    }

    private static Path findProjectDir() {
        Path currentDir = Path.of(System.getProperty("user.dir"));
        return "yudao-server".equals(currentDir.getFileName().toString()) ? currentDir.getParent() : currentDir;
    }

}
