package cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.productcreate;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - DCC 项目代码与产品目录联合新建审批动作 Request VO")
@Data
public class DccProjectProductApprovalActionReqVO {

    @Schema(description = "审批意见；通过时选填，驳回时必填")
    @jakarta.validation.constraints.Size(max=500,message="操作原因不能超过500字符")
    private String reason;
}
