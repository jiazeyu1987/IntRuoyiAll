package cn.iocoder.yudao.module.showroom.support;

/** H2-only equivalents for MySQL text casts and array membership. */
@org.springframework.boot.test.context.TestConfiguration
public class ShowroomMysqlTextTestConfiguration {
    @org.springframework.context.annotation.Bean
    org.apache.ibatis.plugin.Interceptor showroomMysqlTextCasts() { return new MysqlTextCasts(); }

    public static int findInSet(String needle, String values) {
        if (needle == null || values == null) return 0;
        String[] elements = values.split(",", -1);
        for (int i = 0; i < elements.length; i++) if (needle.equals(elements[i])) return i + 1;
        return 0;
    }

    @org.apache.ibatis.plugin.Intercepts(@org.apache.ibatis.plugin.Signature(
            type = org.apache.ibatis.executor.statement.StatementHandler.class, method = "prepare",
            args = {java.sql.Connection.class, Integer.class}))
    static class MysqlTextCasts implements org.apache.ibatis.plugin.Interceptor {
        @Override
        public Object intercept(org.apache.ibatis.plugin.Invocation invocation) throws Throwable {
            var connection = (java.sql.Connection) invocation.getArgs()[0];
            try (var statement = connection.createStatement()) {
                statement.execute("CREATE ALIAS IF NOT EXISTS FIND_IN_SET FOR 'cn.iocoder.yudao.module.showroom.support.ShowroomMysqlTextTestConfiguration.findInSet'");
            }
            var bound = ((org.apache.ibatis.executor.statement.StatementHandler) invocation.getTarget()).getBoundSql();
            org.apache.ibatis.reflection.SystemMetaObject.forObject(bound).setValue("sql",
                    bound.getSql().replace(" AS CHAR)", " AS VARCHAR)"));
            return invocation.proceed();
        }
    }
}
