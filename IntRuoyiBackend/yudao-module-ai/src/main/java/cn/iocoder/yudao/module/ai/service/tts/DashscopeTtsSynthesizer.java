package cn.iocoder.yudao.module.ai.service.tts;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.ai.framework.ai.config.YudaoAiProperties;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesisAudioFormat;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesisParam;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesizer;
import com.alibaba.dashscope.protocol.ConnectionOptions;
import com.alibaba.dashscope.utils.Constants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Set;

@Component
public class DashscopeTtsSynthesizer {

    public static final Set<String> SUPPORTED_VOICES = Set.of(
            "longyang",
            "longxiaochun",
            "longxiaoxia",
            "longxiaobei"
    );

    @Value("${spring.ai.dashscope.api-key:}")
    private String dashscopeApiKey;

    public byte[] synthesize(String text, YudaoAiProperties.Tts tts, String selectedVoice) {
        if (StrUtil.isBlank(dashscopeApiKey)) {
            throw new IllegalStateException("dashscope_api_key_missing");
        }
        Constants.apiKey = StrUtil.trim(dashscopeApiKey);
        String voice = resolveVoice(tts, selectedVoice);
        YudaoAiProperties.Dashscope dashscope = tts != null && tts.getDashscope() != null ? tts.getDashscope() : new YudaoAiProperties.Dashscope();
        String model = StrUtil.blankToDefault(StrUtil.trim(dashscope.getModel()), "cosyvoice-v3-plus");
        long timeoutMs = dashscope.getTimeoutMs() != null ? Math.max(1000L, dashscope.getTimeoutMs()) : 30000L;
        int volume = dashscope.getVolume() != null ? Math.max(0, Math.min(dashscope.getVolume(), 100)) : 50;
        float speechRate = dashscope.getSpeechRate() != null ? Math.max(0.5F, Math.min(dashscope.getSpeechRate(), 2.0F)) : 1.0F;
        SpeechSynthesisParam param = SpeechSynthesisParam.builder()
                .model(model)
                .apiKey(StrUtil.trim(dashscopeApiKey))
                .voice(voice)
                .format(SpeechSynthesisAudioFormat.WAV_16000HZ_MONO_16BIT)
                .volume(volume)
                .speechRate(speechRate)
                .pitchRate(1.0F)
                .build();
        ConnectionOptions connectionOptions = ConnectionOptions.builder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .readTimeout(Duration.ofMillis(timeoutMs))
                .writeTimeout(Duration.ofMillis(timeoutMs))
                .build();
        SpeechSynthesizer synthesizer = new SpeechSynthesizer(param, null, Constants.baseWebsocketApiUrl, connectionOptions);
        ByteBuffer buffer = synthesizer.call(text, timeoutMs);
        if (buffer == null) {
            return new byte[0];
        }
        ByteBuffer copy = buffer.duplicate();
        copy.rewind();
        byte[] audio = new byte[copy.remaining()];
        copy.get(audio);
        return audio;
    }

    private String resolveVoice(YudaoAiProperties.Tts tts, String selectedVoice) {
        String voice = StrUtil.trim(selectedVoice);
        if (StrUtil.isBlank(voice) && tts != null && tts.getDashscope() != null) {
            voice = StrUtil.trim(tts.getDashscope().getVoice());
        }
        return StrUtil.blankToDefault(voice, "longyang");
    }

}
