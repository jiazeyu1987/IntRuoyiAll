package cn.iocoder.yudao.module.bpm.businessapproval.service;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class BusinessApprovalEffectExecutorRegistry {

    private static final Map<String, String> DEFAULT_BPM_PROCESS_DEFINITION_KEYS = Map.of(
            "FORM_TEMPLATE_OBSOLETE", "form-template-obsolete-v1",
            "FORM_TEMPLATE_UPGRADE", "form-template-upgrade-v1",
            "MES_ROUTE_VERSION_PUBLISH", "mes-route-version-approval-v1",
            "MES_BATCH_RECORD_VERSION_PUBLISH", "mes-batch-record-version-approval-v1",
            "EDHR_BATCH_EXECUTION_SUBMIT_REVIEW", "mes-edhr-approval-v1");

    private final Map<String, BusinessApprovalEffectExecutor> executors;

    public BusinessApprovalEffectExecutorRegistry(List<BusinessApprovalEffectExecutor> executorList) {
        Map<String, BusinessApprovalEffectExecutor> byCode = new LinkedHashMap<>();
        for (BusinessApprovalEffectExecutor executor : executorList == null
                ? List.<BusinessApprovalEffectExecutor>of() : executorList) {
            String executorCode = executor == null ? null : executor.getExecutorCode();
            if (executorCode == null || executorCode.isBlank()) {
                throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_EXECUTOR_MISSING,
                        "Business approval executor code is required");
            }
            BusinessApprovalEffectExecutor duplicated = byCode.putIfAbsent(executorCode, executor);
            if (duplicated != null) {
                throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_EXECUTOR_CONFLICT,
                        "Business approval executor duplicated: " + executorCode);
            }
        }
        this.executors = Map.copyOf(byCode);
    }

    public BusinessApprovalEffectExecutor requireExecutor(String executorCode) {
        if (executorCode == null || executorCode.isBlank()) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_EXECUTOR_MISSING,
                    "Business approval executor code is required");
        }
        BusinessApprovalEffectExecutor executor = executors.get(executorCode);
        if (executor == null) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_EXECUTOR_MISSING,
                    "Business approval executor is not registered: " + executorCode);
        }
        return executor;
    }

    public String requireBpmProcessDefinitionKey(String executorCode) {
        BusinessApprovalEffectExecutor executor = requireExecutor(executorCode);
        String processDefinitionKey = StrUtil.trim(executor.getBpmProcessDefinitionKey());
        if (StrUtil.isBlank(processDefinitionKey)) {
            processDefinitionKey = DEFAULT_BPM_PROCESS_DEFINITION_KEYS.get(executorCode);
        }
        if (StrUtil.isBlank(processDefinitionKey)) {
            throw new BusinessApprovalException(
                    BusinessApprovalErrorCode.BUSINESS_APPROVAL_PROCESS_DEFINITION_MISSING,
                    "Business approval executor BPM process definition key is required: " + executorCode);
        }
        return processDefinitionKey;
    }

}
