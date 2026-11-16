package cn.iocoder.yudao.module.erp.service.sync.admin;

import java.time.LocalDateTime;

public interface ErpKingdeeFullSyncHandler {
    String FULL_SYNC_JOB_PARAM = "FULL_SYNC";
    String WINDOW_SYNC_JOB_PREFIX = "FULL_SYNC_WINDOW|";

    String executeFullSync();
    String executeFullSync(LocalDateTime windowStart, LocalDateTime windowEnd);

    default String executeWindowSync(String param) {
        String[] bounds = param.substring(WINDOW_SYNC_JOB_PREFIX.length()).split("\\|", -1);
        return executeFullSync(LocalDateTime.parse(bounds[0]), LocalDateTime.parse(bounds[1]));
    }

    static String windowParam(LocalDateTime start, LocalDateTime end) {
        return WINDOW_SYNC_JOB_PREFIX + start + "|" + end;
    }
}
