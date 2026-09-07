package cn.iocoder.yudao.module.dcc;

import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationFollowupBatchMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationImpactTaskMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationNotificationDeliveryMapper;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import cn.iocoder.yudao.module.dcc.service.file.DccPublicationNotificationDispatchOrchestrator;
import cn.iocoder.yudao.module.dcc.service.file.DccPublicationNotificationTransactionServiceImpl;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DccPublicationNotificationSchemaTest {
    @Test
    void batchStatusRefreshUsesTenantScopedLockingCurrentReads() throws Exception {
        assertLocking(DccPublicationFollowupBatchMapper.class,
                "selectByIdAndTenantForUpdate", Long.class, Long.class);
        assertLocking(DccPublicationNotificationDeliveryMapper.class,
                "selectListByBatchIdForUpdate", Long.class, Long.class);
        assertLocking(DccPublicationImpactTaskMapper.class,
                "selectListByBatchIdForUpdate", Long.class, Long.class);
    }

    @Test
    void dispatchOrchestratorIsNonTransactionalAndStateWorkersRequireNew() throws Exception {
        assertNull(DccPublicationNotificationDispatchOrchestrator.class.getAnnotation(Transactional.class));
        for (String method : new String[]{"beginAttempt", "markSent", "markFailed"}) {
            Transactional transactional = java.util.Arrays.stream(
                            DccPublicationNotificationTransactionServiceImpl.class.getDeclaredMethods())
                    .filter(candidate -> candidate.getName().equals(method)).findFirst().orElseThrow()
                    .getAnnotation(Transactional.class);
            assertEquals(Propagation.REQUIRES_NEW, transactional.propagation());
        }
    }

    @Test
    void myImpactPageDefaultsToUnfinishedTasksAtDatabaseBoundary() throws Exception {
        Select select = DccPublicationImpactTaskMapper.class.getMethod(
                "selectAssigneePage", com.baomidou.mybatisplus.extension.plugins.pagination.Page.class,
                Long.class, Long.class, String.class, String.class).getAnnotation(Select.class);
        String sql = String.join(" ", select.value()).toLowerCase(Locale.ROOT);
        assertTrue(sql.contains("<otherwise>"));
        assertTrue(sql.contains("task_status &lt;&gt; 'completed'"));
        assertTrue(sql.contains("revision_tracking_status in ('not_started','revision_linked')"));
    }

    private void assertLocking(Class<?> mapper, String method, Class<?>... parameterTypes) throws Exception {
        Select select = mapper.getMethod(method, parameterTypes).getAnnotation(Select.class);
        String sql = String.join(" ", select.value()).toLowerCase(Locale.ROOT);
        assertTrue(sql.contains("tenant_id"));
        assertTrue(sql.contains("for update"));
    }
}
