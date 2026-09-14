package cn.iocoder.yudao.module.mes.service.pro.frontline;

public record MesFrontlineTemplateDescriptor(String templateNo,
                                             String templateType,
                                             Long routeProcessId,
                                             Long processId,
                                             Long actualEmployeeId) {
}
