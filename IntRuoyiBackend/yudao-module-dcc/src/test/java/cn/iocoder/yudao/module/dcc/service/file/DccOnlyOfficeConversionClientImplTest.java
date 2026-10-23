package cn.iocoder.yudao.module.dcc.service.file;

import com.sun.net.httpserver.HttpServer;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DccOnlyOfficeConversionClientImplTest {

    @ParameterizedTest
    @ValueSource(strings = {"xls", "xlsx", "doc", "docx"})
    void convertToPdf_sendsConversionFieldsAlongsideToken(String fileType) throws Exception {
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
                    fileType,
                    "DCC-test-key",
                    "Spec." + fileType,
                    "http://host.docker.internal:48104/admin-api/dcc/controlled-files/upload-preview/101/onlyoffice-file?token=source-token"));

            assertEquals("%PDF-converted", new String(result, StandardCharsets.UTF_8));
            String body = converterBody.get();
            assertTrue(body.contains("\"filetype\":\"" + fileType + "\""), body);
            assertTrue(body.contains("\"outputtype\":\"pdf\""), body);
            assertTrue(body.contains("\"title\":\"Spec." + fileType + "\""), body);
            assertTrue(body.contains("\"key\":\"DCC-test-key\""), body);
            assertTrue(body.contains("\"url\":\"http://host.docker.internal:48104/admin-api/dcc/controlled-files/upload-preview/101/onlyoffice-file?token=source-token\""), body);
            assertTrue(body.contains("\"token\":\""), body);
            Map<?, ?> payload = JsonUtils.parseObject(body, Map.class);
            String jwt = (String) payload.get("token");
            Map<?, ?> signedPayload = JsonUtils.parseObject(new String(
                    Base64.getUrlDecoder().decode(jwt.split("\\.")[1]), StandardCharsets.UTF_8), Map.class);
            boolean spreadsheet = fileType.equals("xls") || fileType.equals("xlsx");
            assertEquals(spreadsheet, payload.containsKey("spreadsheetLayout"),
                    "Only spreadsheets require an explicit conversion layout");
            assertEquals(spreadsheet, signedPayload.containsKey("spreadsheetLayout"));
            if (spreadsheet) {
                assertTrue(payload.get("spreadsheetLayout") instanceof Map,
                        "Spreadsheet layout must be a JSON object");
                assertEquals(Map.of(), payload.get("spreadsheetLayout"),
                        "Conversion must retain default spreadsheet layout settings");
                assertEquals(payload.get("spreadsheetLayout"), signedPayload.get("spreadsheetLayout"),
                        "Converter request and JWT must sign the same layout");
            }
        } finally {
            server.stop(0);
        }
    }
}
