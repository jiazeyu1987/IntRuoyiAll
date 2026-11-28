package cn.iocoder.yudao.module.erp.service.sync.runtime;

import lombok.Value;

@Value
public class ErpKingdeeSyncExecution {

    Long runId;
    ErpKingdeeSyncContext context;

    public ErpKingdeeSyncRunResult run(ErpKingdeeSyncTask task) {
        ErpKingdeeOrderSyncContext.setRunId(runId);
        try {
            return task.run(context);
        } finally {
            ErpKingdeeOrderSyncContext.clear();
        }
    }
}
