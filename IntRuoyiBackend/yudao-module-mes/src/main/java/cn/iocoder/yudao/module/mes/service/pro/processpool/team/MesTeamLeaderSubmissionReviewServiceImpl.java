package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REVISION_EVENT_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_SUBMISSION_REVIEW_STATUS_INVALID;

@Service
@Validated
public class MesTeamLeaderSubmissionReviewServiceImpl implements MesTeamLeaderSubmissionReviewService {

    private static final Set<String> VALID_REVIEW_STATUSES = Set.of(
            MesProcessPoolSubmissionReviewDO.STATUS_APPROVED,
            MesProcessPoolSubmissionReviewDO.STATUS_REJECTED);

    private final MesTeamLeaderScopeService scopeService;
    private final MesProProcessPoolEventMapper eventMapper;
    private final MesProcessPoolSubmissionReviewMapper reviewMapper;

    public MesTeamLeaderSubmissionReviewServiceImpl(MesTeamLeaderScopeService scopeService,
                                                    MesProProcessPoolEventMapper eventMapper,
                                                    MesProcessPoolSubmissionReviewMapper reviewMapper) {
        this.scopeService = scopeService;
        this.eventMapper = eventMapper;
        this.reviewMapper = reviewMapper;
    }

    @Override
    public Long reviewSubmission(MesTeamLeaderSubmissionReviewReqBO reqBO) {
        validateReq(reqBO);
        MesProProcessPoolEventDO event = eventMapper.selectById(reqBO.getEventId());
        if (event == null) {
            throw exception(PRO_PROCESS_POOL_REVISION_EVENT_NOT_EXISTS, reqBO.getEventId());
        }
        scopeService.assertCanAccessEmployee(reqBO.getLeaderUserId(), reqBO.getLeaderType(),
                event.getActualEmployeeId());
        MesProcessPoolSubmissionReviewDO review = MesProcessPoolSubmissionReviewDO.builder()
                .eventId(reqBO.getEventId())
                .leaderUserId(reqBO.getLeaderUserId())
                .reviewStatus(reqBO.getReviewStatus())
                .reviewRemark(reqBO.getReviewRemark())
                .reviewedAt(LocalDateTime.now())
                .build();
        reviewMapper.insert(review);
        return review.getId();
    }

    private void validateReq(MesTeamLeaderSubmissionReviewReqBO reqBO) {
        if (reqBO == null || reqBO.getEventId() == null || reqBO.getLeaderUserId() == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "submissionReview");
        }
        if (!VALID_REVIEW_STATUSES.contains(reqBO.getReviewStatus())) {
            throw exception(PRO_PROCESS_POOL_SUBMISSION_REVIEW_STATUS_INVALID, reqBO.getReviewStatus());
        }
    }
}
