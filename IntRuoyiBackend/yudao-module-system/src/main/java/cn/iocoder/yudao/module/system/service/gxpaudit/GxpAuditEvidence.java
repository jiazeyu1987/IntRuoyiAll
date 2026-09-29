package cn.iocoder.yudao.module.system.service.gxpaudit;

public record GxpAuditEvidence(String evidenceType, String sourceId, String sourceVersion,
                               String sha256, String role) {
}
