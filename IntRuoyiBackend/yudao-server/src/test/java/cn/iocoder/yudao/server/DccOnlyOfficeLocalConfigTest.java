package cn.iocoder.yudao.server;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DccOnlyOfficeLocalConfigTest {

    private static final String EXPECTED_LOCAL_ONLYOFFICE_URL = "http://127.0.0.1:8080";
    private static final String STALE_ONLYOFFICE_URL = "http://127.0.0.1:8082";

    private final Path projectDir = findProjectDir();

    @Test
    void localAndDevOnlyOfficeDefaultsShouldMatchLocalDocumentServerPort() throws IOException {
        assertOnlyOfficeDefaultMatchesLocalPort("yudao-server/src/main/resources/application-local.yaml");
        assertOnlyOfficeDefaultMatchesLocalPort("yudao-server/src/main/resources/application-dev.yaml");
    }

    private void assertOnlyOfficeDefaultMatchesLocalPort(String relativePath) throws IOException {
        String content = Files.readString(projectDir.resolve(relativePath), StandardCharsets.UTF_8);

        assertEquals(EXPECTED_LOCAL_ONLYOFFICE_URL, extractOnlyOfficeBaseUrlDefault(content),
                relativePath + " must default DCC OnlyOffice to the local document server port");
        assertFalse(content.contains(STALE_ONLYOFFICE_URL),
                relativePath + " must not point DCC OnlyOffice preview to stale port 8082");
    }

    private static String extractOnlyOfficeBaseUrlDefault(String content) {
        String marker = "base-url: ${DCC_ONLYOFFICE_BASE_URL:";
        int startIndex = content.indexOf(marker);
        if (startIndex < 0) {
            throw new AssertionError("DCC_ONLYOFFICE_BASE_URL placeholder must be present");
        }
        int valueStartIndex = startIndex + marker.length();
        int endIndex = content.indexOf('}', valueStartIndex);
        if (endIndex < 0) {
            throw new AssertionError("DCC_ONLYOFFICE_BASE_URL placeholder must be closed");
        }
        return content.substring(valueStartIndex, endIndex);
    }

    private static Path findProjectDir() {
        Path currentDir = Path.of(System.getProperty("user.dir"));
        return "yudao-server".equals(currentDir.getFileName().toString()) ? currentDir.getParent() : currentDir;
    }

}
