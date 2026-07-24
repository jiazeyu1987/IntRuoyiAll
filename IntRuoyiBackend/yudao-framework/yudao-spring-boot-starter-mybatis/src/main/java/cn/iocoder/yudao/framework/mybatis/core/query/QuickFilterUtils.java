package cn.iocoder.yudao.framework.mybatis.core.query;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.QuickFilter;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

public final class QuickFilterUtils {

    private static final String OPERATOR_CONTAINS = "contains";
    private static final String OPERATOR_EQ = "eq";
    private static final String OPERATOR_BETWEEN = "between";

    private QuickFilterUtils() {
    }

    public static <T> LambdaQueryWrapperX<T> filter(
            LambdaQueryWrapperX<T> wrapper,
            QuickFilter quickFilter,
            Map<String, QuickFilterField<T>> fields) {
        if (quickFilter == null || StrUtil.isBlank(quickFilter.getFieldKey())) {
            return wrapper;
        }
        QuickFilterField<T> field = fields.get(quickFilter.getFieldKey());
        if (field == null) {
            throw new IllegalArgumentException("非法快速过滤字段：" + quickFilter.getFieldKey());
        }
        String operator = StrUtil.trimToEmpty(quickFilter.getOperator());
        if (!field.supports(operator)) {
            throw new IllegalArgumentException("非法快速过滤条件：" + operator);
        }
        if (OPERATOR_CONTAINS.equals(operator)) {
            assertValuePresent(quickFilter.getValue(), quickFilter.getFieldKey());
            wrapper.like(field.column(), StrUtil.trim(quickFilter.getValue()));
            return wrapper;
        }
        if (OPERATOR_EQ.equals(operator)) {
            assertValuePresent(quickFilter.getValue(), quickFilter.getFieldKey());
            wrapper.eq(field.column(), field.convert(quickFilter.getValue()));
            return wrapper;
        }
        if (OPERATOR_BETWEEN.equals(operator)) {
            assertValuePresent(quickFilter.getValue(), quickFilter.getFieldKey());
            assertValuePresent(quickFilter.getValueEnd(), quickFilter.getFieldKey());
            wrapper.between(field.column(), field.convert(quickFilter.getValue()),
                    field.convertEnd(quickFilter.getValueEnd()));
            return wrapper;
        }
        throw new IllegalArgumentException("非法快速过滤条件：" + operator);
    }

    private static void assertValuePresent(String value, String fieldKey) {
        if (StrUtil.isBlank(value)) {
            throw new IllegalArgumentException("快速过滤值不能为空：" + fieldKey);
        }
    }

    public record QuickFilterField<T>(
            SFunction<T, ?> column,
            String type,
            String[] operators,
            QuickFilterValueConverter converter) {

        public static <T> QuickFilterField<T> text(SFunction<T, ?> column) {
            return new QuickFilterField<>(column, "text",
                    new String[]{OPERATOR_CONTAINS, OPERATOR_EQ}, value -> value);
        }

        public static <T> QuickFilterField<T> autocomplete(SFunction<T, ?> column) {
            return new QuickFilterField<>(column, "autocomplete",
                    new String[]{OPERATOR_CONTAINS, OPERATOR_EQ}, value -> value);
        }

        public static <T> QuickFilterField<T> select(SFunction<T, ?> column) {
            return new QuickFilterField<>(column, "select", new String[]{OPERATOR_EQ}, value -> value);
        }

        public static <T> QuickFilterField<T> integerSelect(SFunction<T, ?> column) {
            return new QuickFilterField<>(column, "select", new String[]{OPERATOR_EQ}, Integer::valueOf);
        }

        public static <T> QuickFilterField<T> longSelect(SFunction<T, ?> column) {
            return new QuickFilterField<>(column, "select", new String[]{OPERATOR_EQ}, Long::valueOf);
        }

        public static <T> QuickFilterField<T> dateRange(SFunction<T, ?> column) {
            return localDateRange(column);
        }

        public static <T> QuickFilterField<T> localDateRange(SFunction<T, ?> column) {
            return new QuickFilterField<>(column, "dateRange", new String[]{OPERATOR_BETWEEN}, LocalDate::parse);
        }

        public static <T> QuickFilterField<T> localDateTimeStartRange(SFunction<T, ?> column) {
            return localDateTimeRange(column);
        }

        public static <T> QuickFilterField<T> localDateTimeEndRange(SFunction<T, ?> column) {
            return localDateTimeRange(column);
        }

        public static <T> QuickFilterField<T> localDateTimeRange(SFunction<T, ?> column) {
            return new QuickFilterField<>(column, "dateTimeRange", new String[]{OPERATOR_BETWEEN},
                    value -> LocalDate.parse(value).atStartOfDay());
        }

        public Object convertEnd(String value) {
            if ("dateTimeRange".equals(type)) {
                try {
                    return LocalDate.parse(StrUtil.trim(value)).atTime(LocalTime.MAX);
                } catch (RuntimeException ex) {
                    throw new IllegalArgumentException("快速过滤值类型不匹配：" + value, ex);
                }
            }
            return convert(value);
        }

        public boolean supports(String operator) {
            if (StrUtil.isBlank(operator)) {
                return false;
            }
            for (String supported : operators) {
                if (supported.equals(operator)) {
                    return true;
                }
            }
            return false;
        }

        public Object convert(String value) {
            try {
                return converter.convert(StrUtil.trim(value));
            } catch (RuntimeException ex) {
                throw new IllegalArgumentException("快速过滤值类型不匹配：" + value, ex);
            }
        }
    }

    @FunctionalInterface
    public interface QuickFilterValueConverter {
        Object convert(String value);
    }

}
