package cn.iocoder.yudao.module.bpm.formcenter.service;

import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionInstance;

public class FormBusinessEffectPrecheck {

    private final boolean passed;
    private final String failureReason;

    private FormBusinessEffectPrecheck(boolean passed, String failureReason) {
        this.passed = passed;
        this.failureReason = failureReason;
    }

    public static FormBusinessEffectPrecheck pass() {
        return new FormBusinessEffectPrecheck(true, null);
    }

    public static FormBusinessEffectPrecheck fail(String failureReason) {
        return new FormBusinessEffectPrecheck(false, failureReason);
    }

    public FormBusinessEffectPrecheck check(FormActionInstance instance) {
        return this;
    }

    public boolean isPassed() {
        return passed;
    }

    public String getFailureReason() {
        return failureReason;
    }

}
