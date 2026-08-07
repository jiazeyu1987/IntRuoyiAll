package cn.iocoder.yudao.module.wordparser;

public final class WordParseException extends RuntimeException {

    private final WordParseFailureCode code;
    private final WordParseDiagnostics diagnostics;

    public WordParseException(WordParseFailureCode code, WordParseDiagnostics diagnostics) {
        super("Word parsing failed: " + code);
        this.code = code;
        this.diagnostics = diagnostics;
    }

    public WordParseFailureCode code() {
        return code;
    }

    public WordParseDiagnostics diagnostics() {
        return diagnostics;
    }
}
