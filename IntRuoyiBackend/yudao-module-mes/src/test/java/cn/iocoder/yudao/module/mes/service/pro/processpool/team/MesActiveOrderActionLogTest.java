package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrOperationAuditCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrOperationAuditService;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import com.alibaba.fastjson.JSON;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.time.LocalDateTime;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesActiveOrderActionLogTest {
    private final MesProEdhrOperationAuditService audit = mock(MesProEdhrOperationAuditService.class);
    private final AdminUserService users = mock(AdminUserService.class);
    private final MesActiveOrderActionLog log = new MesActiveOrderActionLog(audit, users);

    @Test void patrolChangeSharesTransactionAndPreservesActorAndOccurrence() {
        when(users.getUser(7L)).thenReturn(new AdminUserDO().setId(7L).setNickname("张三"));
        var before = LocalDateTime.now();
        log.record(7L, 10L, "PQC_PATROL_CREATE", Map.of("roundNo", 3, "taskCount", 5));
        var argument = ArgumentCaptor.forClass(MesProEdhrOperationAuditCommand.class);
        verify(audit).recordInCallerTransaction(argument.capture());
        var command = argument.getValue();
        assertEquals("新建巡检轮次", command.getActionName());
        assertEquals(7L, command.getActorUserId());
        assertEquals("张三", command.getActorUsername());
        assertFalse(command.getOccurredAt().isBefore(before));
        assertFalse(command.getOccurredAt().isAfter(LocalDateTime.now()));
        assertEquals(10L, JSON.parseObject(command.getMetadataJson()).getLong("activeOrderId"));
        verify(audit, never()).record(any());
    }

    @Test void failedAuditIsNotReportedAsSuccessAndUnknownActionsAreRejected() {
        when(users.getUser(7L)).thenReturn(new AdminUserDO().setId(7L).setNickname("张三"));
        doThrow(new IllegalStateException("日志存储失败")).when(audit).recordInCallerTransaction(any());
        assertThrows(IllegalStateException.class, () -> log.record(7L, 10L, "PQC_PATROL_CANCEL", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> log.record(7L, 10L, "UNKNOWN", Map.of()));
        assertThrows(IllegalStateException.class, () -> log.record(8L, 10L, "PQC_PATROL_CANCEL", Map.of()));
        verify(audit, times(1)).recordInCallerTransaction(any());
    }

    @Test void readUsesIndependentAuditAndPreparationUsesWorkOrderIdentity() {
        when(users.getUser(7L)).thenReturn(new AdminUserDO().setId(7L).setNickname("张三"));
        log.recordRead(7L, 10L, 20L, "REVERSE_TRACE_QUERY", Map.of("conditionCount", 2));
        verify(audit).record(argThat(c -> "反查关联批次".equals(c.getActionName()) && Long.valueOf(20).equals(c.getBatchExecutionId())));
        log.recordPreparation(7L, 30L, false, Map.of("领料单", "已有数据"));
        verify(audit).recordInCallerTransaction(argThat(c -> "WORK_ORDER_PREPARATION".equals(c.getObjectType())
                && "30".equals(c.getObjectId()) && !JSON.parseObject(c.getMetadataJson()).containsKey("activeOrderId")));
    }
}
