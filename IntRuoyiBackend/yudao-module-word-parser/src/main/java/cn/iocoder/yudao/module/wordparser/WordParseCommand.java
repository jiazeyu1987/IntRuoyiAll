package cn.iocoder.yudao.module.wordparser;

public record WordParseCommand(byte[] source, String extension, String originalFileName, WordParseProfile profile) {

    public WordParseCommand {
        source = source == null ? null : source.clone();
    }

    @Override
    public byte[] source() {
        return source == null ? null : source.clone();
    }
}
