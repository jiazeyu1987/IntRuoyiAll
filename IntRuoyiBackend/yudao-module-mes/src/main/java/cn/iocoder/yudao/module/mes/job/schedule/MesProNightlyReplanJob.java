package cn.iocoder.yudao.module.mes.job.schedule;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.mes.service.pro.schedule.MesProNightlyReplanResult;
import cn.iocoder.yudao.module.mes.service.pro.schedule.MesProNightlyReplanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component("mesProNightlyReplanJob")
@RequiredArgsConstructor
public class MesProNightlyReplanJob implements JobHandler {

    private final MesProNightlyReplanService nightlyReplanService;

    @Override
    @TenantJob
    public String execute(String param) {
        MesProNightlyReplanResult result = nightlyReplanService.executeNightlyReplan(LocalDateTime.now());
        return result.toJobMessage();
    }

}
