package cn.iocoder.yudao.module.bpm.approval.service.ownership;

public record EngineApprovalIdentity(Long tenantId, String taskId, String processInstanceId,
        String processDefinitionId, String processDefinitionKey, String businessKey, String taskDefinitionKey) {
}
