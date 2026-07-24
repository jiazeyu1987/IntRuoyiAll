package cn.iocoder.yudao.module.dcc.service.filepreview;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePreviewMetadataRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileAccessEventDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileWatermarkTraceDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileAccessEventMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileWatermarkTraceMapper;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFilePreviewKindEnum;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileBinary;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileQueryService;
import cn.iocoder.yudao.module.dcc.service.file.DccOnlyOfficePreviewProperties;
import cn.iocoder.yudao.module.dcc.service.file.DccOnlyOfficePreviewTokenService;
import cn.iocoder.yudao.module.dcc.service.file.DccRequestAuditContext;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledPreviewWatermarkService;
import cn.iocoder.yudao.module.dcc.service.preview.DccControlledPreviewAccessService;
import cn.iocoder.yudao.module.dcc.service.preview.DccPreviewAccessRequest;
import cn.iocoder.yudao.module.dcc.service.preview.DccPreviewAccessResult;
import cn.iocoder.yudao.module.dcc.service.token.DccViewerTokenExpectedContext;
import cn.iocoder.yudao.module.dcc.service.token.DccViewerTokenService;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ACCESS_DENIED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_VIEWER_TOKEN_INVALID;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.FILE_NOT_EXISTS;

@Service
@Validated
public class DccOnlineFilePreviewServiceImpl implements DccOnlineFilePreviewService {

    public static final String RESOURCE_ONLINE_FILE_PREVIEW = "ONLINE_FILE_PREVIEW";
    public static final String ONLINE_FILE_PREVIEW_PURPOSE = "ONLINE_FILE_PREVIEW";
    public static final long ONLINE_FILE_PREVIEW_TTL_SECONDS = 900L;
    private static final String PREVIEW_ACCESS_TYPE = "PREVIEW";
    private static final String ACCESS_RESULT_SUCCESS = "SUCCESS";

    @Resource
    private FileMapper fileMapper;
    @Resource
    private FileService fileService;
    @Resource
    private DccControlledPreviewWatermarkService watermarkService;
    @Resource
    private DccControlledPreviewAccessService previewAccessService;
    @Resource
    private DccViewerTokenService viewerTokenService;
    @Resource
    private DccControlledFileAccessEventMapper accessEventMapper;
    @Resource
    private DccControlledFileWatermarkTraceMapper watermarkTraceMapper;
    @Resource
    private DccOnlyOfficePreviewProperties onlyOfficePreviewProperties;
    @Resource
    private DccOnlyOfficePreviewTokenService onlyOfficePreviewTokenService;
    @Resource
    private DccControlledFileQueryService controlledFileQueryService;

    @Override
    public DccControlledFilePreviewMetadataRespVO getPreviewMetadata(Long userId, Long fileId,
                                                                     DccRequestAuditContext auditContext) {
        DccRequestAuditContext requiredAuditContext = requireAuditContext(auditContext);
        FileDO file = requirePreviewableInfraFile(fileId);
        DccControlledFilePreviewKindEnum previewKind =
                DccControlledFilePreviewKindEnum.resolve(displayFileName(file), file.getType());
        DccPreviewAccessResult accessResult = previewAccessService.prepareAccess(new DccPreviewAccessRequest(
                TenantContextHolder.getRequiredTenantId(),
                userId,
                file.getId(),
                versionId(file.getId()),
                displayFileName(file),
                PREVIEW_ACCESS_TYPE,
                ONLINE_FILE_PREVIEW_PURPOSE,
                ONLINE_FILE_PREVIEW_TTL_SECONDS,
                userId == null ? null : String.valueOf(userId),
                userId == null ? null : String.valueOf(userId),
                null,
                null,
                String.valueOf(TenantContextHolder.getRequiredTenantId()),
                "TRACE_CODE_ONLY",
                requiredAuditContext.sourceIp(),
                requiredAuditContext.userAgent(),
                requiredAuditContext.requireRequestId("online file preview metadata")));
        requirePreviewAccessResult(accessResult);

        DccControlledFilePreviewMetadataRespVO respVO = new DccControlledFilePreviewMetadataRespVO();
        respVO.setPreviewKind(previewKind.getCode());
        respVO.setFileName(displayFileName(file));
        respVO.setContentType(StrUtil.blankToDefault(file.getType(), "application/octet-stream"));
        respVO.setViewerToken(accessResult.viewerToken());
        respVO.setViewerTokenId(accessResult.viewerTokenId());
        respVO.setViewerTokenNonce(accessResult.viewerTokenNonce());
        respVO.setAccessEventCode(accessResult.accessEventCode());
        respVO.setWatermarkTraceCode(accessResult.watermarkTraceCode());
        respVO.setWatermark(watermarkService.build(userId, "preview", displayFileName(file)));
        if (previewKind == DccControlledFilePreviewKindEnum.OFFICE) {
            applyOnlyOfficePreview(respVO, file);
        }
        return respVO;
    }

    @Override
    public DccControlledFileBinary readPreviewFile(Long userId, Long fileId, String viewerToken,
                                                   String accessEventCode, String watermarkTraceCode,
                                                   String viewerTokenId, String viewerTokenNonce,
                                                   DccRequestAuditContext auditContext) {
        requirePreviewContext(viewerToken, accessEventCode, watermarkTraceCode, viewerTokenId, viewerTokenNonce);
        DccRequestAuditContext requiredAuditContext = requireAuditContext(auditContext)
                .withRequestId(auditContext.requestIdOr(accessEventCode));
        FileDO file = requirePreviewableInfraFile(fileId);
        DccControlledFileAccessEventDO accessEvent = selectAccessEvent(accessEventCode);
        DccControlledFileWatermarkTraceDO watermarkTrace = selectWatermarkTrace(watermarkTraceCode);
        requireMatchingPreviewEvidence(userId, file, accessEvent, watermarkTrace);
        viewerTokenService.verify(viewerToken, new DccViewerTokenExpectedContext(
                TenantContextHolder.getRequiredTenantId(),
                userId,
                file.getId(),
                versionId(file.getId()),
                accessEvent.getId(),
                ONLINE_FILE_PREVIEW_PURPOSE,
                ONLINE_FILE_PREVIEW_TTL_SECONDS,
                viewerTokenNonce,
                viewerTokenId));
        try {
            byte[] content = fileService.getFileContent(file.getConfigId(), file.getPath());
            return new DccControlledFileBinary(displayFileName(file),
                    StrUtil.blankToDefault(file.getType(), "application/octet-stream"), content,
                    watermarkService.build(userId, "preview", displayFileName(file)));
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        }
    }

    @Override
    public DccControlledFileBinary readOnlyOfficePreviewFile(Long fileId, String token,
                                                            DccRequestAuditContext auditContext) throws Exception {
        requireOnlyOfficeConfigured();
        onlyOfficePreviewTokenService.verify(token, RESOURCE_ONLINE_FILE_PREVIEW, fileId);
        FileDO file = requirePreviewableInfraFile(fileId);
        byte[] content = fileService.getFileContent(file.getConfigId(), file.getPath());
        return new DccControlledFileBinary(displayFileName(file),
                StrUtil.blankToDefault(file.getType(), "application/octet-stream"), content, null);
    }

    private FileDO requirePreviewableInfraFile(Long fileId) {
        if (fileId == null || fileId <= 0) {
            throw exception(FILE_NOT_EXISTS);
        }
        var controlledFileScope = controlledFileQueryService.identifyControlledFileScope(fileId);
        if (controlledFileScope == null || controlledFileScope.controlled()) {
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        }
        FileDO file = fileMapper.selectById(fileId);
        if (file == null || file.getConfigId() == null || StrUtil.isBlank(file.getPath())) {
            throw exception(FILE_NOT_EXISTS);
        }
        return file;
    }

    private void applyOnlyOfficePreview(DccControlledFilePreviewMetadataRespVO respVO, FileDO file) {
        if (!onlyOfficePreviewProperties.isConfigured()) {
            respVO.setPreviewUnavailableReason(buildOnlyOfficeMissingReason());
            return;
        }
        respVO.setOnlyofficeBaseUrl(trimTrailingSlash(onlyOfficePreviewProperties.getBaseUrl()));
        String token = onlyOfficePreviewTokenService.issue(RESOURCE_ONLINE_FILE_PREVIEW, file.getId());
        respVO.setOnlyofficeDocumentUrl(trimTrailingSlash(onlyOfficePreviewProperties.getPublicFileBaseUrl())
                + "/admin-api/dcc/file-preview/files/" + file.getId() + "/onlyoffice-file?token=" + token);
    }

    private void requireOnlyOfficeConfigured() {
        if (!onlyOfficePreviewProperties.isConfigured()) {
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        }
    }

    private void requirePreviewAccessResult(DccPreviewAccessResult accessResult) {
        if (accessResult == null
                || accessResult.accessEventId() == null
                || StrUtil.isBlank(accessResult.accessEventCode())
                || accessResult.watermarkTraceId() == null
                || StrUtil.isBlank(accessResult.watermarkTraceCode())
                || StrUtil.isBlank(accessResult.viewerToken())
                || StrUtil.isBlank(accessResult.viewerTokenId())
                || StrUtil.isBlank(accessResult.viewerTokenNonce())) {
            throw exception(CONTROLLED_FILE_VIEWER_TOKEN_INVALID);
        }
    }

    private void requirePreviewContext(String viewerToken, String accessEventCode, String watermarkTraceCode,
                                       String viewerTokenId, String viewerTokenNonce) {
        if (StrUtil.isBlank(viewerToken)
                || StrUtil.isBlank(accessEventCode)
                || StrUtil.isBlank(watermarkTraceCode)
                || StrUtil.isBlank(viewerTokenId)
                || StrUtil.isBlank(viewerTokenNonce)) {
            throw exception(CONTROLLED_FILE_VIEWER_TOKEN_INVALID);
        }
    }

    private DccControlledFileAccessEventDO selectAccessEvent(String accessEventCode) {
        DccControlledFileAccessEventDO accessEvent = accessEventMapper.selectOne(
                new LambdaQueryWrapper<DccControlledFileAccessEventDO>()
                        .eq(DccControlledFileAccessEventDO::getAccessEventCode, accessEventCode));
        if (accessEvent == null) {
            throw exception(CONTROLLED_FILE_VIEWER_TOKEN_INVALID);
        }
        return accessEvent;
    }

    private DccControlledFileWatermarkTraceDO selectWatermarkTrace(String watermarkTraceCode) {
        DccControlledFileWatermarkTraceDO watermarkTrace = watermarkTraceMapper.selectOne(
                new LambdaQueryWrapper<DccControlledFileWatermarkTraceDO>()
                        .eq(DccControlledFileWatermarkTraceDO::getTraceCode, watermarkTraceCode));
        if (watermarkTrace == null) {
            throw exception(CONTROLLED_FILE_VIEWER_TOKEN_INVALID);
        }
        return watermarkTrace;
    }

    private void requireMatchingPreviewEvidence(Long userId, FileDO file,
                                                DccControlledFileAccessEventDO accessEvent,
                                                DccControlledFileWatermarkTraceDO watermarkTrace) {
        if (!Objects.equals(accessEvent.getControlledFileId(), file.getId())
                || !Objects.equals(accessEvent.getUserId(), userId)
                || !Objects.equals(accessEvent.getFileVersionNo(), versionId(file.getId()))
                || !PREVIEW_ACCESS_TYPE.equals(accessEvent.getAccessType())
                || !ONLINE_FILE_PREVIEW_PURPOSE.equals(accessEvent.getPurpose())
                || !ACCESS_RESULT_SUCCESS.equals(accessEvent.getResult())
                || !Objects.equals(watermarkTrace.getAccessEventId(), accessEvent.getId())
                || !Objects.equals(watermarkTrace.getAccessEventCode(), accessEvent.getAccessEventCode())
                || !Objects.equals(watermarkTrace.getControlledFileId(), file.getId())
                || !Objects.equals(watermarkTrace.getUserId(), userId)
                || !Objects.equals(watermarkTrace.getFileVersionNo(), versionId(file.getId()))) {
            throw exception(CONTROLLED_FILE_VIEWER_TOKEN_INVALID);
        }
    }

    private DccRequestAuditContext requireAuditContext(DccRequestAuditContext auditContext) {
        if (auditContext == null) {
            throw new IllegalArgumentException("auditContext is required");
        }
        return auditContext;
    }

    private String displayFileName(FileDO file) {
        return StrUtil.blankToDefault(StrUtil.trim(file.getName()),
                StrUtil.blankToDefault(StrUtil.trim(file.getPath()), "file-" + file.getId()));
    }

    private static String versionId(Long fileId) {
        return "INFRA_FILE:" + fileId;
    }

    private String trimTrailingSlash(String value) {
        return StrUtil.removeSuffix(StrUtil.trim(value), "/");
    }

    private String buildOnlyOfficeMissingReason() {
        return "OnlyOffice preview config is missing: " + onlyOfficePreviewProperties.missingReason();
    }
}
