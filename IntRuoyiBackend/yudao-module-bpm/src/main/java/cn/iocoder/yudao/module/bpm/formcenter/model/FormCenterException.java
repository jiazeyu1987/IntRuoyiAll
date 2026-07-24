package cn.iocoder.yudao.module.bpm.formcenter.model;

public class FormCenterException extends RuntimeException {

    private final FormCenterErrorCode errorCode;

    public FormCenterException(FormCenterErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public FormCenterErrorCode getErrorCode() {
        return errorCode;
    }

}
