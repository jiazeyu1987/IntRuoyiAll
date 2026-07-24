package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class MesProRouteProcessFlowGraphRespVO extends MesProRouteProcessFlowValidationRespVO {

    private Long routeId;

    private List<MesProRouteProcessFlowNodeRespVO> nodes = new ArrayList<>();

    private List<MesProRouteProcessFlowEdgeRespVO> edges = new ArrayList<>();

    private List<MesProRouteProcessFlowBoundaryEdgeRespVO> boundaryEdges = new ArrayList<>();

}
