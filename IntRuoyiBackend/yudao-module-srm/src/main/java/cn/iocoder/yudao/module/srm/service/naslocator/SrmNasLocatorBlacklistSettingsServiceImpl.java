package cn.iocoder.yudao.module.srm.service.naslocator;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.infra.controller.admin.config.vo.ConfigSaveReqVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.config.ConfigDO;
import cn.iocoder.yudao.module.infra.service.config.ConfigService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.srm.enums.ErrorCodeConstants.NAS_LOCATOR_BLACKLIST_CONFIG_INVALID;

@Service
@Validated
public class SrmNasLocatorBlacklistSettingsServiceImpl implements SrmNasLocatorBlacklistSettingsService {

    static final String CATEGORY = "srm.nas-locator";
    static final String KEY_BLACKLIST_PATTERNS = "srm.nas-locator.blacklist-patterns";
    private static final String CONFIG_NAME = "NAS定位黑名单规则";
    private static final String CONFIG_REMARK = "SRM NAS定位 黑名单规则";

    @Resource
    private ConfigService configService;

    @Override
    public List<String> getPatterns() {
        ConfigDO config = configService.getConfigByKey(KEY_BLACKLIST_PATTERNS);
        return parseAndNormalizePatterns(config);
    }

    @Override
    public void savePatterns(List<String> patterns) {
        List<String> normalizedPatterns = normalizePatterns(patterns);
        ConfigDO existing = configService.getConfigByKey(KEY_BLACKLIST_PATTERNS);
        if (normalizedPatterns.isEmpty()) {
            if (existing != null) {
                configService.deleteConfig(existing.getId());
            }
            return;
        }
        ConfigSaveReqVO reqVO = new ConfigSaveReqVO();
        reqVO.setCategory(CATEGORY);
        reqVO.setName(CONFIG_NAME);
        reqVO.setKey(KEY_BLACKLIST_PATTERNS);
        reqVO.setValue(JsonUtils.toJsonString(normalizedPatterns));
        reqVO.setVisible(false);
        reqVO.setRemark(CONFIG_REMARK);
        if (existing == null) {
            configService.createConfig(reqVO);
            return;
        }
        reqVO.setId(existing.getId());
        configService.updateConfig(reqVO);
    }

    private List<String> parseAndNormalizePatterns(ConfigDO config) {
        if (config == null) {
            return List.of();
        }
        String rawValue = StrUtil.trim(config.getValue());
        if (StrUtil.isBlank(rawValue)) {
            throw exception(NAS_LOCATOR_BLACKLIST_CONFIG_INVALID);
        }
        try {
            return normalizePatterns(JsonUtils.parseArray(rawValue, String.class));
        } catch (RuntimeException ex) {
            throw exception(NAS_LOCATOR_BLACKLIST_CONFIG_INVALID);
        }
    }

    private List<String> normalizePatterns(List<String> patterns) {
        if (patterns == null || patterns.isEmpty()) {
            return List.of();
        }
        Set<String> normalizedKeys = new LinkedHashSet<>();
        List<String> normalizedPatterns = new ArrayList<>();
        for (String pattern : patterns) {
            String trimmedPattern = StrUtil.trim(pattern);
            if (StrUtil.isBlank(trimmedPattern)) {
                continue;
            }
            String dedupKey = trimmedPattern.toLowerCase(Locale.ROOT);
            if (normalizedKeys.add(dedupKey)) {
                normalizedPatterns.add(trimmedPattern);
            }
        }
        return normalizedPatterns;
    }
}
