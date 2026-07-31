package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProcessPoolFifoAllocationLineDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProcessPoolFifoAllocationLineMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MesProcessPoolFifoAllocationConcurrencyTest {

    @Test
    void shouldNotOverAllocateFragmentWhenConcurrentAllocationsReadExistingLines() throws Exception {
        MesProcessPoolFifoAllocationLineMapper mapper = mock(MesProcessPoolFifoAllocationLineMapper.class);
        List<MesProcessPoolFifoAllocationLineDO> persistedLines = Collections.synchronizedList(new ArrayList<>());
        when(mapper.selectListBySourceQuantityFragmentIdsForUpdate(anyCollection()))
                .thenAnswer(invocation -> List.copyOf(persistedLines));
        when(mapper.insertBatch(anyCollection())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Collection<MesProcessPoolFifoAllocationLineDO> newLines = invocation.getArgument(0, Collection.class);
            persistedLines.addAll(newLines);
            return Boolean.TRUE;
        });
        MesProcessPoolFifoAllocationService allocationService = new MesProcessPoolFifoAllocationService(mapper);
        CyclicBarrier startBarrier = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<MesProcessPoolFifoAllocationResult> first = executor.submit(() -> {
                startBarrier.await();
                return allocationService.allocate(commandForWorkOrder(10L, "WO-A"));
            });
            Future<MesProcessPoolFifoAllocationResult> second = executor.submit(() -> {
                startBarrier.await();
                return allocationService.allocate(commandForWorkOrder(20L, "WO-B"));
            });

            first.get();
            second.get();
        } finally {
            executor.shutdownNow();
        }

        BigDecimal totalAllocated = persistedLines.stream()
                .map(MesProcessPoolFifoAllocationLineDO::getAllocatedQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertAmount("100", totalAllocated);
        assertEquals(2, persistedLines.size());
    }

    private static MesProcessPoolFifoAllocationCommand commandForWorkOrder(Long workOrderId, String workOrderCode) {
        return MesProcessPoolFifoAllocationCommand.of("F7-CONCURRENT-" + workOrderId,
                List.of(MesProcessPoolAllocatableQuantityFragment.builder()
                        .processPoolId(100L)
                        .sourceEventId(1000L)
                        .sourceQuantityFragmentId(2000L)
                        .sourceRouteProcessId(3000L)
                        .sourceProcessId(4000L)
                        .quantity(new BigDecimal("100"))
                        .build()),
                List.of(MesProcessPoolFifoTargetWorkOrder.builder()
                        .workOrderId(workOrderId)
                        .workOrderCode(workOrderCode)
                        .plannedStartTime(LocalDateTime.of(2026, 8, 1, 8, 0).plusDays(workOrderId))
                        .targetRouteProcessId(5000L + workOrderId)
                        .targetProcessId(4000L)
                        .requiredQuantity(new BigDecimal("80"))
                        .alreadyAllocatedQuantity(BigDecimal.ZERO)
                        .build()));
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
