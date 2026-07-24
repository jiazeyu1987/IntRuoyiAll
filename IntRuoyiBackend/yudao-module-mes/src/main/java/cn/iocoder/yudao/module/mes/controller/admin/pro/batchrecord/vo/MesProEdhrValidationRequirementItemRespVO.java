package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - eDHR 验证条目 Response VO")
@Data
@Accessors(chain = true)
public class MesProEdhrValidationRequirementItemRespVO {

    @Schema(description = "主键", example = "1")
    private Long id;

    @Schema(description = "验证包ID")
    private Long packageId;

    @Schema(description = "条目编号")
    private String itemCode;

    @Schema(description = "条目名称")
    private String itemName;

    @Schema(description = "条目类型")
    private String itemType;

    @Schema(description = "条目版本")
    private String itemVersion;

    @Schema(description = "条目状态")
    private String itemStatus;

    @Schema(description = "责任人")
    private String ownerName;

    @Schema(description = "签核角色")
    private String signoffRole;

    @Schema(description = "来源文档")
    private String sourceDocument;

    @Schema(description = "业务过程")
    private String businessProcess;

    @Schema(description = "验收标准")
    private String acceptanceCriteria;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
