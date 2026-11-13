package cn.iocoder.yudao.framework.mybatis.core.util;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.*;

class ListFilterQueryUtilsTest {

    private static final Map<String, Field> FIELDS = Map.of(
            "name", text("t.name"), "amount", number("t.amount"),
            "created", dateTime("t.create_time"), "enabled", bool("t.enabled"));

    @Test
    void filtersAreOptInAndPreserveExistingPermissions() {
        QueryWrapper<Object> query = new QueryWrapper<>().eq("tenant_id", 7);
        assertSame(query, apply(request(null), query, Map.of()));
        assertSame(query, apply(request("[]"), query, FIELDS));
        apply(request("[{\"key\":\"name\",\"operator\":\"eq\",\"value\":\"foo\"}]"), query, FIELDS);
        assertTrue(query.getSqlSegment().contains("tenant_id ="));
        assertTrue(query.getSqlSegment().contains(" AND "));
        assertTrue(query.getParamNameValuePairs().containsValue(7));
        assertTrue(query.getParamNameValuePairs().containsValue("foo"));
    }

    @Test
    void injectionAndLikeWildcardsRemainBoundLiteralValues() {
        QueryWrapper<Object> query = filter("name", "contains", "\" A%_!' OR 1=1 -- \"");
        String sql = query.getSqlSegment();
        assertFalse(sql.contains("OR 1=1"));
        assertTrue(sql.contains("ESCAPE '!'"));
        assertTrue(query.getParamNameValuePairs().containsValue("%a!%!_!!' or 1=1 --%"));
    }

    @Test
    void rejectsMalformedFieldsOperatorsAndComplexValues() {
        for (String invalid : new String[]{"{}", "null", "[] []", "{", "[{}]",
                "[{\"key\":\"name) OR 1=1\",\"operator\":\"eq\",\"value\":1}]",
                "[{\"key\":\"name\",\"operator\":\"raw\",\"value\":1}]",
                "[{\"key\":\"name\",\"operator\":\"eq\",\"value\":{}}]"}) {
            ServiceException exception = assertThrows(ServiceException.class, () -> wrapper(request(invalid), FIELDS));
            assertEquals(400, exception.getCode());
        }
    }

    @Test
    void validatesWholeRequestBeforeChangingCallerWrapper() {
        QueryWrapper<Object> query = new QueryWrapper<>();
        assertThrows(ServiceException.class, () -> apply(request("["
                + "{\"key\":\"name\",\"operator\":\"eq\",\"value\":\"valid\"},"
                + "{\"key\":\"unknown\",\"operator\":\"eq\",\"value\":\"bad\"}]"), query, FIELDS));
        assertTrue(query.isEmptyOfNormal());
    }

    @Test
    void normalizedEqualityPreservesNullEmptyAndBooleanValues() {
        QueryWrapper<Object> nullQuery = filter("name", "eq", "null");
        assertTrue(nullQuery.getSqlSegment().contains("COALESCE"));
        assertTrue(nullQuery.getParamNameValuePairs().containsValue(""));
        QueryWrapper<Object> query = filter("enabled", "in", "[false,true]");
        assertTrue(query.getSqlSegment().contains("CASE WHEN"));
        assertTrue(query.getSqlSegment().contains(" OR "));
        assertTrue(query.getParamNameValuePairs().containsValue("false"));
        assertTrue(query.getParamNameValuePairs().containsValue("true"));
    }

    @Test
    void dateRangeIncludesEntireFinalCalendarDay() {
        QueryWrapper<Object> query = wrapper(request("[{\"key\":\"created\",\"operator\":\"between\","
                + "\"value\":\"2026-11-01\",\"valueEnd\":\"2026-11-13\"}]"), FIELDS);
        query.getSqlSegment();
        assertTrue(query.getParamNameValuePairs().containsValue(LocalDateTime.of(2026, 11, 1, 0, 0)));
        assertTrue(query.getParamNameValuePairs().containsValue(LocalDateTime.of(2026, 11, 13, 23, 59, 59, 999000000)));
    }

    @Test
    void numericRangesUseNumbersAndRejectInvalidBounds() {
        QueryWrapper<Object> query = filter("amount", "gte", "\"12.50\"");
        query.getSqlSegment();
        assertTrue(query.getParamNameValuePairs().containsValue(new BigDecimal("12.50")));
        assertThrows(ServiceException.class, () -> filter("amount", "gte", "\"NaN\""));
        assertThrows(ServiceException.class, () -> filter("name", "gte", "5"));
        assertThrows(ServiceException.class, () -> filter("created", "between", "\"2026-02-30\""));
    }

    @Test
    void lteUsesExplicitEndValueLikeTheColumnFilter() {
        QueryWrapper<Object> query = wrapper(request("[{\"key\":\"amount\",\"operator\":\"lte\","
                + "\"value\":1,\"valueEnd\":5}]"), FIELDS);
        query.getSqlSegment();
        assertTrue(query.getParamNameValuePairs().containsValue(new BigDecimal("5")));
        assertFalse(query.getParamNameValuePairs().containsValue(new BigDecimal("1")));
    }

    @Test
    void collectionEqualityUsesCorrelatedElementsAndContainsUsesDisplayExpression() {
        Map<String, Field> fields = Map.of("names", textCollection("t.names", "SELECT 1 FROM child c WHERE c.parent_id=t.id", "c.name"));
        QueryWrapper<Object> eq = wrapper(request("[{\"key\":\"names\",\"operator\":\"in\",\"value\":[\"Alice\",\"Bob\"]}]"), fields);
        assertTrue(eq.getSqlSegment().contains("EXISTS (SELECT 1 FROM child c WHERE c.parent_id=t.id AND"));
        assertTrue(eq.getSqlSegment().contains("c.name"));
        assertFalse(eq.getSqlSegment().contains("t.names"));
        QueryWrapper<Object> contains = wrapper(request("[{\"key\":\"names\",\"operator\":\"contains\",\"value\":\"alice bob\"}]"), fields);
        assertTrue(contains.getSqlSegment().contains("t.names"));
        assertFalse(contains.getSqlSegment().contains("EXISTS"));
    }

    private static QueryWrapper<Object> filter(String key, String operator, String valueJson) {
        return wrapper(request("[{\"key\":\"" + key + "\",\"operator\":\"" + operator
                + "\",\"value\":" + valueJson + "}]"), FIELDS);
    }

    private static PageParam request(String filters) {
        PageParam request = new PageParam();
        request.setListFilters(filters);
        return request;
    }
}
