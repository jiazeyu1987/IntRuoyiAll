package cn.iocoder.yudao.module.system.job.permission;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.system.service.permission.TemporaryRoleGrantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component("temporaryRoleGrantReminderJob")
@RequiredArgsConstructor
public class TemporaryRoleGrantReminderJob implements JobHandler {

    private final TemporaryRoleGrantService temporaryRoleGrantService;

    @Override
    @TenantJob
    public String execute(String param) {
        int remindedCount = temporaryRoleGrantService.remindExpiringSoonGrants(LocalDateTime.now(), 24, 0L,
                "temporaryRoleGrantReminderJob");
        return "临时角色授权到期提醒数量：" + remindedCount;
    }

}
