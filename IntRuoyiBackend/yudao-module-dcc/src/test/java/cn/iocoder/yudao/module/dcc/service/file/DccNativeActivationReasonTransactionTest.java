package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.system.service.controlledcontent.*;
import cn.iocoder.yudao.module.system.enums.controlledcontent.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.system.dal.mysql.controlledcontent.ControlledContentVersionRefMapper;
import cn.iocoder.yudao.module.system.dal.mysql.controlledcontent.ControlledContentTransitionAuditMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.*;
import javax.sql.DataSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Actual platform core/state machine/Mapper. Standardization/body grants remain upstream contracts. */
class DccNativeActivationReasonTransactionTest extends DccNativePlatformLifecycleTransactionTest {
    @ParameterizedTest @NullSource @ValueSource(strings={"AFTER_STANDARDIZATION_SIGNATURE","PRESET_DATE"})
    void actualPlatformStoresNewActivationReasonWithoutChangingTransitionIdentityOrReplayGuard(String mode){
        seedPendingPlatform();var jdbc=jdbc();var core=core();var tx=tx();var key=key();
        tx.executeWithoutResult(s->{
            retirePriorPlatformAndSaveDomainActivation(mode);
            var files=(DccControlledFileMapper)ReflectionTestUtils.getField(this,"fileMapper");
            var adapter=new DccControlledContentAdapter();ReflectionTestUtils.setField(adapter,"lifecycleCoreService",core);
            adapter.recordNativeActivation(files.selectById(2L),-1L,"DCC:1:2:approval-2:ACTIVATED");
        });
        var recorded=jdbc.queryForMap("SELECT * FROM controlled_content_transition_audit WHERE event_key='DCC:1:2:approval-2:ACTIVATED'");
        String expected="AFTER_STANDARDIZATION_SIGNATURE".equals(mode)?"DCC已签名实际生效且正式受控完成后日常启用"
                :"PRESET_DATE".equals(mode)?"DCC正式受控且预设日期到期后日常启用":"DCC已受控生效版本日常启用";
        assertEquals(expected,recorded.get("reason"));assertEquals("ACTIVATE_CONTROLLED",recorded.get("action"));
        assertEquals("CONTROLLED_PENDING_EFFECTIVE",recorded.get("from_status"));assertEquals("ACTIVE",recorded.get("to_status"));
        assertEquals(-1L,((Number)recorded.get("actor_id")).longValue());assertEquals("DCC_CONTROLLED_FILE",recorded.get("content_type"));
        assertEquals("10",recorded.get("content_key"));
        var all=jdbc.queryForList("SELECT * FROM controlled_content_transition_audit ORDER BY id");
        var file=jdbc.queryForMap("SELECT * FROM dcc_controlled_file WHERE id=2");
        var adapter=new DccControlledContentAdapter();ReflectionTestUtils.setField(adapter,"lifecycleCoreService",core);
        var files=(DccControlledFileMapper)ReflectionTestUtils.getField(this,"fileMapper");
        tx.executeWithoutResult(s->adapter.recordNativeActivation(files.selectById(2L),-1L,"DCC:1:2:approval-2:ACTIVATED"));
        assertEquals(all,jdbc.queryForList("SELECT * FROM controlled_content_transition_audit ORDER BY id"));
        assertEquals(file,jdbc.queryForMap("SELECT * FROM dcc_controlled_file WHERE id=2"));
        assertThrows(IllegalStateException.class,()->tx.executeWithoutResult(s->core.transitionDccControlledRef(key,2L,"B/1","approval-2",
                ControlledContentCanonicalStatus.CONTROLLED_PENDING_EFFECTIVE,ControlledContentCanonicalStatus.ACTIVE,
                ControlledContentTransitionAction.ACTIVATE_CONTROLLED,-1L,"changed payload reason","DCC:1:2:approval-2:ACTIVATED",null)));
        assertEquals(all,jdbc.queryForList("SELECT * FROM controlled_content_transition_audit ORDER BY id"));
    }
    @Test void completedLegacyTechnicalReplayKeepsOldReasonAndNeverCallsNewAdapterReason(){
        seedPendingPlatform();var jdbc=jdbc();var core=core();var tx=tx();var key=key();
        tx.executeWithoutResult(s->{retirePriorPlatformAndSaveDomainActivation(null);
            core.transitionDccControlledRef(key,2L,"B/1","approval-2",ControlledContentCanonicalStatus.CONTROLLED_PENDING_EFFECTIVE,
                    ControlledContentCanonicalStatus.ACTIVE,ControlledContentTransitionAction.ACTIVATE_CONTROLLED,-1L,
                    "DCC预设日期正式生效","DCC:1:2:approval-2:ACTIVATED",null);});
        var beforeAudit=jdbc.queryForList("SELECT * FROM controlled_content_transition_audit ORDER BY id");
        var beforeRef=jdbc.queryForList("SELECT * FROM controlled_content_version_ref ORDER BY id");
        var beforeFile=jdbc.queryForMap("SELECT * FROM dcc_controlled_file WHERE id=2");
        var lifecycle=(DccControlledFileLifecycleService)ReflectionTestUtils.getField(this,"service");
        var observer=spy(new DccControlledContentAdapter());ReflectionTestUtils.setField(observer,"lifecycleCoreService",core);
        ReflectionTestUtils.setField(lifecycle,"platformAdapter",observer);
        assertEquals(false,tx.execute(s->lifecycle.activateDue(2L)));
        var files=(DccControlledFileMapper)ReflectionTestUtils.getField(this,"fileMapper");
        var masters=(DccControlledFileMasterMapper)ReflectionTestUtils.getField(this,"masterMapper");
        tx.executeWithoutResult(s->lifecycle.completeControl(files.selectByIdAndTenantForUpdate(1L,2L),masters.selectByIdForUpdate(10L),99L));
        verify(observer,never()).recordNativeActivation(any(),any(),any());
        assertEquals(beforeAudit,jdbc.queryForList("SELECT * FROM controlled_content_transition_audit ORDER BY id"));
        assertEquals(beforeRef,jdbc.queryForList("SELECT * FROM controlled_content_version_ref ORDER BY id"));
        assertEquals(beforeFile,jdbc.queryForMap("SELECT * FROM dcc_controlled_file WHERE id=2"));
        assertEquals("DCC预设日期正式生效",jdbc.queryForObject("SELECT reason FROM controlled_content_transition_audit WHERE event_key='DCC:1:2:approval-2:ACTIVATED'",String.class));
        // A caller that bypasses completed-version replay must still encounter the unchanged core payload conflict.
        assertThrows(IllegalStateException.class,()->tx.executeWithoutResult(s->observer.recordNativeActivation(
                files.selectById(2L),-1L,"DCC:1:2:approval-2:ACTIVATED")));
        assertEquals(beforeAudit,jdbc.queryForList("SELECT * FROM controlled_content_transition_audit ORDER BY id"));
        assertEquals(beforeRef,jdbc.queryForList("SELECT * FROM controlled_content_version_ref ORDER BY id"));
        assertEquals(beforeFile,jdbc.queryForMap("SELECT * FROM dcc_controlled_file WHERE id=2"));
    }
    private void seedPendingPlatform(){ReflectionTestUtils.invokeMethod(this,"seedBoth",LocalDate.now().plusDays(1));ReflectionTestUtils.invokeMethod(this,"control");}
    private void retirePriorPlatformAndSaveDomainActivation(String mode){var now=LocalDateTime.now().withNano(0);var jdbc=jdbc();
        core().transitionDccControlledRef(key(),1L,"A/1","approval-1",ControlledContentCanonicalStatus.ACTIVE,ControlledContentCanonicalStatus.OBSOLETE,
                ControlledContentTransitionAction.OBSOLETE_ACTIVE,-1L,"新版生效自动作废","W06:OLD:OBSOLETE",2L);
        // These upstream facts are test-owned command inputs, not a substitute for real standardization validation.
        jdbc.update("UPDATE dcc_controlled_file SET status='OBSOLETE',superseded_by_file_id=2 WHERE id=1");
        jdbc.update("UPDATE dcc_controlled_file_master SET current_active_controlled_file_id=2 WHERE id=10");
        jdbc.update("UPDATE dcc_controlled_file SET status='ACTIVE',activated_time=?,effective_mode=?,effective_date=?,effective_time=?,effective_signature_id=?,effective_process_instance_id=? WHERE id=2",
                now,mode,"PRESET_DATE".equals(mode)?LocalDate.now():null,mode==null?null:now.minusMinutes(1),
                "AFTER_STANDARDIZATION_SIGNATURE".equals(mode)?700L:null,mode==null?null:"approval-2");
    }
    private JdbcTemplate jdbc(){return (JdbcTemplate)ReflectionTestUtils.getField(this,"actualJdbc");}
    private ControlledContentLifecycleCoreService core(){return (ControlledContentLifecycleCoreService)ReflectionTestUtils.getField(this,"core");}
    private ControlledContentKey key(){return (ControlledContentKey)ReflectionTestUtils.getField(this,"key");}
    private TransactionTemplate tx(){return new TransactionTemplate((PlatformTransactionManager)ReflectionTestUtils.getField(this,"actualTx"));}
}
