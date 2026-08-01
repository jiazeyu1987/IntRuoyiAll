package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderAllocationTraceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderBatchRecordTraceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderOrderProcessTraceRespVO;

public interface MesTeamLeaderTraceService {

    MesTeamLeaderAllocationTraceRespVO getAllocationTrace(Long eventId, Long workOrderId,
                                                          Long routeProcessId, Long processId);

    MesTeamLeaderOrderProcessTraceRespVO getOrderProcessTrace(Long workOrderId,
                                                              Long routeProcessId, Long processId);

    MesTeamLeaderBatchRecordTraceRespVO getBatchRecordTrace(Long workOrderId,
                                                            Long routeProcessId, Long processId);
}
