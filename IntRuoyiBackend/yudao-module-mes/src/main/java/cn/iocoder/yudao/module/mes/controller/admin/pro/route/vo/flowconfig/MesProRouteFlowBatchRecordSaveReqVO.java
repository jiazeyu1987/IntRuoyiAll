package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - MES 工艺流程批记录保存 Request VO")
@Data
@Accessors(chain = true)
public class MesProRouteFlowBatchRecordSaveReqVO {

    @Schema(description = "批记录报表ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "report-001")
    private String batchRecordReportId;

    @Schema(description = "表单槽位类型：MAIN/LOSS_REPORT/PROCESS_INSPECTION/PARAMETER_RECORD，缺省 MAIN", example = "MAIN")
    private String formSlotType;

    @Schema(description = "实例范围：PROCESS/BATCH_SHARED，缺省 PROCESS", example = "BATCH_SHARED")
    private String instanceScope;

    @Schema(description = "批次共享表单 Key，同一批次同一 Key 共用一张执行实例", example = "IPQC")
    private String sharedFormKey;

    @Schema(description = "本工序可填写范围 JSON，必须包含表格上下文")
    private String fillableScopeJson;

    @Schema(description = "记录类型：BATCH_RECORD/INTERNAL_RECORD，缺省 BATCH_RECORD", example = "INTERNAL_RECORD")
    private String recordCategory;

    @Schema(description = "校验策略：CONTROLLED_BATCH/INTERNAL_TRACE，缺省随记录类型推导", example = "INTERNAL_TRACE")
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

    @Schema(description = "槽位配置快照 Hash，由后端保存时生成", example = "sha256")
    private String slotConfigSnapshotHash;

    @Schema(description = "批记录执行顺序", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "批记录执行顺序不能为空")
    private Integer reportSort;

    @Schema(description = "备注")
    private String remark;

}
