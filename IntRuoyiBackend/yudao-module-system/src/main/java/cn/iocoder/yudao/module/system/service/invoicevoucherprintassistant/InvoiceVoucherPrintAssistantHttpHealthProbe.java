package cn.iocoder.yudao.module.system.service.invoicevoucherprintassistant;

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
public class InvoiceVoucherPrintAssistantHttpHealthProbe implements InvoiceVoucherPrintAssistantHealthProbe {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    @Resource
    private InvoiceVoucherPrintAssistantProperties properties;

    @Override
    public boolean isRunning() {
        String assistantBaseUrl = properties.getAssistantBaseUrl();
        if (StrUtil.isBlank(assistantBaseUrl)) {
            return false;
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(assistantBaseUrl))
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(Duration.ofSeconds(properties.getProbeTimeoutSeconds()))
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200 && response.statusCode() != 302 && response.statusCode() != 401 && response.statusCode() != 403) {
                return false;
            }
            String body = StrUtil.blankToDefault(response.body(), "");
            return body.contains("发票凭证打印助手") || body.contains("没有发票凭证打印权限");
        } catch (IOException ex) {
            return false;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

}
