package cn.iocoder.yudao.module.mes.service.pro.processpool;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
public class MesProcessPoolSubmitEventCreateReqBO {

    private Long feedbackId;
    private String processPoolSubmissionIdempotencyKey;
    private Long recordbookEntryId;
    private Long recordbookEventId;
    private Long activeOrderId;
    private Long workOrderId;
    private Long taskId;
    private Long routeId;
    private Long routeProcessId;
    private Long processId;
    private Long workstationId;
    private Long deviceId;
    private Long deviceAccountUserId;
    private Long actualEmployeeId;
    private Long signatureEmployeeId;
    private Long signatureId;
    private String templateType;
    private BigDecimal outputQuantity;
    private BigDecimal lossQuantity;
    private Long lossReasonId;
    private String lossReasonCodeSnapshot;
    private String lossReasonNameSnapshot;
    private List<?> lossDetails;
    private Object selectedDevice;
    private List<?> selectedDevices;
    private List<?> deviceParameterReadings;
    private Map<String, Object> equipmentParameters;
    private Map<String, Object> rawPayload;
    private LocalDateTime submittedAt;

}
