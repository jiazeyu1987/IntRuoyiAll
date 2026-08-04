package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventRevisionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventRevisionDiffMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventRevisionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProcessPoolEventRevisionFifoLockTest {

    @Mock
    private MesProProcessPoolEventMapper eventMapper;
    @Mock
    private MesProProcessPoolEventRevisionMapper revisionMapper;
    @Mock
    private MesProProcessPoolEventRevisionDiffMapper revisionDiffMapper;
    @Mock
    private MesProcessPoolFifoAllocationService fifoAllocationService;
    @Mock
    private MesProcessPoolSubmissionReviewMapper submissionReviewMapper;

    private MesProcessPoolEventRevisionService service;

    @BeforeEach
    void setUp() {
        service = new MesProcessPoolEventRevisionServiceImpl(eventMapper, revisionMapper,
                revisionDiffMapper, fifoAllocationService, submissionReviewMapper);
    }

    @Test
    void rejectsQuantityFieldUpdateWhenFragmentAllocated() {
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(MesProcessPoolEventRevisionServiceTest.event());
        when(submissionReviewMapper.selectLatestByEventIdForUpdate(1001L))
                .thenReturn(MesProcessPoolEventRevisionServiceTest.rejectedReview());
        doThrow(exception(ErrorCodeConstants.PRO_PROCESS_POOL_FIFO_ALLOCATED_FRAGMENT_LOCKED, 8101L))
                .when(fifoAllocationService)
                .validateOriginalFieldMutationAllowed(8101L, MesProcessPoolFragmentOriginalField.OUTPUT_QUANTITY);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.updateOriginalRecord(quantityReq(8101L)));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_FIFO_ALLOCATED_FRAGMENT_LOCKED.getCode(), ex.getCode());
        verify(revisionMapper, never()).insert(any(MesProProcessPoolEventRevisionDO.class));
        verify(eventMapper, never()).updateById(any(MesProProcessPoolEventDO.class));
    }

    @Test
    void rejectsUpdateWhenFifoLockStatusCannotBeConfirmed() {
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(MesProcessPoolEventRevisionServiceTest.event());
        when(submissionReviewMapper.selectLatestByEventIdForUpdate(1001L))
                .thenReturn(MesProcessPoolEventRevisionServiceTest.rejectedReview());

        ServiceException ex = assertThrows(ServiceException.class, () -> service.updateOriginalRecord(quantityReq(null)));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_REVISION_FIFO_LOCK_STATUS_UNKNOWN.getCode(), ex.getCode());
        verify(fifoAllocationService, never()).validateOriginalFieldMutationAllowed(
                any(), any(MesProcessPoolFragmentOriginalField.class));
        verify(revisionMapper, never()).insert(any(MesProProcessPoolEventRevisionDO.class));
        verify(eventMapper, never()).updateById(any(MesProProcessPoolEventDO.class));
    }

    private static MesProcessPoolEventRevisionUpdateReqBO quantityReq(Long sourceQuantityFragmentId) {
        return MesProcessPoolEventRevisionUpdateReqBO.builder()
                .eventId(1001L)
                .afterPayload("{\"outputQuantity\":12}")
                .changeReason("更正产出数量")
                .revisionSignatureId(9102L)
                .revisionSignatureUserId(2001L)
                .revisionSignatureSnapshot("{\"signedBy\":\"张可莹\"}")
                .modifiedByUserId(2001L)
                .changedFields(List.of(MesProcessPoolEventRevisionFieldChangeBO.builder()
                        .fieldCode("outputQuantity")
                        .fieldName("输出数量")
                        .beforeValue("10")
                        .afterValue("12")
                        .affectsQuantityFragment(true)
                        .sourceQuantityFragmentId(sourceQuantityFragmentId)
                        .originalField(MesProcessPoolFragmentOriginalField.OUTPUT_QUANTITY)
                        .build()))
                .build();
    }
}
