package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectCommand;
import cn.iocoder.yudao.module.signature.dal.dataobject.ElectronicSignatureRecordDO;
import cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureRecordMapper;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditCommand;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditEvidence;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditRelation;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditStateEnvelope;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrBatchExecutionRejectReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrNonconformanceReviewCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrNonconformanceReviewDisposeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrNonconformanceReviewPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrNonconformanceReviewRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrNonconformanceReviewActiveOrderRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrNonconformanceReviewCounterDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrNonconformanceReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrNonconformanceReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrNonconformanceReviewCounterMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskStatus;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskService;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionOriginDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionOriginMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowStatus;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderDetail;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderDetailService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_STATUS_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_NONCONFORMANCE_REVIEW_DISPOSITION_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_NONCONFORMANCE_REVIEW_FROZEN_ACTION_LOCKED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_NONCONFORMANCE_REVIEW_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_NONCONFORMANCE_REVIEW_PENDING_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_NONCONFORMANCE_REVIEW_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_NONCONFORMANCE_REVIEW_SOURCE_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_NONCONFORMANCE_REVIEW_WORK_ORDER_STATE_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrNonconformanceReviewService.DISPOSITION_CONCESSION_RELEASE;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrNonconformanceReviewService.DISPOSITION_REWORK;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrNonconformanceReviewService.DISPOSITION_VOID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrNonconformanceReviewService.SOURCE_TYPE_PQC_RELEASE;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrNonconformanceReviewService.SOURCE_TYPE_PQC_SUBMISSION;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrNonconformanceReviewService.SOURCE_TYPE_ACTIVE_ORDER;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrNonconformanceReviewService.STATUS_CLOSED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrNonconformanceReviewService.STATUS_PENDING_REVIEW;

@Service
@Validated
public class MesProEdhrNonconformanceReviewServiceImpl implements MesProEdhrNonconformanceReviewService {

    private static final Set<String> SUPPORTED_SOURCE_TYPES = Set.of(SOURCE_TYPE_PQC_SUBMISSION, SOURCE_TYPE_PQC_RELEASE);
    private static final Set<String> SUPPORTED_DISPOSITIONS =
            Set.of(DISPOSITION_CONCESSION_RELEASE, DISPOSITION_REWORK, DISPOSITION_VOID);
    private static final DateTimeFormatter REVIEW_CODE_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    private static final long REVIEW_CODE_MAX_SERIAL = 99_999_999L;
    private static final String ADMIN_FILE_ACCESS_PREFIX = "/admin-api/infra/file/";
    private static final String ADMIN_FILE_ACCESS_GET_SEGMENT = "/get/";

    @Resource
    private MesProEdhrNonconformanceReviewMapper reviewMapper;
    @Resource
    private MesProEdhrReleaseTransactionMapper releaseTransactionMapper;
    @Resource
    private MesProEdhrWorkTaskService workTaskService;
    @Resource
    private MesProEdhrNonconformanceReviewCounterMapper reviewCounterMapper;
    @Resource
    private MesProEdhrBatchExecutionMapper batchExecutionMapper;
    @Resource
    private MesProcessPoolActiveOrderReleaseApplicationMapper releaseApplicationMapper;
    @Resource
    private MesProcessPoolActiveOrderMapper activeOrderMapper;
    @Resource
    private cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesActiveOrderReworkCycleService reworkCycleService;
    @Resource
    private MesProProcessPoolEventMapper processPoolEventMapper;
    @Resource
    private MesProEdhrWorkTaskMapper workTaskMapper;
    @Resource
    private MesProWorkOrderMapper workOrderMapper;
    @Resource
    private MesProBatchRecordExecutionSignatureService signatureService;
    @Resource
    private MesProEdhrBatchExecutionOriginMapper batchExecutionOriginMapper;
    @Resource
    private MesPqcInspectionTaskMapper pqcInspectionTaskMapper;
    @Resource
    private MesProEdhrOperationAuditService operationAuditService;
    @Resource
    private MesTeamLeaderActiveOrderDetailService activeOrderDetailService;
    @Resource
    private FileMapper fileMapper;
    @Resource
    private GxpAuditService unifiedAudit;
    @Resource
    private cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditEventMapper auditReceiptMapper;
    @Resource
    private ElectronicSignatureRecordMapper signatureRecordMapper;
    @Resource
    private cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService signatureQueryService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrNonconformanceReviewRespVO create(MesProEdhrNonconformanceReviewCreateReqVO reqVO) {
        String reason = requireText(reqVO.getNonconformanceReason());
        if (reqVO.getActiveOrderId() != null) {
            return createFromActiveOrder(reqVO, reason);
        }
        String sourceType = requireSourceType(reqVO.getSourceType());
        String signaturePassword = requireText(reqVO.getSignaturePassword());
        unifiedAudit.acquireLedgerLock();
        MesProEdhrBatchExecutionDO batch = null;
        MesProcessPoolActiveOrderReleaseApplicationDO application = null;
        MesProProcessPoolEventDO pqcSubmissionEvent = null;
        if (SOURCE_TYPE_PQC_RELEASE.equals(sourceType) && reqVO.getSourceId() != null) {
            application = requirePqcReleaseApplicationForUpdate(reqVO.getSourceId());
            validatePqcReleaseReviewBatchExecutionId(application, reqVO.getBatchExecutionId());
            if (reviewMapper.selectLatestBySource(sourceType, application.getId()) != null) {
                throw exception(PRO_EDHR_NONCONFORMANCE_REVIEW_PENDING_EXISTS);
            }
        } else if (SOURCE_TYPE_PQC_SUBMISSION.equals(sourceType) && reqVO.getSourceId() != null) {
            pqcSubmissionEvent = requirePqcSubmissionEventForUpdate(reqVO.getSourceId());
            if (reviewMapper.selectLatestBySource(sourceType, pqcSubmissionEvent.getId()) != null) {
                throw exception(PRO_EDHR_NONCONFORMANCE_REVIEW_PENDING_EXISTS);
            }
        } else {
            batch = requireBatchExecution(reqVO.getBatchExecutionId());
            if (reviewMapper.selectPendingByBatchExecutionId(batch.getId()) != null) {
                throw exception(PRO_EDHR_NONCONFORMANCE_REVIEW_PENDING_EXISTS);
            }
            if (Objects.equals(batch.getStatus(), MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_VOIDED)
                    || Objects.equals(batch.getStatus(), MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_ARCHIVED)
                    || Objects.equals(batch.getStatus(), MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_REJECTED)
                    || Objects.equals(batch.getStatus(), MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CLOSED)
                    || Objects.equals(batch.getStatus(), MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_FROZEN)) {
                throw exception(PRO_EDHR_BATCH_EXECUTION_STATUS_INVALID);
            }
        }
        Long workOrderId = batch != null ? batch.getWorkOrderId()
                : application != null ? application.getWorkOrderId() : pqcSubmissionEvent.getWorkOrderId();
        MesProWorkOrderDO workOrder = lockWorkOrder(workOrderId);
        Long activeOrderId = requireActiveOrderId(resolveActiveOrderId(batch, application, pqcSubmissionEvent));
        LocalDateTime now = now();
        Boolean previousWorkOrderTemporaryFrozen = captureWorkOrderExternalFreezeAtReviewStart(workOrder, now);
        MesProEdhrNonconformanceReviewDO review = MesProEdhrNonconformanceReviewDO.builder()
                .reviewCode(buildReviewCode(now))
                .sourceType(sourceType)
                .sourceId(reqVO.getSourceId())
                .activeOrderId(activeOrderId)
                .batchExecutionId(batch == null ? null : batch.getId())
                .batchExecutionCode(batch == null ? null : batch.getBatchExecutionCode())
                .workOrderId(workOrderId)
                .workOrderCode(batch != null ? batch.getWorkOrderCode()
                        : application != null ? application.getWorkOrderCode() : workOrder.getCode())
                .batchCode(batch != null ? batch.getBatchCode()
                        : application != null ? application.getBatchCode() : workOrder.getBatchCode())
                .previousBatchStatus(batch == null ? null : batch.getStatus())
                .previousWorkOrderTemporaryFrozen(previousWorkOrderTemporaryFrozen)
                .reviewStatus(STATUS_PENDING_REVIEW)
                .nonconformanceReason(reason)
                .frozenAt(now)
                .remark(StrUtil.trim(reqVO.getRemark()))
                .build();
        reviewMapper.insert(review);
        Long actorId = SecurityFrameworkUtils.getLoginUserId();
        String createAggregateHash = buildCreateAggregateHash(review, activeOrderId, actorId);
        Long signatureId = signatureService.recordNonconformanceReviewCreateSignature(
                actorId, review.getId(), signaturePassword, reason, createAggregateHash);
        if (batch != null) {
            batchExecutionMapper.updateById(new MesProEdhrBatchExecutionDO()
                    .setId(batch.getId())
                    .setStatus(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_FROZEN));
        }
        if (workOrder != null) {
            requireWorkOrderUpdate(workOrder.getId(), true);
        }
        recordReviewOperation("NONCONFORMANCE_REVIEW_CREATE", "创建不合格评审", review, activeOrderId,
                null, now, signatureId, null, null, null, null, "电子签名#" + signatureId, null);
        appendCreationAudit(review, signatureId, false, createAggregateHash);
        return toResp(review);
    }

    private MesProEdhrNonconformanceReviewRespVO createFromActiveOrder(
            MesProEdhrNonconformanceReviewCreateReqVO reqVO, String reason) {
        MesProcessPoolActiveOrderDO activeOrder = activeOrderMapper.selectByIdForUpdate(reqVO.getActiveOrderId());
        if (activeOrder == null || !"ACTIVE".equals(activeOrder.getActiveStatus())
                || activeOrder.getWorkOrderId() == null) {
            throw exception(PRO_EDHR_NONCONFORMANCE_REVIEW_SOURCE_INVALID);
        }
        MesProEdhrNonconformanceReviewDO blockingReview = reviewMapper
                .selectFirstBlockingByWorkOrderId(activeOrder.getWorkOrderId());
        if (blockingReview != null) {
            throw exception(PRO_EDHR_NONCONFORMANCE_REVIEW_PENDING_EXISTS);
        }
        MesProEdhrBatchExecutionDO batch = resolveUniqueActiveOrderBatchExecution(activeOrder);
        if (batch != null) {
            validateBatchCanStartReview(batch);
        }
        MesProWorkOrderDO workOrder = lockWorkOrder(activeOrder.getWorkOrderId());
        LocalDateTime now = now();
        Boolean previousWorkOrderTemporaryFrozen = captureWorkOrderExternalFreezeAtReviewStart(workOrder, now);
        MesProEdhrNonconformanceReviewDO review = MesProEdhrNonconformanceReviewDO.builder()
                .reviewCode(buildReviewCode(now))
                .sourceType(SOURCE_TYPE_ACTIVE_ORDER)
                .sourceId(activeOrder.getId())
                .activeOrderId(activeOrder.getId())
                .batchExecutionId(batch == null ? null : batch.getId())
                .batchExecutionCode(batch == null ? null : batch.getBatchExecutionCode())
                .workOrderId(workOrder.getId())
                .workOrderCode(batch == null ? workOrder.getCode() : batch.getWorkOrderCode())
                .batchCode(batch == null ? workOrder.getBatchCode() : batch.getBatchCode())
                .previousBatchStatus(batch == null ? null : batch.getStatus())
                .previousWorkOrderTemporaryFrozen(previousWorkOrderTemporaryFrozen)
                .reviewStatus(STATUS_PENDING_REVIEW)
                .nonconformanceReason(reason)
                .frozenAt(now)
                .remark(StrUtil.trim(reqVO.getRemark()))
                .build();
        reviewMapper.insert(review);
        if (batch != null) {
            batchExecutionMapper.updateById(new MesProEdhrBatchExecutionDO()
                    .setId(batch.getId())
                    .setStatus(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_FROZEN));
        }
        requireWorkOrderUpdate(workOrder.getId(), true);
        recordReviewOperation("NONCONFORMANCE_REVIEW_CREATE", "创建不合格评审", review, activeOrder.getId(),
                null, now, null, null, null, null, null, null, null);
        return toResp(review);
    }

    private MesProEdhrBatchExecutionDO resolveUniqueActiveOrderBatchExecution(
            MesProcessPoolActiveOrderDO activeOrder) {
        List<Long> batchExecutionIds = batchExecutionOriginMapper
                .selectListByTraceFilter(activeOrder.getId(), activeOrder.getWorkOrderId(), null, null)
                .stream()
                .filter(origin -> MesProEdhrBatchTraceFormalSourceResolver
                        .isActiveOrderEntryType(origin.getEntryType()))
                .map(MesProEdhrBatchExecutionOriginDO::getBatchExecutionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (batchExecutionIds.isEmpty() && "ACTIVE".equals(activeOrder.getBusinessStatus())) {
            return null;
        }
        if (batchExecutionIds.size() != 1) {
            throw exception(PRO_EDHR_NONCONFORMANCE_REVIEW_SOURCE_INVALID);
        }
        MesProEdhrBatchExecutionDO batch = batchExecutionMapper.selectByIdForUpdate(batchExecutionIds.get(0));
        if (batch == null || !Objects.equals(batch.getWorkOrderId(), activeOrder.getWorkOrderId())) {
            throw exception(PRO_EDHR_NONCONFORMANCE_REVIEW_SOURCE_INVALID);
        }
        return batch;
    }

    @Override
    public List<MesProEdhrNonconformanceReviewActiveOrderRespVO> listActiveOrderCandidates() {
        List<MesProcessPoolActiveOrderDO> activeOrders = activeOrderMapper.selectActiveList();
        if (activeOrders.isEmpty()) {
            return List.of();
        }
        List<Long> workOrderIds = activeOrders.stream()
                .map(MesProcessPoolActiveOrderDO::getWorkOrderId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, MesProWorkOrderDO> workOrders = workOrderMapper.selectBatchIds(workOrderIds).stream()
                .collect(java.util.stream.Collectors.toMap(MesProWorkOrderDO::getId,
                        java.util.function.Function.identity(), (left, right) -> left));
        return activeOrders.stream().map(activeOrder -> {
            MesProWorkOrderDO workOrder = workOrders.get(activeOrder.getWorkOrderId());
            return new MesProEdhrNonconformanceReviewActiveOrderRespVO()
                    .setId(activeOrder.getId())
                    .setWorkOrderId(activeOrder.getWorkOrderId())
                    .setWorkOrderCode(workOrder == null ? null : workOrder.getCode())
                    .setBatchCode(workOrder == null ? null : workOrder.getBatchCode())
                    .setActiveStatus(activeOrder.getActiveStatus())
                    .setBusinessStatus(activeOrder.getBusinessStatus());
        }).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrNonconformanceReviewRespVO rejectBatch(MesProEdhrBatchExecutionRejectReqVO reqVO) {
        String reason = requireText(reqVO.getNonconformanceReason());
        String signaturePassword = requireText(reqVO.getSignaturePassword());
        unifiedAudit.acquireLedgerLock();
        MesProEdhrBatchExecutionDO batch = requireBatchExecutionForUpdate(reqVO.getBatchExecutionId());
        validateBatchCanStartReview(batch);
        Long releaseOwnerUserId = SecurityFrameworkUtils.getLoginUserId();
        String aggregateHash = buildReleaseOwnerRejectAggregateHash(batch, reason, releaseOwnerUserId);
        Long signatureId = signatureService.recordBatchActionSignature(
                releaseOwnerUserId, batch.getId(), signaturePassword, reason,
                MesProBatchRecordExecutionSignatureService.ACTION_NONCONFORMANCE_REJECT,
                "eDHR不合格评审发起", aggregateHash);
        MesProEdhrNonconformanceReviewDO review = createBatchReview(
                batch, SOURCE_TYPE_PQC_RELEASE, null, reason, "上市放行负责人电子签名#" + signatureId,
                signatureId);
        appendCreationAudit(review, signatureId, true, aggregateHash);
        return toResp(review);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrNonconformanceReviewRespVO dispose(MesProEdhrNonconformanceReviewDisposeReqVO reqVO) {
        String disposition = requireDisposition(reqVO.getDisposition());
        ResolvedReviewMaterials reviewMaterials = resolveReviewMaterials(reqVO.getReviewMaterials(),
                reqVO.getReviewMaterialEvents());
        String reviewMaterialUrl = reviewMaterials.summaryUrl();
        Long reviewMaterialFileId = reviewMaterials.primaryFileId();
        String reviewOpinion = requireText(reqVO.getReviewOpinion());
        String signaturePassword = requireText(reqVO.getSignaturePassword());
        unifiedAudit.acquireLedgerLock();
        MesProEdhrNonconformanceReviewDO review = reviewMapper.selectByIdForUpdate(reqVO.getId());
        if (review == null) {
            throw exception(PRO_EDHR_NONCONFORMANCE_REVIEW_NOT_EXISTS);
        }
        if (STATUS_CLOSED.equals(review.getReviewStatus()) && DISPOSITION_REWORK.equals(disposition)) {
            verifyReworkReplay(review, disposition, reviewMaterials, reviewOpinion);
            return toResp(review);
        }
        if (!STATUS_PENDING_REVIEW.equals(review.getReviewStatus())) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_STATUS_INVALID);
        }
        MesProEdhrBatchExecutionDO batch = review.getBatchExecutionId() == null
                ? null : requireBatchExecution(review.getBatchExecutionId());
        validateBatchCanDisposeReview(batch, review);
        MesProcessPoolActiveOrderReleaseApplicationDO sourceApplication = review.getActiveOrderId() == null
                && SOURCE_TYPE_PQC_RELEASE.equals(review.getSourceType()) && review.getSourceId() != null
                ? releaseApplicationMapper.selectById(review.getSourceId()) : null;
        Long activeOrderId = requireActiveOrderId(review.getActiveOrderId() != null
                ? review.getActiveOrderId() : resolveActiveOrderId(review, batch, sourceApplication));
        // Completion, correction and rework take the active order lock before the work order lock.
        if (DISPOSITION_REWORK.equals(disposition)) {
            MesProcessPoolActiveOrderDO activeOrder = activeOrderMapper.selectByIdForUpdate(activeOrderId);
            if (activeOrder == null || !Objects.equals(activeOrder.getWorkOrderId(), review.getWorkOrderId())) {
                throw exception(PRO_EDHR_NONCONFORMANCE_REVIEW_SOURCE_INVALID);
            }
        }
        MesProWorkOrderDO workOrder = lockWorkOrder(review.getWorkOrderId());
        MesProcessPoolActiveOrderReleaseApplicationDO application = resolvePqcReleaseApplicationForReview(review);
        if (application != null && !Objects.equals(activeOrderId, application.getActiveOrderId())) {
            throw exception(PRO_EDHR_NONCONFORMANCE_REVIEW_SOURCE_INVALID);
        }
        if (review.getPreviousWorkOrderTemporaryFrozen() == null) {
            throw exception(PRO_EDHR_NONCONFORMANCE_REVIEW_WORK_ORDER_STATE_REQUIRED);
        }
        LocalDateTime now = now();
        Integer nextBatchStatus = batch == null ? null : DISPOSITION_VOID.equals(disposition)
                ? MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_VOIDED
                : DISPOSITION_REWORK.equals(disposition)
                ? MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_REJECTED : review.getPreviousBatchStatus();
        Long qaUserId = SecurityFrameworkUtils.getLoginUserId();
        GxpAuditStateEnvelope auditBefore = dispositionAuditState(review, batch == null ? null : batch.getStatus(),
                workOrder == null ? null : workOrder.getTemporaryFrozen(), application, null, null);
        String qaDispositionAggregateHash = buildQaDispositionAggregateHash(review, disposition, reviewMaterialUrl,
                reviewMaterialFileId, reviewMaterials.materialsJson(), reviewOpinion, qaUserId);
        Long qaDispositionSignatureId = recordQaDispositionSignature(qaUserId, review.getId(),
                signaturePassword, reviewOpinion, qaDispositionAggregateHash);
        ElectronicSignatureRecordDO auditSignature = requireDispositionAuditSignature(
                qaDispositionSignatureId, qaUserId, review.getId(), reviewOpinion, qaDispositionAggregateHash);
        String qaSignature = "电子签名#" + qaDispositionSignatureId;
        String qaSignatureSnapshotJson = buildQaSignatureSnapshotJson(review, qaUserId, qaDispositionSignatureId,
                disposition, now, qaDispositionAggregateHash);
        MesProEdhrNonconformanceReviewDO update = new MesProEdhrNonconformanceReviewDO()
                .setId(review.getId())
                .setActiveOrderId(activeOrderId)
                .setReviewStatus(STATUS_CLOSED)
                .setDisposition(disposition)
                .setReviewMaterialUrl(reviewMaterialUrl)
                .setReviewMaterialFileId(reviewMaterialFileId)
                .setReviewMaterialsJson(reviewMaterials.materialsJson())
                .setReviewOpinion(reviewOpinion)
                .setQaSignature(qaSignature)
                .setQaUserId(qaUserId)
                .setClosedAt(now)
                .setUnfrozenAt(DISPOSITION_VOID.equals(disposition) ? null : now)
                .setVoidedAt(DISPOSITION_VOID.equals(disposition) ? now : null);
        update.setTraceSnapshotJson(buildTraceSnapshotJson(review, update, nextBatchStatus, qaSignatureSnapshotJson));
        if (reviewMapper.updateById(update) != 1) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_STATUS_INVALID);
        }
        if (batch != null) {
            if (batchExecutionMapper.updateById(new MesProEdhrBatchExecutionDO()
                    .setId(batch.getId())
                    .setStatus(nextBatchStatus)) != 1) {
                throw exception(PRO_EDHR_BATCH_EXECUTION_STATUS_INVALID);
            }
        }
        Boolean resultingFreeze = workOrder == null ? null
                : recomputeWorkOrderTemporaryFreeze(workOrder, review, disposition, now);
        if (workOrder != null) {
            requireWorkOrderUpdate(workOrder.getId(), resultingFreeze);
        }
        if (application != null && (DISPOSITION_REWORK.equals(disposition) || DISPOSITION_VOID.equals(disposition))) {
            MesProEdhrWorkTaskDO task = requirePqcTaskForUpdate(application);
            String decision = "NONCONFORMANCE_" + disposition.toUpperCase();
            int applicationUpdated = releaseApplicationMapper.closeFromNonconformance(
                    application.getId(), application.getVersion(), decision,
                    qaUserId, now, reviewOpinion,
                    buildNonconformanceDecisionReceipt(application, task, decision, reviewOpinion, qaUserId, now));
            if (applicationUpdated != 1) {
                throw exception(PRO_EDHR_BATCH_EXECUTION_STATUS_INVALID);
            }
            if (!MesProEdhrWorkTaskStatus.DONE.equals(task.getStatus())
                    && workTaskMapper.completePqcDecisionTask(task.getId(), now, decision) != 1) {
                throw exception(PRO_EDHR_BATCH_EXECUTION_STATUS_INVALID);
            }
            closeManagerReleaseForNonconformance(application, decision, qaUserId, now);
        }
        Long reworkActiveOrderId = null;
        if (DISPOSITION_REWORK.equals(disposition)) {
            reworkActiveOrderId = reworkCycleService.start(activeOrderId, review.getWorkOrderId(), review.getId(), now);
        }
        recordReviewOperation("NONCONFORMANCE_REVIEW_DISPOSE", disposeActionName(disposition), review, activeOrderId,
                disposition, now, qaDispositionSignatureId, reviewMaterialUrl, reviewMaterialFileId,
                reviewMaterials.materialsJson(), reviewOpinion, qaSignature, qaUserId);
        // Build after-state from the checked writes, not from a potentially stale ordinary read.
        MesProEdhrNonconformanceReviewDO auditAfterReview = BeanUtils.toBean(review, MesProEdhrNonconformanceReviewDO.class);
        auditAfterReview.setActiveOrderId(activeOrderId).setReviewStatus(update.getReviewStatus())
                .setDisposition(update.getDisposition()).setReviewOpinion(update.getReviewOpinion())
                .setReviewMaterialsJson(update.getReviewMaterialsJson());
        appendDispositionAudit(auditBefore, dispositionAuditState(auditAfterReview, nextBatchStatus,
                resultingFreeze, application, reworkActiveOrderId, auditSignature),
                auditAfterReview, application, reworkActiveOrderId, auditSignature);
        return toResp(reviewMapper.selectById(review.getId()));
    }

    private ElectronicSignatureRecordDO requireDispositionAuditSignature(
            Long signatureId, Long actorId, Long reviewId, String reason, String aggregateHash) {
        String action = MesProBatchRecordExecutionSignatureService.ACTION_QA_DISPOSITION;
        String subject = MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(0L, action,
                null, null, null, null, null, null, null, "EDHR_NONCONFORMANCE_REVIEW", reviewId,
                "eDHR不合格评审处置", action, null, null, aggregateHash, null);
        ElectronicSignatureRecordDO record = signatureId == null ? null : signatureRecordMapper.selectById(signatureId);
        var expected = new MesBatchRecordSignatureSubjectAdapter().loadAndAuthorize(new SignatureSubjectCommand(
                actorId, "MES", action, "MES_BATCH_RECORD", subject,
                MesBatchRecordSignatureSubjectAdapter.subjectVersion(subject), reason));
        if (actorId == null || record == null || !Objects.equals(signatureId, record.getId())
                || !Objects.equals(TenantContextHolder.getRequiredTenantId(), record.getTenantId())
                || !Objects.equals(actorId, record.getActorId()) || !"MES".equals(record.getModuleCode())
                || !action.equals(record.getActionCode()) || !"MES_BATCH_RECORD".equals(record.getSubjectType())
                || !subject.equals(record.getSubjectId()) || !Objects.equals(expected.subjectVersion(), record.getSubjectVersion())
                || !Objects.equals(reason, record.getReason()) || !"VALID".equals(record.getVerificationStatus())
                || StrUtil.isBlank(record.getCanonicalContentJson()) || StrUtil.isBlank(record.getContentHash())
                || !JsonUtils.parseTree(expected.canonicalContentJson()).equals(JsonUtils.parseTree(record.getCanonicalContentJson()))) {
            throw exception(PRO_EDHR_NONCONFORMANCE_REVIEW_SOURCE_INVALID);
        }
        // Native JSON storage may change formatting; use the signature kernel's canonical verification protocol.
        var verification = signatureQueryService.verifyEvidence(signatureId);
        if (verification == null || !Objects.equals(signatureId, verification.signatureId())
                || !"VALID".equals(verification.verificationStatus())
                || !Objects.equals(record.getContentHash(), verification.storedContentHash())
                || !Objects.equals(record.getContentHash(), verification.calculatedContentHash())
                || StrUtil.isBlank(record.getEvidenceHash())
                || !Objects.equals(record.getEvidenceHash(), verification.storedEvidenceHash())
                || !Objects.equals(record.getEvidenceHash(), verification.calculatedEvidenceHash())) {
            throw exception(PRO_EDHR_NONCONFORMANCE_REVIEW_SOURCE_INVALID);
        }
        return record;
    }

    private GxpAuditStateEnvelope dispositionAuditState(MesProEdhrNonconformanceReviewDO review,
            Integer batchStatus, Boolean temporaryFrozen,
            MesProcessPoolActiveOrderReleaseApplicationDO application, Long reworkActiveOrderId,
            ElectronicSignatureRecordDO signature) {
        Map<String, Object> data = new TreeMap<>();
        data.put("reviewId", auditId(review.getId()));
        data.put("sourceType", review.getSourceType());
        data.put("sourceId", auditId(review.getSourceId()));
        data.put("activeOrderId", auditId(review.getActiveOrderId()));
        data.put("batchExecutionId", auditId(review.getBatchExecutionId()));
        data.put("batchStatus", batchStatus == null ? null : batchStatus.toString());
        data.put("workOrderId", auditId(review.getWorkOrderId()));
        data.put("previousWorkOrderTemporaryFrozen", review.getPreviousWorkOrderTemporaryFrozen());
        data.put("temporaryFrozen", temporaryFrozen);
        data.put("reviewStatus", review.getReviewStatus());
        data.put("disposition", review.getDisposition());
        data.put("nonconformanceReason", review.getNonconformanceReason());
        data.put("reviewOpinion", review.getReviewOpinion());
        List<Map<String, Object>> materials = new ArrayList<>();
        if (StrUtil.isNotBlank(review.getReviewMaterialsJson())) {
            var source = JSON.parseObject(review.getReviewMaterialsJson()).getJSONArray("activeMaterials");
            if (source == null) {
                throw exception(PRO_EDHR_NONCONFORMANCE_REVIEW_SOURCE_INVALID);
            }
            for (int i = 0; i < source.size(); i++) {
                JSONObject material = source.getJSONObject(i);
                Map<String, Object> item = new TreeMap<>();
                item.put("fileId", material.getString("fileId"));
                item.put("configId", material.getString("configId"));
                item.put("fileName", material.getString("fileName"));
                item.put("path", material.getString("path"));
                // infra_file has no persisted content digest. Do not label metadata hashes as file hashes.
                item.put("sha256", null);
                item.put("hashAvailability", "NOT_RECORDED_BY_FILE_SOURCE");
                materials.add(item);
            }
        }
        data.put("materials", materials);
        data.put("signatureRecordId", signature == null ? null : auditId(signature.getId()));
        data.put("signatureContentHash", signature == null ? null : signature.getContentHash());
        data.put("applicationId", application == null ? null : auditId(application.getId()));
        data.put("reworkActiveOrderId", auditId(reworkActiveOrderId));
        // Tree nodes preserve explicit nulls despite the application's NON_NULL mapper configuration.
        String canonical = JsonUtils.toJsonString(JsonUtils.parseTree(JSON.toJSONString(data,
                com.alibaba.fastjson.serializer.SerializerFeature.WriteMapNullValue)));
        return GxpAuditStateEnvelope.builder().state("PRESENT")
                .objectVersion("sha256:" + DigestUtil.sha256Hex(canonical)).canonicalJson(canonical).build();
    }

    private void appendDispositionAudit(GxpAuditStateEnvelope before, GxpAuditStateEnvelope after,
            MesProEdhrNonconformanceReviewDO review, MesProcessPoolActiveOrderReleaseApplicationDO application,
            Long reworkActiveOrderId, ElectronicSignatureRecordDO signature) {
        String operation = "mes.nonconformance." + (DISPOSITION_CONCESSION_RELEASE.equals(review.getDisposition())
                ? "concession" : review.getDisposition());
        Map<String, Object> identity = new TreeMap<>();
        identity.put("tenantId", TenantContextHolder.getRequiredTenantId().toString());
        identity.put("operationId", operation);
        identity.put("identity", List.of(auditId(review.getId()), review.getDisposition()));
        List<GxpAuditRelation> links = new ArrayList<>();
        links.add(new GxpAuditRelation("SUBJECT", "NONCONFORMANCE_REVIEW", auditId(review.getId()), null, null));
        links.add(new GxpAuditRelation("AFFECTED", "ACTIVE_ORDER", auditId(review.getActiveOrderId()), null, null));
        links.add(new GxpAuditRelation("AFFECTED", "WORK_ORDER", auditId(review.getWorkOrderId()), null, null));
        if (review.getBatchExecutionId() != null) {
            links.add(new GxpAuditRelation("AFFECTED", "BATCH_EXECUTION", auditId(review.getBatchExecutionId()), null, null));
        }
        if (application != null) {
            links.add(new GxpAuditRelation("AFFECTED", "RELEASE_APPLICATION", auditId(application.getId()), null, null));
            if (!DISPOSITION_CONCESSION_RELEASE.equals(review.getDisposition())) {
                addDispositionRelation(links, "WORK_TASK", application.getPqcReleaseWorkTaskId());
                addDispositionRelation(links, "WORK_TASK", application.getReleaseApprovalWorkTaskId());
                addDispositionRelation(links, "RELEASE_TRANSACTION", application.getReleaseTransactionId());
            }
        }
        if (reworkActiveOrderId != null) {
            links.add(new GxpAuditRelation("REWORK_CYCLE", "ACTIVE_ORDER", auditId(reworkActiveOrderId), null, null));
        }
        links.add(new GxpAuditRelation("SIGNATURE", "ELECTRONIC_SIGNATURE", auditId(signature.getId()),
                signature.getSubjectVersion(), signature.getContentHash()));
        unifiedAudit.append(GxpAuditCommand.builder().eventSchemaVersion(2).operationId(operation)
                .subjectId(auditId(review.getId())).subjectVersion(after.getObjectVersion())
                .reason(review.getReviewOpinion()).reasonSource("USER").resultStatus("SUCCESS")
                .beforeState(before).afterState(after)
                .idempotencyKey("GXP2:" + DigestUtil.sha256Hex(JsonUtils.toJsonString(identity)))
                .sourceType("SERVICE_METHOD").sourceLocator("MesProEdhrNonconformanceReviewServiceImpl#dispose")
                .signatureRecordId(auditId(signature.getId())).signatureContentHash(signature.getContentHash())
                .links(links).evidences(List.of(new GxpAuditEvidence("SIGNATURE", auditId(signature.getId()),
                        signature.getSubjectVersion(), signature.getContentHash(), "QA_DISPOSITION"))).build());
    }

    private void appendCreationAudit(MesProEdhrNonconformanceReviewDO review, Long signatureId,
                                     boolean batchRejection, String aggregateHash) {
        String action = batchRejection ? "NONCONFORMANCE_REJECT" : "NONCONFORMANCE_REVIEW_CREATE";
        String subject = MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(
                batchRejection ? review.getBatchExecutionId() : 0L, action,
                null, null, null, null, null, null, null,
                batchRejection ? "EDHR_BATCH" : "EDHR_NONCONFORMANCE_REVIEW",
                batchRejection ? review.getBatchExecutionId() : review.getId(),
                batchRejection ? "eDHR不合格评审发起" : "eDHR不合格评审创建", action,
                null, null, aggregateHash, null);
        ElectronicSignatureRecordDO signature = signatureId == null ? null : signatureRecordMapper.selectById(signatureId);
        if (signature == null || !Objects.equals(signature.getId(), signatureId)
                || !Objects.equals(signature.getTenantId(), TenantContextHolder.getRequiredTenantId())
                || !Objects.equals(signature.getActorId(), SecurityFrameworkUtils.getLoginUserId())
                || !Objects.equals(signature.getModuleCode(), "MES")
                || !Objects.equals(signature.getActionCode(), action)
                || !Objects.equals(signature.getSubjectType(), "MES_BATCH_RECORD")
                || !Objects.equals(signature.getSubjectId(), subject)
                || !Objects.equals(signature.getSubjectVersion(), MesBatchRecordSignatureSubjectAdapter.subjectVersion(subject))
                || !Objects.equals(signature.getReason(), review.getNonconformanceReason())
                || !Objects.equals(signature.getVerificationStatus(), "VALID")
                || StrUtil.isBlank(signature.getContentHash())) {
            throw exception(PRO_EDHR_NONCONFORMANCE_REVIEW_SOURCE_INVALID);
        }
        var expected = new MesBatchRecordSignatureSubjectAdapter().loadAndAuthorize(
                new cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectCommand(
                        signature.getActorId(), "MES", action, "MES_BATCH_RECORD", subject,
                        signature.getSubjectVersion(), review.getNonconformanceReason()));
        if (!Objects.equals(JsonUtils.parseTree(signature.getCanonicalContentJson()),
                JsonUtils.parseTree(expected.canonicalContentJson()))) {
            throw exception(PRO_EDHR_NONCONFORMANCE_REVIEW_SOURCE_INVALID);
        }
        var verification = signatureQueryService.verifyEvidence(signatureId);
        if (verification == null || !Objects.equals(verification.signatureId(), signatureId)
                || !"VALID".equals(verification.verificationStatus())
                || !Objects.equals(signature.getContentHash(), verification.storedContentHash())
                || !Objects.equals(signature.getContentHash(), verification.calculatedContentHash())
                || !Objects.equals(verification.storedEvidenceHash(), verification.calculatedEvidenceHash())) {
            throw exception(PRO_EDHR_NONCONFORMANCE_REVIEW_SOURCE_INVALID);
        }
        GxpAuditStateEnvelope after = dispositionAuditState(review,
                review.getBatchExecutionId() == null ? null : MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_FROZEN,
                true, null, null, signature);
        Map<String, Object> identity = new TreeMap<>();
        identity.put("tenantId", TenantContextHolder.getRequiredTenantId().toString());
        identity.put("operationId", "mes.nonconformance.create");
        identity.put("identity", List.of(auditId(review.getId())));
        List<GxpAuditRelation> links = new ArrayList<>();
        links.add(new GxpAuditRelation("SUBJECT", "NONCONFORMANCE_REVIEW", auditId(review.getId()), null, null));
        addDispositionRelation(links, "ACTIVE_ORDER", review.getActiveOrderId());
        addDispositionRelation(links, "WORK_ORDER", review.getWorkOrderId());
        addDispositionRelation(links, "BATCH_EXECUTION", review.getBatchExecutionId());
        if (SOURCE_TYPE_PQC_RELEASE.equals(review.getSourceType())) {
            addDispositionRelation(links, "RELEASE_APPLICATION", review.getSourceId());
        }
        links.add(new GxpAuditRelation("SIGNATURE", "ELECTRONIC_SIGNATURE", auditId(signatureId),
                signature.getSubjectVersion(), signature.getContentHash()));
        unifiedAudit.append(GxpAuditCommand.builder().eventSchemaVersion(2).operationId("mes.nonconformance.create")
                .subjectId(auditId(review.getId())).subjectVersion(after.getObjectVersion())
                .reason(review.getNonconformanceReason()).reasonSource("USER").resultStatus("SUCCESS")
                .beforeState(GxpAuditStateEnvelope.builder().state("ABSENT").canonicalJson("{}").build()).afterState(after)
                .idempotencyKey("GXP2:" + DigestUtil.sha256Hex(JsonUtils.toJsonString(identity)))
                .sourceType("SERVICE_METHOD").sourceLocator("MesProEdhrNonconformanceReviewServiceImpl#"
                        + (batchRejection ? "rejectBatch" : "create"))
                .signatureRecordId(auditId(signatureId)).signatureContentHash(signature.getContentHash())
                .links(links).evidences(List.of(new GxpAuditEvidence("SIGNATURE", auditId(signatureId),
                        signature.getSubjectVersion(), signature.getContentHash(), action))).build());
    }

    private static void addDispositionRelation(List<GxpAuditRelation> links, String type, Long id) {
        if (id != null) {
            links.add(new GxpAuditRelation("AFFECTED", type, auditId(id), null, null));
        }
    }

    private static String auditId(Long id) {
        return id == null ? null : id.toString();
    }

    private void closeManagerReleaseForNonconformance(
            MesProcessPoolActiveOrderReleaseApplicationDO application,
            String decision, Long qaUserId, LocalDateTime closedAt) {
        if (application == null || application.getReleaseTransactionId() == null) {
            if (application != null && application.getReleaseApprovalWorkTaskId() != null) {
                workTaskService.cancelReleaseApprovalTaskById(
                        application.getReleaseApprovalWorkTaskId(), decision);
            }
            return;
        }
        MesProEdhrReleaseTransactionDO transaction = releaseTransactionMapper
                .selectByIdForUpdate(application.getReleaseTransactionId());
        if (transaction == null) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_STATUS_INVALID);
        }
        if (MesProEdhrReleaseServiceImpl.STATUS_PENDING_APPROVAL.equals(transaction.getReleaseStatus())) {
            Integer version = transaction.getVersion();
            if (version == null || version <= 0) {
                throw exception(PRO_EDHR_BATCH_EXECUTION_STATUS_INVALID);
            }
            int updated = releaseTransactionMapper.updateById(new MesProEdhrReleaseTransactionDO()
                    .setId(transaction.getId())
                    .setReleaseStatus(MesProEdhrReleaseServiceImpl.STATUS_REJECTED)
                    .setRejectedBy(qaUserId)
                    .setRejectedAt(closedAt)
                    .setRejectReason(decision)
                    .setVersion(version == null ? null : version + 1));
            if (updated != 1) {
                throw exception(PRO_EDHR_BATCH_EXECUTION_STATUS_INVALID);
            }
            workTaskService.cancelReleaseApprovalTask(transaction.getId(), decision);
            return;
        }
        if (MesProEdhrReleaseServiceImpl.STATUS_REJECTED.equals(transaction.getReleaseStatus())
                || MesProEdhrReleaseServiceImpl.STATUS_WITHDRAWN.equals(transaction.getReleaseStatus())) {
            workTaskService.cancelReleaseApprovalTask(transaction.getId(), decision);
            return;
        }
        throw exception(PRO_EDHR_BATCH_EXECUTION_STATUS_INVALID);
    }

    @Override
    public MesProEdhrNonconformanceReviewRespVO get(Long id) {
        MesProEdhrNonconformanceReviewDO review = reviewMapper.selectById(id);
        if (review == null) {
            throw exception(PRO_EDHR_NONCONFORMANCE_REVIEW_NOT_EXISTS);
        }
        return toResp(review);
    }

    @Override
    public MesTeamLeaderActiveOrderDetail getActiveOrderDetail(Long id) {
        MesProEdhrNonconformanceReviewDO review = reviewMapper.selectById(id);
        if (review == null) {
            throw exception(PRO_EDHR_NONCONFORMANCE_REVIEW_NOT_EXISTS);
        }
        Long activeOrderId = requireActiveOrderId(review.getActiveOrderId());
        return activeOrderDetailService.getFormalDetail(activeOrderId);
    }

    @Override
    public PageResult<MesProEdhrNonconformanceReviewRespVO> getPage(
            MesProEdhrNonconformanceReviewPageReqVO reqVO) {
        PageResult<MesProEdhrNonconformanceReviewDO> page = reviewMapper.selectPage(reqVO);
        return new PageResult<>(page.getList().stream().map(this::toResp).toList(), page.getTotal());
    }

    @Override
    public PageResult<MesProEdhrNonconformanceReviewRespVO> getPendingPage(
            MesProEdhrNonconformanceReviewPageReqVO reqVO) {
        PageResult<MesProEdhrNonconformanceReviewDO> page = reviewMapper.selectPendingPage(reqVO);
        return new PageResult<>(page.getList().stream().map(this::toResp).toList(), page.getTotal());
    }

    @Override
    public List<MesProEdhrNonconformanceReviewRespVO> listByBatchExecutionId(Long batchExecutionId) {
        return reviewMapper.selectListByBatchExecutionId(batchExecutionId).stream()
                .map(this::toResp)
                .toList();
    }

    @Override
    public boolean isBatchFrozen(Long batchExecutionId) {
        if (batchExecutionId == null) {
            return false;
        }
        if (reviewMapper.selectPendingByBatchExecutionId(batchExecutionId) != null) {
            return true;
        }
        List<Long> activeOrderIds = batchExecutionOriginMapper.selectListByBatchExecutionId(batchExecutionId).stream()
                .map(MesProEdhrBatchExecutionOriginDO::getActiveOrderId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        for (Long activeOrderId : activeOrderIds) {
            if (reviewMapper.selectFirstBlockingPqcSubmissionByActiveOrderId(activeOrderId) != null) {
                return true;
            }
        }
        MesProEdhrBatchExecutionDO batch = batchExecutionMapper.selectById(batchExecutionId);
        return batch != null && batch.getWorkOrderId() != null
                && reviewMapper.selectFirstBlockingByWorkOrderId(batch.getWorkOrderId()) != null;
    }

    @Override
    public void ensureBatchNotFrozen(Long batchExecutionId, String actionName) {
        MesProEdhrNonconformanceReviewDO pendingReview = batchExecutionId == null
                ? null : reviewMapper.selectPendingByBatchExecutionId(batchExecutionId);
        if (pendingReview == null && batchExecutionId != null) {
            List<Long> activeOrderIds = batchExecutionOriginMapper.selectListByBatchExecutionId(batchExecutionId).stream()
                    .map(MesProEdhrBatchExecutionOriginDO::getActiveOrderId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            for (Long activeOrderId : activeOrderIds) {
                pendingReview = reviewMapper.selectFirstBlockingPqcSubmissionByActiveOrderId(activeOrderId);
                if (pendingReview != null) {
                    break;
                }
            }
            if (pendingReview == null) {
                MesProEdhrBatchExecutionDO batch = batchExecutionMapper.selectById(batchExecutionId);
                if (batch != null && batch.getWorkOrderId() != null) {
                    pendingReview = reviewMapper.selectFirstBlockingByWorkOrderId(batch.getWorkOrderId());
                }
            }
        }
        if (pendingReview != null) {
            throw exception(PRO_EDHR_NONCONFORMANCE_REVIEW_FROZEN_ACTION_LOCKED,
                    actionName, buildPendingReviewFreezeDetail(actionName, pendingReview));
        }
    }

    @Override
    public void ensureWorkOrderNotFrozen(Long workOrderId, String actionName) {
        MesProEdhrNonconformanceReviewDO blockingReview = workOrderId == null
                ? null : reviewMapper.selectFirstBlockingByWorkOrderId(workOrderId);
        if (blockingReview != null) {
            String branch = STATUS_PENDING_REVIEW.equals(blockingReview.getReviewStatus())
                    ? "待处置不合格评审" : "作废处置不合格评审";
            throw exception(PRO_EDHR_NONCONFORMANCE_REVIEW_FROZEN_ACTION_LOCKED,
                    actionName, buildReviewFreezeDetail(actionName, branch, blockingReview));
        }
        if (workOrderId != null) {
            MesProWorkOrderDO workOrder = lockWorkOrder(workOrderId);
            if (Boolean.TRUE.equals(workOrder.getTemporaryFrozen())) {
                throw exception(cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants
                                .PRO_WORK_ORDER_TEMPORARY_FROZEN_OPERATION_FORBIDDEN,
                        actionName, buildWorkOrderFreezeDetail(actionName, workOrderId, "工单临时冻结"));
            }
        }
    }

    @Override
    public void ensurePqcSubmissionNotFrozen(Long activeOrderId, Long workOrderId, String actionName) {
        Long requiredActiveOrderId = requireActiveOrderId(activeOrderId);
        MesProEdhrNonconformanceReviewDO blockingReview =
                reviewMapper.selectFirstBlockingPqcSubmissionByActiveOrderId(requiredActiveOrderId);
        if (blockingReview != null) {
            String branch = STATUS_PENDING_REVIEW.equals(blockingReview.getReviewStatus())
                    ? "待处置不合格评审" : "作废处置不合格评审";
            throw exception(PRO_EDHR_NONCONFORMANCE_REVIEW_FROZEN_ACTION_LOCKED,
                    actionName, buildReviewFreezeDetail(actionName, branch, blockingReview));
        }
        if (workOrderId != null) {
            MesProWorkOrderDO workOrder = lockWorkOrder(workOrderId);
            if (Boolean.TRUE.equals(workOrder.getTemporaryFrozen())) {
                throw exception(cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants
                                .PRO_WORK_ORDER_TEMPORARY_FROZEN_OPERATION_FORBIDDEN,
                        actionName, buildWorkOrderFreezeDetail(actionName, workOrderId, "工单临时冻结"));
            }
        }
    }

    private String buildPendingReviewFreezeDetail(String actionName, MesProEdhrNonconformanceReviewDO review) {
        return buildReviewFreezeDetail(actionName, "待处置不合格评审", review);
    }

    private String buildReviewFreezeDetail(String actionName, String branch, MesProEdhrNonconformanceReviewDO review) {
        return "冻结分支=" + branch
                + ", action=" + actionName
                + ", batchExecutionId=" + review.getBatchExecutionId()
                + ", workOrderId=" + review.getWorkOrderId()
                + ", reviewId=" + review.getId()
                + ", reviewCode=" + review.getReviewCode()
                + ", sourceType=" + review.getSourceType()
                + ", sourceId=" + review.getSourceId();
    }

    private String buildWorkOrderFreezeDetail(String actionName, Long workOrderId, String branch) {
        return "冻结分支=" + branch
                + ", action=" + actionName
                + ", workOrderId=" + workOrderId;
    }

    private void verifyReworkReplay(MesProEdhrNonconformanceReviewDO review, String disposition,
                                     ResolvedReviewMaterials materials, String opinion) {
        JSONObject trace = JSON.parseObject(review.getTraceSnapshotJson());
        JSONObject signature = trace == null ? null : trace.getJSONObject("qaSignatureSnapshotJson");
        Long actor = SecurityFrameworkUtils.getLoginUserId();
        String hash = buildQaDispositionAggregateHash(review, disposition, materials.summaryUrl(),
                materials.primaryFileId(), materials.materialsJson(), opinion, actor);
        if (!DISPOSITION_REWORK.equals(review.getDisposition())
                || !Objects.equals(review.getQaUserId(), actor) || signature == null
                || signature.getLong("signatureId") == null || signature.getLong("signatureId") <= 0
                || !Objects.equals(signature.getLong("reviewId"), review.getId())
                || !Objects.equals(signature.getString("aggregateHash"), hash)) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_STATUS_INVALID);
        }
        var cycle = activeOrderMapper.selectByReworkReviewId(review.getId());
        if (cycle == null || !Objects.equals(cycle.getWorkOrderId(), review.getWorkOrderId())
                || !Objects.equals(cycle.getReworkSourceActiveOrderId(), review.getActiveOrderId())) {
            throw exception(PRO_EDHR_NONCONFORMANCE_REVIEW_SOURCE_INVALID);
        }
        Map<String, Object> identity = new TreeMap<>();
        identity.put("tenantId", TenantContextHolder.getRequiredTenantId().toString());
        identity.put("operationId", "mes.nonconformance.rework");
        identity.put("identity", List.of(auditId(review.getId()), DISPOSITION_REWORK));
        String key = "GXP2:" + DigestUtil.sha256Hex(JsonUtils.toJsonString(identity));
        var receipt = auditReceiptMapper.selectByIdempotencyKeyForUpdate(TenantContextHolder.getRequiredTenantId(), key);
        if (receipt == null) {
            throw exception(cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.GXP_AUDIT_RECEIPT_MISSING, key);
        }
        if (!Objects.equals(receipt.getTenantId(), TenantContextHolder.getRequiredTenantId())
                || !Objects.equals(receipt.getOperationId(), "mes.nonconformance.rework")
                || !Objects.equals(receipt.getSubjectType(), "NONCONFORMANCE_REVIEW")
                || !Objects.equals(receipt.getSubjectId(), auditId(review.getId()))
                || !Objects.equals(receipt.getIdempotencyKey(), key)
                || !Objects.equals(receipt.getActorId(), actor)
                || !Objects.equals(receipt.getSignatureRecordId(), auditId(signature.getLong("signatureId")))
                || !Objects.equals(receipt.getResultStatus(), "SUCCESS")) {
            throw exception(cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.GXP_AUDIT_IDEMPOTENCY_CONFLICT, key);
        }
    }

    private MesProEdhrBatchExecutionDO requireBatchExecution(Long batchExecutionId) {
        MesProEdhrBatchExecutionDO batch = batchExecutionId == null ? null : batchExecutionMapper.selectById(batchExecutionId);
        if (batch == null) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_NOT_EXISTS);
        }
        return batch;
    }

    private MesProEdhrBatchExecutionDO requireBatchExecutionForUpdate(Long batchExecutionId) {
        MesProEdhrBatchExecutionDO batch = batchExecutionId == null
                ? null : batchExecutionMapper.selectByIdForUpdate(batchExecutionId);
        if (batch == null) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_NOT_EXISTS);
        }
        return batch;
    }

    private void validateBatchCanStartReview(MesProEdhrBatchExecutionDO batch) {
        if (reviewMapper.selectPendingByBatchExecutionId(batch.getId()) != null) {
            throw exception(PRO_EDHR_NONCONFORMANCE_REVIEW_PENDING_EXISTS);
        }
        if (Objects.equals(batch.getStatus(), MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_VOIDED)
                || Objects.equals(batch.getStatus(), MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_ARCHIVED)
                || Objects.equals(batch.getStatus(), MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_REJECTED)
                || Objects.equals(batch.getStatus(), MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CLOSED)
                || Objects.equals(batch.getStatus(), MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_FROZEN)) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_STATUS_INVALID);
        }
    }

    private MesProEdhrNonconformanceReviewDO createBatchReview(MesProEdhrBatchExecutionDO batch,
                                                               String sourceType,
                                                               Long sourceId,
                                                               String reason,
                                                               String remark,
                                                               Long signatureId) {
        MesProWorkOrderDO workOrder = lockWorkOrder(batch.getWorkOrderId());
        LocalDateTime now = now();
        Boolean previousWorkOrderTemporaryFrozen = captureWorkOrderExternalFreezeAtReviewStart(workOrder, now);
        Long activeOrderId = requireActiveOrderId(resolveActiveOrderId(batch, null, null));
        MesProEdhrNonconformanceReviewDO review = MesProEdhrNonconformanceReviewDO.builder()
                .reviewCode(buildReviewCode(now))
                .sourceType(sourceType)
                .sourceId(sourceId)
                .activeOrderId(activeOrderId)
                .batchExecutionId(batch.getId())
                .batchExecutionCode(batch.getBatchExecutionCode())
                .workOrderId(batch.getWorkOrderId())
                .workOrderCode(batch.getWorkOrderCode())
                .batchCode(batch.getBatchCode())
                .previousBatchStatus(batch.getStatus())
                .previousWorkOrderTemporaryFrozen(previousWorkOrderTemporaryFrozen)
                .reviewStatus(STATUS_PENDING_REVIEW)
                .nonconformanceReason(reason)
                .frozenAt(now)
                .remark(StrUtil.trim(remark))
                .build();
        reviewMapper.insert(review);
        batchExecutionMapper.updateById(new MesProEdhrBatchExecutionDO()
                .setId(batch.getId())
                .setStatus(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_FROZEN));
        if (workOrder != null) {
            requireWorkOrderUpdate(workOrder.getId(), true);
        }
        recordReviewOperation("NONCONFORMANCE_REVIEW_CREATE", "创建不合格评审", review, activeOrderId,
                null, now, signatureId, null, null, null, null, "电子签名#" + signatureId, null);
        return review;
    }

    private void validateBatchCanDisposeReview(MesProEdhrBatchExecutionDO batch,
                                               MesProEdhrNonconformanceReviewDO review) {
        if (batch == null) {
            return;
        }
        if (review.getPreviousBatchStatus() == null
                || Objects.equals(batch.getStatus(), MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CLOSED)
                || Objects.equals(batch.getStatus(), MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_ARCHIVED)
                || Objects.equals(batch.getStatus(), MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_REJECTED)
                || Objects.equals(batch.getStatus(), MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_VOIDED)) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_STATUS_INVALID);
        }
    }

    private String buildCreateAggregateHash(MesProEdhrNonconformanceReviewDO review, Long activeOrderId,
                                            Long actorId) {
        JSONObject payload = new JSONObject(true);
        payload.put("reviewId", review.getId());
        payload.put("reviewCode", review.getReviewCode());
        payload.put("activeOrderId", activeOrderId);
        payload.put("sourceType", review.getSourceType());
        payload.put("sourceId", review.getSourceId());
        payload.put("batchExecutionId", review.getBatchExecutionId());
        payload.put("workOrderId", review.getWorkOrderId());
        payload.put("nonconformanceReason", review.getNonconformanceReason());
        payload.put("actorId", actorId);
        return DigestUtil.sha256Hex(JSON.toJSONString(payload));
    }

    private MesProcessPoolActiveOrderReleaseApplicationDO requirePqcReleaseApplicationForUpdate(Long applicationId) {
        MesProcessPoolActiveOrderReleaseApplicationDO application = releaseApplicationMapper
                .selectByIdForUpdate(applicationId);
        if (application == null
                || !MesReleaseFlowStatus.PQC_RELEASE_PENDING.equals(application.getApplicationStatus())
                || application.getWorkOrderId() == null || application.getVersion() == null
                || application.getVersion() <= 0) {
            throw exception(PRO_EDHR_NONCONFORMANCE_REVIEW_SOURCE_INVALID);
        }
        return application;
    }

    private MesProProcessPoolEventDO requirePqcSubmissionEventForUpdate(Long eventId) {
        MesProProcessPoolEventDO event = processPoolEventMapper.selectByIdForUpdate(eventId);
        if (event == null || !MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION.equals(event.getEventType())
                || event.getWorkOrderId() == null) {
            throw exception(PRO_EDHR_NONCONFORMANCE_REVIEW_SOURCE_INVALID);
        }
        return event;
    }

    private MesProWorkOrderDO lockWorkOrder(Long workOrderId) {
        if (workOrderId == null) {
            return null;
        }
        MesProWorkOrderDO workOrder = workOrderMapper.selectByIdForUpdate(workOrderId);
        if (workOrder == null) {
            throw exception(cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_WORK_ORDER_NOT_EXISTS);
        }
        return workOrder;
    }

    private Boolean captureWorkOrderExternalFreezeAtReviewStart(MesProWorkOrderDO workOrder, LocalDateTime frozenAt) {
        if (workOrder == null) {
            return null;
        }
        if (!Boolean.TRUE.equals(workOrder.getTemporaryFrozen())) {
            return Boolean.FALSE;
        }
        List<MesProEdhrNonconformanceReviewDO> lifecycleReviews =
                reviewMapper.selectFreezeLifecycleByWorkOrderId(workOrder.getId());
        boolean alreadyFrozenByReview = lifecycleReviews.stream().anyMatch(this::isReviewFreezeBlocking);
        if (!alreadyFrozenByReview) {
            return Boolean.TRUE;
        }
        return hasExternalFreezeInLifecycle(lifecycleReviews, new FreezeWindow(null, frozenAt,
                LocalDateTime.MAX, false), true);
    }

    private boolean recomputeWorkOrderTemporaryFreeze(MesProWorkOrderDO workOrder,
                                                     MesProEdhrNonconformanceReviewDO closingReview,
                                                     String disposition,
                                                     LocalDateTime closedAt) {
        if (DISPOSITION_VOID.equals(disposition)) {
            return true;
        }
        List<MesProEdhrNonconformanceReviewDO> lifecycleReviews =
                reviewMapper.selectFreezeLifecycleByWorkOrderId(closingReview.getWorkOrderId());
        if (lifecycleReviews.stream()
                .anyMatch(review -> !Objects.equals(review.getId(), closingReview.getId())
                        && isReviewFreezeBlocking(review))) {
            return true;
        }
        if (!Boolean.TRUE.equals(workOrder.getTemporaryFrozen())) {
            return false;
        }
        return hasExternalFreezeInLifecycle(lifecycleReviews,
                toFreezeWindow(closingReview, closingReview.getId(), disposition, closedAt, false), false);
    }

    private boolean hasExternalFreezeInLifecycle(List<MesProEdhrNonconformanceReviewDO> reviews,
                                                 FreezeWindow anchor,
                                                 boolean extendBlockingWindows) {
        List<FreezeWindow> windows = new ArrayList<>();
        boolean anchorIncluded = false;
        for (MesProEdhrNonconformanceReviewDO review : reviews) {
            if (Objects.equals(review.getId(), anchor.id)) {
                windows.add(anchor);
                anchorIncluded = true;
                continue;
            }
            FreezeWindow window = toFreezeWindow(review, anchor.id, null, null, extendBlockingWindows);
            windows.add(window);
        }
        if (!anchorIncluded) {
            windows.add(anchor);
        }
        LocalDateTime cycleStart = anchor.start;
        LocalDateTime cycleEnd = anchor.end;
        boolean changed;
        do {
            changed = false;
            for (FreezeWindow window : windows) {
                if (!overlaps(window, cycleStart, cycleEnd)) {
                    continue;
                }
                if (window.start.isBefore(cycleStart)) {
                    cycleStart = window.start;
                    changed = true;
                }
                if (window.end.isAfter(cycleEnd)) {
                    cycleEnd = window.end;
                    changed = true;
                }
            }
        } while (changed);
        for (FreezeWindow window : windows) {
            if (window.start.equals(cycleStart) && overlaps(window, cycleStart, cycleEnd)
                    && window.previousExternalFreeze) {
                return true;
            }
        }
        return false;
    }

    private FreezeWindow toFreezeWindow(MesProEdhrNonconformanceReviewDO review,
                                        Long closingReviewId,
                                        String closingDisposition,
                                        LocalDateTime closingAt,
                                        boolean extendBlockingWindows) {
        LocalDateTime start = requireFreezeWindowStart(review);
        LocalDateTime end;
        if (Objects.equals(review.getId(), closingReviewId) && closingAt != null) {
            end = DISPOSITION_VOID.equals(closingDisposition) ? LocalDateTime.MAX : closingAt;
        } else if (extendBlockingWindows && isReviewFreezeBlocking(review)) {
            end = LocalDateTime.MAX;
        } else if (review.getClosedAt() != null) {
            end = review.getClosedAt();
        } else if (isReviewFreezeBlocking(review)) {
            end = LocalDateTime.MAX;
        } else {
            throw exception(PRO_EDHR_NONCONFORMANCE_REVIEW_WORK_ORDER_STATE_REQUIRED);
        }
        return new FreezeWindow(review.getId(), start, end,
                Boolean.TRUE.equals(review.getPreviousWorkOrderTemporaryFrozen()));
    }

    private LocalDateTime requireFreezeWindowStart(MesProEdhrNonconformanceReviewDO review) {
        if (review.getFrozenAt() == null) {
            throw exception(PRO_EDHR_NONCONFORMANCE_REVIEW_WORK_ORDER_STATE_REQUIRED);
        }
        return review.getFrozenAt();
    }

    private boolean overlaps(FreezeWindow window, LocalDateTime start, LocalDateTime end) {
        return !window.start.isAfter(end) && !window.end.isBefore(start);
    }

    private boolean isReviewFreezeBlocking(MesProEdhrNonconformanceReviewDO review) {
        return STATUS_PENDING_REVIEW.equals(review.getReviewStatus())
                || DISPOSITION_VOID.equals(review.getDisposition());
    }

    private void requireWorkOrderUpdate(Long workOrderId, Boolean temporaryFrozen) {
        if (workOrderMapper.updateTemporaryFrozenByIds(List.of(workOrderId), temporaryFrozen) != 1) {
            throw exception(cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_WORK_ORDER_NOT_EXISTS);
        }
    }

    private MesProEdhrWorkTaskDO requirePqcTaskForUpdate(
            MesProcessPoolActiveOrderReleaseApplicationDO application) {
        if (application.getPqcReleaseWorkTaskId() == null) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_STATUS_INVALID);
        }
        MesProEdhrWorkTaskDO lockedTask = workTaskMapper.selectByIdForUpdate(application.getPqcReleaseWorkTaskId());
        if (lockedTask == null || !"PQC_PRODUCTION_RELEASE".equals(lockedTask.getTaskType())
                || !"RELEASE_APPLICATION".equals(lockedTask.getBusinessScopeType())
                || !Objects.equals(application.getId(), lockedTask.getBusinessScopeId())
                || !List.of(MesProEdhrWorkTaskStatus.TODO, MesProEdhrWorkTaskStatus.DOING,
                MesProEdhrWorkTaskStatus.OVERDUE, MesProEdhrWorkTaskStatus.DONE).contains(lockedTask.getStatus())) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_STATUS_INVALID);
        }
        return lockedTask;
    }

    private MesProcessPoolActiveOrderReleaseApplicationDO resolvePqcReleaseApplicationForReview(
            MesProEdhrNonconformanceReviewDO review) {
        if (SOURCE_TYPE_PQC_RELEASE.equals(review.getSourceType()) && review.getSourceId() != null) {
            return requirePqcReleaseApplicationForUpdate(review.getSourceId());
        }
        if (review.getActiveOrderId() == null) {
            return null;
        }
        LocalDateTime reviewAt = review.getFrozenAt() != null ? review.getFrozenAt()
                : review.getClosedAt() != null ? review.getClosedAt() : review.getCreateTime();
        return releaseApplicationMapper.selectListByActiveOrderIdsForUpdate(List.of(review.getActiveOrderId())).stream()
                .filter(this::isPqcReleaseApplicationOpenForNonconformance)
                .filter(application -> review.getWorkOrderId() == null
                        || Objects.equals(review.getWorkOrderId(), application.getWorkOrderId()))
                .filter(application -> reviewAt == null || application.getAppliedAt() == null
                        || !application.getAppliedAt().isAfter(reviewAt))
                .max(java.util.Comparator
                        .comparing(MesProcessPoolActiveOrderReleaseApplicationDO::getAppliedAt,
                                java.util.Comparator.nullsFirst(java.util.Comparator.naturalOrder()))
                        .thenComparing(MesProcessPoolActiveOrderReleaseApplicationDO::getId))
                .orElse(null);
    }

    private boolean isPqcReleaseApplicationOpenForNonconformance(
            MesProcessPoolActiveOrderReleaseApplicationDO application) {
        return application != null && List.of(
                MesReleaseFlowStatus.PQC_RELEASE_PENDING,
                MesReleaseFlowStatus.REPORT_UPLOAD_PENDING,
                MesReleaseFlowStatus.MANAGER_RELEASE_PENDING,
                MesReleaseFlowStatus.RELEASED).contains(application.getApplicationStatus());
    }

    private void validatePqcReleaseReviewBatchExecutionId(
            MesProcessPoolActiveOrderReleaseApplicationDO application, Long requestBatchExecutionId) {
        if (requestBatchExecutionId == null) {
            return;
        }
        if (application == null || application.getBatchExecutionId() == null
                || !Objects.equals(application.getBatchExecutionId(), requestBatchExecutionId)) {
            throw exception(PRO_EDHR_NONCONFORMANCE_REVIEW_SOURCE_INVALID);
        }
    }

    private Long resolveActiveOrderId(MesProEdhrBatchExecutionDO batch,
                                      MesProcessPoolActiveOrderReleaseApplicationDO application,
                                      MesProProcessPoolEventDO pqcSubmissionEvent) {
        if (application != null) {
            return application.getActiveOrderId();
        }
        if (batch != null) {
            List<Long> activeOrderIds = batchExecutionOriginMapper.selectListByBatchExecutionId(batch.getId()).stream()
                    .map(MesProEdhrBatchExecutionOriginDO::getActiveOrderId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            if (activeOrderIds.size() > 1) {
                throw new IllegalStateException("不合格评审批次来源未唯一绑定活跃订单：" + batch.getId());
            }
            return activeOrderIds.isEmpty() ? null : activeOrderIds.get(0);
        }
        if (pqcSubmissionEvent != null) {
            MesPqcInspectionTaskDO task = pqcInspectionTaskMapper
                    .selectBySubmittedEventId(pqcSubmissionEvent.getId());
            if (task == null) {
                return null;
            }
            if (task.getActiveOrderId() == null
                    || !Objects.equals(task.getWorkOrderId(), pqcSubmissionEvent.getWorkOrderId())) {
                throw new IllegalStateException("不合格评审PQC提交来源未绑定活跃订单：" + pqcSubmissionEvent.getId());
            }
            return task.getActiveOrderId();
        }
        return null;
    }

    private Long resolveActiveOrderId(MesProEdhrNonconformanceReviewDO review,
                                      MesProEdhrBatchExecutionDO batch,
                                      MesProcessPoolActiveOrderReleaseApplicationDO application) {
        if (SOURCE_TYPE_PQC_SUBMISSION.equals(review.getSourceType())) {
            MesPqcInspectionTaskDO task = pqcInspectionTaskMapper.selectBySubmittedEventId(review.getSourceId());
            if (task == null) {
                return null;
            }
            if (task.getActiveOrderId() == null
                    || !Objects.equals(task.getWorkOrderId(), review.getWorkOrderId())) {
                throw new IllegalStateException("不合格评审PQC提交来源未绑定活跃订单：" + review.getSourceId());
            }
            return task.getActiveOrderId();
        }
        return resolveActiveOrderId(batch, application, null);
    }

    private Long requireActiveOrderId(Long activeOrderId) {
        if (activeOrderId == null || activeOrderId <= 0) {
            throw exception(PRO_EDHR_NONCONFORMANCE_REVIEW_SOURCE_INVALID);
        }
        return activeOrderId;
    }

    private void recordReviewOperation(String operationType, String actionName,
                                       MesProEdhrNonconformanceReviewDO review, Long activeOrderId,
                                       String disposition, LocalDateTime occurredAt,
                                       Long signatureId, String reviewMaterialUrl, Long reviewMaterialFileId,
                                       String reviewMaterialsJson, String reviewOpinion, String qaSignature,
                                       Long qaUserId) {
        Map<String, Object> metadata = new java.util.LinkedHashMap<>();
        metadata.put("activeOrderId", activeOrderId);
        metadata.put("reviewId", review.getId());
        metadata.put("reviewCode", review.getReviewCode());
        metadata.put("sourceType", review.getSourceType());
        metadata.put("sourceId", review.getSourceId());
        metadata.put("batchExecutionId", review.getBatchExecutionId());
        metadata.put("workOrderId", review.getWorkOrderId());
        metadata.put("nonconformanceReason", review.getNonconformanceReason());
        metadata.put("reviewMaterialUrl", reviewMaterialUrl);
        metadata.put("reviewMaterialFileId", reviewMaterialFileId);
        metadata.put("reviewMaterialsJson", reviewMaterialsJson);
        metadata.put("reviewOpinion", reviewOpinion);
        metadata.put("disposition", disposition);
        metadata.put("qaSignature", qaSignature);
        metadata.put("qaUserId", qaUserId);
        metadata.put("signatureId", signatureId);
        String afterSummaryHash = DigestUtil.sha256Hex(JSON.toJSONString(metadata));
        operationAuditService.recordInCallerTransaction(new MesProEdhrOperationAuditCommand()
                .setRequestId("NONCONFORMANCE-REVIEW-" + operationType + "-" + review.getId())
                .setObjectType("NONCONFORMANCE_REVIEW")
                .setObjectId(String.valueOf(review.getId()))
                .setBatchExecutionId(review.getBatchExecutionId())
                .setOperationType(operationType)
                .setActionName(actionName)
                .setActorUserId(SecurityFrameworkUtils.getLoginUserId())
                .setActorUsername(SecurityFrameworkUtils.getLoginUserNickname())
                .setPermissionCode("mes:pro-edhr:nonconformance-review:update")
                .setPermissionDecision("ALLOW")
                .setResultStatus("SUCCESS")
                .setAfterSummaryHash(afterSummaryHash)
                .setMetadataJson(JSON.toJSONString(metadata))
                .setOccurredAt(occurredAt));
    }

    private String disposeActionName(String disposition) {
        if (DISPOSITION_CONCESSION_RELEASE.equals(disposition)) {
            return "让步放行";
        }
        if (DISPOSITION_REWORK.equals(disposition)) {
            return "返工处理";
        }
        if (DISPOSITION_VOID.equals(disposition)) {
            return "作废处理";
        }
        throw new IllegalStateException("不支持的不合格评审处置动作：" + disposition);
    }

    private String requireSourceType(String rawSourceType) {
        String sourceType = StrUtil.trim(rawSourceType);
        if (!SUPPORTED_SOURCE_TYPES.contains(sourceType)) {
            throw exception(PRO_EDHR_NONCONFORMANCE_REVIEW_SOURCE_INVALID);
        }
        return sourceType;
    }

    private String requireDisposition(String rawDisposition) {
        String disposition = StrUtil.trim(rawDisposition);
        if (!SUPPORTED_DISPOSITIONS.contains(disposition)) {
            throw exception(PRO_EDHR_NONCONFORMANCE_REVIEW_DISPOSITION_INVALID);
        }
        return disposition;
    }

    private String requireText(String value) {
        String text = StrUtil.trim(value);
        if (StrUtil.isBlank(text)) {
            throw exception(PRO_EDHR_NONCONFORMANCE_REVIEW_REQUIRED);
        }
        return text;
    }

    private ResolvedReviewMaterials resolveReviewMaterials(
            List<MesProEdhrNonconformanceReviewDisposeReqVO.ReviewMaterialReqVO> reviewMaterials,
            List<MesProEdhrNonconformanceReviewDisposeReqVO.ReviewMaterialEventReqVO> reviewMaterialEvents) {
        if (reviewMaterials == null || reviewMaterials.isEmpty()) {
            throw exception(PRO_EDHR_NONCONFORMANCE_REVIEW_REQUIRED);
        }
        List<MaterialDraft> drafts = new ArrayList<>();
        for (int index = 0; index < reviewMaterials.size(); index++) {
            MesProEdhrNonconformanceReviewDisposeReqVO.ReviewMaterialReqVO material = reviewMaterials.get(index);
            if (material == null || StrUtil.isBlank(material.getUrl())) {
                throw exception(PRO_EDHR_NONCONFORMANCE_REVIEW_REQUIRED);
            }
            String url = requireText(material.getUrl());
            drafts.add(new MaterialDraft(url, StrUtil.trim(material.getFileName()),
                    material.getSortNo() == null ? index + 1 : material.getSortNo(),
                    parseAdminFileAccessLocation(url)));
        }
        Map<FileLocation, Integer> usedByLocation = new LinkedHashMap<>();
        List<Map<String, Object>> activeMaterials = new ArrayList<>();
        List<String> summaryUrls = new ArrayList<>();
        for (MaterialDraft draft : drafts) {
            int usedCount = usedByLocation.getOrDefault(draft.location(), 0);
            FileDO file = resolveReviewMaterialFile(draft.location(), usedCount);
            usedByLocation.put(draft.location(), usedCount + 1);
            Map<String, Object> active = new LinkedHashMap<>();
            active.put("url", draft.url());
            active.put("fileId", file.getId());
            active.put("fileName", file.getName());
            active.put("sortNo", draft.sortNo());
            active.put("configId", file.getConfigId());
            active.put("path", file.getPath());
            activeMaterials.add(active);
            summaryUrls.add(draft.url());
        }
        List<Map<String, Object>> events = normalizeReviewMaterialEvents(reviewMaterialEvents);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("activeMaterials", activeMaterials);
        payload.put("reviewMaterialEvents", events);
        return new ResolvedReviewMaterials(String.join(",", summaryUrls),
                (Long) activeMaterials.get(0).get("fileId"), JSON.toJSONString(payload));
    }

    private FileDO resolveReviewMaterialFile(FileLocation location, int usedCount) {
        List<FileDO> files = fileMapper.selectList(new LambdaQueryWrapperX<FileDO>()
                .eq(FileDO::getConfigId, location.configId)
                .eq(FileDO::getPath, location.path)
                .orderByDesc(FileDO::getId));
        if (files == null || files.size() <= usedCount) {
            throw exception(PRO_EDHR_NONCONFORMANCE_REVIEW_REQUIRED);
        }
        FileDO file = files.get(usedCount);
        if (file == null || file.getId() == null || file.getId() <= 0) {
            throw exception(PRO_EDHR_NONCONFORMANCE_REVIEW_REQUIRED);
        }
        return file;
    }

    private List<Map<String, Object>> normalizeReviewMaterialEvents(
            List<MesProEdhrNonconformanceReviewDisposeReqVO.ReviewMaterialEventReqVO> reviewMaterialEvents) {
        if (reviewMaterialEvents == null || reviewMaterialEvents.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> events = new ArrayList<>();
        for (int index = 0; index < reviewMaterialEvents.size(); index++) {
            MesProEdhrNonconformanceReviewDisposeReqVO.ReviewMaterialEventReqVO event = reviewMaterialEvents.get(index);
            if (event == null || StrUtil.isBlank(event.getAction()) || StrUtil.isBlank(event.getUrl())) {
                throw exception(PRO_EDHR_NONCONFORMANCE_REVIEW_REQUIRED);
            }
            String action = StrUtil.trim(event.getAction()).toUpperCase();
            if (!"UPLOAD".equals(action) && !"DELETE".equals(action)) {
                throw exception(PRO_EDHR_NONCONFORMANCE_REVIEW_REQUIRED);
            }
            String url = requireText(event.getUrl());
            parseAdminFileAccessLocation(url);
            Map<String, Object> normalized = new LinkedHashMap<>();
            normalized.put("action", action);
            normalized.put("url", url);
            normalized.put("fileName", StrUtil.blankToDefault(StrUtil.trim(event.getFileName()), resolveFileName(url)));
            normalized.put("sequence", event.getSequence() == null ? index + 1 : event.getSequence());
            events.add(normalized);
        }
        return events;
    }

    private String resolveFileName(String url) {
        String value = StrUtil.blankToDefault(url, "");
        int index = value.lastIndexOf('/');
        String name = index < 0 ? value : value.substring(index + 1);
        return UriUtils.decode(name, StandardCharsets.UTF_8);
    }

    private FileLocation parseAdminFileAccessLocation(String reviewMaterialUrl) {
        String text = requireText(reviewMaterialUrl);
        int prefixIndex = text.indexOf(ADMIN_FILE_ACCESS_PREFIX);
        if (prefixIndex < 0) {
            throw exception(PRO_EDHR_NONCONFORMANCE_REVIEW_REQUIRED);
        }
        String tail = text.substring(prefixIndex + ADMIN_FILE_ACCESS_PREFIX.length());
        int getIndex = tail.indexOf(ADMIN_FILE_ACCESS_GET_SEGMENT);
        if (getIndex <= 0) {
            throw exception(PRO_EDHR_NONCONFORMANCE_REVIEW_REQUIRED);
        }
        Long configId = parsePositiveFileConfigId(tail.substring(0, getIndex));
        String encodedPath = tail.substring(getIndex + ADMIN_FILE_ACCESS_GET_SEGMENT.length());
        if (StrUtil.isBlank(encodedPath)) {
            throw exception(PRO_EDHR_NONCONFORMANCE_REVIEW_REQUIRED);
        }
        String path = UriUtils.decode(encodedPath, StandardCharsets.UTF_8);
        if (StrUtil.isBlank(path) || StrUtil.contains(path, "..")) {
            throw exception(PRO_EDHR_NONCONFORMANCE_REVIEW_REQUIRED);
        }
        return new FileLocation(configId, path);
    }

    private Long parsePositiveFileConfigId(String value) {
        try {
            Long parsed = Long.valueOf(value);
            if (parsed <= 0) {
                throw exception(PRO_EDHR_NONCONFORMANCE_REVIEW_REQUIRED);
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw exception(PRO_EDHR_NONCONFORMANCE_REVIEW_REQUIRED);
        }
    }

    private Long recordQaDispositionSignature(Long qaUserId, Long reviewId, String signaturePassword,
                                              String reviewOpinion, String qaDispositionAggregateHash) {
        return signatureService.recordQaDispositionSignature(qaUserId, reviewId, signaturePassword, reviewOpinion,
                qaDispositionAggregateHash);
    }

    private String buildQaDispositionAggregateHash(MesProEdhrNonconformanceReviewDO review,
                                                   String disposition,
                                                   String reviewMaterialUrl,
                                                   Long reviewMaterialFileId,
                                                   String reviewMaterialsJson,
                                                   String reviewOpinion,
                                                   Long qaUserId) {
        JSONObject payload = new JSONObject(true);
        payload.put("reviewId", review.getId());
        payload.put("reviewCode", review.getReviewCode());
        payload.put("sourceType", review.getSourceType());
        payload.put("sourceId", review.getSourceId());
        payload.put("batchExecutionId", review.getBatchExecutionId());
        payload.put("workOrderId", review.getWorkOrderId());
        payload.put("disposition", disposition);
        payload.put("reviewMaterialUrl", reviewMaterialUrl);
        payload.put("reviewMaterialFileId", reviewMaterialFileId);
        payload.put("reviewMaterialsJson", reviewMaterialsJson);
        payload.put("reviewOpinion", reviewOpinion);
        payload.put("qaUserId", qaUserId);
        return DigestUtil.sha256Hex(JSON.toJSONString(payload));
    }

    private String buildReleaseOwnerRejectAggregateHash(MesProEdhrBatchExecutionDO batch,
                                                        String reason,
                                                        Long releaseOwnerUserId) {
        JSONObject payload = new JSONObject(true);
        payload.put("batchExecutionId", batch.getId());
        payload.put("batchExecutionCode", batch.getBatchExecutionCode());
        payload.put("workOrderId", batch.getWorkOrderId());
        payload.put("workOrderCode", batch.getWorkOrderCode());
        payload.put("batchCode", batch.getBatchCode());
        payload.put("previousBatchStatus", batch.getStatus());
        payload.put("sourceType", SOURCE_TYPE_PQC_RELEASE);
        payload.put("nonconformanceReason", reason);
        payload.put("releaseOwnerUserId", releaseOwnerUserId);
        return DigestUtil.sha256Hex(JSON.toJSONString(payload));
    }

    private String buildQaSignatureSnapshotJson(MesProEdhrNonconformanceReviewDO review,
                                                Long qaUserId,
                                                Long qaDispositionSignatureId,
                                                String disposition,
                                                LocalDateTime signedAt,
                                                String qaDispositionAggregateHash) {
        JSONObject qaSignatureSnapshot = new JSONObject(true);
        qaSignatureSnapshot.put("reviewId", review.getId());
        qaSignatureSnapshot.put("qaUserId", qaUserId);
        qaSignatureSnapshot.put("signatureId", qaDispositionSignatureId);
        qaSignatureSnapshot.put("actionType", MesProBatchRecordExecutionSignatureService.ACTION_QA_DISPOSITION);
        qaSignatureSnapshot.put("disposition", disposition);
        qaSignatureSnapshot.put("signedAt", signedAt);
        qaSignatureSnapshot.put("aggregateHash", qaDispositionAggregateHash);
        return JSON.toJSONString(qaSignatureSnapshot);
    }

    private String buildReviewCode(LocalDateTime occurredAt) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        reviewCounterMapper.insertOrIncrement(tenantId);
        MesProEdhrNonconformanceReviewCounterDO counter = reviewCounterMapper
                .selectByTenantIdForUpdate(tenantId);
        if (counter == null || counter.getCurrentSerial() == null || counter.getCurrentSerial() < 0) {
            throw new IllegalStateException("不合格评审编号计数器缺失：tenantId=" + tenantId);
        }
        long serial = counter.getCurrentSerial();
        if (serial > REVIEW_CODE_MAX_SERIAL) {
            throw new IllegalStateException("不合格评审编号流水已超过八位上限：tenantId=" + tenantId);
        }
        return "BHGSP-" + REVIEW_CODE_MONTH_FORMATTER.format(occurredAt) + "-"
                + String.format(Locale.ROOT, "%08d", serial);
    }

    private String buildTraceSnapshotJson(MesProEdhrNonconformanceReviewDO review,
                                           MesProEdhrNonconformanceReviewDO update,
                                           Integer nextBatchStatus,
                                           String qaSignatureSnapshotJson) {
        JSONObject snapshot = new JSONObject(true);
        snapshot.put("reviewId", review.getId());
        snapshot.put("reviewCode", review.getReviewCode());
        snapshot.put("sourceType", review.getSourceType());
        snapshot.put("sourceId", review.getSourceId());
        snapshot.put("batchExecutionId", review.getBatchExecutionId());
        snapshot.put("batchExecutionCode", review.getBatchExecutionCode());
        snapshot.put("workOrderCode", review.getWorkOrderCode());
        snapshot.put("batchCode", review.getBatchCode());
        snapshot.put("nonconformanceReason", review.getNonconformanceReason());
        snapshot.put("reviewMaterialUrl", update.getReviewMaterialUrl());
        snapshot.put("reviewMaterialFileId", update.getReviewMaterialFileId());
        snapshot.put("reviewMaterialsJson", update.getReviewMaterialsJson());
        snapshot.put("reviewOpinion", update.getReviewOpinion());
        snapshot.put("qaSignature", update.getQaSignature());
        snapshot.put("qaSignatureSnapshotJson", JSON.parseObject(qaSignatureSnapshotJson));
        snapshot.put("qaUserId", update.getQaUserId());
        snapshot.put("disposition", update.getDisposition());
        snapshot.put("previousBatchStatus", review.getPreviousBatchStatus());
        snapshot.put("previousWorkOrderTemporaryFrozen", review.getPreviousWorkOrderTemporaryFrozen());
        snapshot.put("nextBatchStatus", nextBatchStatus);
        snapshot.put("frozenAt", review.getFrozenAt());
        snapshot.put("unfrozenAt", update.getUnfrozenAt());
        snapshot.put("voidedAt", update.getVoidedAt());
        snapshot.put("closedAt", update.getClosedAt());
        return JSON.toJSONString(snapshot);
    }

    private String buildNonconformanceDecisionReceipt(
            MesProcessPoolActiveOrderReleaseApplicationDO application,
            MesProEdhrWorkTaskDO task,
            String decision,
            String reason,
            Long decidedBy,
            LocalDateTime decidedAt) {
        JSONObject receipt = new JSONObject(true);
        receipt.put("applicationId", application.getId());
        receipt.put("pqcReleaseWorkTaskId", task.getId());
        receipt.put("decision", decision);
        receipt.put("status", MesReleaseFlowStatus.PQC_RELEASE_REJECTED);
        receipt.put("rejectReason", reason);
        receipt.put("batchRecordEvidenceIds", List.of());
        receipt.put("processInspectionEvidenceIds", List.of());
        receipt.put("lossReportEvidenceIds", List.of());
        receipt.put("lossReportFormCenterInstanceIds", List.of());
        receipt.put("lossReportFieldAuditIds", List.of());
        receipt.put("lossReportFieldAuditHeadHashes", List.of());
        receipt.put("reportUploadTasks", List.of());
        receipt.put("sourceSnapshotHash", application.getSourceSnapshotHash());
        receipt.put("version", application.getVersion() + 1);
        receipt.put("decidedBy", decidedBy);
        receipt.put("decidedAt", decidedAt);
        return JSON.toJSONString(receipt);
    }

    private MesProEdhrNonconformanceReviewRespVO toResp(MesProEdhrNonconformanceReviewDO review) {
        return BeanUtils.toBean(review, MesProEdhrNonconformanceReviewRespVO.class);
    }

    private LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    private static final class FreezeWindow {

        private final Long id;
        private final LocalDateTime start;
        private final LocalDateTime end;
        private final boolean previousExternalFreeze;

        private FreezeWindow(Long id, LocalDateTime start, LocalDateTime end, boolean previousExternalFreeze) {
            this.id = id;
            this.start = start;
            this.end = end;
            this.previousExternalFreeze = previousExternalFreeze;
        }
    }

    private record FileLocation(Long configId, String path) {
    }

    private record MaterialDraft(String url, String fileName, Integer sortNo, FileLocation location) {
    }

    private record ResolvedReviewMaterials(String summaryUrl, Long primaryFileId, String materialsJson) {
    }
}
