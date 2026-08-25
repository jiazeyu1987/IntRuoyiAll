package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

/** Internal Flow 7 invocation boundary used by the persisted Flow 6 event. */
@FunctionalInterface
public interface MesProEdhrBatchTraceTxCInvoker {

    MesProEdhrBatchTraceTxCResult produce(MesProEdhrBatchTraceTxCCommand command);
}
