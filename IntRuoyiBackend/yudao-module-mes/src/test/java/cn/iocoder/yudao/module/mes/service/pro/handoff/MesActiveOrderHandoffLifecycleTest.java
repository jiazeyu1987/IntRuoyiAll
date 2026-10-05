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
 MesActiveOrderHandoffService service;MesActiveOrderHandoffTaskMapper tasks;MesProcessPoolActiveOrderMapper orders;MesProProcessPoolEventMapper events;MesProcessPoolSubmissionReviewMapper reviews;MesActiveOrderHandoffOwnerResolver owners;MesQaHandoffAssignmentService assignment;MesActiveOrderHandoffDeliveryService delivery;MesProProcessPoolEventRevisionMapper revisions;MesSignedReturnCorrectionResolver corrections;
 MesProcessPoolActiveOrderDO order;MesProProcessPoolEventDO event;
 MesPqcHandoffAssignmentService pqcAssignment;
 @BeforeEach void fixture(){TenantContextHolder.setTenantId(1L);service=new MesActiveOrderHandoffService();tasks=mock(MesActiveOrderHandoffTaskMapper.class);orders=mock(MesProcessPoolActiveOrderMapper.class);events=mock(MesProProcessPoolEventMapper.class);reviews=mock(MesProcessPoolSubmissionReviewMapper.class);owners=mock(MesActiveOrderHandoffOwnerResolver.class);assignment=mock(MesQaHandoffAssignmentService.class);delivery=mock(MesActiveOrderHandoffDeliveryService.class);revisions=mock(MesProProcessPoolEventRevisionMapper.class);corrections=mock(MesSignedReturnCorrectionResolver.class);
 for(var entry:Map.of("tasks",tasks,"orders",orders,"events",events,"reviews",reviews,"owners",owners,"qaAssignment",assignment,"delivery",delivery,"revisions",revisions,"correctionResolver",corrections).entrySet())ReflectionTestUtils.setField(service,entry.getKey(),entry.getValue());ReflectionTestUtils.setField(service,"audit",mock(MesActiveOrderHandoffAudit.class));ReflectionTestUtils.setField(service,"pqcTasks",mock(MesPqcInspectionTaskMapper.class));
 pqcAssignment=mock(MesPqcHandoffAssignmentService.class);ReflectionTestUtils.setField(service,"pqcAssignment",pqcAssignment);
 var workOrders=mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper.class);var routeProcesses=mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper.class);var processes=mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper.class);
 ReflectionTestUtils.setField(service,"workOrders",workOrders);ReflectionTestUtils.setField(service,"routeProcesses",routeProcesses);ReflectionTestUtils.setField(service,"processes",processes);
 var wo=cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO.builder().id(274L).code("EDHR-413").build();wo.setTenantId(1L);when(workOrders.selectById(274L)).thenReturn(wo);
 var rp=cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO.builder().id(91L).routeId(98L).processId(15L).build();when(routeProcesses.selectById(91L)).thenReturn(rp);
 when(processes.selectById(15L)).thenReturn(cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO.builder().id(15L).name("组装").build());
 when(owners.initiatorSnapshot(anyLong(),any())).thenAnswer(i->Map.<String,Object>of("domain","SYSTEM_USER","id",i.<Long>getArgument(0),"name","实际发起人"));
 order=MesProcessPoolActiveOrderDO.builder().id(413L).workOrderId(274L).routeId(98L).leaderUserId(341L).activeStatus("ACTIVE").build();order.setTenantId(1L);when(orders.selectByIdForUpdate(413L)).thenReturn(order);when(orders.selectById(413L)).thenReturn(order);
 event=MesProProcessPoolEventDO.builder().id(176L).eventType("PRODUCTION_SUBMIT").actualEmployeeId(342L).signatureUserId(342L).signatureId(901L).deviceAccountId(1L).routeProcessId(91L).rawPayload("{\"activeOrderId\":413}").build();event.setTenantId(1L);when(events.selectById(176L)).thenReturn(event);
 when(tasks.insert(any(MesActiveOrderHandoffTaskDO.class))).thenReturn(1);when(owners.productionLeader(order)).thenReturn(341L);
 }
 @AfterEach void clear(){TenantContextHolder.clear();}
 @Test void profileSubmissionContinuesToRealLeaderWithoutImpersonatedPersonalTask(){when(owners.submissionIdentity(event)).thenReturn(new MesActiveOrderHandoffOwnerResolver.SubmissionIdentity("MES_EMPLOYEE_PROFILE",342L,1L));when(owners.initiatorSnapshot(1L,event)).thenReturn(Map.of("domain","MES_EMPLOYEE_PROFILE","id",342L,"operatorId",1L,"name","正式档案提交人"));service.productionSubmitted(413L,176L,342L);var c=ArgumentCaptor.forClass(MesActiveOrderHandoffTaskDO.class);verify(tasks).insert(c.capture());assertEquals("PRODUCTION_REVIEW",c.getValue().getTaskType());assertEquals("341",c.getValue().getCandidateUserSnapshot());assertEquals(1L,c.getValue().getInitiatedBy());assertTrue(c.getValue().getReason().contains("工单：EDHR-413；工序：组装；发起人：正式档案提交人（MES_EMPLOYEE_PROFILE）"));assertTrue(c.getValue().getResponsibilitySnapshotJson().contains("handoffBusinessState"));verify(tasks,never()).close(any(),any(),any(),any(),any(),any(),any());verify(delivery).schedule(c.getValue());}
 @Test void wrongOriginalActorStopsSubmissionBeforeTaskCreation(){when(owners.submissionIdentity(event)).thenReturn(new MesActiveOrderHandoffOwnerResolver.SubmissionIdentity("SYSTEM_USER",342L,1L));assertThrows(RuntimeException.class,()->service.productionSubmitted(413L,176L,344L));verify(tasks,never()).insert(any(MesActiveOrderHandoffTaskDO.class));}
 @Test void repeatedOldReviewCannotCloseCorrectionNewRound(){var old=review(700L);when(reviews.selectById(700L)).thenReturn(old);when(reviews.selectLatestByEventIdForUpdate(176L)).thenReturn(review(701L));assertThrows(RuntimeException.class,()->service.reviewed(176L,700L));verify(tasks,never()).byIdentity(any(),any(),any(),any(),any());verify(tasks,never()).close(any(),any(),any(),any(),any(),any(),any());}
 @Test void frozenQaCandidateRemainsExclusiveWithoutReresolvingChangedRule(){var task=MesActiveOrderHandoffContractTest.review().setTaskType("QA_REVIEW").setCandidateUserSnapshot("345");when(tasks.byIdentity(413L,"QA_REVIEW","NONCONFORMANCE_REVIEW",91L,91L)).thenReturn(task);assertDoesNotThrow(()->service.assertQaCanDispose(91L,413L,345L));assertThrows(RuntimeException.class,()->service.assertQaCanDispose(91L,413L,346L));verifyNoInteractions(assignment);}
 @Test void oldCycleQaCandidateCannotDispose(){order.setActiveStatus("VOID");var task=MesActiveOrderHandoffContractTest.review().setCandidateUserSnapshot("345");when(tasks.byIdentity(413L,"QA_REVIEW","NONCONFORMANCE_REVIEW",91L,91L)).thenReturn(task);assertThrows(RuntimeException.class,()->service.assertQaCanDispose(91L,413L,345L));}
 @Test void voidResultRemainsReadOnlyAfterOrderDeletion(){var t=MesActiveOrderHandoffContractTest.review().setTaskType("QA_DECISION_HANDOFF").setSourceType("NONCONFORMANCE_REVIEW").setSourceId(91L).setRoundId(91L).setCandidateUserSnapshot("341").setStatus("DONE").setReason("void：正式作废").setActionUrl("/user/profile?activeOrderId=413&reviewId=91&handoffTaskId=1900000000000000001&roundId=91&handoffType=QA_DECISION_HANDOFF&tab=notifyMessage");when(tasks.selectById(t.getId())).thenReturn(t);when(orders.selectById(413L)).thenReturn(null);var context=service.navigationContext(t.getId(),341L);assertFalse(context.current());assertFalse(context.processable());assertEquals(91L,context.task().getSourceId());}
 @Test void canceledNotificationNeverResolvesToLatestOrder(){var t=MesActiveOrderHandoffContractTest.review().setStatus("CANCELED");when(tasks.selectById(t.getId())).thenReturn(t);assertThrows(RuntimeException.class,()->service.navigationContext(t.getId(),343L));verifyNoInteractions(events);}
 @Test void completedBusinessStatusStillAllowsFormalLeaderContinuation(){order.setBusinessStatus("COMPLETED");when(tasks.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of());assertDoesNotThrow(()->service.productionLeaderContinued(413L,341L,227L));assertThrows(RuntimeException.class,()->service.productionLeaderContinued(413L,342L,227L));}
 @Test void correctionCompletesOriginalReturnAndCreatesExactRevisionReview(){var t=MesActiveOrderHandoffContractTest.review().setTaskType("PRODUCTION_RETURN").setRoundId(700L).setCandidateUserSnapshot("342");when(tasks.own(1L,342L)).thenReturn(List.of(t));when(tasks.lock(t.getId())).thenReturn(t);when(tasks.selectById(t.getId())).thenReturn(t);when(tasks.close(eq(1L),eq(t.getId()),eq(0),eq("DONE"),eq(342L),eq(800L),any())).thenAnswer(i->{t.setStatus("DONE").setRowVersion(1).setCompletionSourceId(800L);return 1;});
 var previous=review(700L);when(reviews.selectById(700L)).thenReturn(previous);event.setRawPayload("{\"activeOrderId\":413,\"supersededReviewId\":700,\"count\":3}");var revision=MesProProcessPoolEventRevisionDO.builder().id(800L).eventId(176L).revisionStatus("EFFECTIVE").modifiedByUserId(342L).revisionSignatureUserId(342L).revisionSignatureId(902L).revisionSignatureSnapshot("{\"signatureId\":902,\"actorId\":342,\"signedAt\":\"2026-10-05T10:30:00\"}").afterPayload(event.getRawPayload()).changeReason("本人核对后改值").build();revision.setTenantId(1L);when(revisions.selectById(800L)).thenReturn(revision);
 service.completeReturnAndScheduleReview(176L,700L,800L,342L);assertEquals("DONE",t.getStatus());var c=ArgumentCaptor.forClass(MesActiveOrderHandoffTaskDO.class);verify(tasks).insert(c.capture());assertEquals(176L,c.getValue().getSourceId());assertEquals(800L,c.getValue().getRoundId());assertEquals("341",c.getValue().getCandidateUserSnapshot());verify(corrections).verifyRevision(event,previous,revision,342L);assertEquals("REJECTED",previous.getReviewStatus());}
 @Test void formalLeaderPqcCorrectionClosesOriginalReturnWithActualLeaderAndFreezesExplicitSource(){pqcSource();var previous=review(700L).setLeaderType("PQC").setLeaderUserId(343L);when(reviews.selectById(700L)).thenReturn(previous);when(reviews.selectLatestByEventIdForUpdate(176L)).thenReturn(previous);var revision=new MesProProcessPoolEventRevisionDO().setId(800L).setEventId(176L).setChangeReason("原责任组长正式更正");when(revisions.selectById(800L)).thenReturn(revision);when(revisions.selectListByEventId(176L)).thenReturn(List.of(revision));when(owners.originalActor(event)).thenReturn(344L);var returned=MesActiveOrderHandoffContractTest.review().setTaskType("PQC_RETURN").setRoundId(700L).setCandidateUserSnapshot("344");when(tasks.byIdentity(413L,"PQC_RETURN","PROCESS_POOL_EVENT",176L,700L)).thenReturn(returned);when(tasks.lock(returned.getId())).thenReturn(returned);when(tasks.selectById(returned.getId())).thenReturn(returned);when(tasks.close(eq(1L),eq(returned.getId()),eq(0),eq("DONE"),eq(343L),eq(800L),any())).thenAnswer(i->{returned.setStatus("DONE").setRowVersion(1).setCompletedBy(343L).setCompletionSourceId(800L);return 1;});service.completeLeaderPqcCorrectionAndScheduleReview(176L,700L,800L,343L);assertEquals(343L,returned.getCompletedBy());var captured=ArgumentCaptor.forClass(MesActiveOrderHandoffTaskDO.class);verify(tasks).insert(captured.capture());assertEquals("PQC_REVIEW",captured.getValue().getTaskType());assertEquals("343",captured.getValue().getCandidateUserSnapshot());assertEquals(343L,captured.getValue().getInitiatedBy());assertTrue(captured.getValue().getResponsibilitySnapshotJson().contains("LEADER_PQC_CORRECTION"));assertPqcStage(captured.getValue(),800L);verify(corrections).verifyLeaderPqcRevision(event,previous,revision,343L);}
 @Test void submittedPqcReviewUsesFormalTaskStageWithoutMutatingQaEvent(){
  pqcSource();when(owners.originalActor(event)).thenReturn(344L);when(owners.pqcLeader(event)).thenReturn(343L);
  when(tasks.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of());
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
 @Test void productionApprovalFreezesFormalEmployeeAndSeparateLeaderHandlerForExactPqcTask(){
  var approved=review(700L).setReviewStatus("APPROVED");when(reviews.selectById(700L)).thenReturn(approved);when(reviews.selectLatestByEventIdForUpdate(176L)).thenReturn(approved);
  var reviewTask=MesActiveOrderHandoffContractTest.review().setTaskType("PRODUCTION_REVIEW").setRoundId(176L).setCandidateUserSnapshot("341");when(tasks.byIdentity(413L,"PRODUCTION_REVIEW","PROCESS_POOL_EVENT",176L,176L)).thenReturn(reviewTask);stubClose(reviewTask,341L,700L);
  when(tasks.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of());
  var pqcMapper=(MesPqcInspectionTaskMapper)ReflectionTestUtils.getField(service,"pqcTasks");var pqc=MesActiveOrderHandoffOwnerResolverTest.pqcTask().setTaskStatus("PENDING").setSubmittedEventId(null);
  when(pqcMapper.selectListByActiveOrderId(413L)).thenReturn(List.of(pqc));when(pqcAssignment.resolve(98L)).thenReturn(new MesPqcHandoffAssignmentService.ResolvedAssignment(901L,98L,"USER",344L,"344",List.of(343L)));
  service.reviewed(176L,700L);var captured=ArgumentCaptor.forClass(MesActiveOrderHandoffTaskDO.class);verify(tasks).insert(captured.capture());var handoff=captured.getValue();
  assertEquals("PQC_HANDOFF",handoff.getTaskType());assertEquals("344",handoff.getCandidateUserSnapshot());assertEquals(55L,handoff.getSourceId());assertEquals(55L,handoff.getRoundId());assertEquals(91L,handoff.getRouteProcessId());
  var snapshot=JsonUtils.parseTree(handoff.getResponsibilitySnapshotJson());assertEquals(343L,snapshot.path("handlerLeaderUserIds").get(0).longValue());assertEquals(98L,snapshot.path("routeId").longValue());assertFalse(handoff.getCandidateUserSnapshot().contains("343"));verify(delivery).schedule(handoff);
  var rule=snapshot.path("pqcAssignmentRule");assertEquals(901L,rule.path("ruleId").longValue());assertEquals("ROUTE",rule.path("scopeType").asText());assertEquals(98L,rule.path("scopeId").longValue());assertEquals("PQC_HANDOFF",rule.path("taskType").asText());assertEquals("USER",rule.path("candidateSourceType").asText());assertEquals(344L,rule.path("candidateSourceId").longValue());assertEquals("344",rule.path("candidateUserSnapshot").asText());verify(pqcAssignment).resolve(98L);
 }
 @Test void productionApprovalWithMissingExactRouteRulePropagatesAndCreatesNoPqcHandoff(){
  var approved=review(700L).setReviewStatus("APPROVED");when(reviews.selectById(700L)).thenReturn(approved);when(reviews.selectLatestByEventIdForUpdate(176L)).thenReturn(approved);
  var reviewTask=MesActiveOrderHandoffContractTest.review().setTaskType("PRODUCTION_REVIEW").setRoundId(176L).setCandidateUserSnapshot("341");when(tasks.byIdentity(413L,"PRODUCTION_REVIEW","PROCESS_POOL_EVENT",176L,176L)).thenReturn(reviewTask);stubClose(reviewTask,341L,700L);
  when(tasks.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of());var pqcMapper=(MesPqcInspectionTaskMapper)ReflectionTestUtils.getField(service,"pqcTasks");when(pqcMapper.selectListByActiveOrderId(413L)).thenReturn(List.of(MesActiveOrderHandoffOwnerResolverTest.pqcTask().setTaskStatus("PENDING").setSubmittedEventId(null)));
  when(pqcAssignment.resolve(98L)).thenThrow(new IllegalStateException("当前路线未配置PQC检验接手负责人"));var failure=assertThrows(IllegalStateException.class,()->service.reviewed(176L,700L));assertTrue(failure.getMessage().contains("未配置"));verify(tasks,never()).insert(any(MesActiveOrderHandoffTaskDO.class));verifyNoInteractions(delivery);
 }
 @org.junit.jupiter.params.ParameterizedTest
 @org.junit.jupiter.params.provider.ValueSource(booleans={false,true})
 void successfulEmployeeOrFrozenLeaderSelfSubmissionClosesOnlyItsExactTask(boolean self){
  pqcSource();Long actor=self?343L:344L;event.setActualEmployeeId(actor).setSignatureUserId(actor);when(owners.originalActor(event)).thenReturn(actor);when(owners.pqcLeader(event)).thenReturn(343L);
  var handoff=pqcHandoff();when(tasks.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(handoff));stubClose(handoff,actor,176L);
  service.pqcSubmitted(55L,176L,actor);assertEquals("DONE",handoff.getStatus());assertEquals(actor,handoff.getCompletedBy());assertEquals(176L,handoff.getCompletionSourceId());assertEquals("344",handoff.getCandidateUserSnapshot());
  var c=ArgumentCaptor.forClass(MesActiveOrderHandoffTaskDO.class);verify(tasks).insert(c.capture());assertEquals("PQC_REVIEW",c.getValue().getTaskType());assertEquals(actor,c.getValue().getInitiatedBy());assertPqcStage(c.getValue(),176L);verify(tasks,never()).own(any(),any());
 }
 @org.junit.jupiter.params.ParameterizedTest
 @org.junit.jupiter.params.provider.ValueSource(strings={"unfrozenLeader","changedCurrentLeader","otherTask","otherRound","otherCycle","otherWorkOrder","otherTenant","otherStage","snapshotCycle","snapshotWorkOrder","snapshotRoute","snapshotSource","snapshotRound","snapshotStage","snapshotProcess","missingSignature","wrongSignatureActor","confirmedBeforeSubmit"})
 void selfSubmissionWithAnyIncorrectFrozenSourceOrActorMakesNoHandoffWrite(String defect){
  var pqc=pqcSource();event.setActualEmployeeId(343L).setSignatureUserId(343L);when(owners.originalActor(event)).thenReturn(343L);when(owners.pqcLeader(event)).thenReturn(343L);
  var handoff=pqcHandoff();var snapshot=(com.fasterxml.jackson.databind.node.ObjectNode)JsonUtils.parseTree(handoff.getResponsibilitySnapshotJson());
  switch(defect){
   case "unfrozenLeader" -> snapshot.putArray("handlerLeaderUserIds").add(349L);
   case "changedCurrentLeader" -> when(owners.pqcLeader(event)).thenReturn(349L);
   case "otherTask" -> handoff.setSourceId(56L);
   case "otherRound" -> handoff.setRoundId(56L);
   case "otherCycle" -> handoff.setActiveOrderId(414L);
   case "otherWorkOrder" -> handoff.setWorkOrderId(275L);
   case "otherTenant" -> handoff.setTenantId(2L);
   case "otherStage" -> handoff.setRouteProcessId(92L);
   case "snapshotCycle" -> snapshot.put("activeOrderId",414L);
   case "snapshotWorkOrder" -> snapshot.put("workOrderId",275L);
   case "snapshotRoute" -> snapshot.put("routeId",99L);
   case "snapshotSource" -> snapshot.put("sourceId",56L);
   case "snapshotRound" -> snapshot.put("roundId",56L);
   case "snapshotStage" -> snapshot.put("routeProcessId",92L);
   case "snapshotProcess" -> snapshot.put("processId",16L);
   case "missingSignature" -> when(owners.originalActor(event)).thenThrow(new IllegalStateException("原提交签名缺失"));
   case "wrongSignatureActor" -> when(owners.originalActor(event)).thenReturn(344L);
   case "confirmedBeforeSubmit" -> pqc.setTaskStatus("CONFIRMED");
   default -> throw new IllegalArgumentException(defect);
  }
  handoff.setResponsibilitySnapshotJson(snapshot.toString());when(tasks.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(handoff));
  assertThrows(RuntimeException.class,()->service.pqcSubmitted(55L,176L,343L));assertEquals("TODO",handoff.getStatus());verify(tasks,never()).close(any(),any(),any(),any(),any(),any(),any());verify(tasks,never()).insert(any(MesActiveOrderHandoffTaskDO.class));verifyNoInteractions(delivery);
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
 void assertPqcStage(MesActiveOrderHandoffTaskDO task,Long round){assertEquals(413L,task.getActiveOrderId());assertEquals(274L,task.getWorkOrderId());assertEquals(91L,task.getRouteProcessId());assertEquals(176L,task.getSourceId());assertEquals(round,task.getRoundId());var snapshot=JsonUtils.parseTree(task.getResponsibilitySnapshotJson());assertEquals(15L,snapshot.path("processId").longValue());assertEquals(91L,snapshot.path("routeProcessId").longValue());assertEquals("组装",snapshot.path("processName").asText());assertEquals(55L,snapshot.path("pqcTaskId").longValue());assertEquals(985L,snapshot.path("qaProcessId").longValue());assertEquals(17L,snapshot.path("routeVersionId").longValue());assertEquals(19L,snapshot.path("regulationVersionId").longValue());assertFalse(task.getReason().contains("工序：整单"));}
 MesActiveOrderHandoffTaskDO pqcHandoff(){var t=MesActiveOrderHandoffContractTest.review().setTaskType("PQC_HANDOFF").setSourceType("PQC_INSPECTION_TASK").setSourceId(55L).setRoundId(55L).setCandidateUserSnapshot("344").setNotificationOnly(true);var snapshot=new LinkedHashMap<String,Object>();snapshot.put("identityDomain","SYSTEM_USER");snapshot.put("activeOrderId",413L);snapshot.put("workOrderId",274L);snapshot.put("routeId",98L);snapshot.put("sourceType","PQC_INSPECTION_TASK");snapshot.put("sourceId",55L);snapshot.put("roundId",55L);snapshot.put("routeProcessId",91L);snapshot.put("processId",15L);snapshot.put("handlerLeaderUserIds",List.of(343L));t.setResponsibilitySnapshotJson(JsonUtils.toJsonString(snapshot));return t;}
 static MesProcessPoolSubmissionReviewDO review(Long id){var r=MesProcessPoolSubmissionReviewDO.builder().id(id).eventId(176L).leaderUserId(341L).reviewSignatureUserId(341L).reviewSignatureId(905L).reviewStatus("REJECTED").reviewRemark("次数错误，请本人核对").reviewedAt(LocalDateTime.of(2026,10,5,10,0)).reviewRound(0).build();r.setTenantId(1L);return r;}
}
