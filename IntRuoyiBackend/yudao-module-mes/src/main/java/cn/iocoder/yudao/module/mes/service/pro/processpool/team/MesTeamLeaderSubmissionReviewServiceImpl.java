package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionSignatureService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
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
    private MesProBatchRecordExecutionSignatureService signatureService;

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
        if (existingReview != null
                && !MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION.equals(event.getEventType())) {
            throw exception(PRO_PROCESS_POOL_SUBMISSION_REVIEW_TERMINAL_EXISTS,
                    reqBO.getEventId(), existingReview.getReviewStatus());
        }
        if (MesProcessPoolSubmissionReviewDO.STATUS_APPROVED.equals(reqBO.getReviewStatus())
                && MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT.equals(event.getEventType())) {
            throw exception(PRO_PROCESS_POOL_PRODUCTION_REVIEW_ALLOCATION_REQUIRED, reqBO.getEventId());
        }
        ReviewSignaturePayload reviewSignature = recordReviewSignature(reqBO, event);
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
                                                         MesProProcessPoolEventDO event) {
        Long signatureId = signatureService.recordTeamLeaderReviewSignature(
                reqBO.getLeaderUserId(),
                reqBO.getSignaturePassword(),
                buildReviewSignatureComment(reqBO, event));
        return new ReviewSignaturePayload(
                signatureId,
                reqBO.getLeaderUserId(),
                buildReviewSignatureSnapshot(reqBO, event, signatureId));
    }

    private String buildReviewSignatureComment(MesTeamLeaderSubmissionReviewReqBO reqBO,
                                               MesProProcessPoolEventDO event) {
        return "组长复核:" + reqBO.getLeaderType() + ":" + event.getId() + ":" + reqBO.getReviewStatus();
    }

    private String buildReviewSignatureSnapshot(MesTeamLeaderSubmissionReviewReqBO reqBO,
                                                MesProProcessPoolEventDO event,
                                                Long signatureId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("signatureId", signatureId);
        payload.put("actorId", reqBO.getLeaderUserId());
        payload.put("actionType", MesProBatchRecordExecutionSignatureService.ACTION_TEAM_LEADER_REVIEW);
        payload.put("processPoolEventId", event.getId());
        payload.put("eventType", event.getEventType());
        payload.put("leaderType", reqBO.getLeaderType());
        payload.put("reviewStatus", reqBO.getReviewStatus());
        return JsonUtils.toJsonString(payload);
    }

    private record ReviewSignaturePayload(Long reviewSignatureId,
                                          Long reviewSignatureUserId,
                                          String reviewSignatureSnapshotJson) {
    }

}
