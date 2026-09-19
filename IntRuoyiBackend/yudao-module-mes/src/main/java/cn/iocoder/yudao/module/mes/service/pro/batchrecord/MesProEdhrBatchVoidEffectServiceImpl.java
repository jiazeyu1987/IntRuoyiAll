package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrRecordChangeRequestReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrRecordChangeRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionArchiveDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionOriginDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionSignatureDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrRecordChangeEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionArchiveMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionOriginMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionSignatureMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrRecordChangeEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_APPROVAL_PROCESS_DEFINITION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_CHANGE_DUPLICATED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_CHANGE_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_CHANGE_REASON_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_CHANGE_STATUS_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_SIGNATURE_PERSIST_FAILED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_STATUS_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_RELEASE_STATUS_INVALID;

@Service
public class MesProEdhrBatchVoidEffectServiceImpl implements MesProEdhrBatchVoidEffectService {

    private static final int BATCH_STATUS_VOIDED = 60;
    private static final String CHANGE_TYPE_VOID = "VOID";
    private static final String CHANGE_STATUS_SUBMITTED = "SUBMITTED";
    private static final String CHANGE_STATUS_DRAFT = "DRAFT";
    private static final String CHANGE_STATUS_EFFECTIVE = "EFFECTIVE";
    private static final String CHANGE_STATUS_REJECTED = "REJECTED";
    private static final String TARGET_SCOPE_BATCH = "BATCH";
    private static final String BATCH_SIGNATURE_ACTION_VOID_REQUEST = "BATCH_VOID_REQUEST";
    private static final String SIGNATURE_MODE_PASSWORD = "PASSWORD";
    private static final Set<String> OPEN_CHANGE_STATUSES = Set.of("DRAFT", "SUBMITTED", "APPROVED");

    @Resource
    private MesProEdhrBatchExecutionMapper batchExecutionMapper;
    @Resource
    private MesProEdhrBatchExecutionArchiveMapper batchArchiveMapper;
    @Resource
    private MesProEdhrBatchExecutionSignatureMapper batchSignatureMapper;
    @Resource
    private MesProEdhrBatchExecutionOriginMapper batchExecutionOriginMapper;
    @Resource
    private MesProEdhrRecordChangeEventMapper changeEventMapper;
    @Resource
    private MesProEdhrReleaseTransactionMapper releaseTransactionMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private MesProEdhrGoldenFingerPermissionService goldenFingerPermissionService;
    @Resource
    private MesProEdhrWorkTaskService workTaskService;
    @Resource
    private MesProBatchRecordExecutionSignatureService signatureService;
    @Resource
    private MesProEdhrOperationAuditService operationAuditService;

    @Override
    public EdhrRecordChangeRespVO precheckPlatformVoidBatchExecution(EdhrRecordChangeRequestReqVO reqVO) {
        validateReason(reqVO.getReasonCategory(), reqVO.getReasonText());
        MesProEdhrBatchExecutionDO batch = requireBatchExecution(reqVO.getBatchExecutionId());
        requireReleaseActionUnlocked(batch.getId());
        if (Integer.valueOf(BATCH_STATUS_VOIDED).equals(batch.getStatus())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_STATUS_INVALID);
        }
        assertNoOpenBatchChange(batch.getId(), CHANGE_TYPE_VOID);
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EdhrRecordChangeRespVO requestPlatformVoidBatchExecution(EdhrRecordChangeRequestReqVO reqVO,
                                                                    String bpmProcessInstanceId) {
        validateReason(reqVO.getReasonCategory(), reqVO.getReasonText());
        MesProEdhrBatchExecutionDO batch = requireBatchExecution(reqVO.getBatchExecutionId());
        requireReleaseActionUnlocked(batch.getId());
        if (Integer.valueOf(BATCH_STATUS_VOIDED).equals(batch.getStatus())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_STATUS_INVALID);
        }
        assertNoOpenBatchChange(batch.getId(), CHANGE_TYPE_VOID);
        if (StrUtil.isBlank(bpmProcessInstanceId)) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_APPROVAL_PROCESS_DEFINITION_NOT_EXISTS,
                    MesProEdhrRecordChangeServiceImpl.BATCH_EXECUTION_VOID_PROCESS_DEFINITION_KEY);
        }

        Long actorUserId = SecurityFrameworkUtils.getLoginUserId();
        Long signatureId = recordBatchVoidRequestSignature(batch, actorUserId, reqVO.getPassword(), reqVO.getComment());
        MesProEdhrBatchExecutionArchiveDO archive = latestBatchArchive(batch.getId());
        MesProEdhrRecordChangeEventDO event = buildBatchVoidChangeEvent(reqVO, batch, archive, signatureId, actorUserId);
        event.setBpmProcessInstanceId(bpmProcessInstanceId);
        changeEventMapper.insert(event);
        recordBatchVoidOperation("BATCH_VOID_REQUEST", "申请批次作废", event, batch, actorUserId);
        return toResp(event);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EdhrRecordChangeRespVO executeDirectPlatformVoidBatchExecution(EdhrRecordChangeRequestReqVO reqVO,
                                                                         Long actorUserId) {
        validateReason(reqVO.getReasonCategory(), reqVO.getReasonText());
        MesProEdhrBatchExecutionDO batch = requireBatchExecution(reqVO.getBatchExecutionId());
        requireReleaseActionUnlocked(batch.getId());
        if (Integer.valueOf(BATCH_STATUS_VOIDED).equals(batch.getStatus())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_STATUS_INVALID);
        }
        assertNoOpenBatchChange(batch.getId(), CHANGE_TYPE_VOID);

        Long signatureId = recordBatchVoidRequestSignature(batch, actorUserId, reqVO.getPassword(), reqVO.getComment());
        MesProEdhrBatchExecutionArchiveDO archive = latestBatchArchive(batch.getId());
        MesProEdhrRecordChangeEventDO event = buildBatchVoidChangeEvent(reqVO, batch, archive, signatureId, actorUserId);
        changeEventMapper.insert(event);
        recordBatchVoidOperation("BATCH_VOID_REQUEST", "申请批次作废", event, batch, actorUserId);
        return approveVoidBatchExecutionByBpm(event, actorUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EdhrRecordChangeRespVO handleVoidBatchExecutionApprovalCallback(String approvalInstanceId,
                                                                           String approvalEventId,
                                                                           String approvalResult,
                                                                           String rejectReason,
                                                                           Long actorUserId) {
        MesProEdhrRecordChangeEventDO event = requireBatchVoidEventByProcessInstanceId(approvalInstanceId);
        if (!CHANGE_STATUS_SUBMITTED.equals(event.getChangeStatus())) {
            return toResp(event);
        }
        String normalizedApprovalResult = StrUtil.trimToEmpty(approvalResult).toUpperCase(Locale.ROOT);
        if (Objects.equals("APPROVED", normalizedApprovalResult)) {
            return approveVoidBatchExecutionByBpm(event, actorUserId);
        }
        if (Objects.equals("REJECTED", normalizedApprovalResult) || Objects.equals("CANCELLED", normalizedApprovalResult)
                || Objects.equals("CANCELED", normalizedApprovalResult)) {
            LocalDateTime now = now();
            changeEventMapper.updateById(new MesProEdhrRecordChangeEventDO()
                    .setId(event.getId())
                    .setChangeStatus(CHANGE_STATUS_REJECTED)
                    .setApprovedBy(actorUserId)
                    .setApprovedAt(now)
                    .setRemark(StrUtil.blankToDefault(StrUtil.trim(rejectReason), event.getRemark())));
            recordBatchVoidOperation("BATCH_VOID_REJECTED", "驳回批次作废", event,
                    requireBatchExecution(event.getBatchExecutionId()), actorUserId);
            return toResp(changeEventMapper.selectById(event.getId()));
        }
        throw exception(PRO_BATCH_RECORD_EXECUTION_CHANGE_STATUS_INVALID);
    }

    private MesProEdhrBatchExecutionDO requireBatchExecution(Long batchExecutionId) {
        MesProEdhrBatchExecutionDO batch = batchExecutionId == null ? null : batchExecutionMapper.selectById(batchExecutionId);
        if (batch == null) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_NOT_EXISTS);
        }
        return batch;
    }

    private MesProEdhrRecordChangeEventDO requireBatchVoidEventByProcessInstanceId(String approvalInstanceId) {
        MesProEdhrRecordChangeEventDO event = StrUtil.isBlank(approvalInstanceId) ? null : changeEventMapper.selectOne(
                new LambdaQueryWrapperX<MesProEdhrRecordChangeEventDO>()
                        .eq(MesProEdhrRecordChangeEventDO::getBpmProcessInstanceId, approvalInstanceId)
                        .eq(MesProEdhrRecordChangeEventDO::getChangeType, CHANGE_TYPE_VOID)
                        .eq(MesProEdhrRecordChangeEventDO::getTargetScope, TARGET_SCOPE_BATCH)
                        .last("LIMIT 1"));
        if (event == null) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_CHANGE_NOT_EXISTS);
        }
        return event;
    }

    private void validateReason(String reasonCategory, String reasonText) {
        if (StrUtil.isBlank(reasonCategory) || StrUtil.isBlank(reasonText)) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_CHANGE_REASON_REQUIRED);
        }
    }

    private void requireReleaseActionUnlocked(Long batchExecutionId) {
        if (batchExecutionId == null) {
            return;
        }
        MesProEdhrReleaseTransactionDO releaseTransaction =
                releaseTransactionMapper.selectByBatchExecutionId(batchExecutionId);
        if (!hasGoldenFingerActionBypass()
                && releaseTransaction != null
                && MesProEdhrReleaseServiceImpl.STATUS_PENDING_APPROVAL.equals(releaseTransaction.getReleaseStatus())) {
            throw exception(PRO_EDHR_RELEASE_STATUS_INVALID);
        }
    }

    private boolean hasGoldenFingerActionBypass() {
        Long currentUserId = SecurityFrameworkUtils.getLoginUserId();
        return goldenFingerPermissionService != null
                && goldenFingerPermissionService.hasGoldenFingerPermission(currentUserId);
    }

    private void assertNoOpenBatchChange(Long batchExecutionId, String changeType) {
        Long count = changeEventMapper.selectCount(new LambdaQueryWrapperX<MesProEdhrRecordChangeEventDO>()
                .eq(MesProEdhrRecordChangeEventDO::getBatchExecutionId, batchExecutionId)
                .eq(MesProEdhrRecordChangeEventDO::getChangeType, changeType)
                .in(MesProEdhrRecordChangeEventDO::getChangeStatus, OPEN_CHANGE_STATUSES));
        if (count != null && count > 0) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_CHANGE_DUPLICATED);
        }
    }

    private MesProEdhrBatchExecutionArchiveDO latestBatchArchive(Long batchExecutionId) {
        return batchArchiveMapper.selectListByBatchExecutionId(batchExecutionId).stream().findFirst().orElse(null);
    }

    private MesProEdhrRecordChangeEventDO buildBatchVoidChangeEvent(EdhrRecordChangeRequestReqVO reqVO,
                                                                    MesProEdhrBatchExecutionDO batch,
                                                                    MesProEdhrBatchExecutionArchiveDO archive,
                                                                    Long requestSignatureId,
                                                                    Long actorUserId) {
        return MesProEdhrRecordChangeEventDO.builder()
                .changeCode("EDHR-VOID-BATCH-" + batch.getId() + "-" + System.currentTimeMillis())
                .changeType(CHANGE_TYPE_VOID)
                .targetScope(TARGET_SCOPE_BATCH)
                .batchExecutionId(batch.getId())
                .sourceArchiveId(archive == null ? null : archive.getId())
                .changeStatus(CHANGE_STATUS_SUBMITTED)
                .reasonCategory(StrUtil.trim(reqVO.getReasonCategory()))
                .reasonText(StrUtil.trim(reqVO.getReasonText()))
                .requestedBy(actorUserId)
                .requestedAt(now())
                .requestSignatureId(requestSignatureId)
                .previousStatus(String.valueOf(batch.getStatus()))
                .newStatus(String.valueOf(BATCH_STATUS_VOIDED))
                .previousHeadHash(batch.getAggregateHash())
                .newHeadHash(batch.getAggregateHash())
                .previousArchiveHash(archive == null ? null : archive.getContentHash())
                .newArchiveHash(archive == null ? null : archive.getContentHash())
                .remark(StrUtil.blankToDefault(StrUtil.trim(reqVO.getComment()), null))
                .build();
    }

    private Long recordBatchVoidRequestSignature(MesProEdhrBatchExecutionDO batch, Long actorUserId,
                                                 String password, String comment) {
        if (batch == null || batch.getId() == null || actorUserId == null || StrUtil.isBlank(password)) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_SIGNATURE_PERSIST_FAILED);
        }
        return signatureService.recordBatchVoidRequestSignature(actorUserId, batch.getId(), password, comment,
                batch.getAggregateHash());
    }

    private EdhrRecordChangeRespVO approveVoidBatchExecutionByBpm(MesProEdhrRecordChangeEventDO event,
                                                                  Long actorUserId) {
        MesProEdhrBatchExecutionDO batch = requireBatchExecution(event.getBatchExecutionId());
        if (Integer.valueOf(BATCH_STATUS_VOIDED).equals(batch.getStatus())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_STATUS_INVALID);
        }
        LocalDateTime now = now();
        batchExecutionMapper.updateById(new MesProEdhrBatchExecutionDO()
                .setId(batch.getId())
                .setStatus(BATCH_STATUS_VOIDED));
        batchExecutionMapper.clearActiveContextKey(batch.getId());
        MesProEdhrBatchExecutionArchiveDO archive = latestBatchArchive(batch.getId());
        if (archive != null) {
            batchArchiveMapper.updateById(new MesProEdhrBatchExecutionArchiveDO()
                    .setId(archive.getId())
                    .setArchiveValidFlag(Boolean.FALSE)
                    .setArchiveValidStatus("VOIDED")
                    .setInvalidatedByChangeEventId(event.getId()));
        }
        workTaskService.cancelActiveTasksByBatch(batch.getId(), buildBatchVoidWorkTaskCancelReason(event));
        changeEventMapper.updateById(new MesProEdhrRecordChangeEventDO()
                .setId(event.getId())
                .setChangeStatus(CHANGE_STATUS_EFFECTIVE)
                .setApprovedBy(actorUserId)
                .setApprovedAt(now)
                .setEffectiveAt(now)
                .setPreviousArchiveHash(archive == null ? event.getPreviousArchiveHash() : archive.getContentHash())
                .setNewArchiveHash(archive == null ? event.getNewArchiveHash() : archive.getContentHash()));
        recordBatchVoidOperation("BATCH_VOID_EFFECTIVE", "批次作废生效", event, batch, actorUserId);
        return toResp(changeEventMapper.selectById(event.getId()));
    }

    private void recordBatchVoidOperation(String operationType, String actionName,
                                          MesProEdhrRecordChangeEventDO event,
                                          MesProEdhrBatchExecutionDO batch,
                                          Long actorUserId) {
        Long activeOrderId = resolveActiveOrderId(batch.getId());
        if (activeOrderId == null) {
            return;
        }
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("activeOrderId", activeOrderId);
        metadata.put("batchExecutionId", batch.getId());
        metadata.put("changeEventId", event.getId());
        metadata.put("changeCode", event.getChangeCode());
        metadata.put("changeType", event.getChangeType());
        metadata.put("changeStatus", event.getChangeStatus());
        metadata.put("reasonCategory", event.getReasonCategory());
        metadata.put("reasonText", event.getReasonText());
        metadata.put("previousStatus", event.getPreviousStatus());
        metadata.put("newStatus", event.getNewStatus());
        metadata.put("requestSignatureId", event.getRequestSignatureId());
        metadata.put("sourceArchiveId", event.getSourceArchiveId());
        String metadataJson = JSONUtil.toJsonStr(metadata);
        operationAuditService.recordInCallerTransaction(new MesProEdhrOperationAuditCommand()
                .setRequestId("BATCH-VOID-" + operationType + "-" + event.getId())
                .setObjectType("BATCH_EXECUTION_VOID_CHANGE")
                .setObjectId(String.valueOf(event.getId()))
                .setBatchExecutionId(batch.getId())
                .setOperationType(operationType)
                .setActionName(actionName)
                .setActorUserId(actorUserId)
                .setActorUsername(SecurityFrameworkUtils.getLoginUserNickname())
                .setPermissionCode("mes:pro-edhr:batch-void:update")
                .setPermissionDecision("ALLOW")
                .setResultStatus("SUCCESS")
                .setAfterSummaryHash(DigestUtil.sha256Hex(metadataJson))
                .setMetadataJson(metadataJson)
                .setOccurredAt(now()));
    }

    private Long resolveActiveOrderId(Long batchExecutionId) {
        List<Long> activeOrderIds = batchExecutionOriginMapper.selectListByBatchExecutionId(batchExecutionId).stream()
                .map(MesProEdhrBatchExecutionOriginDO::getActiveOrderId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (activeOrderIds.size() > 1) {
            throw new IllegalStateException("批次作废存在多个不同的活跃订单来源：" + batchExecutionId);
        }
        return activeOrderIds.isEmpty() ? null : activeOrderIds.get(0);
    }

    private String buildBatchVoidWorkTaskCancelReason(MesProEdhrRecordChangeEventDO event) {
        String reasonText = StrUtil.blankToDefault(StrUtil.trim(event.getReasonText()), StrUtil.trim(event.getRemark()));
        if (StrUtil.isBlank(reasonText)) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_CHANGE_REASON_REQUIRED);
        }
        return "批次已作废：" + reasonText;
    }

    private EdhrRecordChangeRespVO toResp(MesProEdhrRecordChangeEventDO event) {
        return BeanUtils.toBean(event, EdhrRecordChangeRespVO.class);
    }

    private LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
