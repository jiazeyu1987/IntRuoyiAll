package cn.iocoder.yudao.module.mes.service.pro.frontline;

import java.util.List;

public interface MesFrontlinePqcContextService {

    List<MesFrontlineActiveOrderCandidate> listActiveOrders();

    List<MesFrontlineRouteProcessCandidate> listProcessesByActiveOrder(Long workOrderId, Long routeId);

    List<MesFrontlineEmployeeCandidate> listPqcEmployeeCandidates();

    MesFrontlineRouteProcessCandidate requireActiveOrderProcess(Long workOrderId, Long routeId,
                                                                Long routeProcessId, Long processId);

    MesFrontlineEmployeeCandidate requirePqcEmployee(Long actualEmployeeId);

    MesFrontlineEmployeeSwitchResult switchPqcActualEmployee(Long loginUserId, Long workOrderId, Long routeId,
                                                             Long routeProcessId, Long processId,
                                                             Long actualEmployeeId);
}

