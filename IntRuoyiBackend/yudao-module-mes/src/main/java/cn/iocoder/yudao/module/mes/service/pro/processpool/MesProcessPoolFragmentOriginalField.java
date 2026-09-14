package cn.iocoder.yudao.module.mes.service.pro.processpool;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MesProcessPoolFragmentOriginalField {

    OUTPUT_QUANTITY(true),
    LOSS_QUANTITY(false),
    DEVICE_PARAMETERS(false),
    QUALITY_STATUS(true),
    ALLOCATABLE_STATUS(true),
    REMARK(false);

    private final boolean allocationAffecting;

}
