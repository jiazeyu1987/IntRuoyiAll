package cn.iocoder.yudao.module.dcc.controller.admin.position.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DccApprovalPositionCreateReqVO {

    @Schema(description = "岗位名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "文件审核岗")
    @NotBlank(message = "岗位名称不能为空")
    private String name;

    @Schema(description = "变更原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "新增 DCC 审批岗位")
    @NotBlank(message = "变更原因不能为空")
    private String changeReason;
}
