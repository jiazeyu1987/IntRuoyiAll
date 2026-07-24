package cn.iocoder.yudao.module.ai.service.tts;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.ai.framework.ai.config.YudaoAiProperties;
import cn.iocoder.yudao.module.infra.controller.admin.config.vo.ConfigSaveReqVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.config.ConfigDO;
import cn.iocoder.yudao.module.infra.service.config.ConfigService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.ai.enums.ErrorCodeConstants.TTS_TEST_VOICE_UNSUPPORTED;

@Service
public class AiTtsAliyunNlsCredentialService {

    public static final String ACCESS_TOKEN_CONFIG_KEY = "yudao.ai.tts.aliyun-nls.access-token";
    public static final String VOICE_CONFIG_KEY = "yudao.ai.tts.aliyun-nls.voice";
    public static final String APPKEY_CONFIG_KEY = "yudao.ai.tts.aliyun-nls.appkey";

    private static final String CATEGORY = "ai";
    private static final String ACCESS_TOKEN_NAME = "阿里云 NLS AccessToken";
    private static final String VOICE_NAME = "阿里云 NLS 默认音色";
    private static final String APPKEY_NAME = "阿里云 NLS AppKey";
    private static final String SOURCE_SAVED = "saved";
    private static final String SOURCE_RUNTIME = "runtime";
    private static final String SOURCE_MISSING = "missing";

    @Resource
    private ConfigService configService;

    public AccessTokenStatus getAccessTokenStatus(YudaoAiProperties.Tts tts) {
        ConfigDO savedConfig = getSavedConfig();
        String savedToken = savedConfig != null ? StrUtil.trim(savedConfig.getValue()) : "";
        if (StrUtil.isNotBlank(savedToken)) {
            return new AccessTokenStatus(true, true, SOURCE_SAVED, maskToken(savedToken));
        }
        String runtimeToken = resolveRuntimeAccessToken(tts);
        if (StrUtil.isNotBlank(runtimeToken)) {
            return new AccessTokenStatus(false, true, SOURCE_RUNTIME, maskToken(runtimeToken));
        }
        return new AccessTokenStatus(false, false, SOURCE_MISSING, "");
    }

    public String resolveAccessToken(YudaoAiProperties.Tts tts) {
        ConfigDO savedConfig = getSavedConfig();
        String savedToken = savedConfig != null ? StrUtil.trim(savedConfig.getValue()) : "";
        if (StrUtil.isNotBlank(savedToken)) {
            return savedToken;
        }
        return resolveRuntimeAccessToken(tts);
    }

    public AppKeyStatus getAppKeyStatus(YudaoAiProperties.Tts tts) {
        ConfigDO savedConfig = getSavedAppKeyConfig();
        String savedAppKey = savedConfig != null ? StrUtil.trim(savedConfig.getValue()) : "";
        if (StrUtil.isNotBlank(savedAppKey)) {
            return new AppKeyStatus(true, true, SOURCE_SAVED, maskSecret(savedAppKey));
        }
        String runtimeAppKey = resolveRuntimeAppKey(tts);
        if (StrUtil.isNotBlank(runtimeAppKey)) {
            return new AppKeyStatus(false, true, SOURCE_RUNTIME, maskSecret(runtimeAppKey));
        }
        return new AppKeyStatus(false, false, SOURCE_MISSING, "");
    }

    public String resolveAppKey(YudaoAiProperties.Tts tts) {
        ConfigDO savedConfig = getSavedAppKeyConfig();
        String savedAppKey = savedConfig != null ? StrUtil.trim(savedConfig.getValue()) : "";
        if (StrUtil.isNotBlank(savedAppKey)) {
            return savedAppKey;
        }
        return resolveRuntimeAppKey(tts);
    }

    public VoiceStatus getVoiceStatus(YudaoAiProperties.Tts tts) {
        ConfigDO savedConfig = getSavedVoiceConfig();
        String savedVoice = savedConfig != null ? StrUtil.trim(savedConfig.getValue()) : "";
        if (StrUtil.isNotBlank(savedVoice)) {
            return new VoiceStatus(true, true, SOURCE_SAVED, savedVoice);
        }
        String runtimeVoice = resolveRuntimeVoice(tts);
        if (StrUtil.isNotBlank(runtimeVoice)) {
            return new VoiceStatus(false, true, SOURCE_RUNTIME, runtimeVoice);
        }
        return new VoiceStatus(false, false, SOURCE_MISSING, "");
    }

    public String resolveVoice(YudaoAiProperties.Tts tts, String selectedVoice) {
        String voice = StrUtil.trim(selectedVoice);
        if (StrUtil.isBlank(voice)) {
            ConfigDO savedConfig = getSavedVoiceConfig();
            String savedVoice = savedConfig != null ? StrUtil.trim(savedConfig.getValue()) : "";
            voice = StrUtil.isNotBlank(savedVoice) ? savedVoice : resolveRuntimeVoice(tts);
        }
        if (StrUtil.isBlank(voice) || !AliyunNlsTtsSynthesizer.SUPPORTED_VOICES.contains(voice)) {
            throw exception(TTS_TEST_VOICE_UNSUPPORTED, StrUtil.blankToDefault(voice, ""));
        }
        return voice;
    }

    public void saveAccessToken(String accessToken) {
        String trimmedToken = StrUtil.trim(accessToken);
        ConfigDO savedConfig = getSavedConfig();
        ConfigSaveReqVO reqVO = new ConfigSaveReqVO();
        if (savedConfig != null) {
            reqVO.setId(savedConfig.getId());
        }
        reqVO.setCategory(CATEGORY);
        reqVO.setName(ACCESS_TOKEN_NAME);
        reqVO.setKey(ACCESS_TOKEN_CONFIG_KEY);
        reqVO.setValue(trimmedToken);
        reqVO.setVisible(false);
        reqVO.setRemark("TTS 测试页保存的阿里云 NLS AccessToken");
        if (savedConfig == null) {
            configService.createConfig(reqVO);
            return;
        }
        configService.updateConfig(reqVO);
    }

    public void saveVoice(String voice) {
        String resolvedVoice = resolveVoice(new YudaoAiProperties.Tts(), voice);
        ConfigDO savedConfig = getSavedVoiceConfig();
        ConfigSaveReqVO reqVO = new ConfigSaveReqVO();
        if (savedConfig != null) {
            reqVO.setId(savedConfig.getId());
        }
        reqVO.setCategory(CATEGORY);
        reqVO.setName(VOICE_NAME);
        reqVO.setKey(VOICE_CONFIG_KEY);
        reqVO.setValue(resolvedVoice);
        reqVO.setVisible(true);
        reqVO.setRemark("TTS 页面保存的阿里云 NLS 默认音色");
        if (savedConfig == null) {
            configService.createConfig(reqVO);
            return;
        }
        configService.updateConfig(reqVO);
    }

    public void saveAppKey(String appKey) {
        String trimmedAppKey = StrUtil.trim(appKey);
        ConfigDO savedConfig = getSavedAppKeyConfig();
        ConfigSaveReqVO reqVO = new ConfigSaveReqVO();
        if (savedConfig != null) {
            reqVO.setId(savedConfig.getId());
        }
        reqVO.setCategory(CATEGORY);
        reqVO.setName(APPKEY_NAME);
        reqVO.setKey(APPKEY_CONFIG_KEY);
        reqVO.setValue(trimmedAppKey);
        reqVO.setVisible(false);
        reqVO.setRemark("TTS 页面保存的阿里云 NLS AppKey");
        if (savedConfig == null) {
            configService.createConfig(reqVO);
            return;
        }
        configService.updateConfig(reqVO);
    }

    private ConfigDO getSavedConfig() {
        return configService.getConfigByKey(ACCESS_TOKEN_CONFIG_KEY);
    }

    private ConfigDO getSavedVoiceConfig() {
        return configService.getConfigByKey(VOICE_CONFIG_KEY);
    }

    private ConfigDO getSavedAppKeyConfig() {
        return configService.getConfigByKey(APPKEY_CONFIG_KEY);
    }

    private static String resolveRuntimeAccessToken(YudaoAiProperties.Tts tts) {
        if (tts == null || tts.getAliyunNls() == null) {
            return "";
        }
        return StrUtil.trim(tts.getAliyunNls().getAccessToken());
    }

    private static String resolveRuntimeVoice(YudaoAiProperties.Tts tts) {
        if (tts == null || tts.getAliyunNls() == null) {
            return "";
        }
        return StrUtil.trim(tts.getAliyunNls().getVoice());
    }

    private static String resolveRuntimeAppKey(YudaoAiProperties.Tts tts) {
        if (tts == null || tts.getAliyunNls() == null) {
            return "";
        }
        return StrUtil.trim(tts.getAliyunNls().getAppkey());
    }

    private static String maskToken(String token) {
        String trimmed = StrUtil.trim(token);
        if (StrUtil.isBlank(trimmed)) {
            return "";
        }
        if (trimmed.length() <= 8) {
            return "****";
        }
        return trimmed.substring(0, 4) + "****" + trimmed.substring(trimmed.length() - 4);
    }

    public record AccessTokenStatus(boolean saved, boolean configured, String source, String maskedAccessToken) {
    }

    public record VoiceStatus(boolean saved, boolean configured, String source, String voice) {
    }

    public record AppKeyStatus(boolean saved, boolean configured, String source, String maskedAppKey) {
    }

    private static String maskSecret(String value) {
        return maskToken(value);
    }

}
