package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalContext;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalEffectResult;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalException;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequest;
import cn.iocoder.yudao.module.bpm.businessapproval.service.BusinessApprovalEffectExecutor;
import cn.iocoder.yudao.module.bpm.businessapproval.service.BusinessApprovalErrorCode;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@Transactional(rollbackFor = Exception.class)
public class MesProRouteVersionBusinessApprovalEffectExecutor implements BusinessApprovalEffectExecutor {

    public static final String EXECUTOR_CODE = "MES_ROUTE_VERSION_PUBLISH";
    public static final String PROCESS_DEFINITION_KEY = "mes-route-version-approval-v1";
    private static final String OBJECT_TYPE = "ROUTE_VERSION";
    private static final String ACTION_CODE = "PUBLISH";

    @Resource
    private MesProRouteVersionLifecycleService lifecycleService;
    @Resource
    private MesProRouteVersionMapper routeVersionMapper;
    @Resource
    private MesProRouteControlledContentAdapter platformAdapter;

    @Override
    public String getExecutorCode() {
        return EXECUTOR_CODE;
    }

    public String getBpmProcessDefinitionKey() {
        return PROCESS_DEFINITION_KEY;
    }

    @Override
    public void precheck(BusinessApprovalContext context) {
        requireRouteVersionPublishContext(context);
    }

    @Override
    public BusinessApprovalEffectResult executeDirect(BusinessApprovalContext context,
                                                      BusinessApprovalRequest request) {
        requireRouteVersionPublishContext(context);
        MesProRouteVersionDO candidate = requireVersionWithStatus(context,
                MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT);
        Long applicantUserId = requireActorUserId(context.getApplicantUserId(), "applicantUserId");
        platformAdapter.recordSubmitted(candidate, applicantUserId, null);
        LocalDateTime submittedTime = LocalDateTime.now();
        MesProRouteVersionDO update = new MesProRouteVersionDO();
        update.setId(candidate.getId());
        update.setLifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_READY_TO_PUBLISH);
        update.setSubmittedBy(applicantUserId);
        update.setSubmittedTime(submittedTime);
        update.setApprovalProcessInstanceId(null);
        requireUpdated(routeVersionMapper.updateById(update), "direct approve route version");
        candidate.setLifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_READY_TO_PUBLISH);
        candidate.setSubmittedBy(applicantUserId);
        candidate.setSubmittedTime(submittedTime);
        candidate.setApprovalProcessInstanceId(null);
        platformAdapter.recordApproved(candidate, applicantUserId, directEventKey(request));
        MesProRouteVersionDO published = lifecycleService.publishCandidate(parseRouteVersionId(context), applicantUserId);
        return BusinessApprovalEffectResult.completed(published.getLifecycleStatus());
    }

    @Override
    public BusinessApprovalEffectResult markPending(BusinessApprovalContext context,
                                                    BusinessApprovalRequest request) {
        requireRouteVersionPublishContext(context);
        MesProRouteVersionDO candidate = requireVersionWithStatus(context,
                MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT);
        Long applicantUserId = requireActorUserId(context.getApplicantUserId(), "applicantUserId");
        String processInstanceId = requireProcessInstanceId(request);
        MesProRouteVersionDO update = new MesProRouteVersionDO();
        update.setId(candidate.getId());
        update.setLifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_PENDING_APPROVAL);
        update.setSubmittedBy(applicantUserId);
        update.setSubmittedTime(LocalDateTime.now());
        update.setApprovalProcessInstanceId(processInstanceId);
        platformAdapter.recordSubmitted(candidate, applicantUserId, processInstanceId);
        requireUpdated(routeVersionMapper.updateById(update), "mark pending route version");
        candidate.setLifecycleStatus(update.getLifecycleStatus());
        candidate.setSubmittedBy(update.getSubmittedBy());
        candidate.setSubmittedTime(update.getSubmittedTime());
        candidate.setApprovalProcessInstanceId(update.getApprovalProcessInstanceId());
        return BusinessApprovalEffectResult.pending(MesProRouteVersionLifecycleServiceImpl.STATUS_PENDING_APPROVAL);
    }

    @Override
    public BusinessApprovalEffectResult executeApproved(BusinessApprovalContext context,
                                                        BusinessApprovalRequest request,
                                                        Long actorUserId) {
        requireRouteVersionPublishContext(context);
        MesProRouteVersionDO candidate = requireVersionWithStatus(context,
                MesProRouteVersionLifecycleServiceImpl.STATUS_PENDING_APPROVAL);
        Long publisherUserId = requireActorUserId(actorUserId, "actorUserId");
        MesProRouteVersionDO update = new MesProRouteVersionDO();
        update.setId(candidate.getId());
        update.setLifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_READY_TO_PUBLISH);
        requireUpdated(routeVersionMapper.updateById(update), "approve route version");
        candidate.setLifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_READY_TO_PUBLISH);
        platformAdapter.recordApproved(candidate, publisherUserId, terminalEventKey(request, "APPROVED"));
        MesProRouteVersionDO published = lifecycleService.publishCandidate(parseRouteVersionId(context),
                publisherUserId);
        return BusinessApprovalEffectResult.completed(published.getLifecycleStatus());
    }

    @Override
    public BusinessApprovalEffectResult reject(BusinessApprovalContext context,
                                               BusinessApprovalRequest request,
                                               Long actorUserId,
                                               String reason) {
        requireRouteVersionPublishContext(context);
        MesProRouteVersionDO candidate = requireVersionWithStatus(context,
                MesProRouteVersionLifecycleServiceImpl.STATUS_PENDING_APPROVAL);
        Long rejectUserId = requireActorUserId(actorUserId, "actorUserId");
        String rejectReason = StrUtil.trimToNull(reason);
        MesProRouteVersionDO update = new MesProRouteVersionDO();
        update.setId(candidate.getId());
        update.setLifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_REJECTED);
        update.setRemark(rejectReason);
        requireUpdated(routeVersionMapper.updateById(update), "reject route version");
        candidate.setLifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_REJECTED);
        candidate.setRemark(rejectReason);
        platformAdapter.recordRejected(candidate, rejectUserId, rejectReason, terminalEventKey(request, "REJECTED"));
        return BusinessApprovalEffectResult.rejected(MesProRouteVersionLifecycleServiceImpl.STATUS_REJECTED);
    }

    @Override
    public BusinessApprovalEffectResult cancel(BusinessApprovalContext context,
                                               BusinessApprovalRequest request,
                                               Long actorUserId,
                                               String reason) {
        requireRouteVersionPublishContext(context);
        MesProRouteVersionDO candidate = requireVersionWithStatus(context,
                MesProRouteVersionLifecycleServiceImpl.STATUS_PENDING_APPROVAL);
        Long cancelUserId = requireActorUserId(actorUserId, "actorUserId");
        requireUpdated(routeVersionMapper.updateApprovalFieldsToDraft(candidate.getId()), "cancel route version");
        platformAdapter.recordWithdrawn(candidate, cancelUserId);
        return BusinessApprovalEffectResult.cancelled(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT);
    }

    private void requireRouteVersionPublishContext(BusinessApprovalContext context) {
        if (context == null
                || !OBJECT_TYPE.equals(context.getObjectType())
                || !ACTION_CODE.equals(context.getActionCode())
                || !MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT.equals(context.getObjectState())) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_CONTEXT_INVALID,
                    "MES route version publish approval context is invalid");
        }
        parseRouteVersionId(context);
    }

    private Long parseRouteVersionId(BusinessApprovalContext context) {
        try {
            return Long.valueOf(context.getObjectId());
        } catch (RuntimeException ex) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_CONTEXT_INVALID,
                    "MES route version id is invalid: " + (context == null ? null : context.getObjectId()));
        }
    }

    private MesProRouteVersionDO requireVersionWithStatus(BusinessApprovalContext context, String expectedStatus) {
        Long routeVersionId = parseRouteVersionId(context);
        MesProRouteVersionDO candidate = routeVersionMapper.selectById(routeVersionId);
        if (candidate == null) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_CONTEXT_INVALID,
                    "MES route version does not exist: " + routeVersionId);
        }
        if (Boolean.TRUE.equals(candidate.getActive()) || !expectedStatus.equals(candidate.getLifecycleStatus())) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_CONTEXT_INVALID,
                    "MES route version status is invalid: routeVersionId=" + routeVersionId
                            + ", expected=" + expectedStatus + ", actual=" + candidate.getLifecycleStatus());
        }
        return candidate;
    }

    private Long requireActorUserId(Long actorUserId, String fieldName) {
        if (actorUserId == null) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_CONTEXT_INVALID,
                    "MES route version publisher is required: " + fieldName);
        }
        return actorUserId;
    }

    private String requireProcessInstanceId(BusinessApprovalRequest request) {
        String processInstanceId = request == null ? null : request.getProcessInstanceId();
        if (StrUtil.isBlank(processInstanceId)) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_PROCESS_NOT_STARTED,
                    "MES route version approval process instance is required");
        }
        return StrUtil.trim(processInstanceId);
    }

    private String terminalEventKey(BusinessApprovalRequest request, String terminalStatus) {
        return "BUSINESS_APPROVAL:" + requireProcessInstanceId(request) + ":" + terminalStatus;
    }

    private String directEventKey(BusinessApprovalRequest request) {
        if (request == null || request.getRequestId() == null) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_CONTEXT_INVALID,
                    "MES route version direct approval request id is required");
        }
        return "BUSINESS_APPROVAL:" + request.getRequestId() + ":DIRECT_APPROVED";
    }

    private void requireUpdated(int updated, String action) {
        if (updated <= 0) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_CONTEXT_INVALID,
                    "MES route version update failed: " + action);
        }
    }

}
