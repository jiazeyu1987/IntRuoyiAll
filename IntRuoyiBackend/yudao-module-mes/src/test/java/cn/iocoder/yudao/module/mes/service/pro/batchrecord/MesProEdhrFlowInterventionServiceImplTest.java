package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFlowInterventionAddSignReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFlowInterventionTransferReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrFlowEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskStatus;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

@Import(MesProEdhrFlowInterventionServiceImpl.class)
class MesProEdhrFlowInterventionServiceImplTest extends BaseDbUnitTest {

    private static final Long TENANT_ID = 122L;

    @Resource
    private MesProEdhrFlowInterventionService flowInterventionService;
    @Resource
    private MesProEdhrWorkTaskMapper workTaskMapper;
    @Resource
    private MesProEdhrFlowEventMapper flowEventMapper;

    private MockedStatic<SecurityFrameworkUtils> securityMock;

    @BeforeEach
    void setUpTenant() {
        TenantContextHolder.setTenantId(TENANT_ID);
        securityMock = mockStatic(SecurityFrameworkUtils.class);
        securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9901L);
    }

    @AfterEach
    void clearTenant() {
        if (securityMock != null) {
            securityMock.close();
        }
        TenantContextHolder.clear();
    }

    @Test
    void transfer_activeWorkTask_reassignsAssigneeAndKeepsAuditTrail() {
        MesProEdhrWorkTaskDO workTask = insertActiveWorkTask(8801L);

        flowInterventionService.transfer(new MesProEdhrFlowInterventionTransferReqVO()
                .setBusinessObjectType("WORK_TASK")
                .setBusinessObjectId(String.valueOf(workTask.getId()))
                .setBusinessObjectCode(workTask.getTaskCode())
                .setFlowInstanceId("edhr-exec-7701")
                .setTaskId(String.valueOf(workTask.getId()))
                .setNodeKey("FILL")
                .setFromStatus(MesProEdhrWorkTaskStatus.TODO)
                .setToStatus(MesProEdhrWorkTaskStatus.TODO)
                .setTargetUserId(8802L)
                .setReasonCategory("人员不可用")
                .setReason("原执行人请假，主管签名确认转办")
                .setSignoffEvidenceHash("a".repeat(64))
                .setIdempotencyKey("transfer-work-task-8801-to-8802")
                .setInterventionSource("FRONTEND"));

        MesProEdhrWorkTaskDO reassigned = workTaskMapper.selectById(workTask.getId());
        assertEquals(8802L, reassigned.getAssigneeUserId());
        assertEquals(8801L, reassigned.getSourceUserId());
        assertEquals("8802", reassigned.getCandidateUserSnapshot());
        assertTrue(reassigned.getReason().contains("原执行人请假"));
        assertTrue(reassigned.getRemark().contains("TRANSFER"));
        assertNotNull(flowEventMapper.selectPage(new cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFlowEventPageReqVO()
                .setBusinessObjectType("WORK_TASK")
                .setBusinessObjectId(String.valueOf(workTask.getId()))).getList().get(0));
    }

    @Test
    void addSign_activeWorkTask_createsAdditionalTodoAndKeepsOriginalAssignee() {
        MesProEdhrWorkTaskDO workTask = insertActiveWorkTask(8811L);

        flowInterventionService.addSign(new MesProEdhrFlowInterventionAddSignReqVO()
                .setBusinessObjectType("WORK_TASK")
                .setBusinessObjectId(String.valueOf(workTask.getId()))
                .setBusinessObjectCode(workTask.getTaskCode())
                .setFlowInstanceId("edhr-exec-7711")
                .setTaskId(String.valueOf(workTask.getId()))
                .setNodeKey("REVIEW")
                .setFromStatus(MesProEdhrWorkTaskStatus.TODO)
                .setToStatus(MesProEdhrWorkTaskStatus.TODO)
                .setTargetUserId(8812L)
                .setReasonCategory("补充复核")
                .setReason("主管签名确认追加 QA 复核")
                .setSignoffEvidenceHash("b".repeat(64))
                .setIdempotencyKey("add-sign-work-task-8811-to-8812")
                .setInterventionSource("FRONTEND"));

        MesProEdhrWorkTaskDO original = workTaskMapper.selectById(workTask.getId());
        assertEquals(8811L, original.getAssigneeUserId());
        MesProEdhrWorkTaskDO added = workTaskMapper.selectList().stream()
                .filter(task -> !task.getId().equals(workTask.getId()))
                .filter(task -> "ADD_SIGN:" .concat(String.valueOf(workTask.getId())).equals(task.getRemark()))
                .findFirst()
                .orElseThrow();
        assertNotEquals(workTask.getId(), added.getId());
        assertEquals(8812L, added.getAssigneeUserId());
        assertEquals(8811L, added.getSourceUserId());
        assertEquals(MesProEdhrWorkTaskStatus.TODO, added.getStatus());
        assertTrue(added.getReason().contains("追加 QA 复核"));
        assertNotNull(flowEventMapper.selectPage(new cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFlowEventPageReqVO()
                .setBusinessObjectType("WORK_TASK")
                .setBusinessObjectId(String.valueOf(workTask.getId()))).getList().get(0));
    }

    private MesProEdhrWorkTaskDO insertActiveWorkTask(Long assigneeUserId) {
        MesProEdhrWorkTaskDO workTask = new MesProEdhrWorkTaskDO()
                .setTaskCode("EDHR-WT-TRANSFER-RED")
                .setTaskType(MesProEdhrWorkTaskService.TASK_TYPE_FILL)
                .setBatchExecutionId(7701L)
                .setBatchTaskId(7702L)
                .setBusinessScopeType("BATCH_TASK")
                .setBusinessScopeId(7702L)
                .setExecutionId(7703L)
                .setWorkOrderId(7704L)
                .setWorkOrderCode("WO-TRANSFER-RED")
                .setBatchCode("BATCH-TRANSFER-RED")
                .setRouteId(7705L)
                .setRouteProcessId(7706L)
                .setProcessId(7707L)
                .setProcessName("灌装")
                .setAssigneeUserId(assigneeUserId)
                .setCandidateSourceType("USER")
                .setCandidateSourceId(assigneeUserId)
                .setCandidateUserSnapshot(String.valueOf(assigneeUserId))
                .setStatus(MesProEdhrWorkTaskStatus.TODO)
                .setActionUrl("/mes/pro/feedback/edhr-execution/detail?id=7703&workTaskId=0")
                .setReason("初始分配");
        workTaskMapper.insert(workTask);
        return workTask;
    }
}
