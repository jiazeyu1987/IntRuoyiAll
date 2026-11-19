package cn.iocoder.yudao.module.mes.job.batchrecord;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskOverdueProcessResult;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component("mesEdhrWorkTaskOverdueJob")
@RequiredArgsConstructor
public class MesEdhrWorkTaskOverdueJob implements JobHandler {

    private static final int DEFAULT_LIMIT = 200;

    private final MesProEdhrWorkTaskService workTaskService;

    @Override
    @TenantJob
    public String execute(String param) {
        int limit = parseLimit(param);
        MesProEdhrWorkTaskOverdueProcessResult result =
                workTaskService.processOverdueTasksWithSummary(LocalDateTime.now(), limit);
        return "eDHR work task overdue processing: scanned=" + result.getScannedCount()
                + ", overdue=" + result.getOverdueCount()
                + ", skipped=" + result.getSkippedCount()
                + ", skippedReason=" + result.getSkippedReason()
                + ", limit=" + limit;
    }

    private int parseLimit(String param) {
        if (param == null || param.isBlank()) {
            return DEFAULT_LIMIT;
        }
        JSONObject jsonObject = JSONUtil.parseObj(param);
        int limit = jsonObject.getInt("limit", DEFAULT_LIMIT);
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
        return limit;
    }
}
