package cn.iocoder.yudao.module.mes.service.pro.frontline.template;

public record FrontlineTemplateResolveCommand(Long actualEmployeeId, Long routeProcessId, Long processId,
                                              String templateCode) {
}
