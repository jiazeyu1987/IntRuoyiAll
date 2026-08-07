package cn.iocoder.yudao.module.wordparser;

import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SharedWordParserDiagnosticsTest {

    private final SharedWordDocumentParser parser = new DefaultSharedWordDocumentParser();

    @Test
    void diagnostics_areDeterministicAndContainOnlyApprovedSummaryFields() throws Exception {
        String originalFileName = "sensitive-production-record.docx";
        byte[] bytes = SharedWordParserTestDocuments.canonicalDocx();
        WordParseCommand command = new WordParseCommand(bytes, ".docx", originalFileName,
                WordParseProfile.STRUCTURAL_CANONICAL);

        WordParseDiagnostics first = parser.parse(command).diagnostics();
        WordParseDiagnostics second = parser.parse(command).diagnostics();

        assertEquals(first, second);
        assertEquals(Set.of("parserVersion", "sourceHash", "extension", "fileNameHash",
                        "paragraphCount", "tableCount", "warningCodes", "failureCode"),
                Arrays.stream(WordParseDiagnostics.class.getRecordComponents())
                        .map(RecordComponent::getName)
                        .collect(Collectors.toSet()));
        assertTrue(first.sourceHash().matches("[0-9a-f]{64}"));
        assertTrue(first.fileNameHash().matches("[0-9a-f]{64}"));
        assertEquals(".docx", first.extension());
        assertTrue(first.paragraphCount() > 0);
        assertTrue(first.tableCount() > 0);
        assertNull(first.failureCode());
        assertFalse(first.toString().contains(originalFileName));
        for (String sourceText : Set.of(
                SharedWordParserTestDocuments.OUTSIDE_PARAGRAPH,
                SharedWordParserTestDocuments.HEADER_TEXT,
                SharedWordParserTestDocuments.FOOTER_TEXT,
                "Merged heading",
                "Diagonal cell")) {
            assertFalse(first.toString().contains(sourceText));
        }
        assertFalse(first.toString().contains(java.util.Base64.getEncoder().encodeToString(bytes)));
    }
}
