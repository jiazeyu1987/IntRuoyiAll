package cn.iocoder.yudao.module.signature.gxp.api;

public record GxpAuditSubject(
        String domain,
        String subjectType,
        String subjectId,
        String subjectVersion
) {
}
