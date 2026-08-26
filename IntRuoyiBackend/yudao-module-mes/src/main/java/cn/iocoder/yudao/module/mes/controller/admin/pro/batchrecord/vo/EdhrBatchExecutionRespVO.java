package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
public class EdhrBatchExecutionRespVO {

    private Long id;

    private String batchExecutionCode;

    private Long workOrderId;

    private String workOrderCode;

    private String batchCode;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer attemptNo;

    private Long sourceRejectedBatchExecutionId;

    private Long supersededByBatchExecutionId;

    private Long reexecutedByChangeEventId;

    private Long productId;

    private String productCode;

    private String productName;

    private Long routeId;

    private Long routeVersionId;

    private String routeVersionNo;

    private String routeCode;

    private String routeName;

    private Long currentProcessRouteProcessId;

    private String currentProcessCode;

    private String currentProcessName;

    private List<CurrentProcessFiller> currentProcessProductionFillers;

    private List<CurrentProcessFiller> currentProcessEquipmentFillers;

    private List<CurrentProcessFiller> currentProcessQualityFillers;

    private Integer status;

    private String provisioningStatus;

    private Integer taskTotal;

    private Integer taskApprovedCount;

    private Integer blockedCount;

    private String mainStage;

    private String mainStageLabel;

    private String stageOwnerRole;

    private List<String> stageBlockers;

    private Boolean canClose;

    private Boolean canArchive;

    private List<String> closeBlockers;

    private List<EdhrBatchExecutionTaskRespVO> tasks;

    private Boolean releaseActionLocked;

    private String releaseActionLockReason;

    private Long pendingVoidChangeEventId;

    private String pendingVoidChangeCode;

    private String pendingVoidChangeStatus;

    private String pendingVoidProcessInstanceId;

    private Long pendingVoidRequestedBy;

    private LocalDateTime pendingVoidRequestedAt;

    private Boolean canWithdrawVoidRequest;

    private Long closedBy;

    private LocalDateTime closedAt;

    private Long closeSignatureId;

    private Long rejectSignatureId;

    private Long rejectedBy;

    private LocalDateTime rejectedAt;

    private String rejectReason;

    private String aggregateHash;

    @Data
    @Accessors(chain = true)
    public static class CurrentProcessFiller {

        private Long userId;

        private String displayName;
    }
}
