package cn.iocoder.yudao.framework.mybatis.core.util;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static cn.iocoder.yudao.framework.mybatis.core.util.ListFilterValueUtils.*;
import static org.junit.jupiter.api.Assertions.*;

class ListFilterValueUtilsTest {
    private static final Map<String, Field<Map<String,Object>>> FIELDS = Map.of(
            "display", text(row -> row.get("display")), "amount", number(row -> row.get("amount")),
            "time", dateTime(row -> row.get("time")), "day", date(row -> row.get("day")));
    private Split<Map<String,Object>> filters(String json) { return split(json, Set.of("owner"), FIELDS); }
    private String condition(String key, String operator, String values) {
        return "[{\"key\":\"" + key + "\",\"operator\":\"" + operator + "\",\"value\":" + values + "}]";
    }

    @Test void splitsStaticPredicatesAndAndsLiveDisplayPredicates() {
        var plan = filters("[{\"key\":\"owner\",\"operator\":\"eq\",\"value\":7},{\"key\":\"display\",\"operator\":\"contains\",\"value\":\"审批\"},{\"key\":\"amount\",\"operator\":\"gte\",\"value\":3}]");
        assertTrue(plan.hasDynamicFilters());
        assertTrue(plan.staticFilters().contains("owner"));
        assertFalse(plan.staticFilters().contains("display"));
        assertTrue(plan.matches(Map.of("display", " 待审批 ", "amount", 3)));
        assertFalse(plan.matches(Map.of("display", "已发布", "amount", 9)));
        assertFalse(plan.matches(Map.of("display", "待审批", "amount", 2)));
    }

    @Test void arrayEqualsAndInMatchAnElementWhileContainsJoinsVisibleValues() {
        assertTrue(filters(condition("display", "eq", "\"submit\"")).matches(Map.of("display", List.of(" VIEW ","Submit"))));
        assertTrue(filters(condition("display", "in", "[\"approve\",\"SUBMIT\"]")).matches(Map.of("display", new String[]{"view","submit"})));
        assertTrue(filters(condition("display", "contains", "\"view submit\"")).matches(Map.of("display", List.of("VIEW","submit"))));
        assertFalse(filters(condition("display", "eq", "\"view submit\"")).matches(Map.of("display", List.of("view","submit"))));
        assertFalse(filters(condition("display", "eq", "\"\"")).matches(Map.of("display", List.of())));
        assertTrue(filters(condition("display", "eq", "null")).matches(Collections.singletonMap("display",null)));
    }

    @Test void objectAndNestedArrayDisplayValuesFollowFrontendJoinOrder() {
        Map<String,Object> object = new LinkedHashMap<>();
        object.put("first", " Foo "); object.put("second", List.of("BAR", " Baz "));
        assertTrue(filters(condition("display", "eq", "\"foo bar baz\"")).matches(Map.of("display", object)));
    }

    @Test void numbersLoseSerializedDecimalScaleAndRangesIncludeBothBounds() {
        assertTrue(filters(condition("amount","eq","12")).matches(Map.of("amount", new BigDecimal("12.000"))));
        assertTrue(filters(condition("amount","eq","1")).matches(Map.of("amount", 1.0)));
        var plan = filters(condition("amount", "between", "2,\"valueEnd\":4"));
        assertTrue(plan.matches(Map.of("amount", 2))); assertTrue(plan.matches(Map.of("amount", "4")));
        assertFalse(plan.matches(Map.of("amount", 4.1)));
        assertFalse(plan.matches(Collections.singletonMap("amount",null)));
        assertFalse(plan.matches(Map.of("amount", "")));
    }

    @Test void dateTimeCalendarEndIncludesLastMillisecondAndOffsetInstants() {
        var plan = filters(condition("time", "between", "\"2026-11-13\",\"valueEnd\":\"2026-11-13\""));
        assertTrue(plan.matches(Map.of("time", LocalDateTime.of(2026,11,13,23,59,59,999_000_000))));
        assertFalse(plan.matches(Map.of("time", LocalDateTime.of(2026,11,14,0,0))));
        assertTrue(plan.matches(Map.of("time", "2026-11-14T01:00:00+02:00")));
        assertTrue(plan.matches(Map.of("time", Instant.parse("2026-11-13T12:00:00Z").toEpochMilli())));
        assertTrue(filters(condition("time","lte","\"2026-11-13\"")).matches(Map.of("time", "2026-11-13T23:59:59")));
    }

    @Test void datesAndNumberGteLteUseTheirExplicitFieldTypes() {
        assertTrue(filters(condition("day","eq","\"2026-11-13\"")).matches(Map.of("day", LocalDate.of(2026,11,13))));
        assertTrue(filters(condition("day","gte","\"2026-11-13\"")).matches(Map.of("day", LocalDate.of(2026,11,14))));
        assertFalse(filters(condition("amount","lte","2")).matches(Map.of("amount", 3)));
    }

    @Test void invalidWireFieldsOperatorsValuesAndRangesFailWith400BeforeRows() {
        for (String invalid : List.of("{}", "[] []", "[{}]",
                condition("unknown","eq","1"), condition("display","sql","1"),
                condition("display","eq","{}"), condition("display","in","[]"),
                condition("display","between","1"), condition("amount","gte","\"bad\""),
                condition("time","lte","\"2026-02-30\""), condition("owner","eq","{}"),
                condition("owner","in","[]"), condition("owner","raw","1"))) {
            assertEquals(400, assertThrows(ServiceException.class, () -> filters(invalid)).getCode(), invalid);
        }
    }

    @Test void invalidLiveRangeValuesAndGetterFailuresAreNotHidden() {
        var range = filters(condition("amount","gte","1"));
        assertEquals(400,assertThrows(ServiceException.class, () -> range.matches(Map.of("amount","not-number"))).getCode());
        var broken = ListFilterValueUtils.<String>split(condition("value","eq","1"),Set.of(),Map.of("value", text(row -> {throw new IllegalStateException("proof unavailable");})));
        assertThrows(IllegalStateException.class, () -> broken.matches("row"));
    }

    @Test void emptyAndStaticOnlyRequestsDoNotEvaluateLiveGetters() {
        assertNull(filters(null).staticFilters());
        assertFalse(filters("[]").hasDynamicFilters());
        var plan=filters(condition("owner","eq","7"));
        assertFalse(plan.hasDynamicFilters()); assertTrue(plan.matches(Map.of()));
        assertThrows(IllegalArgumentException.class, () -> split("[]",Set.of("display"),FIELDS));
    }
}
