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

import static org.junit.jupiter.api.Assertions.*;

class DccProductCatalogTenantIsolationTest {
    @Test
    @SuppressWarnings("unchecked")
    void configuredInterceptorScopesCatalogAndBindingsToCurrentTenant() throws Exception {
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
        var defaultIsolation = new TenantLineInnerInterceptor(new TenantDatabaseInterceptor(new TenantProperties()));
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
                    assertTrue(defaultIsolation.parserSingle(sql, null).contains("tenant_id = " + tenantId), sql);
                }
                String inserted = interceptor.parserSingle(
                        "INSERT INTO dcc_product_catalog (product) VALUES ('new')", null);
                assertTrue(inserted.contains("tenant_id"), inserted);
                assertTrue(inserted.contains("'new', " + tenantId), inserted);
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
    @Test
    void newTenantSeesNoCatalogAndCannotChangeAnotherTenantsRows() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), DccProductCatalogDO.class);
        var interceptor = new TenantLineInnerInterceptor(new TenantDatabaseInterceptor(new TenantProperties()));
        var jdbc = new org.springframework.jdbc.core.JdbcTemplate(new org.springframework.jdbc.datasource.DriverManagerDataSource(
                "jdbc:h2:mem:" + java.util.UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE dcc_product_catalog(id BIGINT PRIMARY KEY, product VARCHAR(100), tenant_id BIGINT NOT NULL, deleted INT DEFAULT 0)");
        jdbc.update("INSERT INTO dcc_product_catalog VALUES(1,'已有产品',165,0)");
        try {
            TenantContextHolder.setTenantId(170L);
            assertEquals(0, jdbc.queryForList(interceptor.parserSingle("SELECT * FROM dcc_product_catalog WHERE deleted=0", null)).size());
            assertEquals(0, jdbc.update(interceptor.parserSingle("UPDATE dcc_product_catalog SET product='越权' WHERE id=1", null)));
            assertEquals(0, jdbc.update(interceptor.parserSingle("DELETE FROM dcc_product_catalog WHERE id=1", null)));
            jdbc.update(interceptor.parserSingle("INSERT INTO dcc_product_catalog(id,product) VALUES(2,'新租户产品')", null));
            assertEquals(170L, jdbc.queryForObject("SELECT tenant_id FROM dcc_product_catalog WHERE id=2", Long.class));
            TenantContextHolder.setTenantId(165L);
            assertEquals("已有产品", jdbc.queryForObject(interceptor.parserSingle("SELECT product FROM dcc_product_catalog WHERE id=1", null), String.class));
            assertEquals(1, jdbc.queryForList(interceptor.parserSingle("SELECT * FROM dcc_product_catalog", null)).size());
        } finally {
            TenantContextHolder.clear();
        }
    }

}
