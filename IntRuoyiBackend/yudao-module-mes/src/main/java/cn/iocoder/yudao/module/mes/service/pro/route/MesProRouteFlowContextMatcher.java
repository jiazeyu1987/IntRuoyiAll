package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessConfigDO;

import java.util.Objects;

public final class MesProRouteFlowContextMatcher {

    private MesProRouteFlowContextMatcher() {
    }

    public static boolean isFlowContext(MesProRouteFlowConfigDO flowConfig, Long routeId, String useType) {
        return flowConfig != null
                && flowConfig.getId() != null
                && Objects.equals(flowConfig.getRouteId(), routeId)
                && Objects.equals(flowConfig.getUseType(), useType);
    }

    public static boolean isEnabledFlowContext(MesProRouteFlowConfigDO flowConfig, Long routeId, String useType) {
        return flowConfig == null || (isFlowContext(flowConfig, routeId, useType)
                && Boolean.TRUE.equals(flowConfig.getEnabled()));
    }

    public static boolean isProcessConfigOwnedBy(MesProRouteFlowConfigDO flowConfig,
                                                 MesProRouteFlowProcessConfigDO processConfig,
                                                 Long routeId,
                                                 String useType) {
        return isFlowContext(flowConfig, routeId, useType)
                && processConfig != null
                && Objects.equals(processConfig.getRouteFlowConfigId(), flowConfig.getId())
                && Objects.equals(processConfig.getRouteId(), routeId)
                && Objects.equals(processConfig.getUseType(), useType);
    }

    public static boolean isProcessConfigOwnedBy(MesProRouteFlowConfigDO flowConfig,
                                                 MesProRouteFlowProcessConfigDO processConfig,
                                                 Long routeId,
                                                 Long routeProcessId,
                                                 String useType) {
        return isProcessConfigOwnedBy(flowConfig, processConfig, routeId, useType)
                && Objects.equals(processConfig.getRouteProcessId(), routeProcessId);
    }
}
