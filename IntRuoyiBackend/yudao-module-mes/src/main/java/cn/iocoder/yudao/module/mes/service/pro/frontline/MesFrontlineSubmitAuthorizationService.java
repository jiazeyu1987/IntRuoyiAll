package cn.iocoder.yudao.module.mes.service.pro.frontline;

public interface MesFrontlineSubmitAuthorizationService {

    MesFrontlineSubmitIdentityTrace authorize(MesFrontlineSubmitIdentityCommand command);

    void authorizeActiveOrder(Long loginUserId, Long workOrderId, Long routeId,
                              Long routeProcessId, Long processId);

}
