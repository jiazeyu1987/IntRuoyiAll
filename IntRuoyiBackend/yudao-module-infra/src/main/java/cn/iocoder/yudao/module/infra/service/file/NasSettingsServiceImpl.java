package cn.iocoder.yudao.module.infra.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.infra.controller.admin.config.vo.ConfigSaveReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileNasConfigRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileNasConfigSaveReqVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.config.ConfigDO;
import cn.iocoder.yudao.module.infra.service.config.ConfigService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.FILE_NAS_CONFIG_MISSING;

@Service
public class NasSettingsServiceImpl implements NasSettingsService {

    static final String CATEGORY = "infra.nas";
    static final String KEY_SERVER = "infra.nas.server";
    static final String KEY_PORT = "infra.nas.port";
    static final String KEY_SHARE = "infra.nas.share";
    static final String KEY_DOMAIN = "infra.nas.domain";
    static final String KEY_USERNAME = "infra.nas.username";
    static final String KEY_PASSWORD = "infra.nas.password";

    @Resource
    private ConfigService configService;

    @Override
    public FileNasConfigRespVO getNasConfig() {
        return new FileNasConfigRespVO()
                .setServer(getValue(KEY_SERVER))
                .setPort(getOptionalIntValue(KEY_PORT))
                .setShare(getValue(KEY_SHARE))
                .setDomain(getValue(KEY_DOMAIN))
                .setUsername(getValue(KEY_USERNAME))
                .setPassword(getValue(KEY_PASSWORD));
    }

    @Override
    public void saveNasConfig(FileNasConfigSaveReqVO reqVO) {
        upsert(KEY_SERVER, "NAS 服务器", reqVO.getServer(), true);
        upsertOptional(KEY_PORT, "NAS 服务器端口", reqVO.getPort() == null ? null : String.valueOf(reqVO.getPort()), true);
        upsert(KEY_SHARE, "NAS 共享名", reqVO.getShare(), true);
        upsertOptional(KEY_DOMAIN, "NAS 域", reqVO.getDomain(), true);
        upsert(KEY_USERNAME, "NAS 用户名", reqVO.getUsername(), true);
        upsert(KEY_PASSWORD, "NAS 密码", reqVO.getPassword(), false);
    }

    @Override
    public NasConnectionConfig toConnectionConfig(FileNasConfigSaveReqVO reqVO) {
        return new NasConnectionConfig(
                reqVO.getServer(),
                reqVO.getPort(),
                reqVO.getShare(),
                reqVO.getDomain(),
                reqVO.getUsername(),
                reqVO.getPassword()
        );
    }

    @Override
    public NasConnectionConfig getRequiredNasConfig() {
        FileNasConfigRespVO respVO = getNasConfig();
        if (StrUtil.hasBlank(respVO.getServer(), respVO.getShare(), respVO.getUsername(), respVO.getPassword())) {
            throw exception(FILE_NAS_CONFIG_MISSING);
        }
        return new NasConnectionConfig(
                respVO.getServer(),
                respVO.getPort(),
                respVO.getShare(),
                respVO.getDomain(),
                respVO.getUsername(),
                respVO.getPassword()
        );
    }

    private void upsert(String key, String name, String value, boolean visible) {
        ConfigDO existing = configService.getConfigByKey(key);
        ConfigSaveReqVO reqVO = new ConfigSaveReqVO();
        reqVO.setCategory(CATEGORY);
        reqVO.setName(name);
        reqVO.setKey(key);
        reqVO.setValue(StrUtil.trim(value));
        reqVO.setVisible(visible);
        reqVO.setRemark("NAS 管理页参数");
        if (existing == null) {
            configService.createConfig(reqVO);
            return;
        }
        reqVO.setId(existing.getId());
        configService.updateConfig(reqVO);
    }

    private void upsertOptional(String key, String name, String value, boolean visible) {
        ConfigDO existing = configService.getConfigByKey(key);
        String trimmedValue = StrUtil.trim(value);
        if (StrUtil.isBlank(trimmedValue)) {
            if (existing != null) {
                configService.deleteConfig(existing.getId());
            }
            return;
        }
        ConfigSaveReqVO reqVO = new ConfigSaveReqVO();
        reqVO.setCategory(CATEGORY);
        reqVO.setName(name);
        reqVO.setKey(key);
        reqVO.setValue(trimmedValue);
        reqVO.setVisible(visible);
        reqVO.setRemark("NAS 管理页参数");
        if (existing == null) {
            configService.createConfig(reqVO);
            return;
        }
        reqVO.setId(existing.getId());
        configService.updateConfig(reqVO);
    }

    private String getValue(String key) {
        ConfigDO config = configService.getConfigByKey(key);
        return config == null ? "" : StrUtil.trimToEmpty(config.getValue());
    }

    private Integer getOptionalIntValue(String key) {
        String value = getValue(key);
        if (StrUtil.isBlank(value)) {
            return null;
        }
        return Integer.parseInt(value);
    }
}
