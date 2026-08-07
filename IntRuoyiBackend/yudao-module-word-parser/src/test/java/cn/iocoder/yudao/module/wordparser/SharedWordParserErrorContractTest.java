package cn.iocoder.yudao.module.wordparser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SharedWordParserErrorContractTest {

    private final SharedWordDocumentParser parser = new DefaultSharedWordDocumentParser();

    @Test
    void emptySource_failsFast() {
        assertFailure(new WordParseCommand(new byte[0], ".docx", "empty.docx",
                WordParseProfile.STRUCTURAL_CANONICAL), WordParseFailureCode.EMPTY_SOURCE);
    }

    @Test
    void unsupportedSourceType_failsFast() {
        assertFailure(new WordParseCommand(new byte[]{1, 2, 3}, ".txt", "unsupported.txt",
                WordParseProfile.STRUCTURAL_CANONICAL), WordParseFailureCode.UNSUPPORTED_SOURCE_TYPE);
    }

    @Test
    void corruptSource_failsFast() {
        assertFailure(new WordParseCommand(new byte[]{1, 2, 3}, ".docx", "corrupt.docx",
                WordParseProfile.STRUCTURAL_CANONICAL), WordParseFailureCode.CORRUPT_SOURCE);
        assertFailure(new WordParseCommand(new byte[]{1, 2, 3}, ".doc", "corrupt.doc",
                WordParseProfile.STRUCTURAL_CANONICAL), WordParseFailureCode.CORRUPT_SOURCE);
    }

    @Test
    void noParseableContent_failsFast() throws Exception {
        assertFailure(new WordParseCommand(SharedWordParserTestDocuments.emptyDocx(), ".docx", "empty.docx",
                WordParseProfile.STRUCTURAL_CANONICAL), WordParseFailureCode.NO_PARSEABLE_CONTENT);
    }

    @Test
    void invalidTableStructure_failsFast() throws Exception {
        assertFailure(new WordParseCommand(SharedWordParserTestDocuments.invalidTableDocx(), ".docx", "invalid.docx",
                WordParseProfile.STRUCTURAL_CANONICAL), WordParseFailureCode.INVALID_TABLE_STRUCTURE);
    }

    private void assertFailure(WordParseCommand command, WordParseFailureCode expectedCode) {
        WordParseException exception = assertThrows(WordParseException.class, () -> parser.parse(command));
        assertEquals(expectedCode, exception.code());
        assertEquals(expectedCode, exception.diagnostics().failureCode());
        assertEquals(command.extension(), exception.diagnostics().extension());
        assertEquals(64, exception.diagnostics().sourceHash().length());
        assertEquals(64, exception.diagnostics().fileNameHash().length());
        assertFalse(exception.getMessage().contains(command.originalFileName()));
        assertFalse(exception.diagnostics().toString().contains(command.originalFileName()));
        assertFalse(exception.getMessage().contains("Invalid span"));
        assertFalse(exception.diagnostics().toString().contains("Invalid span"));
    }
}
