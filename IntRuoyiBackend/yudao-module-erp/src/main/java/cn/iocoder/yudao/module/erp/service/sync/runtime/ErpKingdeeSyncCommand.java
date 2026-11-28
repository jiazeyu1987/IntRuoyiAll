package cn.iocoder.yudao.module.erp.service.sync.runtime;

import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTriggerTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTypeEnum;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ErpKingdeeSyncCommand {

    private ErpKingdeeSyncTypeEnum syncType;
    private ErpKingdeeSyncTriggerTypeEnum triggerType;
    private String targetCode;
    /** The task commits each page and its progress together, using the run id from its context. */
    private boolean pageTransactions;
    private boolean forceInitialWindowStart;
    private LocalDateTime initialWindowStart;
    private LocalDateTime windowEnd;

}
