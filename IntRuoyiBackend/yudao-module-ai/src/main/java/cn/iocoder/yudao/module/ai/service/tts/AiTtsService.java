package cn.iocoder.yudao.module.ai.service.tts;

import org.springframework.http.MediaType;

public interface AiTtsService {

    AudioPayload generateSpeech(String text, String provider, String voice);

    record AudioPayload(byte[] audioBytes, MediaType contentType) {
    }

}
