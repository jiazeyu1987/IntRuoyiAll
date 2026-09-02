package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        assertEquals(expectedJson, actualJson);
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
}
