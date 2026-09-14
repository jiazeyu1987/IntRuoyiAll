package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Schema(description = "管理后台 - MES 工艺流程动态表单绑定保存 Request VO")
@Data
@Accessors(chain = true)
public class MesProRouteFlowFormBindingSaveReqVO {

    @Schema(description = "动态表单绑定稳定 Key，由后端生成或前端回传", example = "FB_100_1")
    private String formBindingKey;

    @Schema(description = "全局联动组 Key；为空表示仅当前工序", example = "GFB_1723860000000_1")
    @Size(max = 128, message = "全局联动组 Key 长度不能超过 128")
    private String globalSyncKey;

    @Schema(description = "表单中心模板稳定 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "10001")
    @NotNull(message = "表单中心模板不能为空")
    private Long formTemplateId;

    @Schema(description = "表单中心模板名称快照", example = "清洗记录")
    private String formTemplateName;

    @Schema(description = "表单槽位类型：MAIN/LOSS_REPORT/PROCESS_INSPECTION/PARAMETER_RECORD，缺省 MAIN", example = "MAIN")
    private String formSlotType;

    @Schema(description = "最后校验到的发布版本 ID，由后端填充", example = "20001")
    private Long lastPublishedTemplateVersionId;

    @Schema(description = "最后校验到的发布版本号，由后端填充", example = "V3")
    private String lastPublishedTemplateVersionNo;

    @Schema(description = "实例范围：PROCESS/BATCH_SHARED，缺省 PROCESS", example = "PROCESS")
    private String instanceScope;

    @Schema(description = "批次共享表单 Key，同一批次同一 Key 共用一张执行实例", example = "IPQC")
    private String sharedFormKey;

    @Schema(description = "本工序可填写范围 JSON")
    private String fillableScopeJson;

    @Schema(description = "记录类型：BATCH_RECORD/INTERNAL_RECORD，缺省 BATCH_RECORD", example = "BATCH_RECORD")
    private String recordCategory;

    @Schema(description = "校验策略：CONTROLLED_BATCH/INTERNAL_TRACE，缺省随记录类型推导", example = "CONTROLLED_BATCH")
    private String validationProfile;

    @Schema(description = "是否启用记录本入口", example = "true")
    private Boolean recordbookEnabled;

    @Schema(description = "对象级权限范围ID", example = "5001")
    private Long permissionScopeId;

    @Schema(description = "记录类型快照 Hash，由后端保存时生成", example = "sha256")
    private String recordCategorySnapshotHash;

    @Schema(description = "必填策略：REQUIRED/CONDITIONAL_REQUIRED/OPTIONAL/SKIPPABLE_CONTROLLED", example = "REQUIRED")
    private String requiredPolicy;

    @Schema(description = "条件必填表达式 JSON")
    private String requiredConditionJson;

    @Schema(description = "责任角色 Key：PRODUCTION/QUALITY/EQUIPMENT/QA/ARCHIVE", example = "PRODUCTION")
    private String ownerRoleKey;

    @Schema(description = "归档可见性：FINAL_DHR/INTERNAL_REVIEW/AUDIT_ONLY/ATTACHMENT_REFERENCE", example = "FINAL_DHR")
    private String archiveVisibility;

    @Schema(description = "槽位配置快照 Hash，由后端保存时生成", example = "sha256")
    private String slotConfigSnapshotHash;

    @Schema(description = "填写人来源：USER/USERS/ROLE", example = "USERS")
    private String candidateSourceType;

    @Schema(description = "填写人来源 ID 列表")
    private List<Long> candidateSourceIds;

    @Schema(description = "填写人名称快照")
    private List<String> candidateSourceNames;

    @Schema(description = "表单执行顺序", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "表单执行顺序不能为空")
    private Integer reportSort;

    @Schema(description = "备注")
    private String remark;
}
