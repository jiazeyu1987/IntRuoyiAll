package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MesProRouteProcessFlowLayoutReqVO {

    @NotNull(message = "工艺路线工序不能为空")
    private Long routeProcessId;

    @NotNull(message = "节点横坐标不能为空")
    private Integer x;

    @NotNull(message = "节点纵坐标不能为空")
    private Integer y;

    private Integer width;

    private Integer height;

}
