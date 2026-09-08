package cn.iocoder.yudao.module.signature.gxp.api;

public record GxpAuditStateEnvelope(
        String state,
        String objectVersion,
        String canonicalJson
) {
}
