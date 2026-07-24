package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class RuntimeOpsProbeDefaultHttpClient implements RuntimeOpsProbeHttpClient {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Override
    public RuntimeOpsProbeHttpResult probe(String url, Duration timeout) {
        long startNanos = System.nanoTime();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(timeout)
                .GET()
                .build();
        try {
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            return new RuntimeOpsProbeHttpResult(response.statusCode(), elapsedMillis(startNanos));
        } catch (IOException ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("HTTP probe interrupted", ex);
        }
    }

    private long elapsedMillis(long startNanos) {
        return Math.max(1L, Duration.ofNanos(System.nanoTime() - startNanos).toMillis());
    }
}
