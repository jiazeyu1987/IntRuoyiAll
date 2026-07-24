package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow;

import lombok.Data;

@Data
public class MesProRouteProcessFlowNodeRespVO {

    private Long routeProcessId;

    private Long processId;

    private String processCode;

    private String processName;

    /**
     * 当前路线工序显式绑定的工作站编号。
     *
     * <p>{@link #workstationId} 可用于展示同工序可用工作站，不能作为路线工序已经绑定的判断依据。</p>
     */
    private Long routeProcessWorkstationId;

    private Long workstationId;

    private String workstationCode;

    private String workstationName;

    private Integer sort;

    private Integer x;

    private Integer y;

    private Boolean keyFlag;

    private Boolean checkFlag;

    private String resourceStatus;

}
