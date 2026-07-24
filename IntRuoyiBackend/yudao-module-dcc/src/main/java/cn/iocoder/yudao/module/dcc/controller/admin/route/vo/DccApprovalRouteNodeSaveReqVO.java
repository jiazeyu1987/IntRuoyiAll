package cn.iocoder.yudao.module.dcc.controller.admin.route.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DccApprovalRouteNodeSaveReqVO {
    @NotNull(message = "阶段编号不能为空")
    private Integer stageNo;
    @NotBlank(message = "阶段名称不能为空")
    private String stageName;
    @NotBlank(message = "候选源类型不能为空")
    private String candidateSourceType;
    @NotNull(message = "候选源编号不能为空")
    private Long candidateSourceId;
    @NotBlank(message = "审批方式不能为空")
    private String approveMethod;
    private Integer approveRatio;
    @NotNull(message = "必选标记不能为空")
    private Boolean required;
    @NotNull(message = "排序不能为空")
    private Integer sort;
}
