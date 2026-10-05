package cn.iocoder.yudao.module.mes.service.pro.handoff;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamLeaderScopeMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProductionSignatureEvidenceService;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureEvidenceDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class MesActiveOrderHandoffOwnerResolverTest {
 MesActiveOrderHandoffOwnerResolver service;AdminUserMapper users;PermissionApi permissions;MesProcessPoolTeamLeaderScopeMapper scopes;MesProductionSignatureEvidenceService evidence;ElectronicSignatureQueryService signatures;MesPqcInspectionTaskMapper pqcTasks;
 @BeforeEach void setup(){TenantContextHolder.setTenantId(1L);service=new MesActiveOrderHandoffOwnerResolver();users=mock(AdminUserMapper.class);permissions=mock(PermissionApi.class);scopes=mock(MesProcessPoolTeamLeaderScopeMapper.class);evidence=mock(MesProductionSignatureEvidenceService.class);signatures=mock(ElectronicSignatureQueryService.class);pqcTasks=mock(MesPqcInspectionTaskMapper.class);for(var e:Map.of("users",users,"permissions",permissions,"scopes",scopes,"productionEvidence",evidence,"signatureQuery",signatures,"pqcTasks",pqcTasks).entrySet())ReflectionTestUtils.setField(service,e.getKey(),e.getValue());}
 @AfterEach void clear(){TenantContextHolder.clear();}
 @Test void enabledSystemAccountStillNeedsActualHandlingPermission(){var u=AdminUserDO.builder().id(341L).status(0).build();u.setTenantId(1L);when(users.selectById(341L)).thenReturn(u);when(permissions.getUserRoleIdListByUserId(341L)).thenReturn(Set.of(501L));assertThrows(RuntimeException.class,()->service.user(341L,"mes:pro-process-pool-team-leader:review"));when(permissions.hasAnyPermissionsInRoles(Set.of(501L),"mes:pro-process-pool-team-leader:review")).thenReturn(true);assertDoesNotThrow(()->service.user(341L,"mes:pro-process-pool-team-leader:review"));u.setStatus(1);assertThrows(RuntimeException.class,()->service.user(341L,"mes:pro-process-pool-team-leader:review"));u.setStatus(0);u.setTenantId(2L);assertThrows(RuntimeException.class,()->service.user(341L,"mes:pro-process-pool-team-leader:review"));}
 @Test void employeeBindingSuppliesReviewOwnerWithoutInventedProcessScope(){var e=pqcEvent();when(pqcTasks.selectById(55L)).thenReturn(pqcTask());when(scopes.selectActiveScopesByLeaderType("PQC")).thenReturn(List.of(scope(343L,"EMPLOYEE",null)));allowUser(343L,"mes:pro-process-pool-team-leader:review");assertEquals(343L,service.pqcLeader(e));assertNull(e.getProcessId());assertNull(e.getRouteProcessId());assertEquals(985L,e.getQaProcessId());verify(scopes,never()).selectActiveScopesByLeader(anyLong(),anyString());}
 @Test void reviewOnlyLeaderIsFrozenHandlerButNotAnExtraCreateRecipient(){when(scopes.selectActiveScopesByLeaderType("PQC")).thenReturn(List.of(scope(343L,"EMPLOYEE",null)));allowUser(343L,"mes:pro-process-pool-team-leader:review");allowUser(344L,"mes:pro-feedback:create");assertEquals(List.of(343L),service.validatePqcHandoffCandidates("344"));verify(permissions,never()).hasAnyPermissionsInRoles(Set.of(1343L),"mes:pro-feedback:create");verify(scopes,never()).selectActiveScopesByLeader(anyLong(),anyString());}
 @Test void formalLeaderSelfSubmissionHasTheSameUniqueEmployeeResponsibility(){var e=pqcEvent().setActualEmployeeId(343L);when(pqcTasks.selectById(55L)).thenReturn(pqcTask());when(scopes.selectActiveScopesByLeaderType("PQC")).thenReturn(List.of(scope(343L,"EMPLOYEE",null)));allowUser(343L,"mes:pro-process-pool-team-leader:review");assertEquals(343L,service.pqcLeader(e));}
 @Test void unsignedPqcEventCannotImpersonateItsFormalEmployeeOrLeader(){var e=pqcEvent().setSignatureUserId(344L);assertThrows(RuntimeException.class,()->service.originalActor(e));verifyNoInteractions(pqcTasks,scopes,users,permissions,signatures);}
 @org.junit.jupiter.params.ParameterizedTest
 @org.junit.jupiter.params.provider.ValueSource(booleans={false,true})
 void ambiguousEmployeeOrLeaderSelfResponsibilityCannotResolveReview(boolean self){var e=pqcEvent().setActualEmployeeId(self?343L:344L);when(pqcTasks.selectById(55L)).thenReturn(pqcTask());var other=scope(349L,"EMPLOYEE",null).setEmployeeUserId(self?343L:344L);when(scopes.selectActiveScopesByLeaderType("PQC")).thenReturn(List.of(scope(343L,"EMPLOYEE",null),other));assertThrows(RuntimeException.class,()->service.pqcLeader(e));verifyNoInteractions(users,permissions);}
 @org.junit.jupiter.params.ParameterizedTest
 @org.junit.jupiter.params.provider.ValueSource(strings={"disabledScope","foreignScope","wrongType","wrongLeaderType","noScope","disabledLeader","foreignLeader","missingReviewGrant"})
 void onlyCurrentEnabledFormalPqcPersonnelCanOwnReview(String defect){var e=pqcEvent();when(pqcTasks.selectById(55L)).thenReturn(pqcTask());var row=scope(343L,"EMPLOYEE",null);var account=allowUser(343L,"mes:pro-process-pool-team-leader:review");switch(defect){case "disabledScope"->row.setEnabled(false);case "foreignScope"->row.setTenantId(2L);case "wrongType"->row.setScopeType("PROCESS");case "wrongLeaderType"->row.setLeaderType("PRODUCTION");case "disabledLeader"->account.setStatus(1);case "foreignLeader"->account.setTenantId(2L);case "missingReviewGrant"->when(permissions.hasAnyPermissionsInRoles(Set.of(1343L),"mes:pro-process-pool-team-leader:review")).thenReturn(false);case "noScope"->{}default->throw new IllegalArgumentException(defect);}when(scopes.selectActiveScopesByLeaderType("PQC")).thenReturn("noScope".equals(defect)?List.of():List.of(row));assertThrows(RuntimeException.class,()->service.pqcLeader(e));}
 @org.junit.jupiter.params.ParameterizedTest
 @org.junit.jupiter.params.provider.ValueSource(strings={"noScope","ambiguousEmployee","disabledEmployee","foreignEmployee","missingCreateGrant","missingReviewGrant"})
 void handoffNeverSilentlySkipsAnInvalidFormalEmployee(String defect){var row=scope(343L,"EMPLOYEE",null);var employee=allowUser(344L,"mes:pro-feedback:create");allowUser(343L,"mes:pro-process-pool-team-leader:review");List<MesProcessPoolTeamLeaderScopeDO> rows=List.of(row);switch(defect){case "noScope"->rows=List.of();case "ambiguousEmployee"->rows=List.of(row,scope(349L,"EMPLOYEE",null));case "disabledEmployee"->employee.setStatus(1);case "foreignEmployee"->employee.setTenantId(2L);case "missingCreateGrant"->when(permissions.hasAnyPermissionsInRoles(Set.of(1344L),"mes:pro-feedback:create")).thenReturn(false);case "missingReviewGrant"->when(permissions.hasAnyPermissionsInRoles(Set.of(1343L),"mes:pro-process-pool-team-leader:review")).thenReturn(false);default->throw new IllegalArgumentException(defect);}when(scopes.selectActiveScopesByLeaderType("PQC")).thenReturn(rows);assertThrows(RuntimeException.class,()->service.validatePqcHandoffCandidates("344"));}
 @Test void unrelatedUnqualifiedAndAmbiguousPersonnelNeverBecomeSelectedRouteCandidates(){
  when(scopes.selectActiveScopesByLeaderType("PQC")).thenReturn(List.of(scope(343L,"EMPLOYEE",null),scope(1L,"EMPLOYEE",null).setEmployeeUserId(1606L),scope(512L,"EMPLOYEE",null).setEmployeeUserId(914524L),scope(914524L,"EMPLOYEE",null).setEmployeeUserId(914524L)));
  allowUser(344L,"mes:pro-feedback:create");allowUser(343L,"mes:pro-process-pool-team-leader:review");assertEquals(List.of(343L),service.validatePqcHandoffCandidates("344"));
  verify(users,never()).selectById(1606L);verify(users,never()).selectById(914524L);verify(users,never()).selectById(512L);verify(users,never()).selectById(1L);
 }
 @Test void everyExplicitlySelectedCandidateMustBeValidRatherThanDroppedFromRoleGroup(){
  when(scopes.selectActiveScopesByLeaderType("PQC")).thenReturn(List.of(scope(343L,"EMPLOYEE",null),scope(338L,"EMPLOYEE",null).setEmployeeUserId(339L)));
  allowUser(344L,"mes:pro-feedback:create");allowUser(343L,"mes:pro-process-pool-team-leader:review");allowUser(339L,"mes:pro-feedback:create").setStatus(1);
  assertThrows(RuntimeException.class,()->service.validatePqcHandoffCandidates("339,344"));
 }
 @org.junit.jupiter.params.ParameterizedTest
 @org.junit.jupiter.params.provider.ValueSource(strings={"eventType","eventTenant","mesIdentity","sourceType","missingTask","taskId","taskTenant","taskStatus","taskCycle","workOrder","route","qaProcess","submittedEvent","routeProcess","process"})
 void inconsistentFormalPqcSourceCannotResolveAnyLeader(String defect){
  var e=pqcEvent();var task=pqcTask();when(pqcTasks.selectById(55L)).thenReturn(task);
  switch(defect){
   case "eventType" -> e.setEventType("PRODUCTION_SUBMIT");
   case "eventTenant" -> e.setTenantId(2L);
   case "mesIdentity" -> e.setProcessId(15L).setRouteProcessId(91L);
   case "sourceType" -> e.setFeedbackSourceType("UNRELATED_SOURCE");
   case "missingTask" -> when(pqcTasks.selectById(55L)).thenReturn(null);
   case "taskId" -> task.setId(56L);
   case "taskTenant" -> task.setTenantId(2L);
   case "taskStatus" -> task.setTaskStatus("PENDING");
   case "taskCycle" -> task.setActiveOrderId(null);
   case "workOrder" -> task.setWorkOrderId(275L);
   case "route" -> task.setRouteId(99L);
   case "qaProcess" -> task.setQaProcessId(986L);
   case "submittedEvent" -> task.setSubmittedEventId(177L);
   case "routeProcess" -> task.setRouteProcessId(null);
   case "process" -> task.setProcessId(null);
   default -> throw new IllegalArgumentException(defect);
  }
  assertThrows(RuntimeException.class,()->service.pqcLeader(e));verifyNoInteractions(scopes,users,permissions);
 }
 @org.junit.jupiter.params.ParameterizedTest
 @org.junit.jupiter.params.provider.ValueSource(strings={"SUBMITTED","CONFIRMED"})
 void formalSubmittedAndConfirmedTaskSuppliesStageWithoutPayloadInference(String status){var e=pqcEvent().setRawPayload("{}");var task=pqcTask().setTaskStatus(status);when(pqcTasks.selectById(55L)).thenReturn(task);assertSame(task,service.pqcTask(e));assertEquals(91L,task.getRouteProcessId());assertNull(e.getProcessId());assertNull(e.getRouteProcessId());verifyNoInteractions(scopes,users,permissions);}
 @Test void profileSignatureRemainsValidWithoutInventedSystemReceiver(){var e=production("MES_EMPLOYEE_PROFILE");bind(e,"MES_EMPLOYEE_PROFILE");var identity=service.submissionIdentity(e);assertEquals("MES_EMPLOYEE_PROFILE",identity.domain());assertEquals(342L,identity.signerId());assertEquals("正式档案签名人",service.initiatorSnapshot(1L,e).get("name"));assertEquals("MES_EMPLOYEE_PROFILE",service.initiatorSnapshot(1L,e).get("domain"));assertThrows(RuntimeException.class,()->service.originalActor(e));verifyNoInteractions(users,permissions);}
 @Test void wrongIdentityDomainAndCanonicalSignerAreRejected(){var e=production("MES_EMPLOYEE_PROFILE");bind(e,"SYSTEM_USER");assertThrows(RuntimeException.class,()->service.submissionIdentity(e));bind(e,"DEVICE");assertThrows(RuntimeException.class,()->service.submissionIdentity(e));}
 void bind(MesProProcessPoolEventDO e,String domain){when(evidence.isValidForEvent(e)).thenReturn(true);var dto=mock(ElectronicSignatureEvidenceDTO.class);when(dto.actorId()).thenReturn(342L);when(dto.actorDisplayName()).thenReturn("正式档案签名人");when(dto.canonicalContentJson()).thenReturn("{\"signatureIdentity\":{\"domain\":\""+domain+"\",\"tenantId\":1,\"signerId\":342,\"operatorId\":1}}");when(signatures.getById(901L)).thenReturn(dto);}
 AdminUserDO allowUser(Long id,String permission){var user=AdminUserDO.builder().id(id).status(0).build();user.setTenantId(1L);when(users.selectById(id)).thenReturn(user);var roles=Set.of(id+1000);when(permissions.getUserRoleIdListByUserId(id)).thenReturn(roles);when(permissions.hasAnyPermissionsInRoles(roles,permission)).thenReturn(true);return user;}
 static MesProProcessPoolEventDO production(String domain){var e=MesProProcessPoolEventDO.builder().id(176L).eventType("PRODUCTION_SUBMIT").signatureId(901L).actualEmployeeId(342L).signatureUserId(342L).deviceAccountId(1L).rawPayload("{\"activeOrderId\":413,\"signatureIdentityDomain\":\""+domain+"\"}").build();e.setTenantId(1L);return e;}
 static MesProcessPoolTeamLeaderScopeDO scope(Long leader,String type,Long process){var s=MesProcessPoolTeamLeaderScopeDO.builder().leaderUserId(leader).leaderType("PQC").scopeType(type).employeeUserId(344L).processId(process).enabled(true).build();s.setTenantId(1L);return s;}
 static MesProProcessPoolEventDO pqcEvent(){var e=MesProProcessPoolEventDO.builder().id(176L).eventType("PQC_INSPECTION").actualEmployeeId(344L).workOrderId(274L).routeId(98L).qaProcessId(985L).feedbackSourceType("MES_PQC_INSPECTION_TASK").feedbackSourceId(55L).rawPayload("{\"activeOrderId\":413,\"pqcTaskId\":55}").build();e.setTenantId(1L);return e;}
 static MesPqcInspectionTaskDO pqcTask(){var task=new MesPqcInspectionTaskDO().setId(55L).setActiveOrderId(413L).setWorkOrderId(274L).setRouteId(98L).setRouteVersionId(17L).setRouteProcessId(91L).setProcessId(15L).setQaProcessId(985L).setRegulationVersionId(19L).setSubmittedEventId(176L).setTaskStatus("SUBMITTED");task.setTenantId(1L);return task;}
}
