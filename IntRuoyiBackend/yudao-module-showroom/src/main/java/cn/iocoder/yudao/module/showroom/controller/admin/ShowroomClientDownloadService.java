package cn.iocoder.yudao.module.showroom.controller.admin;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class ShowroomClientDownloadService {

    private static final String EXPOSED_DOWNLOAD_HEADERS = "Accept-Ranges,Content-Disposition";

    public ResponseEntity<Resource> download(ShowroomClientDownloadFile file) {
        ClassPathResource resource = new ClassPathResource(file.resourcePath());
        if (!resource.exists() || !resource.isReadable()) {
            throw new IllegalStateException("SHOWROOM_CLIENT_PACKAGE_MISSING: " + file.resourcePath());
        }
        return ResponseEntity.ok()
                .contentType(file.contentType())
                .contentLength(contentLength(resource, file))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.fileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, EXPOSED_DOWNLOAD_HEADERS)
                .body(resource);
    }

    private static long contentLength(Resource resource, ShowroomClientDownloadFile file) {
        try {
            return resource.contentLength();
        } catch (IOException e) {
            throw new IllegalStateException("SHOWROOM_CLIENT_PACKAGE_UNREADABLE: " + file.resourcePath(), e);
        }
    }
}
