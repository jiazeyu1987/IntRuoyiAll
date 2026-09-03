package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingDO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesFlow6CompletionBackfillReceipt;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/** Server-resolved source facts used by every Flow-6 batch entry. */
@Data
@Accessors(chain = true)
public class MesBatchExecutionAuthoritativeContext {

    private MesBatchExecutionProvisionCommand provisionCommand;
    private MesFlow6CompletionBackfillReceipt completionReceipt;
    private MesIndependentBatchPrerequisiteReceipt independentReceipt;
    /** Immutable Flow-1 snapshots read by Flow 6; never populated from the request. */
    private List<MesProcessPoolActiveOrderPickListBindingDO> pickListBindings;
}
