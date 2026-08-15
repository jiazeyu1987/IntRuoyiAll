package cn.iocoder.yudao.module.mes.productionrelease.core;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

public interface MesReleaseFlowErrorCodeConstants {

    ErrorCode RELEASE_FLOW_BLOCKED = new ErrorCode(1_040_760_359, "生产放行前置条件不满足");
}
