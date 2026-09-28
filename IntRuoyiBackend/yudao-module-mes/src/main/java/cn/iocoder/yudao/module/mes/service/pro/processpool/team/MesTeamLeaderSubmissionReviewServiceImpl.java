package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventRevisionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventRevisionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionSignatureService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditHasher;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditSignatureResult;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_PRODUCTION_REVIEW_ALLOCATION_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REVISION_EVENT_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_SUBMISSION_REVIEW_PQC_LEADER_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_SUBMISSION_REVIEW_REJECT_REMARK_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_SUBMISSION_REVIEW_STATUS_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_SUBMISSION_REVIEW_TERMINAL_EXISTS;

@Service
@Validated
public class MesTeamLeaderSubmissionReviewServiceImpl implements MesTeamLeaderSubmissionReviewService {

    private static final Set<String> VALID_REVIEW_STATUSES = Set.of(
            MesProcessPoolSubmissionReviewDO.STATUS_APPROVED,
            MesProcessPoolSubmissionReviewDO.STATUS_REJECTED);

    private final MesTeamLeaderScopeService scopeService;
    private final MesProProcessPoolEventMapper eventMapper;
    private final MesProcessPoolSubmissionReviewMapper reviewMapper;
    private final MesPqcProcessInspectionAggregationService processInspectionAggregationService;

    @Resource
    private MesReportAllocationCommandService reportAllocationCommandService;

    @Resource
    private MesProBatchRecordExecutionSignatureService signatureService;

    @Resource
    private MesProProcessPoolEventRevisionMapper revisionMapper;

    public MesTeamLeaderSubmissionReviewServiceImpl(MesTeamLeaderScopeService scopeService,
                                                    MesProProcessPoolEventMapper eventMapper,
                                                    MesProcessPoolSubmissionReviewMapper reviewMapper,
                                                    MesPqcProcessInspectionAggregationService processInspectionAggregationService) {
        this.scopeService = scopeService;
        this.eventMapper = eventMapper;
        this.reviewMapper = reviewMapper;
        this.processInspectionAggregationService = processInspectionAggregationService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long reviewSubmission(MesTeamLeaderSubmissionReviewReqBO reqBO) {
        validateReq(reqBO);
        if (MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PRODUCTION.equals(reqBO.getLeaderType())
                && MesProcessPoolSubmissionReviewDO.STATUS_REJECTED.equals(reqBO.getReviewStatus())) {
            return reportAllocationCommandService.rejectProductionSubmission(
                    reqBO.getEventId(), reqBO.getLeaderUserId(), reqBO.getReviewRemark(),
                    reqBO.getSignaturePassword());
        }
        MesProProcessPoolEventDO event = eventMapper.selectByIdForUpdate(reqBO.getEventId());
        if (event == null) {
            throw exception(PRO_PROCESS_POOL_REVISION_EVENT_NOT_EXISTS, reqBO.getEventId());
        }
        if (MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION.equals(event.getEventType())
                && !MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PQC.equals(reqBO.getLeaderType())) {
            throw exception(PRO_PROCESS_POOL_SUBMISSION_REVIEW_PQC_LEADER_REQUIRED,
                    reqBO.getEventId(), reqBO.getLeaderType());
        }
        scopeService.assertCanAccessEmployee(reqBO.getLeaderUserId(), reqBO.getLeaderType(),
                event.getActualEmployeeId());
        MesProcessPoolSubmissionReviewDO existingReview =
                reviewMapper.selectLatestByEventIdForUpdate(reqBO.getEventId());
        MesProProcessPoolEventRevisionDO correction = findSignedCorrection(event, existingReview);
        if (existingReview != null && correction == null) {
            if (isIdempotentReplay(reqBO, existingReview)) {
                return existingReview.getId();
            }
            throw exception(PRO_PROCESS_POOL_SUBMISSION_REVIEW_TERMINAL_EXISTS,
                    reqBO.getEventId(), existingReview.getReviewStatus());
        }
        if (MesProcessPoolSubmissionReviewDO.STATUS_APPROVED.equals(reqBO.getReviewStatus())
                && MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT.equals(event.getEventType())) {
            throw exception(PRO_PROCESS_POOL_PRODUCTION_REVIEW_ALLOCATION_REQUIRED, reqBO.getEventId());
        }
        ReviewSignaturePayload reviewSignature = recordReviewSignature(reqBO, event, correction);
        MesProcessPoolSubmissionReviewDO review = MesProcessPoolSubmissionReviewDO.builder()
                .eventId(reqBO.getEventId())
                .leaderUserId(reqBO.getLeaderUserId())
                .leaderType(reqBO.getLeaderType())
                .reviewStatus(reqBO.getReviewStatus())
                .reviewRemark(reqBO.getReviewRemark())
                .reviewedAt(LocalDateTime.now())
                .reviewSignatureId(reviewSignature.reviewSignatureId())
                .reviewSignatureUserId(reviewSignature.reviewSignatureUserId())
                .reviewSignatureSnapshotJson(reviewSignature.reviewSignatureSnapshotJson())
                .build();
        reviewMapper.insert(review);
        if (MesProcessPoolSubmissionReviewDO.STATUS_APPROVED.equals(reqBO.getReviewStatus())
                && MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION.equals(event.getEventType())) {
            processInspectionAggregationService.aggregateApprovedPqcSubmission(reqBO.getEventId(), review.getId());
        }
        return review.getId();
    }

    private MesProProcessPoolEventRevisionDO findSignedCorrection(
            MesProProcessPoolEventDO event, MesProcessPoolSubmissionReviewDO previous) {
        if (previous == null || !MesProcessPoolSubmissionReviewDO.STATUS_REJECTED.equals(previous.getReviewStatus())
                || !MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION.equals(event.getEventType())) {
            return null;
        }
        var revisions = revisionMapper.selectListByEventId(event.getId());
        if (revisions.isEmpty()) {
            return null;
        }
        MesProProcessPoolEventRevisionDO revision = revisions.get(0);
        if (!Objects.equals(event.getId(), revision.getEventId())
                || !Objects.equals(event.getTenantId(), revision.getTenantId())
                || !MesProProcessPoolEventRevisionDO.STATUS_EFFECTIVE.equals(revision.getRevisionStatus())
                || revision.getId() == null || revision.getRevisionSignatureId() == null
                || revision.getRevisionSignatureId() <= 0 || revision.getRevisionSignatureUserId() == null
                || !Objects.equals(revision.getRevisionSignatureUserId(), revision.getModifiedByUserId())
                || StrUtil.isBlank(revision.getRevisionSignatureSnapshot())
                || StrUtil.isBlank(revision.getAfterPayload())) {
            return null;
        }
        var payload = JsonUtils.parseTree(revision.getAfterPayload());
        if (!payload.isObject() || !payload.path("supersededReviewId").isIntegralNumber()
                || !Objects.equals(previous.getId(), payload.path("supersededReviewId").longValue())
                || !Objects.equals(MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(event.getRawPayload()),
                MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(revision.getAfterPayload()))) {
            return null;
        }
        var signature = JsonUtils.parseObject(revision.getRevisionSignatureSnapshot(),
                MesProBatchRecordExecutionFieldAuditSignatureResult.class);
        return signature != null && signature.getSignedAt() != null
                && Objects.equals(signature.getSignatureId(), revision.getRevisionSignatureId())
                && Objects.equals(signature.getActorId(), revision.getRevisionSignatureUserId()) ? revision : null;
    }

    private boolean isIdempotentReplay(MesTeamLeaderSubmissionReviewReqBO reqBO,
                                       MesProcessPoolSubmissionReviewDO existingReview) {
        return existingReview.getId() != null
                && Objects.equals(reqBO.getLeaderUserId(), existingReview.getLeaderUserId())
                && Objects.equals(reqBO.getLeaderType(), existingReview.getLeaderType())
                && Objects.equals(reqBO.getReviewStatus(), existingReview.getReviewStatus())
                && Objects.equals(normalizeRemark(reqBO.getReviewRemark()),
                normalizeRemark(existingReview.getReviewRemark()));
    }

    private String normalizeRemark(String remark) {
        return StrUtil.isBlank(remark) ? null : StrUtil.trim(remark);
    }

    private void validateReq(MesTeamLeaderSubmissionReviewReqBO reqBO) {
        if (reqBO == null || reqBO.getEventId() == null || reqBO.getLeaderUserId() == null
                || StrUtil.isBlank(reqBO.getLeaderType())) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "submissionReview");
        }
        if (StrUtil.isBlank(reqBO.getSignaturePassword())) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "submissionReview.signaturePassword");
        }
        if (!VALID_REVIEW_STATUSES.contains(reqBO.getReviewStatus())) {
            throw exception(PRO_PROCESS_POOL_SUBMISSION_REVIEW_STATUS_INVALID, reqBO.getReviewStatus());
        }
        if (MesProcessPoolSubmissionReviewDO.STATUS_REJECTED.equals(reqBO.getReviewStatus())
                && StrUtil.isBlank(reqBO.getReviewRemark())) {
            throw exception(PRO_PROCESS_POOL_SUBMISSION_REVIEW_REJECT_REMARK_REQUIRED, reqBO.getEventId());
        }
    }

    private ReviewSignaturePayload recordReviewSignature(MesTeamLeaderSubmissionReviewReqBO reqBO,
                                                         MesProProcessPoolEventDO event,
                                                         MesProProcessPoolEventRevisionDO correction) {
        Long signatureId = signatureService.recordTeamLeaderReviewSignature(
                reqBO.getLeaderUserId(),
                reqBO.getSignaturePassword(),
                buildReviewSignatureComment(reqBO, event),
                "PROCESS_POOL_EVENT", event.getId(), "提交记录组长复核");
        return new ReviewSignaturePayload(
                signatureId,
                reqBO.getLeaderUserId(),
                buildReviewSignatureSnapshot(reqBO, event, signatureId, correction));
    }

    private String buildReviewSignatureComment(MesTeamLeaderSubmissionReviewReqBO reqBO,
                                               MesProProcessPoolEventDO event) {
        return "组长复核:" + reqBO.getLeaderType() + ":" + event.getId() + ":" + reqBO.getReviewStatus();
    }

    private String buildReviewSignatureSnapshot(MesTeamLeaderSubmissionReviewReqBO reqBO,
                                                MesProProcessPoolEventDO event,
                                                Long signatureId,
                                                MesProProcessPoolEventRevisionDO correction) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("signatureId", signatureId);
        payload.put("actorId", reqBO.getLeaderUserId());
        payload.put("actionType", MesProBatchRecordExecutionSignatureService.ACTION_TEAM_LEADER_REVIEW);
        payload.put("processPoolEventId", event.getId());
        payload.put("eventType", event.getEventType());
        payload.put("leaderType", reqBO.getLeaderType());
        payload.put("reviewStatus", reqBO.getReviewStatus());
        payload.put("payloadHash", MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(event.getRawPayload()));
        if (correction != null) {
            payload.put("revisionId", correction.getId());
            payload.put("revisionSignatureId", correction.getRevisionSignatureId());
            payload.put("supersededReviewId", JsonUtils.parseTree(correction.getAfterPayload())
                    .path("supersededReviewId").longValue());
        }
        return JsonUtils.toJsonString(payload);
    }

    private record ReviewSignaturePayload(Long reviewSignatureId,
                                          Long reviewSignatureUserId,
                                          String reviewSignatureSnapshotJson) {
    }

}
