package cn.iocoder.yudao.module.showroom.foundation.contract;

import java.util.Set;

public final class ShowroomV1ScopeContract {

    private static final Set<String> V1_ENTRYPOINTS = Set.of(
            "company",
            "product",
            "hall",
            "approval",
            "assignment",
            "product-comment",
            "narration",
            "preview-asset",
            "display");
    private static final Set<String> EXCLUDED_ENTRYPOINTS = Set.of("knowledge-base", "q-and-a", "knowledge-graph");

    private ShowroomV1ScopeContract() {
    }

    public static Set<String> v1Entrypoints() {
        return V1_ENTRYPOINTS;
    }

    public static Set<String> excludedEntrypoints() {
        return EXCLUDED_ENTRYPOINTS;
    }

}
