package cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - DCC 项目代码新增 Request VO")
@Data
public class DccProjectCodeSaveReqVO {

    @Schema(description = "文控")
    private String docControlNo;

    @Schema(description = "项目名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "项目名称不能为空")
    private String projectName;

    @Schema(description = "项目代码")
    private String projectCode;

    @Schema(description = "类别")
    private String category;

    @Schema(description = "委托生产")
    private String commissionedProduction;

    @Schema(description = "项目组负责人")
    private String projectLeader;

    @Schema(description = "项目工程师")
    private String projectEngineer;

    @Schema(description = "存放位置")
    private String storageLocation;

    @Schema(description = "优先级")
    private String priority;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "状态不能为空")
    private String status;
}
