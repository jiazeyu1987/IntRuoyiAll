package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.Getter;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public enum MesProEdhrCommonStatus {

    DRAFT(false, false),
    PRECHECK_FAILED(false, true),
    BLOCKED(false, true),
    PENDING_APPROVAL(false, false),
    COMPLETED(true, false),
    VOIDED(true, true);

    private final boolean terminal;
    private final boolean blocking;

    MesProEdhrCommonStatus(boolean terminal, boolean blocking) {
        this.terminal = terminal;
        this.blocking = blocking;
    }

    public boolean isTerminal() {
        return terminal;
    }

    public boolean isBlocking() {
        return blocking;
    }

    public static Set<String> names() {
        return Arrays.stream(values())
                .map(Enum::name)
                .collect(Collectors.toUnmodifiableSet());
    }
}
