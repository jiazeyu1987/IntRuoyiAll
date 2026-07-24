package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum MesProBatchRecordExecutionFieldAuditHashVerificationStatus {

    VALID,
    CHAIN_BROKEN,
    SIGNATURE_MISMATCH,
    SOURCE_MISSING,
    CONCURRENCY_CONFLICT;

    public static Set<String> names() {
        return Arrays.stream(values())
                .map(Enum::name)
                .collect(Collectors.toUnmodifiableSet());
    }
}
