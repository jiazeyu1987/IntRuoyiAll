package cn.iocoder.yudao.module.erp.service.sync.admin;

import cn.iocoder.yudao.module.erp.controller.admin.sync.vo.ErpKingdeeProductionOrderCreateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sync.vo.ErpKingdeeProductionOrderCreateRespVO;
import cn.iocoder.yudao.module.erp.service.config.ErpKingdeeConfigService;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProductionOrderClient;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProductionOrderCreateRequest;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProductionOrderCreateResult;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProperties;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErpKingdeeProductionOrderCreateServiceImplTest {

    @Mock
    private ErpKingdeeConfigService kingdeeConfigService;
    @Mock
    private ErpKingdeeProductionOrderClient productionOrderClient;

    private ErpKingdeeProductionOrderCreateServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ErpKingdeeProductionOrderCreateServiceImpl(kingdeeConfigService, productionOrderClient);
    }

    @Test
    void createProductionOrder_usesEffectiveConfigTemplateAndSubmitsToKingdee() {
        ErpKingdeeProperties properties = buildProperties();
        ErpKingdeeProductionOrderCreateReqVO reqVO = buildReqVO();
        ErpKingdeeProductionOrderCreateResult clientResult = ErpKingdeeProductionOrderCreateResult.builder()
                .erpFid("310119")
                .erpBillNo("SMOKE-MO-001")
                .saved(Boolean.TRUE)
                .submitted(Boolean.TRUE)
                .build();
        when(kingdeeConfigService.getEffectiveProperties()).thenReturn(properties);
        when(productionOrderClient.createAndSubmitProductionOrder(eq(properties),
                org.mockito.ArgumentMatchers.any(ErpKingdeeProductionOrderCreateRequest.class)))
                .thenReturn(clientResult);

        ErpKingdeeProductionOrderCreateRespVO result = service.createProductionOrder(reqVO);

        assertEquals("310119", result.getErpFid());
        assertEquals("SMOKE-MO-001", result.getErpBillNo());
        assertEquals(Boolean.TRUE, result.getSaved());
        assertEquals(Boolean.TRUE, result.getSubmitted());
        ArgumentCaptor<ErpKingdeeProductionOrderCreateRequest> requestCaptor =
                ArgumentCaptor.forClass(ErpKingdeeProductionOrderCreateRequest.class);
        verify(productionOrderClient).createAndSubmitProductionOrder(eq(properties), requestCaptor.capture());
        ErpKingdeeProductionOrderCreateRequest request = requestCaptor.getValue();
        assertEquals("SMOKE-MO-001", request.getBillNo());
        assertEquals("TEMPLATE-MO-001", request.getTemplateBillNo());
        assertEquals("MAT-ROUTE-001", request.getMaterialNumber());
        assertEquals("PCS", request.getUnitNumber());
        assertEquals(new BigDecimal("12.5"), request.getQuantity());
        assertEquals(LocalDateTime.of(2026, 6, 15, 8, 0), request.getPlannedStartDate());
        assertEquals(LocalDateTime.of(2026, 6, 16, 18, 0), request.getPlannedFinishDate());
        assertEquals("SMOKE-SO-001", request.getSourceBillNo());
        assertEquals("BATCH-SMOKE-001", request.getBatchNumber());
    }

    @Test
    void createProductionOrder_rejectsWhenExternalWriteDisabledBeforeCallingKingdee() {
        doThrow(new RuntimeException("ERP写权限已关闭")).when(kingdeeConfigService).assertExternalWriteEnabled();

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> service.createProductionOrder(buildReqVO()));

        assertTrue(exception.getMessage().contains("ERP写权限已关闭"));
        verify(kingdeeConfigService).assertExternalWriteEnabled();
        verify(kingdeeConfigService, never()).getEffectiveProperties();
        verifyNoInteractions(productionOrderClient);
    }

    @Test
    void createProductionOrder_rejectsFinishTimeBeforeStartTimeBeforeCallingKingdee() {
        ErpKingdeeProductionOrderCreateReqVO reqVO = buildReqVO();
        reqVO.setPlannedFinishDate(LocalDateTime.of(2026, 6, 14, 8, 0));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> service.createProductionOrder(reqVO));

        assertTrue(exception.getMessage().contains("plannedFinishDate"));
        verifyNoInteractions(kingdeeConfigService, productionOrderClient);
    }

    private static ErpKingdeeProductionOrderCreateReqVO buildReqVO() {
        ErpKingdeeProductionOrderCreateReqVO reqVO = new ErpKingdeeProductionOrderCreateReqVO();
        reqVO.setBillNo("SMOKE-MO-001");
        reqVO.setMaterialNumber("MAT-ROUTE-001");
        reqVO.setUnitNumber("PCS");
        reqVO.setQuantity(new BigDecimal("12.5"));
        reqVO.setPlannedStartDate(LocalDateTime.of(2026, 6, 15, 8, 0));
        reqVO.setPlannedFinishDate(LocalDateTime.of(2026, 6, 16, 18, 0));
        reqVO.setSourceBillNo("SMOKE-SO-001");
        reqVO.setBatchNumber("BATCH-SMOKE-001");
        return reqVO;
    }

    private static ErpKingdeeProperties buildProperties() {
        ErpKingdeeProperties properties = new ErpKingdeeProperties();
        properties.setBaseUrl("https://k3.example.com");
        properties.setAcctId("acct");
        properties.setUsername("kingdee-user");
        properties.setPassword("kingdee-password");
        properties.setLcid(2052);
        properties.getProductionOrder().setQueryLimit(1000);
        properties.getProductionOrder().setTemplateBillNo("TEMPLATE-MO-001");
        return properties;
    }
}
