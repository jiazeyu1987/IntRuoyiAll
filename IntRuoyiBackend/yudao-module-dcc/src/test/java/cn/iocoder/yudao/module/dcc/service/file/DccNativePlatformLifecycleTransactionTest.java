package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.system.dal.mysql.controlledcontent.ControlledContentVersionRefMapper;
import cn.iocoder.yudao.module.system.dal.mysql.controlledcontent.ControlledContentTransitionAuditMapper;
import cn.iocoder.yudao.module.system.service.controlledcontent.*;
import cn.iocoder.yudao.module.system.enums.controlledcontent.*;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

/** Native locked lifecycle + actual platform core/state machine/mapper on the same isolated transaction. */
class DccNativePlatformLifecycleTransactionTest extends DccWorkflowLifecycleTransactionTest {
    @Resource ControlledContentVersionRefMapper refs;
    @Resource ControlledContentTransitionAuditMapper transitions;
    private JdbcTemplate actualJdbc;
    private PlatformTransactionManager actualTx;
    private ControlledContentLifecycleCoreService core;
    private ControlledContentKey key;

    @BeforeEach void realPlatformFixture() {
        actualJdbc=(JdbcTemplate)ReflectionTestUtils.getField(this,"jdbc");
        actualTx=(PlatformTransactionManager)ReflectionTestUtils.getField(this,"transactionManager");
        actualJdbc.execute("""
          CREATE TABLE IF NOT EXISTS controlled_content_version_ref(
          id BIGINT AUTO_INCREMENT PRIMARY KEY,tenant_id BIGINT,content_type VARCHAR(64),content_key VARCHAR(128),
          native_master_id BIGINT,native_version_id BIGINT,version_no VARCHAR(64),canonical_status VARCHAR(64),domain_status VARCHAR(128),
          source_version_ref_id BIGINT,source_native_version_id BIGINT,successor_version_ref_id BIGINT,successor_native_version_id BIGINT,
          active_unique_flag INT,open_candidate_unique_flag INT,approval_process_instance_id VARCHAR(128),last_transition_time TIMESTAMP,
          creator VARCHAR(64),updater VARCHAR(64),create_time TIMESTAMP,update_time TIMESTAMP,deleted BIT DEFAULT 0,
          UNIQUE(tenant_id,content_type,content_key,active_unique_flag),UNIQUE(tenant_id,content_type,content_key,open_candidate_unique_flag))
          """);
        actualJdbc.execute("""
          CREATE TABLE IF NOT EXISTS controlled_content_transition_audit(
          id BIGINT AUTO_INCREMENT PRIMARY KEY,tenant_id BIGINT,version_ref_id BIGINT,content_type VARCHAR(64),content_key VARCHAR(128),
          from_status VARCHAR(64),to_status VARCHAR(64),domain_from_status VARCHAR(128),domain_to_status VARCHAR(128),action VARCHAR(64),
          event_key VARCHAR(128),actor_id BIGINT,reason VARCHAR(1024),create_time TIMESTAMP)
          """);
        actualJdbc.update("DELETE FROM controlled_content_transition_audit");actualJdbc.update("DELETE FROM controlled_content_version_ref");
        core=new ControlledContentLifecycleCoreService(refs,transitions,new ControlledContentStateMachine());
        key=ControlledContentKey.of(1L,ControlledContentType.DCC_CONTROLLED_FILE,"10");
        var adapter=new DccControlledContentAdapter();ReflectionTestUtils.setField(adapter,"lifecycleCoreService",core);
        ReflectionTestUtils.setField(ReflectionTestUtils.getField(this,"service"),"platformAdapter",adapter);
    }

    @AfterEach void clearPlatformRows() {
        if(actualJdbc!=null){actualJdbc.update("DELETE FROM controlled_content_transition_audit");actualJdbc.update("DELETE FROM controlled_content_version_ref");}
    }

    @Test void actualNativeControlClosesPlatformCandidateBeforeTheNextRevision() {
        seedBoth(LocalDate.now());
        ReflectionTestUtils.invokeMethod(this,"control");
        assertEquals("ACTIVE",core.getVersionRef(key,2L).getCanonicalStatus(),"Actual native control must complete its real platform candidate");
        assertNull(core.getVersionRef(key,2L).getOpenCandidateUniqueFlag());
        assertEquals("OBSOLETE",core.getVersionRef(key,1L).getCanonicalStatus());
        new TransactionTemplate(actualTx).executeWithoutResult(s->core.createCandidateRef(key,10L,3L,"B/2","WORKING",
                core.getVersionRef(key,2L).getId(),2L,99L,"next real candidate"));
    }

    private void seedBoth(LocalDate date) {
        ReflectionTestUtils.invokeMethod(this,"seed",date);
        new TransactionTemplate(actualTx).executeWithoutResult(s->{
            var active=core.registerActiveRef(key,10L,1L,"A/1","ACTIVE",99L,"isolated actual baseline");
            actualJdbc.update("UPDATE controlled_content_version_ref SET approval_process_instance_id='approval-1' WHERE native_version_id=1");
            core.createCandidateRef(key,10L,2L,"B/1","READY_TO_PUBLISH",active.getId(),1L,99L,"isolated actual candidate");
            core.transitionVersionRef(key,2L,ControlledContentCanonicalStatus.IN_REVIEW,"PENDING_MATRIX_REVIEW",ControlledContentTransitionAction.SUBMIT,99L,"submit","approval-2");
            core.transitionVersionRefByDomainEvent(key,2L,ControlledContentCanonicalStatus.IN_REVIEW,ControlledContentCanonicalStatus.READY_TO_PUBLISH,"READY_TO_PUBLISH",ControlledContentTransitionAction.APPROVE,99L,"approved","approval-2","g59-approve");
            core.transitionVersionRefByDomainEvent(key,2L,ControlledContentCanonicalStatus.READY_TO_PUBLISH,ControlledContentCanonicalStatus.FINALIZING,"FINALIZING",ControlledContentTransitionAction.START_FINALIZATION,99L,"begin control","approval-2","g59-finalizing");
        });
    }

    @Test void futureControlClosesOnlyOpenFlagAndNextCandidateUsesExactPendingSource() {
        seedBoth(LocalDate.now().plusDays(2));ReflectionTestUtils.invokeMethod(this,"control");
        var pending=core.getVersionRef(key,2L);assertEquals("CONTROLLED_PENDING_EFFECTIVE",pending.getCanonicalStatus());
        assertNull(pending.getOpenCandidateUniqueFlag());assertNull(pending.getActiveUniqueFlag());
        assertEquals(1L,core.getActiveRef(key).getNativeVersionId());
        new TransactionTemplate(actualTx).executeWithoutResult(s->{
            var next=core.createDccCandidateRef(key,10L,3L,"B/2","WORKING",2L,99L,"exact latest pending source");
            assertEquals(2L,next.getSourceNativeVersionId());assertEquals(pending.getId(),next.getSourceVersionRefId());
        });
        assertThrows(IllegalStateException.class,()->new TransactionTemplate(actualTx).execute(s->
                core.createDccCandidateRef(key,10L,4L,"B/3","WORKING",2L,99L,"another open candidate")));
    }

    @Test void dueActivationProjectsObsoleteAndActiveAndLateFailureRollsBackBothTables() {
        seedBoth(LocalDate.now().plusDays(1));ReflectionTestUtils.invokeMethod(this,"control");
        actualJdbc.update("UPDATE dcc_controlled_file SET effective_date=? WHERE id=2",LocalDate.now());
        var lifecycle=(DccControlledFileLifecycleService)ReflectionTestUtils.getField(this,"service");
        int before=actualJdbc.queryForObject("SELECT COUNT(*) FROM controlled_content_transition_audit",Integer.class);
        ReflectionTestUtils.setField(lifecycle,"eventPublisher",(org.springframework.context.ApplicationEventPublisher)event->{
            if(event instanceof DccControlledFileLifecycleEvent e && "ACTIVATED".equals(e.eventType()))throw new IllegalStateException("ISOLATED_LATE_ACTIVATION_FAILURE");
        });
        assertThrows(IllegalStateException.class,()->new TransactionTemplate(actualTx).execute(s->lifecycle.activateDue(2L)));
        assertEquals("ACTIVE",core.getVersionRef(key,1L).getCanonicalStatus());assertEquals("CONTROLLED_PENDING_EFFECTIVE",core.getVersionRef(key,2L).getCanonicalStatus());
        assertEquals(before,actualJdbc.queryForObject("SELECT COUNT(*) FROM controlled_content_transition_audit",Integer.class));
        ReflectionTestUtils.setField(lifecycle,"eventPublisher",(org.springframework.context.ApplicationEventPublisher)event->{});
        assertEquals(true,new TransactionTemplate(actualTx).execute(s->lifecycle.activateDue(2L)));
        assertEquals("OBSOLETE",core.getVersionRef(key,1L).getCanonicalStatus());assertEquals(2L,core.getVersionRef(key,1L).getSuccessorNativeVersionId());
        assertEquals("ACTIVE",core.getVersionRef(key,2L).getCanonicalStatus());
        assertEquals(false,new TransactionTemplate(actualTx).execute(s->lifecycle.activateDue(2L)));
    }

    @Test void exactControlledEventReplayIsZeroWriteAndChangedPayloadIsRejected() {
        seedBoth(LocalDate.now().plusDays(1));ReflectionTestUtils.invokeMethod(this,"control");
        int before=actualJdbc.queryForObject("SELECT COUNT(*) FROM controlled_content_transition_audit",Integer.class);
        String event="DCC:1:2:approval-2:CONTROLLED";
        new TransactionTemplate(actualTx).executeWithoutResult(s->core.transitionDccControlledRef(key,2L,"B/1","approval-2",
                ControlledContentCanonicalStatus.FINALIZING,ControlledContentCanonicalStatus.CONTROLLED_PENDING_EFFECTIVE,
                ControlledContentTransitionAction.COMPLETE_CONTROL,99L,"DCC正式受控完成",event,null));
        assertEquals(before,actualJdbc.queryForObject("SELECT COUNT(*) FROM controlled_content_transition_audit",Integer.class));
        assertThrows(IllegalStateException.class,()->new TransactionTemplate(actualTx).execute(s->core.transitionDccControlledRef(key,2L,"B/1","approval-2",
                ControlledContentCanonicalStatus.FINALIZING,ControlledContentCanonicalStatus.CONTROLLED_PENDING_EFFECTIVE,
                ControlledContentTransitionAction.COMPLETE_CONTROL,99L,"changed reason",event,null)));
        assertThrows(IllegalArgumentException.class,()->core.createDccCandidateRef(ControlledContentKey.of(1L,ControlledContentType.MES_ROUTE,"10"),
                10L,3L,"B/2","WORKING",2L,99L,"foreign module"));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"DOMAIN","OPEN_FLAG","ACTIVE_FLAG"})
    void corruptedUnfinishedProjectionIsRejectedWithoutSilentFlagRepair(String corruption) {
        seedBoth(LocalDate.now().plusDays(1));
        switch(corruption) {
            case "DOMAIN"->actualJdbc.update("UPDATE controlled_content_version_ref SET domain_status='ACTIVE' WHERE native_version_id=2");
            case "OPEN_FLAG"->actualJdbc.update("UPDATE controlled_content_version_ref SET open_candidate_unique_flag=NULL WHERE native_version_id=2");
            case "ACTIVE_FLAG"->actualJdbc.update("UPDATE controlled_content_version_ref SET active_unique_flag=2 WHERE native_version_id=2");
        }
        var before=actualJdbc.queryForList("SELECT * FROM controlled_content_version_ref ORDER BY id");
        assertThrows(IllegalStateException.class,()->ReflectionTestUtils.invokeMethod(this,"control"));
        assertEquals(before,actualJdbc.queryForList("SELECT * FROM controlled_content_version_ref ORDER BY id"));
    }

    @Test void corruptedControlledSourceAndWrongMasterSuccessorCannotAllocateOrRetire() {
        seedBoth(LocalDate.now().plusDays(1));ReflectionTestUtils.invokeMethod(this,"control");
        actualJdbc.update("UPDATE controlled_content_version_ref SET domain_status='FINALIZING' WHERE native_version_id=2");
        assertThrows(IllegalStateException.class,()->new TransactionTemplate(actualTx).execute(s->core.createDccCandidateRef(
                key,10L,3L,"B/2","WORKING",2L,99L,"corrupted pending source")));
        actualJdbc.update("UPDATE controlled_content_version_ref SET domain_status='CONTROLLED_PENDING_EFFECTIVE',native_master_id=999 WHERE native_version_id=2");
        assertThrows(IllegalStateException.class,()->new TransactionTemplate(actualTx).execute(s->core.transitionDccControlledRef(
                key,1L,"A/1","approval-1",ControlledContentCanonicalStatus.ACTIVE,ControlledContentCanonicalStatus.OBSOLETE,
                ControlledContentTransitionAction.OBSOLETE_ACTIVE,-1L,"real higher version","g59-invalid-successor",2L)));
        assertEquals("ACTIVE",core.getVersionRef(key,1L).getCanonicalStatus());
    }
}
