package cn.iocoder.yudao.module.mes.service.pro.frontline;

public record MesFrontlinePqcEmployeeSwitchResult(Long loginUserId,
                                                  Long actualEmployeeId,
                                                  Long routeId,
                                                  Long dccProjectCodeId,
                                                  Long regulationVersionId,
                                                  Long qaProcessId,
                                                  boolean extraVerificationRequired,
                                                  MesFrontlinePqcTemplateDescriptor template) {
}
