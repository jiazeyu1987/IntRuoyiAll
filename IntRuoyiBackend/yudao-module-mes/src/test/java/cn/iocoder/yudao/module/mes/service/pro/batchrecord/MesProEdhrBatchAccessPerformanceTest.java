package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesMarketReleaseHandoverMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MesProEdhrBatchAccessPerformanceTest {
    @Test void resolvedOwnershipAndRevocationSkipTaskQueries() {
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
                new org.apache.ibatis.builder.MapperBuilderAssistant(
                        new com.baomidou.mybatisplus.core.MybatisConfiguration(), "batch-visibility-test"),
                MesProEdhrBatchExecutionTaskDO.class);
        var taskMapper = mock(MesProEdhrBatchExecutionTaskMapper.class);
        var access = mock(MesMarketReleaseAccess.class);
        var service = spy(new MesProEdhrBatchExecutionVisibilityService());
        org.springframework.test.util.ReflectionTestUtils.setField(service, "batchTaskMapper", taskMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "marketReleaseAccess", access);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "permissionApi",
                mock(cn.iocoder.yudao.module.system.api.permission.PermissionApi.class));
        org.springframework.test.util.ReflectionTestUtils.setField(service, "batchOriginMapper",
                mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionOriginMapper.class));
        org.springframework.test.util.ReflectionTestUtils.setField(service, "releaseApplicationMapper",
                mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper.class));
        org.springframework.test.util.ReflectionTestUtils.setField(service, "releaseTransactionMapper",
                mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper.class));
        org.springframework.test.util.ReflectionTestUtils.setField(service, "workTaskMapper",
                mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper.class));
        var revoked = new MesProEdhrBatchExecutionDO().setId(2L);
        var original = new MesProEdhrBatchExecutionDO().setId(1L);
        var first = new MesProEdhrBatchExecutionTaskDO().setId(11L).setBatchExecutionId(1L);
        var other = new MesProEdhrBatchExecutionTaskDO().setId(21L).setBatchExecutionId(2L);
        var second = new MesProEdhrBatchExecutionTaskDO().setId(12L).setBatchExecutionId(1L);
        when(access.getAll(List.of(2L, 1L), 9L)).thenReturn(java.util.Map.of(
                2L, new MesMarketReleaseAccess.Access(false, false, false, true, 2L, 8L, 7L, "owner"),
                1L, new MesMarketReleaseAccess.Access(true, false, true, true, 1L, 9L, 7L, "owner")));
        assertEquals(List.of(original), service.filterVisible(List.of(revoked, original), 9L));
        verify(service, never()).resolveCurrentProcessTask(anyList());
        verifyNoInteractions(taskMapper);
        verify(access).getAll(List.of(2L, 1L), 9L);
        verifyNoMoreInteractions(access);
    }

    @Test void batchHistoryPreservesLatestOwnerAndRevokedRecipients() {
        var mapper = mock(MesMarketReleaseHandoverMapper.class);
        var service = new MesMarketReleaseAccess(mapper, null, null);
        var newer = new MesMarketReleaseHandoverMapper.Row().setId(12L).setBatchExecutionId(1L)
                .setOriginalUserId(10L).setRecipientUserId(30L);
        var other = new MesMarketReleaseHandoverMapper.Row().setId(11L).setBatchExecutionId(2L)
                .setOriginalUserId(10L).setRecipientUserId(20L).setRecalledAt(LocalDateTime.now());
        var older = new MesMarketReleaseHandoverMapper.Row().setId(9L).setBatchExecutionId(1L)
                .setOriginalUserId(10L).setRecipientUserId(20L);
        var latestOne = new MesMarketReleaseHandoverMapper.AccessRow();
        latestOne.setId(newer.getId()); latestOne.setBatchExecutionId(newer.getBatchExecutionId());
        latestOne.setOriginalUserId(newer.getOriginalUserId()); latestOne.setRecipientUserId(newer.getRecipientUserId());
        latestOne.setActorRecipient(true);
        var latestTwo = new MesMarketReleaseHandoverMapper.AccessRow();
        latestTwo.setId(other.getId()); latestTwo.setBatchExecutionId(other.getBatchExecutionId());
        latestTwo.setOriginalUserId(other.getOriginalUserId()); latestTwo.setRecipientUserId(other.getRecipientUserId());
        latestTwo.setRecalledAt(other.getRecalledAt()); latestTwo.setActorRecipient(true);
        when(mapper.selectAccessByBatchIds(List.of(1L, 2L, 3L), 20L)).thenReturn(List.of(latestOne, latestTwo));
        var result = service.getAll(List.of(1L, 2L, 3L), 20L);
        assertEquals(MesMarketReleaseAccess.resolve(List.of(newer, older), 20L), result.get(1L));
        assertEquals(MesMarketReleaseAccess.resolve(List.of(other), 20L), result.get(2L));
        assertEquals(MesMarketReleaseAccess.resolve(List.of(), 20L), result.get(3L));
        assertFalse(result.get(1L).visible());
        assertFalse(result.get(2L).visible());
        assertTrue(result.get(3L).visible());
        verify(mapper).selectAccessByBatchIds(List.of(1L, 2L, 3L), 20L);
        verifyNoMoreInteractions(mapper);
    }
}
