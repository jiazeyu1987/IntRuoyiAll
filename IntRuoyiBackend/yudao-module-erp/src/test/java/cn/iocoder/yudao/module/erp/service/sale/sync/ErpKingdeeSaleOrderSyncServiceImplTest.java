package cn.iocoder.yudao.module.erp.service.sale.sync;

import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.order.ErpSaleOrderSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductCategoryDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductUnitDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpKingdeeCustomerSyncRecordDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpKingdeeSaleOrderSyncRecordDO;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductCategoryMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductUnitMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpCustomerMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpKingdeeCustomerSyncRecordMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpKingdeeSaleOrderSyncRecordMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOrderMapper;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.service.config.ErpKingdeeConfigService;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeMaterialDetail;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProperties;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeePurchaseOrderClient;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeSaleOrder;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeSaleOrderClient;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErpKingdeeSaleOrderSyncServiceImplTest {

    @Mock
    private ErpKingdeeSaleOrderClient saleOrderClient;
    @Mock
    private ErpKingdeeConfigService kingdeeConfigService;
    @Mock
    private ErpKingdeePurchaseOrderClient materialDetailClient;
    @Mock
    private ErpKingdeeSaleOrderSyncRecordMapper syncRecordMapper;
    @Mock
    private ErpSaleOrderMapper saleOrderMapper;
    @Mock
    private ErpSaleOrderService saleOrderService;
    @Mock
    private ErpCustomerMapper customerMapper;
    @Mock
    private ErpKingdeeCustomerSyncRecordMapper customerSyncRecordMapper;
    @Mock
    private ErpProductMapper productMapper;
    @Mock
    private ErpProductCategoryMapper productCategoryMapper;
    @Mock
    private ErpProductUnitMapper productUnitMapper;

    private ErpKingdeeProperties kingdeeProperties;
    private ErpKingdeeSaleOrderSyncServiceImpl syncService;

    @BeforeEach
    void setUp() {
        kingdeeProperties = new ErpKingdeeProperties();
        kingdeeProperties.setBaseUrl("https://k3.example.com");
        kingdeeProperties.setAcctId("acct");
        kingdeeProperties.setUsername("user");
        kingdeeProperties.setPassword("password");
        kingdeeProperties.setLcid(2052);
        when(kingdeeConfigService.getEffectiveProperties()).thenReturn(kingdeeProperties);
        syncService = new ErpKingdeeSaleOrderSyncServiceImpl(
                saleOrderClient, materialDetailClient, kingdeeConfigService, syncRecordMapper, saleOrderMapper, saleOrderService,
                customerMapper, customerSyncRecordMapper, productMapper, productCategoryMapper, productUnitMapper);
    }

    @Test
    void syncSaleOrders_autoCreatesCustomerAndProductAndApprovesOrder() {
        ErpKingdeeSaleOrder saleOrder = buildSaleOrder();
        when(saleOrderClient.fetchSaleOrders(kingdeeProperties)).thenReturn(List.of(saleOrder));
        when(syncRecordMapper.selectBySourceKey(ErpKingdeeSaleOrder.FORM_ID, "348963")).thenReturn(null);
        when(customerSyncRecordMapper.selectBySourceCustomerNumber("C000637")).thenReturn(null);
        when(customerMapper.selectByName("Customer A")).thenReturn(null);
        when(productMapper.selectByBarCode("MAT-001")).thenReturn(null);
        when(productCategoryMapper.selectByCode("CHLB05_SYS")).thenReturn(null);
        when(productUnitMapper.selectByName("PCS")).thenReturn(null);
        when(materialDetailClient.fetchMaterialDetails(eq(kingdeeProperties), any()))
                .thenReturn(Map.of("MAT-001", buildMaterialDetail()));
        when(saleOrderService.createSaleOrder(any(ErpSaleOrderSaveReqVO.class))).thenReturn(501L);
        doAnswer(invocation -> {
            ErpCustomerDO customer = invocation.getArgument(0);
            customer.setId(10L);
            return 1;
        }).when(customerMapper).insert(any(ErpCustomerDO.class));
        doAnswer(invocation -> {
            ErpProductCategoryDO category = invocation.getArgument(0);
            category.setId(30L);
            return 1;
        }).when(productCategoryMapper).insert(any(ErpProductCategoryDO.class));
        doAnswer(invocation -> {
            ErpProductUnitDO unit = invocation.getArgument(0);
            unit.setId(40L);
            return 1;
        }).when(productUnitMapper).insert(any(ErpProductUnitDO.class));
        doAnswer(invocation -> {
            ErpProductDO product = invocation.getArgument(0);
            product.setId(20L);
            return 1;
        }).when(productMapper).insert(any(ErpProductDO.class));
        when(productMapper.selectById(20L)).thenReturn(new ErpProductDO().setId(20L).setUnitId(40L));

        ErpKingdeeSaleOrderSyncResult result = syncService.syncSaleOrders();

        assertEquals(1, result.getCreatedCount());
        ArgumentCaptor<ErpSaleOrderSaveReqVO> orderCaptor = ArgumentCaptor.forClass(ErpSaleOrderSaveReqVO.class);
        verify(saleOrderService).createSaleOrder(orderCaptor.capture());
        assertEquals(10L, orderCaptor.getValue().getCustomerId());
        assertEquals(20L, orderCaptor.getValue().getItems().get(0).getProductId());
        verify(saleOrderService).updateSaleOrderStatus(501L, ErpAuditStatus.APPROVE.getStatus());
        verify(syncRecordMapper).insert(any(ErpKingdeeSaleOrderSyncRecordDO.class));
    }

    @Test
    void syncSaleOrders_updatesExistingSourceRecord() {
        ErpKingdeeSaleOrder saleOrder = buildSaleOrder();
        when(saleOrderClient.fetchSaleOrders(kingdeeProperties)).thenReturn(List.of(saleOrder));
        when(syncRecordMapper.selectBySourceKey(ErpKingdeeSaleOrder.FORM_ID, "348963"))
                .thenReturn(new ErpKingdeeSaleOrderSyncRecordDO().setId(301L).setSaleOrderId(501L));
        when(saleOrderService.getSaleOrder(501L))
                .thenReturn(new cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderDO()
                        .setId(501L)
                        .setStatus(ErpAuditStatus.PROCESS.getStatus()));
        when(customerSyncRecordMapper.selectBySourceCustomerNumber("C000637"))
                .thenReturn(new ErpKingdeeCustomerSyncRecordDO().setCustomerId(10L));
        when(productMapper.selectByBarCode("MAT-001")).thenReturn(new ErpProductDO().setId(20L).setUnitId(40L));
        when(productMapper.selectById(20L)).thenReturn(new ErpProductDO().setId(20L).setUnitId(40L));

        ErpKingdeeSaleOrderSyncResult result = syncService.syncSaleOrders();

        assertEquals(0, result.getCreatedCount());
        assertEquals(1, result.getUpdatedCount());
        assertEquals(0, result.getSkippedCount());
        verify(saleOrderService, never()).createSaleOrder(any(ErpSaleOrderSaveReqVO.class));
        verify(saleOrderService).updateSaleOrder(any(ErpSaleOrderSaveReqVO.class));
    }

    @Test
    void syncSaleOrdersModifiedBetween_usesModifyTimeWindowAndUpdatesExistingSourceRecord() {
        LocalDateTime windowStart = LocalDateTime.of(2026, 6, 12, 8, 0);
        LocalDateTime windowEnd = LocalDateTime.of(2026, 6, 12, 9, 0);
        ErpKingdeeSaleOrder saleOrder = buildSaleOrder();
        when(saleOrderClient.fetchSaleOrdersModifiedBetween(kingdeeProperties, windowStart, windowEnd))
                .thenReturn(List.of(saleOrder));
        when(syncRecordMapper.selectBySourceKey(ErpKingdeeSaleOrder.FORM_ID, "348963"))
                .thenReturn(new ErpKingdeeSaleOrderSyncRecordDO().setId(301L).setSaleOrderId(501L));
        when(saleOrderService.getSaleOrder(501L))
                .thenReturn(new cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderDO()
                        .setId(501L)
                        .setStatus(ErpAuditStatus.PROCESS.getStatus()));
        when(customerSyncRecordMapper.selectBySourceCustomerNumber("C000637"))
                .thenReturn(new ErpKingdeeCustomerSyncRecordDO().setCustomerId(10L));
        when(productMapper.selectByBarCode("MAT-001")).thenReturn(new ErpProductDO().setId(20L).setUnitId(40L));
        when(productMapper.selectById(20L)).thenReturn(new ErpProductDO().setId(20L).setUnitId(40L));

        ErpKingdeeSaleOrderSyncResult result = syncService.syncSaleOrdersModifiedBetween(windowStart, windowEnd);

        assertEquals(0, result.getCreatedCount());
        assertEquals(1, result.getUpdatedCount());
        assertEquals(0, result.getSkippedCount());
        verify(saleOrderClient).fetchSaleOrdersModifiedBetween(kingdeeProperties, windowStart, windowEnd);
        verify(saleOrderClient, never()).fetchSaleOrders(kingdeeProperties);
        verify(saleOrderService, never()).createSaleOrder(any(ErpSaleOrderSaveReqVO.class));
        verify(saleOrderService).updateSaleOrder(any(ErpSaleOrderSaveReqVO.class));
    }

    @Test
    void syncSaleOrdersModifiedBetween_updatesExistingSourceRecordAndRestoresApprovedStatus() {
        LocalDateTime windowStart = LocalDateTime.of(2026, 6, 12, 8, 0);
        LocalDateTime windowEnd = LocalDateTime.of(2026, 6, 12, 9, 0);
        ErpKingdeeSaleOrder saleOrder = buildSaleOrder();
        when(saleOrderClient.fetchSaleOrdersModifiedBetween(kingdeeProperties, windowStart, windowEnd))
                .thenReturn(List.of(saleOrder));
        when(syncRecordMapper.selectBySourceKey(ErpKingdeeSaleOrder.FORM_ID, "348963"))
                .thenReturn(new ErpKingdeeSaleOrderSyncRecordDO()
                        .setId(301L)
                        .setSaleOrderId(501L));
        when(saleOrderService.getSaleOrder(501L))
                .thenReturn(new cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderDO()
                        .setId(501L)
                        .setStatus(ErpAuditStatus.APPROVE.getStatus()));
        when(customerSyncRecordMapper.selectBySourceCustomerNumber("C000637"))
                .thenReturn(new ErpKingdeeCustomerSyncRecordDO().setCustomerId(10L));
        when(productMapper.selectByBarCode("MAT-001")).thenReturn(new ErpProductDO().setId(20L).setUnitId(40L));
        when(productMapper.selectById(20L)).thenReturn(new ErpProductDO().setId(20L).setUnitId(40L));

        ErpKingdeeSaleOrderSyncResult result = syncService.syncSaleOrdersModifiedBetween(windowStart, windowEnd);

        assertEquals(0, result.getCreatedCount());
        assertEquals(1, result.getUpdatedCount());
        assertEquals(0, result.getSkippedCount());
        ArgumentCaptor<ErpSaleOrderSaveReqVO> orderCaptor = ArgumentCaptor.forClass(ErpSaleOrderSaveReqVO.class);
        verify(saleOrderService).updateSaleOrder(orderCaptor.capture());
        assertEquals(501L, orderCaptor.getValue().getId());
        assertEquals(10L, orderCaptor.getValue().getCustomerId());
        assertEquals(20L, orderCaptor.getValue().getItems().get(0).getProductId());
        verify(saleOrderService).updateSaleOrderStatus(501L, ErpAuditStatus.PROCESS.getStatus());
        verify(saleOrderService).updateSaleOrderStatus(501L, ErpAuditStatus.APPROVE.getStatus());
        ArgumentCaptor<cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderDO> statusCaptor =
                ArgumentCaptor.forClass(cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderDO.class);
        verify(saleOrderMapper).updateById(statusCaptor.capture());
        assertEquals("B", statusCaptor.getValue().getKingdeeCloseStatus());
        assertEquals("A", statusCaptor.getValue().getKingdeeCancelStatus());
        verify(syncRecordMapper).updateById(any(ErpKingdeeSaleOrderSyncRecordDO.class));
        verify(syncRecordMapper, never()).insert(any(ErpKingdeeSaleOrderSyncRecordDO.class));
    }

    private static ErpKingdeeSaleOrder buildSaleOrder() {
        ErpKingdeeSaleOrder.Line line = new ErpKingdeeSaleOrder.Line();
        line.setMaterialNumber("MAT-001");
        line.setMaterialName("Material A");
        line.setQuantity(new BigDecimal("10"));
        line.setPrice(new BigDecimal("840.7"));
        line.setTaxPrice(new BigDecimal("950"));
        line.setTaxPercent(new BigDecimal("13"));
        line.setTotalAmount(new BigDecimal("9500"));

        ErpKingdeeSaleOrder saleOrder = new ErpKingdeeSaleOrder();
        saleOrder.setFid("348963");
        saleOrder.setBillNo("908XSDD00103");
        saleOrder.setBillDate(LocalDateTime.of(2026, 3, 12, 0, 0));
        saleOrder.setDocumentStatus("C");
        saleOrder.setCloseStatus("B");
        saleOrder.setCancelStatus("A");
        saleOrder.setCustomerNumber("C000637");
        saleOrder.setCustomerName("Customer A");
        saleOrder.setLines(List.of(line));
        return saleOrder;
    }

    private static ErpKingdeeMaterialDetail buildMaterialDetail() {
        ErpKingdeeMaterialDetail detail = new ErpKingdeeMaterialDetail();
        detail.setMaterialNumber("MAT-001");
        detail.setMaterialName("Material A");
        detail.setSpecification("12ml");
        detail.setCategoryCode("CHLB05_SYS");
        detail.setCategoryName("Finished Goods");
        detail.setUnitName("PCS");
        return detail;
    }

}
