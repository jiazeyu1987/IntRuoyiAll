package cn.iocoder.yudao.module.dcc.registrationcertificate.service.change;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.association.DccRegistrationCertificateProjectCodeFileAssociationService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateBusinessClock;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.notification.event.DccRegistrationCertificateBusinessEventNotifier;
import cn.iocoder.yudao.module.dcc.service.productcatalog.DccProductCatalogRegistrationSyncService;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_CHANGE_HISTORY_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_CHANGE_PENDING_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_CHANGE_PRODUCTION_RELATION_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_CHANGE_TYPE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_CHANGE_VALUE_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_COMPANY_SCOPE_DENIED;
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
    private static final String STATUS_PENDING_APPROVAL = "PENDING_APPROVAL";
    private static final String STATUS_APPLIED = "APPLIED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String REQUEST_STATUS_SUBMITTED = "SUBMITTED";
    private static final String REQUEST_STATUS_APPROVED = "APPROVED";
    private static final String REQUEST_TYPE_UPLOAD_CERTIFICATE = "UPLOAD_CERTIFICATE";
    private static final String REQUEST_OPERATION_CHANGE_CERTIFICATE = "CHANGE_CERTIFICATE";
    private static final String REQUEST_FILE_STATUS_REQUESTED = "REQUESTED";
    private static final String EVENT_CHANGE_APPLIED = "CHANGE_APPLIED";
    private static final String EVENT_CHANGE_SUBMITTED = "CHANGE_SUBMITTED";
    private static final String EVENT_CERTIFICATE_VOIDED = "CERTIFICATE_VOIDED";
    private static final String FILE_OWNER_CHANGE = "CHANGE";
    private static final String FILE_KIND_CHANGE_APPROVAL = "CHANGE_APPROVAL";
    private static final String FILE_STATUS_BOUND = "BOUND";

    private static final Map<String, String> STRUCTURED_COLUMNS = new LinkedHashMap<>();

    static {
        STRUCTURED_COLUMNS.put("CERTIFICATE_INFORMATION", "factsJson");
        STRUCTURED_COLUMNS.put("PRODUCTION_RELATION", "productionRelation");
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
    private final FileService fileService;
    private final DccRegistrationCertificateBusinessClock businessClock;
    private final DccRegistrationCertificateBusinessEventNotifier businessEventNotifier;
    private final DccRegistrationCertificateProjectCodeFileAssociationService projectCodeFileAssociationService;
    private final DccProductCatalogRegistrationSyncService productCatalogRegistrationSyncService;

    public DccRegistrationCertificateChangeService(JdbcTemplate jdbcTemplate,
                                                    FileService fileService,
                                                    DccRegistrationCertificateBusinessClock businessClock,
                                                    DccRegistrationCertificateBusinessEventNotifier businessEventNotifier,
                                                    DccRegistrationCertificateProjectCodeFileAssociationService projectCodeFileAssociationService,
                                                    DccProductCatalogRegistrationSyncService productCatalogRegistrationSyncService) {
        this.jdbcTemplate = require(jdbcTemplate, "jdbcTemplate");
        this.fileService = require(fileService, "fileService");
        this.businessClock = require(businessClock, "businessClock");
        this.businessEventNotifier = require(businessEventNotifier, "businessEventNotifier");
        this.projectCodeFileAssociationService = require(
                projectCodeFileAssociationService, "projectCodeFileAssociationService");
        this.productCatalogRegistrationSyncService = require(
                productCatalogRegistrationSyncService, "productCatalogRegistrationSyncService");
    }

    @Transactional(rollbackFor = Exception.class)
    public DccRegistrationCertificateChangeResult applyChange(DccRegistrationCertificateChangeCommand command) {
        validateBaseCommand(command);
        var retained = retainedMaterials(command);
        ChangeUploadFile uploadFile = primaryUpload(command,retained);
        String payloadHash = payloadHash("CHANGE", command, uploadFile);
        ExistingEvent existing = findExistingEvent(command.tenantId(), command.idempotencyKey());
        if (existing != null) {
            return replayChange(existing, payloadHash);
        }
        ChangeSelection selection = validateSelection(command);
        CertificateState state = requireCurrentState(command.tenantId(), command.certificateId(),
                command.expectedRowVersion());
        validateFactsReplacement(state.snapshot(), command.structuredValues());
        Long infraFileId = fileService.createFileAndReturnId(
                uploadFile.content(), uploadFile.originalName(),
                "dcc/registration-certificate/change/" + command.certificateId(), uploadFile.mimeType());

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
        Long businessFileId = insertChangeFile(command, changeId, infraFileId, uploadFile);
        projectCodeFileAssociationService.bindChangeApprovalFile(
                command.tenantId(), null, changeId, businessFileId, command.actorId());
        insertChangeItems(command, command.tenantId(), changeId, state.snapshot(), target, selection);
        var notificationFacts=cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateFacts.parse(target.factsJson());
        businessEventNotifier.notifyChangeApprovalRecorded(
                command.tenantId(), state.ownerCompanyId(), command.certificateId(), state.versionId(),
                command.actorId(), command.idempotencyKey(), target.productName(), notificationFacts.getCertificateNo(),
                notificationFacts.getEffectiveDate(), notificationFacts.getExpiryDate());
        return new DccRegistrationCertificateChangeResult(command.certificateId(), changeId,
                state.snapshotId(), resultingSnapshotId, "APPLIED");
    }

    /**
     * 保存变更申请事实并进入现有注册证审批流程；当前证件显示信息必须等审批通过后再更新。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long submitChangeForApproval(DccRegistrationCertificateChangeCommand command) {
        validateBaseCommand(command);
        var retained = retainedMaterials(command);
        ChangeUploadFile uploadFile = primaryUpload(command,retained);
        String payloadHash = payloadHash("CHANGE_SUBMIT", command, uploadFile);
        ExistingEvent existing = findExistingEvent(command.tenantId(), command.idempotencyKey());
        if (existing != null) {
            return replayChangeSubmission(existing, payloadHash);
        }
        ChangeSelection selection = validateSelection(command);
        CertificateState state = requireCurrentState(command.tenantId(), command.certificateId(),
                command.expectedRowVersion());
        validateFactsReplacement(state.snapshot(), command.structuredValues());
        ensureNoOpenChangeApproval(command.tenantId(), command.certificateId());
        Long eventId = insertLifecycleEvent(command, state, state.snapshotId(), EVENT_CHANGE_SUBMITTED,
                payloadHash, selection.itemTypes());
        Long changeId = insertPendingChange(command, state, eventId, selection.itemTypes());
        Long infraFileId = fileService.createFileAndReturnId(
                uploadFile.content(), uploadFile.originalName(),
                "dcc/registration-certificate/change/" + command.certificateId(), uploadFile.mimeType());
        Long businessFileId = insertChangeFile(command, changeId, infraFileId, uploadFile);
        projectCodeFileAssociationService.bindChangeApprovalFile(
                command.tenantId(), null, changeId, businessFileId, command.actorId());
        SnapshotRow projected = state.snapshot().withChanges(selection.structuredValues(), command);
        insertChangeItems(command, command.tenantId(), changeId, state.snapshot(), projected, selection);
        Long requestId = insertApprovalRequest(command, state, changeId, payloadHash);
        requireSingle(jdbcTemplate.update("""
                UPDATE dcc_registration_certificate_change
                   SET approval_request_id = ?
                 WHERE id = ? AND tenant_id = ? AND status = ? AND approval_request_id IS NULL
                """, requestId, changeId, command.tenantId(), STATUS_PENDING_APPROVAL),
                REGISTRATION_CERTIFICATE_CHANGE_HISTORY_CONFLICT);
        insertApprovalRequestFile(command.tenantId(), requestId, businessFileId, payloadHash);
        insertAdditionalChangeFiles(command, changeId, requestId, additionalUploads(command.attachments(),retained,"CERTIFICATE_ATTACHMENT"), "CERTIFICATE_ATTACHMENT", payloadHash);
        insertAdditionalChangeFiles(command, changeId, requestId, additionalUploads(command.productDescriptionFiles(),retained,"FDA_PRODUCT_DESCRIPTION"), "FDA_PRODUCT_DESCRIPTION", payloadHash);
        projectCodeFileAssociationService.bindAdditionalChangeMaterials(command.tenantId(),changeId,command.actorId());
        return requestId;
    }

    @Transactional(rollbackFor = Exception.class)
    public DccRegistrationCertificateChangeResult approveChangeRequest(
            Long tenantId, Long approverId, Long requestId, String approvalKey) {
        return approveChangeRequest(tenantId, approverId, requestId, approvalKey, businessClock.now());
    }

    @Transactional(rollbackFor = Exception.class)
    public DccRegistrationCertificateChangeResult approveChangeRequest(
            Long tenantId, Long approverId, Long requestId, String approvalKey, LocalDateTime reviewedAt) {
        ChangeApplication application = requirePendingChangeApplication(tenantId, requestId, REQUEST_STATUS_APPROVED);
        if (isBlank(approvalKey)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_IDEMPOTENCY_KEY_REQUIRED);
        }
        CertificateState state = requireCurrentState(tenantId, application.certificateId(), application.baselineRowVersion());
        if (!Objects.equals(state.snapshotId(), application.sourceSnapshotId())
                || !Objects.equals(state.versionId(), application.sourceVersionId())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_REVISION_CONFLICT);
        }
        ApprovedProjection projection = loadApprovedProjection(tenantId, application.changeId());
        validateFactsReplacement(state.snapshot(), projection.structuredValues());
        Long resultingSnapshotId = state.snapshotId();
        String notificationProductName = state.snapshot().productName();
        var notificationFacts=cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateFacts.parse(state.snapshot().factsJson());
        if (!projection.structuredValues().isEmpty()) {
            SnapshotRow target = state.snapshot().withApprovedChanges(projection);
            resultingSnapshotId = insertSnapshot(tenantId, state.versionId(), target);
            notificationProductName = target.productName();
            notificationFacts=cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateFacts.parse(target.factsJson());
            requireSingle(jdbcTemplate.update("""
                    UPDATE dcc_registration_certificate
                       SET current_snapshot_id = ?, row_version = row_version + 1
                     WHERE id = ? AND tenant_id = ? AND status = ?
                       AND current_version_id = ? AND current_snapshot_id = ?
                    """, resultingSnapshotId, application.certificateId(), tenantId, STATUS_ACTIVE,
                    state.versionId(), state.snapshotId()), REGISTRATION_CERTIFICATE_REVISION_CONFLICT);
        }
        LocalDateTime effectiveReviewedAt = reviewedAt == null ? businessClock.now() : reviewedAt;
        requireSingle(jdbcTemplate.update("""
                UPDATE dcc_registration_certificate_change
                   SET resulting_snapshot_id = ?, status = ?, reviewer_user_id = ?, reviewed_at = ?, applied_at = ?
                 WHERE id = ? AND tenant_id = ? AND status = ? AND approval_request_id = ?
                """, resultingSnapshotId, STATUS_APPLIED, approverId, effectiveReviewedAt, effectiveReviewedAt,
                application.changeId(), tenantId, STATUS_PENDING_APPROVAL, requestId),
                REGISTRATION_CERTIFICATE_CHANGE_HISTORY_CONFLICT);
        updateRequestFileStatuses(tenantId,requestId,"APPROVED");
        businessEventNotifier.notifyChangeApprovalRecorded(
                tenantId, state.ownerCompanyId(), application.certificateId(), state.versionId(),
                approverId, approvalKey.trim(), notificationProductName, notificationFacts.getCertificateNo(),
                notificationFacts.getEffectiveDate(), notificationFacts.getExpiryDate());
        productCatalogRegistrationSyncService.syncByRegistrationCertificateId(application.certificateId());
        return new DccRegistrationCertificateChangeResult(application.certificateId(), application.changeId(),
                state.snapshotId(), resultingSnapshotId, STATUS_APPLIED);
    }

    @Transactional(rollbackFor = Exception.class)
    public void rejectChangeRequest(Long tenantId, Long approverId, Long requestId) {
        rejectChangeRequest(tenantId, approverId, requestId, businessClock.now());
    }

    @Transactional(rollbackFor = Exception.class)
    public void rejectChangeRequest(Long tenantId, Long approverId, Long requestId, LocalDateTime reviewedAt) {
        ChangeApplication application = requirePendingChangeApplication(tenantId, requestId, STATUS_REJECTED);
        LocalDateTime effectiveReviewedAt = reviewedAt == null ? businessClock.now() : reviewedAt;
        requireSingle(jdbcTemplate.update("""
                UPDATE dcc_registration_certificate_change
                   SET status = ?, reviewer_user_id = ?, reviewed_at = ?
                 WHERE id = ? AND tenant_id = ? AND status = ? AND approval_request_id = ?
                """, STATUS_REJECTED, approverId, effectiveReviewedAt, application.changeId(), tenantId,
                STATUS_PENDING_APPROVAL, requestId), REGISTRATION_CERTIFICATE_CHANGE_HISTORY_CONFLICT);
        updateRequestFileStatuses(tenantId,requestId,"REJECTED");
    }

    @Transactional(rollbackFor = Exception.class)
    public DccRegistrationCertificateChangeResult voidCertificate(DccRegistrationCertificateChangeCommand command) {
        validateBaseCommand(command);
        if (isBlank(command.voidReason())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_TOP_LEVEL_VOID_REASON_REQUIRED);
        }
        String payloadHash = payloadHash("VOID", command, null);
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

    private void validateFactsReplacement(SnapshotRow before,Map<String,String> changes) {
        if(changes==null || !changes.containsKey("CERTIFICATE_INFORMATION")) return;
        var existing=cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateFacts.parse(before.factsJson());
        var replacement=cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateFacts.parse(changes.get("CERTIFICATE_INFORMATION"));
        replacement.validate();
        if(!Objects.equals(existing.getCertificateType(),replacement.getCertificateType())
                || !Objects.equals(existing.getOwnProduct(),replacement.getOwnProduct()))
            throw new ServiceException(REGISTRATION_CERTIFICATE_CHANGE_TYPE_INVALID);
        Map<?,?> oldValues=JsonUtils.parseObject(before.factsJson(),Map.class);
        Map<?,?> newValues=JsonUtils.parseObject(changes.get("CERTIFICATE_INFORMATION"),Map.class);
        for(var entry:oldValues.entrySet()) if(entry.getValue()!=null && !newValues.containsKey(entry.getKey()))
            throw new ServiceException(REGISTRATION_CERTIFICATE_CHANGE_VALUE_REQUIRED);
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
                if ("CERTIFICATE_INFORMATION".equals(entry.getKey())) {
                    var facts = cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateFacts.parse(entry.getValue());
                    facts.validate();
                    if (facts.isFda() && isBlank(facts.getChangeDescription())) throw new ServiceException(REGISTRATION_CERTIFICATE_CHANGE_VALUE_REQUIRED);
                }
                structured.put(entry.getKey(), entry.getValue().trim());
            }
        }
        Set<String> selected = new LinkedHashSet<>();
        if (command.changeTypes() != null) {
            for (String type : command.changeTypes()) {
                String normalized = normalize(type);
                if (isBlank(normalized) || (!STRUCTURED_COLUMNS.containsKey(normalized)
                        && !"OTHER_CONTENT".equals(normalized))) {
                    throw new ServiceException(REGISTRATION_CERTIFICATE_CHANGE_TYPE_INVALID);
                }
                selected.add(normalized);
            }
        }
        if (selected.isEmpty()) {
            selected.addAll(structured.keySet());
            if (!isBlank(command.otherDescription())) {
                selected.add("OTHER_CONTENT");
            }
        }
        if (selected.isEmpty()) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_CHANGE_TYPE_INVALID);
        }
        for (String type : structured.keySet()) {
            if (!selected.contains(type)) {
                throw new ServiceException(REGISTRATION_CERTIFICATE_CHANGE_TYPE_INVALID);
            }
        }
        for (String type : selected) {
            if (STRUCTURED_COLUMNS.containsKey(type) && isBlank(structured.get(type))) {
                throw new ServiceException(REGISTRATION_CERTIFICATE_CHANGE_VALUE_REQUIRED);
            }
        }
        if ((selected.contains("PRODUCTION_ADDRESS") || selected.contains("PRODUCTION_RELATION"))) {
            validateProductionRelation(command);
        }
        boolean hasOther = selected.contains("OTHER_CONTENT");
        if (hasOther && isBlank(command.otherDescription())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_CHANGE_VALUE_REQUIRED);
        }
        if (!hasOther && !isBlank(command.otherDescription())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_CHANGE_TYPE_INVALID);
        }
        return new ChangeSelection(structured, hasOther ? command.otherDescription().trim() : null,
                new ArrayList<>(selected));
    }

    private ChangeSelection validateImmediateSelection(DccRegistrationCertificateChangeCommand command) {
        Map<String, String> structured = new LinkedHashMap<>();
        if (command.structuredValues() != null) {
            for (Map.Entry<String, String> entry : command.structuredValues().entrySet()) {
                if (!STRUCTURED_COLUMNS.containsKey(entry.getKey())) {
                    throw new ServiceException(REGISTRATION_CERTIFICATE_CHANGE_TYPE_INVALID);
                }
                if (isBlank(entry.getValue())) {
                    throw new ServiceException(REGISTRATION_CERTIFICATE_CHANGE_VALUE_REQUIRED);
                }
                if ("CERTIFICATE_INFORMATION".equals(entry.getKey())) {
                    var facts = cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateFacts.parse(entry.getValue());
                    facts.validate();
                    if (facts.isFda() && isBlank(facts.getChangeDescription())) throw new ServiceException(REGISTRATION_CERTIFICATE_CHANGE_VALUE_REQUIRED);
                }
                structured.put(entry.getKey(), entry.getValue().trim());
            }
        }
        boolean hasOther = !isBlank(command.otherDescription());
        if (!hasOther && structured.isEmpty()) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_CHANGE_TYPE_INVALID);
        }
        if ((structured.containsKey("PRODUCTION_ADDRESS") || structured.containsKey("PRODUCTION_RELATION"))) {
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
                               v.status AS version_status, v.certificate_no, v.classification,
                               v.effective_date, v.expiry_date,
                               s.id AS snapshot_id, s.version_id, s.revision_no, s.product_name, s.registrant_name,
                               s.model_specification, s.structure_composition, s.intended_use,
                               s.technical_requirements, s.residence_address, s.production_address,
                               s.entrusted_production, s.self_production, s.entrusted_enterprises_json, s.facts_json, s.effective_at
                          FROM dcc_registration_certificate c
                          JOIN dcc_registration_certificate_version v
                            ON v.id = c.current_version_id AND v.tenant_id = c.tenant_id
                          JOIN dcc_registration_certificate_snapshot s
                            ON s.id = c.current_snapshot_id AND s.tenant_id = c.tenant_id
                         WHERE c.tenant_id = ? AND c.id = ?
                         FOR UPDATE
                        """, (rs, rowNum) -> new CertificateState(
                        rs.getLong("owner_company_id"),
                        rs.getLong("current_version_id"),
                        rs.getLong("current_snapshot_id"),
                        rs.getString("status"),
                        rs.getInt("row_version"),
                        rs.getString("version_status"),
                        rs.getString("certificate_no"),
                        rs.getString("classification"),
                        rs.getObject("effective_date", LocalDate.class),
                        rs.getObject("expiry_date", LocalDate.class),
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
                                rs.getString("facts_json"),
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

    private void ensureNoOpenChangeApproval(Long tenantId, Long certificateId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM dcc_registration_certificate_change
                 WHERE tenant_id = ?
                   AND certificate_id = ?
                   AND status = ?
                   AND deleted = 0
                """, Integer.class, tenantId, certificateId, STATUS_PENDING_APPROVAL);
        if (count != null && count > 0) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_CHANGE_PENDING_CONFLICT);
        }
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

    private void updateRequestFileStatuses(Long tenantId,Long requestId,String status) {
        jdbcTemplate.update("UPDATE dcc_registration_certificate_access_request_file SET status=? WHERE tenant_id=? AND request_id=? AND deleted=0",status,tenantId,requestId);
    }

    private void insertAdditionalChangeFiles(DccRegistrationCertificateChangeCommand command,Long changeId,Long requestId,
            List<ChangeUploadFile> files,String kind,String payloadHash) {
        if(files==null)return;
        for(var checked:files) {
            Long infra=fileService.createFileAndReturnId(checked.content(),checked.originalName(),"dcc/registration-certificate/change/"+command.certificateId(),checked.mimeType());
            Long fileId=insertAndReturnId("""
                INSERT INTO dcc_registration_certificate_file
                 (tenant_id,owner_type,owner_id,file_kind,infra_file_id,original_name,mime_type,file_size,sha256,status,bound_at,bound_by,creator)
                VALUES (?,'CHANGE',?,?,?,?,?,?,?,'BOUND',?,?,?)
                """,ps->{ps.setLong(1,command.tenantId());ps.setLong(2,changeId);ps.setString(3,kind);ps.setLong(4,infra);
                    ps.setString(5,checked.originalName());ps.setString(6,checked.mimeType());ps.setLong(7,checked.fileSize());ps.setString(8,checked.sha256());
                    ps.setObject(9,businessClock.now());ps.setLong(10,command.actorId());ps.setString(11,String.valueOf(command.actorId()));});
            requireSingle(jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate_access_request_file
                  (tenant_id,request_id,business_file_id,file_kind,download_requested,status,detail_json)
                VALUES (?,?,?,?,FALSE,'REQUESTED',?)
                """,command.tenantId(),requestId,fileId,kind,JsonUtils.toJsonString(Map.of("payloadHash",payloadHash))),REGISTRATION_CERTIFICATE_FILE_CONFLICT);
        }
    }

    private Long insertChangeFile(DccRegistrationCertificateChangeCommand command, Long changeId,
                                   Long infraFileId, ChangeUploadFile uploadFile) {
        if (infraFileId == null || infraFileId <= 0) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_CONFLICT);
        }
        try {
            return insertAndReturnId("""
                    INSERT INTO dcc_registration_certificate_file
                      (tenant_id, owner_type, owner_id, file_kind, infra_file_id, original_name, mime_type,
                       file_size, sha256, status, bound_at, bound_by, creator)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, ps -> {
                ps.setLong(1, command.tenantId());
                ps.setString(2, FILE_OWNER_CHANGE);
                ps.setLong(3, changeId);
                ps.setString(4, FILE_KIND_CHANGE_APPROVAL);
                ps.setLong(5, infraFileId);
                ps.setString(6, uploadFile.originalName());
                ps.setString(7, uploadFile.mimeType());
                ps.setLong(8, uploadFile.fileSize());
                ps.setString(9, uploadFile.sha256());
                ps.setString(10, FILE_STATUS_BOUND);
                ps.setObject(11, businessClock.now());
                ps.setLong(12, command.actorId());
                ps.setString(13, String.valueOf(command.actorId()));
            });
        } catch (DuplicateKeyException exception) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_CONFLICT);
        }
    }

    private Long insertSnapshot(Long tenantId, Long versionId, SnapshotRow snapshot) {
        return insertAndReturnId("""
                INSERT INTO dcc_registration_certificate_snapshot
                  (tenant_id, version_id, revision_no, product_name, registrant_name,
                   model_specification, structure_composition, intended_use, technical_requirements,
                   residence_address, production_address, entrusted_production, self_production,
                   entrusted_enterprises_json, facts_json, effective_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
            ps.setString(15, snapshot.factsJson());
            ps.setObject(16, businessClock.now());
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
                   status, actor_id, applied_at, creator)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'APPLIED', ?, ?, ?)
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
            ps.setLong(10, command.actorId());
            ps.setObject(11, businessClock.now());
            ps.setString(12, String.valueOf(command.actorId()));
        });
    }

    private Long insertPendingChange(DccRegistrationCertificateChangeCommand command, CertificateState state,
                                     Long eventId, List<String> itemTypes) {
        return insertAndReturnId("""
                INSERT INTO dcc_registration_certificate_change
                  (tenant_id, owner_company_id, certificate_id, source_version_id, source_snapshot_id,
                   resulting_snapshot_id, event_id, approval_date, selected_change_types_json,
                   status, actor_id, applied_at, creator)
                VALUES (?, ?, ?, ?, ?, NULL, ?, ?, ?, ?, ?, ?, ?)
                """, ps -> {
            ps.setLong(1, command.tenantId());
            ps.setLong(2, state.ownerCompanyId());
            ps.setLong(3, command.certificateId());
            ps.setLong(4, state.versionId());
            ps.setLong(5, state.snapshotId());
            ps.setLong(6, eventId);
            ps.setObject(7, command.approvalDate());
            ps.setString(8, JsonUtils.toJsonString(itemTypes));
            ps.setString(9, STATUS_PENDING_APPROVAL);
            ps.setLong(10, command.actorId());
            ps.setNull(11, java.sql.Types.TIMESTAMP);
            ps.setString(12, String.valueOf(command.actorId()));
        });
    }

    private Long insertApprovalRequest(DccRegistrationCertificateChangeCommand command, CertificateState state,
                                       Long changeId, String payloadHash) {
        String ownerCompanyName = resolveOwnerCompanyName(command.tenantId(), state.ownerCompanyId());
        return insertAndReturnId("""
                INSERT INTO dcc_registration_certificate_access_request
                  (tenant_id, owner_company_id, certificate_id, requester_user_id, request_type, request_key,
                   purpose, status, requested_at, detail_json, creator)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, ps -> {
            ps.setLong(1, command.tenantId());
            ps.setLong(2, state.ownerCompanyId());
            ps.setLong(3, command.certificateId());
            ps.setLong(4, command.actorId());
            ps.setString(5, REQUEST_TYPE_UPLOAD_CERTIFICATE);
            ps.setString(6, command.idempotencyKey().trim());
            ps.setString(7, "上传注册证变更批件，待审批");
            ps.setString(8, REQUEST_STATUS_SUBMITTED);
            ps.setObject(9, businessClock.now());
            Map<String,Object> detail=new LinkedHashMap<>();
            detail.put("operation",REQUEST_OPERATION_CHANGE_CERTIFICATE);detail.put("productName",state.snapshot().productName());
            var projectedFacts=cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateFacts.parse(command.structuredValues()!=null?command.structuredValues().getOrDefault("CERTIFICATE_INFORMATION",state.snapshot().factsJson()):state.snapshot().factsJson());
            detail.put("certificateNo",projectedFacts.getCertificateNo());
            detail.put("classification",projectedFacts.getClassification());
            detail.put("ownerCompanyName",ownerCompanyName);detail.put("changeId",changeId);detail.put("payloadHash",payloadHash);
            detail.put("factsJson",command.structuredValues()!=null?command.structuredValues().getOrDefault("CERTIFICATE_INFORMATION",state.snapshot().factsJson()):state.snapshot().factsJson());
            detail.put("draft",state.snapshot().withChanges(command.structuredValues()==null?Map.of():command.structuredValues(),command));
            detail.put("sourceRequestId",command.sourceRequestId());
            detail.put("structuredValues",command.structuredValues());detail.put("approvalDate",command.approvalDate().toString());
            detail.put("otherDescription",command.otherDescription());detail.put("entrustedProduction",command.entrustedProduction());
            detail.put("selfProduction",command.selfProduction());detail.put("entrustedEnterprisesJson",command.entrustedEnterprisesJson());
            ps.setString(10,JsonUtils.toJsonString(detail));
            ps.setString(11, String.valueOf(command.actorId()));
        });
    }

    private String resolveOwnerCompanyName(Long tenantId, Long ownerCompanyId) {
        if (ownerCompanyId == null || ownerCompanyId <= 0) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_COMPANY_SCOPE_DENIED);
        }
        List<String> names = jdbcTemplate.queryForList("""
                SELECT name
                  FROM mdm_enterprise
                 WHERE tenant_id = ? AND id = ? AND type = 'OWNED_COMPANY'
                   AND status = 'ENABLE' AND deleted = 0
                """, String.class, tenantId, ownerCompanyId);
        if (names.size() != 1 || isBlank(names.get(0))) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_COMPANY_SCOPE_DENIED);
        }
        return names.get(0).trim();
    }

    private static String requireSummaryText(String value) {
        if (isBlank(value)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_COMPANY_SCOPE_DENIED);
        }
        return value.trim();
    }

    private void insertApprovalRequestFile(Long tenantId, Long requestId, Long businessFileId, String payloadHash) {
        requireSingle(jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate_access_request_file
                  (tenant_id, request_id, business_file_id, file_kind, download_requested, status, detail_json)
                VALUES (?, ?, ?, ?, FALSE, ?, ?)
                """, tenantId, requestId, businessFileId, FILE_KIND_CHANGE_APPROVAL,
                REQUEST_FILE_STATUS_REQUESTED, JsonUtils.toJsonString(Map.of("payloadHash", payloadHash))),
                REGISTRATION_CERTIFICATE_CHANGE_HISTORY_CONFLICT);
    }

    private ChangeApplication requirePendingChangeApplication(
            Long tenantId, Long requestId, String expectedRequestStatus) {
        if (tenantId == null || tenantId <= 0 || requestId == null || requestId <= 0) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_CHANGE_HISTORY_CONFLICT);
        }
        List<ChangeApplication> applications = jdbcTemplate.query("""
                SELECT c.id AS change_id, c.certificate_id, c.source_version_id, c.source_snapshot_id,
                       c.status AS change_status, r.status AS request_status, e.baseline_row_version
                  FROM dcc_registration_certificate_change c
                  JOIN dcc_registration_certificate_access_request r
                    ON r.id = c.approval_request_id AND r.tenant_id = c.tenant_id AND r.deleted = 0
                  JOIN dcc_registration_certificate_lifecycle_event e
                    ON e.id = c.event_id AND e.tenant_id = c.tenant_id
                 WHERE c.tenant_id = ? AND c.approval_request_id = ? AND c.deleted = 0
                """, (rs, rowNum) -> new ChangeApplication(
                rs.getLong("change_id"), rs.getLong("certificate_id"), rs.getLong("source_version_id"),
                rs.getLong("source_snapshot_id"), rs.getString("change_status"),
                rs.getString("request_status"), rs.getObject("baseline_row_version", Integer.class)), tenantId, requestId);
        if (applications.size() != 1) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_CHANGE_HISTORY_CONFLICT);
        }
        ChangeApplication application = applications.get(0);
        if (!STATUS_PENDING_APPROVAL.equals(application.changeStatus())
                || !expectedRequestStatus.equals(application.requestStatus())
                || application.baselineRowVersion() == null || application.baselineRowVersion() <= 0) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_CHANGE_HISTORY_CONFLICT);
        }
        return application;
    }

    private ApprovedProjection loadApprovedProjection(Long tenantId, Long changeId) {
        List<Map.Entry<String, String>> rows = jdbcTemplate.query("""
                SELECT item_type, after_value_json
                  FROM dcc_registration_certificate_change_item
                 WHERE tenant_id = ? AND change_id = ?
                   AND item_type <> 'OTHER_CONTENT'
                 ORDER BY sort_order ASC, id ASC
                """, (rs, rowNum) -> Map.entry(rs.getString("item_type"), rs.getString("after_value_json")),
                tenantId, changeId);
        Map<String, String> values = new LinkedHashMap<>();
        Boolean entrustedProduction = null;
        Boolean selfProduction = null;
        String entrustedEnterprisesJson = null;
        for (Map.Entry<String, String> row : rows) {
            if (!STRUCTURED_COLUMNS.containsKey(row.getKey())) {
                throw new ServiceException(REGISTRATION_CERTIFICATE_CHANGE_TYPE_INVALID);
            }
            Map<?, ?> parsed = JsonUtils.parseObject(row.getValue(), Map.class);
            String value = parsed == null || parsed.get("value") == null
                    ? null : String.valueOf(parsed.get("value")).trim();
            if (isBlank(value)) {
                throw new ServiceException(REGISTRATION_CERTIFICATE_CHANGE_VALUE_REQUIRED);
            }
            values.put(row.getKey(), value);
            if (("PRODUCTION_ADDRESS".equals(row.getKey()) || "PRODUCTION_RELATION".equals(row.getKey()))) {
                entrustedProduction = parsed.get("entrustedProduction") instanceof Boolean valueFlag
                        ? valueFlag : null;
                selfProduction = parsed.get("selfProduction") instanceof Boolean valueFlag
                        ? valueFlag : null;
                entrustedEnterprisesJson = parsed.get("entrustedEnterprisesJson") == null
                        ? null : String.valueOf(parsed.get("entrustedEnterprisesJson")).trim();
                if (entrustedProduction == null || selfProduction == null
                        || isBlank(entrustedEnterprisesJson)) {
                    throw new ServiceException(REGISTRATION_CERTIFICATE_CHANGE_PRODUCTION_RELATION_REQUIRED);
                }
            }
        }
        return new ApprovedProjection(values, entrustedProduction, selfProduction, entrustedEnterprisesJson);
    }

    private void insertChangeItems(DccRegistrationCertificateChangeCommand command, Long tenantId, Long changeId,
                                   SnapshotRow before,
                                   SnapshotRow after, ChangeSelection selection) {
        int sort = 1;
        for (String itemType : selection.itemTypes()) {
            String beforeJson = valueJson(before.valueOf(itemType));
            String afterJson = afterValueJson(command, itemType, after, selection);
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

    private static String afterValueJson(DccRegistrationCertificateChangeCommand command, String itemType,
                                         SnapshotRow after, ChangeSelection selection) {
        if ("OTHER_CONTENT".equals(itemType)) {
            return valueJson(selection.otherDescription());
        }
        if (("PRODUCTION_ADDRESS".equals(itemType) || "PRODUCTION_RELATION".equals(itemType))) {
            Map<String, Object> relation = new LinkedHashMap<>();
            relation.put("value", after.valueOf(itemType));
            relation.put("entrustedProduction", command.entrustedProduction());
            relation.put("selfProduction", command.selfProduction());
            relation.put("entrustedEnterprisesJson", normalize(command.entrustedEnterprisesJson()));
            return JsonUtils.toJsonString(relation);
        }
        return valueJson(selection.structuredValues().containsKey(itemType) ? after.valueOf(itemType) : "");
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

    private Long replayChangeSubmission(ExistingEvent event, String payloadHash) {
        ChangeEventDetail detail = JsonUtils.parseObject(event.detailJson(), ChangeEventDetail.class);
        if (!EVENT_CHANGE_SUBMITTED.equals(event.eventType()) || detail == null
                || !Objects.equals(payloadHash, detail.payloadHash())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_IDEMPOTENCY_CONFLICT);
        }
        Long requestId = jdbcTemplate.queryForObject("""
                SELECT approval_request_id
                  FROM dcc_registration_certificate_change
                 WHERE tenant_id = ? AND event_id = ? AND status = ?
                """, Long.class, event.tenantId(), event.eventId(), STATUS_PENDING_APPROVAL);
        if (requestId == null || requestId <= 0) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_CHANGE_HISTORY_CONFLICT);
        }
        return requestId;
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
        Long key = extractGeneratedId(keyHolder);
        return key;
    }

    private static Long extractGeneratedId(KeyHolder keyHolder) {
        List<Map<String, Object>> keyList = keyHolder.getKeyList();
        Map<String, Object> keys = keyList.isEmpty() ? Map.of() : keyList.get(0);
        Object id = keys.get("id");
        Number key = id instanceof Number number ? number : null;
        if (key == null && keys.size() == 1) {
            Object singleValue = keys.values().iterator().next();
            key = singleValue instanceof Number number ? number : null;
        }
        if (key == null) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_CHANGE_HISTORY_CONFLICT);
        }
        return key.longValue();
    }

    private static String valueJson(String value) {
        return JsonUtils.toJsonString(Map.of("value", value == null ? "" : value));
    }

    private record RetainedMaterial(String kind,ChangeUploadFile file) {}

    private List<RetainedMaterial> retainedMaterials(DccRegistrationCertificateChangeCommand command) {
        var ids=command.retainedFileIds()==null?List.<Long>of():command.retainedFileIds();
        if(command.sourceRequestId()==null) {
            if(!ids.isEmpty())throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_CONFLICT);
            return List.of();
        }
        var sources=jdbcTemplate.queryForList("""
            SELECT detail_json FROM dcc_registration_certificate_access_request
             WHERE tenant_id=? AND id=? AND certificate_id=? AND requester_user_id=?
               AND request_type='UPLOAD_CERTIFICATE' AND status='REJECTED' AND deleted=0
            """,String.class,command.tenantId(),command.sourceRequestId(),command.certificateId(),command.actorId());
        if(sources.size()!=1 || !REQUEST_OPERATION_CHANGE_CERTIFICATE.equals(JsonUtils.parseObject(sources.get(0),Map.class).get("operation")))
            throw new ServiceException(REGISTRATION_CERTIFICATE_CHANGE_HISTORY_CONFLICT);
        Integer retried=jdbcTemplate.queryForObject("""
            SELECT COUNT(*) FROM dcc_registration_certificate_access_request
             WHERE tenant_id=? AND certificate_id=? AND id>? AND request_type='UPLOAD_CERTIFICATE'
               AND deleted=0 AND request_key<>? AND JSON_UNQUOTE(JSON_EXTRACT(detail_json,'$.sourceRequestId'))=?
            """,Integer.class,command.tenantId(),command.certificateId(),command.sourceRequestId(),command.idempotencyKey().trim(),String.valueOf(command.sourceRequestId()));
        if(retried==null || retried>0 || new java.util.HashSet<>(ids).size()!=ids.size())
            throw new ServiceException(REGISTRATION_CERTIFICATE_CHANGE_HISTORY_CONFLICT);
        List<RetainedMaterial> result=new ArrayList<>();
        for(Long id:ids) {
            var rows=jdbcTemplate.queryForList("""
                SELECT f.infra_file_id AS infraId,f.file_kind AS kind,f.original_name AS name,f.mime_type AS mime,f.sha256
                 FROM dcc_registration_certificate_access_request_file rf
                 JOIN dcc_registration_certificate_file f ON f.id=rf.business_file_id AND f.tenant_id=rf.tenant_id AND f.deleted=0
                 WHERE rf.tenant_id=? AND rf.request_id=? AND rf.business_file_id=? AND rf.deleted=0
                   AND rf.status='REJECTED' AND f.owner_type='CHANGE' AND f.status='BOUND'
                """,command.tenantId(),command.sourceRequestId(),id);
            if(rows.size()!=1)throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_CONFLICT);
            var row=rows.get(0);String kind=String.valueOf(row.get("kind"));
            if(!Set.of("CHANGE_APPROVAL","CERTIFICATE_ATTACHMENT","FDA_PRODUCT_DESCRIPTION").contains(kind))
                throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_CONFLICT);
            var infra=fileService.getFile(((Number)row.get("infraId")).longValue());
            if(infra==null)throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_CONFLICT);
            byte[] content;
            try {content=fileService.getFileContent(infra.getConfigId(),infra.getPath());}
            catch(Exception ex){throw new IllegalStateException("读取原驳回申请文件失败: "+id,ex);}
            if(content==null || content.length==0 || !sha256(content).equals(row.get("sha256")))
                throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_CONFLICT);
            result.add(new RetainedMaterial(kind,new ChangeUploadFile(String.valueOf(row.get("name")),String.valueOf(row.get("mime")),content.length,content,sha256(content))));
        }
        return result;
    }

    private static ChangeUploadFile primaryUpload(DccRegistrationCertificateChangeCommand command,List<RetainedMaterial> retained) {
        if(command.file()!=null && !command.file().isEmpty())return requireUploadFile(command.file());
        var mains=retained.stream().filter(r->"CHANGE_APPROVAL".equals(r.kind())).toList();
        if(mains.size()!=1)throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_REQUIRED);
        return mains.get(0).file();
    }

    private static List<ChangeUploadFile> additionalUploads(List<MultipartFile> uploaded,List<RetainedMaterial> retained,String kind) {
        List<ChangeUploadFile> result=new ArrayList<>();
        if(uploaded!=null)uploaded.forEach(f->result.add(requireUploadFile(f)));
        retained.stream().filter(r->kind.equals(r.kind())).forEach(r->result.add(r.file()));
        return result;
    }

    private static ChangeUploadFile requireUploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_REQUIRED);
        }
        String originalName = normalize(file.getOriginalFilename());
        String mimeType = normalize(file.getContentType());
        if (isBlank(originalName) || isBlank(mimeType) || file.getSize() <= 0) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_REQUIRED);
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException exception) {
            ServiceException mapped = new ServiceException(REGISTRATION_CERTIFICATE_FILE_CONFLICT);
            mapped.initCause(exception);
            throw mapped;
        }
        if (content.length == 0 || content.length != file.getSize()) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_CONFLICT);
        }
        return new ChangeUploadFile(originalName, mimeType, file.getSize(), content, sha256(content));
    }

    private static String payloadHash(String prefix, DccRegistrationCertificateChangeCommand command,
                                      ChangeUploadFile uploadFile) {
        return sha256(prefix + "|" + command.certificateId() + "|" + command.expectedRowVersion()
                + "|" + command.approvalDate() + "|" + normalize(command.changeTypes())
                + "|" + normalize(command.structuredValues())
                + "|source=" + command.sourceRequestId() + "|retained=" + command.retainedFileIds()
                + "|" + normalize(command.otherDescription()) + "|" + command.entrustedProduction()
                + "|" + command.selfProduction() + "|" + normalize(command.entrustedEnterprisesJson())
                + "|" + normalize(command.voidReason()) + "|" + uploadFilePart(uploadFile)
                + "|" + additionalFileParts(command.attachments()) + "|" + additionalFileParts(command.productDescriptionFiles()));
    }

    private static String additionalFileParts(List<MultipartFile> files) {
        return files==null?"":files.stream().map(DccRegistrationCertificateChangeService::requireUploadFile)
                .map(DccRegistrationCertificateChangeService::uploadFilePart).toList().toString();
    }

    private static String uploadFilePart(ChangeUploadFile uploadFile) {
        if (uploadFile == null) {
            return "";
        }
        return uploadFile.originalName() + "|" + uploadFile.mimeType() + "|"
                + uploadFile.fileSize() + "|" + uploadFile.sha256();
    }

    private static String normalize(Map<String, String> values) {
        return values == null ? "" : new java.util.TreeMap<>(values).toString();
    }

    private static String normalize(List<String> values) {
        return values == null ? "" : values.stream().map(DccRegistrationCertificateChangeService::normalize)
                .sorted().toList().toString();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 算法不可用", exception);
        }
    }

    private static String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 算法不可用", exception);
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

    @FunctionalInterface
    private interface SqlBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }

    private record ChangeSelection(Map<String, String> structuredValues, String otherDescription,
                                   List<String> itemTypes) {
    }

    private record ApprovedProjection(Map<String, String> structuredValues,
                                      Boolean entrustedProduction,
                                      Boolean selfProduction,
                                      String entrustedEnterprisesJson) {
    }

    private record ChangeApplication(Long changeId, Long certificateId, Long sourceVersionId,
                                     Long sourceSnapshotId, String changeStatus, String requestStatus,
                                     Integer baselineRowVersion) {
    }

    private record CertificateState(Long ownerCompanyId, Long versionId, Long snapshotId, String status,
                                    Integer rowVersion, String versionStatus, String certificateNo,
                                    String classification,
                                    LocalDate effectiveDate, LocalDate expiryDate,
                                    SnapshotRow snapshot) {
    }

    private record ExistingEvent(Long eventId, Long tenantId, Long certificateId, Long sourceSnapshotId,
                                 Long targetSnapshotId, String eventType, String detailJson) {
    }

    private record ChangeEventDetail(String payloadHash, List<String> itemTypes, String voidReason) {
    }

    private record ChangeUploadFile(
            String originalName,
            String mimeType,
            long fileSize,
            byte[] content,
            String sha256) {
    }

    private record SnapshotRow(Long id, Long versionId, Integer revisionNo, String productName,
                               String registrantName, String modelSpecification, String structureComposition,
                               String intendedUse, String technicalRequirements, String residenceAddress,
                               String productionAddress, Boolean entrustedProduction, Boolean selfProduction,
                               String entrustedEnterprisesJson, String factsJson, LocalDateTime effectiveAt) {

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
                    (changes.containsKey("PRODUCTION_ADDRESS") || changes.containsKey("PRODUCTION_RELATION")) ? command.entrustedProduction() : entrustedProduction,
                    (changes.containsKey("PRODUCTION_ADDRESS") || changes.containsKey("PRODUCTION_RELATION")) ? command.selfProduction() : selfProduction,
                    (changes.containsKey("PRODUCTION_ADDRESS") || changes.containsKey("PRODUCTION_RELATION")) ? normalizeEntrustedJson(command) : entrustedEnterprisesJson,
                    changes.getOrDefault("CERTIFICATE_INFORMATION", factsJson), effectiveAt);
        }

        SnapshotRow withApprovedChanges(ApprovedProjection projection) {
            Map<String, String> changes = projection.structuredValues();
            boolean productionAddressChanged = (changes.containsKey("PRODUCTION_ADDRESS") || changes.containsKey("PRODUCTION_RELATION"));
            return new SnapshotRow(id, versionId, revisionNo,
                    changes.getOrDefault("PRODUCT_NAME", productName),
                    changes.getOrDefault("REGISTRANT_NAME", registrantName),
                    changes.getOrDefault("MODEL_SPECIFICATION", modelSpecification),
                    changes.getOrDefault("STRUCTURE_COMPOSITION", structureComposition),
                    changes.getOrDefault("INTENDED_USE", intendedUse),
                    changes.getOrDefault("TECHNICAL_REQUIREMENTS", technicalRequirements),
                    changes.getOrDefault("RESIDENCE_ADDRESS", residenceAddress),
                    changes.getOrDefault("PRODUCTION_ADDRESS", productionAddress),
                    productionAddressChanged ? projection.entrustedProduction() : entrustedProduction,
                    productionAddressChanged ? projection.selfProduction() : selfProduction,
                    productionAddressChanged ? projection.entrustedEnterprisesJson() : entrustedEnterprisesJson,
                    changes.getOrDefault("CERTIFICATE_INFORMATION", factsJson), effectiveAt);
        }

        String valueOf(String itemType) {
            return switch (itemType) {
                case "CERTIFICATE_INFORMATION" -> factsJson;
                case "PRODUCTION_RELATION" -> JsonUtils.toJsonString(java.util.Arrays.asList(entrustedProduction,selfProduction,entrustedEnterprisesJson));
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
