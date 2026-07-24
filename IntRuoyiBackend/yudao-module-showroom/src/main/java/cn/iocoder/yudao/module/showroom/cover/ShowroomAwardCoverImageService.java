package cn.iocoder.yudao.module.showroom.cover;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ShowroomAwardCoverImageService {

    private static final String ERROR_CODE = "SHOWROOM_AWARD_COVER_GENERATION_FAILED";
    private static final String IMAGE_DIRECTORY = "showroom/award";
    private static final String IMAGE_TYPE = "image/png";
    private static final String ADMIN_FILE_URL_PREFIX = "/admin-api/infra/file/";
    private static final Pattern ADMIN_FILE_URL_PATTERN = Pattern.compile("^/admin-api/infra/file/(\\d+)/get/(.+)$");

    private final FileService fileService;
    private final ShowroomNativeImageGenerationService nativeImageGenerationService;

    public ShowroomAwardCoverImageService(FileService fileService,
                                          ShowroomNativeImageGenerationService nativeImageGenerationService) {
        this.fileService = fileService;
        this.nativeImageGenerationService = nativeImageGenerationService;
    }

    public String generateCoverImage(String awardCode, String promptText, String sourceCoverImageUrl) {
        if (StrUtil.isBlank(awardCode)) {
            throw new IllegalStateException(ERROR_CODE + ": award code is required");
        }
        if (StrUtil.isBlank(sourceCoverImageUrl)) {
            throw new IllegalStateException(ERROR_CODE + ": source cover image url is required");
        }
        requireReadableSourceCoverImage(sourceCoverImageUrl);
        Path generatedFile = nativeImageGenerationService.generatePng(promptText, ERROR_CODE, "award cover");
        byte[] content = readGeneratedImageBytes(generatedFile);
        Long fileId = fileService.createFileAndReturnId(content, buildFileName(awardCode), IMAGE_DIRECTORY, IMAGE_TYPE);
        if (fileId == null) {
            throw new IllegalStateException(ERROR_CODE + ": uploaded award cover file id is empty");
        }
        FileDO file = fileService.getFile(fileId);
        return buildProxyFileUrl(file);
    }

    private static byte[] readGeneratedImageBytes(Path generatedFile) {
        try {
            byte[] content = Files.readAllBytes(generatedFile);
            if (content.length == 0) {
                throw new IllegalStateException(ERROR_CODE + ": generated award cover file is empty: " + generatedFile);
            }
            return content;
        } catch (IOException e) {
            throw new IllegalStateException(ERROR_CODE + ": failed to read generated award cover file: "
                    + generatedFile, e);
        }
    }

    private static String buildFileName(String awardCode) {
        return "award-" + awardCode.trim() + "-cover.png";
    }

    private void requireReadableSourceCoverImage(String sourceCoverImageUrl) {
        AdminFileUrl adminFileUrl = parseAdminFileUrl(sourceCoverImageUrl);
        byte[] sourceContent;
        try {
            sourceContent = fileService.getFileContent(adminFileUrl.configId(), adminFileUrl.path());
        } catch (Exception exception) {
            throw new IllegalStateException(ERROR_CODE + ": failed to read current award cover image: "
                    + sourceCoverImageUrl, exception);
        }
        if (sourceContent == null || sourceContent.length == 0) {
            throw new IllegalStateException(ERROR_CODE + ": current award cover image is empty: " + sourceCoverImageUrl);
        }
    }

    private static AdminFileUrl parseAdminFileUrl(String sourceCoverImageUrl) {
        String normalizedUrl = sourceCoverImageUrl.trim();
        int adminUrlStart = normalizedUrl.indexOf(ADMIN_FILE_URL_PREFIX);
        if (adminUrlStart < 0) {
            throw new IllegalStateException(ERROR_CODE + ": current award cover image url is invalid: "
                    + sourceCoverImageUrl);
        }
        String adminUrl = normalizedUrl.substring(adminUrlStart);
        Matcher matcher = ADMIN_FILE_URL_PATTERN.matcher(adminUrl);
        if (!matcher.matches()) {
            throw new IllegalStateException(ERROR_CODE + ": current award cover image url is invalid: "
                    + sourceCoverImageUrl);
        }
        Long configId = Long.valueOf(matcher.group(1));
        String path = URLDecoder.decode(matcher.group(2), StandardCharsets.UTF_8);
        if (StrUtil.isBlank(path)) {
            throw new IllegalStateException(ERROR_CODE + ": current award cover image path is empty: "
                    + sourceCoverImageUrl);
        }
        return new AdminFileUrl(configId, path);
    }

    private static String buildProxyFileUrl(FileDO file) {
        if (file == null) {
            throw new IllegalStateException(ERROR_CODE + ": uploaded award cover file record not found");
        }
        if (file.getConfigId() == null) {
            throw new IllegalStateException(ERROR_CODE + ": uploaded award cover file config id is empty");
        }
        if (StrUtil.isBlank(file.getPath())) {
            throw new IllegalStateException(ERROR_CODE + ": uploaded award cover file path is empty");
        }
        return "/admin-api/infra/file/" + file.getConfigId() + "/get/" + file.getPath();
    }

    private record AdminFileUrl(Long configId, String path) {
    }
}
