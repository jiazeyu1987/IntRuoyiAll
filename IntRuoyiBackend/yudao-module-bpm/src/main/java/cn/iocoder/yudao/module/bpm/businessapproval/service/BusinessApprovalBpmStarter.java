package cn.iocoder.yudao.module.bpm.businessapproval.service;

import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequest;

import java.util.Map;

public interface BusinessApprovalBpmStarter {

    String start(BusinessApprovalRequest request, String processDefinitionKey, Map<String, Object> variables);

    void cancel(BusinessApprovalRequest request, String processInstanceId, String reason);

}
