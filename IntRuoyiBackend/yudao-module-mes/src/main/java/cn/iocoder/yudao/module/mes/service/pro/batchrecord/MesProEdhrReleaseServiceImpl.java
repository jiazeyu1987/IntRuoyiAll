package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionInstanceDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionInstanceMapper;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormInstanceStatus;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseApproveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseCheckItemPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseCheckItemRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseEventPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseEventRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleasePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleasePrecheckReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseRejectReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseSubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseWithdrawReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionAttachmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionSignatureDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionSignatureDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseCheckItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionAttachmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionSignatureMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionSignatureMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseCheckItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionEventMapper;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleMapper;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants.UNAUTHORIZED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_RELEASE_IDEMPOTENCY_KEY_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_RELEASE_DOSSIER_REQUIREMENT_CONFIG_STALE;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_RELEASE_OWNER_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_RELEASE_PRECHECK_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_RELEASE_REASON_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_RELEASE_SIGNATURE_PASSWORD_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_RELEASE_SIGNOFF_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_RELEASE_STATUS_INVALID;

@Service
public class MesProEdhrReleaseServiceImpl implements MesProEdhrReleaseService {

    public static final String STATUS_PRECHECK_REQUIRED = "PRECHECK_REQUIRED";
    public static final String STATUS_PRECHECK_FAILED = "PRECHECK_FAILED";
    public static final String STATUS_PRECHECK_PASSED = "PRECHECK_PASSED";
    public static final String STATUS_PENDING_APPROVAL = "PENDING_APPROVAL";
    public static final String STATUS_RELEASED = "RELEASED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_WITHDRAWN = "WITHDRAWN";

    public static final String EVENT_TYPE_SUBMIT = "SUBMIT";
    public static final String EVENT_TYPE_APPROVE = "APPROVE";
    public static final String EVENT_TYPE_REJECT = "REJECT";
    public static final String EVENT_TYPE_WITHDRAW = "WITHDRAW";
    public static final String EVENT_TYPE_PRECHECK = "PRECHECK";

    public static final String ITEM_STATUS_OPEN = "OPEN";
    public static final String ITEM_STATUS_SUPERSEDED = "SUPERSEDED";

    public static final String CHECK_RESULT_PASS = "PASS";
    public static final String CHECK_RESULT_FAIL = "FAIL";
    public static final String CHECK_RESULT_BLOCKER = "BLOCKER";
    public static final String CHECK_RESULT_NOT_APPLICABLE = "NOT_APPLICABLE";

    public static final String CHECK_DHR_COMPLETENESS = "DHR_COMPLETENESS";
    public static final String CHECK_INSPECTION_RESULT = "INSPECTION_RESULT";
    public static final String CHECK_DEVIATION_CLOSED = "DEVIATION_CLOSED";
    public static final String CHECK_REWORK_CLOSED = "REWORK_CLOSED";
    public static final String CHECK_SCRAP_RECORDED = "SCRAP_RECORDED";
    public static final String CHECK_INVENTORY_CONSISTENCY = "INVENTORY_CONSISTENCY";
    public static final String CHECK_DOSSIER_INCOMING_INSPECTION_REPORT = "DOSSIER_INCOMING_INSPECTION_REPORT";
    public static final String CHECK_DOSSIER_STERILIZATION_REPORT = "DOSSIER_STERILIZATION_REPORT";
    public static final String CHECK_DOSSIER_FINISHED_PRODUCT_INSPECTION_REPORT =
            "DOSSIER_FINISHED_PRODUCT_INSPECTION_REPORT";
    public static final String CHECK_DOSSIER_FINISHED_PRODUCT_INSPECTION_RECORD =
            "DOSSIER_FINISHED_PRODUCT_INSPECTION_RECORD";

    private static final String SEVERITY_INFO = "INFO";
    private static final String SEVERITY_BLOCKER = "BLOCKER";
    private static final String MODULE_EDHR = "EDHR";
    private static final String MODULE_QMS = "QMS";
    private static final String MODULE_MES = "MES";
    private static final String MODULE_WMS = "WMS";
    private static final String CATEGORY_DOSSIER = "DOSSIER";
    private static final String SOURCE_OBJECT_TYPE_SPECIAL_NODE_ATTACHMENT = "SPECIAL_NODE_ATTACHMENT";
    private static final Long SPECIAL_NODE_ATTACHMENT_EXECUTION_ID = 0L;
    private static final String SPECIAL_NODE_ATTACHMENT_ACTION_ADD = "ADD";
    private static final String RULE_SCOPE_TYPE_ROUTE = "ROUTE";
    private static final String CANDIDATE_SOURCE_TYPE_USER = "USER";
    private static final String ACTION_BATCH_RELEASE = "BATCH_RELEASE";
    private static final String SIGNATURE_MODE_PASSWORD = "PASSWORD";

    @Resource
    private MesProEdhrBatchExecutionMapper batchExecutionMapper;
    @Resource
    private MesProEdhrBatchExecutionTaskMapper batchExecutionTaskMapper;
    @Resource
    private MesProBatchRecordExecutionMapper executionMapper;
    @Resource
    private MesProBatchRecordExecutionAttachmentMapper attachmentMapper;
    @Resource
    private MesProBatchRecordExecutionSignatureMapper executionSignatureMapper;
    @Resource
    private MesProEdhrBatchExecutionSignatureMapper batchSignatureMapper;
    @Resource
    private MesProEdhrReleaseTransactionMapper releaseTransactionMapper;
    @Resource
    private MesProEdhrReleaseCheckItemMapper releaseCheckItemMapper;
    @Resource
    private MesProEdhrReleaseTransactionEventMapper releaseTransactionEventMapper;
    @Resource
    private MesProEdhrWorkTaskAssignmentRuleMapper workTaskAssignmentRuleMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private MesProEdhrWorkTaskService workTaskService;
    @Resource
    private MesProEdhrOperationAuditService operationAuditService;
    @Resource
    private FormActionInstanceMapper formActionInstanceMapper;
    @Resource
    private MesProEdhrReleaseDossierRequirementSettingService dossierRequirementSettingService;

    @Override
    public PageResult<MesProEdhrReleaseRespVO> getPage(MesProEdhrReleasePageReqVO reqVO) {
        if (hasReleaseTransactionFilter(reqVO)) {
            return getTransactionFilteredPage(reqVO);
        }
        EdhrBatchExecutionPageReqVO batchReqVO = toBatchPageReq(reqVO);
        PageResult<MesProEdhrBatchExecutionDO> page = batchExecutionMapper.selectPage(batchReqVO);
        List<Long> batchExecutionIds = page.getList().stream()
                .map(MesProEdhrBatchExecutionDO::getId)
                .toList();
        Map<Long, MesProEdhrReleaseTransactionDO> transactionByBatchId =
                releaseTransactionMapper.selectListByBatchExecutionIds(batchExecutionIds).stream()
                        .collect(Collectors.toMap(MesProEdhrReleaseTransactionDO::getBatchExecutionId,
                                Function.identity(), (left, right) -> left));
        List<MesProEdhrReleaseRespVO> list = page.getList().stream()
                .map(batch -> toResp(batch, transactionByBatchId.get(batch.getId())))
                .filter(item -> statusMatches(reqVO, item))
                .filter(item -> batchExecutionStatusMatches(reqVO, item))
                .toList();
        return new PageResult<>(list, page.getTotal());
    }

    private boolean hasReleaseTransactionFilter(MesProEdhrReleasePageReqVO reqVO) {
        return StrUtil.isNotBlank(reqVO.getDhrStatus())
                || StrUtil.isNotBlank(reqVO.getInspectionStatus())
                || (StrUtil.isNotBlank(reqVO.getReleaseStatus())
                && !STATUS_PRECHECK_REQUIRED.equals(reqVO.getReleaseStatus()));
    }

    private PageResult<MesProEdhrReleaseRespVO> getTransactionFilteredPage(MesProEdhrReleasePageReqVO reqVO) {
        List<MesProEdhrReleaseTransactionDO> transactions = releaseTransactionMapper.selectList(
                new LambdaQueryWrapperX<MesProEdhrReleaseTransactionDO>()
                        .likeIfPresent(MesProEdhrReleaseTransactionDO::getBatchExecutionCode, reqVO.getBatchExecutionCode())
                        .likeIfPresent(MesProEdhrReleaseTransactionDO::getWorkOrderCode, reqVO.getWorkOrderCode())
                        .likeIfPresent(MesProEdhrReleaseTransactionDO::getBatchCode, reqVO.getBatchCode())
                        .likeIfPresent(MesProEdhrReleaseTransactionDO::getProductCode, reqVO.getProductCode())
                        .eqIfPresent(MesProEdhrReleaseTransactionDO::getReleaseStatus, reqVO.getReleaseStatus())
                        .eqIfPresent(MesProEdhrReleaseTransactionDO::getDhrStatus, reqVO.getDhrStatus())
                        .eqIfPresent(MesProEdhrReleaseTransactionDO::getInspectionStatus, reqVO.getInspectionStatus())
                        .betweenIfPresent(MesProEdhrReleaseTransactionDO::getCreateTime, reqVO.getCreateTime())
                        .orderByDesc(MesProEdhrReleaseTransactionDO::getId));
        int pageNo = reqVO.getPageNo() == null ? 1 : Math.max(reqVO.getPageNo(), 1);
        int pageSize = reqVO.getPageSize() == null ? 10 : Math.max(reqVO.getPageSize(), 1);
        List<Long> batchExecutionIds = transactions.stream()
                .map(MesProEdhrReleaseTransactionDO::getBatchExecutionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, MesProEdhrBatchExecutionDO> batchById = batchExecutionIds.isEmpty() ? Map.of()
                : batchExecutionMapper.selectBatchIds(batchExecutionIds).stream()
                .collect(Collectors.toMap(MesProEdhrBatchExecutionDO::getId,
                        Function.identity(), (left, right) -> left, LinkedHashMap::new));
        List<MesProEdhrReleaseRespVO> filteredList = transactions.stream()
                .map(transaction -> toResp(requireBatchExecutionForTransaction(transaction, batchById), transaction))
                .filter(item -> batchExecutionStatusMatches(reqVO, item))
                .toList();
        List<MesProEdhrReleaseRespVO> pageList = filteredList.stream()
                .skip((long) (pageNo - 1) * pageSize)
                .limit(pageSize)
                .toList();
        return new PageResult<>(pageList, (long) filteredList.size());
    }

    private MesProEdhrBatchExecutionDO requireBatchExecutionForTransaction(
            MesProEdhrReleaseTransactionDO transaction,
            Map<Long, MesProEdhrBatchExecutionDO> batchById) {
        MesProEdhrBatchExecutionDO batch = batchById.get(transaction.getBatchExecutionId());
        if (batch == null) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_NOT_EXISTS);
        }
        return batch;
    }

    @Override
    public MesProEdhrReleaseRespVO get(Long id) {
        MesProEdhrReleaseTransactionDO transaction = releaseTransactionMapper.selectById(id);
        if (transaction == null) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_NOT_EXISTS);
        }
        MesProEdhrBatchExecutionDO batch = requireBatchExecution(transaction.getBatchExecutionId());
        return toResp(batch, transaction);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrReleaseRespVO precheck(MesProEdhrReleasePrecheckReqVO reqVO) {
        MesProEdhrReleaseTransactionDO existingTransaction = null;
        if (reqVO.getReleaseTransactionId() != null) {
            existingTransaction = releaseTransactionMapper.selectById(reqVO.getReleaseTransactionId());
            if (existingTransaction == null) {
                throw exception(PRO_EDHR_BATCH_EXECUTION_NOT_EXISTS);
            }
        }
        Long batchExecutionId = existingTransaction == null ? reqVO.getBatchExecutionId() : existingTransaction.getBatchExecutionId();
        MesProEdhrBatchExecutionDO batch = requireBatchExecution(batchExecutionId);
        MesProEdhrReleaseTransactionDO transaction = existingTransaction == null
                ? releaseTransactionMapper.selectByBatchExecutionId(batch.getId()) : existingTransaction;
        if (transaction != null) {
            requirePrecheckEditable(transaction);
        }
        if (transaction == null) {
            transaction = buildInitialTransaction(batch);
            releaseTransactionMapper.insert(transaction);
        }
        String fromStatus = transaction.getReleaseStatus();

        releaseCheckItemMapper.closeOpenByReleaseTransactionId(transaction.getId());
        LocalDateTime checkedAt = now();
        MesProEdhrReleaseDossierRequirementState dossierRequirementState =
                dossierRequirementSettingService.getRequirementState();
        List<MesProEdhrReleaseCheckItemDO> checkItems =
                buildCheckItems(transaction.getId(), batch, checkedAt, dossierRequirementState);
        checkItems.forEach(releaseCheckItemMapper::insert);

        int failedCount = (int) checkItems.stream()
                .filter(this::isFailedCheck)
                .count();
        int blockingCount = (int) checkItems.stream()
                .filter(item -> CHECK_RESULT_BLOCKER.equals(item.getCheckResult())
                        || SEVERITY_BLOCKER.equals(item.getSeverity()))
                .count();
        String releaseStatus = failedCount == 0 ? STATUS_PRECHECK_PASSED : STATUS_PRECHECK_FAILED;
        Map<String, Object> snapshot =
                buildSnapshot(batch, checkItems, releaseStatus, checkedAt, dossierRequirementState.configHash());
        String precheckSnapshotJson = JSON.toJSONString(snapshot);

        transaction = new MesProEdhrReleaseTransactionDO()
                .setId(transaction.getId())
                .setDhrStatus(resultOf(checkItems, CHECK_DHR_COMPLETENESS))
                .setInspectionStatus(resultOf(checkItems, CHECK_INSPECTION_RESULT))
                .setDeviationStatus(resultOf(checkItems, CHECK_DEVIATION_CLOSED))
                .setReworkStatus(resultOf(checkItems, CHECK_REWORK_CLOSED))
                .setScrapStatus(resultOf(checkItems, CHECK_SCRAP_RECORDED))
                .setInventoryStatus(resultOf(checkItems, CHECK_INVENTORY_CONSISTENCY))
                .setReleaseStatus(releaseStatus)
                .setRequiredCheckCount(checkItems.size())
                .setFailedCheckCount(failedCount)
                .setBlockingCheckCount(blockingCount)
                .setLastPrecheckAt(checkedAt)
                .setPrecheckSnapshotJson(precheckSnapshotJson);
        releaseTransactionMapper.updateById(transaction);
        MesProEdhrReleaseTransactionDO updatedTransaction = releaseTransactionMapper.selectById(transaction.getId());
        Long actorUserId = currentAuditActorUserId();
        String idempotencyKey = buildPrecheckIdempotencyKey(updatedTransaction, checkedAt);
        String precheckSnapshotHash = hashReleaseAuditPayload(precheckSnapshotJson);
        recordPrecheckTransactionEvent(updatedTransaction, fromStatus, releaseStatus, actorUserId, idempotencyKey,
                precheckSnapshotHash, checkItems, checkedAt);
        recordPrecheckOperationAudit(batch, updatedTransaction, fromStatus, releaseStatus, actorUserId,
                idempotencyKey, precheckSnapshotHash, checkItems, precheckSnapshotJson, checkedAt);
        return toResp(batch, updatedTransaction);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrReleaseRespVO submit(MesProEdhrReleaseSubmitReqVO reqVO) {
        String idempotencyKey = requireIdempotencyKey(reqVO.getIdempotencyKey());
        MesProEdhrReleaseTransactionEventDO existingEvent =
                releaseTransactionEventMapper.selectByReleaseTransactionIdAndEventTypeAndIdempotencyKey(
                        reqVO.getReleaseTransactionId(), EVENT_TYPE_SUBMIT, idempotencyKey);
        if (existingEvent != null) {
            return get(reqVO.getReleaseTransactionId());
        }

        MesProEdhrReleaseTransactionDO transaction = requireTransaction(reqVO.getReleaseTransactionId());
        requirePrecheckPassed(transaction);
        dossierRequirementSettingService.requireCurrentConfigHash(extractDossierRequirementConfigHash(transaction));
        MesProEdhrBatchExecutionDO batch = requireBatchExecution(transaction.getBatchExecutionId());
        String fromStatus = transaction.getReleaseStatus();
        LocalDateTime occurredAt = now();
        Long actorUserId = SecurityFrameworkUtils.getLoginUserId();
        requireReleaseOwner(batch, actorUserId);
        String password = requireReleaseSignaturePassword(reqVO.getPassword());
        adminUserApi.validatePassword(actorUserId, password);
        String reason = StrUtil.blankToDefault(StrUtil.trim(reqVO.getSubmitReason()), "负责人电子签名放行");
        ReleaseSignatureEvidence signatureEvidence = recordReleaseSignature(batch, actorUserId, password, reason,
                idempotencyKey, occurredAt);
        closeReadyBatchAfterReleaseSignature(batch, actorUserId, signatureEvidence, occurredAt);

        releaseTransactionMapper.updateById(new MesProEdhrReleaseTransactionDO()
                .setId(transaction.getId())
                .setReleaseStatus(STATUS_RELEASED)
                .setSubmitIdempotencyKey(idempotencyKey)
                .setSubmittedBy(actorUserId)
                .setSubmittedAt(occurredAt)
                .setApprovalIdempotencyKey(idempotencyKey)
                .setApprovedBy(actorUserId)
                .setApprovedAt(occurredAt)
                .setApprovalSignoffEvidenceHash(signatureEvidence.aggregateHash())
                .setApprovalOpinion(reason));
        transaction = releaseTransactionMapper.selectById(transaction.getId());
        recordTransactionEvent(transaction, EVENT_TYPE_SUBMIT, fromStatus, STATUS_RELEASED,
                actorUserId, reason, null, idempotencyKey, signatureEvidence.aggregateHash(), occurredAt);
        batch = requireBatchExecution(transaction.getBatchExecutionId());
        return toResp(batch, transaction);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrReleaseRespVO approve(MesProEdhrReleaseApproveReqVO reqVO) {
        String idempotencyKey = requireIdempotencyKey(reqVO.getIdempotencyKey());
        String signoffEvidenceHash = requireSignoffEvidence(reqVO.getSignoffEvidenceHash());
        MesProEdhrReleaseTransactionEventDO existingEvent =
                releaseTransactionEventMapper.selectByReleaseTransactionIdAndEventTypeAndIdempotencyKey(
                        reqVO.getReleaseTransactionId(), EVENT_TYPE_APPROVE, idempotencyKey);
        if (existingEvent != null) {
            return get(reqVO.getReleaseTransactionId());
        }

        MesProEdhrReleaseTransactionDO transaction = requireTransaction(reqVO.getReleaseTransactionId());
        requirePendingApproval(transaction);
        MesProEdhrWorkTaskDO approvalTask =
                workTaskService.validateReleaseApprovalTask(null, transaction.getId());
        String fromStatus = transaction.getReleaseStatus();
        LocalDateTime occurredAt = now();
        Long actorUserId = SecurityFrameworkUtils.getLoginUserId();
        String opinion = StrUtil.trim(reqVO.getApprovalOpinion());

        releaseTransactionMapper.updateById(new MesProEdhrReleaseTransactionDO()
                .setId(transaction.getId())
                .setReleaseStatus(STATUS_RELEASED)
                .setApprovalIdempotencyKey(idempotencyKey)
                .setApprovedBy(actorUserId)
                .setApprovedAt(occurredAt)
                .setApprovalSignoffEvidenceHash(signoffEvidenceHash)
                .setApprovalOpinion(opinion));
        transaction = releaseTransactionMapper.selectById(transaction.getId());
        workTaskService.completeReleaseApprovalTask(approvalTask.getId(), transaction.getId(), "APPROVE", opinion);
        recordTransactionEvent(transaction, EVENT_TYPE_APPROVE, fromStatus, STATUS_RELEASED,
                actorUserId, null, opinion, idempotencyKey, signoffEvidenceHash, occurredAt);
        return toResp(requireBatchExecution(transaction.getBatchExecutionId()), transaction);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrReleaseRespVO reject(MesProEdhrReleaseRejectReqVO reqVO) {
        String idempotencyKey = requireIdempotencyKey(reqVO.getIdempotencyKey());
        String reason = requireReason(reqVO.getRejectReason());
        MesProEdhrReleaseTransactionEventDO existingEvent =
                releaseTransactionEventMapper.selectByReleaseTransactionIdAndEventTypeAndIdempotencyKey(
                        reqVO.getReleaseTransactionId(), EVENT_TYPE_REJECT, idempotencyKey);
        if (existingEvent != null) {
            return get(reqVO.getReleaseTransactionId());
        }

        MesProEdhrReleaseTransactionDO transaction = requireTransaction(reqVO.getReleaseTransactionId());
        requirePendingApproval(transaction);
        MesProEdhrWorkTaskDO approvalTask =
                workTaskService.validateReleaseApprovalTask(null, transaction.getId());
        String fromStatus = transaction.getReleaseStatus();
        LocalDateTime occurredAt = now();
        Long actorUserId = SecurityFrameworkUtils.getLoginUserId();

        releaseTransactionMapper.updateById(new MesProEdhrReleaseTransactionDO()
                .setId(transaction.getId())
                .setReleaseStatus(STATUS_REJECTED)
                .setRejectedBy(actorUserId)
                .setRejectedAt(occurredAt)
                .setRejectReason(reason));
        transaction = releaseTransactionMapper.selectById(transaction.getId());
        workTaskService.completeReleaseApprovalTask(approvalTask.getId(), transaction.getId(), "REJECT", reason);
        recordTransactionEvent(transaction, EVENT_TYPE_REJECT, fromStatus, STATUS_REJECTED,
                actorUserId, reason, null, idempotencyKey, null, occurredAt);
        return toResp(requireBatchExecution(transaction.getBatchExecutionId()), transaction);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrReleaseRespVO withdraw(MesProEdhrReleaseWithdrawReqVO reqVO) {
        String idempotencyKey = requireIdempotencyKey(reqVO.getIdempotencyKey());
        String reason = requireReason(reqVO.getWithdrawReason());
        MesProEdhrReleaseTransactionEventDO existingEvent =
                releaseTransactionEventMapper.selectByReleaseTransactionIdAndEventTypeAndIdempotencyKey(
                        reqVO.getReleaseTransactionId(), EVENT_TYPE_WITHDRAW, idempotencyKey);
        if (existingEvent != null) {
            return get(reqVO.getReleaseTransactionId());
        }

        MesProEdhrReleaseTransactionDO transaction = requireTransaction(reqVO.getReleaseTransactionId());
        requirePendingApproval(transaction);
        String fromStatus = transaction.getReleaseStatus();
        LocalDateTime occurredAt = now();
        Long actorUserId = SecurityFrameworkUtils.getLoginUserId();

        releaseTransactionMapper.updateById(new MesProEdhrReleaseTransactionDO()
                .setId(transaction.getId())
                .setReleaseStatus(STATUS_WITHDRAWN)
                .setWithdrawnBy(actorUserId)
                .setWithdrawnAt(occurredAt)
                .setWithdrawReason(reason));
        transaction = releaseTransactionMapper.selectById(transaction.getId());
        workTaskService.cancelReleaseApprovalTask(transaction.getId(), reason);
        recordTransactionEvent(transaction, EVENT_TYPE_WITHDRAW, fromStatus, STATUS_WITHDRAWN,
                actorUserId, reason, null, idempotencyKey, null, occurredAt);
        return toResp(requireBatchExecution(transaction.getBatchExecutionId()), transaction);
    }

    @Override
    public PageResult<MesProEdhrReleaseCheckItemRespVO> getCheckItemPage(MesProEdhrReleaseCheckItemPageReqVO reqVO) {
        return BeanUtils.toBean(releaseCheckItemMapper.selectPage(reqVO), MesProEdhrReleaseCheckItemRespVO.class);
    }

    @Override
    public PageResult<MesProEdhrReleaseEventRespVO> getEventPage(MesProEdhrReleaseEventPageReqVO reqVO) {
        return BeanUtils.toBean(releaseTransactionEventMapper.selectPage(reqVO), MesProEdhrReleaseEventRespVO.class);
    }

    private EdhrBatchExecutionPageReqVO toBatchPageReq(MesProEdhrReleasePageReqVO reqVO) {
        EdhrBatchExecutionPageReqVO batchReqVO = new EdhrBatchExecutionPageReqVO();
        batchReqVO.setPageNo(reqVO.getPageNo());
        batchReqVO.setPageSize(reqVO.getPageSize());
        batchReqVO.setBatchExecutionCode(reqVO.getBatchExecutionCode());
        batchReqVO.setWorkOrderCode(reqVO.getWorkOrderCode());
        batchReqVO.setBatchCode(reqVO.getBatchCode());
        batchReqVO.setProductCode(reqVO.getProductCode());
        batchReqVO.setStatuses(reqVO.getBatchExecutionStatuses());
        batchReqVO.setExcludeStatuses(reqVO.getExcludeBatchExecutionStatuses());
        batchReqVO.setCompletedTraceOnly(reqVO.getCompletedTraceOnly());
        batchReqVO.setCreateTime(reqVO.getCreateTime());
        return batchReqVO;
    }

    private boolean statusMatches(MesProEdhrReleasePageReqVO reqVO, MesProEdhrReleaseRespVO item) {
        return matches(reqVO.getReleaseStatus(), item.getReleaseStatus())
                && matches(reqVO.getDhrStatus(), item.getDhrStatus())
                && matches(reqVO.getInspectionStatus(), item.getInspectionStatus());
    }

    private boolean batchExecutionStatusMatches(MesProEdhrReleasePageReqVO reqVO, MesProEdhrReleaseRespVO item) {
        List<Integer> excludedStatuses = reqVO.getExcludeBatchExecutionStatuses();
        if (excludedStatuses != null && excludedStatuses.contains(item.getBatchExecutionStatus())) {
            return false;
        }
        List<Integer> expectedStatuses = reqVO.getBatchExecutionStatuses();
        if (expectedStatuses != null && !expectedStatuses.isEmpty()
                && !expectedStatuses.contains(item.getBatchExecutionStatus())) {
            return false;
        }
        if (Boolean.TRUE.equals(reqVO.getCompletedTraceOnly())) {
            return STATUS_RELEASED.equals(item.getReleaseStatus())
                    || Objects.equals(item.getBatchExecutionStatus(),
                    MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_ARCHIVED)
                    || Objects.equals(item.getBatchExecutionStatus(),
                    MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_REJECTED);
        }
        return true;
    }

    private boolean matches(String expected, String actual) {
        return StrUtil.isBlank(expected) || Objects.equals(expected, actual);
    }

    private MesProEdhrBatchExecutionDO requireBatchExecution(Long batchExecutionId) {
        MesProEdhrBatchExecutionDO batch = batchExecutionId == null ? null : batchExecutionMapper.selectById(batchExecutionId);
        if (batch == null) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_NOT_EXISTS);
        }
        return batch;
    }

    private MesProEdhrReleaseTransactionDO requireTransaction(Long releaseTransactionId) {
        MesProEdhrReleaseTransactionDO transaction = releaseTransactionId == null
                ? null : releaseTransactionMapper.selectById(releaseTransactionId);
        if (transaction == null) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_NOT_EXISTS);
        }
        return transaction;
    }

    private void requirePrecheckEditable(MesProEdhrReleaseTransactionDO transaction) {
        if (STATUS_PRECHECK_REQUIRED.equals(transaction.getReleaseStatus())
                || STATUS_PRECHECK_FAILED.equals(transaction.getReleaseStatus())
                || STATUS_PRECHECK_PASSED.equals(transaction.getReleaseStatus())
                || STATUS_REJECTED.equals(transaction.getReleaseStatus())
                || STATUS_WITHDRAWN.equals(transaction.getReleaseStatus())) {
            return;
        }
        throw exception(PRO_EDHR_RELEASE_STATUS_INVALID);
    }

    private void requirePrecheckPassed(MesProEdhrReleaseTransactionDO transaction) {
        Integer failedCheckCount = transaction.getFailedCheckCount();
        Integer blockingCheckCount = transaction.getBlockingCheckCount();
        if (STATUS_PRECHECK_PASSED.equals(transaction.getReleaseStatus())
                && zero(failedCheckCount)
                && zero(blockingCheckCount)) {
            return;
        }
        throw exception(PRO_EDHR_RELEASE_PRECHECK_REQUIRED);
    }

    private void requirePendingApproval(MesProEdhrReleaseTransactionDO transaction) {
        if (STATUS_PENDING_APPROVAL.equals(transaction.getReleaseStatus())) {
            return;
        }
        throw exception(PRO_EDHR_RELEASE_STATUS_INVALID);
    }

    private String requireReason(String rawReason) {
        String reason = StrUtil.trim(rawReason);
        if (StrUtil.isBlank(reason)) {
            throw exception(PRO_EDHR_RELEASE_REASON_REQUIRED);
        }
        return reason;
    }

    private String requireSignoffEvidence(String rawSignoffEvidenceHash) {
        String signoffEvidenceHash = StrUtil.trim(rawSignoffEvidenceHash);
        if (StrUtil.isBlank(signoffEvidenceHash)) {
            throw exception(PRO_EDHR_RELEASE_SIGNOFF_REQUIRED);
        }
        return signoffEvidenceHash;
    }

    private String requireReleaseSignaturePassword(String rawPassword) {
        String password = StrUtil.trim(rawPassword);
        if (StrUtil.isBlank(password)) {
            throw exception(PRO_EDHR_RELEASE_SIGNATURE_PASSWORD_REQUIRED);
        }
        return password;
    }

    private String requireIdempotencyKey(String rawIdempotencyKey) {
        String idempotencyKey = StrUtil.trim(rawIdempotencyKey);
        if (StrUtil.isBlank(idempotencyKey)) {
            throw exception(PRO_EDHR_RELEASE_IDEMPOTENCY_KEY_REQUIRED);
        }
        return idempotencyKey;
    }

    private boolean zero(Integer value) {
        return value == null || value == 0;
    }

    private boolean isFailedCheck(MesProEdhrReleaseCheckItemDO item) {
        return CHECK_RESULT_FAIL.equals(item.getCheckResult())
                || CHECK_RESULT_BLOCKER.equals(item.getCheckResult())
                || SEVERITY_BLOCKER.equals(item.getSeverity());
    }

    private MesProEdhrReleaseTransactionDO buildInitialTransaction(MesProEdhrBatchExecutionDO batch) {
        return new MesProEdhrReleaseTransactionDO()
                .setReleaseCode("EDHR-REL-" + batch.getId())
                .setBatchExecutionId(batch.getId())
                .setBatchExecutionCode(batch.getBatchExecutionCode())
                .setWorkOrderId(batch.getWorkOrderId())
                .setWorkOrderCode(batch.getWorkOrderCode())
                .setBatchCode(batch.getBatchCode())
                .setProductId(batch.getProductId())
                .setProductCode(batch.getProductCode())
                .setProductName(batch.getProductName())
                .setRouteId(batch.getRouteId())
                .setRouteCode(batch.getRouteCode())
                .setRouteName(batch.getRouteName())
                .setDhrStatus(STATUS_PRECHECK_REQUIRED)
                .setInspectionStatus(STATUS_PRECHECK_REQUIRED)
                .setDeviationStatus(STATUS_PRECHECK_REQUIRED)
                .setReworkStatus(STATUS_PRECHECK_REQUIRED)
                .setScrapStatus(STATUS_PRECHECK_REQUIRED)
                .setInventoryStatus(STATUS_PRECHECK_REQUIRED)
                .setReleaseStatus(STATUS_PRECHECK_REQUIRED)
                .setRequiredCheckCount(0)
                .setFailedCheckCount(0)
                .setBlockingCheckCount(0)
                .setVersion(1);
    }

    private List<MesProEdhrReleaseCheckItemDO> buildCheckItems(Long releaseTransactionId,
                                                              MesProEdhrBatchExecutionDO batch,
                                                              LocalDateTime checkedAt,
                                                              MesProEdhrReleaseDossierRequirementState
                                                                      dossierRequirementState) {
        return List.of(
                buildDhrCompletenessItem(releaseTransactionId, batch, checkedAt),
                buildDossierRequirementItem(releaseTransactionId, batch, checkedAt,
                        dossierRequirementState.incomingInspectionReportRequired(),
                        MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_INCOMING_INSPECTION_REPORT,
                        CHECK_DOSSIER_INCOMING_INSPECTION_REPORT, "来料检报告",
                        "来料检报告资料限制"),
                buildDossierRequirementItem(releaseTransactionId, batch, checkedAt,
                        dossierRequirementState.sterilizationReportRequired(),
                        MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_STERILIZATION_REPORT,
                        CHECK_DOSSIER_STERILIZATION_REPORT, "灭菌报告",
                        "灭菌报告资料限制"),
                buildDossierRequirementItem(releaseTransactionId, batch, checkedAt,
                        dossierRequirementState.finishedProductInspectionReportRequired(),
                        MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_FINISHED_PRODUCT_INSPECTION_REPORT,
                        CHECK_DOSSIER_FINISHED_PRODUCT_INSPECTION_REPORT, "成品检报告",
                        "成品检报告资料限制"),
                buildDossierRequirementItem(releaseTransactionId, batch, checkedAt,
                        dossierRequirementState.finishedProductInspectionRecordRequired(),
                        MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_FINISHED_PRODUCT_INSPECTION_RECORD,
                        CHECK_DOSSIER_FINISHED_PRODUCT_INSPECTION_RECORD, "成品检记录",
                        "成品检记录限制"),
                buildSourceNotIntegratedItem(releaseTransactionId, batch, checkedAt, CHECK_INSPECTION_RESULT,
                        "检验结果检查", "INSPECTION", MODULE_QMS,
                        "未接入当前批次/SN 的 IQC/IPQC/OQC/RQC 合格记录来源",
                        "补齐检验记录接口或配置检验来源后重新预检"),
                buildSourceNotIntegratedItem(releaseTransactionId, batch, checkedAt, CHECK_DEVIATION_CLOSED,
                        "偏差关闭检查", "DEVIATION", MODULE_QMS,
                        "未接入当前批次/SN 的偏差关闭状态来源",
                        "确认偏差记录已关闭并接入偏差查询后重新预检"),
                buildSourceNotIntegratedItem(releaseTransactionId, batch, checkedAt, CHECK_REWORK_CLOSED,
                        "返工完成检查", "REWORK", MODULE_MES,
                        "未接入当前批次/SN 的返工审批与完成记录来源",
                        "补齐返工记录和审批状态后重新预检"),
                buildSourceNotIntegratedItem(releaseTransactionId, batch, checkedAt, CHECK_SCRAP_RECORDED,
                        "报废记录检查", "SCRAP", MODULE_MES,
                        "未接入当前批次/SN 的报废记录来源",
                        "确认报废记录齐套或明确无报废后重新预检"),
                buildSourceNotIntegratedItem(releaseTransactionId, batch, checkedAt, CHECK_INVENTORY_CONSISTENCY,
                        "库存一致性检查", "INVENTORY", MODULE_WMS,
                        "未接入当前批次/SN 的库存数量、状态和质量状态来源",
                        "接入库存记录并确认放行数量、批次/SN 与库存状态一致"));
    }

    private MesProEdhrReleaseCheckItemDO buildDossierRequirementItem(Long releaseTransactionId,
                                                                     MesProEdhrBatchExecutionDO batch,
                                                                     LocalDateTime checkedAt,
                                                                     boolean required,
                                                                     String nodeType,
                                                                     String checkCode,
                                                                     String nodeLabel,
                                                                     String checkName) {
        if (!required) {
            return buildItem(releaseTransactionId, batch, checkedAt, checkCode, CATEGORY_DOSSIER, checkName,
                    CHECK_RESULT_NOT_APPLICABLE, SEVERITY_INFO, MODULE_EDHR,
                    SOURCE_OBJECT_TYPE_SPECIAL_NODE_ATTACHMENT, String.valueOf(batch.getId()), nodeLabel,
                    nodeLabel + "限制未开启，放行不要求上传该资料",
                    "无需处理；如需强制上传，请由金手指在个人中心配置页签开启该限制");
        }

        DossierRequirementEvidence evidence = resolveDossierRequirementEvidence(batch.getId(), nodeType, nodeLabel);
        if (evidence.pass()) {
            return buildItem(releaseTransactionId, batch, checkedAt, checkCode, CATEGORY_DOSSIER, checkName,
                    CHECK_RESULT_PASS, SEVERITY_INFO, MODULE_EDHR,
                    SOURCE_OBJECT_TYPE_SPECIAL_NODE_ATTACHMENT, String.valueOf(evidence.sourceTaskId()),
                    evidence.sourceTaskCode(),
                    nodeLabel + "节点已完成且存在已保存 ADD 附件",
                    "无需处理");
        }
        return buildItem(releaseTransactionId, batch, checkedAt, checkCode, CATEGORY_DOSSIER, checkName,
                CHECK_RESULT_BLOCKER, SEVERITY_BLOCKER, MODULE_EDHR,
                SOURCE_OBJECT_TYPE_SPECIAL_NODE_ATTACHMENT, String.valueOf(evidence.sourceTaskId()),
                evidence.sourceTaskCode(), evidence.failureReason(),
                "完成" + nodeLabel + "特殊节点并保存至少 1 个 ADD 附件后重新预检");
    }

    private DossierRequirementEvidence resolveDossierRequirementEvidence(Long batchExecutionId,
                                                                         String nodeType,
                                                                         String nodeLabel) {
        List<MesProEdhrBatchExecutionTaskDO> tasks =
                batchExecutionTaskMapper.selectListByBatchExecutionId(batchExecutionId).stream()
                        .filter(task -> Objects.equals(nodeType, task.getNodeType()))
                        .toList();
        if (tasks.isEmpty()) {
            return DossierRequirementEvidence.fail(batchExecutionId, nodeLabel,
                    nodeLabel + "特殊节点缺失，无法确认放行资料已上传");
        }
        MesProEdhrBatchExecutionTaskDO approvedTaskWithAttachment = tasks.stream()
                .filter(task -> Objects.equals(task.getStatus(), MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED))
                .filter(this::hasSavedSpecialNodeAddAttachment)
                .findFirst()
                .orElse(null);
        if (approvedTaskWithAttachment != null) {
            return DossierRequirementEvidence.pass(approvedTaskWithAttachment.getId(),
                    StrUtil.blankToDefault(approvedTaskWithAttachment.getProcessName(), nodeLabel));
        }

        MesProEdhrBatchExecutionTaskDO approvedTask = tasks.stream()
                .filter(task -> Objects.equals(task.getStatus(), MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED))
                .findFirst()
                .orElse(null);
        if (approvedTask != null) {
            return DossierRequirementEvidence.fail(approvedTask.getId(),
                    StrUtil.blankToDefault(approvedTask.getProcessName(), nodeLabel),
                    nodeLabel + "节点已完成但缺少已保存 ADD 附件");
        }
        MesProEdhrBatchExecutionTaskDO representativeTask = tasks.get(0);
        String reason = Objects.equals(representativeTask.getStatus(), MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_SKIPPED)
                ? nodeLabel + "节点已跳过，缺少已完成资料证据"
                : nodeLabel + "节点未完成，当前状态=" + representativeTask.getStatus();
        return DossierRequirementEvidence.fail(representativeTask.getId(),
                StrUtil.blankToDefault(representativeTask.getProcessName(), nodeLabel), reason);
    }

    private boolean hasSavedSpecialNodeAddAttachment(MesProEdhrBatchExecutionTaskDO task) {
        if (task == null || task.getId() == null) {
            return false;
        }
        return attachmentMapper.selectListByBatchTaskId(task.getId()).stream()
                .anyMatch(this::isSavedSpecialNodeAddAttachment);
    }

    private boolean isSavedSpecialNodeAddAttachment(MesProBatchRecordExecutionAttachmentDO attachment) {
        return attachment != null
                && Objects.equals(attachment.getExecutionId(), SPECIAL_NODE_ATTACHMENT_EXECUTION_ID)
                && Objects.equals(attachment.getAttachmentAction(), SPECIAL_NODE_ATTACHMENT_ACTION_ADD)
                && attachment.getFileId() != null
                && StrUtil.isNotBlank(attachment.getFileUrl())
                && StrUtil.isNotBlank(attachment.getStoragePath())
                && StrUtil.isNotBlank(attachment.getFileName())
                && StrUtil.isNotBlank(attachment.getSha256())
                && StrUtil.isNotBlank(attachment.getAttachmentHash());
    }

    private MesProEdhrReleaseCheckItemDO buildDhrCompletenessItem(Long releaseTransactionId,
                                                                  MesProEdhrBatchExecutionDO batch,
                                                                  LocalDateTime checkedAt) {
        boolean pass = ordinaryProcessFillEvidenceComplete(batch.getId());
        String failureReason = pass ? "普通工序已填写完成并提交电子签名证据"
                : "普通工序未填写完成或缺少提交电子签名证据";
        String suggestion = pass ? "无需处理"
                : "完成普通工序填写并提交电子签名后重新预检";
        return buildItem(releaseTransactionId, batch, checkedAt, CHECK_DHR_COMPLETENESS,
                "DHR", "DHR 完整性检查", pass ? CHECK_RESULT_PASS : CHECK_RESULT_FAIL,
                pass ? SEVERITY_INFO : SEVERITY_BLOCKER, MODULE_EDHR,
                "EDHR_BATCH_EXECUTION", String.valueOf(batch.getId()), batch.getBatchExecutionCode(),
                failureReason, suggestion);
    }

    private boolean ordinaryProcessFillEvidenceComplete(Long batchExecutionId) {
        List<MesProEdhrBatchExecutionTaskDO> tasks =
                batchExecutionTaskMapper.selectListByBatchExecutionId(batchExecutionId);
        List<MesProEdhrBatchExecutionTaskDO> ordinaryTasks = tasks.stream()
                .filter(task -> Objects.equals(MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_ROUTE_FORM, task.getNodeType()))
                .filter(task -> Boolean.TRUE.equals(task.getRequiredFlag()))
                .filter(task -> !"SKIPPABLE_CONTROLLED".equals(task.getRequiredPolicy()))
                .toList();
        if (ordinaryTasks.isEmpty()) {
            return false;
        }
        return ordinaryTasks.stream().allMatch(this::ordinaryTaskFillEvidenceComplete);
    }

    private boolean ordinaryTaskFillEvidenceComplete(MesProEdhrBatchExecutionTaskDO task) {
        if (!Objects.equals(task.getStatus(), MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED)) {
            return false;
        }
        if (hasFormCenterRouteEvidenceContext(task)) {
            return formCenterRouteTaskFillEvidenceComplete(task);
        }
        if (task.getExecutionId() == null) {
            return false;
        }
        MesProBatchRecordExecutionDO execution = executionMapper.selectById(task.getExecutionId());
        if (execution == null || !Objects.equals(execution.getStatus(), MesProEdhrApprovalStatusMapping.EXECUTION_STATUS_FILL_COMPLETED)
                || execution.getSubmittedAt() == null
                || execution.getClosedAt() == null
                || StrUtil.isBlank(execution.getCellValuesHash())
                || execution.getFieldAuditRevision() == null
                || StrUtil.isBlank(execution.getFieldAuditHeadHash())) {
            return false;
        }
        return executionSignatureMapper.selectCount(new LambdaQueryWrapperX<MesProBatchRecordExecutionSignatureDO>()
                .eq(MesProBatchRecordExecutionSignatureDO::getExecutionId, execution.getId())
                .eq(MesProBatchRecordExecutionSignatureDO::getActionType,
                        MesProBatchRecordExecutionSignatureService.ACTION_SUBMIT)
                .eq(MesProBatchRecordExecutionSignatureDO::getPasswordVerified, Boolean.TRUE)
                .eq(MesProBatchRecordExecutionSignatureDO::getFieldAuditRevision, execution.getFieldAuditRevision())
                .eq(MesProBatchRecordExecutionSignatureDO::getFieldAuditHeadHash, execution.getFieldAuditHeadHash())
                .eq(MesProBatchRecordExecutionSignatureDO::getCellValuesHash, execution.getCellValuesHash())) > 0;
    }

    private boolean hasFormCenterRouteEvidenceContext(MesProEdhrBatchExecutionTaskDO task) {
        return task != null && (task.getFormCenterInstanceId() != null
                || task.getFormTemplateId() != null
                || task.getFormTemplateVersionId() != null
                || StrUtil.isNotBlank(task.getFormBindingKey()));
    }

    private boolean formCenterRouteTaskFillEvidenceComplete(MesProEdhrBatchExecutionTaskDO task) {
        if (!hasCompleteFormCenterRouteEvidenceContext(task)) {
            return false;
        }
        FormActionInstanceDO instance = formActionInstanceMapper.selectById(task.getFormCenterInstanceId());
        if (instance == null
                || !Objects.equals(FormInstanceStatus.EFFECTIVE.name(), instance.getStatus())
                || !Objects.equals("MES", instance.getSystemCode())
                || !Objects.equals("EDHR_ROUTE_FORM", instance.getObjectType())
                || !Objects.equals("ACTIVE", instance.getObjectState())) {
            return false;
        }
        return formCenterInstanceBelongsToRouteTask(task, instance);
    }

    private boolean hasCompleteFormCenterRouteEvidenceContext(MesProEdhrBatchExecutionTaskDO task) {
        return task != null
                && task.getFormCenterInstanceId() != null
                && task.getFormTemplateId() != null
                && task.getFormTemplateVersionId() != null
                && StrUtil.isNotBlank(task.getFormBindingKey());
    }

    private boolean formCenterInstanceBelongsToRouteTask(MesProEdhrBatchExecutionTaskDO task,
                                                         FormActionInstanceDO instance) {
        Long instanceObjectTaskId = parsePositiveLongOrNull(instance.getObjectId());
        if (instanceObjectTaskId == null) {
            return false;
        }
        if (Objects.equals(task.getId(), instanceObjectTaskId)) {
            return true;
        }
        if (!Objects.equals("BATCH_SHARED", task.getInstanceScope())) {
            return false;
        }
        MesProEdhrBatchExecutionTaskDO representativeTask = batchExecutionTaskMapper.selectById(instanceObjectTaskId);
        return representativeTask != null
                && Objects.equals(task.getBatchExecutionId(), representativeTask.getBatchExecutionId())
                && Objects.equals(MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_ROUTE_FORM,
                representativeTask.getNodeType())
                && Objects.equals(task.getFormCenterInstanceId(), representativeTask.getFormCenterInstanceId())
                && Objects.equals(task.getFormTemplateId(), representativeTask.getFormTemplateId())
                && Objects.equals(task.getFormTemplateVersionId(), representativeTask.getFormTemplateVersionId())
                && Objects.equals(task.getSharedFormKey(), representativeTask.getSharedFormKey());
    }

    private Long parsePositiveLongOrNull(String value) {
        if (StrUtil.isBlank(value) || value.chars().anyMatch(ch -> !Character.isDigit(ch))) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void requireReleaseOwner(MesProEdhrBatchExecutionDO batch, Long actorUserId) {
        MesProEdhrWorkTaskAssignmentRuleDO closeRule = batch.getRouteId() == null ? null
                : workTaskAssignmentRuleMapper.selectEnabledByScopeAndType(RULE_SCOPE_TYPE_ROUTE,
                batch.getRouteId(), MesProEdhrWorkTaskService.TASK_TYPE_CLOSE);
        if (isCurrentUserCloseOwner(closeRule, actorUserId)) {
            return;
        }
        throw exception(PRO_EDHR_RELEASE_OWNER_INVALID, actorUserId);
    }

    private boolean isCurrentUserCloseOwner(MesProEdhrWorkTaskAssignmentRuleDO closeRule, Long actorUserId) {
        if (closeRule == null || actorUserId == null) {
            return false;
        }
        if (Objects.equals(closeRule.getAssigneeUserId(), actorUserId)) {
            return true;
        }
        String sourceType = StrUtil.blankToDefault(closeRule.getCandidateSourceType(), CANDIDATE_SOURCE_TYPE_USER);
        return Objects.equals(sourceType, CANDIDATE_SOURCE_TYPE_USER)
                && Objects.equals(closeRule.getCandidateSourceId(), actorUserId);
    }

    private void closeReadyBatchAfterReleaseSignature(MesProEdhrBatchExecutionDO batch,
                                                      Long actorUserId,
                                                      ReleaseSignatureEvidence signatureEvidence,
                                                      LocalDateTime closedAt) {
        if (Objects.equals(batch.getStatus(), MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CLOSED)) {
            return;
        }
        if (!Objects.equals(batch.getStatus(), MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_READY_TO_CLOSE)) {
            throw exception(PRO_EDHR_RELEASE_STATUS_INVALID);
        }
        batch.setStatus(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CLOSED)
                .setClosedAt(closedAt)
                .setClosedBy(actorUserId)
                .setCloseSignatureId(signatureEvidence.signatureId())
                .setAggregateHash(signatureEvidence.aggregateHash());
        batchExecutionMapper.updateById(batch);
        workTaskService.createArchiveTaskAfterBatchClose(batch);
    }

    private ReleaseSignatureEvidence recordReleaseSignature(MesProEdhrBatchExecutionDO batch,
                                                            Long actorUserId,
                                                            String password,
                                                            String reason,
                                                            String idempotencyKey,
                                                            LocalDateTime signedAt) {
        String aggregateHash = DigestUtil.sha256Hex(String.join("|",
                String.valueOf(batch.getId()),
                ACTION_BATCH_RELEASE,
                String.valueOf(actorUserId),
                idempotencyKey,
                StrUtil.nullToEmpty(reason),
                String.valueOf(signedAt)));
        String selectedTimeAuditHash = DigestUtil.sha256Hex(String.join("|",
                MesProBatchRecordExecutionSignatureService.SIGNATURE_TIME_POLICY_VERSION,
                String.valueOf(batch.getId()),
                ACTION_BATCH_RELEASE,
                String.valueOf(actorUserId),
                String.valueOf(signedAt),
                MesProBatchRecordExecutionSignatureService.SIGNATURE_TIME_MODE_SERVER,
                String.valueOf(signedAt),
                StrUtil.EMPTY,
                StrUtil.EMPTY,
                StrUtil.EMPTY));
        MesProEdhrBatchExecutionSignatureDO signature = MesProEdhrBatchExecutionSignatureDO.builder()
                .batchExecutionId(batch.getId())
                .actorId(actorUserId)
                .actorName(String.valueOf(actorUserId))
                .actionType(ACTION_BATCH_RELEASE)
                .signatureMode(SIGNATURE_MODE_PASSWORD)
                .passwordVerified(Boolean.TRUE)
                .comment(reason)
                .signedAt(signedAt)
                .signatureDisplayAt(signedAt)
                .signatureTimeMode(MesProBatchRecordExecutionSignatureService.SIGNATURE_TIME_MODE_SERVER)
                .selectedTimePolicyVersion(MesProBatchRecordExecutionSignatureService.SIGNATURE_TIME_POLICY_VERSION)
                .selectedTimeAuditHash(selectedTimeAuditHash)
                .signatureChallengeHash(DigestUtil.sha256Hex(batch.getId() + ":" + password))
                .aggregateHash(aggregateHash)
                .build();
        batchSignatureMapper.insert(signature);
        return new ReleaseSignatureEvidence(signature.getId(), aggregateHash);
    }

    private static final class ReleaseSignatureEvidence {

        private final Long signatureId;
        private final String aggregateHash;

        private ReleaseSignatureEvidence(Long signatureId, String aggregateHash) {
            this.signatureId = signatureId;
            this.aggregateHash = aggregateHash;
        }

        private Long signatureId() {
            return signatureId;
        }

        private String aggregateHash() {
            return aggregateHash;
        }
    }

    private record DossierRequirementEvidence(boolean pass,
                                              Long sourceTaskId,
                                              String sourceTaskCode,
                                              String failureReason) {

        private static DossierRequirementEvidence pass(Long sourceTaskId, String sourceTaskCode) {
            return new DossierRequirementEvidence(true, sourceTaskId, sourceTaskCode, null);
        }

        private static DossierRequirementEvidence fail(Long sourceTaskId, String sourceTaskCode, String failureReason) {
            return new DossierRequirementEvidence(false, sourceTaskId, sourceTaskCode, failureReason);
        }
    }

    private MesProEdhrReleaseCheckItemDO buildSourceNotIntegratedItem(Long releaseTransactionId,
                                                                      MesProEdhrBatchExecutionDO batch,
                                                                      LocalDateTime checkedAt,
                                                                      String checkCode,
                                                                      String checkName,
                                                                      String category,
                                                                      String module,
                                                                      String failureReason,
                                                                      String suggestion) {
        return buildItem(releaseTransactionId, batch, checkedAt, checkCode, category, checkName,
                CHECK_RESULT_NOT_APPLICABLE, SEVERITY_INFO, module,
                "EDHR_BATCH_EXECUTION", String.valueOf(batch.getId()), batch.getBatchExecutionCode(),
                failureReason, suggestion);
    }

    private MesProEdhrReleaseCheckItemDO buildItem(Long releaseTransactionId,
                                                   MesProEdhrBatchExecutionDO batch,
                                                   LocalDateTime checkedAt,
                                                   String checkCode,
                                                   String checkCategory,
                                                   String checkName,
                                                   String checkResult,
                                                   String severity,
                                                   String responsibilityModule,
                                                   String sourceObjectType,
                                                   String sourceObjectId,
                                                   String sourceObjectCode,
                                                   String failureReason,
                                                   String remediationSuggestion) {
        String impactScopeJson = JSON.toJSONString(Map.of(
                "batchExecutionId", batch.getId(),
                "workOrderCode", StrUtil.nullToEmpty(batch.getWorkOrderCode()),
                "batchCode", StrUtil.nullToEmpty(batch.getBatchCode()),
                "productCode", StrUtil.nullToEmpty(batch.getProductCode()),
                "routeCode", StrUtil.nullToEmpty(batch.getRouteCode())));
        String evidenceHash = DigestUtil.sha256Hex(String.join("|",
                checkCode,
                String.valueOf(batch.getId()),
                checkResult,
                severity,
                failureReason,
                impactScopeJson));
        return MesProEdhrReleaseCheckItemDO.builder()
                .releaseTransactionId(releaseTransactionId)
                .checkCode(checkCode)
                .checkCategory(checkCategory)
                .checkName(checkName)
                .checkResult(checkResult)
                .itemStatus(ITEM_STATUS_OPEN)
                .severity(severity)
                .responsibilityModule(responsibilityModule)
                .sourceObjectType(sourceObjectType)
                .sourceObjectId(sourceObjectId)
                .sourceObjectCode(sourceObjectCode)
                .sourceRecordUrl("/mes/pro/feedback/edhr-tracking?batchExecutionId=" + batch.getId())
                .failureReason(failureReason)
                .remediationSuggestion(remediationSuggestion)
                .impactScopeJson(impactScopeJson)
                .evidenceHash(evidenceHash)
                .checkedAt(checkedAt)
                .build();
    }

    private Map<String, Object> buildSnapshot(MesProEdhrBatchExecutionDO batch,
                                              List<MesProEdhrReleaseCheckItemDO> checkItems,
                                              String releaseStatus,
                                              LocalDateTime checkedAt,
                                              String dossierRequirementConfigHash) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("batchExecutionId", batch.getId());
        snapshot.put("batchExecutionCode", batch.getBatchExecutionCode());
        snapshot.put("workOrderCode", batch.getWorkOrderCode());
        snapshot.put("batchCode", batch.getBatchCode());
        snapshot.put("productCode", batch.getProductCode());
        snapshot.put("releaseStatus", releaseStatus);
        snapshot.put("checkedAt", checkedAt);
        snapshot.put("dossierRequirementConfigHash", dossierRequirementConfigHash);
        snapshot.put("items", checkItems.stream()
                .map(item -> Map.of(
                        "checkCode", item.getCheckCode(),
                        "checkResult", item.getCheckResult(),
                        "severity", item.getSeverity(),
                        "responsibilityModule", item.getResponsibilityModule(),
                        "sourceObjectCode", item.getSourceObjectCode()))
                .toList());
        return snapshot;
    }

    private String extractDossierRequirementConfigHash(MesProEdhrReleaseTransactionDO transaction) {
        String snapshotJson = transaction == null ? null : transaction.getPrecheckSnapshotJson();
        if (StrUtil.isBlank(snapshotJson)) {
            return null;
        }
        try {
            JSONObject snapshot = JSON.parseObject(snapshotJson);
            return snapshot == null ? null : snapshot.getString("dossierRequirementConfigHash");
        } catch (RuntimeException ex) {
            throw exception(PRO_EDHR_RELEASE_DOSSIER_REQUIREMENT_CONFIG_STALE);
        }
    }

    private String buildPrecheckIdempotencyKey(MesProEdhrReleaseTransactionDO transaction, LocalDateTime checkedAt) {
        return "EDHR-PRECHECK-" + transaction.getId() + "-" + java.util.UUID.randomUUID();
    }

    private void recordPrecheckTransactionEvent(MesProEdhrReleaseTransactionDO transaction,
                                                String fromStatus,
                                                String toStatus,
                                                Long actorUserId,
                                                String idempotencyKey,
                                                String precheckSnapshotHash,
                                                List<MesProEdhrReleaseCheckItemDO> checkItems,
                                                LocalDateTime occurredAt) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("releaseTransactionId", transaction.getId());
        snapshot.put("releaseCode", transaction.getReleaseCode());
        snapshot.put("batchExecutionId", transaction.getBatchExecutionId());
        snapshot.put("batchExecutionCode", transaction.getBatchExecutionCode());
        snapshot.put("eventType", EVENT_TYPE_PRECHECK);
        snapshot.put("fromStatus", fromStatus);
        snapshot.put("toStatus", toStatus);
        snapshot.put("actorUserId", actorUserId);
        snapshot.put("reason", "放行预检");
        snapshot.put("idempotencyKey", idempotencyKey);
        snapshot.put("requestSource", "RELEASE_PRECHECK");
        snapshot.put("associatedSignatureId", "NOT_APPLICABLE");
        snapshot.put("permissionDecision", "ALLOW");
        snapshot.put("resultStatus", "SUCCESS");
        snapshot.put("precheckSnapshotHash", precheckSnapshotHash);
        snapshot.put("requiredCheckCount", transaction.getRequiredCheckCount());
        snapshot.put("failedCheckCount", transaction.getFailedCheckCount());
        snapshot.put("blockingCheckCount", transaction.getBlockingCheckCount());
        snapshot.put("checkItems", checkItems.stream().map(this::toPrecheckAuditItemPayload).toList());
        snapshot.put("occurredAt", occurredAt);
        String eventSnapshotJson = JSON.toJSONString(snapshot);
        String evidenceHash = DigestUtil.sha256Hex(String.join("|",
                String.valueOf(transaction.getId()),
                EVENT_TYPE_PRECHECK,
                StrUtil.nullToEmpty(fromStatus),
                StrUtil.nullToEmpty(toStatus),
                String.valueOf(actorUserId),
                idempotencyKey,
                precheckSnapshotHash,
                eventSnapshotJson));
        releaseTransactionEventMapper.insert(MesProEdhrReleaseTransactionEventDO.builder()
                .releaseTransactionId(transaction.getId())
                .eventType(EVENT_TYPE_PRECHECK)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .actorUserId(actorUserId)
                .reason("放行预检")
                .idempotencyKey(idempotencyKey)
                .eventSnapshotJson(eventSnapshotJson)
                .evidenceHash(evidenceHash)
                .occurredAt(occurredAt)
                .build());
    }

    private void recordPrecheckOperationAudit(MesProEdhrBatchExecutionDO batch,
                                              MesProEdhrReleaseTransactionDO transaction,
                                              String fromStatus,
                                              String toStatus,
                                              Long actorUserId,
                                              String idempotencyKey,
                                              String precheckSnapshotHash,
                                              List<MesProEdhrReleaseCheckItemDO> checkItems,
                                              String precheckSnapshotJson,
                                              LocalDateTime occurredAt) {
        Map<String, Object> beforePayload = new LinkedHashMap<>();
        beforePayload.put("releaseTransactionId", transaction.getId());
        beforePayload.put("releaseStatus", fromStatus);
        Map<String, Object> afterPayload = new LinkedHashMap<>();
        afterPayload.put("releaseTransactionId", transaction.getId());
        afterPayload.put("releaseStatus", toStatus);
        afterPayload.put("dhrStatus", transaction.getDhrStatus());
        afterPayload.put("inspectionStatus", transaction.getInspectionStatus());
        afterPayload.put("deviationStatus", transaction.getDeviationStatus());
        afterPayload.put("reworkStatus", transaction.getReworkStatus());
        afterPayload.put("scrapStatus", transaction.getScrapStatus());
        afterPayload.put("inventoryStatus", transaction.getInventoryStatus());
        afterPayload.put("requiredCheckCount", transaction.getRequiredCheckCount());
        afterPayload.put("failedCheckCount", transaction.getFailedCheckCount());
        afterPayload.put("blockingCheckCount", transaction.getBlockingCheckCount());
        afterPayload.put("precheckSnapshotHash", precheckSnapshotHash);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("requestSource", "RELEASE_PRECHECK");
        metadata.put("reason", "放行预检");
        metadata.put("idempotencyKey", idempotencyKey);
        metadata.put("associatedSignatureId", "NOT_APPLICABLE");
        metadata.put("permissionDecision", "ALLOW");
        metadata.put("resultStatus", "SUCCESS");
        metadata.put("batchExecutionId", batch.getId());
        metadata.put("releaseTransactionId", transaction.getId());
        metadata.put("fromStatus", fromStatus);
        metadata.put("toStatus", toStatus);
        metadata.put("checkItemCount", checkItems.size());
        metadata.put("failedCheckItems", checkItems.stream()
                .filter(this::isFailedCheck)
                .map(this::toPrecheckAuditItemPayload)
                .toList());
        metadata.put("blockingCheckItems", checkItems.stream()
                .filter(item -> CHECK_RESULT_BLOCKER.equals(item.getCheckResult())
                        || SEVERITY_BLOCKER.equals(item.getSeverity()))
                .map(this::toPrecheckAuditItemPayload)
                .toList());
        metadata.put("checkItems", checkItems.stream().map(this::toPrecheckAuditItemPayload).toList());
        metadata.put("precheckSnapshotHash", precheckSnapshotHash);
        metadata.put("precheckSnapshotJson", precheckSnapshotJson);
        operationAuditService.record(new MesProEdhrOperationAuditCommand()
                .setRequestId(idempotencyKey)
                .setObjectType("RELEASE_TRANSACTION")
                .setObjectId(String.valueOf(transaction.getId()))
                .setBatchExecutionId(batch.getId())
                .setRouteId(batch.getRouteId())
                .setOperationType(EVENT_TYPE_PRECHECK)
                .setActionName("执行 eDHR 放行预检")
                .setActorUserId(actorUserId)
                .setActorUsername(SecurityFrameworkUtils.getLoginUserNickname())
                .setPermissionCode("mes:pro-edhr-release:update")
                .setPermissionDecision("ALLOW")
                .setResultStatus("SUCCESS")
                .setBeforeSummaryHash(hashReleaseAuditPayload(beforePayload))
                .setAfterSummaryHash(hashReleaseAuditPayload(afterPayload))
                .setMetadataJson(JSON.toJSONString(metadata))
                .setOccurredAt(occurredAt));
    }

    private Map<String, Object> toPrecheckAuditItemPayload(MesProEdhrReleaseCheckItemDO item) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", item.getId());
        payload.put("checkCode", item.getCheckCode());
        payload.put("checkCategory", item.getCheckCategory());
        payload.put("checkName", item.getCheckName());
        payload.put("checkResult", item.getCheckResult());
        payload.put("itemStatus", item.getItemStatus());
        payload.put("severity", item.getSeverity());
        payload.put("responsibilityModule", item.getResponsibilityModule());
        payload.put("sourceObjectType", item.getSourceObjectType());
        payload.put("sourceObjectId", item.getSourceObjectId());
        payload.put("sourceObjectCode", item.getSourceObjectCode());
        payload.put("failureReason", item.getFailureReason());
        payload.put("evidenceHash", item.getEvidenceHash());
        return payload;
    }

    private Long currentAuditActorUserId() {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            throw exception(UNAUTHORIZED);
        }
        return loginUserId;
    }

    private String hashReleaseAuditPayload(Object payload) {
        return MesProBatchRecordExecutionFieldAuditHasher.sha256(JSON.toJSONString(payload));
    }

    private void recordTransactionEvent(MesProEdhrReleaseTransactionDO transaction,
                                        String eventType,
                                        String fromStatus,
                                        String toStatus,
                                        Long actorUserId,
                                        String reason,
                                        String opinion,
                                        String idempotencyKey,
                                        String signoffEvidenceHash,
                                        LocalDateTime occurredAt) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("releaseTransactionId", transaction.getId());
        snapshot.put("releaseCode", transaction.getReleaseCode());
        snapshot.put("batchExecutionId", transaction.getBatchExecutionId());
        snapshot.put("batchExecutionCode", transaction.getBatchExecutionCode());
        snapshot.put("eventType", eventType);
        snapshot.put("fromStatus", fromStatus);
        snapshot.put("toStatus", toStatus);
        snapshot.put("actorUserId", actorUserId);
        snapshot.put("reason", reason);
        snapshot.put("opinion", opinion);
        snapshot.put("idempotencyKey", idempotencyKey);
        snapshot.put("signoffEvidenceHash", signoffEvidenceHash);
        snapshot.put("failedCheckCount", transaction.getFailedCheckCount());
        snapshot.put("blockingCheckCount", transaction.getBlockingCheckCount());
        snapshot.put("occurredAt", occurredAt);
        String eventSnapshotJson = JSON.toJSONString(snapshot);
        String evidenceHash = DigestUtil.sha256Hex(String.join("|",
                String.valueOf(transaction.getId()),
                eventType,
                fromStatus,
                toStatus,
                String.valueOf(actorUserId),
                idempotencyKey,
                StrUtil.nullToEmpty(signoffEvidenceHash),
                eventSnapshotJson));
        releaseTransactionEventMapper.insert(MesProEdhrReleaseTransactionEventDO.builder()
                .releaseTransactionId(transaction.getId())
                .eventType(eventType)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .actorUserId(actorUserId)
                .reason(reason)
                .opinion(opinion)
                .idempotencyKey(idempotencyKey)
                .signoffEvidenceHash(signoffEvidenceHash)
                .eventSnapshotJson(eventSnapshotJson)
                .evidenceHash(evidenceHash)
                .occurredAt(occurredAt)
                .build());
    }

    private String resultOf(List<MesProEdhrReleaseCheckItemDO> checkItems, String checkCode) {
        return checkItems.stream()
                .filter(item -> Objects.equals(item.getCheckCode(), checkCode))
                .map(MesProEdhrReleaseCheckItemDO::getCheckResult)
                .findFirst()
                .orElse(STATUS_PRECHECK_REQUIRED);
    }

    private MesProEdhrReleaseRespVO toResp(MesProEdhrBatchExecutionDO batch,
                                           MesProEdhrReleaseTransactionDO transaction) {
        String releaseStatus = transaction == null ? STATUS_PRECHECK_REQUIRED : transaction.getReleaseStatus();
        return new MesProEdhrReleaseRespVO()
                .setReleaseTransactionId(transaction == null ? null : transaction.getId())
                .setReleaseCode(transaction == null ? null : transaction.getReleaseCode())
                .setBatchExecutionId(batch.getId())
                .setBatchExecutionCode(batch.getBatchExecutionCode())
                .setWorkOrderId(batch.getWorkOrderId())
                .setWorkOrderCode(batch.getWorkOrderCode())
                .setBatchCode(batch.getBatchCode())
                .setProductId(batch.getProductId())
                .setProductCode(batch.getProductCode())
                .setProductName(batch.getProductName())
                .setRouteId(batch.getRouteId())
                .setRouteCode(batch.getRouteCode())
                .setRouteName(batch.getRouteName())
                .setBatchExecutionStatus(batch.getStatus())
                .setDhrStatus(transaction == null ? STATUS_PRECHECK_REQUIRED : transaction.getDhrStatus())
                .setInspectionStatus(transaction == null ? STATUS_PRECHECK_REQUIRED : transaction.getInspectionStatus())
                .setDeviationStatus(transaction == null ? STATUS_PRECHECK_REQUIRED : transaction.getDeviationStatus())
                .setReworkStatus(transaction == null ? STATUS_PRECHECK_REQUIRED : transaction.getReworkStatus())
                .setScrapStatus(transaction == null ? STATUS_PRECHECK_REQUIRED : transaction.getScrapStatus())
                .setInventoryStatus(transaction == null ? STATUS_PRECHECK_REQUIRED : transaction.getInventoryStatus())
                .setReleaseStatus(releaseStatus)
                .setRequiredCheckCount(transaction == null ? 0 : transaction.getRequiredCheckCount())
                .setFailedCheckCount(transaction == null ? 0 : transaction.getFailedCheckCount())
                .setBlockingCheckCount(transaction == null ? 0 : transaction.getBlockingCheckCount())
                .setLastPrecheckAt(transaction == null ? null : transaction.getLastPrecheckAt())
                .setPrecheckSummary(resolvePrecheckSummary(releaseStatus, transaction))
                .setPrecheckSnapshotJson(transaction == null ? null : transaction.getPrecheckSnapshotJson())
                .setSubmitIdempotencyKey(transaction == null ? null : transaction.getSubmitIdempotencyKey())
                .setSubmittedBy(transaction == null ? null : transaction.getSubmittedBy())
                .setSubmittedAt(transaction == null ? null : transaction.getSubmittedAt())
                .setApprovalIdempotencyKey(transaction == null ? null : transaction.getApprovalIdempotencyKey())
                .setApprovedBy(transaction == null ? null : transaction.getApprovedBy())
                .setApprovedAt(transaction == null ? null : transaction.getApprovedAt())
                .setApprovalSignoffEvidenceHash(transaction == null ? null : transaction.getApprovalSignoffEvidenceHash())
                .setApprovalOpinion(transaction == null ? null : transaction.getApprovalOpinion())
                .setRejectedBy(transaction == null ? null : transaction.getRejectedBy())
                .setRejectedAt(transaction == null ? null : transaction.getRejectedAt())
                .setRejectReason(transaction == null ? null : transaction.getRejectReason())
                .setWithdrawnBy(transaction == null ? null : transaction.getWithdrawnBy())
                .setWithdrawnAt(transaction == null ? null : transaction.getWithdrawnAt())
                .setWithdrawReason(transaction == null ? null : transaction.getWithdrawReason());
    }

    private String resolvePrecheckSummary(String releaseStatus, MesProEdhrReleaseTransactionDO transaction) {
        if (STATUS_PRECHECK_REQUIRED.equals(releaseStatus) || transaction == null) {
            return "尚未执行放行前检查";
        }
        if (STATUS_PRECHECK_PASSED.equals(releaseStatus)) {
            return "放行前检查通过";
        }
        if (STATUS_PENDING_APPROVAL.equals(releaseStatus)) {
            return "放行审批待处理";
        }
        if (STATUS_RELEASED.equals(releaseStatus)) {
            return "放行已批准";
        }
        if (STATUS_REJECTED.equals(releaseStatus)) {
            return "放行已驳回：" + StrUtil.nullToDefault(transaction.getRejectReason(), "未记录原因");
        }
        if (STATUS_WITHDRAWN.equals(releaseStatus)) {
            return "放行已撤回：" + StrUtil.nullToDefault(transaction.getWithdrawReason(), "未记录原因");
        }
        return "放行前检查失败：" + transaction.getBlockingCheckCount() + " 个阻塞项，"
                + transaction.getFailedCheckCount() + " 个失败项";
    }

    private LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }
}
