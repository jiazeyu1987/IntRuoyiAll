package cn.iocoder.yudao.module.infra.service.config;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.ArrayUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.test.core.util.RandomUtils;
import cn.iocoder.yudao.module.infra.controller.admin.config.vo.ConfigPageReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.config.vo.ConfigSaveReqVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.config.ConfigDO;
import cn.iocoder.yudao.module.infra.dal.mysql.config.ConfigMapper;
import cn.iocoder.yudao.module.infra.enums.config.ConfigTypeEnum;
import cn.iocoder.yudao.module.infra.enums.config.PasswordMaxAgeConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;
import java.util.function.Consumer;
import java.util.List;

import static cn.hutool.core.util.RandomUtil.randomEle;
import static cn.iocoder.yudao.framework.common.util.date.LocalDateTimeUtils.buildBetweenTime;
import static cn.iocoder.yudao.framework.common.util.date.LocalDateTimeUtils.buildTime;
import static cn.iocoder.yudao.framework.common.util.object.ObjectUtils.cloneIgnoreId;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertPojoEquals;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.*;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;

@Import({ConfigServiceImpl.class, TenantConfigService.class})
public class ConfigServiceImplTest extends BaseDbUnitTest {

    private static final String IDLE_LOGOUT_MINUTES_KEY = "system.login.idle-timeout-minutes";
    private static final String TEST_MODE_KEY = "system.ui.test-mode-enabled";

    @Test
    void platformSettingsCannotBeCreatedThroughTenantParameters() {
        for (String key : List.of("system.gxp.audit-mode", "runtime-control.release-package.backend-runtime-base-mode")) {
            ConfigSaveReqVO request = randomPojo(ConfigSaveReqVO.class, row -> {
                row.setId(null); row.setKey(key); row.setValue("FORMAL"); row.setVisible(true);
            });
            assertServiceException(() -> configService.createConfig(request), CONFIG_PLATFORM_READ_ONLY);
        }
    }

    @Test
    public void testCreateConfig_testModeRejectsInvalidValue() {
        for (String value : new String[] { "", "1", "TRUE", " false ", "enabled" }) {
            ConfigSaveReqVO reqVO = randomPojo(ConfigSaveReqVO.class, o -> {
                o.setId(null);
                o.setKey(TEST_MODE_KEY);
                o.setValue(value);
            });
            assertServiceException(() -> configService.createConfig(reqVO), CONFIG_TEST_MODE_INVALID);
        }
    }

    @Test
    public void testUpdateConfig_testModePersistsBothStates() {
        ConfigDO config = randomConfigDO(o -> {
            o.setConfigKey(TEST_MODE_KEY);
            o.setValue("false");
        });
        insertTestConfig(config);
        for (String value : new String[] { "true", "false" }) {
            ConfigSaveReqVO reqVO = randomPojo(ConfigSaveReqVO.class, o -> {
                o.setId(config.getId());
                o.setKey(TEST_MODE_KEY);
                o.setValue(value);
            });
            configService.updateConfig(reqVO);
            assertEquals(value, configService.getConfig(config.getId()).getValue());
        }
    }

    @Test
    public void testUpdateConfig_testModeRejectsInvalidValueWithoutChangingStoredState() {
        ConfigDO config = randomConfigDO(o -> {
            o.setConfigKey(TEST_MODE_KEY);
            o.setValue("false");
        });
        insertTestConfig(config);
        ConfigSaveReqVO reqVO = randomPojo(ConfigSaveReqVO.class, o -> {
            o.setId(config.getId());
            o.setKey(TEST_MODE_KEY);
            o.setValue("TRUE");
        });
        assertServiceException(() -> configService.updateConfig(reqVO), CONFIG_TEST_MODE_INVALID);
        assertEquals("false", configService.getConfig(config.getId()).getValue());
    }

    @Resource
    private ConfigServiceImpl configService;

    @Resource
    private cn.iocoder.yudao.module.infra.dal.mysql.config.TenantConfigMapper tenantConfigMapper;

    @Test
    public void testCreateConfig_success() {
        // 准备参数
        ConfigSaveReqVO reqVO = randomPojo(ConfigSaveReqVO.class)
                .setId(null); // 防止 id 被赋值，导致唯一性校验失败

        // 调用
        Long configId = configService.createConfig(reqVO);
        // 断言
        assertNotNull(configId);
        // 校验记录的属性是否正确
        ConfigDO config = configService.getConfig(configId);
        assertPojoEquals(reqVO, config, "id");
        assertEquals(ConfigTypeEnum.CUSTOM.getType(), config.getType());
    }

    @Test
    public void testUpdateConfig_success() {
        // mock 数据
        ConfigDO dbConfig = randomConfigDO();
        insertTestConfig(dbConfig);// @Sql: 先插入出一条存在的数据
        // 准备参数
        ConfigSaveReqVO reqVO = randomPojo(ConfigSaveReqVO.class, o -> {
            o.setId(dbConfig.getId()); // 设置更新的 ID
        });

        // 调用
        configService.updateConfig(reqVO);
        // 校验是否更新正确
        ConfigDO config = configService.getConfig(reqVO.getId()); // 获取最新的
        assertPojoEquals(reqVO, config);
    }

    @Test
    public void testCreateConfig_idleLogoutMinutesRejectsInvalidValue() {
        ConfigSaveReqVO reqVO = randomPojo(ConfigSaveReqVO.class, o -> {
            o.setId(null);
            o.setKey(IDLE_LOGOUT_MINUTES_KEY);
            o.setValue("0");
        });

        assertServiceException(() -> configService.createConfig(reqVO), CONFIG_IDLE_LOGOUT_MINUTES_INVALID);
    }

    @Test
    public void testUpdateConfig_idleLogoutMinutesRejectsInvalidValue() {
        ConfigDO dbConfig = randomConfigDO(o -> {
            o.setConfigKey(IDLE_LOGOUT_MINUTES_KEY);
            o.setValue("15");
        });
        insertTestConfig(dbConfig);
        ConfigSaveReqVO reqVO = randomPojo(ConfigSaveReqVO.class, o -> {
            o.setId(dbConfig.getId());
            o.setKey(IDLE_LOGOUT_MINUTES_KEY);
            o.setValue("1441");
        });

        assertServiceException(() -> configService.updateConfig(reqVO), CONFIG_IDLE_LOGOUT_MINUTES_INVALID);
    }

    @Test
    public void testUpdateConfig_idleLogoutMinutesAcceptsValidValue() {
        ConfigDO dbConfig = randomConfigDO(o -> {
            o.setConfigKey(IDLE_LOGOUT_MINUTES_KEY);
            o.setValue("15");
        });
        insertTestConfig(dbConfig);
        ConfigSaveReqVO reqVO = randomPojo(ConfigSaveReqVO.class, o -> {
            o.setId(dbConfig.getId());
            o.setKey(IDLE_LOGOUT_MINUTES_KEY);
            o.setValue("30");
        });

        configService.updateConfig(reqVO);

        ConfigDO config = configService.getConfig(reqVO.getId());
        assertEquals("30", config.getValue());
    }

    @Test
    public void testDeleteConfig_success() {
        // mock 数据
        ConfigDO dbConfig = randomConfigDO(o -> {
            o.setType(ConfigTypeEnum.CUSTOM.getType()); // 只能删除 CUSTOM 类型
        });
        insertTestConfig(dbConfig);// @Sql: 先插入出一条存在的数据
        // 准备参数
        Long id = dbConfig.getId();

        // 调用
        configService.deleteConfig(id);
        // 校验数据不存在了
        assertNull(configService.getConfig(id));
    }

    @Test
    public void passwordMaxAgeCanBeSavedAndReadButNotRenamedOrDeleted() {
        ConfigSaveReqVO req = randomPojo(ConfigSaveReqVO.class, o -> {
            o.setId(null);
            o.setKey(PasswordMaxAgeConfig.KEY);
            o.setValue("90");
        });
        Long id = configService.createConfig(req);
        assertEquals(ConfigTypeEnum.SYSTEM.getType(), configService.getConfig(id).getType());
        req.setId(id);
        req.setValue("180");
        configService.updateConfig(req);
        assertEquals("180", configService.getConfigByKey(PasswordMaxAgeConfig.KEY).getValue());

        req.setKey("renamed.password-policy");
        assertServiceException(() -> configService.updateConfig(req), CONFIG_PASSWORD_MAX_AGE_KEY_IMMUTABLE);
        assertServiceException(() -> configService.deleteConfig(id), CONFIG_CAN_NOT_DELETE_SYSTEM_TYPE);
        assertServiceException(() -> configService.deleteConfigList(java.util.List.of(id)), CONFIG_CAN_NOT_DELETE_SYSTEM_TYPE);
        assertEquals("180", configService.getConfigByKey(PasswordMaxAgeConfig.KEY).getValue());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "1.5", " 90", "090", "abc", "2147483648"})
    public void passwordMaxAgeRejectsInvalidCreateAndUpdateWithoutChangingStoredValue(String value) {
        ConfigSaveReqVO req = randomPojo(ConfigSaveReqVO.class, o -> {
            o.setId(null);
            o.setKey(PasswordMaxAgeConfig.KEY);
            o.setValue(value);
        });
        assertServiceException(() -> configService.createConfig(req), CONFIG_PASSWORD_MAX_AGE_INVALID);
        assertNull(configService.getConfigByKey(PasswordMaxAgeConfig.KEY));
        req.setValue("90");
        req.setId(configService.createConfig(req));
        req.setValue(value);
        assertServiceException(() -> configService.updateConfig(req), CONFIG_PASSWORD_MAX_AGE_INVALID);
        assertEquals("90", configService.getConfigByKey(PasswordMaxAgeConfig.KEY).getValue());
    }

    @Test
    public void testDeleteConfig_canNotDeleteSystemType() {
        // mock 数据
        ConfigDO dbConfig = randomConfigDO(o -> {
            o.setType(ConfigTypeEnum.SYSTEM.getType()); // SYSTEM 不允许删除
        });
        insertTestConfig(dbConfig);// @Sql: 先插入出一条存在的数据
        // 准备参数
        Long id = dbConfig.getId();

        // 调用, 并断言异常
        assertServiceException(() -> configService.deleteConfig(id), CONFIG_CAN_NOT_DELETE_SYSTEM_TYPE);
    }

    @Test
    public void testValidateConfigExists_success() {
        // mock 数据
        ConfigDO dbConfigDO = randomConfigDO();
        insertTestConfig(dbConfigDO);// @Sql: 先插入出一条存在的数据

        // 调用成功
        configService.validateConfigExists(dbConfigDO.getId());
    }

    @Test
    public void testValidateConfigExist_notExists() {
        assertServiceException(() -> configService.validateConfigExists(randomLongId()), CONFIG_NOT_EXISTS);
    }

    @Test
    public void testValidateConfigKeyUnique_success() {
        // 调用，成功
        configService.validateConfigKeyUnique(randomLongId(), randomString());
    }

    @Test
    public void testValidateConfigKeyUnique_keyDuplicateForCreate() {
        // 准备参数
        String key = randomString();
        // mock 数据
        insertTestConfig(randomConfigDO(o -> o.setConfigKey(key)));

        // 调用，校验异常
        assertServiceException(() -> configService.validateConfigKeyUnique(null, key),
                CONFIG_KEY_DUPLICATE);
    }

    @Test
    public void testValidateConfigKeyUnique_keyDuplicateForUpdate() {
        // 准备参数
        Long id = randomLongId();
        String key = randomString();
        // mock 数据
        insertTestConfig(randomConfigDO(o -> o.setConfigKey(key)));

        // 调用，校验异常
        assertServiceException(() -> configService.validateConfigKeyUnique(id, key),
                CONFIG_KEY_DUPLICATE);
    }

    @Test
    public void testGetConfigPage() {
        // mock 数据
        ConfigDO dbConfig = randomConfigDO(o -> { // 等会查询到
            o.setName("芋艿");
            o.setConfigKey("yunai");
            o.setType(ConfigTypeEnum.SYSTEM.getType());
            o.setCreateTime(buildTime(2021, 2, 1));
        });
        insertTestConfig(dbConfig);
        // 测试 name 不匹配
        insertTestConfig(cloneIgnoreId(dbConfig, o -> { o.setName("土豆"); o.setConfigKey("yunai-name"); }));
        // 测试 key 不匹配
        insertTestConfig(cloneIgnoreId(dbConfig, o -> o.setConfigKey("tudou")));
        // 测试 type 不匹配
        insertTestConfig(cloneIgnoreId(dbConfig, o -> { o.setType(ConfigTypeEnum.CUSTOM.getType()); o.setConfigKey("yunai-type"); }));
        // 测试 createTime 不匹配
        insertTestConfig(cloneIgnoreId(dbConfig, o -> { o.setCreateTime(buildTime(2021, 1, 1)); o.setConfigKey("yunai-time"); }));
        // 准备参数
        ConfigPageReqVO reqVO = new ConfigPageReqVO();
        reqVO.setName("艿");
        reqVO.setKey("nai");
        reqVO.setType(ConfigTypeEnum.SYSTEM.getType());
        reqVO.setCreateTime(buildBetweenTime(2021, 1, 15, 2021, 2, 15));

        // 调用
        PageResult<ConfigDO> pageResult = configService.getConfigPage(reqVO);
        // 断言
        assertEquals(1, pageResult.getTotal());
        assertEquals(1, pageResult.getList().size());
        assertPojoEquals(dbConfig, pageResult.getList().get(0));
    }

    @Test
    public void testGetConfig() {
        // mock 数据
        ConfigDO dbConfig = randomConfigDO();
        insertTestConfig(dbConfig);// @Sql: 先插入出一条存在的数据
        // 准备参数
        Long id = dbConfig.getId();

        // 调用
        ConfigDO config = configService.getConfig(id);
        // 断言
        assertNotNull(config);
        assertPojoEquals(dbConfig, config);
    }

    @Test
    public void testGetConfigByKey() {
        // mock 数据
        ConfigDO dbConfig = randomConfigDO();
        insertTestConfig(dbConfig);// @Sql: 先插入出一条存在的数据
        // 准备参数
        String key = dbConfig.getConfigKey();

        // 调用
        ConfigDO config = configService.getConfigByKey(key);
        // 断言
        assertNotNull(config);
        assertPojoEquals(dbConfig, config);
    }

    private void insertTestConfig(ConfigDO config) {
        var row = cn.iocoder.yudao.framework.common.util.object.BeanUtils.toBean(config,
                cn.iocoder.yudao.module.infra.dal.dataobject.config.TenantConfigDO.class);
        row.setTenantId(cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId());
        tenantConfigMapper.insert(row);
        config.setId(row.getId());
    }

    // ========== 随机对象 ==========

    @SafeVarargs
    private static ConfigDO randomConfigDO(Consumer<ConfigDO>... consumers) {
        Consumer<ConfigDO> consumer = (o) -> {
            o.setType(randomEle(ConfigTypeEnum.values()).getType()); // 保证 key 的范围
        };
        return RandomUtils.randomPojo(ConfigDO.class, ArrayUtils.append(consumer, consumers));
    }

}
