package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - eDHR 追溯关系 Response VO")
@Data
@Accessors(chain = true)
public class MesProEdhrValidationTraceLinkRespVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "验证包ID")
    private Long packageId;

    @Schema(description = "来源条目ID")
    private Long sourceItemId;

    @Schema(description = "来源条目编号")
    private String sourceItemCode;

    @Schema(description = "来源条目类型")
    private String sourceItemType;

    @Schema(description = "目标条目ID")
    private Long targetItemId;

    @Schema(description = "目标条目编号")
    private String targetItemCode;

    @Schema(description = "目标条目类型")
    private String targetItemType;

    @Schema(description = "追溯类型")
    private String linkType;

    @Schema(description = "追溯状态")
    private String traceStatus;

    @Schema(description = "责任人")
    private String ownerName;

    @Schema(description = "下一步动作")
    private String nextAction;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
