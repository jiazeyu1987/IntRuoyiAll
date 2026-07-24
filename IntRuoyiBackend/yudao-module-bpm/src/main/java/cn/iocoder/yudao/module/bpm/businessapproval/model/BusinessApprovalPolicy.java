package cn.iocoder.yudao.module.bpm.businessapproval.model;

import lombok.Builder;
import lombok.Getter;

import java.util.Objects;

@Getter
@Builder(toBuilder = true)
public class BusinessApprovalPolicy {

    public static final String STATUS_PUBLISHED = "PUBLISHED";
    public static final String STATUS_DISABLED = "DISABLED";
    public static final String OBJECT_STATE_ALL = "ALL";

    private final Long policyId;
    private final Long tenantId;
    private final String dataDomain;
    private final String systemCode;
    private final String objectType;
    private final String actionCode;
    private final String objectState;
    private final BusinessApprovalPolicyMode mode;
    private final String processDefinitionKey;
    private final String effectExecutorCode;
    private final String status;

    public boolean matches(BusinessApprovalContext context) {
        return Objects.equals(tenantId, context.getTenantId())
                && Objects.equals(dataDomain, context.getDataDomain())
                && Objects.equals(systemCode, context.getSystemCode())
                && Objects.equals(objectType, context.getObjectType())
                && Objects.equals(actionCode, context.getActionCode())
                && (Objects.equals(objectState, context.getObjectState()) || OBJECT_STATE_ALL.equals(objectState))
                && STATUS_PUBLISHED.equals(status);
    }

}
