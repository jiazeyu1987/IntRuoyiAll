package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionPickListDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionPickListItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionPickListItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionPickListMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesTeamLeaderActiveOrderPickListCompletionSourceServiceTest {

    @Mock private MesProWorkOrderMapper workOrderMapper;
    @Mock private ErpKingdeeProductionPickListMapper pickListMapper;
    @Mock private ErpKingdeeProductionPickListItemMapper pickListItemMapper;
    @Mock private MesProcessPoolActiveOrderPickListBindingMapper bindingMapper;
    @Mock private MesProcessPoolActiveOrderPickListBindingItemMapper bindingItemMapper;
    private MesTeamLeaderActiveOrderPickListCompletionSourceService service;

    @BeforeEach
    void setUp() {
        service = new MesTeamLeaderActiveOrderPickListCompletionSourceService(
                new MesFormalProductionPickListSourceResolver(workOrderMapper, pickListMapper, pickListItemMapper),
                bindingMapper, bindingItemMapper);
        org.mockito.Mockito.lenient().when(workOrderMapper.selectById(30L)).thenReturn(
                cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO.builder()
                        .id(30L).code("MO-001").build());
    }

    @Test
    void freezesEveryMatchingFormalPickList() {
        ErpKingdeeProductionPickListItemDO firstItem = item(101L, 1001L);
        ErpKingdeeProductionPickListItemDO secondItem = item(102L, 1002L);
        when(pickListItemMapper.selectListByProductionOrderNo("MO-001"))
                .thenReturn(List.of(firstItem, secondItem));
        when(pickListMapper.selectById(101L)).thenReturn(header(101L));
        when(pickListMapper.selectById(102L)).thenReturn(header(102L));
        MesProcessPoolActiveOrderPickListBindingDO first = binding(101L).setId(8101L)
                .setSourceSnapshotHash(MesFormalProductionPickListSourceResolver
                        .snapshotHash(header(101L), List.of(firstItem)));
        MesProcessPoolActiveOrderPickListBindingDO second = binding(102L).setId(8102L)
                .setSourceSnapshotHash(MesFormalProductionPickListSourceResolver
                        .snapshotHash(header(102L), List.of(secondItem)));
        when(bindingMapper.selectListByActiveOrderId(10L)).thenReturn(List.of(), List.of(first, second));
        when(bindingItemMapper.selectListByBindingId(8101L)).thenReturn(List.of(frozen(8101L, firstItem)));
        when(bindingItemMapper.selectListByBindingId(8102L)).thenReturn(List.of(frozen(8102L, secondItem)));
        when(bindingMapper.insert(any(MesProcessPoolActiveOrderPickListBindingDO.class))).thenReturn(1);
        when(bindingItemMapper.insert(any(cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingItemDO.class))).thenReturn(1);

        List<MesProcessPoolActiveOrderPickListBindingDO> result = service.freezeAll(order(), 20L, "complete-1");

        assertEquals(List.of(101L, 102L), result.stream()
                .map(MesProcessPoolActiveOrderPickListBindingDO::getPickListId).toList());
        verify(bindingMapper, org.mockito.Mockito.times(2)).insert(any(MesProcessPoolActiveOrderPickListBindingDO.class));
        verify(bindingItemMapper, org.mockito.Mockito.times(2)).insert(any(cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingItemDO.class));
    }

    @Test
    void missingFormalPickListFailsBeforeWriting() {
        when(pickListItemMapper.selectListByProductionOrderNo("MO-001")).thenReturn(List.of());
        assertThrows(ServiceException.class, () -> service.freezeAll(order(), 20L, "complete-1"));
        verify(bindingMapper, never()).insert(any(MesProcessPoolActiveOrderPickListBindingDO.class));
    }

    @Test
    void invalidSecondPickListFailsBeforeWritingAnyBinding() {
        when(pickListItemMapper.selectListByProductionOrderNo("MO-001"))
                .thenReturn(List.of(item(101L, 1001L), item(102L, 1002L)));
        when(pickListMapper.selectById(101L)).thenReturn(header(101L));
        when(pickListMapper.selectById(102L)).thenReturn(header(102L).setDocumentStatus("A"));

        assertThrows(ServiceException.class, () -> service.freezeAll(order(), 20L, "complete-1"));
        verify(bindingMapper, never()).insert(any(MesProcessPoolActiveOrderPickListBindingDO.class));
        verify(bindingItemMapper, never()).insert(any(cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingItemDO.class));
    }

    @Test
    void secondPickListQuantityChangeChangesSnapshotHash() {
        ErpKingdeeProductionPickListItemDO baseline = item(102L, 1002L)
                .setRequestedQuantity(new BigDecimal("8")).setBaseActualQuantity(new BigDecimal("7"));
        String baselineHash = MesFormalProductionPickListSourceResolver
                .snapshotHash(header(102L), List.of(baseline));

        String requestedChanged = MesFormalProductionPickListSourceResolver.snapshotHash(
                header(102L), List.of(item(102L, 1002L)
                        .setRequestedQuantity(new BigDecimal("9")).setBaseActualQuantity(new BigDecimal("7"))));
        String baseActualChanged = MesFormalProductionPickListSourceResolver.snapshotHash(
                header(102L), List.of(item(102L, 1002L)
                        .setRequestedQuantity(new BigDecimal("8")).setBaseActualQuantity(new BigDecimal("6"))));

        org.junit.jupiter.api.Assertions.assertNotEquals(baselineHash, requestedChanged);
        org.junit.jupiter.api.Assertions.assertNotEquals(baselineHash, baseActualChanged);
    }

    @Test
    void existingSecondBindingWithSameItemCountButChangedFrozenIdentityConflicts() {
        ErpKingdeeProductionPickListItemDO firstItem = item(101L, 1001L);
        ErpKingdeeProductionPickListItemDO secondItem = item(102L, 1002L);
        when(pickListItemMapper.selectListByProductionOrderNo("MO-001"))
                .thenReturn(List.of(firstItem, secondItem));
        when(pickListMapper.selectById(101L)).thenReturn(header(101L));
        when(pickListMapper.selectById(102L)).thenReturn(header(102L));
        MesProcessPoolActiveOrderPickListBindingDO first = binding(101L).setId(8101L)
                .setSourceSnapshotHash(MesFormalProductionPickListSourceResolver
                        .snapshotHash(header(101L), List.of(firstItem)));
        MesProcessPoolActiveOrderPickListBindingDO second = binding(102L).setId(8102L)
                .setSourceSnapshotHash(MesFormalProductionPickListSourceResolver
                        .snapshotHash(header(102L), List.of(secondItem)));
        when(bindingMapper.selectListByActiveOrderId(10L)).thenReturn(List.of(first, second));
        when(bindingItemMapper.selectListByBindingId(8101L)).thenReturn(List.of(frozen(8101L, firstItem)));
        when(bindingItemMapper.selectListByBindingId(8102L)).thenReturn(List.of(
                frozen(8102L, secondItem).setPickListItemId(9999L).setItemSnapshotHash("tampered")));

        assertThrows(ServiceException.class, () -> service.freezeAll(order(), 20L, "complete-1"));
        verify(bindingMapper, never()).insert(any(MesProcessPoolActiveOrderPickListBindingDO.class));
    }

    @Test
    void existingFirstBindingIsReusedAndOnlyMissingSecondBindingIsInserted() {
        ErpKingdeeProductionPickListItemDO firstItem = item(101L, 1001L);
        ErpKingdeeProductionPickListItemDO secondItem = item(102L, 1002L);
        when(pickListItemMapper.selectListByProductionOrderNo("MO-001"))
                .thenReturn(List.of(firstItem, secondItem));
        when(pickListMapper.selectById(101L)).thenReturn(header(101L));
        when(pickListMapper.selectById(102L)).thenReturn(header(102L));
        MesProcessPoolActiveOrderPickListBindingDO first = binding(101L).setId(8101L)
                .setSourceSnapshotHash(MesFormalProductionPickListSourceResolver
                        .snapshotHash(header(101L), List.of(firstItem)));
        MesProcessPoolActiveOrderPickListBindingDO second = binding(102L).setId(8102L)
                .setSourceSnapshotHash(MesFormalProductionPickListSourceResolver
                        .snapshotHash(header(102L), List.of(secondItem)));
        when(bindingMapper.selectListByActiveOrderId(10L)).thenReturn(List.of(first), List.of(first, second));
        when(bindingItemMapper.selectListByBindingId(8101L)).thenReturn(List.of(frozen(8101L, firstItem)));
        when(bindingItemMapper.selectListByBindingId(8102L)).thenReturn(List.of(frozen(8102L, secondItem)));
        when(bindingMapper.insert(any(MesProcessPoolActiveOrderPickListBindingDO.class))).thenReturn(1);
        when(bindingItemMapper.insert(any(cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingItemDO.class))).thenReturn(1);

        List<MesProcessPoolActiveOrderPickListBindingDO> result = service.freezeAll(order(), 20L, "complete-1");

        assertEquals(List.of(101L, 102L), result.stream()
                .map(MesProcessPoolActiveOrderPickListBindingDO::getPickListId).toList());
        verify(bindingMapper, org.mockito.Mockito.times(1)).insert(any(MesProcessPoolActiveOrderPickListBindingDO.class));
        verify(bindingItemMapper, org.mockito.Mockito.times(1)).insert(any(cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingItemDO.class));
    }

    @Test
    void existingExtraBindingConflicts() {
        ErpKingdeeProductionPickListItemDO firstItem = item(101L, 1001L);
        when(pickListItemMapper.selectListByProductionOrderNo("MO-001")).thenReturn(List.of(firstItem));
        when(pickListMapper.selectById(101L)).thenReturn(header(101L));
        MesProcessPoolActiveOrderPickListBindingDO first = binding(101L).setId(8101L)
                .setSourceSnapshotHash(MesFormalProductionPickListSourceResolver
                        .snapshotHash(header(101L), List.of(firstItem)));
        when(bindingMapper.selectListByActiveOrderId(10L)).thenReturn(List.of(first, binding(999L).setId(8999L)));

        assertThrows(ServiceException.class, () -> service.freezeAll(order(), 20L, "complete-1"));
        verify(bindingMapper, never()).insert(any(MesProcessPoolActiveOrderPickListBindingDO.class));
    }

    private MesProcessPoolActiveOrderDO order() {
        return MesProcessPoolActiveOrderDO.builder().id(10L).workOrderId(30L).leaderUserId(20L).build();
    }

    private ErpKingdeeProductionPickListDO header(Long id) {
        return ErpKingdeeProductionPickListDO.builder().id(id).sourceFid("FID-" + id)
                .sourceBillNo("PL-" + id).documentStatus("C").build();
    }

    private ErpKingdeeProductionPickListItemDO item(Long pickListId, Long id) {
        return ErpKingdeeProductionPickListItemDO.builder().id(id).productionPickListId(pickListId)
                .sourceFid("FID-" + pickListId).sourceEntryId(String.valueOf(id))
                .sourceLineKey(pickListId + ":" + id).materialNumber("MAT-" + id)
                .actualQuantity(BigDecimal.ONE).productionOrderNo("MO-001").build();
    }

    private MesProcessPoolActiveOrderPickListBindingDO binding(Long pickListId) {
        return MesProcessPoolActiveOrderPickListBindingDO.builder().id(8000L + pickListId)
                .activeOrderId(10L).workOrderId(30L).pickListId(pickListId).build();
    }

    private cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingItemDO frozen(
            Long bindingId, ErpKingdeeProductionPickListItemDO item) {
        return cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingItemDO
                .builder().id(9000L + item.getId()).bindingId(bindingId).pickListItemId(item.getId())
                .sourceEntryId(item.getSourceEntryId()).sourceLineKey(item.getSourceLineKey())
                .materialNumber(item.getMaterialNumber()).materialName(item.getMaterialName())
                .materialSpecification(item.getMaterialSpecification()).unitName(item.getUnitName())
                .requestedQuantity(item.getRequestedQuantity()).actualQuantity(item.getActualQuantity())
                .baseActualQuantity(item.getBaseActualQuantity()).lotNumber(item.getLotNumber())
                .productionOrderNo(item.getProductionOrderNo()).productionOrderLineNo(item.getProductionOrderLineNo())
                .itemSnapshotHash(MesFormalProductionPickListSourceResolver.itemSnapshotHash(item))
                .build();
    }
}
