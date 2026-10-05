package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.bpm.formcenter.model.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.system.dal.mysql.controlledcontent.*;
import cn.iocoder.yudao.module.system.service.controlledcontent.*;
import cn.iocoder.yudao.module.system.enums.controlledcontent.*;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Approved native effect + actual pending platform tuple and audited H2 transaction. */
class DccPendingObsoletePlatformTransactionTest extends DccWorkflowObsoleteTransactionTest {
    @Resource ControlledContentVersionRefMapper refs;
    @Resource ControlledContentTransitionAuditMapper transitions;
    @Resource DccControlledFileMapper actualFiles;
    @Resource DccControlledFileMasterMapper actualMasters;
    private org.springframework.jdbc.core.JdbcTemplate pendingJdbc;
    private PlatformTransactionManager manager;
    private DccControlledFileObsoleteServiceImpl actualService;
    private ControlledContentLifecycleCoreService core;
    private ControlledContentKey key;

    @BeforeEach void actualPendingProjectionFixture() {
        pendingJdbc=(org.springframework.jdbc.core.JdbcTemplate)ReflectionTestUtils.getField(this,"jdbc");
        manager=(PlatformTransactionManager)ReflectionTestUtils.getField(this,"transactionManager");
        actualService=(DccControlledFileObsoleteServiceImpl)ReflectionTestUtils.getField(this,"service");
        pendingJdbc.execute("""
          CREATE TABLE IF NOT EXISTS controlled_content_version_ref(
          id BIGINT AUTO_INCREMENT PRIMARY KEY,tenant_id BIGINT,content_type VARCHAR(64),content_key VARCHAR(128),
          native_master_id BIGINT,native_version_id BIGINT,version_no VARCHAR(64),canonical_status VARCHAR(64),domain_status VARCHAR(128),
          source_version_ref_id BIGINT,source_native_version_id BIGINT,successor_version_ref_id BIGINT,successor_native_version_id BIGINT,
          active_unique_flag INT,open_candidate_unique_flag INT,approval_process_instance_id VARCHAR(128),last_transition_time TIMESTAMP,
          creator VARCHAR(64),updater VARCHAR(64),create_time TIMESTAMP,update_time TIMESTAMP,deleted BIT DEFAULT 0,
          UNIQUE(tenant_id,content_type,content_key,active_unique_flag),UNIQUE(tenant_id,content_type,content_key,open_candidate_unique_flag))
          """);
        pendingJdbc.execute("""
          CREATE TABLE IF NOT EXISTS controlled_content_transition_audit(
          id BIGINT AUTO_INCREMENT PRIMARY KEY,tenant_id BIGINT,version_ref_id BIGINT,content_type VARCHAR(64),content_key VARCHAR(128),
          from_status VARCHAR(64),to_status VARCHAR(64),domain_from_status VARCHAR(128),domain_to_status VARCHAR(128),action VARCHAR(64),
          event_key VARCHAR(128),actor_id BIGINT,reason VARCHAR(1024),create_time TIMESTAMP)
          """);
        pendingJdbc.update("DELETE FROM controlled_content_transition_audit");pendingJdbc.update("DELETE FROM controlled_content_version_ref");
        pendingJdbc.update("UPDATE dcc_controlled_file SET status='CONTROLLED_PENDING_EFFECTIVE',activated_time=NULL,effective_date=?,process_definition_key='dcc-controlled-file-upload' WHERE id=42",LocalDate.now().plusDays(7));
        pendingJdbc.update("UPDATE dcc_controlled_file_master SET current_active_controlled_file_id=NULL WHERE id=10");
        core=new ControlledContentLifecycleCoreService(refs,transitions,new ControlledContentStateMachine());
        key=ControlledContentKey.of(1L,ControlledContentType.DCC_CONTROLLED_FILE,"10");
        new TransactionTemplate(manager).executeWithoutResult(s->{
            core.createDccCandidateRef(key,10L,42L,"A/1","WORKING",null,99L,"actual isolated original candidate");
            core.transitionVersionRef(key,42L,ControlledContentCanonicalStatus.IN_REVIEW,"PENDING_MATRIX_REVIEW",ControlledContentTransitionAction.SUBMIT,99L,"submitted","upload-round");
            core.transitionVersionRefByDomainEvent(key,42L,ControlledContentCanonicalStatus.IN_REVIEW,ControlledContentCanonicalStatus.READY_TO_PUBLISH,"READY_TO_PUBLISH",ControlledContentTransitionAction.APPROVE,99L,"approved","upload-round","g60-approve");
            core.transitionVersionRefByDomainEvent(key,42L,ControlledContentCanonicalStatus.READY_TO_PUBLISH,ControlledContentCanonicalStatus.FINALIZING,"FINALIZING",ControlledContentTransitionAction.START_FINALIZATION,99L,"control","upload-round","g60-finalize");
            core.transitionDccControlledRef(key,42L,"A/1","upload-round",ControlledContentCanonicalStatus.FINALIZING,
                    ControlledContentCanonicalStatus.CONTROLLED_PENDING_EFFECTIVE,ControlledContentTransitionAction.COMPLETE_CONTROL,99L,"controlled","g60-control",null);
        });
        var adapter=new DccControlledContentAdapter();ReflectionTestUtils.setField(adapter,"lifecycleCoreService",core);
        ReflectionTestUtils.setField(actualService,"platformAdapter",adapter);
    }

    @AfterEach void clearShared() {if(pendingJdbc!=null){pendingJdbc.update("DELETE FROM controlled_content_transition_audit");pendingJdbc.update("DELETE FROM controlled_content_version_ref");}}

    @Test void approvedNativeEffectCanObsoleteControlledPendingVersionWithoutExecutingIt() {
        var before=actualFiles.selectById(42L);
        executeActualEffect();
        var after=actualFiles.selectById(42L);assertEquals("OBSOLETE",after.getStatus());assertNull(after.getActivatedTime());
        assertEquals(before.getControlledTime(),after.getControlledTime());assertEquals(before.getEffectiveDate(),after.getEffectiveDate());
        assertEquals("upload-round",after.getProcessInstanceId());
        var ref=core.getVersionRef(key,42L);assertEquals("OBSOLETE",ref.getCanonicalStatus());assertEquals("OBSOLETE",ref.getDomainStatus());
        assertNull(ref.getActiveUniqueFlag());assertNull(ref.getOpenCandidateUniqueFlag());assertEquals("upload-round",ref.getApprovalProcessInstanceId());
        assertEquals(42L,actualMasters.selectById(10L).getLatestControlledFileId());assertNull(actualMasters.selectById(10L).getCurrentActiveControlledFileId());
        assertEquals(after.getObsoletedTime().plusYears(20),pendingJdbc.queryForObject("SELECT retain_until FROM dcc_controlled_file_name_claim",java.time.LocalDateTime.class));
    }

    @Test void activeLockedPreimageUsesActiveActionAndExactEffectReplayWritesOnce() {
        new TransactionTemplate(manager).executeWithoutResult(s->core.transitionDccControlledRef(key,42L,"A/1","upload-round",
                ControlledContentCanonicalStatus.CONTROLLED_PENDING_EFFECTIVE,ControlledContentCanonicalStatus.ACTIVE,
                ControlledContentTransitionAction.ACTIVATE_CONTROLLED,-1L,"isolated actual effective date","g60-activation",null));
        pendingJdbc.update("UPDATE dcc_controlled_file SET status='ACTIVE',activated_time=CURRENT_TIMESTAMP WHERE id=42");
        pendingJdbc.update("UPDATE dcc_controlled_file_master SET current_active_controlled_file_id=42 WHERE id=10");
        executeActualEffect();executeActualEffect();
        assertEquals("OBSOLETE",core.getVersionRef(key,42L).getCanonicalStatus());
        assertEquals(1,pendingJdbc.queryForObject("SELECT COUNT(*) FROM controlled_content_transition_audit WHERE action='OBSOLETE_ACTIVE'",Integer.class));
        assertEquals(0,pendingJdbc.queryForObject("SELECT COUNT(*) FROM controlled_content_transition_audit WHERE action='OBSOLETE_CONTROLLED'",Integer.class));
        assertEquals(1,pendingJdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_obsolete_audit",Integer.class));
    }

    @Test void lateFailureAfterActualSharedTransitionRollsBackBothStatesRetentionAndAudits() {
        var nativeBefore=pendingJdbc.queryForMap("SELECT * FROM dcc_controlled_file WHERE id=42");
        var sharedBefore=pendingJdbc.queryForMap("SELECT * FROM controlled_content_version_ref WHERE native_version_id=42");
        int sharedAudits=pendingJdbc.queryForObject("SELECT COUNT(*) FROM controlled_content_transition_audit",Integer.class);
        assertThrows(IllegalStateException.class,()->new TransactionTemplate(manager).executeWithoutResult(s->{
            executeActualEffect();throw new IllegalStateException("ISOLATED_LATE_OBSOLETE_EFFECT_FAILURE");
        }));
        assertEquals(nativeBefore,pendingJdbc.queryForMap("SELECT * FROM dcc_controlled_file WHERE id=42"));
        assertEquals(sharedBefore,pendingJdbc.queryForMap("SELECT * FROM controlled_content_version_ref WHERE native_version_id=42"));
        assertEquals(sharedAudits,pendingJdbc.queryForObject("SELECT COUNT(*) FROM controlled_content_transition_audit",Integer.class));
        assertEquals(0,pendingJdbc.queryForObject("SELECT COUNT(*) FROM gxp_audit_event",Integer.class));
        assertEquals(0,pendingJdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_obsolete_audit",Integer.class));
        assertEquals(0,pendingJdbc.queryForObject("SELECT COUNT(*) FROM dcc_workflow_obsolete_archive",Integer.class));
        assertEquals(0,pendingJdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_message_job",Integer.class));
        assertNull(pendingJdbc.queryForObject("SELECT retain_until FROM dcc_controlled_file_name_claim",java.time.LocalDateTime.class));
    }

    private void executeActualEffect() {
        var context=BusinessActionContext.builder().tenantId(1L).dataDomain("DCC").systemCode("DCC").objectType("CONTROLLED_FILE")
                .objectId("42").objectVersion("A/1").objectState("CONTROLLED_PENDING_EFFECTIVE").actionCode("OBSOLETE").reason("批准作废").build();
        var instance=new FormActionInstance("G60-OBSOLETE",null,context,99L,"g60-native-obsolete");
        instance.setStatus(FormInstanceStatus.PENDING_EFFECT);instance.setBpmBinding(new FormBpmBinding("obsolete-round",null));
        instance.setFormData(Map.of("controlledFileId",42L,"reason","批准作废"));
        var executor=new DccControlledFileObsoleteFormEffectExecutor(actualService);
        new TransactionTemplate(manager).executeWithoutResult(s->{
            var result=executor.execute(instance,"g60-native-obsolete");
            if(!result.isSuccess())throw new IllegalStateException(result.getFailureReason());
        });
    }
}
