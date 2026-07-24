package cn.iocoder.yudao.module.bpm.formcenter.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class FormActionPolicy {

    public static final String STATUS_PUBLISHED = "PUBLISHED";

    private final Long policyId;
    private final Long tenantId;
    private final String dataDomain;
    private final String systemCode;
    private final String objectType;
    private final String actionCode;
    private final String objectState;
    private final FormPolicyType policyType;
    private final FormApprovalMode approvalMode;
    private final String bpmProcessKey;
    private final String effectExecutorCode;
    private final String status;
    private final List<FormPolicySlot> slots;

    private FormActionPolicy(Builder builder) {
        this.policyId = builder.policyId;
        this.tenantId = builder.tenantId;
        this.dataDomain = builder.dataDomain;
        this.systemCode = builder.systemCode;
        this.objectType = builder.objectType;
        this.actionCode = builder.actionCode;
        this.objectState = builder.objectState;
        this.policyType = builder.policyType;
        this.approvalMode = builder.approvalMode;
        this.bpmProcessKey = builder.bpmProcessKey;
        this.effectExecutorCode = builder.effectExecutorCode;
        this.status = builder.status;
        this.slots = Collections.unmodifiableList(new ArrayList<>(builder.slots));
    }

    public static Builder builder() {
        return new Builder();
    }

    public FormActionPolicy withStatus(String status) {
        return builder()
                .policyId(policyId)
                .tenantId(tenantId)
                .dataDomain(dataDomain)
                .systemCode(systemCode)
                .objectType(objectType)
                .actionCode(actionCode)
                .objectState(objectState)
                .policyType(policyType)
                .approvalMode(approvalMode)
                .bpmProcessKey(bpmProcessKey)
                .effectExecutorCode(effectExecutorCode)
                .status(status)
                .slots(slots)
                .build();
    }

    public boolean matches(BusinessActionContext context) {
        return Objects.equals(tenantId, context.getTenantId())
                && Objects.equals(dataDomain, context.getDataDomain())
                && Objects.equals(systemCode, context.getSystemCode())
                && Objects.equals(objectType, context.getObjectType())
                && Objects.equals(actionCode, context.getActionCode())
                && Objects.equals(objectState, context.getObjectState())
                && STATUS_PUBLISHED.equals(status);
    }

    public Long getPolicyId() {
        return policyId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public String getDataDomain() {
        return dataDomain;
    }

    public String getSystemCode() {
        return systemCode;
    }

    public String getObjectType() {
        return objectType;
    }

    public String getActionCode() {
        return actionCode;
    }

    public String getObjectState() {
        return objectState;
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

    public String getStatus() {
        return status;
    }

    public List<FormPolicySlot> getSlots() {
        return slots;
    }

    public static final class Builder {

        private Long policyId;
        private Long tenantId;
        private String dataDomain;
        private String systemCode;
        private String objectType;
        private String actionCode;
        private String objectState;
        private FormPolicyType policyType;
        private FormApprovalMode approvalMode = FormApprovalMode.BPM_REQUIRED;
        private String bpmProcessKey;
        private String effectExecutorCode;
        private String status;
        private List<FormPolicySlot> slots = List.of();

        public Builder policyId(Long policyId) {
            this.policyId = policyId;
            return this;
        }

        public Builder tenantId(Long tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder dataDomain(String dataDomain) {
            this.dataDomain = dataDomain;
            return this;
        }

        public Builder systemCode(String systemCode) {
            this.systemCode = systemCode;
            return this;
        }

        public Builder objectType(String objectType) {
            this.objectType = objectType;
            return this;
        }

        public Builder actionCode(String actionCode) {
            this.actionCode = actionCode;
            return this;
        }

        public Builder objectState(String objectState) {
            this.objectState = objectState;
            return this;
        }

        public Builder policyType(FormPolicyType policyType) {
            this.policyType = policyType;
            return this;
        }

        public Builder approvalMode(FormApprovalMode approvalMode) {
            this.approvalMode = approvalMode == null ? FormApprovalMode.BPM_REQUIRED : approvalMode;
            return this;
        }

        public Builder bpmProcessKey(String bpmProcessKey) {
            this.bpmProcessKey = bpmProcessKey;
            return this;
        }

        public Builder effectExecutorCode(String effectExecutorCode) {
            this.effectExecutorCode = effectExecutorCode;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder slots(List<FormPolicySlot> slots) {
            this.slots = slots == null ? List.of() : slots;
            return this;
        }

        public FormActionPolicy build() {
            return new FormActionPolicy(this);
        }

    }

}
