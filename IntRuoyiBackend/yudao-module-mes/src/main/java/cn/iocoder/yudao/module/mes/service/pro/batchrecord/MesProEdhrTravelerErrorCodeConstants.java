package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

public interface MesProEdhrTravelerErrorCodeConstants {

    ErrorCode PRO_EDHR_TRAVELER_TEMPLATE_NOT_EXISTS =
            new ErrorCode(1_040_752_400, "eDHR 流转单模板不存在");
    ErrorCode PRO_EDHR_TRAVELER_TEMPLATE_CODE_DUPLICATE =
            new ErrorCode(1_040_752_401, "eDHR 流转单模板编码已存在");
    ErrorCode PRO_EDHR_TRAVELER_TEMPLATE_STATUS_INVALID =
            new ErrorCode(1_040_752_402, "eDHR 流转单模板状态不允许该操作");
    ErrorCode PRO_EDHR_TRAVELER_ACTIVE_TEMPLATE_EXISTS =
            new ErrorCode(1_040_752_403, "同一产品/路线/工序范围已存在启用流转单模板");
    ErrorCode PRO_EDHR_TRAVELER_BATCH_EXECUTION_NOT_EXISTS =
            new ErrorCode(1_040_752_404, "eDHR 流转单对应批次执行不存在");
    ErrorCode PRO_EDHR_TRAVELER_ROUTE_PROCESS_NOT_EXISTS =
            new ErrorCode(1_040_752_405, "eDHR 流转单对应路线工序不存在");
    ErrorCode PRO_EDHR_TRAVELER_ROUTE_PROCESS_MISMATCH =
            new ErrorCode(1_040_752_406, "eDHR 流转单路线工序不属于当前批次路线");
    ErrorCode PRO_EDHR_TRAVELER_PROCESS_NOT_EXISTS =
            new ErrorCode(1_040_752_407, "eDHR 流转单对应工序不存在");
    ErrorCode PRO_EDHR_TRAVELER_SN_NOT_EXISTS =
            new ErrorCode(1_040_752_408, "eDHR 流转单对应 SN 不存在");
    ErrorCode PRO_EDHR_TRAVELER_SN_MISMATCH =
            new ErrorCode(1_040_752_409, "eDHR 流转单 SN 与工单或批次不匹配");
    ErrorCode PRO_EDHR_TRAVELER_ALREADY_EXISTS =
            new ErrorCode(1_040_752_410, "同一业务对象已存在有效流转单：{}");
    ErrorCode PRO_EDHR_TRAVELER_NOT_EXISTS =
            new ErrorCode(1_040_752_411, "eDHR 流转单不存在");
    ErrorCode PRO_EDHR_TRAVELER_TEMPLATE_SCOPE_MISMATCH =
            new ErrorCode(1_040_752_412, "eDHR 流转单模板适用范围与批次/工序不匹配：{}");
}
