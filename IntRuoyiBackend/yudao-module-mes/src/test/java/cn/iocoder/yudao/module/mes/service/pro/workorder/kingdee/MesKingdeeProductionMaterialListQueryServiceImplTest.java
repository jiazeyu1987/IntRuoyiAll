package cn.iocoder.yudao.module.mes.service.pro.workorder.kingdee;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.kingdee.MesKingdeeProductionMaterialListDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.kingdee.MesKingdeeProductionMaterialListGroupRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.kingdee.MesKingdeeProductionMaterialListPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesKingdeeProductionMaterialListDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesKingdeeProductionMaterialListMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Import(MesKingdeeProductionMaterialListQueryServiceImpl.class)
@TestPropertySource(properties = "yudao.info.base-package=cn.iocoder.yudao.module.mes")
class MesKingdeeProductionMaterialListQueryServiceImplTest extends BaseDbUnitTest {

    @Resource
    private MesKingdeeProductionMaterialListQueryService queryService;

    @Resource
    private MesKingdeeProductionMaterialListMapper materialListMapper;

    @BeforeEach
    void setTenantIdToSqlDefault() {
        TenantContextHolder.setTenantId(0L);
    }

    @Test
    void getGroupPage_shouldReturnOneRowPerBill() {
        insertMaterialRow("BILL-A", "P-A", "WO-A", 2, "CHILD-A2", "子项A2",
                LocalDateTime.of(2026, 6, 30, 10, 0),
                LocalDateTime.of(2026, 6, 30, 10, 30));
        insertMaterialRow("BILL-A", "P-B", "WO-B", 1, "CHILD-A1", "子项A1",
                LocalDateTime.of(2026, 6, 30, 11, 0),
                LocalDateTime.of(2026, 6, 30, 11, 30));
        insertMaterialRow("BILL-A", "P-C", "WO-C", 2, "CHILD-A3", "子项A3",
                LocalDateTime.of(2026, 6, 30, 9, 0),
                LocalDateTime.of(2026, 6, 30, 11, 45));
        insertMaterialRow("BILL-B", "P-D", "WO-D", 1, "CHILD-B1", "子项B1",
                LocalDateTime.of(2026, 6, 29, 18, 0),
                LocalDateTime.of(2026, 6, 29, 18, 30));

        MesKingdeeProductionMaterialListPageReqVO reqVO = new MesKingdeeProductionMaterialListPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);

        PageResult<MesKingdeeProductionMaterialListGroupRespVO> result = queryService.getGroupPage(reqVO);

        assertEquals(2L, result.getTotal());
        assertEquals(2, result.getList().size());
        assertEquals("BILL-A", result.getList().get(0).getSourceBillNo());
        assertEquals(3L, result.getList().get(0).getLineCount());
        assertEquals(LocalDateTime.of(2026, 6, 30, 11, 0), result.getList().get(0).getSourceModifyTime());
        assertEquals(LocalDateTime.of(2026, 6, 30, 11, 45), result.getList().get(0).getLastSyncTime());
        assertEquals("BILL-B", result.getList().get(1).getSourceBillNo());
        assertEquals(1L, result.getList().get(1).getLineCount());
    }

    @Test
    void getGroupPage_shouldKeepWholeBillLineCountWhenFilteringChildMatches() {
        insertMaterialRow("BILL-FULL", "PRODUCT-X", "WO-100", 1, "FILTER-HIT", "命中子项",
                LocalDateTime.of(2026, 6, 30, 8, 0),
                LocalDateTime.of(2026, 6, 30, 8, 30));
        insertMaterialRow("BILL-FULL", "PRODUCT-Y", "WO-101", 3, "FILTER-MISS-1", "未命中子项1",
                LocalDateTime.of(2026, 6, 30, 9, 0),
                LocalDateTime.of(2026, 6, 30, 9, 30));
        insertMaterialRow("BILL-FULL", "PRODUCT-Z", "WO-102", 4, "FILTER-MISS-2", "未命中子项2",
                LocalDateTime.of(2026, 6, 30, 10, 0),
                LocalDateTime.of(2026, 6, 30, 10, 30));
        insertMaterialRow("BILL-OTHER", "PRODUCT-O", "WO-200", 1, "OTHER", "其他子项",
                LocalDateTime.of(2026, 6, 29, 8, 0),
                LocalDateTime.of(2026, 6, 29, 8, 30));

        MesKingdeeProductionMaterialListPageReqVO reqVO = new MesKingdeeProductionMaterialListPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setChildMaterialCode("FILTER-HIT");

        PageResult<MesKingdeeProductionMaterialListGroupRespVO> result = queryService.getGroupPage(reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getList().size());
        assertEquals("BILL-FULL", result.getList().get(0).getSourceBillNo());
        assertEquals(3L, result.getList().get(0).getLineCount());
    }

    @Test
    void getGroupPage_shouldExposeProductionOrderSummary() {
        insertMaterialRow("BILL-LINK", "PRODUCT-A", "WO-001", 1, "CHILD-1", "子项1",
                LocalDateTime.of(2026, 6, 30, 8, 0),
                LocalDateTime.of(2026, 6, 30, 8, 30),
                9001L, "WO-001");
        insertMaterialRow("BILL-LINK", "PRODUCT-A", "WO-002", 2, "CHILD-2", "子项2",
                LocalDateTime.of(2026, 6, 30, 9, 0),
                LocalDateTime.of(2026, 6, 30, 9, 30),
                9002L, "WO-002");

        MesKingdeeProductionMaterialListPageReqVO reqVO = new MesKingdeeProductionMaterialListPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);

        PageResult<MesKingdeeProductionMaterialListGroupRespVO> result = queryService.getGroupPage(reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getList().size());
        assertEquals(2L, result.getList().get(0).getProductionOrderCount());
        assertEquals("WO-001、WO-002", result.getList().get(0).getProductionOrderSummary());
    }

    @Test
    void getDetailList_shouldReturnOnlySpecifiedBillAndSortByLineNoThenId() {
        insertMaterialRow("BILL-DETAIL", "PRODUCT-1", "WO-1", 2, "CHILD-LINE2-A", "子项2-A",
                LocalDateTime.of(2026, 6, 30, 8, 0),
                LocalDateTime.of(2026, 6, 30, 8, 30),
                903245L, "WO-1");
        insertMaterialRow("BILL-DETAIL", "PRODUCT-1", "WO-1", 1, "CHILD-LINE1", "子项1",
                LocalDateTime.of(2026, 6, 30, 9, 0),
                LocalDateTime.of(2026, 6, 30, 9, 30),
                903245L, "WO-1");
        insertMaterialRow("BILL-DETAIL", "PRODUCT-1", "WO-1", 2, "CHILD-LINE2-B", "子项2-B",
                LocalDateTime.of(2026, 6, 30, 10, 0),
                LocalDateTime.of(2026, 6, 30, 10, 30),
                903245L, "WO-1");
        insertMaterialRow("BILL-OTHER", "PRODUCT-2", "WO-2", 1, "CHILD-OTHER", "其他子项",
                LocalDateTime.of(2026, 6, 29, 10, 0),
                LocalDateTime.of(2026, 6, 29, 10, 30),
                null, null);

        List<MesKingdeeProductionMaterialListDetailRespVO> result = queryService.getDetailList("BILL-DETAIL");

        assertEquals(3, result.size());
        assertEquals("CHILD-LINE1", result.get(0).getChildMaterialCode());
        assertEquals("CHILD-LINE2-A", result.get(1).getChildMaterialCode());
        assertEquals("CHILD-LINE2-B", result.get(2).getChildMaterialCode());
        assertEquals("子项1", result.get(0).getChildMaterialName());
        assertEquals("规格-CHILD-LINE1", result.get(0).getChildMaterialSpecification());
        assertEquals("标准件", result.get(0).getChildMaterialType());
        assertEquals(new BigDecimal("2.000000"), result.get(0).getNumerator());
        assertEquals(new BigDecimal("5.000000"), result.get(0).getDenominator());
        assertEquals("支", result.get(0).getChildUnitName());
        assertEquals("WO-1", result.get(0).getProductionOrderNo());
        assertEquals(903245L, result.get(0).getWorkOrderId());
        assertEquals("WO-1", result.get(0).getWorkOrderCode());
    }

    private void insertMaterialRow(String sourceBillNo, String productCode, String productionOrderNo,
                                   int productionOrderLineNo, String childMaterialCode, String childMaterialName,
                                   LocalDateTime sourceModifyTime, LocalDateTime lastSyncTime) {
        insertMaterialRow(sourceBillNo, productCode, productionOrderNo, productionOrderLineNo, childMaterialCode,
                childMaterialName, sourceModifyTime, lastSyncTime, null, null);
    }

    private void insertMaterialRow(String sourceBillNo, String productCode, String productionOrderNo,
                                   int productionOrderLineNo, String childMaterialCode, String childMaterialName,
                                   LocalDateTime sourceModifyTime, LocalDateTime lastSyncTime,
                                   Long workOrderId, String workOrderCode) {
        materialListMapper.insert(MesKingdeeProductionMaterialListDO.builder()
                .sourceFormId("PRD_PPBOM")
                .sourceBillNo(sourceBillNo)
                .sourceEntryId(sourceBillNo + "-" + childMaterialCode)
                .productCode(productCode)
                .productionOrderNo(productionOrderNo)
                .productionOrderLineNo(productionOrderLineNo)
                .productionOrderStatus("计划")
                .childMaterialCode(childMaterialCode)
                .childMaterialName(childMaterialName)
                .childMaterialSpecification("规格-" + childMaterialCode)
                .childMaterialType("标准件")
                .numerator(new BigDecimal("2.000000"))
                .denominator(new BigDecimal("5.000000"))
                .childUnitName("支")
                .requiredQuantity(new BigDecimal("1.000000"))
                .issueMethod("直接领料")
                .demandTime(sourceModifyTime.plusHours(1))
                .workOrderId(workOrderId)
                .workOrderCode(workOrderCode)
                .sourceModifyTime(sourceModifyTime)
                .lastSyncTime(lastSyncTime)
                .rawPayload("{}")
                .build());
    }

}
