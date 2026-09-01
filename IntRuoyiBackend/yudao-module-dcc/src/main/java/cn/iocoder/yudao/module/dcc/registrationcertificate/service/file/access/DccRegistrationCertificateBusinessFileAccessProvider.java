package cn.iocoder.yudao.module.dcc.registrationcertificate.service.file.access;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.audit.DccRegistrationCertificateReadAuditCommand;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.audit.DccRegistrationCertificateReadAuditService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.accesspolicy.DccRegistrationCertificateAccessPolicyService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateBusinessClock;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.reference.DccRegistrationCertificateFileReference;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.reference.DccRegistrationCertificateFileReferenceService;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessOperation;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessProvider;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessReference;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessRequest;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ACCESS_DENIED;

@Service
public class DccRegistrationCertificateBusinessFileAccessProvider implements BusinessFileAccessProvider {

    public static final String PROVIDER_ID = "dcc-registration-certificate";
    public static final String BUSINESS_TYPE = "DCC_REGISTRATION_CERTIFICATE_FILE";
    public static final String QUERY_CURRENT_PERMISSION = "dcc:registration-certificate:query-current";

    private final DccRegistrationCertificateFileReferenceService referenceService;
    private final DccRegistrationCertificateAccessPolicyService accessPolicyService;
    private final PermissionApi permissionApi;
    private final DccRegistrationCertificateReadAuditService readAuditService;
    private final DccRegistrationCertificateBusinessClock businessClock;

    public DccRegistrationCertificateBusinessFileAccessProvider(
            DccRegistrationCertificateFileReferenceService referenceService,
            DccRegistrationCertificateAccessPolicyService accessPolicyService,
            PermissionApi permissionApi,
            DccRegistrationCertificateReadAuditService readAuditService,
            DccRegistrationCertificateBusinessClock businessClock) {
        this.referenceService = require(referenceService, "referenceService");
        this.accessPolicyService = require(accessPolicyService, "accessPolicyService");
        this.permissionApi = require(permissionApi, "permissionApi");
        this.readAuditService = require(readAuditService, "readAuditService");
        this.businessClock = require(businessClock, "businessClock");
    }

    @Override
    public String providerId() {
        return PROVIDER_ID;
    }

    @Override
    public Optional<BusinessFileAccessReference> resolve(Long fileId) {
        return referenceService.resolveByInfraFileId(fileId).map(this::toBusinessReference);
    }

    @Override
    public boolean supports(BusinessFileAccessOperation operation) {
        return operation != null;
    }

    @Override
    public void assertAllowed(BusinessFileAccessRequest request, BusinessFileAccessReference reference) {
        DccRegistrationCertificateFileReference observed = referenceService.resolveByInfraFileId(request.fileId()).orElse(null);
        DccRegistrationCertificateFileReference live;
        try {
            live = requireLiveReference(request, reference);
        } catch (RuntimeException ex) {
            if (observed != null) {
                recordFailure(request, observed, "REFERENCE_DRIFT");
            }
            throw ex;
        }
        if (request.operation() == BusinessFileAccessOperation.DIRECT_LINK) {
            recordFailure(request, live, "DIRECT_LINK_BLOCKED");
            return;
        }
        if (request.operation() != BusinessFileAccessOperation.PREVIEW
                && request.operation() != BusinessFileAccessOperation.ONLYOFFICE_PREVIEW) {
            recordFailure(request, live, "OPERATION_NOT_AVAILABLE_BEFORE_SP06");
            throw new ServiceException(CONTROLLED_FILE_ACCESS_DENIED);
        }
        try {
            requireUserSubject(request);
            requireQueryCurrentPermission(request.userId());
            accessPolicyService.assertFilePreviewAllowed(
                    live.tenantId(), request.userId(), live.certificateId(), live.versionId(), businessClock.now());
        } catch (RuntimeException ex) {
            recordFailure(request, live, "REGISTRATION_CERTIFICATE_FILE_ACCESS_DENIED");
            throw ex;
        }
        recordSuccess(request, live);
    }

    private DccRegistrationCertificateFileReference requireLiveReference(
            BusinessFileAccessRequest request, BusinessFileAccessReference reference) {
        if (request == null || request.operation() == null || request.fileId() == null) {
            throw new ServiceException(CONTROLLED_FILE_ACCESS_DENIED);
        }
        if (reference == null || !PROVIDER_ID.equals(reference.providerId())
                || !BUSINESS_TYPE.equals(reference.businessType()) || reference.businessId() == null) {
            throw new ServiceException(CONTROLLED_FILE_ACCESS_DENIED);
        }
        DccRegistrationCertificateFileReference live = referenceService.requireBoundByReference(
                reference.tenantId(), reference.businessId(), request.fileId());
        BusinessFileAccessReference liveReference = toBusinessReference(live);
        if (!Objects.equals(liveReference, reference)) {
            recordFailure(request, live, "REFERENCE_DRIFT");
            throw new ServiceException(CONTROLLED_FILE_ACCESS_DENIED);
        }
        return live;
    }

    private BusinessFileAccessReference toBusinessReference(DccRegistrationCertificateFileReference reference) {
        return new BusinessFileAccessReference(PROVIDER_ID, BUSINESS_TYPE, reference.businessFileId(),
                reference.versionKey(), reference.tenantId(), reference.ownerCompanyId());
    }

    private void requireUserSubject(BusinessFileAccessRequest request) {
        if (request.userId() == null || request.serviceIdentity() != null) {
            throw new ServiceException(CONTROLLED_FILE_ACCESS_DENIED);
        }
    }

    private void requireQueryCurrentPermission(Long userId) {
        if (!permissionApi.hasAnyPermissions(userId, QUERY_CURRENT_PERMISSION)) {
            throw new ServiceException(CONTROLLED_FILE_ACCESS_DENIED);
        }
    }

    private void recordSuccess(BusinessFileAccessRequest request, DccRegistrationCertificateFileReference reference) {
        readAuditService.record(baseCommand(request, reference, "SUCCESS", "OK")
                .detailJson(toJsonString(Map.of("source", "file-preview", "operation", request.operation().name())))
                .build());
    }

    private void recordFailure(BusinessFileAccessRequest request, DccRegistrationCertificateFileReference reference,
                               String resultCode) {
        readAuditService.record(baseCommand(request, reference, "FAILURE", resultCode)
                .detailJson(toJsonString(Map.of("source", "file-preview", "operation", request.operation().name(),
                        "reason", resultCode)))
                .build());
    }

    private DccRegistrationCertificateReadAuditCommand.DccRegistrationCertificateReadAuditCommandBuilder baseCommand(
            BusinessFileAccessRequest request, DccRegistrationCertificateFileReference reference,
            String result, String resultCode) {
        return DccRegistrationCertificateReadAuditCommand.builder()
                .tenantId(reference.tenantId())
                .ownerCompanyId("SUCCESS".equals(result) ? reference.ownerCompanyId() : null)
                .certificateId("SUCCESS".equals(result) ? reference.certificateId() : null)
                .requestedOwnerCompanyId("SUCCESS".equals(result) ? null : reference.ownerCompanyId())
                .requestedCertificateId("SUCCESS".equals(result) ? null : reference.certificateId())
                .versionId("SUCCESS".equals(result) ? reference.versionId() : null)
                .businessFileId(reference.businessFileId())
                .operation(request.operation().name())
                .actorId(request.userId())
                .result(result)
                .resultCode(resultCode)
                .requestTraceId(request.requestId());
    }

    private static <T> T require(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + "不能为空");
        }
        return value;
    }
}
