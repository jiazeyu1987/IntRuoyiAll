package cn.iocoder.yudao.module.showroom.release;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShowroomPublicReleaseReadbackVerifierTest {

    @Test
    void shouldVerifyCurrentManifestAndRootDocumentThroughPublicWebsiteOrigin() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            if (path.endsWith("/release/current")) {
                writeJson(exchange, "{\"releaseId\":\"rel-1\",\"manifestHash\":\"hash-1\"}");
                return;
            }
            if (path.endsWith("/release/rel-1/manifest")) {
                writeJson(exchange, "{\"releaseId\":\"rel-1\",\"manifestHash\":\"hash-1\"}");
                return;
            }
            if (path.endsWith("/release/rel-1/documents/website-index.json")) {
                writeJson(exchange, "{\"releaseId\":\"rel-1\",\"documentId\":\"website-index\"}");
                return;
            }
            writeText(exchange, 404, "application/json", "{}");
        });
        server.start();
        try {
            var verifier = new ShowroomPublicReleaseReadbackVerifier(
                    "http://127.0.0.1:" + server.getAddress().getPort(), HttpClient.newHttpClient());

            assertDoesNotThrow(() -> verifier.verify("yingtai-showroom", "TEST",
                    "rel-1", "hash-1", "website-index"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldUseHttp11ReadbackRequestsForViteWebsiteCompatibility() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        when(httpClient.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenAnswer(invocation -> {
                    HttpRequest request = invocation.getArgument(0);
                    String path = request.uri().getPath();
                    if (path.endsWith("/release/current")) {
                        return jsonResponse("{\"releaseId\":\"rel-1\",\"manifestHash\":\"hash-1\"}");
                    }
                    if (path.endsWith("/release/rel-1/manifest")) {
                        return jsonResponse("{\"releaseId\":\"rel-1\",\"manifestHash\":\"hash-1\"}");
                    }
                    if (path.endsWith("/release/rel-1/documents/website-index.json")) {
                        return jsonResponse("{\"releaseId\":\"rel-1\",\"documentId\":\"website-index\"}");
                    }
                    throw new AssertionError("Unexpected readback path: " + path);
                });

        var verifier = new ShowroomPublicReleaseReadbackVerifier("http://127.0.0.1:8083", httpClient);

        assertDoesNotThrow(() -> verifier.verify("yingtai-showroom", "TEST",
                "rel-1", "hash-1", "website-index"));

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient, times(3)).send(requestCaptor.capture(),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
        assertTrue(requestCaptor.getAllValues().stream()
                        .allMatch(request -> request.version().orElse(null) == HttpClient.Version.HTTP_1_1),
                "Website release readback must force HTTP/1.1 because the local Vite Website times out with Java h2c upgrade probes");
    }

    @Test
    void shouldFailWhenPublicWebsiteReturnsHtmlInsteadOfCurrentReleaseJson() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> writeText(exchange, 200, "text/html", "<html></html>"));
        server.start();
        try {
            var verifier = new ShowroomPublicReleaseReadbackVerifier(
                    "http://127.0.0.1:" + server.getAddress().getPort(), HttpClient.newHttpClient());

            IllegalStateException exception = assertThrows(IllegalStateException.class,
                    () -> verifier.verify("yingtai-showroom", "TEST", "rel-1", "hash-1", "website-index"));

            assertTrue(exception.getMessage().contains("SHOWROOM_RELEASE_PUBLIC_READBACK_FAILED"));
            assertTrue(exception.getMessage().contains("expected application/json"));
        } finally {
            server.stop(0);
        }
    }

    @SuppressWarnings("unchecked")
    private static HttpResponse<String> jsonResponse(String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.headers()).thenReturn(HttpHeaders.of(
                Map.of("Content-Type", List.of("application/json")), (name, value) -> true));
        when(response.body()).thenReturn(body);
        return response;
    }

    private static void writeJson(HttpExchange exchange, String body) throws IOException {
        writeText(exchange, 200, "application/json", body);
    }

    private static void writeText(HttpExchange exchange, int status, String contentType, String body)
            throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
