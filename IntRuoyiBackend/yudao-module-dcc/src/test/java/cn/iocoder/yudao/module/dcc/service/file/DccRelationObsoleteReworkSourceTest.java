package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionInstanceDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionInstanceMapper;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccRelationStageSnapshotMapper;
import cn.iocoder.yudao.module.dcc.service.file.relations.*;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationStageContracts.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Actual Stage/Store/Round/reserve SQL and transactions; account/access/audit ports stay isolated.
 * Mapper adapters read the same private H2 rows, rather than returning invented round/evidence facts. */
class DccRelationObsoleteReworkSourceTest extends DccRelationStageSnapshotServiceTest {
    DccRelationObsoleteReworkSourceTest() throws Exception { super(); }

    private void actualObsoleteContext() throws Exception {
        DccRelationStageH2Schema.addNullableColumns(jdbc,"20261012_dcc_obsolete_form_draft_round.sql");
        for(String column:List.of("instance_code VARCHAR(64)","applicant_user_id BIGINT","object_state VARCHAR(64)"))
            jdbc.execute("ALTER TABLE bpm_form_action_instance ADD "+column);
        for(String column:List.of("version_no VARCHAR(64)","status VARCHAR(64)","controlled_time TIMESTAMP"))
            jdbc.execute("ALTER TABLE dcc_controlled_file ADD "+column);
        jdbc.update("UPDATE dcc_controlled_file SET version_no='A/1',status='ACTIVE',controlled_time=? WHERE id=100",signedAt);
        for(long id:List.of(200L,300L,400L))jdbc.update("INSERT INTO dcc_controlled_file(id,tenant_id,master_id,dcc_project_code_id,deleted,version_no,status,controlled_time) VALUES(?,1,?,2,0,'A/1','ACTIVE',?)",id,id/10,signedAt);
        var fileRows=mock(DccControlledFileMapper.class);
        when(fileRows.selectById(any())).thenAnswer(c->readFile(c.getArgument(0),false));
        when(fileRows.selectByIdAndTenantForUpdate(any(),any())).thenAnswer(c->readFile(c.getArgument(1),true));
        var masters=mock(DccControlledFileMasterMapper.class);
        when(masters.selectByIdForUpdate(any())).thenAnswer(c->{
            var rows=jdbc.queryForList("SELECT * FROM dcc_controlled_file_master WHERE id=? FOR UPDATE",c.<Long>getArgument(0));
            if(rows.isEmpty())return null;
            var row=rows.get(0);var result=DccControlledFileMasterDO.builder().id(((Number)row.get("ID")).longValue())
                    .dccProjectCodeId(2L).latestControlledFileId(100L).build();
            result.setTenantId(((Number)row.get("TENANT_ID")).longValue());return result;
        });
        var formRows=mock(FormActionInstanceMapper.class);
        when(formRows.selectByIdAndTenantForUpdate(any(),any())).thenAnswer(c->{
            var rows=jdbc.queryForList("SELECT * FROM bpm_form_action_instance WHERE tenant_id=? AND id=? FOR UPDATE",c.<Long>getArgument(0),c.<Long>getArgument(1));
            if(rows.isEmpty())return null;var row=rows.get(0);
            return FormActionInstanceDO.builder().id(((Number)row.get("ID")).longValue()).tenantId(((Number)row.get("TENANT_ID")).longValue())
                    .instanceCode((String)row.get("INSTANCE_CODE")).applicantUserId(((Number)row.get("APPLICANT_USER_ID")).longValue())
                    .systemCode((String)row.get("SYSTEM_CODE")).objectType((String)row.get("OBJECT_TYPE")).actionCode((String)row.get("ACTION_CODE"))
                    .objectId((String)row.get("OBJECT_ID")).objectVersion((String)row.get("OBJECT_VERSION")).objectState((String)row.get("OBJECT_STATE"))
                    .status((String)row.get("STATUS")).bpmProcessInstanceId((String)row.get("BPM_PROCESS_INSTANCE_ID")).build();
        });
        @SuppressWarnings("unchecked") var provider=mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(formRows);
        ReflectionTestUtils.setField(rounds,"files",fileRows);
        ReflectionTestUtils.setField(rounds,"obsoleteMasters",masters);
        ReflectionTestUtils.setField(rounds,"obsoleteForms",provider);
        var resolver=new DccLatestControlledFileResolverImpl();
        ReflectionTestUtils.setField(resolver,"fileMapper",fileRows);
        ReflectionTestUtils.setField(resolver,"masterMapper",masters);
        ReflectionTestUtils.setField(resolver,"jdbc",jdbc);
        service=new DccRelationStageSnapshotService(new DccRelationStore(jdbc,audit),new DccRelationStageSnapshotMapper(jdbc),query,resolver,access);
        ReflectionTestUtils.setField(service,"rounds",rounds);
    }
    private DccControlledFileDO readFile(Long id,boolean locked) {
        var rows=jdbc.queryForList("SELECT * FROM dcc_controlled_file WHERE tenant_id=1 AND id=?"+(locked?" FOR UPDATE":""),id);
        if(rows.isEmpty())return null;var row=rows.get(0);
        var result=DccControlledFileDO.builder().id(id).masterId(((Number)row.get("MASTER_ID")).longValue())
                .dccProjectCodeId(((Number)row.get("DCC_PROJECT_CODE_ID")).longValue()).fileNumber("N"+id).fileName("文件"+id)
                .versionNo((String)row.get("VERSION_NO")).status((String)row.get("STATUS")).processInstanceId((String)row.get("PROCESS_INSTANCE_ID"))
                .controlledTime(((java.sql.Timestamp)row.get("CONTROLLED_TIME")).toLocalDateTime()).build();
        result.setTenantId(((Number)row.get("TENANT_ID")).longValue());return result;
    }
    private int newForm(long id) {
        jdbc.update("INSERT INTO bpm_form_action_instance(id,tenant_id,system_code,object_type,action_code,object_id,object_version,status,bpm_process_instance_id,deleted,instance_code,applicant_user_id,object_state) VALUES(?,1,'DCC','CONTROLLED_FILE','OBSOLETE','100','A/1','DRAFT',NULL,0,?,7,'ACTIVE')",id,"FORM-"+id);
        return tx.execute(s->rounds.reserveObsoleteFormDraft(7L,100L,id,"FORM-"+id));
    }
    private void bindForm(long id,int number,String bpm) {
        jdbc.update("UPDATE bpm_form_action_instance SET status='IN_APPROVAL',bpm_process_instance_id=? WHERE id=?",bpm,id);
        tx.executeWithoutResult(s->rounds.bindObsoleteFormDraft(7L,100L,id,"FORM-"+id,number,bpm));
    }
    private void freezeObsolete(int number,long formId,String bpm,long signatureId) {
        var task=new ValidatedTaskIdentity(1L,bpm,"ot-"+formId,"form-"+formId+":5","MATRIX_REVIEW","node-"+formId,5L,"ASSIGN","OBSOLETE",number,formId);
        var prepared=tx.execute(s->service.prepare(7L,100L,task,List.of(),List.of(),"command-"+formId,null,"正式作废指派"));
        tx.executeWithoutResult(s->{
            jdbc.update("INSERT INTO dcc_controlled_file_signature VALUES(?,100,?,?,7,?,'VALID',?,'ASSIGN',?,0)",signatureId,task.taskId(),bpm,signedAt,"evidence-"+signatureId,"正式作废指派；"+prepared.signedReasonMarker());
            service.freeze(prepared,new SignatureIdentity(signatureId,signedAt,"evidence-"+signatureId));
        });
    }
    private DccRelationRoundEvidence oldSource(boolean head,String status) throws Exception {
        // Native signed supplement is a real frozen fact; obsolete actions add no new selections.
        freeze(prepare("native-task","dept-5",List.of(300L),"native-command",null,List.of()),1);
        actualObsoleteContext();int number=newForm(99);bindForm(99,number,"old-obsolete");
        if(head)freezeObsolete(number,99,"old-obsolete",2);
        jdbc.update("UPDATE bpm_form_action_instance SET status=? WHERE id=99",status);
        return service.readRoundEvidence(7L,100L,"OBSOLETE","old-obsolete");
    }
    private int targetAfterOtherForm() { assertEquals(2,newForm(101));return newForm(102); }
    private DccRelationRoundSource inherit(DccRelationRoundEvidence source,int target) {
        return tx.execute(s->service.inheritRoundSelections(7L,100L,"OBSOLETE","old-obsolete",100L,"OBSOLETE",target,source.relationDigest()));
    }
    private List<Map<String,Object>> oldFacts() {
        var rows=new ArrayList<Map<String,Object>>();
        for(String table:List.of("dcc_controlled_file","dcc_controlled_file_related_file","dcc_controlled_file_signature","dcc_relation_stage_snapshot","dcc_relation_stage_member"))rows.addAll(jdbc.queryForList("SELECT * FROM "+table+" ORDER BY id"));
        rows.addAll(jdbc.queryForList("SELECT * FROM dcc_application_round_link WHERE bpm_round IS NOT NULL ORDER BY id"));
        rows.addAll(jdbc.queryForList("SELECT * FROM bpm_form_action_instance WHERE id=99"));return rows;
    }
    @ParameterizedTest @ValueSource(strings={"REJECTED","ABANDONED"})
    void sameFileNewFormUsesActualNonAdjacentReservedRoundAndNeverMutatesSource(String status) throws Exception {
        var source=oldSource(true,status);var original=oldFacts();int target=targetAfterOtherForm();assertEquals(3,target);
        var other=jdbc.queryForList("SELECT * FROM dcc_application_round_link WHERE form_instance_id=101");
        var inherited=inherit(source,target);assertEquals(100L,inherited.sourceFileId());assertEquals("old-obsolete",inherited.sourceBpmRound());
        assertEquals(source.relationDigest(),inherited.sourceRelationDigest());assertEquals(3,inherited.targetAttributeRound());
        var after=jdbc.queryForList("SELECT * FROM dcc_application_round_link WHERE form_instance_id=102");
        assertEquals(inherited,inherit(source,target));assertEquals(after,jdbc.queryForList("SELECT * FROM dcc_application_round_link WHERE form_instance_id=102"));
        assertEquals(original,oldFacts());assertEquals(other,jdbc.queryForList("SELECT * FROM dcc_application_round_link WHERE form_instance_id=101"));
        verify(audit,times(3)).append(any()); // native freeze, old obsolete freeze, one source bind
        bindForm(102,target,"new-obsolete");
        var next=service.readRoundEvidence(7L,100L,"OBSOLETE","new-obsolete");assertEquals(source.completeSelections(),next.completeSelections());
        assertFalse(next.recorded());assertEquals(3,next.attributeRound());assertEquals(2,next.completeSelections().size());
        freezeObsolete(target,102,"new-obsolete",3);
        var newSigned=service.readRoundEvidence(7L,100L,"OBSOLETE","new-obsolete");
        assertEquals(102L,newSigned.stages().get(0).snapshot().header().formActionInstanceId());
        assertEquals(source,service.readRoundEvidence(7L,100L,"OBSOLETE","old-obsolete"));
        assertEquals("round-1",jdbc.queryForObject("SELECT process_instance_id FROM dcc_controlled_file WHERE id=100",String.class));
    }
    @Test void absentObsoleteHeadStillUsesNativeOriginalAndSignedSupplement() throws Exception {
        var source=oldSource(false,"ABANDONED");assertFalse(source.recorded());assertEquals(2,source.completeSelections().size());
        assertEquals(300L,source.completeSelections().get(1).selectedControlledFileId());inherit(source,targetAfterOtherForm());
    }
    @ParameterizedTest @ValueSource(strings={"FORM","MASTER","PROJECT","ATTRIBUTE","PARTIAL_TYPE","MISSING_ATTRIBUTE","MISSING_FORM"})
    void sourceHeadMustMatchItsActualFormAndFamily(String violation) throws Exception {
        oldSource(true,"REJECTED");int target=targetAfterOtherForm();
        String column=switch(violation){case "FORM","MISSING_FORM"->"form_action_instance_id";case "MASTER"->"source_master_id";
            case "PROJECT"->"project_id";case "PARTIAL_TYPE"->"application_type";default->"attribute_round";};
        String invalid=violation.startsWith("MISSING")||violation.equals("PARTIAL_TYPE")?"NULL":"999";
        jdbc.update("UPDATE dcc_relation_stage_snapshot SET "+column+"="+invalid+" WHERE process_instance_id='old-obsolete'");
        var original=oldFacts();assertThrows(DccRelationFailure.class,()->service.readRoundEvidence(7L,100L,"OBSOLETE","old-obsolete"));
        assertEquals(original,oldFacts());assertNull(rounds.readRelationSource(2L,"OBSOLETE",100L,target));
    }
    @ParameterizedTest @ValueSource(strings={"INVALID","MISSING","WRONG_ACTOR"})
    void invalidNativeSignedSourceCannotBeInheritedThroughValidObsoleteHead(String violation) throws Exception {
        var source=oldSource(true,"REJECTED");int target=targetAfterOtherForm();
        switch(violation){case "INVALID"->jdbc.update("UPDATE dcc_controlled_file_signature SET evidence_status='INVALID' WHERE id=1");
            case "MISSING"->jdbc.update("DELETE FROM dcc_controlled_file_signature WHERE id=1");
            default->jdbc.update("UPDATE dcc_controlled_file_signature SET actor_id=999 WHERE id=1");}
        var original=oldFacts();var failure=assertThrows(DccRelationFailure.class,()->inherit(source,target));
        assertEquals("DCC_RELATION_STAGE_PRIOR_SIGNATURE_INVALID",failure.getMessage());
        assertEquals(original,oldFacts());assertNull(rounds.readRelationSource(2L,"OBSOLETE",100L,target));
    }
    @Test void lateCallerFailureRollsBackOnlyNewFormRelationSource() throws Exception {
        var source=oldSource(true,"REJECTED");int target=targetAfterOtherForm();var original=oldFacts();
        var targetBefore=jdbc.queryForList("SELECT * FROM dcc_application_round_link WHERE form_instance_id=102");
        assertThrows(IllegalStateException.class,()->tx.execute(s->{service.inheritRoundSelections(7L,100L,"OBSOLETE","old-obsolete",100L,"OBSOLETE",target,source.relationDigest());throw new IllegalStateException("late caller failure");}));
        assertEquals(original,oldFacts());assertEquals(targetBefore,jdbc.queryForList("SELECT * FROM dcc_application_round_link WHERE form_instance_id=102"));
    }
    @Test void inheritRechecksNameAccessForEveryFrozenSelectedBody() throws Exception {
        var source=oldSource(true,"REJECTED");int target=targetAfterOtherForm();var original=oldFacts();
        doThrow(new IllegalStateException("selected name access revoked")).when(access).assertNameVisible(7L,300L);
        assertThrows(IllegalStateException.class,()->inherit(source,target));
        assertEquals(original,oldFacts());assertNull(rounds.readRelationSource(2L,"OBSOLETE",100L,target));
    }
    @Test void retiredSelectedVersionRemainsTheFrozenSelectionWithoutBecomingANewCandidate() throws Exception {
        var source=oldSource(true,"REJECTED");int target=targetAfterOtherForm();
        clearInvocations(query);
        jdbc.update("UPDATE dcc_controlled_file SET status='OBSOLETE' WHERE id=300");
        var reread=service.readRoundEvidence(7L,100L,"OBSOLETE","old-obsolete");assertEquals(source.completeSelections(),reread.completeSelections());
        assertEquals("A/1",reread.completeSelections().get(1).versionNo());inherit(reread,target);
        verify(query,never()).getRelationPermissions(7L,300L); // no new-selection eligibility or latest-version substitution
    }
    @Test void obsoleteMappedFormIdentityCannotBorrowAnotherFormsNumericRound() throws Exception {
        oldSource(true,"REJECTED");int target=targetAfterOtherForm();
        jdbc.update("UPDATE dcc_application_round_link SET form_instance_id=101 WHERE application_type='OBSOLETE' AND bpm_round='old-obsolete'");
        var original=oldFacts();assertThrows(DccRelationFailure.class,()->service.readRoundEvidence(7L,100L,"OBSOLETE","old-obsolete"));
        assertEquals(original,oldFacts());assertNull(rounds.readRelationSource(2L,"OBSOLETE",100L,target));
    }
    @Test void genuinelyUnrecordedHistoricalApplicationIdentityRemainsReadOnlyWithoutBackfill() throws Exception {
        oldSource(true,"ABANDONED");
        jdbc.update("UPDATE dcc_relation_stage_snapshot SET application_type=NULL,attribute_round=NULL,form_action_instance_id=NULL WHERE process_instance_id='old-obsolete'");
        jdbc.update("UPDATE dcc_application_round_link SET form_instance_id=NULL WHERE application_type='OBSOLETE' AND bpm_round='old-obsolete'");
        var original=oldFacts();var source=service.readRoundEvidence(7L,100L,"OBSOLETE","old-obsolete");
        assertEquals(2,source.completeSelections().size());inherit(source,targetAfterOtherForm());assertEquals(original,oldFacts());
    }
    @ParameterizedTest @ValueSource(strings={"SOURCE_BPM","SOURCE_DIGEST","CYCLE"})
    void savedObsoleteSourceFailureDoesNotFallBackToNativeUpload(String violation) throws Exception {
        var source=oldSource(true,"REJECTED");int target=targetAfterOtherForm();inherit(source,target);bindForm(102,target,"new-obsolete");
        switch(violation){case "SOURCE_BPM"->jdbc.update("UPDATE dcc_application_round_link SET source_bpm_round='missing-source-bpm' WHERE form_instance_id=102");
            case "SOURCE_DIGEST"->jdbc.update("UPDATE dcc_application_round_link SET source_relation_digest=? WHERE form_instance_id=102","0".repeat(64));
            default->jdbc.update("UPDATE dcc_application_round_link SET source_bpm_round='new-obsolete' WHERE form_instance_id=102");}
        var original=oldFacts();assertThrows(RuntimeException.class,()->service.readRoundEvidence(7L,100L,"OBSOLETE","new-obsolete"));
        assertEquals(original,oldFacts()); // existing native/upload facts cannot make a missing exact source successful
    }
    @ParameterizedTest @ValueSource(strings={"NATIVE_MAPPING","FORM","TENANT"})
    void missingFormalSourceFactsFailRatherThanReturningEmptySelections(String violation) throws Exception {
        oldSource(true,"REJECTED");int target=targetAfterOtherForm();
        switch(violation){case "NATIVE_MAPPING"->jdbc.update("DELETE FROM dcc_application_round_link WHERE application_type='REVISION' AND application_id=100");
            case "FORM"->jdbc.update("DELETE FROM bpm_form_action_instance WHERE id=99");
            default->jdbc.update("UPDATE bpm_form_action_instance SET tenant_id=999 WHERE id=99");}
        var original=oldFacts();assertThrows(RuntimeException.class,()->service.readRoundEvidence(7L,100L,"OBSOLETE","old-obsolete"));
        assertEquals(original,oldFacts());assertNull(rounds.readRelationSource(2L,"OBSOLETE",100L,target));
    }
}
