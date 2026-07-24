package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow;

import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.process.MesProRouteProcessSaveReqVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MesProRouteProcessFlowSaveReqVO {

    @NotNull(message = "工艺路线不能为空")
    private Long routeId;

    private Long routeVersionId;

    @NotNull(message = "关系图版本不能为空")
    private Long graphVersion;

    @Valid
    private List<MesProRouteProcessFlowEdgeReqVO> edges = new ArrayList<>();

    @Valid
    private List<MesProRouteProcessFlowBoundaryEdgeReqVO> boundaryEdges = new ArrayList<>();

    @Valid
    private List<MesProRouteProcessFlowLayoutReqVO> layouts = new ArrayList<>();

    @Valid
    private List<MesProRouteProcessSaveReqVO> routeProcessCreates = new ArrayList<>();

    @Valid
    private List<MesProRouteProcessSaveReqVO> routeProcessUpdates = new ArrayList<>();

    private List<Long> routeProcessDeletes = new ArrayList<>();

}
