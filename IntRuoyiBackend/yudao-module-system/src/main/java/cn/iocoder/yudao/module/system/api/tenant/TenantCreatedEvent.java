package cn.iocoder.yudao.module.system.api.tenant;

/** 在新租户事务及其租户上下文中同步发布，供业务模块初始化必需数据。 */
public record TenantCreatedEvent(Long tenantId) {
}
