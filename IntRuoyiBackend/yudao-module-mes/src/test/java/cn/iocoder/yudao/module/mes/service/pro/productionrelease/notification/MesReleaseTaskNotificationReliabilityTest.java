package cn.iocoder.yudao.module.mes.service.pro.productionrelease.notification;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.productionrelease.MesReleaseTaskNotifyDeliveryDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.productionrelease.MesReleaseTaskNotifyDeliveryMapper;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** eDHR notification delivery is retired; historical receipt reads retain access checks. */
class MesReleaseTaskNotificationReliabilityTest {
    @AfterEach void clear() { TenantContextHolder.clear(); }

    @Test void automaticAndManualHistoricalDeliveryAreRejected() {
        var dispatch = new MesReleaseTaskNotificationDispatchService();
        assertTrue(assertThrows(IllegalStateException.class, () -> dispatch.dispatchAssigned(1L, 30L))
                .getMessage().contains("已停用"));
        assertTrue(assertThrows(IllegalStateException.class, () -> dispatch.retryDelivery(1L, 343L, 10L, 0, "重试"))
                .getMessage().contains("已停用"));
    }

    @Test void historicalOutcomeRemainsReadableOnlyByAuthorizedRecipientAndCannotRetry() {
        TenantContextHolder.setTenantId(1L);
        var mapper = mock(MesReleaseTaskNotifyDeliveryMapper.class);
        var permissions = mock(PermissionApi.class);
        var service = new MesReleaseTaskNotificationService();
        ReflectionTestUtils.setField(service, "deliveryMapper", mapper);
        ReflectionTestUtils.setField(service, "permissionApi", permissions);
        var row = new MesReleaseTaskNotifyDeliveryDO().setTenantId(1L).setWorkTaskId(30L)
                .setUserId(343L).setEventType("MARKET_APPROVED").setStatus("FAILED");
        when(permissions.hasAnyPermissions(343L, MesReleaseTaskNotificationContract.QUERY_PERMISSION)).thenReturn(true);
        when(mapper.selectOwnPendingOutcomes(1L, 343L)).thenReturn(List.of(row));
        when(mapper.selectTaskDeliveries(1L, 30L)).thenReturn(List.of(row));
        assertEquals(List.of(row), service.listOwnPendingOutcomes(1L, 343L));
        assertEquals(List.of(row), service.listForTask(1L, 343L, 30L));
        assertFalse(service.canRetry(343L, row));
        assertThrows(RuntimeException.class, () -> service.listOwnPendingOutcomes(1L, 344L));
        verify(mapper, never()).selectOwnPendingOutcomes(1L, 344L);
        assertThrows(RuntimeException.class, () -> service.listOwnPendingOutcomes(2L, 343L));
        verify(mapper, never()).selectOwnPendingOutcomes(2L, 343L);
    }
}
