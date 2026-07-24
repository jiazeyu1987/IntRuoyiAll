package cn.iocoder.yudao.server;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeControlLocalConfigTest {

    private final Path projectDir = findProjectDir();

    @Test
    void localRuntimeControlLogDirShouldMatchSpringLogRoot() throws IOException {
        String content = Files.readString(projectDir.resolve(
                "yudao-server/src/main/resources/application-local.yaml"), StandardCharsets.UTF_8);

        assertTrue(content.contains("name: ${user.home}/logs/${spring.application.name}.log"),
                "application-local.yaml must write Spring logs to ${user.home}/logs");
        assertTrue(content.contains("storage-guard:"),
                "application-local.yaml must configure runtime-control.storage-guard");
        assertTrue(content.contains("log-dir: ${INTRUOYI_RUNTIME_CONTROL_LOG_DIR:${user.home}/logs}"),
                "runtime-control.storage-guard.log-dir must follow the local Spring log root");
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
