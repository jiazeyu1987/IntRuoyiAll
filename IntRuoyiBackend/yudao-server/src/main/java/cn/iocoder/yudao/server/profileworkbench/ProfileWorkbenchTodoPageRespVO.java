package cn.iocoder.yudao.server.profileworkbench;
import cn.iocoder.yudao.module.system.api.workbench.TaskSummary;
import java.time.LocalDateTime;
import java.util.List;
public record ProfileWorkbenchTodoPageRespVO(int contractVersion, String registryRevision, String queryVersion,
    String contextEpoch, String visibilityVersion, List<TaskSummary> rows, long filteredTotal, long businessTotal,
    long hiddenTotal, int effectivePageNo, int pageSize,
    @com.fasterxml.jackson.databind.annotation.JsonSerialize(using=cn.iocoder.yudao.module.system.api.workbench.WorkbenchLocalDateTimeSerializer.class) LocalDateTime readAt) { }
