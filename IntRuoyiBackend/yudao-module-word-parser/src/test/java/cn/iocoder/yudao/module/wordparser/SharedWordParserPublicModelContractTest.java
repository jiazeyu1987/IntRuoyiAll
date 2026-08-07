package cn.iocoder.yudao.module.wordparser;

import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SharedWordParserPublicModelContractTest {

    @Test
    void publicModelsExposeOnlyApprovedBusinessNeutralCanonicalFields() {
        assertRecordComponents(WordCell.class, Set.of(
                "text", "rowSpan", "colSpan", "columnIndex", "logicalColumnIndex", "logicalColSpan",
                "bold", "fontSize", "horizontalAlign", "verticalAlign", "widthPx", "heightPx",
                "diagonalSlash", "topBorderStyle", "bottomBorderStyle", "leftBorderStyle",
                "rightBorderStyle", "backgroundColor"));
        assertRecordComponents(WordTable.class, Set.of(
                "sourceTopLevelTableIndex", "rowCount", "columnCount", "columnWidths", "rows"));
        assertRecordComponents(WordDocumentFrame.class, Set.of("headerRows", "footerRows"));
        assertRecordComponents(WordParseResult.class, Set.of(
                "paragraphs", "documentFrame", "tables", "diagnostics"));
        assertRecordComponents(WordParseCommand.class, Set.of(
                "source", "extension", "originalFileName", "profile"));
        assertArrayEquals(new WordParseProfile[]{WordParseProfile.STRUCTURAL_CANONICAL},
                WordParseProfile.values(), "canonical structure cannot be disabled through another profile");
        assertTrue(SharedWordDocumentParser.class.isInterface());
        assertEquals(WordParseResult.class,
                SharedWordDocumentParser.class.getDeclaredMethods()[0].getReturnType());
    }

    private void assertRecordComponents(Class<?> type, Set<String> expected) {
        assertTrue(type.isRecord(), () -> type.getSimpleName() + " must be an immutable record");
        Set<String> actual = Arrays.stream(type.getRecordComponents())
                .map(RecordComponent::getName)
                .collect(Collectors.toSet());
        assertEquals(expected, actual, () -> type.getSimpleName() + " has an unapproved business field");
    }
}
