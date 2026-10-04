package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.plugin.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.util.ReflectionTestUtils;
import java.sql.Connection;

/** Test-only SQL dialect adaptation; production mapper predicates and parameters stay intact. */
@TestConfiguration(proxyBeanMethods = false)
public class MesSignatureH2DialectConfiguration {
    @Bean Interceptor signatureH2BitLiteralInterceptor() { return new BitLiterals(); }

    @Intercepts(@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class}))
    static class BitLiterals implements Interceptor {
        @Override public Object intercept(Invocation invocation) throws Throwable {
            var bound = ((StatementHandler) invocation.getTarget()).getBoundSql();
            ReflectionTestUtils.setField(bound, "sql", bound.getSql().replace("b'0'", "0").replace("b'1'", "1"));
            return invocation.proceed();
        }
    }
}
