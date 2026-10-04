package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionSignatureDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionSignatureMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesBatchRecordSignatureSubjectAdapter;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProductionSubmitSignatureContext;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureEvidenceDTO;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureVerificationDTO;
import com.alibaba.fastjson.JSON;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MesSubmissionSignatureIdentityReaderTest {
    @Mock ElectronicSignatureQueryService signatures;
    @Mock MesProProcessPoolEventMapper events;
    @Mock MesProBatchRecordExecutionSignatureMapper simulations;
    @InjectMocks MesSubmissionSignatureIdentityReader reader;
    private final LocalDateTime signedAt = LocalDateTime.of(2026,10,4,10,0);
    @BeforeEach void tenant() { TenantContextHolder.setTenantId(1L); }
    @AfterEach void clear() { TenantContextHolder.clear(); }

    @Test void selectedTemporarySignerBUsesFrozenNameInsteadOfOperatorAOrSameIdSystemUser() {
        var event = event("PRODUCTION_SUBMIT");
        when(events.selectById(11L)).thenReturn(event);
        when(signatures.getById(70L)).thenReturn(evidence("PRODUCTION_SUBMIT", "MES_EMPLOYEE_PROFILE", 1L, 7L, 99L, "Frozen B"));
        valid();
        var result = reader.read(70L,11L,100L,"PRODUCTION_SUBMIT");
        assertEquals("Frozen B",result.getSignerName()); assertEquals(signedAt,result.getSignedAt());
        assertEquals(70L,result.getSignatureId());
    }
    @Test void selectedSystemPqcEmployeeIsIndependentOfOperatorAndSameIdProfile() {
        when(events.selectById(11L)).thenReturn(event("PQC_SUBMIT"));
        when(signatures.getById(70L)).thenReturn(evidence("PQC_SUBMIT", "SYSTEM_USER",1L,7L,99L,"Frozen PQC B"));
        valid();
        assertEquals("Frozen PQC B",reader.read(70L,11L,100L,"PQC_SUBMIT").getSignerName());
    }
    @Test void wrongTenantDomainSignerAndOperatorRemainRejected() {
        when(events.selectById(11L)).thenReturn(event("PRODUCTION_SUBMIT"));
        for (var evidence : java.util.List.of(
                evidence("PRODUCTION_SUBMIT","SYSTEM_USER",1L,7L,99L,"B"),
                evidence("PRODUCTION_SUBMIT","MES_EMPLOYEE_PROFILE",2L,7L,99L,"B"),
                evidence("PRODUCTION_SUBMIT","MES_EMPLOYEE_PROFILE",1L,8L,99L,"B"),
                evidence("PRODUCTION_SUBMIT","MES_EMPLOYEE_PROFILE",1L,7L,98L,"B"))) {
            when(signatures.getById(70L)).thenReturn(evidence);
            assertThrows(IllegalStateException.class,()->reader.read(70L,11L,100L,"PRODUCTION_SUBMIT"));
        }
        verify(signatures,never()).verifyEvidence(anyLong());
    }
    @Test void formalMissingIdentityCannotBeReinterpretedAsSimulation() {
        when(events.selectById(11L)).thenReturn(event("PRODUCTION_SUBMIT"));
        var evidence = evidence("PRODUCTION_SUBMIT","MES_EMPLOYEE_PROFILE",1L,7L,99L,"B");
        var json=JSON.parseObject(evidence.canonicalContentJson()); json.remove("signatureIdentity");
        when(signatures.getById(70L)).thenReturn(withContent(evidence,json.toJSONString()));
        assertThrows(IllegalStateException.class,()->reader.read(70L,11L,100L,"PRODUCTION_SUBMIT"));
        verifyNoInteractions(simulations);
    }
    @Test void wrongAssociationAndHashMismatchCannotProduceSignerDisplay() {
        var event=event("PRODUCTION_SUBMIT"); when(events.selectById(11L)).thenReturn(event);
        event.setSignatureId(71L);
        assertThrows(IllegalStateException.class,()->reader.read(70L,11L,100L,"PRODUCTION_SUBMIT"));
        verifyNoInteractions(signatures);
        event.setSignatureId(70L);
        when(signatures.getById(70L)).thenReturn(evidence("PRODUCTION_SUBMIT","MES_EMPLOYEE_PROFILE",1L,7L,99L,"B"));
        when(signatures.verifyEvidence(70L)).thenReturn(new ElectronicSignatureVerificationDTO(70L,"MISMATCH","c","changed","e","e","SHA-256","v1"));
        assertThrows(IllegalStateException.class,()->reader.read(70L,11L,100L,"PRODUCTION_SUBMIT"));
    }
    @Test void explicitStage1ProductionAndPqcSessionRecordsRemainNonFormalDisplay() {
        for (String action : java.util.List.of("PRODUCTION_SUBMIT","PQC_SUBMIT")) {
            var event=event(action).setTemplateType("PRODUCTION_SUBMIT".equals(action)?"SIMULATED_PRODUCTION_SUBMIT":"SIMULATED_PQC_INSPECTION")
                    .setRawPayload("{\"simulated\":true,\"simulationStage\":\"STAGE1\"}")
                    .setSignatureSnapshot("{\"simulated\":true,\"signatureId\":70,\"actorId\":7,\"objectId\":100,\"actionType\":\""+action+"\"}");
            when(events.selectById(11L)).thenReturn(event);
            var projection=MesProBatchRecordExecutionSignatureDO.builder().id(70L).actorId(7L).actionType(action)
                    .signatureMode("SIMULATION_SESSION").passwordVerified(false).reviewSourceType("MES_ACTIVE_ORDER_SIMULATION")
                    .reviewSourceId(100L).signedAt(signedAt).actorName("Simulation B").build();
            when(simulations.selectById(70L)).thenReturn(projection);
            var result=reader.read(70L,11L,100L,action);
            assertEquals("Simulation B（模拟）",result.getSignerName());
            assertEquals("SIMULATION_SESSION",result.getRole()); assertNull(result.getSignatureId());
            projection.setPasswordVerified(true);
            assertThrows(IllegalStateException.class,()->reader.read(70L,11L,100L,action));
        }
        verifyNoInteractions(signatures);
    }

    private MesProProcessPoolEventDO event(String action) {
        var event = new MesProProcessPoolEventDO().setId(11L).setEventType("PRODUCTION_SUBMIT".equals(action)?"PRODUCTION_SUBMIT":"PQC_INSPECTION")
                .setActualEmployeeId(7L).setSignatureUserId(7L).setDeviceAccountId(99L).setSignatureId(70L)
                .setRouteProcessId(40L).setProcessId(30L).setEventIdempotencyKey("submission")
                .setRawPayload("{\"signatureIdentityDomain\":\"MES_EMPLOYEE_PROFILE\"}")
                .setFeedbackSourceType("MES_PQC_INSPECTION_TASK").setFeedbackSourceId(200L);
        event.setTenantId(1L);return event;
    }
    private void valid() { when(signatures.verifyEvidence(70L)).thenReturn(new ElectronicSignatureVerificationDTO(70L,"VALID","c","c","e","e","SHA-256","v1")); }
    private ElectronicSignatureEvidenceDTO evidence(String action,String domain,Long tenant,Long signer,Long operator,String name) {
        boolean production="PRODUCTION_SUBMIT".equals(action);
        String sourceType=production?MesProductionSubmitSignatureContext.SOURCE_TYPE:"MES_PQC_INSPECTION_TASK";
        Long sourceId=production?100L:200L;
        String sourceName=production?new MesProductionSubmitSignatureContext(100L,40L,30L,"submission").sourceName():"PQC task";
        String subject=MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(0L,action,null,null,null,null,null,null,null,sourceType,sourceId,sourceName,null,null,null,null,null);
        var json=JSON.parseObject("{\"executionId\":\"0\",\"actionType\":\""+action+"\",\"reviewSourceType\":\""+sourceType+"\",\"reviewSourceId\":\""+sourceId+"\"}");
        json.put("reviewSourceName",sourceName);
        json.put("signatureIdentity",Map.of("domain",domain,"tenantId",tenant,"signerId",signer,"operatorId",operator,"displayName",name));
        return new ElectronicSignatureEvidenceDTO(70L,"MES",action,"MES_BATCH_RECORD",subject,"v1",7L,action,action,"reason",signedAt,"time","SESSION_PLUS_SELECTED_USER_PASSWORD","c","e","SHA-256","v1","p1","VALID",null,null,null,null,json.toJSONString(),null,null,null,"Current different name");
    }
    private ElectronicSignatureEvidenceDTO withContent(ElectronicSignatureEvidenceDTO e,String json) {
        return new ElectronicSignatureEvidenceDTO(e.id(),e.moduleCode(),e.actionCode(),e.subjectType(),e.subjectId(),e.subjectVersion(),e.actorId(),e.meaningCode(),e.meaningLabel(),e.reason(),e.signedAt(),e.timeEvidenceId(),e.authenticationMethod(),e.contentHash(),e.evidenceHash(),e.algorithm(),e.keyVersion(),e.policyVersion(),e.verificationStatus(),e.processInstanceId(),e.taskId(),e.nodeCode(),e.nodeOrder(),json,e.beforeContentJson(),e.afterContentJson(),e.fieldDiffJson(),e.actorDisplayName());
    }
}
