package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 生产组长活跃订单工序提交详情 Response VO")
@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderDetailRespVO {

    private Long activeOrderId;
    private Long workOrderId;
    private String workOrderCode;
    private String routeName;
    private List<ProcessDetail> processes;

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
        private List<InputMaterialDetail> inputMaterials;
        private List<SubmissionDetail> submissions;
        private List<PqcSubmissionDetail> pqcSubmissions;
    }

    @Data
    @Accessors(chain = true)
    public static class InputMaterialDetail {
        private Long materialId;
        private String materialCode;
        private String materialName;
        private String materialSpecification;
        private List<String> batchCodes;
        private BigDecimal requestedQuantity;
        private BigDecimal actualQuantity;
        private BigDecimal baseActualQuantity;
        private List<Long> sourcePickListIds;
        private List<String> sourcePickListNos;
        private List<Long> sourcePickListItemIds;
        private String sourceSnapshotHash;
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
        private List<SubmissionDeviceDetail> devices;
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
    public static class PqcSubmissionDetail {
        private Long pqcTaskId;
        private List<Long> pqcTaskIds;
        private Long submittedEventId;
        private List<Long> submittedEventIds;
        private Long qaProcessId;
        private String qaProcessCode;
        private String qaProcessName;
        private String inspectionType;
        private LocalDate businessDate;
        private String shiftCode;
        private Integer roundNo;
        private Integer actualInspectionQuantity;
        private String taskStatus;
        private List<PqcSubmissionItemDetail> items;
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
