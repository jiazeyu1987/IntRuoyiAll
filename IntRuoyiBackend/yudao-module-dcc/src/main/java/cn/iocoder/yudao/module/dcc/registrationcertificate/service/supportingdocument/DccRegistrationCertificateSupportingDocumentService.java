package cn.iocoder.yudao.module.dcc.registrationcertificate.service.supportingdocument;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateBusinessClock;
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
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_FILE_NOT_STAGED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_FILE_OWNER_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_FILE_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_IDEMPOTENCY_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_IDEMPOTENCY_KEY_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_LIFECYCLE_EVENT_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_SUPPORTING_DOCUMENT_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_SUPPORTING_DOCUMENT_STATUS_INVALID;

@Service
public class DccRegistrationCertificateSupportingDocumentService {

    private static final String STATUS_EFFECTIVE = "EFFECTIVE";
    private static final String EVENT_EFFECTIVE = "SUPPORTING_DOCUMENT_EFFECTIVE";

    private final JdbcTemplate jdbcTemplate;
    private final DccRegistrationCertificateBusinessClock businessClock;

    public DccRegistrationCertificateSupportingDocumentService(
            JdbcTemplate jdbcTemplate,
            DccRegistrationCertificateBusinessClock businessClock) {
        this.jdbcTemplate = require(jdbcTemplate, "jdbcTemplate");
        this.businessClock = require(businessClock, "businessClock");
    }

    @jakarta.annotation.Resource
    private cn.iocoder.yudao.module.infra.service.file.FileService fileService;
    @jakarta.annotation.Resource
    private cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateFileMapper registrationCertificateFileMapper;

    @Transactional(rollbackFor = Exception.class)
    public DccRegistrationCertificateSupportingDocumentResult uploadFile(Long tenantId, Long actorId,
            String key, Long certificateId, Long versionId, String documentType,
            org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {
        var check = new DccRegistrationCertificateSupportingDocumentCommand(tenantId, actorId, key, null,
                certificateId, versionId, 1L, null, null, documentType, null);
        validateCommon(check);
        validateDocumentType(documentType);
        requireCertificate(check);
        if (file == null || file.isEmpty() || file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_REQUIRED);
        }
        String name = file.getOriginalFilename();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || !name.substring(dot).toLowerCase(java.util.Locale.ROOT).matches("\\.[a-z0-9]{1,12}")) {
            throw cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException(
                    "支持材料文件必须具有有效扩展名（1至12位英文字母或数字）");
        }
        byte[] content = file.getBytes();
        if (content.length == 0) throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_REQUIRED);
        cn.iocoder.yudao.module.dcc.registrationcertificate.service.file.DccRegistrationCertificateMaterialValidator.validate(name, content);
        ExistingEvent existing = findEvent(tenantId, key);
        if (existing != null) {
            EventDetail detail = JsonUtils.parseObject(existing.detailJson(), EventDetail.class);
            if (detail == null) throw new ServiceException(REGISTRATION_CERTIFICATE_IDEMPOTENCY_CONFLICT);
            String storedHash = jdbcTemplate.queryForObject("SELECT sha256 FROM dcc_registration_certificate_file WHERE id = ? AND tenant_id = ? AND deleted = 0",
                    String.class, detail.businessFileId(), tenantId);
            if (!Objects.equals(storedHash, cn.hutool.crypto.digest.DigestUtil.sha256Hex(content)))
                throw new ServiceException(REGISTRATION_CERTIFICATE_IDEMPOTENCY_CONFLICT);
            return replay(new DccRegistrationCertificateSupportingDocumentCommand(tenantId, actorId, key, null,
                    certificateId, versionId, detail.businessFileId(), null, null, documentType, null), existing, EVENT_EFFECTIVE);
        }
        Long infraId = fileService.createFileAndReturnId(content, file.getOriginalFilename(),
                "dcc/registration-certificate/supporting/" + certificateId, file.getContentType());
        var businessFile = cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateFileDO.builder()
                .ownerType("VERSION").ownerId(versionId).fileKind(documentType.trim()).infraFileId(infraId)
                .originalName(file.getOriginalFilename()).mimeType(file.getContentType()).fileSize((long) content.length)
                .sha256(cn.hutool.crypto.digest.DigestUtil.sha256Hex(content)).status("STAGED").build();
        businessFile.setTenantId(tenantId);
        requireSingle(registrationCertificateFileMapper.insert(businessFile), REGISTRATION_CERTIFICATE_FILE_REQUIRED);
        return upload(new DccRegistrationCertificateSupportingDocumentCommand(tenantId, actorId, key, null,
                certificateId, versionId, businessFile.getId(), null, null, documentType, null));
    }

    @Transactional(rollbackFor = Exception.class)
    public DccRegistrationCertificateSupportingDocumentResult upload(
            DccRegistrationCertificateSupportingDocumentCommand command) {
        validateCommon(command);
        validateDocumentType(command.documentType());
        CertificateRef certificate = requireCertificate(command);
        ExistingEvent existing = findEvent(command.tenantId(), command.idempotencyKey());
        if (existing != null) {
            return replay(command, existing, EVENT_EFFECTIVE);
        }
        requireStagedBusinessFile(command);
        Long supportId = insertEffectiveDocument(command, certificate);
        bindBusinessFile(command, supportId);
        insertLifecycleEvent(command, certificate, supportId, EVENT_EFFECTIVE, null);
        return new DccRegistrationCertificateSupportingDocumentResult(supportId, command.certificateId(),
                command.versionId(), command.documentType().trim(), STATUS_EFFECTIVE, false);
    }

    private void requireStagedBusinessFile(DccRegistrationCertificateSupportingDocumentCommand command) {
        List<BusinessFile> rows = jdbcTemplate.query("""
                SELECT tenant_id, owner_type, owner_id, file_kind, status
                  FROM dcc_registration_certificate_file
                 WHERE id = ? AND deleted = 0
                """, (rs, rowNum) -> new BusinessFile(
                rs.getLong("tenant_id"), rs.getString("owner_type"), rs.getLong("owner_id"),
                rs.getString("file_kind"), rs.getString("status")), command.businessFileId());
        if (rows.isEmpty()) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_REQUIRED);
        }
        BusinessFile file = rows.get(0);
        if (!Objects.equals(file.tenantId(), command.tenantId())
                || !"VERSION".equals(file.ownerType())
                || !Objects.equals(file.ownerId(), command.versionId())
                || !Objects.equals(file.fileKind(), command.documentType().trim())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_OWNER_CONFLICT);
        }
        if (!"STAGED".equals(file.status())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_NOT_STAGED);
        }
    }

    private void bindBusinessFile(DccRegistrationCertificateSupportingDocumentCommand command, Long supportId) {
        LocalDateTime boundAt = businessClock.now();
        requireSingle(jdbcTemplate.update("""
                UPDATE dcc_registration_certificate_file
                   SET owner_type = 'SUPPORTING_DOCUMENT', owner_id = ?, status = 'BOUND',
                       bound_at = ?, bound_by = ?, updater = ?, update_time = ?
                 WHERE id = ? AND tenant_id = ? AND owner_type = 'VERSION' AND owner_id = ?
                   AND file_kind = ? AND status = 'STAGED' AND deleted = 0
                """, supportId, boundAt, command.actorId(), String.valueOf(command.actorId()), boundAt,
                command.businessFileId(), command.tenantId(), command.versionId(), command.documentType().trim()),
                REGISTRATION_CERTIFICATE_SUPPORTING_DOCUMENT_STATUS_INVALID);
    }

    private Long insertEffectiveDocument(DccRegistrationCertificateSupportingDocumentCommand command,
                                       CertificateRef certificate) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            int affected = jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement("""
                        INSERT INTO dcc_registration_certificate_supporting_document
                          (tenant_id, owner_company_id, certificate_id, version_id, business_file_id,
                           document_type, status, row_version, uploaded_at, uploaded_by, creator)
                        VALUES (?, ?, ?, ?, ?, ?, ?, 1, ?, ?, ?)
                        """, new String[] { "id" });
                ps.setLong(1, command.tenantId());
                ps.setLong(2, certificate.ownerCompanyId());
                ps.setLong(3, command.certificateId());
                ps.setLong(4, command.versionId());
                ps.setLong(5, command.businessFileId());
                ps.setString(6, command.documentType().trim());
                ps.setString(7, STATUS_EFFECTIVE);
                ps.setObject(8, businessClock.now());
                ps.setLong(9, command.actorId());
                ps.setString(10, String.valueOf(command.actorId()));
                return ps;
            }, keyHolder);
            requireSingle(affected, REGISTRATION_CERTIFICATE_SUPPORTING_DOCUMENT_STATUS_INVALID);
        } catch (DuplicateKeyException exception) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_SUPPORTING_DOCUMENT_STATUS_INVALID);
        }
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new org.springframework.dao.DataRetrievalFailureException(
                    "Supporting document insert returned no generated primary key");
        }
        return key.longValue();
    }

    private CertificateRef requireCertificate(DccRegistrationCertificateSupportingDocumentCommand command) {
        List<CertificateRef> rows = jdbcTemplate.query("""
                SELECT c.owner_company_id, c.current_version_id, v.status AS version_status, s.facts_json
                  FROM dcc_registration_certificate c
                  JOIN dcc_registration_certificate_version v
                    ON v.id = ? AND v.tenant_id = c.tenant_id AND v.certificate_id = c.id
                  JOIN dcc_registration_certificate_snapshot s ON s.id = c.current_snapshot_id AND s.tenant_id = c.tenant_id AND s.version_id = v.id
                 WHERE c.id = ? AND c.tenant_id = ? AND c.status = 'ACTIVE' AND c.deleted = 0 AND v.deleted = 0 AND s.deleted = 0 FOR UPDATE
                """, (rs, rowNum) -> new CertificateRef(
                rs.getLong("owner_company_id"),
                rs.getLong("current_version_id"),
                rs.getString("version_status"), rs.getString("facts_json")), command.versionId(), command.certificateId(), command.tenantId());
        if (rows.size() != 1 || !Objects.equals(rows.get(0).currentVersionId(), command.versionId())
                || !"CURRENT".equals(rows.get(0).versionStatus())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_SUPPORTING_DOCUMENT_REQUIRED);
        }
        String type = cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateFacts.parse(rows.get(0).factsJson()).getCertificateType();
        if (!"DOMESTIC".equals(type)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_SUPPORTING_DOCUMENT_REQUIRED);
        }
        return rows.get(0);
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
                    command.actorId(), JsonUtils.toJsonString(new EventDetail(supportId, command.businessFileId(),
                            command.documentType().trim(),
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
        if (detail == null || !Objects.equals(detail.businessFileId(), command.businessFileId())
                || !Objects.equals(detail.documentType(), command.documentType().trim())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_IDEMPOTENCY_CONFLICT);
        }
        if (!EVENT_EFFECTIVE.equals(expectedType)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACTIVATION_EVENT_CONFLICT);
        }
        return new DccRegistrationCertificateSupportingDocumentResult(detail.supportingDocumentId(),
                event.certificateId(), event.versionId(), detail.documentType(), STATUS_EFFECTIVE, false);
    }

    private void validateCommon(DccRegistrationCertificateSupportingDocumentCommand command) {
        if (command == null || command.tenantId() == null || command.tenantId() <= 0
                || command.actorId() == null || command.actorId() <= 0
                || command.certificateId() == null || command.certificateId() <= 0
                || command.versionId() == null || command.versionId() <= 0
                || (command.businessFileId() == null || command.businessFileId() <= 0)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_SUPPORTING_DOCUMENT_REQUIRED);
        }
        if (isBlank(command.idempotencyKey())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_IDEMPOTENCY_KEY_REQUIRED);
        }
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
            throw new IllegalArgumentException(name + " 不能为空");
        }
        return value;
    }

    private record CertificateRef(Long ownerCompanyId, Long currentVersionId, String versionStatus, String factsJson) {
    }

    private record BusinessFile(Long tenantId, String ownerType, Long ownerId, String fileKind, String status) {
    }

    private record ExistingEvent(String eventType, Long certificateId, Long versionId, String detailJson) {
    }

    private record EventDetail(Long supportingDocumentId, Long businessFileId, String documentType, String rejectReason) {
    }
}
