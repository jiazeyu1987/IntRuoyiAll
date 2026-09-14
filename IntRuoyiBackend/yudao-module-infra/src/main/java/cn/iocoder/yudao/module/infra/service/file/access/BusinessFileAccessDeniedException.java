package cn.iocoder.yudao.module.infra.service.file.access;

public final class BusinessFileAccessDeniedException extends RuntimeException {

    private final BusinessFileAccessOperation operation;
    private final Long fileId;
    private final String providerId;

    public BusinessFileAccessDeniedException(String message, BusinessFileAccessOperation operation,
                                             Long fileId, String providerId) {
        super(message);
        this.operation = operation;
        this.fileId = fileId;
        this.providerId = providerId;
    }

    public BusinessFileAccessDeniedException(String message, BusinessFileAccessOperation operation,
                                             Long fileId, String providerId, Throwable cause) {
        super(message, cause);
        this.operation = operation;
        this.fileId = fileId;
        this.providerId = providerId;
    }

    public BusinessFileAccessOperation getOperation() {
        return operation;
    }

    public Long getFileId() {
        return fileId;
    }

    public String getProviderId() {
        return providerId;
    }
}
