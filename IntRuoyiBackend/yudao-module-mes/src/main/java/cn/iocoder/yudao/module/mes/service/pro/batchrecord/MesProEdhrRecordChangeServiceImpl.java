package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrRecordChangeApproveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrRecordChangePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrRecordChangeRequestReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrRecordChangeRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionArchiveDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionArchiveDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionSignatureDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrRecordChangeEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionArchiveMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionArchiveMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionSignatureMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrRecordChangeEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_APPROVAL_PROCESS_DEFINITION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_APPROVAL_NOT_ALLOWED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_CHANGE_DUPLICATED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_CHANGE_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_CHANGE_REASON_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_CHANGE_STATUS_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_SIGNATURE_PERSIST_FAILED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_STATUS_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_RELEASE_STATUS_INVALID;

@Service
public class MesProEdhrRecordChangeServiceImpl implements MesProEdhrRecordChangeService {

    public static final String BATCH_EXECUTION_VOID_PROCESS_DEFINITION_KEY =
            "mes-edhr-batch-execution-void-v1";
    private static final String BATCH_EXECUTION_VOID_BUSINESS_TYPE = "EDHR_BATCH_EXECUTION_VOID";
    private static final int STATUS_APPROVED = 3;
    private static final int STATUS_VOIDED = 4;
    private static final int STATUS_REOPENED = 5;
    private static final int BATCH_STATUS_CLOSED = 30;
    private static final int BATCH_STATUS_ARCHIVED = 40;
    private static final int BATCH_STATUS_REJECTED = 50;
    private static final int BATCH_STATUS_VOIDED = 60;
    private static final int BATCH_STATUS_REOPENED = 70;
    private static final String CHANGE_TYPE_VOID = "VOID";
    private static final String CHANGE_TYPE_REOPEN = "REOPEN";
    private static final String CHANGE_TYPE_SUPPLEMENT = "SUPPLEMENT";
    private static final String CHANGE_STATUS_SUBMITTED = "SUBMITTED";
    private static final String CHANGE_STATUS_DRAFT = "DRAFT";
    private static final String CHANGE_STATUS_EFFECTIVE = "EFFECTIVE";
    private static final String CHANGE_STATUS_REJECTED = "REJECTED";
    private static final String TARGET_SCOPE_EXECUTION = "EXECUTION";
    private static final String TARGET_SCOPE_BATCH = "BATCH";
    private static final String BATCH_SIGNATURE_ACTION_VOID_REQUEST = "BATCH_VOID_REQUEST";
    private static final String SIGNATURE_MODE_PASSWORD = "PASSWORD";
    private static final Set<String> OPEN_CHANGE_STATUSES = Set.of("DRAFT", "SUBMITTED", "APPROVED");

    @Resource
    private MesProBatchRecordExecutionMapper executionMapper;
    @Resource
    private MesProBatchRecordExecutionArchiveMapper archiveMapper;
    @Resource
    private MesProEdhrBatchExecutionMapper batchExecutionMapper;
    @Resource
    private MesProEdhrBatchExecutionArchiveMapper batchArchiveMapper;
    @Resource
    private MesProEdhrBatchExecutionSignatureMapper batchSignatureMapper;
    @Resource
    private MesProEdhrRecordChangeEventMapper changeEventMapper;
    @Resource
    private MesProEdhrReleaseTransactionMapper releaseTransactionMapper;
    @Resource
    private MesProBatchRecordExecutionSignatureService signatureService;
    @Resource
    private BpmProcessInstanceApi processInstanceApi;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private MesProEdhrGoldenFingerPermissionService goldenFingerPermissionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EdhrRecordChangeRespVO requestVoidExecution(EdhrRecordChangeRequestReqVO reqVO) {
        validateReason(reqVO.getReasonCategory(), reqVO.getReasonText());
        requireReleaseActionUnlocked(reqVO.getBatchExecutionId());
        MesProBatchRecordExecutionDO execution = requireExecution(reqVO.getExecutionId());
        if (!Integer.valueOf(STATUS_APPROVED).equals(execution.getStatus())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_STATUS_INVALID);
        }
        assertNoOpenChange(execution.getId(), CHANGE_TYPE_VOID);

        Long signatureId = signatureService.recordSubmitSignature(execution.getId(), reqVO.getPassword(), reqVO.getComment());
        MesProBatchRecordExecutionArchiveDO archive = archiveMapper.selectLatestByExecutionId(execution.getId());
        MesProEdhrRecordChangeEventDO event = MesProEdhrRecordChangeEventDO.builder()
                .changeCode("EDHR-VOID-" + execution.getId() + "-" + System.currentTimeMillis())
                .changeType(CHANGE_TYPE_VOID)
                .targetScope(TARGET_SCOPE_EXECUTION)
                .batchExecutionId(reqVO.getBatchExecutionId())
                .executionId(execution.getId())
                .sourceArchiveId(archive == null ? null : archive.getId())
                .changeStatus(CHANGE_STATUS_SUBMITTED)
                .reasonCategory(StrUtil.trim(reqVO.getReasonCategory()))
                .reasonText(StrUtil.trim(reqVO.getReasonText()))
                .requestedBy(SecurityFrameworkUtils.getLoginUserId())
                .requestedAt(now())
                .requestSignatureId(signatureId)
                .previousStatus(String.valueOf(execution.getStatus()))
                .newStatus(String.valueOf(STATUS_VOIDED))
                .previousHeadHash(execution.getFieldAuditHeadHash())
                .newHeadHash(execution.getFieldAuditHeadHash())
                .previousArchiveHash(archive == null ? null : archive.getSha256())
                .newArchiveHash(archive == null ? null : archive.getSha256())
                .remark(StrUtil.blankToDefault(StrUtil.trim(reqVO.getComment()), null))
                .build();
        changeEventMapper.insert(event);
        return toResp(event);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EdhrRecordChangeRespVO approveVoidExecution(EdhrRecordChangeApproveReqVO reqVO) {
        MesProEdhrRecordChangeEventDO event = requireChangeEvent(reqVO.getChangeEventId());
        if (!CHANGE_TYPE_VOID.equals(event.getChangeType()) || !CHANGE_STATUS_SUBMITTED.equals(event.getChangeStatus())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_CHANGE_STATUS_INVALID);
        }
        MesProBatchRecordExecutionDO execution = requireExecution(event.getExecutionId());
        if (!Integer.valueOf(STATUS_APPROVED).equals(execution.getStatus())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_STATUS_INVALID);
        }

        Long approvalSignatureId = signatureService.recordApprovalSignature(
                new MesProBatchRecordExecutionApprovalSignatureCommand()
                        .setExecutionId(execution.getId())
                        .setPassword(reqVO.getPassword())
                        .setComment(reqVO.getComment())
                        .setApprovalResult(MesProBatchRecordExecutionSignatureService.ACTION_APPROVE)
                        .setReason(event.getReasonText())
                        .setFieldAuditRevision(execution.getFieldAuditRevision())
                        .setFieldAuditHeadHash(execution.getFieldAuditHeadHash())
                        .setCellValuesHash(execution.getCellValuesHash()));

        LocalDateTime now = now();
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(execution.getId())
                .setStatus(STATUS_VOIDED)
                .setVoidedByChangeEventId(event.getId()));
        MesProBatchRecordExecutionArchiveDO archive = archiveMapper.selectLatestByExecutionId(execution.getId());
        if (archive != null) {
            archiveMapper.updateById(new MesProBatchRecordExecutionArchiveDO()
                    .setId(archive.getId())
                    .setArchiveValidFlag(Boolean.FALSE)
                    .setArchiveValidStatus("VOIDED")
                    .setInvalidatedByChangeEventId(event.getId()));
        }
        changeEventMapper.updateById(new MesProEdhrRecordChangeEventDO()
                .setId(event.getId())
                .setChangeStatus(CHANGE_STATUS_EFFECTIVE)
                .setApprovedBy(SecurityFrameworkUtils.getLoginUserId())
                .setApprovedAt(now)
                .setApprovalSignatureId(approvalSignatureId)
                .setEffectiveAt(now)
                .setPreviousArchiveHash(archive == null ? event.getPreviousArchiveHash() : archive.getSha256())
                .setNewArchiveHash(archive == null ? event.getNewArchiveHash() : archive.getSha256()));
        return toResp(changeEventMapper.selectById(event.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EdhrRecordChangeRespVO requestVoidBatchExecution(EdhrRecordChangeRequestReqVO reqVO) {
        validateReason(reqVO.getReasonCategory(), reqVO.getReasonText());
        MesProEdhrBatchExecutionDO batch = requireBatchExecution(reqVO.getBatchExecutionId());
        requireReleaseActionUnlocked(batch.getId());
        if (Integer.valueOf(BATCH_STATUS_VOIDED).equals(batch.getStatus())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_STATUS_INVALID);
        }
        assertNoOpenBatchChange(batch.getId(), CHANGE_TYPE_VOID);

        Long actorUserId = SecurityFrameworkUtils.getLoginUserId();
        Long signatureId = recordBatchVoidRequestSignature(batch, actorUserId, reqVO.getPassword(), reqVO.getComment());
        MesProEdhrBatchExecutionArchiveDO archive = latestBatchArchive(batch.getId());
        MesProEdhrRecordChangeEventDO event = buildBatchVoidChangeEvent(reqVO, batch, archive, signatureId, actorUserId);
        String processInstanceId = processInstanceApi.createProcessInstance(actorUserId,
                buildBatchVoidProcessCreateReq(batch, event, reqVO, actorUserId));
        if (StrUtil.isBlank(processInstanceId)) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_APPROVAL_PROCESS_DEFINITION_NOT_EXISTS,
                    BATCH_EXECUTION_VOID_PROCESS_DEFINITION_KEY);
        }
        event.setBpmProcessInstanceId(processInstanceId);
        changeEventMapper.insert(event);
        return toResp(event);
    }

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
                    BATCH_EXECUTION_VOID_PROCESS_DEFINITION_KEY);
        }

        Long actorUserId = SecurityFrameworkUtils.getLoginUserId();
        Long signatureId = recordBatchVoidRequestSignature(batch, actorUserId, reqVO.getPassword(), reqVO.getComment());
        MesProEdhrBatchExecutionArchiveDO archive = latestBatchArchive(batch.getId());
        MesProEdhrRecordChangeEventDO event = buildBatchVoidChangeEvent(reqVO, batch, archive, signatureId, actorUserId);
        event.setBpmProcessInstanceId(bpmProcessInstanceId);
        changeEventMapper.insert(event);
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
        return approveVoidBatchExecutionByBpm(event, null, actorUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EdhrRecordChangeRespVO withdrawVoidBatchExecution(EdhrRecordChangeApproveReqVO reqVO) {
        MesProEdhrRecordChangeEventDO event = requireChangeEvent(reqVO.getChangeEventId());
        if (!CHANGE_TYPE_VOID.equals(event.getChangeType()) || !TARGET_SCOPE_BATCH.equals(event.getTargetScope())
                || !CHANGE_STATUS_SUBMITTED.equals(event.getChangeStatus())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_CHANGE_STATUS_INVALID);
        }
        if (StrUtil.isBlank(event.getBpmProcessInstanceId())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_APPROVAL_PROCESS_DEFINITION_NOT_EXISTS,
                    BATCH_EXECUTION_VOID_PROCESS_DEFINITION_KEY);
        }
        Long actorUserId = SecurityFrameworkUtils.getLoginUserId();
        if (!Objects.equals(event.getRequestedBy(), actorUserId)) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_APPROVAL_NOT_ALLOWED);
        }
        String reason = StrUtil.blankToDefault(StrUtil.trim(reqVO.getComment()), "申请人撤回作废申请");
        processInstanceApi.cancelProcessInstance(actorUserId, event.getBpmProcessInstanceId(), reason);

        MesProEdhrRecordChangeEventDO latest = changeEventMapper.selectById(event.getId());
        if (CHANGE_STATUS_SUBMITTED.equals(latest.getChangeStatus())) {
            LocalDateTime now = now();
            changeEventMapper.updateById(new MesProEdhrRecordChangeEventDO()
                    .setId(event.getId())
                    .setChangeStatus(CHANGE_STATUS_REJECTED)
                    .setApprovedBy(actorUserId)
                    .setApprovedAt(now)
                    .setBpmTaskId("BPM-WITHDRAW-" + event.getBpmProcessInstanceId())
                    .setRemark(reason));
        }
        return toResp(changeEventMapper.selectById(event.getId()));
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
            return approveVoidBatchExecutionByBpm(event, approvalEventId, actorUserId);
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
            return toResp(changeEventMapper.selectById(event.getId()));
        }
        throw exception(PRO_BATCH_RECORD_EXECUTION_CHANGE_STATUS_INVALID);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EdhrRecordChangeRespVO requestReopenBatch(EdhrRecordChangeRequestReqVO reqVO) {
        validateReason(reqVO.getReasonCategory(), reqVO.getReasonText());
        MesProEdhrBatchExecutionDO batch = requireBatchExecution(reqVO.getBatchExecutionId());
        requireReleaseActionUnlocked(batch.getId());
        if (!isReopenableBatchStatus(batch.getStatus())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_STATUS_INVALID);
        }
        assertNoOpenBatchChange(batch.getId(), CHANGE_TYPE_REOPEN);

        Long signatureId = signatureService.recordSubmitSignature(0L, reqVO.getPassword(), reqVO.getComment());
        MesProEdhrBatchExecutionArchiveDO archive = latestBatchArchive(batch.getId());
        MesProEdhrRecordChangeEventDO event = MesProEdhrRecordChangeEventDO.builder()
                .changeCode("EDHR-REOPEN-" + batch.getId() + "-" + System.currentTimeMillis())
                .changeType(CHANGE_TYPE_REOPEN)
                .targetScope(TARGET_SCOPE_BATCH)
                .batchExecutionId(batch.getId())
                .sourceArchiveId(archive == null ? null : archive.getId())
                .changeStatus(CHANGE_STATUS_SUBMITTED)
                .reasonCategory(StrUtil.trim(reqVO.getReasonCategory()))
                .reasonText(StrUtil.trim(reqVO.getReasonText()))
                .requestedBy(SecurityFrameworkUtils.getLoginUserId())
                .requestedAt(now())
                .requestSignatureId(signatureId)
                .previousStatus(String.valueOf(batch.getStatus()))
                .newStatus(String.valueOf(BATCH_STATUS_REOPENED))
                .previousHeadHash(batch.getAggregateHash())
                .newHeadHash(batch.getAggregateHash())
                .previousArchiveHash(archive == null ? null : archive.getContentHash())
                .newArchiveHash(archive == null ? null : archive.getContentHash())
                .remark(StrUtil.blankToDefault(StrUtil.trim(reqVO.getComment()), null))
                .build();
        changeEventMapper.insert(event);
        return toResp(event);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EdhrRecordChangeRespVO approveReopenBatch(EdhrRecordChangeApproveReqVO reqVO) {
        MesProEdhrRecordChangeEventDO event = requireChangeEvent(reqVO.getChangeEventId());
        if (!CHANGE_TYPE_REOPEN.equals(event.getChangeType()) || !CHANGE_STATUS_SUBMITTED.equals(event.getChangeStatus())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_CHANGE_STATUS_INVALID);
        }
        requireReleaseActionUnlocked(event.getBatchExecutionId());
        MesProEdhrBatchExecutionDO batch = requireBatchExecution(event.getBatchExecutionId());
        if (!isReopenableBatchStatus(batch.getStatus())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_STATUS_INVALID);
        }

        Long approvalSignatureId = signatureService.recordApprovalSignature(
                new MesProBatchRecordExecutionApprovalSignatureCommand()
                        .setExecutionId(0L)
                        .setPassword(reqVO.getPassword())
                        .setComment(reqVO.getComment())
                        .setApprovalResult(MesProBatchRecordExecutionSignatureService.ACTION_APPROVE)
                        .setReason(event.getReasonText()));

        LocalDateTime now = now();
        batchExecutionMapper.updateById(new MesProEdhrBatchExecutionDO()
                .setId(batch.getId())
                .setStatus(BATCH_STATUS_REOPENED));
        MesProEdhrBatchExecutionArchiveDO archive = latestBatchArchive(batch.getId());
        if (archive != null) {
            batchArchiveMapper.updateById(new MesProEdhrBatchExecutionArchiveDO()
                    .setId(archive.getId())
                    .setArchiveValidFlag(Boolean.FALSE)
                    .setArchiveValidStatus("SUPERSEDED")
                    .setInvalidatedByChangeEventId(event.getId()));
        }
        changeEventMapper.updateById(new MesProEdhrRecordChangeEventDO()
                .setId(event.getId())
                .setChangeStatus(CHANGE_STATUS_EFFECTIVE)
                .setApprovedBy(SecurityFrameworkUtils.getLoginUserId())
                .setApprovedAt(now)
                .setApprovalSignatureId(approvalSignatureId)
                .setEffectiveAt(now)
                .setPreviousArchiveHash(archive == null ? event.getPreviousArchiveHash() : archive.getContentHash())
                .setNewArchiveHash(archive == null ? event.getNewArchiveHash() : archive.getContentHash()));
        return toResp(changeEventMapper.selectById(event.getId()));
    }

    private boolean isReopenableBatchStatus(Integer status) {
        return Integer.valueOf(BATCH_STATUS_CLOSED).equals(status)
                || Integer.valueOf(BATCH_STATUS_REJECTED).equals(status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EdhrRecordChangeRespVO requestReopenExecution(EdhrRecordChangeRequestReqVO reqVO) {
        validateReason(reqVO.getReasonCategory(), reqVO.getReasonText());
        requireReleaseActionUnlocked(reqVO.getBatchExecutionId());
        MesProBatchRecordExecutionDO execution = requireExecution(reqVO.getExecutionId());
        if (!Integer.valueOf(STATUS_APPROVED).equals(execution.getStatus())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_STATUS_INVALID);
        }
        assertNoOpenChange(execution.getId(), CHANGE_TYPE_REOPEN);

        Long signatureId = signatureService.recordSubmitSignature(execution.getId(), reqVO.getPassword(), reqVO.getComment());
        MesProBatchRecordExecutionArchiveDO archive = archiveMapper.selectLatestByExecutionId(execution.getId());
        MesProEdhrRecordChangeEventDO event = buildExecutionChangeEvent(reqVO, execution, archive,
                CHANGE_TYPE_REOPEN, CHANGE_STATUS_SUBMITTED, STATUS_REOPENED, signatureId);
        changeEventMapper.insert(event);
        return toResp(event);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EdhrRecordChangeRespVO approveReopenExecution(EdhrRecordChangeApproveReqVO reqVO) {
        MesProEdhrRecordChangeEventDO event = requireChangeEvent(reqVO.getChangeEventId());
        if (!CHANGE_TYPE_REOPEN.equals(event.getChangeType()) || !TARGET_SCOPE_EXECUTION.equals(event.getTargetScope())
                || !CHANGE_STATUS_SUBMITTED.equals(event.getChangeStatus())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_CHANGE_STATUS_INVALID);
        }
        requireReleaseActionUnlocked(event.getBatchExecutionId());
        MesProBatchRecordExecutionDO execution = requireExecution(event.getExecutionId());
        if (!Integer.valueOf(STATUS_APPROVED).equals(execution.getStatus())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_STATUS_INVALID);
        }
        Long approvalSignatureId = signatureService.recordApprovalSignature(
                new MesProBatchRecordExecutionApprovalSignatureCommand()
                        .setExecutionId(execution.getId())
                        .setPassword(reqVO.getPassword())
                        .setComment(reqVO.getComment())
                        .setApprovalResult(MesProBatchRecordExecutionSignatureService.ACTION_APPROVE)
                        .setReason(event.getReasonText())
                        .setFieldAuditRevision(execution.getFieldAuditRevision())
                        .setFieldAuditHeadHash(execution.getFieldAuditHeadHash())
                        .setCellValuesHash(execution.getCellValuesHash()));

        LocalDateTime now = now();
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(execution.getId())
                .setStatus(STATUS_REOPENED)
                .setReopenedByChangeEventId(event.getId()));
        MesProBatchRecordExecutionArchiveDO archive = archiveMapper.selectLatestByExecutionId(execution.getId());
        if (archive != null) {
            archiveMapper.updateById(new MesProBatchRecordExecutionArchiveDO()
                    .setId(archive.getId())
                    .setArchiveValidFlag(Boolean.FALSE)
                    .setArchiveValidStatus("SUPERSEDED")
                    .setInvalidatedByChangeEventId(event.getId()));
        }
        changeEventMapper.updateById(new MesProEdhrRecordChangeEventDO()
                .setId(event.getId())
                .setChangeStatus(CHANGE_STATUS_EFFECTIVE)
                .setApprovedBy(SecurityFrameworkUtils.getLoginUserId())
                .setApprovedAt(now)
                .setApprovalSignatureId(approvalSignatureId)
                .setEffectiveAt(now)
                .setPreviousArchiveHash(archive == null ? event.getPreviousArchiveHash() : archive.getSha256())
                .setNewArchiveHash(archive == null ? event.getNewArchiveHash() : archive.getSha256()));
        return toResp(changeEventMapper.selectById(event.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EdhrRecordChangeRespVO requestSupplement(EdhrRecordChangeRequestReqVO reqVO) {
        validateReason(reqVO.getReasonCategory(), reqVO.getReasonText());
        requireReleaseActionUnlocked(reqVO.getBatchExecutionId());
        MesProBatchRecordExecutionDO execution = requireExecution(reqVO.getExecutionId());
        if (!Integer.valueOf(STATUS_APPROVED).equals(execution.getStatus())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_STATUS_INVALID);
        }
        assertNoOpenChange(execution.getId(), CHANGE_TYPE_SUPPLEMENT);

        Long signatureId = signatureService.recordSubmitSignature(execution.getId(), reqVO.getPassword(), reqVO.getComment());
        MesProBatchRecordExecutionArchiveDO archive = archiveMapper.selectLatestByExecutionId(execution.getId());
        MesProEdhrRecordChangeEventDO event = MesProEdhrRecordChangeEventDO.builder()
                .changeCode("EDHR-SUPPLEMENT-" + execution.getId() + "-" + System.currentTimeMillis())
                .changeType(CHANGE_TYPE_SUPPLEMENT)
                .targetScope(TARGET_SCOPE_EXECUTION)
                .batchExecutionId(reqVO.getBatchExecutionId())
                .executionId(execution.getId())
                .sourceArchiveId(archive == null ? null : archive.getId())
                .changeStatus(CHANGE_STATUS_SUBMITTED)
                .reasonCategory(StrUtil.trim(reqVO.getReasonCategory()))
                .reasonText(StrUtil.trim(reqVO.getReasonText()))
                .requestedBy(SecurityFrameworkUtils.getLoginUserId())
                .requestedAt(now())
                .requestSignatureId(signatureId)
                .previousStatus(String.valueOf(execution.getStatus()))
                .newStatus(String.valueOf(execution.getStatus()))
                .previousHeadHash(execution.getFieldAuditHeadHash())
                .newHeadHash(execution.getFieldAuditHeadHash())
                .previousArchiveHash(archive == null ? null : archive.getSha256())
                .newArchiveHash(archive == null ? null : archive.getSha256())
                .remark(StrUtil.blankToDefault(StrUtil.trim(reqVO.getComment()), null))
                .build();
        changeEventMapper.insert(event);
        return toResp(event);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EdhrRecordChangeRespVO saveSupplementDraft(EdhrRecordChangeRequestReqVO reqVO) {
        validateReason(reqVO.getReasonCategory(), reqVO.getReasonText());
        requireReleaseActionUnlocked(reqVO.getBatchExecutionId());
        MesProBatchRecordExecutionDO execution = requireExecution(reqVO.getExecutionId());
        if (!Integer.valueOf(STATUS_APPROVED).equals(execution.getStatus())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_STATUS_INVALID);
        }
        assertNoOpenChange(execution.getId(), CHANGE_TYPE_SUPPLEMENT);
        MesProBatchRecordExecutionArchiveDO archive = archiveMapper.selectLatestByExecutionId(execution.getId());
        MesProEdhrRecordChangeEventDO event = buildExecutionChangeEvent(reqVO, execution, archive,
                CHANGE_TYPE_SUPPLEMENT, CHANGE_STATUS_DRAFT, execution.getStatus(), null);
        changeEventMapper.insert(event);
        return toResp(event);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EdhrRecordChangeRespVO submitSupplement(EdhrRecordChangeApproveReqVO reqVO) {
        MesProEdhrRecordChangeEventDO event = requireChangeEvent(reqVO.getChangeEventId());
        if (!CHANGE_TYPE_SUPPLEMENT.equals(event.getChangeType()) || !CHANGE_STATUS_DRAFT.equals(event.getChangeStatus())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_CHANGE_STATUS_INVALID);
        }
        requireReleaseActionUnlocked(event.getBatchExecutionId());
        MesProBatchRecordExecutionDO execution = requireExecution(event.getExecutionId());
        Long signatureId = signatureService.recordSubmitSignature(execution.getId(), reqVO.getPassword(), reqVO.getComment());
        changeEventMapper.updateById(new MesProEdhrRecordChangeEventDO()
                .setId(event.getId())
                .setChangeStatus(CHANGE_STATUS_SUBMITTED)
                .setRequestedBy(SecurityFrameworkUtils.getLoginUserId())
                .setRequestedAt(now())
                .setRequestSignatureId(signatureId)
                .setRemark(StrUtil.blankToDefault(StrUtil.trim(reqVO.getComment()), event.getRemark())));
        return toResp(changeEventMapper.selectById(event.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EdhrRecordChangeRespVO approveSupplement(EdhrRecordChangeApproveReqVO reqVO) {
        MesProEdhrRecordChangeEventDO event = requireChangeEvent(reqVO.getChangeEventId());
        if (!CHANGE_TYPE_SUPPLEMENT.equals(event.getChangeType()) || !CHANGE_STATUS_SUBMITTED.equals(event.getChangeStatus())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_CHANGE_STATUS_INVALID);
        }
        requireReleaseActionUnlocked(event.getBatchExecutionId());
        MesProBatchRecordExecutionDO execution = requireExecution(event.getExecutionId());
        if (!Integer.valueOf(STATUS_APPROVED).equals(execution.getStatus())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_STATUS_INVALID);
        }

        Long approvalSignatureId = signatureService.recordApprovalSignature(
                new MesProBatchRecordExecutionApprovalSignatureCommand()
                        .setExecutionId(execution.getId())
                        .setPassword(reqVO.getPassword())
                        .setComment(reqVO.getComment())
                        .setApprovalResult(MesProBatchRecordExecutionSignatureService.ACTION_APPROVE)
                        .setReason(event.getReasonText())
                        .setFieldAuditRevision(execution.getFieldAuditRevision())
                        .setFieldAuditHeadHash(execution.getFieldAuditHeadHash())
                        .setCellValuesHash(execution.getCellValuesHash()));

        LocalDateTime now = now();
        changeEventMapper.updateById(new MesProEdhrRecordChangeEventDO()
                .setId(event.getId())
                .setChangeStatus(CHANGE_STATUS_EFFECTIVE)
                .setApprovedBy(SecurityFrameworkUtils.getLoginUserId())
                .setApprovedAt(now)
                .setApprovalSignatureId(approvalSignatureId)
                .setEffectiveAt(now));
        return toResp(changeEventMapper.selectById(event.getId()));
    }

    @Override
    public PageResult<EdhrRecordChangeRespVO> getPage(EdhrRecordChangePageReqVO reqVO) {
        return BeanUtils.toBean(changeEventMapper.selectPage(reqVO), EdhrRecordChangeRespVO.class);
    }

    @Override
    public EdhrRecordChangeRespVO get(Long id) {
        return toResp(requireChangeEvent(id));
    }

    private MesProBatchRecordExecutionDO requireExecution(Long executionId) {
        MesProBatchRecordExecutionDO execution = executionId == null ? null : executionMapper.selectById(executionId);
        if (execution == null) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_NOT_EXISTS);
        }
        return execution;
    }

    private MesProEdhrRecordChangeEventDO requireChangeEvent(Long id) {
        MesProEdhrRecordChangeEventDO event = id == null ? null : changeEventMapper.selectById(id);
        if (event == null) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_CHANGE_NOT_EXISTS);
        }
        return event;
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

    private void assertNoOpenChange(Long executionId, String changeType) {
        Long count = changeEventMapper.selectCount(new LambdaQueryWrapperX<MesProEdhrRecordChangeEventDO>()
                .eq(MesProEdhrRecordChangeEventDO::getExecutionId, executionId)
                .eq(MesProEdhrRecordChangeEventDO::getChangeType, changeType)
                .in(MesProEdhrRecordChangeEventDO::getChangeStatus, OPEN_CHANGE_STATUSES));
        if (count != null && count > 0) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_CHANGE_DUPLICATED);
        }
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
        adminUserApi.validatePassword(actorUserId, password);
        LocalDateTime signedAt = now();
        MesProEdhrBatchExecutionSignatureDO signature = new MesProEdhrBatchExecutionSignatureDO()
                .setBatchExecutionId(batch.getId())
                .setActorId(actorUserId)
                .setActorName(String.valueOf(actorUserId))
                .setActionType(BATCH_SIGNATURE_ACTION_VOID_REQUEST)
                .setSignatureMode(SIGNATURE_MODE_PASSWORD)
                .setPasswordVerified(Boolean.TRUE)
                .setComment(StrUtil.blankToDefault(StrUtil.trim(comment), null))
                .setSignedAt(signedAt)
                .setSignatureDisplayAt(signedAt)
                .setSignatureTimeMode(MesProBatchRecordExecutionSignatureService.SIGNATURE_TIME_MODE_SERVER)
                .setSelectedTimeZone("Asia/Shanghai")
                .setSelectedTimeReason("")
                .setSelectedTimePolicyVersion(MesProBatchRecordExecutionSignatureService.SIGNATURE_TIME_POLICY_VERSION)
                .setSelectedTimeAuditHash(DigestUtil.sha256Hex(String.join("|",
                        MesProBatchRecordExecutionSignatureService.SIGNATURE_TIME_POLICY_VERSION,
                        value(batch.getId()),
                        BATCH_SIGNATURE_ACTION_VOID_REQUEST,
                        value(actorUserId),
                        value(signedAt),
                        MesProBatchRecordExecutionSignatureService.SIGNATURE_TIME_MODE_SERVER,
                        value(signedAt),
                        "",
                        "Asia/Shanghai",
                        "")))
                .setSignatureChallengeHash(DigestUtil.sha256Hex(batch.getId() + ":" + password))
                .setAggregateHash(batch.getAggregateHash());
        int inserted = batchSignatureMapper.insert(signature);
        if (inserted <= 0 || signature.getId() == null) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_SIGNATURE_PERSIST_FAILED);
        }
        return signature.getId();
    }

    private BpmProcessInstanceCreateReqDTO buildBatchVoidProcessCreateReq(MesProEdhrBatchExecutionDO batch,
                                                                          MesProEdhrRecordChangeEventDO event,
                                                                          EdhrRecordChangeRequestReqVO reqVO,
                                                                          Long actorUserId) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("businessType", BATCH_EXECUTION_VOID_BUSINESS_TYPE);
        variables.put("batchExecutionId", batch.getId());
        variables.put("batchExecutionCode", batch.getBatchExecutionCode());
        variables.put("workOrderId", batch.getWorkOrderId());
        variables.put("workOrderCode", batch.getWorkOrderCode());
        variables.put("batchCode", batch.getBatchCode());
        variables.put("routeId", batch.getRouteId());
        variables.put("routeCode", batch.getRouteCode());
        variables.put("reasonCategory", StrUtil.trim(reqVO.getReasonCategory()));
        variables.put("reasonText", StrUtil.trim(reqVO.getReasonText()));
        variables.put("comment", StrUtil.trim(reqVO.getComment()));
        variables.put("submittedBy", actorUserId);
        variables.put("submittedAt", event.getRequestedAt().toString());
        variables.put("tenantId", TenantContextHolder.getTenantId());
        return new BpmProcessInstanceCreateReqDTO()
                .setProcessDefinitionKey(BATCH_EXECUTION_VOID_PROCESS_DEFINITION_KEY)
                .setBusinessKey(BATCH_EXECUTION_VOID_BUSINESS_TYPE + ":" + batch.getId())
                .setVariables(variables);
    }

    private EdhrRecordChangeRespVO approveVoidBatchExecutionByBpm(MesProEdhrRecordChangeEventDO event,
                                                                  String approvalEventId,
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
        changeEventMapper.updateById(new MesProEdhrRecordChangeEventDO()
                .setId(event.getId())
                .setChangeStatus(CHANGE_STATUS_EFFECTIVE)
                .setApprovedBy(actorUserId)
                .setApprovedAt(now)
                .setEffectiveAt(now)
                .setPreviousArchiveHash(archive == null ? event.getPreviousArchiveHash() : archive.getContentHash())
                .setNewArchiveHash(archive == null ? event.getNewArchiveHash() : archive.getContentHash()));
        return toResp(changeEventMapper.selectById(event.getId()));
    }

    private MesProEdhrRecordChangeEventDO buildExecutionChangeEvent(EdhrRecordChangeRequestReqVO reqVO,
                                                                    MesProBatchRecordExecutionDO execution,
                                                                    MesProBatchRecordExecutionArchiveDO archive,
                                                                    String changeType,
                                                                    String changeStatus,
                                                                    Integer newStatus,
                                                                    Long requestSignatureId) {
        return MesProEdhrRecordChangeEventDO.builder()
                .changeCode("EDHR-" + changeType + "-" + execution.getId() + "-" + System.currentTimeMillis())
                .changeType(changeType)
                .targetScope(TARGET_SCOPE_EXECUTION)
                .batchExecutionId(reqVO.getBatchExecutionId())
                .executionId(execution.getId())
                .sourceArchiveId(archive == null ? null : archive.getId())
                .changeStatus(changeStatus)
                .reasonCategory(StrUtil.trim(reqVO.getReasonCategory()))
                .reasonText(StrUtil.trim(reqVO.getReasonText()))
                .requestedBy(requestSignatureId == null ? null : SecurityFrameworkUtils.getLoginUserId())
                .requestedAt(requestSignatureId == null ? null : now())
                .requestSignatureId(requestSignatureId)
                .previousStatus(String.valueOf(execution.getStatus()))
                .newStatus(String.valueOf(newStatus))
                .previousHeadHash(execution.getFieldAuditHeadHash())
                .newHeadHash(execution.getFieldAuditHeadHash())
                .previousArchiveHash(archive == null ? null : archive.getSha256())
                .newArchiveHash(archive == null ? null : archive.getSha256())
                .remark(StrUtil.blankToDefault(StrUtil.trim(reqVO.getComment()), null))
                .build();
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
