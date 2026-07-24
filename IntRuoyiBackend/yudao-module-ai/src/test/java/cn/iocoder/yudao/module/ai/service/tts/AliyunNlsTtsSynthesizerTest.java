package cn.iocoder.yudao.module.ai.service.tts;

import cn.iocoder.yudao.module.ai.framework.ai.config.YudaoAiProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.net.InetSocketAddress;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AliyunNlsTtsSynthesizerTest {

    @Test
    void synthesize_sendsNlsTokenAndParams_returnsAudioBytes() throws Exception {
        byte[] expectedAudio = new byte[] {'R', 'I', 'F', 'F', 1, 2, 3, 4};
        AtomicReference<String> observedMethod = new AtomicReference<>();
        AtomicReference<String> observedQuery = new AtomicReference<>();
        AtomicReference<String> observedBody = new AtomicReference<>();
        AtomicReference<String> observedToken = new AtomicReference<>();
        AtomicReference<String> observedRequestId = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/stream/v1/tts", exchange -> {
            observedMethod.set(exchange.getRequestMethod());
            observedQuery.set(exchange.getRequestURI().getRawQuery());
            observedToken.set(exchange.getRequestHeaders().getFirst("X-NLS-Token"));
            observedRequestId.set(exchange.getRequestHeaders().getFirst("X-Request-ID"));
            observedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(200, expectedAudio.length);
            exchange.getResponseBody().write(expectedAudio);
            exchange.close();
        });
        server.start();
        try {
            AliyunNlsTtsSynthesizer synthesizer = new AliyunNlsTtsSynthesizer();
            YudaoAiProperties.Tts tts = buildTtsConfig("http://127.0.0.1:" + server.getAddress().getPort() + "/stream/v1/tts");

            byte[] audio = synthesizer.synthesize("中文测试", tts, "xiaoyun");

            assertArrayEquals(expectedAudio, audio);
            assertEquals("POST", observedMethod.get());
            assertTrue(observedQuery.get() == null || observedQuery.get().isBlank());
            assertEquals("token-1", observedToken.get());
            assertTrue(observedRequestId.get() != null && !observedRequestId.get().isBlank());
            assertTrue(observedBody.get().contains("\"appkey\":\"appkey-1\""));
            assertTrue(observedBody.get().contains("\"text\":\"中文测试\""));
            assertTrue(observedBody.get().contains("\"format\":\"wav\""));
            assertTrue(observedBody.get().contains("\"sample_rate\":16000"));
            assertTrue(observedBody.get().contains("\"voice\":\"xiaoyun\""));
            assertTrue(observedBody.get().contains("\"volume\":50"));
            assertTrue(!observedBody.get().contains("speech_rate"));
            assertTrue(!observedBody.get().contains("pitch_rate"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void synthesize_whenTextExceedsShortLimit_splitsRequestsAndJoinsWav() throws Exception {
        AtomicInteger requestCount = new AtomicInteger();
        AtomicReference<String> firstMethod = new AtomicReference<>();
        AtomicReference<String> secondMethod = new AtomicReference<>();
        AtomicReference<String> firstBody = new AtomicReference<>();
        AtomicReference<String> secondBody = new AtomicReference<>();
        byte[] chunk1 = buildSilentWavBytes(1);
        byte[] chunk2 = buildSilentWavBytes(2);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/stream/v1/tts", exchange -> {
            int index = requestCount.incrementAndGet();
            byte[] response = index == 1 ? chunk1 : chunk2;
            if (index == 1) {
                firstMethod.set(exchange.getRequestMethod());
                firstBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            } else if (index == 2) {
                secondMethod.set(exchange.getRequestMethod());
                secondBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            }
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            AliyunNlsTtsSynthesizer synthesizer = new AliyunNlsTtsSynthesizer();
            YudaoAiProperties.Tts tts = buildTtsConfig("http://127.0.0.1:" + server.getAddress().getPort() + "/stream/v1/tts");
            String longText = "第一段公司介绍内容".repeat(40) + "。第二段继续补充介绍。";

            byte[] audio = synthesizer.synthesize(longText, tts, "xiaoyun");

            assertEquals(2, requestCount.get());
            assertEquals("POST", firstMethod.get());
            assertEquals("POST", secondMethod.get());
            assertTrue(firstBody.get().contains("\"text\""));
            assertTrue(secondBody.get().contains("\"text\""));
            assertTrue(audio.length > chunk1.length);
            try (AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(audio))) {
                assertEquals(8000F, audioInputStream.getFormat().getSampleRate());
                assertTrue(audioInputStream.getFrameLength() > 0);
            }
        } finally {
            server.stop(0);
        }
    }

    @Test
    void synthesize_whenAccessTokenMissing_failsFast() {
        AliyunNlsTtsSynthesizer synthesizer = new AliyunNlsTtsSynthesizer();
        YudaoAiProperties.Tts tts = buildTtsConfig("http://127.0.0.1:1/stream/v1/tts");
        tts.getAliyunNls().setAccessToken("");

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> synthesizer.synthesize("中文测试", tts, "xiaoyun"));

        assertEquals("aliyun_nls_access_token_missing", ex.getMessage());
    }

    @Test
    void synthesize_whenNlsStatusIsNotOk_failsFastWithStatusAndBody() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/stream/v1/tts", exchange -> {
            byte[] body = "token expired".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(403, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            AliyunNlsTtsSynthesizer synthesizer = new AliyunNlsTtsSynthesizer();
            YudaoAiProperties.Tts tts = buildTtsConfig("http://127.0.0.1:" + server.getAddress().getPort() + "/stream/v1/tts");

            IllegalStateException ex = assertThrows(
                    IllegalStateException.class,
                    () -> synthesizer.synthesize("中文测试", tts, "xiaoyun"));

            assertTrue(ex.getMessage().contains("aliyun_nls_tts_failed status=403"));
            assertTrue(ex.getMessage().contains("token expired"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void synthesize_whenNlsErrorMentionsAccessToken_masksTokenInMessage() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/stream/v1/tts", exchange -> {
            byte[] body = "The token 'token-1' has expired".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(400, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            AliyunNlsTtsSynthesizer synthesizer = new AliyunNlsTtsSynthesizer();
            YudaoAiProperties.Tts tts = buildTtsConfig("http://127.0.0.1:" + server.getAddress().getPort() + "/stream/v1/tts");

            IllegalStateException ex = assertThrows(
                    IllegalStateException.class,
                    () -> synthesizer.synthesize("中文测试", tts, "xiaoyun"));

            assertTrue(ex.getMessage().contains("aliyun_nls_tts_failed status=400"));
            assertFalse(ex.getMessage().contains("token-1"));
            assertTrue(ex.getMessage().contains("The token '****' has expired"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void synthesize_whenNlsErrorMentionsAppKey_masksAppKeyInMessage() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/stream/v1/tts", exchange -> {
            byte[] body = "The appkey 'appkey-1' is invalid".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(400, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            AliyunNlsTtsSynthesizer synthesizer = new AliyunNlsTtsSynthesizer();
            YudaoAiProperties.Tts tts = buildTtsConfig("http://127.0.0.1:" + server.getAddress().getPort() + "/stream/v1/tts");

            IllegalStateException ex = assertThrows(
                    IllegalStateException.class,
                    () -> synthesizer.synthesize("中文测试", tts, "xiaoyun"));

            assertTrue(ex.getMessage().contains("aliyun_nls_tts_failed status=400"));
            assertFalse(ex.getMessage().contains("appkey-1"));
            assertTrue(ex.getMessage().contains("The appkey '****' is invalid"));
        } finally {
            server.stop(0);
        }
    }

    private static YudaoAiProperties.Tts buildTtsConfig(String url) {
        YudaoAiProperties.Tts tts = new YudaoAiProperties.Tts();
        YudaoAiProperties.AliyunNls aliyunNls = new YudaoAiProperties.AliyunNls();
        aliyunNls.setUrl(url);
        aliyunNls.setAppkey("appkey-1");
        aliyunNls.setAccessToken("token-1");
        aliyunNls.setVoice("xiaoyun");
        aliyunNls.setFormat("wav");
        aliyunNls.setSampleRate(16000);
        aliyunNls.setVolume(50);
        aliyunNls.setTimeoutMs(30000L);
        tts.setAliyunNls(aliyunNls);
        return tts;
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
