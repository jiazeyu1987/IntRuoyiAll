package cn.iocoder.yudao.module.erp.service.sync.runtime;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.dal.dataobject.sync.ErpKingdeeSyncRunDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sync.ErpKingdeeSyncRunMapper;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.redisson.api.RLock;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ErpKingdeeSyncCancellationTest {
    @Test
    void productWritersShareTenantLockAndReleaseTypeLockWhenAnotherWriterIsRunning() {
        RedissonClient redis = mock(RedissonClient.class);
        RLock product = mock(RLock.class);
        RLock stock = mock(RLock.class);
        RLock shared = mock(RLock.class);
        TenantContextHolder.setTenantId(186L);
        try {
            when(redis.getLock("erp:kingdee:execution:186:PRODUCT")).thenReturn(product);
            when(redis.getLock("erp:kingdee:execution:186:STOCK")).thenReturn(stock);
            when(redis.getLock("erp:kingdee:execution:186:product-writers")).thenReturn(shared);
            when(product.tryLock()).thenReturn(true);
            when(stock.tryLock()).thenReturn(true);
            when(shared.tryLock()).thenReturn(true, false);
            ErpKingdeeSyncCancellation cancellation = new ErpKingdeeSyncCancellation(mock(ErpKingdeeSyncRunMapper.class), redis);
            cancellation.lock("PRODUCT");
            assertThrows(IllegalStateException.class, () -> cancellation.lock("STOCK"));
            verify(stock).unlock();
            verify(shared, never()).unlock();
            cancellation.unlock("PRODUCT");
            verify(shared).unlock();
            verify(product).unlock();
        } finally { TenantContextHolder.clear(); }
    }

    @Test
    void owningNodeInterruptsOnlyItsExecutionAndClearsSignalBeforeThreadReuse() throws Exception {
        ErpKingdeeSyncRunMapper mapper = mock(ErpKingdeeSyncRunMapper.class);
        ErpKingdeeSyncCancellation cancellation = new ErpKingdeeSyncCancellation(mapper, mock(RedissonClient.class));
        when(mapper.selectById(7L)).thenAnswer(invocation -> {
            assertEquals(9L, TenantContextHolder.getRequiredTenantId());
            return ErpKingdeeSyncRunDO.builder().id(7L).status(40).build();
        });
        CountDownLatch registered = new CountDownLatch(1);
        AtomicBoolean interrupted = new AtomicBoolean();
        AtomicBoolean clean = new AtomicBoolean();
        Thread worker = new Thread(() -> {
            TenantContextHolder.setTenantId(9L);
            cancellation.register(7L);
            registered.countDown();
            try { Thread.sleep(10000); }
            catch (InterruptedException expected) { interrupted.set(true); Thread.currentThread().interrupt(); }
            finally { cancellation.unregister(7L); clean.set(!Thread.currentThread().isInterrupted()); TenantContextHolder.clear(); }
        });
        worker.start();
        try {
            assertTrue(registered.await(3, TimeUnit.SECONDS));
            TenantContextHolder.setTenantId(99L);
            cancellation.interruptRequestedExecutions();
            assertEquals(99L, TenantContextHolder.getRequiredTenantId());
            worker.join(3000);
            assertFalse(worker.isAlive());
            assertTrue(interrupted.get());
            assertTrue(clean.get());
            clearInvocations(mapper);
            cancellation.interruptRequestedExecutions();
            verifyNoInteractions(mapper);
        } finally { worker.interrupt(); worker.join(3000); TenantContextHolder.clear(); }
    }

    @Test
    void clusterLockIsScopedToTenantAndTypeAndContentionFailsExplicitly() {
        RedissonClient redis = mock(RedissonClient.class);
        RLock lock = mock(RLock.class);
        TenantContextHolder.setTenantId(9L);
        try {
            when(redis.getLock("erp:kingdee:execution:9:BOM")).thenReturn(lock);
            ErpKingdeeSyncCancellation cancellation = new ErpKingdeeSyncCancellation(mock(ErpKingdeeSyncRunMapper.class), redis);
            assertThrows(IllegalStateException.class, () -> cancellation.lock("BOM"));
            when(lock.tryLock()).thenReturn(true);
            cancellation.lock("BOM");
            cancellation.unlock("BOM");
            verify(lock).unlock();
        } finally { TenantContextHolder.clear(); }
    }
}
