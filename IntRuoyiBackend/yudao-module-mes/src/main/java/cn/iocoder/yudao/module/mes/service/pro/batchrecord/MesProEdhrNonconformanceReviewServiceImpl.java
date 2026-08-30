package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrNonconformanceReviewCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrNonconformanceReviewDisposeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrNonconformanceReviewPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrNonconformanceReviewRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrNonconformanceReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrNonconformanceReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowStatus;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_STATUS_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_NONCONFORMANCE_REVIEW_DISPOSITION_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_NONCONFORMANCE_REVIEW_FROZEN_ACTION_LOCKED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_NONCONFORMANCE_REVIEW_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_NONCONFORMANCE_REVIEW_PENDING_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_NONCONFORMANCE_REVIEW_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_NONCONFORMANCE_REVIEW_SOURCE_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrNonconformanceReviewService.DISPOSITION_CONCESSION_RELEASE;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrNonconformanceReviewService.DISPOSITION_REWORK;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrNonconformanceReviewService.DISPOSITION_VOID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrNonconformanceReviewService.SOURCE_TYPE_PQC_RELEASE;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrNonconformanceReviewService.SOURCE_TYPE_PQC_SUBMISSION;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrNonconformanceReviewService.STATUS_CLOSED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrNonconformanceReviewService.STATUS_PENDING_REVIEW;

@Service
@Validated
public class MesProEdhrNonconformanceReviewServiceImpl implements MesProEdhrNonconformanceReviewService {

    private static final Set<String> SUPPORTED_SOURCE_TYPES = Set.of(SOURCE_TYPE_PQC_SUBMISSION, SOURCE_TYPE_PQC_RELEASE);
    private static final Set<String> SUPPORTED_DISPOSITIONS =
            Set.of(DISPOSITION_CONCESSION_RELEASE, DISPOSITION_REWORK, DISPOSITION_VOID);
    private static final DateTimeFormatter REVIEW_CODE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Resource
    private MesProEdhrNonconformanceReviewMapper reviewMapper;
    @Resource
    private MesProEdhrBatchExecutionMapper batchExecutionMapper;
    @Resource
    private MesProcessPoolActiveOrderReleaseApplicationMapper releaseApplicationMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrNonconformanceReviewRespVO create(MesProEdhrNonconformanceReviewCreateReqVO reqVO) {
        String sourceType = requireSourceType(reqVO.getSourceType());
        String reason = requireText(reqVO.getNonconformanceReason());
        MesProEdhrBatchExecutionDO batch = null;
        MesProcessPoolActiveOrderReleaseApplicationDO application = null;
        if (SOURCE_TYPE_PQC_RELEASE.equals(sourceType) && reqVO.getBatchExecutionId() == null) {
            application = requirePqcReleaseApplication(reqVO.getSourceId());
            if (reviewMapper.selectLatestBySource(sourceType, application.getId()) != null) {
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
        LocalDateTime now = now();
        MesProEdhrNonconformanceReviewDO review = MesProEdhrNonconformanceReviewDO.builder()
                .reviewCode(buildReviewCode(batch == null ? application.getId() : batch.getId(), now))
                .sourceType(sourceType)
                .sourceId(reqVO.getSourceId())
                .batchExecutionId(batch == null ? null : batch.getId())
                .batchExecutionCode(batch == null ? null : batch.getBatchExecutionCode())
                .workOrderId(batch == null ? application.getWorkOrderId() : batch.getWorkOrderId())
                .workOrderCode(batch == null ? application.getWorkOrderCode() : batch.getWorkOrderCode())
                .batchCode(batch == null ? application.getBatchCode() : batch.getBatchCode())
                .previousBatchStatus(batch == null ? null : batch.getStatus())
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
        return toResp(review);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrNonconformanceReviewRespVO dispose(MesProEdhrNonconformanceReviewDisposeReqVO reqVO) {
        String disposition = requireDisposition(reqVO.getDisposition());
        String reviewMaterialUrl = requireText(reqVO.getReviewMaterialUrl());
        String reviewOpinion = requireText(reqVO.getReviewOpinion());
        String qaSignature = requireText(reqVO.getQaSignature());
        MesProEdhrNonconformanceReviewDO review = requirePendingReview(reqVO.getId());
        MesProEdhrBatchExecutionDO batch = review.getBatchExecutionId() == null
                ? null : requireBatchExecution(review.getBatchExecutionId());
        if (batch != null && (!Objects.equals(batch.getStatus(), MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_FROZEN)
                || review.getPreviousBatchStatus() == null)) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_STATUS_INVALID);
        }
        LocalDateTime now = now();
        Integer nextBatchStatus = batch == null ? null : DISPOSITION_VOID.equals(disposition)
                ? MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_VOIDED : review.getPreviousBatchStatus();
        MesProEdhrNonconformanceReviewDO update = new MesProEdhrNonconformanceReviewDO()
                .setId(review.getId())
                .setReviewStatus(STATUS_CLOSED)
                .setDisposition(disposition)
                .setReviewMaterialUrl(reviewMaterialUrl)
                .setReviewOpinion(reviewOpinion)
                .setQaSignature(qaSignature)
                .setQaUserId(SecurityFrameworkUtils.getLoginUserId())
                .setClosedAt(now)
                .setUnfrozenAt(DISPOSITION_VOID.equals(disposition) ? null : now)
                .setVoidedAt(DISPOSITION_VOID.equals(disposition) ? now : null);
        update.setTraceSnapshotJson(buildTraceSnapshotJson(review, update, nextBatchStatus));
        reviewMapper.updateById(update);
        if (batch != null) {
            batchExecutionMapper.updateById(new MesProEdhrBatchExecutionDO()
                    .setId(batch.getId())
                    .setStatus(nextBatchStatus));
        }
        return toResp(reviewMapper.selectById(review.getId()));
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
        return batchExecutionId != null && reviewMapper.selectPendingByBatchExecutionId(batchExecutionId) != null;
    }

    @Override
    public void ensureBatchNotFrozen(Long batchExecutionId, String actionName) {
        if (isBatchFrozen(batchExecutionId)) {
            throw exception(PRO_EDHR_NONCONFORMANCE_REVIEW_FROZEN_ACTION_LOCKED, actionName);
        }
    }

    @Override
    public void ensureWorkOrderNotFrozen(Long workOrderId, String actionName) {
        if (workOrderId != null && reviewMapper.selectBlockingCountByWorkOrderId(workOrderId) > 0) {
            throw exception(PRO_EDHR_NONCONFORMANCE_REVIEW_FROZEN_ACTION_LOCKED, actionName);
        }
    }

    private MesProEdhrNonconformanceReviewDO requirePendingReview(Long id) {
        MesProEdhrNonconformanceReviewDO review = reviewMapper.selectById(id);
        if (review == null) {
            throw exception(PRO_EDHR_NONCONFORMANCE_REVIEW_NOT_EXISTS);
        }
        if (!STATUS_PENDING_REVIEW.equals(review.getReviewStatus())) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_STATUS_INVALID);
        }
        return review;
    }

    private MesProEdhrBatchExecutionDO requireBatchExecution(Long batchExecutionId) {
        MesProEdhrBatchExecutionDO batch = batchExecutionId == null ? null : batchExecutionMapper.selectById(batchExecutionId);
        if (batch == null) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_NOT_EXISTS);
        }
        return batch;
    }

    private MesProcessPoolActiveOrderReleaseApplicationDO requirePqcReleaseApplication(Long applicationId) {
        MesProcessPoolActiveOrderReleaseApplicationDO application = applicationId == null
                ? null : releaseApplicationMapper.selectById(applicationId);
        if (application == null
                || !MesReleaseFlowStatus.PQC_RELEASE_PENDING.equals(application.getApplicationStatus())
                || application.getWorkOrderId() == null) {
            throw exception(PRO_EDHR_NONCONFORMANCE_REVIEW_SOURCE_INVALID);
        }
        return application;
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

    private String buildReviewCode(Long batchExecutionId, LocalDateTime occurredAt) {
        return "EDHR-NCR-" + REVIEW_CODE_FORMATTER.format(occurredAt) + "-" + batchExecutionId;
    }

    private String buildTraceSnapshotJson(MesProEdhrNonconformanceReviewDO review,
                                          MesProEdhrNonconformanceReviewDO update,
                                          Integer nextBatchStatus) {
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
        snapshot.put("reviewOpinion", update.getReviewOpinion());
        snapshot.put("qaSignature", update.getQaSignature());
        snapshot.put("qaUserId", update.getQaUserId());
        snapshot.put("disposition", update.getDisposition());
        snapshot.put("previousBatchStatus", review.getPreviousBatchStatus());
        snapshot.put("nextBatchStatus", nextBatchStatus);
        snapshot.put("frozenAt", review.getFrozenAt());
        snapshot.put("unfrozenAt", update.getUnfrozenAt());
        snapshot.put("voidedAt", update.getVoidedAt());
        snapshot.put("closedAt", update.getClosedAt());
        return JSON.toJSONString(snapshot);
    }

    private MesProEdhrNonconformanceReviewRespVO toResp(MesProEdhrNonconformanceReviewDO review) {
        return BeanUtils.toBean(review, MesProEdhrNonconformanceReviewRespVO.class);
    }

    private LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }
}
