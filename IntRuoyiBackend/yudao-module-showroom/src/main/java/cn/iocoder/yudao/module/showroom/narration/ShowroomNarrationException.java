package cn.iocoder.yudao.module.showroom.narration;

public class ShowroomNarrationException extends RuntimeException {

    private final String code;

    public ShowroomNarrationException(String code, String message) {
        super(code + ": " + message);
        this.code = code;
    }

    public String code() {
        return code;
    }

}
