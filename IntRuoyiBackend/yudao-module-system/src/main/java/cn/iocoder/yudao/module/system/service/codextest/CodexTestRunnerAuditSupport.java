package cn.iocoder.yudao.module.system.service.codextest;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

final class CodexTestRunnerAuditSupport {

    static final String RUNNER_AUDIT_USER = "codex-runner";

    static void stampRunnerAudit(BaseDO baseDO) {
        if (isBlank(baseDO.getCreator())) {
            baseDO.setCreator(RUNNER_AUDIT_USER);
        }
        if (isBlank(baseDO.getUpdater())) {
            baseDO.setUpdater(RUNNER_AUDIT_USER);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private CodexTestRunnerAuditSupport() {
    }

}
