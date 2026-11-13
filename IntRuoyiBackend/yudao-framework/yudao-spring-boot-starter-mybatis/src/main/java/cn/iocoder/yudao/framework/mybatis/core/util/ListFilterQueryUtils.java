package cn.iocoder.yudao.framework.mybatis.core.util;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

/**
 * Opt-in, database-side list column filtering. SQL expressions MUST be server-owned constants,
 * including aliases/correlated subqueries; never construct the whitelist from request keys.
 * All request values use MyBatis bound parameters. Expressions target the application's MySQL dialect.
 * Apply to the same wrapper used by the paginated select/count, after existing permission conditions.
 * XML callers pass {@link #wrapper} as {@code @Param(Constants.WRAPPER)} and append
 * {@code AND (${ew.sqlSegment})} inside their existing WHERE only when {@code !ew.emptyOfNormal}.
 * Do not pass this filter-only wrapper as a substitute for existing authorization predicates.
 */
public final class ListFilterQueryUtils {

    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    private ListFilterQueryUtils() {
    }

    public enum Type { TEXT, NUMBER, DATE, DATE_TIME, BOOLEAN }

    public record Field(String expression, Type type, String elementsQuery, String elementExpression) {
        public Field {
            if (expression == null || expression.isBlank() || type == null) {
                throw new IllegalArgumentException("List filter field requires a SQL expression and type");
            }
            if ((elementsQuery == null) != (elementExpression == null)) {
                throw new IllegalArgumentException("Collection filter requires both query and element expression");
            }
        }
    }

    public static Field text(String expression) { return field(expression, Type.TEXT); }
    public static Field number(String expression) { return field(expression, Type.NUMBER); }
    public static Field date(String expression) { return field(expression, Type.DATE); }
    public static Field dateTime(String expression) { return field(expression, Type.DATE_TIME); }
    public static Field bool(String expression) { return field(expression, Type.BOOLEAN); }

    /**
     * For array-valued columns: displayExpression produces the same space-joined text as the UI;
     * elementsQuery is a correlated "SELECT 1 FROM ... WHERE ..." without user input or trailing AND.
     * eq/in match individual elements; contains matches the joined display text.
     */
    public static Field textCollection(String displayExpression, String elementsQuery, String elementExpression) {
        return new Field(displayExpression, Type.TEXT, elementsQuery, elementExpression);
    }

    private static Field field(String expression, Type type) {
        return new Field(expression, type, null, null);
    }

    public static QueryWrapper<Object> wrapper(PageParam request, Map<String, Field> fields) {
        return apply(request, new QueryWrapper<>(), fields);
    }

    public static <T, W extends AbstractWrapper<T, ?, ?>> W apply(PageParam request, W wrapper,
                                                               Map<String, Field> fields) {
        if (request == null || request.getListFilters() == null || request.getListFilters().isBlank()) {
            return wrapper;
        }
        JsonNode filters = readFilters(request.getListFilters());
        // Validate all conditions before mutating a caller-owned wrapper.
        List<Predicate> predicates = new ArrayList<>();
        for (JsonNode filter : filters) {
            String key = filter.get("key").asText();
            Field field = fields.get(key);
            if (field == null) {
                throw invalidParamException("未配置筛选字段：{}", key);
            }
            predicates.add(predicate(field, filter));
        }
        for (Predicate predicate : predicates) {
            wrapper.apply("(" + predicate.sql + ")", predicate.values.toArray());
        }
        return wrapper;
    }

    /** Shared wire-format validation for SQL and explicitly computed-value filters. */
    static JsonNode readFilters(String json) {
        JsonNode filters;
        try {
            filters = JSON.readTree(json);
        } catch (JsonProcessingException exception) {
            throw invalidParamException("列表筛选条件必须是有效 JSON 数组");
        }
        if (filters == null || !filters.isArray()) {
            throw invalidParamException("列表筛选条件必须是 JSON 数组");
        }
        for (JsonNode filter : filters) {
            if (!filter.isObject() || !filter.path("key").isTextual()
                    || !filter.path("operator").isTextual() || !filter.has("value")) {
                throw invalidParamException("列表筛选条件缺少 key、operator 或 value");
            }
        }
        return filters;
    }

    private static Predicate predicate(Field field, JsonNode filter) {
        String operator = filter.get("operator").asText();
        JsonNode value = filter.get("value");
        String actual = normalized(field.expression, field.type);
        List<Object> values = new ArrayList<>();
        String sql;
        switch (operator) {
            case "contains":
                String expected = scalar(value).replace("!", "!!").replace("%", "!%")
                        .replace("_", "!_");
                sql = actual + " LIKE {0} ESCAPE '!'";
                values.add("%" + expected + "%");
                break;
            case "eq":
            case "in":
                List<JsonNode> options = new ArrayList<>();
                if ("in".equals(operator) && value.isArray()) {
                    value.forEach(options::add);
                } else {
                    options.add(value);
                }
                if (options.isEmpty()) {
                    throw invalidParamException("集合筛选至少需要一个值");
                }
                String equalActual = field.elementsQuery == null ? actual
                        : normalized(field.elementExpression, field.type);
                List<String> comparisons = new ArrayList<>();
                for (JsonNode option : options) {
                    comparisons.add(equalActual + " = {" + values.size() + "}");
                    values.add(scalar(option));
                }
                sql = "(" + String.join(" OR ", comparisons) + ")";
                if (field.elementsQuery != null) {
                    sql = "EXISTS (" + field.elementsQuery + " AND " + sql + ")";
                }
                break;
            case "between":
            case "gte":
            case "lte":
                if (field.type != Type.NUMBER && field.type != Type.DATE && field.type != Type.DATE_TIME) {
                    throw invalidParamException("该字段不支持范围筛选");
                }
                Object start = rangeValue(value, field.type, false);
                JsonNode endNode = filter.hasNonNull("valueEnd") ? filter.get("valueEnd") : value;
                Object end = rangeValue(endNode, field.type, true);
                sql = field.expression + " IS NOT NULL AND ";
                if ("gte".equals(operator)) {
                    sql += field.expression + " >= {0}";
                    values.add(start);
                } else if ("lte".equals(operator)) {
                    sql += field.expression + " <= {0}";
                    values.add(end);
                } else {
                    sql += field.expression + " >= {0} AND " + field.expression + " <= {1}";
                    values.add(start);
                    values.add(end);
                }
                break;
            default:
                throw invalidParamException("不支持的筛选运算符：{}", operator);
        }
        return new Predicate(sql, values);
    }

    private static String normalized(String expression, Type type) {
        if (type == Type.BOOLEAN) {
            return "(CASE WHEN (" + expression + ") IS NULL THEN '' WHEN (" + expression
                    + ") = 0 THEN 'false' ELSE 'true' END)";
        }
        if (type == Type.NUMBER) {
            String value = "CAST((" + expression + ") AS CHAR)";
            // JSON numbers lose DECIMAL scale in the browser (12.000000 becomes "12").
            // Only trim fractional zeros: integer 1200 must remain 1200.
            return "COALESCE(CASE WHEN (" + expression + ") = 0 THEN '0' WHEN LOCATE('.', "
                    + value + ") > 0 THEN TRIM(TRAILING '.' FROM TRIM(TRAILING '0' FROM "
                    + value + ")) ELSE " + value + " END, '')";
        }
        return "LOWER(TRIM(COALESCE(CAST((" + expression + ") AS CHAR), '')))";
    }

    static String scalar(JsonNode value) {
        if (value == null || value.isNull()) {
            return "";
        }
        if (!value.isValueNode()) {
            throw invalidParamException("筛选值必须是文本、数字或布尔值");
        }
        return value.asText().trim().toLowerCase(Locale.ROOT);
    }

    static Object rangeValue(JsonNode value, Type type, boolean endOfDay) {
        String text = scalar(value);
        if (text.isEmpty()) {
            throw invalidParamException("筛选范围必须为有效日期或数字");
        }
        try {
            if (type == Type.NUMBER) {
                return new BigDecimal(text);
            }
            if (text.matches("\\d{4}-\\d{2}-\\d{2}")) {
                LocalDate day = LocalDate.parse(text);
                if (type == Type.DATE) {
                    return day;
                }
                return endOfDay ? day.atTime(23, 59, 59, 999_000_000) : day.atStartOfDay();
            }
            if (value.isNumber()) {
                return LocalDateTime.ofInstant(Instant.ofEpochMilli(value.longValue()), ZoneOffset.UTC);
            }
            String dateTime = value.asText().trim().replace(' ', 'T');
            if (dateTime.endsWith("Z") || dateTime.matches(".*[+-]\\d{2}:\\d{2}$")) {
                return OffsetDateTime.parse(dateTime).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
            }
            return LocalDateTime.parse(dateTime);
        } catch (NumberFormatException | DateTimeParseException exception) {
            throw invalidParamException("筛选范围必须为有效日期或数字");
        }
    }

    private record Predicate(String sql, List<Object> values) {
    }
}
