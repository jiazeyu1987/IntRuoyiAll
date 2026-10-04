package cn.iocoder.yudao.module.mes.service.pro.batchrecord;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class MesEdhrBatchLifecycleGuardTest {
    private final MesProEdhrNonconformanceReviewMapper reviews = mock(MesProEdhrNonconformanceReviewMapper.class);
    private final MesProEdhrRecordChangeEventMapper changes = mock(MesProEdhrRecordChangeEventMapper.class);
    private final MesProEdhrBatchExecutionMapper batches = mock(MesProEdhrBatchExecutionMapper.class);
    private final MesEdhrBatchLifecycleGuard guard = new MesEdhrBatchLifecycleGuard();
    MesEdhrBatchLifecycleGuardTest() {
        ReflectionTestUtils.setField(guard, "reviewMapper", reviews);
        ReflectionTestUtils.setField(guard, "changeMapper", changes);
        ReflectionTestUtils.setField(guard, "batchMapper", batches);
    }
    private MesProEdhrBatchExecutionDO batch() {
        return MesProEdhrBatchExecutionDO.builder().id(10L).workOrderId(20L).status(30).aggregateHash("head").build();
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {15, 40, 50, 60})
    void terminalOrFrozenBatchCannotAcceptDossierWrites(int status) {
        var batch = batch(); batch.setStatus(status);
        when(batches.selectById(10L)).thenReturn(batch);
        assertThrows(ServiceException.class, () -> guard.requireDossierMutable(10L));
        verifyNoInteractions(changes, reviews);
    }

    @Test void pendingNcrBlocksIndependentVoidAndLaterApproval() {
        when(reviews.selectPendingByBatchExecutionId(10L)).thenReturn(new MesProEdhrNonconformanceReviewDO().setId(88L));
        var ex = assertThrows(ServiceException.class, () -> guard.requireIndependentVoidAllowed(batch()));
        assertTrue(ex.getMessage().contains("QA"));
        assertThrows(ServiceException.class, () -> guard.requireVoidEffectState(batch(), event("30"), null));
        verifyNoInteractions(changes, batches);
    }
    @Test void workOrderNcrAlsoBlocksBatchEvenBeforeBatchAssociation() {
        when(reviews.selectPendingCountByWorkOrderId(20L)).thenReturn(1L);
        assertThrows(ServiceException.class, () -> guard.requireIndependentVoidAllowed(batch()));
    }
    @Test void pendingVoidBlocksReleaseAndRejectedVoidRestoresRelease() {
        when(changes.selectCount(any())).thenReturn(1L, 0L);
        assertThrows(ServiceException.class, () -> guard.requireReleaseAllowed(10L));
        assertDoesNotThrow(() -> guard.requireReleaseAllowed(10L));
        var query = org.mockito.ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.Wrapper.class);
        verify(changes, times(2)).selectCount(query.capture());
        assertNotNull(query.getValue());
    }
    @Test void oldApprovalCannotOverwriteLaterMarketRelease() {
        var batch = batch(); batch.setStatus(40);
        assertThrows(ServiceException.class, () -> guard.requireVoidEffectState(batch, event("30"), archive()));
    }
    @Test void dedicatedPostReleaseArchiveVoidRetainsExactArchiveAssociation() {
        var batch = batch(); batch.setStatus(40);
        var event = event("40"); event.setSourceArchiveId(70L); event.setPreviousArchiveHash("archive");
        assertDoesNotThrow(() -> guard.requireVoidEffectState(batch, event, archive()));
        event.setSourceArchiveId(71L);
        assertThrows(ServiceException.class, () -> guard.requireVoidEffectState(batch, event, archive()));
    }
    @Test void mutableBatchAllowsNormalVoidButChangedHeadRejectsOldApproval() {
        assertDoesNotThrow(() -> guard.requireVoidEffectState(batch(), event("30"), null));
        var batch = batch(); batch.setAggregateHash("new-head");
        assertThrows(ServiceException.class, () -> guard.requireVoidEffectState(batch, event("30"), null));
    }
    private MesProEdhrRecordChangeEventDO event(String status) {
        return MesProEdhrRecordChangeEventDO.builder().batchExecutionId(10L).previousStatus(status).previousHeadHash("head").build();
    }
    private MesProEdhrBatchExecutionArchiveDO archive() {
        return MesProEdhrBatchExecutionArchiveDO.builder().id(70L).batchExecutionId(10L).contentHash("archive").build();
    }
}
