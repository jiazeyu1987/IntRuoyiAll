package cn.iocoder.yudao.module.bpm.controller.admin.businessapproval.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 平台业务审批策略分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class BusinessApprovalPolicyPageReqVO extends PageParam {

    @Schema(description = "租户编号")
    private Long tenantId;

    @Schema(description = "数据域")
    private String dataDomain;

    @Schema(description = "系统编码")
    private String systemCode;

    @Schema(description = "对象类型")
    private String objectType;

    @Schema(description = "动作编码")
    private String actionCode;

    @Schema(description = "对象状态")
    private String objectState;

    @Schema(description = "策略模式")
    private String policyMode;

    @Schema(description = "状态")
    private String status;

}
