package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.bpm.formcenter.model.BusinessActionContext;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionInstance;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionPolicy;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionResolution;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormApprovalMode;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormPolicyType;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormBusinessEffectPrecheck;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormBusinessEffectResult;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MesProEdhrRouteFormFillEffectExecutorTest extends BaseMockitoUnitTest {

    @Mock
    private MesProEdhrBatchExecutionTaskMapper batchTaskMapper;

    @InjectMocks
    private MesProEdhrRouteFormFillEffectExecutor executor;

    @Test
    void executeApprovesWritableRouteFormTask() {
        MesProEdhrBatchExecutionTaskDO task = writableTask();
        when(batchTaskMapper.selectByIdForUpdate(700L)).thenReturn(task);

        FormBusinessEffectResult result = executor.execute(routeFormInstance(), "IDEM-ROUTE-FORM-1");

        assertTrue(result.isSuccess());
        assertEquals("700", result.getResultRef());
        assertEquals(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED, task.getStatus());
        assertNotNull(task.getSubmittedAt());
        assertNotNull(task.getApprovedAt());
        assertEquals(99L, task.getOpenedBy());
        assertNotNull(task.getOpenedAt());
        verify(batchTaskMapper).updateById(task);
    }

    @Test
    void preflightRejectsTaskWithoutFormCenterSnapshot() {
        MesProEdhrBatchExecutionTaskDO task = writableTask().setFormCenterInstanceId(null);
        when(batchTaskMapper.selectByIdForUpdate(700L)).thenReturn(task);

        FormBusinessEffectPrecheck precheck = executor.preflight(routeFormInstance());

        assertFalse(precheck.isPassed());
        assertEquals("eDHR route form task misses form center snapshot: 700", precheck.getFailureReason());
        verify(batchTaskMapper, never()).updateById(task);
    }

    @Test
    void executeRejectsWrongBusinessContext() {
        FormBusinessEffectResult result = executor.execute(instance("EDHR_BATCH_EXECUTION", "RELEASE"),
                "IDEM-WRONG-1");

        assertFalse(result.isSuccess());
        assertEquals("MES_EDHR_ROUTE_FORM_FILL only accepts MES EDHR_ROUTE_FORM ACTIVE actions",
                result.getFailureReason());
        verify(batchTaskMapper, never()).selectByIdForUpdate(700L);
    }

    @Test
    void pendingApprovalStartFailsBecauseRouteFormFillIsDirect() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> executor.onPendingApprovalStarted(routeFormInstance()));

        assertEquals("MES_EDHR_ROUTE_FORM_FILL uses DIRECT approval only", exception.getMessage());
    }

    private MesProEdhrBatchExecutionTaskDO writableTask() {
        return MesProEdhrBatchExecutionTaskDO.builder()
                .id(700L)
                .nodeType(MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_ROUTE_FORM)
                .formBindingKey("FB-DYNAMIC-1")
                .formTemplateId(2001L)
                .formTemplateVersionId(3002L)
                .formTemplateVersionNo("V2.0")
                .formCenterInstanceId(4001L)
                .status(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_WAITING)
                .build();
    }

    private FormActionInstance routeFormInstance() {
        return instance("EDHR_ROUTE_FORM", "EDHR_RF_100_FB-DYNAMIC-1");
    }

    private FormActionInstance instance(String objectType, String actionCode) {
        FormActionPolicy policy = FormActionPolicy.builder()
                .policyId(40L)
                .tenantId(122L)
                .dataDomain("MES")
                .systemCode("MES")
                .objectType(objectType)
                .actionCode(actionCode)
                .objectState("ACTIVE")
                .policyType(FormPolicyType.REQUIRED)
                .approvalMode(FormApprovalMode.DIRECT)
                .effectExecutorCode(MesProEdhrRouteFormFillEffectExecutor.EXECUTOR_CODE)
                .status(FormActionPolicy.STATUS_PUBLISHED)
                .build();
        return new FormActionInstance("FCI-EDHR-ROUTE-FORM-1", FormActionResolution.from(policy),
                BusinessActionContext.builder()
                        .tenantId(122L)
                        .dataDomain("MES")
                        .systemCode("MES")
                        .objectType(objectType)
                        .objectId("700")
                        .objectVersion("100")
                        .actionCode(actionCode)
                        .objectState("ACTIVE")
                        .reason("fill route form")
                        .build(),
                99L, "IDEM-ROUTE-FORM-1");
    }

}
