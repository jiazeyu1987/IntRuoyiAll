package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccBrowserExtensionBlacklistRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccBrowserExtensionBlacklistSaveReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.config.vo.ConfigSaveReqVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.config.ConfigDO;
import cn.iocoder.yudao.module.infra.service.config.ConfigService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_BROWSER_EXTENSION_BLACKLIST_INVALID;

@Service
@Validated
public class DccControlledFileBrowserSettingsServiceImpl implements DccControlledFileBrowserSettingsService {

    public static final String EXTENSION_BLACKLIST_CONFIG_KEY = "dcc.controlled-file.browser.extension-blacklist";

    private static final String CONFIG_CATEGORY = "dcc";
    private static final String CONFIG_NAME = "DCC 文件查阅后缀黑名单";
    private static final String CONFIG_REMARK = "DCC 文件查阅高级设置：命中后缀的文件对所有用户不可见";
    private static final int MAX_EXTENSION_LENGTH = 32;

    @Resource
    private ConfigService configService;

    @Override
    public DccBrowserExtensionBlacklistRespVO getExtensionBlacklist() {
        DccBrowserExtensionBlacklistRespVO respVO = new DccBrowserExtensionBlacklistRespVO();
        respVO.setExtensionPatterns(getBlacklistedExtensionPatterns());
        return respVO;
    }

    @Override
    public List<String> getBlacklistedExtensionPatterns() {
        ConfigDO config = configService.getConfigByKey(EXTENSION_BLACKLIST_CONFIG_KEY);
        if (config == null || config.getValue() == null || config.getValue().isBlank()) {
            return List.of();
        }
        return normalizePatterns(JsonUtils.parseArray(config.getValue(), String.class));
    }

    @Override
    public void saveExtensionBlacklist(DccBrowserExtensionBlacklistSaveReqVO reqVO) {
        List<String> patterns = normalizePatterns(reqVO.getExtensionPatterns());
        String value = JsonUtils.toJsonString(patterns);
        ConfigDO existing = configService.getConfigByKey(EXTENSION_BLACKLIST_CONFIG_KEY);
        ConfigSaveReqVO saveReqVO = new ConfigSaveReqVO();
        if (existing != null) {
            saveReqVO.setId(existing.getId());
        }
        saveReqVO.setCategory(CONFIG_CATEGORY);
        saveReqVO.setName(CONFIG_NAME);
        saveReqVO.setKey(EXTENSION_BLACKLIST_CONFIG_KEY);
        saveReqVO.setValue(value);
        saveReqVO.setVisible(Boolean.TRUE);
        saveReqVO.setRemark(CONFIG_REMARK);
        if (existing == null) {
            configService.createConfig(saveReqVO);
        } else {
            configService.updateConfig(saveReqVO);
        }
    }

    private List<String> normalizePatterns(List<String> rawPatterns) {
        if (rawPatterns == null || rawPatterns.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> normalizedPatterns = new LinkedHashSet<>();
        for (String rawPattern : rawPatterns) {
            String normalizedPattern = normalizePattern(rawPattern);
            if (normalizedPattern != null) {
                normalizedPatterns.add(normalizedPattern);
            }
        }
        return List.copyOf(normalizedPatterns);
    }

    private String normalizePattern(String rawPattern) {
        if (rawPattern == null) {
            return null;
        }
        String normalized = rawPattern.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.startsWith("*")) {
            normalized = normalized.substring(1);
        }
        if (!normalized.startsWith(".")) {
            normalized = "." + normalized;
        }
        if (normalized.length() <= 1 || normalized.length() > MAX_EXTENSION_LENGTH + 1
                || !normalized.substring(1).matches("[a-z0-9][a-z0-9_-]*")) {
            throw exception(DCC_BROWSER_EXTENSION_BLACKLIST_INVALID, rawPattern);
        }
        return "*" + normalized;
    }

}
