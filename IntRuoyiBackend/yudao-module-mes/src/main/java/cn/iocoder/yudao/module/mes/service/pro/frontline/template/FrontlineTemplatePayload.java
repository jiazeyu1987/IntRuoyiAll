package cn.iocoder.yudao.module.mes.service.pro.frontline.template;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record FrontlineTemplatePayload(Long workOrderId, Long routeId, Long processId, Long routeProcessId,
                                       Long actualEmployeeId, String templateCode,
                                       Map<String, Object> fieldValues) {

    public FrontlineTemplatePayload {
        Objects.requireNonNull(fieldValues, "fieldValues");
        fieldValues = Collections.unmodifiableMap(new LinkedHashMap<>(fieldValues));
    }
}
