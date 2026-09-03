package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProBatchRecordTotalRecognitionExtractorTest {

    private static final Path REAL_IDI_DOCX = Path.of("..", "..", "resource", "按压式球囊扩充压力泵IDI-001",
            "RE-PP-IDI-01（A 1） 按压式球囊扩充压力泵生产记录--2026.02.02生效.docx")
            .toAbsolutePath()
            .normalize();
    private static final Path EXPECTED_JSON = Path.of("..", "..", "resource", "按压式球囊扩充压力泵IDI-001",
            "批记录总对应.json")
            .toAbsolutePath()
            .normalize();

    @Test
    void extractRealIdiDocxMatchesExpectedTotalRecognitionJson() throws Exception {
        assertTrue(Files.exists(REAL_IDI_DOCX), "real IDI production record docx fixture is required");
        assertTrue(Files.exists(EXPECTED_JSON), "expected total recognition JSON fixture is required");

        String sourceFileName = REAL_IDI_DOCX.getFileName().toString();
        List<MesProBatchRecordParsedTable> tables = new MesProBatchRecordDocParser().parseWord(
                Files.readAllBytes(REAL_IDI_DOCX), sourceFileName);
        MesProBatchRecordTotalRecognitionExtractor.RecognitionResult actual =
                new MesProBatchRecordTotalRecognitionExtractor().extract(sourceFileName, tables);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode actualJson = objectMapper.valueToTree(actual);
        Files.createDirectories(Path.of("target"));
        Files.writeString(Path.of("target", "idi-total-recognition-actual.json"),
                objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(actualJson), StandardCharsets.UTF_8);

        JsonNode expectedJson = objectMapper.readTree(Files.readString(EXPECTED_JSON, StandardCharsets.UTF_8));
        assertJsonSemanticallyEquals(expectedJson, actualJson, "$");
    }

    @Test
    void importResultCanCarryTotalRecognitionJson() {
        MesProBatchRecordImportResult result = MesProBatchRecordImportResult.builder()
                .importedCount(0)
                .createdCount(0)
                .updatedCount(0)
                .reports(List.of())
                .build()
                .withTotalRecognitionJson("{\"schemaVersion\":1}");

        assertEquals("{\"schemaVersion\":1}", result.totalRecognitionJson());
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
