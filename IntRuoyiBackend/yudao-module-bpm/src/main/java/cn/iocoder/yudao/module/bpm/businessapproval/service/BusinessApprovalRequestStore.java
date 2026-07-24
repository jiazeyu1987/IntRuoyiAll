package cn.iocoder.yudao.module.bpm.businessapproval.service;

import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalContext;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalPolicy;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequest;

import java.util.Optional;

public interface BusinessApprovalRequestStore {

    BusinessApprovalRequest createDirectRequest(BusinessApprovalContext context, BusinessApprovalPolicy policy);

    BusinessApprovalRequest createPendingRequest(BusinessApprovalContext context, BusinessApprovalPolicy policy);

    BusinessApprovalRequest attachProcessInstance(Long requestId, String processInstanceId);

    BusinessApprovalRequest update(BusinessApprovalRequest request);

    Optional<BusinessApprovalRequest> findByProcessInstanceId(String processInstanceId);

    Optional<BusinessApprovalRequest> findPendingByBusinessAction(BusinessApprovalContext context);

    boolean hasPendingRequest(BusinessApprovalContext context);

}
