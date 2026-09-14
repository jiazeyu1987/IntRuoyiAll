package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProBatchRecordReportParseTotalRecognitionJsonTest {

    private static final Path REAL_IDI_DOC = Path.of("..", "..", "resource", "按压式球囊扩充压力泵IDI-001",
            "RE-PP-IDI-01（A 1） 按压式球囊扩充压力泵生产记录--2026.02.02生效.doc")
            .toAbsolutePath()
            .normalize();
    private static final Path EXPECTED_JSON = Path.of("..", "..", "resource", "按压式球囊扩充压力泵IDI-001",
            "批记录总对应.json")
            .toAbsolutePath()
            .normalize();

    @Test
    void parseProductionBatchRecordTotalRecognitionJsonParsesRealDocToExpectedMappingJson() throws Exception {
        assertTrue(Files.exists(REAL_IDI_DOC), "real IDI production record doc fixture is required");
        assertTrue(Files.exists(EXPECTED_JSON), "expected total recognition JSON fixture is required");
        MesProBatchRecordReportServiceImpl service = new MesProBatchRecordReportServiceImpl();
        Field docParserField = MesProBatchRecordReportServiceImpl.class.getDeclaredField("docParser");
        docParserField.setAccessible(true);
        docParserField.set(service, new MesProBatchRecordDocParser());
        MockMultipartFile file = new MockMultipartFile("file", REAL_IDI_DOC.getFileName().toString(),
                "application/msword", Files.readAllBytes(REAL_IDI_DOC));

        String totalRecognitionJson = service.parseProductionBatchRecordTotalRecognitionJson(file);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode actualJson = objectMapper.readTree(totalRecognitionJson);
        Files.createDirectories(Path.of("target"));
        Files.writeString(Path.of("target", "idi-total-recognition-service-actual.json"),
                objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(actualJson), StandardCharsets.UTF_8);
        JsonNode expectedJson = objectMapper.readTree(Files.readString(EXPECTED_JSON, StandardCharsets.UTF_8));
        assertJsonSemanticallyEquals(expectedJson, actualJson, "$");
    }

    private static void assertJsonSemanticallyEquals(JsonNode expected, JsonNode actual, String path) {
        if (expected.isNumber() && actual.isNumber()) {
            assertEquals(0, expected.decimalValue().compareTo(actual.decimalValue()), path);
            return;
        }
        assertEquals(expected.getNodeType(), actual.getNodeType(), path);
        if (expected.isObject()) {
            ObjectNode expectedObject = (ObjectNode) expected;
            ObjectNode actualObject = (ObjectNode) actual;
            assertEquals(expectedObject.size(), actualObject.size(), path);
            expectedObject.fieldNames().forEachRemaining(field -> {
                assertTrue(actualObject.has(field), path + "." + field);
                assertJsonSemanticallyEquals(expectedObject.get(field), actualObject.get(field), path + "." + field);
            });
            return;
        }
        if (expected.isArray()) {
            ArrayNode expectedArray = (ArrayNode) expected;
            ArrayNode actualArray = (ArrayNode) actual;
            assertEquals(expectedArray.size(), actualArray.size(), path);
            for (int index = 0; index < expectedArray.size(); index++) {
                assertJsonSemanticallyEquals(expectedArray.get(index), actualArray.get(index), path + "[" + index + "]");
            }
            return;
        }
        assertEquals(expected, actual, path);
    }
}
