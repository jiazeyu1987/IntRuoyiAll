package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRoutePreviewReqVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real saved provenance, executed route rows and exact historical BPM; no successful verifier stub. */
class DccRejectedApplicationMatrixBaselineDatabaseTest extends DccNewActionBaselinePreparationDatabaseTest {
    void rejectedCopies() throws Exception {
        rejectedPublicSource();
        jdbc.update("UPDATE dcc_controlled_file SET draft_approval_matrix_json=?,effective_mode='AFTER_STANDARDIZATION_SIGNATURE',selected_signoff_department_ids_json='[\"51\"]',distribution_department_ids_json='[]',need_training=0 WHERE id=20",canonicalMatrix(1));
        snapshotExecutedMatrix(canonicalMatrix(2));
        copy(21L,20L);inherit(21L);saveFactsToNewTarget(21L,facts(21L,20L,1));
        copy(22L,21L);inherit(22L);saveFactsToNewTarget(22L,facts(22L,21L,1));
        currentMatrix(3);
    }

    DccControlledFileRoutePreviewReqVO rejectedPreviewRequest() {
        var request=new DccControlledFileRoutePreviewReqVO();
        request.setControlledFileId(22L);request.setCategoryId(2L);request.setDccProjectCodeId(5L);
        request.setActionType("NEW");request.setApplicationContextKey("c10-rejected-body-22");
        return request;
    }

    DccApprovalMatrixBaselineService.Expectations executedExpectation() throws Exception {
        String canonical=canonicalMatrix(2);
        var matrix=DccApprovalMatrixSourceService.readCanonical(canonical);
        var context=new DccApprovalMatrixTokenService.Context(1L,99L,5L,2L,6L,"NEW",22L,"c10-rejected-body-22");
        return new DccApprovalMatrixBaselineService.Expectations(matrix.getRouteId(),matrix.getRouteVersionNo(),
                matrix.getMatrixDigest(),tokens.issueBaseline(context,canonical),null,false,"c10-rejected-body-22");
    }

    @Test void rejectedPreviewKeepsExecutedMatrixThroughRepeatedCheckins() throws Exception {
        rejectedCopies();
        var originalFile=original(20L);var targetFile=original(22L);
        var originalRows=routeSnapshots.selectListByControlledFileId(20L);
        var result=tx().execute(s->baselines.preview(99L,rejectedPreviewRequest()));
        assertEquals(canonicalMatrix(2),result.beforeCanonical());
        assertEquals(canonicalMatrix(2),result.after().canonical(),"same-type rejected body must not select current public v3");
        assertFalse(result.changed());assertNull(result.confirmationToken());
        assertEquals(originalFile,original(20L));assertEquals(targetFile,original(22L));
        assertEquals(originalRows,routeSnapshots.selectListByControlledFileId(20L));
    }

    @Test void rejectedConfirmUsesExecutedExpectationWithoutCurrentConfirmation() throws Exception {
        rejectedCopies();var expected=executedExpectation();
        var originalFile=original(20L);var targetFile=original(22L);
        var result=tx().execute(s->baselines.confirm(99L,5L,2L,22L,"NEW",expected));
        assertEquals(canonicalMatrix(2),result.source().canonical());
        assertEquals(canonicalMatrix(2),result.beforeCanonical());
        assertEquals(originalFile,original(20L));assertEquals(targetFile,original(22L));
    }

    @Test void unsentPublicWorkingCopyStillRequiresLatestMatrixConfirmation() throws Exception {
        Path root=Path.of("").toAbsolutePath();
        while(root!=null&&!Files.isRegularFile(root.resolve("AGENTS.md")))root=root.getParent();
        String id=deploy(Files.readString(root.resolve("doc/tasks/20261011-dcc-w05-approval-signatures/bpmn-v5/upload.bpmn20.xml")));
        String digest=DccLifecycleEffectiveContractResolver.definitionDigest(engine.getRepositoryService().getBpmnModel(id));
        var verified=contracts.requireOrdinaryPublic(DccApplicationExecutionContractService.PUBLIC,id,digest,DccControlledFileProcessDefinitionKeys.UPLOAD);
        jdbc.update("UPDATE dcc_controlled_file SET prepared_workflow_contract=?,prepared_process_definition_id=?,prepared_definition_digest=?,draft_approval_matrix_json=? WHERE id=20",
                verified.executionContract(),verified.processDefinitionId(),verified.definitionDigest(),canonicalMatrix(1));
        copy(21L,20L);inherit(21L);saveFactsToNewTarget(21L,facts(21L,20L,1));
        copy(22L,21L);inherit(22L);saveFactsToNewTarget(22L,facts(22L,21L,1));currentMatrix(3);
        var result=tx().execute(s->baselines.preview(99L,rejectedPreviewRequest()));
        assertTrue(result.changed());assertEquals(canonicalMatrix(1),result.beforeCanonical());
        assertEquals(33L,result.after().baseline().getRouteId());assertNotNull(result.confirmationToken());
    }

    @Test void controlledRevisionAndItsWorkingCopyRemainANewApplication() throws Exception {
        publicRevisionBody();
        var preview=tx().execute(s->baselines.previewNewAction(99L,previewRequest(21L,true)));
        var request=new cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccWorkingApplicationSaveReqVO();
        request.setPrepareNewActionBaseline(true);request.setExecutionContract(files.selectById(21L).getPreparedWorkflowContract());
        request.setExpectedProcessDefinitionId(files.selectById(21L).getPreparedProcessDefinitionId());
        request.setExpectedDefinitionDigest(files.selectById(21L).getPreparedDefinitionDigest());
        request.setApplicationContextKey("exact-body-21");request.setApprovalMatrixBaselineToken(preview.baselineToken());
        request.setExpectedApprovalRouteId(preview.after().baseline().getRouteId());
        request.setExpectedApprovalRouteVersionNo(preview.after().baseline().getRouteVersionNo());
        request.setExpectedApprovalMatrixDigest(preview.after().baseline().getMatrixDigest());
        tx().executeWithoutResult(s->baselines.initializeNewActionDraft(99L,files.selectById(21L),request));
        nextUnpreparedRevisionBody();saveFactsToNewTarget(22L,facts(22L,21L,1));currentMatrix(3);
        var result=tx().execute(s->baselines.preview(99L,previewRequest(22L,false)));
        assertTrue(result.changed());assertEquals(33L,result.after().baseline().getRouteId());
        assertNotNull(result.confirmationToken());
    }

    @ParameterizedTest @ValueSource(strings={"ROUND","CYCLE","FAMILY","MISSING_SNAPSHOT","CHANGED_DRAFT","LOST_PROVENANCE"})
    void rejectedProvenanceConflictNeverFallsBackToLatest(String conflict) throws Exception {
        rejectedCopies();
        switch(conflict){
            case "ROUND" -> jdbc.update("UPDATE dcc_project_application_attributes SET source_application_round=99 WHERE application_id=22");
            case "CYCLE" -> {
                jdbc.update("UPDATE dcc_project_application_attributes SET source_application_id=22,source_application_round=1 WHERE application_id=21");
                jdbc.update("UPDATE dcc_controlled_file SET predecessor_controlled_file_id=22 WHERE id=21");
            }
            case "FAMILY" -> jdbc.update("UPDATE dcc_controlled_file SET file_type_taxonomy_id=8 WHERE id=20");
            case "MISSING_SNAPSHOT" -> jdbc.update("DELETE FROM dcc_controlled_file_route_snapshot WHERE controlled_file_id=20");
            case "CHANGED_DRAFT" -> jdbc.update("UPDATE dcc_controlled_file SET draft_approval_matrix_json=? WHERE id=22",canonicalMatrix(3));
            case "LOST_PROVENANCE" -> jdbc.update("UPDATE dcc_project_application_attributes SET source_application_id=NULL,source_application_round=NULL WHERE application_id=22");
            default -> throw new AssertionError(conflict);
        }
        var before=original(22L);var expected=executedExpectation();
        assertThrows(RuntimeException.class,()->tx().execute(s->baselines.preview(99L,rejectedPreviewRequest())));
        assertThrows(RuntimeException.class,()->tx().execute(s->baselines.confirm(99L,5L,2L,22L,"NEW",expected)));
        assertEquals(before,original(22L));
    }

    @ParameterizedTest @ValueSource(strings={"PREVIEW","CONFIRM"})
    void disabledOriginalApproverIsNotReplacedByCurrentPublicCandidate(String phase) throws Exception {
        rejectedCopies();
        jdbc.update("UPDATE dcc_category_approval_route_node SET candidate_source_ids='100',subject_id=100 WHERE route_id=33 AND stage_type='APPROVAL'");
        var users=mock(cn.iocoder.yudao.module.system.api.user.AdminUserApi.class);
        doAnswer(call->{
            if(call.<java.util.Collection<Long>>getArgument(0).contains(99L))
                throw new cn.iocoder.yudao.framework.common.exception.ServiceException(100,"Original approver is disabled");
            return null;
        }).when(users).validateUserList(anyCollection());
        var departments=mock(cn.iocoder.yudao.module.system.api.dept.DeptApi.class);
        when(departments.getDeptList(anyCollection())).thenReturn(java.util.List.of(
                new cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO().setId(51L).setLeaderUserId(88L)));
        var positions=mock(cn.iocoder.yudao.module.dcc.service.position.DccApprovalPositionRuntimeResolver.class);
        when(positions.isUploaderDerivedPosition(anyLong())).thenReturn(true);
        when(positions.resolveUserIds(anyLong(),anyLong(),anyBoolean())).thenReturn(java.util.List.of(101L));
        var resolver=new DccControlledFileApprovalRouteAssigneeResolver();
        wire(resolver,"adminUserApi",users,"deptApi",departments,"positionRuntimeResolver",positions);
        var expected=executedExpectation();
        var selected=tx().execute(s->phase.equals("PREVIEW")?baselines.preview(99L,rejectedPreviewRequest()).after()
                :baselines.confirm(99L,5L,2L,22L,"NEW",expected).source());
        var rejected=assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                ()->resolver.resolvePublicMatrix(selected,99L,"NEW",java.util.List.of(51L)));
        assertEquals(cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.ROUTE_PREVIEW_APPROVER_NOT_FOUND.getCode(),rejected.getCode());
        verify(users).validateUserList(java.util.List.of(99L));
    }
}
