package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProBatchRecordTotalRecognitionExtractorTest {

    private static final Path REAL_IDI_DOCX = Path.of("..", "..", "resource", "按压式球囊扩充压力泵IDI-001",
            "RE-PP-IDI-01（A 1） 按压式球囊扩充压力泵生产记录--2026.02.02生效.docx")
            .toAbsolutePath()
            .normalize();
    private static final Path REAL_IDI_DOC = Path.of("..", "..", "resource", "按压式球囊扩充压力泵IDI-001",
            "RE-PP-IDI-01（A 1） 按压式球囊扩充压力泵生产记录--2026.02.02生效.doc")
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

        JsonNode expectedJson = expectedJsonWithOriginalReferences(objectMapper);
        assertJsonSemanticallyEquals(expectedJson, actualJson, "$");
    }

    @Test
    void extractRealIdiDocMatchesExpectedTotalRecognitionJson() throws Exception {
        assertTrue(Files.exists(REAL_IDI_DOC), "real IDI production record doc fixture is required");
        assertTrue(Files.exists(EXPECTED_JSON), "expected total recognition JSON fixture is required");

        String sourceFileName = REAL_IDI_DOC.getFileName().toString();
        List<MesProBatchRecordParsedTable> tables = new MesProBatchRecordDocParser().parseWord(
                Files.readAllBytes(REAL_IDI_DOC), sourceFileName);
        MesProBatchRecordTotalRecognitionExtractor.RecognitionResult actual =
                new MesProBatchRecordTotalRecognitionExtractor().extract(sourceFileName, tables);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode actualJson = objectMapper.valueToTree(actual);
        Files.createDirectories(Path.of("target"));
        Files.writeString(Path.of("target", "idi-total-recognition-doc-actual.json"),
                objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(actualJson), StandardCharsets.UTF_8);

        JsonNode expectedJson = expectedJsonWithOriginalReferences(objectMapper);
        assertJsonSemanticallyEquals(expectedJson, actualJson, "$");
    }

    @Test
    void extractRealIdiDocumentsPreservesWaterAndRoomTemperatureReferences() throws Exception {
        for (Path source : List.of(REAL_IDI_DOCX, REAL_IDI_DOC)) {
            assertTrue(Files.exists(source), "real IDI production record fixture is required: " + source);
            String filename = source.getFileName().toString();
            var actual = new MesProBatchRecordTotalRecognitionExtractor().extract(filename,
                    new MesProBatchRecordDocParser().parseWord(Files.readAllBytes(source), filename));
            assertEquals(15, actual.processes().size());
            for (String processName : List.of("粗洗工序", "精洗工序", "清洗工序")) {
                var temperature = parameter(actual, processName, "清洗温度");
                assertEquals("室温", temperature.referenceValue(), filename + ": " + processName);
                assertEquals("text", temperature.ui().control());
                assertEquals("室温", temperature.ui().defaultValue());
                assertNull(temperature.ui().min(), "room temperature has no numeric limit in the source");
                assertNull(temperature.ui().max());
                assertNull(temperature.ui().step());
                assertNull(temperature.ui().unit());
                var medium = parameter(actual, processName, "清洗介质");
                String water = "粗洗工序".equals(processName) ? "纯化水" : "注射用水";
                assertEquals(water, medium.referenceValue());
                assertEquals(water, medium.ui().defaultValue());
                assertEquals(List.of(water), medium.ui().options());
            }
            // Preserve the existing interpretation of explicit numeric ranges and tolerances.
            var power = parameter(actual, "粗洗工序", "清洗功率");
            assertEquals("20-30%", power.referenceValue());
            assertEquals(BigDecimal.valueOf(25), power.ui().defaultValue());
            assertEquals(BigDecimal.valueOf(20), power.ui().min());
            assertEquals(BigDecimal.valueOf(30), power.ui().max());
            var drying = parameter(actual, "清洗工序", "烘干温度℃");
            assertEquals("60±4", drying.referenceValue());
            assertEquals(BigDecimal.valueOf(60), drying.ui().defaultValue());
            assertEquals(BigDecimal.valueOf(56), drying.ui().min());
            assertEquals(BigDecimal.valueOf(64), drying.ui().max());
        }
    }

    private static MesProBatchRecordTotalRecognitionExtractor.Parameter parameter(
            MesProBatchRecordTotalRecognitionExtractor.RecognitionResult result, String processName, String name) {
        return result.processes().stream().filter(process -> processName.equals(process.name()))
                .flatMap(process -> process.equipmentGroups().stream())
                .flatMap(group -> group.parameters().stream())
                .filter(parameter -> name.equals(parameter.name())).findFirst().orElseThrow();
    }

    private static JsonNode expectedJsonWithOriginalReferences(ObjectMapper objectMapper) throws Exception {
        JsonNode expected = objectMapper.readTree(Files.readString(EXPECTED_JSON, StandardCharsets.UTF_8));
        // The historical JSON snapshot substituted tap water and a numeric room-temperature range.
        // Correct only those source contradictions; retain its coverage of all other extracted data.
        for (JsonNode process : expected.path("processes")) {
            for (JsonNode group : process.path("equipmentGroups")) {
                for (JsonNode node : group.path("parameters")) {
                    ObjectNode parameter = (ObjectNode) node;
                    if ("清洗温度".equals(parameter.path("name").asText())) {
                        parameter.put("referenceValue", "室温");
                        parameter.set("ui", objectMapper.createObjectNode()
                                .put("control", "text").put("defaultValue", "室温"));
                    } else if ("粗洗工序".equals(process.path("name").asText())
                            && "清洗介质".equals(parameter.path("name").asText())) {
                        parameter.put("referenceValue", "纯化水");
                        ObjectNode ui = objectMapper.createObjectNode()
                                .put("control", "select").put("defaultValue", "纯化水");
                        ui.putArray("options").add("纯化水");
                        parameter.set("ui", ui);
                    }
                }
            }
        }
        return expected;
    }

    @Test
    void extractRealIdiDocxIncludesCriticalProcessFlags() throws Exception {
        assertTrue(Files.exists(REAL_IDI_DOCX), "real IDI production record docx fixture is required");

        String sourceFileName = REAL_IDI_DOCX.getFileName().toString();
        List<MesProBatchRecordParsedTable> tables = new MesProBatchRecordDocParser().parseWord(
                Files.readAllBytes(REAL_IDI_DOCX), sourceFileName);
        MesProBatchRecordTotalRecognitionExtractor.RecognitionResult actual =
                new MesProBatchRecordTotalRecognitionExtractor().extract(sourceFileName, tables);

        Map<String, MesProBatchRecordTotalRecognitionExtractor.ProcessRecognition> processes =
                actual.processes().stream().collect(Collectors.toMap(
                        MesProBatchRecordTotalRecognitionExtractor.ProcessRecognition::name,
                        process -> process));

        assertTrue(processes.values().stream().allMatch(process -> process.criticalProcess() != null),
                "every process must include criticalProcess parsed from the Word title checkboxes");
        assertEquals(false, processes.get("粗洗工序").criticalProcess());
        assertEquals(true, processes.get("清洗工序").criticalProcess());
        assertEquals(true, processes.get("光固工序").criticalProcess());
    }

    @Test
    void extractRealIdiDocIncludesCriticalProcessFlags() throws Exception {
        assertTrue(Files.exists(REAL_IDI_DOC), "real IDI production record doc fixture is required");

        String sourceFileName = REAL_IDI_DOC.getFileName().toString();
        List<MesProBatchRecordParsedTable> tables = new MesProBatchRecordDocParser().parseWord(
                Files.readAllBytes(REAL_IDI_DOC), sourceFileName);
        MesProBatchRecordTotalRecognitionExtractor.RecognitionResult actual =
                new MesProBatchRecordTotalRecognitionExtractor().extract(sourceFileName, tables);

        Map<String, MesProBatchRecordTotalRecognitionExtractor.ProcessRecognition> processes =
                actual.processes().stream().collect(Collectors.toMap(
                        MesProBatchRecordTotalRecognitionExtractor.ProcessRecognition::name,
                        process -> process));

        assertTrue(processes.values().stream().allMatch(process -> process.criticalProcess() != null),
                "every process must include criticalProcess parsed from the Word title checkboxes");
        assertEquals(false, processes.get("粗洗工序").criticalProcess());
        assertEquals(true, processes.get("清洗工序").criticalProcess());
        assertEquals(true, processes.get("光固工序").criticalProcess());
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

    @Test
    void extractAllowsMissingProductNameWhenProcessDataIsRecognized() {
        MesProBatchRecordParsedTable processTable = MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(1)
                .tableTitle("粗洗工序生产记录")
                .rows(List.of(List.of(MesProBatchRecordParsedCell.builder()
                        .text("☑关键/特殊工序")
                        .columnIndex(0)
                        .build())))
                .build();

        MesProBatchRecordTotalRecognitionExtractor.RecognitionResult actual =
                new MesProBatchRecordTotalRecognitionExtractor().extract(
                        "RE-PP-IDML-01（A 0）生产记录.doc",
                        List.of(processTable));

        assertEquals("", actual.product().name());
        assertEquals("IDML-01", actual.product().code());
        assertEquals(List.of("粗洗工序"), actual.processes().stream()
                .map(MesProBatchRecordTotalRecognitionExtractor.ProcessRecognition::name)
                .toList());
    }

    @Test
    void heatSealEquipmentAndParametersShareTheSameDomain() throws Exception {
        MesProBatchRecordTotalRecognitionExtractor extractor =
                new MesProBatchRecordTotalRecognitionExtractor();
        Method equipmentDomain = MesProBatchRecordTotalRecognitionExtractor.class
                .getDeclaredMethod("equipmentDomain", String.class);
        Method parameterDomain = MesProBatchRecordTotalRecognitionExtractor.class
                .getDeclaredMethod("parameterDomain", String.class);
        equipmentDomain.setAccessible(true);
        parameterDomain.setAccessible(true);

        assertEquals("热合", equipmentDomain.invoke(extractor, "封口热合机"));
        assertEquals("热合", parameterDomain.invoke(extractor, "热合温度℃"));
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
