package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProBatchRecordExecutionOpenOrCreateByContextReqVO {

    @NotNull(message = "workOrderId 不能为空")
    private Long workOrderId;

    private Long routeId;

    private Long batchExecutionId;

    private Long processId;

    private Long routeProcessId;

    private Long taskId;

    private Long workstationId;

    @NotNull(message = "batchRecordReportId 不能为空")
    private String batchRecordReportId;

    private String instanceScope;

    private String sharedFormKey;

    private String formSlotType;

    private String recordCategory;

    private String validationProfile;

    private Boolean recordbookEnabled;

    private Long permissionScopeId;

    private Long routeBindingId;

    private String routeBindingSnapshotHash;

    private String archiveVisibility;

    private String slotConfigSnapshotHash;

    private String batchCode;
}
