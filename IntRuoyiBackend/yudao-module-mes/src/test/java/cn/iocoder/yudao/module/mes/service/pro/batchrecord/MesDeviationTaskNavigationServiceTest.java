package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesDeviationTaskNavigationServiceTest {
    private final MesProEdhrWorkTaskMapper tasks = mock(MesProEdhrWorkTaskMapper.class);
    private final MesProEdhrDeviationAccessService access = mock(MesProEdhrDeviationAccessService.class);
    private final MesDeviationTaskPermissionService permissions = mock(MesDeviationTaskPermissionService.class);
    private final MesDeviationTaskNavigationService service = new MesDeviationTaskNavigationService();

    @BeforeEach void setUp() {
        TenantContextHolder.setTenantId(71L);
        ReflectionTestUtils.setField(service, "taskMapper", tasks);
        ReflectionTestUtils.setField(service, "accessService", access);
        ReflectionTestUtils.setField(service, "taskPermissionService", permissions);
    }
    @AfterEach void tearDown() { TenantContextHolder.clear(); }

    @Test void currentAndHistoricalAssignedTasksResolveWithoutGlobalQueryPermission() {
        for (String type : new String[]{"DEVIATION_RECEIVE", "DEVIATION_CORRECT", "DEVIATION_VERIFY",
                "DEVIATION_SIGN", "DEVIATION_APPROVE", "DEVIATION_CLOSE"}) {
            var task = task().setTaskType(type).setStatus("DONE");
            when(tasks.selectOne(any(QueryWrapper.class))).thenReturn(task);
            var result = service.get(201L, 13L);
            assertEquals(13L, result.id());
            assertEquals(91L, result.businessScopeId());
            assertEquals(type, result.taskType());
        }
        verify(access, times(6)).requireRead(91L);
    }

    @Test void deletedWrongTypeScopeOrAssigneeCannotResolve() {
        var deleted = task(); deleted.setDeleted(true);
        for (var task : new MesProEdhrWorkTaskDO[]{deleted,
                task().setTaskType("FILL"), task().setTaskType(null), task().setBusinessScopeType("BATCH_TASK"),
                task().setBusinessScopeId(null), task().setAssigneeUserId(202L), task().setId(14L)}) {
            when(tasks.selectOne(any(QueryWrapper.class))).thenReturn(task);
            assertThrows(ServiceException.class, () -> service.get(201L, 13L));
        }
        verifyNoInteractions(access);
    }

    @Test void missingActorOrTaskCannotResolve() {
        assertThrows(ServiceException.class, () -> service.get(null, 13L));
        assertThrows(ServiceException.class, () -> service.get(201L, 0L));
        assertThrows(ServiceException.class, () -> service.get(201L, 13L));
    }

    @Test void participantAccessIsStillRequiredForHistoricalTask() {
        when(tasks.selectOne(any(QueryWrapper.class))).thenReturn(task());
        doThrow(new IllegalStateException("不再是该单参与人")).when(access).requireRead(91L);
        assertThrows(IllegalStateException.class, () -> service.get(201L, 13L));
    }

    @Test void revokedResponsibilityPermissionPreventsNavigationEvenForParticipant() {
        when(tasks.selectOne(any(QueryWrapper.class))).thenReturn(task().setSignatureCellKey("PREPARER"));
        when(permissions.blockedReason(71L, 201L, "DEVIATION_SIGN", "PREPARER"))
                .thenReturn("当前账号已无此偏差职责的办理权限");
        var error = assertThrows(ServiceException.class, () -> service.get(201L, 13L));
        assertTrue(error.getMessage().contains("当前账号已无此偏差职责的办理权限"));
        verify(access).requireRead(91L);
    }

    @Test void storedNavigationMustIdentifyItsFormalDeviationAndTask() {
        for (String url : new String[]{"/mes/pro/feedback/edhr-deviation?deviationId=92",
                "/mes/pro/feedback/edhr-deviation?deviationId=91&deviationId=91",
                "/mes/pro/feedback/edhr-deviation/detail?deviationId=91&workTaskId=14",
                "/mes/pro/feedback/edhr-deviation/detail?deviationId=91&workTaskId=13&workTaskId=13",
                "https://example.invalid/mes/pro/feedback/edhr-deviation?deviationId=91",
                "//example.invalid/mes/pro/feedback/edhr-deviation?deviationId=91",
                "/mes/pro/feedback/edhr-deviation/detail"}) {
            when(tasks.selectOne(any(QueryWrapper.class))).thenReturn(task().setActionUrl(url));
            assertThrows(ServiceException.class, () -> service.get(201L, 13L));
        }
    }

    @Test void taskQueryAlwaysIncludesExactIdentityTenantAndUndeletedConditions() {
        when(tasks.selectOne(any(QueryWrapper.class))).thenReturn(task());
        service.get(201L, 13L);
        var query = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(tasks).selectOne(query.capture());
        String sql = query.getValue().getSqlSegment();
        assertTrue(sql.contains("id ="));
        assertTrue(sql.contains("tenant_id ="));
        assertTrue(sql.contains("deleted ="));
        assertEquals(java.util.Set.of(13L, 71L, false),
                java.util.Set.copyOf(query.getValue().getParamNameValuePairs().values()));
    }

    @Test void malformedStoredUrlIsAnExplicitBusinessRejection() {
        for (String url : new String[]{"/mes/pro/feedback/edhr-deviation?deviationId=91%",
                "/mes/pro/feedback/edhr-deviation?deviationId=91%GG",
                "/mes/pro/feedback/edhr-deviation?deviationId=91&%zz=value",
                "/mes/pro/feedback/edhr-deviation?deviationId=91 bad",
                "http://[invalid"}) {
            when(tasks.selectOne(any(QueryWrapper.class))).thenReturn(task().setActionUrl(url));
            assertThrows(ServiceException.class, () -> service.get(201L, 13L));
        }
    }

    private MesProEdhrWorkTaskDO task() {
        var task = new MesProEdhrWorkTaskDO().setId(13L).setAssigneeUserId(201L).setTaskType("DEVIATION_SIGN")
                .setBusinessScopeType("EDHR_DEVIATION").setBusinessScopeId(91L)
                .setActionUrl("/mes/pro/feedback/edhr-deviation?deviationId=91");
        task.setDeleted(false);
        return task;
    }
}
