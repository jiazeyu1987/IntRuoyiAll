package cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateFileDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateSnapshotDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateSnapshotEntrustedDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateVersionDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateFileMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateSnapshotEntrustedMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateSnapshotMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateVersionMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateEntrustedEnterprise;
import cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateProductionRelation;
import cn.iocoder.yudao.module.dcc.registrationcertificate.enums.DccRegistrationCertificateFileKind;
import cn.iocoder.yudao.module.dcc.registrationcertificate.enums.DccRegistrationCertificateFileOwnerType;
import cn.iocoder.yudao.module.dcc.registrationcertificate.enums.DccRegistrationCertificateFileStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Component;

import java.util.List;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_DRAFT_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_FILE_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_FORMALIZATION_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_PROJECTION_MISMATCH;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_REVISION_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_TENANT_MISMATCH;

@Component
public class DccRegistrationCertificateDraftRepository {

    private final DccRegistrationCertificateMapper certificateMapper;
    private final DccRegistrationCertificateVersionMapper versionMapper;
    private final DccRegistrationCertificateSnapshotMapper snapshotMapper;
    private final DccRegistrationCertificateSnapshotEntrustedMapper entrustedMapper;
    private final DccRegistrationCertificateFileMapper fileMapper;

    public DccRegistrationCertificateDraftRepository(
            DccRegistrationCertificateMapper certificateMapper,
            DccRegistrationCertificateVersionMapper versionMapper,
            DccRegistrationCertificateSnapshotMapper snapshotMapper,
            DccRegistrationCertificateSnapshotEntrustedMapper entrustedMapper,
            DccRegistrationCertificateFileMapper fileMapper) {
        this.certificateMapper = require(certificateMapper, "certificateMapper");
        this.versionMapper = require(versionMapper, "versionMapper");
        this.snapshotMapper = require(snapshotMapper, "snapshotMapper");
        this.entrustedMapper = require(entrustedMapper, "entrustedMapper");
        this.fileMapper = require(fileMapper, "fileMapper");
    }

    public DccRegistrationCertificateDraftState load(
            Long tenantId, Long certificateId, Integer expectedRowVersion, Integer expectedSnapshotRevision,
            DccRegistrationCertificateCommandContext context) {
        DccRegistrationCertificateDO certificate = certificateMapper.selectById(certificateId);
        if (certificate == null || !"DRAFT".equals(certificate.getStatus())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_DRAFT_NOT_EXISTS);
        }
        if (!tenantId.equals(certificate.getTenantId())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_TENANT_MISMATCH);
        }
        context.resolveTrustedIdentity(certificate.getOwnerCompanyId(), certificate.getId());
        if (!expectedRowVersion.equals(certificate.getRowVersion())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_REVISION_CONFLICT);
        }

        DccRegistrationCertificateVersionDO version = versionMapper.selectOne(
                new LambdaQueryWrapperX<DccRegistrationCertificateVersionDO>()
                        .eq(DccRegistrationCertificateVersionDO::getTenantId, tenantId)
                        .eq(DccRegistrationCertificateVersionDO::getCertificateId, certificateId)
                        .eq(DccRegistrationCertificateVersionDO::getStatus, "DRAFT"));
        if (version == null) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FORMALIZATION_CONFLICT);
        }
        List<DccRegistrationCertificateSnapshotDO> snapshots = snapshotMapper.selectListByVersionId(version.getId());
        if (snapshots == null || snapshots.size() != 1) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FORMALIZATION_CONFLICT);
        }
        DccRegistrationCertificateSnapshotDO snapshot = snapshots.get(0);
        if (!tenantId.equals(snapshot.getTenantId())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_TENANT_MISMATCH);
        }
        if (!expectedSnapshotRevision.equals(snapshot.getRevisionNo())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_REVISION_CONFLICT);
        }
        List<DccRegistrationCertificateSnapshotEntrustedDO> projection =
                entrustedMapper.selectListBySnapshotId(snapshot.getId());
        if (projection == null || projection.stream().anyMatch(row -> !tenantId.equals(row.getTenantId()))) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_PROJECTION_MISMATCH);
        }
        assertProjectionMatches(snapshot, projection);
        return new DccRegistrationCertificateDraftState(certificate, version, snapshot, projection);
    }

    public void replaceDraft(DccRegistrationCertificateDraftState state,
                             DccRegistrationCertificateDraftData draft,
                             DccRegistrationCertificateResolvedDraft resolved,
                             Long tenantId, Integer expectedRowVersion, Integer expectedSnapshotRevision) {
        int masterUpdated = certificateMapper.update(null, new LambdaUpdateWrapper<DccRegistrationCertificateDO>()
                .eq(DccRegistrationCertificateDO::getId, state.certificate().getId())
                .eq(DccRegistrationCertificateDO::getTenantId, tenantId)
                .eq(DccRegistrationCertificateDO::getStatus, "DRAFT")
                .eq(DccRegistrationCertificateDO::getRowVersion, expectedRowVersion)
                .set(DccRegistrationCertificateDO::getOwnerCompanyId, draft.ownerCompanyId())
                .set(DccRegistrationCertificateDO::getProductMasterId, draft.productMasterId())
                .set(DccRegistrationCertificateDO::getProjectCodeId, draft.projectCodeId())
                .set(DccRegistrationCertificateDO::getFirstObtainedDate, draft.firstObtainedDate())
                .setSql("row_version = row_version + 1"));
        requireSingle(masterUpdated, REGISTRATION_CERTIFICATE_REVISION_CONFLICT);

        int versionUpdated = versionMapper.update(null, new LambdaUpdateWrapper<DccRegistrationCertificateVersionDO>()
                .eq(DccRegistrationCertificateVersionDO::getId, state.version().getId())
                .eq(DccRegistrationCertificateVersionDO::getTenantId, tenantId)
                .eq(DccRegistrationCertificateVersionDO::getCertificateId, state.certificate().getId())
                .eq(DccRegistrationCertificateVersionDO::getStatus, "DRAFT")
                .set(DccRegistrationCertificateVersionDO::getCertificateNo, trim(draft.certificateNo()))
                .set(DccRegistrationCertificateVersionDO::getApprovalDate, draft.approvalDate())
                .set(DccRegistrationCertificateVersionDO::getEffectiveDate, draft.effectiveDate())
                .set(DccRegistrationCertificateVersionDO::getExpiryDate, draft.expiryDate())
                .set(DccRegistrationCertificateVersionDO::getClassification, trim(draft.classification()))
                .set(DccRegistrationCertificateVersionDO::getRemark, trim(draft.remark())));
        requireSingle(versionUpdated, REGISTRATION_CERTIFICATE_FORMALIZATION_CONFLICT);

        deleteProjection(state, tenantId, expectedSnapshotRevision);
        DccRegistrationCertificateSnapshotDO update = snapshotFor(
                state.snapshot().getId(), state.version().getId(), tenantId, draft, resolved);
        requireSingle(snapshotMapper.updateDraftByIdAndRevision(update, tenantId, expectedSnapshotRevision),
                REGISTRATION_CERTIFICATE_REVISION_CONFLICT);
        insertProjection(state.snapshot().getId(), tenantId, resolved.productionRelation());
    }

    public void deleteDraft(DccRegistrationCertificateDraftState state, Long tenantId,
                            Integer expectedRowVersion, Integer expectedSnapshotRevision) {
        transitionStagedFilesToCleanupRequired(state, tenantId);
        deleteProjection(state, tenantId, expectedSnapshotRevision);
        requireSingle(snapshotMapper.deleteDraftByIdAndRevision(
                state.snapshot().getId(), tenantId, expectedSnapshotRevision),
                REGISTRATION_CERTIFICATE_REVISION_CONFLICT);
        requireSingle(versionMapper.delete(new LambdaQueryWrapperX<DccRegistrationCertificateVersionDO>()
                        .eq(DccRegistrationCertificateVersionDO::getId, state.version().getId())
                        .eq(DccRegistrationCertificateVersionDO::getTenantId, tenantId)
                        .eq(DccRegistrationCertificateVersionDO::getCertificateId, state.certificate().getId())
                        .eq(DccRegistrationCertificateVersionDO::getStatus, "DRAFT")),
                REGISTRATION_CERTIFICATE_FORMALIZATION_CONFLICT);
        requireSingle(certificateMapper.delete(new LambdaQueryWrapperX<DccRegistrationCertificateDO>()
                        .eq(DccRegistrationCertificateDO::getId, state.certificate().getId())
                        .eq(DccRegistrationCertificateDO::getTenantId, tenantId)
                        .eq(DccRegistrationCertificateDO::getStatus, "DRAFT")
                        .eq(DccRegistrationCertificateDO::getRowVersion, expectedRowVersion)),
                REGISTRATION_CERTIFICATE_REVISION_CONFLICT);
    }

    private void transitionStagedFilesToCleanupRequired(
            DccRegistrationCertificateDraftState state, Long tenantId) {
        List<DccRegistrationCertificateFileDO> files = fileMapper.selectList(
                new LambdaQueryWrapperX<DccRegistrationCertificateFileDO>()
                        .eq(DccRegistrationCertificateFileDO::getTenantId, tenantId)
                        .eq(DccRegistrationCertificateFileDO::getOwnerType,
                                DccRegistrationCertificateFileOwnerType.VERSION.name())
                        .eq(DccRegistrationCertificateFileDO::getOwnerId, state.version().getId())
                        .eq(DccRegistrationCertificateFileDO::getFileKind,
                                DccRegistrationCertificateFileKind.REGISTRATION_CERTIFICATE.name()));
        List<Long> stagedFileIds = files.stream().map(file -> {
            DccRegistrationCertificateFileStatus status;
            try {
                status = DccRegistrationCertificateFileStatus.fromCode(file.getStatus());
            } catch (IllegalArgumentException exception) {
                ServiceException mapped = new ServiceException(REGISTRATION_CERTIFICATE_FILE_CONFLICT);
                mapped.initCause(exception);
                throw mapped;
            }
            if (status == DccRegistrationCertificateFileStatus.BOUND) {
                throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_CONFLICT);
            }
            return status == DccRegistrationCertificateFileStatus.STAGED ? file.getId() : null;
        }).filter(java.util.Objects::nonNull).toList();
        if (stagedFileIds.isEmpty()) {
            return;
        }
        int affected = fileMapper.update(null, new LambdaUpdateWrapper<DccRegistrationCertificateFileDO>()
                .in(DccRegistrationCertificateFileDO::getId, stagedFileIds)
                .eq(DccRegistrationCertificateFileDO::getTenantId, tenantId)
                .eq(DccRegistrationCertificateFileDO::getOwnerType,
                        DccRegistrationCertificateFileOwnerType.VERSION.name())
                .eq(DccRegistrationCertificateFileDO::getOwnerId, state.version().getId())
                .eq(DccRegistrationCertificateFileDO::getFileKind,
                        DccRegistrationCertificateFileKind.REGISTRATION_CERTIFICATE.name())
                .eq(DccRegistrationCertificateFileDO::getStatus,
                        DccRegistrationCertificateFileStatus.STAGED.name())
                .set(DccRegistrationCertificateFileDO::getStatus,
                        DccRegistrationCertificateFileStatus.CLEANUP_REQUIRED.name()));
        if (affected != stagedFileIds.size()) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_CONFLICT);
        }
    }

    private void deleteProjection(DccRegistrationCertificateDraftState state, Long tenantId,
                                  Integer expectedSnapshotRevision) {
        int affected = entrustedMapper.deleteDraftBySnapshotIdAndRevision(
                state.snapshot().getId(), tenantId, expectedSnapshotRevision);
        if (affected != state.entrustedProjection().size()) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_PROJECTION_MISMATCH);
        }
    }

    private void insertProjection(Long snapshotId, Long tenantId,
                                  DccRegistrationCertificateProductionRelation relation) {
        int sort = 1;
        for (DccRegistrationCertificateEntrustedEnterprise enterprise : relation.entrustedEnterprises()) {
            DccRegistrationCertificateSnapshotEntrustedDO row =
                    DccRegistrationCertificateSnapshotEntrustedDO.builder()
                            .snapshotId(snapshotId)
                            .enterpriseId(enterprise.enterpriseId())
                            .enterpriseNameSnapshot(enterprise.enterpriseName())
                            .sortOrder(sort++)
                            .build();
            row.setTenantId(tenantId);
            requireSingle(entrustedMapper.insert(row), REGISTRATION_CERTIFICATE_PROJECTION_MISMATCH);
        }
    }

    private void assertProjectionMatches(DccRegistrationCertificateSnapshotDO snapshot,
                                         List<DccRegistrationCertificateSnapshotEntrustedDO> projection) {
        try {
            List<DccRegistrationCertificateEntrustedEnterprise> authority = JsonUtils.parseObject(
                    snapshot.getEntrustedEnterprisesJson(), new TypeReference<>() {
                    });
            DccRegistrationCertificateProductionRelation relation =
                    new DccRegistrationCertificateProductionRelation(
                            Boolean.TRUE.equals(snapshot.getEntrustedProduction()),
                            Boolean.TRUE.equals(snapshot.getSelfProduction()), authority);
            relation.assertProjectionMatches(projection.stream()
                    .map(row -> new DccRegistrationCertificateEntrustedEnterprise(
                            row.getEnterpriseId(), row.getEnterpriseNameSnapshot()))
                    .toList());
        } catch (RuntimeException exception) {
            ServiceException mapped = new ServiceException(REGISTRATION_CERTIFICATE_PROJECTION_MISMATCH);
            mapped.initCause(exception);
            throw mapped;
        }
    }

    public static DccRegistrationCertificateSnapshotDO snapshotFor(
            Long snapshotId, Long versionId, Long tenantId,
            DccRegistrationCertificateDraftData draft,
            DccRegistrationCertificateResolvedDraft resolved) {
        DccRegistrationCertificateSnapshotDO snapshot = DccRegistrationCertificateSnapshotDO.builder()
                .id(snapshotId)
                .versionId(versionId)
                .revisionNo(1)
                .productName(resolved.productName())
                .registrantName(trim(draft.registrantName()))
                .modelSpecification(trim(draft.modelSpecification()))
                .structureComposition(trim(draft.structureComposition()))
                .intendedUse(trim(draft.intendedUse()))
                .technicalRequirements(trim(draft.technicalRequirements()))
                .residenceAddress(trim(draft.residenceAddress()))
                .productionAddress(trim(draft.productionAddress()))
                .entrustedProduction(resolved.productionRelation().entrustedProduction())
                .selfProduction(resolved.productionRelation().selfProduction())
                .entrustedEnterprisesJson(JsonUtils.toJsonString(resolved.productionRelation().entrustedEnterprises()))
                .effectiveAt(draft.effectiveDate().atStartOfDay())
                .build();
        snapshot.setTenantId(tenantId);
        return snapshot;
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static void requireSingle(int affected, cn.iocoder.yudao.framework.common.exception.ErrorCode errorCode) {
        if (affected != 1) {
            throw new ServiceException(errorCode);
        }
    }

    private static <T> T require(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " 不能为空");
        }
        return value;
    }
}
