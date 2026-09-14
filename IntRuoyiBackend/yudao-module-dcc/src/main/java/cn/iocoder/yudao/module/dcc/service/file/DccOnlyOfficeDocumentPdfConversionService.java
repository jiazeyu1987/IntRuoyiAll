package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessDeniedException;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessOperation;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessReference;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessRequest;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ACCESS_DENIED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PDF_CONVERSION_CONFIG_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PDF_CONVERSION_FAILED;

@Service
public class DccOnlyOfficeDocumentPdfConversionService implements DccDocumentPdfConversionService {

    @Resource
    private DccOnlyOfficePreviewProperties onlyOfficePreviewProperties;
    @Resource
    private DccOnlyOfficePreviewTokenService onlyOfficePreviewTokenService;
    @Resource
    private DccOnlyOfficeConversionClient onlyOfficeConversionClient;
    @Resource
    private BusinessFileAccessService businessFileAccessService;

    @Override
    public DccConvertedPdf convertToPdf(FileDO sourceFile) {
        requireSourceFile(sourceFile);
        requireConfigured();
        String fileType = resolveFileType(sourceFile.getName());
        BusinessFileAccessReference reference = requireConversionAccess(sourceFile.getId());
        String documentUrl = buildDocumentUrl(sourceFile.getId(), reference);
        DccOnlyOfficeConversionCommand command = new DccOnlyOfficeConversionCommand(
                buildConverterUrl(sourceFile),
                onlyOfficePreviewProperties.getJwtSecret(),
                fileType,
                buildConversionKey(sourceFile),
                sourceFile.getName(),
                documentUrl);
        byte[] convertedBytes = onlyOfficeConversionClient.convertToPdf(command);
        requirePdfContent(convertedBytes);
        return new DccConvertedPdf(toPdfFileName(sourceFile.getName()), convertedBytes);
    }

    private void requireSourceFile(FileDO sourceFile) {
        if (sourceFile == null || sourceFile.getId() == null || StrUtil.isBlank(sourceFile.getName())) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
    }

    private void requireConfigured() {
        if (!onlyOfficePreviewProperties.isConfigured()) {
            throw exception(CONTROLLED_FILE_PDF_CONVERSION_CONFIG_MISSING,
                    onlyOfficePreviewProperties.missingReason());
        }
    }

    private String resolveFileType(String fileName) {
        String extension = StrUtil.subAfter(StrUtil.trimToEmpty(fileName), '.', true).toLowerCase(Locale.ROOT);
        if (StrUtil.isBlank(extension) || "pdf".equals(extension)) {
            throw conversionFailed("Unsupported source file type for PDF conversion: " + fileName);
        }
        return extension;
    }

    private String buildDocumentUrl(Long sourceFileId, BusinessFileAccessReference reference) {
        DccOnlyOfficePreviewTokenService.IssuedPreviewToken issuedToken =
                onlyOfficePreviewTokenService.issueBusinessFile(
                        DccOnlyOfficePreviewTokenService.AUDIENCE_UPLOAD_PREVIEW,
                        BusinessFileAccessOperation.CONVERT, sourceFileId,
                        TenantContextHolder.getRequiredTenantId(), null,
                        DccOnlyOfficePreviewTokenService.SERVICE_DCC_PDF_CONVERSION,
                        reference, onlyOfficePreviewProperties.getTokenExpireSeconds().longValue());
        return trimTrailingSlash(onlyOfficePreviewProperties.getPublicFileBaseUrl())
                + "/admin-api/dcc/controlled-files/upload-preview/" + sourceFileId
                + "/onlyoffice-file?token=" + issuedToken.token();
    }

    private BusinessFileAccessReference requireConversionAccess(Long sourceFileId) {
        try {
            return businessFileAccessService.assertAllowed(new BusinessFileAccessRequest(
                            BusinessFileAccessOperation.CONVERT, sourceFileId,
                            TenantContextHolder.getRequiredTenantId(), null,
                            DccOnlyOfficePreviewTokenService.SERVICE_DCC_PDF_CONVERSION,
                            "DCC-CONVERT-" + UUID.randomUUID(), null, null, null))
                    .orElseThrow(() -> exception(CONTROLLED_FILE_ACCESS_DENIED));
        } catch (BusinessFileAccessDeniedException ex) {
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        }
    }

    private String buildConverterUrl(FileDO sourceFile) {
        String key = buildConversionKey(sourceFile);
        return trimTrailingSlash(onlyOfficePreviewProperties.getBaseUrl())
                + "/converter?shardkey=" + key;
    }

    private String buildConversionKey(FileDO sourceFile) {
        String rawKey = sourceFile.getId() + ":" + StrUtil.blankToDefault(sourceFile.getPath(), "")
                + ":" + StrUtil.blankToDefault(sourceFile.getName(), "");
        byte[] digest;
        try {
            digest = MessageDigest.getInstance("SHA-256").digest(rawKey.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw conversionFailed("PDF conversion key generation failed");
        }
        return "DCC-" + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(digest)
                .substring(0, 32);
    }

    private void requirePdfContent(byte[] content) {
        if (content == null || content.length < 5
                || content[0] != '%'
                || content[1] != 'P'
                || content[2] != 'D'
                || content[3] != 'F'
                || content[4] != '-') {
            throw conversionFailed("OnlyOffice conversion result is not a real PDF");
        }
    }

    private String toPdfFileName(String sourceName) {
        String name = StrUtil.trimToEmpty(sourceName);
        int dotIndex = name.lastIndexOf('.');
        String baseName = dotIndex > 0 ? name.substring(0, dotIndex) : name;
        return StrUtil.blankToDefault(baseName, "converted") + ".pdf";
    }

    private String trimTrailingSlash(String value) {
        String normalized = StrUtil.trimToEmpty(value);
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private ServiceException conversionFailed(String reason) {
        return new ServiceException(CONTROLLED_FILE_PDF_CONVERSION_FAILED.getCode(),
                StrUtil.blankToDefault(reason, "OnlyOffice conversion failed"));
    }
}
