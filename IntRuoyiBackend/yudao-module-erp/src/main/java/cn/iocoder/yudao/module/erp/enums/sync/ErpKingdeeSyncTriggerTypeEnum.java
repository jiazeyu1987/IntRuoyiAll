package cn.iocoder.yudao.module.erp.enums.sync;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErpKingdeeSyncTriggerTypeEnum {

    AUTO("AUTO"),
    MANUAL("MANUAL"),
    FULL("FULL");

    private final String triggerType;

}
