package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

public interface MesProEdhrPermissionErrorCodeConstants {

    ErrorCode PRO_EDHR_PERMISSION_CONTEXT_MISSING =
            new ErrorCode(1_040_750_433, "eDHR 对象级权限上下文缺失");
    ErrorCode PRO_EDHR_OBJECT_PERMISSION_DENIED =
            new ErrorCode(1_040_750_434, "eDHR 对象级权限不足：{}");
    ErrorCode PRO_EDHR_PERMISSION_SCOPE_REQUIRED =
            new ErrorCode(1_040_750_435, "eDHR 对象级权限范围不存在或未启用：{}");
    ErrorCode PRO_EDHR_PERMISSION_VERSION_CONFLICT =
            new ErrorCode(1_040_750_436, "eDHR 对象级权限版本冲突：当前版本 {}，提交版本 {}");
}
