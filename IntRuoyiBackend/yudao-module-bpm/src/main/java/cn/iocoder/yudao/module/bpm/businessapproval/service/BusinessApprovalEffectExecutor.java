package cn.iocoder.yudao.module.bpm.businessapproval.service;

import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalContext;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalEffectResult;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequest;

public interface BusinessApprovalEffectExecutor {

    String getExecutorCode();

    default String getBpmProcessDefinitionKey() {
        return null;
    }

    void precheck(BusinessApprovalContext context);

    BusinessApprovalEffectResult executeDirect(BusinessApprovalContext context, BusinessApprovalRequest request);

    BusinessApprovalEffectResult markPending(BusinessApprovalContext context, BusinessApprovalRequest request);

    BusinessApprovalEffectResult executeApproved(BusinessApprovalContext context,
                                                 BusinessApprovalRequest request,
                                                 Long actorUserId);

    BusinessApprovalEffectResult reject(BusinessApprovalContext context,
                                        BusinessApprovalRequest request,
                                        Long actorUserId,
                                        String reason);

    BusinessApprovalEffectResult cancel(BusinessApprovalContext context,
                                        BusinessApprovalRequest request,
                                        Long actorUserId,
                                        String reason);

}
