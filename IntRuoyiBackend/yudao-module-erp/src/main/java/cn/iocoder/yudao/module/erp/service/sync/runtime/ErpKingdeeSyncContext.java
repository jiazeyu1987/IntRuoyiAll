package cn.iocoder.yudao.module.erp.service.sync.runtime;

import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTriggerTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTypeEnum;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class ErpKingdeeSyncContext {

    ErpKingdeeSyncTypeEnum syncType;
    ErpKingdeeSyncTriggerTypeEnum triggerType;
    boolean initialSync;
    LocalDateTime windowStart;
    LocalDateTime windowEnd;

}
