package cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.productcreate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - DCC 项目代码与产品目录联合新建审批动作 Request VO")
@Data
public class DccProjectProductApprovalActionReqVO {

    @NotBlank(message = "审批意见不能为空")
    private String reason;
}
