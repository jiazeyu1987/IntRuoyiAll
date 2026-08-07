package cn.iocoder.yudao.module.wordparser;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SharedWordDocumentParserTest {

    private final SharedWordDocumentParser parser = new DefaultSharedWordDocumentParser();

    @Test
    void parseCanonicalDocx_preservesParagraphFrameMergeGeometryAndStyle() throws Exception {
        byte[] bytes = SharedWordParserTestDocuments.canonicalDocx();

        WordParseResult result = parser.parse(command(bytes, ".docx", "synthetic.docx"));

        assertEquals(List.of(SharedWordParserTestDocuments.OUTSIDE_PARAGRAPH), result.paragraphs());
        assertEquals(SharedWordParserTestDocuments.HEADER_TEXT,
                result.documentFrame().headerRows().get(0).get(0).text());
        assertEquals(SharedWordParserTestDocuments.FOOTER_TEXT,
                result.documentFrame().footerRows().get(0).get(0).text());
        assertEquals(1, result.tables().size());

        WordTable table = result.tables().get(0);
        assertEquals(0, table.sourceTopLevelTableIndex());
        assertEquals(3, table.rowCount());
        assertEquals(3, table.columnCount());
        assertEquals(List.of(120, 120, 120), table.columnWidths());
        assertEquals(List.of(2, 3, 2), table.rows().stream().map(List::size).toList());

        WordCell mergedHeader = table.rows().get(0).get(0);
        assertEquals("Merged heading", mergedHeader.text());
        assertEquals(2, mergedHeader.colSpan());
        assertTrue(mergedHeader.bold());
        assertEquals(14, mergedHeader.fontSize());
        assertEquals("center", mergedHeader.horizontalAlign());
        assertEquals("center", mergedHeader.verticalAlign());
        assertEquals(0, mergedHeader.columnIndex());
        assertEquals(0, mergedHeader.logicalColumnIndex());
        assertEquals(2, mergedHeader.logicalColSpan());
        assertEquals(240, mergedHeader.widthPx());
        assertEquals(48, mergedHeader.heightPx());

        WordCell verticalStart = table.rows().get(1).get(0);
        assertEquals("Vertical value", verticalStart.text());
        assertEquals(2, verticalStart.rowSpan());
        assertEquals(0, verticalStart.columnIndex());
        assertEquals(0, verticalStart.logicalColumnIndex());
        assertEquals(1, verticalStart.logicalColSpan());
        assertEquals(120, verticalStart.widthPx());
        assertEquals(36, verticalStart.heightPx());

        WordCell diagonal = table.rows().get(1).stream()
                .filter(cell -> "Diagonal cell".equals(cell.text()))
                .findFirst()
                .orElseThrow();
        assertEquals(1, diagonal.columnIndex());
        assertEquals(1, diagonal.logicalColumnIndex());
        assertEquals(1, diagonal.logicalColSpan());
        assertEquals(120, diagonal.widthPx());
        assertTrue(diagonal.diagonalSlash());
        assertEquals("single", diagonal.topBorderStyle());
        assertEquals("single", diagonal.bottomBorderStyle());
        assertEquals("single", diagonal.leftBorderStyle());
        assertEquals("single", diagonal.rightBorderStyle());
        assertEquals("D9EAF7", diagonal.backgroundColor());
        assertNotNull(result.diagnostics());
    }

    @Test
    void parseTrackedDoc_isMandatoryDeterministicAndBusinessNeutral() throws Exception {
        Path fixture = reactorRoot().resolve("yudao-module-mes/src/test/resources/fixtures/pressure-pump-record.doc");
        assertTrue(Files.isRegularFile(fixture), "mandatory real DOC fixture is missing: " + fixture);
        byte[] bytes = Files.readAllBytes(fixture);

        WordParseResult first = parser.parse(command(bytes, ".doc", "opaque-source-a.doc"));
        WordParseResult second = parser.parse(command(bytes, ".doc", "opaque-source-a.doc"));
        WordParseResult renamed = parser.parse(command(bytes, ".doc", "opaque-source-b.doc"));

        assertEquals(first, second);
        assertStructureEquals(first, renamed);
        assertNotEquals(first.diagnostics().fileNameHash(), renamed.diagnostics().fileNameHash());
        assertFalse(first.paragraphs().isEmpty());
        assertFalse(first.documentFrame().headerRows().isEmpty());
        assertFalse(first.documentFrame().footerRows().isEmpty());
        assertFalse(first.tables().isEmpty());
        assertTrue(first.tables().stream().allMatch(table -> table.sourceTopLevelTableIndex() >= 0));
        List<WordCell> cells = first.tables().stream().flatMap(table -> table.rows().stream())
                .flatMap(List::stream).toList();
        assertTrue(cells.size() > 100, "tracked DOC must expose its full raw cell structure");
        assertTrue(cells.stream().anyMatch(cell -> cell.text().contains("组装Ⅰ工序生产记录")));
        assertTrue(cells.stream().allMatch(cell -> cell.rowSpan() >= 1 && cell.colSpan() >= 1));
        assertTrue(cells.stream().allMatch(cell -> cell.columnIndex() >= 0
                && cell.logicalColumnIndex() >= 0 && cell.logicalColSpan() >= 1));
        assertTrue(cells.stream().allMatch(cell -> cell.widthPx() > 0 && cell.heightPx() > 0));
        assertTrue(cells.stream().anyMatch(cell -> cell.rowSpan() > 1 || cell.colSpan() > 1));
        assertTrue(cells.stream().anyMatch(WordCell::bold));
        assertTrue(cells.stream().anyMatch(cell -> cell.topBorderStyle() != null
                || cell.bottomBorderStyle() != null || cell.leftBorderStyle() != null
                || cell.rightBorderStyle() != null));
        assertEquals(first.paragraphs().size(), first.diagnostics().paragraphCount());
        assertEquals(first.tables().size(), first.diagnostics().tableCount());
    }

    private void assertStructureEquals(WordParseResult expected, WordParseResult actual) {
        assertEquals(expected.paragraphs(), actual.paragraphs());
        assertEquals(expected.documentFrame(), actual.documentFrame());
        assertEquals(expected.tables(), actual.tables());
        assertEquals(expected.diagnostics().sourceHash(), actual.diagnostics().sourceHash());
        assertEquals(expected.diagnostics().extension(), actual.diagnostics().extension());
        assertEquals(expected.diagnostics().paragraphCount(), actual.diagnostics().paragraphCount());
        assertEquals(expected.diagnostics().tableCount(), actual.diagnostics().tableCount());
        assertEquals(expected.diagnostics().warningCodes(), actual.diagnostics().warningCodes());
        assertEquals(expected.diagnostics().failureCode(), actual.diagnostics().failureCode());
    }

    private WordParseCommand command(byte[] bytes, String extension, String fileName) {
        return new WordParseCommand(bytes, extension, fileName, WordParseProfile.STRUCTURAL_CANONICAL);
    }

    private Path reactorRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("pom.xml"))
                    && Files.isDirectory(current.resolve("yudao-module-mes"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Unable to locate IntRuoyiBackend reactor root");
    }
}
