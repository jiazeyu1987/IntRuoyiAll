package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesDeviationInvestigationWithdrawReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrDeviationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrDeviationHandlingDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrDeviationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrDeviationHandlingMapper;
import cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureRecordMapper;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MesDeviationInvestigationWithdrawTest {
    private final MesProEdhrDeviationMapper deviations = mock(MesProEdhrDeviationMapper.class);
    private final MesProEdhrDeviationHandlingMapper handlings = mock(MesProEdhrDeviationHandlingMapper.class);
    private final MesProEdhrDeviationReviewTaskService reviews = mock(MesProEdhrDeviationReviewTaskService.class);
    private final MesProEdhrWorkTaskService tasks = mock(MesProEdhrWorkTaskService.class);
    private final SecurityFrameworkService permissions = mock(SecurityFrameworkService.class);
    private final MesProEdhrOperationAuditService audit = mock(MesProEdhrOperationAuditService.class);
    private final MesProEdhrDeviationHandlingServiceImpl service = new MesProEdhrDeviationHandlingServiceImpl(
            deviations, handlings, mock(ElectronicSignatureRecordMapper.class),
            mock(MesProBatchRecordExecutionSignatureService.class), audit);
    private MesProEdhrDeviationDO deviation;
    private MesProEdhrDeviationHandlingDO current;

    @BeforeEach void setUp() {
        TenantContextHolder.setTenantId(71L);
        ReflectionTestUtils.setField(service, "securityFrameworkService", permissions);
        ReflectionTestUtils.setField(service, "reviewTaskService", reviews);
        ReflectionTestUtils.setField(service, "workTaskService", tasks);
        ReflectionTestUtils.setField(service, "gxpAuditService", mock(GxpAuditService.class));
        ReflectionTestUtils.setField(service, "accessService", mock(MesProEdhrDeviationAccessService.class));
        when(permissions.hasPermission(anyString())).thenReturn(true);
        deviation = new MesProEdhrDeviationDO().setId(901L).setTenantId(71L).setStatus("OPEN")
                .setLevel("NORMAL").setReceiverId(101L).setDeviationCode("PC-901");
        current = new MesProEdhrDeviationHandlingDO().setId(701L).setTenantId(71L).setDeviationId(901L)
                .setContentVersion(3).setContentHash("hash3").setInvestigationSubmitted(true)
                .setRootCauseAnalysis("根因").setImpactScope("影响范围").setRiskAssessment("风险")
                .setProductDisposition("产品处置").setHandlingConclusion("CLOSED_LOOP")
                .setVerificationResult("PASS").setVerificationContent("独立验证证据")
                .setCorrectiveStatus("VERIFIED").setCorrectiveOwnerId(301L).setCorrectiveResult("完成整改")
                .setCompletedAt(LocalDateTime.of(2026, 11, 17, 10, 0))
                .setReviewerAssignmentsJson("{\"PREPARER\":101,\"VERIFIER\":201,\"DEPARTMENT_OWNER\":401,\"QA\":501,\"QUALITY_OWNER\":601}");
        when(deviations.selectByTenantAndIdForUpdate(71L, 901L)).thenReturn(deviation);
        when(handlings.selectByTenantAndDeviationIdForUpdate(71L, 901L)).thenReturn(current);
    }

    @AfterEach void tearDown() { TenantContextHolder.clear(); }

    @Test void receiverWithdrawsSubmittedVersionPreservingEvidenceAndReopeningInvestigation() {
        when(handlings.updateContent(eq(71L),eq(901L),eq(3),any())).thenReturn(1);
        var response=service.withdrawInvestigation(101L,request());
        var changed=ArgumentCaptor.forClass(MesProEdhrDeviationHandlingDO.class);
        verify(handlings).updateContent(eq(71L),eq(901L),eq(3),changed.capture());
        assertEquals(4,response.getContentVersion());
        assertFalse(response.getInvestigationSubmitted());
        assertFalse(response.getReviewReady());
        assertEquals("DRAFT",changed.getValue().getCorrectiveStatus());
        assertNull(changed.getValue().getVerificationResult());
        assertNull(changed.getValue().getVerificationContent());
        assertEquals("完成整改",changed.getValue().getCorrectiveResult());
        assertEquals(current.getCompletedAt(),changed.getValue().getCompletedAt());
        assertNotEquals("hash3",response.getContentHash());
        verify(reviews).cancelAll(eq(901L),anyString());
        verify(reviews,never()).assign(any(),any(),anyString(),anyLong());
        verify(tasks).assignDeviationTask(eq(deviation),eq("DEVIATION_CORRECT"),isNull(),anyString());
        verify(tasks).assignDeviationTask(eq(deviation),eq("DEVIATION_RECEIVE"),eq(101L),contains("更换"));
        var command=ArgumentCaptor.forClass(MesProEdhrOperationAuditCommand.class);
        verify(audit).recordInCallerTransaction(command.capture());
        assertTrue(command.getValue().getMetadataJson().contains("撤回调查"));
    }
    @Test void onlyReceiverCanWithdrawAndHandlePermissionIsRequired() {
        assertThrows(ServiceException.class,()->service.withdrawInvestigation(201L,request()));
        when(permissions.hasPermission("mes:pro-edhr-deviation:handle")).thenReturn(false);
        assertThrows(ServiceException.class,()->service.withdrawInvestigation(101L,request()));
        noWrites();
    }
    @Test void staleVersionAndHashFailBeforeWrites() {
        var req=request(); req.setExpectedContentVersion(2);
        assertThrows(ServiceException.class,()->service.withdrawInvestigation(101L,req));
        req.setExpectedContentVersion(3);req.setExpectedContentHash("stale");
        assertThrows(ServiceException.class,()->service.withdrawInvestigation(101L,req)); noWrites();
    }
    @Test void closedDraftAndBlankReasonCannotWithdraw() {
        var req=request();req.setReason("  ");
        assertThrows(ServiceException.class,()->service.withdrawInvestigation(101L,req));
        deviation.setStatus("CLOSED");
        assertThrows(ServiceException.class,()->service.withdrawInvestigation(101L,request()));
        deviation.setStatus("OPEN");current.setInvestigationSubmitted(false);
        assertThrows(ServiceException.class,()->service.withdrawInvestigation(101L,request()));noWrites();
    }
    @Test void withdrawDuringCorrectiveAndVerificationDoesNotNeedDisabledReviewerToAct() {
        when(handlings.updateContent(eq(71L),eq(901L),eq(3),any())).thenReturn(1);
        for(String status:java.util.List.of("DRAFT","SUBMITTED","RETURNED")) {
            current.setCorrectiveStatus(status);
            assertFalse(service.withdrawInvestigation(101L,request()).getInvestigationSubmitted());
        }
        verify(reviews,never()).require(anyLong(),any(),anyString(),anyLong());
    }
    private void noWrites(){
        verify(handlings,never()).updateContent(anyLong(),anyLong(),anyInt(),any());
        verifyNoInteractions(tasks,audit);
    }
    private MesDeviationInvestigationWithdrawReqVO request(){
        var req=new MesDeviationInvestigationWithdrawReqVO();req.setDeviationId(901L);
        req.setExpectedContentVersion(3);req.setExpectedContentHash("hash3");req.setReason("更换已停用的责任人");return req;
    }
}
