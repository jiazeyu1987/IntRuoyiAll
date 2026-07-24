package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - eDHR OQ/PQ 用例 Response VO")
@Data
@Accessors(chain = true)
public class MesProEdhrOqPqCaseRespVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "验证包ID")
    private Long packageId;

    @Schema(description = "用例编号")
    private String caseCode;

    @Schema(description = "用例名称")
    private String caseName;

    @Schema(description = "用例类型")
    private String caseType;

    @Schema(description = "用例版本")
    private String caseVersion;

    @Schema(description = "用例状态")
    private String caseStatus;

    @Schema(description = "步骤编号")
    private String stepNo;

    @Schema(description = "步骤标题")
    private String stepTitle;

    @Schema(description = "预期结果")
    private String expectedResult;

    @Schema(description = "证据要求")
    private String evidenceRequirement;

    @Schema(description = "责任人")
    private String ownerName;

    @Schema(description = "复核人")
    private String reviewerName;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
