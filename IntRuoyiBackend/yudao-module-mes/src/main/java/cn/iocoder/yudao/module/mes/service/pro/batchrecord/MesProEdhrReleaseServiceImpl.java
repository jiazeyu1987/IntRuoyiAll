package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.bpm.dal.dataobject.signature.BpmApprovalSignatureRecordDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionInstanceDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.signature.BpmApprovalSignatureRecordMapper;
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
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseCheckItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseDecisionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionAttachmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionSignatureMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseCheckItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseDecisionMapper;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleMapper;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesProductionReportManagementSummaryService;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseManagerApprovalResult;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseManagerApprovalService;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.MesReleaseUpstreamClosureCommand;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.MesReleaseUpstreamStatePort;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFinalizationCommand;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFinalizationAction;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFinalizationValidator;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseAuthoritativeContextPort;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFinalizationEvidence;
import cn.iocoder.yudao.module.mes.productionrelease.core.CompletionBackfillReceipt;
import cn.iocoder.yudao.module.mes.productionrelease.core.IndependentBatchPrerequisiteReceipt;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlocker;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlockerException;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlockerType;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowFailureRespVO;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowIdempotency;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowStage;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseMaterialGateReceipt;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseOrigin;
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
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_RELEASE_OWNER_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_RELEASE_PRECHECK_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_RELEASE_REASON_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_RELEASE_SIGNATURE_PASSWORD_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_RELEASE_SIGNOFF_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_RELEASE_STATUS_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_RELEASE_MATERIAL_MANIFEST_STALE;

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
    private static final String APPROVAL_SOURCE_TASK_TYPE_EDHR_WORK_TASK = "EDHR_WORK_TASK";
    private static final String APPROVAL_REVIEW_RESULT_APPROVE = "APPROVE";

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
    private MesProEdhrReleaseTransactionMapper releaseTransactionMapper;
    @Resource
    private MesProEdhrReleaseCheckItemMapper releaseCheckItemMapper;
    @Resource
    private MesProEdhrReleaseTransactionEventMapper releaseTransactionEventMapper;
    @Resource
    private MesProEdhrReleaseDecisionMapper releaseDecisionMapper;
    @Resource
    private MesProEdhrWorkTaskAssignmentRuleMapper workTaskAssignmentRuleMapper;
    @Resource
    private BpmApprovalSignatureRecordMapper approvalSignatureRecordMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private MesProEdhrWorkTaskService workTaskService;
    @Resource
    private MesProEdhrOperationAuditService operationAuditService;
    @Resource
    private FormActionInstanceMapper formActionInstanceMapper;
    @Resource
    private MesProEdhrCandidateResolver candidateResolver;
    @Resource
    private MesOrderReleaseCompletenessService releaseCompletenessService;
    @Resource
    private MesProductionReportManagementSummaryService reportManagementSummaryService;
    @Resource
    private MesProductionReleaseManagerApprovalService managerApprovalService;
    @Resource
    private MesReleaseUpstreamStatePort upstreamStatePort;
    @Resource
    private MesReleaseAuthoritativeContextPort authoritativeContextPort;
    @Resource
    private MesProEdhrFourMaterialGateService fourMaterialGateService;

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

    private MesProEdhrReleaseRespVO finalizeReject(MesReleaseFinalizationCommand command) {
        managerApprovalService.assertActionSupported(command.getReleaseTransactionId(), EVENT_TYPE_REJECT);
        String idempotencyKey = requireIdempotencyKey(command.getIdempotencyKey());
        String reason = requireReason(command.getDecisionReason());
        Long actorUserId = requireFinalizationActor(command);
        String finalizationPayloadHash = finalizationPayloadHash(command);
        MesProEdhrReleaseTransactionEventDO existingEvent =
                releaseTransactionEventMapper.selectByReleaseTransactionIdAndEventTypeAndIdempotencyKey(
                        command.getReleaseTransactionId(), EVENT_TYPE_REJECT, idempotencyKey);
        if (existingEvent != null) {
            requireSameFinalizationPayload(existingEvent, finalizationPayloadHash);
            return get(command.getReleaseTransactionId());
        }

        reportManagementSummaryService.lockProductionEventsByReleaseTransactionId(
                command.getReleaseTransactionId());
        MesProEdhrReleaseTransactionDO transaction = requireTransactionForUpdate(
                command.getReleaseTransactionId());
        MesProEdhrBatchExecutionDO batch = requireBatchExecution(transaction.getBatchExecutionId());
        String fromStatus = transaction.getReleaseStatus();
        LocalDateTime occurredAt = now();
        MesProEdhrWorkTaskDO approvalTask = null;
        if (STATUS_PRECHECK_PASSED.equals(fromStatus)) {
            requirePrecheckPassed(transaction);
            requireReleaseOwner(batch, actorUserId);
        } else if (STATUS_PENDING_APPROVAL.equals(fromStatus)) {
            approvalTask = workTaskService.validateReleaseApprovalTask(null, transaction.getId());
            if (approvalTask == null || approvalTask.getId() == null) {
                throw exception(PRO_EDHR_RELEASE_SIGNOFF_REQUIRED);
            }
        } else {
            throw exception(PRO_EDHR_RELEASE_STATUS_INVALID);
        }

        releaseTransactionMapper.updateById(new MesProEdhrReleaseTransactionDO()
                .setId(transaction.getId())
                .setReleaseStatus(STATUS_REJECTED)
                .setRejectedBy(actorUserId)
                .setRejectedAt(occurredAt)
                .setRejectReason(reason));
        transaction = releaseTransactionMapper.selectById(transaction.getId());
        MesProEdhrReleaseDecisionDO decision = recordFinalizationDecision(command, transaction,
                STATUS_REJECTED, finalizationPayloadHash, null, reason, occurredAt);
        releaseTransactionMapper.updateById(new MesProEdhrReleaseTransactionDO()
                .setId(transaction.getId())
                .setReleaseDecisionId(decision.getId())
                .setFinalizationPayloadHash(finalizationPayloadHash));
        transaction.setReleaseDecisionId(decision.getId())
                .setFinalizationPayloadHash(finalizationPayloadHash);
        if (approvalTask != null) {
            workTaskService.completeReleaseApprovalTask(approvalTask.getId(), transaction.getId(), "REJECT", reason);
        }
        recordTransactionEvent(transaction, EVENT_TYPE_REJECT, fromStatus, STATUS_REJECTED,
                actorUserId, reason, null, idempotencyKey, null, occurredAt, finalizationPayloadHash);
        recordTerminalOperationAudit(batch, transaction, EVENT_TYPE_REJECT, fromStatus, STATUS_REJECTED,
                actorUserId, reason, null, idempotencyKey, null, occurredAt);
        return toResp(batch, transaction);
    }

    private MesProEdhrReleaseRespVO finalizeWithdraw(MesReleaseFinalizationCommand command) {
        managerApprovalService.assertActionSupported(command.getReleaseTransactionId(), EVENT_TYPE_WITHDRAW);
        String idempotencyKey = requireIdempotencyKey(command.getIdempotencyKey());
        String reason = requireReason(command.getDecisionReason());
        Long actorUserId = requireFinalizationActor(command);
        String finalizationPayloadHash = finalizationPayloadHash(command);
        MesProEdhrReleaseTransactionEventDO existingEvent =
                releaseTransactionEventMapper.selectByReleaseTransactionIdAndEventTypeAndIdempotencyKey(
                        command.getReleaseTransactionId(), EVENT_TYPE_WITHDRAW, idempotencyKey);
        if (existingEvent != null) {
            requireSameFinalizationPayload(existingEvent, finalizationPayloadHash);
            return get(command.getReleaseTransactionId());
        }

        reportManagementSummaryService.lockProductionEventsByReleaseTransactionId(
                command.getReleaseTransactionId());
        MesProEdhrReleaseTransactionDO transaction = requireTransactionForUpdate(
                command.getReleaseTransactionId());
        requirePendingApproval(transaction);
        String fromStatus = transaction.getReleaseStatus();
        LocalDateTime occurredAt = now();

        releaseTransactionMapper.updateById(new MesProEdhrReleaseTransactionDO()
                .setId(transaction.getId())
                .setReleaseStatus(STATUS_WITHDRAWN)
                .setWithdrawnBy(actorUserId)
                .setWithdrawnAt(occurredAt)
                .setWithdrawReason(reason));
        transaction = releaseTransactionMapper.selectById(transaction.getId());
        MesProEdhrReleaseDecisionDO decision = recordFinalizationDecision(command, transaction,
                STATUS_WITHDRAWN, finalizationPayloadHash, null, reason, occurredAt);
        releaseTransactionMapper.updateById(new MesProEdhrReleaseTransactionDO()
                .setId(transaction.getId())
                .setReleaseDecisionId(decision.getId())
                .setFinalizationPayloadHash(finalizationPayloadHash));
        transaction.setReleaseDecisionId(decision.getId())
                .setFinalizationPayloadHash(finalizationPayloadHash);
        workTaskService.cancelReleaseApprovalTask(transaction.getId(), reason);
        recordTransactionEvent(transaction, EVENT_TYPE_WITHDRAW, fromStatus, STATUS_WITHDRAWN,
                actorUserId, reason, null, idempotencyKey, null, occurredAt, finalizationPayloadHash);
        MesProEdhrBatchExecutionDO batch = requireBatchExecution(transaction.getBatchExecutionId());
        recordTerminalOperationAudit(batch, transaction, EVENT_TYPE_WITHDRAW, fromStatus, STATUS_WITHDRAWN,
                actorUserId, reason, null, idempotencyKey, null, occurredAt);
        return toResp(batch, transaction);
    }

    private Long requireFinalizationActor(MesReleaseFinalizationCommand command) {
        if (command.getActorUserId() == null) {
            throw exception(UNAUTHORIZED);
        }
        return command.getActorUserId();
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
        MesProEdhrFourMaterialGateResult fourMaterialGate = fourMaterialGateService.evaluate(batch.getId());
        List<MesProEdhrReleaseCheckItemDO> checkItems =
                buildCheckItems(transaction.getId(), batch, checkedAt, fourMaterialGate);
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
                buildSnapshot(batch, checkItems, releaseStatus, checkedAt, fourMaterialGate);
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

        MesProEdhrReleaseTransactionDO transaction = requireTransactionForUpdate(reqVO.getReleaseTransactionId());
        requirePrecheckPassed(transaction);
        requirePrecheckMaterialManifestCurrent(transaction);
        MesProEdhrBatchExecutionDO batch = requireBatchExecution(transaction.getBatchExecutionId());
        String fromStatus = transaction.getReleaseStatus();
        LocalDateTime occurredAt = now();
        Long actorUserId = SecurityFrameworkUtils.getLoginUserId();
        requireReleaseOwner(batch, actorUserId);
        String password = requireReleaseSignaturePassword(reqVO.getPassword());
        adminUserApi.validatePassword(actorUserId, password);
        String reason = StrUtil.blankToDefault(StrUtil.trim(reqVO.getSubmitReason()), "负责人电子签名放行");
        MesProEdhrWorkTaskDO approvalTask = workTaskService.createReleaseApprovalTaskAfterSubmit(transaction, batch);
        if (approvalTask == null || approvalTask.getId() == null) {
            throw exception(PRO_EDHR_RELEASE_SIGNOFF_REQUIRED);
        }

        releaseTransactionMapper.updateById(new MesProEdhrReleaseTransactionDO()
                .setId(transaction.getId())
                .setReleaseStatus(STATUS_PENDING_APPROVAL)
                .setSubmitIdempotencyKey(idempotencyKey)
                .setSubmittedBy(actorUserId)
                .setSubmittedAt(occurredAt));
        transaction = releaseTransactionMapper.selectById(transaction.getId());
        recordTransactionEvent(transaction, EVENT_TYPE_SUBMIT, fromStatus, STATUS_PENDING_APPROVAL,
                actorUserId, reason, null, idempotencyKey, null, occurredAt);
        recordTerminalOperationAudit(batch, transaction, EVENT_TYPE_SUBMIT, fromStatus, STATUS_PENDING_APPROVAL,
                actorUserId, reason, null, idempotencyKey, null, occurredAt);
        return toResp(batch, transaction).setReleaseApprovalWorkTaskId(approvalTask.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrReleaseRespVO approve(MesProEdhrReleaseApproveReqVO reqVO) {
        return finalizeRelease(new MesReleaseFinalizationCommand()
                .setReleaseTransactionId(reqVO.getReleaseTransactionId())
                .setAction(MesReleaseFinalizationAction.APPROVE)
                .setReleaseApplicationId(reqVO.getReleaseApplicationId())
                .setBatchExecutionId(reqVO.getBatchExecutionId())
                .setWorkOrderId(reqVO.getWorkOrderId())
                .setOrigin(reqVO.getOrigin())
                .setEntryType(reqVO.getEntryType())
                .setActiveOrderId(reqVO.getActiveOrderId())
                .setActiveOrderExpectedVersion(reqVO.getActiveOrderExpectedVersion())
                .setPickListBindingId(reqVO.getPickListBindingId())
                .setPickListId(reqVO.getPickListId())
                .setCompletionEventId(reqVO.getCompletionEventId())
                .setCompletionBackfillReceiptId(reqVO.getCompletionBackfillReceiptId())
                .setIndependentPrerequisiteReceiptId(reqVO.getIndependentPrerequisiteReceiptId())
                .setMaterialGateReceiptId(reqVO.getMaterialGateReceiptId())
                .setMaterialGateManifestHash(reqVO.getMaterialGateManifestHash())
                .setMaterialGateSourceSnapshotHash(reqVO.getMaterialGateSourceSnapshotHash())
                .setDualProgressCompleted(reqVO.getDualProgressCompleted())
                .setThreeBackfillsSucceeded(reqVO.getThreeBackfillsSucceeded())
                .setSourceRelation(reqVO.getSourceRelation())
                .setSourceSnapshotHash(reqVO.getSourceSnapshotHash())
                .setIdempotencyKey(reqVO.getIdempotencyKey())
                .setWorkTaskId(reqVO.getWorkTaskId())
                .setExpectedVersion(reqVO.getExpectedVersion())
                .setSignoffEvidenceHash(reqVO.getSignoffEvidenceHash())
                .setApprovalOpinion(reqVO.getApprovalOpinion())
                .setIndependentPrerequisiteReceipt(reqVO.getIndependentPrerequisiteReceipt())
                .setMaterialGateReceipt(reqVO.getMaterialGateReceipt()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrReleaseRespVO finalizeRelease(MesReleaseFinalizationCommand command) {
        if (command == null) {
            throw exception(PRO_EDHR_RELEASE_STATUS_INVALID);
        }
        if (command.getAction() == null) {
            throw exception(PRO_EDHR_RELEASE_STATUS_INVALID);
        }
        Long authenticatedActorUserId = SecurityFrameworkUtils.getLoginUserId();
        if (authenticatedActorUserId == null) {
            throw exception(UNAUTHORIZED);
        }
        command.setActorUserId(authenticatedActorUserId);
        if (command.getAction() == MesReleaseFinalizationAction.APPROVE) {
            MesProEdhrFourMaterialGateResult currentGate =
                    fourMaterialGateService.requireMaterialsReady(command.getBatchExecutionId());
            MesReleaseFinalizationEvidence evidence = authoritativeContextPort.require(command);
            hydrateFromAuthoritativeEvidence(command, evidence);
            if (!Objects.equals(currentGate.manifestHash(), command.getMaterialGateManifestHash())) {
                throw exception(PRO_EDHR_RELEASE_MATERIAL_MANIFEST_STALE);
            }
            MesReleaseFinalizationValidator.validate(command, evidence, java.time.Clock.systemUTC());
            return finalizeApproval(command, evidence);
        }
        return switch (command.getAction()) {
            case REJECT -> finalizeReject(command);
            case WITHDRAW -> finalizeWithdraw(command);
            case APPROVE -> throw new IllegalStateException("approve action must use approval finalizer");
        };
    }

    private void hydrateFromAuthoritativeEvidence(MesReleaseFinalizationCommand command,
                                                   MesReleaseFinalizationEvidence evidence) {
        if (evidence == null || evidence.getMaterialGateReceipt() == null) {
            return;
        }
        MesReleaseMaterialGateReceipt gate = evidence.getMaterialGateReceipt();
        if (command.getBatchExecutionId() == null) {
            command.setBatchExecutionId(gate.getBatchExecutionId());
        }
        if (command.getMaterialGateReceiptId() == null) {
            command.setMaterialGateReceiptId(gate.getReceiptId());
        }
        if (command.getMaterialGateManifestHash() == null) {
            command.setMaterialGateManifestHash(gate.getManifestHash());
        }
        if (command.getMaterialGateSourceSnapshotHash() == null) {
            command.setMaterialGateSourceSnapshotHash(gate.getSourceSnapshotHash());
        }
    }

    private MesProEdhrReleaseRespVO finalizeApproval(
            MesReleaseFinalizationCommand command,
            MesReleaseFinalizationEvidence evidence) {
        MesProEdhrReleaseTransactionDO transaction = requireTransactionForUpdate(command.getReleaseTransactionId());
        if (!Objects.equals(transaction.getBatchExecutionId(), command.getBatchExecutionId())) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_NOT_EXISTS);
        }
        MesProEdhrBatchExecutionDO batch = requireBatchExecution(command.getBatchExecutionId());
        if (managerApprovalService.isManagedReleaseTransaction(command.getReleaseTransactionId())) {
            MesProEdhrReleaseApproveReqVO approve = new MesProEdhrReleaseApproveReqVO()
                    .setReleaseTransactionId(command.getReleaseTransactionId())
                    .setReleaseApplicationId(command.getReleaseApplicationId())
                    .setBatchExecutionId(command.getBatchExecutionId())
                    .setWorkOrderId(command.getWorkOrderId())
                    .setOrigin(command.getOrigin())
                    .setEntryType(command.getEntryType())
                    .setActiveOrderId(command.getActiveOrderId())
                    .setPickListId(command.getPickListId())
                    .setPickListBindingId(command.getPickListBindingId())
                    .setCompletionEventId(command.getCompletionEventId())
                    .setCompletionBackfillReceiptId(command.getCompletionBackfillReceiptId())
                    .setIndependentPrerequisiteReceiptId(command.getIndependentPrerequisiteReceiptId())
                    .setMaterialGateReceiptId(command.getMaterialGateReceiptId())
                    .setMaterialGateManifestHash(command.getMaterialGateManifestHash())
                    .setMaterialGateSourceSnapshotHash(command.getMaterialGateSourceSnapshotHash())
                    .setWorkTaskId(command.getWorkTaskId())
                    .setExpectedVersion(command.getExpectedVersion())
                    .setIdempotencyKey(command.getIdempotencyKey())
                    .setSignoffEvidenceHash(command.getSignoffEvidenceHash())
                    .setApprovalOpinion(command.getApprovalOpinion());
            MesProductionReleaseManagerApprovalResult prepared = managerApprovalService.prepareForFinalization(
                    command.getActorUserId(), approve);
            if (prepared.isReplayed()) {
                return toResp(prepared.getBatchExecution(), prepared.getReleaseTransaction());
            }
            String managerPayloadHash = finalizationPayloadHash(command, evidence);
            LocalDateTime occurredAt = now();
            if (releaseTransactionMapper.approveProductionRelease(
                    transaction.getId(), command.getExpectedVersion(), command.getActorUserId(),
                    command.getIdempotencyKey(), command.getSignoffEvidenceHash(),
                    StrUtil.trim(command.getApprovalOpinion()), occurredAt) != 1) {
                throw exception(PRO_EDHR_RELEASE_STATUS_INVALID);
            }
            MesProEdhrReleaseTransactionDO released = releaseTransactionMapper.selectById(transaction.getId());
            if (released == null || !Objects.equals(released.getReleaseStatus(), STATUS_RELEASED)) {
                throw exception(PRO_EDHR_RELEASE_STATUS_INVALID);
            }
            MesProEdhrReleaseDecisionDO decision = recordFinalizationDecision(command, released,
                    evidence, STATUS_RELEASED, managerPayloadHash, null, command.getApprovalOpinion(), occurredAt);
            released.setReleaseDecisionId(decision.getId())
                    .setFinalizationPayloadHash(managerPayloadHash);
            releaseTransactionMapper.updateById(new MesProEdhrReleaseTransactionDO()
                    .setId(released.getId())
                    .setReleaseDecisionId(decision.getId())
                    .setFinalizationPayloadHash(managerPayloadHash));
            MesProductionReleaseManagerApprovalResult result = managerApprovalService.completeAfterFinalization(
                    command.getActorUserId(), approve, prepared, released);
            closeUpstreamAfterRelease(command, released, decision);
            reportManagementSummaryService.refreshByReleaseTransactionId(command.getReleaseTransactionId());
            return toResp(result.getBatchExecution(), released);
        }
        String finalizationPayloadHash = finalizationPayloadHash(command, evidence);
        MesProEdhrReleaseTransactionEventDO existingEvent =
                releaseTransactionEventMapper.selectByReleaseTransactionIdAndEventTypeAndIdempotencyKey(
                        command.getReleaseTransactionId(), EVENT_TYPE_APPROVE, command.getIdempotencyKey());
        if (existingEvent != null) {
            requireSameFinalizationPayload(existingEvent, finalizationPayloadHash);
            return get(command.getReleaseTransactionId());
        }
        requirePendingApproval(transaction);
        MesProEdhrWorkTaskDO approvalTask = workTaskService.validateReleaseApprovalTask(
                command.getWorkTaskId(), transaction.getId());
        if (approvalTask == null || approvalTask.getId() == null) {
            throw exception(PRO_EDHR_RELEASE_SIGNOFF_REQUIRED);
        }
        command.setWorkTaskId(approvalTask.getId());
        requireApprovalCenterSignoffEvidence(approvalTask, command.getActorUserId(),
                command.getSignoffEvidenceHash());
        LocalDateTime occurredAt = now();
        String opinion = StrUtil.trim(command.getApprovalOpinion());
        if (releaseTransactionMapper.approveProductionRelease(
                transaction.getId(), command.getExpectedVersion(), command.getActorUserId(),
                command.getIdempotencyKey(), command.getSignoffEvidenceHash(), opinion, occurredAt) != 1) {
            throw exception(PRO_EDHR_RELEASE_STATUS_INVALID);
        }
        transaction = releaseTransactionMapper.selectById(transaction.getId());
        MesProEdhrReleaseDecisionDO decision = recordFinalizationDecision(command, transaction,
                evidence, STATUS_RELEASED, finalizationPayloadHash, null, opinion, occurredAt);
        releaseTransactionMapper.updateById(new MesProEdhrReleaseTransactionDO()
                .setId(transaction.getId())
                .setReleaseDecisionId(decision.getId())
                .setFinalizationPayloadHash(finalizationPayloadHash));
        transaction.setReleaseDecisionId(decision.getId())
                .setFinalizationPayloadHash(finalizationPayloadHash);
        closeUpstreamAfterRelease(command, transaction, decision);
        workTaskService.completeReleaseApprovalTask(approvalTask.getId(), transaction.getId(), EVENT_TYPE_APPROVE, opinion);
        recordTransactionEvent(transaction, EVENT_TYPE_APPROVE, STATUS_PENDING_APPROVAL, STATUS_RELEASED,
                command.getActorUserId(), null, opinion, command.getIdempotencyKey(),
                command.getSignoffEvidenceHash(), occurredAt, finalizationPayloadHash);
        recordTerminalOperationAudit(batch, transaction, EVENT_TYPE_APPROVE, STATUS_PENDING_APPROVAL, STATUS_RELEASED,
                command.getActorUserId(), null, opinion, command.getIdempotencyKey(),
                command.getSignoffEvidenceHash(), occurredAt);
        return toResp(batch, transaction);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrReleaseRespVO reject(MesProEdhrReleaseRejectReqVO reqVO) {
        return finalizeRelease(new MesReleaseFinalizationCommand()
                .setAction(MesReleaseFinalizationAction.REJECT)
                .setReleaseTransactionId(reqVO.getReleaseTransactionId())
                .setIdempotencyKey(reqVO.getIdempotencyKey())
                .setDecisionReason(reqVO.getRejectReason())
                .setActorUserId(SecurityFrameworkUtils.getLoginUserId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrReleaseRespVO withdraw(MesProEdhrReleaseWithdrawReqVO reqVO) {
        return finalizeRelease(new MesReleaseFinalizationCommand()
                .setAction(MesReleaseFinalizationAction.WITHDRAW)
                .setReleaseTransactionId(reqVO.getReleaseTransactionId())
                .setIdempotencyKey(reqVO.getIdempotencyKey())
                .setDecisionReason(reqVO.getWithdrawReason())
                .setActorUserId(SecurityFrameworkUtils.getLoginUserId()));
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

    private MesProEdhrReleaseTransactionDO requireTransactionForUpdate(Long releaseTransactionId) {
        MesProEdhrReleaseTransactionDO transaction = releaseTransactionMapper.selectByIdForUpdate(releaseTransactionId);
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

    private void requireApprovalCenterSignoffEvidence(MesProEdhrWorkTaskDO approvalTask,
                                                       Long actorUserId,
                                                       String signoffEvidenceHash) {
        if (approvalTask == null || approvalTask.getId() == null || actorUserId == null) {
            throw exception(PRO_EDHR_RELEASE_SIGNOFF_REQUIRED);
        }
        List<BpmApprovalSignatureRecordDO> records = approvalSignatureRecordMapper.selectList(
                new LambdaQueryWrapperX<BpmApprovalSignatureRecordDO>()
                        .eq(BpmApprovalSignatureRecordDO::getModuleCode, MODULE_EDHR)
                        .eq(BpmApprovalSignatureRecordDO::getSourceTaskType, APPROVAL_SOURCE_TASK_TYPE_EDHR_WORK_TASK)
                        .eq(BpmApprovalSignatureRecordDO::getSourceTaskId, String.valueOf(approvalTask.getId()))
                        .eq(BpmApprovalSignatureRecordDO::getSignerUserId, actorUserId)
                        .eq(BpmApprovalSignatureRecordDO::getReviewResult, APPROVAL_REVIEW_RESULT_APPROVE));
        boolean evidenceMatched = records.stream().anyMatch(record ->
                Boolean.TRUE.equals(record.getPasswordVerified())
                        && matchesSignoffEvidenceHash(record.getSignatureImageFileUrl(), signoffEvidenceHash));
        if (!evidenceMatched) {
            throw exception(PRO_EDHR_RELEASE_SIGNOFF_REQUIRED);
        }
    }

    private boolean matchesSignoffEvidenceHash(String signatureImageFileUrl, String signoffEvidenceHash) {
        String normalizedUrl = StrUtil.trim(signatureImageFileUrl);
        return StrUtil.isNotBlank(normalizedUrl)
                && Objects.equals(DigestUtil.sha256Hex(normalizedUrl), signoffEvidenceHash);
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
                                                              MesProEdhrFourMaterialGateResult fourMaterialGate) {
        return List.of(
                buildDhrCompletenessItem(releaseTransactionId, batch, checkedAt),
                buildDossierRequirementItem(releaseTransactionId, batch, checkedAt, fourMaterialGate,
                        MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_INCOMING_INSPECTION_REPORT,
                        CHECK_DOSSIER_INCOMING_INSPECTION_REPORT, "来料检报告",
                        "来料检报告资料限制"),
                buildDossierRequirementItem(releaseTransactionId, batch, checkedAt, fourMaterialGate,
                        MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_STERILIZATION_REPORT,
                        CHECK_DOSSIER_STERILIZATION_REPORT, "灭菌报告",
                        "灭菌报告资料限制"),
                buildDossierRequirementItem(releaseTransactionId, batch, checkedAt, fourMaterialGate,
                        MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_FINISHED_PRODUCT_INSPECTION_REPORT,
                        CHECK_DOSSIER_FINISHED_PRODUCT_INSPECTION_REPORT, "成品检报告",
                        "成品检报告资料限制"),
                buildDossierRequirementItem(releaseTransactionId, batch, checkedAt, fourMaterialGate,
                        MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_FINISHED_PRODUCT_INSPECTION_RECORD,
                        CHECK_DOSSIER_FINISHED_PRODUCT_INSPECTION_RECORD, "成品检记录",
                        "成品检记录限制"),
                buildCompletenessSourceItem(releaseTransactionId, batch, checkedAt,
                        releaseCompletenessService.evaluateInspectionResult(batch)),
                buildCompletenessSourceItem(releaseTransactionId, batch, checkedAt,
                        releaseCompletenessService.evaluateDeviationClosed(batch)),
                buildCompletenessSourceItem(releaseTransactionId, batch, checkedAt,
                        releaseCompletenessService.evaluateReworkClosed(batch)),
                buildCompletenessSourceItem(releaseTransactionId, batch, checkedAt,
                        releaseCompletenessService.evaluateScrapRecorded(batch)),
                buildCompletenessSourceItem(releaseTransactionId, batch, checkedAt,
                        releaseCompletenessService.evaluateInventoryConsistency(batch)));
    }

    private MesProEdhrReleaseCheckItemDO buildCompletenessSourceItem(Long releaseTransactionId,
                                                                     MesProEdhrBatchExecutionDO batch,
                                                                     LocalDateTime checkedAt,
                                                                     MesOrderReleaseCompletenessCheck sourceCheck) {
        return buildItem(releaseTransactionId, batch, checkedAt, sourceCheck.checkCode(),
                sourceCheck.checkCategory(), sourceCheck.checkName(), sourceCheck.checkResult(),
                sourceCheck.severity(), sourceCheck.responsibilityModule(), sourceCheck.sourceObjectType(),
                sourceCheck.sourceObjectId(), sourceCheck.sourceObjectCode(), sourceCheck.failureReason(),
                sourceCheck.remediationSuggestion());
    }

    private MesProEdhrReleaseCheckItemDO buildDossierRequirementItem(Long releaseTransactionId,
                                                                     MesProEdhrBatchExecutionDO batch,
                                                                     LocalDateTime checkedAt,
                                                                     MesProEdhrFourMaterialGateResult fourMaterialGate,
                                                                     String nodeType,
                                                                     String checkCode,
                                                                     String nodeLabel,
                                                                     String checkName) {
        MesProBatchRecordExecutionAttachmentDO evidence = fourMaterialGate.materials().stream()
                .filter(item -> Objects.equals(nodeType, item.getFieldKey())
                        || Objects.equals(nodeType, item.getAttachmentGroupKey()))
                .findFirst().orElse(null);
        if (evidence != null) {
            return buildItem(releaseTransactionId, batch, checkedAt, checkCode, CATEGORY_DOSSIER, checkName,
                    CHECK_RESULT_PASS, SEVERITY_INFO, MODULE_EDHR,
                    SOURCE_OBJECT_TYPE_SPECIAL_NODE_ATTACHMENT, String.valueOf(evidence.getBatchTaskId()),
                    nodeLabel,
                    nodeLabel + "当前版本已完成并通过文件元数据与摘要校验",
                    "无需处理");
        }
        return buildItem(releaseTransactionId, batch, checkedAt, checkCode, CATEGORY_DOSSIER, checkName,
                CHECK_RESULT_BLOCKER, SEVERITY_BLOCKER, MODULE_EDHR,
                SOURCE_OBJECT_TYPE_SPECIAL_NODE_ATTACHMENT, String.valueOf(batch.getId()),
                nodeLabel, nodeLabel + "缺失或当前版本无效：" + fourMaterialGate.status(),
                "完成" + nodeLabel + "当前版本上传后重新预检");
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
        MesProEdhrWorkTaskAssignmentRuleDO releaseRule = batch.getRouteId() == null ? null
                : workTaskAssignmentRuleMapper.selectEnabledByScopeAndType(RULE_SCOPE_TYPE_ROUTE,
                batch.getRouteId(), MesProEdhrWorkTaskService.TASK_TYPE_RELEASE_APPROVE);
        if (releaseRule == null) {
            throw exception(PRO_EDHR_RELEASE_OWNER_INVALID, "放行责任人未配置");
        }
        MesProEdhrCandidateResolver.MesProEdhrCandidateContract candidate =
                candidateResolver.resolveAssignmentRule(releaseRule);
        if (isCurrentUserReleaseOwner(candidate, actorUserId)) {
            return;
        }
        throw exception(PRO_EDHR_RELEASE_OWNER_INVALID, actorUserId);
    }

    private boolean isCurrentUserReleaseOwner(MesProEdhrCandidateResolver.MesProEdhrCandidateContract candidate,
                                             Long actorUserId) {
        if (candidate == null || actorUserId == null || StrUtil.isBlank(candidate.userSnapshot())) {
            return false;
        }
        for (String item : candidate.userSnapshot().split(",")) {
            if (StrUtil.isBlank(item)) {
                continue;
            }
            if (Objects.equals(Long.valueOf(item.trim()), actorUserId)) {
                return true;
            }
        }
        return false;
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
                                              MesProEdhrFourMaterialGateResult materialGate) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("batchExecutionId", batch.getId());
        snapshot.put("batchExecutionCode", batch.getBatchExecutionCode());
        snapshot.put("workOrderCode", batch.getWorkOrderCode());
        snapshot.put("batchCode", batch.getBatchCode());
        snapshot.put("productCode", batch.getProductCode());
        snapshot.put("releaseStatus", releaseStatus);
        snapshot.put("checkedAt", checkedAt);
        snapshot.put("materialGateReceiptId", materialGate == null ? null : materialGate.receiptId());
        snapshot.put("materialGateManifestHash", materialGate == null ? null : materialGate.manifestHash());
        snapshot.put("materialGateReceiptHash", materialGate == null ? null : materialGate.receiptHash());
        snapshot.put("materialGateVersionSetHash", materialGate == null ? null : materialGate.materialVersionSetHash());
        snapshot.put("materialGateVersion", materialGate == null ? null : materialGate.version());
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

    private String extractMaterialGateManifestHash(MesProEdhrReleaseTransactionDO transaction) {
        String snapshotJson = transaction == null ? null : transaction.getPrecheckSnapshotJson();
        if (StrUtil.isBlank(snapshotJson)) {
            return null;
        }
        try {
            JSONObject snapshot = JSON.parseObject(snapshotJson);
            return snapshot == null ? null : snapshot.getString("materialGateManifestHash");
        } catch (RuntimeException ex) {
            throw exception(PRO_EDHR_RELEASE_MATERIAL_MANIFEST_STALE);
        }
    }

    private MesProEdhrFourMaterialGateResult requirePrecheckMaterialManifestCurrent(
            MesProEdhrReleaseTransactionDO transaction) {
        MesProEdhrFourMaterialGateResult current =
                fourMaterialGateService.requireMaterialsReady(transaction.getBatchExecutionId());
        String precheckManifestHash = extractMaterialGateManifestHash(transaction);
        if (StrUtil.isBlank(precheckManifestHash)
                || !Objects.equals(precheckManifestHash, current.manifestHash())) {
            throw exception(PRO_EDHR_RELEASE_MATERIAL_MANIFEST_STALE);
        }
        return current;
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

    private void recordTerminalOperationAudit(MesProEdhrBatchExecutionDO batch,
                                              MesProEdhrReleaseTransactionDO transaction,
                                              String eventType,
                                              String fromStatus,
                                              String toStatus,
                                              Long actorUserId,
                                              String reason,
                                              String opinion,
                                              String idempotencyKey,
                                              String signoffEvidenceHash,
                                              LocalDateTime occurredAt) {
        Map<String, Object> beforePayload = new LinkedHashMap<>();
        beforePayload.put("releaseTransactionId", transaction.getId());
        beforePayload.put("releaseStatus", fromStatus);
        Map<String, Object> afterPayload = new LinkedHashMap<>();
        afterPayload.put("releaseTransactionId", transaction.getId());
        afterPayload.put("releaseStatus", toStatus);
        afterPayload.put("submittedBy", transaction.getSubmittedBy());
        afterPayload.put("approvedBy", transaction.getApprovedBy());
        afterPayload.put("rejectedBy", transaction.getRejectedBy());
        afterPayload.put("withdrawnBy", transaction.getWithdrawnBy());
        afterPayload.put("signoffEvidenceHash", signoffEvidenceHash);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("requestSource", "RELEASE_TERMINAL");
        metadata.put("eventType", eventType);
        metadata.put("reason", reason);
        metadata.put("opinion", opinion);
        metadata.put("idempotencyKey", idempotencyKey);
        metadata.put("signoffEvidenceHash", signoffEvidenceHash);
        metadata.put("permissionDecision", "ALLOW");
        metadata.put("resultStatus", "SUCCESS");
        metadata.put("batchExecutionId", batch.getId());
        metadata.put("releaseTransactionId", transaction.getId());
        metadata.put("fromStatus", fromStatus);
        metadata.put("toStatus", toStatus);

        operationAuditService.record(new MesProEdhrOperationAuditCommand()
                .setRequestId(idempotencyKey)
                .setObjectType("RELEASE_TRANSACTION")
                .setObjectId(String.valueOf(transaction.getId()))
                .setBatchExecutionId(batch.getId())
                .setRouteId(batch.getRouteId())
                .setOperationType(eventType)
                .setActionName(resolveTerminalAuditActionName(eventType))
                .setActorUserId(actorUserId)
                .setActorUsername(SecurityFrameworkUtils.getLoginUserNickname())
                .setPermissionCode(resolveTerminalAuditPermissionCode(eventType))
                .setPermissionDecision("ALLOW")
                .setResultStatus("SUCCESS")
                .setBeforeSummaryHash(hashReleaseAuditPayload(beforePayload))
                .setAfterSummaryHash(hashReleaseAuditPayload(afterPayload))
                .setMetadataJson(JSON.toJSONString(metadata))
                .setOccurredAt(occurredAt));
    }

    private String resolveTerminalAuditActionName(String eventType) {
        return switch (eventType) {
            case EVENT_TYPE_SUBMIT -> "放行负责人电子签名放行";
            case EVENT_TYPE_APPROVE -> "审批中心批准放行";
            case EVENT_TYPE_REJECT -> "放行退回";
            case EVENT_TYPE_WITHDRAW -> "撤回放行审批";
            default -> "eDHR 放行终态操作";
        };
    }

    private String resolveTerminalAuditPermissionCode(String eventType) {
        return switch (eventType) {
            case EVENT_TYPE_SUBMIT -> "mes:pro-edhr-release:submit";
            case EVENT_TYPE_APPROVE -> "mes:pro-edhr-release:approve";
            case EVENT_TYPE_REJECT -> "mes:pro-edhr-release:reject";
            case EVENT_TYPE_WITHDRAW -> "mes:pro-edhr-release:withdraw";
            default -> "mes:pro-edhr-release:update";
        };
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

    private String finalizationPayloadHash(MesReleaseFinalizationCommand command) {
        return MesReleaseFlowIdempotency.payloadHash(
                String.valueOf(command.getReleaseTransactionId()),
                String.valueOf(command.getAction()),
                String.valueOf(command.getBatchExecutionId()),
                String.valueOf(command.getOrigin()),
                command.getEntryType(),
                String.valueOf(command.getActiveOrderId()),
                command.getPickListBindingId(),
                command.getCompletionEventId(),
                command.getCompletionBackfillReceiptId(),
                String.valueOf(command.getDualProgressCompleted()),
                String.valueOf(command.getThreeBackfillsSucceeded()),
                command.getSourceRelation(),
                command.getSourceSnapshotHash(),
                String.valueOf(command.getReleaseApplicationId()),
                String.valueOf(command.getActorUserId()),
                String.valueOf(command.getWorkTaskId()),
                String.valueOf(command.getExpectedVersion()),
                command.getSignoffEvidenceHash(),
                command.getApprovalOpinion(),
                command.getDecisionReason(),
                JSON.toJSONString(command.getIndependentPrerequisiteReceipt()),
                JSON.toJSONString(command.getMaterialGateReceipt()));
    }

    private String finalizationPayloadHash(
            MesReleaseFinalizationCommand command, MesReleaseFinalizationEvidence evidence) {
        MesReleaseMaterialGateReceipt gate = evidence == null ? null : evidence.getMaterialGateReceipt();
        IndependentBatchPrerequisiteReceipt independent = evidence == null
                ? null : evidence.getIndependentPrerequisiteReceipt();
        CompletionBackfillReceipt completion = evidence == null
                ? null : evidence.getCompletionBackfillReceipt();
        return MesReleaseFlowIdempotency.payloadHash(
                String.valueOf(command.getReleaseTransactionId()),
                String.valueOf(command.getAction()),
                String.valueOf(command.getBatchExecutionId()),
                String.valueOf(command.getOrigin()),
                command.getEntryType(),
                String.valueOf(command.getActiveOrderId()),
                String.valueOf(command.getPickListId()),
                command.getPickListBindingId(),
                command.getCompletionEventId(),
                command.getCompletionBackfillReceiptId(),
                String.valueOf(command.getIndependentPrerequisiteReceiptId()),
                String.valueOf(command.getMaterialGateReceiptId()),
                gate == null ? null : gate.getManifestHash(),
                gate == null ? null : gate.getSourceSnapshotHash(),
                gate == null ? null : gate.getMaterialVersionSetHash(),
                gate == null ? null : gate.getReceiptHash(),
                independent == null ? null : independent.getReceiptId(),
                independent == null ? null : independent.getPayloadHash(),
                completion == null ? null : completion.getReceiptHash(),
                completion == null ? null : completion.getCompletionEventId(),
                command.getSourceRelation(),
                command.getSourceSnapshotHash(),
                String.valueOf(command.getReleaseApplicationId()),
                String.valueOf(command.getActorUserId()),
                String.valueOf(command.getWorkTaskId()),
                String.valueOf(command.getExpectedVersion()),
                command.getSignoffEvidenceHash(),
                command.getApprovalOpinion(),
                command.getDecisionReason());
    }

    private MesProEdhrReleaseDecisionDO recordFinalizationDecision(
            MesReleaseFinalizationCommand command,
            MesProEdhrReleaseTransactionDO transaction,
            String decisionStatus,
            String payloadHash,
            String signoffEvidenceHash,
            String opinionOrReason,
            LocalDateTime decidedAt) {
        return recordFinalizationDecision(command, transaction, null, decisionStatus, payloadHash,
                signoffEvidenceHash, opinionOrReason, decidedAt);
    }

    private MesProEdhrReleaseDecisionDO recordFinalizationDecision(
            MesReleaseFinalizationCommand command,
            MesProEdhrReleaseTransactionDO transaction,
            MesReleaseFinalizationEvidence evidence,
            String decisionStatus,
            String payloadHash,
            String signoffEvidenceHash,
            String opinionOrReason,
            LocalDateTime decidedAt) {
        MesProEdhrReleaseDecisionDO existing = releaseDecisionMapper
                .selectByTransactionIdAndStatusAndIdempotencyKey(
                        transaction.getId(), decisionStatus, command.getIdempotencyKey());
        if (existing != null) {
            if (!Objects.equals(existing.getPayloadHash(), payloadHash)) {
                throw new MesReleaseFlowBlockerException("finalization decision payload conflict",
                        new MesReleaseFlowFailureRespVO()
                                .setBlockers(List.of(new MesReleaseFlowBlocker()
                                        .setBlockerType(MesReleaseFlowBlockerType.IDEMPOTENCY_PAYLOAD_CONFLICT)
                                        .setObjectType("RELEASE_DECISION")
                                        .setReason("the idempotency key was already used with another finalization payload")
                                        .setSuggestion("reuse the original finalization payload or create a new key"))));
            }
            return existing;
        }
        MesProEdhrReleaseDecisionDO existingTerminal = releaseDecisionMapper
                .selectByTransactionIdForUpdate(transaction.getId());
        if (existingTerminal != null) {
            throw new MesReleaseFlowBlockerException("release transaction already has a terminal decision",
                    new MesReleaseFlowFailureRespVO()
                            .setStage(MesReleaseFlowStage.SP_4)
                            .setBlockers(List.of(new MesReleaseFlowBlocker()
                                    .setBlockerType(MesReleaseFlowBlockerType.RELEASE_DECISION_ALREADY_FINALIZED)
                                    .setObjectType("RELEASE_TRANSACTION")
                                    .setObjectId(String.valueOf(transaction.getId()))
                                    .setReason("the release transaction already has terminal decision "
                                            + existingTerminal.getDecisionStatus())
                                    .setSuggestion("open a new release transaction for a new release attempt"))));
        }
        MesReleaseMaterialGateReceipt gate = evidence == null
                ? command.getMaterialGateReceipt() : evidence.getMaterialGateReceipt();
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("releaseTransactionId", transaction.getId());
        snapshot.put("releaseApplicationId", command.getReleaseApplicationId());
        snapshot.put("batchExecutionId", transaction.getBatchExecutionId());
        snapshot.put("workOrderId", transaction.getWorkOrderId());
        snapshot.put("activeOrderId", command.getActiveOrderId());
        snapshot.put("pickListBindingId", command.getPickListBindingId());
        snapshot.put("completionEventId", command.getCompletionEventId());
        snapshot.put("completionBackfillReceiptId", command.getCompletionBackfillReceiptId());
        snapshot.put("origin", command.getOrigin());
        snapshot.put("entryType", command.getEntryType());
        snapshot.put("sourceRelation", command.getSourceRelation());
        snapshot.put("sourceSnapshotHash", command.getSourceSnapshotHash());
        snapshot.put("materialGateReceiptId", gate == null ? null : gate.getReceiptId());
        snapshot.put("materialGateManifestHash", gate == null ? null : gate.getManifestHash());
        snapshot.put("materialGateSourceSnapshotHash", gate == null ? null : gate.getSourceSnapshotHash());
        snapshot.put("materialGateVersionSetHash", gate == null ? null : gate.getMaterialVersionSetHash());
        snapshot.put("materialGateReceiptHash", gate == null ? null : gate.getReceiptHash());
        snapshot.put("materialGateVersion", gate == null ? null : gate.getVersion());
        snapshot.put("decisionStatus", decisionStatus);
        snapshot.put("idempotencyKey", command.getIdempotencyKey());
        snapshot.put("payloadHash", payloadHash);
        snapshot.put("actorUserId", command.getActorUserId());
        snapshot.put("expectedVersion", command.getExpectedVersion());
        snapshot.put("dualProgressCompleted", command.getDualProgressCompleted());
        snapshot.put("threeBackfillsSucceeded", command.getThreeBackfillsSucceeded());
        snapshot.put("decidedAt", decidedAt);
        MesProEdhrReleaseDecisionDO decision = MesProEdhrReleaseDecisionDO.builder()
                .releaseTransactionId(transaction.getId())
                .releaseApplicationId(command.getReleaseApplicationId())
                .batchExecutionId(transaction.getBatchExecutionId())
                .workOrderId(transaction.getWorkOrderId())
                .activeOrderId(command.getActiveOrderId())
                .pickListBindingId(command.getPickListBindingId())
                .completionEventId(command.getCompletionEventId())
                .completionBackfillReceiptId(command.getCompletionBackfillReceiptId())
                .origin(command.getOrigin() == null ? null : command.getOrigin().name())
                .entryType(command.getEntryType())
                .sourceRelation(command.getSourceRelation())
                .sourceSnapshotHash(command.getSourceSnapshotHash())
                .materialGateReceiptId(gate == null ? null : gate.getReceiptId())
                .materialGateSnapshotHash(gate == null ? null : gate.getManifestHash())
                .materialGateVersion(gate == null ? null : gate.getVersion())
                .decisionStatus(decisionStatus)
                .idempotencyKey(command.getIdempotencyKey())
                .payloadHash(payloadHash)
                .actorUserId(command.getActorUserId())
                .signoffEvidenceHash(signoffEvidenceHash)
                .approvalOpinion(STATUS_RELEASED.equals(decisionStatus) ? opinionOrReason : null)
                .decisionReason(STATUS_RELEASED.equals(decisionStatus) ? null : opinionOrReason)
                .auditSnapshotJson(JSON.toJSONString(snapshot))
                .decidedAt(decidedAt)
                .version(1)
                .build();
        releaseDecisionMapper.insert(decision);
        return decision;
    }

    private void closeUpstreamAfterRelease(MesReleaseFinalizationCommand command,
                                           MesProEdhrReleaseTransactionDO transaction,
                                           MesProEdhrReleaseDecisionDO decision) {
        if (command.getOrigin() != MesReleaseOrigin.ACTIVE_ORDER) {
            return;
        }
        upstreamStatePort.closeAfterRelease(new MesReleaseUpstreamClosureCommand()
                .setReleaseDecisionId(decision.getId())
                .setActiveOrderId(command.getActiveOrderId())
                .setActiveOrderExpectedVersion(command.getActiveOrderExpectedVersion())
                .setWorkOrderId(transaction.getWorkOrderId())
                .setActorUserId(command.getActorUserId()));
    }

    private void requireSameFinalizationPayload(MesProEdhrReleaseTransactionEventDO existingEvent,
                                                  String incomingPayloadHash) {
        JSONObject snapshot = JSON.parseObject(existingEvent.getEventSnapshotJson());
        String storedPayloadHash = snapshot == null ? null : snapshot.getString("finalizationPayloadHash");
        if (!Objects.equals(storedPayloadHash, incomingPayloadHash)) {
            throw new MesReleaseFlowBlockerException(
                    "release approval idempotency key was used with a different payload",
                    new MesReleaseFlowFailureRespVO()
                            .setStage(MesReleaseFlowStage.SP_4)
                            .setBlockers(List.of(new MesReleaseFlowBlocker()
                                    .setBlockerType(MesReleaseFlowBlockerType.IDEMPOTENCY_PAYLOAD_CONFLICT)
                                    .setObjectType("RELEASE_APPROVAL")
                                    .setObjectId(String.valueOf(existingEvent.getReleaseTransactionId()))
                                    .setReason("the stored approval event payload does not match the replay payload")
                                    .setSuggestion("reuse the original source, task, version and signoff payload"))));
        }
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
        recordTransactionEvent(transaction, eventType, fromStatus, toStatus, actorUserId, reason, opinion,
                idempotencyKey, signoffEvidenceHash, occurredAt, null);
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
                                        LocalDateTime occurredAt,
                                        String finalizationPayloadHash) {
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
        if (finalizationPayloadHash != null) {
            snapshot.put("finalizationPayloadHash", finalizationPayloadHash);
        }
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
                .setReleaseDecisionId(transaction == null ? null : transaction.getReleaseDecisionId())
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
                .setWithdrawReason(transaction == null ? null : transaction.getWithdrawReason())
                .setVersion(transaction == null ? null : transaction.getVersion());
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrReleaseRespVO submitForApproval(MesProEdhrReleaseSubmitForApprovalCommand command) {
        String idempotencyKey = requireIdempotencyKey(command.getIdempotencyKey());
        MesProEdhrReleaseTransactionEventDO existingEvent =
                releaseTransactionEventMapper.selectByReleaseTransactionIdAndEventTypeAndIdempotencyKey(
                        command.getReleaseTransactionId(), EVENT_TYPE_SUBMIT, idempotencyKey);
        if (existingEvent != null) {
            return get(command.getReleaseTransactionId());
        }

        MesProEdhrReleaseTransactionDO transaction = requireTransactionForUpdate(command.getReleaseTransactionId());
        requirePrecheckPassed(transaction);
        requirePrecheckMaterialManifestCurrent(transaction);
        MesProEdhrBatchExecutionDO batch = requireBatchExecution(transaction.getBatchExecutionId());
        String fromStatus = transaction.getReleaseStatus();
        LocalDateTime occurredAt = now();
        Long actorUserId = SecurityFrameworkUtils.getLoginUserId();
        String reason = StrUtil.blankToDefault(StrUtil.trim(command.getSubmitReason()),
                "生产组长申请生成放行资料，提交负责人审批");
        MesProEdhrWorkTaskDO approvalTask = workTaskService.createReleaseApprovalTaskAfterSubmit(transaction, batch);
        if (approvalTask == null || approvalTask.getId() == null) {
            throw exception(PRO_EDHR_RELEASE_SIGNOFF_REQUIRED);
        }

        releaseTransactionMapper.updateById(new MesProEdhrReleaseTransactionDO()
                .setId(transaction.getId())
                .setReleaseStatus(STATUS_PENDING_APPROVAL)
                .setSubmitIdempotencyKey(idempotencyKey)
                .setSubmittedBy(actorUserId)
                .setSubmittedAt(occurredAt));
        transaction = releaseTransactionMapper.selectById(transaction.getId());
        recordTransactionEvent(transaction, EVENT_TYPE_SUBMIT, fromStatus, STATUS_PENDING_APPROVAL,
                actorUserId, reason, null, idempotencyKey, null, occurredAt);
        recordTerminalOperationAudit(batch, transaction, EVENT_TYPE_SUBMIT, fromStatus, STATUS_PENDING_APPROVAL,
                actorUserId, reason, null, idempotencyKey, null, occurredAt);
        return toResp(batch, transaction).setReleaseApprovalWorkTaskId(approvalTask.getId());
    }
}
