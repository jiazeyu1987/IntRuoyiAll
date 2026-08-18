package cn.iocoder.yudao.module.mes.service.pro.frontline;

public record MesFrontlineEmployeeSwitchCommand(Long loginUserId,
                                                Long activeOrderId,
                                                Long routeId,
                                                Long routeProcessId,
                                                Long processId,
                                                Long actualEmployeeId) {
}
