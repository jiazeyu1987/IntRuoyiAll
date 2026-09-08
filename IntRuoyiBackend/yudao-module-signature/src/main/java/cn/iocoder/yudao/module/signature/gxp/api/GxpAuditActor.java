package cn.iocoder.yudao.module.signature.gxp.api;

public record GxpAuditActor(
        Long actorId,
        String username,
        String displayName
) {
}
