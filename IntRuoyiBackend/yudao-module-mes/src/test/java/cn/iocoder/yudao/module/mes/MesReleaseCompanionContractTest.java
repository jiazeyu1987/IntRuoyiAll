package cn.iocoder.yudao.module.mes;

import cn.iocoder.yudao.module.bpm.businessapproval.service.BusinessApprovalEffectExecutor;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionTaskOpenReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionTaskRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrRecordChangeApproveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrRecordChangeRespVO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionBusinessApprovalEffectExecutor;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrRecordChangeService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordParsedCell;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MesReleaseCompanionContractTest {

    @Test
    void batchExecutionResponseShouldExposeReleaseLockContract() {
        EdhrBatchExecutionRespVO response = new EdhrBatchExecutionRespVO()
                .setReleaseActionLocked(Boolean.TRUE)
                .setReleaseActionLockReason("pending release approval")
                .setPendingVoidChangeEventId(1L)
                .setPendingVoidChangeCode("VOID-1")
                .setPendingVoidChangeStatus("PENDING")
                .setPendingVoidProcessInstanceId("process-1")
                .setPendingVoidRequestedBy(2L)
                .setCanWithdrawVoidRequest(Boolean.TRUE);

        assertEquals(Boolean.TRUE, response.getReleaseActionLocked());
        assertEquals("pending release approval", response.getReleaseActionLockReason());
        assertEquals(1L, response.getPendingVoidChangeEventId());
        assertEquals("VOID-1", response.getPendingVoidChangeCode());
        assertEquals("PENDING", response.getPendingVoidChangeStatus());
        assertEquals("process-1", response.getPendingVoidProcessInstanceId());
        assertEquals(2L, response.getPendingVoidRequestedBy());
        assertEquals(Boolean.TRUE, response.getCanWithdrawVoidRequest());
    }

    @Test
    void batchExecutionTaskContractsShouldExposeWorkTaskAndVersionIdentity() {
        EdhrBatchExecutionTaskOpenReqVO openReq = new EdhrBatchExecutionTaskOpenReqVO()
                .setBatchExecutionId(10L)
                .setTaskId(20L)
                .setWorkTaskId(30L);
        EdhrBatchExecutionTaskRespVO taskResp = new EdhrBatchExecutionTaskRespVO()
                .setBatchRecordVersionId(40L)
                .setBatchRecordVersionNo("V2");

        assertEquals(30L, openReq.getWorkTaskId());
        assertEquals("V2", taskResp.getBatchRecordVersionNo());
    }

    @Test
    void serviceAndMapperShouldExposeCompanionMethods() throws NoSuchMethodException {
        assertNotNull(MesProEdhrRecordChangeService.class.getMethod(
                "withdrawVoidBatchExecution", EdhrRecordChangeApproveReqVO.class));
        assertEquals(EdhrRecordChangeRespVO.class, MesProEdhrRecordChangeService.class
                .getMethod("withdrawVoidBatchExecution", EdhrRecordChangeApproveReqVO.class)
                .getReturnType());
        assertNotNull(MesProRouteVersionMapper.class.getMethod("selectActiveByRouteIdForUpdate", Long.class));
        assertNotNull(MesProRouteVersionMapper.class.getMethod("updateApprovalFieldsToDraft", Long.class));
    }

    @Test
    void parsedCellShouldExposeReviewedCellRuleContract() {
        MesProBatchRecordParsedCell cell = MesProBatchRecordParsedCell.builder()
                .text("☑ 通过 □ 不通过")
                .reviewedCellRule(true)
                .cellRuleSource("MANUAL")
                .build();

        assertEquals(true, cell.isReviewedCellRule());
        assertEquals("MANUAL", cell.getCellRuleSource());
    }

    @Test
    void businessApprovalExecutorShouldExposeBpmProcessDefinitionKey() throws NoSuchMethodException {
        assertNotNull(BusinessApprovalEffectExecutor.class.getMethod("getBpmProcessDefinitionKey"));
        assertEquals("mes-edhr-approval-v1",
                new MesProBatchRecordExecutionBusinessApprovalEffectExecutor().getBpmProcessDefinitionKey());
    }
}
