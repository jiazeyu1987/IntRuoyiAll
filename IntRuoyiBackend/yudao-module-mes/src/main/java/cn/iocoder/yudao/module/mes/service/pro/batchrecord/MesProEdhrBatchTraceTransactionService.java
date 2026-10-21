package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/** Independent Tx-C attempts, including the separate failure-recording transaction. */
@Service
public class MesProEdhrBatchTraceTransactionService {
    private final TransactionTemplate transaction;

    public MesProEdhrBatchTraceTransactionService(PlatformTransactionManager manager) {
        transaction = new TransactionTemplate(manager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public <T> T execute(TransactionCallback<T> callback) {
        return transaction.execute(callback);
    }
}
