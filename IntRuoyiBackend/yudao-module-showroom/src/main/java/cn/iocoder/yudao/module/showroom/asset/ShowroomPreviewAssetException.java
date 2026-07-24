package cn.iocoder.yudao.module.showroom.asset;

public class ShowroomPreviewAssetException extends RuntimeException {

    private final String code;

    public ShowroomPreviewAssetException(String code, String message) {
        super(code + ": " + message);
        this.code = code;
    }

    public String code() {
        return code;
    }

}
