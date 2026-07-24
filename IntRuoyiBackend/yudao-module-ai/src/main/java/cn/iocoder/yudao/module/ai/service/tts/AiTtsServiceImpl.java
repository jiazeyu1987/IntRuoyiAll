package cn.iocoder.yudao.module.ai.service.tts;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.ai.framework.ai.config.YudaoAiProperties;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.ai.enums.ErrorCodeConstants.TTS_TEST_AUDIO_EMPTY;
import static cn.iocoder.yudao.module.ai.enums.ErrorCodeConstants.TTS_TEST_REQUEST_FAILED;
import static cn.iocoder.yudao.module.ai.enums.ErrorCodeConstants.TTS_TEST_TARGET_NOT_CONFIGURED;
import static cn.iocoder.yudao.module.ai.enums.ErrorCodeConstants.TTS_TEST_VOICE_UNSUPPORTED;

@Service
@Slf4j
public class AiTtsServiceImpl implements AiTtsService {

    private static final MediaType WAV_MEDIA_TYPE = MediaType.parseMediaType("audio/wav");

    @Resource
    private YudaoAiProperties yudaoAiProperties;
    @Resource
    private WindowsTtsSynthesizer windowsTtsSynthesizer;
    @Resource
    private DashscopeTtsSynthesizer dashscopeTtsSynthesizer;
    @Resource
    private AliyunNlsTtsSynthesizer aliyunNlsTtsSynthesizer;
    @Resource
    private AiTtsAliyunNlsCredentialService aliyunNlsCredentialService;

    @Override
    public AudioPayload generateSpeech(String text, String provider, String voice) {
        try {
            YudaoAiProperties.Tts tts = resolveTtsConfig();
            String resolvedProvider = resolveProvider(tts, provider);
            MediaType contentType = WAV_MEDIA_TYPE;
            byte[] audio = switch (resolvedProvider) {
                case "windows" -> windowsTtsSynthesizer.synthesize(text, tts);
                case "dashscope" -> {
                    validateDashscopeVoice(voice);
                    yield dashscopeTtsSynthesizer.synthesize(text, tts, voice);
                }
                case "aliyun_nls" -> {
                    String resolvedVoice = aliyunNlsCredentialService.resolveVoice(tts, voice);
                    contentType = resolveAliyunNlsMediaType(tts);
                    String accessToken = aliyunNlsCredentialService.resolveAccessToken(tts);
                    String appKey = aliyunNlsCredentialService.resolveAppKey(tts);
                    yield aliyunNlsTtsSynthesizer.synthesize(text, tts, resolvedVoice, accessToken, appKey);
                }
                default -> throw exception(TTS_TEST_TARGET_NOT_CONFIGURED);
            };
            if (audio == null || audio.length == 0) {
                throw exception(TTS_TEST_AUDIO_EMPTY);
            }
            return new AudioPayload(audio, contentType);
        } catch (ServiceException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.error("[generateSpeech][Internal TTS request failed]", ex);
            throw exception(TTS_TEST_REQUEST_FAILED, StrUtil.blankToDefault(ex.getMessage(), ex.getClass().getSimpleName()));
        }
    }

    private YudaoAiProperties.Tts resolveTtsConfig() {
        YudaoAiProperties.Tts tts = yudaoAiProperties.getTts();
        if (tts == null || !tts.isEnable()) {
            throw exception(TTS_TEST_TARGET_NOT_CONFIGURED);
        }
        return tts;
    }

    private String resolveProvider(YudaoAiProperties.Tts tts, String provider) {
        String resolved = StrUtil.blankToDefault(provider, "").trim().toLowerCase();
        if (StrUtil.isBlank(resolved)) {
            resolved = StrUtil.blankToDefault(tts.getProvider(), "").trim().toLowerCase();
        }
        if ("flash".equals(resolved)) {
            resolved = "dashscope";
        }
        if ("nls".equals(resolved)) {
            resolved = "aliyun_nls";
        }
        return resolved;
    }

    private void validateDashscopeVoice(String voice) {
        String selectedVoice = StrUtil.trim(voice);
        if (StrUtil.isBlank(selectedVoice)) {
            return;
        }
        if (!DashscopeTtsSynthesizer.SUPPORTED_VOICES.contains(selectedVoice)) {
            throw exception(TTS_TEST_VOICE_UNSUPPORTED, selectedVoice);
        }
    }

    private void validateAliyunNlsVoice(String voice) {
        String selectedVoice = StrUtil.trim(voice);
        if (StrUtil.isBlank(selectedVoice)) {
            return;
        }
        if (!AliyunNlsTtsSynthesizer.SUPPORTED_VOICES.contains(selectedVoice)) {
            throw exception(TTS_TEST_VOICE_UNSUPPORTED, selectedVoice);
        }
    }

    private MediaType resolveAliyunNlsMediaType(YudaoAiProperties.Tts tts) {
        YudaoAiProperties.AliyunNls aliyunNls = tts != null && tts.getAliyunNls() != null
                ? tts.getAliyunNls() : new YudaoAiProperties.AliyunNls();
        String format = StrUtil.blankToDefault(StrUtil.trim(aliyunNls.getFormat()), "wav").toLowerCase();
        return switch (format) {
            case "wav" -> WAV_MEDIA_TYPE;
            case "mp3" -> MediaType.parseMediaType("audio/mpeg");
            default -> throw new IllegalArgumentException("aliyun_nls_format_unsupported:" + format);
        };
    }

}
