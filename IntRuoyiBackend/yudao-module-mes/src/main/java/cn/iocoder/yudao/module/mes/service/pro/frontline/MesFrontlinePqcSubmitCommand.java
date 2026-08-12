package cn.iocoder.yudao.module.mes.service.pro.frontline;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesFrontlinePqcSubmitCommand {

    private Long workOrderId;
    private Long activeOrderId;
    private Long pqcTaskId;
    private Long routeId;
    private Long regulationVersionId;
    private Long qaProcessId;
    private String inspectionType;
    private LocalDate businessDate;
    private String shiftCode;
    private Integer roundNo;
    private Integer actualInspectionQuantity;
    private Long actualEmployeeId;
    private Long deviceAccountId;
    private Long deviceId;
    private Long workstationId;
    private String pqcSubmissionIdempotencyKey;
    private String signaturePassword;
    private String templateType;
    private Integer scrapQuantity;
    private String nonconformanceDescription;
    private List<ItemResult> itemResults;
    private Map<String, Object> rawPayload;
    private LocalDateTime clientSubmitTime;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemResult {

        private String itemCode;
        private Long selectedEquipmentId;
        private String selectedEquipmentNumber;
        private List<String> sampleValues;
    }
}
