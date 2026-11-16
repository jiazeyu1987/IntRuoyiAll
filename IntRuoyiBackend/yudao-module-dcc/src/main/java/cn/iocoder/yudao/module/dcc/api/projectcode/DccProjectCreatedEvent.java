package cn.iocoder.yudao.module.dcc.api.projectcode;

/** 在正式项目创建事务及所属租户上下文中发布。 */
public record DccProjectCreatedEvent(Long projectId) {}
