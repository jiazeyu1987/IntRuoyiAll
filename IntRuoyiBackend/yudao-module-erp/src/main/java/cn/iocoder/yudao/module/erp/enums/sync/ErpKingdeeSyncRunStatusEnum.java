package cn.iocoder.yudao.module.erp.enums.sync;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErpKingdeeSyncRunStatusEnum {

    RUNNING(10),
    SUCCESS(20),
    FAILED(30);

    private final Integer status;

}
