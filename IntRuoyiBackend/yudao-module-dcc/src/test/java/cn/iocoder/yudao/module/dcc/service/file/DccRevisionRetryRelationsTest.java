package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationRoundEvidence;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationStageSnapshotService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static cn.iocoder.yudao.module.dcc.service.file.DccControlledFileProcessDefinitionKeys.REVISION;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccRevisionRetryRelationsTest {
    private final DccControlledFileWorkflowServiceImpl workflow = new DccControlledFileWorkflowServiceImpl();
    private final DccControlledFileMapper files = mock(DccControlledFileMapper.class);
    private final DccApplicationRoundService rounds = mock(DccApplicationRoundService.class);
    private final DccRelationStageSnapshotService relations = mock(DccRelationStageSnapshotService.class);
    private final DccWorkingApplicationDraftInitializer drafts = mock(DccWorkingApplicationDraftInitializer.class);

    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L);
        ReflectionTestUtils.setField(workflow, "controlledFileMapper", files);
        ReflectionTestUtils.setField(workflow, "applicationRoundService", rounds);
        ReflectionTestUtils.setField(workflow, "relationStageSnapshots", relations);
        ReflectionTestUtils.setField(workflow, "workingApplicationDraftInitializer", drafts);
    }

    @AfterEach void cleanup() { TenantContextHolder.clear(); }

    @ParameterizedTest
    @CsvSource({"PARTIAL,A/2,101", "PARTIAL,A/2,102", "REPLACEMENT,B/1,102"})
    void retryUsesExactFailedRoundEvenWhenSelectingANewBody(String intent, String version, long selectedId) {
        var failed = candidate(800L, intent, version, 101L);
        failed.setStatus("REJECTED");
        failed.setProcessDefinitionKey(REVISION);
        failed.setProcessInstanceId("failed-bpm");
        var target = candidate(901L, intent, version, selectedId);
        target.setReworkPredecessorControlledFileId(800L);
        when(files.selectByIdAndTenantForUpdate(1L, 800L)).thenReturn(failed);
        when(rounds.require("REVISION", 800L, "failed-bpm")).thenReturn(3);
        when(rounds.requireDraft("REVISION", 901L)).thenReturn(4);
        when(relations.readRoundEvidence(99L, 800L, "REVISION", "failed-bpm"))
                .thenReturn(new DccRelationRoundEvidence(1L, 800L, 700L, 5L, "REVISION", 3,
                        "failed-bpm", true, null, List.of(), List.of(), List.of(), "exact-digest"));

        assertDoesNotThrow(() -> inherit(target));
        verify(rounds).assertBoundRound(5L, "REVISION", 800L, 3);
        verify(relations).inheritRoundSelections(99L, 800L, "REVISION", "failed-bpm", 901L,
                "REVISION", 4, "exact-digest");
        verifyNoInteractions(drafts);
    }

    @ParameterizedTest
    @CsvSource({"requester", "baseline", "intent", "version", "master"})
    void newBodyCannotInheritAnUnrelatedFailedAttempt(String mismatch) {
        var failed = candidate(800L, "PARTIAL", "A/2", 101L);
        var target = candidate(901L, "PARTIAL", "A/2", 102L);
        target.setReworkPredecessorControlledFileId(800L);
        switch (mismatch) {
            case "requester" -> failed.setRequesterId(100L);
            case "baseline" -> failed.setRevisionSourceControlledFileId(21L);
            case "intent" -> failed.setRevisionChangeType("REPLACEMENT");
            case "version" -> failed.setVersionNo("A/3");
            case "master" -> failed.setMasterId(701L);
        }
        when(files.selectByIdAndTenantForUpdate(1L, 800L)).thenReturn(failed);
        assertThrows(IllegalStateException.class, () -> inherit(target));
        verifyNoInteractions(rounds, relations, drafts);
    }

    @Test void missingFailedRoundEvidenceStillBlocksSubmission() {
        var failed = candidate(800L, "PARTIAL", "A/2", 101L);
        failed.setStatus("WITHDRAWN");
        failed.setProcessDefinitionKey(REVISION);
        failed.setProcessInstanceId("failed-bpm");
        var target = candidate(901L, "PARTIAL", "A/2", 102L);
        target.setReworkPredecessorControlledFileId(800L);
        when(files.selectByIdAndTenantForUpdate(1L, 800L)).thenReturn(failed);
        assertThrows(IllegalStateException.class, () -> inherit(target));
        verify(relations, never()).inheritRoundSelections(any(), any(), any(), any(), any(), any(), anyInt(), any());
    }

    private void inherit(DccControlledFileDO target) {
        ReflectionTestUtils.invokeMethod(workflow, "inheritReservedProvenanceRelations", 99L, target, REVISION);
    }

    private DccControlledFileDO candidate(long id, String intent, String version, long selectedId) {
        return DccControlledFileDO.builder().id(id).tenantId(1L).masterId(700L).dccProjectCodeId(5L)
                .categoryId(2L).fileTypeTaxonomyId(6L).fileNumber("999999").sourceOriginalFileName("测试word.docx")
                .requesterId(99L).revisionSourceControlledFileId(20L).revisionBaseActiveControlledFileId(20L)
                .revisionChangeType(intent).versionNo(version).selectedIterationControlledFileId(selectedId).build();
    }
}
