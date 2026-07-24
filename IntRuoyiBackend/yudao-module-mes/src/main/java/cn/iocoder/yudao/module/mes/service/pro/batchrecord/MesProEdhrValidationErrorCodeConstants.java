package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

public interface MesProEdhrValidationErrorCodeConstants {

    ErrorCode PRO_EDHR_VALIDATION_PACKAGE_NOT_EXISTS =
            new ErrorCode(1_040_750_800, "eDHR 验证包不存在");
    ErrorCode PRO_EDHR_VALIDATION_PACKAGE_CREATE_FAILED =
            new ErrorCode(1_040_750_801, "eDHR 验证包初始化失败，未生成完整条目和追溯关系");
    ErrorCode PRO_EDHR_VALIDATION_ITEM_NOT_EXISTS =
            new ErrorCode(1_040_750_802, "eDHR 验证条目不存在");
    ErrorCode PRO_EDHR_VALIDATION_ITEM_TYPE_INVALID =
            new ErrorCode(1_040_750_803, "eDHR 验证条目类型无效");
    ErrorCode PRO_EDHR_VALIDATION_TRACE_LINK_INVALID =
            new ErrorCode(1_040_750_804, "eDHR 追溯关系无效");
    ErrorCode PRO_EDHR_VALIDATION_TRACE_GATE_BLOCKED =
            new ErrorCode(1_040_750_805, "eDHR 追溯矩阵未完整，OQ Ready 不能放行");
    ErrorCode PRO_EDHR_OQ_PQ_PACKAGE_NOT_OQ_READY =
            new ErrorCode(1_040_750_806, "eDHR 验证包尚未 OQ Ready，不能创建 OQ/PQ 执行");
    ErrorCode PRO_EDHR_OQ_PQ_CASE_NOT_EXISTS =
            new ErrorCode(1_040_750_807, "eDHR OQ/PQ 用例不存在");
    ErrorCode PRO_EDHR_OQ_PQ_RUN_NOT_EXISTS =
            new ErrorCode(1_040_750_808, "eDHR OQ/PQ 执行记录不存在");
    ErrorCode PRO_EDHR_OQ_PQ_RUN_EVIDENCE_MISSING =
            new ErrorCode(1_040_750_809, "eDHR OQ/PQ 执行环境、版本或证据缺失");
    ErrorCode PRO_EDHR_OQ_PQ_PQ_REAL_DATA_REQUIRED =
            new ErrorCode(1_040_750_810, "eDHR PQ 必须记录真实业务路径、真实测试数据来源和目标环境证明");
    ErrorCode PRO_EDHR_OQ_PQ_DEVIATION_OPEN =
            new ErrorCode(1_040_750_811, "eDHR OQ/PQ 存在开放偏差，不能标记执行通过");
    ErrorCode PRO_EDHR_OQ_PQ_DEVIATION_CLOSE_REQUIRED =
            new ErrorCode(1_040_750_812, "eDHR 偏差关闭必须包含原因、整改、复测、复核和关闭签核");
}
