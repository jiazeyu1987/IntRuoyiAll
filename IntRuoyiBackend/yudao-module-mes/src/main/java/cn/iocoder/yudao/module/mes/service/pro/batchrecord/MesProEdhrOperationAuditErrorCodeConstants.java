package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

public interface MesProEdhrOperationAuditErrorCodeConstants {

    ErrorCode PRO_EDHR_OPERATION_AUDIT_CONTEXT_MISSING =
            new ErrorCode(1_040_750_430, "eDHR 操作审计上下文缺失");
    ErrorCode PRO_EDHR_OPERATION_AUDIT_WRITE_FAILED =
            new ErrorCode(1_040_750_431, "eDHR 操作审计写入失败：{}");
    ErrorCode PRO_EDHR_OPERATION_AUDIT_NOT_EXISTS =
            new ErrorCode(1_040_750_432, "eDHR 操作审计事件不存在");
}
