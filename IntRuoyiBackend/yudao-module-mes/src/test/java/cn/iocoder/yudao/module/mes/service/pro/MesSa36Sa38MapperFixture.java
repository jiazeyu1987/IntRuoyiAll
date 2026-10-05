package cn.iocoder.yudao.module.mes.service.pro;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.plugin.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.sql.Connection;

import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

/** Isolated H2/MyBatis fixture, following the existing rework transaction fixture schema. */
public final class MesSa36Sa38MapperFixture {
    public final DriverManagerDataSource dataSource = new DriverManagerDataSource(
            "jdbc:h2:mem:sa36_38_" + UUID.randomUUID()
                    + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false", "sa", "");
    public final JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    public final TransactionTemplate transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    private final MybatisConfiguration configuration = new MybatisConfiguration();
    private SqlSessionTemplate session;

    public void table(Class<?> type, String name) {
        var columns = new ArrayList<String>();
        for (Class<?> current = type; current != Object.class; current = current.getSuperclass()) {
            for (var field : current.getDeclaredFields()) {
                var mapping = field.getAnnotation(TableField.class);
                if (Modifier.isStatic(field.getModifiers()) || mapping != null && !mapping.exist()) continue;
                String column = mapping != null && !mapping.value().isBlank() ? mapping.value()
                        : field.getName().replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
                Class<?> value = field.getType();
                String sqlType = value == Long.class ? "BIGINT" : value == Integer.class ? "INT"
                        : value == Boolean.class ? "BOOLEAN" : value == BigDecimal.class ? "DECIMAL(24,6)"
                        : value == LocalDateTime.class ? "TIMESTAMP" : value == LocalDate.class ? "DATE"
                        : "VARCHAR(65535)";
                columns.add(column + " " + sqlType + (column.equals("id") ? " PRIMARY KEY"
                        : column.equals("deleted") ? " DEFAULT FALSE" : ""));
            }
        }
        jdbc.execute("CREATE TABLE " + name + "(" + String.join(",", columns) + ")");
    }

    public void register(Class<?>... mappers) {
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.addInterceptor(new MysqlBitsForH2());
        var tenants = new com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor();
        new cn.iocoder.yudao.framework.tenant.config.YudaoTenantAutoConfiguration().tenantLineInnerInterceptor(
                new cn.iocoder.yudao.framework.tenant.config.TenantProperties(), tenants);
        configuration.addInterceptor(tenants);
        for (Class<?> mapper : mappers) configuration.addMapper(mapper);
    }

    public <T> T mapper(Class<T> type) throws Exception {
        if (session == null) {
            var bean = new MybatisSqlSessionFactoryBean();
            bean.setDataSource(dataSource);
            bean.setConfiguration(configuration);
            session = new SqlSessionTemplate(bean.getObject());
        }
        return session.getMapper(type);
    }

    public AutoCloseable enableBatchPersistence() {
        var priorBeans = ReflectionTestUtils.getField(cn.hutool.extra.spring.SpringUtil.class, "beanFactory");
        var compatible = com.baomidou.mybatisplus.extension.spi.CompatibleHelper.getCompatibleSet();
        var priorContext = ReflectionTestUtils.getField(compatible.getClass(), "applicationContext");
        var context = new org.springframework.context.support.GenericApplicationContext();
        context.getBeanFactory().registerSingleton("dataSource", dataSource);
        context.getBeanFactory().registerSingleton("sqlSessionFactory", session.getSqlSessionFactory());
        context.refresh();
        compatible.setContext(context);
        new cn.hutool.extra.spring.SpringUtil().postProcessBeanFactory(context.getBeanFactory());
        return () -> {
            ReflectionTestUtils.setField(cn.hutool.extra.spring.SpringUtil.class, "beanFactory", priorBeans);
            compatible.setContext(priorContext);
            context.close();
        };
    }

    // Test dialect translation only, identical to the existing correction transaction fixture.
    @Intercepts(@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class}))
    public static class MysqlBitsForH2 implements Interceptor {
        @Override public Object intercept(Invocation invocation) throws Throwable {
            var bound = ((StatementHandler) invocation.getTarget()).getBoundSql();
            ReflectionTestUtils.setField(bound, "sql", bound.getSql().replaceAll("(?i)b'0'", "0").replaceAll("(?i)b'1'", "1"));
            return invocation.proceed();
        }
    }
}
