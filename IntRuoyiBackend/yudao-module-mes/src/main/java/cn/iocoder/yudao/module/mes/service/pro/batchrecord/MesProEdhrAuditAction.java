package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum MesProEdhrAuditAction {

    CREATE,
    UPDATE,
    PRECHECK,
    IMPORT,
    SUBMIT,
    APPROVE,
    REJECT,
    WITHDRAW,
    VOID,
    REOPEN,
    PRINT,
    SIGNOFF,
    GATE_CHECK,
    EXPORT;

    public static Set<String> names() {
        return Arrays.stream(values())
                .map(Enum::name)
                .collect(Collectors.toUnmodifiableSet());
    }
}
