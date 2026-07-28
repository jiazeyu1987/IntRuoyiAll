package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import cn.iocoder.yudao.module.mes.service.pro.batchrecordcelllink.BatchRecordCellLinkAutoPersistResult;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProBatchRecordExecutionOpenOrCreateByContextRespVO {

    private Long id;

    private String executionCode;

    private Long templateId;

    private Long routeProcessId;

    private Long batchExecutionId;

    private Long routeId;

    private Long taskId;

    private Long workstationId;

    private String batchRecordReportId;

    private Long batchRecordDefinitionId;

    private Long batchRecordVersionId;

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

    private Integer status;

    private String activeContextKey;

    private Boolean created;

    private BatchRecordCellLinkAutoPersistResult cellLinkAutoPersist;
}
