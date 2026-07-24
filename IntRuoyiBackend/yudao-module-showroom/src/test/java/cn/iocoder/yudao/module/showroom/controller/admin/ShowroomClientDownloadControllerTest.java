package cn.iocoder.yudao.module.showroom.controller.admin;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShowroomClientDownloadControllerTest {

    @Test
    void downloadAndroidClientShouldReturnPackagedApk() throws Exception {
        ShowroomClientDownloadService service = new ShowroomClientDownloadService();
        ShowroomClientDownloadController controller = new ShowroomClientDownloadController(service);

        ResponseEntity<Resource> response = controller.downloadAndroidClient();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.parseMediaType("application/vnd.android.package-archive"),
                response.getHeaders().getContentType());
        assertEquals("YingtaiShowroomClient-Android-v1.0.apk",
                response.getHeaders().getContentDisposition().getFilename());
        assertEquals("Accept-Ranges,Content-Disposition", response.getHeaders()
                .getFirst(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS));
        assertPackagedZipLikeResource(response.getBody());
    }

    @Test
    void downloadDesktopClientShouldReturnPackagedWin7Zip() throws Exception {
        ShowroomClientDownloadService service = new ShowroomClientDownloadService();
        ShowroomClientDownloadController controller = new ShowroomClientDownloadController(service);

        ResponseEntity<Resource> response = controller.downloadDesktopClient();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.parseMediaType("application/zip"), response.getHeaders().getContentType());
        assertEquals("YingtaiShowroomClient-Win7-v1.0.zip",
                response.getHeaders().getContentDisposition().getFilename());
        assertEquals("Accept-Ranges,Content-Disposition", response.getHeaders()
                .getFirst(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS));
        assertPackagedZipLikeResource(response.getBody());
    }

    @Test
    void missingClientPackageShouldFailFast() {
        ShowroomClientDownloadService service = new ShowroomClientDownloadService();
        ShowroomClientDownloadFile missingFile = new ShowroomClientDownloadFile(
                "missing",
                "showroom/client-downloads/v1.0/missing-client-package.zip",
                "missing-client-package.zip",
                MediaType.APPLICATION_OCTET_STREAM);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.download(missingFile));

        assertTrue(exception.getMessage().contains("SHOWROOM_CLIENT_PACKAGE_MISSING"));
        assertTrue(exception.getMessage().contains("showroom/client-downloads/v1.0/missing-client-package.zip"));
    }

    private static void assertPackagedZipLikeResource(Resource resource) throws Exception {
        assertNotNull(resource);
        assertTrue(resource.exists(), "download resource must exist on the classpath");
        assertTrue(resource.contentLength() > 0, "download resource must not be empty");
        try (InputStream inputStream = resource.getInputStream()) {
            assertArrayEquals(new byte[] {'P', 'K'}, inputStream.readNBytes(2));
        }
    }
}
