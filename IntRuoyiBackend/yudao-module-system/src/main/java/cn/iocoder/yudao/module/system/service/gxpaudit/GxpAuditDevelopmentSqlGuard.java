package cn.iocoder.yudao.module.system.service.gxpaudit;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.mapping.BoundSql;
import org.springframework.stereotype.Component;

/** A forgotten direct audit mapper call must fail clearly, never acquire an audit SQL connection. */
@Component
@Intercepts({
    @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
    @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
    @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class})
})
public class GxpAuditDevelopmentSqlGuard implements Interceptor {
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement statement = (MappedStatement) invocation.getArgs()[0];
        if (GxpAuditDevelopmentMode.disabled() && statement.getId().contains(".dal.mysql.gxpaudit."))
            throw new IllegalStateException("GXP_AUDIT_DEVELOPMENT_DISABLED: audit database access is disabled");
        return invocation.proceed();
    }
}
