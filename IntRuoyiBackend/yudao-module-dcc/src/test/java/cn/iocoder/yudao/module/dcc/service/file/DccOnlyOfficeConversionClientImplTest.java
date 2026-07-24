package cn.iocoder.yudao.module.dcc.service.file;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DccOnlyOfficeConversionClientImplTest {

    @Test
    void convertToPdf_sendsConversionFieldsAlongsideToken() throws Exception {
        AtomicReference<String> converterBody = new AtomicReference<>();
        AtomicReference<String> baseUrl = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/converter", exchange -> {
            converterBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] body = ("{\"endConvert\":true,\"fileType\":\"pdf\",\"fileUrl\":\""
                    + baseUrl.get() + "/converted.pdf\"}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/converted.pdf", exchange -> {
            byte[] body = "%PDF-converted".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/pdf");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        baseUrl.set("http://127.0.0.1:" + server.getAddress().getPort());
        try {
            DccOnlyOfficeConversionClientImpl client = new DccOnlyOfficeConversionClientImpl();

            byte[] result = client.convertToPdf(new DccOnlyOfficeConversionCommand(
                    baseUrl.get() + "/converter",
                    "secret",
                    "xlsx",
                    "DCC-test-key",
                    "Spec.xlsx",
                    "http://host.docker.internal:48104/admin-api/dcc/controlled-files/upload-preview/101/onlyoffice-file?token=source-token"));

            assertEquals("%PDF-converted", new String(result, StandardCharsets.UTF_8));
            String body = converterBody.get();
            assertTrue(body.contains("\"filetype\":\"xlsx\""), body);
            assertTrue(body.contains("\"outputtype\":\"pdf\""), body);
            assertTrue(body.contains("\"title\":\"Spec.xlsx\""), body);
            assertTrue(body.contains("\"key\":\"DCC-test-key\""), body);
            assertTrue(body.contains("\"url\":\"http://host.docker.internal:48104/admin-api/dcc/controlled-files/upload-preview/101/onlyoffice-file?token=source-token\""), body);
            assertTrue(body.contains("\"token\":\""), body);
        } finally {
            server.stop(0);
        }
    }
}
