package cn.iocoder.yudao.server;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UploadMultipartLimitConfigTest {

    private static final Pattern MAX_FILE_SIZE_UNLIMITED =
            Pattern.compile("(?m)^\\s*max-file-size:\\s*-1\\s*(#.*)?$");
    private static final Pattern MAX_REQUEST_SIZE_UNLIMITED =
            Pattern.compile("(?m)^\\s*max-request-size:\\s*-1\\s*(#.*)?$");
    private static final Pattern TOMCAT_MAX_PART_COUNT_UNLIMITED =
            Pattern.compile("(?m)^\\s*max-part-count:\\s*-1\\s*(#.*)?$");
    private static final Pattern FRONTEND_NGINX_CLIENT_MAX_BODY_SIZE =
            Pattern.compile("(?m)^\\s*client_max_body_size\\s+0;\\s*(#.*)?$");
    private static final Pattern FORCE_RESPONSE_DISABLED =
            Pattern.compile("(?m)^\\s*force-response:\\s*false\\s*(#.*)?$");
    private static final Pattern FORCE_ENCODING_ENABLED =
            Pattern.compile("(?m)^\\s*force:\\s*true\\s*(#.*)?$");

    private final Path projectDir = findProjectDir();

    @Test
    void applicationYamlShouldDisableApplicationMultipartSizeLimits() throws IOException {
        String content = Files.readString(projectDir.resolve("yudao-server/src/main/resources/application.yaml"));

        assertTrue(MAX_FILE_SIZE_UNLIMITED.matcher(content).find(),
                "application.yaml must set spring.servlet.multipart.max-file-size=-1 for 200GB local folder imports");
        assertTrue(MAX_REQUEST_SIZE_UNLIMITED.matcher(content).find(),
                "application.yaml must set spring.servlet.multipart.max-request-size=-1 for 200GB local folder imports");
    }

    @Test
    void applicationYamlShouldDisableBulkLocalFolderMultipartPartLimit() throws IOException {
        String content = Files.readString(projectDir.resolve("yudao-server/src/main/resources/application.yaml"));

        assertTrue(TOMCAT_MAX_PART_COUNT_UNLIMITED.matcher(content).find(),
                "application.yaml must set server.tomcat.max-part-count=-1 for 200GB local folder imports");
    }

    @Test
    void frontendNginxShouldDisableClientBodySizeCheck() throws IOException {
        String content = Files.readString(projectDir.resolve("script/deploy/int-ruoyi-test/nginx.conf"));

        assertTrue(FRONTEND_NGINX_CLIENT_MAX_BODY_SIZE.matcher(content).find(),
                "frontend Nginx must set client_max_body_size 0 to disable request body size checks");
    }

    @Test
    void applicationYamlShouldNotForceUtf8CharsetOntoBinaryResponses() throws IOException {
        String content = Files.readString(projectDir.resolve("yudao-server/src/main/resources/application.yaml"));

        assertFalse(FORCE_ENCODING_ENABLED.matcher(content).find(),
                "application.yaml must not force UTF-8 onto every response because DCC encrypted downloads are binary");
        assertTrue(FORCE_RESPONSE_DISABLED.matcher(content).find(),
                "application.yaml must set server.servlet.encoding.force-response=false for binary download integrity");
    }

    private static Path findProjectDir() {
        Path currentDir = Path.of(System.getProperty("user.dir"));
        return "yudao-server".equals(currentDir.getFileName().toString()) ? currentDir.getParent() : currentDir;
    }

}
