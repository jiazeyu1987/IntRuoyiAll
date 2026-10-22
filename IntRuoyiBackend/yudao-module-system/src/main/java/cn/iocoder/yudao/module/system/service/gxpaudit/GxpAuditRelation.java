package cn.iocoder.yudao.module.system.service.gxpaudit;

public record GxpAuditRelation(String relationType, String objectType, String objectId,
                               String objectVersion, String objectHash) {
}
