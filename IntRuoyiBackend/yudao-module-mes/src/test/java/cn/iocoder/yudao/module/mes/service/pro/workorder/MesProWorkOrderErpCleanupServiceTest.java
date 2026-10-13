package cn.iocoder.yudao.module.mes.service.pro.workorder;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.service.config.ErpKingdeeConfigService;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProductionOrderClient;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProperties;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProWorkOrderErpCleanupReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesKingdeeProductionOrderSyncRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.*;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class MesProWorkOrderErpCleanupServiceTest {
    @InjectMocks private MesProWorkOrderErpCleanupService service;
    @Mock private ErpKingdeeProductionOrderClient productionOrderClient;
    @Mock private ErpKingdeeConfigService configService;
    @Mock private MesProWorkOrderMapper workOrderMapper;
    @Mock private MesKingdeeProductionOrderSyncRecordMapper syncRecordMapper;
    @Mock private MesProWorkOrderErpCleanupMapper cleanupMapper;
    @Mock private MesProWorkOrderBomMapper bomMapper;
    @Mock private GxpAuditService gxpAuditService;
    private ErpKingdeeProperties properties;

    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L);
        properties = new ErpKingdeeProperties();
        properties.setBaseUrl("https://erp.example.com"); properties.setAcctId("test-account");
        properties.setUsername("test-user"); properties.setPassword("test-password"); properties.setLcid(2052);
        when(configService.getEffectiveProperties()).thenReturn(properties);
    }
    @AfterEach void cleanup() { TenantContextHolder.clear(); }

    @Test void analysisSeparatesDeletableRetainedAndUnverifiableWithoutWrites() {
        when(workOrderMapper.selectListAll()).thenReturn(List.of(order(1L), order(2L), order(3L), order(4L)));
        when(syncRecordMapper.selectList()).thenReturn(List.of(record(1L), record(2L), record(3L)));
        when(productionOrderClient.findExistingProductionOrderBillNos(eq(properties), anyCollection())).thenReturn(Set.of("MO-1"));
        when(cleanupMapper.selectRetainReason(1L, 2L)).thenReturn(null);
        when(cleanupMapper.selectRetainReason(1L, 3L)).thenReturn("已进入排产，保留工单");
        var result = service.analyze();
        assertEquals(3, result.getAnalyzedCount()); assertEquals(1, result.getUnverifiableCount());
        assertEquals(2, result.getMissingWorkOrders().size());
        assertTrue(result.getMissingWorkOrders().get(0).isDeletable());
        assertFalse(result.getMissingWorkOrders().get(1).isDeletable());
        verify(workOrderMapper, never()).deleteById(anyLong());
        verify(cleanupMapper, never()).deleteSyncRecords(anyLong(), anyLong());
    }

    @Test void analysisErpFailureIsExposedWithoutDeletes() {
        when(workOrderMapper.selectListAll()).thenReturn(List.of(order(1L)));
        when(syncRecordMapper.selectList()).thenReturn(List.of(record(1L)));
        when(productionOrderClient.findExistingProductionOrderBillNos(eq(properties), anyCollection())).thenThrow(new IllegalStateException("ERP 查询失败"));
        assertThrows(IllegalStateException.class, service::analyze);
        verifyNoInteractions(cleanupMapper, bomMapper);
    }

    @Test void confirmationRechecksErpThenDeletesSnapshotsAndWorkOrder() {
        var request = request();
        when(syncRecordMapper.selectByWorkOrderId(1L)).thenReturn(record(1L));
        when(workOrderMapper.selectByIdForUpdate(1L)).thenReturn(order(1L));
        when(productionOrderClient.findExistingProductionOrderBillNos(eq(properties), anyCollection())).thenReturn(Set.of());
        when(cleanupMapper.deleteSyncRecords(1L, 1L)).thenReturn(1);
        when(workOrderMapper.deleteById(1L)).thenReturn(1);
        assertEquals(1, service.deleteMissing(request));
        var sequence = inOrder(gxpAuditService, workOrderMapper, productionOrderClient, bomMapper, cleanupMapper);
        sequence.verify(gxpAuditService).acquireLedgerLock();
        sequence.verify(workOrderMapper).selectByIdForUpdate(1L);
        sequence.verify(productionOrderClient).findExistingProductionOrderBillNos(eq(properties), anyCollection());
        sequence.verify(bomMapper).deleteByWorkOrderId(1L);
        sequence.verify(cleanupMapper).deleteMaterialSnapshots(1L, 1L);
        sequence.verify(cleanupMapper).deleteSyncRecords(1L, 1L);
        sequence.verify(workOrderMapper).deleteById(1L);
    }

    @Test void erpOrderReappearingAfterPreviewBlocksAllWrites() {
        var request = request();
        when(syncRecordMapper.selectByWorkOrderId(1L)).thenReturn(record(1L));
        when(workOrderMapper.selectByIdForUpdate(1L)).thenReturn(order(1L));
        when(productionOrderClient.findExistingProductionOrderBillNos(eq(properties), anyCollection())).thenReturn(Set.of("MO-1"));
        assertTrue(assertThrows(RuntimeException.class, () -> service.deleteMissing(request)).getMessage().contains("ERP 中已存在"));
        verifyNoInteractions(bomMapper);
        verify(cleanupMapper, never()).deleteSyncRecords(anyLong(), anyLong());
    }

    @Test void newProductionHistoryAfterPreviewBlocksDeletion() {
        var request = request();
        when(syncRecordMapper.selectByWorkOrderId(1L)).thenReturn(record(1L));
        when(workOrderMapper.selectByIdForUpdate(1L)).thenReturn(order(1L));
        when(cleanupMapper.selectRetainReason(1L, 1L)).thenReturn("已有生产报工记录");
        assertTrue(assertThrows(RuntimeException.class, () -> service.deleteMissing(request)).getMessage().contains("报工"));
        verifyNoInteractions(productionOrderClient, bomMapper);
    }

    @Test void switchedEnvironmentRequiresNewPreview() {
        var request = request(); properties.setAcctId("different-account");
        assertTrue(assertThrows(RuntimeException.class, () -> service.deleteMissing(request)).getMessage().contains("ERP 环境已变化"));
        verifyNoInteractions(gxpAuditService, bomMapper);
        verify(workOrderMapper, never()).selectByIdForUpdate(anyLong());
    }

    @Test void changedSyncIdentityRequiresNewPreview() {
        var request = request();
        when(syncRecordMapper.selectByWorkOrderId(1L)).thenReturn(record(1L)); request.getItems().get(0).setSourceFid("forged");
        when(workOrderMapper.selectByIdForUpdate(1L)).thenReturn(order(1L));
        assertTrue(assertThrows(RuntimeException.class, () -> service.deleteMissing(request)).getMessage().contains("同步关联已变化"));
        verifyNoInteractions(productionOrderClient, bomMapper);
    }

    @Test void producedOrFrozenWorkOrdersAreRetained() {
        var row = order(1L); row.setQuantityProduced(BigDecimal.ONE);
        when(workOrderMapper.selectListAll()).thenReturn(List.of(row));
        when(syncRecordMapper.selectList()).thenReturn(List.of(record(1L)));
        when(productionOrderClient.findExistingProductionOrderBillNos(eq(properties), anyCollection())).thenReturn(Set.of());
        assertFalse(service.analyze().getMissingWorkOrders().get(0).isDeletable());
        row.setQuantityProduced(BigDecimal.ZERO); row.setTemporaryFrozen(true);
        assertTrue(service.analyze().getMissingWorkOrders().get(0).getRetainReason().contains("冻结"));
    }

    private MesProWorkOrderErpCleanupReqVO request() {
        when(workOrderMapper.selectListAll()).thenReturn(List.of(order(1L)));
        when(syncRecordMapper.selectList()).thenReturn(List.of(record(1L)));
        when(productionOrderClient.findExistingProductionOrderBillNos(eq(properties), anyCollection())).thenReturn(Set.of());
        var preview = service.analyze();
        clearInvocations(productionOrderClient, cleanupMapper, workOrderMapper);
        var request = new MesProWorkOrderErpCleanupReqVO(); request.setEnvironmentKey(preview.getEnvironmentKey());
        var item = new MesProWorkOrderErpCleanupReqVO.Item(); item.setWorkOrderId(1L); item.setSyncRecordId(11L);
        item.setSourceFid("101"); item.setSourceBillNo("MO-1"); item.setSourceMaterialNumber("ITEM-1");
        request.setItems(List.of(item)); return request;
    }
    private MesProWorkOrderDO order(Long id) {
        return new MesProWorkOrderDO().setId(id).setCode("MO-"+id).setStatus(1).setTemporaryFrozen(false);
    }
    private MesKingdeeProductionOrderSyncRecordDO record(Long id) {
        return new MesKingdeeProductionOrderSyncRecordDO().setId(id+10).setWorkOrderId(id)
                .setSourceFid(String.valueOf(id+100)).setSourceBillNo("MO-"+id).setSourceMaterialNumber("ITEM-"+id);
    }
}
