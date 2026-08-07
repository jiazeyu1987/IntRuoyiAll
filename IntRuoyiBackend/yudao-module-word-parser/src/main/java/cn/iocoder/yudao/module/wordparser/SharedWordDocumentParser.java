package cn.iocoder.yudao.module.wordparser;

public interface SharedWordDocumentParser {

    WordParseResult parse(WordParseCommand command);
}
