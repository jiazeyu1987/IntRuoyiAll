package cn.iocoder.yudao.module.showroom.release;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class ShowroomReleaseApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final boolean retryable;
    private final Map<String, Object> details;

    public ShowroomReleaseApiException(HttpStatus status, String code, String message, boolean retryable,
                                       Map<String, Object> details) {
        super(message);
        this.status = status;
        this.code = code;
        this.retryable = retryable;
        this.details = details == null ? Map.of() : Map.copyOf(details);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
