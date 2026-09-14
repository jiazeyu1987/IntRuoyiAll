package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProBatchRecordProcessMaterialExtractorTest {

    private static final Path REAL_IDI_DOCX = Path.of("..", "..", "resource", "按压式球囊扩充压力泵IDI-001",
            "RE-PP-IDI-01（A 1） 按压式球囊扩充压力泵生产记录--2026.02.02生效.docx")
            .toAbsolutePath()
            .normalize();
    private static final Path EXPECTED_MAPPING = Path.of("..", "..", "resource", "按压式球囊扩充压力泵IDI-001",
            "批记录物料对应.txt")
            .toAbsolutePath()
            .normalize();
    private enum MaterialSection {
        NONE,
        INPUT,
        OUTPUT
    }

    @Test
    void extractRealIdiDocxMatchesBatchRecordMaterialMapping() throws Exception {
        assertTrue(Files.exists(REAL_IDI_DOCX), "real IDI production record docx fixture is required");
        assertTrue(Files.exists(EXPECTED_MAPPING), "expected material mapping fixture is required");

        MesProBatchRecordDocParser docParser = new MesProBatchRecordDocParser();
        List<MesProBatchRecordParsedTable> tables = docParser.parseWord(
                Files.readAllBytes(REAL_IDI_DOCX), REAL_IDI_DOCX.getFileName().toString());

        List<MesProBatchRecordProcessMaterialExtractor.ProcessMaterialMapping> actual =
                new MesProBatchRecordProcessMaterialExtractor().extract(tables);
        Files.createDirectories(Path.of("target"));
        Files.writeString(Path.of("target", "idi-material-actual.txt"), format(actual), StandardCharsets.UTF_8);
        Files.writeString(Path.of("target", "idi-material-parsed-tables.txt"), debugRows(tables), StandardCharsets.UTF_8);

        assertEquals(parseExpected(Files.readString(EXPECTED_MAPPING, StandardCharsets.UTF_8)), actual);
    }

    private static List<MesProBatchRecordProcessMaterialExtractor.ProcessMaterialMapping> parseExpected(String text) {
        List<ExpectedProcessMaterialMappingBuilder> mappings = new java.util.ArrayList<>();
        ExpectedProcessMaterialMappingBuilder current = null;
        MaterialSection section = MaterialSection.NONE;
        for (String rawLine : text.lines().map(String::stripTrailing).toList()) {
            if (rawLine.isBlank()) {
                continue;
            }
            if (!rawLine.startsWith("\t")) {
                current = new ExpectedProcessMaterialMappingBuilder(rawLine.strip());
                mappings.add(current);
                section = MaterialSection.NONE;
                continue;
            }
            if (current == null) {
                continue;
            }
            String line = rawLine.strip();
            if ("输入:".equals(line)) {
                section = MaterialSection.INPUT;
                continue;
            }
            if ("输出:".equals(line)) {
                section = MaterialSection.OUTPUT;
                continue;
            }
            if ("输入:无".equals(line)) {
                section = MaterialSection.NONE;
                continue;
            }
            if ("输出:无".equals(line)) {
                section = MaterialSection.NONE;
                continue;
            }
            if ("无".equals(line)) {
                continue;
            }
            MesProBatchRecordProcessMaterialExtractor.ProcessMaterial material = parseExpectedMaterial(line);
            if (section == MaterialSection.INPUT) {
                current.inputMaterials.add(material);
            } else if (section == MaterialSection.OUTPUT) {
                current.outputMaterials.add(material);
            }
        }
        return mappings.stream()
                .map(ExpectedProcessMaterialMappingBuilder::build)
                .toList();
    }

    private static MesProBatchRecordProcessMaterialExtractor.ProcessMaterial parseExpectedMaterial(String line) {
        int separatorIndex = line.indexOf('/');
        assertTrue(separatorIndex > 0, "expected material line must be code/name: " + line);
        return new MesProBatchRecordProcessMaterialExtractor.ProcessMaterial(
                line.substring(0, separatorIndex),
                line.substring(separatorIndex + 1));
    }

    private static String format(List<MesProBatchRecordProcessMaterialExtractor.ProcessMaterialMapping> mappings) {
        return mappings.stream()
                .map(MesProBatchRecordProcessMaterialExtractorTest::format)
                .collect(Collectors.joining("\n"));
    }

    private static String format(MesProBatchRecordProcessMaterialExtractor.ProcessMaterialMapping mapping) {
        return mapping.processName() + "\n"
                + "\t输入:" + formatMaterials(mapping.inputMaterials())
                + "\n\t输出:" + formatMaterials(mapping.outputMaterials());
    }

    private static String formatMaterials(List<MesProBatchRecordProcessMaterialExtractor.ProcessMaterial> materials) {
        if (materials == null || materials.isEmpty()) {
            return "无";
        }
        return "\n" + materials.stream()
                .map(material -> "\t\t" + material.code() + "/" + material.name())
                .collect(Collectors.joining("\n"));
    }

    private static String debugRows(List<MesProBatchRecordParsedTable> tables) {
        StringBuilder builder = new StringBuilder();
        for (MesProBatchRecordParsedTable table : tables) {
            builder.append("TABLE ").append(table.getTableTitle())
                    .append(" rows=").append(table.getRowCount())
                    .append(" cols=").append(table.getColumnCount())
                    .append('\n');
            List<List<MesProBatchRecordParsedCell>> rows = table.getRows();
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                builder.append(" R").append(rowIndex + 1).append(": ");
                builder.append(rows.get(rowIndex).stream()
                        .map(cell -> "[" + cell.getColumnIndex() + "," + cell.getRowSpan() + "x"
                                + cell.getColSpan() + "]" + String.valueOf(cell.getText()).replace("\n", "\\n"))
                        .collect(Collectors.joining(" | ")));
                builder.append('\n');
            }
        }
        return builder.toString();
    }

    private static class ExpectedProcessMaterialMappingBuilder {

        private final String processName;
        private final List<MesProBatchRecordProcessMaterialExtractor.ProcessMaterial> inputMaterials =
                new java.util.ArrayList<>();
        private final List<MesProBatchRecordProcessMaterialExtractor.ProcessMaterial> outputMaterials =
                new java.util.ArrayList<>();

        private ExpectedProcessMaterialMappingBuilder(String processName) {
            this.processName = processName;
        }

        private MesProBatchRecordProcessMaterialExtractor.ProcessMaterialMapping build() {
            return new MesProBatchRecordProcessMaterialExtractor.ProcessMaterialMapping(processName,
                    inputMaterials, outputMaterials);
        }
    }
}
