package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

public record MesOrderReleaseCompletenessCheck(
        String checkCode,
        String checkName,
        String checkCategory,
        String checkResult,
        String severity,
        String responsibilityModule,
        String sourceObjectType,
        String sourceObjectId,
        String sourceObjectCode,
        String failureReason,
        String remediationSuggestion) {
}
