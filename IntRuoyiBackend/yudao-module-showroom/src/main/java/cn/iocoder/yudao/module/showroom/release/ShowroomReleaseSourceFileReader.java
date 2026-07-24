package cn.iocoder.yudao.module.showroom.release;

import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.framework.file.core.utils.FileTypeUtils;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.web.util.UriUtils;

@Component
public class ShowroomReleaseSourceFileReader {

    private static final Pattern ADMIN_FILE_URL = Pattern.compile("^/admin-api/infra/file/(\\d+)/get/(.+)$");
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private final FileMapper fileMapper;
    private final FileService fileService;

    public ShowroomReleaseSourceFileReader(FileMapper fileMapper, FileService fileService) {
        this.fileMapper = fileMapper;
        this.fileService = fileService;
    }

    public ResolvedBinarySource readFileById(String assetId, String assetType, Long fileId,
                                              Long previewAssetVersionId, Long narrationVersionId) {
        FileDO file = fileMapper.selectById(fileId);
        if (file == null) {
            throw new IllegalStateException("SHOWROOM_RELEASE_SOURCE_MISSING: file not found: " + fileId);
        }
        try {
            byte[] bytes = fileService.getFileContent(file.getConfigId(), file.getPath());
            if (bytes == null || bytes.length == 0) {
                throw new IllegalStateException("SHOWROOM_RELEASE_SOURCE_MISSING: source file is empty: " + fileId);
            }
            String mimeType = file.getType();
            if (mimeType == null || mimeType.isBlank()) {
                mimeType = FileTypeUtils.getMineType(bytes, file.getName());
            }
            return new ResolvedBinarySource(assetId, assetType, mimeType, bytes,
                    file.getConfigId() + ":" + file.getPath(), previewAssetVersionId, narrationVersionId);
        } catch (Exception exception) {
            throw new IllegalStateException("SHOWROOM_RELEASE_SOURCE_MISSING: failed to read file " + fileId,
                    exception);
        }
    }

    public String requireAdminFileUrlById(Long fileId) {
        FileDO file = fileMapper.selectById(fileId);
        if (file == null) {
            throw new IllegalStateException("SHOWROOM_RELEASE_SOURCE_MISSING: file not found: " + fileId);
        }
        if (file.getConfigId() == null || file.getPath() == null || file.getPath().isBlank()) {
            throw new IllegalStateException("SHOWROOM_RELEASE_SOURCE_MISSING: file metadata incomplete: " + fileId);
        }
        return "/admin-api/infra/file/" + file.getConfigId() + "/get/"
                + UriUtils.encodePath(file.getPath(), StandardCharsets.UTF_8);
    }

    public ResolvedBinarySource readByAdminUrl(String assetId, String assetType, String adminUrl) {
        Matcher matcher = ADMIN_FILE_URL.matcher(adminUrl);
        if (matcher.matches()) {
            Long configId = Long.valueOf(matcher.group(1));
            String path = URLDecoder.decode(matcher.group(2), StandardCharsets.UTF_8);
            try {
                byte[] bytes = fileService.getFileContent(configId, path);
                if (bytes == null || bytes.length == 0) {
                    throw new IllegalStateException("SHOWROOM_RELEASE_SOURCE_MISSING: source file is empty: " + adminUrl);
                }
                String name = path.substring(path.lastIndexOf('/') + 1);
                String mimeType = FileTypeUtils.getMineType(bytes, name);
                return new ResolvedBinarySource(assetId, assetType, mimeType, bytes, configId + ":" + path, null, null);
            } catch (Exception exception) {
                throw new IllegalStateException("SHOWROOM_RELEASE_SOURCE_MISSING: failed to read file " + adminUrl,
                        exception);
            }
        }
        if (adminUrl != null && (adminUrl.startsWith("http://") || adminUrl.startsWith("https://"))) {
            return readByAbsoluteUrl(assetId, assetType, adminUrl);
        }
        throw new IllegalStateException("SHOWROOM_RELEASE_SOURCE_MISSING: unsupported admin file url " + adminUrl);
    }

    private ResolvedBinarySource readByAbsoluteUrl(String assetId, String assetType, String sourceUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(sourceUrl)).GET().build();
            HttpResponse<byte[]> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "SHOWROOM_RELEASE_SOURCE_MISSING: source file request failed: " + response.statusCode());
            }
            byte[] bytes = response.body();
            if (bytes == null || bytes.length == 0) {
                throw new IllegalStateException("SHOWROOM_RELEASE_SOURCE_MISSING: source file is empty: " + sourceUrl);
            }
            String mimeType = response.headers().firstValue("Content-Type")
                    .map(value -> {
                        int separator = value.indexOf(';');
                        return separator >= 0 ? value.substring(0, separator).trim() : value.trim();
                    })
                    .filter(value -> !value.isBlank())
                    .orElseGet(() -> FileTypeUtils.getMineType(bytes, fileNameFromUrl(sourceUrl)));
            return new ResolvedBinarySource(assetId, assetType, mimeType, bytes, sourceUrl, null, null);
        } catch (Exception exception) {
            throw new IllegalStateException("SHOWROOM_RELEASE_SOURCE_MISSING: failed to read file " + sourceUrl,
                    exception);
        }
    }

    private String fileNameFromUrl(String sourceUrl) {
        String path = URI.create(sourceUrl).getPath();
        if (path == null || path.isBlank()) {
            return sourceUrl;
        }
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }
}
