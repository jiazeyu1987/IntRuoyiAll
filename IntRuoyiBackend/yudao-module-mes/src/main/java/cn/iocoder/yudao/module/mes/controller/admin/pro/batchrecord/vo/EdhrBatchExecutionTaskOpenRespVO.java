package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import cn.iocoder.yudao.module.mes.service.pro.batchrecordcelllink.BatchRecordCellLinkAutoPersistResult;

import lombok.Data;
import lombok.experimental.Accessors;

import cn.iocoder.yudao.module.bpm.formcenter.model.FormRecognizedField;

import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
public class EdhrBatchExecutionTaskOpenRespVO {

    private Long taskId;

    private Long executionId;

    private Long workTaskId;

    private Long assistUserId;

    private Long routeProcessId;

    private String batchRecordReportId;

    private Long batchRecordDefinitionId;

    private Long batchRecordVersionId;

    private Integer batchRecordSort;

    private String instanceScope;

    private String sharedFormKey;

    private String fillableScopeJson;

    private String executionMode;

    private String formSlotType;

    private String formBindingKey;

    private Long formTemplateId;

    private String formTemplateName;

    private Long formTemplateVersionId;

    private String formTemplateVersionNo;

    private Long formCenterInstanceId;

    private String formTemplateJimuSchemaJson;

    private List<FormRecognizedField> formTemplateRecognizedFields;

    private String recordCategory;

    private String validationProfile;

    private Boolean recordbookEnabled;

    private Long permissionScopeId;

    private Long routeBindingId;

    private String routeBindingSnapshotHash;

    private String requiredPolicy;

    private String requiredConditionJson;

    private String ownerRoleKey;

    private String archiveVisibility;

    private String slotConfigSnapshotHash;

    private Integer status;

    private Map<String, Object> executionPageQuery;

    private BatchRecordCellLinkAutoPersistResult cellLinkAutoPersist;
}
