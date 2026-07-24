package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

public interface MesProEdhrDeliveryErrorCodeConstants {

    ErrorCode PRO_EDHR_DELIVERY_PROJECT_NOT_EXISTS =
            new ErrorCode(1_040_750_700, "eDHR 交付项目不存在");
    ErrorCode PRO_EDHR_DELIVERY_PROJECT_CREATE_FAILED =
            new ErrorCode(1_040_750_701, "eDHR 交付项目初始化失败，未生成完整证据包和门禁项");
    ErrorCode PRO_EDHR_DELIVERY_GATE_ITEM_MISSING =
            new ErrorCode(1_040_750_702, "eDHR 交付门禁项缺失，不能判断签核状态");
}
