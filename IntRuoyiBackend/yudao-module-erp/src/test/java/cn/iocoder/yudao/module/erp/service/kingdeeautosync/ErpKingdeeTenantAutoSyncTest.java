package cn.iocoder.yudao.module.erp.service.kingdeeautosync;

import cn.hutool.extra.spring.SpringUtil;
import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJobParam;
import cn.iocoder.yudao.module.erp.dal.dataobject.kingdeeautosync.ErpKingdeeTableAutoSyncPlanDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.kingdeeautosync.ErpKingdeeTableAutoSyncPlanItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.kingdeeautosync.ErpKingdeeTableAutoSyncPlanMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.kingdeeautosync.ErpKingdeeTableAutoSyncPlanItemMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class ErpKingdeeTenantAutoSyncTest {
    private final ErpKingdeeTableAutoSyncPlanMapper plans = mock(ErpKingdeeTableAutoSyncPlanMapper.class);
    private final ErpKingdeeTableAutoSyncPlanItemMapper items = mock(ErpKingdeeTableAutoSyncPlanItemMapper.class);
    private final ErpKingdeeTableAutoSyncServiceImpl service = new ErpKingdeeTableAutoSyncServiceImpl();

    private void prepare() {
        TenantContextHolder.setTenantId(163L);
        ReflectionTestUtils.setField(service, "planMapper", plans);
        ReflectionTestUtils.setField(service, "planItemMapper", items);
        when(plans.selectCurrentTenantPlan()).thenReturn(new ErpKingdeeTableAutoSyncPlanDO()
                .setId(7L).setEnabled(true).setDailyStartTime(LocalTime.MIDNIGHT));
        when(items.selectListByPlanId(7L)).thenReturn(List.of(
                new ErpKingdeeTableAutoSyncPlanItemDO().setSyncType("STOCK").setEnabled(true).setSortOrder(20),
                new ErpKingdeeTableAutoSyncPlanItemDO().setSyncType("PRODUCT").setEnabled(true).setSortOrder(10),
                new ErpKingdeeTableAutoSyncPlanItemDO().setSyncType("BOM").setEnabled(false).setSortOrder(30)));
    }

    @AfterEach void clearTenant() { TenantContextHolder.clear(); }

    @Test void duePlanRunsSelectedTablesInOrderOnlyForCurrentTenant() throws Exception {
        prepare();
        JobHandler product = mock(JobHandler.class), stock = mock(JobHandler.class);
        String scoped = TenantJobParam.forTenant(163L, "");
        when(product.execute(scoped)).thenReturn("product ok");
        when(stock.execute(scoped)).thenReturn("stock ok");
        try (var spring = mockStatic(SpringUtil.class)) {
            spring.when(() -> SpringUtil.getBean("kingdeeProductItemSyncJob")).thenReturn(product);
            spring.when(() -> SpringUtil.getBean("kingdeeStockSyncJob")).thenReturn(stock);
            assertEquals("status=SUCCESS, selected=2, success=2", service.executeAutoForCurrentTenant());
            var order = inOrder(product, stock);
            order.verify(product).execute(scoped);
            order.verify(stock).execute(scoped);
            ArgumentCaptor<ErpKingdeeTableAutoSyncPlanDO> updates = ArgumentCaptor.forClass(ErpKingdeeTableAutoSyncPlanDO.class);
            verify(plans, times(2)).updateById(updates.capture());
            assertEquals(LocalDate.now(), updates.getAllValues().get(0).getLastAutoRunDate());
            assertEquals("SUCCESS", updates.getAllValues().get(1).getLastStatus());
            spring.verify(() -> SpringUtil.getBean("kingdeeBomSyncJob"), never());
        }
    }

    @Test void failedTableStopsSequenceAndDoesNotMarkDateSuccessful() throws Exception {
        prepare();
        JobHandler product = mock(JobHandler.class), stock = mock(JobHandler.class);
        when(product.execute(any())).thenThrow(new IllegalStateException("source unavailable"));
        try (var spring = mockStatic(SpringUtil.class)) {
            spring.when(() -> SpringUtil.getBean("kingdeeProductItemSyncJob")).thenReturn(product);
            spring.when(() -> SpringUtil.getBean("kingdeeStockSyncJob")).thenReturn(stock);
            assertThrows(RuntimeException.class, service::executeAutoForCurrentTenant);
            verifyNoInteractions(stock);
            ArgumentCaptor<ErpKingdeeTableAutoSyncPlanDO> update = ArgumentCaptor.forClass(ErpKingdeeTableAutoSyncPlanDO.class);
            verify(plans).updateById(update.capture());
            assertEquals("FAILED", update.getValue().getLastStatus());
            assertEquals("source unavailable", update.getValue().getLastMessage());
            assertNull(update.getValue().getLastAutoRunDate());
        }
    }

    @Test void completedDailyPlanIsNotExecutedAgain() {
        prepare();
        when(plans.selectCurrentTenantPlan()).thenReturn(new ErpKingdeeTableAutoSyncPlanDO()
                .setId(7L).setEnabled(true).setDailyStartTime(LocalTime.MIDNIGHT).setLastAutoRunDate(LocalDate.now()));
        assertEquals("skipped: already run today", service.executeAutoForCurrentTenant());
        verifyNoInteractions(items);
    }

    @Test void tenantWithoutPlanDoesNotStartAnySync() {
        prepare();
        when(plans.selectCurrentTenantPlan()).thenReturn(null);
        assertEquals("skipped: ERP table auto sync disabled", service.executeAutoForCurrentTenant());
        verifyNoInteractions(items);
    }
}
