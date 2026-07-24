package cn.iocoder.yudao.module.bpm.formcenter.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FormActionResolution {

    private final Long policyId;
    private final FormPolicyType policyType;
    private final FormApprovalMode approvalMode;
    private final String bpmProcessKey;
    private final String effectExecutorCode;
    private final List<FormPolicySlot> slots;

    private FormActionResolution(FormActionPolicy policy) {
        this.policyId = policy.getPolicyId();
        this.policyType = policy.getPolicyType();
        this.approvalMode = policy.getApprovalMode();
        this.bpmProcessKey = policy.getBpmProcessKey();
        this.effectExecutorCode = policy.getEffectExecutorCode();
        this.slots = Collections.unmodifiableList(new ArrayList<>(policy.getSlots()));
    }

    public static FormActionResolution from(FormActionPolicy policy) {
        return new FormActionResolution(policy);
    }

    public boolean requiresForm() {
        return policyType != FormPolicyType.NONE;
    }

    public boolean requiresBpm() {
        return approvalMode == FormApprovalMode.BPM_REQUIRED;
    }

    public Long getPolicyId() {
        return policyId;
    }

    public FormPolicyType getPolicyType() {
        return policyType;
    }

    public FormApprovalMode getApprovalMode() {
        return approvalMode;
    }

    public String getBpmProcessKey() {
        return bpmProcessKey;
    }

    public String getEffectExecutorCode() {
        return effectExecutorCode;
    }

    public List<FormPolicySlot> getSlots() {
        return slots;
    }

}
