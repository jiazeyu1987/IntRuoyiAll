package cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.MesProWorkOrderPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

public class MesProWorkOrderMapperTest extends BaseDbUnitTest {

    @Resource
    private MesProWorkOrderMapper workOrderMapper;

    private MesProWorkOrderDO createWorkOrder(Consumer<MesProWorkOrderDO> consumer) {
        return randomPojo(MesProWorkOrderDO.class, workOrder -> {
            workOrder.setQuantity(new BigDecimal("10.00"));
            workOrder.setQuantityProduced(BigDecimal.ZERO);
            workOrder.setQuantityChanged(BigDecimal.ZERO);
            workOrder.setQuantityScheduled(BigDecimal.ZERO);
            workOrder.setTemporaryFrozen(Boolean.FALSE);
            workOrder.setParentId(0L);
            workOrder.setRequestDate(LocalDateTime.of(2026, 5, 16, 0, 0));
            consumer.accept(workOrder);
        });
    }

    @Test
    void updateErpSnapshot_clearsSourceFieldsAndPreservesLocalProductionState() {
        MesProWorkOrderDO existing = createWorkOrder(workOrder -> {
            workOrder.setCode("ERP-SNAPSHOT-001");
            workOrder.setWorkshopName("旧车间");
            workOrder.setBomVersion("旧BOM");
            workOrder.setPickMode("是");
            workOrder.setAuxiliaryCode("旧助记码");
            workOrder.setBusinessStatus("2");
            workOrder.setScheduleStatus("2");
            workOrder.setDrawingNumber("OLD-DRAWING");
            workOrder.setRefNo("OLD-REF");
            workOrder.setDemandBillNo("OLD-DEMAND");
            workOrder.setOrderSourceCode("OLD-SOURCE");
            workOrder.setMaterialSpecification("OLD-SPEC");
            workOrder.setPlannedStartTime(LocalDateTime.of(2026, 10, 1, 0, 0));
            workOrder.setPlannedEndTime(LocalDateTime.of(2026, 10, 2, 0, 0));
            workOrder.setQuantityProduced(new BigDecimal("3.00"));
            workOrder.setQuantityScheduled(new BigDecimal("7.00"));
            workOrder.setTemporaryFrozen(Boolean.TRUE);
        });
        workOrderMapper.insert(existing);
        MesProWorkOrderDO snapshot = new MesProWorkOrderDO().setId(existing.getId())
                .setCode(existing.getCode()).setName("新物料名称").setType(existing.getType())
                .setOrderSourceType(existing.getOrderSourceType()).setProductId(existing.getProductId())
                .setQuantity(new BigDecimal("20.00")).setBatchCode(existing.getBatchCode())
                .setBusinessStatus("4").setScheduleStatus("1").setPickMode("否")
                .setRequestDate(existing.getRequestDate()).setRemark("ERP snapshot");

        assertEquals(1, workOrderMapper.updateErpSnapshot(snapshot));

        MesProWorkOrderDO actual = workOrderMapper.selectById(existing.getId());
        assertEquals("新物料名称", actual.getName());
        assertEquals(0, actual.getQuantity().compareTo(new BigDecimal("20.00")));
        assertEquals("否", actual.getPickMode());
        assertEquals("4", actual.getBusinessStatus());
        assertEquals("1", actual.getScheduleStatus());
        assertNull(actual.getAuxiliaryCode());
        assertNull(actual.getWorkshopName());
        assertNull(actual.getBomVersion());
        assertNull(actual.getDrawingNumber());
        assertNull(actual.getRefNo());
        assertNull(actual.getOrderSourceCode());
        assertNull(actual.getDemandBillNo());
        assertNull(actual.getMaterialSpecification());
        assertNull(actual.getPlannedStartTime());
        assertNull(actual.getPlannedEndTime());
        assertEquals(existing.getStatus(), actual.getStatus());
        assertEquals(existing.getClientId(), actual.getClientId());
        assertEquals(existing.getVendorId(), actual.getVendorId());
        assertEquals(0, actual.getQuantityProduced().compareTo(new BigDecimal("3.00")));
        assertEquals(0, actual.getQuantityScheduled().compareTo(new BigDecimal("7.00")));
        assertTrue(actual.getTemporaryFrozen());
        assertEquals(0, workOrderMapper.updateErpSnapshot(snapshot.setId(Long.MAX_VALUE)));
    }

    @Test
    void testSelectPage_orderByTemporaryFrozenThenIdDesc() {
        MesProWorkOrderDO normalOlder = createWorkOrder(workOrder -> {
            workOrder.setCode("WO-NORMAL-OLD");
            workOrder.setTemporaryFrozen(Boolean.FALSE);
        });
        workOrderMapper.insert(normalOlder);

        MesProWorkOrderDO frozenOlder = createWorkOrder(workOrder -> {
            workOrder.setCode("WO-FROZEN-OLD");
            workOrder.setTemporaryFrozen(Boolean.TRUE);
        });
        workOrderMapper.insert(frozenOlder);

        MesProWorkOrderDO normalNewer = createWorkOrder(workOrder -> {
            workOrder.setCode("WO-NORMAL-NEW");
            workOrder.setTemporaryFrozen(Boolean.FALSE);
        });
        workOrderMapper.insert(normalNewer);

        MesProWorkOrderDO frozenNewer = createWorkOrder(workOrder -> {
            workOrder.setCode("WO-FROZEN-NEW");
            workOrder.setTemporaryFrozen(Boolean.TRUE);
        });
        workOrderMapper.insert(frozenNewer);

        MesProWorkOrderPageReqVO reqVO = new MesProWorkOrderPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        PageResult<MesProWorkOrderDO> result = workOrderMapper.selectPage(reqVO);

        assertEquals(4, result.getTotal());
        List<MesProWorkOrderDO> list = result.getList();
        assertFalse(list.get(0).getTemporaryFrozen());
        assertFalse(list.get(1).getTemporaryFrozen());
        assertTrue(list.get(2).getTemporaryFrozen());
        assertTrue(list.get(3).getTemporaryFrozen());
        assertEquals(List.of("WO-NORMAL-NEW", "WO-NORMAL-OLD", "WO-FROZEN-NEW", "WO-FROZEN-OLD"),
                list.stream().map(MesProWorkOrderDO::getCode).toList());
    }

    @Test
    void testSelectPage_filtersByExpandedProductIds() {
        MesProWorkOrderDO selectedProductOrder = createWorkOrder(workOrder -> {
            workOrder.setCode("WO-BALLOON-SELECTED");
            workOrder.setProductId(1001L);
        });
        workOrderMapper.insert(selectedProductOrder);

        MesProWorkOrderDO sameNameProductOrder = createWorkOrder(workOrder -> {
            workOrder.setCode("WO-BALLOON-SAME-NAME");
            workOrder.setProductId(1002L);
        });
        workOrderMapper.insert(sameNameProductOrder);

        MesProWorkOrderDO unrelatedProductOrder = createWorkOrder(workOrder -> {
            workOrder.setCode("WO-OTHER-PRODUCT");
            workOrder.setProductId(2001L);
        });
        workOrderMapper.insert(unrelatedProductOrder);

        MesProWorkOrderPageReqVO reqVO = new MesProWorkOrderPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setProductId(1001L);
        PageResult<MesProWorkOrderDO> result = workOrderMapper.selectPageByProductIds(reqVO, List.of(1001L, 1002L));

        assertEquals(2, result.getTotal());
        assertEquals(List.of("WO-BALLOON-SAME-NAME", "WO-BALLOON-SELECTED"),
                result.getList().stream().map(MesProWorkOrderDO::getCode).toList());
    }

    @Test
    void testSelectCandidatesByKeyword_doesNotTruncateBeforeEligibilityEvaluation() {
        List<String> expectedCodes = new ArrayList<>();
        for (int index = 1; index <= 24; index++) {
            String code = "881MO-SEARCH-" + index;
            expectedCodes.add(code);
            workOrderMapper.insert(createWorkOrder(workOrder -> workOrder.setCode(code)));
        }

        List<MesProWorkOrderDO> result = workOrderMapper.selectCandidatesByKeyword("88", List.of());

        assertEquals(24, result.size());
        assertTrue(result.stream().map(MesProWorkOrderDO::getCode).toList().containsAll(expectedCodes));
    }
}
