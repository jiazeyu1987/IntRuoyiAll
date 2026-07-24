package cn.iocoder.yudao.module.showroom.release;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

final class ShowroomReleaseHttpSupport {

    private static final DateTimeFormatter HTTP_DATE_FORMATTER =
            DateTimeFormatter.RFC_1123_DATE_TIME.withLocale(Locale.US).withZone(ZoneOffset.UTC);

    private ShowroomReleaseHttpSupport() {
    }

    static String toHttpDate(Instant instant) {
        return HTTP_DATE_FORMATTER.format(instant);
    }

    static Instant parseHttpDate(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return null;
        }
        return Instant.from(HTTP_DATE_FORMATTER.parse(headerValue));
    }

    static boolean matchesEtag(HttpHeaders headers, String etag) {
        String requestEtag = headers.getFirst(HttpHeaders.IF_NONE_MATCH);
        return requestEtag != null && requestEtag.equals(etag);
    }

    static boolean matchesLastModified(HttpHeaders headers, Instant lastModified) {
        Instant ifModifiedSince = parseHttpDate(headers.getFirst(HttpHeaders.IF_MODIFIED_SINCE));
        return ifModifiedSince != null && !ifModifiedSince.isBefore(lastModified);
    }

    static ResponseEntity<String> notModified(String etag, Instant lastModified, CacheControl cacheControl) {
        return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                .eTag(etag)
                .lastModified(lastModified.toEpochMilli())
                .cacheControl(cacheControl)
                .build();
    }

    static ResponseEntity<String> jsonOk(String body, String etag, Instant lastModified, CacheControl cacheControl) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .eTag(etag)
                .lastModified(lastModified.toEpochMilli())
                .cacheControl(cacheControl)
                .body(body);
    }

    static ResponseEntity<byte[]> binaryOk(byte[] body, String contentType, String etag, Instant lastModified) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(body.length)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .eTag(etag)
                .lastModified(lastModified.toEpochMilli())
                .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable())
                .body(body);
    }

    static ResponseEntity<String> error(ShowroomReleaseApiException exception) {
        String body = JsonUtils.toJsonString(new ErrorEnvelope(new ErrorBody(
                exception.getCode(), exception.getMessage(), exception.isRetryable(), exception.getDetails())));
        return ResponseEntity.status(exception.getStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    static long utf8Bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8).length;
    }

    record ErrorEnvelope(ErrorBody error) {
    }

    record ErrorBody(String code, String message, boolean retryable, java.util.Map<String, Object> details) {
    }
}
