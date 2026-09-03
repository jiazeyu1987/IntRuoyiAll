package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionPickListItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionPickListDO;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionPickListItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionPickListMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesFormalProductionPickListSourceResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class MesProFeedbackMaterialBatchQueryServiceTest {

    private static final Long WORK_ORDER_ID = 4101L;

    @Mock
    private MesProWorkOrderMapper workOrderMapper;
    @Mock
    private ErpKingdeeProductionPickListItemMapper pickListItemMapper;
    @Mock
    private ErpKingdeeProductionPickListMapper pickListMapper;
    @Mock
    private MesProcessPoolActiveOrderPickListBindingMapper bindingMapper;
    @Mock
    private MesProcessPoolActiveOrderPickListBindingItemMapper bindingItemMapper;

    private MesProFeedbackMaterialBatchQueryService service;

    @BeforeEach
    void setUp() {
        service = new MesProFeedbackMaterialBatchQueryServiceImpl(
                new MesFormalProductionPickListSourceResolver(workOrderMapper, pickListMapper, pickListItemMapper));
    }

    @Test
    void listBatchCodes_returnsAllDistinctInternalSyncLotsForExactOrderAndMaterial() {
        when(workOrderMapper.selectById(WORK_ORDER_ID))
                .thenReturn(MesProWorkOrderDO.builder().id(WORK_ORDER_ID).code("MO-001").build());
        when(pickListItemMapper.selectListByProductionOrderNo("MO-001")).thenReturn(List.of(
                item(101L, 1001L, "A001", "LOT-002"),
                item(101L, 1002L, "A001", " LOT-001 "),
                item(102L, 1003L, "A001", "LOT-002"),
                item(102L, 1004L, "A001", " "),
                item(102L, 1005L, "A002", "OTHER-MATERIAL")));
        when(pickListMapper.selectById(101L)).thenReturn(header(101L, "C"));
        when(pickListMapper.selectById(102L)).thenReturn(header(102L, "C"));

        List<String> batchCodes = service.listBatchCodes(WORK_ORDER_ID, "A001");

        assertEquals(List.of("LOT-001", "LOT-002"), batchCodes);
        verifyNoInteractions(bindingMapper, bindingItemMapper);
    }

    @Test
    void listBatchCodes_rejectsWhenProductionOrderHasNoFormalPickList() {
        when(workOrderMapper.selectById(WORK_ORDER_ID))
                .thenReturn(MesProWorkOrderDO.builder().id(WORK_ORDER_ID).code("MO-001").build());
        when(pickListItemMapper.selectListByProductionOrderNo("MO-001")).thenReturn(List.of());

        assertThrows(ServiceException.class, () -> service.listBatchCodes(WORK_ORDER_ID, "A001"));
        verifyNoInteractions(bindingMapper, bindingItemMapper);
    }

    @Test
    void listBatchCodes_rejectsWholeResultWhenSecondPickListIsNotApproved() {
        when(workOrderMapper.selectById(WORK_ORDER_ID))
                .thenReturn(MesProWorkOrderDO.builder().id(WORK_ORDER_ID).code("MO-001").build());
        when(pickListItemMapper.selectListByProductionOrderNo("MO-001")).thenReturn(List.of(
                item(101L, 1001L, "A001", "LOT-001"),
                item(102L, 1002L, "A001", "LOT-002")));
        when(pickListMapper.selectById(101L)).thenReturn(header(101L, "C"));
        when(pickListMapper.selectById(102L)).thenReturn(header(102L, "A"));

        assertThrows(ServiceException.class, () -> service.listBatchCodes(WORK_ORDER_ID, "A001"));
        verifyNoInteractions(bindingMapper, bindingItemMapper);
    }

    @Test
    void listBatchCodes_rejectsWholeResultWhenSecondPickListHeaderIsMissing() {
        when(workOrderMapper.selectById(WORK_ORDER_ID))
                .thenReturn(MesProWorkOrderDO.builder().id(WORK_ORDER_ID).code("MO-001").build());
        when(pickListItemMapper.selectListByProductionOrderNo("MO-001")).thenReturn(List.of(
                item(101L, 1001L, "A001", "LOT-001"),
                item(102L, 1002L, "A001", "LOT-002")));
        when(pickListMapper.selectById(101L)).thenReturn(header(101L, "C"));
        when(pickListMapper.selectById(102L)).thenReturn(null);

        assertThrows(ServiceException.class, () -> service.listBatchCodes(WORK_ORDER_ID, "A001"));
    }

    @Test
    void listBatchCodes_rejectsWholeResultWhenAnyItemIdentityIsIncomplete() {
        when(workOrderMapper.selectById(WORK_ORDER_ID))
                .thenReturn(MesProWorkOrderDO.builder().id(WORK_ORDER_ID).code("MO-001").build());
        when(pickListItemMapper.selectListByProductionOrderNo("MO-001")).thenReturn(List.of(
                item(101L, 1001L, "A001", "LOT-001"),
                item(102L, 1002L, "A001", "LOT-002").setSourceLineKey(null)));
        when(pickListMapper.selectById(101L)).thenReturn(header(101L, "C"));
        when(pickListMapper.selectById(102L)).thenReturn(header(102L, "C"));

        assertThrows(ServiceException.class, () -> service.listBatchCodes(WORK_ORDER_ID, "A001"));
    }

    @Test
    void listBatchCodes_rejectsMissingFormalWorkOrderIdentity() {
        when(workOrderMapper.selectById(WORK_ORDER_ID)).thenReturn(null);

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.listBatchCodes(WORK_ORDER_ID, "A001"));

        assertTrue(error.getMessage().contains("生产工单"));
    }

    private static ErpKingdeeProductionPickListItemDO item(Long pickListId, Long id,
                                                            String materialNumber, String lotNumber) {
        return ErpKingdeeProductionPickListItemDO.builder()
                .id(id)
                .productionPickListId(pickListId)
                .sourceFid("FID-" + pickListId)
                .sourceEntryId(String.valueOf(id))
                .sourceLineKey(pickListId + ":" + id)
                .sourceBillNo("PL-" + pickListId)
                .materialNumber(materialNumber)
                .materialName("物料-" + materialNumber)
                .unitName("件")
                .lotNumber(lotNumber)
                .productionOrderNo("MO-001")
                .build();
    }

    private static ErpKingdeeProductionPickListDO header(Long id, String status) {
        return ErpKingdeeProductionPickListDO.builder().id(id).sourceFid("FID-" + id)
                .sourceBillNo("PL-" + id).documentStatus(status).build();
    }
}
