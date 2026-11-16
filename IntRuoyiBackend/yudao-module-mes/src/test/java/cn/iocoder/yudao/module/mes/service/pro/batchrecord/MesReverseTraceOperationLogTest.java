package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionOriginDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionOriginMapper;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesActiveOrderActionLog;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesReverseTraceOperationLogTest {
    private final MesProEdhrBatchExecutionOriginMapper origins = mock(MesProEdhrBatchExecutionOriginMapper.class);
    private final MesActiveOrderActionLog logs = mock(MesActiveOrderActionLog.class);
    private final MesReverseTraceOperationLog service = new MesReverseTraceOperationLog(origins, logs);
    @AfterEach void clear() { TenantContextHolder.clear(); SecurityContextHolder.clearContext(); }

    @Test void resolvesTenantScopedOriginAndKeepsAuthenticatedActor() {
        TenantContextHolder.setTenantId(168L);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new LoginUser().setId(7L), null, List.of()));
        when(origins.selectListByBatchExecutionId(20L)).thenReturn(List.of(origin(168L, 10L), origin(1L, 99L)));
        var details = Map.<String, Object>of("queryHash", "hash", "conditionCount", 2);
        service.record("20", "REVERSE_TRACE_QUERY", details);
        verify(logs).recordRead(7L, 10L, 20L, "REVERSE_TRACE_QUERY", details);
    }

    @Test void missingOrAmbiguousOriginsCannotAttributeActivityToAnotherOrder() {
        TenantContextHolder.setTenantId(168L);
        when(origins.selectListByBatchExecutionId(20L)).thenReturn(List.of(origin(1L, 99L)));
        assertThrows(IllegalStateException.class, () -> service.record("20", "REVERSE_TRACE_QUERY", Map.of()));
        when(origins.selectListByBatchExecutionId(20L)).thenReturn(List.of(origin(168L, 10L), origin(168L, 11L)));
        assertThrows(IllegalStateException.class, () -> service.record("20", "REVERSE_TRACE_QUERY", Map.of()));
        verifyNoInteractions(logs);
    }

    private MesProEdhrBatchExecutionOriginDO origin(long tenant, long order) {
        var row = new MesProEdhrBatchExecutionOriginDO().setBatchExecutionId(20L).setActiveOrderId(order);
        row.setTenantId(tenant);
        return row;
    }
}
