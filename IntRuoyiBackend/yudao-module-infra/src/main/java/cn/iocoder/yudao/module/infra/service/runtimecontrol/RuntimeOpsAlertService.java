package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlAlertCreateReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlAlertPageReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlAlertRespVO;

public interface RuntimeOpsAlertService {

    PageResult<RuntimeControlAlertRespVO> getAlertsPage(RuntimeControlAlertPageReqVO pageReqVO);

    RuntimeControlAlertRespVO createAlert(RuntimeControlAlertCreateReqVO reqVO);

    RuntimeControlAlertRespVO resendSiteMessage(Long id);

    RuntimeControlAlertRespVO acknowledge(Long id, String acknowledgedBy);
}
