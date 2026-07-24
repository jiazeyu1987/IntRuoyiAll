package cn.iocoder.yudao.module.showroom.cover;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.ai.framework.ai.config.YudaoAiProperties;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 展厅产品 AI 封面生成服务。
 */
@Service
@Slf4j
public class ShowroomProductCoverImageService {

    private static final String ERROR_CODE = "SHOWROOM_COVER_GENERATION_FAILED";
    private static final String IMAGE_DIRECTORY = "showroom/product/cover";
    private static final String IMAGE_TYPE = "image/png";
    private static final String ADMIN_FILE_URL_PREFIX = "/admin-api/infra/file/";
    private static final Pattern ADMIN_FILE_URL_PATTERN = Pattern.compile(
            "^/admin-api/infra/file/(\\d+)/get/(.+)$");
    private static final Pattern IMPORTED_COVER_HASHED_FILE_NAME_PATTERN = Pattern.compile(
            "^product-.+-imported-cover-([0-9a-f]{16})\\.[A-Za-z0-9]+$");

    private final FileService fileService;
    private final ShowroomNativeImageGenerationService nativeImageGenerationService;

    @Autowired
    public ShowroomProductCoverImageService(FileService fileService,
                                            ShowroomNativeImageGenerationService nativeImageGenerationService) {
        this.fileService = fileService;
        this.nativeImageGenerationService = nativeImageGenerationService;
    }

    public ShowroomProductCoverImageService(FileService fileService,
                                            YudaoAiProperties yudaoAiProperties) {
        this(fileService, new ShowroomNativeImageGenerationService(yudaoAiProperties));
    }

    public String generateCoverImage(String productCode, String promptText) {
        if (StrUtil.isBlank(productCode)) {
            throw new IllegalStateException(ERROR_CODE + ": product code is required");
        }
        if (StrUtil.isBlank(promptText)) {
            throw new IllegalStateException(ERROR_CODE + ": rendered prompt text is required");
        }

        Path generatedFile = nativeImageGenerationService.generatePng(promptText.trim(), ERROR_CODE, "product cover");
        byte[] content = readGeneratedImageBytes(generatedFile);
        Long fileId = fileService.createFileAndReturnId(content, buildFileName(productCode), IMAGE_DIRECTORY, IMAGE_TYPE);
        if (fileId == null) {
            throw new IllegalStateException(ERROR_CODE + ": uploaded product cover file id is empty");
        }
        FileDO file = fileService.getFile(fileId);
        return buildProxyFileUrl(file);
    }

    public String uploadImportedCoverImage(String productCode, byte[] content, String fileExtension, String mimeType) {
        if (StrUtil.isBlank(productCode)) {
            throw new IllegalStateException(ERROR_CODE + ": product code is required");
        }
        if (content == null || content.length == 0) {
            throw new IllegalStateException(ERROR_CODE + ": imported product cover image is empty");
        }
        if (StrUtil.isBlank(fileExtension)) {
            throw new IllegalStateException(ERROR_CODE + ": imported product cover image extension is required");
        }
        if (StrUtil.isBlank(mimeType)) {
            throw new IllegalStateException(ERROR_CODE + ": imported product cover image mime type is required");
        }
        String normalizedExtension = fileExtension.trim().toLowerCase();
        if (normalizedExtension.startsWith(".")) {
            normalizedExtension = normalizedExtension.substring(1);
        }
        Long fileId = fileService.createFileAndReturnId(content,
                buildImportedCoverFileName(productCode, content, normalizedExtension),
                IMAGE_DIRECTORY, mimeType.trim());
        if (fileId == null) {
            throw new IllegalStateException(ERROR_CODE + ": uploaded imported product cover file id is empty");
        }
        FileDO file = fileService.getFile(fileId);
        return buildProxyFileUrl(file);
    }

    public boolean importedCoverImageMatchesCurrentCover(String currentCoverImageUrl, byte[] importedContent) {
        if (StrUtil.isBlank(currentCoverImageUrl) || importedContent == null || importedContent.length == 0) {
            return false;
        }
        AdminFileUrl adminFileUrl = parseAdminFileUrl(currentCoverImageUrl);
        if (adminFileUrl == null) {
            return false;
        }
        try {
            byte[] currentContent = fileService.getFileContent(adminFileUrl.configId(), adminFileUrl.path());
            if (currentContent == null || currentContent.length == 0) {
                throw new IllegalStateException(ERROR_CODE + ": current product cover image is empty: "
                        + currentCoverImageUrl);
            }
            return MessageDigest.isEqual(currentContent, importedContent);
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException(ERROR_CODE + ": failed to read current product cover image: "
                    + currentCoverImageUrl, exception);
        }
    }

    public boolean importedCoverImageUrlMatchesContentHash(String currentCoverImageUrl, byte[] importedContent) {
        if (StrUtil.isBlank(currentCoverImageUrl) || importedContent == null || importedContent.length == 0) {
            return false;
        }
        AdminFileUrl adminFileUrl = parseAdminFileUrl(currentCoverImageUrl);
        if (adminFileUrl == null) {
            return false;
        }
        String fileName = fileNameOf(adminFileUrl.path());
        Matcher matcher = IMPORTED_COVER_HASHED_FILE_NAME_PATTERN.matcher(fileName);
        return matcher.matches() && matcher.group(1).equals(contentHashPrefix(importedContent));
    }

    public int resolveBatchParallelism() {
        return nativeImageGenerationService.resolveBatchParallelism();
    }

    static String resolveCodexCommand(YudaoAiProperties.CodexCli codexCli) {
        return ShowroomNativeImageGenerationService.resolveCodexCommand(codexCli);
    }

    private static byte[] readGeneratedImageBytes(Path generatedFile) {
        try {
            byte[] content = Files.readAllBytes(generatedFile);
            if (content.length == 0) {
                throw new IllegalStateException(ERROR_CODE + ": generated product cover file is empty: " + generatedFile);
            }
            return content;
        } catch (IOException e) {
            throw new IllegalStateException(ERROR_CODE + ": failed to read generated product cover file: " + generatedFile, e);
        }
    }

    private static String buildFileName(String productCode) {
        return "product-" + productCode + "-cover.png";
    }

    private static String buildImportedCoverFileName(String productCode, byte[] content, String extension) {
        return "product-" + productCode.trim() + "-imported-cover-" + contentHashPrefix(content) + "." + extension;
    }

    private static AdminFileUrl parseAdminFileUrl(String currentCoverImageUrl) {
        String normalizedUrl = currentCoverImageUrl.trim();
        int adminUrlStart = normalizedUrl.indexOf(ADMIN_FILE_URL_PREFIX);
        if (adminUrlStart < 0) {
            return null;
        }
        String adminUrl = normalizedUrl.substring(adminUrlStart);
        Matcher matcher = ADMIN_FILE_URL_PATTERN.matcher(adminUrl);
        if (!matcher.matches()) {
            throw new IllegalStateException(ERROR_CODE + ": current product cover image url is invalid: "
                    + currentCoverImageUrl);
        }
        Long configId = Long.valueOf(matcher.group(1));
        String path = URLDecoder.decode(matcher.group(2), StandardCharsets.UTF_8);
        if (StrUtil.isBlank(path)) {
            throw new IllegalStateException(ERROR_CODE + ": current product cover image path is empty: "
                    + currentCoverImageUrl);
        }
        return new AdminFileUrl(configId, path);
    }

    private static String fileNameOf(String path) {
        int separatorIndex = path.lastIndexOf('/');
        return separatorIndex >= 0 ? path.substring(separatorIndex + 1) : path;
    }

    private static String contentHashPrefix(byte[] content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(content);
            StringBuilder builder = new StringBuilder(16);
            for (int index = 0; index < 8; index++) {
                builder.append(String.format("%02x", digest[index]));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(ERROR_CODE + ": SHA-256 digest algorithm is unavailable", exception);
        }
    }

    private static String buildProxyFileUrl(FileDO file) {
        if (file == null) {
            throw new IllegalStateException(ERROR_CODE + ": uploaded product cover file record not found");
        }
        if (file.getConfigId() == null) {
            throw new IllegalStateException(ERROR_CODE + ": uploaded product cover file config id is empty");
        }
        if (StrUtil.isBlank(file.getPath())) {
            throw new IllegalStateException(ERROR_CODE + ": uploaded product cover file path is empty");
        }
        return "/admin-api/infra/file/" + file.getConfigId() + "/get/" + file.getPath();
    }

    private record AdminFileUrl(Long configId, String path) {
    }
}
