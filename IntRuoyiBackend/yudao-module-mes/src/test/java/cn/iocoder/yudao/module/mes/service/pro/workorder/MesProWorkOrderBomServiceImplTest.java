package cn.iocoder.yudao.module.mes.service.pro.workorder;

import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdProductBomDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderBomDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderBomMapper;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdProductBomService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProWorkOrderBomServiceImplTest {

    @InjectMocks
    private MesProWorkOrderBomServiceImpl service;

    @Mock
    private MesProWorkOrderBomMapper workOrderBomMapper;
    @Mock
    private MesProWorkOrderService workOrderService;
    @Mock
    private MesMdItemService itemService;
    @Mock
    private MesMdProductBomService productBomService;

    @Test
    void getWorkOrderMaterialDemandByWorkOrderId_shouldExpandToLeafItems() {
        when(workOrderBomMapper.selectListByWorkOrderId(1L)).thenReturn(List.of(
                MesProWorkOrderBomDO.builder().workOrderId(1L).itemId(10L).quantity(new BigDecimal("2")).build(),
                MesProWorkOrderBomDO.builder().workOrderId(1L).itemId(50L).quantity(BigDecimal.ONE).build()
        ));
        when(productBomService.getProductBomListByItemIds(anyCollection())).thenAnswer(invocation -> {
            Collection<Long> itemIds = invocation.getArgument(0);
            List<MesMdProductBomDO> result = new ArrayList<>();
            if (itemIds.contains(10L)) {
                result.add(MesMdProductBomDO.builder().itemId(10L).bomItemId(20L).quantity(new BigDecimal("3")).build());
                result.add(MesMdProductBomDO.builder().itemId(10L).bomItemId(30L).quantity(BigDecimal.ONE).build());
            }
            if (itemIds.contains(30L)) {
                result.add(MesMdProductBomDO.builder().itemId(30L).bomItemId(40L).quantity(new BigDecimal("2")).build());
            }
            return result;
        });

        Map<Long, BigDecimal> demand = service.getWorkOrderMaterialDemandByWorkOrderId(1L);

        assertEquals(3, demand.size());
        assertEquals(0, new BigDecimal("6").compareTo(demand.get(20L)));
        assertEquals(0, new BigDecimal("4").compareTo(demand.get(40L)));
        assertEquals(0, BigDecimal.ONE.compareTo(demand.get(50L)));
    }

    @Test
    void getWorkOrderMaterialDemandMapByWorkOrderIds_shouldKeepRequestedIdsWithoutDemand() {
        when(workOrderBomMapper.selectListByWorkOrderIds(eq(List.of(1L, 2L, 3L)))).thenReturn(List.of(
                MesProWorkOrderBomDO.builder().workOrderId(1L).itemId(10L).quantity(new BigDecimal("2")).build(),
                MesProWorkOrderBomDO.builder().workOrderId(2L).itemId(60L).quantity(new BigDecimal("3")).build()
        ));
        when(productBomService.getProductBomListByItemIds(anyCollection())).thenAnswer(invocation -> {
            Collection<Long> itemIds = invocation.getArgument(0);
            if (itemIds.contains(10L)) {
                return List.of(MesMdProductBomDO.builder().itemId(10L).bomItemId(20L).quantity(new BigDecimal("2")).build());
            }
            return Collections.emptyList();
        });

        Map<Long, Map<Long, BigDecimal>> demandMap = service.getWorkOrderMaterialDemandMapByWorkOrderIds(List.of(1L, 2L, 3L));

        assertEquals(3, demandMap.size());
        assertEquals(0, new BigDecimal("4").compareTo(demandMap.get(1L).get(20L)));
        assertEquals(0, new BigDecimal("3").compareTo(demandMap.get(2L).get(60L)));
        assertTrue(demandMap.get(3L).isEmpty());
    }
}
