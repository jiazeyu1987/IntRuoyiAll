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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesTeamLeaderActiveOrderReleaseApplicationServiceImplTest {

    @Mock
    private MesTeamLeaderActiveOrderCompletionService completionService;
    @Mock
    private MesTeamLeaderActiveOrderReleaseGenerationService generationService;
    @Mock
    private cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptMapper receiptMapper;
    @Mock
    private cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper batchMapper;
    @Mock
    private cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper;

    private MesTeamLeaderActiveOrderReleaseApplicationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MesTeamLeaderActiveOrderReleaseApplicationServiceImpl(
                generationService, completionService, receiptMapper, batchMapper, applicationMapper);
    }

    @Test
    void pushGeneratedRejectsMissingP2WithoutBackfill() {
        var command = new MesTeamLeaderActiveOrderReleaseApplyCommand().setActiveOrderId(10L).setIdempotencyKey("P3-10");
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> service.applyGenerated(20L, command));
        org.mockito.Mockito.verifyNoInteractions(completionService);
        verify(generationService, never()).generate(20L, command);
    }

    @Test
    void pushGeneratedUsesPersistedP2WithoutBackfill() {
        var command = new MesTeamLeaderActiveOrderReleaseApplyCommand().setActiveOrderId(10L).setIdempotencyKey("P3-10");
        var receipt = cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptDO.builder()
                .id(88L).activeOrderId(10L).leaderUserId(20L).workOrderId(90L).batchCode("B90").routeId(30L)
                .receiptStatus("BACKFILL_SUCCEEDED").batchRecordStatus("SUCCESS").processInspectionStatus("SUCCESS")
                .batchRecordId(91L).processInspectionId(92L).build();
        when(receiptMapper.selectByActiveOrderIdForUpdate(10L)).thenReturn(receipt);
        when(batchMapper.selectByContext(90L, "B90", 30L)).thenReturn(
                cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO.builder().id(93L).status(0).build());
        var expected = new MesTeamLeaderActiveOrderReleaseApplicationResult().setApplicationId(99L).setVersion(1);
        when(generationService.generate(20L, command)).thenReturn(expected);
        when(applicationMapper.bindP3BatchExecution(99L, 1, 93L)).thenReturn(1);
        assertSame(expected, service.applyGenerated(20L, command));
        verify(applicationMapper).bindP3BatchExecution(99L, 1, 93L);
        org.mockito.Mockito.verifyNoInteractions(completionService);
    }

    @Test
    void pushGeneratedBindsExistingPendingApplicationToPersistedP2Batch() {
        var command = new MesTeamLeaderActiveOrderReleaseApplyCommand()
                .setActiveOrderId(10L).setIdempotencyKey("P3-10");
        var existing = new MesTeamLeaderActiveOrderReleaseApplicationResult()
                .setApplicationId(99L).setVersion(1);
        when(generationService.replayExisting(20L, command)).thenReturn(existing);
        var receipt = cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptDO.builder()
                .id(88L).activeOrderId(10L).leaderUserId(20L).workOrderId(90L).batchCode("B90").routeId(30L)
                .receiptStatus("BACKFILL_SUCCEEDED").batchRecordStatus("SUCCESS").processInspectionStatus("SUCCESS")
                .batchRecordId(91L).processInspectionId(92L).routeVersionId(31L).build();
        when(receiptMapper.selectByActiveOrderIdForUpdate(10L)).thenReturn(receipt);
        when(batchMapper.selectByContext(90L, "B90", 30L)).thenReturn(
                cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO.builder()
                        .id(93L).status(0).build());
        when(applicationMapper.bindP3BatchExecution(99L, 1, 93L)).thenReturn(1);

        var actual = service.applyGenerated(20L, command);

        org.junit.jupiter.api.Assertions.assertEquals(93L, actual.getBatchExecutionId());
        org.junit.jupiter.api.Assertions.assertEquals(2, actual.getVersion());
        verify(applicationMapper).bindP3BatchExecution(99L, 1, 93L);
        verify(generationService, never()).generate(20L, command);
    }

    @Test
    void pushGeneratedReplaysWithoutGeneratingAgain() {
        var command = new MesTeamLeaderActiveOrderReleaseApplyCommand().setActiveOrderId(10L).setIdempotencyKey("P3-10");
        var expected = new MesTeamLeaderActiveOrderReleaseApplicationResult()
                .setApplicationId(99L).setVersion(2).setBatchExecutionId(93L);
        when(generationService.replayExisting(20L, command)).thenReturn(expected);
        assertSame(expected, service.applyGenerated(20L, command));
        org.mockito.Mockito.verifyNoInteractions(completionService, receiptMapper, batchMapper);
        verify(generationService, never()).generate(20L, command);
    }

    @Test
    void applyCompletesAndBackfillsActiveOrderBeforeGeneratingPqcReleaseApplication() {
        MesTeamLeaderActiveOrderReleaseApplyCommand command = new MesTeamLeaderActiveOrderReleaseApplyCommand()
                .setActiveOrderId(10L).setConfirmNoReplenishmentInfo(true)
                .setIdempotencyKey("release-key")
                .setApplyRemark("生产组长申请放行");
        MesTeamLeaderActiveOrderReleaseApplicationResult expected =
                new MesTeamLeaderActiveOrderReleaseApplicationResult().setApplicationId(99L);
        when(generationService.replayExisting(20L, command)).thenReturn(null);
        when(completionService.completeForRelease(20L, 10L, "release-key", true))
                .thenReturn(new MesTeamLeaderActiveOrderCompletionResult().setCompletionReceiptId(88L));
        when(generationService.generate(20L, command)).thenReturn(expected);

        MesTeamLeaderActiveOrderReleaseApplicationResult actual = service.apply(20L, command);

        assertSame(expected, actual);
        InOrder order = inOrder(completionService, generationService);
        order.verify(generationService).replayExisting(20L, command);
        order.verify(completionService).completeForRelease(20L, 10L, "release-key", true);
        order.verify(generationService).generate(20L, command);
    }

    @Test
    void applyReplaysExistingApplicationBeforeCompletionBackfill() {
        MesTeamLeaderActiveOrderReleaseApplyCommand command = new MesTeamLeaderActiveOrderReleaseApplyCommand()
                .setActiveOrderId(10L)
                .setIdempotencyKey("release-key")
                .setApplyRemark("生产组长重试申请放行");
        MesTeamLeaderActiveOrderReleaseApplicationResult expected =
                new MesTeamLeaderActiveOrderReleaseApplicationResult().setApplicationId(99L);
        when(generationService.replayExisting(20L, command)).thenReturn(expected);

        MesTeamLeaderActiveOrderReleaseApplicationResult actual = service.apply(20L, command);

        assertSame(expected, actual);
        verify(completionService, never()).completeForRelease(20L, 10L, "release-key", null);
        verify(generationService, never()).generate(20L, command);
    }

    @Test
    void applyDoesNotCreatePqcReleaseApplicationWhenCompletionBackfillFails() {
        MesTeamLeaderActiveOrderReleaseApplyCommand command = new MesTeamLeaderActiveOrderReleaseApplyCommand()
                .setActiveOrderId(10L)
                .setIdempotencyKey("release-key");
        when(generationService.replayExisting(20L, command)).thenReturn(null);
        when(completionService.completeForRelease(20L, 10L, "release-key", null))
                .thenThrow(new IllegalStateException("template rules are not confirmed"));

        assertThrows(IllegalStateException.class, () -> service.apply(20L, command));

        verify(generationService, never()).generate(20L, command);
    }
}
