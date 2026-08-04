package cn.iocoder.yudao.module.mes.service.pro.processpool.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesProcessPoolCreateEventReqDTO {

    private String eventType;
    private String eventIdempotencyKey;
    private Long workOrderId;
    private Long routeId;
    private Long routeProcessId;
    private Long processId;
    private Long actualEmployeeId;
    private Long deviceAccountId;
    private Long deviceId;
    private Long workstationId;
    private String templateType;
    private String feedbackSourceType;
    private Long feedbackSourceId;
    private Long recordbookEntryId;
    private String recordbookSourceType;
    private Long recordbookSourceId;
    private String rawPayload;
    private LocalDateTime clientSubmitTime;
    private Long signatureId;
    private Long signatureUserId;
    private String signatureSnapshot;
    private List<MesProcessPoolQuantityFragmentCreateDTO> quantityFragments;
}
