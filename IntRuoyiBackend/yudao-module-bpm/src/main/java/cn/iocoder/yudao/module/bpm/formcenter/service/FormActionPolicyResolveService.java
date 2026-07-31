package cn.iocoder.yudao.module.bpm.formcenter.service;

import cn.iocoder.yudao.module.bpm.formcenter.model.BusinessActionContext;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionPolicy;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionResolution;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterErrorCode;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterException;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormPolicySlot;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FormActionPolicyResolveService {

    private final List<FormActionPolicy> policies;

    public FormActionPolicyResolveService(List<FormActionPolicy> policies) {
        this.policies = List.copyOf(policies);
    }

    public FormActionResolution resolve(BusinessActionContext context) {
        validateContext(context);
        List<FormActionPolicy> matchedPolicies = policies.stream()
                .filter(policy -> policy.matches(context))
                .toList();
        if (matchedPolicies.isEmpty()) {
            throw new FormCenterException(FormCenterErrorCode.FORM_POLICY_NOT_FOUND,
                    "No published business approval policy matched action " + context.getActionCode());
        }
        if (matchedPolicies.size() > 1) {
            throw new FormCenterException(FormCenterErrorCode.FORM_POLICY_CONFLICT,
                    "More than one published business approval policy matched action " + context.getActionCode());
        }
        FormActionPolicy policy = matchedPolicies.get(0);
        validateSlots(policy.getSlots());
        return FormActionResolution.from(policy);
    }

    private void validateContext(BusinessActionContext context) {
        requirePresent(context.getTenantId(), "tenantId");
        requirePresent(context.getDataDomain(), "dataDomain");
        requirePresent(context.getSystemCode(), "systemCode");
        requirePresent(context.getObjectType(), "objectType");
        requirePresent(context.getObjectId(), "objectId");
        requirePresent(context.getObjectVersion(), "objectVersion");
        requirePresent(context.getActionCode(), "actionCode");
        requirePresent(context.getObjectState(), "objectState");
    }

    private void requirePresent(Object value, String fieldName) {
        if (value == null || (value instanceof String text && text.isBlank())) {
            throw new FormCenterException(FormCenterErrorCode.FORM_ACTION_CONTEXT_INVALID,
                    "Business action context misses required field: " + fieldName);
        }
    }

    private void validateSlots(List<FormPolicySlot> slots) {
        Set<String> slotCodes = new HashSet<>();
        for (FormPolicySlot slot : slots) {
            if (!slotCodes.add(slot.getSlotCode())) {
                throw new FormCenterException(FormCenterErrorCode.FORM_TEMPLATE_SLOT_CONFLICT,
                        "Form policy slot matched more than one template: " + slot.getSlotCode());
            }
        }
    }

}
