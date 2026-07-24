package cn.iocoder.yudao.module.bpm.formcenter.service;

import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionPolicy;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterErrorCode;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterException;

import java.util.Set;

public class FormPolicyPublishService {

    private final Set<String> registeredEffectExecutors;

    public FormPolicyPublishService(Set<String> registeredEffectExecutors) {
        this.registeredEffectExecutors = Set.copyOf(registeredEffectExecutors);
    }

    public FormActionPolicy publish(FormActionPolicy policy) {
        String executorCode = policy.getEffectExecutorCode();
        if (executorCode == null || executorCode.isBlank() || !registeredEffectExecutors.contains(executorCode)) {
            throw new FormCenterException(FormCenterErrorCode.EFFECT_EXECUTOR_MISSING,
                    "Effect executor is not registered: " + executorCode);
        }
        return policy.withStatus(FormActionPolicy.STATUS_PUBLISHED);
    }

}
