package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - MES 工艺流程批记录 Response VO")
@Data
public class MesProRouteFlowBatchRecordRespVO {

    @Schema(description = "批记录报表ID", example = "report-001")
    private String batchRecordReportId;

    @Schema(description = "批记录报表编码", example = "BR-001")
    private String batchRecordReportCode;

    @Schema(description = "批记录报表名称", example = "电子批记录-表1")
    private String batchRecordReportName;

    @Schema(description = "批记录定义 ID", example = "10")
    private Long batchRecordDefinitionId;

    @Schema(description = "批记录版本 ID", example = "20")
    private Long batchRecordVersionId;

    @Schema(description = "表单槽位类型：MAIN/LOSS_REPORT/PROCESS_INSPECTION/PARAMETER_RECORD", example = "MAIN")
    private String formSlotType;

    @Schema(description = "实例范围：PROCESS/BATCH_SHARED", example = "BATCH_SHARED")
    private String instanceScope;

    @Schema(description = "批次共享表单 Key", example = "IPQC")
    private String sharedFormKey;

    @Schema(description = "本工序可填写范围 JSON")
    private String fillableScopeJson;

    @Schema(description = "记录类型：BATCH_RECORD/INTERNAL_RECORD", example = "INTERNAL_RECORD")
    private String recordCategory;

    @Schema(description = "校验策略：CONTROLLED_BATCH/INTERNAL_TRACE", example = "INTERNAL_TRACE")
    private String validationProfile;

    @Schema(description = "对象级权限范围ID", example = "5001")
    private Long permissionScopeId;

    @Schema(description = "必填策略：REQUIRED/CONDITIONAL_REQUIRED/OPTIONAL/SKIPPABLE_CONTROLLED", example = "REQUIRED")
    private String requiredPolicy;

    @Schema(description = "条件必填表达式 JSON", example = "{\"trigger\":\"lossQuantityPositive\"}")
    private String requiredConditionJson;

    @Schema(description = "责任角色 Key：PRODUCTION/QUALITY/EQUIPMENT/QA/ARCHIVE", example = "QUALITY")
    private String ownerRoleKey;

    @Schema(description = "归档可见性：FINAL_DHR/INTERNAL_REVIEW/AUDIT_ONLY/ATTACHMENT_REFERENCE", example = "FINAL_DHR")
    private String archiveVisibility;

    @Schema(description = "槽位配置快照 Hash", example = "sha256")
    private String slotConfigSnapshotHash;

    @Schema(description = "批记录执行顺序", example = "1")
    private Integer reportSort;

    @Schema(description = "备注")
    private String remark;

}
