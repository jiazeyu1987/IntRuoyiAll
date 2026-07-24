package cn.iocoder.yudao.module.ai.service.tts;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.ai.framework.ai.config.YudaoAiProperties;
import com.alibaba.fastjson.JSON;
import org.springframework.stereotype.Component;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class AliyunNlsTtsSynthesizer {

    public static final Set<String> SUPPORTED_VOICES = Set.of(
            "xiaoyun",
            "xiaogang",
            "ruoxi",
            "siqi"
    );

    private static final Set<String> SUPPORTED_FORMATS = Set.of("wav", "mp3");
    private static final String DEFAULT_REGION = "cn-shanghai";
    private static final int MAX_TEXT_CHARS_PER_REQUEST = 300;
    private static final String APPLICATION_JSON = "application/json";

    public byte[] synthesize(String text, YudaoAiProperties.Tts tts, String selectedVoice) {
        return synthesize(text, tts, selectedVoice, null, null);
    }

    public byte[] synthesize(String text, YudaoAiProperties.Tts tts, String selectedVoice, String accessTokenOverride) {
        return synthesize(text, tts, selectedVoice, accessTokenOverride, null);
    }

    public byte[] synthesize(String text, YudaoAiProperties.Tts tts, String selectedVoice,
                             String accessTokenOverride, String appKeyOverride) {
        YudaoAiProperties.AliyunNls config = tts != null && tts.getAliyunNls() != null
                ? tts.getAliyunNls() : new YudaoAiProperties.AliyunNls();
        String appkey = required(StrUtil.blankToDefault(appKeyOverride, config.getAppkey()),
                "aliyun_nls_appkey_missing");
        String accessToken = required(StrUtil.blankToDefault(accessTokenOverride, config.getAccessToken()),
                "aliyun_nls_access_token_missing");
        String voice = resolveVoice(config, selectedVoice);
        String format = resolveFormat(config);
        int sampleRate = resolveSampleRate(config);
        long timeoutMs = resolveTimeoutMs(config);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .build();
        List<String> textChunks = splitText(String.valueOf(text == null ? "" : text), MAX_TEXT_CHARS_PER_REQUEST);
        List<byte[]> chunkAudios = new ArrayList<>(textChunks.size());
        try {
            for (String textChunk : textChunks) {
                Map<String, Object> params = new LinkedHashMap<>();
                params.put("appkey", appkey);
                params.put("text", textChunk);
                params.put("format", format);
                params.put("sample_rate", sampleRate);
                params.put("voice", voice);
                putIfPresent(params, "speech_rate", config.getSpeechRate());
                putIfPresent(params, "pitch_rate", config.getPitchRate());
                putIfPresent(params, "volume", resolveVolume(config));

                HttpRequest request = HttpRequest.newBuilder(buildUri(config))
                        .timeout(Duration.ofMillis(timeoutMs))
                        .header("Content-Type", APPLICATION_JSON)
                        .header("X-NLS-Token", accessToken)
                        .header("X-Request-ID", UUID.randomUUID().toString())
                        .POST(HttpRequest.BodyPublishers.ofString(JSON.toJSONString(params), StandardCharsets.UTF_8))
                        .build();
                HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() != 200) {
                    throw new IllegalStateException("aliyun_nls_tts_failed status=" + response.statusCode()
                            + " body=" + responseBodyPreview(response.body(), accessToken, appkey));
                }
                chunkAudios.add(response.body());
            }
            return chunkAudios.size() == 1 ? chunkAudios.get(0) : mergeWavAudios(chunkAudios);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("aliyun_nls_tts_interrupted", ex);
        } catch (IOException ex) {
            throw new IllegalStateException("aliyun_nls_tts_io_failed:" + ex.getMessage(), ex);
        }
    }

    private static String required(String value, String message) {
        String trimmed = StrUtil.trim(value);
        if (StrUtil.isBlank(trimmed)) {
            throw new IllegalStateException(message);
        }
        return trimmed;
    }

    private static String resolveVoice(YudaoAiProperties.AliyunNls config, String selectedVoice) {
        String voice = StrUtil.trim(selectedVoice);
        if (StrUtil.isBlank(voice)) {
            voice = StrUtil.trim(config.getVoice());
        }
        if (StrUtil.isBlank(voice)) {
            throw new IllegalStateException("aliyun_nls_voice_missing");
        }
        if (!SUPPORTED_VOICES.contains(voice)) {
            throw new IllegalStateException("aliyun_nls_voice_unsupported:" + voice);
        }
        return voice;
    }

    private static String resolveFormat(YudaoAiProperties.AliyunNls config) {
        String format = StrUtil.blankToDefault(StrUtil.trim(config.getFormat()), "wav").toLowerCase();
        if (!SUPPORTED_FORMATS.contains(format)) {
            throw new IllegalArgumentException("aliyun_nls_format_unsupported:" + format);
        }
        return format;
    }

    private static int resolveSampleRate(YudaoAiProperties.AliyunNls config) {
        Integer sampleRate = config.getSampleRate();
        if (sampleRate == null || (!Integer.valueOf(8000).equals(sampleRate) && !Integer.valueOf(16000).equals(sampleRate))) {
            throw new IllegalArgumentException("aliyun_nls_sample_rate_invalid");
        }
        return sampleRate;
    }

    private static Integer resolveVolume(YudaoAiProperties.AliyunNls config) {
        Integer volume = config.getVolume();
        if (volume == null) {
            return null;
        }
        if (volume < 0 || volume > 100) {
            throw new IllegalArgumentException("aliyun_nls_volume_invalid");
        }
        return volume;
    }

    private static long resolveTimeoutMs(YudaoAiProperties.AliyunNls config) {
        Long timeoutMs = config.getTimeoutMs();
        if (timeoutMs == null || timeoutMs < 1000L) {
            throw new IllegalArgumentException("aliyun_nls_timeout_ms_invalid");
        }
        return timeoutMs;
    }

    private static void putIfPresent(Map<String, Object> params, String key, Object value) {
        if (value == null || StrUtil.isBlank(String.valueOf(value))) {
            return;
        }
        params.put(key, value);
    }

    private static URI buildUri(YudaoAiProperties.AliyunNls config) {
        String url = StrUtil.trim(config.getUrl());
        if (StrUtil.isBlank(url)) {
            String region = StrUtil.blankToDefault(StrUtil.trim(config.getRegion()), DEFAULT_REGION);
            url = "https://nls-gateway-" + region + ".aliyuncs.com/stream/v1/tts";
        }
        return URI.create(url);
    }

    private static List<String> splitText(String text, int maxCharsPerRequest) {
        if (text.length() <= maxCharsPerRequest) {
            return List.of(text);
        }
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + maxCharsPerRequest, text.length());
            if (end < text.length()) {
                int splitIndex = findPreferredSplitIndex(text, start, end);
                if (splitIndex > start) {
                    end = splitIndex;
                }
            }
            chunks.add(text.substring(start, end));
            start = end;
        }
        return chunks;
    }

    private static int findPreferredSplitIndex(String text, int startInclusive, int endExclusive) {
        for (int index = endExclusive - 1; index > startInclusive; index--) {
            if (isPreferredSplitChar(text.charAt(index))) {
                return index + 1;
            }
        }
        return endExclusive;
    }

    private static boolean isPreferredSplitChar(char ch) {
        return switch (ch) {
            case '。', '！', '？', '；', '，', '、', '：', '.', '!', '?', ';', ',', ':',
                 '\n', '\r', '\t', ' ' -> true;
            default -> false;
        };
    }

    private static byte[] mergeWavAudios(List<byte[]> chunkAudios) {
        AudioFormat expectedFormat = null;
        long totalFrames = 0L;
        ByteArrayOutputStream pcmOutput = new ByteArrayOutputStream();
        try {
            for (byte[] chunkAudio : chunkAudios) {
                try (AudioInputStream audioInputStream =
                             AudioSystem.getAudioInputStream(new ByteArrayInputStream(chunkAudio))) {
                    AudioFormat chunkFormat = audioInputStream.getFormat();
                    if (expectedFormat == null) {
                        expectedFormat = chunkFormat;
                    } else if (!sameFormat(expectedFormat, chunkFormat)) {
                        throw new IllegalStateException("aliyun_nls_wav_format_mismatch");
                    }
                    byte[] pcmBytes = audioInputStream.readAllBytes();
                    pcmOutput.write(pcmBytes);
                    long frameLength = audioInputStream.getFrameLength();
                    if (frameLength <= 0 && chunkFormat.getFrameSize() > 0) {
                        frameLength = pcmBytes.length / chunkFormat.getFrameSize();
                    }
                    totalFrames += frameLength;
                }
            }
            if (expectedFormat == null) {
                throw new IllegalStateException("aliyun_nls_wav_merge_failed:no_audio_chunks");
            }
            try (ByteArrayInputStream pcmInput = new ByteArrayInputStream(pcmOutput.toByteArray());
                 AudioInputStream mergedInputStream = new AudioInputStream(pcmInput, expectedFormat, totalFrames);
                 ByteArrayOutputStream wavOutput = new ByteArrayOutputStream()) {
                AudioSystem.write(mergedInputStream, AudioFileFormat.Type.WAVE, wavOutput);
                return wavOutput.toByteArray();
            }
        } catch (UnsupportedAudioFileException | IOException ex) {
            throw new IllegalStateException("aliyun_nls_wav_merge_failed:" + ex.getMessage(), ex);
        }
    }

    private static boolean sameFormat(AudioFormat left, AudioFormat right) {
        return left.getEncoding().equals(right.getEncoding())
                && Float.compare(left.getSampleRate(), right.getSampleRate()) == 0
                && left.getSampleSizeInBits() == right.getSampleSizeInBits()
                && left.getChannels() == right.getChannels()
                && left.getFrameSize() == right.getFrameSize()
                && Float.compare(left.getFrameRate(), right.getFrameRate()) == 0
                && left.isBigEndian() == right.isBigEndian();
    }

    private static String responseBodyPreview(byte[] body, String accessToken, String appKey) {
        if (body == null || body.length == 0) {
            return "";
        }
        String text = new String(body, StandardCharsets.UTF_8);
        if (StrUtil.isNotBlank(accessToken)) {
            text = text.replace(accessToken, "****");
        }
        if (StrUtil.isNotBlank(appKey)) {
            text = text.replace(appKey, "****");
        }
        return text.length() > 200 ? text.substring(0, 200) : text;
    }

}
