package cn.iocoder.yudao.module.wordparser;

import java.util.List;

public record WordParseResult(
        List<String> paragraphs,
        WordDocumentFrame documentFrame,
        List<WordTable> tables,
        WordParseDiagnostics diagnostics) {

    public WordParseResult {
        paragraphs = List.copyOf(paragraphs);
        tables = List.copyOf(tables);
    }
}
