package cn.iocoder.yudao.module.dcc.service.projectcode;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/** 同正式B服务/Mapper/H2配合真实预留轮次，不包含A实际创建事务接线。 */
class DccProjectReservedDraftCombinationTest extends DccProjectFormalCombinationTest {
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    cn.iocoder.yudao.module.dcc.service.file.DccApplicationRoundService roundSpy;
    @ParameterizedTest @ValueSource(strings={"UPLOAD","REVISION","OBSOLETE"})
    void realUnboundFileDraftKeepsOriginalDefaultsThroughManualSaveAndExactBpmSubmit(String type){
        var project=project("REAL-DRAFT-"+type,7L);var file=file(project.getId(),101L,type+"-draft.pdf");
        String fileBefore=JsonUtils.toJsonString(files.selectById(file.getId()));
        var draft=tx(()->snapshots.prepareDraft(7L,project.getId(),type,file.getId(),value("CE")));
        assertFalse(draft.getSubmitted());assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_application_round_link WHERE bpm_round IS NULL",Integer.class));
        project.setDefaultAttributesJson(attributes.encode(value("FDA")));projects.updateById(project);
        assertEquals(draft.getId(),tx(()->snapshots.prepareDraft(7L,project.getId(),type,file.getId(),null)).getId());
        tx(()->snapshots.saveReservedDraft(7L,project.getId(),type,file.getId(),value("MADSAP")));
        var saved=snapshots.readReservedDraft(7L,project.getId(),type,file.getId());
        assertEquals(value("NMPA"),attributes.readValue(saved.getDefaultSourceJson()));assertEquals(value("MADSAP"),attributes.readValue(saved.getActualAttributesJson()));
        var frozen=tx(()->snapshots.submitReservedDraft(7L,project.getId(),type,file.getId(),saved.getApplicationRound(),"actual-bpm-"+type,value("MADSAP")));
        assertEquals(draft.getId(),frozen.getId());assertTrue(frozen.getSubmitted());
        assertEquals(value("NMPA"),attributes.readValue(frozen.getDefaultSourceJson()));
        assertEquals(fileBefore,JsonUtils.toJsonString(files.selectById(file.getId())));
        assertEquals(frozen.getId(),tx(()->snapshots.submitReservedDraft(7L,project.getId(),type,file.getId(),saved.getApplicationRound(),"actual-bpm-"+type,value("MADSAP"))).getId());
        assertThrows(RuntimeException.class,()->tx(()->snapshots.submitReservedDraft(7L,project.getId(),type,file.getId(),saved.getApplicationRound(),"actual-bpm-"+type,value("CE"))));
    }
    @Test void successiveCrossFileReworksCopySavedSourceAndActualAndProtectOriginalSnapshots(){
        var project=project("CROSS-FILE",7L);var source=file(project.getId(),101L,"source.pdf");
        var original=tx(()->snapshots.submit(7L,project.getId(),"UPLOAD",source.getId(),"original-bpm",value("CE")));
        project.setDefaultAttributesJson(attributes.encode(value("FDA")));projects.updateById(project);
        var target=file(project.getId(),101L,"target.pdf");int round=tx(()->rounds.reserveDraft(project.getId(),"UPLOAD",target.getId()));
        var copied=tx(()->snapshots.forkToNewApplication(7L,project.getId(),"UPLOAD",source.getId(),original.getApplicationRound(),target.getId(),round));
        assertNotEquals(original.getApplicationId(),copied.getApplicationId());assertFalse(copied.getSubmitted());
        assertEquals(original.getDefaultSourceJson(),copied.getDefaultSourceJson());assertEquals(original.getActualAttributesJson(),copied.getActualAttributesJson());
        tx(()->snapshots.saveReservedDraft(7L,project.getId(),"UPLOAD",target.getId(),value("MADSAP")));
        assertEquals(copied.getId(),tx(()->snapshots.forkToNewApplication(7L,project.getId(),"UPLOAD",source.getId(),original.getApplicationRound(),target.getId(),round)).getId());
        tx(()->snapshots.submitReservedDraft(7L,project.getId(),"UPLOAD",target.getId(),round,"second-bpm",value("MADSAP")));
        var next=file(project.getId(),101L,"next.pdf");int nextRound=tx(()->rounds.reserveDraft(project.getId(),"UPLOAD",next.getId()));
        var nextCopy=tx(()->snapshots.forkToNewApplication(7L,project.getId(),"UPLOAD",target.getId(),round,next.getId(),nextRound));
        assertEquals(original.getDefaultSourceJson(),nextCopy.getDefaultSourceJson());assertEquals(value("MADSAP"),attributes.readValue(nextCopy.getActualAttributesJson()));
        assertEquals(original.getActualAttributesJson(),snapshots.read(7L,project.getId(),"UPLOAD",source.getId(),"original-bpm").getActualAttributesJson());
        assertThrows(RuntimeException.class,()->tx(()->snapshots.forkToNewApplication(7L,project.getId(),"UPLOAD",source.getId(),original.getApplicationRound(),next.getId(),nextRound)));
    }
    @Test void lateFailureRollsBackReservationAndCopyWithoutInventingTodayDefaults(){
        var project=project("DRAFT-LATE",7L);var source=file(project.getId(),101L,"late-source.pdf");
        var original=tx(()->snapshots.submit(7L,project.getId(),"REVISION",source.getId(),"source-bpm",value("CE")));
        var target=file(project.getId(),101L,"late-target.pdf");
        assertThrows(IllegalStateException.class,()->tx(()->{int round=rounds.reserveDraft(project.getId(),"REVISION",target.getId());
            snapshots.forkToNewApplication(7L,project.getId(),"REVISION",source.getId(),original.getApplicationRound(),target.getId(),round);
            throw new IllegalStateException("返工业务晚失败");}));
        assertThrows(RuntimeException.class,()->rounds.requireDraft("REVISION",target.getId()));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_application_attributes",Integer.class));
        assertThrows(RuntimeException.class,()->tx(()->snapshots.saveReservedDraft(7L,project.getId(),"REVISION",target.getId(),value("FDA"))));
        TenantContextHolder.setTenantId(2L);
        try{assertThrows(RuntimeException.class,()->tx(()->snapshots.prepareDraft(7L,project.getId(),"REVISION",target.getId(),value("CE"))));}
        finally{TenantContextHolder.setTenantId(1L);}
    }
    @Test void unboundSubmittedAttributeRowsWithoutFormalSourceRoundCannotBeForked(){
        var project=project("UNMAPPED-SOURCE",7L);var source=file(project.getId(),101L,"unmapped.pdf");var target=file(project.getId(),101L,"fork.pdf");
        attributes.saveDraft(7L,project.getId(),"UPLOAD",source.getId(),9,value("CE"));attributes.freeze(7L,project.getId(),"UPLOAD",source.getId(),9);
        int round=tx(()->rounds.reserveDraft(project.getId(),"UPLOAD",target.getId()));
        assertThrows(RuntimeException.class,()->tx(()->snapshots.forkToNewApplication(7L,project.getId(),"UPLOAD",source.getId(),9,target.getId(),round)),
                "源数字轮次必须来自正式Root BPM映射，不能仅相信任意submitted属性行");
    }
    @Test void exactForkReplayAfterTargetBpmBindingReturnsFrozenTargetWithoutOverwritingItsManualActual(){
        var project=project("FORK-BOUND-REPLAY",7L);var source=file(project.getId(),101L,"bound-source.pdf");var target=file(project.getId(),101L,"bound-target.pdf");
        var original=tx(()->snapshots.submit(7L,project.getId(),"REVISION",source.getId(),"source-bpm",value("CE")));
        int round=tx(()->rounds.reserveDraft(project.getId(),"REVISION",target.getId()));
        var copy=tx(()->snapshots.forkToNewApplication(7L,project.getId(),"REVISION",source.getId(),original.getApplicationRound(),target.getId(),round));
        tx(()->snapshots.submitReservedDraft(7L,project.getId(),"REVISION",target.getId(),round,"target-bpm",value("MADSAP")));
        var replay=tx(()->snapshots.forkToNewApplication(7L,project.getId(),"REVISION",source.getId(),original.getApplicationRound(),target.getId(),round));
        assertEquals(copy.getId(),replay.getId());assertTrue(replay.getSubmitted());assertEquals(value("MADSAP"),attributes.readValue(replay.getActualAttributesJson()));
    }
    @Test void alreadyBoundTargetWithoutCopiedSnapshotCannotBeInitializedAsReworkDraft(){
        var project=project("FORK-BOUND-NO-SOURCE",7L);var source=file(project.getId(),101L,"source-bound.pdf");var target=file(project.getId(),101L,"target-bound.pdf");
        var original=tx(()->snapshots.submit(7L,project.getId(),"UPLOAD",source.getId(),"source-bpm",value("CE")));
        int bound=tx(()->rounds.bind(project.getId(),"UPLOAD",target.getId(),"target-submitted-bpm"));
        assertThrows(RuntimeException.class,()->tx(()->snapshots.forkToNewApplication(7L,project.getId(),"UPLOAD",source.getId(),original.getApplicationRound(),target.getId(),bound)),
                "只有已存在同源目标可在绑定后重放，不能补建已送审目标草稿");
    }
    @Test void forkOpenDraftCheckAndCopyHoldProjectLockAgainstConcurrentBpmBinding() throws Exception {
        var project=project("FORK-BIND-RACE",7L);var source=file(project.getId(),101L,"race-source.pdf");var target=file(project.getId(),101L,"race-target.pdf");
        var original=tx(()->snapshots.submit(7L,project.getId(),"REVISION",source.getId(),"race-source-bpm",value("CE")));
        int targetRound=tx(()->rounds.reserveDraft(project.getId(),"REVISION",target.getId()));
        var checked=new java.util.concurrent.CountDownLatch(1);var release=new java.util.concurrent.CountDownLatch(1);
        org.mockito.Mockito.doAnswer(call->{
            int allocated=(Integer)call.callRealMethod();
            checked.countDown();
            if(!release.await(10,java.util.concurrent.TimeUnit.SECONDS))throw new IllegalStateException("测试返工锁等待超时");
            return allocated;
        }).when(roundSpy).requireDraft("REVISION",target.getId());
        var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var copying=pool.submit(()->{
                TenantContextHolder.setTenantId(1L);login(7L);
                try{return tx(()->snapshots.forkToNewApplication(7L,project.getId(),"REVISION",source.getId(),original.getApplicationRound(),target.getId(),targetRound));}
                finally{TenantContextHolder.clear();org.springframework.security.core.context.SecurityContextHolder.clearContext();}
            });
            assertTrue(checked.await(10,java.util.concurrent.TimeUnit.SECONDS));
            var binding=pool.submit(()->{
                TenantContextHolder.setTenantId(1L);
                try{return tx(()->rounds.bindReservedDraft(project.getId(),"REVISION",target.getId(),targetRound,"race-target-bpm"));}
                finally{TenantContextHolder.clear();}
            });
            assertThrows(java.util.concurrent.TimeoutException.class,()->binding.get(300,java.util.concurrent.TimeUnit.MILLISECONDS),
                    "真实BPM绑定必须等待返工快照事务，不能在来源检查与首次复制之间提前提交");
            release.countDown();
            var copied=copying.get(10,java.util.concurrent.TimeUnit.SECONDS);
            assertEquals(targetRound,binding.get(10,java.util.concurrent.TimeUnit.SECONDS));
            assertFalse(copied.getSubmitted());assertEquals(original.getDefaultSourceJson(),copied.getDefaultSourceJson());
            assertEquals(original.getActualAttributesJson(),copied.getActualAttributesJson());
            assertEquals(targetRound,rounds.require("REVISION",target.getId(),"race-target-bpm"));
        } finally {release.countDown();pool.shutdownNow();}
    }
}
