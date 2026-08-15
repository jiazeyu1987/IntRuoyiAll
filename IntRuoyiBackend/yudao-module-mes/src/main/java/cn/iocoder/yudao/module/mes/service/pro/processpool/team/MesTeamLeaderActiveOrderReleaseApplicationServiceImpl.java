package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class MesTeamLeaderActiveOrderReleaseApplicationServiceImpl
        implements MesTeamLeaderActiveOrderReleaseApplicationService {

    private final MesTeamLeaderActiveOrderReleaseGenerationService generationService;

    public MesTeamLeaderActiveOrderReleaseApplicationServiceImpl(
            MesTeamLeaderActiveOrderReleaseGenerationService generationService) {
        this.generationService = generationService;
    }

    @Override
    public MesTeamLeaderActiveOrderReleaseApplicationResult apply(
            Long leaderUserId, MesTeamLeaderActiveOrderReleaseApplyCommand command) {
        return generationService.generate(leaderUserId, command);
    }

    @Override
    public MesTeamLeaderActiveOrderReleaseApplicationResult get(Long userId, Long activeOrderId) {
        return generationService.get(userId, activeOrderId);
    }
}
