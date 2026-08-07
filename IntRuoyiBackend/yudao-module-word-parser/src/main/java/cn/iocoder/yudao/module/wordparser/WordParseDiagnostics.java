package cn.iocoder.yudao.module.wordparser;

import java.util.List;

public record WordParseDiagnostics(
        String parserVersion,
        String sourceHash,
        String extension,
        String fileNameHash,
        int paragraphCount,
        int tableCount,
        List<String> warningCodes,
        WordParseFailureCode failureCode) {

    public WordParseDiagnostics {
        warningCodes = List.copyOf(warningCodes);
    }
}
