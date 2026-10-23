package cn.iocoder.yudao.module.dcc.enums;

import java.util.Set;

public interface DccProjectProductCreateStatusConstants {

    String PENDING_REVIEW = "PENDING_REVIEW";
    String PENDING_APPROVAL = "PENDING_APPROVAL";
    String REJECTED = "REJECTED";
    String WRITING = "WRITING";
    String COMPLETED = "COMPLETED";
    String WRITE_FAILED = "WRITE_FAILED";

    Set<String> ACTIVE_REQUEST_STATUSES = Set.of(PENDING_REVIEW, PENDING_APPROVAL, WRITING);
}
