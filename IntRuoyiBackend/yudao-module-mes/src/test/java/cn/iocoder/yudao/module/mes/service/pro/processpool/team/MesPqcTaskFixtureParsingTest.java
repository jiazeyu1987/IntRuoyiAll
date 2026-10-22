package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Standalone offline regression: no Spring, Mapper, Docker or database initialization. */
public class MesPqcTaskFixtureParsingTest {
    public static void main(String[] args) throws Exception {
        ObjectMapper json = new ObjectMapper();
        int rejected = 0;
        for (String value : new String[]{"1.5", "9223372036854775808", "\"12\"", "true", "{}", "[]", "null", "0", "-1"}) {
            try {
                PqtFixtureValues.positiveId(json.readTree("{\"id\":" + value + "}"), "id");
                throw new AssertionError("Accepted invalid ID: " + value);
            } catch (IllegalArgumentException expected) { rejected++; }
        }
        for (String value : new String[]{"123", "true", "{}", "[]", "null"}) {
            try {
                PqtFixtureValues.text(json.readTree("{\"text\":" + value + "}"), "text");
                throw new AssertionError("Accepted invalid text: " + value);
            } catch (IllegalArgumentException expected) { rejected++; }
        }
        for (boolean id : new boolean[]{true, false}) {
            try {
                if (id) PqtFixtureValues.positiveId(json.readTree("{}"), "missing");
                else PqtFixtureValues.text(json.readTree("{}"), "missing");
                throw new AssertionError("Accepted missing field");
            } catch (IllegalArgumentException expected) { rejected++; }
        }
        if (PqtFixtureValues.positiveId(json.readTree("{\"id\":9223372036854775807}"), "id") != Long.MAX_VALUE)
            throw new AssertionError("Lost integer precision");
        for (String value : new String[]{"", " 冻结😀 "}) {
            if (!value.equals(PqtFixtureValues.text(json.createObjectNode().put("text", value), "text")))
                throw new AssertionError("Changed text");
        }
        System.out.println("PASS: " + rejected + " rejected malformed fields; exact max ID and empty/Unicode text preserved");
    }
}

/** Shared by the actual MySQL fixture consumer and the offline regression. */
final class PqtFixtureValues {
    private PqtFixtureValues() { }
    static long positiveId(JsonNode object, String key) {
        JsonNode value = object.get(key);
        if (value == null || !value.isIntegralNumber() || !value.canConvertToLong() || value.longValue() <= 0)
            throw new IllegalArgumentException("PREFLIGHT: positive exact long required: " + key);
        return value.longValue();
    }
    static String text(JsonNode object, String key) {
        JsonNode value = object.get(key);
        if (value == null || !value.isTextual())
            throw new IllegalArgumentException("PREFLIGHT: nonNULL text required: " + key);
        return value.textValue();
    }
}
