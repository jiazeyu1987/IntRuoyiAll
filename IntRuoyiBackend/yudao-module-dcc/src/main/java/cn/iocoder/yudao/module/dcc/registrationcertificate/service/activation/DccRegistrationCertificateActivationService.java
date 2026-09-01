package cn.iocoder.yudao.module.dcc.registrationcertificate.service.activation;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateSnapshotDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateSnapshotEntrustedDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateVersionDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateSnapshotEntrustedMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateSnapshotMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateVersionMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateBusinessClock;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.notification.event.DccRegistrationCertificateBusinessEventNotifier;
import cn.iocoder.yudao.module.system.service.controlledcontent.ControlledContentKey;
import cn.iocoder.yudao.module.system.service.controlledcontent.ControlledContentProjectionSnapshot;
import cn.iocoder.yudao.module.system.service.controlledcontent.ControlledContentRegistrationProjectionService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_ACTIVATION_BASE_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_ACTIVATION_EVENT_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_ACTIVATION_REPLAY_INCOMPLETE;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentType.DCC_REGISTRATION_CERTIFICATE;

@Service
public class DccRegistrationCertificateActivationService {

    private static final String MASTER_STATUS_ACTIVE = "ACTIVE";
    private static final String VERSION_STATUS_CURRENT = "CURRENT";
    private static final String VERSION_STATUS_PENDING = "PENDING_EFFECTIVE";
    private static final String VERSION_STATUS_OLD = "OLD";
    private static final String EVENT_TYPE_RENEWAL_UPLOADED = "RENEWAL_UPLOADED";
    private static final String EVENT_TYPE_CHANGE_APPLIED = "CHANGE_APPLIED";
    private static final String EVENT_TYPE_ACTIVATION_APPLIED = "ACTIVATION_APPLIED";

    private final DccRegistrationCertificateMapper certificateMapper;
    private final DccRegistrationCertificateVersionMapper versionMapper;
    private final DccRegistrationCertificateSnapshotMapper snapshotMapper;
    private final DccRegistrationCertificateSnapshotEntrustedMapper entrustedMapper;
    private final JdbcTemplate jdbcTemplate;
    private final ControlledContentRegistrationProjectionService projectionService;
    private final DccRegistrationCertificateBusinessClock businessClock;
    private final DccRegistrationCertificateBusinessEventNotifier businessEventNotifier;

    public DccRegistrationCertificateActivationService(
            DccRegistrationCertificateMapper certificateMapper,
            DccRegistrationCertificateVersionMapper versionMapper,
            DccRegistrationCertificateSnapshotMapper snapshotMapper,
            DccRegistrationCertificateSnapshotEntrustedMapper entrustedMapper,
            JdbcTemplate jdbcTemplate,
            ControlledContentRegistrationProjectionService projectionService,
            DccRegistrationCertificateBusinessClock businessClock,
            DccRegistrationCertificateBusinessEventNotifier businessEventNotifier) {
        this.certificateMapper = require(certificateMapper, "certificateMapper");
        this.versionMapper = require(versionMapper, "versionMapper");
        this.snapshotMapper = require(snapshotMapper, "snapshotMapper");
        this.entrustedMapper = require(entrustedMapper, "entrustedMapper");
        this.jdbcTemplate = require(jdbcTemplate, "jdbcTemplate");
        this.projectionService = require(projectionService, "projectionService");
        this.businessClock = require(businessClock, "businessClock");
        this.businessEventNotifier = require(businessEventNotifier, "businessEventNotifier");
    }

    @Transactional(rollbackFor = Exception.class)
    public DccRegistrationCertificateActivationResult activateDueCandidate(
            DccRegistrationCertificateActivationCommand command) {
        validateCommand(command);
        ExistingActivation existing = findActivation(command.tenantId(), command.idempotencyKey());
        if (existing != null) {
            return existing.toResult();
        }

        DccRegistrationCertificateDO certificate = certificateMapper.selectById(command.certificateId());
        if (alreadyActivated(command, certificate)) {
            return activatedResult(command, certificate.getCurrentSnapshotId(), false);
        }
        requirePendingState(command, certificate);
        DccRegistrationCertificateVersionDO current = requireVersion(
                command.tenantId(), command.certificateId(), command.currentVersionId(), VERSION_STATUS_CURRENT);
        DccRegistrationCertificateVersionDO pending = requireVersion(
                command.tenantId(), command.certificateId(), command.pendingVersionId(), VERSION_STATUS_PENDING);
        if (pending.getEffectiveDate() == null || pending.getEffectiveDate().isAfter(businessClock.businessDate())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACTIVATION_BASE_CONFLICT);
        }
        DccRegistrationCertificateSnapshotDO activationSnapshot = buildActivationSnapshot(command, certificate);

        requireSingle(versionMapper.update(null,
                new LambdaUpdateWrapper<DccRegistrationCertificateVersionDO>()
                        .eq(DccRegistrationCertificateVersionDO::getId, current.getId())
                        .eq(DccRegistrationCertificateVersionDO::getTenantId, command.tenantId())
                        .eq(DccRegistrationCertificateVersionDO::getCertificateId, command.certificateId())
                        .eq(DccRegistrationCertificateVersionDO::getStatus, VERSION_STATUS_CURRENT)
                        .set(DccRegistrationCertificateVersionDO::getStatus, VERSION_STATUS_OLD)),
                REGISTRATION_CERTIFICATE_ACTIVATION_BASE_CONFLICT);
        requireSingle(versionMapper.update(null,
                new LambdaUpdateWrapper<DccRegistrationCertificateVersionDO>()
                        .eq(DccRegistrationCertificateVersionDO::getId, pending.getId())
                        .eq(DccRegistrationCertificateVersionDO::getTenantId, command.tenantId())
                        .eq(DccRegistrationCertificateVersionDO::getCertificateId, command.certificateId())
                        .eq(DccRegistrationCertificateVersionDO::getStatus, VERSION_STATUS_PENDING)
                        .set(DccRegistrationCertificateVersionDO::getStatus, VERSION_STATUS_CURRENT)),
                REGISTRATION_CERTIFICATE_ACTIVATION_BASE_CONFLICT);
        requireSingle(certificateMapper.update(null,
                new LambdaUpdateWrapper<DccRegistrationCertificateDO>()
                        .eq(DccRegistrationCertificateDO::getId, command.certificateId())
                        .eq(DccRegistrationCertificateDO::getTenantId, command.tenantId())
                        .eq(DccRegistrationCertificateDO::getStatus, MASTER_STATUS_ACTIVE)
                        .eq(DccRegistrationCertificateDO::getCurrentVersionId, command.currentVersionId())
                        .eq(DccRegistrationCertificateDO::getPendingVersionId, command.pendingVersionId())
                        .eq(DccRegistrationCertificateDO::getRowVersion, command.expectedRowVersion())
                        .set(DccRegistrationCertificateDO::getCurrentVersionId, command.pendingVersionId())
                        .set(DccRegistrationCertificateDO::getPendingVersionId, null)
                        .set(DccRegistrationCertificateDO::getCurrentSnapshotId, activationSnapshot.getId())
                        .setSql("row_version = row_version + 1")),
                REGISTRATION_CERTIFICATE_ACTIVATION_BASE_CONFLICT);

        publishProjection(command);
        Long activationEventId = insertActivationEvent(command, certificate, activationSnapshot);
        insertReplayRows(command, activationEventId);
        businessEventNotifier.notifyRenewalCandidateActivated(
                command.tenantId(), certificate.getOwnerCompanyId(), command.certificateId(), pending.getId(),
                command.actorId(), command.idempotencyKey(), activationSnapshot.getProductName(),
                pending.getCertificateNo(), pending.getEffectiveDate(), pending.getExpiryDate());
        return new DccRegistrationCertificateActivationResult(command.certificateId(), command.currentVersionId(),
                command.pendingVersionId(), activationSnapshot.getId(), true);
    }

    private DccRegistrationCertificateSnapshotDO buildActivationSnapshot(
            DccRegistrationCertificateActivationCommand command,
            DccRegistrationCertificateDO certificate) {
        List<LifecycleRow> rows = lifecycleRows(command.tenantId(), command.certificateId());
        LifecycleRow latestChange = validateReplayRows(command, rows);
        if (latestChange == null) {
            return requireSnapshot(command.pendingVersionId(), REGISTRATION_CERTIFICATE_ACTIVATION_BASE_CONFLICT);
        }
        DccRegistrationCertificateSnapshotDO source = snapshotMapper.selectById(latestChange.targetSnapshotId());
        if (source == null || !Objects.equals(source.getTenantId(), command.tenantId())
                || !Objects.equals(source.getVersionId(), command.currentVersionId())
                || !Objects.equals(source.getRevisionNo(), latestChange.baselineSnapshotRevision())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACTIVATION_REPLAY_INCOMPLETE);
        }
        DccRegistrationCertificateSnapshotDO copy = copySnapshot(source, command.pendingVersionId());
        copy.setSourceChangeId(latestChange.id());
        copy.setEffectiveAt(businessClock.now());
        requireSingle(snapshotMapper.insert(copy), REGISTRATION_CERTIFICATE_ACTIVATION_REPLAY_INCOMPLETE);
        for (DccRegistrationCertificateSnapshotEntrustedDO row : entrustedMapper.selectListBySnapshotId(source.getId())) {
            DccRegistrationCertificateSnapshotEntrustedDO entrusted =
                    DccRegistrationCertificateSnapshotEntrustedDO.builder()
                            .snapshotId(copy.getId())
                            .enterpriseId(row.getEnterpriseId())
                            .enterpriseNameSnapshot(row.getEnterpriseNameSnapshot())
                            .sortOrder(row.getSortOrder())
                            .build();
            entrusted.setTenantId(command.tenantId());
            requireSingle(entrustedMapper.insert(entrusted), REGISTRATION_CERTIFICATE_ACTIVATION_REPLAY_INCOMPLETE);
        }
        return copy;
    }

    private LifecycleRow validateReplayRows(DccRegistrationCertificateActivationCommand command,
                                            List<LifecycleRow> rows) {
        if (rows.isEmpty()) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACTIVATION_REPLAY_INCOMPLETE);
        }
        LifecycleRow latestChange = null;
        int expectedSequence = 1;
        for (LifecycleRow row : rows) {
            if (row.eventSequence() != expectedSequence) {
                throw new ServiceException(REGISTRATION_CERTIFICATE_ACTIVATION_REPLAY_INCOMPLETE);
            }
            if (expectedSequence == 1 && !EVENT_TYPE_RENEWAL_UPLOADED.equals(row.eventType())) {
                throw new ServiceException(REGISTRATION_CERTIFICATE_ACTIVATION_REPLAY_INCOMPLETE);
            }
            if (EVENT_TYPE_CHANGE_APPLIED.equals(row.eventType())) {
                if (!Objects.equals(row.sourceVersionId(), command.currentVersionId())
                        || row.targetSnapshotId() == null
                        || row.baselineSnapshotRevision() == null) {
                    throw new ServiceException(REGISTRATION_CERTIFICATE_ACTIVATION_REPLAY_INCOMPLETE);
                }
                latestChange = row;
            }
            expectedSequence++;
        }
        return latestChange;
    }

    private List<LifecycleRow> lifecycleRows(Long tenantId, Long certificateId) {
        return jdbcTemplate.query("""
                SELECT id, source_version_id, target_version_id, source_snapshot_id, target_snapshot_id,
                       event_type, event_sequence, baseline_row_version, baseline_snapshot_revision
                  FROM dcc_registration_certificate_lifecycle_event
                 WHERE tenant_id = ? AND certificate_id = ?
                 ORDER BY event_sequence ASC, id ASC
                """, (rs, rowNum) -> new LifecycleRow(
                rs.getLong("id"),
                rs.getObject("source_version_id", Long.class),
                rs.getObject("target_version_id", Long.class),
                rs.getObject("source_snapshot_id", Long.class),
                rs.getObject("target_snapshot_id", Long.class),
                rs.getString("event_type"),
                rs.getInt("event_sequence"),
                rs.getObject("baseline_row_version", Integer.class),
                rs.getObject("baseline_snapshot_revision", Integer.class)), tenantId, certificateId);
    }

    private void publishProjection(DccRegistrationCertificateActivationCommand command) {
        ControlledContentKey key = ControlledContentKey.of(command.tenantId(), DCC_REGISTRATION_CERTIFICATE,
                String.valueOf(command.certificateId()));
        try {
            projectionService.publish(key,
                    ControlledContentProjectionSnapshot.of(key, command.currentVersionId(), command.pendingVersionId()),
                    ControlledContentProjectionSnapshot.of(key, command.pendingVersionId(), null),
                    VERSION_STATUS_OLD, VERSION_STATUS_CURRENT, command.actorId(),
                    "注册证候选版本已到达生效日期");
        } catch (RuntimeException exception) {
            ServiceException mapped = new ServiceException(REGISTRATION_CERTIFICATE_ACTIVATION_BASE_CONFLICT);
            mapped.initCause(exception);
            throw mapped;
        }
    }

    private Long insertActivationEvent(DccRegistrationCertificateActivationCommand command,
                                       DccRegistrationCertificateDO certificate,
                                       DccRegistrationCertificateSnapshotDO activationSnapshot) {
        Integer nextSequence = jdbcTemplate.queryForObject("""
                SELECT COALESCE(MAX(event_sequence), 0) + 1
                  FROM dcc_registration_certificate_lifecycle_event
                 WHERE tenant_id = ? AND certificate_id = ?
                """, Integer.class, command.tenantId(), command.certificateId());
        try {
            int affected = jdbcTemplate.update("""
                    INSERT INTO dcc_registration_certificate_lifecycle_event
                      (tenant_id, owner_company_id, certificate_id, source_version_id, target_version_id,
                       source_snapshot_id, target_snapshot_id, event_key, event_type, event_sequence,
                       baseline_row_version, baseline_snapshot_revision, actor_id, detail_json, occurred_at, creator)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, command.tenantId(), certificate.getOwnerCompanyId(), command.certificateId(),
                    command.currentVersionId(), command.pendingVersionId(), certificate.getCurrentSnapshotId(),
                    activationSnapshot.getId(), command.idempotencyKey(), EVENT_TYPE_ACTIVATION_APPLIED,
                    nextSequence, command.expectedRowVersion(), activationSnapshot.getRevisionNo(),
                    command.actorId(), JsonUtils.toJsonString(new ActivationDetail(command.certificateId(),
                            command.currentVersionId(), command.pendingVersionId(), activationSnapshot.getId())),
                    businessClock.now(), String.valueOf(command.actorId()));
            requireSingle(affected, REGISTRATION_CERTIFICATE_ACTIVATION_EVENT_CONFLICT);
        } catch (DuplicateKeyException exception) {
            ExistingActivation existing = findActivation(command.tenantId(), command.idempotencyKey());
            if (existing != null) {
                return existing.activationEventId();
            }
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACTIVATION_EVENT_CONFLICT);
        }
        Long id = jdbcTemplate.queryForObject("""
                SELECT id
                  FROM dcc_registration_certificate_lifecycle_event
                 WHERE tenant_id = ? AND event_key = ? AND event_type = ?
                """, Long.class, command.tenantId(), command.idempotencyKey(), EVENT_TYPE_ACTIVATION_APPLIED);
        if (id == null) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACTIVATION_EVENT_CONFLICT);
        }
        return id;
    }

    private void insertReplayRows(DccRegistrationCertificateActivationCommand command, Long activationEventId) {
        List<LifecycleRow> sourceRows = lifecycleRows(command.tenantId(), command.certificateId()).stream()
                .filter(row -> EVENT_TYPE_CHANGE_APPLIED.equals(row.eventType()))
                .toList();
        int appliedSequence = 1;
        for (LifecycleRow row : sourceRows) {
            try {
                requireSingle(jdbcTemplate.update("""
                        INSERT INTO dcc_registration_certificate_activation_replay
                          (tenant_id, activation_event_id, source_event_id, certificate_id, source_sequence,
                           applied_sequence, replay_result, detail_json, creator)
                        VALUES (?, ?, ?, ?, ?, ?, 'APPLIED', ?, ?)
                        """, command.tenantId(), activationEventId, row.id(), command.certificateId(),
                        row.eventSequence(), appliedSequence++,
                        JsonUtils.toJsonString(new ReplayDetail(row.id(), row.targetSnapshotId())),
                        String.valueOf(command.actorId())),
                        REGISTRATION_CERTIFICATE_ACTIVATION_EVENT_CONFLICT);
            } catch (DuplicateKeyException exception) {
                throw new ServiceException(REGISTRATION_CERTIFICATE_ACTIVATION_EVENT_CONFLICT);
            }
        }
    }

    private ExistingActivation findActivation(Long tenantId, String eventKey) {
        if (isBlank(eventKey)) {
            return null;
        }
        List<ExistingActivation> rows = jdbcTemplate.query("""
                SELECT id, certificate_id, source_version_id, target_version_id, target_snapshot_id
                  FROM dcc_registration_certificate_lifecycle_event
                 WHERE tenant_id = ? AND event_key = ? AND event_type = ?
                """, (rs, rowNum) -> new ExistingActivation(
                rs.getLong("id"),
                rs.getLong("certificate_id"),
                rs.getLong("source_version_id"),
                rs.getLong("target_version_id"),
                rs.getLong("target_snapshot_id")), tenantId, eventKey, EVENT_TYPE_ACTIVATION_APPLIED);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private boolean alreadyActivated(DccRegistrationCertificateActivationCommand command,
                                     DccRegistrationCertificateDO certificate) {
        if (certificate == null || !Objects.equals(certificate.getTenantId(), command.tenantId())) {
            return false;
        }
        DccRegistrationCertificateVersionDO old = versionMapper.selectById(command.currentVersionId());
        DccRegistrationCertificateVersionDO current = versionMapper.selectById(command.pendingVersionId());
        return MASTER_STATUS_ACTIVE.equals(certificate.getStatus())
                && Objects.equals(certificate.getCurrentVersionId(), command.pendingVersionId())
                && certificate.getPendingVersionId() == null
                && old != null && Objects.equals(old.getTenantId(), command.tenantId())
                && VERSION_STATUS_OLD.equals(old.getStatus())
                && current != null && Objects.equals(current.getTenantId(), command.tenantId())
                && VERSION_STATUS_CURRENT.equals(current.getStatus());
    }

    private DccRegistrationCertificateActivationResult activatedResult(
            DccRegistrationCertificateActivationCommand command, Long snapshotId, boolean activated) {
        return new DccRegistrationCertificateActivationResult(command.certificateId(), command.currentVersionId(),
                command.pendingVersionId(), snapshotId, activated);
    }

    private void requirePendingState(DccRegistrationCertificateActivationCommand command,
                                     DccRegistrationCertificateDO certificate) {
        if (certificate == null || !Objects.equals(certificate.getTenantId(), command.tenantId())
                || !Objects.equals(certificate.getId(), command.certificateId())
                || !MASTER_STATUS_ACTIVE.equals(certificate.getStatus())
                || !Objects.equals(certificate.getCurrentVersionId(), command.currentVersionId())
                || !Objects.equals(certificate.getPendingVersionId(), command.pendingVersionId())
                || !Objects.equals(certificate.getRowVersion(), command.expectedRowVersion())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACTIVATION_BASE_CONFLICT);
        }
    }

    private DccRegistrationCertificateVersionDO requireVersion(
            Long tenantId, Long certificateId, Long versionId, String status) {
        DccRegistrationCertificateVersionDO version = versionMapper.selectById(versionId);
        if (version == null || !Objects.equals(version.getTenantId(), tenantId)
                || !Objects.equals(version.getCertificateId(), certificateId)
                || !status.equals(version.getStatus())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACTIVATION_BASE_CONFLICT);
        }
        return version;
    }

    private DccRegistrationCertificateSnapshotDO requireSnapshot(Long versionId, ErrorCode errorCode) {
        List<DccRegistrationCertificateSnapshotDO> snapshots = snapshotMapper.selectListByVersionId(versionId);
        if (snapshots == null || snapshots.isEmpty()) {
            throw new ServiceException(errorCode);
        }
        return snapshots.get(snapshots.size() - 1);
    }

    private DccRegistrationCertificateSnapshotDO copySnapshot(DccRegistrationCertificateSnapshotDO source,
                                                              Long pendingVersionId) {
        DccRegistrationCertificateSnapshotDO copy = DccRegistrationCertificateSnapshotDO.builder()
                .versionId(pendingVersionId)
                .revisionNo(source.getRevisionNo())
                .sourceChangeId(source.getSourceChangeId())
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

    private void validateCommand(DccRegistrationCertificateActivationCommand command) {
        if (command == null || command.tenantId() == null || command.tenantId() <= 0
                || command.actorId() == null || command.actorId() <= 0
                || command.certificateId() == null || command.certificateId() <= 0
                || command.expectedRowVersion() == null || command.expectedRowVersion() <= 0
                || command.currentVersionId() == null || command.currentVersionId() <= 0
                || command.pendingVersionId() == null || command.pendingVersionId() <= 0
                || isBlank(command.idempotencyKey()) || isBlank(command.requestTraceId())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACTIVATION_BASE_CONFLICT);
        }
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
            throw new IllegalArgumentException(name + " 不能为空");
        }
        return value;
    }

    private record LifecycleRow(Long id, Long sourceVersionId, Long targetVersionId,
                                Long sourceSnapshotId, Long targetSnapshotId, String eventType,
                                int eventSequence, Integer baselineRowVersion,
                                Integer baselineSnapshotRevision) {
    }

    private record ExistingActivation(Long activationEventId, Long certificateId, Long oldVersionId,
                                      Long currentVersionId, Long currentSnapshotId) {
        DccRegistrationCertificateActivationResult toResult() {
            return new DccRegistrationCertificateActivationResult(certificateId, oldVersionId, currentVersionId,
                    currentSnapshotId, false);
        }
    }

    private record ActivationDetail(Long certificateId, Long oldVersionId, Long currentVersionId,
                                    Long currentSnapshotId) {
    }

    private record ReplayDetail(Long sourceEventId, Long targetSnapshotId) {
    }
}
