package cn.iocoder.yudao.module.dcc.service.file;

import java.time.LocalDate;

/** Explicit new-application intent. Historical null modes are not inferred or rewritten. */
public final class DccApplicationEffectiveSelection {
    public static final String AFTER_STANDARDIZATION_SIGNATURE = "AFTER_STANDARDIZATION_SIGNATURE";
    public static final String PRESET_DATE = "PRESET_DATE";
    public static final String DELAYED = "DELAYED";

    private DccApplicationEffectiveSelection() { }

    public static boolean isComplete(String mode, LocalDate date) {
        return AFTER_STANDARDIZATION_SIGNATURE.equals(mode) || DELAYED.equals(mode) ? date == null
                : PRESET_DATE.equals(mode) && date != null;
    }

    public static void requireSubmission(String mode, LocalDate date, LocalDate businessDate) {
        if (!isComplete(mode, date)) {
            throw new IllegalArgumentException("请选择有效生效方式；按日期生效须填写日期，标准化后生效不得填写预设日期");
        }
        if (businessDate == null) throw new IllegalArgumentException("文控业务日期未确定，无法校验生效选择");
        if (PRESET_DATE.equals(mode) && date.isBefore(businessDate)) {
            throw new IllegalArgumentException("预设生效日期不得早于当日");
        }
    }
}
