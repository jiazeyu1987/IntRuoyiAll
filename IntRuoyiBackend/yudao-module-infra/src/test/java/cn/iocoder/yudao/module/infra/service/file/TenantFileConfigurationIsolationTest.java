package cn.iocoder.yudao.module.infra.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileConfigDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileConfigMapper;
import cn.iocoder.yudao.module.infra.framework.file.core.client.*;
import cn.iocoder.yudao.module.infra.framework.file.core.client.local.LocalFileClientConfig;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@Import({FileConfigServiceImpl.class, TenantFileConfigInitializer.class, TenantFileConfigurationIsolationTest.SqlConfig.class,
        org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration.class})
class TenantFileConfigurationIsolationTest extends BaseDbUnitTest {
    @Resource private FileConfigService service;
    @Resource private FileConfigMapper mapper;
    @Resource private JdbcTemplate jdbc;
    @Resource private TenantFileConfigInitializer initializer;
    @Resource private org.springframework.transaction.PlatformTransactionManager transactions;
    @MockitoBean private jakarta.validation.Validator validator;
    @MockitoBean private ProtectedFileClientProvider protectedProvider;

    @Test void reservedRuntimeStorageRequiresExplicitProviderAndTenant() {
        org.mockito.Mockito.when(protectedProvider.getFileConfigId()).thenReturn(-10L);
        var client = org.mockito.Mockito.mock(FileClient.class);
        org.mockito.Mockito.when(protectedProvider.getFileClient()).thenReturn(client);
        TenantContextHolder.setTenantId(11L);
        assertSame(client,service.getFileClient(-10L));
        assertThrows(IllegalStateException.class,()->service.getFileClient(-11L));
        TenantContextHolder.clear();
        assertThrows(RuntimeException.class,()->service.getFileClient(-10L));
        org.mockito.Mockito.verify(protectedProvider).getFileClient();
    }

    @Test void sameIdHasIndependentDefaultAndClientAndIgnoredContextCannotBypassTenant() {
        for (long tenant : new long[]{11,22}) {
            TenantContextHolder.setTenantId(tenant);
            jdbc.update("INSERT INTO infra_tenant_file_config_snapshot (tenant_id) VALUES (?)",tenant);
            mapper.insert(config(10L,"First",true,tenant));
            mapper.insert(config(20L,"Second",false,tenant));
        }
        var secondTenantClient=service.getFileClient(10L);
        TenantContextHolder.setTenantId(11L);
        TenantContextHolder.setIgnore(true);
        var firstTenantClient=service.getFileClient(10L);
        assertNotSame(firstTenantClient,secondTenantClient);
        service.updateFileConfigMaster(20L);
        assertEquals(20L,service.getMasterFileClient().getId());
        TenantContextHolder.setTenantId(22L);
        assertEquals(10L,service.getMasterFileClient().getId());
        assertSame(secondTenantClient,service.getFileClient(10L));
        assertEquals(2L,service.getFileConfigPage(new cn.iocoder.yudao.module.infra.controller.admin.file.vo.config.FileConfigPageReqVO()).getTotal());
        TenantContextHolder.clear();
        assertThrows(RuntimeException.class,()->service.getFileClient(10L));
    }

    @Test void foreignIdInDeleteBatchRejectsAllAndDeletedConfigCannotReuseFactoryClient() {
        TenantContextHolder.setTenantId(11L);
        mapper.insert(config(10L,"A",false,11));
        TenantContextHolder.setTenantId(22L);
        mapper.insert(config(20L,"B",false,22));
        service.getFileClient(20L);
        assertThrows(RuntimeException.class,()->service.deleteFileConfigList(List.of(20L,10L)));
        assertNotNull(service.getFileConfig(20L));
        service.deleteFileConfig(20L);
        assertThrows(RuntimeException.class,()->service.getFileClient(20L));
        TenantContextHolder.setTenantId(11L);
        assertNotNull(service.getFileConfig(10L));
    }

    @Test void initializationRequiresCreationTransactionAndRollsBackWithIt() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS system_tenant (id bigint PRIMARY KEY)");
        jdbc.update("INSERT INTO system_tenant (id) VALUES (101),(102)");
        try {
            jdbc.update("INSERT INTO infra_file_config (id,name,storage,master,config) VALUES (10,'Template',10,TRUE,'{}')");
            var tx = new org.springframework.transaction.support.TransactionTemplate(transactions);
            TenantContextHolder.setTenantId(101L);
            assertThrows(RuntimeException.class, () -> initializer.initialize());
            tx.executeWithoutResult(status -> initializer.initialize());
            assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM infra_tenant_file_config WHERE tenant_id=101 AND id=10",Long.class));
            TenantContextHolder.setTenantId(102L);
            assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(status -> {
                initializer.initialize();
                throw new IllegalStateException("LATER_CREATION_FAILURE");
            }));
            assertEquals(0L,jdbc.queryForObject("SELECT COUNT(*) FROM infra_tenant_file_config WHERE tenant_id=102",Long.class));
            assertEquals(0L,jdbc.queryForObject("SELECT COUNT(*) FROM infra_tenant_file_config_snapshot WHERE tenant_id=102",Long.class));
        } finally {
            jdbc.update("DELETE FROM system_tenant WHERE id IN (101,102)");
        }
    }

    private FileConfigDO config(Long id,String name,boolean master,long tenant) {
        var row=new FileConfigDO().setId(id).setName(name).setStorage(10).setMaster(master)
                .setConfig(new LocalFileClientConfig().setBasePath("tenant-"+tenant).setDomain("https://tenant-"+tenant+".invalid"));
        row.setTenantId(tenant);
        return row;
    }

    @TestConfiguration(proxyBeanMethods=false)
    static class SqlConfig {
        @Bean JdbcTemplate jdbc(javax.sql.DataSource ds) { return new JdbcTemplate(ds); }
        @Bean FileClientFactory factory() { return new FileClientFactoryImpl(); }
        @Bean @Lazy(false) org.springframework.beans.factory.InitializingBean interceptor(com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor interceptor) {
            return ()->cn.iocoder.yudao.framework.mybatis.core.util.MyBatisUtils.addInterceptor(interceptor,
                new com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor(
                    new cn.iocoder.yudao.framework.tenant.core.db.TenantDatabaseInterceptor(new cn.iocoder.yudao.framework.tenant.config.TenantProperties())),0);
        }
    }
}
