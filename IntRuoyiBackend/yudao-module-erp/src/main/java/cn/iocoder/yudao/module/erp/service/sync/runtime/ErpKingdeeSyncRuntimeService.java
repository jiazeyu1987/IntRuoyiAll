package cn.iocoder.yudao.module.erp.service.sync.runtime;

public interface ErpKingdeeSyncRuntimeService {

    ErpKingdeeSyncRunResult executeSync(ErpKingdeeSyncCommand command, ErpKingdeeSyncTask task);

}
