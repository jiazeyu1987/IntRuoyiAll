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
    private Integer version;
    private String udiControlDocumentNo;
    private Long workOrderId;
    private String workOrderCode;
    private String batchCode;
    private BigDecimal workOrderQuantity;
    private String demandBillNo;
    private String drawingNumber;
    private String productCode;
    private String productName;
    private String productSpecification;
    private String udiControlDocumentNo;
    private LocalDateTime workOrderCreateTime;
    private String routeName;
    private List<InputMaterialDetail> inputMaterialUsages = List.of();
    private List<ProcessDetail> processes = List.of();
    private ActiveOrderStatusSummary activeOrderStatus;
    private PqcProductionReleaseSummary pqcProductionRelease;
    private List<OperationFact> operationFacts = List.of();

    @Data
    @Accessors(chain = true)
    public static class ProcessDetail {
        private Long routeProcessId;
        private Long processId;
        private String processCode;
        private String processName;
        private Boolean keyFlag;
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
        private List<SourcePickListDocument> sourcePickListDocuments = List.of();
        private String sourceSnapshotHash;
    }

    @Data
    @Accessors(chain = true)
    public static class SourcePickListDocument {
        private Long id;
        private String billNo;
        private String documentStatus;
        private LocalDateTime billDate;
        private List<String> productionOrderNos = List.of();
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
        private SignatureDetail submitterSignature;
        private SignatureDetail reviewerSignature;
        private Boolean quantityConflict;
        private List<SubmissionDeviceDetail> devices = List.of();
        private List<SubmissionDeviceParameterDetail> deviceParameters = List.of();
        private List<ClearanceConfirmationDetail> clearanceConfirmations = List.of();
        private List<SubmissionMaterialDetail> materials = List.of();
    }

    @Data
    @Accessors(chain = true)
    public static class SubmissionDeviceDetail {
        private Long deviceId;
        private String deviceCode;
        private String deviceName;
        private Boolean inMeteringValidityPeriod;
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
        private List<SubmissionDeviceParameterDetail> deviceParameters = List.of();
        private List<ClearanceConfirmationDetail> clearanceConfirmations = List.of();
    }

    @Data
    @Accessors(chain = true)
    public static class SubmissionDeviceParameterDetail {
        private Long deviceId;
        private String deviceCode;
        private String deviceName;
        private String parameterCode;
        private String parameterName;
        private String unit;
        private BigDecimal value;
        private String textValue;
        private BigDecimal lowerLimit;
        private BigDecimal upperLimit;
        private String parameterStatus;
    }

    @Data
    @Accessors(chain = true)
    public static class ClearanceConfirmationDetail {
        private String key;
        private String label;
        private Boolean confirmed;
        private String description;
    }

    @Data
    @Accessors(chain = true)
    public static class PqcSubmissionDetail {
        private Long pqcTaskId;
        private List<Long> pqcTaskIds = List.of();
        private Long submittedEventId;
        private List<Long> submittedEventIds = List.of();
        private Long productionEventId;
        private List<Long> productionEventIds = List.of();
        private List<SignatureDetail> productionSubmitterSignatures = List.of();
        private Long qaProcessId;
        private String qaProcessCode;
        private String qaProcessName;
        private Integer qaProcessSort;
        private String qaItemCode;
        private String inspectionRuleKey;
        private String inspectionType;
        private LocalDate businessDate;
        private String shiftCode;
        private Integer roundNo;
        private Integer actualInspectionQuantity;
        private Integer scrapQuantity;
        private Integer submittedInspectionQuantity;
        private Integer submittedScrapQuantity;
        private String taskStatus;
        private String submitterName;
        private String reviewerName;
        private List<SignatureDetail> submitterSignatures = List.of();
        private List<SignatureDetail> reviewerSignatures = List.of();
        private List<PqcSubmissionItemDetail> submittedItems = List.of();
        private List<PqcSubmissionItemDetail> processInspectionItems = List.of();
    }

    @Data
    @Accessors(chain = true)
    public static class SignatureDetail {
        private Long signatureId;
        private String signerName;
        private LocalDateTime signedAt;
        private String role;
    }

    @Data
    @Accessors(chain = true)
    public static class PqcProductionReleaseSummary {
        private String status;
        private String statusLabel;
        private SignatureDetail signature;
    }

    @Data
    @Accessors(chain = true)
    public static class ActiveOrderStatusSummary {
        private String status;
        private String statusLabel;
    }

    @Data
    @Accessors(chain = true)
    public static class OperationFact {
        private Long id;
        private String operationType;
        private String operationName;
        private String sourceType;
        private String sourceId;
        private Long actorUserId;
        private String actorName;
        private Long signatureId;
        private String nonconformanceReason;
        private String reviewMaterialUrl;
        private Long reviewMaterialFileId;
        private String reviewMaterialsJson;
        private String reviewOpinion;
        private String disposition;
        private String qaSignature;
        private Long qaUserId;
        private String resultStatus;
        private LocalDateTime occurredAt;
        private String sourceSnapshotHash;
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
