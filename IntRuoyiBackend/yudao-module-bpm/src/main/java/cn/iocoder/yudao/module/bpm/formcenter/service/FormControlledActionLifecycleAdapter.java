package cn.iocoder.yudao.module.bpm.formcenter.service;

import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionInstance;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormControlledActionApprovalOutcome;

public interface FormControlledActionLifecycleAdapter {

    boolean supports(FormActionInstance instance);

    FormBusinessEffectPrecheck preflight(FormActionInstance instance);

    void onPendingApprovalStarted(FormActionInstance instance);

    void onPendingApprovalClosed(FormActionInstance instance, FormControlledActionApprovalOutcome outcome,
            String reason);

}
