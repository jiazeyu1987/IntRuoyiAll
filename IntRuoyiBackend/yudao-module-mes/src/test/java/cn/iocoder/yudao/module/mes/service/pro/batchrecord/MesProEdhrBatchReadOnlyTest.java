package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MesProEdhrBatchReadOnlyTest {
    @Mock private MesProEdhrBatchExecutionMapper batchExecutionMapper;
    @Mock private cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionOriginMapper batchExecutionOriginMapper;
    @Mock private MesProEdhrBatchExecutionTaskMapper batchTaskMapper;
    @Mock private MesMarketReleaseAccess marketReleaseAccess;
    @Mock private MesProEdhrBatchExecutionVisibilityService batchExecutionVisibilityService;
    @InjectMocks private MesProEdhrBatchExecutionServiceImpl service;

    @ParameterizedTest
    @ValueSource(ints = {0, 10, 20, 40, 50})
    void readsPersistedBatchAtEveryStageWithoutStatusWritesOrTaskProvisioning(int status) {
        var batch = new MesProEdhrBatchExecutionDO().setId(800L).setStatus(status).setWorkOrderId(200L);
        when(batchExecutionMapper.selectById(800L)).thenReturn(batch);
        when(batchExecutionOriginMapper.selectListByBatchExecutionId(800L)).thenReturn(java.util.List.of(
                new cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionOriginDO().setActiveOrderId(300L)));
        var source = service.getReadableBatchSource(800L);
        assertEquals(800L, source.getId());
        assertEquals(200L, source.getWorkOrderId());
        assertEquals(300L, source.getActiveOrderId());
        assertEquals(status, batch.getStatus());
        verify(marketReleaseAccess).requireReadable(800L);
        verify(batchExecutionVisibilityService).requireVisibleBatch(eq(batch), nullable(Long.class));
        verify(batchExecutionMapper).selectById(800L);
        verifyNoMoreInteractions(batchExecutionMapper);
        verifyNoInteractions(batchTaskMapper);
    }

    @Test
    void rejectsAuthorizationBeforeReadingSource() {
        doThrow(new IllegalStateException("access denied")).when(marketReleaseAccess).requireReadable(800L);
        assertThrows(IllegalStateException.class, () -> service.getReadableBatchSource(800L));
        verifyNoInteractions(batchExecutionMapper, batchTaskMapper, batchExecutionVisibilityService);
    }

    @Test
    void rejectsInvisibleBatchWithoutWrites() {
        var batch = new MesProEdhrBatchExecutionDO().setId(800L);
        when(batchExecutionMapper.selectById(800L)).thenReturn(batch);
        doThrow(new IllegalStateException("not visible")).when(batchExecutionVisibilityService)
                .requireVisibleBatch(eq(batch), nullable(Long.class));
        assertThrows(IllegalStateException.class, () -> service.getReadableBatchSource(800L));
        verify(batchExecutionMapper).selectById(800L);
        verifyNoMoreInteractions(batchExecutionMapper);
        verifyNoInteractions(batchTaskMapper);
    }
}
