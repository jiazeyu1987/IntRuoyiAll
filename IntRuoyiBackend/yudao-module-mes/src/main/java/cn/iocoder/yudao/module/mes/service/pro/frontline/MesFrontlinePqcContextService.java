package cn.iocoder.yudao.module.mes.service.pro.frontline;

import java.util.List;
import java.util.Optional;

public interface MesFrontlinePqcContextService {

    List<MesFrontlineActiveOrderCandidate> listActiveOrders();

    List<MesFrontlinePqcProcessCandidate> listProcessesByActiveOrder(Long workOrderId, Long routeId);

    List<MesFrontlineEmployeeCandidate> listPqcEmployeeCandidates(Long loginUserId);

    MesFrontlinePqcProcessCandidate requireActiveOrderProcess(Long workOrderId, Long routeId,
                                                              Long regulationVersionId, Long qaProcessId);

    MesFrontlineEmployeeCandidate requirePqcEmployee(Long loginUserId, Long actualEmployeeId);

    MesFrontlinePqcEmployeeSwitchResult switchPqcActualEmployee(Long loginUserId, Long workOrderId, Long routeId,
                                                                Long regulationVersionId, Long qaProcessId,
                                                                Long actualEmployeeId);

    Optional<MesFrontlinePqcSubmitResult> getSubmittedPqcInspection(Long loginUserId, Long pqcTaskId);

    MesFrontlinePqcSubmitResult submitPqcInspection(Long loginUserId, MesFrontlinePqcSubmitCommand command);
}
