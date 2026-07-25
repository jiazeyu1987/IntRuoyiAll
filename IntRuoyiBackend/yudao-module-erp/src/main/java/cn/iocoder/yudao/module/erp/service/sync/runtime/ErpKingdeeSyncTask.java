package cn.iocoder.yudao.module.erp.service.sync.runtime;

@FunctionalInterface
public interface ErpKingdeeSyncTask {

    ErpKingdeeSyncRunResult run(ErpKingdeeSyncContext context);

}
