package cn.iocoder.yudao.module.dcc.registrationcertificate.service.supportingdocument;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateBusinessClock;
import cn.iocoder.yudao.module.mdm.api.companyscope.MdmCompanyScopeApi;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_ACTIVATION_EVENT_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_IDEMPOTENCY_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_IDEMPOTENCY_KEY_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_LIFECYCLE_EVENT_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_SUPPORTING_DOCUMENT_REJECT_REASON_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_SUPPORTING_DOCUMENT_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_SUPPORTING_DOCUMENT_STATUS_INVALID;

@Service
public class DccRegistrationCertificateSupportingDocumentService {

    private static final String STATUS_PENDING = "PENDING_CONFIRMATION";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String EVENT_UPLOADED = "SUPPORTING_DOCUMENT_UPLOADED";
    private static final String EVENT_CONFIRMED = "SUPPORTING_DOCUMENT_CONFIRMED";
    private static final String EVENT_REJECTED = "SUPPORTING_DOCUMENT_REJECTED";

    private final JdbcTemplate jdbcTemplate;
    private final MdmCompanyScopeApi companyScopeApi;
    private final DccRegistrationCertificateBusinessClock businessClock;

    public DccRegistrationCertificateSupportingDocumentService(
            JdbcTemplate jdbcTemplate,
            MdmCompanyScopeApi companyScopeApi,
            DccRegistrationCertificateBusinessClock businessClock) {
        this.jdbcTemplate = require(jdbcTemplate, "jdbcTemplate");
        this.companyScopeApi = require(companyScopeApi, "companyScopeApi");
        this.businessClock = require(businessClock, "businessClock");
    }

    @Transactional(rollbackFor = Exception.class)
    public DccRegistrationCertificateSupportingDocumentResult upload(
            DccRegistrationCertificateSupportingDocumentCommand command) {
        validateCommon(command);
        validateDocumentType(command.documentType());
        ExistingEvent existing = findEvent(command.tenantId(), command.idempotencyKey());
        if (existing != null) {
            return replay(command, existing, EVENT_UPLOADED);
        }
        CertificateRef certificate = requireCertificate(command);
        companyScopeApi.validateUserCompanyAccess(command.actorId(), certificate.ownerCompanyId());
        Long supportId = insertPendingDocument(command, certificate);
        insertLifecycleEvent(command, certificate, supportId, EVENT_UPLOADED, null);
        return new DccRegistrationCertificateSupportingDocumentResult(supportId, command.certificateId(),
                command.versionId(), command.documentType().trim(), STATUS_PENDING, true);
    }

    @Transactional(rollbackFor = Exception.class)
    public DccRegistrationCertificateSupportingDocumentResult confirm(
            DccRegistrationCertificateSupportingDocumentCommand command) {
        validateReview(command);
        ExistingEvent existing = findEvent(command.tenantId(), command.idempotencyKey());
        if (existing != null) {
            return replay(command, existing, EVENT_CONFIRMED);
        }
        SupportingDocument document = requirePendingDocument(command);
        companyScopeApi.validateUserCompanyAccess(command.actorId(), document.ownerCompanyId());
        requireSingle(jdbcTemplate.update("""
                UPDATE dcc_registration_certificate_supporting_document
                   SET status = ?, open_unique_flag = NULL, row_version = row_version + 1,
                       confirmed_at = ?, confirmed_by = ?, updater = ?, update_time = ?
                 WHERE id = ? AND tenant_id = ? AND status = ? AND row_version = ? AND deleted = 0
                """, STATUS_CONFIRMED, businessClock.now(), command.actorId(), String.valueOf(command.actorId()),
                businessClock.now(), document.id(), command.tenantId(), STATUS_PENDING, command.expectedRowVersion()),
                REGISTRATION_CERTIFICATE_SUPPORTING_DOCUMENT_STATUS_INVALID);
        insertLifecycleEvent(command, document.toCertificateRef(), document.id(), EVENT_CONFIRMED, null);
        return new DccRegistrationCertificateSupportingDocumentResult(document.id(), document.certificateId(),
                document.versionId(), document.documentType(), STATUS_CONFIRMED, false);
    }

    @Transactional(rollbackFor = Exception.class)
    public DccRegistrationCertificateSupportingDocumentResult reject(
            DccRegistrationCertificateSupportingDocumentCommand command) {
        validateReview(command);
        if (isBlank(command.rejectReason())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_SUPPORTING_DOCUMENT_REJECT_REASON_REQUIRED);
        }
        ExistingEvent existing = findEvent(command.tenantId(), command.idempotencyKey());
        if (existing != null) {
            return replay(command, existing, EVENT_REJECTED);
        }
        SupportingDocument document = requirePendingDocument(command);
        companyScopeApi.validateUserCompanyAccess(command.actorId(), document.ownerCompanyId());
        String reason = command.rejectReason().trim();
        requireSingle(jdbcTemplate.update("""
                UPDATE dcc_registration_certificate_supporting_document
                   SET status = ?, open_unique_flag = 1, row_version = row_version + 1,
                       rejected_at = ?, rejected_by = ?, reject_reason = ?, updater = ?, update_time = ?
                 WHERE id = ? AND tenant_id = ? AND status = ? AND row_version = ? AND deleted = 0
                """, STATUS_REJECTED, businessClock.now(), command.actorId(), reason,
                String.valueOf(command.actorId()), businessClock.now(), document.id(), command.tenantId(),
                STATUS_PENDING, command.expectedRowVersion()),
                REGISTRATION_CERTIFICATE_SUPPORTING_DOCUMENT_STATUS_INVALID);
        insertLifecycleEvent(command, document.toCertificateRef(), document.id(), EVENT_REJECTED, reason);
        return new DccRegistrationCertificateSupportingDocumentResult(document.id(), document.certificateId(),
                document.versionId(), document.documentType(), STATUS_REJECTED, true);
    }

    private Long insertPendingDocument(DccRegistrationCertificateSupportingDocumentCommand command,
                                       CertificateRef certificate) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            int affected = jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement("""
                        INSERT INTO dcc_registration_certificate_supporting_document
                          (tenant_id, owner_company_id, certificate_id, version_id, business_file_id,
                           document_type, status, open_unique_flag, row_version, uploaded_at, uploaded_by, creator)
                        VALUES (?, ?, ?, ?, ?, ?, ?, 1, 1, ?, ?, ?)
                        """, Statement.RETURN_GENERATED_KEYS);
                ps.setLong(1, command.tenantId());
                ps.setLong(2, certificate.ownerCompanyId());
                ps.setLong(3, command.certificateId());
                ps.setLong(4, command.versionId());
                ps.setLong(5, command.businessFileId());
                ps.setString(6, command.documentType().trim());
                ps.setString(7, STATUS_PENDING);
                ps.setObject(8, businessClock.now());
                ps.setLong(9, command.actorId());
                ps.setString(10, String.valueOf(command.actorId()));
                return ps;
            }, keyHolder);
            requireSingle(affected, REGISTRATION_CERTIFICATE_SUPPORTING_DOCUMENT_STATUS_INVALID);
        } catch (DuplicateKeyException exception) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_SUPPORTING_DOCUMENT_STATUS_INVALID);
        }
        Number key = keyHolder.getKeyList().isEmpty() ? null
                : (Number) keyHolder.getKeyList().get(0).get("id");
        if (key == null) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_SUPPORTING_DOCUMENT_STATUS_INVALID);
        }
        return key.longValue();
    }

    private CertificateRef requireCertificate(DccRegistrationCertificateSupportingDocumentCommand command) {
        List<CertificateRef> rows = jdbcTemplate.query("""
                SELECT c.owner_company_id, c.current_version_id, v.status AS version_status
                  FROM dcc_registration_certificate c
                  JOIN dcc_registration_certificate_version v
                    ON v.id = ? AND v.tenant_id = c.tenant_id AND v.certificate_id = c.id
                 WHERE c.id = ? AND c.tenant_id = ? AND c.status = 'ACTIVE' AND c.deleted = 0
                """, (rs, rowNum) -> new CertificateRef(
                rs.getLong("owner_company_id"),
                rs.getLong("current_version_id"),
                rs.getString("version_status")), command.versionId(), command.certificateId(), command.tenantId());
        if (rows.size() != 1 || !Objects.equals(rows.get(0).currentVersionId(), command.versionId())
                || !"CURRENT".equals(rows.get(0).versionStatus())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_SUPPORTING_DOCUMENT_REQUIRED);
        }
        return rows.get(0);
    }

    private SupportingDocument requirePendingDocument(DccRegistrationCertificateSupportingDocumentCommand command) {
        List<SupportingDocument> rows = jdbcTemplate.query("""
                SELECT id, owner_company_id, certificate_id, version_id, business_file_id,
                       document_type, status, row_version
                  FROM dcc_registration_certificate_supporting_document
                 WHERE id = ? AND tenant_id = ? AND deleted = 0
                """, (rs, rowNum) -> new SupportingDocument(
                rs.getLong("id"),
                rs.getLong("owner_company_id"),
                rs.getLong("certificate_id"),
                rs.getLong("version_id"),
                rs.getLong("business_file_id"),
                rs.getString("document_type"),
                rs.getString("status"),
                rs.getInt("row_version")), command.supportingDocumentId(), command.tenantId());
        if (rows.size() != 1) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_SUPPORTING_DOCUMENT_REQUIRED);
        }
        SupportingDocument document = rows.get(0);
        if (!Objects.equals(document.certificateId(), command.certificateId())
                || !Objects.equals(document.versionId(), command.versionId())
                || !Objects.equals(document.businessFileId(), command.businessFileId())
                || !Objects.equals(document.documentType(), command.documentType().trim())
                || !STATUS_PENDING.equals(document.status())
                || !Objects.equals(document.rowVersion(), command.expectedRowVersion())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_SUPPORTING_DOCUMENT_STATUS_INVALID);
        }
        return document;
    }

    private void insertLifecycleEvent(DccRegistrationCertificateSupportingDocumentCommand command,
                                      CertificateRef certificate, Long supportId,
                                      String eventType, String rejectReason) {
        Integer nextSequence = jdbcTemplate.queryForObject("""
                SELECT COALESCE(MAX(event_sequence), 0) + 1
                  FROM dcc_registration_certificate_lifecycle_event
                 WHERE tenant_id = ? AND certificate_id = ?
                """, Integer.class, command.tenantId(), command.certificateId());
        try {
            requireSingle(jdbcTemplate.update("""
                    INSERT INTO dcc_registration_certificate_lifecycle_event
                      (tenant_id, owner_company_id, certificate_id, source_version_id, target_version_id,
                       source_snapshot_id, target_snapshot_id, event_key, event_type, event_sequence,
                       baseline_row_version, baseline_snapshot_revision, actor_id, detail_json, occurred_at, creator)
                    VALUES (?, ?, ?, ?, ?, NULL, NULL, ?, ?, ?, NULL, NULL, ?, ?, ?, ?)
                    """, command.tenantId(), certificate.ownerCompanyId(), command.certificateId(),
                    command.versionId(), command.versionId(), command.idempotencyKey(), eventType, nextSequence,
                    command.actorId(), JsonUtils.toJsonString(new EventDetail(supportId, command.documentType().trim(),
                            rejectReason)), businessClock.now(), String.valueOf(command.actorId())),
                    REGISTRATION_CERTIFICATE_LIFECYCLE_EVENT_CONFLICT);
        } catch (DuplicateKeyException exception) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_LIFECYCLE_EVENT_CONFLICT);
        }
    }

    private ExistingEvent findEvent(Long tenantId, String eventKey) {
        if (isBlank(eventKey)) {
            return null;
        }
        List<ExistingEvent> rows = jdbcTemplate.query("""
                SELECT event_type, certificate_id, target_version_id, detail_json
                  FROM dcc_registration_certificate_lifecycle_event
                 WHERE tenant_id = ? AND event_key = ?
                """, (rs, rowNum) -> new ExistingEvent(
                rs.getString("event_type"),
                rs.getLong("certificate_id"),
                rs.getLong("target_version_id"),
                rs.getString("detail_json")), tenantId, eventKey);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private DccRegistrationCertificateSupportingDocumentResult replay(
            DccRegistrationCertificateSupportingDocumentCommand command, ExistingEvent event, String expectedType) {
        if (!expectedType.equals(event.eventType())
                || !Objects.equals(event.certificateId(), command.certificateId())
                || !Objects.equals(event.versionId(), command.versionId())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_IDEMPOTENCY_CONFLICT);
        }
        EventDetail detail = JsonUtils.parseObject(event.detailJson(), EventDetail.class);
        if (detail == null || !Objects.equals(detail.documentType(), command.documentType().trim())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_IDEMPOTENCY_CONFLICT);
        }
        String status = switch (expectedType) {
            case EVENT_UPLOADED -> STATUS_PENDING;
            case EVENT_CONFIRMED -> STATUS_CONFIRMED;
            case EVENT_REJECTED -> STATUS_REJECTED;
            default -> throw new ServiceException(REGISTRATION_CERTIFICATE_ACTIVATION_EVENT_CONFLICT);
        };
        return new DccRegistrationCertificateSupportingDocumentResult(detail.supportingDocumentId(),
                event.certificateId(), event.versionId(), detail.documentType(), status,
                !STATUS_CONFIRMED.equals(status));
    }

    private void validateCommon(DccRegistrationCertificateSupportingDocumentCommand command) {
        if (command == null || command.tenantId() == null || command.tenantId() <= 0
                || command.actorId() == null || command.actorId() <= 0
                || command.certificateId() == null || command.certificateId() <= 0
                || command.versionId() == null || command.versionId() <= 0
                || command.businessFileId() == null || command.businessFileId() <= 0) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_SUPPORTING_DOCUMENT_REQUIRED);
        }
        if (isBlank(command.idempotencyKey())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_IDEMPOTENCY_KEY_REQUIRED);
        }
    }

    private void validateReview(DccRegistrationCertificateSupportingDocumentCommand command) {
        validateCommon(command);
        if (command.supportingDocumentId() == null || command.supportingDocumentId() <= 0
                || command.expectedRowVersion() == null || command.expectedRowVersion() <= 0) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_SUPPORTING_DOCUMENT_REQUIRED);
        }
        validateDocumentType(command.documentType());
    }

    private void validateDocumentType(String documentType) {
        if (!"RENEWAL_ACCEPTANCE_RECEIPT".equals(trim(documentType))
                && !"RENEWAL_SUPPLEMENT_NOTICE".equals(trim(documentType))) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_SUPPORTING_DOCUMENT_REQUIRED);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
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

    private record CertificateRef(Long ownerCompanyId, Long currentVersionId, String versionStatus) {
    }

    private record SupportingDocument(Long id, Long ownerCompanyId, Long certificateId, Long versionId,
                                      Long businessFileId, String documentType, String status, Integer rowVersion) {
        CertificateRef toCertificateRef() {
            return new CertificateRef(ownerCompanyId, versionId, "CURRENT");
        }
    }

    private record ExistingEvent(String eventType, Long certificateId, Long versionId, String detailJson) {
    }

    private record EventDetail(Long supportingDocumentId, String documentType, String rejectReason) {
    }
}
