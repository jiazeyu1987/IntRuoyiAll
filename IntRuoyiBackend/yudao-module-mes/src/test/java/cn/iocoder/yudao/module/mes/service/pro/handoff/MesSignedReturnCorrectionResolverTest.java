package cn.iocoder.yudao.module.mes.service.pro.handoff;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.handoff.*;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesCorrectionSignatureEvidenceReader;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class MesSignedReturnCorrectionResolverTest {
 MesSignedReturnCorrectionResolver resolver;MesProProcessPoolEventRevisionMapper revisions;MesActiveOrderHandoffTaskMapper tasks;MesCorrectionSignatureEvidenceReader evidence;
 MesProProcessPoolEventDO event;MesProProcessPoolEventRevisionDO revision;MesProcessPoolSubmissionReviewDO previous;
 @BeforeEach void setup(){TenantContextHolder.setTenantId(1L);resolver=new MesSignedReturnCorrectionResolver();revisions=mock(MesProProcessPoolEventRevisionMapper.class);tasks=mock(MesActiveOrderHandoffTaskMapper.class);evidence=mock(MesCorrectionSignatureEvidenceReader.class);var owners=mock(MesActiveOrderHandoffOwnerResolver.class);for(var e:Map.of("revisions",revisions,"tasks",tasks,"owners",owners,"correctionEvidence",evidence).entrySet())ReflectionTestUtils.setField(resolver,e.getKey(),e.getValue());
 event=MesActiveOrderHandoffOwnerResolverTest.production("SYSTEM_USER").setRawPayload("{\"activeOrderId\":413,\"supersededReviewId\":700,\"count\":3}");previous=MesActiveOrderHandoffLifecycleTest.review(700L);revision=MesProProcessPoolEventRevisionDO.builder().id(800L).eventId(176L).revisionStatus("EFFECTIVE").modifiedByUserId(342L).revisionSignatureUserId(342L).revisionSignatureId(902L).revisionSignatureSnapshot("{\"signatureId\":902,\"actorId\":342,\"signedAt\":\"2026-10-05T11:00:00\"}").serverRevisionTime(LocalDateTime.of(2026,10,5,11,0)).beforePayload("{\"activeOrderId\":413,\"count\":2}").afterPayload(event.getRawPayload()).build();revision.setTenantId(1L);when(revisions.selectListByEventId(176L)).thenReturn(List.of(revision));when(owners.originalActor(event)).thenReturn(342L);when(owners.submissionIdentity(event)).thenReturn(new MesActiveOrderHandoffOwnerResolver.SubmissionIdentity("SYSTEM_USER",342L,1L));
 }
 @AfterEach void clear(){TenantContextHolder.clear();}
 @Test void exactActiveAndCorrectionRoundAreRequired(){var task=MesActiveOrderHandoffContractTest.review().setSourceType("PROCESS_POOL_EVENT_SIGNED_REVISION").setTaskType("PRODUCTION_REVIEW").setRoundId(800L).setCandidateUserSnapshot("341").setInitiatedBy(342L).setResponsibilitySnapshotJson("{\"correctionOrigin\":\"OWN_RETURN_CORRECTION\"}");when(tasks.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(task));assertSame(revision,resolver.find(event,previous));verify(evidence).require(event,revision);task.setActiveOrderId(414L);assertThrows(RuntimeException.class,()->resolver.find(event,previous));task.setActiveOrderId(413L).setCandidateUserSnapshot("349");assertThrows(RuntimeException.class,()->resolver.find(event,previous));}
 @Test void explicitLeaderPqcSourceUsesActualLeaderSignatureAndFormalScope(){event.setEventType("PQC_INSPECTION").setFeedbackSourceId(55L);previous.setLeaderType("PQC");revision.setModifiedByUserId(343L).setRevisionSignatureUserId(343L).setRevisionSignatureSnapshot("{\"signatureId\":902,\"actorId\":343,\"signedAt\":\"2026-10-05T11:00:00\"}");var pqc=mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper.class);var source=new cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO().setId(55L).setActiveOrderId(413L);source.setTenantId(1L);when(pqc.selectById(55L)).thenReturn(source);ReflectionTestUtils.setField(resolver,"pqcTasks",pqc);var task=MesActiveOrderHandoffContractTest.review().setSourceType("PROCESS_POOL_EVENT_SIGNED_REVISION").setTaskType("PQC_REVIEW").setRoundId(800L).setCandidateUserSnapshot("341").setInitiatedBy(343L).setResponsibilitySnapshotJson("{\"correctionOrigin\":\"LEADER_PQC_CORRECTION\"}");when(tasks.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(task));assertSame(revision,resolver.find(event,previous));verify(evidence).require(event,revision);task.setResponsibilitySnapshotJson("{\"correctionOrigin\":\"OWN_RETURN_CORRECTION\"}");assertThrows(RuntimeException.class,()->resolver.find(event,previous));}
 @Test void snapshotAloneNeverBypassesFormalFieldChangeEvidence(){doThrow(new IllegalStateException("missing formal FIELD_CHANGE challenge/audit/hash")).when(evidence).require(event,revision);var task=MesActiveOrderHandoffContractTest.review().setSourceType("PROCESS_POOL_EVENT_SIGNED_REVISION").setTaskType("PRODUCTION_REVIEW").setRoundId(800L).setCandidateUserSnapshot("341").setInitiatedBy(342L).setResponsibilitySnapshotJson("{\"correctionOrigin\":\"OWN_RETURN_CORRECTION\"}");when(tasks.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(task));assertThrows(IllegalStateException.class,()->resolver.find(event,previous));}
 @Test void alteredAfterPayloadOrAnotherRejectedRoundCannotBecomeCurrentReview(){event.setRawPayload("{\"activeOrderId\":413,\"supersededReviewId\":700,\"count\":4}");assertThrows(RuntimeException.class,()->resolver.find(event,previous));verifyNoInteractions(evidence,tasks);event.setRawPayload(revision.getAfterPayload());revision.setAfterPayload("{\"activeOrderId\":413,\"supersededReviewId\":701,\"count\":3}");assertNull(resolver.find(event,previous));verifyNoInteractions(evidence,tasks);}

 @Test void profileCorrectionRequiresItsOwnOriginAndOriginalLeaderSeparateSignature(){
  var owners=(MesActiveOrderHandoffOwnerResolver)ReflectionTestUtils.getField(resolver,"owners");
  when(owners.profileProductionLeader(event)).thenReturn(341L);when(owners.originalActor(event)).thenThrow(new IllegalStateException("PROFILE is not SYSTEM_USER"));
  previous.setLeaderType("PRODUCTION");revision.setModifiedByUserId(341L).setRevisionSignatureUserId(341L)
      .setRevisionSignatureSnapshot("{\"signatureId\":902,\"actorId\":341,\"signedAt\":\"2026-10-05T11:00:00\"}");
  var task=MesActiveOrderHandoffContractTest.review().setSourceType("PROCESS_POOL_EVENT_SIGNED_REVISION").setTaskType("PRODUCTION_REVIEW").setRoundId(800L).setCandidateUserSnapshot("341")
      .setInitiatedBy(341L).setResponsibilitySnapshotJson("{\"correctionOrigin\":\"LEADER_PROFILE_CORRECTION\"}");
  when(tasks.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(task));
  assertSame(revision,resolver.find(event,previous));verify(evidence).require(event,revision);
  task.setResponsibilitySnapshotJson("{\"correctionOrigin\":\"OWN_RETURN_CORRECTION\"}");assertThrows(RuntimeException.class,()->resolver.find(event,previous));
  task.setResponsibilitySnapshotJson("{\"correctionOrigin\":\"LEADER_PQC_CORRECTION\"}");assertThrows(RuntimeException.class,()->resolver.find(event,previous));
  task.setResponsibilitySnapshotJson("{\"correctionOrigin\":\"LEADER_PROFILE_CORRECTION\"}");revision.setModifiedByUserId(349L);assertThrows(RuntimeException.class,()->resolver.find(event,previous));
 }

}
