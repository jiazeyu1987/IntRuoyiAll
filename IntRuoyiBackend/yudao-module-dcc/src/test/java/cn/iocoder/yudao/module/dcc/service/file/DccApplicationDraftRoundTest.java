package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.context.jdbc.*;
import static org.junit.jupiter.api.Assertions.*;

@Import({DccApplicationRoundService.class,DccApplicationDraftRoundTest.JdbcConfig.class})
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql(scripts="/sql/dcc_b_combination_tables.sql",executionPhase=Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts="/sql/dcc_b_combination_clean.sql",executionPhase=Sql.ExecutionPhase.AFTER_TEST_METHOD)
class DccApplicationDraftRoundTest extends BaseDbUnitTest {
    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods=false)
    static class JdbcConfig {
        @org.springframework.context.annotation.Bean JdbcTemplate jdbcTemplate(javax.sql.DataSource ds){return new JdbcTemplate(ds);}
    }
    @Resource DccApplicationRoundService rounds;
    @Resource JdbcTemplate jdbc;
    @Resource PlatformTransactionManager transactions;
    @BeforeEach void fixture(){
        jdbc.update("INSERT INTO dcc_project_code(id,tenant_id,project_name,status,deleted) VALUES(5,1,'draft-round-project','ENABLE',0)");
        jdbc.update("INSERT INTO dcc_controlled_file(id,tenant_id,master_id,category_id,directory_id,source_file_id,original_file_id,file_name,title,file_number,version_no,submitter_id,requester_id,process_type,change_type,status,dcc_project_code_id,deleted) VALUES(20,1,10,2,3,100,100,'SOP.pdf','SOP','SOP-1','A/1-1',99,99,'CONTROLLED_FILE','NEW','WORKING',5,0)");
    }
    <T>T tx(java.util.function.Supplier<T> action){return new TransactionTemplate(transactions).execute(s->action.get());}
    int reserve(){return tx(()->rounds.reserveDraft(5L,"UPLOAD",20L));}
    @Test void reservePersistsRealUnboundDraftAndReplaysSameRound(){
        int n=reserve();assertEquals(n,reserve());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_application_round_link WHERE bpm_round IS NULL",Integer.class));
        assertEquals(n,rounds.requireDraft("UPLOAD",20L));
    }
    @Test void exactReservedRoundBindingKeepsSourceRoundAndRejectsDifferentBpm(){
        int n=reserve();
        int bound=tx(()->rounds.bindReservedDraft(5L,"UPLOAD",20L,n,"actual-bpm-opaque"));
        assertEquals(n,bound);assertEquals(n,rounds.require("UPLOAD",20L,"actual-bpm-opaque"));
        assertEquals(n,tx(()->rounds.bindReservedDraft(5L,"UPLOAD",20L,n,"actual-bpm-opaque")));
        assertThrows(RuntimeException.class,()->tx(()->rounds.bindReservedDraft(5L,"UPLOAD",20L,n,"changed-bpm")));
    }
    @Test void reserveAndBindingRollBackWithTheirParentTransaction(){
        assertThrows(IllegalStateException.class,()->tx(()->{rounds.reserveDraft(5L,"UPLOAD",20L);throw new IllegalStateException("真实File创建失败");}));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_application_round_link",Integer.class));
        int n=reserve();
        assertThrows(IllegalStateException.class,()->tx(()->{rounds.bindReservedDraft(5L,"UPLOAD",20L,n,"late-failure-bpm");throw new IllegalStateException("实际BPM提交失败");}));
        assertEquals(n,rounds.requireDraft("UPLOAD",20L));
        assertThrows(IllegalStateException.class,()->rounds.require("UPLOAD",20L,"late-failure-bpm"));
    }
    @Test void reservedDraftCannotBeBypassedByDirectBindAndOnlyExactIdentityCanBind(){
        int n=reserve();
        assertThrows(RuntimeException.class,()->tx(()->rounds.bind(5L,"UPLOAD",20L,"bypass-bpm")));
        assertThrows(RuntimeException.class,()->tx(()->rounds.bindReservedDraft(5L,"REVISION",20L,n,"wrong-action-bpm")));
        assertThrows(RuntimeException.class,()->tx(()->rounds.bindReservedDraft(5L,"UPLOAD",20L,n+1,"wrong-round-bpm")));
        assertThrows(RuntimeException.class,()->tx(()->rounds.bindReservedDraft(6L,"UPLOAD",20L,n,"wrong-project-bpm")));
        assertThrows(org.springframework.transaction.IllegalTransactionStateException.class,()->rounds.reserveDraft(5L,"UPLOAD",20L));
        TenantContextHolder.setTenantId(2L);
        try {assertThrows(RuntimeException.class,()->tx(()->rounds.reserveDraft(5L,"UPLOAD",20L)));assertThrows(RuntimeException.class,()->rounds.requireDraft("UPLOAD",20L));}
        finally {TenantContextHolder.setTenantId(1L);}
        assertEquals(n,rounds.requireDraft("UPLOAD",20L));assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_application_round_link",Integer.class));
    }
    @Test void boundRowsRemainBoundAndNewDraftGetsPersistentlyAllocatedNextRound(){
        int first=reserve();tx(()->rounds.bindReservedDraft(5L,"UPLOAD",20L,first,"first-bpm"));
        assertThrows(RuntimeException.class,()->rounds.requireDraft("UPLOAD",20L));
        int second=reserve();assertEquals(first+1,second);assertEquals(first,rounds.require("UPLOAD",20L,"first-bpm"));
        assertEquals(second,reserve());assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_application_round_link WHERE bpm_round IS NULL",Integer.class));
    }
    @Test void simultaneousReserveSerializesOnProjectAndCreatesOnlyOneOpenDraft() throws Exception {
        var pool=java.util.concurrent.Executors.newFixedThreadPool(3);var start=new java.util.concurrent.CountDownLatch(1);
        try {
            var futures=java.util.stream.IntStream.range(0,3).mapToObj(i->pool.submit(()->{
                TenantContextHolder.setTenantId(1L);
                try {start.await();return tx(()->rounds.reserveDraft(5L,"UPLOAD",20L));}
                finally{TenantContextHolder.clear();}
            })).toList();start.countDown();
            int expected=futures.get(0).get(20,java.util.concurrent.TimeUnit.SECONDS);
            for(var future:futures)assertEquals(expected,future.get(20,java.util.concurrent.TimeUnit.SECONDS));
            assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_application_round_link",Integer.class));
        }finally{pool.shutdownNow();}
    }
}
