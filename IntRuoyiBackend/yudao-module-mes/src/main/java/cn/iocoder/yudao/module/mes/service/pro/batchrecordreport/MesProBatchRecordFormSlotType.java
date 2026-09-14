package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import cn.hutool.core.util.StrUtil;

import java.util.Arrays;

public enum MesProBatchRecordFormSlotType {

    MAIN("MAIN", "主批记录"),
    FORM("FORM", "表单"),
    LOSS_REPORT("LOSS_REPORT", "损耗单"),
    PROCESS_INSPECTION("PROCESS_INSPECTION", "过程检验单"),
    PARAMETER_RECORD("PARAMETER_RECORD", "参数记录表");

    private final String type;
    private final String displayName;

    MesProBatchRecordFormSlotType(String type, String displayName) {
        this.type = type;
        this.displayName = displayName;
    }

    public String getType() {
        return type;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static String normalize(String value) {
        String trimmed = StrUtil.trim(value);
        if (StrUtil.isBlank(trimmed)) {
            return MAIN.type;
        }
        return Arrays.stream(values())
                .filter(slotType -> StrUtil.equalsIgnoreCase(slotType.type, trimmed))
                .findFirst()
                .map(MesProBatchRecordFormSlotType::getType)
                .orElse(null);
    }

    public static boolean isExtraSlot(String value) {
        String normalized = normalize(value);
        return normalized != null && !MAIN.type.equals(normalized);
    }

    public static String displayName(String value) {
        String normalized = normalize(value);
        return Arrays.stream(values())
                .filter(slotType -> StrUtil.equals(slotType.type, normalized))
                .findFirst()
                .map(MesProBatchRecordFormSlotType::getDisplayName)
                .orElse(value);
    }

}
