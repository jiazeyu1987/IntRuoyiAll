package cn.iocoder.yudao.module.dcc.enums;

import java.util.Set;

public interface DccProductOnboardingStatusConstants {

    String PENDING_APPROVAL = "PENDING_APPROVAL";
    String APPROVED = "APPROVED";
    String REJECTED = "REJECTED";

    Set<String> ALL = Set.of(PENDING_APPROVAL, APPROVED, REJECTED);

    static boolean isValid(String status) {
        return ALL.contains(status);
    }
}
