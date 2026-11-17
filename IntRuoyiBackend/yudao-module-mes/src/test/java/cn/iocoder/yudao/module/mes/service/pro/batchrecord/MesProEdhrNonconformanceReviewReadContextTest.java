package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrNonconformanceReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrNonconformanceReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderDetail;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderDetailService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MesProEdhrNonconformanceReviewReadContextTest {
    @Mock MesProEdhrNonconformanceReviewMapper reviewMapper;
    @Mock MesProcessPoolActiveOrderMapper activeOrderMapper;
    @Mock MesTeamLeaderActiveOrderDetailService activeOrderDetailService;
    @InjectMocks MesProEdhrNonconformanceReviewServiceImpl service;
    @BeforeEach void tenant() { TenantContextHolder.setTenantId(1L); }
    @AfterEach void clear() { TenantContextHolder.clear(); }
    private MesProEdhrNonconformanceReviewDO review() {
        return new MesProEdhrNonconformanceReviewDO().setId(501L).setActiveOrderId(409L).setWorkOrderId(201L).setTenantId(1L);
    }
    private MesProcessPoolActiveOrderDO archivedOrder() {
        var order = new MesProcessPoolActiveOrderDO().setId(409L).setWorkOrderId(201L).setBusinessStatus("REWORKED");
        order.setTenantId(1L);
        order.setDeleted(true);
        return order;
    }
    @Test void replacedCycleRemainsReadOnlyAndUsesArchivedProjection() {
        when(reviewMapper.selectById(501L)).thenReturn(review());
        when(activeOrderMapper.selectByIdIgnoreDeleted(409L)).thenReturn(archivedOrder());
        var detail = new MesTeamLeaderActiveOrderDetail().setActiveOrderId(409L);
        when(activeOrderDetailService.getArchivedFormalDetail(409L)).thenReturn(detail);
        when(activeOrderDetailService.getArchivedSignatureDetail(409L, 742L)).thenReturn(detail);
        assertSame(detail, service.getActiveOrderDetail(501L));
        assertSame(detail, service.getActiveOrderSignatureDetail(501L, 742L));
        verify(activeOrderDetailService, never()).getFormalDetail(anyLong());
        verify(activeOrderDetailService, never()).getDetail(anyLong(), anyLong());
    }
    @Test void wrongWorkOrderOrTenantIsRejectedBeforeDetailRead() {
        when(reviewMapper.selectById(501L)).thenReturn(review());
        var order = archivedOrder().setWorkOrderId(202L);
        when(activeOrderMapper.selectByIdIgnoreDeleted(409L)).thenReturn(order);
        assertThrows(RuntimeException.class, () -> service.getActiveOrderSignatureDetail(501L, 742L));
        order.setWorkOrderId(201L); order.setTenantId(2L);
        assertThrows(RuntimeException.class, () -> service.getActiveOrderDetail(501L));
        order.setTenantId(1L);
        when(reviewMapper.selectById(501L)).thenReturn(review().setTenantId(2L));
        assertThrows(RuntimeException.class, () -> service.getActiveOrderSignatureDetail(501L, 742L));
        verifyNoInteractions(activeOrderDetailService);
    }
    @Test void missingReviewFailsBeforeAnyOrderRead() {
        assertThrows(RuntimeException.class, () -> service.getActiveOrderSignatureDetail(501L, 742L));
        verifyNoInteractions(activeOrderMapper, activeOrderDetailService);
    }
}
