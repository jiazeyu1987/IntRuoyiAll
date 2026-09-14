package cn.iocoder.yudao.module.mes.service.pro.frontline;

public interface MesFrontlineRuntimeConfigService {

    default MesFrontlineRuntimeConfig getRuntimeConfig(Long loginUserId, Long routeId, Long routeProcessId,
                                                       Long processId) {
        return getRuntimeConfig(loginUserId, null, routeId, routeProcessId, processId);
    }

    MesFrontlineRuntimeConfig getRuntimeConfig(Long loginUserId, Long activeOrderId, Long routeId,
                                               Long routeProcessId, Long processId);

}
