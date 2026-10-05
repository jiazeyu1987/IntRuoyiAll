package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventRevisionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionPieceDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProductionSignatureEvidenceService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrNonconformanceReviewService;
import cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffService;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.handoff.MesActiveOrderHandoffTaskDO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesReportAllocationReleaseStateService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesSubmissionSignatureIdentityReader;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderDetail;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureEvidenceDTO;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesFrontlineReturnCorrectionServiceTest {
    @AfterEach void clearTenant() { TenantContextHolder.clear(); }

    @Test void ownSystemSignerCanCorrectExactRejectedRound() {
        Fixture f = new Fixture();
        var context = f.service.requireOwnReturned(176L, 413L, 91L, 0L, 342L, "PRODUCTION");
        assertEquals(91L, context.review().getId());
        assertEquals(413L, context.activeOrderId());
        assertEquals("1900000000000000001", context.returnTaskId());
        verify(f.productionEvidence).isValidForEvent(f.event);
        verify(f.handoff).findOwnReturn(176L, 91L, 342L);
        verifyNoInteractions(f.pqcReader);
        var ordered=inOrder(f.activeOrders,f.events,f.reviews,f.revisions);
        ordered.verify(f.activeOrders).selectByIdForUpdate(413L);
        ordered.verify(f.events).selectByIdForUpdate(176L);
        ordered.verify(f.reviews).selectLatestByEventIdForUpdate(176L);
        ordered.verify(f.revisions).selectListByEventIdForUpdate(176L);
    }

    @Test void detailAcquiresTheWriterLedgerBeforeAnyActiveOrEventRowLock() {
        Fixture f=new Fixture();
        f.event.setSignatureUserId(343L);
        assertThrows(ServiceException.class,()->f.service.getOwnReturned(176L,413L,91L,0L,342L,"PRODUCTION"));
        var ordered=inOrder(f.audit,f.activeOrders,f.events);
        ordered.verify(f.audit).acquireLedgerLock();
        ordered.verify(f.activeOrders).selectByIdForUpdate(413L);
        ordered.verify(f.events).selectByIdForUpdate(176L);
        verify(f.handoff,never()).completeReturnAndScheduleReview(anyLong(),anyLong(),anyLong(),anyLong());
    }

    @Test void listAcquiresTheWriterLedgerBeforePendingTaskReadAndRowLocks() {
        Fixture f=new Fixture();
        f.event.setSignatureUserId(343L);
        var pending=new MesActiveOrderHandoffTaskDO().setId(1900000000000000001L)
                .setActiveOrderId(413L).setWorkOrderId(990274L).setSourceType("PROCESS_POOL_EVENT")
                .setSourceId(176L).setRoundId(91L);
        when(f.handoff.listOwnReturnTasks(342L,"PRODUCTION")).thenReturn(List.of(pending));
        assertThrows(ServiceException.class,()->f.service.listOwnReturned(342L,"PRODUCTION"));
        var ordered=inOrder(f.audit,f.handoff,f.activeOrders,f.events);
        ordered.verify(f.audit).acquireLedgerLock();
        ordered.verify(f.handoff).listOwnReturnTasks(342L,"PRODUCTION");
        ordered.verify(f.activeOrders).selectByIdForUpdate(413L);
        ordered.verify(f.events).selectByIdForUpdate(176L);
        verify(f.handoff,never()).completeReturnAndScheduleReview(anyLong(),anyLong(),anyLong(),anyLong());
    }

    @Test void failedLedgerCannotAcquireBusinessRowLocksOrReadAFalseSuccess() {
        Fixture f=new Fixture();
        doThrow(new IllegalStateException("ledger unavailable")).when(f.audit).acquireLedgerLock();
        assertThrows(IllegalStateException.class,()->f.service.listOwnReturned(342L,"PRODUCTION"));
        assertThrows(IllegalStateException.class,()->f.service.getOwnReturned(176L,413L,91L,0L,342L,"PRODUCTION"));
        verifyNoInteractions(f.activeOrders,f.events,f.handoff);
    }

    @ParameterizedTest @ValueSource(strings={"otherSigner","employeeDomain","tenant","eventType",
            "oldCycle","wrongCycle","businessClosed","latestApproved","wrongReview","reviewTenant",
            "reviewType","sourceSignature","sourceDomain","sourceActor","simulated","staleRevision"})
    void invalidOwnershipOrRoundIsRejectedBeforeHandoffCompletion(String defect) {
        Fixture f = new Fixture();
        Long cycle = 413L, review = 91L, revision = 0L;
        switch (defect) {
            case "otherSigner" -> f.event.setSignatureUserId(343L);
            case "employeeDomain" -> f.event.setRawPayload("{\"activeOrderId\":413,\"signatureIdentityDomain\":\"MES_EMPLOYEE_PROFILE\"}");
            case "tenant" -> f.event.setTenantId(2L);
            case "eventType" -> f.event.setEventType("PQC_INSPECTION");
            case "oldCycle" -> f.active.setActiveStatus("REMOVED");
            case "wrongCycle" -> cycle = 414L;
            case "businessClosed" -> f.active.setBusinessStatus("RELEASED");
            case "latestApproved" -> f.review.setReviewStatus("APPROVED");
            case "wrongReview" -> review = 90L;
            case "reviewTenant" -> f.review.setTenantId(2L);
            case "reviewType" -> f.review.setLeaderType("PQC");
            case "sourceSignature" -> when(f.productionEvidence.isValidForEvent(f.event)).thenReturn(false);
            case "sourceDomain" -> f.evidence = evidence("MES_EMPLOYEE_PROFILE",342L);
            case "sourceActor" -> f.evidence = evidence("SYSTEM_USER",343L);
            case "simulated" -> f.event.setSimulated(true);
            case "staleRevision" -> revision = 99L;
            default -> throw new IllegalArgumentException(defect);
        }
        Long requestCycle=cycle, requestReview=review, requestRevision=revision;
        assertThrows(ServiceException.class, () -> f.service.requireOwnReturned(
                176L,requestCycle,requestReview,requestRevision,342L,"PRODUCTION"));
        verify(f.handoff, never()).completeReturnAndScheduleReview(anyLong(),anyLong(),anyLong(),anyLong());
    }

    @Test void releasedOrderAndFrozenWorkOrderFailBeforeCorrection() {
        Fixture released=new Fixture();
        when(released.releaseState.findReleasedActiveOrderIdsForUpdate(List.of(413L))).thenReturn(Set.of(413L));
        assertThrows(ServiceException.class,()->released.service.requireOwnReturned(176L,413L,91L,0L,342L,"PRODUCTION"));
        Fixture frozen=new Fixture();
        doThrow(new IllegalStateException("frozen")).when(frozen.nonconformance)
                .ensureWorkOrderNotFrozen(990274L,"本人退回更正");
        assertThrows(IllegalStateException.class,()->frozen.service.requireOwnReturned(176L,413L,91L,0L,342L,"PRODUCTION"));
    }

    @Test void missingRealReturnTaskDoesNotBecomeNavigationOnlySuccess() {
        Fixture f=new Fixture();
        when(f.handoff.findOwnReturn(176L,91L,342L)).thenThrow(new IllegalStateException("return task missing"));
        assertThrows(IllegalStateException.class,()->f.service.requireOwnReturned(176L,413L,91L,0L,342L,"PRODUCTION"));
    }

    @ParameterizedTest @ValueSource(strings={"missing","missingId","tenant","cycle","workOrder"})
    void returnTaskMustMatchTheLockedOriginalContext(String defect) {
        Fixture f=new Fixture();
        var pending=new MesActiveOrderHandoffTaskDO().setId(1900000000000000001L).setActiveOrderId(413L).setWorkOrderId(990274L);
        pending.setTenantId(1L);
        if("missingId".equals(defect))pending.setId(null);
        if("tenant".equals(defect))pending.setTenantId(2L);
        if("cycle".equals(defect))pending.setActiveOrderId(412L);
        if("workOrder".equals(defect))pending.setWorkOrderId(990275L);
        when(f.handoff.findOwnReturn(176L,91L,342L)).thenReturn("missing".equals(defect)?null:pending);
        assertThrows(ServiceException.class,()->f.service.requireOwnReturned(176L,413L,91L,0L,342L,"PRODUCTION"));
    }

    @Test void pqcOwnReturnRequiresTheFormalBoundTaskAndPqcSourceSignature() {
        Fixture f=new Fixture();
        f.event.setEventType("PQC_INSPECTION").setFeedbackSourceType("MES_PQC_INSPECTION_TASK")
                .setFeedbackSourceId(51L).setQaProcessId(61L);
        f.review.setLeaderType("PQC");
        f.evidence=evidence("SYSTEM_USER",342L,"PQC_SUBMIT");
        var task=MesPqcInspectionTaskDO.builder().id(51L).activeOrderId(413L).workOrderId(990274L)
                .routeId(920001L).qaProcessId(61L).submittedEventId(176L).build();task.setTenantId(1L);
        when(f.tasks.selectByIdForUpdate(51L)).thenReturn(task);
        when(f.pqcReader.read(11L,176L,413L,"PQC_SUBMIT")).thenReturn(new MesTeamLeaderActiveOrderDetail.SignatureDetail());
        assertEquals(91L,f.service.requireOwnReturned(176L,413L,91L,0L,342L,"PQC").review().getId());
        verifyNoInteractions(f.productionEvidence);
        task.setActiveOrderId(412L);
        assertThrows(ServiceException.class,()->f.service.requireOwnReturned(176L,413L,91L,0L,342L,"PQC"));
    }

    static ElectronicSignatureEvidenceDTO evidence(String domain,Long actor) {
        return evidence(domain,actor,"PRODUCTION_SUBMIT");
    }
    static ElectronicSignatureEvidenceDTO evidence(String domain,Long actor,String action) {
        return new ElectronicSignatureEvidenceDTO(11L,"MES",action,"MES_BATCH_RECORD",
                "subject","version",actor,"meaning","提交","reason",LocalDateTime.now(),
                "clock","SESSION_PLUS_PASSWORD","content","evidence","SHA-256","v1","v1","VALID",
                null,null,null,null,
                "{\"signatureIdentity\":{\"domain\":\""+domain+"\",\"tenantId\":1,\"signerId\":"+actor+",\"operatorId\":342}}",
                null,null,null,"actor","Asia/Shanghai");
    }

    static class Fixture {
        final GxpAuditService audit=mock(GxpAuditService.class);
        final MesProProcessPoolEventMapper events=mock(MesProProcessPoolEventMapper.class);
        final MesProcessPoolActiveOrderMapper activeOrders=mock(MesProcessPoolActiveOrderMapper.class);
        final MesProcessPoolSubmissionReviewMapper reviews=mock(MesProcessPoolSubmissionReviewMapper.class);
        final MesProProcessPoolEventRevisionMapper revisions=mock(MesProProcessPoolEventRevisionMapper.class);
        final MesPqcInspectionTaskMapper tasks=mock(MesPqcInspectionTaskMapper.class);
        final MesPqcInspectionPieceDetailMapper pieces=mock(MesPqcInspectionPieceDetailMapper.class);
        final ElectronicSignatureQueryService signatures=mock(ElectronicSignatureQueryService.class);
        final MesProductionSignatureEvidenceService productionEvidence=mock(MesProductionSignatureEvidenceService.class);
        final MesSubmissionSignatureIdentityReader pqcReader=mock(MesSubmissionSignatureIdentityReader.class);
        final MesProEdhrNonconformanceReviewService nonconformance=mock(MesProEdhrNonconformanceReviewService.class);
        final MesReportAllocationReleaseStateService releaseState=mock(MesReportAllocationReleaseStateService.class);
        final MesActiveOrderHandoffService handoff=mock(MesActiveOrderHandoffService.class);
        final MesProProcessPoolEventDO event=MesProProcessPoolEventDO.builder().id(176L).eventType("PRODUCTION_SUBMIT")
                .workOrderId(990274L).routeId(920001L).signatureUserId(342L).actualEmployeeId(342L)
                .deviceAccountId(342L).signatureId(11L).simulated(false)
                .rawPayload("{\"activeOrderId\":413,\"signatureIdentityDomain\":\"SYSTEM_USER\"}").build();
        final MesProcessPoolActiveOrderDO active=MesProcessPoolActiveOrderDO.builder().id(413L)
                .workOrderId(990274L).routeId(920001L).activeStatus("ACTIVE").businessStatus("ACTIVE").build();
        final MesProcessPoolSubmissionReviewDO review=MesProcessPoolSubmissionReviewDO.builder().id(91L)
                .eventId(176L).leaderType("PRODUCTION").reviewStatus("REJECTED").reviewedAt(LocalDateTime.now()).build();
        ElectronicSignatureEvidenceDTO evidence=evidence("SYSTEM_USER",342L);
        final MesFrontlineReturnCorrectionService service;
        Fixture() {
            TenantContextHolder.setTenantId(1L); event.setTenantId(1L);active.setTenantId(1L);review.setTenantId(1L);
            when(events.selectByIdForUpdate(176L)).thenReturn(event);
            when(activeOrders.selectByIdForUpdate(413L)).thenReturn(active);
            when(reviews.selectLatestByEventIdForUpdate(176L)).thenReturn(review);
            when(revisions.selectListByEventId(176L)).thenReturn(List.of());
            when(revisions.selectListByEventIdForUpdate(176L)).thenReturn(List.of());
            when(signatures.getById(11L)).thenAnswer(i->evidence);
            when(productionEvidence.isValidForEvent(event)).thenReturn(true);
            when(releaseState.findReleasedActiveOrderIdsForUpdate(List.of(413L))).thenReturn(Set.of());
            var pending=new MesActiveOrderHandoffTaskDO().setId(1900000000000000001L).setActiveOrderId(413L).setWorkOrderId(990274L);
            pending.setTenantId(1L);
            when(handoff.findOwnReturn(176L,91L,342L)).thenReturn(pending);
            service=new MesFrontlineReturnCorrectionService(events,activeOrders,reviews,revisions,tasks,pieces,
                    signatures,productionEvidence,pqcReader,nonconformance,releaseState,handoff);
            org.springframework.test.util.ReflectionTestUtils.setField(service,"gxpAuditService",audit);
        }
    }
}
