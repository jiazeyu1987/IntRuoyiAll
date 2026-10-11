package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesProEdhrReleaseTaskEligibilityTest {
    private final MesProEdhrWorkTaskMapper mapper = mock(MesProEdhrWorkTaskMapper.class);
    private final MesProEdhrWorkTaskServiceImpl service = new MesProEdhrWorkTaskServiceImpl();
    private MockedStatic<SecurityFrameworkUtils> security;
    private MesProEdhrWorkTaskDO task;

    @BeforeEach void setup() {
        security = mockStatic(SecurityFrameworkUtils.class);
        security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(100L);
        cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.setTenantId(1L);
        ReflectionTestUtils.setField(service, "sourceLedger", mock(cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService.class));
        ReflectionTestUtils.setField(service, "sourceAuthority", mock(cn.iocoder.yudao.module.system.api.permission.ApprovalAuthorityApi.class));
        var batches = mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper.class);
        var releases = mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper.class);
        ReflectionTestUtils.setField(service, "batchExecutionMapper", batches);
        ReflectionTestUtils.setField(service, "releaseTransactionMapper", releases);
        lenient().when(batches.selectByTenantIdAndIdForUpdate(1L, 2L)).thenReturn(new cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO().setId(2L).setTenantId(1L));
        lenient().when(releases.selectByIdForUpdate(9200L)).thenReturn(new cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO().setId(9200L).setBatchExecutionId(2L));
        ReflectionTestUtils.setField(service, "workTaskMapper", mapper);
        task = new MesProEdhrWorkTaskDO().setId(177L).setTaskType("RELEASE_APPROVE")
                .setBatchExecutionId(2L).setBusinessScopeType("RELEASE_TRANSACTION").setBusinessScopeId(9200L).setStatus("TODO");
        when(mapper.selectById(177L)).thenReturn(task);
    }
    @AfterEach void cleanup() { security.close(); cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.clear(); }

    @Test void formalCompletionRechecksTaskAfterSuccessfulPreflightValidation() {
        task.setAssigneeUserId(100L);
        assertSame(task, service.validateReleaseApprovalTask(177L, null));
        task.setStatus("DONE");
        when(mapper.selectByIdAndTenantForUpdate(177L, 1L)).thenReturn(task);
        assertThrows(RuntimeException.class,
                () -> service.completeReleaseApprovalTask(177L, 9200L, "APPROVE", "reviewed"));
        verify(mapper, times(3)).selectById(177L);
        verify(mapper).selectByIdAndTenantForUpdate(177L, 1L);
        verifyNoMoreInteractions(mapper);
    }

    @ParameterizedTest @ValueSource(strings = {"assigned", "candidate", "overdue"})
    void existingAssignedOrCandidatePolicyAcceptsProcessableTasks(String policy) {
        if ("assigned".equals(policy)) task.setAssigneeUserId(100L);
        else task.setAssigneeUserId(101L).setCandidateUserSnapshot("99,100");
        if ("overdue".equals(policy)) task.setStatus("OVERDUE");
        assertSame(task, service.validateReleaseApprovalTask(177L, null));
        verify(mapper).selectById(177L);
        verifyNoMoreInteractions(mapper);
    }

    @ParameterizedTest @ValueSource(strings = {"missing", "type", "scope", "scopeId", "candidate", "DONE", "DOING", "CANCELED"})
    void invalidTaskFailsWithoutDomainWrites(String failure) {
        task.setAssigneeUserId(100L);
        switch (failure) {
            case "missing" -> when(mapper.selectById(177L)).thenReturn(null);
            case "type" -> task.setTaskType("PQC_REVIEW");
            case "scope" -> task.setBusinessScopeType("BATCH_EXECUTION");
            case "scopeId" -> task.setBusinessScopeId(null);
            case "candidate" -> task.setAssigneeUserId(101L).setCandidateUserSnapshot("102");
            default -> task.setStatus(failure);
        }
        assertThrows(RuntimeException.class, () -> service.validateReleaseApprovalTask(177L, null));
        verify(mapper).selectById(177L);
        verifyNoMoreInteractions(mapper);
    }
}
