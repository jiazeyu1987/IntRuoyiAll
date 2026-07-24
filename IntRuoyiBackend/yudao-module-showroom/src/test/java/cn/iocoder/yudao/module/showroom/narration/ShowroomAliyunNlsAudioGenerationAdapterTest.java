package cn.iocoder.yudao.module.showroom.narration;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.ai.framework.ai.config.YudaoAiProperties;
import cn.iocoder.yudao.module.ai.service.tts.AiTtsAliyunNlsCredentialService;
import cn.iocoder.yudao.module.ai.service.tts.AliyunNlsTtsSynthesizer;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ShowroomAliyunNlsAudioGenerationAdapterTest extends BaseMockitoUnitTest {

    @Mock
    private YudaoAiProperties yudaoAiProperties;
    @Mock
    private AiTtsAliyunNlsCredentialService aliyunNlsCredentialService;
    @Mock
    private AliyunNlsTtsSynthesizer aliyunNlsTtsSynthesizer;
    @Mock
    private FileService fileService;

    @InjectMocks
    private ShowroomAliyunNlsAudioGenerationAdapter adapter;

    @Test
    void generate_whenSharedDefaultsConfigured_uploadsWavAndReturnsVoiceAndDuration() throws Exception {
        YudaoAiProperties.Tts tts = new YudaoAiProperties.Tts();
        tts.getAliyunNls().setFormat("wav");
        when(yudaoAiProperties.getTts()).thenReturn(tts);
        when(aliyunNlsCredentialService.resolveVoice(tts, null)).thenReturn("ruoxi");
        when(aliyunNlsCredentialService.resolveAccessToken(tts)).thenReturn("saved-token");
        when(aliyunNlsCredentialService.resolveAppKey(tts)).thenReturn("saved-appkey");
        byte[] wavBytes = buildSilentWavBytes(2);
        when(aliyunNlsTtsSynthesizer.synthesize("产品讲解脚本", tts, "ruoxi", "saved-token", "saved-appkey"))
                .thenReturn(wavBytes);
        when(fileService.createFileAndReturnId(eq(wavBytes),
                argThat(name -> name.contains("product-88-zh-ruoxi") && name.endsWith(".wav")),
                eq("showroom/narration"), eq("audio/wav"))).thenReturn(9101L);

        ShowroomGeneratedAudio audio = adapter.generate(new ShowroomAudioGenerationRequest(sampleNarrationVersion()));

        assertEquals(9101L, audio.audioFileId());
        assertEquals(2, audio.audioDurationSeconds());
        assertEquals("ruoxi", audio.voice());
    }

    @Test
    void generate_whenFormatNotWav_failsFast() {
        YudaoAiProperties.Tts tts = new YudaoAiProperties.Tts();
        tts.getAliyunNls().setFormat("mp3");
        when(yudaoAiProperties.getTts()).thenReturn(tts);

        ShowroomNarrationException exception = assertThrows(ShowroomNarrationException.class,
                () -> adapter.generate(new ShowroomAudioGenerationRequest(sampleNarrationVersion())));

        assertEquals("SHOWROOM_AUDIO_GENERATION_FAILED", exception.code());
        verifyNoInteractions(aliyunNlsCredentialService, aliyunNlsTtsSynthesizer, fileService);
    }

    @Test
    void generate_whenSharedTokenMissing_failsFast() {
        YudaoAiProperties.Tts tts = new YudaoAiProperties.Tts();
        when(yudaoAiProperties.getTts()).thenReturn(tts);
        when(aliyunNlsCredentialService.resolveVoice(tts, null)).thenReturn("xiaoyun");
        when(aliyunNlsCredentialService.resolveAccessToken(tts))
                .thenThrow(new IllegalStateException("aliyun_nls_access_token_missing"));

        ShowroomNarrationException exception = assertThrows(ShowroomNarrationException.class,
                () -> adapter.generate(new ShowroomAudioGenerationRequest(sampleNarrationVersion())));

        assertEquals("SHOWROOM_AUDIO_GENERATION_FAILED", exception.code());
    }

    @Test
    void generate_whenFilePersistenceFails_failsFast() throws Exception {
        YudaoAiProperties.Tts tts = new YudaoAiProperties.Tts();
        when(yudaoAiProperties.getTts()).thenReturn(tts);
        when(aliyunNlsCredentialService.resolveVoice(tts, null)).thenReturn("xiaoyun");
        when(aliyunNlsCredentialService.resolveAccessToken(tts)).thenReturn("saved-token");
        when(aliyunNlsCredentialService.resolveAppKey(tts)).thenReturn("saved-appkey");
        byte[] wavBytes = buildSilentWavBytes(1);
        when(aliyunNlsTtsSynthesizer.synthesize(any(), eq(tts), eq("xiaoyun"), eq("saved-token"),
                eq("saved-appkey"))).thenReturn(wavBytes);
        when(fileService.createFileAndReturnId(any(byte[].class), any(), any(), any()))
                .thenThrow(new IllegalStateException("file_store_down"));

        ShowroomNarrationException exception = assertThrows(ShowroomNarrationException.class,
                () -> adapter.generate(new ShowroomAudioGenerationRequest(sampleNarrationVersion())));

        assertEquals("SHOWROOM_AUDIO_GENERATION_FAILED", exception.code());
    }

    @Test
    void generate_whenSharedAppKeyMissing_failsFast() {
        YudaoAiProperties.Tts tts = new YudaoAiProperties.Tts();
        when(yudaoAiProperties.getTts()).thenReturn(tts);
        when(aliyunNlsCredentialService.resolveVoice(tts, null)).thenReturn("xiaoyun");
        when(aliyunNlsCredentialService.resolveAccessToken(tts)).thenReturn("saved-token");
        when(aliyunNlsCredentialService.resolveAppKey(tts))
                .thenThrow(new IllegalStateException("aliyun_nls_appkey_missing"));

        ShowroomNarrationException exception = assertThrows(ShowroomNarrationException.class,
                () -> adapter.generate(new ShowroomAudioGenerationRequest(sampleNarrationVersion())));

        assertEquals("SHOWROOM_AUDIO_GENERATION_FAILED", exception.code());
    }

    private static ShowroomNarrationVersion sampleNarrationVersion() {
        return new ShowroomNarrationVersion(501L,
                new ShowroomNarrationKey(ShowroomNarrationTargetType.PRODUCT, 88L,
                        ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.ZH),
                601L, 1, "产品讲解脚本", null, null, null,
                ShowroomNarrationGenerationStatus.SCRIPT_GENERATED, ShowroomNarrationStatus.DRAFT,
                true, Instant.parse("2026-05-19T08:00:00Z"), null, false);
    }

    private static byte[] buildSilentWavBytes(int durationSeconds) throws IOException {
        AudioFormat format = new AudioFormat(8000F, 16, 1, true, false);
        int frameCount = 8000 * durationSeconds;
        byte[] pcm = new byte[frameCount * format.getFrameSize()];
        try (ByteArrayInputStream input = new ByteArrayInputStream(pcm);
             AudioInputStream audioInputStream = new AudioInputStream(input, format, frameCount);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, output);
            return output.toByteArray();
        }
    }
}
