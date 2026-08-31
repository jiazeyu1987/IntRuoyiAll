package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesTeamLeaderActiveOrderReleaseApplicationServiceImplTest {

    @Mock
    private MesTeamLeaderActiveOrderCompletionService completionService;
    @Mock
    private MesTeamLeaderActiveOrderReleaseGenerationService generationService;

    private MesTeamLeaderActiveOrderReleaseApplicationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MesTeamLeaderActiveOrderReleaseApplicationServiceImpl(
                generationService, completionService);
    }

    @Test
    void applyCompletesAndBackfillsActiveOrderBeforeGeneratingPqcReleaseApplication() {
        MesTeamLeaderActiveOrderReleaseApplyCommand command = new MesTeamLeaderActiveOrderReleaseApplyCommand()
                .setActiveOrderId(10L)
                .setIdempotencyKey("release-key")
                .setApplyRemark("生产组长申请放行");
        MesTeamLeaderActiveOrderReleaseApplicationResult expected =
                new MesTeamLeaderActiveOrderReleaseApplicationResult().setApplicationId(99L);
        when(completionService.completeForRelease(20L, 10L, "release-key"))
                .thenReturn(new MesTeamLeaderActiveOrderCompletionResult().setCompletionReceiptId(88L));
        when(generationService.generate(20L, command)).thenReturn(expected);

        MesTeamLeaderActiveOrderReleaseApplicationResult actual = service.apply(20L, command);

        assertSame(expected, actual);
        InOrder order = inOrder(completionService, generationService);
        order.verify(completionService).completeForRelease(20L, 10L, "release-key");
        order.verify(generationService).generate(20L, command);
    }

    @Test
    void applyDoesNotCreatePqcReleaseApplicationWhenCompletionBackfillFails() {
        MesTeamLeaderActiveOrderReleaseApplyCommand command = new MesTeamLeaderActiveOrderReleaseApplyCommand()
                .setActiveOrderId(10L)
                .setIdempotencyKey("release-key");
        when(completionService.completeForRelease(20L, 10L, "release-key"))
                .thenThrow(new IllegalStateException("template rules are not confirmed"));

        assertThrows(IllegalStateException.class, () -> service.apply(20L, command));

        verifyNoInteractions(generationService);
    }
}
