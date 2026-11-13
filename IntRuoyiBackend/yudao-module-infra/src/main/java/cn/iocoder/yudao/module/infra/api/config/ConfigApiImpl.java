package cn.iocoder.yudao.module.infra.api.config;

import cn.iocoder.yudao.module.infra.dal.dataobject.config.ConfigDO;
import cn.iocoder.yudao.module.infra.service.config.ConfigService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 * 参数配置 API 实现类
 *
 * @author 瑛泰源码
 */
@Service
@Validated
public class ConfigApiImpl implements ConfigApi {

    @Resource
    private ConfigService configService;

    @Resource
    private cn.iocoder.yudao.module.infra.service.config.TenantConfigService tenantConfigService;

    @Resource
    private cn.iocoder.yudao.module.infra.service.file.TenantFileConfigInitializer fileConfigInitializer;

    @Override
    public void initializeTenantConfiguration() {
        tenantConfigService.initializeFromPlatformTemplate();
        fileConfigInitializer.initialize();
    }

    @Override
    public String getConfigValueByKey(String key) {
        ConfigDO config = configService.getConfigByKey(key);
        return config != null ? config.getValue() : null;
    }

}
