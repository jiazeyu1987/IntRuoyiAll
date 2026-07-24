package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - eDHR 追溯门禁阻断明细 Response VO")
@Data
@Accessors(chain = true)
public class MesProEdhrValidationTraceIssueRespVO {

    @Schema(description = "验证包ID")
    private Long packageId;

    @Schema(description = "来源条目ID")
    private Long sourceItemId;

    @Schema(description = "来源条目编号")
    private String sourceItemCode;

    @Schema(description = "来源条目类型")
    private String sourceItemType;

    @Schema(description = "缺失条目类型")
    private String missingItemType;

    @Schema(description = "缺失条目名称")
    private String missingItemName;

    @Schema(description = "责任人")
    private String ownerName;

    @Schema(description = "签核角色")
    private String signoffRole;

    @Schema(description = "下一步动作")
    private String nextAction;

    @Schema(description = "阻断原因")
    private String blockingReason;

    @Schema(description = "签核影响")
    private String signoffImpact;
}
