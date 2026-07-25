package cn.iocoder.yudao.module.erp.service.sync.runtime;

import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTriggerTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTypeEnum;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ErpKingdeeSyncContext {

    private ErpKingdeeSyncTypeEnum syncType;
    private ErpKingdeeSyncTriggerTypeEnum triggerType;
    private boolean initialSync;
    private LocalDateTime windowStart;
    private LocalDateTime windowEnd;

}