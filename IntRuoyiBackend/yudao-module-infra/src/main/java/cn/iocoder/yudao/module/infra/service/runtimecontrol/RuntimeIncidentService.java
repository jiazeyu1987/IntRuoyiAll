package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlIncidentActionReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlIncidentCloseReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlIncidentCreateReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlIncidentPageReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlIncidentRespVO;

public interface RuntimeIncidentService {

    PageResult<RuntimeControlIncidentRespVO> getIncidentsPage(RuntimeControlIncidentPageReqVO pageReqVO);

    RuntimeControlIncidentRespVO createIncident(RuntimeControlIncidentCreateReqVO reqVO, String createdBy);

    RuntimeControlIncidentRespVO recordAction(Long id, RuntimeControlIncidentActionReqVO reqVO, String operator);

    RuntimeControlIncidentRespVO closeIncident(Long id, RuntimeControlIncidentCloseReqVO reqVO, String closedBy);
}
