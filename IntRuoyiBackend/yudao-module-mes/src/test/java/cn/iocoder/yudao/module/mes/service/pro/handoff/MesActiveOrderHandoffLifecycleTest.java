package cn.iocoder.yudao.module.mes.service.pro.handoff;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.handoff.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.handoff.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class MesActiveOrderHandoffLifecycleTest {
 MesActiveOrderHandoffService service;MesActiveOrderHandoffTaskMapper tasks;MesProcessPoolActiveOrderMapper orders;MesProProcessPoolEventMapper events;MesProcessPoolSubmissionReviewMapper reviews;MesActiveOrderHandoffOwnerResolver owners;MesActiveOrderHandoffDeliveryService delivery;MesProProcessPoolEventRevisionMapper revisions;MesSignedReturnCorrectionResolver corrections;
 MesProcessPoolActiveOrderDO order;MesProProcessPoolEventDO event;
 @BeforeEach void fixture(){TenantContextHolder.setTenantId(1L);service=new MesActiveOrderHandoffService();tasks=mock(MesActiveOrderHandoffTaskMapper.class);orders=mock(MesProcessPoolActiveOrderMapper.class);events=mock(MesProProcessPoolEventMapper.class);reviews=mock(MesProcessPoolSubmissionReviewMapper.class);owners=mock(MesActiveOrderHandoffOwnerResolver.class);delivery=mock(MesActiveOrderHandoffDeliveryService.class);revisions=mock(MesProProcessPoolEventRevisionMapper.class);corrections=mock(MesSignedReturnCorrectionResolver.class);
 for(var entry:Map.of("tasks",tasks,"orders",orders,"events",events,"reviews",reviews,"owners",owners,"delivery",delivery,"revisions",revisions,"correctionResolver",corrections).entrySet())ReflectionTestUtils.setField(service,entry.getKey(),entry.getValue());ReflectionTestUtils.setField(service,"audit",mock(MesActiveOrderHandoffAudit.class));ReflectionTestUtils.setField(service,"pqcTasks",mock(MesPqcInspectionTaskMapper.class));
 var workOrders=mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper.class);var routeProcesses=mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper.class);var processes=mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper.class);
 ReflectionTestUtils.setField(service,"workOrders",workOrders);ReflectionTestUtils.setField(service,"routeProcesses",routeProcesses);ReflectionTestUtils.setField(service,"processes",processes);
 var wo=cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO.builder().id(274L).code("EDHR-413").build();wo.setTenantId(1L);when(workOrders.selectById(274L)).thenReturn(wo);
 var rp=cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO.builder().id(91L).routeId(98L).processId(15L).build();when(routeProcesses.selectById(91L)).thenReturn(rp);
 when(processes.selectById(15L)).thenReturn(cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO.builder().id(15L).name("组装").build());
 when(owners.initiatorSnapshot(anyLong(),any())).thenAnswer(i->Map.<String,Object>of("domain","SYSTEM_USER","id",i.<Long>getArgument(0),"name","实际发起人"));
 order=MesProcessPoolActiveOrderDO.builder().id(413L).workOrderId(274L).routeId(98L).leaderUserId(341L).activeStatus("ACTIVE").build();order.setTenantId(1L);when(orders.selectByIdForUpdate(413L)).thenReturn(order);when(orders.selectById(413L)).thenReturn(order);
 event=MesProProcessPoolEventDO.builder().id(176L).eventType("PRODUCTION_SUBMIT").actualEmployeeId(342L).signatureUserId(342L).signatureId(901L).deviceAccountId(1L).routeProcessId(91L).rawPayload("{\"activeOrderId\":413}").build();event.setTenantId(1L);when(events.selectById(176L)).thenReturn(event);
 when(tasks.insert(any(MesActiveOrderHandoffTaskDO.class))).thenReturn(1);when(owners.productionLeader(order)).thenReturn(341L);
  ReflectionTestUtils.setField(owners,"handoffTasks",tasks);ReflectionTestUtils.setField(owners,"orders",orders);
  when(owners.frozenPqcReviewers(any(),any())).thenCallRealMethod();
 }
 @Test void productionReadyOnlyNotifiesProductionWithoutRouteAssignments() {
  when(owners.productionEmployees(order)).thenReturn("342");
  service.productionReady(413L,341L);
  var captured=ArgumentCaptor.forClass(MesActiveOrderHandoffTaskDO.class);
  verify(tasks).insert(captured.capture());
  assertEquals("PRODUCTION_HANDOFF",captured.getValue().getTaskType());
  verify(delivery).schedule(captured.getValue());
  verifyNoInteractions((MesPqcInspectionTaskMapper)ReflectionTestUtils.getField(service,"pqcTasks"));
 }
 @Test void reviewDispositionRequiresBusinessPermissionWithoutRouteAssignment() {
  assertDoesNotThrow(()->service.assertQaCanDispose(91L,413L,345L));
  verify(owners).user(345L,"mes:pro-edhr-nonconformance-review:dispose");
  verifyNoInteractions(tasks);
  doThrow(new IllegalStateException("denied")).when(owners).user(346L,"mes:pro-edhr-nonconformance-review:dispose");
  assertThrows(RuntimeException.class,()->service.assertQaCanDispose(91L,413L,346L));
 }
 @Test void dispositionCompletesWithoutConfiguredQaTaskAndNotifiesProductionLeader() {
  var review=new cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrNonconformanceReviewDO()
    .setId(91L).setWorkOrderId(274L).setReviewStatus("closed").setQaUserId(345L)
    .setClosedAt(LocalDateTime.now()).setDisposition("concession_release").setReviewOpinion("评审材料已核实");
  service.qaDisposed(review,413L,null,345L);
  var captured=ArgumentCaptor.forClass(MesActiveOrderHandoffTaskDO.class);verify(tasks).insert(captured.capture());
  assertEquals("QA_DECISION_HANDOFF",captured.getValue().getTaskType());
  assertEquals("341",captured.getValue().getCandidateUserSnapshot());
  verify(delivery).schedule(captured.getValue());
  verify(tasks,never()).close(any(),any(),any(),any(),any(),any(),any());
 }
 @Test void oldCycleQaCandidateCannotDispose(){order.setActiveStatus("VOID");var task=MesActiveOrderHandoffContractTest.review().setCandidateUserSnapshot("345");when(tasks.byIdentity(413L,"QA_REVIEW","NONCONFORMANCE_REVIEW",91L,91L)).thenReturn(task);assertThrows(RuntimeException.class,()->service.assertQaCanDispose(91L,413L,345L));}
 @Test void voidResultRemainsReadOnlyAfterOrderDeletion(){var t=MesActiveOrderHandoffContractTest.review().setTaskType("QA_DECISION_HANDOFF").setSourceType("NONCONFORMANCE_REVIEW").setSourceId(91L).setRoundId(91L).setCandidateUserSnapshot("341").setStatus("DONE").setReason("void：正式作废").setActionUrl("/user/profile?activeOrderId=413&reviewId=91&handoffTaskId=1900000000000000001&roundId=91&handoffType=QA_DECISION_HANDOFF&tab=notifyMessage");when(tasks.selectById(t.getId())).thenReturn(t);when(orders.selectById(413L)).thenReturn(null);var context=service.navigationContext(t.getId(),341L);assertFalse(context.current());assertFalse(context.processable());assertEquals(91L,context.task().getSourceId());}
 @Test void canceledNotificationNeverResolvesToLatestOrder(){var t=MesActiveOrderHandoffContractTest.review().setStatus("CANCELED");when(tasks.selectById(t.getId())).thenReturn(t);assertThrows(RuntimeException.class,()->service.navigationContext(t.getId(),343L));verifyNoInteractions(events);}
 private MesActiveOrderHandoffTaskDO completedTask(){return MesActiveOrderHandoffContractTest.review().setStatus("DONE").setCompletedBy(343L).setCompletedAt(LocalDateTime.of(2026,10,6,6,30)).setCompletionSourceId(700L).setRowVersion(1);}
 @Test void closedDoneNavigationReturnsOnlyOriginalCompletionWithoutBusinessWrites(){
  var t=completedTask();String original=JsonUtils.toJsonString(t);when(tasks.selectById(t.getId())).thenReturn(t);
  for(String status:List.of("RELEASED","VOID","ARCHIVED")){order.setActiveStatus(status);var c=service.navigationContext(t.getId(),343L);assertSame(t,c.task());assertFalse(c.current());assertFalse(c.processable());assertFalse(c.profileLeaderCorrection());assertEquals(700L,c.task().getCompletionSourceId());assertEquals(original,JsonUtils.toJsonString(t));}
  verify(tasks,times(3)).selectById(t.getId());verifyNoMoreInteractions(tasks);verifyNoInteractions(events,reviews,revisions,delivery);
 }
 @Test void closedDoneCanShowOwnFrozenTaskWithoutReconstructingDeletedSource(){
  var t=completedTask();when(tasks.selectById(t.getId())).thenReturn(t);when(orders.selectById(t.getActiveOrderId())).thenReturn(null);
  var c=service.navigationContext(t.getId(),343L);assertSame(t,c.task());assertFalse(c.current());assertFalse(c.processable());assertEquals(701L,c.task().getRoundId());verifyNoInteractions(events,reviews,revisions,delivery);
 }
 @Test void closedDoneRequiresAllActualCompletionFieldsAndNeverAllowsTodoOrCanceled(){
  order.setActiveStatus("RELEASED");
  for(var t:List.of(completedTask().setCompletedBy(null),completedTask().setCompletedBy(0L),completedTask().setCompletedAt(null),completedTask().setCompletionSourceId(null),completedTask().setCompletionSourceId(0L),completedTask().setStatus("TODO"),completedTask().setStatus("CANCELED"))){when(tasks.selectById(t.getId())).thenReturn(t);assertThrows(RuntimeException.class,()->service.navigationContext(t.getId(),343L));}
  verifyNoInteractions(events,reviews,revisions,delivery);
 }
 @Test void closedDoneRetainsTenantFrozenCandidateAndExactActionSourceRoundGuards(){
  order.setActiveStatus("RELEASED");var wrongTenant=completedTask();wrongTenant.setTenantId(2L);when(tasks.selectById(wrongTenant.getId())).thenReturn(wrongTenant);assertThrows(RuntimeException.class,()->service.navigationContext(wrongTenant.getId(),343L));
  var t=completedTask();when(tasks.selectById(t.getId())).thenReturn(t);assertThrows(RuntimeException.class,()->service.navigationContext(t.getId(),344L));
  for(String url:List.of(t.getActionUrl().replace("activeOrderId=413","activeOrderId=414"),t.getActionUrl().replace("eventId=176","eventId=177"),t.getActionUrl().replace("roundId=701","roundId=702"),t.getActionUrl()+"&eventId=176","https://outside.invalid"+t.getActionUrl())){var altered=completedTask().setActionUrl(url);when(tasks.selectById(altered.getId())).thenReturn(altered);assertThrows(RuntimeException.class,()->service.navigationContext(altered.getId(),343L));}
  verifyNoInteractions(events,reviews,revisions,delivery);
 }
 @Test void activeDoneStillUsesExistingCurrentReadonlySourcePath(){var t=completedTask();when(tasks.selectById(t.getId())).thenReturn(t);var c=service.navigationContext(t.getId(),343L);assertTrue(c.current());assertFalse(c.processable());assertSame(t,c.task());verifyNoInteractions(events,reviews,revisions,delivery);}
 @Test void completedBusinessStatusStillAllowsFormalLeaderContinuation(){order.setBusinessStatus("COMPLETED");when(tasks.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of());assertDoesNotThrow(()->service.productionLeaderContinued(413L,341L,227L));assertThrows(RuntimeException.class,()->service.productionLeaderContinued(413L,342L,227L));}
 @Test void correctionCompletesOriginalReturnAndCreatesExactRevisionReview(){var t=MesActiveOrderHandoffContractTest.review().setRouteProcessId(91L).setTaskType("PRODUCTION_RETURN").setRoundId(700L).setCandidateUserSnapshot("342");when(tasks.own(1L,342L)).thenReturn(List.of(t));when(tasks.lock(t.getId())).thenReturn(t);when(tasks.selectById(t.getId())).thenReturn(t);when(tasks.close(eq(1L),eq(t.getId()),eq(0),eq("DONE"),eq(342L),eq(800L),any())).thenAnswer(i->{t.setStatus("DONE").setRowVersion(1).setCompletionSourceId(800L);return 1;});
 var previous=review(700L);when(reviews.selectById(700L)).thenReturn(previous);event.setRawPayload("{\"activeOrderId\":413,\"supersededReviewId\":700,\"count\":3}");var revision=MesProProcessPoolEventRevisionDO.builder().id(800L).eventId(176L).revisionStatus("EFFECTIVE").modifiedByUserId(342L).revisionSignatureUserId(342L).revisionSignatureId(902L).revisionSignatureSnapshot("{\"signatureId\":902,\"actorId\":342,\"signedAt\":\"2026-10-05T10:30:00\"}").afterPayload(event.getRawPayload()).changeReason("本人核对后改值").build();revision.setTenantId(1L);when(revisions.selectById(800L)).thenReturn(revision);
 service.completeReturnAndScheduleReview(176L,700L,800L,342L);assertEquals("DONE",t.getStatus());var c=ArgumentCaptor.forClass(MesActiveOrderHandoffTaskDO.class);verify(tasks).insert(c.capture());assertEquals(176L,c.getValue().getSourceId());assertEquals(800L,c.getValue().getRoundId());assertEquals("341",c.getValue().getCandidateUserSnapshot());verify(corrections).verifyRevision(event,previous,revision,342L);assertEquals("REJECTED",previous.getReviewStatus());}
 @Test void formalLeaderPqcCorrectionClosesOriginalReturnWithActualLeaderAndFreezesExplicitSource(){pqcSource();var previous=review(700L).setLeaderType("PQC").setLeaderUserId(343L);when(reviews.selectById(700L)).thenReturn(previous);when(reviews.selectLatestByEventIdForUpdate(176L)).thenReturn(previous);var revision=new MesProProcessPoolEventRevisionDO().setId(800L).setEventId(176L).setChangeReason("原责任组长正式更正");when(revisions.selectById(800L)).thenReturn(revision);when(revisions.selectListByEventId(176L)).thenReturn(List.of(revision));when(owners.originalActor(event)).thenReturn(344L);var returned=MesActiveOrderHandoffContractTest.review().setTaskType("PQC_RETURN").setRoundId(700L).setCandidateUserSnapshot("344");when(tasks.byIdentity(413L,"PQC_RETURN","PROCESS_POOL_EVENT",176L,700L)).thenReturn(returned);when(tasks.lock(returned.getId())).thenReturn(returned);when(tasks.selectById(returned.getId())).thenReturn(returned);when(tasks.close(eq(1L),eq(returned.getId()),eq(0),eq("DONE"),eq(343L),eq(800L),any())).thenAnswer(i->{returned.setStatus("DONE").setRowVersion(1).setCompletedBy(343L).setCompletionSourceId(800L);return 1;});service.completeLeaderPqcCorrectionAndScheduleReview(176L,700L,800L,343L);assertEquals(343L,returned.getCompletedBy());var captured=ArgumentCaptor.forClass(MesActiveOrderHandoffTaskDO.class);verify(tasks).insert(captured.capture());assertEquals("PQC_REVIEW",captured.getValue().getTaskType());assertEquals("343",captured.getValue().getCandidateUserSnapshot());assertEquals(343L,captured.getValue().getInitiatedBy());assertTrue(captured.getValue().getResponsibilitySnapshotJson().contains("LEADER_PQC_CORRECTION"));assertPqcStage(captured.getValue(),800L);verify(corrections).verifyLeaderPqcRevision(event,previous,revision,343L);}
  @Test void submittedPqcReviewUsesFormalTaskStageWithoutMutatingQaEvent(){
   pqcSource();when(owners.originalActor(event)).thenReturn(344L);
   when(owners.pqcLeader(event)).thenReturn(343L);
  service.pqcSubmitted(55L,176L,344L);var c=ArgumentCaptor.forClass(MesActiveOrderHandoffTaskDO.class);verify(tasks).insert(c.capture());
  assertEquals("PQC_REVIEW",c.getValue().getTaskType());assertEquals("343",c.getValue().getCandidateUserSnapshot());assertPqcStage(c.getValue(),176L);
  verify(owners).pqcLeader(event);assertNull(event.getRouteProcessId());assertNull(event.getProcessId());assertEquals(985L,event.getQaProcessId());
 }
 @Test void rejectedPqcReviewCreatesOnlyTheOriginalTaskStageReturn(){
  pqcSource();var rejected=review(700L).setLeaderType("PQC").setLeaderUserId(343L).setReviewSignatureUserId(343L);
  when(reviews.selectById(700L)).thenReturn(rejected);when(reviews.selectLatestByEventIdForUpdate(176L)).thenReturn(rejected);
  var reviewTask=MesActiveOrderHandoffContractTest.review().setRoundId(176L);
  when(tasks.byIdentity(413L,"PQC_REVIEW","PROCESS_POOL_EVENT",176L,176L)).thenReturn(reviewTask);stubClose(reviewTask,343L,700L);
  when(owners.submissionIdentity(event)).thenReturn(new MesActiveOrderHandoffOwnerResolver.SubmissionIdentity("SYSTEM_USER",344L,1L));
  service.reviewed(176L,700L);var c=ArgumentCaptor.forClass(MesActiveOrderHandoffTaskDO.class);verify(tasks).insert(c.capture());
  assertEquals("PQC_RETURN",c.getValue().getTaskType());assertEquals("344",c.getValue().getCandidateUserSnapshot());assertPqcStage(c.getValue(),700L);
  assertTrue(c.getValue().getReason().startsWith(rejected.getReviewRemark()));assertEquals("DONE",reviewTask.getStatus());
  verify(owners,never()).pqcLeader(any());assertNull(event.getRouteProcessId());assertNull(event.getProcessId());
 }
 @Test void approvedPqcTaskAlreadyConfirmedByAggregationClosesItsReviewWithoutCreatingReturn(){
  pqcSource().setTaskStatus("CONFIRMED");var approved=review(700L).setLeaderType("PQC").setLeaderUserId(343L).setReviewSignatureUserId(343L).setReviewStatus("APPROVED");
  when(reviews.selectById(700L)).thenReturn(approved);when(reviews.selectLatestByEventIdForUpdate(176L)).thenReturn(approved);
  var reviewTask=MesActiveOrderHandoffContractTest.review().setRoundId(176L);when(tasks.byIdentity(413L,"PQC_REVIEW","PROCESS_POOL_EVENT",176L,176L)).thenReturn(reviewTask);stubClose(reviewTask,343L,700L);
  service.reviewed(176L,700L);assertEquals("DONE",reviewTask.getStatus());verify(tasks,never()).insert(any(MesActiveOrderHandoffTaskDO.class));verifyNoInteractions(delivery);assertNull(event.getProcessId());assertNull(event.getRouteProcessId());
 }

 @Test void productionApprovalDoesNotRequireOrCreatePqcHandoff() {
  var approved=review(700L).setReviewStatus("APPROVED");when(reviews.selectById(700L)).thenReturn(approved);when(reviews.selectLatestByEventIdForUpdate(176L)).thenReturn(approved);
  var reviewTask=MesActiveOrderHandoffContractTest.review().setRouteProcessId(91L).setTaskType("PRODUCTION_REVIEW").setRoundId(176L).setCandidateUserSnapshot("341");when(tasks.byIdentity(413L,"PRODUCTION_REVIEW","PROCESS_POOL_EVENT",176L,176L)).thenReturn(reviewTask);stubClose(reviewTask,341L,700L);
  when(tasks.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of());
  service.reviewed(176L,700L);
  assertEquals("DONE",reviewTask.getStatus());
  verify(tasks,never()).insert(any(MesActiveOrderHandoffTaskDO.class));verifyNoInteractions(delivery);
 }
 @org.junit.jupiter.params.ParameterizedTest
 @org.junit.jupiter.params.provider.ValueSource(booleans={false,true})
 void successfulPqcSubmissionCreatesReviewWithoutReceptionNotification(boolean self) {
  pqcSource();Long actor=self?343L:344L;event.setActualEmployeeId(actor).setSignatureUserId(actor);when(owners.originalActor(event)).thenReturn(actor);
  when(owners.pqcLeader(event)).thenReturn(343L);
  service.pqcSubmitted(55L,176L,actor);
  var c=ArgumentCaptor.forClass(MesActiveOrderHandoffTaskDO.class);verify(tasks).insert(c.capture());
  assertEquals("PQC_REVIEW",c.getValue().getTaskType());assertEquals("343",c.getValue().getCandidateUserSnapshot());assertPqcStage(c.getValue(),176L);
  verify(tasks,never()).close(any(),any(),any(),any(),any(),any(),any());
  verify(delivery).schedule(c.getValue());
 }
 @org.junit.jupiter.params.ParameterizedTest
 @org.junit.jupiter.params.provider.ValueSource(strings={"missingSignature","wrongSignatureActor","confirmedBeforeSubmit","missingLeader"})
 void invalidSubmissionCannotCreatePqcReview(String defect) {
  var pqc=pqcSource();when(owners.originalActor(event)).thenReturn(344L);when(owners.pqcLeader(event)).thenReturn(343L);
  switch(defect) {
   case "missingSignature" -> when(owners.originalActor(event)).thenThrow(new IllegalStateException("missing signature"));
   case "wrongSignatureActor" -> when(owners.originalActor(event)).thenReturn(345L);
   case "confirmedBeforeSubmit" -> pqc.setTaskStatus("CONFIRMED");
   case "missingLeader" -> when(owners.pqcLeader(event)).thenThrow(new IllegalStateException("missing leader"));
  }
  assertThrows(RuntimeException.class,()->service.pqcSubmitted(55L,176L,344L));
  verify(tasks,never()).insert(any(MesActiveOrderHandoffTaskDO.class));verifyNoInteractions(delivery);
 }
 @org.junit.jupiter.params.ParameterizedTest
 @org.junit.jupiter.params.provider.ValueSource(booleans={false,true})
 void ownPqcCorrectionPreservesExactReturnedStageAndRejectsAlteredStage(boolean alteredReturn){
  pqcSource();var returned=MesActiveOrderHandoffContractTest.review().setTaskType("PQC_RETURN").setRoundId(700L).setCandidateUserSnapshot("344");
  if(alteredReturn)returned.setRouteProcessId(92L);
  when(tasks.own(1L,344L)).thenReturn(List.of(returned));stubClose(returned,344L,800L);
  var previous=review(700L).setLeaderType("PQC").setLeaderUserId(343L);when(reviews.selectById(700L)).thenReturn(previous);
  event.setRawPayload("{\"activeOrderId\":413,\"pqcTaskId\":55,\"supersededReviewId\":700,\"inspectionQuantity\":3}");
  var revision=MesProProcessPoolEventRevisionDO.builder().id(800L).eventId(176L).revisionStatus("EFFECTIVE").modifiedByUserId(344L).revisionSignatureUserId(344L).revisionSignatureId(902L).revisionSignatureSnapshot("{\"signatureId\":902,\"actorId\":344,\"signedAt\":\"2026-10-05T10:30:00\"}").afterPayload(event.getRawPayload()).changeReason("本人核对后改值").build();revision.setTenantId(1L);when(revisions.selectById(800L)).thenReturn(revision);
  if(alteredReturn){assertThrows(RuntimeException.class,()->service.completeReturnAndScheduleReview(176L,700L,800L,344L));assertEquals("TODO",returned.getStatus());verify(tasks,never()).close(any(),any(),any(),any(),any(),any(),any());verify(tasks,never()).insert(any(MesActiveOrderHandoffTaskDO.class));verifyNoInteractions(delivery);return;}
  service.completeReturnAndScheduleReview(176L,700L,800L,344L);var c=ArgumentCaptor.forClass(MesActiveOrderHandoffTaskDO.class);verify(tasks).insert(c.capture());
  assertEquals("PQC_REVIEW",c.getValue().getTaskType());assertEquals("343",c.getValue().getCandidateUserSnapshot());assertEquals("DONE",returned.getStatus());assertPqcStage(c.getValue(),800L);
  assertEquals("OWN_RETURN_CORRECTION",JsonUtils.parseTree(c.getValue().getResponsibilitySnapshotJson()).path("correctionOrigin").asText());verify(corrections).verifyRevision(event,previous,revision,344L);
 }
 @org.junit.jupiter.params.ParameterizedTest
 @org.junit.jupiter.params.provider.ValueSource(strings={"callerTask","orderCycle","orderWorkOrder","orderRoute","orderTenant","taskTenant","stageId","stageProcess","stageRoute","missingStage"})
 void pqcStageMismatchStopsBeforeHandoffWrites(String defect){
  var source=pqcSource();when(owners.originalActor(event)).thenReturn(344L);
  var routeMapper=(cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper)ReflectionTestUtils.getField(service,"routeProcesses");
  var rp=routeMapper.selectById(91L);Long callerTask=55L;
  switch(defect){
   case "callerTask" -> callerTask=56L;
   case "orderCycle" -> order.setId(414L);
   case "orderWorkOrder" -> order.setWorkOrderId(275L);
   case "orderRoute" -> order.setRouteId(99L);
   case "orderTenant" -> order.setTenantId(2L);
   case "taskTenant" -> source.setTenantId(2L);
   case "stageId" -> rp.setId(92L);
   case "stageProcess" -> source.setProcessId(16L);
   case "stageRoute" -> rp.setRouteId(99L);
   case "missingStage" -> when(routeMapper.selectById(91L)).thenReturn(null);
   default -> throw new IllegalArgumentException(defect);
  }
  Long actualCaller=callerTask;assertThrows(RuntimeException.class,()->service.pqcSubmitted(actualCaller,176L,344L));
  verify(tasks,never()).selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));verify(tasks,never()).close(any(),any(),any(),any(),any(),any(),any());verify(tasks,never()).insert(any(MesActiveOrderHandoffTaskDO.class));verifyNoInteractions(delivery);
 }
 MesPqcInspectionTaskDO pqcSource(){event.setEventType("PQC_INSPECTION").setRouteProcessId(null).setProcessId(null).setQaProcessId(985L).setActualEmployeeId(344L).setSignatureUserId(344L).setWorkOrderId(274L).setRouteId(98L).setFeedbackSourceType("MES_PQC_INSPECTION_TASK").setFeedbackSourceId(55L).setRawPayload("{\"activeOrderId\":413,\"pqcTaskId\":55}");var source=MesActiveOrderHandoffOwnerResolverTest.pqcTask();when(owners.pqcTask(event)).thenReturn(source);return source;}
 void stubClose(MesActiveOrderHandoffTaskDO task,Long actor,Long source){when(tasks.lock(task.getId())).thenReturn(task);when(tasks.selectById(task.getId())).thenReturn(task);when(tasks.close(eq(1L),eq(task.getId()),eq(0),eq("DONE"),eq(actor),eq(source),any())).thenAnswer(i->{task.setStatus("DONE").setRowVersion(1).setCompletedBy(actor).setCompletionSourceId(source);return 1;});}
 void assertPqcStage(MesActiveOrderHandoffTaskDO task,Long round){assertEquals(413L,task.getActiveOrderId());assertEquals(274L,task.getWorkOrderId());assertNull(task.getRouteProcessId());assertEquals(176L,task.getSourceId());assertEquals(round,task.getRoundId());var snapshot=JsonUtils.parseTree(task.getResponsibilitySnapshotJson());assertFalse(snapshot.hasNonNull("processId"));assertFalse(snapshot.hasNonNull("routeProcessId"));assertEquals("整单",snapshot.path("processName").asText());assertEquals(55L,snapshot.path("pqcTaskId").longValue());assertEquals(985L,snapshot.path("qaProcessId").longValue());assertEquals(17L,snapshot.path("routeVersionId").longValue());assertEquals(19L,snapshot.path("regulationVersionId").longValue());assertTrue(task.getReason().contains("工序：整单"));}

 static MesProcessPoolSubmissionReviewDO review(Long id){var r=MesProcessPoolSubmissionReviewDO.builder().id(id).eventId(176L).leaderUserId(341L).reviewSignatureUserId(341L).reviewSignatureId(905L).reviewStatus("REJECTED").reviewRemark("次数错误，请本人核对").reviewedAt(LocalDateTime.of(2026,10,5,10,0)).reviewRound(0).build();r.setTenantId(1L);return r;}

 @Test void archivedSourceReviewPreservesAllocationWithoutPqcReceptionNotification(){
  order.setActiveStatus("ARCHIVED");var approved=review(700L).setReviewStatus("APPROVED");
  when(reviews.selectById(700L)).thenReturn(approved);when(reviews.selectLatestByEventIdForUpdate(176L)).thenReturn(approved);
  var sourceTask=MesActiveOrderHandoffContractTest.review().setRouteProcessId(91L).setTaskType("PRODUCTION_REVIEW").setRoundId(176L).setCandidateUserSnapshot("341")
      .setStatus("DONE").setCompletionSourceId(700L);
  when(tasks.byIdentity(413L,"PRODUCTION_REVIEW","PROCESS_POOL_EVENT",176L,176L)).thenReturn(sourceTask);when(tasks.lock(sourceTask.getId())).thenReturn(sourceTask);
  var target=MesProcessPoolActiveOrderDO.builder().id(414L).workOrderId(275L).routeId(98L).leaderUserId(341L).activeStatus("ACTIVE").build();target.setTenantId(1L);
  when(orders.selectByIdForUpdate(414L)).thenReturn(target);
  var allocation=MesProcessPoolReportAllocationDO.builder().id(772L).eventId(176L).activeOrderId(414L).workOrderId(275L).routeProcessId(91L).processId(15L)
      .allocatedQuantity(new java.math.BigDecimal("120")).lifecycleStatus("CURRENT").build();
  var allocations=mock(MesProcessPoolReportAllocationMapper.class);ReflectionTestUtils.setField(service,"allocations",allocations);
  when(allocations.selectListByEventIdForUpdate(176L)).thenReturn(List.of(allocation));
  var pqc=(MesPqcInspectionTaskMapper)ReflectionTestUtils.getField(service,"pqcTasks");
  var inspection=new cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO().setId(56L).setActiveOrderId(414L)
      .setWorkOrderId(275L).setRouteId(98L).setRouteProcessId(91L).setProcessId(15L).setTaskStatus("PENDING");inspection.setTenantId(1L);
  when(pqc.selectListByActiveOrderId(414L)).thenReturn(List.of(inspection));
  var workOrders=(cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper)ReflectionTestUtils.getField(service,"workOrders");
  var wo=cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO.builder().id(275L).code("TARGET-B").build();wo.setTenantId(1L);when(workOrders.selectById(275L)).thenReturn(wo);
  service.allocationReviewed(176L,700L,List.of(allocation));
  verify(tasks,never()).insert(any(MesActiveOrderHandoffTaskDO.class));verifyNoInteractions(delivery);
  assertEquals("DONE",sourceTask.getStatus());assertEquals(700L,sourceTask.getCompletionSourceId());verify(pqc,never()).selectListByActiveOrderId(413L);
 }
 @Test void profileReturnHasActionableExactLeaderCorrectionThenIndependentReviewRound(){
  event.setWorkOrderId(274L);when(owners.submissionIdentity(event)).thenReturn(new MesActiveOrderHandoffOwnerResolver.SubmissionIdentity("MES_EMPLOYEE_PROFILE",342L,1L));
  when(owners.profileProductionLeader(event)).thenReturn(341L);
  var previous=review(700L).setLeaderType("PRODUCTION");when(reviews.selectById(700L)).thenReturn(previous);when(reviews.selectLatestByEventIdForUpdate(176L)).thenReturn(previous);
  var original=MesActiveOrderHandoffContractTest.review().setRouteProcessId(91L).setTaskType("PRODUCTION_REVIEW").setRoundId(176L).setCandidateUserSnapshot("341");
  when(tasks.byIdentity(413L,"PRODUCTION_REVIEW","PROCESS_POOL_EVENT",176L,176L)).thenReturn(original);when(tasks.lock(original.getId())).thenReturn(original);when(tasks.selectById(original.getId())).thenReturn(original);
  when(tasks.close(eq(1L),eq(original.getId()),eq(0),eq("DONE"),eq(341L),eq(700L),any())).thenAnswer(i->{original.setStatus("DONE").setCompletionSourceId(700L).setRowVersion(1);return 1;});
  service.reviewed(176L,700L);var captured=ArgumentCaptor.forClass(MesActiveOrderHandoffTaskDO.class);verify(tasks).insert(captured.capture());
  var returned=captured.getValue();assertEquals("341",returned.getCandidateUserSnapshot());assertFalse(returned.getNotificationOnly());assertTrue(returned.getResponsibilitySnapshotJson().contains("LEADER_PROFILE_RETURN"));
  when(tasks.selectById(returned.getId())).thenReturn(returned);when(tasks.byIdentity(413L,"PRODUCTION_REVIEW","PROCESS_POOL_EVENT_REJECTED_REVIEW",176L,700L)).thenReturn(returned);when(tasks.lock(returned.getId())).thenReturn(returned);
  assertTrue(service.navigationContext(returned.getId(),341L).profileLeaderCorrection());
  assertThrows(RuntimeException.class,()->service.navigationContext(returned.getId(),342L));
  var revision=MesProProcessPoolEventRevisionDO.builder().id(800L).eventId(176L).modifiedByUserId(341L).revisionSignatureUserId(341L).revisionSignatureId(902L).revisionStatus("EFFECTIVE").build();revision.setTenantId(1L);
  when(revisions.selectById(800L)).thenReturn(revision);when(revisions.selectListByEventId(176L)).thenReturn(List.of(revision));
  when(tasks.close(eq(1L),eq(returned.getId()),eq(0),eq("DONE"),eq(341L),eq(800L),any())).thenAnswer(i->{returned.setStatus("DONE").setRowVersion(1).setCompletionSourceId(800L);return 1;});
  service.completeProfileLeaderCorrection(176L,700L,800L,341L);verify(corrections).verifyProfileLeaderRevision(event,previous,revision,341L);
  var rounds=ArgumentCaptor.forClass(MesActiveOrderHandoffTaskDO.class);verify(tasks,times(2)).insert(rounds.capture());var pending=rounds.getAllValues().get(1);
  assertEquals(800L,pending.getRoundId());assertEquals("341",pending.getCandidateUserSnapshot());assertTrue(pending.getNotificationOnly());assertTrue(pending.getResponsibilitySnapshotJson().contains("LEADER_PROFILE_CORRECTION"));
  when(tasks.selectById(pending.getId())).thenReturn(pending);when(corrections.find(event,previous)).thenReturn(revision);
  var context=service.navigationContext(pending.getId(),341L);assertTrue(context.processable());assertFalse(context.profileLeaderCorrection());
  when(corrections.find(event,previous)).thenReturn(null);assertThrows(RuntimeException.class,()->service.navigationContext(pending.getId(),341L));
  assertEquals("REJECTED",previous.getReviewStatus());assertEquals(342L,event.getSignatureUserId());assertEquals(901L,event.getSignatureId());
 }

 @Test void equalPqcSignedRoundNavigatesOnlyAfterActualCorrectionResolverAcceptsLatestRevision(){
  pqcSource();var previous=review(176L).setLeaderType("PQC").setLeaderUserId(343L);
  when(reviews.selectLatestByEventIdForUpdate(176L)).thenReturn(previous);
  var task=MesActiveOrderHandoffContractTest.review().setTaskType("PQC_REVIEW")
      .setSourceType(MesActiveOrderHandoffContract.EVENT_SIGNED_REVISION).setSourceId(176L).setRoundId(176L).setCandidateUserSnapshot("343");
  task.setActionUrl(task.getActionUrl().replace("roundId=701","roundId=176"));
  when(tasks.selectById(task.getId())).thenReturn(task);
  var revision=new MesProProcessPoolEventRevisionDO().setId(176L).setEventId(176L);
  when(corrections.find(event,previous)).thenReturn(revision);
  var context=service.navigationContext(task.getId(),343L);assertTrue(context.processable());assertFalse(context.profileLeaderCorrection());
  verify(corrections).find(event,previous);assertNull(event.getProcessId());assertNull(event.getRouteProcessId());
  when(corrections.find(event,previous)).thenReturn(null);assertThrows(RuntimeException.class,()->service.navigationContext(task.getId(),343L));
  when(corrections.find(event,previous)).thenReturn(new MesProProcessPoolEventRevisionDO().setId(177L));
  assertThrows(RuntimeException.class,()->service.navigationContext(task.getId(),343L));
  assertEquals("REJECTED",previous.getReviewStatus());verify(tasks,never()).insert(any(MesActiveOrderHandoffTaskDO.class));
 }


 @Test void reviewerSnapshotSurvivesPersonnelChangesWithoutReceptionTask() {
   pqcSource();when(owners.originalActor(event)).thenReturn(344L);when(owners.pqcLeader(event)).thenReturn(343L);
   service.pqcSubmitted(55L,176L,344L);
   var capture=ArgumentCaptor.forClass(MesActiveOrderHandoffTaskDO.class);verify(tasks).insert(capture.capture());var review=capture.getValue();
   when(owners.pqcLeader(event)).thenThrow(new IllegalStateException("personnel changed"));
   when(tasks.byIdentity(413L,"PQC_REVIEW","PROCESS_POOL_EVENT",176L,176L)).thenReturn(review);
   assertDoesNotThrow(()->service.assertCanReviewPqcEvent(176L,null,343L));
   assertThrows(RuntimeException.class,()->service.assertCanReviewPqcEvent(176L,null,344L));
   when(tasks.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(review));
   when(events.selectBatchIds(anyCollection())).thenReturn(List.of(event));when(orders.selectBatchIds(anyCollection())).thenReturn(List.of(order));
   var batchPqc=mock(MesPqcInspectionTaskMapper.class);ReflectionTestUtils.setField(owners,"pqcTasks",batchPqc);
   when(batchPqc.selectBatchIds(anyCollection())).thenReturn(List.of(MesActiveOrderHandoffOwnerResolverTest.pqcTask()));
   when(owners.frozenPqcReviewEventIds(anyList(),anyList(),anyLong())).thenCallRealMethod();
   assertEquals(Set.of(176L),service.listOwnPqcReviewEventIds(343L));
   verify(owners,times(1)).pqcLeader(event);
 }
 @Test void missingInspectorPermissionPreventsReviewCreation() {
   pqcSource();when(owners.originalActor(event)).thenReturn(344L);
   doThrow(new IllegalStateException("PQC grant missing")).when(owners).qualifiedPqcSubmitter(344L);
   assertThrows(RuntimeException.class,()->service.pqcSubmitted(55L,176L,344L));
   verify(tasks,never()).insert(any(MesActiveOrderHandoffTaskDO.class));verifyNoInteractions(delivery);
 }
  @Test void exactSignedReviewRoundAndDoneReplayKeepFrozenResponsibility(){
   pqcSource();when(owners.originalActor(event)).thenReturn(344L);when(owners.pqcLeader(event)).thenReturn(343L);
   service.pqcSubmitted(55L,176L,344L);var capture=ArgumentCaptor.forClass(MesActiveOrderHandoffTaskDO.class);verify(tasks).insert(capture.capture());var review=capture.getValue();
   review.setSourceType(MesActiveOrderHandoffContract.EVENT_SIGNED_REVISION).setRoundId(800L).setStatus("DONE");
   var n=(com.fasterxml.jackson.databind.node.ObjectNode)JsonUtils.parseTree(review.getResponsibilitySnapshotJson());n.put("sourceType",MesActiveOrderHandoffContract.EVENT_SIGNED_REVISION).put("roundId",800L);review.setResponsibilitySnapshotJson(n.toString());
   review.setActionUrl(review.getActionUrl().replace("roundId=176","roundId=800"));
   var revision=new MesProProcessPoolEventRevisionDO().setId(800L).setEventId(176L).setRevisionStatus("EFFECTIVE");revision.setTenantId(1L);when(revisions.selectById(800L)).thenReturn(revision);
   when(tasks.byIdentity(413L,"PQC_REVIEW",MesActiveOrderHandoffContract.EVENT_SIGNED_REVISION,176L,800L)).thenReturn(review);
   assertDoesNotThrow(()->service.assertCanReviewPqcEvent(176L,800L,343L));assertThrows(RuntimeException.class,()->service.assertCanReviewPqcEvent(176L,null,343L));
   revision.setTenantId(2L);assertThrows(RuntimeException.class,()->service.assertCanReviewPqcEvent(176L,800L,343L));
  }

  @Test void equalEventReviewAndRevisionIdsPersistIndependentRoundsAndReplaySameRound() throws Exception {
  var ds=new org.springframework.jdbc.datasource.DriverManagerDataSource("jdbc:h2:mem:handoff_equal_"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1","sa","");
  var jdbc=new org.springframework.jdbc.core.JdbcTemplate(ds);
  jdbc.execute("CREATE TABLE mes_active_order_handoff_task(id BIGINT PRIMARY KEY,tenant_id BIGINT,active_order_id BIGINT,work_order_id BIGINT,route_process_id BIGINT,task_type VARCHAR(40),source_type VARCHAR(40),source_id BIGINT,round_id BIGINT,candidate_user_snapshot VARCHAR(100),responsibility_snapshot_json VARCHAR(5000),initiated_by BIGINT,notification_only BOOLEAN,status VARCHAR(20),action_url VARCHAR(1000),reason VARCHAR(2000),row_version INT,completed_by BIGINT,completion_source_id BIGINT,completed_at TIMESTAMP,creator VARCHAR(64),updater VARCHAR(64),create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,deleted BOOLEAN DEFAULT FALSE,UNIQUE(tenant_id,active_order_id,task_type,source_type,source_id,round_id))");
  var config=new com.baomidou.mybatisplus.core.MybatisConfiguration();config.setMapUnderscoreToCamelCase(true);config.addMapper(MesActiveOrderHandoffTaskMapper.class);
  var factory=new com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean();factory.setDataSource(ds);factory.setConfiguration(config);
  var session=new org.mybatis.spring.SqlSessionTemplate(factory.getObject());var realTasks=session.getMapper(MesActiveOrderHandoffTaskMapper.class);
  ReflectionTestUtils.setField(service,"tasks",realTasks);
  var realResolver=new MesSignedReturnCorrectionResolver();
  var evidence=mock(cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesCorrectionSignatureEvidenceReader.class);
  for(var entry:Map.of("tasks",realTasks,"revisions",revisions,"owners",owners,"correctionEvidence",evidence).entrySet())ReflectionTestUtils.setField(realResolver,entry.getKey(),entry.getValue());
  ReflectionTestUtils.setField(service,"correctionResolver",realResolver);
  event.setWorkOrderId(274L);when(owners.submissionIdentity(event)).thenReturn(new MesActiveOrderHandoffOwnerResolver.SubmissionIdentity("MES_EMPLOYEE_PROFILE",342L,1L));when(owners.profileProductionLeader(event)).thenReturn(341L);
  service.productionSubmitted(413L,176L,342L);service.productionSubmitted(413L,176L,342L);
  assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM mes_active_order_handoff_task",Integer.class));
  var rejected=review(176L).setLeaderType("PRODUCTION");when(reviews.selectById(176L)).thenReturn(rejected);when(reviews.selectLatestByEventIdForUpdate(176L)).thenReturn(rejected);
  service.reviewed(176L,176L);service.reviewed(176L,176L);
  var returned=realTasks.byIdentity(413L,"PRODUCTION_REVIEW",MesActiveOrderHandoffContract.EVENT_REJECTED_REVIEW,176L,176L);
  assertNotNull(returned);assertEquals("TODO",returned.getStatus());assertTrue(service.navigationContext(returned.getId(),341L).profileLeaderCorrection());
  assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM mes_active_order_handoff_task",Integer.class));
  var before=event.getRawPayload();event.setRawPayload("{\"activeOrderId\":413,\"supersededReviewId\":176,\"count\":3}");
  var revision=MesProProcessPoolEventRevisionDO.builder().id(176L).eventId(176L).revisionStatus("EFFECTIVE").modifiedByUserId(341L).revisionSignatureUserId(341L)
    .revisionSignatureId(902L).revisionSignatureSnapshot("{\"signatureId\":902,\"actorId\":341,\"signedAt\":\"2026-10-05T11:00:00\"}")
    .serverRevisionTime(LocalDateTime.of(2026,10,5,11,0)).beforePayload(before).afterPayload(event.getRawPayload()).build();revision.setTenantId(1L);
  when(revisions.selectById(176L)).thenReturn(revision);when(revisions.selectListByEventId(176L)).thenReturn(List.of(revision));
  service.completeProfileLeaderCorrection(176L,176L,176L,341L);service.completeProfileLeaderCorrection(176L,176L,176L,341L);
  var pending=realTasks.byIdentity(413L,"PRODUCTION_REVIEW",MesActiveOrderHandoffContract.EVENT_SIGNED_REVISION,176L,176L);
  assertNotNull(pending);assertNotEquals(returned.getId(),pending.getId());assertEquals("TODO",pending.getStatus());assertEquals("REJECTED",rejected.getReviewStatus());
  assertTrue(pending.getResponsibilitySnapshotJson().contains("LEADER_PROFILE_CORRECTION"));assertFalse(service.navigationContext(pending.getId(),341L).profileLeaderCorrection());assertTrue(service.navigationContext(pending.getId(),341L).processable());
  assertEquals(3,jdbc.queryForObject("SELECT COUNT(*) FROM mes_active_order_handoff_task",Integer.class));assertSame(revision,realResolver.find(event,rejected));verify(evidence,atLeastOnce()).require(event,revision);
  assertEquals(342L,event.getSignatureUserId());assertEquals(901L,event.getSignatureId());
  var approved=review(177L).setReviewStatus("APPROVED").setSourceRevisionId(176L).setLeaderType("PRODUCTION");
  when(reviews.selectById(177L)).thenReturn(approved);when(reviews.selectLatestByEventIdForUpdate(176L)).thenReturn(approved);
  service.reviewed(176L,177L);assertEquals("DONE",realTasks.selectById(pending.getId()).getStatus());assertEquals(177L,realTasks.selectById(pending.getId()).getCompletionSourceId());
  assertEquals("DONE",realTasks.byIdentity(413L,"PRODUCTION_REVIEW","PROCESS_POOL_EVENT",176L,176L).getStatus());
  assertEquals(176L,realTasks.selectById(returned.getId()).getCompletionSourceId());
 }

 @org.junit.jupiter.params.ParameterizedTest
 @org.junit.jupiter.params.provider.ValueSource(strings={"valid","wrongOldCompletion","wrongOldActor","previousRejected","roundGap","foreignApproval"})
 void approvedProductionCanStartANewRejectedRoundWithoutRewritingOldCompletion(String defect){
  when(owners.submissionIdentity(event)).thenReturn(new MesActiveOrderHandoffOwnerResolver.SubmissionIdentity("SYSTEM_USER",342L,1L));
  var approved=review(7000L).setReviewStatus("APPROVED").setReviewRound(0).setLeaderType("PRODUCTION");
  var rejected=review(7001L).setReviewStatus("REJECTED").setReviewRound(1).setLeaderType("PRODUCTION").setSupersededReviewId(7000L);
  var original=new MesActiveOrderHandoffTaskDO().setId(5L).setStatus("DONE").setCompletedBy(341L).setCompletionSourceId(7000L);original.setTenantId(1L);
  switch(defect){case "wrongOldCompletion"->original.setCompletionSourceId(6999L);case "wrongOldActor"->original.setCompletedBy(999L);case "previousRejected"->approved.setReviewStatus("REJECTED");case "roundGap"->rejected.setReviewRound(2);case "foreignApproval"->approved.setTenantId(2L);case "valid"->{}default->throw new IllegalArgumentException(defect);}
  when(reviews.selectById(7000L)).thenReturn(approved);when(reviews.selectById(7001L)).thenReturn(rejected);when(reviews.selectLatestByEventIdForUpdate(176L)).thenReturn(rejected);
  when(tasks.byIdentity(413L,"PRODUCTION_REVIEW","PROCESS_POOL_EVENT",176L,176L)).thenReturn(original);when(tasks.lock(5L)).thenReturn(original);
  if("valid".equals(defect)){
   service.reviewed(176L,7001L);assertEquals(7000L,original.getCompletionSourceId());
   var captured=ArgumentCaptor.forClass(MesActiveOrderHandoffTaskDO.class);verify(tasks).insert(captured.capture());
   assertEquals("PRODUCTION_RETURN",captured.getValue().getTaskType());assertEquals(7001L,captured.getValue().getRoundId());
   verify(tasks,never()).close(any(),eq(5L),any(),any(),any(),any(),any());
  }else assertThrows(RuntimeException.class,()->service.reviewed(176L,7001L));
 }

  @Test void pqcReviewListQueriesLiveSourcesWithoutStaleResetTasksAndRetainsValidHistory() throws Exception {
   var fixture=new cn.iocoder.yudao.module.mes.service.pro.MesSa36Sa38MapperFixture();
   fixture.table(MesActiveOrderHandoffTaskDO.class,"mes_active_order_handoff_task");fixture.table(MesProProcessPoolEventDO.class,"mes_pro_process_pool_event");fixture.table(MesProcessPoolActiveOrderDO.class,"mes_pro_process_pool_active_order");
   fixture.register(MesActiveOrderHandoffTaskMapper.class);var realTasks=fixture.mapper(MesActiveOrderHandoffTaskMapper.class);ReflectionTestUtils.setField(service,"tasks",realTasks);
   for(long id=413;id<=417;id++)fixture.jdbc.update("INSERT INTO mes_pro_process_pool_active_order(id,tenant_id,active_status,deleted) VALUES(?,?,?,?)",id,id==416?2:1,id==414?"ARCHIVED":"ACTIVE",id==417);
   for(long id=176;id<=185;id++)if(id!=178)fixture.jdbc.update("INSERT INTO mes_pro_process_pool_event(id,tenant_id,deleted) VALUES(?,?,?)",id,id==180?2:1,id==179);
   var current=listReviewTask(1L,176L,413L,"TODO");var history=listReviewTask(2L,177L,414L,"DONE");
   for(var task:List.of(current,history,listReviewTask(3L,178L,413L,"DONE"),listReviewTask(4L,179L,413L,"TODO"),listReviewTask(5L,180L,413L,"TODO"),listReviewTask(6L,181L,999L,"TODO"),listReviewTask(7L,182L,416L,"DONE"),listReviewTask(8L,183L,417L,"DONE"),listReviewTask(9L,184L,414L,"TODO"),listReviewTask(10L,185L,413L,"CANCELED"),listReviewTask(11L,176L,413L,"TODO").setCandidateUserSnapshot("3343"))){assertEquals(1,realTasks.insert(task));}
   var currentEvent=MesProProcessPoolEventDO.builder().id(176L).build();currentEvent.setTenantId(1L);var historyEvent=MesProProcessPoolEventDO.builder().id(177L).build();historyEvent.setTenantId(1L);
   when(events.selectBatchIds(Set.of(176L,177L))).thenReturn(List.of(currentEvent,historyEvent));
   when(owners.frozenPqcReviewEventIds(anyList(),eq(List.of(currentEvent,historyEvent)),eq(343L))).thenAnswer(i->{
    List<MesActiveOrderHandoffTaskDO> own=i.getArgument(0);assertEquals(List.of(1L,2L),own.stream().map(MesActiveOrderHandoffTaskDO::getId).toList());
    assertEquals(List.of(176L,177L),own.stream().map(MesActiveOrderHandoffTaskDO::getSourceId).toList());return Set.of(176L,177L);
   });
   assertEquals(Set.of(176L,177L),service.listOwnPqcReviewEventIds(343L));
   verify(events).selectBatchIds(Set.of(176L,177L));verifyNoMoreInteractions(events);
   verify(owners).frozenPqcReviewEventIds(anyList(),eq(List.of(currentEvent,historyEvent)),eq(343L));assertEquals(11,fixture.jdbc.queryForObject("SELECT COUNT(*) FROM mes_active_order_handoff_task",Integer.class));
   // A live row with corrupt frozen identity is still rejected, rather than removed by the source filter.
   current.setActionUrl(current.getActionUrl().replace("roundId=176","roundId=999"));assertEquals(1,realTasks.updateById(current));
   assertThrows(RuntimeException.class,()->service.listOwnPqcReviewEventIds(343L));
  }
  MesActiveOrderHandoffTaskDO listReviewTask(Long id,Long sourceId,Long activeId,String status){
   var task=new MesActiveOrderHandoffTaskDO().setId(id).setActiveOrderId(activeId).setWorkOrderId(274L).setRouteProcessId(null).setTaskType("PQC_REVIEW").setSourceType("PROCESS_POOL_EVENT").setSourceId(sourceId).setRoundId(sourceId).setCandidateUserSnapshot("343").setStatus(status).setRowVersion(0);
   task.setTenantId(1L);task.setActionUrl("/mes/pro/process-pool/pqc-leader?activeOrderId="+activeId+"&eventId="+sourceId+"&handoffTaskId="+id+"&roundId="+sourceId+"&handoffType=PQC_REVIEW");return task;
  }

}
