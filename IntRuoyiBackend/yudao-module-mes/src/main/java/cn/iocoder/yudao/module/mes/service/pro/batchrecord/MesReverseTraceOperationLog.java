package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionOriginMapper;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesActiveOrderActionLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.Objects;

/** Called only after the existing reverse-trace service has checked the tenant and readable batch scope. */
@Service
@RequiredArgsConstructor
public class MesReverseTraceOperationLog {
    private final MesProEdhrBatchExecutionOriginMapper origins;
    private final MesActiveOrderActionLog actionLog;

    public void record(String batchId, String operation, Map<String, Object> details) {
        Long batch = Long.valueOf(batchId);
        Long tenant = TenantContextHolder.getRequiredTenantId();
        var sourceIds = origins.selectListByBatchExecutionId(batch).stream()
                .filter(o -> Objects.equals(tenant, o.getTenantId()) && Objects.equals(batch, o.getBatchExecutionId()))
                .map(o -> o.getActiveOrderId()).filter(Objects::nonNull).distinct().toList();
        if (sourceIds.size() != 1) throw new IllegalStateException("反查日志缺少唯一的正式活跃订单来源");
        actionLog.recordRead(SecurityFrameworkUtils.getLoginUserId(), sourceIds.get(0), batch, operation, details);
    }
}
