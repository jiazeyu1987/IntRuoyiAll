package cn.iocoder.yudao.server;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalBackendPortConfigTest {

    private static final String FIXED_LOCAL_BACKEND_PORT = "48081";
    private static final Pattern SERVER_PORT_PATTERN = Pattern.compile("(?m)^server:\\R\\s{2}port:\\s*(\\d+)\\s*$");

    private final Path projectDir = findProjectDir();

    @Test
    void localAndDevProfilesShouldUseFixedBackendPort() throws IOException {
        assertProfileUsesFixedBackendPort("yudao-server/src/main/resources/application-local.yaml");
        assertProfileUsesFixedBackendPort("yudao-server/src/main/resources/application-dev.yaml");
    }

    @Test
    void localFrameAncestorsShouldOnlyAllowFixedBackendPort() throws IOException {
        String content = readProfile("yudao-server/src/main/resources/application-local.yaml");

        assertTrue(content.contains("localhost:" + FIXED_LOCAL_BACKEND_PORT),
                "local frame-ancestors must allow localhost fixed backend port");
        assertTrue(content.contains("127.0.0.1:" + FIXED_LOCAL_BACKEND_PORT),
                "local frame-ancestors must allow 127.0.0.1 fixed backend port");
        assertFalse(content.contains("localhost:48080"),
                "local frame-ancestors must not keep stale localhost port 48080");
        assertFalse(content.contains("127.0.0.1:48080"),
                "local frame-ancestors must not keep stale 127.0.0.1 port 48080");
    }

    private void assertProfileUsesFixedBackendPort(String relativePath) throws IOException {
        String content = readProfile(relativePath);

        assertEquals(FIXED_LOCAL_BACKEND_PORT, extractServerPort(content),
                relativePath + " must use the fixed local backend port");
        assertFalse(content.contains("port: 48080"),
                relativePath + " must not keep stale server.port 48080");
    }

    private String readProfile(String relativePath) throws IOException {
        return Files.readString(projectDir.resolve(relativePath), StandardCharsets.UTF_8);
    }

    private static String extractServerPort(String content) {
        Matcher matcher = SERVER_PORT_PATTERN.matcher(content);
        if (!matcher.find()) {
            throw new AssertionError("server.port must be declared at the top of the profile");
        }
        return matcher.group(1);
    }

    private static Path findProjectDir() {
        Path currentDir = Path.of(System.getProperty("user.dir"));
        return "yudao-server".equals(currentDir.getFileName().toString()) ? currentDir.getParent() : currentDir;
    }

}
