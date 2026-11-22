package cn.iocoder.yudao.module.mes.service.pro.frontline;

import java.util.List;

public interface MesFrontlineActiveOrderProcessService {

    List<MesFrontlineActiveOrderProcess> listProcesses(Long leaderUserId, Long activeOrderId);

    MesFrontlineActiveOrderProcess requireProcess(Long leaderUserId, Long activeOrderId, Long routeId,
                                                  Long routeProcessId, Long processId);

    /** Reads SOP only from the active order's locked route-version snapshot. */
    String getFrozenSopFileUrl(Long leaderUserId, Long activeOrderId, Long routeId,
                               Long routeProcessId, Long processId);
}
