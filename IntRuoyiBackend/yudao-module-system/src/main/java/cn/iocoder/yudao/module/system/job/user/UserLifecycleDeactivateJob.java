package cn.iocoder.yudao.module.system.job.user;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component("userLifecycleDeactivateJob")
@RequiredArgsConstructor
public class UserLifecycleDeactivateJob implements JobHandler {

    private final AdminUserService userService;

    @Override
    @TenantJob
    public String execute(String param) {
        int limit = parseLimit(param);
        int processedCount = userService.processDueLifecycleDeactivations(LocalDateTime.now(), limit);
        return "用户离职/转岗到期停用处理数量：" + processedCount + "，limit=" + limit;
    }

    private int parseLimit(String param) {
        if (param == null || param.isBlank()) {
            throw new IllegalArgumentException("param.limit is required");
        }
        JSONObject jsonObject = JSONUtil.parseObj(param);
        Integer limit = jsonObject.getInt("limit");
        if (limit == null || limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
        return limit;
    }

}
