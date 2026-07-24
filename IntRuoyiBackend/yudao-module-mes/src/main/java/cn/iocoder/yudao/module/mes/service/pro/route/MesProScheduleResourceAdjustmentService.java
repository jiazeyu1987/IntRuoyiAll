package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.resourceadjustment.MesProScheduleResourceAdjustmentSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProScheduleResourceAdjustmentDO;
import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.List;

public interface MesProScheduleResourceAdjustmentService {

    List<MesProScheduleResourceAdjustmentDO> getAdjustmentList(Long routeId, LocalDate calendarDate);

    void saveAdjustment(@Valid MesProScheduleResourceAdjustmentSaveReqVO saveReqVO);

}
