package cn.iocoder.yudao.module.mes.service.pro.frontline.template;

import java.util.Map;

public record FrontlineTemplatePayloadCommand(Long workOrderId, Long routeId, Long processId, Long routeProcessId,
                                              Long actualEmployeeId, String templateCode,
                                              Map<String, Object> fieldValues) {
}
