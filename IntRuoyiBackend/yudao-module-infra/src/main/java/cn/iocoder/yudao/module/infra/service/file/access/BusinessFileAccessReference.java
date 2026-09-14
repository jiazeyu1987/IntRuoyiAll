package cn.iocoder.yudao.module.infra.service.file.access;

public record BusinessFileAccessReference(
        String providerId,
        String businessType,
        Long businessId,
        String versionKey,
        Long tenantId,
        Long companyId) {
}
