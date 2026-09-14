package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesFrontlinePqcProcessRespVO;

import java.util.List;
import java.util.Optional;

public interface MesFrontlinePqcContextService {

    List<MesFrontlineActiveOrderCandidate> listActiveOrders();

    List<MesFrontlinePqcProcessRespVO> listProcessesByActiveOrder(Long activeOrderId);

    List<MesFrontlinePqcProcessRespVO> listProcessesByActiveOrder(Long activeOrderId,
                                                                   Long loginUserId,
                                                                   Long actualEmployeeId);

    List<MesFrontlineEmployeeCandidate> listPqcEmployeeCandidates(Long loginUserId);

    MesFrontlineEmployeeCandidate requirePqcEmployee(Long loginUserId, Long actualEmployeeId);

    MesFrontlinePqcEmployeeSwitchResult switchPqcActualEmployee(Long loginUserId, Long activeOrderId,
                                                                Long regulationVersionId, Long qaProcessId,
                                                                Long pqcTaskId,
                                                                Long actualEmployeeId);

    Optional<MesFrontlinePqcSubmitResult> getSubmittedPqcInspection(Long loginUserId, Long pqcTaskId);

    MesFrontlinePqcSubmitResult submitPqcInspection(Long loginUserId, MesFrontlinePqcSubmitCommand command);
}
