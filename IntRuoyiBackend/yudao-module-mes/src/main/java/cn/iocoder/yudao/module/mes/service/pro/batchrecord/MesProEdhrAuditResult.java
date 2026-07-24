package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum MesProEdhrAuditResult {

    SUCCESS,
    FAILED,
    BLOCKED;

    public static Set<String> names() {
        return Arrays.stream(values())
                .map(Enum::name)
                .collect(Collectors.toUnmodifiableSet());
    }
}
