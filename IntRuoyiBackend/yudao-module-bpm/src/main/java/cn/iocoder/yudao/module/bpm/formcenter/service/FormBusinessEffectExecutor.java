package cn.iocoder.yudao.module.bpm.formcenter.service;

import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionInstance;

public interface FormBusinessEffectExecutor {

    default String getExecutorCode() {
        return getClass().getSimpleName();
    }

    FormBusinessEffectResult execute(FormActionInstance instance, String idempotencyKey);

}
