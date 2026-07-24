package cn.iocoder.yudao.module.ai.service.tts;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.ai.framework.ai.config.YudaoAiProperties;
import cn.iocoder.yudao.module.infra.controller.admin.config.vo.ConfigSaveReqVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.config.ConfigDO;
import cn.iocoder.yudao.module.infra.service.config.ConfigService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.ai.enums.ErrorCodeConstants.TTS_TEST_VOICE_UNSUPPORTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiTtsAliyunNlsCredentialServiceTest extends BaseMockitoUnitTest {

    @Mock
    private ConfigService configService;

    @InjectMocks
    private AiTtsAliyunNlsCredentialService credentialService;

    @Test
    void saveAccessToken_whenConfigMissing_createsInvisibleConfig() {
        when(configService.getConfigByKey(AiTtsAliyunNlsCredentialService.ACCESS_TOKEN_CONFIG_KEY)).thenReturn(null);
        when(configService.createConfig(any(ConfigSaveReqVO.class))).thenReturn(100L);

        credentialService.saveAccessToken(" saved-token ");

        ArgumentCaptor<ConfigSaveReqVO> captor = ArgumentCaptor.forClass(ConfigSaveReqVO.class);
        verify(configService).createConfig(captor.capture());
        ConfigSaveReqVO reqVO = captor.getValue();
        assertEquals("ai", reqVO.getCategory());
        assertEquals("阿里云 NLS AccessToken", reqVO.getName());
        assertEquals(AiTtsAliyunNlsCredentialService.ACCESS_TOKEN_CONFIG_KEY, reqVO.getKey());
        assertEquals("saved-token", reqVO.getValue());
        assertFalse(reqVO.getVisible());
    }

    @Test
    void saveAccessToken_whenConfigExists_updatesExistingConfig() {
        ConfigDO config = new ConfigDO();
        config.setId(100L);
        config.setConfigKey(AiTtsAliyunNlsCredentialService.ACCESS_TOKEN_CONFIG_KEY);
        config.setValue("old-token");
        when(configService.getConfigByKey(AiTtsAliyunNlsCredentialService.ACCESS_TOKEN_CONFIG_KEY)).thenReturn(config);

        credentialService.saveAccessToken("new-token");

        ArgumentCaptor<ConfigSaveReqVO> captor = ArgumentCaptor.forClass(ConfigSaveReqVO.class);
        verify(configService).updateConfig(captor.capture());
        ConfigSaveReqVO reqVO = captor.getValue();
        assertEquals(100L, reqVO.getId());
        assertEquals("new-token", reqVO.getValue());
        assertFalse(reqVO.getVisible());
    }

    @Test
    void getAccessTokenStatus_whenSaved_masksToken() {
        ConfigDO config = new ConfigDO();
        config.setValue("testtoken1234567890abcd");
        when(configService.getConfigByKey(AiTtsAliyunNlsCredentialService.ACCESS_TOKEN_CONFIG_KEY)).thenReturn(config);

        AiTtsAliyunNlsCredentialService.AccessTokenStatus status =
                credentialService.getAccessTokenStatus(new YudaoAiProperties.Tts());

        assertTrue(status.saved());
        assertTrue(status.configured());
        assertEquals("test****abcd", status.maskedAccessToken());
    }

    @Test
    void resolveAccessToken_whenSaved_usesSavedTokenBeforeRuntimeToken() {
        ConfigDO config = new ConfigDO();
        config.setValue("saved-token");
        when(configService.getConfigByKey(AiTtsAliyunNlsCredentialService.ACCESS_TOKEN_CONFIG_KEY)).thenReturn(config);
        YudaoAiProperties.Tts tts = new YudaoAiProperties.Tts();
        tts.getAliyunNls().setAccessToken("runtime-token");

        String token = credentialService.resolveAccessToken(tts);

        assertEquals("saved-token", token);
    }

    @Test
    void saveAppKey_whenConfigMissing_createsInvisibleConfig() {
        when(configService.getConfigByKey(AiTtsAliyunNlsCredentialService.APPKEY_CONFIG_KEY)).thenReturn(null);
        when(configService.createConfig(any(ConfigSaveReqVO.class))).thenReturn(102L);

        credentialService.saveAppKey(" test-appkey ");

        ArgumentCaptor<ConfigSaveReqVO> captor = ArgumentCaptor.forClass(ConfigSaveReqVO.class);
        verify(configService).createConfig(captor.capture());
        ConfigSaveReqVO reqVO = captor.getValue();
        assertEquals("ai", reqVO.getCategory());
        assertEquals("阿里云 NLS AppKey", reqVO.getName());
        assertEquals(AiTtsAliyunNlsCredentialService.APPKEY_CONFIG_KEY, reqVO.getKey());
        assertEquals("test-appkey", reqVO.getValue());
        assertFalse(reqVO.getVisible());
    }

    @Test
    void getAppKeyStatus_whenSaved_masksValue() {
        ConfigDO config = new ConfigDO();
        config.setValue("i0nmL1mF7xPNUXM9");
        when(configService.getConfigByKey(AiTtsAliyunNlsCredentialService.APPKEY_CONFIG_KEY)).thenReturn(config);

        AiTtsAliyunNlsCredentialService.AppKeyStatus status =
                credentialService.getAppKeyStatus(new YudaoAiProperties.Tts());

        assertTrue(status.saved());
        assertTrue(status.configured());
        assertEquals("i0nm****UXM9", status.maskedAppKey());
    }

    @Test
    void resolveAppKey_whenSaved_usesSavedValueBeforeRuntimeValue() {
        ConfigDO config = new ConfigDO();
        config.setValue("saved-appkey");
        when(configService.getConfigByKey(AiTtsAliyunNlsCredentialService.APPKEY_CONFIG_KEY)).thenReturn(config);
        YudaoAiProperties.Tts tts = new YudaoAiProperties.Tts();
        tts.getAliyunNls().setAppkey("runtime-appkey");

        String appKey = credentialService.resolveAppKey(tts);

        assertEquals("saved-appkey", appKey);
    }

    @Test
    void saveVoice_whenConfigMissing_createsConfigWithValidatedVoice() {
        when(configService.getConfigByKey(AiTtsAliyunNlsCredentialService.VOICE_CONFIG_KEY)).thenReturn(null);
        when(configService.createConfig(any(ConfigSaveReqVO.class))).thenReturn(101L);

        credentialService.saveVoice(" ruoxi ");

        ArgumentCaptor<ConfigSaveReqVO> captor = ArgumentCaptor.forClass(ConfigSaveReqVO.class);
        verify(configService).createConfig(captor.capture());
        ConfigSaveReqVO reqVO = captor.getValue();
        assertEquals("ai", reqVO.getCategory());
        assertEquals("阿里云 NLS 默认音色", reqVO.getName());
        assertEquals(AiTtsAliyunNlsCredentialService.VOICE_CONFIG_KEY, reqVO.getKey());
        assertEquals("ruoxi", reqVO.getValue());
        assertTrue(reqVO.getVisible());
    }

    @Test
    void saveVoice_whenUnsupported_throwsServiceException() {
        assertServiceException(() -> credentialService.saveVoice("badvoice"),
                TTS_TEST_VOICE_UNSUPPORTED, "badvoice");
    }

    @Test
    void getVoiceStatus_whenSaved_returnsSavedVoice() {
        ConfigDO config = new ConfigDO();
        config.setValue("ruoxi");
        when(configService.getConfigByKey(AiTtsAliyunNlsCredentialService.VOICE_CONFIG_KEY)).thenReturn(config);

        AiTtsAliyunNlsCredentialService.VoiceStatus status =
                credentialService.getVoiceStatus(new YudaoAiProperties.Tts());

        assertTrue(status.saved());
        assertTrue(status.configured());
        assertEquals("saved", status.source());
        assertEquals("ruoxi", status.voice());
    }

    @Test
    void resolveVoice_whenSaved_usesSavedVoiceBeforeRuntimeVoice() {
        ConfigDO config = new ConfigDO();
        config.setValue("ruoxi");
        when(configService.getConfigByKey(AiTtsAliyunNlsCredentialService.VOICE_CONFIG_KEY)).thenReturn(config);
        YudaoAiProperties.Tts tts = new YudaoAiProperties.Tts();
        tts.getAliyunNls().setVoice("xiaoyun");

        String voice = credentialService.resolveVoice(tts, "");

        assertEquals("ruoxi", voice);
    }

}
