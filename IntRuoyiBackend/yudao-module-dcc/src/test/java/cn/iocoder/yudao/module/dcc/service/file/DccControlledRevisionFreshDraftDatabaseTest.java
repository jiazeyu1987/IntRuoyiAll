package cn.iocoder.yudao.module.dcc.service.file;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real isolated H2 initializer/round mapping and exact public Flowable source; no production DB. */
class DccControlledRevisionFreshDraftDatabaseTest extends DccNewActionBaselinePreparationDatabaseTest {
    void controlledRevision() throws Exception {
        publicRevisionBody();
        String bpm=engine.getRuntimeService().createProcessInstanceBuilder().processDefinitionId(revisionId)
                .businessKey("21").variables(Map.of("controlledFileId",21L)).start().getId();
        tx().executeWithoutResult(s->snapshots.submitReservedDraft(99L,5L,"REVISION",21L,
                rounds.requireDraft("REVISION",21L),bpm,actual));
        jdbc.update("UPDATE dcc_controlled_file SET status='ACTIVE',version_no='B/1',controlled_time=CURRENT_TIMESTAMP,process_instance_id=?,process_definition_key=? WHERE id=21",bpm,DccControlledFileProcessDefinitionKeys.REVISION);
        jdbc.update("UPDATE dcc_controlled_file_master SET latest_controlled_file_id=21,current_active_controlled_file_id=21 WHERE id=10");
    }
    void newBody() {
        add(22L,21L);
        jdbc.update("UPDATE dcc_controlled_file SET change_type='REVISION',revision_base_active_controlled_file_id=21,version_no='B/1-1' WHERE id=22");
        tx().executeWithoutResult(s->initializer.initialize(99L,21L,22L,null));
    }
    @Test void controlledRevisionStartsNewApplicationAndKeepsItsExactSourceDefinitionAndHistory() throws Exception {
        controlledRevision();var old=original(21L);newBody();
        assertNull(attributeRows.find("REVISION",22L,1).getSourceApplicationId());
        var prepared=tx().execute(s->contracts.inheritPrepared(99L,22L,21L,1));
        assertEquals(revisionId,prepared.processDefinitionId());assertEquals(DccApplicationExecutionContractService.PUBLIC,prepared.executionContract());
        var facts=tx().execute(s->contracts.readInheritedDraftFacts(99L,22L,21L,1));
        assertFalse(facts.sameApplicationType(),"same type must not mean same draft application after control");
        assertTrue(facts.newActionBaselineRequired());assertNull(facts.approvalMatrixCanonical());assertNull(facts.executedSourceApplicationId());
        var eligible=tx().execute(s->contracts.discoverNewActionBaselinePreparation(99L,files.selectById(22L)));
        assertTrue(eligible.required());assertTrue(eligible.allowed());
        assertEquals(prepared,tx().execute(s->contracts.requireNewActionBaselinePreparation(99L,22L)));
        assertEquals(old,original(21L));verify(definitions,never()).getActiveProcessDefinition(anyString());
    }
    @Test void forgedInheritedPointersCannotTurnANewControlledRevisionIntoTheOldApplication() throws Exception {
        controlledRevision();newBody();jdbc.update("UPDATE dcc_project_application_attributes SET source_application_id=21,source_application_round=1 WHERE application_type='REVISION' AND application_id=22");
        assertThrows(RuntimeException.class,()->tx().execute(s->contracts.inheritPrepared(99L,22L,21L,1)));
        assertNull(files.selectById(22L).getPreparedWorkflowContract());
    }
    @Test void wrongFormalBaselineStillRejectsBeforePreparedWrites() throws Exception {
        controlledRevision();newBody();jdbc.update("UPDATE dcc_controlled_file SET revision_base_active_controlled_file_id=20 WHERE id=22");
        assertThrows(RuntimeException.class,()->tx().execute(s->contracts.inheritPrepared(99L,22L,21L,1)));
        assertNull(files.selectById(22L).getPreparedWorkflowContract());
    }
}
