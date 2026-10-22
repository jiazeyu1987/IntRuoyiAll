package cn.iocoder.yudao.module.mes.controller.admin.pro.gxpaudit;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.MesProEdhrBatchExecutionController;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchActiveOrderDetailService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderDetail;
import cn.iocoder.yudao.module.system.controller.admin.gxpaudit.vo.GxpAuditEventPageReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditEventDO;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditEventRelationDO;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MesBatchAuditSourceSelectionTest {
    private final MesProEdhrBatchExecutionController controller = new MesProEdhrBatchExecutionController();
    private final MesProEdhrBatchActiveOrderDetailService details = mock(MesProEdhrBatchActiveOrderDetailService.class);
    private final GxpAuditQueryService audits = mock(GxpAuditQueryService.class);

    @BeforeEach
    void inject() {
        ReflectionTestUtils.setField(controller, "batchActiveOrderDetailService", details);
        ReflectionTestUtils.setField(controller, "gxpAuditQueryService", audits);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void activeOrderInputUsesTheSamePriorityAsDetail(boolean includeBatch) {
        when(details.getDetailByActiveOrderId(42L)).thenReturn(new MesTeamLeaderActiveOrderDetail().setActiveOrderId(42L));
        when(audits.page(any())).thenReturn(new PageResult<>(List.of(), 0L));
        controller.auditPage(new GxpAuditEventPageReqVO(), includeBatch ? 99L : null, 42L);
        controller.auditGet(includeBatch ? 99L : null, 7L, 42L);
        verify(details, times(2)).getDetailByActiveOrderId(42L);
        verify(details, never()).getDetail(any());
        verify(audits).page(argThat(query -> "ACTIVE_ORDER".equals(query.getScopeType()) && Long.valueOf(42L).equals(query.getScopeId())));
    }

    @Test
    void batchInputResolvesItsFormalActiveOrder() {
        when(details.getDetail(99L)).thenReturn(new MesTeamLeaderActiveOrderDetail().setActiveOrderId(42L));
        when(audits.page(any())).thenReturn(new PageResult<>(List.of(), 0L));
        controller.auditPage(new GxpAuditEventPageReqVO(), 99L, null);
        controller.auditGet(99L, 7L, null);
        verify(details, times(2)).getDetail(99L);
        verify(details, never()).getDetailByActiveOrderId(any());
        verify(audits).page(argThat(query -> Long.valueOf(42L).equals(query.getScopeId())));
    }

    @Test
    void sourceFailureStopsQueries() {
        var failure = new IllegalArgumentException("invalid source");
        when(details.getDetailByActiveOrderId(42L)).thenThrow(failure);
        assertSame(failure, assertThrows(IllegalArgumentException.class,
                () -> controller.auditPage(new GxpAuditEventPageReqVO(), 99L, 42L)));
        assertSame(failure, assertThrows(IllegalArgumentException.class,
                () -> controller.auditGet(99L, 7L, 42L)));
        verifyNoInteractions(audits);
        verify(details, never()).getDetail(any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void eventMustBelongToTheResolvedActiveOrder(boolean matches) {
        when(details.getDetailByActiveOrderId(42L)).thenReturn(new MesTeamLeaderActiveOrderDetail().setActiveOrderId(42L));
        when(audits.get(7L)).thenReturn(new GxpAuditEventDO().setId(7L));
        when(audits.listRelations(7L)).thenReturn(List.of(new GxpAuditEventRelationDO()
                .setTargetType("ACTIVE_ORDER").setTargetId(matches ? "42" : "99")));
        var response = controller.auditGet(null, 7L, 42L);
        if (matches) {
            assertEquals(7L, response.getData().getId());
        } else {
            assertNull(response.getData());
        }
    }
}
