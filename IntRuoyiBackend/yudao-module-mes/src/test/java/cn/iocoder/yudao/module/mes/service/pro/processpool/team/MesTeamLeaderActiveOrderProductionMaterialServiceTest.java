package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.kingdee.MesKingdeeProductionMaterialListPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.kingdee.MesKingdeeProductionMaterialListRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.service.pro.workorder.kingdee.MesKingdeeProductionMaterialListQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_NOT_EXISTS;

@ExtendWith(MockitoExtension.class)
class MesTeamLeaderActiveOrderProductionMaterialServiceTest {
    @Mock MesTeamLeaderActiveOrderDetailService detailService;
    @Mock MesKingdeeProductionMaterialListQueryService materialQueryService;
    @InjectMocks MesTeamLeaderActiveOrderProductionMaterialService service;

    @Test
    void ownerReadsOnlyFormalOrderAndAllPagesIncludingSecondPage() {
        when(detailService.getDetail(341L, 409L)).thenReturn(detail("MO-409"));
        List<MesKingdeeProductionMaterialListRespVO> first = IntStream.rangeClosed(1, 100)
                .mapToObj(id -> row(id, "MO-409")).toList();
        when(materialQueryService.getPage(any())).thenAnswer(invocation -> {
            MesKingdeeProductionMaterialListPageReqVO request = invocation.getArgument(0);
            assertEquals("MO-409", request.getProductionOrderNo());
            assertEquals(100, request.getPageSize());
            assertNull(request.getSourceBillNo());
            assertNull(request.getProductCode());
            assertNull(request.getChildMaterialCode());
            return request.getPageNo() == 1 ? new PageResult<>(first, 101L)
                    : new PageResult<>(List.of(row(101, "MO-409")), 101L);
        });
        var result = service.getList(341L, 409L);
        assertEquals(101, result.size());
        assertEquals(101L, result.get(100).getId());
        var order = inOrder(detailService, materialQueryService);
        order.verify(detailService).getDetail(341L, 409L);
        order.verify(materialQueryService, times(2)).getPage(any());
        verifyNoMoreInteractions(materialQueryService);
    }

    @Test
    void actualDetailGuardRejectsCrossOwnerMissingAndInactiveOrdersBeforeMaterialRead() {
        var mapper = mock(MesProcessPoolActiveOrderMapper.class);
        var formalDetailService = mock(MesTeamLeaderActiveOrderDetailServiceImpl.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(formalDetailService, "activeOrderMapper", mapper);
        var guardedService = new MesTeamLeaderActiveOrderProductionMaterialService(formalDetailService, materialQueryService);
        when(mapper.selectById(409L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(409L).leaderUserId(341L).activeStatus("ACTIVE").build());
        var crossOwner = assertThrows(ServiceException.class, () -> guardedService.getList(342L, 409L));
        assertEquals(PRO_PROCESS_POOL_ACTIVE_ORDER_NOT_EXISTS.getCode(), crossOwner.getCode());
        var missing = assertThrows(ServiceException.class, () -> guardedService.getList(341L, 999L));
        assertEquals(PRO_PROCESS_POOL_ACTIVE_ORDER_NOT_EXISTS.getCode(), missing.getCode());
        when(mapper.selectById(409L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(409L).leaderUserId(341L).activeStatus("REMOVED").build());
        var inactive = assertThrows(ServiceException.class, () -> guardedService.getList(341L, 409L));
        assertEquals(PRO_PROCESS_POOL_ACTIVE_ORDER_NOT_EXISTS.getCode(), inactive.getCode());
        verifyNoInteractions(materialQueryService);
    }

    @Test
    void missingFormalOrderNumberCannotRemoveFilterAndReadWholeLibrary() {
        when(detailService.getDetail(341L, 409L)).thenReturn(detail("  "));
        assertThrows(IllegalStateException.class, () -> service.getList(341L, 409L));
        verifyNoInteractions(materialQueryService);
    }

    @Test
    void materialFailurePropagatesInsteadOfReturningEmptySuccess() {
        when(detailService.getDetail(341L, 409L)).thenReturn(detail("MO-409"));
        var failure = new IllegalStateException("formal ERP query failed");
        when(materialQueryService.getPage(any())).thenThrow(failure);
        assertSame(failure, assertThrows(IllegalStateException.class, () -> service.getList(341L, 409L)));
    }

    @Test
    void secondPageFailureDoesNotReturnFirstPageAsSuccess() {
        when(detailService.getDetail(341L, 409L)).thenReturn(detail("MO-409"));
        var failure = new IllegalStateException("second ERP page failed");
        when(materialQueryService.getPage(any())).thenReturn(new PageResult<>(List.of(row(1, "MO-409")), 2L))
                .thenThrow(failure);
        assertSame(failure, assertThrows(IllegalStateException.class, () -> service.getList(341L, 409L)));
    }

    @Test
    void mismatchedOrderRowIsRejectedRatherThanLeaked() {
        when(detailService.getDetail(341L, 409L)).thenReturn(detail("MO-409"));
        when(materialQueryService.getPage(any())).thenReturn(new PageResult<>(List.of(row(1, "MO-OTHER")), 1L));
        assertThrows(IllegalStateException.class, () -> service.getList(341L, 409L));
    }

    @Test
    void optionalWorkOrderLinkAcceptsUnlinkedSyncRowButRejectsWrongAssociatedId() {
        when(detailService.getDetail(341L, 409L)).thenReturn(detail("MO-409"));
        var unlinked = row(1, "MO-409");
        unlinked.setWorkOrderId(null);
        var correctlyLinked = row(2, "MO-409");
        when(materialQueryService.getPage(any())).thenReturn(new PageResult<>(List.of(unlinked, correctlyLinked), 2L));
        assertEquals(List.of(unlinked, correctlyLinked), service.getList(341L, 409L));
        var wronglyLinked = row(3, "MO-409");
        wronglyLinked.setWorkOrderId(521L);
        when(materialQueryService.getPage(any())).thenReturn(new PageResult<>(List.of(wronglyLinked), 1L));
        var failure = assertThrows(IllegalStateException.class, () -> service.getList(341L, 409L));
        assertTrue(failure.getMessage().startsWith("ACTIVE_ORDER_PRODUCTION_MATERIAL_SOURCE_MISMATCH:"));
    }

    @Test
    void incompletePageAndChangingTotalAreRejected() {
        when(detailService.getDetail(341L, 409L)).thenReturn(detail("MO-409"));
        when(materialQueryService.getPage(any())).thenReturn(new PageResult<>(List.of(), 2L));
        assertThrows(IllegalStateException.class, () -> service.getList(341L, 409L));
        when(materialQueryService.getPage(any())).thenReturn(new PageResult<>(List.of(row(1, "MO-409")), 2L))
                .thenReturn(new PageResult<>(List.of(row(2, "MO-409")), 3L));
        assertThrows(IllegalStateException.class, () -> service.getList(341L, 409L));
    }

    @Test
    void duplicatePagesCannotPretendToBeComplete() {
        when(detailService.getDetail(341L, 409L)).thenReturn(detail("MO-409"));
        when(materialQueryService.getPage(any())).thenReturn(new PageResult<>(List.of(row(1, "MO-409")), 2L));
        assertThrows(IllegalStateException.class, () -> service.getList(341L, 409L));
    }

    @Test
    void successfulFormalZeroTotalIsReturnedAsEmpty() {
        when(detailService.getDetail(341L, 409L)).thenReturn(detail("MO-409"));
        when(materialQueryService.getPage(any())).thenReturn(new PageResult<>(List.of(), 0L));
        assertTrue(service.getList(341L, 409L).isEmpty());
        verify(materialQueryService).getPage(any());
    }

    private static MesTeamLeaderActiveOrderDetail detail(String orderNo) {
        return new MesTeamLeaderActiveOrderDetail().setActiveOrderId(409L).setWorkOrderId(520L).setWorkOrderCode(orderNo);
    }

    private static MesKingdeeProductionMaterialListRespVO row(long id, String orderNo) {
        var row = new MesKingdeeProductionMaterialListRespVO();
        row.setId(id);
        row.setProductionOrderNo(orderNo);
        row.setWorkOrderId(520L);
        return row;
    }
}
