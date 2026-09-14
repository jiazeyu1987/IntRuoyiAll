package cn.iocoder.yudao.module.dcc.service.file.access;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileTemporaryFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileTemporaryFileMapper;
import cn.iocoder.yudao.module.dcc.service.audit.DccControlledFileAccessAuditService;
import cn.iocoder.yudao.module.dcc.service.audit.DccDirectLinkDeniedLogCreateCommand;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileArtifactReference;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileQueryService;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileScope;
import cn.iocoder.yudao.module.dcc.service.file.DccRequestAuditContext;
import cn.iocoder.yudao.module.dcc.service.file.DccOnlyOfficePreviewTokenService;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessOperation;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessProvider;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessReference;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessRequest;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketServiceImpl.CLEANUP_ACTIVE;
import static cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketServiceImpl.STATUS_AVAILABLE;

@Service
public class DccBusinessFileAccessProvider implements BusinessFileAccessProvider {

    public static final String PROVIDER_ID = "dcc";
    private static final String BUSINESS_TYPE_CONTROLLED_FILE = "DCC_CONTROLLED_FILE";
    private static final String BUSINESS_TYPE_TEMPORARY_UPLOAD = "DCC_TEMPORARY_UPLOAD";
    private static final String ACTION_DIRECT_LINK = "DIRECT_LINK";
    private static final String PURPOSE_INFRA_DIRECT_LINK = "INFRA_DIRECT_LINK";
    private static final String RESULT_DENIED = "DENIED";
    private static final String FAILURE_DCC_DIRECT_LINK_BLOCKED = "DCC_DIRECT_LINK_BLOCKED";

    private final DccControlledFileQueryService controlledFileQueryService;
    private final DccControlledFileMapper controlledFileMapper;
    private final DccControlledFileTemporaryFileMapper temporaryFileMapper;
    private final DccControlledFileAccessAuditService accessAuditService;

    public DccBusinessFileAccessProvider(@Lazy DccControlledFileQueryService controlledFileQueryService,
                                         DccControlledFileMapper controlledFileMapper,
                                         DccControlledFileTemporaryFileMapper temporaryFileMapper,
                                         DccControlledFileAccessAuditService accessAuditService) {
        this.controlledFileQueryService = controlledFileQueryService;
        this.controlledFileMapper = controlledFileMapper;
        this.temporaryFileMapper = temporaryFileMapper;
        this.accessAuditService = accessAuditService;
    }

    @Override
    public String providerId() {
        return PROVIDER_ID;
    }

    @Override
    public Optional<BusinessFileAccessReference> resolve(Long fileId) {
        return executeTenantNeutral(() -> resolveTenantNeutral(fileId));
    }

    private Optional<BusinessFileAccessReference> resolveTenantNeutral(Long fileId) {
        DccControlledFileScope scope = Objects.requireNonNull(
                controlledFileQueryService.identifyControlledFileScope(fileId),
                "DCC controlled file scope is required");
        if (!Objects.equals(fileId, scope.infraFileId())) {
            throw new IllegalStateException("DCC scope file identity mismatch: fileId=" + fileId);
        }
        if (!scope.controlled()) {
            return resolveTemporaryUpload(fileId);
        }
        Set<BusinessObjectKey> objectKeys = scope.references().stream()
                .map(reference -> new BusinessObjectKey(reference.controlledFileId(), reference.tenantId()))
                .collect(Collectors.toSet());
        if (objectKeys.size() != 1) {
            throw new IllegalStateException("ambiguous DCC formal references: fileId=" + fileId
                    + ", objectCount=" + objectKeys.size());
        }
        BusinessObjectKey key = objectKeys.iterator().next();
        DccControlledFileDO controlledFile = controlledFileMapper.selectById(key.controlledFileId());
        if (controlledFile == null) {
            throw new IllegalStateException("DCC formal business object does not exist: controlledFileId="
                    + key.controlledFileId());
        }
        if (!Objects.equals(controlledFile.getId(), key.controlledFileId())
                || !Objects.equals(controlledFile.getTenantId(), key.tenantId())) {
            throw new IllegalStateException("DCC formal business object identity mismatch: controlledFileId="
                    + key.controlledFileId());
        }
        if (isBlank(controlledFile.getVersionNo())) {
            throw new IllegalStateException("DCC formal business version is required: controlledFileId="
                    + key.controlledFileId());
        }
        return Optional.of(new BusinessFileAccessReference(PROVIDER_ID, BUSINESS_TYPE_CONTROLLED_FILE,
                controlledFile.getId(), controlledFile.getVersionNo(), controlledFile.getTenantId(), null));
    }

    private Optional<BusinessFileAccessReference> resolveTemporaryUpload(Long fileId) {
        List<DccControlledFileTemporaryFileDO> temporaryFiles = temporaryFileMapper.selectList(
                new LambdaQueryWrapperX<DccControlledFileTemporaryFileDO>()
                        .eq(DccControlledFileTemporaryFileDO::getStorageFileId, fileId));
        if (temporaryFiles.isEmpty()) {
            return Optional.empty();
        }
        if (temporaryFiles.size() != 1) {
            throw new IllegalStateException("ambiguous DCC temporary upload references: fileId=" + fileId
                    + ", objectCount=" + temporaryFiles.size());
        }
        DccControlledFileTemporaryFileDO temporaryFile = temporaryFiles.get(0);
        if (temporaryFile.getId() == null || temporaryFile.getTenantId() == null) {
            throw new IllegalStateException("DCC temporary upload formal identity is incomplete: fileId=" + fileId);
        }
        return Optional.of(new BusinessFileAccessReference(PROVIDER_ID, BUSINESS_TYPE_TEMPORARY_UPLOAD,
                temporaryFile.getId(), temporaryVersionKey(temporaryFile.getId()),
                temporaryFile.getTenantId(), null));
    }

    @Override
    public boolean supports(BusinessFileAccessOperation operation) {
        return operation != null;
    }

    @Override
    public void assertAllowed(BusinessFileAccessRequest request, BusinessFileAccessReference reference) {
        requireReference(reference);
        if (request.operation() == BusinessFileAccessOperation.DIRECT_LINK) {
            if (BUSINESS_TYPE_CONTROLLED_FILE.equals(reference.businessType())) {
                recordDirectLinkDenial(request, reference);
            }
            return;
        }
        if (BUSINESS_TYPE_TEMPORARY_UPLOAD.equals(reference.businessType())) {
            assertTemporaryUploadAllowed(request, reference);
            return;
        }
        if (request.operation() == BusinessFileAccessOperation.CONVERT) {
            if (request.userId() != null
                    || !DccOnlyOfficePreviewTokenService.SERVICE_DCC_PDF_CONVERSION
                    .equals(request.serviceIdentity())) {
                throw new IllegalArgumentException("DCC conversion service identity is invalid");
            }
            return;
        }
        if (request.userId() == null || request.serviceIdentity() != null) {
            throw new IllegalArgumentException("DCC user operation requires a user subject");
        }
        controlledFileQueryService.assertBusinessFileAccess(request.userId(), reference.businessId(),
                request.operation(), new DccRequestAuditContext(
                        request.sourceIp(), request.userAgent(), request.requestId()));
    }

    private void recordDirectLinkDenial(BusinessFileAccessRequest request, BusinessFileAccessReference reference) {
        if (isBlank(request.sourceIp()) || isBlank(request.userAgent()) || isBlank(request.requestId())) {
            throw new IllegalArgumentException("DCC direct link audit context is required");
        }
        DccControlledFileScope scope = executeTenantNeutral(() -> Objects.requireNonNull(
                controlledFileQueryService.identifyControlledFileScope(request.fileId()),
                "DCC controlled file scope is required"));
        List<DccControlledFileArtifactReference> references = scope.references().stream()
                .filter(item -> Objects.equals(item.controlledFileId(), reference.businessId())
                        && Objects.equals(item.tenantId(), reference.tenantId()))
                .toList();
        if (references.isEmpty() || references.size() != scope.references().size()) {
            throw new IllegalStateException("DCC direct link formal reference changed: fileId=" + request.fileId());
        }
        for (DccControlledFileArtifactReference artifact : references) {
            accessAuditService.recordDirectLinkDeniedLog(new DccDirectLinkDeniedLogCreateCommand(
                    artifact.tenantId(), artifact.controlledFileId(), request.fileId(), artifact.role().name(),
                    ACTION_DIRECT_LINK, PURPOSE_INFRA_DIRECT_LINK, RESULT_DENIED,
                    FAILURE_DCC_DIRECT_LINK_BLOCKED,
                    "DCC controlled file direct link is blocked: infraFileId=" + request.fileId()
                            + ", controlledFileId=" + artifact.controlledFileId()
                            + ", artifactRole=" + artifact.role().name(),
                    request.sourceIp(), request.requestId(), request.userAgent()));
        }
    }

    private void requireReference(BusinessFileAccessReference reference) {
        if (reference == null || !PROVIDER_ID.equals(reference.providerId())
                || (!BUSINESS_TYPE_CONTROLLED_FILE.equals(reference.businessType())
                && !BUSINESS_TYPE_TEMPORARY_UPLOAD.equals(reference.businessType()))
                || reference.businessId() == null
                || reference.tenantId() == null || isBlank(reference.versionKey())) {
            throw new IllegalArgumentException("complete DCC business file reference is required");
        }
    }

    private void assertTemporaryUploadAllowed(BusinessFileAccessRequest request,
                                              BusinessFileAccessReference reference) {
        DccControlledFileTemporaryFileDO temporaryFile = executeTenantNeutral(
                () -> temporaryFileMapper.selectById(reference.businessId()));
        if (temporaryFile == null
                || !Objects.equals(temporaryFile.getId(), reference.businessId())
                || !Objects.equals(temporaryFile.getTenantId(), reference.tenantId())
                || !Objects.equals(temporaryFile.getStorageFileId(), request.fileId())
                || !Objects.equals(reference.versionKey(), temporaryVersionKey(temporaryFile.getId()))) {
            throw new IllegalArgumentException("DCC temporary upload formal reference changed");
        }
        if (!STATUS_AVAILABLE.equals(temporaryFile.getStatus())
                || !CLEANUP_ACTIVE.equals(temporaryFile.getCleanupStatus())
                || temporaryFile.getBoundControlledFileId() != null
                || temporaryFile.getExpireTime() == null
                || !LocalDateTime.now().isBefore(temporaryFile.getExpireTime())) {
            throw new IllegalArgumentException("DCC temporary upload is not active");
        }
        if (request.operation() == BusinessFileAccessOperation.CONVERT) {
            if (request.userId() != null
                    || !DccOnlyOfficePreviewTokenService.SERVICE_DCC_PDF_CONVERSION
                    .equals(request.serviceIdentity())) {
                throw new IllegalArgumentException("DCC temporary upload conversion identity is invalid");
            }
            return;
        }
        if (request.operation() != BusinessFileAccessOperation.PREVIEW
                && request.operation() != BusinessFileAccessOperation.ONLYOFFICE_PREVIEW) {
            throw new IllegalArgumentException("DCC temporary upload operation is not allowed");
        }
        if (request.serviceIdentity() != null
                || !Objects.equals(request.userId(), temporaryFile.getUploaderId())) {
            throw new IllegalArgumentException("DCC temporary upload owner is required");
        }
    }

    private String temporaryVersionKey(Long temporaryFileId) {
        return "TEMP-" + temporaryFileId;
    }

    private <T> T executeTenantNeutral(Supplier<T> action) {
        boolean oldIgnore = TenantContextHolder.isIgnore();
        try {
            TenantContextHolder.setIgnore(true);
            return action.get();
        } finally {
            TenantContextHolder.setIgnore(oldIgnore);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record BusinessObjectKey(Long controlledFileId, Long tenantId) {
        private BusinessObjectKey {
            Objects.requireNonNull(controlledFileId, "controlledFileId");
            Objects.requireNonNull(tenantId, "tenantId");
        }
    }
}
