package cn.iocoder.yudao.module.mes.service.pro.frontline;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesFrontlinePqcSubmitCommand {

    private Long workOrderId;
    private Long routeId;
    private Long routeProcessId;
    private Long processId;
    private Long actualEmployeeId;
    private Long signatureId;
    private Long signatureEmployeeId;
    private String signatureSnapshot;
    private String templateType;
    private String inspectionResult;
    private Map<String, Object> rawPayload;
    private LocalDateTime clientSubmitTime;
}
