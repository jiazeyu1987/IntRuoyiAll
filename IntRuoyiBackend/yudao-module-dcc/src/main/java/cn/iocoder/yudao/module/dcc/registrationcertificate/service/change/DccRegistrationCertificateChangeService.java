package cn.iocoder.yudao.module.dcc.registrationcertificate.service.change;

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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_CHANGE_HISTORY_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_CHANGE_PRODUCTION_RELATION_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_CHANGE_TYPE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_CHANGE_VALUE_FORBIDDEN;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_CHANGE_VALUE_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_FILE_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_FILE_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_IDEMPOTENCY_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_IDEMPOTENCY_KEY_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_LIFECYCLE_EVENT_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_REVISION_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_STATUS_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_TOP_LEVEL_VOID_REASON_REQUIRED;

@Service
public class DccRegistrationCertificateChangeService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_CURRENT = "CURRENT";
    private static final String STATUS_VOIDED = "VOIDED";
    private static final String EVENT_CHANGE_APPLIED = "CHANGE_APPLIED";
    private static final String EVENT_CERTIFICATE_VOIDED = "CERTIFICATE_VOIDED";

    private static final Map<String, String> STRUCTURED_COLUMNS = new LinkedHashMap<>();

    static {
        STRUCTURED_COLUMNS.put("PRODUCT_NAME", "productName");
        STRUCTURED_COLUMNS.put("REGISTRANT_NAME", "registrantName");
        STRUCTURED_COLUMNS.put("MODEL_SPECIFICATION", "modelSpecification");
        STRUCTURED_COLUMNS.put("STRUCTURE_COMPOSITION", "structureComposition");
        STRUCTURED_COLUMNS.put("INTENDED_USE", "intendedUse");
        STRUCTURED_COLUMNS.put("TECHNICAL_REQUIREMENTS", "technicalRequirements");
        STRUCTURED_COLUMNS.put("RESIDENCE_ADDRESS", "residenceAddress");
        STRUCTURED_COLUMNS.put("PRODUCTION_ADDRESS", "productionAddress");
    }

    private final JdbcTemplate jdbcTemplate;
    private final DccRegistrationCertificateBusinessClock businessClock;

    public DccRegistrationCertificateChangeService(JdbcTemplate jdbcTemplate,
                                                   DccRegistrationCertificateBusinessClock businessClock) {
        this.jdbcTemplate = require(jdbcTemplate, "jdbcTemplate");
        this.businessClock = require(businessClock, "businessClock");
    }

    @Transactional(rollbackFor = Exception.class)
    public DccRegistrationCertificateChangeResult applyChange(DccRegistrationCertificateChangeCommand command) {
        validateBaseCommand(command);
        String payloadHash = payloadHash("CHANGE", command);
        ExistingEvent existing = findExistingEvent(command.tenantId(), command.idempotencyKey());
        if (existing != null) {
            return replayChange(existing, payloadHash);
        }
        ChangeSelection selection = validateSelection(command);
        CertificateState state = requireCurrentState(command.tenantId(), command.certificateId(),
                command.expectedRowVersion());
        validateChangeFile(command, state);

        Long resultingSnapshotId = state.snapshotId();
        SnapshotRow target = state.snapshot();
        if (!selection.structuredValues().isEmpty()) {
            target = state.snapshot().withChanges(selection.structuredValues(), command);
            resultingSnapshotId = insertSnapshot(command.tenantId(), state.versionId(), target);
        }

        requireSingle(jdbcTemplate.update("""
                        UPDATE dcc_registration_certificate
                           SET current_snapshot_id = ?, row_version = row_version + 1
                         WHERE id = ? AND tenant_id = ? AND status = ?
                           AND row_version = ? AND current_version_id = ? AND current_snapshot_id = ?
                        """, resultingSnapshotId, command.certificateId(), command.tenantId(), STATUS_ACTIVE,
                command.expectedRowVersion(), state.versionId(), state.snapshotId()),
                REGISTRATION_CERTIFICATE_REVISION_CONFLICT);

        Long eventId = insertLifecycleEvent(command, state, resultingSnapshotId, EVENT_CHANGE_APPLIED,
                payloadHash, selection.itemTypes());
        Long changeId = insertChange(command, state, resultingSnapshotId, eventId, selection.itemTypes());
        bindChangeFile(command, state, changeId);
        insertChangeItems(command.tenantId(), changeId, state.snapshot(), target, selection);
        return new DccRegistrationCertificateChangeResult(command.certificateId(), changeId,
                state.snapshotId(), resultingSnapshotId, "APPLIED");
    }

    @Transactional(rollbackFor = Exception.class)
    public DccRegistrationCertificateChangeResult voidCertificate(DccRegistrationCertificateChangeCommand command) {
        validateBaseCommand(command);
        if (isBlank(command.voidReason())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_TOP_LEVEL_VOID_REASON_REQUIRED);
        }
        String payloadHash = payloadHash("VOID", command);
        ExistingEvent existing = findExistingEvent(command.tenantId(), command.idempotencyKey());
        if (existing != null) {
            return replayVoid(existing, payloadHash);
        }
        CertificateState state = requireCurrentState(command.tenantId(), command.certificateId(),
                command.expectedRowVersion());

        requireSingle(jdbcTemplate.update("""
                        UPDATE dcc_registration_certificate
                           SET status = ?, row_version = row_version + 1
                         WHERE id = ? AND tenant_id = ? AND status = ?
                           AND row_version = ? AND current_version_id = ? AND current_snapshot_id = ?
                        """, STATUS_VOIDED, command.certificateId(), command.tenantId(), STATUS_ACTIVE,
                command.expectedRowVersion(), state.versionId(), state.snapshotId()),
                REGISTRATION_CERTIFICATE_REVISION_CONFLICT);
        requireSingle(jdbcTemplate.update("""
                        UPDATE dcc_registration_certificate_version
                           SET status = ?, voided_at = ?, voided_by = ?, void_reason = ?
                         WHERE id = ? AND tenant_id = ? AND certificate_id = ? AND status = ?
                        """, STATUS_VOIDED, businessClock.now(), command.actorId(), command.voidReason().trim(),
                state.versionId(), command.tenantId(), command.certificateId(), STATUS_CURRENT),
                REGISTRATION_CERTIFICATE_STATUS_INVALID);
        voidBoundRegistrationFiles(command.tenantId(), state.versionId());

        Long eventId = insertLifecycleEvent(command, state, state.snapshotId(), EVENT_CERTIFICATE_VOIDED,
                payloadHash, List.of("CERTIFICATE_VOIDED"));
        return new DccRegistrationCertificateChangeResult(command.certificateId(), eventId,
                state.snapshotId(), state.snapshotId(), STATUS_VOIDED);
    }

    private void validateBaseCommand(DccRegistrationCertificateChangeCommand command) {
        if (command == null || command.tenantId() == null || command.tenantId() <= 0
                || command.actorId() == null || command.actorId() <= 0
                || command.certificateId() == null || command.certificateId() <= 0
                || command.expectedRowVersion() == null || command.expectedRowVersion() <= 0
                || command.approvalDate() == null) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_STATUS_INVALID);
        }
        if (isBlank(command.idempotencyKey())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_IDEMPOTENCY_KEY_REQUIRED);
        }
        if (isBlank(command.requestTraceId())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_LIFECYCLE_EVENT_CONFLICT);
        }
    }

    private ChangeSelection validateSelection(DccRegistrationCertificateChangeCommand command) {
        Map<String, String> structured = new LinkedHashMap<>();
        if (command.structuredValues() != null) {
            for (Map.Entry<String, String> entry : command.structuredValues().entrySet()) {
                if (!STRUCTURED_COLUMNS.containsKey(entry.getKey())) {
                    throw new ServiceException(REGISTRATION_CERTIFICATE_CHANGE_TYPE_INVALID);
                }
                if (isBlank(entry.getValue())) {
                    throw new ServiceException(REGISTRATION_CERTIFICATE_CHANGE_VALUE_REQUIRED);
                }
                structured.put(entry.getKey(), entry.getValue().trim());
            }
        }
        boolean hasOther = !isBlank(command.otherDescription());
        if (hasOther && !structured.isEmpty()) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_CHANGE_VALUE_FORBIDDEN);
        }
        if (!hasOther && structured.isEmpty()) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_CHANGE_TYPE_INVALID);
        }
        if (structured.containsKey("PRODUCTION_ADDRESS")) {
            validateProductionRelation(command);
        }
        List<String> itemTypes = new ArrayList<>(structured.keySet());
        if (hasOther) {
            itemTypes.add("OTHER_CONTENT");
        }
        return new ChangeSelection(structured, hasOther ? command.otherDescription().trim() : null, itemTypes);
    }

    private void validateProductionRelation(DccRegistrationCertificateChangeCommand command) {
        if (command.entrustedProduction() == null || command.selfProduction() == null
                || (!command.entrustedProduction() && !command.selfProduction())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_CHANGE_PRODUCTION_RELATION_REQUIRED);
        }
        if (Boolean.TRUE.equals(command.entrustedProduction())) {
            if (isBlank(command.entrustedEnterprisesJson()) || "[]".equals(command.entrustedEnterprisesJson().trim())) {
                throw new ServiceException(REGISTRATION_CERTIFICATE_CHANGE_PRODUCTION_RELATION_REQUIRED);
            }
        } else if (!isBlank(command.entrustedEnterprisesJson())
                && !"[]".equals(command.entrustedEnterprisesJson().trim())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_CHANGE_PRODUCTION_RELATION_REQUIRED);
        }
    }

    private CertificateState requireCurrentState(Long tenantId, Long certificateId, Integer expectedRowVersion) {
        List<CertificateState> rows = jdbcTemplate.query("""
                        SELECT c.owner_company_id, c.current_version_id, c.current_snapshot_id, c.status, c.row_version,
                               v.status AS version_status,
                               s.id AS snapshot_id, s.version_id, s.revision_no, s.product_name, s.registrant_name,
                               s.model_specification, s.structure_composition, s.intended_use,
                               s.technical_requirements, s.residence_address, s.production_address,
                               s.entrusted_production, s.self_production, s.entrusted_enterprises_json, s.effective_at
                          FROM dcc_registration_certificate c
                          JOIN dcc_registration_certificate_version v
                            ON v.id = c.current_version_id AND v.tenant_id = c.tenant_id
                          JOIN dcc_registration_certificate_snapshot s
                            ON s.id = c.current_snapshot_id AND s.tenant_id = c.tenant_id
                         WHERE c.tenant_id = ? AND c.id = ?
                        """, (rs, rowNum) -> new CertificateState(
                        rs.getLong("owner_company_id"),
                        rs.getLong("current_version_id"),
                        rs.getLong("current_snapshot_id"),
                        rs.getString("status"),
                        rs.getInt("row_version"),
                        rs.getString("version_status"),
                        new SnapshotRow(
                                rs.getLong("snapshot_id"),
                                rs.getLong("version_id"),
                                rs.getInt("revision_no"),
                                rs.getString("product_name"),
                                rs.getString("registrant_name"),
                                rs.getString("model_specification"),
                                rs.getString("structure_composition"),
                                rs.getString("intended_use"),
                                rs.getString("technical_requirements"),
                                rs.getString("residence_address"),
                                rs.getString("production_address"),
                                rs.getBoolean("entrusted_production"),
                                rs.getBoolean("self_production"),
                                rs.getString("entrusted_enterprises_json"),
                                rs.getObject("effective_at", LocalDateTime.class))),
                tenantId, certificateId);
        if (rows.isEmpty()) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_NOT_EXISTS);
        }
        CertificateState state = rows.get(0);
        if (!STATUS_ACTIVE.equals(state.status()) || !STATUS_CURRENT.equals(state.versionStatus())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_STATUS_INVALID);
        }
        if (!Objects.equals(state.rowVersion(), expectedRowVersion)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_REVISION_CONFLICT);
        }
        return state;
    }

    private void voidBoundRegistrationFiles(Long tenantId, Long versionId) {
        Integer boundCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM dcc_registration_certificate_file
                 WHERE tenant_id = ? AND owner_type = 'VERSION' AND owner_id = ?
                   AND file_kind = 'REGISTRATION_CERTIFICATE' AND status = 'BOUND'
                """, Integer.class, tenantId, versionId);
        if (boundCount == null || boundCount <= 0) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_REQUIRED);
        }
        int affected = jdbcTemplate.update("""
                UPDATE dcc_registration_certificate_file
                   SET status = 'VOIDED'
                 WHERE tenant_id = ? AND owner_type = 'VERSION' AND owner_id = ?
                   AND file_kind = 'REGISTRATION_CERTIFICATE' AND status = 'BOUND'
                """, tenantId, versionId);
        if (affected != boundCount) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_CONFLICT);
        }
    }

    private void validateChangeFile(DccRegistrationCertificateChangeCommand command, CertificateState state) {
        if (command.businessFileId() == null) {
            return;
        }
        Integer matching = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM dcc_registration_certificate_file
                 WHERE id = ? AND tenant_id = ? AND owner_type = 'VERSION' AND owner_id = ?
                   AND file_kind = 'CHANGE_APPROVAL' AND status = 'STAGED'
                """, Integer.class, command.businessFileId(), command.tenantId(), state.versionId());
        if (matching == null || matching != 1) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_REQUIRED);
        }
    }

    private void bindChangeFile(DccRegistrationCertificateChangeCommand command, CertificateState state,
                                Long changeId) {
        if (command.businessFileId() == null) {
            return;
        }
        int affected;
        try {
            affected = jdbcTemplate.update("""
                    UPDATE dcc_registration_certificate_file
                       SET owner_type = 'CHANGE', owner_id = ?, status = 'BOUND',
                           bound_at = ?, bound_by = ?
                     WHERE id = ? AND tenant_id = ? AND owner_type = 'VERSION' AND owner_id = ?
                       AND file_kind = 'CHANGE_APPROVAL' AND status = 'STAGED'
                    """, changeId, businessClock.now(), command.actorId(), command.businessFileId(),
                    command.tenantId(), state.versionId());
        } catch (DuplicateKeyException exception) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_CONFLICT);
        }
        if (affected != 1) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_CONFLICT);
        }
    }

    private Long insertSnapshot(Long tenantId, Long versionId, SnapshotRow snapshot) {
        return insertAndReturnId("""
                INSERT INTO dcc_registration_certificate_snapshot
                  (tenant_id, version_id, revision_no, product_name, registrant_name,
                   model_specification, structure_composition, intended_use, technical_requirements,
                   residence_address, production_address, entrusted_production, self_production,
                   entrusted_enterprises_json, effective_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, ps -> {
            ps.setLong(1, tenantId);
            ps.setLong(2, versionId);
            ps.setInt(3, snapshot.revisionNo() + 1);
            ps.setString(4, snapshot.productName());
            ps.setString(5, snapshot.registrantName());
            ps.setString(6, snapshot.modelSpecification());
            ps.setString(7, snapshot.structureComposition());
            ps.setString(8, snapshot.intendedUse());
            ps.setString(9, snapshot.technicalRequirements());
            ps.setString(10, snapshot.residenceAddress());
            ps.setString(11, snapshot.productionAddress());
            ps.setBoolean(12, Boolean.TRUE.equals(snapshot.entrustedProduction()));
            ps.setBoolean(13, Boolean.TRUE.equals(snapshot.selfProduction()));
            ps.setString(14, snapshot.entrustedEnterprisesJson());
            ps.setObject(15, businessClock.now());
        });
    }

    private Long insertLifecycleEvent(DccRegistrationCertificateChangeCommand command, CertificateState state,
                                      Long targetSnapshotId, String eventType, String payloadHash,
                                      List<String> itemTypes) {
        Integer nextSequence = jdbcTemplate.queryForObject("""
                SELECT COALESCE(MAX(event_sequence), 0) + 1
                  FROM dcc_registration_certificate_lifecycle_event
                 WHERE tenant_id = ? AND certificate_id = ?
                """, Integer.class, command.tenantId(), command.certificateId());
        try {
            return insertAndReturnId("""
                    INSERT INTO dcc_registration_certificate_lifecycle_event
                      (tenant_id, owner_company_id, certificate_id, source_version_id, target_version_id,
                       source_snapshot_id, target_snapshot_id, event_key, event_type, event_sequence,
                       baseline_row_version, baseline_snapshot_revision, actor_id, detail_json, occurred_at, creator)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, ps -> {
                ps.setLong(1, command.tenantId());
                ps.setLong(2, state.ownerCompanyId());
                ps.setLong(3, command.certificateId());
                ps.setLong(4, state.versionId());
                ps.setLong(5, state.versionId());
                ps.setLong(6, state.snapshotId());
                ps.setLong(7, targetSnapshotId);
                ps.setString(8, command.idempotencyKey());
                ps.setString(9, eventType);
                ps.setInt(10, nextSequence == null ? 1 : nextSequence);
                ps.setInt(11, command.expectedRowVersion());
                ps.setInt(12, state.snapshot().revisionNo());
                ps.setLong(13, command.actorId());
                ps.setString(14, JsonUtils.toJsonString(new ChangeEventDetail(payloadHash,
                        itemTypes, command.voidReason())));
                ps.setObject(15, businessClock.now());
                ps.setString(16, String.valueOf(command.actorId()));
            });
        } catch (DuplicateKeyException exception) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_LIFECYCLE_EVENT_CONFLICT);
        }
    }

    private Long insertChange(DccRegistrationCertificateChangeCommand command, CertificateState state,
                              Long resultingSnapshotId, Long eventId, List<String> itemTypes) {
        return insertAndReturnId("""
                INSERT INTO dcc_registration_certificate_change
                  (tenant_id, owner_company_id, certificate_id, source_version_id, source_snapshot_id,
                   resulting_snapshot_id, event_id, approval_date, selected_change_types_json,
                   selected_item_count, status, actor_id, applied_at, creator)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'APPLIED', ?, ?, ?)
                """, ps -> {
            ps.setLong(1, command.tenantId());
            ps.setLong(2, state.ownerCompanyId());
            ps.setLong(3, command.certificateId());
            ps.setLong(4, state.versionId());
            ps.setLong(5, state.snapshotId());
            ps.setLong(6, resultingSnapshotId);
            ps.setLong(7, eventId);
            ps.setObject(8, command.approvalDate());
            ps.setString(9, JsonUtils.toJsonString(itemTypes));
            ps.setInt(10, itemTypes.size());
            ps.setLong(11, command.actorId());
            ps.setObject(12, businessClock.now());
            ps.setString(13, String.valueOf(command.actorId()));
        });
    }

    private void insertChangeItems(Long tenantId, Long changeId, SnapshotRow before,
                                   SnapshotRow after, ChangeSelection selection) {
        int sort = 1;
        for (String itemType : selection.itemTypes()) {
            String beforeJson = valueJson(before.valueOf(itemType));
            String afterJson = "OTHER_CONTENT".equals(itemType)
                    ? valueJson(selection.otherDescription())
                    : valueJson(after.valueOf(itemType));
            try {
                requireSingle(jdbcTemplate.update("""
                                INSERT INTO dcc_registration_certificate_change_item
                                  (tenant_id, change_id, item_type, before_value_json, after_value_json, sort_order)
                                VALUES (?, ?, ?, ?, ?, ?)
                                """, tenantId, changeId, itemType, beforeJson, afterJson, sort++),
                        REGISTRATION_CERTIFICATE_CHANGE_HISTORY_CONFLICT);
            } catch (DuplicateKeyException exception) {
                throw new ServiceException(REGISTRATION_CERTIFICATE_CHANGE_HISTORY_CONFLICT);
            }
        }
    }

    private DccRegistrationCertificateChangeResult replayChange(ExistingEvent event, String payloadHash) {
        ChangeEventDetail detail = JsonUtils.parseObject(event.detailJson(), ChangeEventDetail.class);
        if (!EVENT_CHANGE_APPLIED.equals(event.eventType()) || detail == null
                || !Objects.equals(payloadHash, detail.payloadHash())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_IDEMPOTENCY_CONFLICT);
        }
        Long changeId = jdbcTemplate.queryForObject("""
                SELECT id FROM dcc_registration_certificate_change
                 WHERE tenant_id = ? AND event_id = ?
                """, Long.class, event.tenantId(), event.eventId());
        return new DccRegistrationCertificateChangeResult(event.certificateId(), changeId,
                event.sourceSnapshotId(), event.targetSnapshotId(), "APPLIED");
    }

    private DccRegistrationCertificateChangeResult replayVoid(ExistingEvent event, String payloadHash) {
        ChangeEventDetail detail = JsonUtils.parseObject(event.detailJson(), ChangeEventDetail.class);
        if (!EVENT_CERTIFICATE_VOIDED.equals(event.eventType()) || detail == null
                || !Objects.equals(payloadHash, detail.payloadHash())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_IDEMPOTENCY_CONFLICT);
        }
        return new DccRegistrationCertificateChangeResult(event.certificateId(), event.eventId(),
                event.sourceSnapshotId(), event.targetSnapshotId(), STATUS_VOIDED);
    }

    private ExistingEvent findExistingEvent(Long tenantId, String eventKey) {
        if (isBlank(eventKey)) {
            return null;
        }
        List<ExistingEvent> events = jdbcTemplate.query("""
                SELECT id, tenant_id, certificate_id, source_snapshot_id, target_snapshot_id, event_type, detail_json
                  FROM dcc_registration_certificate_lifecycle_event
                 WHERE tenant_id = ? AND event_key = ?
                """, (rs, rowNum) -> new ExistingEvent(
                rs.getLong("id"),
                rs.getLong("tenant_id"),
                rs.getLong("certificate_id"),
                rs.getObject("source_snapshot_id", Long.class),
                rs.getObject("target_snapshot_id", Long.class),
                rs.getString("event_type"),
                rs.getString("detail_json")), tenantId, eventKey);
        return events.isEmpty() ? null : events.get(0);
    }

    private Long insertAndReturnId(String sql, SqlBinder binder) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int affected = jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            binder.bind(statement);
            return statement;
        }, keyHolder);
        requireSingle(affected, REGISTRATION_CERTIFICATE_CHANGE_HISTORY_CONFLICT);
        Map<String, Object> keys = keyHolder.getKeys();
        Object id = keys == null ? null : keys.get("id");
        Number key = id instanceof Number number ? number : null;
        if (key == null) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_CHANGE_HISTORY_CONFLICT);
        }
        return key.longValue();
    }

    private static String valueJson(String value) {
        return JsonUtils.toJsonString(Map.of("value", value == null ? "" : value));
    }

    private static String payloadHash(String prefix, DccRegistrationCertificateChangeCommand command) {
        return sha256(prefix + "|" + command.certificateId() + "|" + command.expectedRowVersion()
                + "|" + command.approvalDate() + "|" + normalize(command.structuredValues())
                + "|" + normalize(command.otherDescription()) + "|" + command.entrustedProduction()
                + "|" + command.selfProduction() + "|" + normalize(command.entrustedEnterprisesJson())
                + "|" + normalize(command.voidReason()) + "|" + command.businessFileId());
    }

    private static String normalize(Map<String, String> values) {
        return values == null ? "" : new java.util.TreeMap<>(values).toString();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
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
            throw new IllegalArgumentException(name + " must not be null");
        }
        return value;
    }

    @FunctionalInterface
    private interface SqlBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }

    private record ChangeSelection(Map<String, String> structuredValues, String otherDescription,
                                   List<String> itemTypes) {
    }

    private record CertificateState(Long ownerCompanyId, Long versionId, Long snapshotId, String status,
                                    Integer rowVersion, String versionStatus, SnapshotRow snapshot) {
    }

    private record ExistingEvent(Long eventId, Long tenantId, Long certificateId, Long sourceSnapshotId,
                                 Long targetSnapshotId, String eventType, String detailJson) {
    }

    private record ChangeEventDetail(String payloadHash, List<String> itemTypes, String voidReason) {
    }

    private record SnapshotRow(Long id, Long versionId, Integer revisionNo, String productName,
                               String registrantName, String modelSpecification, String structureComposition,
                               String intendedUse, String technicalRequirements, String residenceAddress,
                               String productionAddress, Boolean entrustedProduction, Boolean selfProduction,
                               String entrustedEnterprisesJson, LocalDateTime effectiveAt) {

        SnapshotRow withChanges(Map<String, String> changes, DccRegistrationCertificateChangeCommand command) {
            return new SnapshotRow(id, versionId, revisionNo,
                    changes.getOrDefault("PRODUCT_NAME", productName),
                    changes.getOrDefault("REGISTRANT_NAME", registrantName),
                    changes.getOrDefault("MODEL_SPECIFICATION", modelSpecification),
                    changes.getOrDefault("STRUCTURE_COMPOSITION", structureComposition),
                    changes.getOrDefault("INTENDED_USE", intendedUse),
                    changes.getOrDefault("TECHNICAL_REQUIREMENTS", technicalRequirements),
                    changes.getOrDefault("RESIDENCE_ADDRESS", residenceAddress),
                    changes.getOrDefault("PRODUCTION_ADDRESS", productionAddress),
                    changes.containsKey("PRODUCTION_ADDRESS") ? command.entrustedProduction() : entrustedProduction,
                    changes.containsKey("PRODUCTION_ADDRESS") ? command.selfProduction() : selfProduction,
                    changes.containsKey("PRODUCTION_ADDRESS") ? normalizeEntrustedJson(command) : entrustedEnterprisesJson,
                    effectiveAt);
        }

        String valueOf(String itemType) {
            return switch (itemType) {
                case "PRODUCT_NAME" -> productName;
                case "REGISTRANT_NAME" -> registrantName;
                case "MODEL_SPECIFICATION" -> modelSpecification;
                case "STRUCTURE_COMPOSITION" -> structureComposition;
                case "INTENDED_USE" -> intendedUse;
                case "TECHNICAL_REQUIREMENTS" -> technicalRequirements;
                case "RESIDENCE_ADDRESS" -> residenceAddress;
                case "PRODUCTION_ADDRESS" -> productionAddress;
                case "OTHER_CONTENT" -> "";
                default -> throw new ServiceException(REGISTRATION_CERTIFICATE_CHANGE_TYPE_INVALID);
            };
        }

        private static String normalizeEntrustedJson(DccRegistrationCertificateChangeCommand command) {
            return isBlank(command.entrustedEnterprisesJson()) ? "[]" : command.entrustedEnterprisesJson().trim();
        }
    }
}
