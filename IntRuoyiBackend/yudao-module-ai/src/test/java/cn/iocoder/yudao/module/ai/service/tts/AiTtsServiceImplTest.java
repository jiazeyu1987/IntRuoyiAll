package cn.iocoder.yudao.module.ai.service.tts;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.ai.framework.ai.config.YudaoAiProperties;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.ai.enums.ErrorCodeConstants.TTS_TEST_AUDIO_EMPTY;
import static cn.iocoder.yudao.module.ai.enums.ErrorCodeConstants.TTS_TEST_TARGET_NOT_CONFIGURED;
import static cn.iocoder.yudao.module.ai.enums.ErrorCodeConstants.TTS_TEST_VOICE_UNSUPPORTED;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiTtsServiceImplTest extends BaseMockitoUnitTest {

    @Mock
    private YudaoAiProperties yudaoAiProperties;
    @Mock
    private WindowsTtsSynthesizer windowsTtsSynthesizer;
    @Mock
    private DashscopeTtsSynthesizer dashscopeTtsSynthesizer;
    @Mock
    private AliyunNlsTtsSynthesizer aliyunNlsTtsSynthesizer;
    @Mock
    private AiTtsAliyunNlsCredentialService aliyunNlsCredentialService;

    @InjectMocks
    private AiTtsServiceImpl aiTtsService;

    @Test
    void generateSpeech_whenWindowsProviderReturnsAudio_returnsBytesAndMimeType() {
        String text = "测试一句内部 TTS";
        byte[] expectedAudio = new byte[] {1, 2, 3, 4};
        YudaoAiProperties.Tts tts = new YudaoAiProperties.Tts();
        tts.setEnable(true);
        tts.setProvider("windows");
        when(yudaoAiProperties.getTts()).thenReturn(tts);
        when(windowsTtsSynthesizer.synthesize(text, tts)).thenReturn(expectedAudio);

        AiTtsService.AudioPayload payload = aiTtsService.generateSpeech(text, "windows", "");

        assertArrayEquals(expectedAudio, payload.audioBytes());
        assertEquals("audio/wav", payload.contentType().toString());
    }

    @Test
    void generateSpeech_whenDashScopeProviderUsesSelectedVoice_returnsBytesAndMimeType() {
        String text = "测试一句 DashScope TTS";
        byte[] expectedAudio = new byte[] {5, 6, 7, 8};
        YudaoAiProperties.Tts tts = new YudaoAiProperties.Tts();
        tts.setEnable(true);
        tts.setProvider("windows");
        when(yudaoAiProperties.getTts()).thenReturn(tts);
        when(dashscopeTtsSynthesizer.synthesize(text, tts, "longxiaochun")).thenReturn(expectedAudio);

        AiTtsService.AudioPayload payload = aiTtsService.generateSpeech(text, "dashscope", "longxiaochun");

        assertArrayEquals(expectedAudio, payload.audioBytes());
        assertEquals("audio/wav", payload.contentType().toString());
    }

    @Test
    void generateSpeech_whenRagIntFlashProviderUsesSelectedVoice_returnsBytesAndMimeType() {
        String text = "测试一句 RagInt Flash TTS";
        byte[] expectedAudio = new byte[] {9, 10, 11, 12};
        YudaoAiProperties.Tts tts = new YudaoAiProperties.Tts();
        tts.setEnable(true);
        tts.setProvider("windows");
        when(yudaoAiProperties.getTts()).thenReturn(tts);
        when(dashscopeTtsSynthesizer.synthesize(text, tts, "longxiaoxia")).thenReturn(expectedAudio);

        AiTtsService.AudioPayload payload = aiTtsService.generateSpeech(text, "flash", "longxiaoxia");

        assertArrayEquals(expectedAudio, payload.audioBytes());
        assertEquals("audio/wav", payload.contentType().toString());
    }

    @Test
    void generateSpeech_whenDashScopeVoiceUnsupported_throwsServiceException() {
        YudaoAiProperties.Tts tts = new YudaoAiProperties.Tts();
        tts.setEnable(true);
        tts.setProvider("windows");
        when(yudaoAiProperties.getTts()).thenReturn(tts);

        assertServiceException(
                () -> aiTtsService.generateSpeech("测试一句 DashScope TTS", "dashscope", "badvoice"),
                TTS_TEST_VOICE_UNSUPPORTED, "badvoice");
    }

    @Test
    void generateSpeech_whenAliyunNlsProviderUsesSelectedVoice_returnsBytesAndMimeType() {
        String text = "测试一句阿里云 NLS TTS";
        byte[] expectedAudio = new byte[] {13, 14, 15, 16};
        YudaoAiProperties.Tts tts = new YudaoAiProperties.Tts();
        tts.setEnable(true);
        tts.setProvider("windows");
        when(yudaoAiProperties.getTts()).thenReturn(tts);
        when(aliyunNlsCredentialService.resolveVoice(tts, "xiaoyun")).thenReturn("xiaoyun");
        when(aliyunNlsCredentialService.resolveAccessToken(tts)).thenReturn("saved-token");
        when(aliyunNlsCredentialService.resolveAppKey(tts)).thenReturn("saved-appkey");
        when(aliyunNlsTtsSynthesizer.synthesize(text, tts, "xiaoyun", "saved-token", "saved-appkey"))
                .thenReturn(expectedAudio);

        AiTtsService.AudioPayload payload = aiTtsService.generateSpeech(text, "aliyun_nls", "xiaoyun");

        assertArrayEquals(expectedAudio, payload.audioBytes());
        assertEquals("audio/wav", payload.contentType().toString());
        verify(aliyunNlsTtsSynthesizer).synthesize(text, tts, "xiaoyun", "saved-token", "saved-appkey");
    }

    @Test
    void generateSpeech_whenAliyunNlsVoiceBlank_usesSharedResolvedVoice() {
        String text = "测试一句阿里云 NLS 默认音色";
        byte[] expectedAudio = new byte[] {21, 22, 23, 24};
        YudaoAiProperties.Tts tts = new YudaoAiProperties.Tts();
        tts.setEnable(true);
        tts.setProvider("windows");
        when(yudaoAiProperties.getTts()).thenReturn(tts);
        when(aliyunNlsCredentialService.resolveVoice(tts, "")).thenReturn("ruoxi");
        when(aliyunNlsCredentialService.resolveAccessToken(tts)).thenReturn("saved-token");
        when(aliyunNlsCredentialService.resolveAppKey(tts)).thenReturn("saved-appkey");
        when(aliyunNlsTtsSynthesizer.synthesize(text, tts, "ruoxi", "saved-token", "saved-appkey"))
                .thenReturn(expectedAudio);

        AiTtsService.AudioPayload payload = aiTtsService.generateSpeech(text, "aliyun_nls", "");

        assertArrayEquals(expectedAudio, payload.audioBytes());
        assertEquals("audio/wav", payload.contentType().toString());
        verify(aliyunNlsTtsSynthesizer).synthesize(text, tts, "ruoxi", "saved-token", "saved-appkey");
    }

    @Test
    void generateSpeech_whenRagIntNlsAliasUsesSelectedVoice_returnsBytesAndMimeType() {
        String text = "测试一句 RagInt NLS TTS";
        byte[] expectedAudio = new byte[] {17, 18, 19, 20};
        YudaoAiProperties.Tts tts = new YudaoAiProperties.Tts();
        tts.setEnable(true);
        tts.setProvider("windows");
        when(yudaoAiProperties.getTts()).thenReturn(tts);
        when(aliyunNlsCredentialService.resolveVoice(tts, "ruoxi")).thenReturn("ruoxi");
        when(aliyunNlsCredentialService.resolveAccessToken(tts)).thenReturn("saved-token");
        when(aliyunNlsCredentialService.resolveAppKey(tts)).thenReturn("saved-appkey");
        when(aliyunNlsTtsSynthesizer.synthesize(text, tts, "ruoxi", "saved-token", "saved-appkey"))
                .thenReturn(expectedAudio);

        AiTtsService.AudioPayload payload = aiTtsService.generateSpeech(text, "nls", "ruoxi");

        assertArrayEquals(expectedAudio, payload.audioBytes());
        assertEquals("audio/wav", payload.contentType().toString());
        verify(aliyunNlsTtsSynthesizer).synthesize(text, tts, "ruoxi", "saved-token", "saved-appkey");
    }

    @Test
    void generateSpeech_whenAliyunNlsVoiceUnsupported_throwsServiceException() {
        YudaoAiProperties.Tts tts = new YudaoAiProperties.Tts();
        tts.setEnable(true);
        tts.setProvider("windows");
        when(yudaoAiProperties.getTts()).thenReturn(tts);
        when(aliyunNlsCredentialService.resolveVoice(tts, "badvoice"))
                .thenThrow(exception(TTS_TEST_VOICE_UNSUPPORTED, "badvoice"));

        assertServiceException(
                () -> aiTtsService.generateSpeech("测试一句阿里云 NLS TTS", "aliyun_nls", "badvoice"),
                TTS_TEST_VOICE_UNSUPPORTED, "badvoice");
    }

    @Test
    void generateSpeech_whenDashScopeProviderReturnsEmptyAudio_throwsServiceException() {
        YudaoAiProperties.Tts tts = new YudaoAiProperties.Tts();
        tts.setEnable(true);
        tts.setProvider("windows");
        when(yudaoAiProperties.getTts()).thenReturn(tts);
        when(dashscopeTtsSynthesizer.synthesize("测试一句 DashScope TTS", tts, "longyang")).thenReturn(new byte[0]);

        assertServiceException(
                () -> aiTtsService.generateSpeech("测试一句 DashScope TTS", "dashscope", "longyang"),
                TTS_TEST_AUDIO_EMPTY);
    }

    @Test
    void generateSpeech_whenProviderUnsupported_throwsServiceException() {
        YudaoAiProperties.Tts tts = new YudaoAiProperties.Tts();
        tts.setEnable(true);
        tts.setProvider("windows");
        when(yudaoAiProperties.getTts()).thenReturn(tts);

        assertServiceException(
                () -> aiTtsService.generateSpeech("测试一句内部 TTS", "mystery", ""),
                TTS_TEST_TARGET_NOT_CONFIGURED);
    }

}
