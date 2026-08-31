package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionPickListItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionPickListItemMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
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

@ExtendWith(MockitoExtension.class)
class MesProFeedbackMaterialBatchQueryServiceTest {

    private static final Long WORK_ORDER_ID = 4101L;

    @Mock
    private MesProWorkOrderMapper workOrderMapper;
    @Mock
    private ErpKingdeeProductionPickListItemMapper pickListItemMapper;

    private MesProFeedbackMaterialBatchQueryService service;

    @BeforeEach
    void setUp() {
        service = new MesProFeedbackMaterialBatchQueryServiceImpl(workOrderMapper, pickListItemMapper);
    }

    @Test
    void listBatchCodes_returnsAllDistinctInternalSyncLotsForExactOrderAndMaterial() {
        when(workOrderMapper.selectById(WORK_ORDER_ID))
                .thenReturn(MesProWorkOrderDO.builder().id(WORK_ORDER_ID).code("MO-001").build());
        when(pickListItemMapper.selectListByProductionOrderNo("MO-001")).thenReturn(List.of(
                item("A001", "LOT-002"),
                item("A001", " LOT-001 "),
                item("A001", "LOT-002"),
                item("A001", " "),
                item("A002", "OTHER-MATERIAL")));

        List<String> batchCodes = service.listBatchCodes(WORK_ORDER_ID, "A001");

        assertEquals(List.of("LOT-001", "LOT-002"), batchCodes);
    }

    @Test
    void listBatchCodes_returnsEmptyWhenSystemSyncTableHasNoBatch() {
        when(workOrderMapper.selectById(WORK_ORDER_ID))
                .thenReturn(MesProWorkOrderDO.builder().id(WORK_ORDER_ID).code("MO-001").build());
        when(pickListItemMapper.selectListByProductionOrderNo("MO-001")).thenReturn(List.of());

        assertEquals(List.of(), service.listBatchCodes(WORK_ORDER_ID, "A001"));
    }

    @Test
    void listBatchCodes_rejectsMissingFormalWorkOrderIdentity() {
        when(workOrderMapper.selectById(WORK_ORDER_ID)).thenReturn(null);

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.listBatchCodes(WORK_ORDER_ID, "A001"));

        assertTrue(error.getMessage().contains("生产工单"));
    }

    private static ErpKingdeeProductionPickListItemDO item(String materialNumber, String lotNumber) {
        return ErpKingdeeProductionPickListItemDO.builder()
                .materialNumber(materialNumber)
                .lotNumber(lotNumber)
                .productionOrderNo("MO-001")
                .build();
    }
}
