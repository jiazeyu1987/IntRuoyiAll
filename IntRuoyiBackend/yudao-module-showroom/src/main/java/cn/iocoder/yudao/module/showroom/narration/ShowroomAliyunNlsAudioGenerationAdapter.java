package cn.iocoder.yudao.module.showroom.narration;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.ai.framework.ai.config.YudaoAiProperties;
import cn.iocoder.yudao.module.ai.service.tts.AiTtsAliyunNlsCredentialService;
import cn.iocoder.yudao.module.ai.service.tts.AliyunNlsTtsSynthesizer;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;

@Component
public class ShowroomAliyunNlsAudioGenerationAdapter implements ShowroomAudioGenerationAdapter {

    private static final String SHOWROOM_NARRATION_DIRECTORY = "showroom/narration";
    private static final MediaType WAV_MEDIA_TYPE = MediaType.parseMediaType("audio/wav");

    @Resource
    private YudaoAiProperties yudaoAiProperties;
    @Resource
    private AiTtsAliyunNlsCredentialService aliyunNlsCredentialService;
    @Resource
    private AliyunNlsTtsSynthesizer aliyunNlsTtsSynthesizer;
    @Resource
    private FileService fileService;

    @Override
    public ShowroomGeneratedAudio generate(ShowroomAudioGenerationRequest request) {
        ShowroomNarrationVersion narrationVersion = request.narrationVersion();
        if (isBlank(narrationVersion.scriptText())) {
            throw new ShowroomNarrationException("SHOWROOM_SCRIPT_MISSING",
                    "script text is required before narration audio generation");
        }
        YudaoAiProperties.Tts tts = yudaoAiProperties.getTts();
        if (tts == null || tts.getAliyunNls() == null) {
            throw new ShowroomNarrationException("SHOWROOM_AUDIO_GENERATION_FAILED",
                    "aliyun nls configuration is missing");
        }
        String format = StrUtil.blankToDefault(StrUtil.trim(tts.getAliyunNls().getFormat()), "wav").toLowerCase();
        if (!"wav".equals(format)) {
            throw new ShowroomNarrationException("SHOWROOM_AUDIO_GENERATION_FAILED",
                    "showroom narration requires aliyun nls wav output");
        }
        try {
            String resolvedVoice = aliyunNlsCredentialService.resolveVoice(tts, null);
            String accessToken = aliyunNlsCredentialService.resolveAccessToken(tts);
            String appKey = aliyunNlsCredentialService.resolveAppKey(tts);
            byte[] audioBytes = aliyunNlsTtsSynthesizer.synthesize(narrationVersion.scriptText(), tts, resolvedVoice,
                    accessToken, appKey);
            int audioDurationSeconds = resolveWavDurationSeconds(audioBytes);
            Long audioFileId = fileService.createFileAndReturnId(audioBytes, buildFileName(narrationVersion,
                    resolvedVoice), SHOWROOM_NARRATION_DIRECTORY, WAV_MEDIA_TYPE.toString());
            return new ShowroomGeneratedAudio(audioFileId, audioDurationSeconds, resolvedVoice);
        } catch (ShowroomNarrationException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new ShowroomNarrationException("SHOWROOM_AUDIO_GENERATION_FAILED",
                    StrUtil.blankToDefault(ex.getMessage(), ex.getClass().getSimpleName()));
        }
    }

    private static int resolveWavDurationSeconds(byte[] audioBytes) {
        try (ByteArrayInputStream input = new ByteArrayInputStream(audioBytes);
             AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(input)) {
            if (!WAV_MEDIA_TYPE.toString().equalsIgnoreCase(audioInputStream.getFormat().getEncoding().toString())
                    && audioInputStream.getFrameLength() <= 0) {
                throw new IllegalStateException("wav_duration_unavailable");
            }
            float frameRate = audioInputStream.getFormat().getFrameRate();
            if (frameRate <= 0F) {
                throw new IllegalStateException("wav_frame_rate_invalid");
            }
            return Math.max(1, (int) Math.ceil(audioInputStream.getFrameLength() / frameRate));
        } catch (Exception ex) {
            throw new IllegalStateException("wav_duration_parse_failed:" + ex.getMessage(), ex);
        }
    }

    private static String buildFileName(ShowroomNarrationVersion narrationVersion, String voice) {
        return narrationVersion.key().targetType().name().toLowerCase() + "-"
                + narrationVersion.key().targetId() + "-"
                + narrationVersion.key().language().name().toLowerCase() + "-"
                + voice + ".wav";
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
