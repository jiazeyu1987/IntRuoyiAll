package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProcessPoolFifoAllocationLineMapper;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProcessPoolAllocatedFragmentLockTest {

    @Mock
    private MesProcessPoolFifoAllocationLineMapper allocationLineMapper;

    private MesProcessPoolFifoAllocationService allocationService;

    @BeforeEach
    void setUp() {
        allocationService = new MesProcessPoolFifoAllocationService(allocationLineMapper);
    }

    @Test
    void shouldRejectMutatingAllocatedFragmentQuantityQualityOrAllocatableFields() {
        when(allocationLineMapper.selectCountBySourceQuantityFragmentId(3000L)).thenReturn(1L);

        for (MesProcessPoolFragmentOriginalField field : List.of(
                MesProcessPoolFragmentOriginalField.OUTPUT_QUANTITY,
                MesProcessPoolFragmentOriginalField.QUALITY_STATUS,
                MesProcessPoolFragmentOriginalField.ALLOCATABLE_STATUS)) {
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> allocationService.validateOriginalFieldMutationAllowed(3000L, field));
            assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_FIFO_ALLOCATED_FRAGMENT_LOCKED.getCode(), ex.getCode());
        }
    }

    @Test
    void shouldAllowMutatingNonAllocationAffectingOriginalFields() {
        when(allocationLineMapper.selectCountBySourceQuantityFragmentId(3000L)).thenReturn(1L);

        assertDoesNotThrow(() -> allocationService.validateOriginalFieldMutationAllowed(
                3000L, MesProcessPoolFragmentOriginalField.REMARK));
    }
}
