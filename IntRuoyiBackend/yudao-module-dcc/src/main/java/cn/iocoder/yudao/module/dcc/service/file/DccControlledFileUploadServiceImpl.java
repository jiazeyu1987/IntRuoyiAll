package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileUploadPreviewReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileUploadRespVO;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFilePreviewKindEnum;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import cn.iocoder.yudao.module.dcc.service.audit.DccAccessBoundaryLogCreateCommand;
import cn.iocoder.yudao.module.dcc.service.audit.DccControlledFileAccessAuditService;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadSizePolicyService;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketCreateCommand;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketCreated;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketService;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ACCESS_DENIED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ONLYOFFICE_PREVIEW_CONFIG_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_DRAWING_PDF_FILE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SOURCE_FILE_TYPE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_UPLOAD_PURPOSE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_UPLOAD_PREVIEW_SINGLE_FILE_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_UPLOAD_SESSION_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_UPLOAD_SIZE_EXCEEDED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_UPLOAD_SIZE_POLICY_MISSING;

@Service
public class DccControlledFileUploadServiceImpl implements DccControlledFileUploadService {

    private static final String ORIGINAL_DIRECTORY = "dcc/original";

    @Resource
    private FileService fileService;
    @Resource
    private FileMapper fileMapper;
    @Resource
    private DccControlledPreviewWatermarkService watermarkService;
    @Resource
    private DccOnlyOfficePreviewProperties onlyOfficePreviewProperties;
    @Resource
    private DccOnlyOfficePreviewTokenService onlyOfficePreviewTokenService;
    @Resource
    private DccUploadSizePolicyService uploadSizePolicyService;
    @Resource
    private DccUploadTicketService uploadTicketService;
    @Resource
    private DccControlledFileAccessAuditService accessAuditService;
    @Resource
    private DccControlledFileCategoryPermissionSupport permissionSupport;

    @Override
    public DccControlledFileUploadRespVO uploadPreviewFile(Long userId, DccControlledFileUploadPreviewReqVO reqVO,
                                                           DccRequestAuditContext auditContext) throws Exception {
        MultipartFile file = null;
        String purpose = null;
        boolean uploadCompleted = false;
        try {
            file = validatePreviewFile(reqVO);
            validatePreviewSession(reqVO.getSessionId());
            purpose = validatePreviewPurposeName(reqVO.getPurpose(), file.getOriginalFilename());
            validateCategoryUploadPermission(reqVO.getCategoryId(), userId);
            uploadSizePolicyService.validateUploadSize(reqVO.getCategoryId(),
                    purpose, file.getSize(), null);
            byte[] content = IoUtil.readBytes(file.getInputStream());
            validatePreviewPurposeContent(purpose, file.getOriginalFilename(), content);
            String url = fileService.createFile(content, file.getOriginalFilename(), ORIGINAL_DIRECTORY, file.getContentType());
            FileDO storedFile = fileMapper.selectFirstOne(FileDO::getUrl, url);
            if (storedFile == null) {
                throw exception(CONTROLLED_FILE_NOT_EXISTS);
            }
            String requestId = auditContext.requireRequestId("upload preview");
            DccUploadTicketCreated uploadTicket = uploadTicketService.createTicket(new DccUploadTicketCreateCommand(
                    userId, reqVO.getCategoryId(), reqVO.getSessionId(),
                    purpose, storedFile.getId(),
                    storedFile.getName(), file.getContentType(), file.getSize(), content, requestId));
            DccControlledFileUploadRespVO respVO = new DccControlledFileUploadRespVO();
            respVO.setUploadTicket(uploadTicket.uploadTicket());
            respVO.setSessionId(uploadTicket.sessionId());
            respVO.setPurpose(uploadTicket.purpose());
            respVO.setStatus(uploadTicket.status());
            respVO.setExpireTime(uploadTicket.expireTime());
            respVO.setRequestId(requestId);
            respVO.setFileName(storedFile.getName());
            respVO.setContentType(file.getContentType());
            DccControlledFilePreviewKindEnum previewKind =
                    DccControlledFilePreviewKindEnum.resolve(storedFile.getName(), file.getContentType());
            respVO.setPreviewKind(previewKind.getCode());
            applyOfficePreview(storedFile, previewKind, respVO);
            respVO.setFileSize(file.getSize());
            respVO.setWatermark(watermarkService.build(userId, "preview", storedFile.getName()));
            uploadCompleted = true;
            recordUploadBoundary(userId, purpose, "SUCCESS", null, null, auditContext);
            return respVO;
        } catch (Exception ex) {
            if (!uploadCompleted) {
                recordUploadBoundary(userId, purpose, "DENIED", failureCode(ex), ex.getMessage(), auditContext);
            }
            throw ex;
        }
    }

    @Override
    public DccControlledFileBinary readUploadPreviewOnlyOfficeFile(Long fileId, String token) throws Exception {
        requireOnlyOfficeConfigured();
        onlyOfficePreviewTokenService.verify(token,
                DccOnlyOfficePreviewTokenService.RESOURCE_UPLOAD_PREVIEW, fileId);
        FileDO storedFile = fileMapper.selectById(fileId);
        if (storedFile == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        byte[] content = fileService.getFileContent(storedFile.getConfigId(), storedFile.getPath());
        return new DccControlledFileBinary(storedFile.getName(), storedFile.getType(), content, null);
    }

    private MultipartFile validatePreviewFile(DccControlledFileUploadPreviewReqVO reqVO) {
        if (reqVO == null || reqVO.getFiles() == null || reqVO.getFiles().length != 1 || reqVO.getFiles()[0] == null
                || reqVO.getFiles()[0].isEmpty()) {
            throw exception(CONTROLLED_FILE_UPLOAD_PREVIEW_SINGLE_FILE_REQUIRED);
        }
        MultipartFile file = reqVO.getFiles()[0];
        if (StrUtil.isBlank(file.getOriginalFilename())) {
            throw exception(CONTROLLED_FILE_UPLOAD_PREVIEW_SINGLE_FILE_REQUIRED);
        }
        return file;
    }

    private String validatePreviewPurposeName(String purpose, String fileName) {
        if (StrUtil.isBlank(purpose)) {
            throw exception(CONTROLLED_FILE_UPLOAD_PURPOSE_INVALID);
        }
        if (!DccControlledFileUploadTypePolicy.isSupportedPurpose(purpose)) {
            throw exception(CONTROLLED_FILE_UPLOAD_PURPOSE_INVALID);
        }
        String normalizedPurpose = DccControlledFileUploadTypePolicy.normalizePurpose(purpose);
        if (DccControlledFileUploadTypePolicy.isSourcePurpose(normalizedPurpose)
                && !DccControlledFileUploadTypePolicy.isAllowedEditableSourceName(fileName)) {
            throw exception(CONTROLLED_FILE_SOURCE_FILE_TYPE_INVALID);
        }
        return normalizedPurpose;
    }

    private void validatePreviewPurposeContent(String purpose, String fileName, byte[] content) {
        if (DccControlledFileUploadTypePolicy.isDrawingPdfPurpose(purpose)
                && !DccControlledFileUploadTypePolicy.isRealPdfFile(fileName, content)) {
            throw exception(CONTROLLED_FILE_DRAWING_PDF_FILE_INVALID);
        }
    }

    private void applyOfficePreview(FileDO storedFile,
                                    DccControlledFilePreviewKindEnum previewKind,
                                    DccControlledFileUploadRespVO respVO) {
        if (previewKind != DccControlledFilePreviewKindEnum.OFFICE) {
            return;
        }
        if (!onlyOfficePreviewProperties.isConfigured()) {
            respVO.setPreviewUnavailableReason(buildOnlyOfficeMissingReason());
            return;
        }
        respVO.setOnlyofficeBaseUrl(trimTrailingSlash(onlyOfficePreviewProperties.getBaseUrl()));
    }

    private void validatePreviewSession(String sessionId) {
        if (StrUtil.isBlank(sessionId)) {
            throw exception(CONTROLLED_FILE_UPLOAD_SESSION_INVALID);
        }
    }

    private void validateCategoryUploadPermission(Long categoryId, Long userId) {
        if (!permissionSupport.hasCategoryPermission(categoryId, userId, DccFileCategoryPermissionActionEnum.UPLOAD)) {
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        }
    }

    private void requireOnlyOfficeConfigured() {
        if (!onlyOfficePreviewProperties.isConfigured()) {
            throw exception(CONTROLLED_FILE_ONLYOFFICE_PREVIEW_CONFIG_MISSING,
                    onlyOfficePreviewProperties.missingReason());
        }
    }

    private void recordUploadBoundary(Long userId, String purpose, String result, String failureCode,
                                      String reason, DccRequestAuditContext auditContext) {
        accessAuditService.recordBoundaryLog(new DccAccessBoundaryLogCreateCommand(userId, "UPLOAD",
                StrUtil.blankToDefault(purpose, "UNKNOWN"), result, failureCode, reason,
                auditContext.sourceIp(), auditContext.requireRequestId("upload preview"), auditContext.userAgent()));
    }

    private String failureCode(Exception ex) {
        if (ex instanceof ServiceException serviceException) {
            Integer code = serviceException.getCode();
            if (matches(code, DCC_UPLOAD_SIZE_POLICY_MISSING)) {
                return "DCC_UPLOAD_SIZE_POLICY_MISSING";
            }
            if (matches(code, DCC_UPLOAD_SIZE_EXCEEDED)) {
                return "DCC_UPLOAD_SIZE_EXCEEDED";
            }
            if (matches(code, CONTROLLED_FILE_UPLOAD_PURPOSE_INVALID)) {
                return "CONTROLLED_FILE_UPLOAD_PURPOSE_INVALID";
            }
            if (matches(code, CONTROLLED_FILE_UPLOAD_PREVIEW_SINGLE_FILE_REQUIRED)) {
                return "CONTROLLED_FILE_UPLOAD_PREVIEW_SINGLE_FILE_REQUIRED";
            }
            if (matches(code, CONTROLLED_FILE_SOURCE_FILE_TYPE_INVALID)) {
                return "CONTROLLED_FILE_SOURCE_FILE_TYPE_INVALID";
            }
            if (matches(code, CONTROLLED_FILE_DRAWING_PDF_FILE_INVALID)) {
                return "CONTROLLED_FILE_DRAWING_PDF_FILE_INVALID";
            }
            if (matches(code, CONTROLLED_FILE_UPLOAD_SESSION_INVALID)) {
                return "CONTROLLED_FILE_UPLOAD_SESSION_INVALID";
            }
            return "SERVICE_EXCEPTION_" + code;
        }
        return ex.getClass().getSimpleName();
    }

    private boolean matches(Integer actualCode, ErrorCode expected) {
        return actualCode != null && actualCode.equals(expected.getCode());
    }

    private String buildOnlyOfficeMissingReason() {
        return "OnlyOffice preview config is missing: " + onlyOfficePreviewProperties.missingReason();
    }

    private String trimTrailingSlash(String value) {
        String normalized = StrUtil.trimToEmpty(value);
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
