package cn.iocoder.yudao.module.system.api.workbench.index;

/** Formal notification boundary used by the owning Showroom exporter; final index queries never join notifications. */
public interface WorkbenchNotificationExport {
    String requireContent(long tenantId,long messageId);
}
