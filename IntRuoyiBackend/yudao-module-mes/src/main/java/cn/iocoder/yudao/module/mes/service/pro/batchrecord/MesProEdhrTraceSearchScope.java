package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import java.util.Set;

/** Server-created request scope. Never accepted as an HTTP request field. */
public record MesProEdhrTraceSearchScope(Set<Long> visibleUserIds) {
    public MesProEdhrTraceSearchScope {
        visibleUserIds = Set.copyOf(visibleUserIds);
    }
    public static MesProEdhrTraceSearchScope empty() { return new MesProEdhrTraceSearchScope(Set.of()); }
}
