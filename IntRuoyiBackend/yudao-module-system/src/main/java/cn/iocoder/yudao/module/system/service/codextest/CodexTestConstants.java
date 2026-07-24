package cn.iocoder.yudao.module.system.service.codextest;

import java.util.Set;

final class CodexTestConstants {

    static final String MODE_SEQUENTIAL = "SEQUENTIAL";
    static final String MODE_PARALLEL = "PARALLEL";

    static final String STATUS_ENABLE = "ENABLE";
    static final String STATUS_DISABLE = "DISABLE";

    static final String EXECUTION_PENDING = "PENDING";
    static final String EXECUTION_CLAIMED = "CLAIMED";
    static final String EXECUTION_RUNNING = "RUNNING";
    static final String EXECUTION_PASS = "PASS";
    static final String EXECUTION_FAIL = "FAIL";
    static final String EXECUTION_BLOCKED = "BLOCKED";
    static final String EXECUTION_CANCELED = "CANCELED";
    static final String EXECUTION_TIMEOUT = "TIMEOUT";

    static final String CHECKPOINT_NOT_RUN = "NOT_RUN";
    static final String CHECKPOINT_PASS = "PASS";
    static final String CHECKPOINT_FAIL = "FAIL";
    static final String CHECKPOINT_BLOCKED = "BLOCKED";

    static final String RUNNER_ONLINE = "ONLINE";

    static final Set<String> EXECUTION_MODES = Set.of(MODE_SEQUENTIAL, MODE_PARALLEL);
    static final Set<String> CASE_STATUSES = Set.of(STATUS_ENABLE, STATUS_DISABLE);
    static final Set<String> CHECKPOINT_RESULT_STATUSES = Set.of(CHECKPOINT_PASS, CHECKPOINT_FAIL, CHECKPOINT_BLOCKED);
    static final Set<String> COMPLETE_CASE_STATUSES = Set.of(EXECUTION_PASS, EXECUTION_FAIL, EXECUTION_BLOCKED, EXECUTION_TIMEOUT);

    private CodexTestConstants() {
    }

}
