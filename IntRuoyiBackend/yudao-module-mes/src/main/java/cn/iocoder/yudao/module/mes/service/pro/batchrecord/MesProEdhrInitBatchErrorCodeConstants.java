package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

public interface MesProEdhrInitBatchErrorCodeConstants {

    ErrorCode PRO_EDHR_INIT_BATCH_NOT_EXISTS =
            new ErrorCode(1_040_750_437, "eDHR 初始化批次不存在");
    ErrorCode PRO_EDHR_INIT_BATCH_MANIFEST_INVALID =
            new ErrorCode(1_040_750_438, "eDHR 初始化 manifest 无效：{}");
}
