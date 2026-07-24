package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

public interface MesProEdhrContractErrorCodeConstants {

    ErrorCode PRO_EDHR_CONTRACT_IDEMPOTENCY_KEY_REQUIRED =
            new ErrorCode(1_040_750_500, "eDHR 公共契约幂等键不能为空：{}");
    ErrorCode PRO_EDHR_CONTRACT_IDEMPOTENCY_KEY_INVALID =
            new ErrorCode(1_040_750_501, "eDHR 公共契约幂等键格式非法：{}");
    ErrorCode PRO_EDHR_CONTRACT_EVIDENCE_HASH_INPUT_REQUIRED =
            new ErrorCode(1_040_750_502, "eDHR 公共契约证据 hash 输入不能为空：{}");
    ErrorCode PRO_EDHR_CONTRACT_AUDIT_EVENT_FIELD_REQUIRED =
            new ErrorCode(1_040_750_503, "eDHR 公共契约审计事件字段不能为空或非法：{}");
}
