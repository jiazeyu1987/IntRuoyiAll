package cn.iocoder.yudao.module.dcc.service.upload;

import java.util.Arrays;

public enum DccUploadSizePolicyScopeType {

    GLOBAL(1),
    PURPOSE(2),
    CATEGORY(3),
    CATEGORY_PURPOSE(4);

    private final int priority;

    DccUploadSizePolicyScopeType(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }

    public static DccUploadSizePolicyScopeType of(String scopeType) {
        if (scopeType == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(item -> item.name().equals(scopeType.trim()))
                .findFirst()
                .orElse(null);
    }

}
