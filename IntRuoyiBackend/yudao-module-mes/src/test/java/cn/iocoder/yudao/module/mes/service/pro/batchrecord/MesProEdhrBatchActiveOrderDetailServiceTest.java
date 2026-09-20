package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderDetail;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderDetailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProEdhrBatchActiveOrderDetailServiceTest {

    @Mock
    private MesProEdhrBatchExecutionService batchExecutionService;
    @Mock
    private MesProcessPoolActiveOrderMapper activeOrderMapper;
    @Mock
    private MesTeamLeaderActiveOrderDetailService detailService;
    @InjectMocks
    private MesProEdhrBatchActiveOrderDetailService service;

    @Test
    void getDetail_usesBatchArchivedFormalSource() {
        EdhrBatchExecutionRespVO batch = new EdhrBatchExecutionRespVO()
                .setId(100L)
                .setWorkOrderId(200L)
                .setActiveOrderId(300L);
        MesProcessPoolActiveOrderDO activeOrder = new MesProcessPoolActiveOrderDO()
                .setId(300L)
                .setLeaderUserId(400L)
                .setWorkOrderId(200L);
        MesTeamLeaderActiveOrderDetail detail = new MesTeamLeaderActiveOrderDetail()
                .setActiveOrderId(300L);
        when(batchExecutionService.get(100L)).thenReturn(batch);
        when(activeOrderMapper.selectByIdIgnoreDeleted(300L)).thenReturn(activeOrder);
        when(detailService.getArchivedFormalDetail(300L)).thenReturn(detail);

        assertSame(detail, service.getDetail(100L));

        verify(batchExecutionService).get(100L);
        verify(activeOrderMapper).selectByIdIgnoreDeleted(300L);
        verify(detailService).getArchivedFormalDetail(300L);
    }

    @Test
    void getDetail_failsWhenBatchHasNoFormalActiveOrderSource() {
        when(batchExecutionService.get(100L)).thenReturn(new EdhrBatchExecutionRespVO()
                .setId(100L)
                .setWorkOrderId(200L));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.getDetail(100L));

        assertEquals("EDHR_BATCH_ACTIVE_ORDER_SOURCE_MISSING", error.getMessage());
    }

    @Test
    void getDetail_failsWhenActiveOrderDoesNotBelongToBatchWorkOrder() {
        when(batchExecutionService.get(100L)).thenReturn(new EdhrBatchExecutionRespVO()
                .setId(100L)
                .setWorkOrderId(200L)
                .setActiveOrderId(300L));
        when(activeOrderMapper.selectByIdIgnoreDeleted(300L)).thenReturn(new MesProcessPoolActiveOrderDO()
                .setId(300L)
                .setLeaderUserId(400L)
                .setWorkOrderId(999L));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.getDetail(100L));

        assertEquals("EDHR_BATCH_ACTIVE_ORDER_SOURCE_INVALID", error.getMessage());
    }
}
