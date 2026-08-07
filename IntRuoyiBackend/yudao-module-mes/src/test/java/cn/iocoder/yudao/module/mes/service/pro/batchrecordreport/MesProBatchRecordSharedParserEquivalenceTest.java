package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import cn.iocoder.yudao.module.bpm.formcenter.model.FormTemplateImportCommand;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormTemplateRecognition;
import cn.iocoder.yudao.module.bpm.formcenter.runtime.DefaultWordFormTemplateRecognizer;
import cn.iocoder.yudao.module.wordparser.DefaultSharedWordDocumentParser;
import cn.iocoder.yudao.module.wordparser.SharedWordDocumentParser;
import cn.iocoder.yudao.module.wordparser.WordParseCommand;
import cn.iocoder.yudao.module.wordparser.WordParseResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcBorders;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProBatchRecordSharedParserEquivalenceTest {

    private static final String BASELINE = "contracts/pressure-pump-parser-baseline.json";

    @Test
    void legacyRealDoc_matchesFrozenCompleteSnapshotWithoutSkip() throws Exception {
        Path fixture = BatchRecordReportTestFixtures.pressurePumpRecordDoc();
        assertTrue(Files.isRegularFile(fixture), "mandatory real DOC fixture is missing: " + fixture);
        byte[] bytes = Files.readAllBytes(fixture);
        MesProBatchRecordDocParser parser = TestBatchRecordFixtures.wordParser();

        List<MesProBatchRecordParsedTable> first = parser.parse(bytes);
        List<MesProBatchRecordParsedTable> second = parser.parse(bytes);

        assertEquals(first, second, "legacy real-DOC output must be deterministic before migration");
        assertBaseline("realDoc", first);
    }

    @Test
    void legacySyntheticDocx_matchesFrozenCompleteSnapshotWithoutFilenameRules() throws Exception {
        byte[] bytes = syntheticDocx();
        MesProBatchRecordDocParser parser = TestBatchRecordFixtures.wordParser();

        List<MesProBatchRecordParsedTable> first = parser.parseDocx(bytes);
        List<MesProBatchRecordParsedTable> second = parser.parseDocx(bytes);

        assertEquals(first, second, "legacy synthetic-DOCX output must be deterministic before migration");
        assertBaseline("syntheticDocx", first);
    }

    @Test
    void mesAdapter_requiresSharedCanonicalParserConstructor() throws Exception {
        Class<?> parserContract = Class.forName("cn.iocoder.yudao.module.wordparser.SharedWordDocumentParser");
        assertNotNull(MesProBatchRecordDocParser.class.getConstructor(parserContract),
                "MES adapter must require the shared canonical parser through constructor injection");
    }

    @Test
    void bpmAndMesAdaptersConsumeTheSameCanonicalRawContractForRealDoc() throws Exception {
        Path fixture = BatchRecordReportTestFixtures.pressurePumpRecordDoc();
        assertTrue(Files.isRegularFile(fixture), "mandatory real DOC fixture is missing: " + fixture);
        byte[] source = Files.readAllBytes(fixture);
        List<WordParseCommand> commands = new ArrayList<>();
        List<WordParseResult> rawResults = new ArrayList<>();
        SharedWordDocumentParser delegate = new DefaultSharedWordDocumentParser();
        SharedWordDocumentParser recordingParser = command -> {
            commands.add(command);
            WordParseResult result = delegate.parse(command);
            rawResults.add(result);
            return result;
        };

        FormTemplateRecognition bpmRecognition = new DefaultWordFormTemplateRecognizer(recordingParser)
                .recognize(FormTemplateImportCommand.of(
                        "Pressure Pump", "V1.0", fixture.getFileName().toString(), source, null));
        List<MesProBatchRecordParsedTable> mesTables = new MesProBatchRecordDocParser(recordingParser).parse(source);

        assertTrue(bpmRecognition.isSuccess());
        assertFalse(mesTables.isEmpty());
        assertEquals(2, commands.size());
        assertEquals(commands.get(0).extension(), commands.get(1).extension());
        assertEquals(commands.get(0).profile(), commands.get(1).profile());
        assertArrayEquals(commands.get(0).source(), commands.get(1).source());
        assertSameCanonicalRawResult(rawResults.get(0), rawResults.get(1));
    }

    private void assertSameCanonicalRawResult(WordParseResult first, WordParseResult second) {
        assertEquals(first.paragraphs(), second.paragraphs());
        assertEquals(first.documentFrame(), second.documentFrame());
        assertEquals(first.tables(), second.tables());
        assertEquals(first.diagnostics().parserVersion(), second.diagnostics().parserVersion());
        assertEquals(first.diagnostics().sourceHash(), second.diagnostics().sourceHash());
        assertEquals(first.diagnostics().extension(), second.diagnostics().extension());
        assertEquals(first.diagnostics().paragraphCount(), second.diagnostics().paragraphCount());
        assertEquals(first.diagnostics().tableCount(), second.diagnostics().tableCount());
        assertEquals(first.diagnostics().warningCodes(), second.diagnostics().warningCodes());
        assertEquals(first.diagnostics().failureCode(), second.diagnostics().failureCode());
    }

    private void assertBaseline(String key, List<MesProBatchRecordParsedTable> tables) throws Exception {
        JsonNode expected = baseline().path(key);
        assertEquals(expected.path("tableCount").asInt(), tables.size(), key + " table count changed");
        assertEquals(expected.path("snapshotHash").asText(), snapshotHash(tables),
                key + " complete ordered parser snapshot changed");
    }

    private JsonNode baseline() throws Exception {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(BASELINE)) {
            assertNotNull(input, "mandatory parser baseline is missing: " + BASELINE);
            return new ObjectMapper().readTree(input);
        }
    }

    private String snapshotHash(List<MesProBatchRecordParsedTable> tables) throws Exception {
        StringBuilder snapshot = new StringBuilder();
        value(snapshot, tables.size());
        for (MesProBatchRecordParsedTable table : tables) {
            value(snapshot, table.getSourceTableIndex());
            value(snapshot, table.getSourceTopLevelTableIndex());
            value(snapshot, table.getSourceSplitIndex());
            value(snapshot, table.getTableTitle());
            value(snapshot, table.getRowCount());
            value(snapshot, table.getColumnCount());
            integers(snapshot, table.getColumnWidths());
            value(snapshot, table.getPreserveSourceGrid());
            value(snapshot, table.getRouteBSource());
            frame(snapshot, table.getDocumentFrame());
            rows(snapshot, table.getRows());
        }
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(snapshot.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    private void frame(StringBuilder snapshot, MesProBatchRecordDocumentFrame frame) {
        if (frame == null) {
            value(snapshot, null);
            return;
        }
        value(snapshot, "frame");
        rows(snapshot, frame.getHeaderRows());
        rows(snapshot, frame.getFooterRows());
    }

    private void rows(StringBuilder snapshot, List<List<MesProBatchRecordParsedCell>> rows) {
        if (rows == null) {
            value(snapshot, null);
            return;
        }
        value(snapshot, rows.size());
        for (List<MesProBatchRecordParsedCell> row : rows) {
            value(snapshot, row.size());
            for (MesProBatchRecordParsedCell cell : row) {
                value(snapshot, cell.getText());
                value(snapshot, cell.getRowSpan());
                value(snapshot, cell.getColSpan());
                value(snapshot, cell.getColumnIndex());
                value(snapshot, cell.getLogicalColumnIndex());
                value(snapshot, cell.getLogicalColSpan());
                value(snapshot, cell.isBold());
                value(snapshot, cell.getFontSize());
                value(snapshot, cell.getHorizontalAlign());
                value(snapshot, cell.getVerticalAlign());
                value(snapshot, cell.getWidthPx());
                value(snapshot, cell.getHeightPx());
                value(snapshot, cell.isFillable());
                value(snapshot, cell.isVisualBlank());
                value(snapshot, cell.isBorderless());
                value(snapshot, cell.isDiagonalSlash());
                value(snapshot, cell.isReviewedCellRule());
                value(snapshot, cell.getCellRuleSource());
                value(snapshot, cell.getTopBorderStyle());
                value(snapshot, cell.getBottomBorderStyle());
                value(snapshot, cell.getLeftBorderStyle());
                value(snapshot, cell.getRightBorderStyle());
                value(snapshot, cell.getBackgroundColor());
                value(snapshot, cell.getDocumentFrameRole());
                value(snapshot, cell.getPlaceholder());
                value(snapshot, cell.getInputType());
            }
        }
    }

    private void integers(StringBuilder snapshot, List<Integer> values) {
        if (values == null) {
            value(snapshot, null);
            return;
        }
        value(snapshot, values.size());
        values.forEach(value -> value(snapshot, value));
    }

    private void value(StringBuilder snapshot, Object value) {
        if (value == null) {
            snapshot.append("-1:");
            return;
        }
        String text = value.toString();
        snapshot.append(text.length()).append(':').append(text);
    }

    private byte[] syntheticDocx() throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFParagraph outside = document.createParagraph();
            run(outside, "Canonical outside text", false, 11);
            XWPFHeader header = document.createHeader(HeaderFooterType.DEFAULT);
            run(header.createParagraph(), "Canonical frame header", true, 9);
            XWPFFooter footer = document.createFooter(HeaderFooterType.DEFAULT);
            run(footer.createParagraph(), "Canonical frame footer", false, 9);

            XWPFTable table = document.createTable(3, 3);
            XWPFTableRow first = table.getRow(0);
            first.setHeight(720);
            XWPFTableCell heading = first.getCell(0);
            heading.setWidth("3600");
            heading.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
            text(heading, "Canonical merged heading", true, 14, ParagraphAlignment.CENTER);
            properties(heading).addNewGridSpan().setVal(BigInteger.valueOf(2));
            first.removeCell(1);
            text(first.getCell(1), "Canonical side", false, 11, ParagraphAlignment.LEFT);
            first.getCell(1).setWidth("1800");

            XWPFTableCell vertical = table.getRow(1).getCell(0);
            text(vertical, "Canonical vertical", false, 11, ParagraphAlignment.LEFT);
            vertical.setWidth("1800");
            properties(vertical).addNewVMerge().setVal(STMerge.RESTART);
            XWPFTableCell follower = table.getRow(2).getCell(0);
            follower.setWidth("1800");
            properties(follower).addNewVMerge().setVal(STMerge.CONTINUE);

            XWPFTableCell diagonal = table.getRow(1).getCell(1);
            text(diagonal, "", false, 11, ParagraphAlignment.CENTER);
            diagonal.setWidth("1800");
            CTTcBorders borders = properties(diagonal).addNewTcBorders();
            border(borders.addNewTop());
            border(borders.addNewBottom());
            border(borders.addNewLeft());
            border(borders.addNewRight());
            border(borders.addNewTl2Br());
            properties(diagonal).addNewShd().setFill("D9EAF7");

            text(table.getRow(1).getCell(2), "Canonical normal", false, 11, ParagraphAlignment.LEFT);
            table.getRow(1).getCell(2).setWidth("1800");
            text(table.getRow(2).getCell(1), "Canonical tail one", false, 11, ParagraphAlignment.LEFT);
            table.getRow(2).getCell(1).setWidth("1800");
            text(table.getRow(2).getCell(2), "Canonical tail two", false, 11, ParagraphAlignment.LEFT);
            table.getRow(2).getCell(2).setWidth("1800");

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.write(output);
            return output.toByteArray();
        }
    }

    private void text(XWPFTableCell cell, String text, boolean bold, int size, ParagraphAlignment alignment) {
        XWPFParagraph paragraph = cell.getParagraphArray(0);
        paragraph.setAlignment(alignment);
        run(paragraph, text, bold, size);
    }

    private void run(XWPFParagraph paragraph, String text, boolean bold, int size) {
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setBold(bold);
        run.setFontSize(size);
    }

    private CTTcPr properties(XWPFTableCell cell) {
        return cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
    }

    private void border(CTBorder border) {
        border.setVal(STBorder.SINGLE);
        border.setSz(BigInteger.valueOf(8));
    }
}
