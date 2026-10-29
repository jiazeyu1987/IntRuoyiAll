package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.*;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.apache.ibatis.exceptions.TooManyResultsException;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesProEdhrBatchSummaryReaderTest {
    final MesProEdhrReleaseTransactionMapper releases = mock(MesProEdhrReleaseTransactionMapper.class);
    final MesProEdhrBatchExecutionOriginMapper origins = mock(MesProEdhrBatchExecutionOriginMapper.class);
    final MesProEdhrOperationAuditEventMapper audits = mock(MesProEdhrOperationAuditEventMapper.class);
    final MesProEdhrRecordChangeEventMapper changes = mock(MesProEdhrRecordChangeEventMapper.class);
    final MesProEdhrNonconformanceReviewMapper reviews = mock(MesProEdhrNonconformanceReviewMapper.class);
    final MesMarketReleaseAccess access = mock(MesMarketReleaseAccess.class);
    final MesProEdhrBatchSummaryReader reader = new MesProEdhrBatchSummaryReader(releases, origins, audits, changes, reviews, access);
    final List<MesProEdhrBatchExecutionDO> batches = List.of(
            new MesProEdhrBatchExecutionDO().setId(1L).setWorkOrderId(101L),
            new MesProEdhrBatchExecutionDO().setId(2L).setWorkOrderId(102L));

    @Test void readsEachFactOnceForTheWholePageAndKeepsLatestReleaseAndOriginPrecedence() {
        var latest = new MesProEdhrReleaseTransactionDO().setId(9L).setBatchExecutionId(1L).setReleaseStatus("RELEASED");
        when(releases.selectLatestByBatchExecutionIds(List.of(1L, 2L))).thenReturn(List.of(latest));
        when(origins.selectList(any(Wrapper.class))).thenReturn(List.of(
                new MesProEdhrBatchExecutionOriginDO().setBatchExecutionId(1L).setActiveOrderId(11L),
                new MesProEdhrBatchExecutionOriginDO().setBatchExecutionId(1L).setActiveOrderId(11L)));
        when(audits.selectList(any(Wrapper.class))).thenReturn(List.of(
                new MesProEdhrOperationAuditEventDO().setBatchExecutionId(2L).setId(22L)));
        when(reviews.selectFreezeCounts(anyCollection(), anyCollection(), anyCollection())).thenReturn(List.of(
                new MesProEdhrNonconformanceReviewMapper.FreezeCount().setScope("WORK_ORDER").setScopeId(102L).setReviewCount(1L)));
        var facts = reader.read(batches, 5L, audit -> audit.getId());
        assertSame(latest, facts.get(1L).release());
        assertEquals(11L, facts.get(1L).activeOrderId());
        assertEquals(22L, facts.get(2L).activeOrderId());
        assertFalse(facts.get(1L).frozen());
        assertTrue(facts.get(2L).frozen());
        verify(releases).selectLatestByBatchExecutionIds(List.of(1L, 2L));
        verify(origins).selectList(any(Wrapper.class));
        verify(audits).selectList(any(Wrapper.class));
        verify(changes).selectList(any(Wrapper.class));
        verify(reviews).selectFreezeCounts(List.of(1L, 2L), Set.of(101L, 102L), Set.of(11L));
        verify(access).getAll(List.of(1L, 2L), 5L);
        verifyNoMoreInteractions(releases, origins, audits, changes, reviews, access);
    }

    @Test void duplicateFormalSourcesStillFailInsteadOfChoosingOne() {
        when(origins.selectList(any(Wrapper.class))).thenReturn(List.of(
                new MesProEdhrBatchExecutionOriginDO().setBatchExecutionId(1L).setActiveOrderId(11L),
                new MesProEdhrBatchExecutionOriginDO().setBatchExecutionId(1L).setActiveOrderId(12L)));
        var error = assertThrows(IllegalStateException.class, () -> reader.read(batches, 5L, audit -> null));
        assertEquals("批次执行存在多个不同的活跃订单来源：1", error.getMessage());
        verifyNoInteractions(audits);
    }

    @Test void duplicatePendingReviewsRetainSelectOneFailure() {
        when(reviews.selectFreezeCounts(anyCollection(), anyCollection(), anyCollection())).thenReturn(List.of(
                new MesProEdhrNonconformanceReviewMapper.FreezeCount().setScope("BATCH").setScopeId(1L).setReviewCount(2L)));
        assertThrows(TooManyResultsException.class, () -> reader.read(batches, 5L, audit -> null));
    }

    @Test void pqcReleaseAndNullSourceDoNotFreezeAnotherBatchByActiveOrderAlone() {
        when(origins.selectList(any(Wrapper.class))).thenReturn(List.of(
                new MesProEdhrBatchExecutionOriginDO().setBatchExecutionId(1L).setActiveOrderId(11L)));
        when(reviews.selectFreezeCounts(anyCollection(), anyCollection(), anyCollection())).thenReturn(List.of());
        assertFalse(reader.read(batches, 5L, audit -> null).get(1L).frozen());
        when(reviews.selectFreezeCounts(anyCollection(), anyCollection(), anyCollection())).thenReturn(List.of(
                new MesProEdhrNonconformanceReviewMapper.FreezeCount().setScope("ACTIVE_ORDER").setScopeId(11L).setReviewCount(1L)));
        assertTrue(reader.read(batches, 5L, audit -> null).get(1L).frozen());
    }

    @Test void corruptAuditSourceIsNotSwallowed() {
        when(audits.selectList(any(Wrapper.class))).thenReturn(List.of(new MesProEdhrOperationAuditEventDO().setBatchExecutionId(1L)));
        var error = assertThrows(IllegalStateException.class, () -> reader.read(batches, 5L, audit -> {
            throw new IllegalStateException("invalid audit source");
        }));
        assertEquals("invalid audit source", error.getMessage());
    }

    @Test void emptyPageDoesNotQueryFacts() {
        assertTrue(reader.read(List.of(), 5L, audit -> null).isEmpty());
        verifyNoInteractions(releases, origins, audits, changes, reviews, access);
    }
}
