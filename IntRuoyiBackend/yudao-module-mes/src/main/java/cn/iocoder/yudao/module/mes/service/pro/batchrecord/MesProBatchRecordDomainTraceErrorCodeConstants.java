package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

public interface MesProBatchRecordDomainTraceErrorCodeConstants {

    ErrorCode PRO_BATCH_RECORD_DOMAIN_TRACE_HASH_MISMATCH =
            new ErrorCode(1_040_750_250, "eDHR 主数据追溯 hash 与调用方期望不一致");
    ErrorCode PRO_BATCH_RECORD_DOMAIN_TRACE_BLOCKED =
            new ErrorCode(1_040_750_251, "eDHR 主数据追溯存在阻塞项");
    ErrorCode PRO_BATCH_RECORD_DOMAIN_TRACE_PERSIST_FAILED =
            new ErrorCode(1_040_750_252, "eDHR 主数据追溯快照保存失败");
}
