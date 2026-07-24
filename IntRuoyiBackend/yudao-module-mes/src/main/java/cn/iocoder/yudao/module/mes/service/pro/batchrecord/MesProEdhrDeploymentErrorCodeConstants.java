package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

public interface MesProEdhrDeploymentErrorCodeConstants {

    ErrorCode PRO_EDHR_DEPLOYMENT_PROJECT_NOT_EXISTS =
            new ErrorCode(1_040_750_850, "eDHR 交付项目不存在，不能登记部署证据");
    ErrorCode PRO_EDHR_DEPLOYMENT_EVIDENCE_NOT_EXISTS =
            new ErrorCode(1_040_750_851, "eDHR 部署授权接口证据不存在");
    ErrorCode PRO_EDHR_DEPLOYMENT_MANIFEST_REQUIRED =
            new ErrorCode(1_040_750_852, "eDHR 部署必须记录 manifest、schema、required SQL 和制品校验值");
    ErrorCode PRO_EDHR_DEPLOYMENT_LICENSE_REQUIRED =
            new ErrorCode(1_040_750_853, "eDHR 商业化交付必须记录授权许可范围、有效期、文件和校验结果");
    ErrorCode PRO_EDHR_DEPLOYMENT_INTERFACE_RESPONSE_REQUIRED =
            new ErrorCode(1_040_750_854, "eDHR 接口确认必须包含真实请求响应、失败整改和复测证据");
    ErrorCode PRO_EDHR_DEPLOYMENT_VERSION_INCONSISTENT =
            new ErrorCode(1_040_750_855, "eDHR 部署 releaseTag 或 schema 与交付项目不一致");
}

