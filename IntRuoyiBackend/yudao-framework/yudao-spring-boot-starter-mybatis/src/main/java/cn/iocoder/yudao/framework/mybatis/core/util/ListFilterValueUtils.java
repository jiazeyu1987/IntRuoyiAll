package cn.iocoder.yudao.framework.mybatis.core.util;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;
import static cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.Type;

/**
 * Splits database predicates from explicitly declared live/computed display fields.
 * Callers must apply staticFilters to their authorized SQL query, evaluate live facts in bounded
 * windows, then retain only the requested page and a count. This utility never loads or pages data.
 */
public final class ListFilterValueUtils {
    private ListFilterValueUtils() { }

    public record Field<T>(Type type, Function<T, ?> getter) {
        public Field { Objects.requireNonNull(type); Objects.requireNonNull(getter); }
    }
    public static <T> Field<T> text(Function<T, ?> getter) { return new Field<>(Type.TEXT, getter); }
    public static <T> Field<T> number(Function<T, ?> getter) { return new Field<>(Type.NUMBER, getter); }
    public static <T> Field<T> date(Function<T, ?> getter) { return new Field<>(Type.DATE, getter); }
    public static <T> Field<T> dateTime(Function<T, ?> getter) { return new Field<>(Type.DATE_TIME, getter); }
    public static <T> Field<T> bool(Function<T, ?> getter) { return new Field<>(Type.BOOLEAN, getter); }

    public static final class Split<T> {
        private final String staticFilters;
        private final List<Predicate<T>> dynamicPredicates;
        private Split(String staticFilters, List<Predicate<T>> predicates) {
            this.staticFilters = staticFilters;
            this.dynamicPredicates = List.copyOf(predicates);
        }
        public String staticFilters() { return staticFilters; }
        public boolean hasDynamicFilters() { return !dynamicPredicates.isEmpty(); }
        public boolean matches(T row) {
            for (Predicate<T> predicate : dynamicPredicates) if (!predicate.test(row)) return false;
            return true;
        }
    }

    public static <T> Split<T> split(String json, Set<String> staticKeys, Map<String, Field<T>> dynamicFields) {
        Objects.requireNonNull(staticKeys);
        Objects.requireNonNull(dynamicFields);
        if (!Collections.disjoint(staticKeys, dynamicFields.keySet())) {
            throw new IllegalArgumentException("A list field must have exactly one filtering owner");
        }
        if (json == null || json.isBlank()) return new Split<>(null, List.of());
        JsonNode filters = ListFilterQueryUtils.readFilters(json);
        var staticFilters = JsonNodeFactory.instance.arrayNode();
        List<Predicate<T>> predicates = new ArrayList<>();
        for (JsonNode filter : filters) {
            String key = filter.get("key").asText();
            Field<T> field = dynamicFields.get(key);
            if (field != null) {
                // Use exactly the SQL filter's condition/value/type validation before compiling a getter.
                PageParam validation = new PageParam();
                validation.setListFilters(JsonNodeFactory.instance.arrayNode().add(filter).toString());
                ListFilterQueryUtils.wrapper(validation, Map.of(key,
                        new ListFilterQueryUtils.Field("live_value", field.type(), null, null)));
                predicates.add(predicate(field, filter));
            } else if (staticKeys.contains(key)) {
                if (!Set.of("contains", "eq", "in", "between", "gte", "lte").contains(filter.get("operator").asText())) {
                    throw invalidParamException("不支持的筛选运算符：{}", filter.get("operator").asText());
                }
                validateStaticValue(filter);
                staticFilters.add(filter);
            } else {
                throw invalidParamException("未配置筛选字段：{}", key);
            }
        }
        return new Split<>(staticFilters.isEmpty() ? null : staticFilters.toString(), predicates);
    }

    private static void validateStaticValue(JsonNode filter) {
        String operator = filter.get("operator").asText();
        JsonNode value = filter.get("value");
        if ("in".equals(operator) && value.isArray()) {
            if (value.isEmpty()) throw invalidParamException("集合筛选至少需要一个值");
            value.forEach(ListFilterQueryUtils::scalar);
        } else {
            String scalar = ListFilterQueryUtils.scalar(value);
            if (Set.of("between", "gte", "lte").contains(operator) && scalar.isEmpty()) {
                throw invalidParamException("筛选范围必须为有效日期或数字");
            }
        }
        if (filter.hasNonNull("valueEnd")) ListFilterQueryUtils.scalar(filter.get("valueEnd"));
    }

    private static <T> Predicate<T> predicate(Field<T> field, JsonNode filter) {
        String operator = filter.get("operator").asText();
        JsonNode expected = filter.get("value");
        if ("contains".equals(operator)) {
            String wanted = ListFilterQueryUtils.scalar(expected);
            return row -> textValue(field.getter().apply(row)).contains(wanted);
        }
        if ("eq".equals(operator)) {
            String wanted = ListFilterQueryUtils.scalar(expected);
            return row -> elements(field.getter().apply(row)).stream().anyMatch(value -> textValue(value).equals(wanted));
        }
        if ("in".equals(operator)) {
            List<String> wanted = new ArrayList<>();
            if (expected.isArray()) expected.forEach(value -> wanted.add(ListFilterQueryUtils.scalar(value)));
            else wanted.add(ListFilterQueryUtils.scalar(expected));
            return row -> elements(field.getter().apply(row)).stream().anyMatch(value -> wanted.contains(textValue(value)));
        }
        Object start = ListFilterQueryUtils.rangeValue(expected, field.type(), false);
        JsonNode endValue = filter.hasNonNull("valueEnd") ? filter.get("valueEnd") : expected;
        Object end = ListFilterQueryUtils.rangeValue(endValue, field.type(), true);
        BigDecimal minimum = comparable(start, field.type());
        BigDecimal maximum = comparable(end, field.type());
        return row -> {
            Object actual = field.getter().apply(row);
            if (actual == null || "".equals(actual)) return false;
            BigDecimal value = comparable(actual, field.type());
            if ("gte".equals(operator)) return value.compareTo(minimum) >= 0;
            if ("lte".equals(operator)) return value.compareTo(maximum) <= 0;
            return value.compareTo(minimum) >= 0 && value.compareTo(maximum) <= 0;
        };
    }

    /** Array eq/in compares elements; contains uses the frontend's recursively space-joined display. */
    private static List<?> elements(Object value) {
        if (value instanceof Collection<?> collection) return new ArrayList<>(collection);
        if (value != null && value.getClass().isArray()) {
            List<Object> values = new ArrayList<>();
            for (int index = 0; index < Array.getLength(value); index++) values.add(Array.get(value, index));
            return values;
        }
        return Collections.singletonList(value);
    }

    private static String textValue(Object value) {
        if (value == null) return "";
        if (value instanceof Map<?, ?> map) return join(map.values());
        if (value instanceof Collection<?> collection) return join(collection);
        if (value.getClass().isArray()) return join(elements(value));
        if (value instanceof Number number) {
            try { return new BigDecimal(number.toString()).stripTrailingZeros().toPlainString(); }
            catch (NumberFormatException exception) { throw invalidParamException("筛选字段必须为有效数字"); }
        }
        return String.valueOf(value).trim().toLowerCase(Locale.ROOT);
    }
    private static String join(Collection<?> values) {
        return values.stream().map(ListFilterValueUtils::textValue).collect(java.util.stream.Collectors.joining(" "));
    }

    private static BigDecimal comparable(Object value, Type type) {
        try {
            if (type == Type.NUMBER) return new BigDecimal(String.valueOf(value).trim());
            if (type == Type.DATE) {
                LocalDate day;
                if (value instanceof LocalDate date) day = date;
                else if (value instanceof LocalDateTime dateTime) day = dateTime.toLocalDate();
                else day = instantValue(value).atOffset(ZoneOffset.UTC).toLocalDate();
                return BigDecimal.valueOf(day.toEpochDay());
            }
            return BigDecimal.valueOf(instantValue(value).toEpochMilli());
        } catch (NumberFormatException | DateTimeException | ArithmeticException exception) {
            throw invalidParamException("筛选范围必须为有效日期或数字");
        }
    }
    private static Instant instantValue(Object value) {
        if (value instanceof Instant instant) return instant;
        if (value instanceof LocalDate day) return day.atStartOfDay().toInstant(ZoneOffset.UTC);
        if (value instanceof LocalDateTime time) return time.toInstant(ZoneOffset.UTC);
        if (value instanceof OffsetDateTime time) return time.toInstant();
        if (value instanceof ZonedDateTime time) return time.toInstant();
        if (value instanceof java.sql.Date day) return day.toLocalDate().atStartOfDay().toInstant(ZoneOffset.UTC);
        if (value instanceof java.util.Date date) return date.toInstant();
        if (value instanceof Number number) return Instant.ofEpochMilli(new BigDecimal(number.toString()).toBigInteger().longValueExact());
        String text = String.valueOf(value).trim().replace(' ', 'T');
        if (text.matches("\\d{4}-\\d{2}-\\d{2}")) return LocalDate.parse(text).atStartOfDay().toInstant(ZoneOffset.UTC);
        if (text.endsWith("Z") || text.matches(".*[+-]\\d{2}:\\d{2}$")) return OffsetDateTime.parse(text).toInstant();
        return LocalDateTime.parse(text).toInstant(ZoneOffset.UTC);
    }
}
