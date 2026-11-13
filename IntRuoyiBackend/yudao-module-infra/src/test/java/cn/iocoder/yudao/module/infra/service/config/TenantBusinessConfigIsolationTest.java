package cn.iocoder.yudao.module.infra.service.config;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.infra.controller.admin.config.vo.ConfigPageReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.config.vo.ConfigSaveReqVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.config.ConfigDO;
import cn.iocoder.yudao.module.infra.dal.mysql.config.ConfigMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Import({ConfigServiceImpl.class, TenantConfigService.class,
        cn.iocoder.yudao.module.infra.service.file.NasSettingsServiceImpl.class})
class TenantBusinessConfigIsolationTest extends BaseDbUnitTest {
    @Resource private ConfigService configService;
    @Resource private TenantConfigService tenantConfigService;
    @Resource private ConfigMapper configMapper;
    @Resource private PlatformTransactionManager transactionManager;
    @Resource private cn.iocoder.yudao.module.infra.service.file.NasSettingsService nasSettingsService;

    @Test
    void changingNasServerCredentialsAndShareOnlyChangesTheCurrentTenant() {
        var request = new cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileNasConfigSaveReqVO()
                .setServer("nas-a").setShare("tenant-a").setUsername("user-a").setPassword("fixture-a");
        TenantContextHolder.setTenantId(11L);
        nasSettingsService.saveNasConfig(request);
        TenantContextHolder.setTenantId(22L);
        nasSettingsService.saveNasConfig(request.setServer("nas-b").setShare("tenant-b")
                .setUsername("user-b").setPassword("fixture-b"));
        assertEquals("nas-b", nasSettingsService.getNasConfig().getServer());
        TenantContextHolder.setTenantId(11L);
        assertEquals("nas-a", nasSettingsService.getNasConfig().getServer());
        assertEquals("tenant-a", nasSettingsService.getNasConfig().getShare());
        assertEquals("fixture-a", nasSettingsService.getNasConfig().getPassword());
    }

    @Test
    void optionalSettingsCanBeRemovedAndRecreatedRepeatedly() {
        for (int i = 0; i < 3; i++) {
            Long id = configService.createConfig(request("15"));
            configService.deleteConfig(id);
            assertNull(configService.getConfigByKey("system.login.idle-timeout-minutes"));
        }
    }

    @Test
    void genericReadUpdatePageAndBatchDeleteStayWithinTenantEvenWithIgnoredInterceptor() {
        TenantContextHolder.setTenantId(11L);
        Long first = configService.createConfig(request("15"));
        TenantContextHolder.setTenantId(22L);
        Long second = configService.createConfig(request("30"));
        TenantContextHolder.setIgnore(true);
        assertNull(configService.getConfig(first));
        assertEquals(List.of(second), configService.getConfigPage(new ConfigPageReqVO()).getList()
                .stream().map(ConfigDO::getId).toList());
        ConfigSaveReqVO update = request("60");
        update.setId(first);
        assertThrows(RuntimeException.class, () -> configService.updateConfig(update));
        assertThrows(RuntimeException.class, () -> configService.deleteConfig(first));
        assertThrows(RuntimeException.class, () -> configService.deleteConfigList(List.of(second, first)));
        assertEquals("30", configService.getConfig(second).getValue());
        update.setId(second);
        configService.updateConfig(update);
        configService.deleteConfigList(List.of(second));
        TenantContextHolder.setTenantId(11L);
        assertEquals("15", configService.getConfig(first).getValue());
    }

    @Test
    void missingTenantConfigurationNeverReadsTheOldGlobalValue() {
        configMapper.insert(template("system.login.idle-timeout-minutes", "120"));
        assertNull(configService.getConfigByKey("system.login.idle-timeout-minutes"));
        assertEquals(0, configService.getConfigPage(new ConfigPageReqVO()).getTotal());
    }

    @Test
    void initializationCopiesValuesOnceAndExcludesPlatformSettings() {
        ConfigDO source = template("system.login.idle-timeout-minutes", "15");
        configMapper.insert(source);
        configMapper.insert(template("system.gxp.audit-mode", "FORMAL"));
        configMapper.insert(template("runtime-control.release-package.backend-runtime-base-mode", "offline-tar"));
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> tenantConfigService.initializeFromPlatformTemplate());
        assertEquals(1, configService.getConfigPage(new ConfigPageReqVO()).getTotal());
        source.setValue("120");
        configMapper.updateById(source);
        assertEquals("15", configService.getConfigByKey(source.getConfigKey()).getValue());
        assertThrows(RuntimeException.class, () -> new TransactionTemplate(transactionManager)
                .executeWithoutResult(status -> tenantConfigService.initializeFromPlatformTemplate()));
        assertEquals("15", configService.getConfigByKey(source.getConfigKey()).getValue());
    }

    @Test
    void businessQueriesRequireTenantAndPlatformKeysCannotReplaceTenantKeys() {
        Long id = configService.createConfig(request("15"));
        ConfigSaveReqVO update = request("FORMAL");
        update.setId(id);
        update.setKey("system.gxp.audit-mode");
        assertThrows(RuntimeException.class, () -> configService.updateConfig(update));
        TenantContextHolder.clear();
        assertThrows(RuntimeException.class, () -> configService.getConfig(id));
        assertThrows(RuntimeException.class, () -> configService.getConfigPage(new ConfigPageReqVO()));
    }

    private ConfigSaveReqVO request(String value) {
        return new ConfigSaveReqVO().setCategory("system").setName("Idle timeout")
                .setKey("system.login.idle-timeout-minutes").setValue(value).setVisible(true);
    }

    private ConfigDO template(String key, String value) {
        return new ConfigDO().setCategory("system").setName("Template")
                .setConfigKey(key).setValue(value).setVisible(true).setType(2);
    }
}
