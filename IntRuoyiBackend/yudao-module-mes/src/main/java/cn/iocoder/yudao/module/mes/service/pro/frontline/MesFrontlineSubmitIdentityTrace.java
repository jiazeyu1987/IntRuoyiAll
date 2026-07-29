package cn.iocoder.yudao.module.mes.service.pro.frontline;

public record MesFrontlineSubmitIdentityTrace(Long loginUserId,
                                             Long actualEmployeeId,
                                             Long signatureEmployeeId,
                                             Long deviceId,
                                             Long workstationId,
                                             Long routeId,
                                             Long routeProcessId,
                                             Long processId,
                                             String templateNo) {
}
