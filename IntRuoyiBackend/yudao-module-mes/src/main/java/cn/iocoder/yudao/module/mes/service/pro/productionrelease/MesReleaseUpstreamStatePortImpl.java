package cn.iocoder.yudao.module.mes.service.pro.productionrelease;

import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderService;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class MesReleaseUpstreamStatePortImpl implements MesReleaseUpstreamStatePort {

    @Resource
    private MesTeamLeaderActiveOrderService activeOrderService;
    @Resource
    private MesProWorkOrderService workOrderService;

    @Override
    public MesReleaseUpstreamClosureResult closeAfterRelease(MesReleaseUpstreamClosureCommand command) {
        if (command == null || command.getReleaseDecisionId() == null) {
            throw new IllegalArgumentException("releaseDecisionId is required for upstream closure");
        }
        MesReleaseUpstreamClosureResult result = new MesReleaseUpstreamClosureResult()
                .setReleaseDecisionId(command.getReleaseDecisionId())
                .setPickListStatus("READ_ONLY_SOURCE");
        if (command.getActiveOrderId() != null) {
            activeOrderService.closeForRelease(command.getActiveOrderId(),
                    command.getActiveOrderExpectedVersion(), command.getReleaseDecisionId(), command.getActorUserId());
            result.setActiveOrderId(command.getActiveOrderId()).setActiveOrderStatus("CLOSED");
        }
        if (command.getWorkOrderId() != null) {
            workOrderService.finishWorkOrderForRelease(command.getWorkOrderId(),
                    command.getReleaseDecisionId(), command.getActorUserId());
            result.setWorkOrderId(command.getWorkOrderId()).setWorkOrderStatus("FINISHED");
        }
        return result;
    }
}
