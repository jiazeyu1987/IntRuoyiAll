package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccExternalFileReviewApproveTaskReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccExternalFileReviewSubmitReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccExternalFileReviewDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccExternalFileReviewMapper;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileProcessTypeEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketBoundFile;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketMarkBoundCommand;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketResolveCommand;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketService;
import org.flowable.engine.repository.ProcessDefinition;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDate;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_UPLOAD_TICKET_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.EXTERNAL_FILE_REVIEW_OUTPUT_FILE_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.EXTERNAL_FILE_REVIEW_PROCESS_DEFINITION_MISSING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccExternalFileReviewServiceImplTest extends BaseMockitoUnitTest {

    @Mock
    private DccControlledFileWorkflowServiceImpl workflowService;
    @Mock
    private DccExternalFileReviewMapper externalReviewMapper;
    @Mock
    private BpmProcessDefinitionService processDefinitionService;
    @Mock
    private BpmProcessInstanceApi bpmProcessInstanceApi;
    @Mock
    private DccControlledFileMapper controlledFileMapper;
    @Mock
    private DccUploadTicketService uploadTicketService;

    @InjectMocks
    private DccExternalFileReviewServiceImpl externalFileReviewService;

    @Test
    void submitExternalReview_success_usesIndependentProcessKeyAndPersistsExternalFields() {
        ProcessDefinition definition = org.mockito.Mockito.mock(ProcessDefinition.class);
        when(processDefinitionService.getActiveProcessDefinition(
                DccExternalFileReviewServiceImpl.BPM_PROCESS_DEFINITION_KEY)).thenReturn(definition);
        when(workflowService.submitControlledFileWithProcessDefinitionKey(eq(99L), any(),
                eq(DccExternalFileReviewServiceImpl.BPM_PROCESS_DEFINITION_KEY))).thenReturn(900L);
        DccExternalFileReviewSubmitReqVO reqVO = buildSubmitReqVO();

        Long fileId = externalFileReviewService.submitExternalReview(99L, reqVO);

        assertEquals(900L, fileId);
        ArgumentCaptor<cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSubmitReqVO> submitCaptor =
                ArgumentCaptor.forClass(cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSubmitReqVO.class);
        verify(workflowService).submitControlledFileWithProcessDefinitionKey(eq(99L), submitCaptor.capture(),
                eq(DccExternalFileReviewServiceImpl.BPM_PROCESS_DEFINITION_KEY));
        assertEquals(DccControlledFileProcessTypeEnum.EXTERNAL_REVIEW.getCode(), submitCaptor.getValue().getProcessType());
        assertEquals("session-external", submitCaptor.getValue().getSessionId());
        assertEquals("UT-EXTERNAL-ORIGINAL", submitCaptor.getValue().getOriginalUploadTicket());
        assertEquals(5000L, submitCaptor.getValue().getProductMasterId());
        assertEquals(List.of(201L, 202L), submitCaptor.getValue().getSelectedSignoffUserIds());
        ArgumentCaptor<DccExternalFileReviewDO> reviewCaptor = ArgumentCaptor.forClass(DccExternalFileReviewDO.class);
        verify(externalReviewMapper).insert(reviewCaptor.capture());
        assertEquals(900L, reviewCaptor.getValue().getControlledFileId());
        assertEquals("客户来图", reviewCaptor.getValue().getExternalSource());
        assertEquals("供应商A", reviewCaptor.getValue().getExternalOwner());
        assertEquals("201,202", reviewCaptor.getValue().getParticipantUserIds());
    }

    @Test
    void submitExternalReview_missingDefinition_failsFastBeforeCreatingAnyControlledFlow() {
        when(processDefinitionService.getActiveProcessDefinition(
                DccExternalFileReviewServiceImpl.BPM_PROCESS_DEFINITION_KEY)).thenReturn(null);

        assertServiceException(() -> externalFileReviewService.submitExternalReview(99L, buildSubmitReqVO()),
                EXTERNAL_FILE_REVIEW_PROCESS_DEFINITION_MISSING);

        verify(workflowService, never()).submitControlledFileWithProcessDefinitionKey(any(), any(), any());
        verify(bpmProcessInstanceApi, never()).createProcessInstance(any(Long.class), any(BpmProcessInstanceCreateReqDTO.class));
        verify(externalReviewMapper, never()).insert(any(DccExternalFileReviewDO.class));
    }

    @Test
    void approveExternalReview_finalNode_persistsConclusionAndOutputWithoutStampedPdfGovernance() {
        DccExternalFileReviewApproveTaskReqVO reqVO = new DccExternalFileReviewApproveTaskReqVO();
        reqVO.setTaskId("task-4");
        reqVO.setPassword("secret");
        reqVO.setReason("approved");
        reqVO.setReviewConclusion("ACCEPTED_WITH_NOTES");
        reqVO.setConclusionComment("输出评审确认稿");
        reqVO.setSessionId("session-external");
        reqVO.setOutputUploadTicket("UT-EXTERNAL-OUTPUT");
        when(controlledFileMapper.selectById(900L)).thenReturn(DccControlledFileDO.builder()
                .id(900L).status(DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL.getStatus()).build());
        when(uploadTicketService.resolveForBinding(new DccUploadTicketResolveCommand(
                "UT-EXTERNAL-OUTPUT", 99L, "session-external", "EXTERNAL_REVIEW_OUTPUT")))
                .thenReturn(new DccUploadTicketBoundFile("UT-EXTERNAL-OUTPUT", 800L,
                        "external-output.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", 32L));

        externalFileReviewService.approveTask(99L, 900L, reqVO);

        verify(workflowService).approveTaskWithProcessDefinitionKey(eq(99L), eq(900L), eq(reqVO),
                eq(DccExternalFileReviewServiceImpl.BPM_PROCESS_DEFINITION_KEY), eq(false));
        ArgumentCaptor<DccExternalFileReviewDO> updateCaptor = ArgumentCaptor.forClass(DccExternalFileReviewDO.class);
        verify(externalReviewMapper).updateByControlledFileId(eq(900L), updateCaptor.capture());
        assertEquals("ACCEPTED_WITH_NOTES", updateCaptor.getValue().getReviewConclusion());
        assertEquals(800L, updateCaptor.getValue().getOutputFileId());
        verify(uploadTicketService).markBound(new DccUploadTicketMarkBoundCommand(
                "UT-EXTERNAL-OUTPUT", 99L, "session-external", "EXTERNAL_REVIEW_OUTPUT", 900L));
    }

    @Test
    void approveExternalReview_rejectsRawOutputFileId() {
        DccExternalFileReviewApproveTaskReqVO reqVO = new DccExternalFileReviewApproveTaskReqVO();
        reqVO.setTaskId("task-4");
        reqVO.setPassword("secret");
        reqVO.setReason("approved");
        reqVO.setReviewConclusion("ACCEPTED_WITH_NOTES");
        reqVO.setSessionId("session-external");
        reqVO.setOutputFileId(800L);
        when(controlledFileMapper.selectById(900L)).thenReturn(DccControlledFileDO.builder()
                .id(900L).status(DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL.getStatus()).build());

        assertServiceException(() -> externalFileReviewService.approveTask(99L, 900L, reqVO),
                CONTROLLED_FILE_UPLOAD_TICKET_INVALID);

        verify(externalReviewMapper, never()).updateByControlledFileId(eq(900L), any());
    }

    @Test
    void approveExternalReview_finalNodeRequiresConclusionAndRealOutputFile() {
        DccExternalFileReviewApproveTaskReqVO reqVO = new DccExternalFileReviewApproveTaskReqVO();
        reqVO.setTaskId("task-4");
        reqVO.setPassword("secret");
        reqVO.setReason("approved");
        reqVO.setSessionId("session-external");
        when(controlledFileMapper.selectById(900L)).thenReturn(DccControlledFileDO.builder()
                .id(900L).status(DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL.getStatus()).build());

        assertServiceException(() -> externalFileReviewService.approveTask(99L, 900L, reqVO),
                EXTERNAL_FILE_REVIEW_OUTPUT_FILE_REQUIRED);

        verify(workflowService, never()).approveTaskWithProcessDefinitionKey(any(), any(), any(), any(), eq(false));
    }

    @Test
    void approveExternalReview_intermediateNodeDoesNotRequireConclusionOrOutputFile() {
        DccExternalFileReviewApproveTaskReqVO reqVO = new DccExternalFileReviewApproveTaskReqVO();
        reqVO.setTaskId("task-2");
        reqVO.setPassword("secret");
        reqVO.setReason("stage approved");
        reqVO.setSessionId("session-external");
        when(controlledFileMapper.selectById(900L)).thenReturn(DccControlledFileDO.builder()
                .id(900L).status(DccControlledFileStatusEnum.PENDING_MATRIX_REVIEW.getStatus()).build());

        externalFileReviewService.approveTask(99L, 900L, reqVO);

        verify(workflowService).approveTaskWithProcessDefinitionKey(eq(99L), eq(900L), eq(reqVO),
                eq(DccExternalFileReviewServiceImpl.BPM_PROCESS_DEFINITION_KEY), eq(false));
        verify(externalReviewMapper, never()).updateByControlledFileId(eq(900L), any());
    }

    private DccExternalFileReviewSubmitReqVO buildSubmitReqVO() {
        DccExternalFileReviewSubmitReqVO reqVO = new DccExternalFileReviewSubmitReqVO();
        reqVO.setCategoryId(10L);
        reqVO.setDirectoryId(21L);
        reqVO.setSessionId("session-external");
        reqVO.setOriginalUploadTicket("UT-EXTERNAL-ORIGINAL");
        reqVO.setSourceUploadTicket("UT-EXTERNAL-SOURCE");
        reqVO.setSourceFileName("external.docx");
        reqVO.setFileName("外来文件评审");
        reqVO.setFileNumber("EXT-001");
        reqVO.setProductMasterId(5000L);
        reqVO.setProductCode("PRD20260525001");
        reqVO.setNeedTraining(Boolean.FALSE);
        reqVO.setVersionNo("V1.0");
        reqVO.setEffectiveDate(LocalDate.of(2026, 5, 27));
        reqVO.setRemark("外来评审提交");
        reqVO.setExternalSource("客户来图");
        reqVO.setExternalOwner("供应商A");
        reqVO.setReviewReason("变更前评审");
        reqVO.setParticipantUserIds(List.of(201L, 202L));
        return reqVO;
    }
}
