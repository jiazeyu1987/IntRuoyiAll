package cn.iocoder.yudao.server.profileworkbench;
import java.time.LocalDateTime;
public record ProfileWorkbenchTodoCountRespVO(int contractVersion, String registryRevision, String queryVersion,
    String contextEpoch, String visibilityVersion, long filteredTotal, long businessTotal, long hiddenTotal,
    @com.fasterxml.jackson.databind.annotation.JsonSerialize(using=cn.iocoder.yudao.module.system.api.workbench.WorkbenchLocalDateTimeSerializer.class) LocalDateTime readAt) { }
