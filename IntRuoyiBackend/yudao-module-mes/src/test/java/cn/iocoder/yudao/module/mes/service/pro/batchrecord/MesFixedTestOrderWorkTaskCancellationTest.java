package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MesFixedTestOrderWorkTaskCancellationTest {
    private final MesProEdhrWorkTaskMapper mapper = mock(MesProEdhrWorkTaskMapper.class);
    private final PermissionApi permissions = mock(PermissionApi.class);
    private final MesWorkTaskAuxiliaryAudit audit = mock(MesWorkTaskAuxiliaryAudit.class);
    private final MesProEdhrWorkTaskServiceImpl service = new MesProEdhrWorkTaskServiceImpl();

    @AfterEach void clearTenant() { TenantContextHolder.clear(); }

    @Test
    void terminalTaskStillRevokesTaskOwnedPermissionWithoutChangingDecision() throws Exception {
        prepare();
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(task("DONE"));
        cancel();
        verify(permissions).revokeEntitlementSource(argThat(req -> req.getTenantId().equals(1L)
                && req.getSourceType().equals("EDHR_WORK_TASK_ASSIGNEE")
                && req.getSourceKey().equals("WORK_TASK|801")));
        verify(mapper, never()).updateById(any(MesProEdhrWorkTaskDO.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"TODO", "DOING", "OVERDUE"})
    void activeTaskUsesCanceledStateAndRevokesPermission(String status) throws Exception {
        prepare();
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(task(status));
        when(mapper.updateById(any(MesProEdhrWorkTaskDO.class))).thenReturn(1);
        cancel();
        verify(mapper).updateById(argThat((MesProEdhrWorkTaskDO row) -> row.getId().equals(801L)
                && row.getStatus().equals("CANCELED")));
        verify(permissions).revokeEntitlementSource(any());
    }

    @Test
    void missingScopedTaskFailsBeforePermissionOrUpdate() throws Exception {
        prepare();
        when(mapper.selectOne(any(Wrapper.class))).thenAnswer(call -> {
            Wrapper<?> query = call.getArgument(0);
            assertTrue(query.getSqlSegment().contains("tenant_id"));
            assertTrue(query.getSqlSegment().contains("FOR UPDATE"));
            return null;
        });
        assertThrows(RuntimeException.class, this::cancel);
        verifyNoInteractions(permissions);
        verify(mapper, never()).updateById(any(MesProEdhrWorkTaskDO.class));
    }

    private void prepare() {
        TenantContextHolder.setTenantId(1L);
        ReflectionTestUtils.setField(service, "workTaskMapper", mapper);
        ReflectionTestUtils.setField(service, "permissionApi", permissions);
        ReflectionTestUtils.setField(service, "auxiliaryAudit", audit);
        doAnswer(call -> { ((Runnable) call.getArgument(3)).run(); return null; })
                .when(audit).entitlements(any(), anyString(), anySet(), any());
    }

    private MesProEdhrWorkTaskDO task(String status) {
        return new MesProEdhrWorkTaskDO().setId(801L).setTaskType("RELEASE_APPROVE")
                .setStatus(status).setBusinessScopeType("RELEASE_APPLICATION").setBusinessScopeId(601L);
    }

    private void cancel() throws Exception {
        try {
            service.getClass().getMethod("cancelTasksForTestReset", List.class, String.class)
                    .invoke(service, List.of(801L), "固定测试订单重置");
        } catch (InvocationTargetException error) {
            if (error.getCause() instanceof Exception cause) throw cause;
            throw error;
        }
    }
}
