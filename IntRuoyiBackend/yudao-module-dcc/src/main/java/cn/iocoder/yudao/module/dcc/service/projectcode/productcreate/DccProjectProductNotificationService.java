package cn.iocoder.yudao.module.dcc.service.projectcode.productcreate;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectProductCreateRequestDO;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi;
import cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserIdempotentReqDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.LinkedHashMap;
import java.util.Objects;

/** Official station-message INSERT participates in the request/Gxp physical transaction. */
@Service
public class DccProjectProductNotificationService {
    public static final String TEMPLATE_CODE = "dcc-project-product-application-event";
    @Resource private NotifyMessageSendApi messages;
    @Resource private DccProjectProductActorSupport actors;

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void reviewTodo(DccProjectProductCreateRequestDO request, String submittedReason) {
        if (!actors.canUpdate(request.getConfiguredReviewerUserId())) throw new IllegalStateException("PROJECT_REVIEWER_PERMISSION_REQUIRED");
        send(request, request.getConfiguredReviewerUserId(), "REVIEW_TODO", "项目产品审核待办", submittedReason);
    }

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void approvalTodo(DccProjectProductCreateRequestDO request) {
        send(request, actors.requireApprover(), "APPROVAL_TODO", "项目产品批准待办", request.getReviewReason());
    }

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void rejected(DccProjectProductCreateRequestDO request, boolean atApproval) {
        send(request, request.getApplicantUserId(), atApproval ? "APPROVAL_REJECTED" : "REVIEW_REJECTED",
                atApproval ? "项目产品批准驳回" : "项目产品审核驳回", request.getRejectReason());
    }

    private void send(DccProjectProductCreateRequestDO request, Long recipient, String event, String name, String reason) {
        Long tenant = TenantContextHolder.getRequiredTenantId();
        actors.requireReadableAccount(recipient);
        if (request.getId() == null || request.getId() <= 0 || !Objects.equals(request.getTenantId(), tenant)
                || reason == null || reason.isBlank()) throw new IllegalStateException("PROJECT_NOTIFICATION_CONTEXT_INVALID");
        var params = new LinkedHashMap<String, Object>();
        params.put("businessTitle", request.getProjectName() + " · " + request.getProductName());
        params.put("businessCode", request.getProjectCode());
        params.put("eventName", name);
        params.put("reason", reason);
        params.put("notifyTargetType", "DCC_PROJECT_PRODUCT_REQUEST");
        params.put("notifyTargetId", request.getId().toString());
        params.put("actionUrl", "/mdm/product-catalog?requestId=" + request.getId() + "&requestOpen=records&from=notification");
        var command = new NotifySendSingleToUserIdempotentReqDTO();
        command.setUserId(recipient);command.setTemplateCode(TEMPLATE_CODE);command.setTemplateParams(params);
        command.setBusinessKey("DCC_PROJECT_PRODUCT:" + tenant + ":" + request.getId() + ":" + event + ":" + recipient);
        Long id = messages.sendSingleMessageIdempotentlyToAdmin(command);
        if (id == null || id <= 0) throw new IllegalStateException("PROJECT_NOTIFICATION_MESSAGE_ID_REQUIRED");
    }
}
