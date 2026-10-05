package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchActiveOrderDetailService;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc.MesPqcReleaseOrderDetailService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MesActiveOrderCorrectionEvidenceServiceTest {
    @AfterEach void clear() { TenantContextHolder.clear(); }
    @Test void authorizedTeamAndArchivedBatchExposeCorrectionAndUseExistingSignatureEntryWithoutWrites() {
        var f = new MesCorrectionSignatureEvidenceReaderTest.Fixture();
        var team=mock(MesTeamLeaderActiveOrderDetailService.class);var pqc=mock(MesPqcReleaseOrderDetailService.class);
        var batch=mock(MesProEdhrBatchActiveOrderDetailService.class);var events=mock(MesProProcessPoolEventMapper.class);
        var revisions=mock(MesProProcessPoolEventRevisionMapper.class);var binding=mock(MesProductionSubmissionReadBinding.class);
        var service=new MesActiveOrderCorrectionEvidenceService(team,pqc,batch,events,revisions,mock(MesPqcInspectionTaskMapper.class),binding,f.reader);
        var detail=new MesTeamLeaderActiveOrderDetail().setActiveOrderId(100L).setProcesses(List.of(new MesTeamLeaderActiveOrderDetail.ProcessDetail()
                .setProcessName("生产工序").setSubmissions(List.of(new MesTeamLeaderActiveOrderDetail.SubmissionDetail().setEventId(1L)))));
        when(team.getDetail(7L,100L)).thenReturn(detail);when(batch.getDetail(900L)).thenReturn(detail);
        when(events.selectById(1L)).thenReturn(f.event);when(revisions.selectListByEventId(1L)).thenReturn(List.of(f.revision));
        for(var timeline:List.of(service.getTeam(7L,100L),service.getBatch(900L,null))) {
            assertEquals(100L,timeline.activeOrderId());assertEquals(1,timeline.corrections().size());
            assertEquals("冻结本人",timeline.corrections().get(0).signerName());assertEquals("10",timeline.corrections().get(0).changes().get(0).beforeValue());
        }
        var signatures=new MesActiveOrderSignatureEvidenceService(team,pqc,batch,f.signatures,events,null,revisions,null,binding,null,null,null,null,null);
        ReflectionTestUtils.setField(signatures,"correctionEvidence",service);
        assertEquals(11L,signatures.getBatch(900L,null,11L).evidence().id());
        assertEquals("VALID",signatures.getTeam(7L,100L,11L).verification().verificationStatus());
        verify(binding,atLeastOnce()).require(f.event,100L);
        verify(revisions,never()).insert(any(cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventRevisionDO.class));
    }
    @Test void authorizationFailurePrecedesAnyCorrectionReadAndAmbiguousBatchFailsBeforeDetail() {
        var team=mock(MesTeamLeaderActiveOrderDetailService.class);var events=mock(MesProProcessPoolEventMapper.class);
        var revisions=mock(MesProProcessPoolEventRevisionMapper.class);var batch=mock(MesProEdhrBatchActiveOrderDetailService.class);
        var service=new MesActiveOrderCorrectionEvidenceService(team,null,batch,events,revisions,null,null,null);
        when(team.getDetail(9L,100L)).thenThrow(new IllegalStateException("denied"));
        assertThrows(IllegalStateException.class,()->service.getTeam(9L,100L));
        assertThrows(IllegalStateException.class,()->service.getBatch(900L,100L));
        assertThrows(IllegalStateException.class,()->service.getBatch(null,null));
        verifyNoInteractions(events,revisions,batch);
    }
    @Test void pqcReleaseReadsSignedRevisionOnlyFromExactTaskAndCycle() {
        var f = new MesCorrectionSignatureEvidenceReaderTest.Fixture(); f.pqc();
        var pqc=mock(MesPqcReleaseOrderDetailService.class);var events=mock(MesProProcessPoolEventMapper.class);
        var revisions=mock(MesProProcessPoolEventRevisionMapper.class);var tasks=mock(MesPqcInspectionTaskMapper.class);
        var binding=mock(MesProductionSubmissionReadBinding.class);
        var detail=new MesTeamLeaderActiveOrderDetail().setActiveOrderId(100L).setProcesses(List.of(new MesTeamLeaderActiveOrderDetail.ProcessDetail()
                .setProcessName("PQC工序").setPqcSubmissions(List.of(new MesTeamLeaderActiveOrderDetail.PqcSubmissionDetail().setSubmittedEventIds(List.of(1L))))));
        var task=new MesPqcInspectionTaskDO().setId(31L).setActiveOrderId(100L).setSubmittedEventId(1L)
                .setWorkOrderId(3L).setRouteId(4L).setRouteProcessId(5L).setProcessId(6L);task.setTenantId(1L);
        when(pqc.get(7L,200L)).thenReturn(new MesPqcReleaseOrderDetailService.Result(detail,List.of()));
        when(events.selectById(1L)).thenReturn(f.event);when(revisions.selectListByEventId(1L)).thenReturn(List.of(f.revision));
        when(tasks.selectById(31L)).thenReturn(task);
        var service=new MesActiveOrderCorrectionEvidenceService(null,pqc,null,events,revisions,tasks,binding,f.reader);
        var timeline=service.getPqc(7L,200L);
        assertEquals("PQC_INSPECTION",timeline.corrections().get(0).eventType());
        assertEquals(11L,timeline.corrections().get(0).signatureId());verifyNoInteractions(binding);
        var signatures=new MesActiveOrderSignatureEvidenceService(null,pqc,null,f.signatures,events,null,revisions,null,binding,null,null,null,null,null);
        ReflectionTestUtils.setField(signatures,"correctionEvidence",service);
        assertEquals("VALID",signatures.getPqc(7L,200L,11L).verification().verificationStatus());
        task.setActiveOrderId(99L);
        assertThrows(IllegalStateException.class,()->service.getPqc(7L,200L));
        assertThrows(IllegalStateException.class,()->signatures.getPqc(7L,200L,11L));
        task.setActiveOrderId(100L);task.setSubmittedEventId(99L);
        assertThrows(IllegalStateException.class,()->service.getPqc(7L,200L));
    }
}
