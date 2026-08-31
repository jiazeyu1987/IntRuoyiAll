package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowIdempotency;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class MesTeamLeaderActiveOrderReleaseApplicationServiceImpl
        implements MesTeamLeaderActiveOrderReleaseApplicationService {

    private final MesTeamLeaderActiveOrderReleaseGenerationService generationService;
    private final MesTeamLeaderActiveOrderCompletionService completionService;

    public MesTeamLeaderActiveOrderReleaseApplicationServiceImpl(
            MesTeamLeaderActiveOrderReleaseGenerationService generationService,
            MesTeamLeaderActiveOrderCompletionService completionService) {
        this.generationService = generationService;
        this.completionService = completionService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesTeamLeaderActiveOrderReleaseApplicationResult apply(
            Long leaderUserId, MesTeamLeaderActiveOrderReleaseApplyCommand command) {
        String releaseIdempotencyKey = MesReleaseFlowIdempotency.requireKey(
                command == null ? null : command.getIdempotencyKey());
        command.setIdempotencyKey(releaseIdempotencyKey);
        completionService.completeForRelease(
                leaderUserId, command.getActiveOrderId(), releaseIdempotencyKey);
        return generationService.generate(leaderUserId, command);
    }

    @Override
    public MesTeamLeaderActiveOrderReleaseApplicationResult get(Long userId, Long activeOrderId) {
        return generationService.get(userId, activeOrderId);
    }
}
