package cn.iocoder.yudao.module.mes.service.pro.workorder.sync;

import cn.iocoder.yudao.module.erp.service.config.ErpKingdeeConfigService;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProductionOrder;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProductionOrderClient;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProductionOrderCreateRequest;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProductionOrderCreateResult;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProperties;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.unitmeasure.MesMdUnitMeasureDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesKingdeeProductionOrderSyncRecordMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProWorkOrderStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProWorkOrderTypeEnum;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import cn.iocoder.yudao.module.mes.service.md.unitmeasure.MesMdUnitMeasureService;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class MesKingdeeProductionOrderCreateServiceImplTest {

    @Mock
    private ErpKingdeeProductionOrderClient productionOrderClient;
    @Mock
    private ErpKingdeeConfigService kingdeeConfigService;
    @Mock
    private MesProWorkOrderService workOrderService;
    @Mock
    private MesMdItemService itemService;
    @Mock
    private MesMdUnitMeasureService unitMeasureService;
    @Mock
    private MesKingdeeProductionOrderSyncRecordMapper syncRecordMapper;

    private ErpKingdeeProperties kingdeeProperties;
    private MesKingdeeProductionOrderCreateServiceImpl createService;

    @BeforeEach
    void setUp() {
        kingdeeProperties = new ErpKingdeeProperties();
        kingdeeProperties.setBaseUrl("https://k3.example.com");
        kingdeeProperties.setAcctId("acct");
        kingdeeProperties.setUsername("kingdee-user");
        kingdeeProperties.setPassword("kingdee-password");
        kingdeeProperties.setLcid(2052);
        kingdeeProperties.getProductionOrder().setQueryLimit(500);
        kingdeeProperties.getProductionOrder().setTemplateBillNo("TEMPLATE-MO-001");
        createService = new MesKingdeeProductionOrderCreateServiceImpl(
                productionOrderClient, kingdeeConfigService, workOrderService, itemService,
                unitMeasureService, syncRecordMapper);
    }

    @Test
    void createAndSubmitProductionOrder_buildsRandomizedCloneRequestWithoutLocalLinkRecord() {
        MesProWorkOrderDO workOrder = buildQualifiedWorkOrder();
        MesKingdeeProductionOrderCreateServiceImpl service = spy(createService);
        doReturn("TEST-ERP-001").when(service).generateTestBillNo(workOrder);
        doReturn(new BigDecimal("256")).when(service).generateTestQuantity();
        when(workOrderService.validateWorkOrderExists(100L)).thenReturn(workOrder);
        when(itemService.getItem(20L)).thenReturn(new MesMdItemDO().setId(20L).setCode("MAT-001").setUnitMeasureId(30L));
        when(unitMeasureService.getUnitMeasure(30L)).thenReturn(new MesMdUnitMeasureDO().setId(30L).setCode("kg"));
        when(kingdeeConfigService.getEffectiveProperties()).thenReturn(kingdeeProperties);
        when(productionOrderClient.getProductionOrderByBillNo(kingdeeProperties, "TEST-ERP-001")).thenReturn(null);
        when(productionOrderClient.createAndSubmitProductionOrder(any(), any()))
                .thenAnswer(invocation -> {
                    ErpKingdeeProductionOrderCreateRequest request = invocation.getArgument(1);
                    return ErpKingdeeProductionOrderCreateResult.builder()
                            .erpFid("310119")
                            .erpBillNo(request.getBillNo())
                            .saved(Boolean.TRUE)
                            .submitted(Boolean.TRUE)
                            .build();
                });

        MesKingdeeProductionOrderCreateResult result = service.createAndSubmitProductionOrder(100L);

        assertEquals(100L, result.getWorkOrderId());
        assertEquals("310119", result.getErpFid());
        assertEquals("TEST-ERP-001", result.getErpBillNo());
        assertEquals(Boolean.TRUE, result.getSaved());
        assertEquals(Boolean.TRUE, result.getSubmitted());
        ArgumentCaptor<ErpKingdeeProductionOrderCreateRequest> requestCaptor =
                ArgumentCaptor.forClass(ErpKingdeeProductionOrderCreateRequest.class);
        verify(productionOrderClient).createAndSubmitProductionOrder(
                eq(kingdeeProperties), requestCaptor.capture());
        assertEquals("TEST-ERP-001", requestCaptor.getValue().getBillNo());
        assertEquals("MAT-001", requestCaptor.getValue().getMaterialNumber());
        assertEquals(new BigDecimal("256"), requestCaptor.getValue().getQuantity());
        assertEquals("TEMPLATE-MO-001", requestCaptor.getValue().getTemplateBillNo());
        verifyNoInteractions(syncRecordMapper);
    }

    @Test
    void createAndSubmitProductionOrder_rejectsWhenExternalWriteDisabledBeforeLoadingWorkOrder() {
        doThrow(new RuntimeException("ERP写权限已关闭")).when(kingdeeConfigService).assertExternalWriteEnabled();

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> createService.createAndSubmitProductionOrder(100L));

        org.assertj.core.api.Assertions.assertThat(exception.getMessage()).contains("ERP写权限已关闭");
        verify(kingdeeConfigService).assertExternalWriteEnabled();
        verifyNoInteractions(workOrderService, itemService, unitMeasureService, productionOrderClient, syncRecordMapper);
    }

    @Test
    void createAndSubmitProductionOrder_allowsRepeatedCloneCreationForSameBaseWorkOrder() {
        MesProWorkOrderDO workOrder = buildQualifiedWorkOrder();
        MesKingdeeProductionOrderCreateServiceImpl service = spy(createService);
        doReturn("TEST-ERP-001", "TEST-ERP-002").when(service).generateTestBillNo(workOrder);
        doReturn(new BigDecimal("256"), new BigDecimal("512")).when(service).generateTestQuantity();
        when(workOrderService.validateWorkOrderExists(100L)).thenReturn(workOrder);
        when(itemService.getItem(20L)).thenReturn(new MesMdItemDO().setId(20L).setCode("MAT-001").setUnitMeasureId(30L));
        when(unitMeasureService.getUnitMeasure(30L)).thenReturn(new MesMdUnitMeasureDO().setId(30L).setCode("kg"));
        when(kingdeeConfigService.getEffectiveProperties()).thenReturn(kingdeeProperties);
        when(productionOrderClient.getProductionOrderByBillNo(kingdeeProperties, "TEST-ERP-001")).thenReturn(null);
        when(productionOrderClient.getProductionOrderByBillNo(kingdeeProperties, "TEST-ERP-002")).thenReturn(null);
        when(productionOrderClient.createAndSubmitProductionOrder(any(), any()))
                .thenAnswer(invocation -> {
                    ErpKingdeeProductionOrderCreateRequest request = invocation.getArgument(1);
                    return ErpKingdeeProductionOrderCreateResult.builder()
                            .erpFid("FID-" + request.getBillNo())
                            .erpBillNo(request.getBillNo())
                            .saved(Boolean.TRUE)
                            .submitted(Boolean.TRUE)
                            .build();
                });

        MesKingdeeProductionOrderCreateResult first = service.createAndSubmitProductionOrder(100L);
        MesKingdeeProductionOrderCreateResult second = service.createAndSubmitProductionOrder(100L);

        assertEquals("TEST-ERP-001", first.getErpBillNo());
        assertEquals("TEST-ERP-002", second.getErpBillNo());
        verify(productionOrderClient, times(2)).createAndSubmitProductionOrder(eq(kingdeeProperties), any());
        verifyNoInteractions(syncRecordMapper);
    }

    @Test
    void createAndSubmitProductionOrder_failsFastWhenWorkOrderIsFinished() {
        MesProWorkOrderDO workOrder = buildQualifiedWorkOrder();
        workOrder.setStatus(MesProWorkOrderStatusEnum.FINISHED.getStatus());
        when(workOrderService.validateWorkOrderExists(100L)).thenReturn(workOrder);

        assertThrows(RuntimeException.class, () -> createService.createAndSubmitProductionOrder(100L));

        verify(kingdeeConfigService, never()).getEffectiveProperties();
        verify(productionOrderClient, never()).createAndSubmitProductionOrder(any(), any());
    }

    @Test
    void createAndSubmitProductionOrder_failsFastWhenKingdeeBillAlreadyExists() {
        MesProWorkOrderDO workOrder = buildQualifiedWorkOrder();
        MesKingdeeProductionOrderCreateServiceImpl service = spy(createService);
        doReturn("TEST-ERP-001").when(service).generateTestBillNo(workOrder);
        when(workOrderService.validateWorkOrderExists(100L)).thenReturn(workOrder);
        when(itemService.getItem(20L)).thenReturn(new MesMdItemDO().setId(20L).setCode("MAT-001").setUnitMeasureId(30L));
        when(unitMeasureService.getUnitMeasure(30L)).thenReturn(new MesMdUnitMeasureDO().setId(30L).setCode("kg"));
        when(kingdeeConfigService.getEffectiveProperties()).thenReturn(kingdeeProperties);
        ErpKingdeeProductionOrder existingOrder = new ErpKingdeeProductionOrder();
        existingOrder.setFid("310119");
        existingOrder.setBillNo("TEST-ERP-001");
        when(productionOrderClient.getProductionOrderByBillNo(kingdeeProperties, "TEST-ERP-001")).thenReturn(existingOrder);

        assertThrows(RuntimeException.class, () -> service.createAndSubmitProductionOrder(100L));

        verify(productionOrderClient, never()).createAndSubmitProductionOrder(any(), any());
        verifyNoInteractions(syncRecordMapper);
    }

    @Test
    void createAndSubmitProductionOrder_doesNotWriteLocalRecordWhenSubmitFailsAfterSave() {
        MesProWorkOrderDO workOrder = buildQualifiedWorkOrder();
        MesKingdeeProductionOrderCreateServiceImpl service = spy(createService);
        doReturn("TEST-ERP-001").when(service).generateTestBillNo(workOrder);
        doReturn(new BigDecimal("256")).when(service).generateTestQuantity();
        when(workOrderService.validateWorkOrderExists(100L)).thenReturn(workOrder);
        when(itemService.getItem(20L)).thenReturn(new MesMdItemDO().setId(20L).setCode("MAT-001").setUnitMeasureId(30L));
        when(unitMeasureService.getUnitMeasure(30L)).thenReturn(new MesMdUnitMeasureDO().setId(30L).setCode("kg"));
        when(kingdeeConfigService.getEffectiveProperties()).thenReturn(kingdeeProperties);
        when(productionOrderClient.getProductionOrderByBillNo(kingdeeProperties, "TEST-ERP-001")).thenReturn(null);
        when(productionOrderClient.createAndSubmitProductionOrder(any(), any()))
                .thenThrow(new RuntimeException("Save succeeded but Submit failed, billNo=TEST-ERP-001, fid=310119"));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> service.createAndSubmitProductionOrder(100L));

        org.assertj.core.api.Assertions.assertThat(exception.getMessage())
                .contains("TEST-ERP-001")
                .contains("310119");
        verifyNoInteractions(syncRecordMapper);
    }

    private static MesProWorkOrderDO buildQualifiedWorkOrder() {
        return MesProWorkOrderDO.builder()
                .id(100L)
                .code("WO-001")
                .type(MesProWorkOrderTypeEnum.SELF.getType())
                .productId(20L)
                .quantity(new BigDecimal("12"))
                .requestDate(LocalDateTime.of(2026, 6, 12, 8, 0))
                .orderSourceCode("SO-001")
                .status(MesProWorkOrderStatusEnum.CONFIRMED.getStatus())
                .temporaryFrozen(Boolean.FALSE)
                .build();
    }
}
