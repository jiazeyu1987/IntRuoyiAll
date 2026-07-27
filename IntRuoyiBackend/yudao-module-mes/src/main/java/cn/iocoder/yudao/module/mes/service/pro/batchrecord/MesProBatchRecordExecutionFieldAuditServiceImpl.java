package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldAuditBatchRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldAuditDetailReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldAuditDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldAuditExportReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldAuditExportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldAuditHashVerificationRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldAuditItemRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldAuditPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldAuditSignatureRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldAuditVerifyReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldAuditVerifyRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldResponsibilityExportReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldResponsibilityExportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldResponsibilityHistoryReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldResponsibilityHistoryRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldResponsibilitySummaryReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldResponsibilitySummaryRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionAttachmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionFieldAuditBatchDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionFieldAuditItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionSignatureDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionFieldAuditBatchMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionAttachmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionFieldAuditItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionSignatureMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DecimalNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import jakarta.annotation.Resource;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception0;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_BASELINE_MISSING;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_CHAIN_CONFLICT;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_CHAIN_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_EXPORT_FAILED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_FIELD_NOT_DECLARED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_IDEMPOTENCY_CONFLICT;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_OLD_VALUE_MISMATCH;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_PERSIST_FAILED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_REASON_CATEGORY_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_REASON_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_SIGNATURE_CELL_VALUE_FORBIDDEN;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_SIGNATURE_BIND_FAILED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_VALUE_CONSTRAINT_VIOLATION;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_VALUE_TYPE_UNSUPPORTED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_ATTACHMENT_FILE_METADATA_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_STATUS_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_WRITE_TASK_INVALID;

@Service
public class MesProBatchRecordExecutionFieldAuditServiceImpl implements MesProBatchRecordExecutionFieldAuditService {

    private static final int EXECUTION_STATUS_DRAFT = 0;
    private static final int EXECUTION_STATUS_FILL_COMPLETED = MesProEdhrApprovalStatusMapping.EXECUTION_STATUS_FILL_COMPLETED;
    private static final String ACTION_FIELD_CHANGE = "FIELD_CHANGE";
    private static final String VALIDATION_PROFILE_INTERNAL = "INTERNAL_TRACE";
    private static final String FILL_CARRIER_RECORDBOOK = "RECORDBOOK";
    private static final String FILL_MODE_RECORDBOOK_UNRESTRICTED = "RECORDBOOK_UNRESTRICTED";
    private static final String INSTANCE_SCOPE_BATCH_SHARED = "BATCH_SHARED";
    private static final String ENTITLEMENT_SOURCE_TYPE_FILLER = "EDHR_PROCESS_FORM_FILLER";
    private static final DateTimeFormatter DEFAULT_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DEFAULT_DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String FIELD_AUDIT_PDF_FONT_PATH = "C:/Windows/Fonts/simhei.ttf";
    private static final String FIELD_AUDIT_SYSTEM_STATEMENT =
            "本系统电子签名证据；可通过签名 ID 与证据哈希在系统内复核。本系统认证并可校验。";
    private static final Set<String> REASON_CATEGORIES = Set.of(
            "CORRECTION",
            "PROCESS_OBSERVATION",
            "CALCULATION_FIX",
            "OPERATOR_ENTRY",
            "OTHER");

    @Resource
    private MesProBatchRecordExecutionMapper executionMapper;
    @Resource
    private MesProBatchRecordExecutionFieldAuditBatchMapper batchMapper;
    @Resource
    private MesProBatchRecordExecutionFieldAuditItemMapper itemMapper;
    @Resource
    private MesProBatchRecordExecutionAttachmentMapper attachmentMapper;
    @Resource
    private MesProBatchRecordExecutionSignatureService signatureService;
    @Resource
    private MesProBatchRecordExecutionSignatureMapper signatureMapper;
    @Resource
    private MesProEdhrBatchExecutionTaskMapper batchTaskMapper;
    @Resource
    private MesProBatchRecordExecutionAttachmentService attachmentService;
    @Resource
    private MesProEdhrWorkTaskService workTaskService;
    @Resource
    private MesProEdhrPreReleaseEditabilityService preReleaseEditabilityService;
    @Resource
    private MesProEdhrOperationAuditService operationAuditService;
    @Resource
    private MesProBatchRecordExecutionFieldResponsibilityService responsibilityService;
    @Resource
    private MesProEdhrGoldenFingerPermissionService goldenFingerPermissionService;
    @Resource
    private MesProEdhrRecordbookGlobalSettingService recordbookGlobalSettingService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProBatchRecordExecutionFieldAuditSaveResult saveChanges(
            MesProBatchRecordExecutionFieldAuditSaveChangesCommand command) {
        return saveChangesInternal(command, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProBatchRecordExecutionFieldAuditSaveResult saveSystemCellLinkChanges(
            MesProBatchRecordExecutionFieldAuditSaveChangesCommand command) {
        return saveChangesInternal(command, false);
    }

    private MesProBatchRecordExecutionFieldAuditSaveResult saveChangesInternal(
            MesProBatchRecordExecutionFieldAuditSaveChangesCommand command,
            boolean requireWorkTaskValidation) {
        validateCommandShape(command, requireWorkTaskValidation);
        List<MesProBatchRecordExecutionFieldAuditChange> sortedChanges = sortedChanges(command.getChanges());
        command.setChanges(sortedChanges)
                .setReasonText(StrUtil.trim(command.getReasonText()));

        MesProBatchRecordExecutionDO execution = executionMapper.selectByIdForUpdate(command.getExecutionId());
        if (execution == null) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_NOT_EXISTS);
        }
        boolean draftExecution = Objects.equals(execution.getStatus(), EXECUTION_STATUS_DRAFT);
        boolean preReleaseEditableExecution = Objects.equals(execution.getStatus(), EXECUTION_STATUS_FILL_COMPLETED);
        if (!draftExecution && (!requireWorkTaskValidation || !preReleaseEditableExecution)) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_STATUS_INVALID);
        }
        Long currentUserId = currentAuditUserId();
        boolean goldenFingerMode = requireWorkTaskValidation
                && goldenFingerPermissionService.hasGoldenFingerPermission(currentUserId);
        if (requireWorkTaskValidation) {
            if (draftExecution) {
                MesProEdhrWorkTaskDO workTask = goldenFingerMode
                        ? workTaskService.validateGoldenFingerFillTaskForExecution(command.getWorkTaskId(), execution.getId())
                        : workTaskService.validateWritableFillTaskForExecution(command.getWorkTaskId(), execution.getId());
                if (!goldenFingerMode) {
                    validateBatchSharedFillScope(command, execution, workTask);
                }
            } else {
                if (goldenFingerMode) {
                    preReleaseEditabilityService.requireSubmittedOrdinaryGoldenFingerEditable(
                            execution, command.getWorkTaskId());
                } else {
                    preReleaseEditabilityService.requireSubmittedOrdinaryEditable(execution, command.getWorkTaskId());
                }
            }
        }
        validateBaselineAvailable(execution);
        validateBaselineMatches(command, execution);
        if (isRecordbookUnrestrictedMode(command)) {
            if (!Boolean.TRUE.equals(execution.getRecordbookEnabled())) {
                throw exception(PRO_BATCH_RECORD_EXECUTION_STATUS_INVALID);
            }
            recordbookGlobalSettingService.requireRecordbookWriteAllowed(execution.getRecordbookEnabled(), execution.getRecordCategory());
        }

        String requestHash = MesProBatchRecordExecutionFieldAuditHasher.hashRequest(command);
        Long tenantId = currentTenantId();
        MesProBatchRecordExecutionFieldAuditBatchDO existingBatch =
                batchMapper.selectByIdempotencyKey(tenantId, execution.getId(), command.getIdempotencyKey());
        if (existingBatch != null) {
            if (!Objects.equals(existingBatch.getRequestHash(), requestHash)) {
                throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_IDEMPOTENCY_CONFLICT);
            }
            return existingResult(execution.getId(), existingBatch);
        }

        Map<String, SnapshotField> fields = snapshotFields(execution.getExecutionSnapshotJson());
        Map<String, ObjectNode> currentCells = parseCellValues(execution.getCellValuesJson());
        List<ResolvedChange> resolvedChanges = resolveChanges(command, fields, currentCells, execution);
        boolean hasAttachmentChanges = command.getAttachmentChanges() != null
                && !command.getAttachmentChanges().isEmpty();
        if (resolvedChanges.isEmpty() && !hasAttachmentChanges) {
            return noChangeResult(execution);
        }

        validateReasonAndSignature(command);
        String signatureChallengeHash = MesProBatchRecordExecutionFieldAuditHasher.hashSignatureChallenge(
                command, SecurityFrameworkUtils.getLoginUserId());
        MesProBatchRecordExecutionFieldAuditSignatureResult signature =
                recordFieldAuditSaveEvidence(execution.getId(), command, signatureChallengeHash);

        String afterCellValuesJson = buildAfterCellValuesJson(currentCells, resolvedChanges);
        String afterCellValuesHash = MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(afterCellValuesJson);
        long beforeRevision = execution.getFieldAuditRevision();
        long afterRevision = beforeRevision + resolvedChanges.size();
        Long batchId = IdWorker.getId();
        String signatureProjectionHash = MesProBatchRecordExecutionFieldAuditHasher.hashSignatureProjection(
                new MesProBatchRecordExecutionFieldAuditSignatureProjection()
                        .setId(signature.getSignatureId())
                        .setExecutionId(execution.getId())
                        .setActionType(ACTION_FIELD_CHANGE)
                        .setActorId(signature.getActorId())
                        .setActorName(signature.getActorName())
                        .setSignatureMode(MesProBatchRecordExecutionSignatureService.SIGNATURE_MODE_PASSWORD)
                        .setPasswordVerified(Boolean.TRUE)
                        .setSignedAt(signature.getSignedAt())
                        .setSelectedSignedAt(signature.getSelectedSignedAt())
                        .setSignatureDisplayAt(signature.getSignatureDisplayAt())
                        .setSignatureTimeMode(signature.getSignatureTimeMode())
                        .setSelectedTimeZone(signature.getSelectedTimeZone())
                        .setSelectedTimeReason(signature.getSelectedTimeReason())
                        .setSelectedTimePolicyVersion(signature.getSelectedTimePolicyVersion())
                        .setSelectedTimeAuditHash(signature.getSelectedTimeAuditHash())
                        .setReasonCategory(command.getReasonCategory())
                        .setReasonText(command.getReasonText())
                        .setAuditBatchId(batchId)
                        .setSignatureChallengeHash(signatureChallengeHash)
                        .setFieldAuditRevision(afterRevision)
                        .setCellValuesHash(afterCellValuesHash));
        List<MesProBatchRecordExecutionFieldAuditItemDO> items = buildItems(execution, resolvedChanges,
                batchId, tenantId, signature, signatureProjectionHash, afterCellValuesHash);
        String newHeadHash = items.isEmpty() ? execution.getFieldAuditHeadHash()
                : items.get(items.size() - 1).getAuditHash();
        MesProBatchRecordExecutionFieldAuditHashVerification expectedVerification =
                MesProBatchRecordExecutionFieldAuditHashVerification.valid(newHeadHash, newHeadHash, 1L, items.size());

        insertBatch(execution, command, requestHash, tenantId, signature, signatureChallengeHash,
                signatureProjectionHash, afterCellValuesHash, afterRevision, batchId, newHeadHash,
                expectedVerification, items.size());
        insertItems(items);
        attachSignature(execution.getId(), signature.getSignatureId(), batchId, signatureChallengeHash,
                afterRevision, newHeadHash, afterCellValuesHash);
        if (!items.isEmpty()) {
            int updated = executionMapper.updateFieldAuditProjection(execution.getId(), afterCellValuesJson,
                    execution.getCellValuesHash(), afterCellValuesHash, beforeRevision, afterRevision,
                    execution.getFieldAuditHeadHash(), newHeadHash, batchId);
            if (updated <= 0) {
                throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_CHAIN_CONFLICT);
            }
        }
        bindAttachmentChanges(command, execution, batchId, signature);

        MesProBatchRecordExecutionFieldAuditHashVerification verification = verifyChain(execution.getId());
        if (verification.getStatus() != MesProBatchRecordExecutionFieldAuditHashVerificationStatus.VALID) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_CHAIN_INVALID);
        }
        MesProBatchRecordExecutionFieldAuditSaveResult result = new MesProBatchRecordExecutionFieldAuditSaveResult()
                .setFieldAuditRevision(afterRevision)
                .setFieldAuditHeadHash(newHeadHash)
                .setCellValuesHash(afterCellValuesHash)
                .setAuditBatchId(batchId)
                .setSignatureId(signature.getSignatureId())
                .setChangedAt(signature.getSignedAt())
                .setChangedFieldCount(items.size())
                .setHashVerification(verification);
        operationAuditService.record(new MesProEdhrOperationAuditCommand()
                .setRequestId("EDHR-AUD-" + IdWorker.getId())
                .setObjectType("FIELD_AUDIT_BATCH")
                .setObjectId(String.valueOf(batchId))
                .setBatchExecutionId(execution.getId())
                .setExecutionId(execution.getId())
                .setWorkTaskId(command.getWorkTaskId())
                .setRouteProcessId(execution.getRouteProcessId())
                .setReportId(execution.getBatchRecordReportId())
                .setRecordCategory(execution.getRecordCategory())
                .setOperationType(ACTION_FIELD_CHANGE)
                .setActionName("保存字段审计变更")
                .setActorUserId(currentUserId)
                .setActorUsername(SecurityFrameworkUtils.getLoginUserNickname())
                .setPermissionCode(goldenFingerMode
                        ? MesProEdhrGoldenFingerPermissionService.PERMISSION
                        : "mes:pro-batch-record-execution:field-audit-update")
                .setPermissionDecision(goldenFingerMode ? "ALLOW_GOLDEN_FINGER" : "ALLOW")
                .setResultStatus("SUCCESS")
                .setBeforeSummaryHash(execution.getFieldAuditHeadHash())
                .setAfterSummaryHash(newHeadHash)
                .setMetadataJson(buildFieldAuditOperationMetadata(execution, resolvedChanges,
                        items.size(), signature.getSignatureId(), goldenFingerMode)));
        return result;
    }

    @Override
    public MesProBatchRecordExecutionFieldAuditHashVerification verifyChain(Long executionId) {
        MesProBatchRecordExecutionDO execution = executionMapper.selectById(executionId);
        if (execution == null || StrUtil.isBlank(execution.getFieldAuditHeadHash())
                || execution.getFieldAuditRevision() == null || StrUtil.isBlank(execution.getCellValuesHash())) {
            return withAttachmentChain(executionId, MesProBatchRecordExecutionFieldAuditHashVerification.failed(
                    MesProBatchRecordExecutionFieldAuditHashVerificationStatus.SOURCE_MISSING,
                    null, execution == null ? null : execution.getFieldAuditHeadHash(),
                    0L, 0L, null, null, "field audit source projection missing"));
        }
        List<MesProBatchRecordExecutionFieldAuditBatchDO> batches = batchMapper.selectListByExecutionId(executionId);
        List<MesProBatchRecordExecutionFieldAuditItemDO> items = itemMapper.selectListByExecutionId(executionId);
        if (items.isEmpty()) {
            if (Objects.equals(0L, execution.getFieldAuditRevision())
                    && Objects.equals(MesProBatchRecordExecutionFieldAuditHasher.GENESIS_HEAD_HASH,
                    execution.getFieldAuditHeadHash())) {
                return withAttachmentChain(executionId, MesProBatchRecordExecutionFieldAuditHashVerification.valid(
                        execution.getFieldAuditHeadHash(), execution.getFieldAuditHeadHash(), batches.size(), 0L));
            }
            return withAttachmentChain(executionId, MesProBatchRecordExecutionFieldAuditHashVerification.failed(
                    MesProBatchRecordExecutionFieldAuditHashVerificationStatus.SOURCE_MISSING,
                    MesProBatchRecordExecutionFieldAuditHasher.GENESIS_HEAD_HASH,
                    execution.getFieldAuditHeadHash(), batches.size(), 0L, null, null,
                    "field audit items missing"));
        }

        Map<Long, MesProBatchRecordExecutionFieldAuditBatchDO> batchMap = new HashMap<>();
        for (MesProBatchRecordExecutionFieldAuditBatchDO batch : batches) {
            batchMap.put(batch.getId(), batch);
        }
        String previousHash = MesProBatchRecordExecutionFieldAuditHasher.GENESIS_HEAD_HASH;
        long checkedItems = 0L;
        Set<Long> checkedSignatureBatchIds = new HashSet<>();
        for (MesProBatchRecordExecutionFieldAuditItemDO item : items) {
            checkedItems++;
            MesProBatchRecordExecutionFieldAuditBatchDO batch = batchMap.get(item.getAuditBatchId());
            if (batch == null) {
                return withAttachmentChain(executionId, MesProBatchRecordExecutionFieldAuditHashVerification.failed(
                        MesProBatchRecordExecutionFieldAuditHashVerificationStatus.SOURCE_MISSING,
                        previousHash, execution.getFieldAuditHeadHash(), batches.size(), checkedItems,
                        item.getAuditBatchId(), item.getId(), "field audit batch missing"));
            }
            if (!Objects.equals(previousHash, item.getPreviousHash())) {
                return withAttachmentChain(executionId, MesProBatchRecordExecutionFieldAuditHashVerification.failed(
                        MesProBatchRecordExecutionFieldAuditHashVerificationStatus.CHAIN_BROKEN,
                        previousHash, execution.getFieldAuditHeadHash(), batches.size(), checkedItems,
                        item.getAuditBatchId(), item.getId(), "previous hash mismatch"));
            }
            if (checkedSignatureBatchIds.add(batch.getId()) && !signatureBindingMatchesBatch(batch)) {
                return withAttachmentChain(executionId, MesProBatchRecordExecutionFieldAuditHashVerification.failed(
                        MesProBatchRecordExecutionFieldAuditHashVerificationStatus.SIGNATURE_MISMATCH,
                        previousHash, execution.getFieldAuditHeadHash(), batches.size(), checkedItems,
                        item.getAuditBatchId(), item.getId(), "signature binding mismatch"));
            }
            if (!Objects.equals(batch.getSignatureProjectionHash(), item.getSignatureProjectionHash())) {
                return withAttachmentChain(executionId, MesProBatchRecordExecutionFieldAuditHashVerification.failed(
                        MesProBatchRecordExecutionFieldAuditHashVerificationStatus.SIGNATURE_MISMATCH,
                        previousHash, execution.getFieldAuditHeadHash(), batches.size(), checkedItems,
                        item.getAuditBatchId(), item.getId(), "signature projection hash mismatch"));
            }
            String calculated = MesProBatchRecordExecutionFieldAuditHasher.hashItem(toHashInput(item));
            if (!Objects.equals(calculated, item.getAuditHash())) {
                return withAttachmentChain(executionId, MesProBatchRecordExecutionFieldAuditHashVerification.failed(
                        MesProBatchRecordExecutionFieldAuditHashVerificationStatus.CHAIN_BROKEN,
                        calculated, execution.getFieldAuditHeadHash(), batches.size(), checkedItems,
                        item.getAuditBatchId(), item.getId(), "item hash mismatch"));
            }
            previousHash = calculated;
        }
        if (!Objects.equals(previousHash, execution.getFieldAuditHeadHash())) {
            return withAttachmentChain(executionId, MesProBatchRecordExecutionFieldAuditHashVerification.failed(
                    MesProBatchRecordExecutionFieldAuditHashVerificationStatus.CHAIN_BROKEN,
                    previousHash, execution.getFieldAuditHeadHash(), batches.size(), checkedItems,
                    null, null, "head hash mismatch"));
        }
        return withAttachmentChain(executionId, MesProBatchRecordExecutionFieldAuditHashVerification.valid(
                previousHash, execution.getFieldAuditHeadHash(), batches.size(), checkedItems));
    }

    private MesProBatchRecordExecutionFieldAuditHashVerification withAttachmentChain(
            Long executionId, MesProBatchRecordExecutionFieldAuditHashVerification verification) {
        MesProBatchRecordExecutionAttachmentChainVerifyResult attachmentChain =
                attachmentService.verifyAttachmentChain(executionId);
        int issueCount = attachmentChain == null || attachmentChain.getIssues() == null
                ? 0 : attachmentChain.getIssues().size();
        String failedReason = issueCount == 0 ? null : attachmentChain.getIssues().get(0).getMessage();
        verification
                .setAttachmentChainStatus(attachmentChain != null && attachmentChain.isValid() ? "VALID" : "INVALID")
                .setCheckedAttachmentCount(attachmentChain == null ? 0 : attachmentChain.getCheckedEventCount())
                .setAttachmentChainHeadHash(attachmentChain == null ? null : attachmentChain.getHeadHash())
                .setAttachmentChainIssueCount(issueCount)
                .setAttachmentChainFailedReason(failedReason);
        if (attachmentChain == null || !attachmentChain.isValid()) {
            verification.setStatus(MesProBatchRecordExecutionFieldAuditHashVerificationStatus.CHAIN_BROKEN);
            if (StrUtil.isBlank(verification.getFailedReason())) {
                verification.setFailedReason(StrUtil.blankToDefault(failedReason, "attachment chain invalid"));
            }
        }
        return verification;
    }

    @Override
    public PageResult<MesProBatchRecordExecutionFieldAuditItemRespVO> getPage(
            MesProBatchRecordExecutionFieldAuditPageReqVO pageReqVO) {
        MesProBatchRecordExecutionFieldAuditPageReqVO reqVO =
                pageReqVO == null ? new MesProBatchRecordExecutionFieldAuditPageReqVO() : pageReqVO;
        PageResult<MesProBatchRecordExecutionFieldAuditItemDO> page =
                itemMapper.selectPage(reqVO, buildItemQuery(reqVO)
                        .orderByDesc(MesProBatchRecordExecutionFieldAuditItemDO::getChangedAt)
                        .orderByDesc(MesProBatchRecordExecutionFieldAuditItemDO::getId));
        Map<Long, MesProBatchRecordExecutionFieldAuditHashVerificationRespVO> verificationCache = new HashMap<>();
        List<MesProBatchRecordExecutionFieldAuditItemRespVO> rows = page.getList().stream()
                .map(item -> toItemResp(item, verificationCache.computeIfAbsent(item.getExecutionId(),
                        id -> toVerificationResp(verifyChain(id)))))
                .toList();
        PageResult<MesProBatchRecordExecutionFieldAuditItemRespVO> result = new PageResult<>(rows, page.getTotal());
        operationAuditService.record(new MesProEdhrOperationAuditCommand()
                .setRequestId("EDHR-AUD-" + IdWorker.getId())
                .setObjectType("FIELD_AUDIT_PAGE")
                .setObjectId("LIST")
                .setExecutionId(reqVO.getExecutionId())
                .setOperationType("QUERY")
                .setActionName("查询字段审计列表")
                .setActorUserId(currentAuditUserId())
                .setActorUsername(SecurityFrameworkUtils.getLoginUserNickname())
                .setPermissionCode("mes:pro-batch-record-execution:field-audit-query")
                .setPermissionDecision("ALLOW")
                .setResultStatus("SUCCESS"));
        return result;
    }

    @Override
    public MesProBatchRecordExecutionFieldAuditDetailRespVO getDetail(
            MesProBatchRecordExecutionFieldAuditDetailReqVO reqVO) {
        if (reqVO == null || reqVO.getExecutionId() == null
                || (reqVO.getAuditBatchId() == null && reqVO.getAuditItemId() == null)) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_CHAIN_INVALID);
        }
        MesProBatchRecordExecutionDO execution = executionMapper.selectById(reqVO.getExecutionId());
        if (execution == null) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_NOT_EXISTS);
        }
        Long auditBatchId = reqVO.getAuditBatchId();
        if (reqVO.getAuditItemId() != null) {
            MesProBatchRecordExecutionFieldAuditItemDO item = itemMapper.selectById(reqVO.getAuditItemId());
            if (item == null || !Objects.equals(item.getExecutionId(), reqVO.getExecutionId())) {
                throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_CHAIN_INVALID);
            }
            if (auditBatchId != null && !Objects.equals(auditBatchId, item.getAuditBatchId())) {
                throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_CHAIN_INVALID);
            }
            auditBatchId = item.getAuditBatchId();
        }
        MesProBatchRecordExecutionFieldAuditBatchDO batch = batchMapper.selectById(auditBatchId);
        if (batch == null || !Objects.equals(batch.getExecutionId(), reqVO.getExecutionId())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_CHAIN_INVALID);
        }
        MesProBatchRecordExecutionFieldAuditHashVerification verification = verifyChain(reqVO.getExecutionId());
        if (verification.getStatus() != MesProBatchRecordExecutionFieldAuditHashVerificationStatus.VALID) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_CHAIN_INVALID);
        }
        MesProBatchRecordExecutionFieldAuditHashVerificationRespVO verificationResp = toVerificationResp(verification);
        List<MesProBatchRecordExecutionFieldAuditItemRespVO> items = itemMapper.selectListByBatchId(batch.getId()).stream()
                .map(item -> toItemResp(item, verificationResp))
                .toList();
        MesProBatchRecordExecutionSignatureDO signature = batch.getSignatureId() == null
                ? null : signatureMapper.selectById(batch.getSignatureId());
        MesProBatchRecordExecutionFieldAuditDetailRespVO result = new MesProBatchRecordExecutionFieldAuditDetailRespVO()
                .setExecutionId(execution.getId())
                .setExecutionCode(execution.getExecutionCode())
                .setAuditBatch(toBatchResp(batch))
                .setItems(items)
                .setAttachmentSummaries(attachmentMapper.selectListByAuditBatchId(batch.getId()).stream()
                        .map(this::toAttachmentSummary)
                        .toList())
                .setSignature(toSignatureResp(signature))
                .setHashVerification(verificationResp);
        operationAuditService.record(new MesProEdhrOperationAuditCommand()
                .setRequestId("EDHR-AUD-" + IdWorker.getId())
                .setObjectType("FIELD_AUDIT_BATCH")
                .setObjectId(String.valueOf(batch.getId()))
                .setBatchExecutionId(execution.getId())
                .setExecutionId(execution.getId())
                .setOperationType("VIEW")
                .setActionName("查看字段审计详情")
                .setActorUserId(currentAuditUserId())
                .setActorUsername(SecurityFrameworkUtils.getLoginUserNickname())
                .setPermissionCode("mes:pro-batch-record-execution:field-audit-query")
                .setPermissionDecision("ALLOW")
                .setResultStatus("SUCCESS")
                .setMetadataJson(JsonUtils.toJsonString(java.util.Map.of("auditBatchId", batch.getId()))));
        return result;
    }

    @Override
    public MesProBatchRecordExecutionFieldAuditVerifyRespVO verifyChain(
            MesProBatchRecordExecutionFieldAuditVerifyReqVO reqVO) {
        if (reqVO == null || reqVO.getExecutionId() == null) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_CHAIN_INVALID);
        }
        MesProBatchRecordExecutionDO execution = executionMapper.selectById(reqVO.getExecutionId());
        MesProBatchRecordExecutionFieldAuditHashVerification verification = verifyChain(reqVO.getExecutionId());
        if (execution != null && verification.getStatus() == MesProBatchRecordExecutionFieldAuditHashVerificationStatus.VALID
                && ((StrUtil.isNotBlank(reqVO.getExpectedFieldAuditHeadHash())
                && !Objects.equals(reqVO.getExpectedFieldAuditHeadHash(), execution.getFieldAuditHeadHash()))
                || (StrUtil.isNotBlank(reqVO.getExpectedCellValuesHash())
                && !Objects.equals(reqVO.getExpectedCellValuesHash(), execution.getCellValuesHash())))) {
            verification = MesProBatchRecordExecutionFieldAuditHashVerification.failed(
                    MesProBatchRecordExecutionFieldAuditHashVerificationStatus.CONCURRENCY_CONFLICT,
                    verification.getCalculatedHeadHash(), execution.getFieldAuditHeadHash(),
                    verification.getCheckedBatchCount(), verification.getCheckedItemCount(),
                    verification.getBrokenBatchId(), verification.getBrokenItemId(),
                    "expected field audit projection mismatch");
        }
        MesProBatchRecordExecutionFieldAuditVerifyRespVO result =
                toVerifyResp(reqVO.getExecutionId(), execution, verification);
        operationAuditService.record(new MesProEdhrOperationAuditCommand()
                .setRequestId("EDHR-AUD-" + IdWorker.getId())
                .setObjectType("FIELD_AUDIT_CHAIN")
                .setObjectId(String.valueOf(reqVO.getExecutionId()))
                .setExecutionId(reqVO.getExecutionId())
                .setOperationType("VERIFY")
                .setActionName("校验字段审计链")
                .setActorUserId(currentAuditUserId())
                .setActorUsername(SecurityFrameworkUtils.getLoginUserNickname())
                .setPermissionCode("mes:pro-batch-record-execution:field-audit-verify")
                .setPermissionDecision("ALLOW")
                .setResultStatus("SUCCESS")
                .setAfterSummaryHash(verification.getCalculatedHeadHash())
                .setMetadataJson(JsonUtils.toJsonString(java.util.Map.of(
                        "status", verification.getStatus().name(),
                        "checkedBatchCount", verification.getCheckedBatchCount(),
                        "checkedItemCount", verification.getCheckedItemCount()))));
        return result;
    }

    @Override
    public MesProBatchRecordExecutionFieldAuditExportRespVO export(
            MesProBatchRecordExecutionFieldAuditExportReqVO reqVO) {
        if (reqVO == null || reqVO.getExecutionId() == null || StrUtil.isBlank(reqVO.getFormat())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_EXPORT_FAILED);
        }
        String format = StrUtil.trim(reqVO.getFormat()).toUpperCase(Locale.ROOT);
        if (!Set.of("PDF", "CSV", "XLSX").contains(format)) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_EXPORT_FAILED);
        }
        MesProBatchRecordExecutionDO execution = executionMapper.selectById(reqVO.getExecutionId());
        if (execution == null) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_NOT_EXISTS);
        }
        MesProBatchRecordExecutionFieldAuditHashVerification verification = verifyChain(reqVO.getExecutionId());
        if (verification.getStatus() != MesProBatchRecordExecutionFieldAuditHashVerificationStatus.VALID) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_CHAIN_INVALID);
        }
        MesProBatchRecordExecutionFieldAuditHashVerificationRespVO verificationResp = toVerificationResp(verification);
        List<MesProBatchRecordExecutionFieldAuditItemRespVO> items = itemMapper.selectList(buildItemQuery(reqVO)
                        .orderByAsc(MesProBatchRecordExecutionFieldAuditItemDO::getFieldAuditRevision)
                        .orderByAsc(MesProBatchRecordExecutionFieldAuditItemDO::getId))
                .stream()
                .map(item -> toItemResp(item, verificationResp))
                .toList();
        byte[] content = renderExportContent(format, execution, verificationResp, items);
        MesProBatchRecordExecutionFieldAuditExportRespVO result = new MesProBatchRecordExecutionFieldAuditExportRespVO()
                .setFileName("field-audit-" + execution.getId() + "." + format.toLowerCase(Locale.ROOT))
                .setContentType(contentType(format))
                .setFileSize((long) content.length)
                .setSha256(sha256(content))
                .setExecutionId(execution.getId())
                .setRecordCount((long) items.size())
                .setFieldAuditRevision(execution.getFieldAuditRevision())
                .setFieldAuditHeadHash(execution.getFieldAuditHeadHash())
                .setCellValuesHash(execution.getCellValuesHash())
                .setHashVerification(verificationResp)
                .setGeneratedAt(LocalDateTime.now())
                .setContent(content);
        operationAuditService.record(new MesProEdhrOperationAuditCommand()
                .setRequestId("EDHR-AUD-" + IdWorker.getId())
                .setObjectType("FIELD_AUDIT_EXPORT")
                .setObjectId(String.valueOf(execution.getId()))
                .setExecutionId(execution.getId())
                .setOperationType("EXPORT")
                .setActionName("导出字段审计")
                .setActorUserId(currentAuditUserId())
                .setActorUsername(SecurityFrameworkUtils.getLoginUserNickname())
                .setPermissionCode("mes:pro-batch-record-execution:field-audit-export")
                .setPermissionDecision("ALLOW")
                .setResultStatus("SUCCESS")
                .setAfterSummaryHash(result.getSha256())
                .setMetadataJson(JsonUtils.toJsonString(java.util.Map.of(
                        "format", format,
                        "recordCount", items.size()))));
        return result;
    }

    @Override
    public MesProBatchRecordExecutionFieldResponsibilitySummaryRespVO getResponsibilitySummary(
            MesProBatchRecordExecutionFieldResponsibilitySummaryReqVO reqVO) {
        return responsibilityService.getSummary(reqVO);
    }

    @Override
    public MesProBatchRecordExecutionFieldResponsibilityHistoryRespVO getResponsibilityHistory(
            MesProBatchRecordExecutionFieldResponsibilityHistoryReqVO reqVO) {
        return responsibilityService.getHistory(reqVO);
    }

    @Override
    public MesProBatchRecordExecutionFieldResponsibilityExportRespVO exportResponsibility(
            MesProBatchRecordExecutionFieldResponsibilityExportReqVO reqVO) {
        return responsibilityService.export(reqVO);
    }

    private LambdaQueryWrapperX<MesProBatchRecordExecutionFieldAuditItemDO> buildItemQuery(
            MesProBatchRecordExecutionFieldAuditPageReqVO reqVO) {
        return new LambdaQueryWrapperX<MesProBatchRecordExecutionFieldAuditItemDO>()
                .eqIfPresent(MesProBatchRecordExecutionFieldAuditItemDO::getExecutionId, reqVO.getExecutionId())
                .eqIfPresent(MesProBatchRecordExecutionFieldAuditItemDO::getAuditBatchId, reqVO.getAuditBatchId())
                .eqIfPresent(MesProBatchRecordExecutionFieldAuditItemDO::getFieldPath, reqVO.getFieldPath())
                .eqIfPresent(MesProBatchRecordExecutionFieldAuditItemDO::getFieldKey, reqVO.getFieldKey())
                .eqIfPresent(MesProBatchRecordExecutionFieldAuditItemDO::getActorId, reqVO.getActorId())
                .likeIfPresent(MesProBatchRecordExecutionFieldAuditItemDO::getActorName, reqVO.getActorName())
                .eqIfPresent(MesProBatchRecordExecutionFieldAuditItemDO::getReasonCategory, reqVO.getReasonCategory())
                .likeIfPresent(MesProBatchRecordExecutionFieldAuditItemDO::getReasonText, reqVO.getReasonKeyword())
                .betweenIfPresent(MesProBatchRecordExecutionFieldAuditItemDO::getChangedAt,
                        reqVO.getChangedAtStart(), reqVO.getChangedAtEnd());
    }

    private MesProBatchRecordExecutionFieldAuditItemRespVO toItemResp(
            MesProBatchRecordExecutionFieldAuditItemDO item,
            MesProBatchRecordExecutionFieldAuditHashVerificationRespVO verification) {
        MesProBatchRecordExecutionDO execution = executionMapper.selectById(item.getExecutionId());
        return new MesProBatchRecordExecutionFieldAuditItemRespVO()
                .setId(item.getId())
                .setAuditBatchId(item.getAuditBatchId())
                .setExecutionId(item.getExecutionId())
                .setExecutionCode(execution == null ? null : execution.getExecutionCode())
                .setFieldAuditRevision(item.getFieldAuditRevision())
                .setFieldPath(item.getFieldPath())
                .setFieldKey(item.getFieldKey())
                .setFieldLabel(item.getFieldLabel())
                .setRowIndex(item.getRowIndex())
                .setColumnIndex(item.getColumnIndex())
                .setComponent(item.getComponent())
                .setValueType(item.getValueType())
                .setOldValueJson(item.getOldValueJson())
                .setOldValueDisplay(item.getOldValueDisplay())
                .setOldValueHash(item.getOldValueHash())
                .setNewValueJson(item.getNewValueJson())
                .setNewValueDisplay(item.getNewValueDisplay())
                .setNewValueHash(item.getNewValueHash())
                .setRecordbookValueJson(parseOptionalJson(item.getRecordbookValueJson()))
                .setRecordbookValueDisplay(item.getRecordbookValueDisplay())
                .setBatchRecordValueJson(parseOptionalJson(item.getBatchRecordValueJson()))
                .setBatchRecordValueDisplay(item.getBatchRecordValueDisplay())
                .setReasonCategory(item.getReasonCategory())
                .setReasonText(item.getReasonText())
                .setActorId(item.getActorId())
                .setActorName(item.getActorName())
                .setSignatureId(item.getSignatureId())
                .setPreviousHash(item.getPreviousHash())
                .setAuditHash(item.getAuditHash())
                .setChangedAt(item.getChangedAt())
                .setHashVerification(verification);
    }

    private MesProBatchRecordExecutionFieldAuditBatchRespVO toBatchResp(
            MesProBatchRecordExecutionFieldAuditBatchDO batch) {
        return new MesProBatchRecordExecutionFieldAuditBatchRespVO()
                .setId(batch.getId())
                .setExecutionId(batch.getExecutionId())
                .setIdempotencyKey(batch.getIdempotencyKey())
                .setRequestHash(batch.getRequestHash())
                .setActionType(batch.getActionType())
                .setReasonCategory(batch.getReasonCategory())
                .setReasonText(batch.getReasonText())
                .setFieldCount(batch.getFieldCount())
                .setActorId(batch.getActorId())
                .setActorName(batch.getActorName())
                .setSignatureId(batch.getSignatureId())
                .setSignatureChallengeHash(batch.getSignatureChallengeHash())
                .setSignatureProjectionHash(batch.getSignatureProjectionHash())
                .setBaseCellValuesHash(batch.getBaseCellValuesHash())
                .setBeforeCellValuesHash(batch.getBeforeCellValuesHash())
                .setAfterCellValuesHash(batch.getAfterCellValuesHash())
                .setBaseFieldAuditRevision(batch.getBaseFieldAuditRevision())
                .setBeforeFieldAuditRevision(batch.getBeforeFieldAuditRevision())
                .setAfterFieldAuditRevision(batch.getAfterFieldAuditRevision())
                .setBaseFieldAuditHeadHash(batch.getBaseFieldAuditHeadHash())
                .setPreviousHeadHash(batch.getPreviousHeadHash())
                .setNewHeadHash(batch.getNewHeadHash())
                .setChangedAt(batch.getChangedAt());
    }

    private MesProBatchRecordExecutionFieldAuditDetailRespVO.AttachmentSummary toAttachmentSummary(
            MesProBatchRecordExecutionAttachmentDO attachment) {
        return new MesProBatchRecordExecutionFieldAuditDetailRespVO.AttachmentSummary()
                .setId(attachment.getId())
                .setAuditBatchId(attachment.getAuditBatchId())
                .setSignatureId(attachment.getSignatureId())
                .setExecutionId(attachment.getExecutionId())
                .setWorkTaskId(attachment.getWorkTaskId())
                .setRowIndex(attachment.getRowIndex())
                .setColumnIndex(attachment.getColumnIndex())
                .setFieldKey(attachment.getFieldKey())
                .setFieldPath(attachment.getFieldPath())
                .setFieldLabel(attachment.getFieldLabel())
                .setAttachmentType(attachment.getAttachmentType())
                .setAttachmentGroupKey(attachment.getAttachmentGroupKey())
                .setAttachmentAction(attachment.getAttachmentAction())
                .setVersionNo(attachment.getVersionNo())
                .setFileId(attachment.getFileId())
                .setFileUrl(attachment.getFileUrl())
                .setStorageConfigId(attachment.getStorageConfigId())
                .setStoragePath(attachment.getStoragePath())
                .setFileName(attachment.getFileName())
                .setContentType(attachment.getContentType())
                .setFileSize(attachment.getFileSize())
                .setSha256(attachment.getSha256())
                .setStorageRetentionHash(attachment.getStorageRetentionHash())
                .setPreviousAttachmentHash(attachment.getPreviousAttachmentHash())
                .setAttachmentHash(attachment.getAttachmentHash())
                .setOperatorId(attachment.getOperatorId())
                .setOperatorName(attachment.getOperatorName())
                .setOperatedAt(attachment.getOperatedAt())
                .setReasonCategory(attachment.getReasonCategory())
                .setReasonText(attachment.getReasonText());
    }

    private MesProBatchRecordExecutionFieldAuditSignatureRespVO toSignatureResp(
            MesProBatchRecordExecutionSignatureDO signature) {
        if (signature == null) {
            return null;
        }
        return new MesProBatchRecordExecutionFieldAuditSignatureRespVO()
                .setSignatureId(signature.getId())
                .setActionType(signature.getActionType())
                .setSignatureMode(signature.getSignatureMode())
                .setActorId(signature.getActorId())
                .setActorName(signature.getActorName())
                .setSignedAt(signature.getSignedAt())
                .setPasswordVerified(signature.getPasswordVerified())
                .setSignatureChallengeHash(signature.getSignatureChallengeHash())
                .setFieldAuditRevision(signature.getFieldAuditRevision())
                .setFieldAuditHeadHash(signature.getFieldAuditHeadHash())
                .setCellValuesHash(signature.getCellValuesHash());
    }

    private MesProBatchRecordExecutionFieldAuditVerifyRespVO toVerifyResp(
            Long executionId,
            MesProBatchRecordExecutionDO execution,
            MesProBatchRecordExecutionFieldAuditHashVerification verification) {
        return new MesProBatchRecordExecutionFieldAuditVerifyRespVO()
                .setExecutionId(executionId)
                .setHashVerification(toVerificationResp(verification))
                .setVerifiedCount(verification.getCheckedItemCount())
                .setFieldAuditRevision(execution == null ? null : execution.getFieldAuditRevision())
                .setFieldAuditHeadHash(execution == null ? null : execution.getFieldAuditHeadHash())
                .setCellValuesHash(execution == null ? null : execution.getCellValuesHash())
                .setCheckedAt(verification.getCheckedAt());
    }

    private MesProBatchRecordExecutionFieldAuditHashVerificationRespVO toVerificationResp(
            MesProBatchRecordExecutionFieldAuditHashVerification verification) {
        if (verification == null) {
            return null;
        }
        return new MesProBatchRecordExecutionFieldAuditHashVerificationRespVO()
                .setStatus(verification.getStatus() == null ? null : verification.getStatus().name())
                .setCalculatedHeadHash(verification.getCalculatedHeadHash())
                .setStoredHeadHash(verification.getStoredHeadHash())
                .setCheckedBatchCount(verification.getCheckedBatchCount())
                .setCheckedItemCount(verification.getCheckedItemCount())
                .setBrokenBatchId(verification.getBrokenBatchId())
                .setBrokenItemId(verification.getBrokenItemId())
                .setFailedReason(verification.getFailedReason())
                .setCheckedAt(verification.getCheckedAt())
                .setAttachmentChainStatus(verification.getAttachmentChainStatus())
                .setCheckedAttachmentCount(verification.getCheckedAttachmentCount())
                .setAttachmentChainHeadHash(verification.getAttachmentChainHeadHash())
                .setAttachmentChainIssueCount(verification.getAttachmentChainIssueCount())
                .setAttachmentChainFailedReason(verification.getAttachmentChainFailedReason());
    }

    private Long currentAuditUserId() {
        return SecurityFrameworkUtils.getLoginUserId();
    }

    private byte[] renderExportContent(String format,
                                       MesProBatchRecordExecutionDO execution,
                                       MesProBatchRecordExecutionFieldAuditHashVerificationRespVO verification,
                                       List<MesProBatchRecordExecutionFieldAuditItemRespVO> items) {
        if ("PDF".equals(format)) {
            return renderFieldAuditPdf(execution, verification, items);
        }
        if ("CSV".equals(format)) {
            StringBuilder builder = new StringBuilder();
            builder.append("id,auditBatchId,executionId,executionCode,fieldAuditRevision,fieldPath,fieldKey,fieldLabel,rowIndex,columnIndex,valueType,oldValueJson,oldValueDisplay,oldValueHash,newValueJson,newValueDisplay,newValueHash,recordbookValueJson,recordbookValueDisplay,batchRecordValueJson,batchRecordValueDisplay,reasonCategory,reasonText,actorId,actorName,signatureId,previousHash,auditHash,changedAt\n");
            for (MesProBatchRecordExecutionFieldAuditItemRespVO item : items) {
                builder.append(csv(item.getId())).append(',')
                        .append(csv(item.getAuditBatchId())).append(',')
                        .append(csv(item.getExecutionId())).append(',')
                        .append(csv(item.getExecutionCode())).append(',')
                        .append(csv(item.getFieldAuditRevision())).append(',')
                        .append(csv(item.getFieldPath())).append(',')
                        .append(csv(item.getFieldKey())).append(',')
                        .append(csv(item.getFieldLabel())).append(',')
                        .append(csv(item.getRowIndex())).append(',')
                        .append(csv(item.getColumnIndex())).append(',')
                        .append(csv(item.getValueType())).append(',')
                        .append(csv(item.getOldValueJson())).append(',')
                        .append(csv(item.getOldValueDisplay())).append(',')
                        .append(csv(item.getOldValueHash())).append(',')
                        .append(csv(item.getNewValueJson())).append(',')
                        .append(csv(item.getNewValueDisplay())).append(',')
                        .append(csv(item.getNewValueHash())).append(',')
                        .append(csv(item.getRecordbookValueJson())).append(',')
                        .append(csv(item.getRecordbookValueDisplay())).append(',')
                        .append(csv(item.getBatchRecordValueJson())).append(',')
                        .append(csv(item.getBatchRecordValueDisplay())).append(',')
                        .append(csv(item.getReasonCategory())).append(',')
                        .append(csv(item.getReasonText())).append(',')
                        .append(csv(item.getActorId())).append(',')
                        .append(csv(item.getActorName())).append(',')
                        .append(csv(item.getSignatureId())).append(',')
                        .append(csv(item.getPreviousHash())).append(',')
                        .append(csv(item.getAuditHash())).append(',')
                        .append(csv(item.getChangedAt()))
                        .append('\n');
            }
            return builder.toString().getBytes(StandardCharsets.UTF_8);
        }
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("field-audit");
            String[] headers = {"id", "auditBatchId", "executionId", "executionCode", "fieldAuditRevision",
                    "fieldPath", "fieldKey", "fieldLabel", "rowIndex", "columnIndex", "valueType",
                    "oldValueJson", "oldValueDisplay", "oldValueHash", "newValueJson", "newValueDisplay",
                    "newValueHash", "recordbookValueJson", "recordbookValueDisplay", "batchRecordValueJson",
                    "batchRecordValueDisplay", "reasonCategory", "reasonText", "actorId", "actorName",
                    "signatureId", "previousHash", "auditHash", "changedAt"};
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            for (int rowIndex = 0; rowIndex < items.size(); rowIndex++) {
                MesProBatchRecordExecutionFieldAuditItemRespVO item = items.get(rowIndex);
                Row row = sheet.createRow(rowIndex + 1);
                Object[] values = {item.getId(), item.getAuditBatchId(), item.getExecutionId(), item.getExecutionCode(),
                        item.getFieldAuditRevision(), item.getFieldPath(), item.getFieldKey(), item.getFieldLabel(),
                        item.getRowIndex(), item.getColumnIndex(), item.getValueType(), item.getOldValueJson(),
                        item.getOldValueDisplay(), item.getOldValueHash(), item.getNewValueJson(),
                        item.getNewValueDisplay(), item.getNewValueHash(), item.getRecordbookValueJson(),
                        item.getRecordbookValueDisplay(), item.getBatchRecordValueJson(),
                        item.getBatchRecordValueDisplay(), item.getReasonCategory(), item.getReasonText(),
                        item.getActorId(), item.getActorName(), item.getSignatureId(), item.getPreviousHash(),
                        item.getAuditHash(), item.getChangedAt()};
                for (int cellIndex = 0; cellIndex < values.length; cellIndex++) {
                    row.createCell(cellIndex).setCellValue(Objects.toString(values[cellIndex], ""));
                }
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (Exception ex) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_EXPORT_FAILED);
        }
    }

    private String contentType(String format) {
        return switch (format) {
            case "PDF" -> "application/pdf";
            case "CSV" -> "text/csv;charset=UTF-8";
            case "XLSX" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            default -> throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_EXPORT_FAILED);
        };
    }

    private byte[] renderFieldAuditPdf(MesProBatchRecordExecutionDO execution,
                                       MesProBatchRecordExecutionFieldAuditHashVerificationRespVO verification,
                                       List<MesProBatchRecordExecutionFieldAuditItemRespVO> items) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDFont font = PDType0Font.load(document, new File(FIELD_AUDIT_PDF_FONT_PATH));
            FieldAuditPdfWriter writer = new FieldAuditPdfWriter(document, font);
            LocalDateTime generatedAt = LocalDateTime.now();
            writer.title("字段审计签名证据页");
            writer.section("Record Tracking");
            writer.line("记录状态: Original");
            writer.line("业务模块: MES/eDHR");
            writer.line("执行记录 ID: " + pdfValue(execution.getId()));
            writer.line("执行记录编号: " + pdfValue(execution.getExecutionCode()));
            writer.line("字段审计版本: " + pdfValue(execution.getFieldAuditRevision()));
            writer.line("字段审计链 Head Hash: " + pdfValue(execution.getFieldAuditHeadHash()));
            writer.line("单元格值 Hash: " + pdfValue(execution.getCellValuesHash()));
            writer.line("导出时间: " + pdfValue(generatedAt));
            writer.blank();
            writer.section("System Verification");
            writer.line("校验声明: 本系统认证并可校验");
            writer.line("校验规则: 字段审计 hash 链 + 签名挑战哈希 + 系统内证据复核");
            writer.line("校验状态: " + pdfValue(verification.getStatus()));
            writer.line("校验时间: " + pdfValue(verification.getCheckedAt()));
            writer.line("计算 Head Hash: " + pdfValue(verification.getCalculatedHeadHash()));
            writer.line("存储 Head Hash: " + pdfValue(verification.getStoredHeadHash()));
            writer.line(FIELD_AUDIT_SYSTEM_STATEMENT);
            writer.blank();
            for (int index = 0; index < items.size(); index++) {
                MesProBatchRecordExecutionFieldAuditItemRespVO item = items.get(index);
                MesProBatchRecordExecutionSignatureDO signature = item.getSignatureId() == null
                        ? null : signatureMapper.selectById(item.getSignatureId());
                writer.section("Field Audit Item " + (index + 1));
                writer.line("字段路径: " + pdfValue(item.getFieldPath()));
                writer.line("字段标签: " + pdfValue(item.getFieldLabel()));
                if (item.getRecordbookValueDisplay() != null || item.getBatchRecordValueDisplay() != null) {
                    writer.line("记录本填写值: " + pdfValue(item.getRecordbookValueDisplay()));
                    writer.line("批记录存储值: " + pdfValue(item.getBatchRecordValueDisplay()));
                } else {
                    writer.line("旧值: " + pdfValue(item.getOldValueDisplay()));
                    writer.line("新值: " + pdfValue(item.getNewValueDisplay()));
                }
                writer.line("变更原因分类: " + pdfValue(item.getReasonCategory()));
                writer.line("变更原因: " + pdfValue(item.getReasonText()));
                writer.line("操作人: " + pdfValue(item.getActorName()));
                writer.line("签名 ID: " + pdfValue(item.getSignatureId()));
                writer.line("签名挑战哈希: " + pdfValue(signature == null ? null : signature.getSignatureChallengeHash()));
                writer.line("签名时间模式: " + pdfValue(signature == null ? null : signature.getSignatureTimeMode()));
                writer.line("签名时间: " + pdfValue(signature == null ? null : signature.getSignatureDisplayAt()));
                writer.line("签名动作: " + pdfValue(signature == null ? null : signature.getActionType()));
                writer.line("认证方式: " + pdfValue(signature == null ? null : signature.getAuthenticationMethod()));
                writer.line("客户端 IP: " + pdfValue(signature == null ? null : signature.getClientIpSnapshot()));
                writer.line("User-Agent: " + pdfValue(signature == null ? null : signature.getUserAgentSnapshot()));
                writer.line("上一节点 Hash: " + pdfValue(item.getPreviousHash()));
                writer.line("审计 Hash: " + pdfValue(item.getAuditHash()));
                writer.line("校验状态: " + pdfValue(verification.getStatus()));
                writer.blank();
            }
            writer.close();
            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (IOException | RuntimeException ex) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_EXPORT_FAILED);
        }
    }

    private static String pdfValue(Object value) {
        String text = Objects.toString(value, "");
        return StrUtil.isBlank(text) ? "-" : text;
    }

    private static final class FieldAuditPdfWriter {

        private static final float MARGIN = 42F;
        private static final float LEADING = 15F;
        private static final float BODY_FONT_SIZE = 10F;
        private static final float TITLE_FONT_SIZE = 16F;
        private static final float SECTION_FONT_SIZE = 12F;
        private final PDDocument document;
        private final PDFont font;
        private PDPage page;
        private PDPageContentStream contentStream;
        private float y;

        private FieldAuditPdfWriter(PDDocument document, PDFont font) throws IOException {
            this.document = document;
            this.font = font;
            newPage();
        }

        private void title(String text) throws IOException {
            write(text, TITLE_FONT_SIZE, true);
            blank();
        }

        private void section(String text) throws IOException {
            ensureSpace(LEADING * 2);
            write(text, SECTION_FONT_SIZE, true);
        }

        private void line(String text) throws IOException {
            for (String line : wrap(text, BODY_FONT_SIZE)) {
                write(line, BODY_FONT_SIZE, false);
            }
        }

        private void blank() throws IOException {
            ensureSpace(LEADING);
            y -= LEADING;
        }

        private void write(String text, float fontSize, boolean underline) throws IOException {
            ensureSpace(LEADING);
            contentStream.beginText();
            contentStream.setFont(font, fontSize);
            contentStream.newLineAtOffset(MARGIN, y);
            contentStream.showText(text);
            contentStream.endText();
            if (underline) {
                contentStream.moveTo(MARGIN, y - 2);
                contentStream.lineTo(PDRectangle.A4.getWidth() - MARGIN, y - 2);
                contentStream.stroke();
            }
            y -= LEADING;
        }

        private List<String> wrap(String text, float fontSize) throws IOException {
            List<String> lines = new ArrayList<>();
            float maxWidth = PDRectangle.A4.getWidth() - MARGIN * 2;
            StringBuilder current = new StringBuilder();
            for (int i = 0; i < text.length(); i++) {
                char ch = text.charAt(i);
                String candidate = current + String.valueOf(ch);
                if (font.getStringWidth(candidate) / 1000 * fontSize > maxWidth && current.length() > 0) {
                    lines.add(current.toString());
                    current = new StringBuilder(String.valueOf(ch));
                } else {
                    current.append(ch);
                }
            }
            if (current.length() > 0) {
                lines.add(current.toString());
            }
            return lines.isEmpty() ? List.of("") : lines;
        }

        private void ensureSpace(float requiredHeight) throws IOException {
            if (y - requiredHeight >= MARGIN) {
                return;
            }
            closePage();
            newPage();
        }

        private void newPage() throws IOException {
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            contentStream = new PDPageContentStream(document, page);
            y = page.getMediaBox().getHeight() - MARGIN;
        }

        private void closePage() throws IOException {
            if (contentStream != null) {
                contentStream.close();
            }
        }

        private void close() throws IOException {
            closePage();
        }
    }

    private String csv(Object value) {
        String text = Objects.toString(value, "");
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    private String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 calculation failed", ex);
        }
    }

    private MesProBatchRecordExecutionFieldAuditSaveResult existingResult(
            Long executionId, MesProBatchRecordExecutionFieldAuditBatchDO batch) {
        MesProBatchRecordExecutionFieldAuditHashVerification verification = verifyChain(executionId);
        if (verification.getStatus() != MesProBatchRecordExecutionFieldAuditHashVerificationStatus.VALID) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_CHAIN_INVALID);
        }
        return new MesProBatchRecordExecutionFieldAuditSaveResult()
                .setFieldAuditRevision(batch.getAfterFieldAuditRevision())
                .setFieldAuditHeadHash(batch.getNewHeadHash())
                .setCellValuesHash(batch.getAfterCellValuesHash())
                .setAuditBatchId(batch.getId())
                .setSignatureId(batch.getSignatureId())
                .setChangedAt(batch.getChangedAt())
                .setChangedFieldCount(batch.getFieldCount())
                .setHashVerification(verification);
    }

    private MesProBatchRecordExecutionFieldAuditSaveResult noChangeResult(MesProBatchRecordExecutionDO execution) {
        return new MesProBatchRecordExecutionFieldAuditSaveResult()
                .setFieldAuditRevision(execution.getFieldAuditRevision())
                .setFieldAuditHeadHash(execution.getFieldAuditHeadHash())
                .setCellValuesHash(execution.getCellValuesHash())
                .setChangedFieldCount(0)
                .setHashVerification(verifyChain(execution.getId()));
    }

    private void validateCommandShape(MesProBatchRecordExecutionFieldAuditSaveChangesCommand command,
                                      boolean requireWorkTaskId) {
        if (command == null || command.getExecutionId() == null
                || (requireWorkTaskId && command.getWorkTaskId() == null)
                || StrUtil.isBlank(command.getIdempotencyKey())
                || StrUtil.isBlank(command.getBaseCellValuesHash())
                || command.getBaseFieldAuditRevision() == null
                || StrUtil.isBlank(command.getBaseFieldAuditHeadHash())
                || ((command.getChanges() == null || command.getChanges().isEmpty())
                && (command.getAttachmentChanges() == null || command.getAttachmentChanges().isEmpty()))) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_BASELINE_MISSING);
        }
        if (command.getChanges() == null) {
            command.setChanges(java.util.List.of());
        }
        if (command.getAttachmentChanges() == null) {
            command.setAttachmentChanges(java.util.List.of());
        }
    }

    private void validateBaselineAvailable(MesProBatchRecordExecutionDO execution) {
        if (StrUtil.isBlank(execution.getCellValuesHash()) || execution.getFieldAuditRevision() == null
                || StrUtil.isBlank(execution.getFieldAuditHeadHash())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_BASELINE_MISSING);
        }
        String actualHash = MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(execution.getCellValuesJson());
        if (!Objects.equals(actualHash, execution.getCellValuesHash())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_CHAIN_CONFLICT);
        }
    }

    private void validateBaselineMatches(MesProBatchRecordExecutionFieldAuditSaveChangesCommand command,
                                         MesProBatchRecordExecutionDO execution) {
        if (!Objects.equals(command.getBaseCellValuesHash(), execution.getCellValuesHash())
                || !Objects.equals(command.getBaseFieldAuditRevision(), execution.getFieldAuditRevision())
                || !Objects.equals(command.getBaseFieldAuditHeadHash(), execution.getFieldAuditHeadHash())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_CHAIN_CONFLICT);
        }
    }

    private void validateReasonAndSignature(MesProBatchRecordExecutionFieldAuditSaveChangesCommand command) {
        if (StrUtil.isBlank(command.getReasonText())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_REASON_REQUIRED);
        }
        if (!REASON_CATEGORIES.contains(command.getReasonCategory())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_REASON_CATEGORY_INVALID);
        }
    }

    private MesProBatchRecordExecutionFieldAuditSignatureResult recordFieldAuditSaveEvidence(
            Long executionId,
            MesProBatchRecordExecutionFieldAuditSaveChangesCommand command,
            String signatureChallengeHash) {
        MesProBatchRecordExecutionFieldAuditSaveChangesCommand.Signature signature = command.getSignature();
        MesProBatchRecordExecutionFieldAuditSignatureCommand signatureCommand =
                new MesProBatchRecordExecutionFieldAuditSignatureCommand()
                        .setExecutionId(executionId)
                        .setReasonCategory(command.getReasonCategory())
                        .setReasonText(command.getReasonText())
                        .setSignatureChallengeHash(signatureChallengeHash);
        if (signature != null && StrUtil.isNotBlank(signature.getPassword())) {
            return signatureService.recordFieldChangeSignature(signatureCommand
                    .setPassword(signature.getPassword())
                    .setSignatureTimeCommand(signature.getSignatureTimeCommand()));
        }
        return signatureService.recordFieldChangeDraftSave(signatureCommand);
    }

    private void validateBatchSharedFillScope(MesProBatchRecordExecutionFieldAuditSaveChangesCommand command,
                                              MesProBatchRecordExecutionDO execution,
                                              MesProEdhrWorkTaskDO workTask) {
        if (workTask == null || workTask.getBatchTaskId() == null) {
            return;
        }
        MesProEdhrBatchExecutionTaskDO batchTask = batchTaskMapper.selectById(workTask.getBatchTaskId());
        if (batchTask == null || !INSTANCE_SCOPE_BATCH_SHARED.equals(StrUtil.trim(batchTask.getInstanceScope()))) {
            return;
        }
        if (!Objects.equals(batchTask.getExecutionId(), execution.getId())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_WRITE_TASK_INVALID, "共享表单任务与执行实例不匹配");
        }
        Integer sourceTableIndex = resolveExecutionSourceTableIndex(execution);
        if (sourceTableIndex == null || StrUtil.isBlank(batchTask.getFillableScopeJson())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_WRITE_TASK_INVALID, "共享表单填写范围缺失");
        }
        for (MesProBatchRecordExecutionFieldAuditChange change : command.getChanges()) {
            requireCellInFillScope(batchTask.getFillableScopeJson(), sourceTableIndex, change.getRowIndex());
            requireCellInResponsibilityScope(workTask, sourceTableIndex, change.getRowIndex(), change.getColumnIndex());
        }
        for (MesProBatchRecordExecutionFieldAuditAttachmentChange change : command.getAttachmentChanges()) {
            if (!Objects.equals(command.getWorkTaskId(), change.getWorkTaskId())) {
                throw exception(PRO_BATCH_RECORD_EXECUTION_WRITE_TASK_INVALID, "附件写入任务与当前任务不一致");
            }
            requireCellInFillScope(batchTask.getFillableScopeJson(), sourceTableIndex, change.getRowIndex());
            requireCellInResponsibilityScope(workTask, sourceTableIndex, change.getRowIndex(), change.getColumnIndex());
        }
    }

    private Integer resolveExecutionSourceTableIndex(MesProBatchRecordExecutionDO execution) {
        try {
            JsonNode meta = JsonUtils.getObjectMapper().readTree(execution.getMetaJson());
            return integer(meta, "sourceTableIndex");
        } catch (JsonProcessingException e) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_WRITE_TASK_INVALID, "执行快照缺少表格上下文");
        }
    }

    private void requireCellInFillScope(String fillableScopeJson, Integer sourceTableIndex, Integer rowIndex) {
        if (rowIndex == null || !cellInFillScope(fillableScopeJson, sourceTableIndex, rowIndex)) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_WRITE_TASK_INVALID, "单元格不在当前工序填写范围内");
        }
    }

    private boolean cellInFillScope(String fillableScopeJson, Integer sourceTableIndex, Integer rowIndex) {
        try {
            JsonNode root = JsonUtils.getObjectMapper().readTree(fillableScopeJson);
            JsonNode ranges = root.path("ranges");
            if (!ranges.isArray() || ranges.isEmpty()) {
                throw exception(PRO_BATCH_RECORD_EXECUTION_WRITE_TASK_INVALID, "共享表单填写范围缺失");
            }
            for (JsonNode range : ranges) {
                Integer rangeSourceTableIndex = integer(range, "sourceTableIndex");
                Integer startRow = integer(range, "startRow");
                Integer endRow = integer(range, "endRow");
                if (rangeSourceTableIndex == null || startRow == null || endRow == null) {
                    throw exception(PRO_BATCH_RECORD_EXECUTION_WRITE_TASK_INVALID, "共享表单填写范围无效");
                }
                if (Objects.equals(sourceTableIndex, rangeSourceTableIndex)
                        && rowIndex >= startRow && rowIndex <= endRow) {
                    return true;
                }
            }
            return false;
        } catch (JsonProcessingException e) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_WRITE_TASK_INVALID, "共享表单填写范围无效");
        }
    }

    private void requireCellInResponsibilityScope(MesProEdhrWorkTaskDO workTask,
                                                  Integer sourceTableIndex,
                                                  Integer rowIndex,
                                                  Integer columnIndex) {
        if (workTask == null || !ENTITLEMENT_SOURCE_TYPE_FILLER.equals(workTask.getResponsibilitySourceType())) {
            return;
        }
        if (sourceTableIndex == null || rowIndex == null || columnIndex == null
                || StrUtil.isBlank(workTask.getResponsibilityScopeJson())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_WRITE_TASK_INVALID, "工作任务责任范围缺失");
        }
        if (!cellInResponsibilityScope(workTask.getResponsibilityScopeJson(), sourceTableIndex, rowIndex, columnIndex)) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_WRITE_TASK_INVALID, "单元格不在当前用户责任范围内");
        }
    }

    private boolean cellInResponsibilityScope(String responsibilityScopeJson,
                                              Integer sourceTableIndex,
                                              Integer rowIndex,
                                              Integer columnIndex) {
        try {
            JsonNode root = JsonUtils.getObjectMapper().readTree(responsibilityScopeJson);
            JsonNode scopes = root.path("scopes");
            if (!scopes.isArray() || scopes.isEmpty()) {
                throw exception(PRO_BATCH_RECORD_EXECUTION_WRITE_TASK_INVALID, "工作任务责任范围无效");
            }
            for (JsonNode scope : scopes) {
                JsonNode cells = scope.path("fillableScope").path("cells");
                if (!cells.isArray() || cells.isEmpty()) {
                    throw exception(PRO_BATCH_RECORD_EXECUTION_WRITE_TASK_INVALID, "工作任务责任范围无效");
                }
                for (JsonNode cell : cells) {
                    Integer cellSourceTableIndex = integer(cell, "sourceTableIndex");
                    Integer cellRowIndex = integer(cell, "rowIndex");
                    Integer cellColumnIndex = integer(cell, "columnIndex");
                    if (cellSourceTableIndex == null || cellRowIndex == null || cellColumnIndex == null) {
                        throw exception(PRO_BATCH_RECORD_EXECUTION_WRITE_TASK_INVALID, "工作任务责任范围无效");
                    }
                    if (Objects.equals(sourceTableIndex, cellSourceTableIndex)
                            && Objects.equals(rowIndex, cellRowIndex)
                            && Objects.equals(columnIndex, cellColumnIndex)) {
                        return true;
                    }
                }
            }
            return false;
        } catch (JsonProcessingException e) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_WRITE_TASK_INVALID, "工作任务责任范围无效");
        }
    }

    private List<MesProBatchRecordExecutionFieldAuditChange> sortedChanges(
            List<MesProBatchRecordExecutionFieldAuditChange> changes) {
        List<MesProBatchRecordExecutionFieldAuditChange> sorted = new ArrayList<>(changes);
        sorted.sort(Comparator
                .comparing(MesProBatchRecordExecutionFieldAuditChange::getFieldPath,
                        Comparator.nullsFirst(String::compareTo))
                .thenComparing(MesProBatchRecordExecutionFieldAuditChange::getFieldKey,
                        Comparator.nullsFirst(String::compareTo))
                .thenComparing(MesProBatchRecordExecutionFieldAuditChange::getRowIndex,
                        Comparator.nullsFirst(Integer::compareTo))
                .thenComparing(MesProBatchRecordExecutionFieldAuditChange::getColumnIndex,
                        Comparator.nullsFirst(Integer::compareTo)));
        Set<String> keys = new HashSet<>();
        for (MesProBatchRecordExecutionFieldAuditChange change : sorted) {
            if (!keys.add(changeKey(change))) {
                throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_FIELD_NOT_DECLARED);
            }
        }
        return sorted;
    }

    private Map<String, SnapshotField> snapshotFields(String executionSnapshotJson) {
        try {
            JsonNode root = JsonUtils.getObjectMapper().readTree(executionSnapshotJson);
            JsonNode fields = root.path("fields");
            Map<String, SnapshotField> result = new HashMap<>();
            if (!fields.isArray()) {
                return result;
            }
            for (JsonNode field : fields) {
                String fieldPath = text(field, "fieldPath");
                String fieldKey = text(field, "fieldKey");
                Integer rowIndex = integer(field, "rowIndex");
                Integer columnIndex = integer(field, "columnIndex");
                if (StrUtil.isBlank(fieldPath) || StrUtil.isBlank(fieldKey)
                        || rowIndex == null || columnIndex == null) {
                    continue;
                }
                result.put(fieldKey(fieldPath, fieldKey, rowIndex, columnIndex),
                        new SnapshotField(fieldPath, fieldKey, StrUtil.blankToDefault(text(field, "label"), fieldKey),
                                rowIndex, columnIndex, text(field, "component"), snapshotDefaultValue(field),
                                snapshotValueType(field), booleanValue(field, "required"),
                                field.path("constraints").isObject() ? field.path("constraints").deepCopy()
                                        : JsonNodeFactory.instance.objectNode(),
                                text(field, "unit")));
            }
            return result;
        } catch (JsonProcessingException e) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_FIELD_NOT_DECLARED);
        }
    }

    private JsonNode snapshotDefaultValue(JsonNode field) {
        JsonNode defaultValue = field.get("defaultValue");
        if (defaultValue != null && !defaultValue.isMissingNode()) {
            return defaultValue.deepCopy();
        }
        JsonNode value = field.get("value");
        return value == null || value.isMissingNode() ? NullNode.instance : value.deepCopy();
    }

    private MesProBatchRecordExecutionFieldAuditValueType snapshotValueType(JsonNode field) {
        String valueType = text(field, "valueType");
        if (StrUtil.isBlank(valueType)) {
            return null;
        }
        try {
            return MesProBatchRecordExecutionFieldAuditValueType.valueOf(valueType);
        } catch (IllegalArgumentException ex) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_VALUE_TYPE_UNSUPPORTED);
        }
    }

    private List<ResolvedChange> resolveChanges(MesProBatchRecordExecutionFieldAuditSaveChangesCommand command,
                                                Map<String, SnapshotField> fields,
                                                Map<String, ObjectNode> currentCells,
                                                MesProBatchRecordExecutionDO execution) {
        List<ResolvedChange> result = new ArrayList<>();
        boolean recordbookMode = isRecordbookUnrestrictedMode(command);
        boolean internalTraceMode = isInternalTraceExecution(execution);
        boolean skipNumberBounds = internalTraceMode;
        for (MesProBatchRecordExecutionFieldAuditChange change : command.getChanges()) {
            SnapshotField field = fields.get(changeKey(change));
            if (field == null) {
                throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_FIELD_NOT_DECLARED);
            }
            if (change.getValueType() == null || StrUtil.isBlank(change.getNewValueDisplay())) {
                throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_VALUE_TYPE_UNSUPPORTED);
            }
            ObjectNode cell = currentCells.get(cellKey(change.getRowIndex(), change.getColumnIndex()));
            validateSnapshotValueType(change, field);
            validateNotSignatureCellValue(change, field);
            JsonNode oldNode = oldValueNode(cell == null ? field.defaultValue() : cell.get("value"),
                    change.getValueType());
            String oldValueJson = canonicalizeOldValue(change.getValueType(), oldNode);
            String oldValueHash = MesProBatchRecordExecutionFieldAuditHasher.hashCanonicalTypedValue(oldValueJson);
            validateExpectedOld(change, oldValueJson, oldValueHash);
            String newValueJson = canonicalizeNewValue(change.getValueType(), change.getNewValueJson());
            JsonNode newValueNode = MesProBatchRecordExecutionFieldAuditHasher.toJsonNode(change.getNewValueJson());
            if (recordbookMode) {
                validateRecordbookSourceShape(field, newValueNode);
            } else {
                validateSnapshotConstraints(change, field, newValueNode, skipNumberBounds);
            }
            JsonNode batchRecordValueNode = recordbookMode
                    ? resolveBatchRecordValueNode(field, newValueNode)
                    : newValueNode;
            String batchRecordValueJson = canonicalizeNewValue(change.getValueType(), batchRecordValueNode);
            String batchRecordValueDisplay = recordbookMode && !Objects.equals(newValueJson, batchRecordValueJson)
                    ? displayValue(batchRecordValueNode)
                    : StrUtil.trim(change.getNewValueDisplay());
            NonBlockingLimitWarning nonBlockingLimitWarning = internalTraceMode
                    ? nonBlockingLimitWarning(field, newValueNode)
                    : null;
            String newValueHash = MesProBatchRecordExecutionFieldAuditHasher.hashCanonicalTypedValue(batchRecordValueJson);
            if (Objects.equals(oldValueJson, batchRecordValueJson)
                    && (!recordbookMode || Objects.equals(oldValueJson, newValueJson))) {
                continue;
            }
            result.add(new ResolvedChange(change, field, command.getReasonCategory(), command.getReasonText(),
                    oldValueJson, oldValueDisplay(cell, oldNode), oldValueHash,
                    batchRecordValueJson, batchRecordValueDisplay, newValueHash,
                    recordbookMode ? newValueJson : null,
                    recordbookMode ? StrUtil.trim(change.getNewValueDisplay()) : null,
                    recordbookMode ? batchRecordValueJson : null,
                    recordbookMode ? batchRecordValueDisplay : null,
                    nonBlockingLimitWarning));
        }
        return result;
    }

    private boolean isRecordbookUnrestrictedMode(MesProBatchRecordExecutionFieldAuditSaveChangesCommand command) {
        return command != null
                && FILL_CARRIER_RECORDBOOK.equals(StrUtil.trim(command.getFillCarrier()))
                && FILL_MODE_RECORDBOOK_UNRESTRICTED.equals(StrUtil.trim(command.getFillMode()));
    }

    private void validateNotSignatureCellValue(MesProBatchRecordExecutionFieldAuditChange change,
                                               SnapshotField field) {
        if (change.getValueType() == MesProBatchRecordExecutionFieldAuditValueType.SIGNATURE
                || field.valueType() == MesProBatchRecordExecutionFieldAuditValueType.SIGNATURE
                || "signature".equalsIgnoreCase(StrUtil.trim(field.component()))) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_SIGNATURE_CELL_VALUE_FORBIDDEN);
        }
    }

    private void validateSnapshotValueType(MesProBatchRecordExecutionFieldAuditChange change, SnapshotField field) {
        if (field.valueType() == null) {
            return;
        }
        if (change.getValueType() != field.valueType()) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_VALUE_TYPE_UNSUPPORTED);
        }
    }

    private void validateSnapshotConstraints(MesProBatchRecordExecutionFieldAuditChange change,
                                             SnapshotField field,
                                             JsonNode newValueNode,
                                             boolean skipNumberBounds) {
        if (field.valueType() == null) {
            return;
        }
        if (field.required() && isBlankValue(newValueNode)) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_VALUE_CONSTRAINT_VIOLATION,
                    field.label() + " 为必填项");
        }
        switch (field.valueType()) {
            case NUMBER -> validateNumberConstraints(field, newValueNode, skipNumberBounds);
            case STRING -> validateStringConstraints(field, newValueNode);
            case DATE -> validateDateConstraint(field, newValueNode);
            case DATETIME -> validateDatetimeConstraint(field, newValueNode);
            case BOOLEAN -> {
                if (!newValueNode.isBoolean()) {
                    throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_VALUE_TYPE_UNSUPPORTED);
                }
            }
            default -> throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_VALUE_TYPE_UNSUPPORTED);
        }
    }

    private void validateRecordbookSourceShape(SnapshotField field, JsonNode newValueNode) {
        if (field.valueType() == null) {
            return;
        }
        switch (field.valueType()) {
            case NUMBER -> {
                if (!newValueNode.isNumber()) {
                    throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_VALUE_TYPE_UNSUPPORTED);
                }
            }
            case STRING, DATE, DATETIME -> {
                if (!newValueNode.isTextual()) {
                    throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_VALUE_TYPE_UNSUPPORTED);
                }
            }
            case BOOLEAN -> {
                if (!newValueNode.isBoolean()) {
                    throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_VALUE_TYPE_UNSUPPORTED);
                }
            }
            default -> throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_VALUE_TYPE_UNSUPPORTED);
        }
    }

    private boolean isInternalTraceExecution(MesProBatchRecordExecutionDO execution) {
        return execution != null && VALIDATION_PROFILE_INTERNAL.equals(StrUtil.trim(execution.getValidationProfile()));
    }

    private NonBlockingLimitWarning nonBlockingLimitWarning(SnapshotField field, JsonNode valueNode) {
        if (field.valueType() != MesProBatchRecordExecutionFieldAuditValueType.NUMBER || !valueNode.isNumber()) {
            return null;
        }
        BigDecimal value = MesProBatchRecordExecutionFieldAuditHasher.normalizeNumber(valueNode.decimalValue());
        BigDecimal min = decimalConstraint(field.constraints(), "min");
        BigDecimal max = decimalConstraint(field.constraints(), "max");
        boolean belowMin = min != null && value.compareTo(min) < 0;
        boolean aboveMax = max != null && value.compareTo(max) > 0;
        return belowMin || aboveMax ? new NonBlockingLimitWarning(value, min, max) : null;
    }

    private JsonNode resolveBatchRecordValueNode(SnapshotField field, JsonNode sourceNode) {
        if (field.valueType() != MesProBatchRecordExecutionFieldAuditValueType.NUMBER || !sourceNode.isNumber()) {
            return sourceNode;
        }
        BigDecimal value = MesProBatchRecordExecutionFieldAuditHasher.normalizeNumber(sourceNode.decimalValue());
        BigDecimal min = decimalConstraint(field.constraints(), "min");
        BigDecimal max = decimalConstraint(field.constraints(), "max");
        if (min != null && value.compareTo(min) < 0) {
            value = min;
        }
        if (max != null && value.compareTo(max) > 0) {
            value = max;
        }
        return DecimalNode.valueOf(MesProBatchRecordExecutionFieldAuditHasher.normalizeNumber(value));
    }

    private void validateNumberConstraints(SnapshotField field, JsonNode valueNode, boolean skipNumberBounds) {
        if (!valueNode.isNumber()) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_VALUE_TYPE_UNSUPPORTED);
        }
        BigDecimal value = MesProBatchRecordExecutionFieldAuditHasher.normalizeNumber(valueNode.decimalValue());
        if (!skipNumberBounds) {
            BigDecimal min = decimalConstraint(field.constraints(), "min");
            if (min != null && value.compareTo(min) < 0) {
                throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_VALUE_CONSTRAINT_VIOLATION,
                        field.label() + " 小于最小值 " + min.toPlainString());
            }
            BigDecimal max = decimalConstraint(field.constraints(), "max");
            if (max != null && value.compareTo(max) > 0) {
                throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_VALUE_CONSTRAINT_VIOLATION,
                        field.label() + " 大于最大值 " + max.toPlainString());
            }
        }
        Integer scale = integerConstraint(field.constraints(), "scale");
        if (scale != null && Math.max(value.scale(), 0) > scale) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_VALUE_CONSTRAINT_VIOLATION,
                    field.label() + " 小数位超过 " + scale);
        }
        Integer precision = integerConstraint(field.constraints(), "precision");
        if (precision != null && value.precision() > precision) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_VALUE_CONSTRAINT_VIOLATION,
                    field.label() + " 总位数超过 " + precision);
        }
    }

    private void validateStringConstraints(SnapshotField field, JsonNode valueNode) {
        if (!valueNode.isTextual()) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_VALUE_TYPE_UNSUPPORTED);
        }
        int length = valueNode.textValue().length();
        Integer minLength = integerConstraint(field.constraints(), "minLength");
        if (minLength != null && length < minLength) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_VALUE_CONSTRAINT_VIOLATION,
                    field.label() + " 长度小于 " + minLength);
        }
        Integer maxLength = integerConstraint(field.constraints(), "maxLength");
        if (maxLength != null && length > maxLength) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_VALUE_CONSTRAINT_VIOLATION,
                    field.label() + " 长度超过 " + maxLength);
        }
    }

    private void validateDateConstraint(SnapshotField field, JsonNode valueNode) {
        if (!valueNode.isTextual()) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_VALUE_TYPE_UNSUPPORTED);
        }
        try {
            LocalDate.parse(valueNode.textValue(), dateFormatter(field, DEFAULT_DATE_FORMATTER));
        } catch (DateTimeParseException ex) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_VALUE_CONSTRAINT_VIOLATION,
                    field.label() + " 日期格式不符合规则");
        }
    }

    private void validateDatetimeConstraint(SnapshotField field, JsonNode valueNode) {
        if (!valueNode.isTextual()) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_VALUE_TYPE_UNSUPPORTED);
        }
        try {
            LocalDateTime.parse(valueNode.textValue(), dateFormatter(field, DEFAULT_DATETIME_FORMATTER));
        } catch (DateTimeParseException ex) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_VALUE_CONSTRAINT_VIOLATION,
                    field.label() + " 日期时间格式不符合规则");
        }
    }

    private void validateExpectedOld(MesProBatchRecordExecutionFieldAuditChange change,
                                     String oldValueJson,
                                     String oldValueHash) {
        if (StrUtil.isNotBlank(change.getExpectedOldValueHash())
                && !Objects.equals(change.getExpectedOldValueHash(), oldValueHash)) {
            throwOldValueMismatch(change, "expectedHash=" + trimForMessage(change.getExpectedOldValueHash())
                    + ", currentHash=" + trimForMessage(oldValueHash));
        }
        if (change.getExpectedOldValueJson() != null) {
            String expected = canonicalizeOldValue(change.getValueType(),
                    MesProBatchRecordExecutionFieldAuditHasher.toJsonNode(change.getExpectedOldValueJson()));
            if (!Objects.equals(expected, oldValueJson)) {
                throwOldValueMismatch(change, "expected=" + trimForMessage(expected)
                        + ", current=" + trimForMessage(oldValueJson));
            }
        }
    }

    private void throwOldValueMismatch(MesProBatchRecordExecutionFieldAuditChange change, String detail) {
        throw exception0(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_OLD_VALUE_MISMATCH.getCode(),
                "字段旧值与当前记录不一致：rowIndex={}, columnIndex={}, valueType={}, {}",
                change.getRowIndex(), change.getColumnIndex(), change.getValueType(), detail);
    }

    private String trimForMessage(String value) {
        if (value == null || value.length() <= 80) {
            return value;
        }
        return value.substring(0, 80) + "...";
    }

    private String canonicalizeNewValue(MesProBatchRecordExecutionFieldAuditValueType valueType, Object value) {
        try {
            return MesProBatchRecordExecutionFieldAuditHasher.canonicalizeTypedValue(valueType, value);
        } catch (IllegalArgumentException e) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_VALUE_TYPE_UNSUPPORTED);
        }
    }

    private String canonicalizeOldValue(MesProBatchRecordExecutionFieldAuditValueType valueType, JsonNode oldNode) {
        if (oldNode == null || oldNode.isNull() || oldNode.isMissingNode()) {
            return "null";
        }
        if (valueType == MesProBatchRecordExecutionFieldAuditValueType.NULL) {
            return MesProBatchRecordExecutionFieldAuditHasher.canonicalize(oldNode);
        }
        try {
            return MesProBatchRecordExecutionFieldAuditHasher.canonicalizeTypedValue(valueType, oldNode);
        } catch (IllegalArgumentException e) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_VALUE_TYPE_UNSUPPORTED);
        }
    }

    private JsonNode oldValueNode(JsonNode value, MesProBatchRecordExecutionFieldAuditValueType valueType) {
        if (value == null || value.isNull() || value.isMissingNode()) {
            return NullNode.instance;
        }
        if (!value.isTextual()) {
            return value;
        }
        String text = value.textValue();
        if (StrUtil.isBlank(text)
                && valueType != MesProBatchRecordExecutionFieldAuditValueType.STRING
                && valueType != MesProBatchRecordExecutionFieldAuditValueType.NULL) {
            if (valueType == MesProBatchRecordExecutionFieldAuditValueType.BOOLEAN) {
                return BooleanNode.FALSE;
            }
            return NullNode.instance;
        }
        if (valueType == MesProBatchRecordExecutionFieldAuditValueType.NUMBER) {
            return DecimalNode.valueOf(MesProBatchRecordExecutionFieldAuditHasher.normalizeNumber(new BigDecimal(text)));
        }
        if (valueType == MesProBatchRecordExecutionFieldAuditValueType.BOOLEAN) {
            if (!"true".equalsIgnoreCase(text) && !"false".equalsIgnoreCase(text)) {
                throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_VALUE_TYPE_UNSUPPORTED);
            }
            return JsonNodeFactory.instance.booleanNode(Boolean.parseBoolean(text));
        }
        if (valueType == MesProBatchRecordExecutionFieldAuditValueType.NULL) {
            return value;
        }
        if (valueType == MesProBatchRecordExecutionFieldAuditValueType.JSON) {
            try {
                return JsonUtils.getObjectMapper().readTree(text);
            } catch (JsonProcessingException e) {
                throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_VALUE_TYPE_UNSUPPORTED);
            }
        }
        return TextNode.valueOf(text);
    }

    private String buildAfterCellValuesJson(Map<String, ObjectNode> currentCells, List<ResolvedChange> changes) {
        for (ResolvedChange change : changes) {
            String key = cellKey(change.change().getRowIndex(), change.change().getColumnIndex());
            ObjectNode cell = currentCells.get(key);
            if (cell == null) {
                cell = JsonNodeFactory.instance.objectNode();
                cell.put("rowIndex", change.change().getRowIndex());
                cell.put("columnIndex", change.change().getColumnIndex());
                currentCells.put(key, cell);
            }
            if (change.change().getValueType() == MesProBatchRecordExecutionFieldAuditValueType.NULL) {
                cell.set("value", NullNode.instance);
            } else if (change.field().valueType() != null) {
                cell.put("valueType", change.field().valueType().name());
                cell.set("value", parseCanonicalValueJson(change.newValueJson()));
                cell.put("valueDisplay", change.newValueDisplay());
                cell.put("valueHash", change.newValueHash());
                if (StrUtil.isNotBlank(change.field().unit())) {
                    cell.put("unit", change.field().unit());
                } else {
                    cell.remove("unit");
                }
            } else {
                cell.put("value", change.newValueDisplay());
            }
        }
        ArrayNode array = JsonNodeFactory.instance.arrayNode();
        currentCells.values().stream()
                .sorted(Comparator
                        .comparing((ObjectNode cell) -> cell.path("rowIndex").asInt())
                        .thenComparing(cell -> cell.path("columnIndex").asInt()))
                .forEach(array::add);
        return array.toString();
    }

    private List<MesProBatchRecordExecutionFieldAuditItemDO> buildItems(
            MesProBatchRecordExecutionDO execution,
            List<ResolvedChange> changes,
            Long batchId,
            Long tenantId,
            MesProBatchRecordExecutionFieldAuditSignatureResult signature,
            String signatureProjectionHash,
            String afterCellValuesHash) {
        List<MesProBatchRecordExecutionFieldAuditItemDO> items = new ArrayList<>();
        String previousHash = execution.getFieldAuditHeadHash();
        long revision = execution.getFieldAuditRevision();
        String executionSnapshotHash = MesProBatchRecordExecutionFieldAuditHasher.hashExecutionSnapshot(
                execution.getExecutionSnapshotJson());
        for (int index = 0; index < changes.size(); index++) {
            ResolvedChange change = changes.get(index);
            revision++;
            MesProBatchRecordExecutionFieldAuditItemHashInput hashInput =
                    MesProBatchRecordExecutionFieldAuditItemHashInput.builder()
                            .fieldPath(change.field().fieldPath())
                            .fieldKey(change.field().fieldKey())
                            .rowIndex(change.field().rowIndex())
                            .columnIndex(change.field().columnIndex())
                            .valueType(change.change().getValueType())
                            .oldValueJson(change.oldValueJson())
                            .oldValueDisplay(change.oldValueDisplay())
                            .oldValueHash(change.oldValueHash())
                            .newValueJson(change.newValueJson())
                            .newValueDisplay(change.newValueDisplay())
                            .newValueHash(change.newValueHash())
                            .recordbookValueJson(change.recordbookValueJson())
                            .recordbookValueDisplay(change.recordbookValueDisplay())
                            .batchRecordValueJson(change.batchRecordValueJson())
                            .batchRecordValueDisplay(change.batchRecordValueDisplay())
                            .reasonCategory(change.reasonCategory())
                            .reasonText(change.reasonText())
                            .actorId(signature.getActorId())
                            .actorName(signature.getActorName())
                            .signatureProjectionHash(signatureProjectionHash)
                            .previousHash(previousHash)
                            .changedAt(signature.getSignedAt())
                            .build();
            String auditHash = MesProBatchRecordExecutionFieldAuditHasher.hashItem(hashInput);
            items.add(MesProBatchRecordExecutionFieldAuditItemDO.builder()
                    .id(IdWorker.getId())
                    .auditBatchId(batchId)
                    .executionId(execution.getId())
                    .fieldAuditRevision(revision)
                    .batchItemIndex(index + 1)
                    .fieldPath(change.field().fieldPath())
                    .fieldKey(change.field().fieldKey())
                    .fieldLabel(change.field().label())
                    .rowIndex(change.field().rowIndex())
                    .columnIndex(change.field().columnIndex())
                    .component(change.field().component())
                    .valueType(change.change().getValueType().name())
                    .oldValueJson(change.oldValueJson())
                    .oldValueDisplay(change.oldValueDisplay())
                    .oldValueHash(change.oldValueHash())
                    .newValueJson(change.newValueJson())
                    .newValueDisplay(change.newValueDisplay())
                    .newValueHash(change.newValueHash())
                    .recordbookValueJson(change.recordbookValueJson())
                    .recordbookValueDisplay(change.recordbookValueDisplay())
                    .batchRecordValueJson(change.batchRecordValueJson())
                    .batchRecordValueDisplay(change.batchRecordValueDisplay())
                    .reasonCategory(change.reasonCategory())
                    .reasonText(change.reasonText())
                    .actorId(signature.getActorId())
                    .actorName(signature.getActorName())
                    .signatureId(signature.getSignatureId())
                    .signatureProjectionHash(signatureProjectionHash)
                    .previousHash(previousHash)
                    .auditHash(auditHash)
                    .beforeCellValuesHash(execution.getCellValuesHash())
                    .afterCellValuesHash(afterCellValuesHash)
                    .executionSnapshotHash(executionSnapshotHash)
                    .changedAt(signature.getSignedAt())
                    .tenantId(tenantId)
                    .build());
            previousHash = auditHash;
        }
        return items;
    }

    private String buildFieldAuditOperationMetadata(MesProBatchRecordExecutionDO execution,
                                                    List<ResolvedChange> resolvedChanges,
                                                    int changedFieldCount,
                                                    Long signatureId,
                                                    boolean goldenFingerMode) {
        ObjectNode metadata = JsonNodeFactory.instance.objectNode();
        metadata.put("changedFieldCount", changedFieldCount);
        if (signatureId == null) {
            metadata.putNull("signatureId");
        } else {
            metadata.put("signatureId", signatureId);
        }
        metadata.put("recordCategory", execution.getRecordCategory());
        metadata.put("validationProfile", execution.getValidationProfile());
        metadata.put("goldenFingerMode", goldenFingerMode);
        if (goldenFingerMode) {
            ArrayNode bypassedChecks = metadata.putArray("bypassedChecks");
            bypassedChecks.add("ASSIGNEE");
            bypassedChecks.add("FILL_SCOPE");
            bypassedChecks.add("ACTION_LOCKS");
        }

        boolean recordbookMode = resolvedChanges.stream()
                .anyMatch(change -> change.recordbookValueJson() != null || change.batchRecordValueJson() != null);
        if (!recordbookMode) {
            ArrayNode warnings = metadata.putArray("nonBlockingLimitWarnings");
            for (ResolvedChange change : resolvedChanges) {
                NonBlockingLimitWarning warning = change.nonBlockingLimitWarning();
                if (warning == null) {
                    continue;
                }
                ObjectNode warningNode = warnings.addObject();
                warningNode.put("fieldPath", change.field().fieldPath());
                warningNode.put("fieldKey", change.field().fieldKey());
                warningNode.put("fieldLabel", change.field().label());
                putNullableInteger(warningNode, "rowIndex", change.field().rowIndex());
                putNullableInteger(warningNode, "columnIndex", change.field().columnIndex());
                warningNode.put("value", warning.value());
                putNullableDecimal(warningNode, "min", warning.min());
                putNullableDecimal(warningNode, "max", warning.max());
                warningNode.put("reasonCategory", change.reasonCategory());
                warningNode.put("reasonText", change.reasonText());
                warningNode.put("recordCategory", execution.getRecordCategory());
                warningNode.put("validationProfile", execution.getValidationProfile());
            }
            metadata.put("nonBlockingLimitWarningCount", warnings.size());
        }
        return metadata.toString();
    }

    private void putNullableInteger(ObjectNode node, String key, Integer value) {
        if (value == null) {
            node.putNull(key);
        } else {
            node.put(key, value);
        }
    }

    private void putNullableDecimal(ObjectNode node, String key, BigDecimal value) {
        if (value == null) {
            node.putNull(key);
        } else {
            node.put(key, value);
        }
    }

    private void insertBatch(MesProBatchRecordExecutionDO execution,
                             MesProBatchRecordExecutionFieldAuditSaveChangesCommand command,
                             String requestHash,
                             Long tenantId,
                             MesProBatchRecordExecutionFieldAuditSignatureResult signature,
                             String signatureChallengeHash,
                             String signatureProjectionHash,
                             String afterCellValuesHash,
                             long afterRevision,
                             Long batchId,
                             String newHeadHash,
                             MesProBatchRecordExecutionFieldAuditHashVerification verification,
                             int fieldCount) {
        MesProBatchRecordExecutionFieldAuditBatchDO batch =
                MesProBatchRecordExecutionFieldAuditBatchDO.builder()
                        .id(batchId)
                        .executionId(execution.getId())
                        .idempotencyKey(command.getIdempotencyKey())
                        .requestHash(requestHash)
                        .actionType(ACTION_FIELD_CHANGE)
                        .reasonCategory(command.getReasonCategory())
                        .reasonText(command.getReasonText())
                        .fieldCount(fieldCount)
                        .actorId(signature.getActorId())
                        .actorName(signature.getActorName())
                        .signatureId(signature.getSignatureId())
                        .signatureChallengeHash(signatureChallengeHash)
                        .signatureProjectionHash(signatureProjectionHash)
                        .baseCellValuesHash(command.getBaseCellValuesHash())
                        .beforeCellValuesHash(execution.getCellValuesHash())
                        .afterCellValuesHash(afterCellValuesHash)
                        .baseFieldAuditRevision(command.getBaseFieldAuditRevision())
                        .beforeFieldAuditRevision(execution.getFieldAuditRevision())
                        .afterFieldAuditRevision(afterRevision)
                        .baseFieldAuditHeadHash(command.getBaseFieldAuditHeadHash())
                        .previousHeadHash(execution.getFieldAuditHeadHash())
                        .newHeadHash(newHeadHash)
                        .hashVerificationJson(JsonUtils.toJsonString(verification))
                        .changedAt(signature.getSignedAt())
                        .tenantId(tenantId)
                        .build();
        if (batchMapper.insert(batch) <= 0) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_PERSIST_FAILED);
        }
    }

    private void insertItems(List<MesProBatchRecordExecutionFieldAuditItemDO> items) {
        for (MesProBatchRecordExecutionFieldAuditItemDO item : items) {
            if (itemMapper.insert(item) <= 0) {
                throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_PERSIST_FAILED);
            }
        }
    }

    private void attachSignature(Long executionId, Long signatureId, Long batchId, String signatureChallengeHash,
                                 long afterRevision, String newHeadHash, String afterCellValuesHash) {
        try {
            signatureService.attachFieldChangeSignature(new MesProBatchRecordExecutionFieldAuditSignatureAttachCommand()
                    .setSignatureId(signatureId)
                    .setExecutionId(executionId)
                    .setAuditBatchId(batchId)
                    .setSignatureChallengeHash(signatureChallengeHash)
                    .setFieldAuditRevision(afterRevision)
                    .setFieldAuditHeadHash(newHeadHash)
                    .setCellValuesHash(afterCellValuesHash));
        } catch (RuntimeException e) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_SIGNATURE_BIND_FAILED);
        }
    }

    private void bindAttachmentChanges(MesProBatchRecordExecutionFieldAuditSaveChangesCommand command,
                                       MesProBatchRecordExecutionDO execution,
                                       Long batchId,
                                       MesProBatchRecordExecutionFieldAuditSignatureResult signature) {
        if (command.getAttachmentChanges() == null || command.getAttachmentChanges().isEmpty()) {
            return;
        }
        for (MesProBatchRecordExecutionFieldAuditAttachmentChange change : command.getAttachmentChanges()) {
            MesProBatchRecordExecutionAttachmentBindCommand attachmentCommand = new MesProBatchRecordExecutionAttachmentBindCommand()
                    .setExecutionId(execution.getId())
                    .setWorkTaskId(change.getWorkTaskId())
                    .setAuditBatchId(batchId)
                    .setSignatureId(signature.getSignatureId())
                    .setRowIndex(change.getRowIndex())
                    .setColumnIndex(change.getColumnIndex())
                    .setFieldKey(change.getFieldKey())
                    .setFieldPath(change.getFieldPath())
                    .setFieldLabel(change.getFieldLabel())
                    .setAttachmentType(change.getAttachmentType())
                    .setAttachmentGroupKey(change.getAttachmentGroupKey())
                    .setFileId(change.getFileId())
                    .setFileUrl(change.getFileUrl())
                    .setStorageConfigId(change.getStorageConfigId())
                    .setStoragePath(change.getStoragePath())
                    .setFileName(change.getFileName())
                    .setContentType(change.getContentType())
                    .setFileSize(change.getFileSize())
                    .setSha256(change.getSha256())
                    .setStorageRetentionJson(change.getStorageRetentionJson())
                    .setExpectedPreviousAttachmentHash(change.getExpectedPreviousAttachmentHash())
                    .setOperatorId(signature.getActorId())
                    .setOperatorName(signature.getActorName())
                    .setReasonCategory(command.getReasonCategory())
                    .setReasonText(command.getReasonText());
            String action = StrUtil.blankToDefault(StrUtil.trim(change.getAttachmentAction()), "ADD");
            if ("ADD".equals(action)) {
                attachmentService.bindAttachment(attachmentCommand);
            } else if ("REPLACE".equals(action)) {
                attachmentService.replaceAttachment(attachmentCommand);
            } else {
                throw exception(PRO_BATCH_RECORD_EXECUTION_ATTACHMENT_FILE_METADATA_INVALID);
            }
        }
    }

    private boolean signatureBindingMatchesBatch(MesProBatchRecordExecutionFieldAuditBatchDO batch) {
        if (batch == null || batch.getSignatureId() == null) {
            return false;
        }
        MesProBatchRecordExecutionSignatureDO signature = signatureMapper.selectById(batch.getSignatureId());
        return signature != null
                && Objects.equals(signature.getId(), batch.getSignatureId())
                && Objects.equals(signature.getExecutionId(), batch.getExecutionId())
                && Objects.equals(signature.getActionType(), ACTION_FIELD_CHANGE)
                && Objects.equals(signature.getAuditBatchId(), batch.getId())
                && Objects.equals(signature.getSignatureChallengeHash(), batch.getSignatureChallengeHash())
                && Objects.equals(signature.getFieldAuditRevision(), batch.getAfterFieldAuditRevision())
                && Objects.equals(signature.getFieldAuditHeadHash(), batch.getNewHeadHash())
                && Objects.equals(signature.getCellValuesHash(), batch.getAfterCellValuesHash())
                && Objects.equals(signature.getActorId(), batch.getActorId())
                && Objects.equals(signature.getActorName(), batch.getActorName())
                && Objects.equals(signature.getReasonCategory(), batch.getReasonCategory())
                && Objects.equals(signature.getReason(), batch.getReasonText());
    }

    private MesProBatchRecordExecutionFieldAuditItemHashInput toHashInput(
            MesProBatchRecordExecutionFieldAuditItemDO item) {
        return MesProBatchRecordExecutionFieldAuditItemHashInput.builder()
                .fieldPath(item.getFieldPath())
                .fieldKey(item.getFieldKey())
                .rowIndex(item.getRowIndex())
                .columnIndex(item.getColumnIndex())
                .valueType(MesProBatchRecordExecutionFieldAuditValueType.valueOf(item.getValueType()))
                .oldValueJson(item.getOldValueJson())
                .oldValueDisplay(item.getOldValueDisplay())
                .oldValueHash(item.getOldValueHash())
                .newValueJson(item.getNewValueJson())
                .newValueDisplay(item.getNewValueDisplay())
                .newValueHash(item.getNewValueHash())
                .recordbookValueJson(item.getRecordbookValueJson())
                .recordbookValueDisplay(item.getRecordbookValueDisplay())
                .batchRecordValueJson(item.getBatchRecordValueJson())
                .batchRecordValueDisplay(item.getBatchRecordValueDisplay())
                .reasonCategory(item.getReasonCategory())
                .reasonText(item.getReasonText())
                .actorId(item.getActorId())
                .actorName(item.getActorName())
                .signatureProjectionHash(item.getSignatureProjectionHash())
                .previousHash(item.getPreviousHash())
                .changedAt(item.getChangedAt())
                .build();
    }

    private Map<String, ObjectNode> parseCellValues(String cellValuesJson) {
        try {
            JsonNode root = JsonUtils.getObjectMapper().readTree(cellValuesJson);
            if (!root.isArray()) {
                throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_BASELINE_MISSING);
            }
            Map<String, ObjectNode> cells = new HashMap<>();
            for (JsonNode node : root) {
                if (!node.isObject()) {
                    throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_BASELINE_MISSING);
                }
                Integer rowIndex = integer(node, "rowIndex");
                Integer columnIndex = integer(node, "columnIndex");
                if (rowIndex == null || columnIndex == null) {
                    throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_BASELINE_MISSING);
                }
                cells.put(cellKey(rowIndex, columnIndex), ((ObjectNode) node).deepCopy());
            }
            return cells;
        } catch (JsonProcessingException e) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_BASELINE_MISSING);
        }
    }

    private String displayValue(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return "";
        }
        if (node.isTextual()) {
            return node.textValue();
        }
        return MesProBatchRecordExecutionFieldAuditHasher.canonicalize(node);
    }

    private JsonNode parseOptionalJson(String value) {
        if (value == null) {
            return null;
        }
        try {
            return JsonUtils.getObjectMapper().readTree(value);
        } catch (JsonProcessingException e) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_CHAIN_INVALID);
        }
    }

    private JsonNode parseCanonicalValueJson(String value) {
        try {
            return JsonUtils.getObjectMapper().readTree(value);
        } catch (JsonProcessingException e) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_VALUE_TYPE_UNSUPPORTED);
        }
    }

    private String oldValueDisplay(ObjectNode cell, JsonNode node) {
        if (cell != null && cell.get("valueDisplay") != null && !cell.get("valueDisplay").isNull()) {
            return cell.get("valueDisplay").asText();
        }
        return displayValue(node);
    }

    private boolean booleanValue(JsonNode node, String fieldName) {
        JsonNode child = node.get(fieldName);
        return child != null && child.isBoolean() && child.booleanValue();
    }

    private boolean isBlankValue(JsonNode node) {
        return node == null || node.isNull() || node.isMissingNode()
                || (node.isTextual() && StrUtil.isBlank(node.textValue()));
    }

    private BigDecimal decimalConstraint(JsonNode constraints, String key) {
        JsonNode value = constraints == null ? null : constraints.get(key);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isNumber()) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_VALUE_CONSTRAINT_VIOLATION,
                    key + " 必须为数字");
        }
        return MesProBatchRecordExecutionFieldAuditHasher.normalizeNumber(value.decimalValue());
    }

    private Integer integerConstraint(JsonNode constraints, String key) {
        JsonNode value = constraints == null ? null : constraints.get(key);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.canConvertToInt() || value.asInt() < 0) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_VALUE_CONSTRAINT_VIOLATION,
                    key + " 必须为非负整数");
        }
        return value.asInt();
    }

    private DateTimeFormatter dateFormatter(SnapshotField field, DateTimeFormatter defaultFormatter) {
        JsonNode format = field.constraints() == null ? null : field.constraints().get("format");
        if (format == null || !format.isTextual() || StrUtil.isBlank(format.textValue())) {
            return defaultFormatter;
        }
        return DateTimeFormatter.ofPattern(format.textValue());
    }

    private static String changeKey(MesProBatchRecordExecutionFieldAuditChange change) {
        return fieldKey(change.getFieldPath(), change.getFieldKey(), change.getRowIndex(), change.getColumnIndex());
    }

    private static String fieldKey(String fieldPath, String fieldKey, Integer rowIndex, Integer columnIndex) {
        return StrUtil.nullToEmpty(fieldPath) + "|" + StrUtil.nullToEmpty(fieldKey) + "|"
                + rowIndex + "|" + columnIndex;
    }

    private static String cellKey(Integer rowIndex, Integer columnIndex) {
        return rowIndex + "|" + columnIndex;
    }

    private static String text(JsonNode node, String fieldName) {
        JsonNode child = node.get(fieldName);
        return child == null || child.isNull() ? null : child.asText();
    }

    private static Integer integer(JsonNode node, String fieldName) {
        JsonNode child = node.get(fieldName);
        return child == null || !child.canConvertToInt() ? null : child.asInt();
    }

    private Long currentTenantId() {
        return TenantContextHolder.getRequiredTenantId();
    }

    private record SnapshotField(String fieldPath, String fieldKey, String label, Integer rowIndex,
                                 Integer columnIndex, String component, JsonNode defaultValue,
                                 MesProBatchRecordExecutionFieldAuditValueType valueType,
                                 boolean required,
                                 JsonNode constraints,
                                 String unit) {
    }

    private record ResolvedChange(MesProBatchRecordExecutionFieldAuditChange change,
                                  SnapshotField field,
                                  String reasonCategory,
                                  String reasonText,
                                  String oldValueJson,
                                  String oldValueDisplay,
                                   String oldValueHash,
                                   String newValueJson,
                                   String newValueDisplay,
                                   String newValueHash,
                                   String recordbookValueJson,
                                   String recordbookValueDisplay,
                                   String batchRecordValueJson,
                                   String batchRecordValueDisplay,
                                   NonBlockingLimitWarning nonBlockingLimitWarning) {
    }

    private record NonBlockingLimitWarning(BigDecimal value,
                                           BigDecimal min,
                                           BigDecimal max) {
    }
}
