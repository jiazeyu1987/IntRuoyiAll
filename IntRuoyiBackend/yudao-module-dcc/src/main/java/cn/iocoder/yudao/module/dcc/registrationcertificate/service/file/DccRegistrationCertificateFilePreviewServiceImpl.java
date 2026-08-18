package cn.iocoder.yudao.module.dcc.registrationcertificate.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePreviewMetadataRespVO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.audit.DccRegistrationCertificateReadAuditCommand;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.audit.DccRegistrationCertificateReadAuditService;
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
    private final DccOnlineFilePreviewService onlineFilePreviewService;
    private final DccRegistrationCertificateReadAuditService readAuditService;

    public DccRegistrationCertificateFilePreviewServiceImpl(
            DccRegistrationCertificateFileReferenceService referenceService,
            DccOnlineFilePreviewService onlineFilePreviewService,
            DccRegistrationCertificateReadAuditService readAuditService) {
        this.referenceService = require(referenceService, "referenceService");
        this.onlineFilePreviewService = require(onlineFilePreviewService, "onlineFilePreviewService");
        this.readAuditService = require(readAuditService, "readAuditService");
    }

    @Override
    public DccControlledFilePreviewMetadataRespVO getPreviewMetadata(Long tenantId, Long userId, Long businessFileId,
                                                                     DccRequestAuditContext auditContext) {
        DccRegistrationCertificateFileReference reference = requireCurrentForPreview(
                tenantId, userId, businessFileId, auditContext, "preview metadata");
        return onlineFilePreviewService.getPreviewMetadata(userId, reference.infraFileId(), auditContext);
    }

    @Override
    public DccControlledFileBinary readPreviewFile(Long tenantId, Long userId, Long businessFileId,
                                                   String viewerToken, String accessEventCode,
                                                   String watermarkTraceCode, String viewerTokenId,
                                                   String viewerTokenNonce, DccRequestAuditContext auditContext) {
        DccRegistrationCertificateFileReference reference = requireCurrentForPreview(
                tenantId, userId, businessFileId, auditContext, "preview binary");
        return onlineFilePreviewService.readPreviewFile(userId, reference.infraFileId(), viewerToken, accessEventCode,
                watermarkTraceCode, viewerTokenId, viewerTokenNonce, auditContext);
    }

    private DccRegistrationCertificateFileReference requireCurrentForPreview(
            Long tenantId, Long userId, Long businessFileId, DccRequestAuditContext auditContext, String purpose) {
        auditContext.requireRequestId("registration certificate " + purpose);
        try {
            return referenceService.requireCurrentByBusinessFileId(tenantId, businessFileId);
        } catch (RuntimeException ex) {
            recordFailure(tenantId, userId, businessFileId, auditContext, "REGISTRATION_CERTIFICATE_FILE_ACCESS_DENIED");
            throw ex;
        }
    }

    private void recordFailure(Long tenantId, Long userId, Long businessFileId,
                               DccRequestAuditContext auditContext, String resultCode) {
        String requestId = auditContext.requireRequestId("registration certificate preview failure");
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
    private static <T> T require(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }
}