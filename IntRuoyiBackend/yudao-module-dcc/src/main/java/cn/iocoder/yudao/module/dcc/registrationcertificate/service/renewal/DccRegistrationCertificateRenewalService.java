package cn.iocoder.yudao.module.dcc.registrationcertificate.service.renewal;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;
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
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateBusinessClock;
import cn.iocoder.yudao.module.system.service.controlledcontent.ControlledContentKey;
import cn.iocoder.yudao.module.system.service.controlledcontent.ControlledContentProjectionSnapshot;
import cn.iocoder.yudao.module.system.service.controlledcontent.ControlledContentRegistrationProjectionService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_CANDIDATE_VOID_REASON_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_FILE_NOT_STAGED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_FILE_OWNER_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_FILE_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_FILE_TENANT_MISMATCH;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_IDEMPOTENCY_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_IDEMPOTENCY_KEY_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_LIFECYCLE_EVENT_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_RENEWAL_BASE_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_RENEWAL_CATEGORY_CHANGE_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_RENEWAL_FIELD_FORBIDDEN;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_RENEWAL_PENDING_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_REVISION_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_STATUS_INVALID;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentType.DCC_REGISTRATION_CERTIFICATE;

@Service
public class DccRegistrationCertificateRenewalService {

    private static final String VERSION_TYPE_RENEWAL = "RENEWAL_CERTIFICATE";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_CURRENT = "CURRENT";
    private static final String STATUS_PENDING = "PENDING_EFFECTIVE";
    private static final String STATUS_VOIDED = "VOIDED";
    private static final String FILE_STATUS_STAGED = "STAGED";
    private static final String FILE_STATUS_BOUND = "BOUND";
    private static final String FILE_STATUS_VOIDED = "VOIDED";
    private static final String FILE_OWNER_VERSION = "VERSION";
    private static final String FILE_KIND_REGISTRATION_CERTIFICATE = "REGISTRATION_CERTIFICATE";

    private final DccRegistrationCertificateMapper certificateMapper;
    private final DccRegistrationCertificateVersionMapper versionMapper;
    private final DccRegistrationCertificateSnapshotMapper snapshotMapper;
    private final DccRegistrationCertificateSnapshotEntrustedMapper entrustedMapper;
    private final DccRegistrationCertificateFileMapper fileMapper;
    private final JdbcTemplate jdbcTemplate;
    private final ControlledContentRegistrationProjectionService projectionService;
    private final DccRegistrationCertificateBusinessClock businessClock;

    public DccRegistrationCertificateRenewalService(
            DccRegistrationCertificateMapper certificateMapper,
            DccRegistrationCertificateVersionMapper versionMapper,
            DccRegistrationCertificateSnapshotMapper snapshotMapper,
            DccRegistrationCertificateSnapshotEntrustedMapper entrustedMapper,
            DccRegistrationCertificateFileMapper fileMapper,
            JdbcTemplate jdbcTemplate,
            ControlledContentRegistrationProjectionService projectionService,
            DccRegistrationCertificateBusinessClock businessClock) {
        this.certificateMapper = require(certificateMapper, "certificateMapper");
        this.versionMapper = require(versionMapper, "versionMapper");
        this.snapshotMapper = require(snapshotMapper, "snapshotMapper");
        this.entrustedMapper = require(entrustedMapper, "entrustedMapper");
        this.fileMapper = require(fileMapper, "fileMapper");
        this.jdbcTemplate = require(jdbcTemplate, "jdbcTemplate");
        this.projectionService = require(projectionService, "projectionService");
        this.businessClock = require(businessClock, "businessClock");
    }

    @Transactional(rollbackFor = Exception.class)
    public DccRegistrationCertificateRenewalResult uploadRenewalCandidate(
            DccRegistrationCertificateRenewalCommand command) {
        validateEventInput(command.tenantId(), command.actorId(), command.idempotencyKey(), command.requestTraceId());
        String payloadHash = uploadPayloadHash(command);
        LifecycleEvent existing = findEvent(command.tenantId(), command.idempotencyKey());
        if (existing != null) {
            return replayUpload(command, payloadHash, existing);
        }

        DccRegistrationCertificateDO certificate = requireActiveCertificate(command);
        DccRegistrationCertificateVersionDO currentVersion = requireCurrentVersion(certificate, command.currentVersionId());
        DccRegistrationCertificateSnapshotDO currentSnapshot = requireSnapshot(certificate.getCurrentSnapshotId(),
                currentVersion.getId());
        List<DccRegistrationCertificateSnapshotEntrustedDO> entrustedRows =
                entrustedMapper.selectListBySnapshotId(currentSnapshot.getId());
        DccRegistrationCertificateFileDO file = requireStagedRenewalFile(
                command.tenantId(), currentVersion.getId(), command.businessFileId());
        validateRenewalDates(certificate.getFirstObtainedDate(), command.approvalDate(),
                command.effectiveDate(), command.expiryDate());

        String renewalCertificateNo = resolveRenewalCertificateNo(command, currentVersion);
        String renewalClassification = resolveRenewalClassification(command, currentVersion);

        DccRegistrationCertificateVersionDO renewalVersion = DccRegistrationCertificateVersionDO.builder()
                .certificateId(certificate.getId())
                .versionNo(currentVersion.getVersionNo() + 1)
                .versionType(VERSION_TYPE_RENEWAL)
                .certificateNo(renewalCertificateNo)
                .approvalDate(command.approvalDate())
                .effectiveDate(command.effectiveDate())
                .expiryDate(command.expiryDate())
                .classification(renewalClassification)
                .categoryChanged(Boolean.TRUE.equals(command.categoryChanged()))
                .baseSnapshotId(currentSnapshot.getId())
                .status(STATUS_PENDING)
                .formalizedAt(businessClock.now())
                .formalizedBy(command.actorId())
                .build();
        renewalVersion.setTenantId(command.tenantId());
        try {
            requireSingle(versionMapper.insert(renewalVersion), REGISTRATION_CERTIFICATE_RENEWAL_BASE_CONFLICT);
        } catch (DuplicateKeyException exception) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_RENEWAL_PENDING_CONFLICT);
        }

        DccRegistrationCertificateSnapshotDO renewalSnapshot = copySnapshot(currentSnapshot, renewalVersion.getId());
        requireSingle(snapshotMapper.insert(renewalSnapshot), REGISTRATION_CERTIFICATE_RENEWAL_BASE_CONFLICT);
        for (DccRegistrationCertificateSnapshotEntrustedDO row : entrustedRows) {
            DccRegistrationCertificateSnapshotEntrustedDO copy =
                    DccRegistrationCertificateSnapshotEntrustedDO.builder()
                            .snapshotId(renewalSnapshot.getId())
                            .enterpriseId(row.getEnterpriseId())
                            .enterpriseNameSnapshot(row.getEnterpriseNameSnapshot())
                            .sortOrder(row.getSortOrder())
                            .build();
            copy.setTenantId(command.tenantId());
            requireSingle(entrustedMapper.insert(copy), REGISTRATION_CERTIFICATE_RENEWAL_BASE_CONFLICT);
        }

        requireSingle(certificateMapper.update(null,
                new LambdaUpdateWrapper<DccRegistrationCertificateDO>()
                        .eq(DccRegistrationCertificateDO::getId, certificate.getId())
                        .eq(DccRegistrationCertificateDO::getTenantId, command.tenantId())
                        .eq(DccRegistrationCertificateDO::getStatus, STATUS_ACTIVE)
                        .eq(DccRegistrationCertificateDO::getCurrentVersionId, currentVersion.getId())
                        .isNull(DccRegistrationCertificateDO::getPendingVersionId)
                        .eq(DccRegistrationCertificateDO::getRowVersion, command.expectedRowVersion())
                        .set(DccRegistrationCertificateDO::getPendingVersionId, renewalVersion.getId())
                        .setSql("row_version = row_version + 1")),
                REGISTRATION_CERTIFICATE_RENEWAL_PENDING_CONFLICT);

        requireSingle(fileMapper.update(null,
                new LambdaUpdateWrapper<DccRegistrationCertificateFileDO>()
                        .eq(DccRegistrationCertificateFileDO::getId, file.getId())
                        .eq(DccRegistrationCertificateFileDO::getTenantId, command.tenantId())
                        .eq(DccRegistrationCertificateFileDO::getOwnerType, FILE_OWNER_VERSION)
                        .eq(DccRegistrationCertificateFileDO::getOwnerId, currentVersion.getId())
                        .eq(DccRegistrationCertificateFileDO::getFileKind, FILE_KIND_REGISTRATION_CERTIFICATE)
                        .eq(DccRegistrationCertificateFileDO::getStatus, FILE_STATUS_STAGED)
                        .set(DccRegistrationCertificateFileDO::getOwnerId, renewalVersion.getId())
                        .set(DccRegistrationCertificateFileDO::getStatus, FILE_STATUS_BOUND)
                        .set(DccRegistrationCertificateFileDO::getBoundAt, businessClock.now())
                        .set(DccRegistrationCertificateFileDO::getBoundBy, command.actorId())),
                REGISTRATION_CERTIFICATE_FILE_NOT_STAGED);

        registerPlatformCandidate(command, certificate, currentVersion, renewalVersion);
        insertLifecycleEvent(command.tenantId(), certificate.getOwnerCompanyId(), certificate.getId(),
                currentVersion.getId(), renewalVersion.getId(), currentSnapshot.getId(), renewalSnapshot.getId(),
                command.idempotencyKey(), "RENEWAL_UPLOADED", command.expectedRowVersion(),
                currentSnapshot.getRevisionNo(), command.actorId(),
                new RenewalEventDetail(payloadHash, certificate.getId(), renewalVersion.getId(),
                        renewalSnapshot.getId(), file.getId(), false));
        return new DccRegistrationCertificateRenewalResult(certificate.getId(), renewalVersion.getId(),
                renewalSnapshot.getId(), file.getId(), STATUS_ACTIVE, STATUS_PENDING, false);
    }

    @Transactional(rollbackFor = Exception.class)
    public DccRegistrationCertificateRenewalResult voidPendingCandidate(
            Long tenantId, Long actorId, String idempotencyKey, String requestTraceId,
            Long certificateId, Integer expectedRowVersion, Long pendingVersionId, String voidReason) {
        validateEventInput(tenantId, actorId, idempotencyKey, requestTraceId);
        if (isBlank(voidReason)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_CANDIDATE_VOID_REASON_REQUIRED);
        }
        LifecycleEvent existing = findEvent(tenantId, idempotencyKey);
        if (existing != null) {
            RenewalEventDetail detail = parseDetail(existing);
            if (!Objects.equals("CANDIDATE_VOIDED", existing.eventType())) {
                throw new ServiceException(REGISTRATION_CERTIFICATE_IDEMPOTENCY_CONFLICT);
            }
            return new DccRegistrationCertificateRenewalResult(
                    detail.certificateId(), detail.targetVersionId(), detail.targetSnapshotId(),
                    detail.businessFileId(), STATUS_ACTIVE, STATUS_VOIDED, true);
        }
        DccRegistrationCertificateDO certificate = certificateMapper.selectById(certificateId);
        if (certificate == null || !Objects.equals(certificate.getTenantId(), tenantId)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_NOT_EXISTS);
        }
        if (!STATUS_ACTIVE.equals(certificate.getStatus())
                || !Objects.equals(certificate.getPendingVersionId(), pendingVersionId)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_RENEWAL_PENDING_CONFLICT);
        }
        DccRegistrationCertificateVersionDO pending = versionMapper.selectById(pendingVersionId);
        if (pending == null || !Objects.equals(pending.getTenantId(), tenantId)
                || !Objects.equals(pending.getCertificateId(), certificateId)
                || !VERSION_TYPE_RENEWAL.equals(pending.getVersionType())
                || !STATUS_PENDING.equals(pending.getStatus())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_RENEWAL_PENDING_CONFLICT);
        }
        List<DccRegistrationCertificateSnapshotDO> snapshots =
                snapshotMapper.selectListByVersionId(pendingVersionId);
        DccRegistrationCertificateSnapshotDO snapshot = snapshots.isEmpty() ? null : snapshots.get(snapshots.size() - 1);
        DccRegistrationCertificateFileDO file = selectRenewalFile(tenantId, pendingVersionId);

        requireSingle(versionMapper.update(null,
                new LambdaUpdateWrapper<DccRegistrationCertificateVersionDO>()
                        .eq(DccRegistrationCertificateVersionDO::getId, pendingVersionId)
                        .eq(DccRegistrationCertificateVersionDO::getTenantId, tenantId)
                        .eq(DccRegistrationCertificateVersionDO::getStatus, STATUS_PENDING)
                        .set(DccRegistrationCertificateVersionDO::getStatus, STATUS_VOIDED)
                        .set(DccRegistrationCertificateVersionDO::getVoidedAt, businessClock.now())
                        .set(DccRegistrationCertificateVersionDO::getVoidedBy, actorId)
                        .set(DccRegistrationCertificateVersionDO::getVoidReason, voidReason.trim())),
                REGISTRATION_CERTIFICATE_RENEWAL_PENDING_CONFLICT);
        requireSingle(certificateMapper.update(null,
                new LambdaUpdateWrapper<DccRegistrationCertificateDO>()
                        .eq(DccRegistrationCertificateDO::getId, certificateId)
                        .eq(DccRegistrationCertificateDO::getTenantId, tenantId)
                        .eq(DccRegistrationCertificateDO::getStatus, STATUS_ACTIVE)
                        .eq(DccRegistrationCertificateDO::getPendingVersionId, pendingVersionId)
                        .eq(DccRegistrationCertificateDO::getRowVersion, expectedRowVersion)
                        .set(DccRegistrationCertificateDO::getPendingVersionId, null)
                        .setSql("row_version = row_version + 1")),
                REGISTRATION_CERTIFICATE_REVISION_CONFLICT);
        if (file != null) {
            requireSingle(fileMapper.update(null,
                    new LambdaUpdateWrapper<DccRegistrationCertificateFileDO>()
                            .eq(DccRegistrationCertificateFileDO::getId, file.getId())
                            .eq(DccRegistrationCertificateFileDO::getTenantId, tenantId)
                            .eq(DccRegistrationCertificateFileDO::getOwnerId, pendingVersionId)
                            .eq(DccRegistrationCertificateFileDO::getStatus, FILE_STATUS_BOUND)
                            .set(DccRegistrationCertificateFileDO::getStatus, FILE_STATUS_VOIDED)),
                    REGISTRATION_CERTIFICATE_FILE_OWNER_CONFLICT);
        }
        insertLifecycleEvent(tenantId, certificate.getOwnerCompanyId(), certificateId,
                certificate.getCurrentVersionId(), pendingVersionId, certificate.getCurrentSnapshotId(),
                snapshot == null ? null : snapshot.getId(), idempotencyKey, "CANDIDATE_VOIDED",
                expectedRowVersion, snapshot == null ? null : snapshot.getRevisionNo(), actorId,
                new RenewalEventDetail(voidPayloadHash(certificateId, expectedRowVersion, pendingVersionId, voidReason),
                        certificateId, pendingVersionId, snapshot == null ? null : snapshot.getId(),
                        file == null ? null : file.getId(), true));
        return new DccRegistrationCertificateRenewalResult(certificateId, pendingVersionId,
                snapshot == null ? null : snapshot.getId(), file == null ? null : file.getId(),
                STATUS_ACTIVE, STATUS_VOIDED, true);
    }

    public boolean isRenewalUploadMissing(Long tenantId, Long certificateId) {
        DccRegistrationCertificateDO certificate = certificateMapper.selectById(certificateId);
        if (certificate == null || !Objects.equals(certificate.getTenantId(), tenantId)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_NOT_EXISTS);
        }
        return certificate.getPendingVersionId() == null;
    }

    private DccRegistrationCertificateDO requireActiveCertificate(DccRegistrationCertificateRenewalCommand command) {
        DccRegistrationCertificateDO certificate = certificateMapper.selectById(command.certificateId());
        if (certificate == null || !Objects.equals(certificate.getTenantId(), command.tenantId())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_NOT_EXISTS);
        }
        if (!STATUS_ACTIVE.equals(certificate.getStatus()) || certificate.getCurrentVersionId() == null) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_STATUS_INVALID);
        }
        if (certificate.getPendingVersionId() != null) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_RENEWAL_PENDING_CONFLICT);
        }
        if (!Objects.equals(certificate.getRowVersion(), command.expectedRowVersion())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_REVISION_CONFLICT);
        }
        return certificate;
    }

    private DccRegistrationCertificateVersionDO requireCurrentVersion(DccRegistrationCertificateDO certificate,
                                                                     Long expectedCurrentVersionId) {
        if (!Objects.equals(certificate.getCurrentVersionId(), expectedCurrentVersionId)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_RENEWAL_BASE_CONFLICT);
        }
        DccRegistrationCertificateVersionDO version = versionMapper.selectById(expectedCurrentVersionId);
        if (version == null || !Objects.equals(version.getTenantId(), certificate.getTenantId())
                || !Objects.equals(version.getCertificateId(), certificate.getId())
                || !STATUS_CURRENT.equals(version.getStatus())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_RENEWAL_BASE_CONFLICT);
        }
        return version;
    }

    private DccRegistrationCertificateSnapshotDO requireSnapshot(Long snapshotId, Long currentVersionId) {
        DccRegistrationCertificateSnapshotDO snapshot = snapshotMapper.selectById(snapshotId);
        if (snapshot == null || !Objects.equals(snapshot.getVersionId(), currentVersionId)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_RENEWAL_BASE_CONFLICT);
        }
        return snapshot;
    }

    private DccRegistrationCertificateFileDO requireStagedRenewalFile(Long tenantId, Long currentVersionId,
                                                                      Long businessFileId) {
        if (businessFileId == null || businessFileId <= 0) {
            List<DccRegistrationCertificateFileDO> candidates = fileMapper.selectList(
                    new LambdaQueryWrapperX<DccRegistrationCertificateFileDO>()
                            .eq(DccRegistrationCertificateFileDO::getTenantId, tenantId)
                            .eq(DccRegistrationCertificateFileDO::getOwnerType, FILE_OWNER_VERSION)
                            .eq(DccRegistrationCertificateFileDO::getOwnerId, currentVersionId)
                            .eq(DccRegistrationCertificateFileDO::getFileKind, FILE_KIND_REGISTRATION_CERTIFICATE)
                            .eq(DccRegistrationCertificateFileDO::getStatus, FILE_STATUS_STAGED));
            if (candidates.isEmpty()) {
                throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_REQUIRED);
            }
            if (candidates.size() > 1) {
                throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_OWNER_CONFLICT);
            }
            return candidates.get(0);
        }
        DccRegistrationCertificateFileDO file = fileMapper.selectById(businessFileId);
        if (file == null) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_REQUIRED);
        }
        if (!Objects.equals(file.getTenantId(), tenantId)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_TENANT_MISMATCH);
        }
        if (!FILE_OWNER_VERSION.equals(file.getOwnerType()) || !Objects.equals(file.getOwnerId(), currentVersionId)
                || !FILE_KIND_REGISTRATION_CERTIFICATE.equals(file.getFileKind())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_OWNER_CONFLICT);
        }
        if (!FILE_STATUS_STAGED.equals(file.getStatus())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_NOT_STAGED);
        }
        return file;
    }

    private void validateRenewalDates(LocalDate firstObtainedDate, LocalDate approvalDate,
                                      LocalDate effectiveDate, LocalDate expiryDate) {
        if (approvalDate == null || effectiveDate == null || expiryDate == null
                || firstObtainedDate != null && firstObtainedDate.isAfter(approvalDate)
                || approvalDate.isAfter(effectiveDate)
                || !effectiveDate.isBefore(expiryDate)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_RENEWAL_BASE_CONFLICT);
        }
        if (approvalDate.isAfter(businessClock.businessDate())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_RENEWAL_BASE_CONFLICT);
        }
    }

    private String resolveRenewalCertificateNo(DccRegistrationCertificateRenewalCommand command,
                                               DccRegistrationCertificateVersionDO currentVersion) {
        if (Boolean.TRUE.equals(command.categoryChanged())) {
            if (isBlank(command.certificateNo()) || isBlank(command.classification())) {
                throw new ServiceException(REGISTRATION_CERTIFICATE_RENEWAL_CATEGORY_CHANGE_REQUIRED);
            }
            return command.certificateNo().trim();
        }
        if (!isBlank(command.certificateNo())
                && !Objects.equals(command.certificateNo().trim(), currentVersion.getCertificateNo())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_RENEWAL_FIELD_FORBIDDEN);
        }
        return currentVersion.getCertificateNo();
    }

    private String resolveRenewalClassification(DccRegistrationCertificateRenewalCommand command,
                                                DccRegistrationCertificateVersionDO currentVersion) {
        if (Boolean.TRUE.equals(command.categoryChanged())) {
            if (isBlank(command.certificateNo()) || isBlank(command.classification())) {
                throw new ServiceException(REGISTRATION_CERTIFICATE_RENEWAL_CATEGORY_CHANGE_REQUIRED);
            }
            return command.classification().trim();
        }
        if (!isBlank(command.classification())
                && !Objects.equals(command.classification().trim(), currentVersion.getClassification())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_RENEWAL_FIELD_FORBIDDEN);
        }
        return currentVersion.getClassification();
    }

    private DccRegistrationCertificateSnapshotDO copySnapshot(DccRegistrationCertificateSnapshotDO source,
                                                              Long renewalVersionId) {
        DccRegistrationCertificateSnapshotDO copy = DccRegistrationCertificateSnapshotDO.builder()
                .versionId(renewalVersionId)
                .revisionNo(1)
                .sourceChangeId(null)
                .productName(source.getProductName())
                .registrantName(source.getRegistrantName())
                .modelSpecification(source.getModelSpecification())
                .structureComposition(source.getStructureComposition())
                .intendedUse(source.getIntendedUse())
                .technicalRequirements(source.getTechnicalRequirements())
                .residenceAddress(source.getResidenceAddress())
                .productionAddress(source.getProductionAddress())
                .entrustedProduction(source.getEntrustedProduction())
                .selfProduction(source.getSelfProduction())
                .entrustedEnterprisesJson(source.getEntrustedEnterprisesJson())
                .effectiveAt(source.getEffectiveAt())
                .build();
        copy.setTenantId(source.getTenantId());
        return copy;
    }

    private void registerPlatformCandidate(DccRegistrationCertificateRenewalCommand command,
                                           DccRegistrationCertificateDO certificate,
                                           DccRegistrationCertificateVersionDO currentVersion,
                                           DccRegistrationCertificateVersionDO renewalVersion) {
        ControlledContentKey key = ControlledContentKey.of(command.tenantId(), DCC_REGISTRATION_CERTIFICATE,
                String.valueOf(certificate.getId()));
        try {
            projectionService.registerReadyCandidate(key,
                    ControlledContentProjectionSnapshot.of(key, currentVersion.getId(), null),
                    ControlledContentProjectionSnapshot.of(key, currentVersion.getId(), renewalVersion.getId()),
                    certificate.getId(), renewalVersion.getId(), String.valueOf(renewalVersion.getVersionNo()),
                    STATUS_PENDING, command.actorId(), "Renewal candidate awaits effective date");
        } catch (RuntimeException exception) {
            ServiceException mapped = new ServiceException(REGISTRATION_CERTIFICATE_RENEWAL_BASE_CONFLICT);
            mapped.initCause(exception);
            throw mapped;
        }
    }

    private void insertLifecycleEvent(Long tenantId, Long ownerCompanyId, Long certificateId,
                                      Long sourceVersionId, Long targetVersionId, Long sourceSnapshotId,
                                      Long targetSnapshotId, String eventKey, String eventType,
                                      Integer baselineRowVersion, Integer baselineSnapshotRevision,
                                      Long actorId, RenewalEventDetail detail) {
        Integer nextSequence = jdbcTemplate.queryForObject("""
                SELECT COALESCE(MAX(event_sequence), 0) + 1
                  FROM dcc_registration_certificate_lifecycle_event
                 WHERE tenant_id = ? AND certificate_id = ?
                """, Integer.class, tenantId, certificateId);
        try {
            int affected = jdbcTemplate.update("""
                    INSERT INTO dcc_registration_certificate_lifecycle_event
                      (tenant_id, owner_company_id, certificate_id, source_version_id, target_version_id,
                       source_snapshot_id, target_snapshot_id, event_key, event_type, event_sequence,
                       baseline_row_version, baseline_snapshot_revision, actor_id, detail_json, occurred_at, creator)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, tenantId, ownerCompanyId, certificateId, sourceVersionId, targetVersionId,
                    sourceSnapshotId, targetSnapshotId, eventKey, eventType, nextSequence,
                    baselineRowVersion, baselineSnapshotRevision, actorId, JsonUtils.toJsonString(detail),
                    businessClock.now(), String.valueOf(actorId));
            requireSingle(affected, REGISTRATION_CERTIFICATE_LIFECYCLE_EVENT_CONFLICT);
        } catch (DuplicateKeyException exception) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_LIFECYCLE_EVENT_CONFLICT);
        }
    }

    private LifecycleEvent findEvent(Long tenantId, String eventKey) {
        List<LifecycleEvent> events = jdbcTemplate.query("""
                SELECT event_type, target_version_id, target_snapshot_id, detail_json
                  FROM dcc_registration_certificate_lifecycle_event
                 WHERE tenant_id = ? AND event_key = ?
                """, (rs, rowNum) -> new LifecycleEvent(
                rs.getString("event_type"),
                rs.getLong("target_version_id"),
                rs.getLong("target_snapshot_id"),
                rs.getString("detail_json")), tenantId, eventKey);
        return events.isEmpty() ? null : events.get(0);
    }

    private DccRegistrationCertificateRenewalResult replayUpload(
            DccRegistrationCertificateRenewalCommand command, String payloadHash, LifecycleEvent event) {
        RenewalEventDetail detail = parseDetail(event);
        if (!Objects.equals("RENEWAL_UPLOADED", event.eventType())
                || !Objects.equals(payloadHash, detail.payloadHash())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_IDEMPOTENCY_CONFLICT);
        }
        DccRegistrationCertificateDO certificate = certificateMapper.selectById(detail.certificateId());
        DccRegistrationCertificateVersionDO version = versionMapper.selectById(detail.targetVersionId());
        return new DccRegistrationCertificateRenewalResult(detail.certificateId(), detail.targetVersionId(),
                detail.targetSnapshotId(), detail.businessFileId(),
                certificate == null ? STATUS_ACTIVE : certificate.getStatus(),
                version == null ? STATUS_PENDING : version.getStatus(),
                false);
    }

    private RenewalEventDetail parseDetail(LifecycleEvent event) {
        return JsonUtils.parseObject(event.detailJson(), RenewalEventDetail.class);
    }

    private DccRegistrationCertificateFileDO selectRenewalFile(Long tenantId, Long pendingVersionId) {
        List<DccRegistrationCertificateFileDO> files = fileMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DccRegistrationCertificateFileDO>()
                        .eq(DccRegistrationCertificateFileDO::getTenantId, tenantId)
                        .eq(DccRegistrationCertificateFileDO::getOwnerType, FILE_OWNER_VERSION)
                        .eq(DccRegistrationCertificateFileDO::getOwnerId, pendingVersionId)
                        .eq(DccRegistrationCertificateFileDO::getFileKind, FILE_KIND_REGISTRATION_CERTIFICATE)
                        .eq(DccRegistrationCertificateFileDO::getStatus, FILE_STATUS_BOUND));
        if (files.size() > 1) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_OWNER_CONFLICT);
        }
        return files.isEmpty() ? null : files.get(0);
    }

    private void validateEventInput(Long tenantId, Long actorId, String eventKey, String requestTraceId) {
        if (tenantId == null || tenantId <= 0 || actorId == null || actorId <= 0) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_RENEWAL_BASE_CONFLICT);
        }
        if (isBlank(eventKey)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_IDEMPOTENCY_KEY_REQUIRED);
        }
        if (isBlank(requestTraceId)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_LIFECYCLE_EVENT_CONFLICT);
        }
    }

    private static String uploadPayloadHash(DccRegistrationCertificateRenewalCommand command) {
        return sha256("UPLOAD|" + command.certificateId() + "|" + command.expectedRowVersion()
                + "|" + command.currentVersionId() + "|" + command.businessFileId()
                + "|" + command.categoryChanged() + "|" + normalize(command.certificateNo())
                + "|" + normalize(command.classification()) + "|" + command.approvalDate()
                + "|" + command.effectiveDate() + "|" + command.expiryDate());
    }

    private static String voidPayloadHash(Long certificateId, Integer expectedRowVersion,
                                          Long pendingVersionId, String voidReason) {
        return sha256("VOID|" + certificateId + "|" + expectedRowVersion + "|" + pendingVersionId
                + "|" + normalize(voidReason));
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static void requireSingle(int affected, ErrorCode errorCode) {
        if (affected != 1) {
            throw new ServiceException(errorCode);
        }
    }

    private static <T> T require(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        return value;
    }

    private record LifecycleEvent(
            String eventType,
            Long targetVersionId,
            Long targetSnapshotId,
            String detailJson) {
    }

    private record RenewalEventDetail(
            String payloadHash,
            Long certificateId,
            Long targetVersionId,
            Long targetSnapshotId,
            Long businessFileId,
            Boolean renewalUploadMissing) {
    }
}
