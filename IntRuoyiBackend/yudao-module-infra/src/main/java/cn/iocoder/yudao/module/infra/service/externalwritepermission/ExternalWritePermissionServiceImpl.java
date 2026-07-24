package cn.iocoder.yudao.module.infra.service.externalwritepermission;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.infra.controller.admin.config.vo.ConfigSaveReqVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.config.ConfigDO;
import cn.iocoder.yudao.module.infra.service.config.ConfigService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class ExternalWritePermissionServiceImpl implements ExternalWritePermissionService {

    private static final String CONFIG_CATEGORY = "erp";
    private static final String ERP_EXTERNAL_WRITE_ENABLED_CONFIG_NAME = "ERP外部写入权限开关";
    private static final String ERP_EXTERNAL_WRITE_ENABLED_CONFIG_REMARK = "ERP external write permission switch";

    @Resource
    private ConfigService configService;

    @Override
    public boolean isErpExternalWriteEnabled() {
        ConfigDO config = configService.getConfigByKey(ERP_EXTERNAL_WRITE_ENABLED_CONFIG_KEY);
        return config != null && Boolean.parseBoolean(StrUtil.trim(config.getValue()));
    }

    @Override
    public void updateErpExternalWriteEnabled(Boolean enabled) {
        ConfigDO existingConfig = configService.getConfigByKey(ERP_EXTERNAL_WRITE_ENABLED_CONFIG_KEY);
        ConfigSaveReqVO configSaveReqVO = new ConfigSaveReqVO();
        configSaveReqVO.setId(existingConfig != null ? existingConfig.getId() : null);
        configSaveReqVO.setCategory(CONFIG_CATEGORY);
        configSaveReqVO.setName(ERP_EXTERNAL_WRITE_ENABLED_CONFIG_NAME);
        configSaveReqVO.setKey(ERP_EXTERNAL_WRITE_ENABLED_CONFIG_KEY);
        configSaveReqVO.setValue(String.valueOf(Boolean.TRUE.equals(enabled)));
        configSaveReqVO.setVisible(Boolean.FALSE);
        configSaveReqVO.setRemark(ERP_EXTERNAL_WRITE_ENABLED_CONFIG_REMARK);
        if (existingConfig == null) {
            configService.createConfig(configSaveReqVO);
        } else {
            configService.updateConfig(configSaveReqVO);
        }
    }

}
