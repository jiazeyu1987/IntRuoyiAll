package cn.iocoder.yudao.module.mes.service.pro.frontline;

import java.util.List;
import java.util.Optional;

public interface MesFrontlinePqcContextService {

    List<MesFrontlineActiveOrderCandidate> listActiveOrders();

    List<MesFrontlineRouteProcessCandidate> listProcessesByActiveOrder(Long workOrderId, Long routeId);

    List<MesFrontlineEmployeeCandidate> listPqcEmployeeCandidates(Long loginUserId);

    MesFrontlineRouteProcessCandidate requireActiveOrderProcess(Long workOrderId, Long routeId,
                                                                Long routeProcessId, Long processId);

    MesFrontlineEmployeeCandidate requirePqcEmployee(Long loginUserId, Long actualEmployeeId);

    MesFrontlineEmployeeSwitchResult switchPqcActualEmployee(Long loginUserId, Long workOrderId, Long routeId,
                                                             Long routeProcessId, Long processId,
                                                             Long actualEmployeeId);

    Optional<MesFrontlinePqcSubmitResult> getSubmittedPqcInspection(Long loginUserId, Long pqcTaskId);

    MesFrontlinePqcSubmitResult submitPqcInspection(Long loginUserId, MesFrontlinePqcSubmitCommand command);
}
