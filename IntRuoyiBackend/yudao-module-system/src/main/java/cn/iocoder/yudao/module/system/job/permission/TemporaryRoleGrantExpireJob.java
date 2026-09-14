package cn.iocoder.yudao.module.system.job.permission;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.system.service.permission.TemporaryRoleGrantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component("temporaryRoleGrantExpireJob")
@RequiredArgsConstructor
public class TemporaryRoleGrantExpireJob implements JobHandler {

    private final TemporaryRoleGrantService temporaryRoleGrantService;

    @Override
    @TenantJob
    public String execute(String param) {
        int expiredCount = temporaryRoleGrantService.expireOverdueGrants(LocalDateTime.now(), 0L,
                "temporaryRoleGrantExpireJob");
        return "临时角色授权到期回收数量：" + expiredCount;
    }

}
