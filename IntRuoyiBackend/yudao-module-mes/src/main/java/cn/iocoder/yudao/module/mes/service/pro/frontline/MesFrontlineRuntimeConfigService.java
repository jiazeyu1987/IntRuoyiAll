package cn.iocoder.yudao.module.mes.service.pro.frontline;

public interface MesFrontlineRuntimeConfigService {

    MesFrontlineRuntimeConfig getRuntimeConfig(Long loginUserId, Long activeOrderId, Long routeId,
                                               Long routeProcessId, Long processId);

}
