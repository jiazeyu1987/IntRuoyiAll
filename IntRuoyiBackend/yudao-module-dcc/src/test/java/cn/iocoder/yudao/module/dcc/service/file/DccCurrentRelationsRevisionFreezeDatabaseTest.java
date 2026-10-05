package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccLatestControlledFileResolverImpl;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationAccessPolicy;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationStore;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real public selected-body submit, relation SQL/resolver, immutable attributes and V4 Flowable.
 * Account/name permission/storage/platform ports remain the parent's declared isolated dependencies. */
class DccCurrentRelationsRevisionFreezeDatabaseTest extends DccWorkflowSelectedIterationDatabaseTest {
    @BeforeEach
    void currentRelationsFixture() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS dcc_current_file_relation(tenant_id BIGINT NOT NULL,source_master_id BIGINT NOT NULL,related_master_id BIGINT NOT NULL,PRIMARY KEY(tenant_id,source_master_id,related_master_id))");
        jdbc.execute("CREATE TABLE IF NOT EXISTS dcc_current_file_relation_set(tenant_id BIGINT NOT NULL,source_master_id BIGINT NOT NULL,controlled_file_id BIGINT NOT NULL,row_version BIGINT NOT NULL DEFAULT 0,PRIMARY KEY(tenant_id,source_master_id))");
        jdbc.update("DELETE FROM dcc_current_file_relation");
        jdbc.update("DELETE FROM dcc_current_file_relation_set");
        ReflectionTestUtils.setField(related,"relationStore",new DccRelationStore(jdbc,mock(GxpAuditService.class)));
        ReflectionTestUtils.setField(related,"relationAccessPolicy",mock(DccRelationAccessPolicy.class));
        var resolver=new DccLatestControlledFileResolverImpl();
        ReflectionTestUtils.setField(resolver,"fileMapper",files);
        ReflectionTestUtils.setField(resolver,"masterMapper",masters);
        ReflectionTestUtils.setField(resolver,"jdbc",jdbc);
        ReflectionTestUtils.setField(related,"latestFileResolver",resolver);
    }

    private long savedDraftThenCurrentChanged() {
        controlled("A/3");
        changeDefault();
        long selected=checkin(20L,"existing saved CE body");
        workflow.saveWorkingApplicationAttributes(99L,selected,ce);
        jdbc.update("UPDATE dcc_project_code SET default_attributes_json=? WHERE id=5",attributes.encode(nmpa));
        jdbc.update("INSERT INTO dcc_controlled_file_master(id,tenant_id,category_id,directory_id,file_name,file_number,dcc_project_code_id,file_type_taxonomy_leaf_id,normalized_file_number,status,latest_controlled_file_id,current_active_controlled_file_id,deleted) VALUES(40,1,2,3,'Related.pdf','R-1',5,6,'R-1','ACTIVE_CHAIN',400,400,0)");
        files.insert(DccControlledFileDO.builder().id(400L).tenantId(1L).masterId(40L).dccProjectCodeId(5L)
                .categoryId(2L).directoryId(3L).fileTypeTaxonomyId(6L).fileName("Related.pdf").title("Related")
                .fileNumber("R-1").sourceOriginalFileName("Related.pdf").versionNo("A/1").status("ACTIVE")
                .requesterId(99L).submitterId(99L)
                .controlledTime(LocalDateTime.of(2026,10,1,12,0)).activatedTime(LocalDateTime.of(2026,10,1,12,0))
                .sourceFileId(100L).originalFileId(100L).publishedFileId(100L).stampedFileId(100L).build());
        jdbc.update("UPDATE dcc_current_file_relation_set SET row_version=1 WHERE tenant_id=1 AND source_master_id=10 AND controlled_file_id=20");
        jdbc.update("INSERT INTO dcc_current_file_relation VALUES(1,10,40)");
        assertEquals(0,relations.selectListByControlledFileId(selected).size());
        assertEquals(0,relations.selectListByControlledFileId(20L).size());
        return selected;
    }

    @Test
    void actualPublicRevisionFreezesCurrentRelationsWithoutOverwritingSavedBodyAttributesOrOldSnapshots() {
        long selected=savedDraftThenCurrentChanged();
        var before=snapshots.readReservedDraft(99L,5L,"REVISION",selected);
        String sourceDefault=before.getDefaultSourceJson(),sourceActual=before.getActualAttributesJson();
        long candidate=workflow.submitWorkingIteration(99L,selected,request("REPLACEMENT"));
        var frozen=relations.selectListByControlledFileId(candidate);
        assertEquals(1,frozen.size(),"new application must freeze the edited controlled baseline's current set");
        assertEquals(40L,frozen.get(0).getRelatedMasterId());
        assertEquals(400L,frozen.get(0).getRelatedControlledFileId());
        assertEquals("R-1",frozen.get(0).getRelatedFileNumberSnapshot());
        assertEquals("A/1",frozen.get(0).getRelatedVersionNoSnapshot());
        assertEquals("B/1",files.selectById(candidate).getVersionNo());
        assertEquals(0,relations.selectListByControlledFileId(20L).size());
        assertEquals(0,relations.selectListByControlledFileId(selected).size());
        var saved=attributeRows.find("REVISION",candidate,rounds.require("REVISION",candidate,files.selectById(candidate).getProcessInstanceId()));
        assertEquals(sourceDefault,saved.getDefaultSourceJson());assertEquals(sourceActual,saved.getActualAttributesJson());
        assertEquals(sourceDefault,snapshots.readReservedDraft(99L,5L,"REVISION",selected).getDefaultSourceJson());
        assertEquals(400L,tx().execute(s->related.listHistoricalRelatedFiles(99L,candidate)).get(0).getControlledFileId());
        var task=engine.getTaskService().createTaskQuery().processInstanceId(files.selectById(candidate).getProcessInstanceId()).singleResult();
        assertEquals("MATRIX_REVIEW",task.getTaskDefinitionKey());
        assertTrue(task.getProcessDefinitionId().startsWith("dcc-controlled-file-revision:"));
        assertEquals(1L,jdbc.queryForObject("SELECT row_version FROM dcc_current_file_relation_set WHERE tenant_id=1 AND source_master_id=10",Long.class));
    }

    @Test
    void authoritativeEmptyCurrentSetDoesNotFallBackToTheWorkingHistoricalSelection() {
        long selected=savedDraftThenCurrentChanged();
        jdbc.update("DELETE FROM dcc_current_file_relation WHERE tenant_id=1 AND source_master_id=10");
        jdbc.update("INSERT INTO dcc_controlled_file_related_file(controlled_file_id,related_controlled_file_id,project_code_id,related_master_id,related_file_number_snapshot,related_file_name_snapshot,related_version_no_snapshot,relation_source,tenant_id) VALUES(?,400,5,40,'R-1','Related.pdf','A/1','CHECKIN_INHERITED',1)",selected);
        long candidate=workflow.submitWorkingIteration(99L,selected,request("REPLACEMENT"));
        assertTrue(relations.selectListByControlledFileId(candidate).isEmpty());
        assertEquals(1,relations.selectListByControlledFileId(selected).size());
    }

    @Test
    void exactReplayAfterASecondCurrentEditKeepsTheOriginalFrozenApplicationAndBpm() {
        long selected=savedDraftThenCurrentChanged();var request=request("REPLACEMENT");
        long candidate=workflow.submitWorkingIteration(99L,selected,request);
        var originalRows=jdbc.queryForList("SELECT * FROM dcc_controlled_file_related_file WHERE controlled_file_id=?",candidate);
        String originalProcess=files.selectById(candidate).getProcessInstanceId();int bodyCount=bytes.size();
        jdbc.update("DELETE FROM dcc_current_file_relation WHERE tenant_id=1 AND source_master_id=10");
        jdbc.update("UPDATE dcc_current_file_relation_set SET row_version=2 WHERE tenant_id=1 AND source_master_id=10");
        assertEquals(candidate,workflow.submitWorkingIteration(99L,selected,request));
        assertEquals(originalRows,jdbc.queryForList("SELECT * FROM dcc_controlled_file_related_file WHERE controlled_file_id=?",candidate));
        assertEquals(originalProcess,files.selectById(candidate).getProcessInstanceId());
        assertEquals(bodyCount,bytes.size());assertEquals(1,engine.getRuntimeService().createProcessInstanceQuery().count());
    }

    @Test
    void lateActualBpmFailureRollsBackCandidateSnapshotBodyAttributesAndKeepsCurrentTuple() {
        long selected=savedDraftThenCurrentChanged();int countBefore=count("dcc_controlled_file"),bodyBefore=bytes.size();
        var api=(BpmProcessInstanceApi)ReflectionTestUtils.getField(workflow,"bpmProcessInstanceApi");
        doAnswer(call->{
            cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO request=call.getArgument(1);
            engine.getIdentityService().setAuthenticatedUserId("99");
            try{engine.getRuntimeService().startProcessInstanceByKeyAndTenantId(request.getProcessDefinitionKey(),request.getBusinessKey(),request.getVariables(),"1");}
            finally{engine.getIdentityService().setAuthenticatedUserId(null);}
            throw new IllegalStateException("LATE_ACTUAL_BPM_FAILURE");
        }).when(api).createProcessInstance(anyLong(),any());
        var error=assertThrows(IllegalStateException.class,()->workflow.submitWorkingIteration(99L,selected,request("REPLACEMENT")));
        assertEquals("LATE_ACTUAL_BPM_FAILURE",error.getMessage());
        assertEquals(countBefore,count("dcc_controlled_file"));assertEquals(bodyBefore,bytes.size());
        assertEquals(0,count("dcc_controlled_file_related_file"));assertEquals(0,engine.getRuntimeService().createProcessInstanceQuery().count());
        assertEquals(20L,masters.selectById(10L).getLatestControlledFileId());
        assertEquals(1L,jdbc.queryForObject("SELECT row_version FROM dcc_current_file_relation_set WHERE tenant_id=1 AND source_master_id=10",Long.class));
        assertEquals(List.of(40L),jdbc.queryForList("SELECT related_master_id FROM dcc_current_file_relation WHERE tenant_id=1 AND source_master_id=10",Long.class));
    }

    @Test
    void wrongOrMissingAuthoritativeCurrentTupleFailsWithoutHealingOrCandidateWrites() {
        long selected=savedDraftThenCurrentChanged();int fileCount=count("dcc_controlled_file"),bodyCount=bytes.size();
        jdbc.update("UPDATE dcc_current_file_relation_set SET controlled_file_id=? WHERE tenant_id=1 AND source_master_id=10",selected);
        var wrong=assertThrows(cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationFailure.class,
                ()->workflow.submitWorkingIteration(99L,selected,request("REPLACEMENT")));
        assertEquals("DCC_REVISION_RELATION_CURRENT_TUPLE_INVALID",wrong.getMessage());
        assertEquals(selected,jdbc.queryForObject("SELECT controlled_file_id FROM dcc_current_file_relation_set WHERE tenant_id=1 AND source_master_id=10",Long.class));
        jdbc.update("DELETE FROM dcc_current_file_relation_set WHERE tenant_id=1 AND source_master_id=10");
        var missing=assertThrows(cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationFailure.class,
                ()->workflow.submitWorkingIteration(99L,selected,request("REPLACEMENT")));
        assertEquals("DCC_REVISION_RELATION_CURRENT_TUPLE_INVALID",missing.getMessage());
        assertEquals(0,count("dcc_current_file_relation_set"));assertEquals(fileCount,count("dcc_controlled_file"));
        assertEquals(bodyCount,bytes.size());assertEquals(0,count("dcc_controlled_file_related_file"));
        assertEquals(0,engine.getRuntimeService().createProcessInstanceQuery().count());
    }
}
