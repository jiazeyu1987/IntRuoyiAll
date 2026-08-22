package cn.iocoder.yudao.module.erp.service.sync.runtime;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class ErpKingdeeSyncRuntimeServiceImpl implements ErpKingdeeSyncRuntimeService {

    private final ErpKingdeeSyncRuntimeTransactionService transactionService;

    @Override
    public ErpKingdeeSyncRunResult executeSync(ErpKingdeeSyncCommand command, ErpKingdeeSyncTask task) {
        ErpKingdeeSyncExecution execution = transactionService.start(command);
        try {
            ErpKingdeeSyncRunResult result = task.run(execution.getContext());
            transactionService.complete(execution, result);
            return result;
        } catch (RuntimeException failure) {
            transactionService.fail(execution, failure);
            throw failure;
        }
    }

}
