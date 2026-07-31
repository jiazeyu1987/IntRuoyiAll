package cn.iocoder.yudao.module.mes.service.pro.frontline;

public record MesFrontlineTemplateRequest(Long loginUserId,
                                          Long actualEmployeeId,
                                          Long routeId,
                                          Long routeProcessId,
                                          Long processId) {
}
