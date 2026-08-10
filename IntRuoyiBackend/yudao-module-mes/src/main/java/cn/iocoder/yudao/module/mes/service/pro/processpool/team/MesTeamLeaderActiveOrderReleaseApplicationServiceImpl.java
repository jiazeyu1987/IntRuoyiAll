package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class MesTeamLeaderActiveOrderReleaseApplicationServiceImpl
        implements MesTeamLeaderActiveOrderReleaseApplicationService {

    private final MesTeamLeaderActiveOrderReleaseGenerationService generationService;
    private final MesTeamLeaderActiveOrderReleaseApplicationPersistenceService persistenceService;

    public MesTeamLeaderActiveOrderReleaseApplicationServiceImpl(
            MesTeamLeaderActiveOrderReleaseGenerationService generationService,
            MesTeamLeaderActiveOrderReleaseApplicationPersistenceService persistenceService) {
        this.generationService = generationService;
        this.persistenceService = persistenceService;
    }

    @Override
    public MesTeamLeaderActiveOrderReleaseApplicationResult apply(
            Long leaderUserId, MesTeamLeaderActiveOrderReleaseApplyCommand command) {
        try {
            return generationService.generate(leaderUserId, command);
        } catch (MesTeamLeaderActiveOrderReleaseBlockedException ex) {
            return persistenceService.persistBlocked(ex.getApplication());
        }
    }
}
