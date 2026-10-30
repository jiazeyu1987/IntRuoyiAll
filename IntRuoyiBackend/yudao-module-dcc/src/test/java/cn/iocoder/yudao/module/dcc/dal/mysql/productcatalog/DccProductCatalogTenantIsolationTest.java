package cn.iocoder.yudao.module.dcc.dal.mysql.productcatalog;

import cn.iocoder.yudao.framework.tenant.config.TenantProperties;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.db.TenantDatabaseInterceptor;
import cn.iocoder.yudao.module.dcc.dal.dataobject.productcatalog.DccProductCatalogDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.relation.DccDataRelationDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateVersionDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateSnapshotDO;
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
import static org.junit.jupiter.api.Assertions.assertFalse;

class DccProductCatalogTenantIsolationTest {
    @Test
    @SuppressWarnings("unchecked")
    void configuredInterceptorSharesCatalogAndScopesTenantOwnedBindings() throws Exception {
        // Exercise the shipped interceptor and configuration, including selectById used by registration sync.
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
        Map<String, Class<?>> privateTables = Map.of(
                "dcc_project_code", DccProjectCodeDO.class,
                "dcc_data_relation", DccDataRelationDO.class,
                "dcc_registration_certificate", DccRegistrationCertificateDO.class,
                "dcc_registration_certificate_version", DccRegistrationCertificateVersionDO.class,
                "dcc_registration_certificate_snapshot", DccRegistrationCertificateSnapshotDO.class);
        privateTables.values().forEach(type -> TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), type));
        var interceptor = new TenantLineInnerInterceptor(new TenantDatabaseInterceptor(properties));
        var explicitSharing = new TenantLineInnerInterceptor(new TenantDatabaseInterceptor(new TenantProperties()));
        try {
            for (long tenantId : new long[] {1L, 2L}) {
                TenantContextHolder.setTenantId(tenantId);
                for (String sql : List.of(
                        "SELECT * FROM dcc_product_catalog WHERE deleted = 0",
                        "SELECT * FROM dcc_product_catalog WHERE id = 42",
                        "UPDATE dcc_product_catalog SET product = 'updated' WHERE id = 42",
                        "DELETE FROM dcc_product_catalog WHERE id = 42")) {
                    String scoped = interceptor.parserSingle(sql, null);
                    assertFalse(scoped.contains("tenant_id"), scoped);
                    assertFalse(explicitSharing.parserSingle(sql, null).contains("tenant_id"), sql);
                }
                String inserted = interceptor.parserSingle(
                        "INSERT INTO dcc_product_catalog (product, tenant_id) VALUES ('new', 1)", null);
                assertTrue(inserted.contains("tenant_id"), inserted);
                assertTrue(inserted.contains("'new', 1"), inserted);
                for (String table : privateTables.keySet()) {
                    for (String sql : List.of("SELECT * FROM " + table + " WHERE id = 42",
                            "UPDATE " + table + " SET deleted = 1 WHERE id = 42",
                            "DELETE FROM " + table + " WHERE id = 42")) {
                        String scoped = interceptor.parserSingle(sql, null);
                        assertTrue(scoped.contains("tenant_id = " + tenantId), scoped);
                    }
                }
            }
        } finally {
            TenantContextHolder.clear();
        }
    }
}
