package cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateSnapshotEntrustedDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateSnapshotDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateVersionDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateSnapshotEntrustedMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateSnapshotMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateVersionMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateEntrustedEnterprise;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.formalization.DccRegistrationCertificateFormalizationResult;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.formalization.DccRegistrationCertificateFormalizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_FORMALIZATION_CONFLICT;

@Service
public class DccRegistrationCertificateCommandTransactionService {

    private final DccRegistrationCertificateMapper certificateMapper;
    private final DccRegistrationCertificateVersionMapper versionMapper;
    private final DccRegistrationCertificateSnapshotMapper snapshotMapper;
    private final DccRegistrationCertificateSnapshotEntrustedMapper entrustedMapper;
    private final DccRegistrationCertificateDraftRepository draftRepository;
    private final DccRegistrationCertificatePrerequisiteValidator prerequisiteValidator;
    private final DccRegistrationCertificateFormalizationService formalizationService;
    private final DccRegistrationCertificateTerminalAuditService auditService;

    public DccRegistrationCertificateCommandTransactionService(
            DccRegistrationCertificateMapper certificateMapper,
            DccRegistrationCertificateVersionMapper versionMapper,
            DccRegistrationCertificateSnapshotMapper snapshotMapper,
            DccRegistrationCertificateSnapshotEntrustedMapper entrustedMapper,
            DccRegistrationCertificateDraftRepository draftRepository,
            DccRegistrationCertificatePrerequisiteValidator prerequisiteValidator,
            DccRegistrationCertificateFormalizationService formalizationService,
            DccRegistrationCertificateTerminalAuditService auditService) {
        this.certificateMapper = certificateMapper;
        this.versionMapper = versionMapper;
        this.snapshotMapper = snapshotMapper;
        this.entrustedMapper = entrustedMapper;
        this.draftRepository = draftRepository;
        this.prerequisiteValidator = prerequisiteValidator;
        this.formalizationService = formalizationService;
        this.auditService = auditService;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createDraft(DccRegistrationCertificateCommandMetadata metadata,
                            DccRegistrationCertificateCommandContext context,
                            DccRegistrationCertificateDraftData draft) {
        DccRegistrationCertificateResolvedDraft resolved = prerequisiteValidator.validate(
                metadata.tenantId(), metadata.actorId(), draft);
        DccRegistrationCertificateDO certificate = DccRegistrationCertificateDO.builder()
                .ownerCompanyId(draft.ownerCompanyId())
                .productMasterId(draft.productMasterId())
                .projectCodeId(draft.projectCodeId())
                .firstObtainedDate(draft.firstObtainedDate())
                .status("DRAFT")
                .rowVersion(1)
                .build();
        certificate.setTenantId(metadata.tenantId());
        requireSingle(certificateMapper.insert(certificate));
        if (certificate.getId() == null || certificate.getId() <= 0) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FORMALIZATION_CONFLICT);
        }
        context.resolveTrustedIdentity(draft.ownerCompanyId(), certificate.getId());

        DccRegistrationCertificateVersionDO version = DccRegistrationCertificateVersionDO.builder()
                .certificateId(certificate.getId())
                .versionNo(1)
                .versionType("INITIAL_CERTIFICATE")
                .certificateNo(draft.certificateNo().trim())
                .approvalDate(draft.approvalDate())
                .effectiveDate(draft.effectiveDate())
                .expiryDate(draft.expiryDate())
                .classification(draft.classification().trim())
                .categoryChanged(false)
                .remark(draft.remark() == null ? null : draft.remark().trim())
                .status("DRAFT")
                .build();
        version.setTenantId(metadata.tenantId());
        requireSingle(versionMapper.insert(version));

        DccRegistrationCertificateSnapshotDO snapshot = DccRegistrationCertificateDraftRepository.snapshotFor(
                null, version.getId(), metadata.tenantId(), draft, resolved);
        requireSingle(snapshotMapper.insert(snapshot));
        int sort = 1;
        for (DccRegistrationCertificateEntrustedEnterprise enterprise
                : resolved.productionRelation().entrustedEnterprises()) {
            DccRegistrationCertificateSnapshotEntrustedDO row =
                    DccRegistrationCertificateSnapshotEntrustedDO.builder()
                            .snapshotId(snapshot.getId())
                            .enterpriseId(enterprise.enterpriseId())
                            .enterpriseNameSnapshot(enterprise.enterpriseName())
                            .sortOrder(sort++)
                            .build();
            row.setTenantId(metadata.tenantId());
            requireSingle(entrustedMapper.insert(row));
        }
        auditService.recordSuccess(metadata, context, version.getId(), snapshot.getId(), null);
        return certificate.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public Long updateDraft(DccRegistrationCertificateCommandMetadata metadata,
                            DccRegistrationCertificateCommandContext context,
                            Long certificateId, Integer expectedRowVersion, Integer expectedSnapshotRevision,
                            DccRegistrationCertificateDraftData draft) {
        DccRegistrationCertificateDraftState state = draftRepository.load(
                metadata.tenantId(), certificateId, expectedRowVersion, expectedSnapshotRevision, context);
        prerequisiteValidator.validateCompanyScope(
                metadata.actorId(), state.certificate().getOwnerCompanyId());
        DccRegistrationCertificateResolvedDraft resolved = prerequisiteValidator.validate(
                metadata.tenantId(), metadata.actorId(), draft);
        draftRepository.replaceDraft(state, draft, resolved, metadata.tenantId(),
                expectedRowVersion, expectedSnapshotRevision);
        context.resolveTrustedIdentity(draft.ownerCompanyId(), certificateId);
        auditService.recordSuccess(metadata, context, state.version().getId(), state.snapshot().getId(), null);
        return certificateId;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long deleteDraft(DccRegistrationCertificateCommandMetadata metadata,
                            DccRegistrationCertificateCommandContext context,
                            Long certificateId, Integer expectedRowVersion, Integer expectedSnapshotRevision) {
        DccRegistrationCertificateDraftState state = draftRepository.load(
                metadata.tenantId(), certificateId, expectedRowVersion, expectedSnapshotRevision, context);
        prerequisiteValidator.validateCompanyScope(
                metadata.actorId(), state.certificate().getOwnerCompanyId());
        draftRepository.deleteDraft(state, metadata.tenantId(), expectedRowVersion, expectedSnapshotRevision);
        auditService.recordSuccess(metadata, context, state.version().getId(), state.snapshot().getId(), null);
        return certificateId;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long formalize(DccRegistrationCertificateCommandMetadata metadata,
                          DccRegistrationCertificateCommandContext context,
                          Long certificateId, Integer expectedRowVersion, Integer expectedSnapshotRevision,
                          Long businessFileId) {
        DccRegistrationCertificateDraftState state = draftRepository.load(
                metadata.tenantId(), certificateId, expectedRowVersion, expectedSnapshotRevision, context);
        DccRegistrationCertificateDraftData stored = storedDraft(state);
        DccRegistrationCertificateResolvedDraft resolved = prerequisiteValidator.validate(
                metadata.tenantId(), metadata.actorId(), stored);
        DccRegistrationCertificateFormalizationResult result = formalizationService.formalize(
                state, resolved, metadata.tenantId(), metadata.actorId(), expectedRowVersion, businessFileId);
        auditService.recordSuccess(metadata, context, result.versionId(), result.snapshotId(), result.businessFileId());
        return result.certificateId();
    }

    private static DccRegistrationCertificateDraftData storedDraft(DccRegistrationCertificateDraftState state) {
        return new DccRegistrationCertificateDraftData(
                state.certificate().getOwnerCompanyId(), state.certificate().getProductMasterId(),
                state.certificate().getProjectCodeId(), state.certificate().getFirstObtainedDate(),
                state.version().getCertificateNo(), state.version().getApprovalDate(),
                state.version().getEffectiveDate(), state.version().getExpiryDate(),
                state.version().getClassification(), state.snapshot().getRegistrantName(),
                state.snapshot().getModelSpecification(), state.snapshot().getStructureComposition(),
                state.snapshot().getIntendedUse(), state.snapshot().getTechnicalRequirements(),
                state.snapshot().getResidenceAddress(), state.snapshot().getProductionAddress(),
                state.snapshot().getEntrustedProduction(), state.snapshot().getSelfProduction(),
                state.entrustedProjection().stream()
                        .map(DccRegistrationCertificateSnapshotEntrustedDO::getEnterpriseId).toList(),
                state.version().getRemark());
    }

    private static void requireSingle(int affected) {
        if (affected != 1) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FORMALIZATION_CONFLICT);
        }
    }
}
