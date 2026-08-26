package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Objects;

/** Formal Flow 6 -> Flow 7 Tx-C boundary. */
@Service
public class MesProEdhrBatchTraceTxCApplicationService {

    private final MesProEdhrBatchTraceTxCInvoker producer;

    public MesProEdhrBatchTraceTxCApplicationService(MesProEdhrBatchTraceTxCInvoker producer) {
        this.producer = Objects.requireNonNull(producer, "producer");
    }

    public MesProEdhrBatchTraceTxCResult handle(MesProEdhrBatchProvisionedEvent event) {
        Objects.requireNonNull(event, "event");
        MesProEdhrBatchTraceTxCCommand command = event.toCommand();
        return TenantUtils.execute(event.getTenantId(), () -> producer.produce(command));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBatchProvisioned(MesProEdhrBatchProvisionedEvent event) {
        handle(event);
    }
}
