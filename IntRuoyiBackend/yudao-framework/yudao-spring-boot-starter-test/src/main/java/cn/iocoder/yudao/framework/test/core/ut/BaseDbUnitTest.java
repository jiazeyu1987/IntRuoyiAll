package cn.iocoder.yudao.framework.test.core.ut;

import cn.hutool.extra.spring.SpringUtil;
import cn.iocoder.yudao.framework.datasource.config.YudaoDataSourceAutoConfiguration;
import cn.iocoder.yudao.framework.mybatis.config.YudaoMybatisAutoConfiguration;
import cn.iocoder.yudao.framework.test.config.SqlInitializationTestConfiguration;
import com.alibaba.druid.spring.boot3.autoconfigure.DruidDataSourceAutoConfigure;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.github.yulichang.autoconfigure.MybatisPlusJoinAutoConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.lang.reflect.InvocationTargetException;

/**
 * 依赖内存 DB 的单元测试
 *
 * 注意，Service 层同样适用。对于 Service 层的单元测试，我们针对自己模块的 Mapper 走的是 H2 内存数据库，针对别的模块的 Service 走的是 Mock 方法
 *
 * @author 瑛泰源码
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = BaseDbUnitTest.Application.class)
@ActiveProfiles("unit-test") // 设置使用 application-unit-test 配置文件
@Sql(scripts = "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD) // 每个单元测试结束后，清理 DB
public class BaseDbUnitTest {

    @BeforeEach
    protected void setDefaultTenantContext() {
        invokeTenantContext("setTenantId", new Class<?>[]{Long.class}, new Object[]{1L});
        invokeTenantContext("setIgnore", new Class<?>[]{Boolean.class}, new Object[]{false});
    }

    @AfterEach
    protected void clearDefaultTenantContext() {
        invokeTenantContext("clear", new Class<?>[0], new Object[0]);
    }

    private void invokeTenantContext(String methodName, Class<?>[] parameterTypes, Object[] args) {
        try {
            Class<?> holderClass = Class.forName("cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder");
            holderClass.getMethod(methodName, parameterTypes).invoke(null, args);
        } catch (ClassNotFoundException ignored) {
            // BaseDbUnitTest is shared by modules that do not depend on tenant support.
        } catch (NoSuchMethodException | IllegalAccessException exception) {
            throw new IllegalStateException("TenantContextHolder." + methodName + " is unavailable", exception);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("TenantContextHolder." + methodName + " failed", cause);
        }
    }

    @Import({
            // DB 配置类
            YudaoDataSourceAutoConfiguration.class, // 自己的 DB 配置类
            DataSourceAutoConfiguration.class, // Spring DB 自动配置类
            DataSourceTransactionManagerAutoConfiguration.class, // Spring 事务自动配置类
            DruidDataSourceAutoConfigure.class, // Druid 自动配置类
            SqlInitializationTestConfiguration.class, // SQL 初始化
            // MyBatis 配置类
            YudaoMybatisAutoConfiguration.class, // 自己的 MyBatis 配置类
            MybatisPlusAutoConfiguration.class, // MyBatis 的自动配置类
            MybatisPlusJoinAutoConfiguration.class, // MyBatis 的Join配置类

            // 其它配置类
            SpringUtil.class
    })
    public static class Application {
    }

}
