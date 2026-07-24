package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProEdhrWorkTaskReleaseApprovalRuleReqVO {

    @NotNull(message = "工艺路线不能为空")
    private Long routeId;

    @NotBlank(message = "最终放行审批候选类型不能为空")
    private String candidateSourceType;

    @NotNull(message = "最终放行审批候选不能为空")
    private Long candidateSourceId;

    @NotNull(message = "启用状态不能为空")
    private Boolean enabled;

    private String remark;
}
