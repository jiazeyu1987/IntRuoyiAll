package cn.iocoder.yudao.module.mes.support;

import cn.iocoder.yudao.framework.mybatis.core.util.MyBatisUtils;
import cn.iocoder.yudao.framework.tenant.config.TenantProperties;
import cn.iocoder.yudao.framework.tenant.core.db.TenantDatabaseInterceptor;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/** Enables production tenant SQL handling for the tenant mappers in these H2 tests. */
@TestConfiguration(proxyBeanMethods = false)
public class TenantMapperTestConfiguration {
    @Bean
    @org.springframework.context.annotation.Lazy(false)
    TenantLineInnerInterceptor tenantMapperTestInterceptor(MybatisPlusInterceptor interceptor) {
        var handler = new TenantDatabaseInterceptor(new TenantProperties()) {
            @Override
            public boolean ignoreTable(String tableName) {
                // These shared H2 reference tables have no tenant_id column. Other mapped
                // tables follow production handling, including DOs with no tenantId field.
                return java.util.Set.of("system_menu", "system_user_role", "system_notify_template",
                                "mes_pro_edhr_release_check_item", "mes_pro_edhr_release_transaction_event")
                        .contains(tableName.replace("`", "").replace("\"", "").toLowerCase(java.util.Locale.ROOT))
                        || super.ignoreTable(tableName);
            }
        };
        var tenantInterceptor = new TenantLineInnerInterceptor(handler);
        MyBatisUtils.addInterceptor(interceptor, tenantInterceptor, 0);
        return tenantInterceptor;
    }
}
