package cn.iocoder.yudao.module.dcc.service.projectcode;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectApplicationAttributesDO;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** H06: real B snapshots, Root round mappings and real File/Master persisted in isolated H2. */
class DccProjectUnsubmittedInheritanceTest extends DccProjectFormalCombinationTest {
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    cn.iocoder.yudao.module.dcc.service.file.DccApplicationRoundService roundSpy;
    DccControlledFileDO working(Long projectId, Long masterId, String type, Long predecessor) {
        var row=DccControlledFileDO.builder().tenantId(1L).masterId(masterId).dccProjectCodeId(projectId)
                .categoryId(1L).directoryId(101L).sourceFileId(1L).originalFileId(1L)
                .fileName("draft.pdf").title("draft").fileNumber("H06").versionNo("A/1")
                .status("WORKING").changeType("REVISION".equals(type)?"REVISION":"NEW")
                .requesterId(7L).submitterId(7L).predecessorControlledFileId(predecessor).build();
        row.setVersionNo(predecessor==null?"A/1":"A/1-1");
        assertEquals(1,files.insert(row));return row;
    }
    DccControlledFileDO source(Long projectId,String type) {
        var master=cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO.builder()
                .tenantId(1L).dccProjectCodeId(projectId).categoryId(1L).directoryId(101L)
                .fileName("draft.pdf").fileNumber("H06").status("ACTIVE_CHAIN").build();
        assertEquals(1,masters.insert(master));return working(projectId,master.getId(),type,null);
    }
    DccProjectApplicationAttributesDO inherit(Long projectId,String type,Long sourceId,Integer sourceRound,
                                               Long targetId,Integer targetRound) {
        return snapshots.inheritReservedDraftToNewApplication(7L,projectId,type,sourceId,sourceRound,targetId,targetRound);
    }
    @ParameterizedTest @ValueSource(strings={"UPLOAD","REVISION"})
    void h06TwoSuccessiveRealFilesInheritSavedSourceAndActualAfterProjectDefaultsChange(String type) {
        var project=project("H06-CHAIN-"+type,7L);var source=source(project.getId(),type);
        var saved=tx(()->snapshots.prepareDraft(7L,project.getId(),type,source.getId(),value("CE")));
        String sourceFileBefore=JsonUtils.toJsonString(files.selectById(source.getId()));
        String sourceSnapshotBefore=JsonUtils.toJsonString(saved);
        project.setDefaultAttributesJson(attributes.encode(value("FDA")));projects.updateById(project);
        var previous=source;var previousSaved=saved;
        for(int step=0;step<2;step++) {
            var target=working(project.getId(),source.getMasterId(),type,previous.getId());
            if(step==1) {
                target.setVersionNo("UPLOAD".equals(type)?"A/1":"A/2");
                target.setRevisionChangeType("UPLOAD".equals(type)?"INITIAL":"PARTIAL");
                target.setSelectedIterationControlledFileId(previous.getId());assertEquals(1,files.updateById(target));
            }
            String targetFileBefore=JsonUtils.toJsonString(files.selectById(target.getId()));
            String previousSnapshotBefore=JsonUtils.toJsonString(snapshots.readReservedDraft(7L,project.getId(),type,previous.getId()));
            int targetRound=tx(()->rounds.reserveDraft(project.getId(),type,target.getId()));
            Long from=previous.getId();int fromRound=previousSaved.getApplicationRound();
            var copied=tx(()->inherit(project.getId(),type,from,fromRound,target.getId(),targetRound));
            assertEquals(value("NMPA"),attributes.readValue(copied.getDefaultSourceJson()));
            assertEquals(value("CE"),attributes.readValue(copied.getActualAttributesJson()));
            assertEquals(from,copied.getSourceApplicationId());assertEquals(fromRound,copied.getSourceApplicationRound());
            assertFalse(copied.getSubmitted());assertEquals(targetRound,rounds.requireDraft(type,target.getId()));
            assertEquals(copied.getId(),snapshots.readReservedDraft(7L,project.getId(),type,target.getId()).getId());
            assertEquals(targetFileBefore,JsonUtils.toJsonString(files.selectById(target.getId())));
            assertEquals(previousSnapshotBefore,JsonUtils.toJsonString(snapshots.readReservedDraft(7L,project.getId(),type,from)));
            previous=target;previousSaved=copied;
        }
        assertEquals(sourceSnapshotBefore,JsonUtils.toJsonString(snapshots.readReservedDraft(7L,project.getId(),type,source.getId())));
        assertEquals(sourceFileBefore,JsonUtils.toJsonString(files.selectById(source.getId())));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_application_round_link WHERE bpm_round IS NOT NULL",Integer.class));
    }

    @Test void h06ExactSourceReplayPreservesTargetManualActualBeforeAndAfterRealBpmBinding() {
        var project=project("H06-REPLAY",7L);var source=source(project.getId(),"UPLOAD");
        var saved=tx(()->snapshots.prepareDraft(7L,project.getId(),"UPLOAD",source.getId(),value("CE")));
        var target=working(project.getId(),source.getMasterId(),"UPLOAD",source.getId());
        int targetRound=tx(()->rounds.reserveDraft(project.getId(),"UPLOAD",target.getId()));
        var copy=tx(()->inherit(project.getId(),"UPLOAD",source.getId(),saved.getApplicationRound(),target.getId(),targetRound));
        tx(()->snapshots.saveReservedDraft(7L,project.getId(),"UPLOAD",target.getId(),value("MADSAP")));
        var replay=tx(()->inherit(project.getId(),"UPLOAD",source.getId(),saved.getApplicationRound(),target.getId(),targetRound));
        assertEquals(copy.getId(),replay.getId());assertEquals(value("MADSAP"),attributes.readValue(replay.getActualAttributesJson()));
        tx(()->{
            var frozen=snapshots.submitReservedDraft(7L,project.getId(),"UPLOAD",target.getId(),targetRound,"real-target-bpm",value("MADSAP"));
            assertEquals(1,files.updateById(DccControlledFileDO.builder().id(target.getId()).status("PENDING_REVIEW")
                    .processInstanceId("real-target-bpm").build()));return frozen;
        });
        String frozenBefore=JsonUtils.toJsonString(snapshots.read(7L,project.getId(),"UPLOAD",target.getId(),"real-target-bpm"));
        var frozenReplay=tx(()->inherit(project.getId(),"UPLOAD",source.getId(),saved.getApplicationRound(),target.getId(),targetRound));
        assertEquals(frozenBefore,JsonUtils.toJsonString(frozenReplay));assertTrue(frozenReplay.getSubmitted());
        assertEquals(JsonUtils.toJsonString(saved),JsonUtils.toJsonString(snapshots.readReservedDraft(7L,project.getId(),"UPLOAD",source.getId())));
        var other=working(project.getId(),source.getMasterId(),"UPLOAD",source.getId());
        var otherSaved=tx(()->snapshots.prepareDraft(7L,project.getId(),"UPLOAD",other.getId(),value("FDA")));
        assertThrows(RuntimeException.class,()->tx(()->inherit(project.getId(),"UPLOAD",other.getId(),otherSaved.getApplicationRound(),target.getId(),targetRound)));
        assertEquals(frozenBefore,JsonUtils.toJsonString(snapshots.read(7L,project.getId(),"UPLOAD",target.getId(),"real-target-bpm")));
    }

    @ParameterizedTest @ValueSource(strings={"SOURCE_TENANT","TARGET_TENANT","SOURCE_PROJECT","TARGET_PROJECT",
            "MASTER_MISMATCH","MASTER_MISSING","MASTER_TENANT","MASTER_PROJECT","SOURCE_REQUESTER","TARGET_REQUESTER",
            "USER_DISABLED","NO_PROJECT_SCOPE","SOURCE_ACTION","TARGET_ACTION","SOURCE_NOT_WORKING","SOURCE_FILE_BPM",
            "SOURCE_MAPPING_BOUND","SOURCE_SUBMITTED","SOURCE_MISSING_ATTRIBUTES","SOURCE_BAD_ATTRIBUTES",
            "SOURCE_FILE_MISSING","TARGET_FILE_MISSING","SOURCE_ROUND","TARGET_ROUND","TYPE_MISMATCH","TARGET_MAPPING_BOUND","TARGET_FILE_BPM",
            "SOURCE_MAPPING_MISSING","TARGET_MAPPING_MISSING","SOURCE_SNAPSHOT_PROJECT","SOURCE_SNAPSHOT_TENANT","CALLER_TENANT"})
    void h06InvalidFormalIdentityOrSourceCannotCopyOrFallbackToTodaysDefaults(String violation) {
        var project=project("H06-INVALID",7L);var source=source(project.getId(),"UPLOAD");
        var saved=tx(()->snapshots.prepareDraft(7L,project.getId(),"UPLOAD",source.getId(),value("CE")));
        var target=working(project.getId(),source.getMasterId(),"UPLOAD",source.getId());
        int targetRound=tx(()->rounds.reserveDraft(project.getId(),"UPLOAD",target.getId()));
        final int reservedTargetRound=targetRound;
        project.setDefaultAttributesJson(attributes.encode(value("FDA")));projects.updateById(project);
        String type="UPLOAD";int sourceRound=saved.getApplicationRound();
        switch(violation) {
            case "SOURCE_TENANT" -> jdbc.update("UPDATE dcc_controlled_file SET tenant_id=2 WHERE id=?",source.getId());
            case "TARGET_TENANT" -> jdbc.update("UPDATE dcc_controlled_file SET tenant_id=2 WHERE id=?",target.getId());
            case "SOURCE_PROJECT" -> jdbc.update("UPDATE dcc_controlled_file SET dcc_project_code_id=999 WHERE id=?",source.getId());
            case "TARGET_PROJECT" -> jdbc.update("UPDATE dcc_controlled_file SET dcc_project_code_id=999 WHERE id=?",target.getId());
            case "MASTER_MISMATCH" -> jdbc.update("UPDATE dcc_controlled_file SET master_id=999 WHERE id=?",target.getId());
            case "MASTER_MISSING" -> masters.deleteById(source.getMasterId());
            case "MASTER_TENANT" -> jdbc.update("UPDATE dcc_controlled_file_master SET tenant_id=2 WHERE id=?",source.getMasterId());
            case "MASTER_PROJECT" -> jdbc.update("UPDATE dcc_controlled_file_master SET dcc_project_code_id=999 WHERE id=?",source.getMasterId());
            case "SOURCE_REQUESTER" -> jdbc.update("UPDATE dcc_controlled_file SET requester_id=8 WHERE id=?",source.getId());
            case "TARGET_REQUESTER" -> jdbc.update("UPDATE dcc_controlled_file SET requester_id=8 WHERE id=?",target.getId());
            case "USER_DISABLED" -> org.mockito.Mockito.when(users.getUser(7L)).thenReturn(new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO().setId(7L).setStatus(1));
            case "NO_PROJECT_SCOPE" -> rules.deleteById(rules.selectActiveRules(project.getId(),java.time.LocalDateTime.now()).get(0).getId());
            case "SOURCE_ACTION" -> jdbc.update("UPDATE dcc_controlled_file SET change_type='REVISION' WHERE id=?",source.getId());
            case "TARGET_ACTION" -> jdbc.update("UPDATE dcc_controlled_file SET change_type='REVISION' WHERE id=?",target.getId());
            case "SOURCE_NOT_WORKING" -> jdbc.update("UPDATE dcc_controlled_file SET status='PENDING_REVIEW' WHERE id=?",source.getId());
            case "SOURCE_FILE_BPM" -> jdbc.update("UPDATE dcc_controlled_file SET process_instance_id='opaque-real-source-bpm' WHERE id=?",source.getId());
            case "SOURCE_MAPPING_BOUND" -> tx(()->rounds.bindReservedDraft(project.getId(),"UPLOAD",source.getId(),saved.getApplicationRound(),"source-bpm"));
            case "SOURCE_SUBMITTED" -> attributes.freeze(7L,project.getId(),"UPLOAD",source.getId(),saved.getApplicationRound());
            case "SOURCE_MISSING_ATTRIBUTES" -> jdbc.update("DELETE FROM dcc_project_application_attributes WHERE id=?",saved.getId());
            case "SOURCE_BAD_ATTRIBUTES" -> jdbc.update("UPDATE dcc_project_application_attributes SET actual_attributes_json='{}' WHERE id=?",saved.getId());
            case "SOURCE_FILE_MISSING" -> files.deleteById(source.getId());
            case "TARGET_FILE_MISSING" -> files.deleteById(target.getId());
            case "SOURCE_MAPPING_MISSING" -> jdbc.update("DELETE FROM dcc_application_round_link WHERE application_id=?",source.getId());
            case "TARGET_MAPPING_MISSING" -> jdbc.update("DELETE FROM dcc_application_round_link WHERE application_id=?",target.getId());
            case "SOURCE_SNAPSHOT_PROJECT" -> jdbc.update("UPDATE dcc_project_application_attributes SET project_code_id=999 WHERE id=?",saved.getId());
            case "SOURCE_SNAPSHOT_TENANT" -> jdbc.update("UPDATE dcc_project_application_attributes SET tenant_id=2 WHERE id=?",saved.getId());
            case "CALLER_TENANT" -> TenantContextHolder.setTenantId(2L);
            case "SOURCE_ROUND" -> sourceRound++;
            case "TARGET_ROUND" -> targetRound++;
            case "TYPE_MISMATCH" -> type="REVISION";
            case "TARGET_MAPPING_BOUND" -> tx(()->rounds.bindReservedDraft(project.getId(),"UPLOAD",target.getId(),reservedTargetRound,"target-bpm"));
            case "TARGET_FILE_BPM" -> jdbc.update("UPDATE dcc_controlled_file SET process_instance_id='target-bpm' WHERE id=?",target.getId());
            default -> throw new IllegalArgumentException(violation);
        }
        final String action=type;final int fromRound=sourceRound;final int toRound=targetRound;
        String before=jdbc.queryForList("SELECT * FROM dcc_project_application_attributes ORDER BY id").toString();
        try {
            assertThrows(RuntimeException.class,()->tx(()->inherit(project.getId(),action,source.getId(),fromRound,target.getId(),toRound)),violation);
        } finally {TenantContextHolder.setTenantId(1L);}
        assertEquals(before,jdbc.queryForList("SELECT * FROM dcc_project_application_attributes ORDER BY id").toString());
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_application_attributes WHERE application_id=?",Integer.class,target.getId()));
    }

    @Test void h06ExistingTargetWithoutSourceProvenanceCannotBeReinitializedEvenWhenFrozen() {
        var project=project("H06-CONFLICT",7L);var source=source(project.getId(),"UPLOAD");
        var saved=tx(()->snapshots.prepareDraft(7L,project.getId(),"UPLOAD",source.getId(),value("CE")));
        var target=working(project.getId(),source.getMasterId(),"UPLOAD",source.getId());
        var own=tx(()->snapshots.prepareDraft(7L,project.getId(),"UPLOAD",target.getId(),value("FDA")));
        assertThrows(RuntimeException.class,()->tx(()->inherit(project.getId(),"UPLOAD",source.getId(),saved.getApplicationRound(),target.getId(),own.getApplicationRound())));
        tx(()->snapshots.submitReservedDraft(7L,project.getId(),"UPLOAD",target.getId(),own.getApplicationRound(),"frozen-conflict-bpm",value("FDA")));
        String before=JsonUtils.toJsonString(snapshots.read(7L,project.getId(),"UPLOAD",target.getId(),"frozen-conflict-bpm"));
        assertThrows(RuntimeException.class,()->tx(()->inherit(project.getId(),"UPLOAD",source.getId(),saved.getApplicationRound(),target.getId(),own.getApplicationRound())));
        assertEquals(before,JsonUtils.toJsonString(snapshots.read(7L,project.getId(),"UPLOAD",target.getId(),"frozen-conflict-bpm")));
    }

    @Test void h06ParentLateFailureRollsBackNewRealFileReservationAndCopyAndRequiresTransaction() {
        var project=project("H06-LATE",7L);var source=source(project.getId(),"REVISION");
        var saved=tx(()->snapshots.prepareDraft(7L,project.getId(),"REVISION",source.getId(),value("CE")));
        var targetId=new java.util.concurrent.atomic.AtomicReference<Long>();
        assertThrows(IllegalStateException.class,()->tx(()->{
            var target=working(project.getId(),source.getMasterId(),"REVISION",source.getId());targetId.set(target.getId());
            int round=rounds.reserveDraft(project.getId(),"REVISION",target.getId());
            inherit(project.getId(),"REVISION",source.getId(),saved.getApplicationRound(),target.getId(),round);
            throw new IllegalStateException("parent late failure");
        }));
        assertNull(files.selectById(targetId.get()));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_application_round_link",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_application_attributes",Integer.class));
        assertEquals(JsonUtils.toJsonString(saved),JsonUtils.toJsonString(snapshots.readReservedDraft(7L,project.getId(),"REVISION",source.getId())));
        var target=working(project.getId(),source.getMasterId(),"REVISION",source.getId());
        int round=tx(()->rounds.reserveDraft(project.getId(),"REVISION",target.getId()));
        assertThrows(org.springframework.transaction.IllegalTransactionStateException.class,
                ()->inherit(project.getId(),"REVISION",source.getId(),saved.getApplicationRound(),target.getId(),round));
    }

    @ParameterizedTest @ValueSource(strings={"SOURCE","TARGET"})
    void h06InheritanceHoldsSameProjectLockUntilCopyCompletesAgainstConcurrentRealBpmBinding(String bindingSide) throws Exception {
        var project=project("H06-RACE",7L);var source=source(project.getId(),"UPLOAD");
        var saved=tx(()->snapshots.prepareDraft(7L,project.getId(),"UPLOAD",source.getId(),value("CE")));
        var target=working(project.getId(),source.getMasterId(),"UPLOAD",source.getId());
        int targetRound=tx(()->rounds.reserveDraft(project.getId(),"UPLOAD",target.getId()));
        var checked=new CountDownLatch(1);var release=new CountDownLatch(1);var startedBinding=new CountDownLatch(1);
        org.mockito.Mockito.doAnswer(call->{int allocated=(Integer)call.callRealMethod();checked.countDown();
            if(!release.await(10,TimeUnit.SECONDS))throw new IllegalStateException("inheritance pause timed out");return allocated;
        }).when(roundSpy).requireDraft("UPLOAD",source.getId());
        var pool=Executors.newFixedThreadPool(2);
        try {
            var copying=pool.submit(()->{
                TenantContextHolder.setTenantId(1L);login(7L);
                try{return tx(()->inherit(project.getId(),"UPLOAD",source.getId(),saved.getApplicationRound(),target.getId(),targetRound));}
                finally{TenantContextHolder.clear();org.springframework.security.core.context.SecurityContextHolder.clearContext();}
            });
            assertTrue(checked.await(10,TimeUnit.SECONDS));
            Long bindingFile="SOURCE".equals(bindingSide)?source.getId():target.getId();
            int bindingRound="SOURCE".equals(bindingSide)?saved.getApplicationRound():targetRound;
            var binding=pool.submit(()->{
                TenantContextHolder.setTenantId(1L);login(7L);startedBinding.countDown();
                try{return tx(()->snapshots.submitReservedDraft(7L,project.getId(),"UPLOAD",bindingFile,bindingRound,"race-real-bpm",value("MADSAP")));}
                finally{TenantContextHolder.clear();org.springframework.security.core.context.SecurityContextHolder.clearContext();}
            });
            assertTrue(startedBinding.await(10,TimeUnit.SECONDS));
            assertThrows(TimeoutException.class,()->binding.get(300,TimeUnit.MILLISECONDS),"BPM binding must wait for the actual inheritance transaction");
            release.countDown();var copied=copying.get(10,TimeUnit.SECONDS);var frozen=binding.get(10,TimeUnit.SECONDS);
            assertEquals(value("NMPA"),attributes.readValue(copied.getDefaultSourceJson()));
            assertEquals(value("CE"),attributes.readValue(copied.getActualAttributesJson()));assertFalse(copied.getSubmitted());
            assertTrue(frozen.getSubmitted());assertEquals(bindingRound,rounds.require("UPLOAD",bindingFile,"race-real-bpm"));
            assertEquals(value("MADSAP"),attributes.readValue(frozen.getActualAttributesJson()));
        } finally {release.countDown();pool.shutdownNow();}
    }
}
