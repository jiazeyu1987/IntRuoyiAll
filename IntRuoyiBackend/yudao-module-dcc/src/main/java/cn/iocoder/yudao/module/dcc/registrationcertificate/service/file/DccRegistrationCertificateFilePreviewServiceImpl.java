package cn.iocoder.yudao.module.dcc.registrationcertificate.service.file;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePreviewMetadataRespVO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.accesspolicy.DccRegistrationCertificateAccessPolicyService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.audit.DccRegistrationCertificateReadAuditCommand;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.audit.DccRegistrationCertificateReadAuditService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateBusinessClock;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.reference.DccRegistrationCertificateFileReference;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.reference.DccRegistrationCertificateFileReferenceService;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileBinary;
import cn.iocoder.yudao.module.dcc.service.file.DccRequestAuditContext;
import cn.iocoder.yudao.module.dcc.service.filepreview.DccOnlineFilePreviewService;
import org.springframework.stereotype.Service;

import java.util.Map;

import static cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString;

@Service
public class DccRegistrationCertificateFilePreviewServiceImpl implements DccRegistrationCertificateFilePreviewService {

    private final DccRegistrationCertificateFileReferenceService referenceService;
    private final DccRegistrationCertificateAccessPolicyService accessPolicyService;
    private final DccOnlineFilePreviewService onlineFilePreviewService;
    private final DccRegistrationCertificateReadAuditService readAuditService;
    private final DccRegistrationCertificateBusinessClock businessClock;

    public DccRegistrationCertificateFilePreviewServiceImpl(
            DccRegistrationCertificateFileReferenceService referenceService,
            DccRegistrationCertificateAccessPolicyService accessPolicyService,
            DccOnlineFilePreviewService onlineFilePreviewService,
            DccRegistrationCertificateReadAuditService readAuditService,
            DccRegistrationCertificateBusinessClock businessClock) {
        this.referenceService = require(referenceService, "referenceService");
        this.accessPolicyService = require(accessPolicyService, "accessPolicyService");
        this.onlineFilePreviewService = require(onlineFilePreviewService, "onlineFilePreviewService");
        this.readAuditService = require(readAuditService, "readAuditService");
        this.businessClock = require(businessClock, "businessClock");
    }

    @Override
    public DccControlledFilePreviewMetadataRespVO getPreviewMetadata(Long tenantId, Long userId, Long businessFileId,
                                                                     DccRequestAuditContext auditContext) {
        DccRegistrationCertificateFileReference reference = requireAuthorizedForPreview(
                tenantId, userId, businessFileId, auditContext, "预览信息");
        DccControlledFilePreviewMetadataRespVO metadata =
                onlineFilePreviewService.getPreviewMetadata(userId, reference.infraFileId(), auditContext);
        metadata.setFileName(previewDownloadFileName(reference, metadata.getFileName()));
        return metadata;
    }

    @Override
    public DccControlledFileBinary readPreviewFile(Long tenantId, Long userId, Long businessFileId,
                                                   String viewerToken, String accessEventCode,
                                                   String watermarkTraceCode, String viewerTokenId,
                                                   String viewerTokenNonce, DccRequestAuditContext auditContext) {
        DccRegistrationCertificateFileReference reference = requireAuthorizedForPreview(
                tenantId, userId, businessFileId, auditContext, "预览文件内容");
        DccControlledFileBinary binary = onlineFilePreviewService.readPreviewFile(
                userId, reference.infraFileId(), viewerToken, accessEventCode,
                watermarkTraceCode, viewerTokenId, viewerTokenNonce, auditContext);
        return new DccControlledFileBinary(previewDownloadFileName(reference, binary.fileName()),
                binary.contentType(), binary.bytes(), binary.watermark());
    }

    private DccRegistrationCertificateFileReference requireAuthorizedForPreview(
            Long tenantId, Long userId, Long businessFileId, DccRequestAuditContext auditContext, String purpose) {
        auditContext.requireRequestId("注册证" + purpose);
        try {
            DccRegistrationCertificateFileReference reference =
                    referenceService.requireBoundByBusinessFileId(tenantId, businessFileId);
            accessPolicyService.assertFilePreviewAllowed(
                    tenantId, userId, reference.certificateId(), reference.versionId(), businessClock.now());
            return reference;
        } catch (ServiceException ex) {
            recordFailure(tenantId, userId, businessFileId, auditContext, "REGISTRATION_CERTIFICATE_FILE_ACCESS_DENIED");
            throw new ServiceException(cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ACCESS_DENIED);
        }
    }

    private void recordFailure(Long tenantId, Long userId, Long businessFileId,
                               DccRequestAuditContext auditContext, String resultCode) {
        String requestId = auditContext.requireRequestId("注册证预览失败");
        referenceService.resolveByBusinessFileId(tenantId, businessFileId).ifPresent(reference ->
                readAuditService.record(DccRegistrationCertificateReadAuditCommand.builder()
                        .tenantId(reference.tenantId())
                        .ownerCompanyId(null)
                        .certificateId(null)
                        .requestedOwnerCompanyId(reference.ownerCompanyId())
                        .requestedCertificateId(reference.certificateId())
                        .versionId(null)
                        .businessFileId(reference.businessFileId())
                        .operation("PREVIEW")
                        .actorId(userId)
                        .result("FAILURE")
                        .resultCode(resultCode)
                        .requestTraceId(requestId)
                        .detailJson(toJsonString(Map.of("source", "file-preview", "operation", "PREVIEW",
                                "reason", resultCode)))
                        .build()));
    }

    private String previewDownloadFileName(DccRegistrationCertificateFileReference reference, String fileName) {
        if (!"OLD".equals(reference.versionStatus()) || fileName == null || fileName.isBlank()) {
            return fileName;
        }
        String trimmed = fileName.trim();
        int extensionIndex = trimmed.lastIndexOf('.');
        if (extensionIndex <= 0 || extensionIndex == trimmed.length() - 1) {
            return trimmed + "_已失效";
        }
        return trimmed.substring(0, extensionIndex) + "_已失效" + trimmed.substring(extensionIndex);
    }

    private static <T> T require(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + "不能为空");
        }
        return value;
    }
}
