package cn.iocoder.yudao.module.infra.service.config;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.infra.controller.admin.config.vo.ConfigPageReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.config.vo.ConfigSaveReqVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.config.ConfigDO;
import cn.iocoder.yudao.module.infra.dal.mysql.config.ConfigMapper;
import cn.iocoder.yudao.module.infra.enums.config.ConfigTypeEnum;
import cn.iocoder.yudao.module.infra.enums.config.PasswordMaxAgeConfig;
import cn.iocoder.yudao.module.infra.enums.config.GxpAuditMode;
import com.google.common.annotations.VisibleForTesting;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.regex.Pattern;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.*;

/**
 * 参数配置 Service 实现类
 */
@Service
@Slf4j
@Validated
public class ConfigServiceImpl implements ConfigService {

    public static final String IDLE_LOGOUT_MINUTES_CONFIG_KEY = "system.login.idle-timeout-minutes";
    public static final String TEST_MODE_CONFIG_KEY = "system.ui.test-mode-enabled";
    private static final int IDLE_LOGOUT_MINUTES_MIN = 1;
    private static final int IDLE_LOGOUT_MINUTES_MAX = 1440;
    private static final Pattern POSITIVE_INTEGER_PATTERN = Pattern.compile("^[1-9]\\d*$");

    @Resource
    private TenantConfigService tenantConfigService;

    @Resource
    private ConfigMapper platformConfigMapper;

    @Override
    public Long createConfig(ConfigSaveReqVO createReqVO) {
        TenantConfigService.requireTenantKey(createReqVO.getKey());
        validateConfigValue(createReqVO.getKey(), createReqVO.getValue());
        // 校验参数配置 key 的唯一性
        validateConfigKeyUnique(null, createReqVO.getKey());

        // 插入参数配置
        Integer type = PasswordMaxAgeConfig.KEY.equals(createReqVO.getKey())
                ? ConfigTypeEnum.SYSTEM.getType() : ConfigTypeEnum.CUSTOM.getType();
        return tenantConfigService.createConfig(createReqVO, type);
    }

    @Override
    public void updateConfig(ConfigSaveReqVO updateReqVO) {
        // 校验自己存在
        ConfigDO current = validateConfigExists(updateReqVO.getId());
        TenantConfigService.requireTenantKey(updateReqVO.getKey());
        if (PasswordMaxAgeConfig.KEY.equals(current.getConfigKey())
                && !PasswordMaxAgeConfig.KEY.equals(updateReqVO.getKey())) {
            throw exception(CONFIG_PASSWORD_MAX_AGE_KEY_IMMUTABLE);
        }
        validateConfigValue(updateReqVO.getKey(), updateReqVO.getValue());
        // 校验参数配置 key 的唯一性
        validateConfigKeyUnique(updateReqVO.getId(), updateReqVO.getKey());

        // 更新参数配置
        Integer type = PasswordMaxAgeConfig.KEY.equals(updateReqVO.getKey()) ? ConfigTypeEnum.SYSTEM.getType() : null;
        tenantConfigService.updateConfig(updateReqVO, type);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public void deleteConfig(Long id) {
        // 校验配置存在
        ConfigDO config = validateConfigExists(id);
        // 内置配置，不允许删除
        if (ConfigTypeEnum.SYSTEM.getType().equals(config.getType())) {
            throw exception(CONFIG_CAN_NOT_DELETE_SYSTEM_TYPE);
        }
        // 删除
        tenantConfigService.deleteConfigs(List.of(id));
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public void deleteConfigList(List<Long> ids) {
        if (ids.isEmpty()) return;
        List<Long> uniqueIds = ids.stream().distinct().toList();
        for (Long id : uniqueIds) {
            ConfigDO config = validateConfigExists(id);
            if (ConfigTypeEnum.SYSTEM.getType().equals(config.getType())) {
                throw exception(CONFIG_CAN_NOT_DELETE_SYSTEM_TYPE);
            }
        }
        tenantConfigService.deleteConfigs(uniqueIds);
    }

    @Override
    public ConfigDO getConfig(Long id) {
        return tenantConfigService.getConfig(id);
    }

    @Override
    public ConfigDO getConfigByKey(String key) {
        // Explicit read-only platform definition; never a fallback for missing tenant configuration.
        if (GxpAuditMode.CONFIG_KEY.equals(key)) return platformConfigMapper.selectByKey(key);
        return tenantConfigService.getConfigByKey(key);
    }

    @Override
    public PageResult<ConfigDO> getConfigPage(ConfigPageReqVO pageReqVO) {
        return tenantConfigService.getConfigPage(pageReqVO);
    }

    @VisibleForTesting
    public ConfigDO validateConfigExists(Long id) {
        if (id == null) {
            throw exception(CONFIG_NOT_EXISTS);
        }
        ConfigDO config = tenantConfigService.getConfig(id);
        if (config == null) {
            throw exception(CONFIG_NOT_EXISTS);
        }
        return config;
    }

    @VisibleForTesting
    public void validateConfigKeyUnique(Long id, String key) {
        ConfigDO config = tenantConfigService.getConfigByKey(key);
        if (config == null) {
            return;
        }
        // 如果 id 为空，说明不用比较是否为相同 id 的参数配置
        if (id == null) {
            throw exception(CONFIG_KEY_DUPLICATE);
        }
        if (!config.getId().equals(id)) {
            throw exception(CONFIG_KEY_DUPLICATE);
        }
    }

    private void validateConfigValue(String key, String value) {
        if (PasswordMaxAgeConfig.KEY.equals(key)) {
            PasswordMaxAgeConfig.parseRequired(value);
            return;
        }
        if (TEST_MODE_CONFIG_KEY.equals(key)) {
            if (!"true".equals(value) && !"false".equals(value)) {
                throw exception(CONFIG_TEST_MODE_INVALID);
            }
            return;
        }
        if (!IDLE_LOGOUT_MINUTES_CONFIG_KEY.equals(key)) {
            return;
        }
        if (value == null || !POSITIVE_INTEGER_PATTERN.matcher(value).matches()) {
            throw exception(CONFIG_IDLE_LOGOUT_MINUTES_INVALID);
        }
        int minutes;
        try {
            minutes = Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw exception(CONFIG_IDLE_LOGOUT_MINUTES_INVALID);
        }
        if (minutes < IDLE_LOGOUT_MINUTES_MIN || minutes > IDLE_LOGOUT_MINUTES_MAX) {
            throw exception(CONFIG_IDLE_LOGOUT_MINUTES_INVALID);
        }
    }

}
