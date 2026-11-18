package cn.iocoder.yudao.module.erp.service.purchase.sync;

import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductCategoryDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductUnitDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpKingdeePurchaseOrderSyncRecordDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpKingdeeSupplierSyncRecordDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductCategoryMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductUnitMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpKingdeePurchaseOrderSyncRecordMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpKingdeeSupplierSyncRecordMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseOrderMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpSupplierMapper;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants;
import cn.iocoder.yudao.module.erp.service.config.ErpKingdeeConfigService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseOrderService;
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

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErpKingdeePurchaseOrderSyncServiceImplTest {

    @Mock
    private ErpKingdeePurchaseOrderClient kingdeePurchaseOrderClient;
    @Mock
    private ErpKingdeeConfigService kingdeeConfigService;
    @Mock
    private ErpKingdeePurchaseOrderSyncRecordMapper syncRecordMapper;
    @Mock
    private ErpPurchaseOrderMapper purchaseOrderMapper;
    @Mock
    private ErpPurchaseOrderService purchaseOrderService;
    @Mock
    private ErpSupplierMapper supplierMapper;
    @Mock
    private ErpKingdeeSupplierSyncRecordMapper supplierSyncRecordMapper;
    @Mock
    private ErpProductMapper productMapper;
    @Mock
    private ErpProductCategoryMapper productCategoryMapper;
    @Mock
    private ErpProductUnitMapper productUnitMapper;

    private ErpKingdeeProperties kingdeeProperties;
    private ErpKingdeePurchaseOrderSyncServiceImpl syncService;

    @BeforeEach
    void setUp() {
        kingdeeProperties = buildConfiguredProperties();
        when(kingdeeConfigService.getEffectiveProperties()).thenReturn(kingdeeProperties);
        org.mockito.Mockito.lenient().when(productUnitMapper.selectByName("PCS"))
                .thenReturn(new ErpProductUnitDO().setId(40L).setName("PCS"));
        syncService = new ErpKingdeePurchaseOrderSyncServiceImpl(
                kingdeePurchaseOrderClient, kingdeeConfigService, syncRecordMapper, purchaseOrderMapper, purchaseOrderService,
                supplierMapper, supplierSyncRecordMapper, productMapper, productCategoryMapper, productUnitMapper);
    }

    @Test
    void syncPurchaseOrders_observesStopBeforeWritingNextOrder() {
        when(kingdeePurchaseOrderClient.fetchPurchaseOrders(kingdeeProperties))
                .thenReturn(List.of(buildKingdeeOrder("10001", "PO20260512001", "MAT-001")));
        Thread.currentThread().interrupt();
        try {
            org.junit.jupiter.api.Assertions.assertThrows(java.util.concurrent.CancellationException.class,
                    () -> syncService.syncPurchaseOrders());
            verifyNoInteractions(purchaseOrderService, productMapper);
            verify(syncRecordMapper, never()).insert(any(ErpKingdeePurchaseOrderSyncRecordDO.class));
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void syncPurchaseOrders_createsLocalPurchaseOrderAndRecord() {
        ErpKingdeePurchaseOrder kingdeeOrder = buildKingdeeOrder("10001", "PO20260512001", "MAT-001");
        when(kingdeePurchaseOrderClient.fetchPurchaseOrders(kingdeeProperties)).thenReturn(List.of(kingdeeOrder));
        when(syncRecordMapper.selectBySourceKey(ErpKingdeePurchaseOrder.FORM_ID, "10001")).thenReturn(null);
        when(purchaseOrderService.createPurchaseOrder(any(ErpPurchaseOrderSaveReqVO.class))).thenReturn(501L);

        ErpKingdeePurchaseOrderSyncResult result = syncService.syncPurchaseOrders();

        assertEquals(1, result.getCreatedCount());
        assertEquals(0, result.getSkippedCount());
        assertEquals(List.of(501L), result.getCreatedPurchaseOrderIds());

        ArgumentCaptor<ErpPurchaseOrderSaveReqVO> orderCaptor = ArgumentCaptor.forClass(ErpPurchaseOrderSaveReqVO.class);
        verify(purchaseOrderService).createPurchaseOrder(orderCaptor.capture());
        ErpPurchaseOrderSaveReqVO createReqVO = orderCaptor.getValue();
        assertEquals(10L, createReqVO.getSupplierId());
        assertEquals(new BigDecimal("10"), createReqVO.getDepositPrice());
        assertEquals(new BigDecimal("60.12"), createReqVO.getKingdeeAmounts().getTotalAmount());
        assertEquals(LocalDateTime.of(2026, 5, 12, 9, 30), createReqVO.getOrderTime());
        assertEquals(1, createReqVO.getItems().size());
        assertEquals(20L, createReqVO.getItems().get(0).getProductId());
        assertEquals(new BigDecimal("3.500"), createReqVO.getItems().get(0).getCount());
        assertEquals(new BigDecimal("15.20"), createReqVO.getItems().get(0).getProductPrice());
        assertEquals(new BigDecimal("13"), createReqVO.getItems().get(0).getTaxPercent());

        ArgumentCaptor<ErpKingdeePurchaseOrderSyncRecordDO> recordCaptor =
                ArgumentCaptor.forClass(ErpKingdeePurchaseOrderSyncRecordDO.class);
        verify(syncRecordMapper).insert(recordCaptor.capture());
        ErpKingdeePurchaseOrderSyncRecordDO syncRecord = recordCaptor.getValue();
        assertEquals(ErpKingdeePurchaseOrder.FORM_ID, syncRecord.getSourceFormId());
        assertEquals("10001", syncRecord.getSourceFid());
        assertEquals("PO20260512001", syncRecord.getSourceBillNo());
        assertEquals(501L, syncRecord.getPurchaseOrderId());
        assertEquals(ErpKingdeePurchaseOrderSyncRecordDO.SYNC_STATUS_SUCCESS, syncRecord.getSyncStatus());
    }

    @Test
    void fullHistoryCreatesSourceDraftWithoutInventingSupplierMaterialOrUnit() {
        var order = buildKingdeeOrder("158553", "test", "MAT-001");
        order.setDocumentStatus("Z");
        order.setSupplierNumber(null);
        order.setSupplierName(null);
        order.getLines().get(0).setMaterialNumber(null);
        order.getLines().get(0).setMaterialName(null);
        order.getLines().get(0).setUnitName(null);
        order.getLines().get(0).setPriceUnitName(null);
        when(kingdeePurchaseOrderClient.fetchAllPurchaseOrders(kingdeeProperties)).thenReturn(List.of(order));
        when(purchaseOrderService.createPurchaseOrder(any())).thenReturn(501L);
        var result = syncService.syncAllWithUpdates();
        assertEquals(1, result.getCreatedCount());
        assertEquals(0, result.getSkippedCount());
        var request = ArgumentCaptor.forClass(ErpPurchaseOrderSaveReqVO.class);
        verify(purchaseOrderService).createPurchaseOrder(request.capture());
        org.junit.jupiter.api.Assertions.assertNull(request.getValue().getSupplierId());
        org.junit.jupiter.api.Assertions.assertNull(request.getValue().getItems().get(0).getProductId());
        org.junit.jupiter.api.Assertions.assertNull(request.getValue().getItems().get(0).getProductUnitId());
        assertEquals(order.getLines().get(0).getPrice(), request.getValue().getItems().get(0).getProductPrice());
        verify(purchaseOrderService, never()).updatePurchaseOrderStatus(any(), any());
        org.mockito.Mockito.verifyNoInteractions(supplierMapper, supplierSyncRecordMapper, productMapper, productUnitMapper);
        verify(syncRecordMapper).insert(any(ErpKingdeePurchaseOrderSyncRecordDO.class));
    }

    @Test
    void syncPurchaseOrders_updatesExistingSourceRecord() {
        ErpKingdeePurchaseOrder kingdeeOrder = buildKingdeeOrder("10001", "PO20260512001", "MAT-001");
        when(kingdeePurchaseOrderClient.fetchPurchaseOrders(kingdeeProperties)).thenReturn(List.of(kingdeeOrder));
        when(syncRecordMapper.selectBySourceKey(ErpKingdeePurchaseOrder.FORM_ID, "10001"))
                .thenReturn(new ErpKingdeePurchaseOrderSyncRecordDO().setId(301L).setPurchaseOrderId(501L));
        when(purchaseOrderService.getPurchaseOrder(501L))
                .thenReturn(new ErpPurchaseOrderDO().setId(501L).setStatus(ErpAuditStatus.PROCESS.getStatus()));

        ErpKingdeePurchaseOrderSyncResult result = syncService.syncPurchaseOrders();

        assertEquals(0, result.getCreatedCount());
        assertEquals(1, result.getUpdatedCount());
        assertEquals(0, result.getSkippedCount());
        verify(purchaseOrderService, never()).createPurchaseOrder(any());
        verify(purchaseOrderService).updatePurchaseOrder(any(ErpPurchaseOrderSaveReqVO.class));
        verify(syncRecordMapper, never()).insert(any(ErpKingdeePurchaseOrderSyncRecordDO.class));
    }

    @Test
    void syncAllWithUpdates_readsFullHistoryAndUpdatesExistingSourceRecord() {
        ErpKingdeePurchaseOrder kingdeeOrder = buildKingdeeOrder("10001", "PO20260512001", "MAT-001");
        when(kingdeePurchaseOrderClient.fetchAllPurchaseOrders(kingdeeProperties)).thenReturn(List.of(kingdeeOrder));
        when(syncRecordMapper.selectBySourceKey(ErpKingdeePurchaseOrder.FORM_ID, "10001"))
                .thenReturn(new ErpKingdeePurchaseOrderSyncRecordDO().setId(301L).setPurchaseOrderId(501L));
        when(purchaseOrderService.getPurchaseOrder(501L))
                .thenReturn(new ErpPurchaseOrderDO().setId(501L).setStatus(ErpAuditStatus.PROCESS.getStatus()));

        ErpKingdeePurchaseOrderSyncResult result = syncService.syncAllWithUpdates();

        assertEquals(0, result.getCreatedCount());
        assertEquals(1, result.getUpdatedCount());
        assertEquals(0, result.getSkippedCount());
        verify(purchaseOrderService, never()).createPurchaseOrder(any());
        verify(purchaseOrderService).updatePurchaseOrder(any(ErpPurchaseOrderSaveReqVO.class));
        verify(syncRecordMapper, never()).insert(any(ErpKingdeePurchaseOrderSyncRecordDO.class));
    }

    @Test
    void syncPurchaseOrdersModifiedBetween_usesModifyTimeWindowAndUpdatesExistingSourceRecord() {
        LocalDateTime windowStart = LocalDateTime.of(2026, 6, 12, 8, 0);
        LocalDateTime windowEnd = LocalDateTime.of(2026, 6, 12, 9, 0);
        ErpKingdeePurchaseOrder kingdeeOrder = buildKingdeeOrder("10001", "PO20260512001", "MAT-001");
        when(kingdeePurchaseOrderClient.fetchPurchaseOrdersModifiedBetween(kingdeeProperties, windowStart, windowEnd))
                .thenReturn(List.of(kingdeeOrder));
        when(syncRecordMapper.selectBySourceKey(ErpKingdeePurchaseOrder.FORM_ID, "10001"))
                .thenReturn(new ErpKingdeePurchaseOrderSyncRecordDO().setId(301L).setPurchaseOrderId(501L));
        when(purchaseOrderService.getPurchaseOrder(501L))
                .thenReturn(new ErpPurchaseOrderDO().setId(501L).setStatus(ErpAuditStatus.PROCESS.getStatus()));

        ErpKingdeePurchaseOrderSyncResult result = syncService.syncPurchaseOrdersModifiedBetween(windowStart, windowEnd);

        assertEquals(0, result.getCreatedCount());
        assertEquals(1, result.getUpdatedCount());
        assertEquals(0, result.getSkippedCount());
        verify(kingdeePurchaseOrderClient).fetchPurchaseOrdersModifiedBetween(kingdeeProperties, windowStart, windowEnd);
        verify(kingdeePurchaseOrderClient, never()).fetchPurchaseOrders(kingdeeProperties);
        verify(purchaseOrderService, never()).createPurchaseOrder(any());
        verify(purchaseOrderService).updatePurchaseOrder(any(ErpPurchaseOrderSaveReqVO.class));
        verify(syncRecordMapper, never()).insert(any(ErpKingdeePurchaseOrderSyncRecordDO.class));
    }

    @Test
    void syncPurchaseOrdersModifiedBetween_updatesExistingSourceRecordAndRestoresApprovedStatus() {
        LocalDateTime windowStart = LocalDateTime.of(2026, 6, 12, 8, 0);
        LocalDateTime windowEnd = LocalDateTime.of(2026, 6, 12, 9, 0);
        ErpKingdeePurchaseOrder kingdeeOrder = buildKingdeeOrder("10001", "PO20260512001", "MAT-001");
        when(kingdeePurchaseOrderClient.fetchPurchaseOrdersModifiedBetween(kingdeeProperties, windowStart, windowEnd))
                .thenReturn(List.of(kingdeeOrder));
        when(syncRecordMapper.selectBySourceKey(ErpKingdeePurchaseOrder.FORM_ID, "10001"))
                .thenReturn(new ErpKingdeePurchaseOrderSyncRecordDO()
                        .setId(301L)
                        .setPurchaseOrderId(501L));
        when(purchaseOrderService.getPurchaseOrder(501L))
                .thenReturn(new ErpPurchaseOrderDO().setId(501L).setStatus(ErpAuditStatus.APPROVE.getStatus()));

        ErpKingdeePurchaseOrderSyncResult result = syncService.syncPurchaseOrdersModifiedBetween(windowStart, windowEnd);

        assertEquals(0, result.getCreatedCount());
        assertEquals(1, result.getUpdatedCount());
        assertEquals(0, result.getSkippedCount());
        ArgumentCaptor<ErpPurchaseOrderSaveReqVO> orderCaptor = ArgumentCaptor.forClass(ErpPurchaseOrderSaveReqVO.class);
        verify(purchaseOrderService).updatePurchaseOrder(orderCaptor.capture());
        assertEquals(501L, orderCaptor.getValue().getId());
        assertEquals(10L, orderCaptor.getValue().getSupplierId());
        assertEquals(20L, orderCaptor.getValue().getItems().get(0).getProductId());
        verify(purchaseOrderService).updatePurchaseOrderStatus(501L, ErpAuditStatus.PROCESS.getStatus());
        verify(purchaseOrderService).updatePurchaseOrderStatus(501L, ErpAuditStatus.APPROVE.getStatus());
        ArgumentCaptor<ErpPurchaseOrderDO> statusCaptor = ArgumentCaptor.forClass(ErpPurchaseOrderDO.class);
        verify(purchaseOrderMapper).updateById(statusCaptor.capture());
        assertEquals("B", statusCaptor.getValue().getKingdeeCloseStatus());
        assertEquals("A", statusCaptor.getValue().getKingdeeCancelStatus());
        verify(syncRecordMapper).updateById(any(ErpKingdeePurchaseOrderSyncRecordDO.class));
        verify(syncRecordMapper, never()).insert(any(ErpKingdeePurchaseOrderSyncRecordDO.class));
    }

    @Test
    void syncPurchaseOrders_missingBaseUrlFailsBeforeExternalCall() {
        kingdeeProperties.setBaseUrl("");

        assertServiceException(() -> syncService.syncPurchaseOrders(),
                ErrorCodeConstants.KINGDEE_PURCHASE_ORDER_CONFIG_MISSING,
                "yudao.erp.kingdee.base-url");
        verifyNoInteractions(kingdeePurchaseOrderClient, syncRecordMapper, purchaseOrderService);
    }

    @Test
    void syncPurchaseOrders_missingMaterialDetailFailsWithoutCreatingOrder() {
        ErpKingdeePurchaseOrder kingdeeOrder = buildKingdeeOrder("10001", "PO20260512001", "MAT-NO-MAP");
        kingdeeProperties.getPurchaseOrder().setSupplierMappings(Map.of());
        kingdeeProperties.getPurchaseOrder().setMaterialMappings(Map.of());
        when(kingdeePurchaseOrderClient.fetchPurchaseOrders(kingdeeProperties)).thenReturn(List.of(kingdeeOrder));
        when(kingdeePurchaseOrderClient.fetchMaterialDetails(eq(kingdeeProperties), any())).thenReturn(Map.of());
        when(syncRecordMapper.selectBySourceKey(ErpKingdeePurchaseOrder.FORM_ID, "10001")).thenReturn(null);
        when(supplierSyncRecordMapper.selectBySourceSupplierNumber("SUP-001"))
                .thenReturn(new ErpKingdeeSupplierSyncRecordDO().setSupplierId(10L));
        when(productMapper.selectByBarCode("MAT-NO-MAP")).thenReturn(null);

        assertServiceException(() -> syncService.syncPurchaseOrders(),
                ErrorCodeConstants.KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID,
                "material detail is missing for material MAT-NO-MAP");
        verify(purchaseOrderService, never()).createPurchaseOrder(any());
        verify(syncRecordMapper, never()).insert(any(ErpKingdeePurchaseOrderSyncRecordDO.class));
    }

    @Test
    void syncPurchaseOrders_autoProvisionsMissingSupplierAndProductMasters() {
        ErpKingdeePurchaseOrder kingdeeOrder = buildKingdeeOrder("10001", "PO20260512001", "MAT-001");
        kingdeeProperties.getPurchaseOrder().setSupplierMappings(Map.of());
        kingdeeProperties.getPurchaseOrder().setMaterialMappings(Map.of());
        when(kingdeePurchaseOrderClient.fetchPurchaseOrders(kingdeeProperties)).thenReturn(List.of(kingdeeOrder));
        when(kingdeePurchaseOrderClient.fetchMaterialDetails(eq(kingdeeProperties), any()))
                .thenReturn(Map.of("MAT-001", buildMaterialDetail()));
        when(syncRecordMapper.selectBySourceKey(ErpKingdeePurchaseOrder.FORM_ID, "10001")).thenReturn(null);
        when(supplierSyncRecordMapper.selectBySourceSupplierNumber("SUP-001")).thenReturn(null);
        when(productMapper.selectByBarCode("MAT-001")).thenReturn(null);
        when(productCategoryMapper.selectByCode("CHLB05_SYS")).thenReturn(null);
        when(productUnitMapper.selectByName("PCS")).thenReturn(null);
        when(purchaseOrderService.createPurchaseOrder(any(ErpPurchaseOrderSaveReqVO.class))).thenReturn(501L);
        doAnswer(invocation -> {
            ErpSupplierDO supplier = invocation.getArgument(0);
            supplier.setId(10L);
            return 1;
        }).when(supplierMapper).insert(any(ErpSupplierDO.class));
        doAnswer(invocation -> {
            ErpProductCategoryDO category = invocation.getArgument(0);
            category.setId(30L);
            return 1;
        }).when(productCategoryMapper).insert(any(ErpProductCategoryDO.class));
        doAnswer(invocation -> {
            ErpProductUnitDO unit = invocation.getArgument(0);
            unit.setId(40L);
            when(productUnitMapper.selectByName("PCS")).thenReturn(unit);
            return 1;
        }).when(productUnitMapper).insert(any(ErpProductUnitDO.class));
        doAnswer(invocation -> {
            ErpProductDO product = invocation.getArgument(0);
            product.setId(20L);
            return 1;
        }).when(productMapper).insert(any(ErpProductDO.class));

        ErpKingdeePurchaseOrderSyncResult result = syncService.syncPurchaseOrders();

        assertEquals(1, result.getCreatedCount());
        ArgumentCaptor<ErpPurchaseOrderSaveReqVO> orderCaptor = ArgumentCaptor.forClass(ErpPurchaseOrderSaveReqVO.class);
        verify(purchaseOrderService).createPurchaseOrder(orderCaptor.capture());
        ErpPurchaseOrderSaveReqVO createReqVO = orderCaptor.getValue();
        assertEquals(10L, createReqVO.getSupplierId());
        assertEquals(new BigDecimal("10"), createReqVO.getDepositPrice());
        assertEquals(new BigDecimal("60.12"), createReqVO.getKingdeeAmounts().getTotalAmount());
        assertEquals(20L, createReqVO.getItems().get(0).getProductId());
        verify(supplierMapper).insert(any(ErpSupplierDO.class));
        verify(supplierSyncRecordMapper).insert(any(ErpKingdeeSupplierSyncRecordDO.class));
        verify(productCategoryMapper).insert(any(ErpProductCategoryDO.class));
        verify(productUnitMapper).insert(any(ErpProductUnitDO.class));
        verify(productMapper).insert(any(ErpProductDO.class));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"false,false", "true,false", "false,true", "true,true"})
    void sourceAmountDifferencesArePreservedAndReportedOnCreateAndUpdate(boolean updating, boolean headerOnly) {
        var source = buildKingdeeOrder("264222", "881CGDD31637", "MAT-001");
        source.setTotalProductAmount(BigDecimal.ZERO);
        source.setTotalTaxAmount(BigDecimal.ZERO);
        source.setTotalAmount(new BigDecimal("448.25"));
        source.getLines().get(0).setNetAmount(BigDecimal.ZERO);
        source.getLines().get(0).setTaxAmount(BigDecimal.ZERO);
        source.getLines().get(0).setTotalAmount(headerOnly ? BigDecimal.ZERO : new BigDecimal("448.25"));
        when(kingdeePurchaseOrderClient.fetchPurchaseOrders(kingdeeProperties)).thenReturn(List.of(source));
        if (updating) {
            when(syncRecordMapper.selectBySourceKey(ErpKingdeePurchaseOrder.FORM_ID, "264222"))
                    .thenReturn(new ErpKingdeePurchaseOrderSyncRecordDO().setId(301L).setPurchaseOrderId(501L));
            when(purchaseOrderService.getPurchaseOrder(501L))
                    .thenReturn(new ErpPurchaseOrderDO().setId(501L).setStatus(ErpAuditStatus.PROCESS.getStatus()));
        } else {
            when(purchaseOrderService.createPurchaseOrder(any(ErpPurchaseOrderSaveReqVO.class))).thenReturn(501L);
        }
        var logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(ErpKingdeePurchaseOrderSyncServiceImpl.class);
        var logs = new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
        logs.start();
        logger.addAppender(logs);
        try {
            var result = syncService.syncPurchaseOrders();
            assertEquals(updating ? 0 : 1, result.getCreatedCount());
            assertEquals(updating ? 1 : 0, result.getUpdatedCount());
            var request = ArgumentCaptor.forClass(ErpPurchaseOrderSaveReqVO.class);
            var record = ArgumentCaptor.forClass(ErpKingdeePurchaseOrderSyncRecordDO.class);
            if (updating) {
                verify(purchaseOrderService).updatePurchaseOrder(request.capture());
                verify(syncRecordMapper).updateById(record.capture());
            } else {
                verify(purchaseOrderService).createPurchaseOrder(request.capture());
                verify(syncRecordMapper).insert(record.capture());
            }
            assertEquals(BigDecimal.ZERO, request.getValue().getKingdeeAmounts().getProductAmount());
            assertEquals(BigDecimal.ZERO, request.getValue().getKingdeeAmounts().getTaxAmount());
            assertEquals(new BigDecimal("448.25"), request.getValue().getKingdeeAmounts().getTotalAmount());
            var persistedSource = cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(
                    record.getValue().getRawPayload(), ErpKingdeePurchaseOrder.class);
            assertEquals(new BigDecimal("448.25"), persistedSource.getTotalAmount());
            assertEquals(source.getLines().get(0).getTotalAmount(), persistedSource.getLines().get(0).getTotalAmount());
            assertEquals(1, logs.list.stream().filter(event -> event.getLevel() == ch.qos.logback.classic.Level.WARN
                    && event.getFormattedMessage().contains("采购ERP原值快照金额差异")
                    && event.getFormattedMessage().contains("264222")
                    && event.getFormattedMessage().contains("881CGDD31637")
                    && event.getFormattedMessage().contains("448.25")).count());
        } finally {
            logger.detachAppender(logs);
            logs.stop();
        }
    }

    private static ErpKingdeeProperties buildConfiguredProperties() {
        ErpKingdeeProperties properties = new ErpKingdeeProperties();
        properties.setBaseUrl("https://k3.example.com/K3Cloud");
        properties.setAcctId("acct");
        properties.setUsername("kingdee-user");
        properties.setPassword("kingdee-password");
        properties.setLcid(2052);

        ErpKingdeeProperties.PurchaseOrderProperties purchaseOrder = new ErpKingdeeProperties.PurchaseOrderProperties();
        purchaseOrder.setPurchaseOrgNumber("881");
        purchaseOrder.setQueryDays(1);
        purchaseOrder.setQueryLimit(200);
        purchaseOrder.setSupplierMappings(Map.of("SUP-001", 10L));
        purchaseOrder.setMaterialMappings(Map.of("MAT-001", 20L));
        properties.setPurchaseOrder(purchaseOrder);
        return properties;
    }

    private static ErpKingdeePurchaseOrder buildKingdeeOrder(String fid, String billNo, String materialNumber) {
        ErpKingdeePurchaseOrder.Line line = new ErpKingdeePurchaseOrder.Line();
        line.setUnitName("PCS");
        line.setPriceUnitName("PCS");
        line.setNetAmount(new BigDecimal("53.20"));
        line.setTaxAmount(new BigDecimal("6.92"));
        line.setTotalAmount(new BigDecimal("60.12"));
        line.setMaterialNumber(materialNumber);
        line.setMaterialName("Material A");
        line.setQuantity(new BigDecimal("3.500"));
        line.setPriceUnitQuantity(line.getQuantity());
        line.setPrice(new BigDecimal("15.20"));
        line.setTaxPercent(new BigDecimal("13"));
        line.setRemark("Line remark");

        ErpKingdeePurchaseOrder order = new ErpKingdeePurchaseOrder();
        order.setFid(fid);
        order.setBillNo(billNo);
        order.setBillDate(LocalDateTime.of(2026, 5, 12, 9, 30));
        order.setDocumentStatus("C");
        order.setCloseStatus("B");
        order.setCancelStatus("A");
        order.setSupplierNumber("SUP-001");
        order.setSupplierName("Supplier A");
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setDepositAmount(new BigDecimal("10"));
        order.setTotalProductAmount(new BigDecimal("53.20"));
        order.setTotalTaxAmount(new BigDecimal("6.92"));
        order.setTotalAmount(new BigDecimal("60.12"));
        order.setLines(List.of(line));
        return order;
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
