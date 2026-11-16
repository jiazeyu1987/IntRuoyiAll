package cn.iocoder.yudao.module.erp.service.sync.runtime;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class ErpKingdeeSyncRuntimeServiceImpl implements ErpKingdeeSyncRuntimeService {

    private final ErpKingdeeSyncRuntimeTransactionService transactionService;
    private final ErpKingdeeSyncCancellation cancellation;

    @Override
    public ErpKingdeeSyncRunResult executeSync(ErpKingdeeSyncCommand command, ErpKingdeeSyncTask task) {
        String syncType = command.getSyncType().getType();
        cancellation.lock(syncType);
        ErpKingdeeSyncExecution execution = null;
        try {
            execution = transactionService.start(command);
            cancellation.register(execution.getRunId());
            try {
                ErpKingdeeSyncRunResult result = task.run(execution.getContext());
                cancellation.check(execution.getRunId());
                transactionService.complete(execution, result);
                return result;
            } catch (RuntimeException failure) {
                if (cancellation.isRequested(execution.getRunId())) {
                    Thread.interrupted();
                    java.util.concurrent.CancellationException stopped = new java.util.concurrent.CancellationException("同步任务已按删除请求停止");
                    stopped.initCause(failure);
                    transactionService.fail(execution, stopped);
                    throw stopped;
                }
                transactionService.fail(execution, failure);
                throw failure;
            }
        } finally {
            if (execution != null) cancellation.unregister(execution.getRunId());
            cancellation.unlock(syncType);
        }
    }
}
