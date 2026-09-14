package cn.iocoder.yudao.module.erp.enums.nastablesync;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErpNasTableSyncTriggerTypeEnum {

    AUTO("AUTO"),
    MANUAL("MANUAL");

    private final String type;
}
