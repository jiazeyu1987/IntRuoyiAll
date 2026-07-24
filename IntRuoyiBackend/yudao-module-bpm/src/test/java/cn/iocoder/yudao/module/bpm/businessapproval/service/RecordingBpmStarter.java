package cn.iocoder.yudao.module.bpm.businessapproval.service;

import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequest;

import java.util.Map;

final class RecordingBpmStarter implements BusinessApprovalBpmStarter {

    private final String processInstanceId;
    private int startCount;
    private int cancelCount;
    private String lastProcessDefinitionKey;
    private String lastCancelledProcessInstanceId;
    private Map<String, Object> lastVariables = Map.of();

    RecordingBpmStarter(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    @Override
    public String start(BusinessApprovalRequest request, String processDefinitionKey, Map<String, Object> variables) {
        startCount++;
        lastProcessDefinitionKey = processDefinitionKey;
        lastVariables = Map.copyOf(variables);
        return processInstanceId;
    }

    @Override
    public void cancel(BusinessApprovalRequest request, String processInstanceId, String reason) {
        cancelCount++;
        lastCancelledProcessInstanceId = processInstanceId;
    }

    int getStartCount() {
        return startCount;
    }

    String getLastProcessDefinitionKey() {
        return lastProcessDefinitionKey;
    }

    Map<String, Object> getLastVariables() {
        return lastVariables;
    }

    int getCancelCount() {
        return cancelCount;
    }

    String getLastCancelledProcessInstanceId() {
        return lastCancelledProcessInstanceId;
    }

}
