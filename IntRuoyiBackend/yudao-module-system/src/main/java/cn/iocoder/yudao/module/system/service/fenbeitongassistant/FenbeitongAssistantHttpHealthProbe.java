package cn.iocoder.yudao.module.system.service.fenbeitongassistant;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class FenbeitongAssistantHttpHealthProbe implements FenbeitongAssistantHealthProbe {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    @Resource
    private FenbeitongAssistantProperties properties;

    @Override
    public boolean isRunning() {
        String healthUrl = properties.getAssistantHealthUrl();
        if (StrUtil.isBlank(healthUrl)) {
            return false;
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(healthUrl))
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(Duration.ofSeconds(properties.getProbeTimeoutSeconds()))
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return response.statusCode() == 200
                    && StrUtil.blankToDefault(response.body(), "").contains("\"status\":\"ok\"");
        } catch (IOException ex) {
            return false;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

}
