package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class MesProRouteProcessFlowBoundaryEdgeReqVO {

    @NotBlank(message = "边界类型不能为空")
    @Pattern(regexp = "START|END", message = "边界类型必须为 START 或 END")
    private String boundaryType;

    @NotNull(message = "边界关联工艺路线工序不能为空")
    private Long routeProcessId;

    private Integer sort;

}
