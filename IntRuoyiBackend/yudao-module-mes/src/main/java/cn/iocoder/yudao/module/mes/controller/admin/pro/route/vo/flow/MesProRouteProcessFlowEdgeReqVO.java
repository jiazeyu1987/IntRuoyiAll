package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MesProRouteProcessFlowEdgeReqVO {

    @NotNull(message = "源工艺路线工序不能为空")
    private Long sourceRouteProcessId;

    @NotNull(message = "目标工艺路线工序不能为空")
    private Long targetRouteProcessId;

    private String relationType;

}
