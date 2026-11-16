package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalContext;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalPolicyMode;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalPolicyResolution;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequest;
import cn.iocoder.yudao.module.bpm.businessapproval.service.BusinessApprovalOrchestrator;
import cn.iocoder.yudao.module.bpm.businessapproval.service.BusinessApprovalPolicyResolveService;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrRecordChangeApproveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchVoidApprovalResolutionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchVoidApprovalResolutionRespVO;
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
import java.util.LinkedHashMap;
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
    @jakarta.annotation.Resource private MesMarketReleaseAccess marketReleaseAccess;

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
    private static final Set<String> OPEN_CHANGE_STATUSES = Set.of("DRAFT", "SUBMITTED", "APPROVED", "RETURNED");

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
    private BusinessApprovalOrchestrator businessApprovalOrchestrator;
    @Resource
    private BusinessApprovalPolicyResolveService businessApprovalPolicyResolveService;
    @Resource
    private MesProEdhrGoldenFingerPermissionService goldenFingerPermissionService;
    @Resource
    private MesProEdhrWorkTaskService workTaskService;
    @Resource
    private MesEdhrRecordChangeTaskService recordChangeTaskService;
    @Resource private MesEdhrSupplementRevisionService supplementRevisionService;
    @Resource private MesProEdhrNonconformanceReviewService nonconformanceReviewService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EdhrRecordChangeRespVO decide(cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrRecordChangeDecideReqVO req) {
        var event = requireLockedChangeEvent(req.getChangeEventId());
        if (!(Set.of(CHANGE_TYPE_REOPEN, CHANGE_TYPE_SUPPLEMENT).contains(event.getChangeType())
                || (CHANGE_TYPE_VOID.equals(event.getChangeType()) && TARGET_SCOPE_EXECUTION.equals(event.getTargetScope())))
                || !CHANGE_STATUS_SUBMITTED.equals(event.getChangeStatus())
                || !Set.of("REJECTED", "RETURNED").contains(req.getDecision())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_CHANGE_STATUS_INVALID);
        }
        if (StrUtil.isBlank(req.getReason()) || req.getReason().length() > 500) {
            throw new IllegalArgumentException("请填写不超过500字的决定原因");
        }
        recordChangeTaskService.requireReviewer(event);
        requireReleaseActionUnlocked(event.getBatchExecutionId(), false);
        validateSourceSnapshot(event);
        var signatureCommand = new MesProBatchRecordExecutionApprovalSignatureCommand()
                .setExecutionId(event.getExecutionId() == null ? 0L : event.getExecutionId())
                .setPassword(req.getPassword()).setComment(req.getDecision() + "：" + req.getReason().trim())
                .setApprovalResult(MesProBatchRecordExecutionSignatureService.ACTION_REJECT)
                .setReason(req.getReason().trim()).setReviewSourceType("RECORD_CHANGE")
                .setReviewSourceId(event.getId()).setReviewSourceName(event.getChangeCode())
                .setFieldAuditHeadHash(event.getPreviousHeadHash());
        if (TARGET_SCOPE_EXECUTION.equals(event.getTargetScope())) {
            var execution = executionMapper.selectByIdForUpdate(event.getExecutionId());
            signatureCommand.setFieldAuditRevision(execution.getFieldAuditRevision()).setCellValuesHash(execution.getCellValuesHash());
        }
        Long signatureId = signatureService.recordApprovalSignature(signatureCommand);
        if (signatureId == null || signatureId <= 0) throw new IllegalStateException("决定签名未保存");
        event.setChangeStatus(req.getDecision()).setDecisionReason(req.getReason().trim())
                .setApprovedBy(SecurityFrameworkUtils.getLoginUserId()).setApprovedAt(now()).setApprovalSignatureId(signatureId);
        if (changeEventMapper.updateById(event) != 1) throw new IllegalStateException("变更决定保存失败");
        recordChangeTaskService.completeNegativeDecision(event);
        return toResp(event);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EdhrRecordChangeRespVO resubmit(cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrRecordChangeResubmitReqVO req) {
        var previous = requireLockedChangeEvent(req.getChangeEventId());
        if (!"RETURNED".equals(previous.getChangeStatus())
                || !(Set.of(CHANGE_TYPE_REOPEN, CHANGE_TYPE_SUPPLEMENT).contains(previous.getChangeType())
                || (CHANGE_TYPE_VOID.equals(previous.getChangeType()) && TARGET_SCOPE_EXECUTION.equals(previous.getTargetScope())))) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_CHANGE_STATUS_INVALID);
        }
        recordChangeTaskService.requireRequester(previous);
        validateReason(req.getReasonCategory(), req.getReasonText());
        req.setBatchExecutionId(previous.getBatchExecutionId()).setExecutionId(previous.getExecutionId());
        // The old signed request and decision stay immutable; the new request receives its own signature and task.
        if (changeEventMapper.updateById(new MesProEdhrRecordChangeEventDO().setId(previous.getId())
                .setChangeStatus("RESUBMITTED")) != 1) throw new IllegalStateException("原轮次保存失败");
        EdhrRecordChangeRespVO next;
        if (CHANGE_TYPE_VOID.equals(previous.getChangeType())) next = requestVoidExecution(req);
        else if (CHANGE_TYPE_SUPPLEMENT.equals(previous.getChangeType())) next = requestSupplement(req);
        else if (TARGET_SCOPE_BATCH.equals(previous.getTargetScope())) next = requestReopenBatch(req);
        else next = requestReopenExecution(req);
        if (changeEventMapper.updateById(new MesProEdhrRecordChangeEventDO().setId(next.getId())
                .setPreviousChangeEventId(previous.getId())) != 1) throw new IllegalStateException("补正轮次关联保存失败");
        recordChangeTaskService.completeCorrection(previous);
        return toResp(requireChangeEvent(next.getId()));
    }

    private void validateSourceSnapshot(MesProEdhrRecordChangeEventDO event) {
        String status;
        String head;
        Long archiveId;
        String archiveHash;
        if (TARGET_SCOPE_BATCH.equals(event.getTargetScope())) {
            var batch = batchExecutionMapper.selectByTenantIdAndIdForUpdate(TenantContextHolder.getRequiredTenantId(), event.getBatchExecutionId());
            if (batch == null) throw exception(PRO_BATCH_RECORD_EXECUTION_NOT_EXISTS);
            status = String.valueOf(batch.getStatus()); head = batch.getAggregateHash();
            var archive = latestBatchArchive(batch.getId());
            archiveId = archive == null ? null : archive.getId();
            archiveHash = archive == null ? null : archive.getContentHash();
        } else {
            var execution = executionMapper.selectByIdForUpdate(event.getExecutionId());
            if (execution == null) throw exception(PRO_BATCH_RECORD_EXECUTION_NOT_EXISTS);
            if (!Objects.equals(event.getBatchExecutionId(), execution.getBatchExecutionId())) {
                throw new IllegalArgumentException("变更目标记录与批次归属不一致");
            }
            if (Boolean.FALSE.equals(execution.getActiveRevisionFlag()) || execution.getSupersededByExecutionId() != null) {
                throw exception(PRO_BATCH_RECORD_EXECUTION_STATUS_INVALID);
            }
            status = String.valueOf(execution.getStatus()); head = execution.getFieldAuditHeadHash();
            var archive = archiveMapper.selectLatestByExecutionId(execution.getId());
            archiveId = archive == null ? null : archive.getId();
            archiveHash = archive == null ? null : archive.getSha256();
        }
        if (!Objects.equals(event.getPreviousStatus(), status) || !Objects.equals(event.getPreviousHeadHash(), head)
                || !Objects.equals(event.getSourceArchiveId(), archiveId) || !Objects.equals(event.getPreviousArchiveHash(), archiveHash)) {
            throw new IllegalArgumentException("原记录状态或内容版本已改变，请重新核对后申请");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EdhrRecordChangeRespVO requestVoidExecution(EdhrRecordChangeRequestReqVO reqVO) {
        validateReason(reqVO.getReasonCategory(), reqVO.getReasonText());
        MesProBatchRecordExecutionDO execution = requireLockedExecutionForRequest(reqVO);
        if (!Integer.valueOf(STATUS_APPROVED).equals(execution.getStatus())
                || Boolean.FALSE.equals(execution.getActiveRevisionFlag()) || execution.getSupersededByExecutionId() != null) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_STATUS_INVALID);
        }
        assertNoOpenChange(execution.getId(), CHANGE_TYPE_VOID);
        recordChangeTaskService.validateApprover(reqVO.getApproverUserId(), SecurityFrameworkUtils.getLoginUserId());

        Long signatureId = signatureService.recordSubmitSignature(execution.getId(), reqVO.getPassword(), reqVO.getComment());
        if (signatureId == null || signatureId <= 0) throw new IllegalStateException("作废申请签名未保存");
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
        if (changeEventMapper.insert(event) != 1 || event.getId() == null) throw new IllegalStateException("作废申请保存失败");
        recordChangeTaskService.assign(event, reqVO.getApproverUserId());
        return toResp(event);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EdhrRecordChangeRespVO approveVoidExecution(EdhrRecordChangeApproveReqVO reqVO) {
        MesProEdhrRecordChangeEventDO event = requireLockedChangeEvent(reqVO.getChangeEventId());
        if (!CHANGE_TYPE_VOID.equals(event.getChangeType()) || !TARGET_SCOPE_EXECUTION.equals(event.getTargetScope())
                || !CHANGE_STATUS_SUBMITTED.equals(event.getChangeStatus())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_CHANGE_STATUS_INVALID);
        }
        recordChangeTaskService.requireReviewer(event);
        requireReleaseActionUnlocked(event.getBatchExecutionId(), false);
        validateSourceSnapshot(event);
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
                        .setReviewSourceType("RECORD_CHANGE")
                        .setReviewSourceId(event.getId())
                        .setReviewSourceName(event.getChangeCode())
                        .setFieldAuditRevision(execution.getFieldAuditRevision())
                        .setFieldAuditHeadHash(execution.getFieldAuditHeadHash())
                        .setCellValuesHash(execution.getCellValuesHash()));

        if (approvalSignatureId == null || approvalSignatureId <= 0) throw new IllegalStateException("作废批准签名未保存");
        LocalDateTime now = now();
        if (executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(execution.getId())
                .setStatus(STATUS_VOIDED)
                .setVoidedByChangeEventId(event.getId())) != 1) throw new IllegalStateException("作废记录保存失败");
        MesProBatchRecordExecutionArchiveDO archive = archiveMapper.selectLatestByExecutionId(execution.getId());
        // Invalidate every still-valid artifact; preserve earlier invalidation events and all audit bytes/hashes.
        for (var artifact : archiveMapper.selectList(new LambdaQueryWrapperX<MesProBatchRecordExecutionArchiveDO>()
                .eq(MesProBatchRecordExecutionArchiveDO::getExecutionId, execution.getId())
                .eq(MesProBatchRecordExecutionArchiveDO::getArchiveValidFlag, Boolean.TRUE))) {
            if (archiveMapper.updateById(new MesProBatchRecordExecutionArchiveDO()
                    .setId(artifact.getId())
                    .setArchiveValidFlag(Boolean.FALSE)
                    .setArchiveValidStatus("VOIDED")
                    .setInvalidatedByChangeEventId(event.getId())) != 1) throw new IllegalStateException("作废归档失效保存失败");
        }
        if (changeEventMapper.updateById(new MesProEdhrRecordChangeEventDO()
                .setId(event.getId())
                .setChangeStatus(CHANGE_STATUS_EFFECTIVE)
                .setApprovedBy(SecurityFrameworkUtils.getLoginUserId())
                .setApprovedAt(now)
                .setApprovalSignatureId(approvalSignatureId)
                .setEffectiveAt(now)
                .setPreviousArchiveHash(archive == null ? event.getPreviousArchiveHash() : archive.getSha256())
                .setNewArchiveHash(archive == null ? event.getNewArchiveHash() : archive.getSha256())) != 1) {
            throw new IllegalStateException("作废批准事件保存失败");
        }
        recordChangeTaskService.complete(changeEventMapper.selectById(event.getId()));
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
        BusinessApprovalRequest approvalRequest = businessApprovalOrchestrator.submit(
                buildBatchVoidBusinessApprovalContext(reqVO, batch, actorUserId));
        return toResp(requireLatestBatchVoidEventAfterBusinessApprovalSubmit(batch.getId(), approvalRequest));
    }

    @Override
    public EdhrBatchVoidApprovalResolutionRespVO resolveVoidBatchExecutionApproval(
            EdhrBatchVoidApprovalResolutionReqVO reqVO) {
        MesProEdhrBatchExecutionDO batch = requireBatchExecution(reqVO.getBatchExecutionId());
        requireReleaseActionUnlocked(batch.getId());
        if (Integer.valueOf(BATCH_STATUS_VOIDED).equals(batch.getStatus())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_STATUS_INVALID);
        }
        assertNoOpenBatchChange(batch.getId(), CHANGE_TYPE_VOID);

        Long actorUserId = SecurityFrameworkUtils.getLoginUserId();
        BusinessApprovalPolicyResolution resolution = businessApprovalPolicyResolveService.resolve(
                buildBatchVoidBusinessApprovalResolutionContext(batch, actorUserId));
        return new EdhrBatchVoidApprovalResolutionRespVO()
                .setPolicyId(resolution.getPolicyId())
                .setPolicyMode(resolution.getMode().name())
                .setRequiresBpm(resolution.getMode() == BusinessApprovalPolicyMode.BPM_REQUIRED)
                .setBpmProcessKey(resolution.getProcessDefinitionKey())
                .setEffectExecutorCode(resolution.getEffectExecutorCode());
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
        batch = batchExecutionMapper.selectByTenantIdAndIdForUpdate(TenantContextHolder.getRequiredTenantId(), batch.getId());
        if (batch == null) throw exception(PRO_BATCH_RECORD_EXECUTION_NOT_EXISTS);
        requireReleaseActionUnlocked(batch.getId());
        nonconformanceReviewService.ensureBatchNotFrozen(batch.getId(), "记录变更申请");
        if (!isReopenableBatchStatus(batch.getStatus())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_STATUS_INVALID);
        }
        assertNoOpenBatchChange(batch.getId(), CHANGE_TYPE_REOPEN);
        recordChangeTaskService.validateApprover(reqVO.getApproverUserId(), SecurityFrameworkUtils.getLoginUserId());

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
        recordChangeTaskService.assign(event, reqVO.getApproverUserId());
        return toResp(event);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EdhrRecordChangeRespVO approveReopenBatch(EdhrRecordChangeApproveReqVO reqVO) {
        MesProEdhrRecordChangeEventDO event = requireLockedChangeEvent(reqVO.getChangeEventId());
        if (!CHANGE_TYPE_REOPEN.equals(event.getChangeType()) || !TARGET_SCOPE_BATCH.equals(event.getTargetScope())
                || !CHANGE_STATUS_SUBMITTED.equals(event.getChangeStatus())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_CHANGE_STATUS_INVALID);
        }
        recordChangeTaskService.requireReviewer(event);
        validateSourceSnapshot(event);
        requireReleaseActionUnlocked(event.getBatchExecutionId(), false);
        MesProEdhrBatchExecutionDO batch = batchExecutionMapper.selectByTenantIdAndIdForUpdate(
                TenantContextHolder.getRequiredTenantId(), event.getBatchExecutionId());
        if (!isReopenableBatchStatus(batch.getStatus())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_STATUS_INVALID);
        }

        Long approvalSignatureId = signatureService.recordApprovalSignature(
                new MesProBatchRecordExecutionApprovalSignatureCommand()
                        .setExecutionId(0L)
                        .setPassword(reqVO.getPassword())
                        .setComment(reqVO.getComment())
                        .setApprovalResult(MesProBatchRecordExecutionSignatureService.ACTION_APPROVE)
                        .setReason(event.getReasonText())
                        .setReviewSourceType("RECORD_CHANGE")
                        .setReviewSourceId(event.getId())
                        .setReviewSourceName(event.getChangeCode()));

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
        recordChangeTaskService.complete(changeEventMapper.selectById(event.getId()));
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
        MesProBatchRecordExecutionDO execution = requireLockedExecutionForRequest(reqVO);
        if (!Integer.valueOf(STATUS_APPROVED).equals(execution.getStatus())
                || Boolean.FALSE.equals(execution.getActiveRevisionFlag()) || execution.getSupersededByExecutionId() != null) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_STATUS_INVALID);
        }
        assertNoOpenChange(execution.getId(), CHANGE_TYPE_REOPEN);
        recordChangeTaskService.validateApprover(reqVO.getApproverUserId(), SecurityFrameworkUtils.getLoginUserId());

        Long signatureId = signatureService.recordSubmitSignature(execution.getId(), reqVO.getPassword(), reqVO.getComment());
        MesProBatchRecordExecutionArchiveDO archive = archiveMapper.selectLatestByExecutionId(execution.getId());
        MesProEdhrRecordChangeEventDO event = buildExecutionChangeEvent(reqVO, execution, archive,
                CHANGE_TYPE_REOPEN, CHANGE_STATUS_SUBMITTED, STATUS_REOPENED, signatureId);
        changeEventMapper.insert(event);
        recordChangeTaskService.assign(event, reqVO.getApproverUserId());
        return toResp(event);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EdhrRecordChangeRespVO approveReopenExecution(EdhrRecordChangeApproveReqVO reqVO) {
        MesProEdhrRecordChangeEventDO event = requireLockedChangeEvent(reqVO.getChangeEventId());
        if (!CHANGE_TYPE_REOPEN.equals(event.getChangeType()) || !TARGET_SCOPE_EXECUTION.equals(event.getTargetScope())
                || !CHANGE_STATUS_SUBMITTED.equals(event.getChangeStatus())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_CHANGE_STATUS_INVALID);
        }
        recordChangeTaskService.requireReviewer(event);
        validateSourceSnapshot(event);
        requireReleaseActionUnlocked(event.getBatchExecutionId(), false);
        MesProBatchRecordExecutionDO execution = requireExecution(event.getExecutionId());
        execution = executionMapper.selectByIdForUpdate(execution.getId());
        if (!Integer.valueOf(STATUS_APPROVED).equals(execution.getStatus())
                || Boolean.FALSE.equals(execution.getActiveRevisionFlag()) || execution.getSupersededByExecutionId() != null) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_STATUS_INVALID);
        }
        Long approvalSignatureId = signatureService.recordApprovalSignature(
                new MesProBatchRecordExecutionApprovalSignatureCommand()
                        .setExecutionId(execution.getId())
                        .setPassword(reqVO.getPassword())
                        .setComment(reqVO.getComment())
                        .setApprovalResult(MesProBatchRecordExecutionSignatureService.ACTION_APPROVE)
                        .setReason(event.getReasonText())
                        .setReviewSourceType("RECORD_CHANGE")
                        .setReviewSourceId(event.getId())
                        .setReviewSourceName(event.getChangeCode())
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
        workTaskService.createRecordChangeRework(event.getId(), execution.getId(), execution.getId(), event.getReasonText());
        recordChangeTaskService.complete(changeEventMapper.selectById(event.getId()));
        return toResp(changeEventMapper.selectById(event.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EdhrRecordChangeRespVO requestSupplement(EdhrRecordChangeRequestReqVO reqVO) {
        validateReason(reqVO.getReasonCategory(), reqVO.getReasonText());
        MesProBatchRecordExecutionDO execution = requireLockedExecutionForRequest(reqVO);
        if (!Integer.valueOf(STATUS_APPROVED).equals(execution.getStatus())
                || Boolean.FALSE.equals(execution.getActiveRevisionFlag()) || execution.getSupersededByExecutionId() != null) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_STATUS_INVALID);
        }
        assertNoOpenChange(execution.getId(), CHANGE_TYPE_SUPPLEMENT);
        recordChangeTaskService.validateApprover(reqVO.getApproverUserId(), SecurityFrameworkUtils.getLoginUserId());

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
        recordChangeTaskService.assign(event, reqVO.getApproverUserId());
        return toResp(event);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EdhrRecordChangeRespVO saveSupplementDraft(EdhrRecordChangeRequestReqVO reqVO) {
        validateReason(reqVO.getReasonCategory(), reqVO.getReasonText());
        MesProBatchRecordExecutionDO execution = requireLockedExecutionForRequest(reqVO);
        if (!Integer.valueOf(STATUS_APPROVED).equals(execution.getStatus())
                || Boolean.FALSE.equals(execution.getActiveRevisionFlag()) || execution.getSupersededByExecutionId() != null) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_STATUS_INVALID);
        }
        assertNoOpenChange(execution.getId(), CHANGE_TYPE_SUPPLEMENT);
        MesProBatchRecordExecutionArchiveDO archive = archiveMapper.selectLatestByExecutionId(execution.getId());
        MesProEdhrRecordChangeEventDO event = buildExecutionChangeEvent(reqVO, execution, archive,
                CHANGE_TYPE_SUPPLEMENT, CHANGE_STATUS_DRAFT, execution.getStatus(), null);
        event.setRequestedBy(SecurityFrameworkUtils.getLoginUserId());
        changeEventMapper.insert(event);
        return toResp(event);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EdhrRecordChangeRespVO submitSupplement(EdhrRecordChangeApproveReqVO reqVO) {
        MesProEdhrRecordChangeEventDO event = requireLockedChangeEvent(reqVO.getChangeEventId());
        if (!CHANGE_TYPE_SUPPLEMENT.equals(event.getChangeType()) || !CHANGE_STATUS_DRAFT.equals(event.getChangeStatus())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_CHANGE_STATUS_INVALID);
        }
        if (!Objects.equals(event.getRequestedBy(), SecurityFrameworkUtils.getLoginUserId())) {
            throw new IllegalArgumentException("只有原草稿申请人可以签名提交补录申请");
        }
        recordChangeTaskService.validateApprover(reqVO.getApproverUserId(), event.getRequestedBy());
        requireReleaseActionUnlocked(event.getBatchExecutionId());
        nonconformanceReviewService.ensureBatchNotFrozen(event.getBatchExecutionId(), "记录变更申请");
        MesProBatchRecordExecutionDO execution = requireExecution(event.getExecutionId());
        if (!Integer.valueOf(STATUS_APPROVED).equals(execution.getStatus())
                || Boolean.FALSE.equals(execution.getActiveRevisionFlag()) || execution.getSupersededByExecutionId() != null) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_STATUS_INVALID);
        }
        Long signatureId = signatureService.recordSubmitSignature(execution.getId(), reqVO.getPassword(), reqVO.getComment());
        changeEventMapper.updateById(new MesProEdhrRecordChangeEventDO()
                .setId(event.getId())
                .setChangeStatus(CHANGE_STATUS_SUBMITTED)
                .setRequestedBy(SecurityFrameworkUtils.getLoginUserId())
                .setRequestedAt(now())
                .setRequestSignatureId(signatureId)
                .setRemark(StrUtil.blankToDefault(StrUtil.trim(reqVO.getComment()), event.getRemark())));
        recordChangeTaskService.assign(changeEventMapper.selectById(event.getId()), reqVO.getApproverUserId());
        return toResp(changeEventMapper.selectById(event.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EdhrRecordChangeRespVO approveSupplement(EdhrRecordChangeApproveReqVO reqVO) {
        MesProEdhrRecordChangeEventDO event = requireLockedChangeEvent(reqVO.getChangeEventId());
        if (!CHANGE_TYPE_SUPPLEMENT.equals(event.getChangeType()) || !CHANGE_STATUS_SUBMITTED.equals(event.getChangeStatus())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_CHANGE_STATUS_INVALID);
        }
        recordChangeTaskService.requireReviewer(event);
        validateSourceSnapshot(event);
        requireReleaseActionUnlocked(event.getBatchExecutionId(), false);
        MesProBatchRecordExecutionDO execution = requireExecution(event.getExecutionId());
        execution = executionMapper.selectByIdForUpdate(execution.getId());
        if (!Integer.valueOf(STATUS_APPROVED).equals(execution.getStatus())
                || Boolean.FALSE.equals(execution.getActiveRevisionFlag()) || execution.getSupersededByExecutionId() != null) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_STATUS_INVALID);
        }

        Long approvalSignatureId = signatureService.recordApprovalSignature(
                new MesProBatchRecordExecutionApprovalSignatureCommand()
                        .setExecutionId(execution.getId())
                        .setPassword(reqVO.getPassword())
                        .setComment(reqVO.getComment())
                        .setApprovalResult(MesProBatchRecordExecutionSignatureService.ACTION_APPROVE)
                        .setReason(event.getReasonText())
                        .setReviewSourceType("RECORD_CHANGE")
                        .setReviewSourceId(event.getId())
                        .setReviewSourceName(event.getChangeCode())
                        .setFieldAuditRevision(execution.getFieldAuditRevision())
                        .setFieldAuditHeadHash(execution.getFieldAuditHeadHash())
                        .setCellValuesHash(execution.getCellValuesHash()));

        MesProBatchRecordExecutionDO revision = supplementRevisionService.create(event, execution);
        workTaskService.createRecordChangeRework(event.getId(), execution.getId(), revision.getId(), event.getReasonText());
        LocalDateTime now = now();
        changeEventMapper.updateById(new MesProEdhrRecordChangeEventDO()
                .setId(event.getId())
                .setChangeStatus(CHANGE_STATUS_EFFECTIVE)
                .setSourceExecutionId(execution.getId()).setNewExecutionId(revision.getId())
                .setApprovedBy(SecurityFrameworkUtils.getLoginUserId())
                .setApprovedAt(now)
                .setApprovalSignatureId(approvalSignatureId)
                .setEffectiveAt(now));
        recordChangeTaskService.complete(changeEventMapper.selectById(event.getId()));
        return toResp(changeEventMapper.selectById(event.getId()));
    }

    @Override
    public PageResult<EdhrRecordChangeRespVO> getPage(EdhrRecordChangePageReqVO reqVO) {
        PageResult<MesProEdhrRecordChangeEventDO> page = changeEventMapper.selectPage(reqVO);
        return new PageResult<>(page.getList().stream().map(this::toResp).toList(), page.getTotal());
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

    private MesProBatchRecordExecutionDO requireLockedExecutionForRequest(EdhrRecordChangeRequestReqVO request) {
        MesProBatchRecordExecutionDO execution = requireExecution(request.getExecutionId());
        execution = executionMapper.selectByIdForUpdate(execution.getId());
        if (request.getBatchExecutionId() != null && !Objects.equals(request.getBatchExecutionId(), execution.getBatchExecutionId())) {
            throw new IllegalArgumentException("执行记录不属于指定批次");
        }
        request.setBatchExecutionId(execution.getBatchExecutionId());
        requireReleaseActionUnlocked(execution.getBatchExecutionId());
        nonconformanceReviewService.ensureBatchNotFrozen(execution.getBatchExecutionId(), "记录变更申请");
        return execution;
    }

    private MesProEdhrRecordChangeEventDO requireLockedChangeEvent(Long id) {
        MesProEdhrRecordChangeEventDO event = changeEventMapper.selectByIdAndTenantForUpdate(id, TenantContextHolder.getRequiredTenantId());
        if (event == null) throw exception(PRO_BATCH_RECORD_EXECUTION_CHANGE_NOT_EXISTS);
        return event;
    }

    private MesProEdhrRecordChangeEventDO requireChangeEvent(Long id) {
        MesProEdhrRecordChangeEventDO event = id == null ? null : changeEventMapper.selectById(id);
        if (event == null) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_CHANGE_NOT_EXISTS);
        }
        return event;
    }

    private MesProEdhrBatchExecutionDO requireBatchExecution(Long batchExecutionId) {
        marketReleaseAccess.requireReadable(batchExecutionId);
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
        requireReleaseActionUnlocked(batchExecutionId, true);
    }

    private void requireReleaseActionUnlocked(Long batchExecutionId, boolean requiresBatchWriteOwnership) {
        if (batchExecutionId != null) {
            if (requiresBatchWriteOwnership) marketReleaseAccess.requireWritable(batchExecutionId);
            // A formal independent reviewer acts under the selected record-change task, not market handover ownership.
            else nonconformanceReviewService.ensureBatchNotFrozen(batchExecutionId, "记录变更审批");
        }
        if (batchExecutionId == null) {
            return;
        }
        MesProEdhrReleaseTransactionDO releaseTransaction =
                releaseTransactionMapper.selectByBatchExecutionId(batchExecutionId);
        if ((!requiresBatchWriteOwnership || !hasGoldenFingerActionBypass())
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
        return signatureService.recordBatchVoidRequestSignature(actorUserId, batch.getId(), password, comment,
                batch.getAggregateHash());
    }

    private BusinessApprovalContext buildBatchVoidBusinessApprovalContext(EdhrRecordChangeRequestReqVO reqVO,
                                                                          MesProEdhrBatchExecutionDO batch,
                                                                          Long actorUserId) {
        Map<String, Object> transientVariables = new LinkedHashMap<>();
        transientVariables.put("password", StrUtil.trim(reqVO.getPassword()));
        return BusinessApprovalContext.builder()
                .tenantId(TenantContextHolder.getRequiredTenantId())
                .dataDomain("MES")
                .systemCode("MES")
                .objectType("EDHR_BATCH_EXECUTION")
                .objectId(String.valueOf(batch.getId()))
                .objectVersion(String.valueOf(batch.getId()))
                .actionCode(CHANGE_TYPE_VOID)
                .objectState(batch.getStatus() == null ? null : String.valueOf(batch.getStatus()))
                .applicantUserId(actorUserId)
                .reason(StrUtil.trim(reqVO.getReasonText()))
                .startUserSelectAssignees(reqVO.getStartUserSelectAssignees())
                .variables(buildBatchVoidBusinessApprovalVariables(batch, reqVO, actorUserId))
                .transientVariables(transientVariables)
                .build();
    }

    private BusinessApprovalContext buildBatchVoidBusinessApprovalResolutionContext(MesProEdhrBatchExecutionDO batch,
                                                                                   Long actorUserId) {
        return BusinessApprovalContext.builder()
                .tenantId(TenantContextHolder.getRequiredTenantId())
                .dataDomain("MES")
                .systemCode("MES")
                .objectType("EDHR_BATCH_EXECUTION")
                .objectId(String.valueOf(batch.getId()))
                .objectVersion(String.valueOf(batch.getId()))
                .actionCode(CHANGE_TYPE_VOID)
                .objectState(batch.getStatus() == null ? null : String.valueOf(batch.getStatus()))
                .applicantUserId(actorUserId)
                .reason("eDHR batch void approval")
                .variables(Map.of())
                .transientVariables(Map.of())
                .build();
    }

    private Map<String, Object> buildBatchVoidBusinessApprovalVariables(MesProEdhrBatchExecutionDO batch,
                                                                        EdhrRecordChangeRequestReqVO reqVO,
                                                                        Long actorUserId) {
        Map<String, Object> variables = new LinkedHashMap<>();
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
        variables.put("submittedAt", now().toString());
        variables.put("tenantId", TenantContextHolder.getTenantId());
        return variables;
    }

    private MesProEdhrRecordChangeEventDO requireLatestBatchVoidEventAfterBusinessApprovalSubmit(
            Long batchExecutionId, BusinessApprovalRequest approvalRequest) {
        LambdaQueryWrapperX<MesProEdhrRecordChangeEventDO> query = new LambdaQueryWrapperX<MesProEdhrRecordChangeEventDO>()
                .eq(MesProEdhrRecordChangeEventDO::getBatchExecutionId, batchExecutionId)
                .eq(MesProEdhrRecordChangeEventDO::getChangeType, CHANGE_TYPE_VOID)
                .eq(MesProEdhrRecordChangeEventDO::getTargetScope, TARGET_SCOPE_BATCH)
                .orderByDesc(MesProEdhrRecordChangeEventDO::getId)
                .last("LIMIT 1");
        if (approvalRequest != null && StrUtil.isNotBlank(approvalRequest.getProcessInstanceId())) {
            query.eq(MesProEdhrRecordChangeEventDO::getBpmProcessInstanceId,
                    StrUtil.trim(approvalRequest.getProcessInstanceId()));
        }
        MesProEdhrRecordChangeEventDO event = changeEventMapper.selectOne(query);
        if (event == null) {
            throw new IllegalStateException("eDHR batch void business approval did not create a change event: "
                    + batchExecutionId);
        }
        return event;
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
        workTaskService.cancelActiveTasksByBatch(batch.getId(), buildBatchVoidWorkTaskCancelReason(event));
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

    private String buildBatchVoidWorkTaskCancelReason(MesProEdhrRecordChangeEventDO event) {
        String reasonText = StrUtil.blankToDefault(StrUtil.trim(event.getReasonText()), StrUtil.trim(event.getRemark()));
        if (StrUtil.isBlank(reasonText)) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_CHANGE_REASON_REQUIRED);
        }
        return "批次已作废：" + reasonText;
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
        EdhrRecordChangeRespVO response = BeanUtils.toBean(event, EdhrRecordChangeRespVO.class);
        if (CHANGE_TYPE_REOPEN.equals(event.getChangeType()) || CHANGE_TYPE_SUPPLEMENT.equals(event.getChangeType())
                || (CHANGE_TYPE_VOID.equals(event.getChangeType()) && TARGET_SCOPE_EXECUTION.equals(event.getTargetScope()))) {
            var task = recordChangeTaskService.find(event.getId());
            response.setCanApprove(false).setCanResubmit(false);
            var correction = recordChangeTaskService.findCorrection(event.getId());
            if (correction != null) {
                response.setCorrectionWorkTaskId(correction.getId()).setCanResubmit("RETURNED".equals(event.getChangeStatus())
                        && Objects.equals(event.getRequestedBy(), SecurityFrameworkUtils.getLoginUserId())
                        && Objects.equals(correction.getAssigneeUserId(), SecurityFrameworkUtils.getLoginUserId()));
            }
            var revisionTask = recordChangeTaskService.findOwnRevision(event.getId());
            if (revisionTask != null) {
                response.setRevisionWorkTaskId(revisionTask.getId()).setRevisionActionUrl(revisionTask.getActionUrl());
            }
            if (task != null) {
                response.setApproverUserId(task.getAssigneeUserId()).setApprovalWorkTaskId(task.getId())
                        .setCanApprove(CHANGE_STATUS_SUBMITTED.equals(event.getChangeStatus())
                                && Objects.equals(task.getAssigneeUserId(), SecurityFrameworkUtils.getLoginUserId())
                                && !Objects.equals(event.getRequestedBy(), SecurityFrameworkUtils.getLoginUserId())
                                && Set.of("TODO", "OVERDUE").contains(task.getStatus()));
            }
        }
        return response;
    }

    private LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
