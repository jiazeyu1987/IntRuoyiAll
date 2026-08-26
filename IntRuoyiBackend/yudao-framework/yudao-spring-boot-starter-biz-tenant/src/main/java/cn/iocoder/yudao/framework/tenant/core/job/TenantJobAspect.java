package cn.iocoder.yudao.framework.tenant.core.job;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.service.TenantFrameworkService;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多租户 JobHandler AOP
 * 任务执行时，会按照租户逐个执行 Job 的逻辑
 *
 * 注意，需要保证 JobHandler 的幂等性。因为 Job 因为某个租户执行失败重试时，之前执行成功的租户也会再次执行。
 *
 * @author 瑛泰源码
 */
@Aspect
@RequiredArgsConstructor
@Slf4j
public class TenantJobAspect {

    private final TenantFrameworkService tenantFrameworkService;

    @Around("@annotation(tenantJob)")
    public String around(ProceedingJoinPoint joinPoint, TenantJob tenantJob) {
        TenantJobParam scopedParam = getScopedParam(joinPoint.getArgs());
        List<Long> tenantIds = getTenantIds(scopedParam);
        if (CollUtil.isEmpty(tenantIds)) {
            return null;
        }
        Object[] handlerArgs = scopedParam != null ? new Object[]{scopedParam.handlerParam()} : joinPoint.getArgs();

        // 逐个租户，执行 Job
        Map<Long, String> results = new ConcurrentHashMap<>();
        tenantIds.parallelStream().forEach(tenantId -> {
            // TODO 芋艿：先通过 parallel 实现并行；1）多个租户，是一条执行日志；2）异常的情况
            TenantUtils.execute(tenantId, () -> {
                try {
                    Object result = scopedParam != null ? joinPoint.proceed(handlerArgs) : joinPoint.proceed();
                    results.put(tenantId, StrUtil.toStringOrEmpty(result));
                } catch (Throwable e) {
                    log.error("[execute][租户({}) 执行 Job 发生异常", tenantId, e);
                    results.put(tenantId, ExceptionUtil.getRootCauseMessage(e));
                }
            });
        });
        return JsonUtils.toJsonString(results);
    }

    private TenantJobParam getScopedParam(Object[] args) {
        if (args == null || args.length != 1 || !(args[0] instanceof String handlerParam)) {
            return null;
        }
        return TenantJobParam.parse(handlerParam);
    }

    private List<Long> getTenantIds(TenantJobParam scopedParam) {
        if (scopedParam == null) {
            return tenantFrameworkService.getTenantIds();
        }
        tenantFrameworkService.validTenant(scopedParam.tenantId());
        return List.of(scopedParam.tenantId());
    }

}
