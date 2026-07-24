package cn.iocoder.yudao.framework.common.util.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import cn.iocoder.yudao.framework.common.util.json.databind.TimestampLocalDateTimeDeserializer;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JsonUtilsTest {

    @Test
    public void testToJsonString_ignoresNullProperties() {
        String json = JsonUtils.toJsonString(new NullFieldBean());

        assertTrue(json.contains("\"present\":\"value\""));
        assertFalse(json.contains("missing"));
    }

    @Test
    public void testParseObject_typeReference_returnsNullForEmptyText() {
        assertNull(JsonUtils.parseObject(null, new TypeReference<NullFieldBean>() {}));
        assertNull(JsonUtils.parseObject("", new TypeReference<NullFieldBean>() {}));
    }

    @Test
    public void testTimestampLocalDateTimeDeserializer_shouldParseFormattedDateTimeString() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(LocalDateTime.class, TimestampLocalDateTimeDeserializer.INSTANCE);
        objectMapper.registerModule(module);

        DateTimeBean bean = objectMapper.readValue("{\"startTime\":\"2026-07-06 00:00:00\"}", DateTimeBean.class);

        assertEquals(LocalDateTime.of(2026, 7, 6, 0, 0), bean.getStartTime());
    }

    private static class NullFieldBean {

        private final String present = "value";
        private final String missing = null;

        public String getPresent() {
            return present;
        }

        public String getMissing() {
            return missing;
        }

    }

    private static class DateTimeBean {

        private LocalDateTime startTime;

        public LocalDateTime getStartTime() {
            return startTime;
        }

        public void setStartTime(LocalDateTime startTime) {
            this.startTime = startTime;
        }

    }

}
