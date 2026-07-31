package cn.iocoder.yudao.module.erp.service.sync.runtime;

import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTriggerTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTypeEnum;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ErpKingdeeSyncContext {

    private ErpKingdeeSyncTypeEnum syncType;

    private ErpKingdeeSyncTriggerTypeEnum triggerType;

    private boolean initialSync;

    private LocalDateTime windowStart;

    private LocalDateTime windowEnd;

}