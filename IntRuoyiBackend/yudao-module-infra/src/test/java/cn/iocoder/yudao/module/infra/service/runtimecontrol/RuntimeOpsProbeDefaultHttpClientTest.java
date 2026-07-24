package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeOpsProbeDefaultHttpClientTest {

    @Test
    void probeShouldUsePlainHttp11ForLocalFrontendServers() throws Exception {
        RuntimeOpsProbeDefaultHttpClient client = new RuntimeOpsProbeDefaultHttpClient();
        AtomicReference<String> rawRequest = new AtomicReference<>("");
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            Future<?> handler = executor.submit(() -> serveFrontendProbe(server, rawRequest));

            RuntimeOpsProbeHttpResult result = client.probe(
                    "http://127.0.0.1:" + server.getLocalPort() + "/", Duration.ofMillis(300));

            assertEquals(200, result.getStatusCode());
            assertTrue(result.getDurationMillis() > 0);
            handler.get(2, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
        assertFalse(rawRequest.get().toLowerCase(Locale.ROOT).contains("upgrade: h2c"),
                "local frontend probes must not use h2c upgrade requests");
    }

    private void serveFrontendProbe(ServerSocket server, AtomicReference<String> rawRequest) {
        try (Socket socket = server.accept()) {
            socket.setSoTimeout(1000);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(),
                    StandardCharsets.US_ASCII));
            StringBuilder request = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                request.append(line).append('\n');
            }
            rawRequest.set(request.toString());
            if (request.toString().toLowerCase(Locale.ROOT).contains("upgrade: h2c")) {
                Thread.sleep(600);
                return;
            }
            socket.getOutputStream().write(("HTTP/1.1 200 OK\r\n"
                    + "Content-Length: 0\r\n"
                    + "Connection: close\r\n"
                    + "\r\n").getBytes(StandardCharsets.US_ASCII));
            socket.getOutputStream().flush();
        } catch (Exception ignored) {
            throw new IllegalStateException("frontend probe test server failed", ignored);
        }
    }
}

