package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.module.mes.service.pro.processpool.dto.MesProcessPoolCreateEventReqDTO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.dto.MesProcessPoolCreatePqcInspectionReqDTO;

import java.util.Optional;

public interface MesProcessPoolEventService {

    Optional<MesProcessPoolSubmitEventResult> findExistingSubmitEvent(MesProcessPoolCreateEventReqDTO reqDTO);

    Optional<Long> findExistingPqcInspectionTaskId(MesProcessPoolCreatePqcInspectionReqDTO reqDTO);

    Long createEvent(MesProcessPoolCreateEventReqDTO reqDTO);

    Long createPqcInspectionEvent(MesProcessPoolCreatePqcInspectionReqDTO reqDTO);
}
