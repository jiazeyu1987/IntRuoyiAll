package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.*;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesKingdeeProductionMaterialListDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesKingdeeProductionMaterialListMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.testdata.MesLocalTestRunManifestDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.testdata.MesLocalTestRunManifestMapper;
import cn.iocoder.yudao.module.mes.service.pro.testdata.MesLocalTestRunInputHash;
import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesLocalTestRunSourceGuardTest {
    final MesLocalTestRunManifestMapper manifests = mock(MesLocalTestRunManifestMapper.class);
    final MesProWorkOrderMapper orders = mock(MesProWorkOrderMapper.class);
    final ErpKingdeeProductionPickListMapper picks = mock(ErpKingdeeProductionPickListMapper.class);
    final ErpKingdeeProductionPickListItemMapper items = mock(ErpKingdeeProductionPickListItemMapper.class);
    final ErpKingdeeProductionReplenishmentListMapper replenishments = mock(ErpKingdeeProductionReplenishmentListMapper.class);
    final ErpKingdeeProductionReplenishmentListItemMapper replenishmentItems = mock(ErpKingdeeProductionReplenishmentListItemMapper.class);
    final MesFixedTestOrderModeGuard mode = mock(MesFixedTestOrderModeGuard.class);
    final MesKingdeeProductionMaterialListMapper materials = mock(MesKingdeeProductionMaterialListMapper.class);
    final MesLocalTestRunSourceGuard guard = new MesLocalTestRunSourceGuard(manifests, orders, picks, items, replenishments, replenishmentItems, mode, materials);
    MesLocalTestRunManifestDO manifest;
    MesProWorkOrderDO order;
    ErpKingdeeProductionPickListDO header;
    ErpKingdeeProductionPickListItemDO item;

    @BeforeEach void fixture() {
        TenantContextHolder.setTenantId(165L);
        order = MesProWorkOrderDO.builder().id(10L).code("LT-RUN1-A").productId(20L).quantity(new BigDecimal("100"))
                .batchCode("BATCH-A").orderSourceCode("LOCAL_TEST_RUN:1").build();
        order.setTenantId(165L);
        manifest = new MesLocalTestRunManifestDO();
        manifest.setId(1L); manifest.setTenantId(165L); manifest.setSource("LOCAL_TEST_RUN");
        manifest.setRunId("RUN1"); manifest.setSlot("A"); manifest.setWorkOrderId(10L);
        manifest.setRequestId(2L); manifest.setProjectId(3L); manifest.setCatalogId(4L); manifest.setRelationId(5L);
        manifest.setMesProductId(20L); manifest.setQuantity(new BigDecimal("100.000")); manifest.setBatchCode("BATCH-A");
        manifest.setIncludeReplenishment(false); manifest.setPickHeaderId(30L); manifest.setPickItemIdsJson("[31]");
        manifest.setReplenishmentItemIdsJson("[]"); manifest.setInputHash(MesLocalTestRunInputHash.hash(manifest));
        header = ErpKingdeeProductionPickListDO.builder().id(30L).sourceFormId("LOCAL_TEST_RUN")
                .sourceFid("LOCAL_TEST_RUN:1:P").sourceBillNo("LT-RUN1-A-P").documentStatus("LOCAL_TEST").build();
        header.setTenantId(165L);
        item = ErpKingdeeProductionPickListItemDO.builder().id(31L).productionPickListId(30L)
                .sourceFormId("LOCAL_TEST_RUN").sourceFid(header.getSourceFid()).sourceBillNo(header.getSourceBillNo())
                .sourceEntryId("101").sourceLineKey("LOCAL_TEST_RUN:1:P:101").productionOrderNo(order.getCode())
                .materialNumber("MAT-1").actualQuantity(new BigDecimal("100")).build();
        item.setTenantId(165L);
        manifest.setPickSnapshotHash(MesLocalTestRunSourceGuard.pickSnapshotHash(header, List.of(item)));
        var material = MesKingdeeProductionMaterialListDO.builder().id(50L).sourceFormId("LOCAL_TEST_RUN")
                .sourceBillNo(order.getCode() + "-M").sourceEntryId("101").productionOrderStatus("LOCAL_TEST")
                .productionOrderNo(order.getCode()).workOrderCode(order.getCode()).workOrderId(order.getId())
                .workOrderBomId(101L).productId(20L).childMaterialCode("MAT-1").requiredQuantity(new BigDecimal("100")).build();
        manifest.setMaterialItemIdsJson("[50]");
        manifest.setMaterialSnapshotHash(MesLocalTestRunSourceGuard.materialSnapshotHash(List.of(material)));
        when(materials.selectListByProductionOrderNo(order.getCode())).thenReturn(List.of(material));
        when(manifests.selectByWorkOrderId(10L)).thenReturn(manifest);
        when(picks.selectById(30L)).thenReturn(header);
        when(items.selectListByPickListIds(List.of(30L))).thenReturn(List.of(item));
        when(items.selectListByProductionOrderNo(order.getCode())).thenReturn(List.of(item));
        when(replenishmentItems.selectListByProductionOrderNo(order.getCode())).thenReturn(List.of());
    }
    @AfterEach void clear() { TenantContextHolder.clear(); }

    @Test void localProgressionUsesModeButReadOnlyProvenanceDoesNot() {
        doThrow(new IllegalStateException("MODE_DISABLED")).when(mode).requireEnabled(any());
        assertTrue(guard.validate(order, false));
        assertThrows(IllegalStateException.class, () -> guard.validate(order, true));
        verify(mode).requireEnabled(any());
    }
    @Test void wrongTenantProductQuantityOrBatchAreRejected() {
        order.setTenantId(166L); assertInvalid(); order.setTenantId(165L);
        order.setProductId(99L); assertInvalid(); order.setProductId(20L);
        order.setQuantity(BigDecimal.ONE); assertInvalid(); order.setQuantity(new BigDecimal("100"));
        order.setBatchCode("OTHER"); assertInvalid();
    }
    @Test void prefixAloneCannotAuthorizeLocalDocuments() {
        when(manifests.selectByWorkOrderId(10L)).thenReturn(null);
        assertInvalid();
        order.setOrderSourceCode(null);
        assertFalse(guard.validate(order, false));
        var resolver = new MesFormalProductionPickListSourceResolver(orders, picks, items, guard);
        when(orders.selectById(10L)).thenReturn(order);
        assertThrows(MesFormalProductionPickListSourceException.class, () -> resolver.resolve(10L));
    }
    @Test void statusCDoesNotMasqueradeAsLocalApproval() {
        header.setDocumentStatus("C"); assertInvalid();
    }
    @Test void changedMaterialOrDocumentOrExtraEntryAreRejected() {
        item.setMaterialNumber("CHANGED"); assertInvalid(); item.setMaterialNumber("MAT-1");
        item.setProductionPickListId(99L); assertInvalid(); item.setProductionPickListId(30L);
        when(items.selectListByProductionOrderNo(order.getCode())).thenReturn(List.of(item,
                ErpKingdeeProductionPickListItemDO.builder().id(32L).productionPickListId(30L).build()));
        assertInvalid();
    }
    @Test void nullDuplicateAndEmptyPickIdentitySetsAreRejected() {
        for (String json : List.of("[null]", "[31,31]", "[]")) { manifest.setPickItemIdsJson(json); assertInvalid(); }
    }
    @Test void noReplenishmentManifestRejectsUnexpectedErpOrLocalReplenishment() {
        when(replenishmentItems.selectListByProductionOrderNo(order.getCode()))
                .thenReturn(List.of(ErpKingdeeProductionReplenishmentListItemDO.builder().id(90L).build()));
        assertInvalid();
    }
    @Test void exactOptionalReplenishmentIsAcceptedAndChangesAreRejected() {
        manifest.setIncludeReplenishment(true); manifest.setReplenishmentHeaderId(40L); manifest.setReplenishmentItemIdsJson("[41]");
        manifest.setInputHash(MesLocalTestRunInputHash.hash(manifest));
        var replenishment = ErpKingdeeProductionReplenishmentListDO.builder().id(40L).sourceFormId("LOCAL_TEST_RUN")
                .sourceFid("LOCAL_TEST_RUN:1:R").sourceBillNo("LT-RUN1-A-R").documentStatus("LOCAL_TEST").build();
        replenishment.setTenantId(165L);
        var line = ErpKingdeeProductionReplenishmentListItemDO.builder().id(41L).productionReplenishmentListId(40L)
                .sourceFormId("LOCAL_TEST_RUN").sourceFid(replenishment.getSourceFid()).sourceBillNo(replenishment.getSourceBillNo())
                .sourceEntryId("101").sourceLineKey("LOCAL_TEST_RUN:1:R:101").productionOrderNo(order.getCode())
                .materialNumber("MAT-1").actualQuantity(BigDecimal.ONE).build();
        line.setTenantId(165L);
        manifest.setReplenishmentSnapshotHash(MesLocalTestRunSourceGuard.replenishmentSnapshotHash(replenishment, List.of(line)));
        when(replenishments.selectById(40L)).thenReturn(replenishment);
        when(replenishmentItems.selectListByProductionOrderNo(order.getCode())).thenReturn(List.of(line));
        when(replenishmentItems.selectListByReplenishmentListIds(List.of(40L))).thenReturn(List.of(line));
        assertTrue(guard.validate(order, false));
        line.setActualQuantity(BigDecimal.TEN); assertInvalid();
    }
    @Test void resolverAcceptsExplicitLocalSourceAndKeepsLocalStatus() {
        when(orders.selectById(10L)).thenReturn(order);
        var resolver = new MesFormalProductionPickListSourceResolver(orders, picks, items, guard);
        assertEquals("LOCAL_TEST", resolver.resolve(10L).sources().get(0).header().getDocumentStatus());
        verify(mode).requireEnabled(any());
    }
    @Test void admissionRequiresConfirmedButReadOnlyCompletedOrderStillWorks() {
        order.setStatus(0);
        assertThrows(MesFormalProductionPickListSourceException.class, () -> guard.requireProductionAdmission(order));
        order.setStatus(1);
        assertDoesNotThrow(() -> guard.requireProductionAdmission(order));
        order.setStatus(2);
        assertTrue(guard.validate(order, false));
    }
    @Test void materialReplacementAndExtraLinesAreRejected() {
        when(materials.selectListByProductionOrderNo(order.getCode())).thenReturn(List.of(
                MesKingdeeProductionMaterialListDO.builder().id(99L).build()));
        assertInvalid();
    }
    @Test void mutatedManifestInputCannotKeepOldInputHash() {
        manifest.setProjectId(999L);
        assertInvalid();
    }
    @Test void detailReadUsesProvenanceWithoutEnablingProgression() {
        var reader = mock(MesTeamLeaderActiveOrderDetailServiceImpl.class, CALLS_REAL_METHODS);
        var readerGuard = mock(MesLocalTestRunSourceGuard.class);
        var rMapper = mock(ErpKingdeeProductionReplenishmentListMapper.class);
        var rHeader = ErpKingdeeProductionReplenishmentListDO.builder().id(40L).sourceBillNo("LT-RUN1-A-R")
                .sourceFormId("LOCAL_TEST_RUN").documentStatus("LOCAL_TEST").build();
        var rItem = ErpKingdeeProductionReplenishmentListItemDO.builder().id(41L).productionReplenishmentListId(40L).build();
        org.springframework.test.util.ReflectionTestUtils.setField(reader, "localSources", readerGuard);
        org.springframework.test.util.ReflectionTestUtils.setField(reader, "replenishmentListMapper", rMapper);
        when(rMapper.selectBatchIds(List.of(40L))).thenReturn(List.of(rHeader));
        when(readerGuard.validateOrderId(10L, false)).thenReturn(true);
        assertNotNull(org.springframework.test.util.ReflectionTestUtils.invokeMethod(reader,
                "loadReplenishmentHeaders", 10L, 100L, List.of(rItem)));
        verify(readerGuard).validateOrderId(10L, false);
        verify(readerGuard, never()).validateOrderId(10L, true);
    }
    @Test void forgedCWithoutManifestCannotUseLocalHeaderOrItemMarkers() {
        when(manifests.selectByWorkOrderId(10L)).thenReturn(null);
        order.setOrderSourceCode(null);
        header.setDocumentStatus("C");
        when(orders.selectById(10L)).thenReturn(order);
        var resolver = new MesFormalProductionPickListSourceResolver(orders, picks, items, guard);
        assertThrows(MesFormalProductionPickListSourceException.class, () -> resolver.resolve(10L));
    }
    private void assertInvalid() { assertThrows(MesFormalProductionPickListSourceException.class, () -> guard.validate(order, false)); }
}
