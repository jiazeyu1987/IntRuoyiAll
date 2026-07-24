package cn.iocoder.yudao.module.bpm.businessapproval.model;

import lombok.Getter;

@Getter
public class BusinessApprovalPolicyResolution {

    private final BusinessApprovalPolicy policy;

    private BusinessApprovalPolicyResolution(BusinessApprovalPolicy policy) {
        this.policy = policy;
    }

    public static BusinessApprovalPolicyResolution from(BusinessApprovalPolicy policy) {
        return new BusinessApprovalPolicyResolution(policy);
    }

    public Long getPolicyId() {
        return policy.getPolicyId();
    }

    public BusinessApprovalPolicyMode getMode() {
        return policy.getMode();
    }

    public String getProcessDefinitionKey() {
        return policy.getProcessDefinitionKey();
    }

    public String getEffectExecutorCode() {
        return policy.getEffectExecutorCode();
    }

}
