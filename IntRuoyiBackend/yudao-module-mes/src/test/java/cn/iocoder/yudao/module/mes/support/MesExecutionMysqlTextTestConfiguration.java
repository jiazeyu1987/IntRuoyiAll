package cn.iocoder.yudao.module.mes.support;

/** Test-only dialect alignment: H2 otherwise truncates MySQL CHAR casts to one character. */
@org.springframework.boot.test.context.TestConfiguration
public class MesExecutionMysqlTextTestConfiguration {
    @org.springframework.context.annotation.Bean
    org.apache.ibatis.plugin.Interceptor mesExecutionMysqlTextCasts() { return new MysqlTextCasts(); }

    @org.apache.ibatis.plugin.Intercepts(@org.apache.ibatis.plugin.Signature(
            type = org.apache.ibatis.executor.statement.StatementHandler.class, method = "prepare",
            args = {java.sql.Connection.class, Integer.class}))
    static class MysqlTextCasts implements org.apache.ibatis.plugin.Interceptor {
        @Override
        public Object intercept(org.apache.ibatis.plugin.Invocation invocation) throws Throwable {
            var handler = (org.apache.ibatis.executor.statement.StatementHandler) invocation.getTarget();
            var bound = handler.getBoundSql();
            org.apache.ibatis.reflection.SystemMetaObject.forObject(bound).setValue("sql",
                    bound.getSql().replace(" AS CHAR)", " AS VARCHAR)"));
            return invocation.proceed();
        }
    }
}
