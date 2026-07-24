package cn.iocoder.yudao.module.bpm.formcenter.model;

public class FormTemplateImpact {

    private final Long policyId;
    private final String policyName;

    private FormTemplateImpact(Long policyId, String policyName) {
        this.policyId = policyId;
        this.policyName = policyName;
    }

    public static FormTemplateImpact policyReference(Long policyId, String policyName) {
        return new FormTemplateImpact(policyId, policyName);
    }

    public Long getPolicyId() {
        return policyId;
    }

    public String getPolicyName() {
        return policyName;
    }

}
