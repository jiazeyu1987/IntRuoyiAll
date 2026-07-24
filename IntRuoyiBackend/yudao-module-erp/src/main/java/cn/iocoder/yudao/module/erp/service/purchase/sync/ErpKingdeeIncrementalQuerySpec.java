package cn.iocoder.yudao.module.erp.service.purchase.sync;

import cn.hutool.core.util.StrUtil;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Value
@Builder
public class ErpKingdeeIncrementalQuerySpec {

    private static final String MODIFY_TIME_FIELD = "FModifyDate";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    String formId;
    String fieldKeys;
    String modifyTimeField;
    String baseFilter;
    int startRow;
    int limit;

    public Map<String, Object> toQuery(LocalDateTime windowStart, LocalDateTime windowEnd) {
        validate(windowStart, windowEnd);
        String effectiveModifyTimeField = getEffectiveModifyTimeField();
        String modifyTimeFilter = "(" + effectiveModifyTimeField + " >= '" + DATE_TIME_FORMATTER.format(windowStart)
                + "' and " + effectiveModifyTimeField + " < '" + DATE_TIME_FORMATTER.format(windowEnd) + "')";
        String filterString = StrUtil.isBlank(baseFilter) ? modifyTimeFilter : baseFilter + " and " + modifyTimeFilter;

        Map<String, Object> query = new LinkedHashMap<>();
        query.put("FormId", formId);
        query.put("FieldKeys", fieldKeys);
        query.put("FilterString", filterString);
        query.put("OrderString", effectiveModifyTimeField + " ASC");
        query.put("StartRow", startRow);
        query.put("Limit", limit);
        return query;
    }

    private void validate(LocalDateTime windowStart, LocalDateTime windowEnd) {
        if (StrUtil.isBlank(formId)) {
            throw new IllegalArgumentException("Kingdee incremental query FormId is blank");
        }
        String effectiveModifyTimeField = getEffectiveModifyTimeField();
        if (StrUtil.isBlank(fieldKeys) || !fieldKeys.contains(effectiveModifyTimeField)) {
            throw new IllegalArgumentException("Kingdee incremental query FieldKeys must contain "
                    + effectiveModifyTimeField);
        }
        if (windowStart == null) {
            throw new IllegalArgumentException("Kingdee incremental query windowStart is null");
        }
        if (windowEnd == null || !windowEnd.isAfter(windowStart)) {
            throw new IllegalArgumentException("Kingdee incremental query windowEnd must be after windowStart");
        }
        if (startRow < 0) {
            throw new IllegalArgumentException("Kingdee incremental query startRow must not be negative");
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("Kingdee incremental query limit must be positive");
        }
    }

    private String getEffectiveModifyTimeField() {
        return StrUtil.blankToDefault(modifyTimeField, MODIFY_TIME_FIELD);
    }

}
