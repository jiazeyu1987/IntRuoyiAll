package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.scheduleconfig.MesProRouteResourceCapacityPreviewRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.scheduleconfig.MesProRouteScheduleConfigSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.scheduleconfig.MesProRouteScheduleConfigRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteScheduleConfigDO;

import jakarta.validation.Valid;
import java.util.List;

public interface MesProRouteScheduleConfigService {

    Long saveConfig(@Valid MesProRouteScheduleConfigSaveReqVO reqVO);

    List<MesProRouteScheduleConfigDO> getConfigListByRouteVersionId(Long routeVersionId);

    List<MesProRouteScheduleConfigRespVO> getConfigRespListByRouteVersionId(Long routeVersionId);

    MesProRouteResourceCapacityPreviewRespVO getResourcePreview(Long routeProcessId);

    void validateNightShiftResources(Long routeProcessId, String capacityMode);

}
