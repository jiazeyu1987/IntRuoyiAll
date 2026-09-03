package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.Data;
import lombok.experimental.Accessors;

/** Immutable Flow-1 pick-list binding source carried by batch execution contracts. */
@Data
@Accessors(chain = true)
public class MesBatchExecutionPickListSource {

    private Long pickListBindingId;
    private Long pickListId;
    private Long bindingVersion;
    private String sourceSnapshotHash;
}
