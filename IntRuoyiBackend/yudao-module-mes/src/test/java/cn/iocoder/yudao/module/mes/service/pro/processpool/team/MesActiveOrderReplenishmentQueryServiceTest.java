package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.*;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MesActiveOrderReplenishmentQueryServiceTest {
    @Mock MesProcessPoolActiveOrderMapper orders;
    @Mock MesProWorkOrderMapper workOrders;
    @Mock ErpKingdeeProductionReplenishmentListItemMapper items;
    @Mock ErpKingdeeProductionReplenishmentListMapper headers;
    @Mock MesPackagingLeaderAccess packaging;
    MesActiveOrderReplenishmentQueryService service;

    @BeforeEach void setup() {
        service = new MesActiveOrderReplenishmentQueryService(orders, workOrders, items, headers, packaging);
        lenient().when(workOrders.selectById(30L)).thenReturn(MesProWorkOrderDO.builder().id(30L).code("MO-30").build());
    }
    private ErpKingdeeProductionReplenishmentListItemDO item(long id, long header, String code, String lot, String qty) {
        return ErpKingdeeProductionReplenishmentListItemDO.builder().id(id).productionReplenishmentListId(header)
                .productionOrderNo("MO-30").materialNumber(code).materialName("材料" + code).lotNumber(lot)
                .actualQuantity(qty == null ? null : new BigDecimal(qty)).build();
    }
    private ErpKingdeeProductionReplenishmentListDO header(long id, String status) {
        return ErpKingdeeProductionReplenishmentListDO.builder().id(id).sourceBillNo("BL-" + id).documentStatus(status).build();
    }
    @Test void listsAllBillsAndLotsWithoutAnyProcessOrMaterialConfiguration() {
        when(orders.selectById(10L)).thenReturn(MesProcessPoolActiveOrderDO.builder().id(10L).leaderUserId(20L).workOrderId(30L).build());
        when(items.selectListByProductionOrderNo("MO-30")).thenReturn(List.of(
                item(1, 91, "LABEL", "LOT-1", "510"), item(2, 91, "PAPER", "LOT-2", "151"),
                item(3, 92, "LABEL", "LOT-3", "150"), item(4, 92, "PAPER", "LOT-4", "174")));
        when(headers.selectBatchIds(List.of(91L, 92L))).thenReturn(List.of(header(91, "C"), header(92, "C")));
        var lines = service.list(20L, 10L);
        assertEquals(List.of("BL-91", "BL-91", "BL-92", "BL-92"), lines.stream().map(MesActiveOrderReplenishmentQueryService.Line::billNo).toList());
        assertEquals(List.of("LOT-1", "LOT-2", "LOT-3", "LOT-4"), lines.stream().map(MesActiveOrderReplenishmentQueryService.Line::lotNumber).toList());
        assertEquals(new BigDecimal("510"), lines.get(0).actualQuantity());
        verifyNoInteractions(packaging);
    }
    @Test void displayPreservesDraftStatusAndAbsentActualQuantity() {
        when(items.selectListByProductionOrderNo("MO-30")).thenReturn(List.of(item(1, 91, "LABEL", "LOT-1", null)));
        when(headers.selectBatchIds(List.of(91L))).thenReturn(List.of(header(91, "A")));
        var line = service.readSources(30L).get(0);
        assertEquals("A", line.documentStatus());
        assertNull(line.actualQuantity());
    }
    @Test void quantityChangeUpdatesFrozenEvidenceHash() {
        when(items.selectListByProductionOrderNo("MO-30")).thenReturn(List.of(item(1, 91, "LABEL", "LOT-1", "5")), List.of(item(1, 91, "LABEL", "LOT-1", "6")));
        when(headers.selectBatchIds(List.of(91L))).thenReturn(List.of(header(91, "C")));
        assertNotEquals(service.readSources(30L).get(0).sourceHash(), service.readSources(30L).get(0).sourceHash());
    }
    @Test void anotherLeaderCannotReadOrderSources() {
        when(orders.selectById(10L)).thenReturn(MesProcessPoolActiveOrderDO.builder().id(10L).leaderUserId(20L).workOrderId(30L).build());
        assertThrows(ServiceException.class, () -> service.list(21L, 10L));
        verifyNoInteractions(items, headers);
    }
    @Test void absentBillsAreAnEmptyList() {
        when(items.selectListByProductionOrderNo("MO-30")).thenReturn(List.of());
        assertTrue(service.readSources(30L).isEmpty());
        verifyNoInteractions(headers);
    }
    @Test void missingHeaderIsReportedInsteadOfHidingSourceRows() {
        when(items.selectListByProductionOrderNo("MO-30")).thenReturn(List.of(item(1, 91, "LABEL", "LOT-1", "5")));
        when(headers.selectBatchIds(List.of(91L))).thenReturn(List.of());
        assertThrows(ServiceException.class, () -> service.readSources(30L));
    }
    @Test void foreignOrderItemIsRejected() {
        when(items.selectListByProductionOrderNo("MO-30")).thenReturn(List.of(item(1, 91, "LABEL", "LOT-1", "5").setProductionOrderNo("MO-OTHER")));
        assertThrows(ServiceException.class, () -> service.readSources(30L));
    }
}
