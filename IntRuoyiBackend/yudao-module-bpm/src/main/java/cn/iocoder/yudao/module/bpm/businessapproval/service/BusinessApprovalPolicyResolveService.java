package cn.iocoder.yudao.module.bpm.businessapproval.service;

import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalContext;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalException;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalPolicy;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalPolicyResolution;

import java.util.List;

public class BusinessApprovalPolicyResolveService {

    private final List<BusinessApprovalPolicy> policies;

    public BusinessApprovalPolicyResolveService(List<BusinessApprovalPolicy> policies) {
        this.policies = policies == null ? List.of() : List.copyOf(policies);
    }

    public BusinessApprovalPolicyResolution resolve(BusinessApprovalContext context) {
        validateContext(context);
        List<BusinessApprovalPolicy> matchedPolicies = findPublishedPolicies(context);
        if (matchedPolicies.isEmpty()) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_POLICY_NOT_FOUND,
                    "No published business approval policy matched action " + context.getActionCode());
        }
        if (matchedPolicies.size() > 1) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_POLICY_CONFLICT,
                    "More than one published business approval policy matched action " + context.getActionCode());
        }
        return BusinessApprovalPolicyResolution.from(matchedPolicies.get(0));
    }

    protected List<BusinessApprovalPolicy> findPublishedPolicies(BusinessApprovalContext context) {
        return policies.stream()
                .filter(policy -> policy.matches(context))
                .toList();
    }

    protected void validateContext(BusinessApprovalContext context) {
        requirePresent(context == null ? null : context.getTenantId(), "tenantId");
        requirePresent(context.getDataDomain(), "dataDomain");
        requirePresent(context.getSystemCode(), "systemCode");
        requirePresent(context.getObjectType(), "objectType");
        requirePresent(context.getObjectId(), "objectId");
        requirePresent(context.getObjectVersion(), "objectVersion");
        requirePresent(context.getActionCode(), "actionCode");
        requirePresent(context.getObjectState(), "objectState");
        requirePresent(context.getApplicantUserId(), "applicantUserId");
    }

    protected void requirePresent(Object value, String fieldName) {
        if (value == null || (value instanceof String text && text.isBlank())) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_CONTEXT_INVALID,
                    "Business approval context misses required field: " + fieldName);
        }
    }

}
