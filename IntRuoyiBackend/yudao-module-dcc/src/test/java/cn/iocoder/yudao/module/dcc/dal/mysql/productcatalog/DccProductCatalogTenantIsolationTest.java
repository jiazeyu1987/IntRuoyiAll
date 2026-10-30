package cn.iocoder.yudao.module.dcc.dal.mysql.productcatalog;

import cn.iocoder.yudao.framework.tenant.config.TenantProperties;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.db.TenantDatabaseInterceptor;
import cn.iocoder.yudao.module.dcc.dal.dataobject.productcatalog.DccProductCatalogDO;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DccProductCatalogTenantIsolationTest {
    @Test
    @SuppressWarnings("unchecked")
    void configuredInterceptorScopesCatalogReadsAndWritesToCurrentTenant() throws Exception {
        // Use the shipped configuration so restoring the old ignore entry fails this test.
        Path config = Path.of("../yudao-server/src/main/resources/application.yaml");
        Map<String, Object> root = new java.util.HashMap<>();
        try (var input = Files.newInputStream(config)) {
            for (Object document : new Yaml().loadAll(input)) {
                root.putAll((Map<String, Object>) document);
            }
        }
        var tenant = (Map<String, Object>) ((Map<String, Object>) root.get("yudao")).get("tenant");
        TenantProperties properties = new TenantProperties();
        properties.setIgnoreTables(new HashSet<>((List<String>) tenant.get("ignore-tables")));
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                DccProductCatalogDO.class);
        var interceptor = new TenantLineInnerInterceptor(new TenantDatabaseInterceptor(properties));
        try {
            for (long tenantId : new long[] {1L, 2L}) {
                TenantContextHolder.setTenantId(tenantId);
                for (String sql : List.of(
                        "SELECT * FROM dcc_product_catalog WHERE deleted = 0",
                        "SELECT * FROM dcc_product_catalog WHERE id = 42",
                        "UPDATE dcc_product_catalog SET product = 'updated' WHERE id = 42",
                        "DELETE FROM dcc_product_catalog WHERE id = 42")) {
                    String scoped = interceptor.parserSingle(sql, null);
                    assertTrue(scoped.contains("tenant_id = " + tenantId), scoped);
                }
                String inserted = interceptor.parserSingle(
                        "INSERT INTO dcc_product_catalog (product) VALUES ('new')", null);
                assertTrue(inserted.contains("tenant_id"), inserted);
                assertTrue(inserted.contains("'new', " + tenantId), inserted);
            }
        } finally {
            TenantContextHolder.clear();
        }
    }
}
