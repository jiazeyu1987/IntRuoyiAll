package cn.iocoder.yudao.framework.apilog.core.filter;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ApiAccessLogCredentialSanitizationTest {
    @Test void signingAndPasswordChangeCredentialsAreRemovedRecursively() {
        String raw = "{\"activeOrderId\":10,\"signaturePassword\":\"secret-sign\",\"items\":[{\"currentPassword\":\"secret-old\",\"newPassword\":\"secret-new\",\"action\":\"签署\"}]}";
        String result = ReflectionTestUtils.invokeMethod(ApiAccessLogFilter.class, "sanitizeJson", raw, (String[]) null);
        assertFalse(result.contains("secret"));
        var json = JsonUtils.parseTree(result);
        assertEquals(10, json.path("activeOrderId").asInt());
        assertEquals("签署", json.path("items").get(0).path("action").asText());
    }

    @Test void malformedPayloadNeverReturnsUnredactedText() {
        var root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        var output = new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
        output.start(); root.addAppender(output);
        try {
            String result = ReflectionTestUtils.invokeMethod(ApiAccessLogFilter.class, "sanitizeJson",
                    "{\"signaturePassword\":\"secret-sign\"", (String[]) null);
            assertFalse(result.contains("secret-sign"));
            assertTrue(result.contains("脱敏失败"));
            assertTrue(output.list.stream().noneMatch(e -> e.getFormattedMessage().contains("secret-sign")));
        } finally { root.detachAppender(output); output.stop(); }
    }

    @Test void queryCredentialsAreRemovedWhileActorAndOrderRemain() {
        var query = new LinkedHashMap<>(Map.of("signaturePassword", "secret-sign", "currentPassword", "secret-old", "activeOrderId", "10"));
        String result = ReflectionTestUtils.invokeMethod(ApiAccessLogFilter.class, "sanitizeMap", query, (String[]) null);
        assertFalse(result.contains("secret"));
        assertTrue(result.contains("activeOrderId"));
    }
}
