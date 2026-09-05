package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderDetail {

    private Long activeOrderId;
    private Long workOrderId;
    private String workOrderCode;
    private String routeName;
    private List<ProcessDetail> processes = List.of();

    @Data
    @Accessors(chain = true)
    public static class ProcessDetail {
        private Long routeProcessId;
        private Long processId;
        private String processCode;
        private String processName;
        private BigDecimal requiredQuantity;
        private BigDecimal submittedQuantity;
        private Integer submissionCount;
        private Boolean quantityConflict;
        private BigDecimal overageQuantity;
        private List<InputMaterialDetail> inputMaterials = List.of();
        private List<SupplementMaterialDetail> supplementMaterials = List.of();
        private List<SubmissionDetail> submissions = List.of();
        private List<PqcSubmissionDetail> pqcSubmissions = List.of();
    }

    @Data
    @Accessors(chain = true)
    public static class InputMaterialDetail {
        private Long materialId;
        private String materialCode;
        private String materialName;
        private String materialSpecification;
        private List<String> batchCodes = List.of();
        private BigDecimal requestedQuantity;
        private BigDecimal actualQuantity;
        private BigDecimal baseActualQuantity;
        private List<Long> sourcePickListIds = List.of();
        private List<String> sourcePickListNos = List.of();
        private List<Long> sourcePickListItemIds = List.of();
        private String sourceSnapshotHash;
    }

    @Data
    @Accessors(chain = true)
    public static class SupplementMaterialDetail {
        private Long materialId;
        private String materialCode;
        private String materialName;
        private String materialSpecification;
        private List<String> batchCodes = List.of();
        private BigDecimal requestedQuantity;
        private BigDecimal actualQuantity;
        private BigDecimal baseActualQuantity;
        private List<Long> sourceReplenishmentListIds = List.of();
        private List<String> sourceReplenishmentListNos = List.of();
        private List<Long> sourceReplenishmentListItemIds = List.of();
    }

    @Data
    @Accessors(chain = true)
    public static class SubmissionDetail {
        private Long eventId;
        private BigDecimal submittedQuantity;
        private String submitterName;
        private String reviewerName;
        private LocalDateTime submittedAt;
        private Boolean quantityConflict;
        private List<SubmissionDeviceDetail> devices = List.of();
        private List<SubmissionMaterialDetail> materials = List.of();
    }

    @Data
    @Accessors(chain = true)
    public static class SubmissionDeviceDetail {
        private Long deviceId;
        private String deviceCode;
        private String deviceName;
    }

    @Data
    @Accessors(chain = true)
    public static class SubmissionMaterialDetail {
        private Long materialId;
        private String materialCode;
        private String materialName;
        private String materialSpecification;
        private BigDecimal outputQuantity;
        private BigDecimal lossQuantity;
        private List<SubmissionDeviceDetail> devices = List.of();
    }

    @Data
    @Accessors(chain = true)
    public static class PqcSubmissionDetail {
        private Long pqcTaskId;
        private List<Long> pqcTaskIds = List.of();
        private Long submittedEventId;
        private List<Long> submittedEventIds = List.of();
        private Long qaProcessId;
        private String qaProcessCode;
        private String qaProcessName;
        private String inspectionType;
        private LocalDate businessDate;
        private String shiftCode;
        private Integer roundNo;
        private Integer actualInspectionQuantity;
        private String taskStatus;
        private List<PqcSubmissionItemDetail> items = List.of();
    }

    @Data
    @Accessors(chain = true)
    public static class PqcSubmissionItemDetail {
        private Long aggregateDetailId;
        private Integer sampleNo;
        private String itemCode;
        private String itemName;
        private String inspectionMethod;
        private String standardText;
        private String measuredValue;
        private String itemResult;
        private String judgement;
        private String selectedEquipmentName;
        private String selectedEquipmentNumber;
    }
}
