package cn.iocoder.yudao.module.mes.service.pro.batchrecordcelllink;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionPickListDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionPickListItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionPickListItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionPickListMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductBomDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesRouteDccProjectBindingDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductBomMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesRouteDccProjectBindingMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProductionPickListSourceServiceImplTest {

    @Mock private MesRouteDccProjectBindingMapper routeDccProjectBindingMapper;
    @Mock private MesProRouteProductMapper routeProductMapper;
    @Mock private MesProRouteProductBomMapper routeProductBomMapper;
    @Mock private MesProRouteProcessMapper routeProcessMapper;
    @Mock private MesMdItemMapper itemMapper;
    @Mock private ErpKingdeeProductionPickListMapper pickListMapper;
    @Mock private ErpKingdeeProductionPickListItemMapper pickListItemMapper;

    private MesProductionPickListSourceService service;

    @BeforeEach
    void setUp() {
        service = new MesProductionPickListSourceServiceImpl(routeDccProjectBindingMapper, routeProductMapper,
                routeProductBomMapper, routeProcessMapper, itemMapper, pickListMapper, pickListItemMapper);
    }

    @Test
    void listSourceFields_exposesCurrentRouteProcessBomMaterials() {
        when(routeDccProjectBindingMapper.selectCurrentByRouteId(7001L))
                .thenReturn(MesRouteDccProjectBindingDO.builder().routeId(7001L).dccProjectCodeId(8001L).build());
        when(routeProductMapper.selectListByRouteId(7001L))
                .thenReturn(List.of(MesProRouteProductDO.builder().routeId(7001L).itemId(3101L).build()));
        when(routeProcessMapper.selectListByRouteId(7001L))
                .thenReturn(List.of(MesProRouteProcessDO.builder().id(5001L).routeId(7001L).processId(6001L).build()));
        when(routeProductBomMapper.selectListByRouteIdAndProductId(7001L, 3101L))
                .thenReturn(List.of(MesProRouteProductBomDO.builder()
                        .routeId(7001L).processId(6001L).productId(3101L).itemId(3201L).build()));
        when(itemMapper.selectListByIds(List.of(3201L)))
                .thenReturn(List.of(MesMdItemDO.builder().id(3201L).code("MAT-001").name("手柄").build()));

        List<MesProductionPickListSourceService.SourceField> fields = service.listSourceFields(7001L);

        assertEquals(8, fields.size());
        assertEquals(1, fields.stream().filter(field -> "material.3201.lotNumber".equals(field.fieldCode())
                && "手柄（MAT-001）- 物料批次号".equals(field.fieldName())
                && Long.valueOf(5001L).equals(field.routeProcessId())).count());
    }

    @Test
    void resolveValue_usesFirstFormalEntryOfUniqueApprovedPickList() {
        mockReleaseIdentity();
        when(pickListItemMapper.selectListByProductionOrderNo("MO-9001")).thenReturn(List.of(
                pickItem(9102L, "20", "LOT-SECOND"), pickItem(9101L, "10", "LOT-FIRST")));
        when(pickListMapper.selectBatchIds(List.of(9001L))).thenReturn(List.of(pickList(9001L, "C")));

        MesProductionPickListSourceService.ResolvedValue result = service.resolveValue(
                new MesProductionPickListSourceService.ResolveCommand(7001L, 5001L, 3101L, 8001L,
                        "MO-9001", "material.3201.lotNumber"));

        assertEquals("LOT-FIRST", result.value());
        assertEquals(9001L, result.pickListId());
        assertEquals(9101L, result.pickListItemId());
    }

    @Test
    void resolveValue_rejectsMoreThanOneApprovedPickListBeforeChoosingMaterial() {
        mockReleaseIdentity();
        ErpKingdeeProductionPickListItemDO second = pickItem(9201L, "10", "LOT-B")
                .setProductionPickListId(9002L).setSourceBillNo("PICK-002");
        when(pickListItemMapper.selectListByProductionOrderNo("MO-9001"))
                .thenReturn(List.of(pickItem(9101L, "10", "LOT-A"), second));
        when(pickListMapper.selectBatchIds(List.of(9001L, 9002L)))
                .thenReturn(List.of(pickList(9001L, "C"), pickList(9002L, "C")));

        assertThrows(ServiceException.class, () -> service.resolveValue(
                new MesProductionPickListSourceService.ResolveCommand(7001L, 5001L, 3101L, 8001L,
                        "MO-9001", "material.3201.lotNumber")));
    }

    private void mockReleaseIdentity() {
        when(routeDccProjectBindingMapper.selectCurrentByRouteId(7001L))
                .thenReturn(MesRouteDccProjectBindingDO.builder().routeId(7001L).dccProjectCodeId(8001L).build());
        when(routeProductMapper.selectByRouteIdAndItemId(7001L, 3101L))
                .thenReturn(MesProRouteProductDO.builder().routeId(7001L).itemId(3101L).build());
        when(routeProcessMapper.selectById(5001L))
                .thenReturn(MesProRouteProcessDO.builder().id(5001L).routeId(7001L).processId(6001L).build());
        when(routeProductBomMapper.selectList(7001L, 6001L, 3101L))
                .thenReturn(List.of(MesProRouteProductBomDO.builder()
                        .routeId(7001L).processId(6001L).productId(3101L).itemId(3201L).build()));
        when(itemMapper.selectById(3201L))
                .thenReturn(MesMdItemDO.builder().id(3201L).code("MAT-001").name("手柄").build());
    }

    private ErpKingdeeProductionPickListDO pickList(Long id, String status) {
        return ErpKingdeeProductionPickListDO.builder()
                .id(id).sourceFormId("PRD_PickMtrl").sourceFid(String.valueOf(id))
                .sourceBillNo("PICK-" + id).documentStatus(status)
                .billDate(LocalDateTime.of(2026, 8, 15, 8, 0)).build();
    }

    private ErpKingdeeProductionPickListItemDO pickItem(Long id, String entryId, String lotNumber) {
        return ErpKingdeeProductionPickListItemDO.builder()
                .id(id).productionPickListId(9001L).sourceFormId("PRD_PickMtrl")
                .sourceFid("9001").sourceEntryId(entryId).sourceLineKey("9001:" + entryId)
                .sourceBillNo("PICK-001").materialNumber("MAT-001").materialName("手柄")
                .unitName("个").actualQuantity(new BigDecimal("5")).requestedQuantity(new BigDecimal("6"))
                .lotNumber(lotNumber).productionOrderNo("MO-9001").build();
    }
}
