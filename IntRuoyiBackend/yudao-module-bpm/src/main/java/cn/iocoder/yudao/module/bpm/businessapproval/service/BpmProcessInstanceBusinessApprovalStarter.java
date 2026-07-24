package cn.iocoder.yudao.module.bpm.businessapproval.service;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalException;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequest;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class BpmProcessInstanceBusinessApprovalStarter implements BusinessApprovalBpmStarter {

    @Resource
    private BpmProcessInstanceApi processInstanceApi;

    @Override
    public String start(BusinessApprovalRequest request, String processDefinitionKey, Map<String, Object> variables) {
        if (request == null || request.getContext() == null || request.getContext().getApplicantUserId() == null) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_CONTEXT_INVALID,
                    "Business approval applicant user is required");
        }
        if (StrUtil.isBlank(processDefinitionKey)) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_PROCESS_DEFINITION_MISSING,
                    "BPM process definition key is required");
        }

        BpmProcessInstanceCreateReqDTO reqDTO = new BpmProcessInstanceCreateReqDTO();
        reqDTO.setProcessDefinitionKey(processDefinitionKey);
        reqDTO.setBusinessKey("BUSINESS_APPROVAL:" + request.getRequestId());
        reqDTO.setVariables(variables == null ? Map.of() : new LinkedHashMap<>(variables));
        reqDTO.setStartUserSelectAssignees(request.getContext().getStartUserSelectAssignees());
        return processInstanceApi.createProcessInstance(request.getContext().getApplicantUserId(), reqDTO);
    }

    @Override
    public void cancel(BusinessApprovalRequest request, String processInstanceId, String reason) {
        if (request == null || request.getContext() == null || request.getContext().getApplicantUserId() == null) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_CONTEXT_INVALID,
                    "Business approval applicant user is required");
        }
        if (StrUtil.isBlank(processInstanceId)) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_PROCESS_NOT_STARTED,
                    "BPM process instance id is required for cancellation");
        }
        processInstanceApi.cancelProcessInstance(request.getContext().getApplicantUserId(),
                StrUtil.trim(processInstanceId), reason);
    }

}
