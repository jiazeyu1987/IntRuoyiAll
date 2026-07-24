package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow;

import lombok.Data;

@Data
public class MesProRouteProcessFlowEdgeRespVO {

    private Long id;

    private Long sourceRouteProcessId;

    private Long targetRouteProcessId;

    private String relationType;

}
