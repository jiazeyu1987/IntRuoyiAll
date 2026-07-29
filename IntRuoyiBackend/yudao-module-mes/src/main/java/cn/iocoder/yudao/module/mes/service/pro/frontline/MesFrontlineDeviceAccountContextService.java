package cn.iocoder.yudao.module.mes.service.pro.frontline;

import java.util.List;

public interface MesFrontlineDeviceAccountContextService {

    List<MesFrontlineRouteProcessCandidate> listSwitchableProcesses(Long loginUserId);

    List<MesFrontlineEmployeeCandidate> listEmployeeCandidates(Long loginUserId, Long routeId,
                                                               Long routeProcessId, Long processId);

    MesFrontlineRouteProcessCandidate requireAuthorizedProcess(Long loginUserId, Long routeId,
                                                               Long routeProcessId, Long processId);

    MesFrontlineEmployeeCandidate requireBoundEmployee(Long loginUserId, Long routeId, Long routeProcessId,
                                                       Long processId, Long actualEmployeeId);

}
