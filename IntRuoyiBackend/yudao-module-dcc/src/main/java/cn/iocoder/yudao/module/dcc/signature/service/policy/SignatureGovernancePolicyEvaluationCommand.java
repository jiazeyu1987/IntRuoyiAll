package cn.iocoder.yudao.module.dcc.signature.service.policy;

import cn.iocoder.yudao.module.dcc.signature.core.SignatureGovernanceModuleCode;

public record SignatureGovernancePolicyEvaluationCommand(SignatureGovernanceModuleCode moduleCode,
                                                          String actionCode,
                                                          SignatureGovernanceOperationMode operationMode,
                                                          Long actorId,
                                                          Long sourceRecordId,
                                                          String taskId,
                                                          String password,
                                                          String comment) {

    public SignatureGovernancePolicyEvaluationCommand {
        if (moduleCode == null || isBlank(actionCode) || operationMode == null) {
            throw new IllegalArgumentException("Signature governance evaluation requires module, action, and mode");
        }
        actionCode = actionCode.trim();
        taskId = trimToNull(taskId);
        password = trimToNull(password);
        comment = trimToNull(comment);
    }

    public static SignatureGovernancePolicyEvaluationCommand of(SignatureGovernanceModuleCode moduleCode,
                                                                String actionCode,
                                                                SignatureGovernanceOperationMode operationMode) {
        return new SignatureGovernancePolicyEvaluationCommand(moduleCode, actionCode, operationMode,
                null, null, null, null, null);
    }

    public static SignatureGovernancePolicyEvaluationCommand signature(SignatureGovernanceModuleCode moduleCode,
                                                                       String actionCode,
                                                                       SignatureGovernanceOperationMode operationMode,
                                                                       Long actorId,
                                                                       Long sourceRecordId,
                                                                       String taskId,
                                                                       String password,
                                                                       String comment) {
        return new SignatureGovernancePolicyEvaluationCommand(moduleCode, actionCode, operationMode,
                actorId, sourceRecordId, taskId, password, comment);
    }

    private static String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
