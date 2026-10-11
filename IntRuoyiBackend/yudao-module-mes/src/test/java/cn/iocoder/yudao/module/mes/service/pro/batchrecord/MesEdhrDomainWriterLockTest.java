package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesEdhrDomainWriterLockTest {
    @Test void lockedReleaseRelationMustStillMatchLocatedTenantBatch() {
        var service = new MesProEdhrReleaseServiceImpl();
        var releases = mock(MesProEdhrReleaseTransactionMapper.class);
        var batches = mock(MesProEdhrBatchExecutionMapper.class);
        ReflectionTestUtils.setField(service, "releaseTransactionMapper", releases);
        ReflectionTestUtils.setField(service, "batchExecutionMapper", batches);
        when(releases.selectById(3L)).thenReturn(new MesProEdhrReleaseTransactionDO().setId(3L).setBatchExecutionId(2L));
        when(batches.selectByTenantIdAndIdForUpdate(1L, 2L)).thenReturn(
            new cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO().setId(2L).setTenantId(1L));
        when(releases.selectByIdForUpdate(3L)).thenReturn(new MesProEdhrReleaseTransactionDO().setId(3L).setBatchExecutionId(99L));
        TenantContextHolder.setTenantId(1L);
        try {
            assertThrows(RuntimeException.class, () -> ReflectionTestUtils.invokeMethod(service, "requireTransactionForUpdate", 3L));
            var order = inOrder(releases, batches);
            order.verify(releases).selectById(3L);
            order.verify(batches).selectByTenantIdAndIdForUpdate(1L, 2L);
            order.verify(releases).selectByIdForUpdate(3L);
            verify(releases, never()).updateById(any(MesProEdhrReleaseTransactionDO.class));
        } finally { TenantContextHolder.clear(); }
    }
    @Test void missingTenantBatchPreventsReleaseRowLock() {
        var service = new MesProEdhrReleaseServiceImpl();
        var releases = mock(MesProEdhrReleaseTransactionMapper.class);
        var batches = mock(MesProEdhrBatchExecutionMapper.class);
        ReflectionTestUtils.setField(service, "releaseTransactionMapper", releases);
        ReflectionTestUtils.setField(service, "batchExecutionMapper", batches);
        when(releases.selectById(3L)).thenReturn(new MesProEdhrReleaseTransactionDO().setId(3L).setBatchExecutionId(2L));
        TenantContextHolder.setTenantId(1L);
        try {
            assertThrows(RuntimeException.class, () -> ReflectionTestUtils.invokeMethod(service, "requireTransactionForUpdate", 3L));
            var order = inOrder(releases, batches);
            order.verify(releases).selectById(3L);
            order.verify(batches).selectByTenantIdAndIdForUpdate(1L, 2L);
            verify(releases, never()).selectByIdForUpdate(anyLong());
        } finally { TenantContextHolder.clear(); }
    }
}