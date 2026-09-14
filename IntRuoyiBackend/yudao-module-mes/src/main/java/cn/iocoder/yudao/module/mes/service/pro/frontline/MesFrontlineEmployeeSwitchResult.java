package cn.iocoder.yudao.module.mes.service.pro.frontline;

public record MesFrontlineEmployeeSwitchResult(Long loginUserId,
                                               Long actualEmployeeId,
                                               Long routeId,
                                               Long routeProcessId,
                                               Long processId,
                                               boolean extraVerificationRequired,
                                               MesFrontlineTemplateDescriptor template) {
}
