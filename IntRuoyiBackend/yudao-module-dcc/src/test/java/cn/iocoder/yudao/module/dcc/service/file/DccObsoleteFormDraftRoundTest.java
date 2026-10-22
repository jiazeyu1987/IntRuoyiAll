package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionInstanceMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Actual private H2 form/round/Project/Master/File; the inherited six native draft cases remain intact. */
class DccObsoleteFormDraftRoundTest extends DccApplicationDraftRoundTest {
    @Resource org.mybatis.spring.SqlSessionTemplate session;
    @Resource DccControlledFileMasterMapper masters;
    @BeforeEach void formalForms() {
        authenticate();
        jdbc.execute("CREATE TABLE IF NOT EXISTS bpm_form_action_instance(id BIGINT PRIMARY KEY,instance_code VARCHAR(128),tenant_id BIGINT,policy_id BIGINT,applicant_user_id BIGINT,status VARCHAR(64),data_domain VARCHAR(64),system_code VARCHAR(64),object_type VARCHAR(64),object_id VARCHAR(128),object_version VARCHAR(128),action_code VARCHAR(64),object_state VARCHAR(64),idempotency_key VARCHAR(128),business_context_json CLOB,form_data_json CLOB,bpm_process_instance_id VARCHAR(128),creator VARCHAR(64),updater VARCHAR(64),create_time TIMESTAMP,update_time TIMESTAMP,deleted BIT DEFAULT 0)");
        jdbc.update("DELETE FROM bpm_form_action_instance WHERE id IN (901,902) AND tenant_id=1 AND object_id='20' AND system_code='DCC'");
        if (!session.getConfiguration().hasMapper(FormActionInstanceMapper.class))
            session.getConfiguration().addMapper(FormActionInstanceMapper.class);
        ObjectProvider<FormActionInstanceMapper> forms=mock(ObjectProvider.class);
        when(forms.getIfAvailable()).thenReturn(session.getMapper(FormActionInstanceMapper.class));
        if (java.util.Arrays.stream(rounds.getClass().getSuperclass().getDeclaredFields()).anyMatch(f->f.getName().equals("obsoleteForms"))
                || java.util.Arrays.stream(DccApplicationRoundService.class.getDeclaredFields()).anyMatch(f->f.getName().equals("obsoleteForms"))) {
            ReflectionTestUtils.setField(rounds,"obsoleteForms",forms);
            ReflectionTestUtils.setField(rounds,"obsoleteMasters",masters);
        }
        jdbc.update("INSERT INTO bpm_form_action_instance(id,instance_code,tenant_id,applicant_user_id,status,system_code,object_type,object_id,object_version,action_code,object_state,form_data_json,deleted) VALUES(901,'FORM-901',1,99,'DRAFT','DCC','CONTROLLED_FILE','20','A/1','OBSOLETE','ACTIVE','{}',0)");
        jdbc.update("INSERT INTO bpm_form_action_instance(id,instance_code,tenant_id,applicant_user_id,status,system_code,object_type,object_id,object_version,action_code,object_state,form_data_json,deleted) VALUES(902,'FORM-902',1,99,'DRAFT','DCC','CONTROLLED_FILE','20','A/1','OBSOLETE','ACTIVE','{}',0)");
    }
    @AfterEach void removeOwnForms() {
        jdbc.update("DELETE FROM bpm_form_action_instance WHERE id IN (901,902) AND tenant_id=1 AND object_id='20' AND system_code='DCC'");
        SecurityContextHolder.clearContext();
    }
    void authenticate(){SecurityFrameworkUtils.setLoginUser(new LoginUser().setId(99L).setTenantId(1L).setUserType(2),new org.springframework.mock.web.MockHttpServletRequest());}
    void controlledFamily(){
        jdbc.update("UPDATE dcc_controlled_file SET status='ACTIVE',version_no='A/1',controlled_time=CURRENT_TIMESTAMP,file_type_taxonomy_id=6 WHERE id=20");
        jdbc.update("INSERT INTO dcc_controlled_file_master(id,tenant_id,file_name,file_number,dcc_project_code_id,file_type_taxonomy_leaf_id,category_id,normalized_file_number,latest_controlled_file_id,current_active_controlled_file_id,status,deleted) VALUES(10,1,'SOP.pdf','SOP-1',5,6,2,'SOP-1',20,20,'ACTIVE_CHAIN',0)");
    }
    int reserveForm(long id){return tx(()->ReflectionTestUtils.invokeMethod(rounds,"reserveObsoleteFormDraft",99L,20L,id,"FORM-"+id));}
    int bindForm(long id,int number,String bpm){return tx(()->ReflectionTestUtils.invokeMethod(rounds,"bindObsoleteFormDraft",99L,20L,id,"FORM-"+id,number,bpm));}
    @Test void twoRealFormsReceiveIndependentNumbersAndExactReopenDoesNotGuessNativeRound(){
        controlledFamily();
        int nativeRound=reserve();
        int first=reserveForm(901);int second=reserveForm(902);
        assertEquals(first,reserveForm(901));assertEquals(first+1,second);
        assertEquals(nativeRound,rounds.requireDraft("UPLOAD",20L));
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_application_round_link WHERE application_type='OBSOLETE' AND bpm_round IS NULL",Integer.class));
        int reopened=tx(()->ReflectionTestUtils.invokeMethod(rounds,"requireObsoleteFormDraft",99L,20L,901L,"FORM-901"));
        assertEquals(first,reopened);
    }
    @Test void onlyActualSubmittedFormCanBindItsExactReservedNumberAndReplayIsStable(){
        controlledFamily();int number=reserveForm(901);
        assertThrows(RuntimeException.class,()->bindForm(901,number,"bpm-A"));
        jdbc.update("UPDATE bpm_form_action_instance SET status='IN_APPROVAL',bpm_process_instance_id='bpm-A' WHERE id=901");
        assertEquals(number,bindForm(901,number,"bpm-A"));assertEquals(number,bindForm(901,number,"bpm-A"));
        assertThrows(RuntimeException.class,()->bindForm(901,number+1,"bpm-A"));
        assertThrows(RuntimeException.class,()->bindForm(901,number,"bpm-B"));
        assertEquals(number,rounds.require("OBSOLETE",20L,"bpm-A"));
    }
    @Test void wrongFormActorCodeVersionActionOrFamilyRejectsWithoutAnyRoundInsert(){
        controlledFamily();
        for (String field : java.util.List.of("applicant_user_id","object_id","object_version","action_code","tenant_id","instance_code")) {
            Object original=jdbc.queryForObject("SELECT "+field+" FROM bpm_form_action_instance WHERE id=901",Object.class);
            Object bad=field.endsWith("_id")?98: "INVALID";
            jdbc.update("UPDATE bpm_form_action_instance SET "+field+"=? WHERE id=901",bad);
            assertThrows(RuntimeException.class,()->reserveForm(901));jdbc.update("UPDATE bpm_form_action_instance SET "+field+"=? WHERE id=901",original);
        }
        jdbc.update("UPDATE dcc_controlled_file_master SET dcc_project_code_id=6 WHERE id=10");
        assertThrows(RuntimeException.class,()->reserveForm(901));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_application_round_link",Integer.class));
    }
    @Test void reserveAndBindRespectTheRealOuterTransactionRollback(){
        controlledFamily();
        assertThrows(IllegalStateException.class,()->tx(()->{ReflectionTestUtils.invokeMethod(rounds,"reserveObsoleteFormDraft",99L,20L,901L,"FORM-901");throw new IllegalStateException("late defaults save failed");}));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_application_round_link",Integer.class));
        int number=reserveForm(901);
        assertThrows(IllegalStateException.class,()->tx(()->{jdbc.update("UPDATE bpm_form_action_instance SET status='IN_APPROVAL',bpm_process_instance_id='bpm-A' WHERE id=901");
            ReflectionTestUtils.invokeMethod(rounds,"bindObsoleteFormDraft",99L,20L,901L,"FORM-901",number,"bpm-A");throw new IllegalStateException("late signed obligation failed");}));
        assertEquals(number,reserveForm(901));assertThrows(RuntimeException.class,()->rounds.require("OBSOLETE",20L,"bpm-A"));
    }
    @Test void concurrentSameFormSerializesExactlyOneRealReservation() throws Exception {
        controlledFamily();var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        var start=new java.util.concurrent.CountDownLatch(1);
        try{
            var work=(java.util.concurrent.Callable<Integer>)()->{TenantContextHolder.setTenantId(1L);authenticate();try{start.await();return reserveForm(901);}finally{TenantContextHolder.clear();SecurityContextHolder.clearContext();}};
            var one=pool.submit(work);var two=pool.submit(work);start.countDown();
            assertEquals(one.get(15,java.util.concurrent.TimeUnit.SECONDS),two.get(15,java.util.concurrent.TimeUnit.SECONDS));
            assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_application_round_link WHERE form_instance_id=901",Integer.class));
        }finally{pool.shutdownNow();}
    }
    @Test void realAuthenticationAndOuterTransactionAreRequiredAndOtherTenantNativeHistoryStaysUnchanged(){
        controlledFamily();
        jdbc.update("INSERT INTO dcc_application_round_link(tenant_id,project_id,application_type,application_id,bpm_round,attribute_round) VALUES(2,8,'UPLOAD',200,'old-real-bpm',7)");
        var before=jdbc.queryForList("SELECT * FROM dcc_application_round_link WHERE tenant_id=2");
        assertThrows(org.springframework.transaction.IllegalTransactionStateException.class,()->
                ReflectionTestUtils.invokeMethod(rounds,"reserveObsoleteFormDraft",99L,20L,901L,"FORM-901"));
        SecurityContextHolder.clearContext();
        assertThrows(RuntimeException.class,()->reserveForm(901));authenticate();
        var first=reserveForm(901);
        assertEquals(before,jdbc.queryForList("SELECT * FROM dcc_application_round_link WHERE tenant_id=2"));
        assertThrows(RuntimeException.class,()->tx(()->ReflectionTestUtils.invokeMethod(rounds,"requireObsoleteFormDraft",99L,20L,901L,"FORM-902")));
        assertEquals(first,reserveForm(901));
    }
}
