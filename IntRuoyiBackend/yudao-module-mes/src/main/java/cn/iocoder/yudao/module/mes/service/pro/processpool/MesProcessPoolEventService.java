package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.module.mes.service.pro.processpool.dto.MesProcessPoolCreateEventReqDTO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.dto.MesProcessPoolCreatePqcInspectionReqDTO;

public interface MesProcessPoolEventService {

    Long createEvent(MesProcessPoolCreateEventReqDTO reqDTO);

    Long createPqcInspectionEvent(MesProcessPoolCreatePqcInspectionReqDTO reqDTO);
}
