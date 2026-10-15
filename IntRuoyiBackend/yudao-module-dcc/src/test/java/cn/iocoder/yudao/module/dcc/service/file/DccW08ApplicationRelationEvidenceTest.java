package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectApplicationAttributesDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.service.file.relations.*;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.lang.reflect.Modifier;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Query wiring decisions; relation payload and authority are explicit ports, not a substitute for H2 reader verification. */
class DccW08ApplicationRelationEvidenceTest {
    DccControlledFileQueryServiceImpl query;
    DccControlledFileApplicationEvidenceMapper evidence;
    DccControlledFileDetailAuthorizationGuard detail;
    DccApplicationHistoryGuard history;
    DccRelationStageSnapshotService stages;
    DccControlledFileDO file;
    DccProjectApplicationAttributesDO attributes;

    @BeforeEach void setup() throws Exception {
        TenantContextHolder.setTenantId(1L);query=new DccControlledFileQueryServiceImpl();
        for(var field:query.getClass().getDeclaredFields())if(!Modifier.isStatic(field.getModifiers())&&field.getAnnotation(Resource.class)!=null)
            ReflectionTestUtils.setField(query,field.getName(),mock(field.getType()));
        file=DccControlledFileDO.builder().id(23L).tenantId(1L).masterId(12L).dccProjectCodeId(5L).categoryId(2L)
                .requesterId(88L).versionNo("A/1").status("ACTIVE").build();
        DccControlledFileMapper files=port("controlledFileMapper");when(files.selectById(23L)).thenReturn(file);
        detail=port("detailAuthorizationGuard");when(detail.isAllowed(eq(99L),eq(file),anyBoolean(),any())).thenReturn(true);
        evidence=port("applicationEvidenceMapper");history=port("applicationHistoryGuard");stages=port("relationStageSnapshotService");
        when(evidence.selectBoundRounds(1L,5L,"REVISION",23L,"actual-bpm")).thenReturn(List.of(7));
        attributes=new DccProjectApplicationAttributesDO();attributes.setTenantId(1L);attributes.setProjectCodeId(5L);
        attributes.setApplicationType("REVISION");attributes.setApplicationId(23L);attributes.setApplicationRound(7);attributes.setSubmitted(true);
        attributes.setDefaultSourceJson("{\"targetMarkets\":[\"CE\"],\"licenseHolder\":\"Y\",\"actualManufacturer\":\"N\",\"documentTransfer\":\"N\"}");
        attributes.setActualAttributesJson("{\"targetMarkets\":[\"FDA\"],\"licenseHolder\":\"N\",\"actualManufacturer\":\"Y\",\"documentTransfer\":\"N\"}");
        when(evidence.selectSnapshot(1L,5L,"REVISION",23L,7)).thenReturn(attributes);
    }
    @AfterEach void clear(){TenantContextHolder.clear();}
    @SuppressWarnings("unchecked")private <T>T port(String name){return (T)ReflectionTestUtils.getField(query,name);}
    private DccControlledFileApplicationEvidence read(){return query.getApplicationEvidence(99L,23L,"REVISION","actual-bpm");}

    @Test void authorizedBoundFrozenReadReturnsExactPayloadAfterHistoryAndSnapshotValidation() {
        var relations=new DccRelationRoundEvidence(1L,23L,12L,5L,"REVISION",7,"actual-bpm",false,
                "本轮没有签名节点",List.of(),List.of(),List.of(),"a".repeat(64));
        when(stages.readRoundEvidence(99L,23L,"REVISION","actual-bpm")).thenReturn(relations);
        var result=read();assertTrue(result.recorded());assertSame(relations,result.relationEvidence());
        var order=inOrder(detail,evidence,history,stages);
        order.verify(detail).isAllowed(eq(99L),eq(file),anyBoolean(),any());
        order.verify(evidence).selectBoundRounds(1L,5L,"REVISION",23L,"actual-bpm");
        order.verify(history).require(file,"REVISION","actual-bpm");
        order.verify(evidence).selectSnapshot(1L,5L,"REVISION",23L,7);
        order.verify(stages).readRoundEvidence(99L,23L,"REVISION","actual-bpm");
    }
    @Test void absentMappingKeepsExplicitAbsenceWithoutRelationQuery() {
        when(evidence.selectBoundRounds(1L,5L,"REVISION",23L,"actual-bpm")).thenReturn(List.of());
        var result=read();assertEquals("NOT_RECORDED",result.unavailableReason());assertNull(result.relationEvidence());
        verifyNoInteractions(stages,history);verify(evidence,never()).selectSnapshot(anyLong(),anyLong(),anyString(),anyLong(),anyInt());
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"NO_ATTRIBUTES","DRAFT","BAD_SNAPSHOT","BAD_JSON","HISTORY_REFUSED","DENIED"})
    void incompleteOrUnauthorizedContextNeverInvokesStageReader(String invalid) {
        switch(invalid){
            case "NO_ATTRIBUTES" -> when(evidence.selectSnapshot(1L,5L,"REVISION",23L,7)).thenReturn(null);
            case "DRAFT" -> attributes.setSubmitted(false);
            case "BAD_SNAPSHOT" -> attributes.setApplicationId(24L);
            case "BAD_JSON" -> attributes.setActualAttributesJson("{}");
            case "HISTORY_REFUSED" -> doThrow(new IllegalStateException("actual BPM refused")).when(history).require(file,"REVISION","actual-bpm");
            case "DENIED" -> when(detail.isAllowed(eq(99L),eq(file),anyBoolean(),any())).thenReturn(false);
        }
        if("NO_ATTRIBUTES".equals(invalid)||"DRAFT".equals(invalid))assertNull(read().relationEvidence());
        else assertThrows(RuntimeException.class,this::read);
        verifyNoInteractions(stages);
    }
    @Test void relationPortSchemaFailurePropagatesAndNullIsNotSuccessfulEvidence() {
        when(stages.readRoundEvidence(99L,23L,"REVISION","actual-bpm")).thenThrow(new IllegalStateException("real stage table missing"));
        assertEquals("real stage table missing",assertThrows(IllegalStateException.class,this::read).getMessage());
        doReturn(null).when(stages).readRoundEvidence(99L,23L,"REVISION","actual-bpm");
        assertThrows(NullPointerException.class,this::read);
    }
}
