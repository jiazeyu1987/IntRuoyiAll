package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.file.DccControlledFileController;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileApproveTaskReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileCreateSignTaskReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileMessageJobReplayReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRejectTaskReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileReturnTaskReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSignatureExportSummaryRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileTransferTaskReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccSignatureActionRespVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.mock.web.MockHttpServletRequest;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccControlledFileTaskActionApiTest extends BaseMockitoUnitTest {

    @Mock
    private DccControlledFileWorkflowService workflowService;
    @Mock
    private DccControlledFileFinalizationService finalizationService;
    @Mock
    private DccElectronicSignatureManagementService signatureManagementService;
    @Mock
    private DccControlledFileQueryService queryService;
    @Mock
    private DccControlledFileUploadService uploadService;
    @Mock
    private DccControlledFileMessageReplayService messageReplayService;

    @InjectMocks
    private DccControlledFileController controller;

    @Test
    void approveTask_preAuthorizeAllowsSubmitForApplicantReworkButKeepsReviewApprove() throws Exception {
        Method method = DccControlledFileController.class.getMethod("approveTask",
                Long.class, DccControlledFileApproveTaskReqVO.class);

        String expression = method.getAnnotation(PreAuthorize.class).value();

        Assertions.assertTrue(expression.contains("dcc:controlled-file:submit"));
        Assertions.assertTrue(expression.contains("dcc:controlled-file:review"));
        Assertions.assertTrue(expression.contains("dcc:controlled-file:approve"));
    }

    @Test
    void approveTask_delegatesToWorkflowService() {
        DccControlledFileApproveTaskReqVO reqVO = new DccControlledFileApproveTaskReqVO();
        reqVO.setTaskId("task-1");
        reqVO.setPassword("secret");
        reqVO.setReason("approved");

        try (MockedStatic<SecurityFrameworkUtils> securityFrameworkUtilsMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityFrameworkUtilsMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            DccSignatureActionRespVO actionResp = new DccSignatureActionRespVO();
            actionResp.setTaskActionResult("APPROVED");
            actionResp.setSignatureId(1001L);
            actionResp.setControlledFileId(900L);
            actionResp.setRevisionId(900L);
            actionResp.setVersionNo("A.1");
            actionResp.setMeaningCode("REVIEW_APPROVE");
            actionResp.setControlledCopyHashStatus("NOT_APPLICABLE");
            actionResp.setEvidenceStatus("VALID");
            actionResp.setEvidenceHashShort("6f2c91ab03d4");
            actionResp.setNextStatus("PENDING_MATRIX_REVIEW");
            when(workflowService.approveTask(99L, 900L, reqVO)).thenReturn(actionResp);

            CommonResult<DccSignatureActionRespVO> result = controller.approveTask(900L, reqVO);

            assertEquals("APPROVED", result.getData().getTaskActionResult());
            assertEquals(1001L, result.getData().getSignatureId());
            assertEquals("VALID", result.getData().getEvidenceStatus());
            verify(workflowService).approveTask(99L, 900L, reqVO);
        }
    }

    @Test
    void getSignatureExportSummary_checksFileVisibilityBeforeSignatureSummary() {
        DccControlledFileSignatureExportSummaryRespVO summary = new DccControlledFileSignatureExportSummaryRespVO();
        summary.setControlledFileId(900L);
        when(signatureManagementService.getSignatureExportSummary(900L)).thenReturn(summary);

        try (MockedStatic<SecurityFrameworkUtils> securityFrameworkUtilsMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityFrameworkUtilsMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);

            CommonResult<DccControlledFileSignatureExportSummaryRespVO> result =
                    controller.getSignatureExportSummary(900L);

            assertEquals(900L, result.getData().getControlledFileId());
            verify(queryService).getControlledFile(99L, 900L);
            verify(signatureManagementService).getSignatureExportSummary(900L);
        }
    }

    @Test
    void migrateSignatureBinding_delegatesWithAuditedRequestId() {
        DccControlledFileSignatureExportSummaryRespVO summary = new DccControlledFileSignatureExportSummaryRespVO();
        summary.setControlledFileId(900L);
        when(signatureManagementService.migratePublishedCopyBindings(900L, 99L, "REQ-MIGRATE-1"))
                .thenReturn(summary);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(DccControlledFileController.REQUEST_ID_HEADER, "REQ-MIGRATE-1");
        request.addHeader("User-Agent", "dcc-contract-test");
        request.setRemoteAddr("127.0.0.1");

        try (MockedStatic<SecurityFrameworkUtils> securityFrameworkUtilsMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityFrameworkUtilsMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);

            CommonResult<DccControlledFileSignatureExportSummaryRespVO> result =
                    controller.migrateSignatureBinding(900L, request);

            assertEquals(900L, result.getData().getControlledFileId());
            verify(signatureManagementService).migratePublishedCopyBindings(900L, 99L, "REQ-MIGRATE-1");
        }
    }

    @Test
    void rejectTask_delegatesToWorkflowService() {
        DccControlledFileRejectTaskReqVO reqVO = new DccControlledFileRejectTaskReqVO();
        reqVO.setTaskId("task-2");
        reqVO.setPassword("secret");
        reqVO.setReason("rejected");

        try (MockedStatic<SecurityFrameworkUtils> securityFrameworkUtilsMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityFrameworkUtilsMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            DccSignatureActionRespVO actionResp = new DccSignatureActionRespVO();
            actionResp.setTaskActionResult("REJECTED");
            actionResp.setSignatureId(1002L);
            actionResp.setControlledFileId(900L);
            actionResp.setRevisionId(900L);
            actionResp.setVersionNo("A.1");
            actionResp.setMeaningCode("REVIEW_REJECT");
            actionResp.setControlledCopyHashStatus("NOT_APPLICABLE");
            actionResp.setEvidenceStatus("VALID");
            actionResp.setEvidenceHashShort("52c91ab03d46");
            actionResp.setNextStatus("REJECTED");
            when(workflowService.rejectTask(99L, 900L, reqVO)).thenReturn(actionResp);

            CommonResult<DccSignatureActionRespVO> result = controller.rejectTask(900L, reqVO);

            assertEquals("REJECTED", result.getData().getTaskActionResult());
            assertEquals(1002L, result.getData().getSignatureId());
            assertEquals("REJECTED", result.getData().getNextStatus());
            verify(workflowService).rejectTask(99L, 900L, reqVO);
        }
    }

    @Test
    void returnTask_delegatesToWorkflowService() {
        DccControlledFileReturnTaskReqVO reqVO = new DccControlledFileReturnTaskReqVO();
        reqVO.setTaskId("task-3");
        reqVO.setPassword("secret");
        reqVO.setTargetTaskDefinitionKey("MATRIX_REVIEW");
        reqVO.setReason("退回补充");

        try (MockedStatic<SecurityFrameworkUtils> securityFrameworkUtilsMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityFrameworkUtilsMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);

            CommonResult<Boolean> result = controller.returnTask(900L, reqVO);

            assertTrue(Boolean.TRUE.equals(result.getData()));
            verify(workflowService).returnTask(99L, 900L, reqVO);
        }
    }

    @Test
    void transferTask_delegatesToWorkflowService() {
        DccControlledFileTransferTaskReqVO reqVO = new DccControlledFileTransferTaskReqVO();
        reqVO.setTaskId("task-2");
        reqVO.setPassword("secret");
        reqVO.setAssigneeUserId(101L);
        reqVO.setReason("转交评审");

        try (MockedStatic<SecurityFrameworkUtils> securityFrameworkUtilsMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityFrameworkUtilsMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);

            CommonResult<Boolean> result = controller.transferTask(900L, reqVO);

            assertTrue(Boolean.TRUE.equals(result.getData()));
            verify(workflowService).transferTask(99L, 900L, reqVO);
        }
    }

    @Test
    void createSignTask_delegatesToWorkflowService() {
        DccControlledFileCreateSignTaskReqVO reqVO = new DccControlledFileCreateSignTaskReqVO();
        reqVO.setTaskId("task-2");
        reqVO.setPassword("secret");
        reqVO.setUserIds(Set.of(101L, 102L));
        reqVO.setType("before");
        reqVO.setReason("加签确认");

        try (MockedStatic<SecurityFrameworkUtils> securityFrameworkUtilsMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityFrameworkUtilsMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);

            CommonResult<Boolean> result = controller.createSignTask(900L, reqVO);

            assertTrue(Boolean.TRUE.equals(result.getData()));
            verify(workflowService).createSignTask(99L, 900L, reqVO);
        }
    }

    @Test
    void replayMessageJobs_delegatesToReplayService() {
        DccControlledFileMessageJobReplayReqVO reqVO = new DccControlledFileMessageJobReplayReqVO();
        reqVO.setJobIds(List.of(1001L, 1002L));
        when(messageReplayService.replayMessageJobs(reqVO)).thenReturn(2);

        CommonResult<Integer> result = controller.replayMessageJobs(reqVO);

        assertTrue(Integer.valueOf(2).equals(result.getData()));
        verify(messageReplayService).replayMessageJobs(reqVO);
    }
}
