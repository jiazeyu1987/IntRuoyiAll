package cn.iocoder.yudao.module.erp.enums.nastablesync;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErpNasTableSyncRunStatusEnum {

    RUNNING("RUNNING"),
    SUCCESS("SUCCESS"),
    FAILED("FAILED");

    private final String status;
}
