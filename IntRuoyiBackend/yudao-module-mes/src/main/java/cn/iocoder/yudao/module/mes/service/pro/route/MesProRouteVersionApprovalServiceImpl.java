package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Locale;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_VERSION_APPROVAL_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_VERSION_APPROVAL_RESULT_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE;

@Service
@Validated
public class MesProRouteVersionApprovalServiceImpl implements MesProRouteVersionApprovalService {

    private static final String APPROVED = "APPROVED";
    private static final String REJECTED = "REJECTED";
    private static final String CANCELED = "CANCELED";
    private static final String PROCESSED = "PROCESSED";
    private static final String DUPLICATE = "DUPLICATE";

    @Resource
    private MesProRouteVersionMapper routeVersionMapper;
    @Resource
    private MesProRouteVersionLifecycleService lifecycleService;
    @Resource
    private MesProRouteControlledContentAdapter platformAdapter;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProRouteVersionApprovalResult handleApprovalCallback(String approvalProcessInstanceId,
                                                                   String approvalEventId,
                                                                   String approvalResult,
                                                                   String rejectReason,
                                                                   Long actorUserId) {
        String normalizedApprovalResult = normalizeApprovalResult(approvalResult);
        MesProRouteVersionDO candidate = routeVersionMapper.selectByApprovalProcessInstanceId(approvalProcessInstanceId);
        if (candidate == null) {
            if (CANCELED.equals(normalizedApprovalResult)) {
                return result(null, approvalEventId, normalizedApprovalResult, DUPLICATE);
            }
            throw exception(PRO_ROUTE_VERSION_APPROVAL_NOT_EXISTS, approvalProcessInstanceId);
        }
        if (APPROVED.equals(normalizedApprovalResult)) {
            return approveCandidate(candidate, approvalEventId, normalizedApprovalResult, actorUserId);
        }
        if (CANCELED.equals(normalizedApprovalResult)) {
            return cancelCandidate(candidate, approvalEventId, normalizedApprovalResult, actorUserId);
        }
        return rejectCandidate(candidate, approvalEventId, normalizedApprovalResult, rejectReason, actorUserId);
    }

    private MesProRouteVersionApprovalResult approveCandidate(MesProRouteVersionDO candidate,
                                                              String approvalEventId,
                                                              String approvalResult,
                                                              Long actorUserId) {
        if (MesProRouteVersionLifecycleServiceImpl.STATUS_ACTIVE.equals(candidate.getLifecycleStatus())) {
            return result(candidate, approvalEventId, approvalResult, DUPLICATE);
        }
        Long publisherUserId = Objects.requireNonNull(actorUserId, "ROUTE_VERSION_APPROVAL_ACTOR_REQUIRED");
        requirePendingApproval(candidate);
        MesProRouteVersionDO update = new MesProRouteVersionDO();
        update.setId(candidate.getId());
        update.setLifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_READY_TO_PUBLISH);
        routeVersionMapper.updateById(update);
        candidate.setLifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_READY_TO_PUBLISH);
        platformAdapter.recordApproved(candidate, actorUserId, approvalEventId);
        MesProRouteVersionDO published = lifecycleService.publishCandidate(candidate.getId(), publisherUserId);
        return result(published, approvalEventId, approvalResult, PROCESSED);
    }

    private MesProRouteVersionApprovalResult rejectCandidate(MesProRouteVersionDO candidate,
                                                             String approvalEventId,
                                                             String approvalResult,
                                                             String rejectReason,
                                                             Long actorUserId) {
        if (MesProRouteVersionLifecycleServiceImpl.STATUS_REJECTED.equals(candidate.getLifecycleStatus())) {
            return result(candidate, approvalEventId, approvalResult, DUPLICATE);
        }
        requirePendingApproval(candidate);
        MesProRouteVersionDO update = new MesProRouteVersionDO();
        update.setId(candidate.getId());
        update.setLifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_REJECTED);
        update.setRemark(StrUtil.trimToNull(rejectReason));
        routeVersionMapper.updateById(update);
        candidate.setLifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_REJECTED);
        candidate.setRemark(update.getRemark());
        platformAdapter.recordRejected(candidate, actorUserId, update.getRemark(), approvalEventId);
        return result(candidate, approvalEventId, approvalResult, PROCESSED);
    }

    private MesProRouteVersionApprovalResult cancelCandidate(MesProRouteVersionDO candidate,
                                                             String approvalEventId,
                                                             String approvalResult,
                                                             Long actorUserId) {
        if (MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT.equals(candidate.getLifecycleStatus())) {
            routeVersionMapper.updateApprovalFieldsToDraft(candidate.getId());
            candidate.setSubmittedBy(null);
            candidate.setSubmittedTime(null);
            candidate.setApprovalProcessInstanceId(null);
            return result(candidate, approvalEventId, approvalResult, DUPLICATE);
        }
        requirePendingApproval(candidate);
        routeVersionMapper.updateApprovalFieldsToDraft(candidate.getId());
        platformAdapter.recordWithdrawn(candidate, actorUserId);
        candidate.setLifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT);
        candidate.setSubmittedBy(null);
        candidate.setSubmittedTime(null);
        candidate.setApprovalProcessInstanceId(null);
        return result(candidate, approvalEventId, approvalResult, PROCESSED);
    }

    private void requirePendingApproval(MesProRouteVersionDO candidate) {
        if (!Objects.equals(MesProRouteVersionLifecycleServiceImpl.STATUS_PENDING_APPROVAL,
                candidate.getLifecycleStatus())) {
            throw exception(PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE,
                    candidate.getId(), candidate.getLifecycleStatus());
        }
    }

    private String normalizeApprovalResult(String approvalResult) {
        String normalized = StrUtil.trimToEmpty(approvalResult).toUpperCase(Locale.ROOT);
        if (!APPROVED.equals(normalized) && !REJECTED.equals(normalized) && !CANCELED.equals(normalized)) {
            throw exception(PRO_ROUTE_VERSION_APPROVAL_RESULT_INVALID, approvalResult);
        }
        return normalized;
    }

    private MesProRouteVersionApprovalResult result(MesProRouteVersionDO candidate,
                                                    String approvalEventId,
                                                    String approvalResult,
                                                    String processedResult) {
        return MesProRouteVersionApprovalResult.builder()
                .routeVersionId(candidate == null ? null : candidate.getId())
                .lifecycleStatus(candidate == null ? null : candidate.getLifecycleStatus())
                .approvalProcessInstanceId(candidate == null ? null : candidate.getApprovalProcessInstanceId())
                .approvalEventId(approvalEventId)
                .approvalResult(approvalResult)
                .processedResult(processedResult)
                .build();
    }
}
