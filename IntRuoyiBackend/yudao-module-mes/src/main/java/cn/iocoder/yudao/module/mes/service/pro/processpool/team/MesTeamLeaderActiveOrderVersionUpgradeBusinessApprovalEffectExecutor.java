package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalContext;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalEffectResult;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalException;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequest;
import cn.iocoder.yudao.module.bpm.businessapproval.service.BusinessApprovalEffectExecutor;
import cn.iocoder.yudao.module.bpm.businessapproval.service.BusinessApprovalErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(rollbackFor = Exception.class)
public class MesTeamLeaderActiveOrderVersionUpgradeBusinessApprovalEffectExecutor
        implements BusinessApprovalEffectExecutor {

    public static final String EXECUTOR_CODE = "MES_ACTIVE_ORDER_VERSION_UPGRADE_RESTART";
    public static final String PROCESS_DEFINITION_KEY = "mes-active-order-version-upgrade-v1";

    private static final String OBJECT_TYPE = "MES_ACTIVE_ORDER";
    private static final String ACTION_CODE = "VERSION_UPGRADE_RESTART";
    private static final String OBJECT_STATE = "VERSION_UPGRADE_PENDING";
    private static final String STATUS_PENDING = "PENDING_APPROVAL";
    private static final String STATUS_APPLIED = "APPLIED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_CANCELLED = "CANCELLED";

    private final MesTeamLeaderActiveOrderVersionUpgradeService versionUpgradeService;

    public MesTeamLeaderActiveOrderVersionUpgradeBusinessApprovalEffectExecutor(
            MesTeamLeaderActiveOrderVersionUpgradeService versionUpgradeService) {
        this.versionUpgradeService = versionUpgradeService;
    }

    @Override
    public String getExecutorCode() {
        return EXECUTOR_CODE;
    }

    @Override
    public String getBpmProcessDefinitionKey() {
        return PROCESS_DEFINITION_KEY;
    }

    @Override
    public void precheck(BusinessApprovalContext context) {
        parseRequestId(context);
    }

    @Override
    public BusinessApprovalEffectResult executeDirect(BusinessApprovalContext context,
                                                      BusinessApprovalRequest request) {
        throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_MODE_INVALID,
                "MES active-order version upgrade must use BPM approval");
    }

    @Override
    public BusinessApprovalEffectResult markPending(BusinessApprovalContext context,
                                                    BusinessApprovalRequest request) {
        Long requestId = parseRequestId(context);
        String processInstanceId = request == null ? null : request.getProcessInstanceId();
        if (StrUtil.isBlank(processInstanceId)) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_PROCESS_NOT_STARTED,
                    "MES active-order version-upgrade BPM process instance is required");
        }
        versionUpgradeService.markApprovalPending(requestId, StrUtil.trim(processInstanceId),
                context.getApplicantUserId());
        return BusinessApprovalEffectResult.pending(STATUS_PENDING);
    }

    @Override
    public BusinessApprovalEffectResult executeApproved(BusinessApprovalContext context,
                                                        BusinessApprovalRequest request,
                                                        Long actorUserId) {
        MesTeamLeaderActiveOrderVersionUpgradeApplyResult result =
                versionUpgradeService.applyApprovedUpgrade(parseRequestId(context), requireActorUserId(actorUserId));
        return BusinessApprovalEffectResult.completed(result == null ? STATUS_APPLIED : result.getRequestStatus());
    }

    @Override
    public BusinessApprovalEffectResult reject(BusinessApprovalContext context,
                                               BusinessApprovalRequest request,
                                               Long actorUserId,
                                               String reason) {
        versionUpgradeService.rejectOrCancelApproval(parseRequestId(context), requireActorUserId(actorUserId),
                reason, false);
        return BusinessApprovalEffectResult.rejected(STATUS_REJECTED);
    }

    @Override
    public BusinessApprovalEffectResult cancel(BusinessApprovalContext context,
                                               BusinessApprovalRequest request,
                                               Long actorUserId,
                                               String reason) {
        versionUpgradeService.rejectOrCancelApproval(parseRequestId(context), requireActorUserId(actorUserId),
                reason, true);
        return BusinessApprovalEffectResult.cancelled(STATUS_CANCELLED);
    }

    private Long parseRequestId(BusinessApprovalContext context) {
        if (context == null
                || !OBJECT_TYPE.equals(context.getObjectType())
                || !ACTION_CODE.equals(context.getActionCode())
                || !OBJECT_STATE.equals(context.getObjectState())
                || StrUtil.isBlank(context.getObjectId())) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_CONTEXT_INVALID,
                    "MES active-order version-upgrade approval context is invalid");
        }
        try {
            return Long.valueOf(StrUtil.trim(context.getObjectId()));
        } catch (RuntimeException ex) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_CONTEXT_INVALID,
                    "MES active-order version-upgrade request id is invalid: " + context.getObjectId());
        }
    }

    private Long requireActorUserId(Long actorUserId) {
        if (actorUserId == null) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_CONTEXT_INVALID,
                    "MES active-order version-upgrade actor user is required");
        }
        return actorUserId;
    }
}
