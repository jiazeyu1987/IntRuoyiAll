package cn.iocoder.yudao.module.showroom.workflow.service;

import cn.iocoder.yudao.module.showroom.content.model.ShowroomCompanySnapshot;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductRevision;
import cn.iocoder.yudao.module.showroom.content.service.ShowroomPersistentContentService;
import cn.iocoder.yudao.module.showroom.foundation.contract.ShowroomNotifyPrerequisiteChecker;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomChangeRequest;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi;
import cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserReqDTO;
import cn.iocoder.yudao.module.system.dal.mysql.notify.NotifyMessageMapper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ShowroomWorkflowNotifyService {

    static final String PENDING_TEMPLATE_CODE = "SHOWROOM_APPROVAL_PENDING";
    static final String PUBLISHED_TEMPLATE_CODE = "SHOWROOM_APPROVAL_PUBLISHED";
    static final String REJECTED_TEMPLATE_CODE = "SHOWROOM_APPROVAL_REJECTED";

    private static final String STATUS_PENDING_SUPERVISOR_REVIEW = "PENDING_SUPERVISOR_REVIEW";
    private static final String STATUS_PENDING_GAOXIN_APPROVAL = "PENDING_GAOXIN_APPROVAL";
    private static final String TARGET_PRODUCT = "PRODUCT";
    private static final String TARGET_COMPANY = "COMPANY";

    private final ShowroomPersistentContentService contentService;
    private final NotifyMessageSendApi notifyMessageSendApi;
    private final NotifyMessageMapper notifyMessageMapper;

    public ShowroomWorkflowNotifyService(ShowroomPersistentContentService contentService,
                                         NotifyMessageSendApi notifyMessageSendApi,
                                         NotifyMessageMapper notifyMessageMapper) {
        this.contentService = contentService;
        this.notifyMessageSendApi = notifyMessageSendApi;
        this.notifyMessageMapper = notifyMessageMapper;
    }

    public void notifyPendingApproval(ShowroomChangeRequest request) {
        requireNonNull(request, "SHOWROOM_NOTIFY_SEND_FAILED: change request is required");
        if (STATUS_PENDING_SUPERVISOR_REVIEW.equals(request.status())) {
            send(request.supervisorUserId(), PENDING_TEMPLATE_CODE, buildTemplateParams(request, "主管审核", "list"));
            return;
        }
        if (STATUS_PENDING_GAOXIN_APPROVAL.equals(request.status())) {
            send(request.gaoxinUserId(), PENDING_TEMPLATE_CODE, buildTemplateParams(request, "企宣审批", "list"));
            return;
        }
        throw new IllegalStateException("SHOWROOM_NOTIFY_SEND_FAILED: unsupported approval notify status "
                + request.status());
    }

    public void notifyPublished(ShowroomChangeRequest request) {
        requireNonNull(request, "SHOWROOM_NOTIFY_SEND_FAILED: change request is required");
        send(request.submittedBy(), PUBLISHED_TEMPLATE_CODE, buildTemplateParams(request, "已发布", "list"));
    }

    public void notifyRejected(ShowroomChangeRequest request, String approvalStage) {
        requireNonNull(request, "SHOWROOM_NOTIFY_SEND_FAILED: change request is required");
        send(request.submittedBy(), REJECTED_TEMPLATE_CODE, buildTemplateParams(request, approvalStage, "edit"));
    }

    private Map<String, Object> buildTemplateParams(ShowroomChangeRequest request, String approvalStage,
                                                    String notifyOpen) {
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("targetTypeText", resolveTargetTypeText(request.targetType()));
        params.put("targetName", resolveTargetName(request));
        params.put("approvalStage", approvalStage);
        params.put("rejectionReason", request.rejectionReason());
        params.put("targetType", request.targetType());
        params.put("targetId", request.targetId());
        params.put("changeRequestId", request.changeRequestId());
        params.put("notifyTargetType", request.targetType());
        params.put("notifyTargetId", request.targetId());
        params.put("notifyChangeRequestId", request.changeRequestId());
        params.put("notifyOpen", notifyOpen);
        return params;
    }

    private String resolveTargetName(ShowroomChangeRequest request) {
        if (TARGET_PRODUCT.equals(request.targetType())) {
            ShowroomProductRevision revision = contentService.getProductRevision(request.targetRevisionId());
            return revision.nameCn();
        }
        if (TARGET_COMPANY.equals(request.targetType())) {
            ShowroomCompanySnapshot company = contentService.getCompany(request.targetId());
            return company.displayName();
        }
        throw new IllegalStateException("SHOWROOM_NOTIFY_SEND_FAILED: unsupported notify target "
                + request.targetType());
    }

    private static String resolveTargetTypeText(String targetType) {
        if (TARGET_PRODUCT.equals(targetType)) {
            return "产品";
        }
        if (TARGET_COMPANY.equals(targetType)) {
            return "公司";
        }
        return targetType;
    }

    private void send(Long recipientUserId, String templateCode, Map<String, Object> templateParams) {
        NotifySendSingleToUserReqDTO reqDTO = new NotifySendSingleToUserReqDTO();
        reqDTO.setUserId(recipientUserId);
        reqDTO.setTemplateCode(templateCode);
        reqDTO.setTemplateParams(templateParams);
        try {
            Long notifyMessageId = notifyMessageSendApi.sendSingleMessageToAdmin(reqDTO);
            ShowroomNotifyPrerequisiteChecker.validateBeforeSend(recipientUserId, templateCode, notifyMessageId);
            if (notifyMessageMapper.selectById(notifyMessageId) == null) {
                throw new IllegalStateException("SHOWROOM_NOTIFY_SEND_FAILED: persisted notify message is required");
            }
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new IllegalStateException("SHOWROOM_NOTIFY_SEND_FAILED: " + ex.getMessage(), ex);
        }
    }

    private static void requireNonNull(Object value, String message) {
        if (value == null) {
            throw new IllegalStateException(message);
        }
    }

}
