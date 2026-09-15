package cn.iocoder.yudao.module.dcc.service.file;

import static org.mockito.Mockito.doReturn;
import static org.mockito.ArgumentMatchers.anyLong;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_MAJOR_REVISION_NOT_ALLOWED;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskApproveReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskRejectReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskReturnReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskSignCreateReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskTransferReqVO;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance.BpmProcessInstanceCancelReqVO;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileApproveTaskReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccProjectProductRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileCreateSignTaskReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRejectTaskReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileReturnTaskReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileActionProjectionRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileCurrentVersionRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRoutePreviewRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRouteReadinessRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSubmitIterationReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSubmitReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileTrainingRecordReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileTaskReadinessReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileTaskReadinessRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileTransferTaskReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileWithdrawReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccSignatureActionRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccCategoryDirectoryBindingDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryPermissionRuleDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccFileDirectoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDistributionDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDistributionRecipientDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileRouteSnapshotDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSignatureDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.position.DccPositionAssignmentDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteNodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccCategoryDirectoryBindingMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryPermissionRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.directory.DccFileDirectoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileCheckoutMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionRecipientMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileRouteSnapshotMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSignatureMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.position.DccPositionAssignmentMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.route.DccCategoryApprovalRouteMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.route.DccCategoryApprovalRouteNodeMapper;
import cn.iocoder.yudao.module.dcc.enums.DccAccessTypeEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileProcessTypeEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileMasterStatusEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileChangeTypeEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStageCodeEnum;
import cn.iocoder.yudao.module.dcc.enums.DccDistributionMediumEnum;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import cn.iocoder.yudao.module.dcc.enums.DccProjectCodeStatusConstants;
import cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyAdminService;
import cn.iocoder.yudao.module.dcc.service.audit.DccControlledFileAccessAuditService;
import cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyPath;
import cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryAccessPermissionService;
import cn.iocoder.yudao.module.dcc.service.position.DccApprovalPositionRuntimeResolver;
import cn.iocoder.yudao.module.dcc.service.projectcode.DccProjectFileTemplateService;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketBoundFile;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketMarkBoundCommand;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketResolveCommand;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketService;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.mdm.api.product.MdmProductApi;
import cn.iocoder.yudao.module.mdm.api.product.dto.MdmProductRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_APPROVER_POST_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_FILE_NUMBER_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_DRAWING_PDF_FILE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_DRAWING_PDF_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PROCESS_TYPE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ROUTE_NOT_CONFIGURED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ROUTE_NOT_READY;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_FINAL_APPROVAL_NOT_READY;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SOURCE_FILE_TYPE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_STAMPED_PDF_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_DISTRIBUTION_DEPARTMENT_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_DISTRIBUTION_MEDIUM_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SUBMIT_DIRECTORY_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SUBMIT_DIRECTORY_NOT_LEAF;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_TASK_TARGET_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_TASK_STAGE_UNSUPPORTED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_TRAINING_RECORD_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_UPLOAD_TICKET_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_VERSION_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_VERSION_NOT_GREATER;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_WITHDRAW_NOT_ALLOWED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_WITHDRAWN_ACTION_NOT_ALLOWED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_WORKFLOW_IN_PROGRESS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.EXTERNAL_FILE_REVIEW_ENDPOINT_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_CATEGORY_DIRECTORY_BINDING_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_CATEGORY_UNCLASSIFIED_DIRECTORY_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_CATEGORY_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_TYPE_TAXONOMY_LEVEL_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_FILE_TEMPLATE_SELECTION_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_PROJECT_ACCESS_DENIED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.ROUTE_PREVIEW_APPROVER_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@MockitoSettings(strictness = Strictness.LENIENT)
class DccControlledFileWorkflowServiceImplTest extends BaseMockitoUnitTest {

    @Test
    void ordinaryUploadCannotCreateARevisionWorkingVersion() {
        DccControlledFileSubmitReqVO request = buildSubmitReqVO("B/1");
        request.setChangeType("REVISION");
        request.setRevisionSourceControlledFileId(900L);
        assertServiceException(() -> workflowService.createWorkingControlledFile(99L, request),
                CONTROLLED_FILE_MAJOR_REVISION_NOT_ALLOWED);
        org.mockito.Mockito.verifyNoInteractions(submitMutex, uploadTicketService, sourceOwnershipService);
    }

    @Test
    void workingSubmitMustReadCurrentLockedStatusInsteadOfEarlierSnapshot() {
        TenantContextHolder.setTenantId(31L);
        DccControlledFileDO stale = DccControlledFileDO.builder().id(900L).masterId(700L)
                .status("WORKING").requesterId(99L).categoryId(10L).dccProjectCodeId(3000L).build();
        DccControlledFileDO current = DccControlledFileDO.builder().id(900L).masterId(700L)
                .status("PENDING_MATRIX_REVIEW").requesterId(99L).build();
        when(controlledFileMapper.selectById(900L)).thenReturn(stale);
        when(controlledFileMasterMapper.selectByIdForUpdate(700L)).thenReturn(DccControlledFileMasterDO.builder().id(700L).build());
        doReturn(current).when(controlledFileMapper).selectByIdAndTenantForUpdate(31L, 900L);
        DccControlledFileSubmitIterationReqVO request = new DccControlledFileSubmitIterationReqVO();
        request.setIdempotencyKey("locked-submit");
        assertThrows(ServiceException.class, () -> workflowService.submitWorkingIteration(99L, 900L, request));
        verify(controlledFileMapper).selectByIdAndTenantForUpdate(31L, 900L);
        verify(bpmProcessInstanceApi, never()).createProcessInstance(any(), any());
    }

    @Test
    void approvalPdfUploadAuthorizesOnlyTheExactCurrentFinalTask() {
        mockTaskActionContext(900L, 99L, "task-4", "approveTask",
                DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL,
                DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL, false);
        ReflectionTestUtils.invokeMethod(workflowService, "validateApprovalPdfUpload",
                99L, 900L, "task-4", 10L, "dcc-approval:900:6:task-4:session-1");
        assertThrows(ServiceException.class, () -> ReflectionTestUtils.invokeMethod(workflowService,
                "validateApprovalPdfUpload", 99L, 900L, "task-4", 10L, "dcc-approval:901:6:task-4:session-1"));
        assertThrows(ServiceException.class, () -> ReflectionTestUtils.invokeMethod(workflowService,
                "validateApprovalPdfUpload", 99L, 900L, "task-4", 11L, "dcc-approval:900:6:task-4:session-1"));
    }

    @Test
    void approvalPdfUploadRejectsEarlierApprovalStages() {
        mockTaskActionContext(900L, 99L, "task-2", "reviewTask",
                DccControlledFileStatusEnum.PENDING_MATRIX_REVIEW,
                DccControlledFileStageCodeEnum.MATRIX_REVIEW, false);
        assertThrows(ServiceException.class, () -> ReflectionTestUtils.invokeMethod(workflowService,
                "validateApprovalPdfUpload", 99L, 900L, "task-2", 10L, "dcc-approval:900:6:task-2:session-1"));
    }

    @Test
    void finalApprovalRejectsATicketSessionOwnedByAnotherVersionBeforeSigning() {
        mockTaskActionContext(900L, 99L, "task-4", "approveTask",
                DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL,
                DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL, false);
        DccControlledFileApproveTaskReqVO reqVO = new DccControlledFileApproveTaskReqVO();
        reqVO.setTaskId("task-4");
        reqVO.setPassword("secret");
        reqVO.setReason("approved");
        reqVO.setSessionId("dcc-approval:901:6:task-4:session-1");
        reqVO.setStampedPdfUploadTicket("UT-OTHER-VERSION");
        useConfirmedDirectory(reqVO);
        assertFinalApprovalNotReady(() -> workflowService.approveTask(99L, 900L, reqVO), "盖章 PDF");
        verify(uploadTicketService, never()).resolveForBinding(any());
        org.mockito.Mockito.verifyNoInteractions(signatureVerificationService);
    }

    @Test
    void projectProductPreviewUsesTheBoundProductInsteadOfProjectCode() {
        when(projectCodeMapper.selectById(3000L)).thenReturn(DccProjectCodeDO.builder()
                .id(3000L).projectCode("PROJECT-CODE").projectName("Project")
                .productMasterId(5000L).status(DccProjectCodeStatusConstants.ENABLE).build());
        when(mdmProductApi.getEnabledDccProduct(5000L)).thenReturn(MdmProductRespDTO.builder()
                .id(5000L).dccProductCode("A1234567890123").nameCn("Product").build());
        Object result = ReflectionTestUtils.invokeMethod(workflowService, "previewProjectProduct", 99L, 3000L);
        assertEquals("A1234567890123", ReflectionTestUtils.getField(result, "productCode"));
        assertEquals(5000L, ReflectionTestUtils.getField(result, "productMasterId"));
        verify(projectAccessService).assertProjectEditorOrOwner(99L, 3000L);
    }

    @Test
    void projectProductPreviewRejectsAnInvalidBoundProduct() {
        when(projectCodeMapper.selectById(3000L)).thenReturn(DccProjectCodeDO.builder()
                .id(3000L).projectCode("PROJECT-CODE").projectName("Project")
                .productMasterId(5000L).status(DccProjectCodeStatusConstants.ENABLE).build());
        assertThrows(ServiceException.class, () -> ReflectionTestUtils.invokeMethod(workflowService,
                "previewProjectProduct", 99L, 3000L));
    }

    @Test
    void projectProductPreviewDoesNotMasqueradeProjectCodeAsProductCode() {
        when(projectCodeMapper.selectById(3000L)).thenReturn(DccProjectCodeDO.builder()
                .id(3000L).projectCode("PROJECT-CODE").projectName("Project")
                .status(DccProjectCodeStatusConstants.ENABLE).build());
        DccProjectProductRespVO result = workflowService.previewProjectProduct(99L, 3000L);
        assertNull(result.getProductMasterId());
        assertNull(result.getProductCode());
        assertNull(result.getProductName());
        assertEquals("UNBOUND", result.getSource());
    }

    @Test
    void relatedWriteRequiresProjectEditPermissionBeforeBinding() {
        ServiceException denied = new ServiceException(403, "project edit denied");
        doThrow(denied).when(projectAccessService).assertProjectEditorOrOwner(99L, 3000L);
        assertSame(denied, assertThrows(ServiceException.class, () -> ReflectionTestUtils.invokeMethod(
                workflowService, "validateAndBindRelatedFiles", 99L, 900L, 3000L, List.of(901L))));
        org.mockito.Mockito.verifyNoInteractions(relatedFileService);
    }

    @Test
    void relatedWriteRejectsAnInvisibleTargetBeforeBindingAnyRelation() {
        DccControlledFileDO visible = DccControlledFileDO.builder().id(901L).build();
        DccControlledFileDO hidden = DccControlledFileDO.builder().id(902L).build();
        when(controlledFileMapper.selectById(901L)).thenReturn(visible);
        when(controlledFileMapper.selectById(902L)).thenReturn(hidden);
        when(queryService.canViewFileName(99L, visible)).thenReturn(true);
        when(queryService.canViewFileName(99L, hidden)).thenReturn(false);
        assertThrows(ServiceException.class, () -> ReflectionTestUtils.invokeMethod(workflowService,
                "validateAndBindRelatedFiles", 99L, 900L, 3000L, List.of(901L, 902L)));
        org.mockito.Mockito.verifyNoInteractions(relatedFileService);
    }

    @Test
    void relatedWriteAllowsNameOnlyTargetsWithSeparateProjectEditPermission() {
        DccControlledFileDO visible = DccControlledFileDO.builder().id(901L).build();
        when(controlledFileMapper.selectById(901L)).thenReturn(visible);
        when(queryService.canViewFileName(99L, visible)).thenReturn(true);
        ReflectionTestUtils.invokeMethod(workflowService, "validateAndBindRelatedFiles",
                99L, 900L, 3000L, List.of(901L));
        verify(projectAccessService).assertProjectEditorOrOwner(99L, 3000L);
        verify(relatedFileService).validateAndBindRelatedFiles(900L, 3000L, List.of(901L));
    }

    private void assertFinalApprovalIgnoresRetiredInputs(boolean needTraining,
            List<DccControlledFileApproveTaskReqVO.DistributionScope> scopes, Long trainingFileId) {
        Task current = mockTaskActionContext(900L, 99L, "task-4", "DOC_CONTROL_APPROVAL",
                DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL,
                DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL, needTraining);
        when(bpmTaskService.getRunningTaskListByProcessInstanceId("proc-1", null, null))
                .thenReturn(List.of(current), List.of());
        var request = new DccControlledFileApproveTaskReqVO();
        request.setTaskId("task-4");
        request.setPassword("secret");
        request.setReason("approved");
        useStampedPdfTicket(request);
        useConfirmedDirectory(request, 22L);
        request.setSelectedDistributionScopes(scopes);
        request.setTrainingRecordFileId(trainingFileId);
        mockActionSignature(900L, "task-4", 99L, "APPROVE",
                actionSignature(1010L, "task-4", "APPROVE", "APPROVED", "DOC_CONTROL_APPROVAL_APPROVE"));

        workflowService.approveTask(99L, 900L, request);

        verify(signatureVerificationService).verifyPasswordAndCreateSignature(99L, 900L, "task-4",
                "DOC_CONTROL_APPROVAL", "APPROVE", "secret", "approved");
        ArgumentCaptor<DccControlledFileMasterDO> updated = ArgumentCaptor.forClass(DccControlledFileMasterDO.class);
        verify(controlledFileMasterMapper).updateById(updated.capture());
        assertEquals(700L, updated.getValue().getId());
        assertEquals(20L, updated.getValue().getDirectoryId());
        org.mockito.Mockito.verifyNoInteractions(distributionMapper, distributionRecipientMapper);
        verify(uploadTicketService).markBound(new DccUploadTicketMarkBoundCommand(
                "UT-STAMPED", 99L, 10L, "dcc-approval:900:6:task-4:session-stamped", "APPROVAL_PDF", 900L));
    }

    @Test
    void ordinaryFinalApprovalKeepsSeparateMasterWhenDefaultDirectoryContainsSameName() {
        Task current = mockTaskActionContext(900L, 99L, "task-4", "DOC_CONTROL_APPROVAL",
                DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL,
                DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL, Boolean.FALSE);
        when(bpmTaskService.getRunningTaskListByProcessInstanceId("proc-1", null, null))
                .thenReturn(List.of(current), List.of());
        var request = new DccControlledFileApproveTaskReqVO();
        useStampedPdfTicket(request);
        useConfirmedDirectory(request);
        request.setConfirmedDirectoryId(null);
        request.setTaskId("task-4");
        request.setPassword("secret");
        request.setReason("approve current logical identity");
        when(controlledFileMasterMapper.selectByCategoryIdAndDirectoryIdAndFileName(10L, 20L, "SOP-001"))
                .thenReturn(DccControlledFileMasterDO.builder().id(701L).dccProjectCodeId(999L)
                        .fileNumber("OTHER-001").fileName("SOP-001").build());
        mockActionSignature(900L, "task-4", 99L, "APPROVE",
                actionSignature(1010L, "task-4", "APPROVE", "APPROVED", "DOC_CONTROL_APPROVAL_APPROVE"));

        workflowService.approveTask(99L, 900L, request);

        ArgumentCaptor<DccControlledFileMasterDO> updated = ArgumentCaptor.forClass(DccControlledFileMasterDO.class);
        verify(controlledFileMasterMapper).updateById(updated.capture());
        assertEquals(700L, updated.getValue().getId());
        assertEquals(20L, updated.getValue().getDirectoryId());
        verify(distributionMapper, never()).insert(any(DccControlledFileDistributionDO.class));
    }

    @Test
    void ordinaryFinalApprovalReadinessUsesDefaultDirectoryWithoutTrainingOrDistribution() {
        mockTaskActionContext(900L, 99L, "task-4", "DOC_CONTROL_APPROVAL",
                DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL,
                DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL, Boolean.TRUE);
        var upload = new DccControlledFileApproveTaskReqVO();
        useStampedPdfTicket(upload);
        useConfirmedDirectory(upload);
        var request = new cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileTaskReadinessReqVO();
        request.setTaskId("task-4");
        request.setSessionId(upload.getSessionId());
        request.setStampedPdfUploadTicket(upload.getStampedPdfUploadTicket());

        var readiness = workflowService.getTaskActionReadiness(99L, 900L, request);

        assertTrue(readiness.getReady(), () -> String.valueOf(readiness.getBlockers()));
        assertTrue(readiness.getBlockers().isEmpty());
        org.mockito.Mockito.verifyNoInteractions(distributionMapper, distributionRecipientMapper);
    }

    @Test
    void ordinaryFinalApprovalReadinessRejectsMissingDefaultDirectory() {
        mockTaskActionContext(900L, 99L, "task-4", "DOC_CONTROL_APPROVAL",
                DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL,
                DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL, Boolean.FALSE);
        var upload = new DccControlledFileApproveTaskReqVO();
        useStampedPdfTicket(upload);
        var request = new cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileTaskReadinessReqVO();
        request.setTaskId("task-4");
        request.setSessionId(upload.getSessionId());
        request.setStampedPdfUploadTicket(upload.getStampedPdfUploadTicket());

        var readiness = workflowService.getTaskActionReadiness(99L, 900L, request);

        assertFalse(readiness.getReady());
        assertEquals(List.of("DEFAULT_DIRECTORY_INVALID"), readiness.getBlockers().stream()
                .map(cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileTaskReadinessBlockerRespVO::getReasonCode).toList());
    }

    @Mock
    private DccFileCategoryMapper categoryMapper;
    @Mock
    private DccCategoryDirectoryBindingMapper categoryDirectoryBindingMapper;
    @Mock
    private DccCategoryApprovalRouteMapper routeMapper;
    @Mock
    private DccCategoryApprovalRouteNodeMapper routeNodeMapper;
    @Mock
    private DccPositionAssignmentMapper positionAssignmentMapper;
    @Mock
    private DccControlledFileMapper controlledFileMapper;
    @Mock
    private DccControlledFileCheckoutMapper checkoutMapper;
    @Mock
    private DccFileDirectoryMapper directoryMapper;
    @Mock
    private DccControlledFileMasterMapper controlledFileMasterMapper;
    @Mock
    private DccControlledFileRouteSnapshotMapper routeSnapshotMapper;
    @Mock
    private DccControlledFileDistributionMapper distributionMapper;
    @Mock
    private DccControlledFileDistributionRecipientMapper distributionRecipientMapper;
    @Mock
    private DccControlledFileSignatureMapper signatureMapper;
    @Mock
    private DccControlledFileSignatureBindingService signatureBindingService;
    @Mock
    private DccControlledFileSourceOwnershipService sourceOwnershipService;
    @Mock
    private DccFileCategoryPermissionRuleMapper permissionRuleMapper;
    @Mock
    private FileMapper fileMapper;
    @Mock
    private FileService fileService;
    @Mock
    private DccProjectCodeMapper projectCodeMapper;
    @Mock
    private DccFileTypeTaxonomyAdminService fileTypeTaxonomyAdminService;
    @Mock
    private DccControlledFileQueryService queryService;
    @Mock
    private DccControlledFileRelatedFileService relatedFileService;
    @Mock
    private BpmProcessInstanceApi bpmProcessInstanceApi;
    @Mock
    private BpmProcessInstanceService bpmProcessInstanceService;
    @Mock
    private BpmTaskService bpmTaskService;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private DccApprovalParticipantPostValidator approvalParticipantPostValidator;
    @Mock
    private DccElectronicSignatureAuthorizationService signatureAuthorizationService;
    @Mock
    private DccElectronicSignatureImageService signatureImageService;
    @Mock
    private DccDirectoryAccessPermissionService directoryAccessPermissionService;
    @Mock
    private DccSignatureVerificationService signatureVerificationService;
    @Mock
    private DccApprovalPositionRuntimeResolver positionRuntimeResolver;
    @Mock
    private DccControlledFileFinalizationService finalizationService;
    @Mock
    private DccUploadTicketService uploadTicketService;
    @Mock
    private PermissionApi permissionApi;
    @Mock
    private MdmProductApi mdmProductApi;
    @Mock
    private DccControlledFileCategoryPermissionSupport permissionSupport;
    @Mock
    private DccControlledContentAdapter platformAdapter;
    @Mock
    private DccControlledFileAccessAuditService lifecycleAuditService;
    @Mock
    private DccProjectFileTemplateService projectFileTemplateService;
    @Mock
    private DccProjectAccessService projectAccessService;
    @Mock
    private DccControlledFileCategoryPermissionSupport categoryPermissionSupport;
    @Mock
    private DccControlledFileSubmitMutex submitMutex;

    private final DccControlledFileApprovalRouteAssigneeResolver approvalRouteAssigneeResolver =
            new DccControlledFileApprovalRouteAssigneeResolver();
    private final DccControlledFileRouteReadinessService routeReadinessService =
            new DccControlledFileRouteReadinessService();

    @InjectMocks
    private DccControlledFileWorkflowServiceImpl workflowService;

    @BeforeEach
    void setDefaultUploadTicketBindings() {
        lenient().doAnswer(invocation -> controlledFileMapper.selectById(invocation.<Long>getArgument(1)))
                .when(controlledFileMapper).selectByIdAndTenantForUpdate(anyLong(), anyLong());
        lenient().doAnswer(invocation -> controlledFileMapper.selectListByMasterId(invocation.<Long>getArgument(0)))
                .when(controlledFileMapper).selectListByMasterIdForUpdate(anyLong());
        TenantContextHolder.setTenantId(1L);
        lenient().doNothing().when(projectAccessService).assertProjectOwner(any(), any());
        lenient().doNothing().when(projectAccessService).assertProjectEditorOrOwner(any(), any());
        lenient().when(categoryPermissionSupport.hasCategoryPermission(any(), any(), any())).thenReturn(true);
        lenient().when(submitMutex.execute(any(), any())).thenAnswer(invocation ->
                ((Supplier<?>) invocation.getArgument(1)).get());
        ReflectionTestUtils.setField(approvalRouteAssigneeResolver, "routeMapper", routeMapper);
        ReflectionTestUtils.setField(approvalRouteAssigneeResolver, "routeNodeMapper", routeNodeMapper);
        ReflectionTestUtils.setField(approvalRouteAssigneeResolver, "positionAssignmentMapper", positionAssignmentMapper);
        ReflectionTestUtils.setField(approvalRouteAssigneeResolver, "positionRuntimeResolver", positionRuntimeResolver);
        ReflectionTestUtils.setField(approvalRouteAssigneeResolver, "adminUserApi", adminUserApi);
        ReflectionTestUtils.setField(approvalRouteAssigneeResolver, "approvalParticipantPostValidator",
                approvalParticipantPostValidator);
        ReflectionTestUtils.setField(workflowService, "approvalRouteAssigneeResolver", approvalRouteAssigneeResolver);
        ReflectionTestUtils.setField(routeReadinessService, "routeAssigneeResolver", approvalRouteAssigneeResolver);
        ReflectionTestUtils.setField(routeReadinessService, "adminUserApi", adminUserApi);
        ReflectionTestUtils.setField(routeReadinessService, "permissionApi", permissionApi);
        ReflectionTestUtils.setField(routeReadinessService, "signatureAuthorizationService",
                signatureAuthorizationService);
        ReflectionTestUtils.setField(routeReadinessService, "signatureImageService", signatureImageService);
        ReflectionTestUtils.setField(workflowService, "routeReadinessService", routeReadinessService);
        lenient().when(adminUserApi.getUserList(any())).thenAnswer(invocation -> {
            Collection<Long> userIds = invocation.getArgument(0);
            return userIds.stream()
                    .map(userId -> new AdminUserRespDTO().setId(userId).setStatus(0).setNickname("用户" + userId)
                            .setPostIds(java.util.Set.of(10_000L + userId)))
                    .toList();
        });
        lenient().when(signatureAuthorizationService.getAuthorizationMap(any())).thenAnswer(invocation -> {
            Collection<Long> userIds = invocation.getArgument(0);
            return userIds.stream().collect(java.util.stream.Collectors.toMap(userId -> userId, userId -> true));
        });
        lenient().when(signatureImageService.requireActiveSnapshot(any(Long.class))).thenReturn(
                DccElectronicSignatureImageSnapshot.builder()
                        .imageId(501L).fileId(1501L).sha256("valid-sha256")
                        .imageStatus("ENABLED").verifiedStatus("VALID").build());
        lenient().when(uploadTicketService.resolveForBinding(any(DccUploadTicketResolveCommand.class)))
                .thenAnswer(invocation -> {
                    DccUploadTicketResolveCommand command = invocation.getArgument(0);
                    if ("DRAWING_PDF".equals(command.purpose())) {
                        return new DccUploadTicketBoundFile(command.uploadTicket(), 101L,
                                "SOP-001.pdf", "application/pdf", 8L);
                    }
                    return new DccUploadTicketBoundFile(command.uploadTicket(), 100L,
                            "SOP-001.docx",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", 4L);
                });
        lenient().when(sourceOwnershipService.prepareSubmissionSource(any(Long.class), any(Boolean.class)))
                .thenAnswer(invocation -> {
                    Long sourceFileId = invocation.getArgument(0);
                    return new DccControlledFilePreparedSource(sourceFileId, sourceFileId,
                            "test-source-sha256", false);
                });
        lenient().when(permissionApi.hasAnyPermissions(any(Long.class), any(String[].class))).thenReturn(true);
        lenient().when(controlledFileMasterMapper.selectByIdForUpdate(any(Long.class))).thenAnswer(invocation ->
                DccControlledFileMasterDO.builder().id(invocation.getArgument(0)).build());
        lenient().when(projectCodeMapper.selectById(3000L)).thenReturn(DccProjectCodeDO.builder()
                .id(3000L)
                .projectName("验证项目")
                .projectCode("PRJ-20260719")
                .status(DccProjectCodeStatusConstants.ENABLE)
                .build());
        lenient().when(fileTypeTaxonomyAdminService.resolveActivePath(8803L))
                .thenReturn(defaultTaxonomyPath());
        lenient().when(fileTypeTaxonomyAdminService.listActiveDescendantIds(8803L))
                .thenReturn(List.of(8803L));
        lenient().when(fileTypeTaxonomyAdminService.listActiveDescendantPaths(8803L))
                .thenReturn(List.of(
                        new DccFileTypeTaxonomyPath(8803L, "一级", "二级", "三级", "四级", null)));
        lenient().when(signatureVerificationService.verifyPasswordAndCreateSignature(
                any(Long.class), any(Long.class), any(String.class), any(String.class), any(String.class),
                any(String.class), any(String.class))).thenAnswer(invocation -> {
            Long controlledFileId = invocation.getArgument(1);
            String taskId = invocation.getArgument(2);
            String stageCode = invocation.getArgument(3);
            String actionType = invocation.getArgument(4);
            return DccUnifiedSignatureResult.builder()
                    .signatureId(defaultUnifiedSignatureId(taskId))
                    .controlledFileId(controlledFileId)
                    .revisionId(controlledFileId + 1)
                    .versionNo("A.1")
                    .meaningCode(defaultMeaningCode(stageCode, actionType))
                    .controlledCopyHashStatus("NOT_APPLICABLE")
                    .evidenceStatus("VALID")
                    .evidenceHash("abcdef1234567890")
                    .signedAt(LocalDateTime.of(2026, 9, 8, 10, 0, 0))
                    .build();
        });
    }

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    private static Long defaultUnifiedSignatureId(String taskId) {
        return switch (taskId) {
            case "task-1" -> 1001L;
            case "task-3" -> 1003L;
            case "task-4" -> 1004L;
            case "task-9" -> 1009L;
            default -> 1000L;
        };
    }

    private static String defaultMeaningCode(String stageCode, String actionType) {
        if ("DISTRIBUTION_ACK".equals(actionType) || "DISTRIBUTION_SIGN".equals(actionType)) {
            return actionType;
        }
        return stageCode + "_" + actionType;
    }

    @Test
    void submitControlledFile_withCategoryUploadPermission_success() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("A/1");
        reqVO.setChangeType(DccControlledFileChangeTypeEnum.NEW.getCode());
        mockCommonSubmitDependencies();
        when(routeNodeMapper.selectListByRouteId(30L)).thenReturn(List.of(
                routeNode(1, DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode(), "Doc Control Review", "POSITION", 50L),
                routeNode(2, DccControlledFileStageCodeEnum.MATRIX_REVIEW.getCode(), "Matrix Review", "POSITION", 51L),
                routeNode(3, DccControlledFileStageCodeEnum.MATRIX_APPROVAL.getCode(), "Matrix Approval", "POSITION", 52L),
                routeNode(4, DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL.getCode(), "Doc Control Approval", "POSITION", 53L)));
        when(positionAssignmentMapper.selectActiveListByPositionId(50L)).thenReturn(List.of(
                DccPositionAssignmentDO.builder().id(60L).positionId(50L).assignmentType("POST").systemPostId(500L).active(Boolean.TRUE).build()));
        when(positionAssignmentMapper.selectActiveListByPositionId(51L)).thenReturn(List.of(
                DccPositionAssignmentDO.builder().id(61L).positionId(51L).assignmentType("POST").systemPostId(501L).active(Boolean.TRUE).build(),
                DccPositionAssignmentDO.builder().id(62L).positionId(51L).assignmentType("POST").systemPostId(502L).active(Boolean.TRUE).build()));
        when(positionAssignmentMapper.selectActiveListByPositionId(52L)).thenReturn(List.of(
                DccPositionAssignmentDO.builder().id(63L).positionId(52L).assignmentType("POST").systemPostId(503L).active(Boolean.TRUE).build(),
                DccPositionAssignmentDO.builder().id(64L).positionId(52L).assignmentType("POST").systemPostId(504L).active(Boolean.TRUE).build()));
        when(positionAssignmentMapper.selectActiveListByPositionId(53L)).thenReturn(List.of(
                DccPositionAssignmentDO.builder().id(65L).positionId(53L).assignmentType("POST").systemPostId(505L).active(Boolean.TRUE).build()));
        when(adminUserApi.getUserListByPostIds(List.of(500L))).thenReturn(List.of(new AdminUserRespDTO().setId(200L).setStatus(0)));
        when(adminUserApi.getUserListByPostIds(List.of(501L))).thenReturn(List.of(new AdminUserRespDTO().setId(201L).setStatus(0)));
        when(adminUserApi.getUserListByPostIds(List.of(502L))).thenReturn(List.of(new AdminUserRespDTO().setId(202L).setStatus(0)));
        when(adminUserApi.getUserListByPostIds(List.of(503L))).thenReturn(List.of(new AdminUserRespDTO().setId(203L).setStatus(0)));
        when(adminUserApi.getUserListByPostIds(List.of(504L))).thenReturn(List.of(new AdminUserRespDTO().setId(204L).setStatus(0)));
        when(adminUserApi.getUserListByPostIds(List.of(505L))).thenReturn(List.of(new AdminUserRespDTO().setId(205L).setStatus(0)));
        doAnswer(invocation -> {
            DccControlledFileMasterDO master = invocation.getArgument(0);
            master.setId(700L);
            return 1;
        }).when(controlledFileMasterMapper).insert(any(DccControlledFileMasterDO.class));
        doAnswer(invocation -> {
            DccControlledFileDO file = invocation.getArgument(0);
            file.setId(900L);
            return 1;
        }).when(controlledFileMapper).insert(any(DccControlledFileDO.class));
        when(bpmProcessInstanceApi.createProcessInstance(any(Long.class), any())).thenReturn("proc-1");

        Long fileId = workflowService.submitControlledFile(99L, reqVO);

        assertEquals(900L, fileId);
        verify(relatedFileService).validateAndBindRelatedFiles(900L, 3000L, null);
        ArgumentCaptor<DccControlledFileMasterDO> masterCaptor = ArgumentCaptor.forClass(DccControlledFileMasterDO.class);
        verify(controlledFileMasterMapper).insert(masterCaptor.capture());
        verify(controlledFileMasterMapper).selectByIdForUpdate(700L);
        assertEquals("SOP-001", masterCaptor.getValue().getFileName());
        assertEquals("SOP-001", masterCaptor.getValue().getFileNumber());
        assertEquals(1L, masterCaptor.getValue().getTenantId());
        assertEquals(3000L, masterCaptor.getValue().getDccProjectCodeId());
        assertEquals(8803L, masterCaptor.getValue().getFileTypeTaxonomyLeafId());
        assertEquals("SOP-001", masterCaptor.getValue().getNormalizedFileNumber());
        ArgumentCaptor<DccControlledFileDO> fileCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).insert(fileCaptor.capture());
        assertEquals(700L, fileCaptor.getValue().getMasterId());
        assertEquals(21L, fileCaptor.getValue().getDirectoryId());
        assertEquals("SOP-001", fileCaptor.getValue().getFileName());
        assertEquals("SOP-001", fileCaptor.getValue().getFileNumber());
        assertEquals(DccControlledFileChangeTypeEnum.NEW.getCode(), fileCaptor.getValue().getChangeType());
        assertNull(fileCaptor.getValue().getProductMasterId());
        assertEquals(100L, fileCaptor.getValue().getSourceFileId());
        assertEquals("test-source-sha256", fileCaptor.getValue().getSourceSha256());
        assertEquals(101L, fileCaptor.getValue().getDrawingPdfFileId());
        assertNull(fileCaptor.getValue().getProductCode());
        assertNull(fileCaptor.getValue().getProductName());
        assertEquals(3000L, fileCaptor.getValue().getDccProjectCodeId());
        assertEquals(8803L, fileCaptor.getValue().getFileTypeTaxonomyId());
        assertEquals("一级", fileCaptor.getValue().getFileTypeLevel1());
        assertEquals("二级", fileCaptor.getValue().getFileTypeLevel2());
        assertEquals("三级", fileCaptor.getValue().getFileTypeLevel3());
        assertEquals("四级", fileCaptor.getValue().getFileTypeLevel4());
        assertNull(fileCaptor.getValue().getFileTypeLevel5());
        assertEquals(Boolean.FALSE, fileCaptor.getValue().getNeedTraining());
        assertEquals("CONTROLLED_FILE", fileCaptor.getValue().getProcessType());
        assertEquals("A/1", fileCaptor.getValue().getVersionNo());
        assertEquals("A", fileCaptor.getValue().getRevisionCode());
        assertEquals(1, fileCaptor.getValue().getIterationNo());
        assertEquals(DccControlledFileStatusEnum.PENDING_DOC_CONTROL_REVIEW.getStatus(), fileCaptor.getValue().getStatus());
        assertEquals(LocalDate.of(2026, 5, 13), fileCaptor.getValue().getEffectiveDate());
        verify(projectFileTemplateService).validateUploadSelection(3000L, 8803L, "SOP-001");
        ArgumentCaptor<DccControlledFileRouteSnapshotDO> snapshotCaptor =
                ArgumentCaptor.forClass(DccControlledFileRouteSnapshotDO.class);
        verify(routeSnapshotMapper, org.mockito.Mockito.times(4)).insert(snapshotCaptor.capture());
        assertEquals(List.of(
                        DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode(),
                        DccControlledFileStageCodeEnum.MATRIX_REVIEW.getCode(),
                        DccControlledFileStageCodeEnum.MATRIX_APPROVAL.getCode(),
                        DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL.getCode()),
                snapshotCaptor.getAllValues().stream().map(DccControlledFileRouteSnapshotDO::getStageCode).toList());
        assertEquals("51", snapshotCaptor.getAllValues().get(1).getCandidateSourceIds());
        assertEquals("52", snapshotCaptor.getAllValues().get(2).getCandidateSourceIds());
        ArgumentCaptor<BpmProcessInstanceCreateReqDTO> processCaptor = ArgumentCaptor.forClass(BpmProcessInstanceCreateReqDTO.class);
        verify(bpmProcessInstanceApi).createProcessInstance(eq(99L), processCaptor.capture());
        @SuppressWarnings("unchecked")
        Map<String, List<Long>> startUserSelectAssignees = (Map<String, List<Long>>) processCaptor.getValue()
                .getVariables().get(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_START_USER_SELECT_ASSIGNEES);
        @SuppressWarnings("unchecked")
        Map<String, List<Long>> nextAssignees = (Map<String, List<Long>>) processCaptor.getValue()
                .getVariables().get(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_APPROVE_USER_SELECT_ASSIGNEES);
        assertEquals(List.of(200L), processCaptor.getValue().getStartUserSelectAssignees()
                .get(DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode()));
        assertEquals(List.of(200L), startUserSelectAssignees.get(DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode()));
        assertEquals(List.of(201L, 202L), nextAssignees.get(DccControlledFileStageCodeEnum.MATRIX_REVIEW.getCode()));
        assertEquals(List.of(203L, 204L), nextAssignees.get(DccControlledFileStageCodeEnum.MATRIX_APPROVAL.getCode()));
        assertEquals(List.of(205L), nextAssignees.get(DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL.getCode()));
        verify(controlledFileMapper).updateById(any(DccControlledFileDO.class));
        verify(platformAdapter).recordSubmitted(
                org.mockito.ArgumentMatchers.argThat(file -> Long.valueOf(900L).equals(file.getId())
                        && "proc-1".equals(file.getProcessInstanceId())),
                eq(99L), eq("proc-1"));
        verify(uploadTicketService).markBound(new DccUploadTicketMarkBoundCommand(
                "UT-ORIGINAL", 99L, 10L, "session-1", "SOURCE", 900L));
        verify(uploadTicketService).markBound(new DccUploadTicketMarkBoundCommand(
                "UT-DRAWING", 99L, 10L, "session-1", "DRAWING_PDF", 900L));
        verify(permissionSupport, never()).hasCategoryPermission(any(Long.class), any(Long.class),
                any(DccFileCategoryPermissionActionEnum.class));
    }

    @Test
    void submitControlledFile_missingProjectOrTaxonomy_failsBeforeInsert() {
        DccControlledFileSubmitReqVO missingProjectReqVO = buildSubmitReqVO("A/1");
        missingProjectReqVO.setDccProjectCodeId(null);

        assertServiceException(() -> workflowService.submitControlledFile(99L, missingProjectReqVO),
                CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING);

        DccControlledFileSubmitReqVO missingTaxonomyReqVO = buildSubmitReqVO("A/1");
        missingTaxonomyReqVO.setFileTypeTaxonomyId(null);

        assertServiceException(() -> workflowService.submitControlledFile(99L, missingTaxonomyReqVO),
                CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING);
        verify(controlledFileMapper, never()).insert(any(DccControlledFileDO.class));
        verify(bpmProcessInstanceApi, never()).createProcessInstance(any(Long.class), any());
    }

    @Test
    void submitControlledFile_sameIdempotencyPayload_returnsExistingFile() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("A/1");
        String payloadHash = cn.hutool.crypto.digest.DigestUtil.sha256Hex(
                cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(reqVO));
        when(controlledFileMapper.selectBySubmitIdempotency(1L, 99L, "dcc-submit-test-1"))
                .thenReturn(DccControlledFileDO.builder().id(990L).submitPayloadHash(payloadHash).build());

        assertEquals(990L, workflowService.submitControlledFile(99L, reqVO));

        verify(controlledFileMapper, never()).insert(any(DccControlledFileDO.class));
        verify(bpmProcessInstanceApi, never()).createProcessInstance(any(Long.class), any());
    }

    @Test
    void submitControlledFile_sameIdempotencyKeyDifferentPayload_rejectsConflict() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("A/1");
        when(controlledFileMapper.selectBySubmitIdempotency(1L, 99L, "dcc-submit-test-1"))
                .thenReturn(DccControlledFileDO.builder().id(990L).submitPayloadHash("different").build());

        assertServiceException(() -> workflowService.submitControlledFile(99L, reqVO),
                cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SUBMIT_IDEMPOTENCY_CONFLICT);

        verify(controlledFileMapper, never()).insert(any(DccControlledFileDO.class));
    }

    @Test
    void submitControlledFile_projectTemplateMismatchFailsBeforeInsert() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("A/1");
        doThrow(new ServiceException(PROJECT_FILE_TEMPLATE_SELECTION_INVALID))
                .when(projectFileTemplateService).validateUploadSelection(3000L, 8803L, "SOP-001");

        assertServiceException(() -> workflowService.submitControlledFile(99L, reqVO),
                PROJECT_FILE_TEMPLATE_SELECTION_INVALID);

        verify(controlledFileMapper, never()).insert(any(DccControlledFileDO.class));
        verify(bpmProcessInstanceApi, never()).createProcessInstance(any(Long.class), any());
    }

    @Test
    void createWorkingControlledFile_createsA1WithoutRouteSnapshotOrBpm() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO(null);
        mockCommonSubmitDependencies();
        doAnswer(invocation -> {
            DccControlledFileDO file = invocation.getArgument(0);
            file.setId(920L);
            return 1;
        }).when(controlledFileMapper).insert(any(DccControlledFileDO.class));

        Long fileId = workflowService.createWorkingControlledFile(99L, reqVO);

        assertEquals(920L, fileId);
        ArgumentCaptor<DccControlledFileDO> fileCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).insert(fileCaptor.capture());
        assertEquals("A/1", fileCaptor.getValue().getVersionNo());
        assertEquals(DccControlledFileStatusEnum.WORKING.getStatus(), fileCaptor.getValue().getStatus());
        assertNull(fileCaptor.getValue().getSubmitterId());
        assertNull(fileCaptor.getValue().getSubmittedTime());
        assertNull(fileCaptor.getValue().getProcessInstanceId());
        verify(routeSnapshotMapper, never()).insert(any(DccControlledFileRouteSnapshotDO.class));
        verify(bpmProcessInstanceApi, never()).createProcessInstance(any(), any());
        verify(platformAdapter, never()).recordSubmitted(any(), any(), any());
    }

    @Test
    void createWorkingControlledFile_sameKeyAndPayloadReturnsExistingWorkingIteration() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO(null);
        String payloadHash = cn.hutool.crypto.digest.DigestUtil.sha256Hex("CREATE_WORKING:WORKING_CREATE:"
                + cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(reqVO));
        when(controlledFileMapper.selectByCreationIdempotency(1L, 99L, "dcc-submit-test-1"))
                .thenReturn(DccControlledFileDO.builder().id(920L).creationPayloadHash(payloadHash).build());

        assertEquals(920L, workflowService.createWorkingControlledFile(99L, reqVO));

        verify(controlledFileMasterMapper, never()).insert(any(DccControlledFileMasterDO.class));
        verify(controlledFileMapper, never()).insert(any(DccControlledFileDO.class));
    }

    @Test
    void submitControlledFile_businessFailureMustNotReplayAnotherRecord() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("A/1");
        String payloadHash = cn.hutool.crypto.digest.DigestUtil.sha256Hex(
                cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(reqVO));
        mockCommonSubmitDependencies();
        doThrow(exception(PROJECT_FILE_TEMPLATE_SELECTION_INVALID)).when(projectFileTemplateService)
                .validateUploadSelection(3000L, 8803L, "SOP-001");
        when(controlledFileMapper.selectBySubmitIdempotencyForUpdate(1L, 99L, "dcc-submit-test-1"))
                .thenReturn(DccControlledFileDO.builder().id(921L).submitPayloadHash(payloadHash).build());

        assertServiceException(() -> workflowService.submitControlledFile(99L, reqVO),
                PROJECT_FILE_TEMPLATE_SELECTION_INVALID);

        verify(controlledFileMapper, never()).insert(any(DccControlledFileDO.class));
        verify(bpmProcessInstanceApi, never()).createProcessInstance(any(), any());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"relation", "ticket", "relation-duplicate"})
    void createWorkingControlledFile_postInsertFailureMustNotReplayItsOwnRow(String failurePoint) {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO(null);
        mockCommonSubmitDependencies();
        java.util.concurrent.atomic.AtomicReference<DccControlledFileDO> inserted =
                new java.util.concurrent.atomic.AtomicReference<>();
        doAnswer(invocation -> {
            DccControlledFileDO file = invocation.getArgument(0);
            file.setId(920L);
            inserted.set(file);
            return 1;
        }).when(controlledFileMapper).insert(any(DccControlledFileDO.class));
        when(controlledFileMapper.selectByCreationIdempotencyForUpdate(1L, 99L, "dcc-submit-test-1"))
                .thenAnswer(ignored -> inserted.get());
        RuntimeException failure = "relation-duplicate".equals(failurePoint)
                ? new org.springframework.dao.DuplicateKeyException("related file uniqueness violated")
                : new IllegalStateException("binding rejected");
        if ("ticket".equals(failurePoint)) {
            doThrow(failure).when(uploadTicketService).markBound(any());
        } else {
            doThrow(failure).when(relatedFileService).validateAndBindRelatedFiles(eq(920L), eq(3000L), any());
        }

        assertSame(failure, assertThrows(RuntimeException.class,
                () -> workflowService.createWorkingControlledFile(99L, reqVO)));
        verify(bpmProcessInstanceApi, never()).createProcessInstance(any(), any());
    }

    @Test
    void createWorkingControlledFile_rootInsertRaceReturnsCommittedWinnerWithoutBindingAgain() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO(null);
        String hash = cn.hutool.crypto.digest.DigestUtil.sha256Hex("CREATE_WORKING:WORKING_CREATE:"
                + cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(reqVO));
        mockCommonSubmitDependencies();
        doThrow(new org.springframework.dao.DuplicateKeyException("creation idempotency key"))
                .when(controlledFileMapper).insert(any(DccControlledFileDO.class));
        when(controlledFileMapper.selectByCreationIdempotencyForUpdate(1L, 99L, "dcc-submit-test-1"))
                .thenReturn(DccControlledFileDO.builder().id(920L).creationPayloadHash(hash).build());

        assertEquals(920L, workflowService.createWorkingControlledFile(99L, reqVO));
        verify(relatedFileService, never()).validateAndBindRelatedFiles(any(), any(), any());
        verify(uploadTicketService, never()).markBound(any());
    }

    @Test
    void submitWorkingIteration_repeatedCheckinsCloseReturnedAncestorAndUnifiedCandidateBeforeNewSubmission() {
        mockCommonSubmitDependencies();
        mockFourStageRoute();
        when(permissionApi.hasAnyPermissions(eq(200L), any(String[].class))).thenReturn(true);
        DccControlledFileMasterDO master = DccControlledFileMasterDO.builder()
                .id(700L).categoryId(10L).directoryId(21L).fileName("SOP-001").fileNumber("SOP-001")
                .status(DccControlledFileMasterStatusEnum.ACTIVE_CHAIN.getCode()).build();
        DccControlledFileDO returnedA1 = DccControlledFileDO.builder()
                .id(900L).masterId(700L).categoryId(10L).directoryId(21L)
                .fileName("SOP-001").fileNumber("SOP-001").versionNo("A/1").revisionCode("A").iterationNo(1)
                .dccProjectCodeId(3000L).fileTypeTaxonomyId(8803L).requesterId(99L)
                .changeType(DccControlledFileChangeTypeEnum.NEW.getCode())
                .status(DccControlledFileStatusEnum.PENDING_APPLICANT_REWORK.getStatus())
                .processInstanceId("proc-returned-a1").build();
        DccControlledFileDO firstCheckinA2 = DccControlledFileDO.builder()
                .id(901L).masterId(700L).categoryId(10L).directoryId(21L)
                .fileName("SOP-001").fileNumber("SOP-001").versionNo("A/2").revisionCode("A").iterationNo(2)
                .dccProjectCodeId(3000L).fileTypeTaxonomyId(8803L).requesterId(99L)
                .changeType(DccControlledFileChangeTypeEnum.NEW.getCode())
                .status(DccControlledFileStatusEnum.WORKING.getStatus())
                .predecessorControlledFileId(900L).build();
        DccControlledFileDO secondCheckinA3 = DccControlledFileDO.builder()
                .id(902L).masterId(700L).categoryId(10L).directoryId(21L)
                .fileName("SOP-001").fileNumber("SOP-001").versionNo("A/3").revisionCode("A").iterationNo(3)
                .dccProjectCodeId(3000L).fileTypeTaxonomyId(8803L).requesterId(99L)
                .changeType(DccControlledFileChangeTypeEnum.NEW.getCode())
                .status(DccControlledFileStatusEnum.WORKING.getStatus())
                .predecessorControlledFileId(901L).build();
        when(controlledFileMapper.selectById(902L)).thenReturn(secondCheckinA3);
        when(controlledFileMasterMapper.selectByIdForUpdate(700L)).thenReturn(master);
        when(controlledFileMapper.selectListByMasterId(700L)).thenReturn(List.of(returnedA1, firstCheckinA2, secondCheckinA3));
        when(controlledFileMapper.updateById(any(DccControlledFileDO.class))).thenReturn(1);
        when(bpmProcessInstanceApi.createProcessInstance(eq(99L), any(BpmProcessInstanceCreateReqDTO.class)))
                .thenReturn("proc-new-a3");
        DccControlledFileSubmitIterationReqVO reqVO = new DccControlledFileSubmitIterationReqVO();
        reqVO.setIdempotencyKey("submit-a3-after-rework");
        when(controlledFileMapper.claimWorkingIterationSubmission(eq(1L), eq(99L), any())).thenReturn(1);

        Long result = workflowService.submitWorkingIteration(99L, 902L, reqVO);

        assertEquals(902L, result);
        ArgumentCaptor<BpmProcessInstanceCancelReqVO> cancelCaptor =
                ArgumentCaptor.forClass(BpmProcessInstanceCancelReqVO.class);
        verify(bpmProcessInstanceService).cancelProcessInstanceByStartUser(eq(99L), cancelCaptor.capture());
        assertEquals("proc-returned-a1", cancelCaptor.getValue().getId());
        assertTrue(cancelCaptor.getValue().getReason().contains("A/3"));
        ArgumentCaptor<DccControlledFileDO> updateCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper, org.mockito.Mockito.times(2)).updateById(updateCaptor.capture());
        verify(controlledFileMapper).claimWorkingIterationSubmission(eq(1L), eq(99L), any());
        assertTrue(updateCaptor.getAllValues().stream().anyMatch(update -> Long.valueOf(900L).equals(update.getId())
                && DccControlledFileStatusEnum.WITHDRAWN.getStatus().equals(update.getStatus())
                && update.getRejectReason().contains("A/3")));
        org.mockito.InOrder platformOrder = org.mockito.Mockito.inOrder(platformAdapter);
        platformOrder.verify(platformAdapter).recordWithdrawn(
                org.mockito.ArgumentMatchers.argThat(file -> Long.valueOf(900L).equals(file.getId())),
                eq(99L), org.mockito.ArgumentMatchers.contains("A/3"));
        platformOrder.verify(platformAdapter).recordSubmitted(
                org.mockito.ArgumentMatchers.argThat(file -> Long.valueOf(902L).equals(file.getId())
                        && "proc-new-a3".equals(file.getProcessInstanceId())),
                eq(99L), eq("proc-new-a3"));
        platformOrder.verify(platformAdapter).recordResubmitted(
                org.mockito.ArgumentMatchers.argThat(file -> Long.valueOf(900L).equals(file.getId())),
                eq(902L));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"transfer,permission", "transfer,authorization", "transfer,image",
            "sign,permission", "sign,authorization", "sign,image"})
    void taskRecipientWithoutRequiredQualificationMustNotChangeTask(String action, String missing) {
        mockTaskActionContext(900L, 99L, "task-2", "approveTask",
                DccControlledFileStatusEnum.PENDING_MATRIX_REVIEW,
                DccControlledFileStageCodeEnum.MATRIX_REVIEW, Boolean.FALSE);
        if ("permission".equals(missing)) {
            when(permissionApi.hasAnyPermissions(eq(101L), any(String[].class))).thenReturn(false);
        } else if ("authorization".equals(missing)) {
            when(signatureAuthorizationService.getAuthorizationMap(List.of(101L))).thenReturn(Map.of(101L, false));
        } else {
            when(signatureImageService.requireActiveSnapshot(101L)).thenReturn(null);
        }
        Runnable actionCall;
        if ("transfer".equals(action)) {
            DccControlledFileTransferTaskReqVO req = new DccControlledFileTransferTaskReqVO();
            req.setTaskId("task-2");
            req.setAssigneeUserId(101L);
            req.setPassword("test-password");
            req.setReason("任务转交");
            actionCall = () -> workflowService.transferTask(99L, 900L, req);
        } else {
            DccControlledFileCreateSignTaskReqVO req = new DccControlledFileCreateSignTaskReqVO();
            req.setTaskId("task-2");
            req.setUserIds(new java.util.LinkedHashSet<>(List.of(101L)));
            req.setType("before");
            req.setPassword("test-password");
            req.setReason("增加审核人");
            actionCall = () -> workflowService.createSignTask(99L, 900L, req);
        }

        assertThrows(ServiceException.class, actionCall::run);
        verify(signatureVerificationService, never()).verifyPasswordAndCreateSignature(any(), any(), any(), any(), any(), any(), any());
        verify(bpmTaskService, never()).transferTask(any(), any());
        verify(bpmTaskService, never()).createSignTask(any(), any());
        verify(routeSnapshotMapper, never()).updateById(any(DccControlledFileRouteSnapshotDO.class));
    }

    @Test
    void submitControlledFile_categoryWithoutExactTaxonomyBindingFailsBeforeInsert() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("A/1");
        mockCommonSubmitDependencies();
        when(categoryMapper.selectById(10L)).thenReturn(DccFileCategoryDO.builder()
                .id(10L).code("SOP").name("SOP").active(Boolean.TRUE).source("LOCAL")
                .fileTypeTaxonomyId(null).build());

        assertServiceException(() -> workflowService.submitControlledFile(99L, reqVO),
                PROJECT_FILE_TEMPLATE_SELECTION_INVALID);

        verify(controlledFileMapper, never()).insert(any(DccControlledFileDO.class));
    }

    @Test
    void submitControlledFile_taxonomyMustReachThirdLevel() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("A/1");
        when(fileTypeTaxonomyAdminService.resolveActivePath(8803L))
                .thenReturn(new DccFileTypeTaxonomyPath(8803L, "一级", "二级", null, null, null));

        assertServiceException(() -> workflowService.submitControlledFile(99L, reqVO),
                FILE_TYPE_TAXONOMY_LEVEL_INVALID);

        verify(controlledFileMapper, never()).insert(any(DccControlledFileDO.class));
        verify(bpmProcessInstanceApi, never()).createProcessInstance(any(Long.class), any());
    }

    /* Retired standalone major-revision tests are preserved here only as migration history.
       Major revision behavior is now covered by checkin tests in DccControlledFileQueryServiceTest.
    @Test
    void createMajorRevision_fromSelectedA2_generatesB1AndPreservesFormalBaseline() {
        mockCommonSubmitDependencies();
        mockFourStageRoute();
        DccControlledFileMasterDO master = DccControlledFileMasterDO.builder()
                .id(700L).categoryId(10L).directoryId(21L).fileName("SOP-001").fileNumber("SOP-001")
                .currentActiveControlledFileId(900L).status(DccControlledFileMasterStatusEnum.ACTIVE_CHAIN.getCode()).build();
        DccControlledFileDO active = DccControlledFileDO.builder()
                .id(900L).masterId(700L).categoryId(10L).directoryId(21L).fileName("SOP-001")
                .fileNumber("SOP-001").versionNo("A/1").revisionCode("A").iterationNo(1)
                .dccProjectCodeId(3000L).fileTypeTaxonomyId(8803L).sourceFileId(100L).originalFileId(100L)
                .requesterId(99L).status(DccControlledFileStatusEnum.ACTIVE.getStatus()).needTraining(Boolean.FALSE).build();
        DccControlledFileDO source = DccControlledFileDO.builder()
                .id(901L).masterId(700L).categoryId(10L).directoryId(21L).fileName("SOP-001")
                .fileNumber("SOP-001").versionNo("A/2").revisionCode("A").iterationNo(2)
                .dccProjectCodeId(3000L).fileTypeTaxonomyId(8803L).sourceFileId(101L).originalFileId(100L)
                .requesterId(99L).status(DccControlledFileStatusEnum.WORKING.getStatus()).needTraining(Boolean.FALSE).build();
        when(controlledFileMasterMapper.selectByIdForUpdate(700L)).thenReturn(master);
        when(controlledFileMapper.selectById(901L)).thenReturn(source);
        when(controlledFileMapper.selectById(900L)).thenReturn(active);
        when(controlledFileMapper.selectListByMasterId(700L)).thenReturn(List.of(active, source));
        when(controlledFileMapper.selectAssociatedFilesByProjectCodeId(3000L, List.of(901L))).thenReturn(List.of(source));
        when(controlledFileMapper.insert(any(DccControlledFileDO.class))).thenAnswer(invocation -> {
            DccControlledFileDO inserted = invocation.getArgument(0);
            inserted.setId(902L);
            return 1;
        });
        when(bpmProcessInstanceApi.createProcessInstance(any(Long.class), any())).thenReturn("proc-major-1");
        DccControlledFileMajorRevisionReqVO request = new DccControlledFileMajorRevisionReqVO();
        request.setSourceControlledFileId(901L);
        request.setReason("重大工艺变更");
        request.setIdempotencyKey("major-revision-901-1");

        Long createdId = workflowService.createMajorRevision(99L, request);

        assertEquals(902L, createdId);
        ArgumentCaptor<DccControlledFileDO> fileCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).insert(fileCaptor.capture());
        assertEquals("B/1", fileCaptor.getValue().getVersionNo());
        assertEquals("B", fileCaptor.getValue().getRevisionCode());
        assertEquals(1, fileCaptor.getValue().getIterationNo());
        assertEquals(901L, fileCaptor.getValue().getPredecessorControlledFileId());
        assertEquals(900L, fileCaptor.getValue().getRevisionBaseActiveControlledFileId());
        assertEquals(DccControlledFileStatusEnum.WORKING.getStatus(), fileCaptor.getValue().getStatus());
        assertNull(fileCaptor.getValue().getSubmitterId());
        assertNull(fileCaptor.getValue().getSubmittedTime());
        verify(routeSnapshotMapper, never()).insert(any(DccControlledFileRouteSnapshotDO.class));
        verify(bpmProcessInstanceApi, never()).createProcessInstance(any(Long.class), any());
        verify(platformAdapter, never()).recordSubmitted(any(DccControlledFileDO.class), any(), any());
        verify(sourceOwnershipService).claimSubmissionSource(eq(902L), any(), eq(99L), eq("MAJOR_REVISION"));
        verify(projectFileTemplateService, never()).validateUploadSelection(any(), any(), any());
        verify(projectAccessService, org.mockito.Mockito.atLeastOnce()).assertProjectOwner(99L, 3000L);
    }

    @Test
    void createMajorRevision_originalRequesterWithoutProjectOwnerIsRejected() {
        DccControlledFileDO source = DccControlledFileDO.builder()
                .id(901L).masterId(700L).dccProjectCodeId(3000L)
                .requesterId(99L).status(DccControlledFileStatusEnum.ACTIVE.getStatus()).build();
        when(controlledFileMapper.selectById(901L)).thenReturn(source);
        doThrow(exception(DCC_PROJECT_ACCESS_DENIED))
                .when(projectAccessService).assertProjectOwner(99L, 3000L);
        DccControlledFileMajorRevisionReqVO request = new DccControlledFileMajorRevisionReqVO();
        request.setSourceControlledFileId(901L);
        request.setReason("重大工艺变更");
        request.setIdempotencyKey("major-revision-901-2");

        assertServiceException(() -> workflowService.createMajorRevision(99L, request),
                DCC_PROJECT_ACCESS_DENIED);

        verify(controlledFileMasterMapper, never()).selectByIdForUpdate(any());
        verify(controlledFileMapper, never()).insert(any(DccControlledFileDO.class));
    }

    @Test
    void createMajorRevision_projectOwnerDifferentFromRequesterIsAllowed() {
        mockCommonSubmitDependencies();
        mockFourStageRoute();
        DccControlledFileMasterDO master = DccControlledFileMasterDO.builder()
                .id(700L).categoryId(10L).directoryId(21L).fileName("SOP-001").fileNumber("SOP-001")
                .currentActiveControlledFileId(900L).status(DccControlledFileMasterStatusEnum.ACTIVE_CHAIN.getCode()).build();
        DccControlledFileDO active = DccControlledFileDO.builder()
                .id(900L).masterId(700L).categoryId(10L).directoryId(21L).fileName("SOP-001")
                .fileNumber("SOP-001").versionNo("A/1").revisionCode("A").iterationNo(1)
                .dccProjectCodeId(3000L).fileTypeTaxonomyId(8803L).sourceFileId(100L).originalFileId(100L)
                .requesterId(99L).status(DccControlledFileStatusEnum.ACTIVE.getStatus()).needTraining(Boolean.FALSE).build();
        DccControlledFileDO source = DccControlledFileDO.builder()
                .id(901L).masterId(700L).categoryId(10L).directoryId(21L).fileName("SOP-001")
                .fileNumber("SOP-001").versionNo("A/2").revisionCode("A").iterationNo(2)
                .dccProjectCodeId(3000L).fileTypeTaxonomyId(8803L).sourceFileId(101L).originalFileId(100L)
                .requesterId(99L).status(DccControlledFileStatusEnum.WORKING.getStatus()).needTraining(Boolean.FALSE).build();
        when(controlledFileMasterMapper.selectByIdForUpdate(700L)).thenReturn(master);
        when(controlledFileMapper.selectById(901L)).thenReturn(source);
        when(controlledFileMapper.selectById(900L)).thenReturn(active);
        when(controlledFileMapper.selectListByMasterId(700L)).thenReturn(List.of(active, source));
        when(controlledFileMapper.selectAssociatedFilesByProjectCodeId(3000L, List.of(901L))).thenReturn(List.of(source));
        when(controlledFileMapper.insert(any(DccControlledFileDO.class))).thenAnswer(invocation -> {
            DccControlledFileDO inserted = invocation.getArgument(0);
            inserted.setId(902L);
            return 1;
        });
        when(bpmProcessInstanceApi.createProcessInstance(any(Long.class), any())).thenReturn("proc-owner-1");
        DccControlledFileMajorRevisionReqVO request = new DccControlledFileMajorRevisionReqVO();
        request.setSourceControlledFileId(901L);
        request.setReason("重大工艺变更");
        request.setIdempotencyKey("major-revision-901-3");

        Long createdId = workflowService.createMajorRevision(1074L, request);

        assertEquals(902L, createdId);
        verify(projectAccessService, org.mockito.Mockito.atLeastOnce()).assertProjectOwner(1074L, 3000L);
    }

    @Test
    void createMajorRevision_fromSelectedB2_generatesC1() {
        mockCommonSubmitDependencies();
        mockFourStageRoute();
        DccControlledFileMasterDO master = DccControlledFileMasterDO.builder()
                .id(700L).categoryId(10L).directoryId(21L).fileName("SOP-001").fileNumber("SOP-001")
                .currentActiveControlledFileId(900L)
                .status(DccControlledFileMasterStatusEnum.ACTIVE_CHAIN.getCode()).build();
        DccControlledFileDO active = DccControlledFileDO.builder()
                .id(900L).masterId(700L).categoryId(10L).directoryId(21L).fileName("SOP-001")
                .fileNumber("SOP-001").versionNo("B/1").revisionCode("B").iterationNo(1)
                .dccProjectCodeId(3000L).fileTypeTaxonomyId(8803L).sourceFileId(100L).originalFileId(100L)
                .requesterId(99L).status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .needTraining(Boolean.FALSE).build();
        DccControlledFileDO source = DccControlledFileDO.builder()
                .id(901L).masterId(700L).categoryId(10L).directoryId(21L).fileName("SOP-001")
                .fileNumber("SOP-001").versionNo("B/2").revisionCode("B").iterationNo(2)
                .dccProjectCodeId(3000L).fileTypeTaxonomyId(8803L).sourceFileId(101L).originalFileId(100L)
                .requesterId(99L).status(DccControlledFileStatusEnum.WORKING.getStatus())
                .needTraining(Boolean.FALSE).build();
        when(controlledFileMasterMapper.selectByIdForUpdate(700L)).thenReturn(master);
        when(controlledFileMapper.selectById(901L)).thenReturn(source);
        when(controlledFileMapper.selectById(900L)).thenReturn(active);
        when(controlledFileMapper.selectListByMasterId(700L)).thenReturn(List.of(active, source));
        when(controlledFileMapper.selectAssociatedFilesByProjectCodeId(3000L, List.of(901L)))
                .thenReturn(List.of(source));
        when(controlledFileMapper.insert(any(DccControlledFileDO.class))).thenAnswer(invocation -> {
            DccControlledFileDO inserted = invocation.getArgument(0);
            inserted.setId(902L);
            return 1;
        });
        when(bpmProcessInstanceApi.createProcessInstance(any(Long.class), any())).thenReturn("proc-major-c1");
        DccControlledFileMajorRevisionReqVO request = new DccControlledFileMajorRevisionReqVO();
        request.setSourceControlledFileId(901L);
        request.setReason("B版本族重大变更");
        request.setIdempotencyKey("major-revision-901-4");

        workflowService.createMajorRevision(99L, request);

        ArgumentCaptor<DccControlledFileDO> fileCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).insert(fileCaptor.capture());
        assertEquals("C/1", fileCaptor.getValue().getVersionNo());
        assertEquals("C", fileCaptor.getValue().getRevisionCode());
        assertEquals(1, fileCaptor.getValue().getIterationNo());
        assertEquals(901L, fileCaptor.getValue().getPredecessorControlledFileId());
        assertEquals(900L, fileCaptor.getValue().getRevisionBaseActiveControlledFileId());
    }

    */
    @Test
    void submitControlledFile_newFileRequiresTaxonomyLeaf() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("A/1");
        mockCommonSubmitDependencies();
        when(fileTypeTaxonomyAdminService.listActiveDescendantIds(8803L)).thenReturn(List.of(8803L, 8804L));

        assertServiceException(() -> workflowService.submitControlledFile(99L, reqVO),
                FILE_TYPE_TAXONOMY_LEVEL_INVALID);

        verify(controlledFileMapper, never()).insert(any(DccControlledFileDO.class));
        verify(controlledFileMasterMapper, never()).insert(any(DccControlledFileMasterDO.class));
    }

    @Test
    void getCurrentVersionByFileNumber_uniqueActiveVersion_returnsTraceableVersionInfo() {
        DccControlledFileMasterDO master = DccControlledFileMasterDO.builder()
                .id(700L)
                .categoryId(10L)
                .directoryId(21L)
                .fileName("SOP-001")
                .fileNumber("SOP-001")
                .currentActiveControlledFileId(800L)
                .status(DccControlledFileMasterStatusEnum.ACTIVE_CHAIN.getCode())
                .build();
        DccControlledFileDO activeFile = DccControlledFileDO.builder()
                .id(800L)
                .masterId(700L)
                .categoryId(10L)
                .directoryId(21L)
                .fileName("SOP-001")
                .title("SOP-001")
                .fileNumber("SOP-001")
                .versionNo("A/1")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .sourceFileId(100L)
                .originalFileId(100L)
                .publishedFileId(120L)
                .stampedFileId(121L)
                .build();
        when(controlledFileMasterMapper.selectListByFileNumber("SOP-001")).thenReturn(List.of(master));
        when(controlledFileMapper.selectById(800L)).thenReturn(activeFile);
        when(fileMapper.selectById(100L)).thenReturn(FileDO.builder()
                .id(100L).name("SOP-001.docx").path("dcc/source/SOP-001.docx").build());
        when(fileMapper.selectById(120L)).thenReturn(FileDO.builder()
                .id(120L).name("SOP-001-published.pdf").path("dcc/published/SOP-001-published.pdf").build());
        when(fileMapper.selectById(121L)).thenReturn(FileDO.builder()
                .id(121L).name("SOP-001-stamped.pdf").path("dcc/stamped/SOP-001-stamped.pdf").build());

        DccControlledFileActionProjectionRespVO actionProjection = new DccControlledFileActionProjectionRespVO();
        actionProjection.setActionLocked(Boolean.FALSE);
        actionProjection.setAllowedActions(List.of("VIEW", "PREVIEW", "DOWNLOAD", "OBSOLETE"));
        DccControlledFileRespVO projectedDetail = new DccControlledFileRespVO();
        projectedDetail.setActionProjection(actionProjection);
        when(queryService.getControlledFile(99L, 800L)).thenReturn(projectedDetail);

        DccControlledFileCurrentVersionRespVO respVO =
                workflowService.getCurrentVersionByFileNumber(99L, " SOP-001 ");

        assertEquals(800L, respVO.getCurrentControlledFileId());
        assertEquals("SOP-001", respVO.getFileNumber());
        assertEquals("SOP-001", respVO.getFileName());
        assertEquals("A/1", respVO.getCurrentVersionNo());
        assertEquals(DccControlledFileStatusEnum.ACTIVE.getStatus(), respVO.getStatus());
        assertEquals(100L, respVO.getOriginalFileId());
        assertEquals(120L, respVO.getPublishedFileId());
        assertEquals(121L, respVO.getStampedFileId());
        assertEquals("SOP-001.docx", respVO.getOriginalFileName());
        assertEquals("dcc/source/SOP-001.docx", respVO.getOriginalFilePath());
        assertEquals("SOP-001.docx", respVO.getSourceFileName());
        assertEquals("dcc/source/SOP-001.docx", respVO.getSourceFilePath());
        assertEquals("SOP-001-published.pdf", respVO.getPublishedFileName());
        assertEquals("dcc/published/SOP-001-published.pdf", respVO.getPublishedFilePath());
        assertEquals("SOP-001-stamped.pdf", respVO.getStampedFileName());
        assertEquals("dcc/stamped/SOP-001-stamped.pdf", respVO.getStampedFilePath());
        assertEquals(Boolean.FALSE, respVO.getModifying());
        assertSame(actionProjection, respVO.getActionProjection());
    }

    @Test
    void getCurrentVersionByFileNumber_legacyMasterWithoutCompositeIdentityResolvesByUniqueActiveIteration() {
        DccControlledFileMasterDO master = DccControlledFileMasterDO.builder()
                .id(700L).categoryId(10L).directoryId(21L).fileName("SOP-001").fileNumber("SOP-001")
                .currentActiveControlledFileId(800L).status(DccControlledFileMasterStatusEnum.ACTIVE_CHAIN.getCode())
                .build();
        DccControlledFileDO activeFile = DccControlledFileDO.builder()
                .id(800L).masterId(700L).categoryId(10L).directoryId(21L).fileName("SOP-001")
                .fileNumber("SOP-001").versionNo("A/1").status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .dccProjectCodeId(3000L).fileTypeTaxonomyId(8803L).sourceFileId(100L).originalFileId(100L)
                .build();
        when(projectCodeMapper.selectById(3000L)).thenReturn(DccProjectCodeDO.builder()
                .id(3000L).status(DccProjectCodeStatusConstants.ENABLE).build());
        when(fileTypeTaxonomyAdminService.resolveActivePath(8803L)).thenReturn(defaultTaxonomyPath());
        when(fileTypeTaxonomyAdminService.listActiveDescendantIds(8803L)).thenReturn(List.of(8803L));
        when(fileTypeTaxonomyAdminService.listActiveDescendantPaths(8803L)).thenReturn(List.of(defaultTaxonomyPath()));
        when(controlledFileMasterMapper.selectListByLogicalIdentity(3000L, 8803L, "SOP-001"))
                .thenReturn(List.of());
        when(controlledFileMapper.selectActiveByLegacyProjectAndFileNumber(3000L, "SOP-001"))
                .thenReturn(List.of(activeFile));
        when(controlledFileMasterMapper.selectById(700L)).thenReturn(master);
        when(controlledFileMapper.selectById(800L)).thenReturn(activeFile);
        when(fileMapper.selectById(100L)).thenReturn(FileDO.builder()
                .id(100L).name("SOP-001.docx").path("dcc/source/SOP-001.docx").build());
        DccControlledFileRespVO detail = new DccControlledFileRespVO();
        detail.setActionProjection(new DccControlledFileActionProjectionRespVO());
        when(queryService.getControlledFile(99L, 800L)).thenReturn(detail);

        DccControlledFileCurrentVersionRespVO result = workflowService.getCurrentVersionByFileNumber(
                99L, "SOP-001", 3000L, 8803L);

        assertEquals(Boolean.TRUE, result.getMatched());
        assertEquals(700L, result.getMasterId());
        assertEquals(800L, result.getCurrentControlledFileId());
        assertEquals("A/1", result.getCurrentVersionNo());
    }

    @Test
    void getCurrentVersionByFileNumber_rejectsMasterActiveFileIdentityMismatch() {
        DccControlledFileMasterDO master = DccControlledFileMasterDO.builder()
                .id(700L)
                .categoryId(10L)
                .directoryId(21L)
                .fileName("SOP-001")
                .fileNumber("DOC-OLD")
                .dccProjectCodeId(3000L)
                .fileTypeTaxonomyLeafId(8803L)
                .normalizedFileNumber("DOC-OLD")
                .currentActiveControlledFileId(800L)
                .status(DccControlledFileMasterStatusEnum.ACTIVE_CHAIN.getCode())
                .build();
        DccControlledFileDO activeFile = DccControlledFileDO.builder()
                .id(800L)
                .masterId(700L)
                .categoryId(10L)
                .directoryId(21L)
                .fileName("SOP-001")
                .fileNumber("DOC-NEW")
                .versionNo("A/1")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .dccProjectCodeId(3000L)
                .fileTypeTaxonomyId(8803L)
                .sourceFileId(100L)
                .originalFileId(100L)
                .build();
        when(projectCodeMapper.selectById(3000L)).thenReturn(DccProjectCodeDO.builder()
                .id(3000L).status(DccProjectCodeStatusConstants.ENABLE).build());
        when(fileTypeTaxonomyAdminService.resolveActivePath(8803L)).thenReturn(defaultTaxonomyPath());
        when(fileTypeTaxonomyAdminService.listActiveDescendantIds(8803L)).thenReturn(List.of(8803L));
        when(fileTypeTaxonomyAdminService.listActiveDescendantPaths(8803L)).thenReturn(List.of(defaultTaxonomyPath()));
        when(controlledFileMasterMapper.selectListByLogicalIdentity(3000L, 8803L, "DOC-OLD"))
                .thenReturn(List.of(master));
        when(controlledFileMapper.selectById(800L)).thenReturn(activeFile);
        when(fileMapper.selectById(100L)).thenReturn(FileDO.builder()
                .id(100L).name("DOC-NEW.docx").path("dcc/source/DOC-NEW.docx").build());
        DccControlledFileRespVO detail = new DccControlledFileRespVO();
        detail.setActionProjection(new DccControlledFileActionProjectionRespVO());
        when(queryService.getControlledFile(99L, 800L)).thenReturn(detail);

        assertServiceException(() -> workflowService.getCurrentVersionByFileNumber(99L, "DOC-OLD", 3000L, 8803L),
                CONTROLLED_FILE_FILE_NUMBER_CONFLICT);

        verify(queryService, never()).getControlledFile(any(), any());
    }

    @Test
    void submitControlledFile_rejectsRawFileIdsWithoutUploadTicket() {
        DccControlledFileSubmitReqVO reqVO = buildRawFileIdSubmitReqVO("V1.0");

        assertServiceException(() -> workflowService.submitControlledFile(99L, reqVO),
                CONTROLLED_FILE_UPLOAD_TICKET_INVALID);

        verify(uploadTicketService, never()).resolveForBinding(any(DccUploadTicketResolveCommand.class));
        verify(controlledFileMapper, never()).insert(any(DccControlledFileDO.class));
    }

    @Test
    void submitControlledFile_explicitSourceTicketBindsSourceSeparately() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("A/1");
        reqVO.setSourceUploadTicket("UT-SOURCE");
        mockCommonSubmitDependencies();
        when(uploadTicketService.resolveForBinding(any(DccUploadTicketResolveCommand.class)))
                .thenAnswer(invocation -> {
                    DccUploadTicketResolveCommand command = invocation.getArgument(0);
                    if ("UT-SOURCE".equals(command.uploadTicket())) {
                        return new DccUploadTicketBoundFile(command.uploadTicket(), 102L,
                                "SOP-source.docx",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", 4L);
                    }
                    if ("DRAWING_PDF".equals(command.purpose())) {
                        return new DccUploadTicketBoundFile(command.uploadTicket(), 101L,
                                "SOP-001.pdf", "application/pdf", 8L);
                    }
                    return new DccUploadTicketBoundFile(command.uploadTicket(), 100L,
                            "SOP-original.docx",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", 4L);
                });
        when(fileMapper.selectById(102L)).thenReturn(FileDO.builder()
                .id(102L)
                .name("SOP-source.docx")
                .type("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                .build());
        when(routeNodeMapper.selectListByRouteId(30L)).thenReturn(fullApprovalRoute(
                routeNode(1, DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode(), "Doc Control Review", "POSITION", 50L)));
        when(positionAssignmentMapper.selectActiveListByPositionId(50L)).thenReturn(List.of(
                DccPositionAssignmentDO.builder().id(60L).positionId(50L).assignmentType("POST").systemPostId(500L).active(Boolean.TRUE).build()));
        when(adminUserApi.getUserListByPostIds(List.of(500L))).thenReturn(List.of(new AdminUserRespDTO().setId(200L).setStatus(0)));
        doAnswer(invocation -> {
            DccControlledFileMasterDO master = invocation.getArgument(0);
            master.setId(700L);
            return 1;
        }).when(controlledFileMasterMapper).insert(any(DccControlledFileMasterDO.class));
        doAnswer(invocation -> {
            DccControlledFileDO file = invocation.getArgument(0);
            file.setId(900L);
            return 1;
        }).when(controlledFileMapper).insert(any(DccControlledFileDO.class));
        when(bpmProcessInstanceApi.createProcessInstance(any(Long.class), any())).thenReturn("proc-1");

        Long fileId = workflowService.submitControlledFile(99L, reqVO);

        assertEquals(900L, fileId);
        ArgumentCaptor<DccControlledFileDO> fileCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).insert(fileCaptor.capture());
        assertEquals(100L, fileCaptor.getValue().getOriginalFileId());
        assertEquals(102L, fileCaptor.getValue().getSourceFileId());
        verify(uploadTicketService).markBound(new DccUploadTicketMarkBoundCommand(
                "UT-ORIGINAL", 99L, 10L, "session-1", "SOURCE", 900L));
        verify(uploadTicketService).markBound(new DccUploadTicketMarkBoundCommand(
                "UT-SOURCE", 99L, 10L, "session-1", "SOURCE", 900L));
    }

    @Test
    @SuppressWarnings("unchecked")
    void submitControlledFile_selectedSignoffUsers_mustMatchConfiguredMatrix() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("A/1");
        reqVO.setSelectedSignoffUserIds(List.of(301L, 302L));
        mockCommonSubmitDependencies();
        when(routeNodeMapper.selectListByRouteId(30L)).thenReturn(List.of(
                routeNode(1, DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode(), "Doc Control Review", "POSITION", 50L),
                routeNode(2, DccControlledFileStageCodeEnum.MATRIX_REVIEW.getCode(), "Matrix Review", "POSITION", 51L),
                routeNode(3, DccControlledFileStageCodeEnum.MATRIX_APPROVAL.getCode(), "Matrix Approval", "POSITION", 52L),
                routeNode(4, DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL.getCode(), "Doc Control Approval", "POSITION", 53L)));
        when(positionAssignmentMapper.selectActiveListByPositionId(50L)).thenReturn(List.of(
                DccPositionAssignmentDO.builder().id(60L).positionId(50L).assignmentType("POST").systemPostId(500L).active(Boolean.TRUE).build()));
        when(positionAssignmentMapper.selectActiveListByPositionId(51L)).thenReturn(List.of(
                DccPositionAssignmentDO.builder().id(61L).positionId(51L).assignmentType("POST").systemPostId(501L).active(Boolean.TRUE).build()));
        when(positionAssignmentMapper.selectActiveListByPositionId(52L)).thenReturn(List.of(
                DccPositionAssignmentDO.builder().id(62L).positionId(52L).assignmentType("POST").systemPostId(502L).active(Boolean.TRUE).build()));
        when(positionAssignmentMapper.selectActiveListByPositionId(53L)).thenReturn(List.of(
                DccPositionAssignmentDO.builder().id(63L).positionId(53L).assignmentType("POST").systemPostId(503L).active(Boolean.TRUE).build()));
        when(adminUserApi.getUserListByPostIds(List.of(500L))).thenReturn(List.of(new AdminUserRespDTO().setId(200L).setStatus(0)));
        when(adminUserApi.getUserListByPostIds(List.of(501L))).thenReturn(List.of(new AdminUserRespDTO().setId(201L).setStatus(0)));
        when(adminUserApi.getUserListByPostIds(List.of(502L))).thenReturn(List.of(new AdminUserRespDTO().setId(203L).setStatus(0)));
        when(adminUserApi.getUserListByPostIds(List.of(503L))).thenReturn(List.of(new AdminUserRespDTO().setId(205L).setStatus(0)));
        doAnswer(invocation -> {
            DccControlledFileMasterDO master = invocation.getArgument(0);
            master.setId(700L);
            return 1;
        }).when(controlledFileMasterMapper).insert(any(DccControlledFileMasterDO.class));
        doAnswer(invocation -> {
            DccControlledFileDO file = invocation.getArgument(0);
            file.setId(900L);
            return 1;
        }).when(controlledFileMapper).insert(any(DccControlledFileDO.class));
        when(bpmProcessInstanceApi.createProcessInstance(any(Long.class), any())).thenReturn("proc-1");

        assertServiceException(() -> workflowService.submitControlledFile(99L, reqVO),
                cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ROUTE_NOT_CONFIGURED);
        verify(controlledFileMapper, never()).insert(any(DccControlledFileDO.class));
    }

    @Test
    void submitControlledFile_selectedSignoffUserInvalid_throwsBeforeInsertSnapshotAndBpm() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("A/1");
        reqVO.setSelectedSignoffUserIds(List.of(301L));
        mockCommonSubmitDependencies();
        when(routeNodeMapper.selectListByRouteId(30L)).thenReturn(List.of(
                routeNode(1, DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode(), "Doc Control Review", "POSITION", 50L),
                routeNode(2, DccControlledFileStageCodeEnum.MATRIX_REVIEW.getCode(), "Matrix Review", "POSITION", 51L),
                routeNode(3, DccControlledFileStageCodeEnum.MATRIX_APPROVAL.getCode(), "Matrix Approval", "USER", 200L),
                routeNode(4, DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL.getCode(), "Doc Control Approval", "USER", 200L)));
        when(positionAssignmentMapper.selectActiveListByPositionId(50L)).thenReturn(List.of(
                DccPositionAssignmentDO.builder().id(60L).positionId(50L).assignmentType("POST").systemPostId(500L).active(Boolean.TRUE).build()));
        when(positionAssignmentMapper.selectActiveListByPositionId(51L)).thenReturn(List.of(
                DccPositionAssignmentDO.builder().id(61L).positionId(51L).assignmentType("POST").systemPostId(501L).active(Boolean.TRUE).build()));
        when(adminUserApi.getUserListByPostIds(List.of(500L))).thenReturn(List.of(new AdminUserRespDTO().setId(200L).setStatus(0)));
        when(adminUserApi.getUserListByPostIds(List.of(501L))).thenReturn(List.of(new AdminUserRespDTO().setId(201L).setStatus(0)));
        doAnswer(invocation -> {
            Collection<Long> userIds = invocation.getArgument(0);
            if (userIds.contains(301L)) {
                throw new ServiceException(1_002_000_004, "user disabled");
            }
            return null;
        }).when(adminUserApi).validateUserList(any());

        assertServiceException(() -> workflowService.submitControlledFile(99L, reqVO),
                ROUTE_PREVIEW_APPROVER_NOT_FOUND);
        verify(controlledFileMapper, never()).insert(any(DccControlledFileDO.class));
        verify(routeSnapshotMapper, never()).insert(any(DccControlledFileRouteSnapshotDO.class));
        verify(bpmProcessInstanceApi, never()).createProcessInstance(any(Long.class), any());
    }

    @Test
    void submitControlledFile_routeBecomesNotReadyRejectsBeforeFormalRecordTicketBindAndBpm() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("A/1");
        mockCommonSubmitDependencies();
        mockFourStageRoute();
        when(permissionApi.hasAnyPermissions(200L, "dcc:controlled-file:review")).thenReturn(false);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> workflowService.submitControlledFile(99L, reqVO));

        assertEquals(CONTROLLED_FILE_ROUTE_NOT_READY.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("审批人缺少当前阶段文控权限"));
        verify(controlledFileMapper, never()).insert(any(DccControlledFileDO.class));
        verify(routeSnapshotMapper, never()).insert(any(DccControlledFileRouteSnapshotDO.class));
        verify(uploadTicketService, never()).markBound(any(DccUploadTicketMarkBoundCommand.class));
        verify(bpmProcessInstanceApi, never()).createProcessInstance(any(Long.class), any());
    }

    @Test
    void submitControlledFile_rejectsDrawingSourceWithoutPdf() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("A/1");
        reqVO.setSourceFileName("pump-housing.dwg");
        reqVO.setDrawingPdfUploadTicket(null);
        mockCommonSubmitDependencies();
        when(fileMapper.selectById(100L)).thenReturn(FileDO.builder()
                .id(100L).name("pump-housing.dwg").type("application/acad").build());

        assertServiceException(() -> workflowService.submitControlledFile(99L, reqVO),
                CONTROLLED_FILE_DRAWING_PDF_REQUIRED);
        verify(controlledFileMapper, never()).insert(any(DccControlledFileDO.class));
    }

    @Test
    void submitControlledFile_rejectsUnsupportedSourceExtension() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("A/1");
        reqVO.setSourceFileName("archive.zip");
        mockCommonSubmitDependencies();
        when(fileMapper.selectById(100L)).thenReturn(FileDO.builder()
                .id(100L).name("archive.zip").type("application/octet-stream").build());

        assertServiceException(() -> workflowService.submitControlledFile(99L, reqVO),
                CONTROLLED_FILE_SOURCE_FILE_TYPE_INVALID);
        verify(controlledFileMapper, never()).insert(any(DccControlledFileDO.class));
    }

    @Test
    void submitControlledFile_rejectsInvalidDrawingPdfFile() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("A/1");
        reqVO.setSourceFileName("pump-housing.dwg");
        mockCommonSubmitDependencies();
        when(fileMapper.selectById(100L)).thenReturn(FileDO.builder()
                .id(100L).name("pump-housing.dwg").type("application/acad").build());
        when(fileMapper.selectById(101L)).thenReturn(FileDO.builder()
                .id(101L).name("not-a-pdf.txt").type("text/plain").build());

        assertServiceException(() -> workflowService.submitControlledFile(99L, reqVO),
                CONTROLLED_FILE_DRAWING_PDF_FILE_INVALID);
        verify(controlledFileMapper, never()).insert(any(DccControlledFileDO.class));
    }

    @Test
    void submitControlledFile_ignoresClientProductAndKeepsUnboundProjectProductEmpty() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("A/1");
        mockCommonSubmitDependencies();
        when(categoryMapper.selectById(10L)).thenReturn(DccFileCategoryDO.builder()
                .id(10L).code("SOP").name("SOP").active(Boolean.TRUE).source("LOCAL")
                .fileTypeTaxonomyId(8803L).build());
        reqVO.setProductMasterId(5000L);
        reqVO.setProductCode("not-authoritative");
        mockFourStageRoute();
        doAnswer(invocation -> {
            DccControlledFileDO file = invocation.getArgument(0);
            file.setId(907L);
            return 1;
        }).when(controlledFileMapper).insert(any(DccControlledFileDO.class));

        Long fileId = workflowService.submitControlledFile(99L, reqVO);

        assertEquals(907L, fileId);
        ArgumentCaptor<DccControlledFileDO> fileCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).insert(fileCaptor.capture());
        assertNull(fileCaptor.getValue().getProductMasterId());
        assertNull(fileCaptor.getValue().getProductCode());
        assertNull(fileCaptor.getValue().getProductName());
    }

    @Test
    void submitControlledFile_dhfCategoryRequiresBoundProductMaster() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("A/1");
        reqVO.setProductMasterId(null);
        reqVO.setProductCode(null);
        mockCommonSubmitDependencies();
        when(categoryMapper.selectById(10L)).thenReturn(DccFileCategoryDO.builder()
                .id(10L).code("DCC_FVM_DHF_005").name("项目策划书").active(Boolean.TRUE).source("LOCAL")
                .fileTypeTaxonomyId(8803L).build());
        assertServiceException(() -> workflowService.submitControlledFile(99L, reqVO),
                CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING);
        verify(controlledFileMapper, never()).insert(any(DccControlledFileDO.class));
    }

    @Test
    void submitControlledFile_projectCodeWithMdmBindingPersistsMdmProduct() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("A/1");
        mockCommonSubmitDependencies();
        when(projectCodeMapper.selectById(3000L)).thenReturn(DccProjectCodeDO.builder()
                .id(3000L)
                .productMasterId(5000L)
                .projectName("旧项目文本")
                .projectCode("OLD-PROJECT-CODE")
                .status(DccProjectCodeStatusConstants.ENABLE)
                .build());
        when(mdmProductApi.getEnabledDccProduct(5000L)).thenReturn(MdmProductRespDTO.builder()
                .id(5000L)
                .productCode("P-5000")
                .dccProductCode("A1234567890123")
                .nameCn("MDM正式产品")
                .status("ENABLE")
                .build());
        mockFourStageRoute();
        doAnswer(invocation -> {
            DccControlledFileDO file = invocation.getArgument(0);
            file.setId(908L);
            return 1;
        }).when(controlledFileMapper).insert(any(DccControlledFileDO.class));

        Long fileId = workflowService.submitControlledFile(99L, reqVO);

        assertEquals(908L, fileId);
        ArgumentCaptor<DccControlledFileDO> fileCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).insert(fileCaptor.capture());
        assertEquals(5000L, fileCaptor.getValue().getProductMasterId());
        assertEquals("A1234567890123", fileCaptor.getValue().getProductCode());
        assertEquals("MDM正式产品", fileCaptor.getValue().getProductName());
    }

    @Test
    void submitControlledFile_dhfCategoryRequiresProjectCodeProductNumber() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("A/1");
        reqVO.setProductMasterId(null);
        reqVO.setProductCode(null);
        when(categoryMapper.selectById(10L)).thenReturn(DccFileCategoryDO.builder()
                .id(10L).code("DCC_FVM_DHF_005").name("项目策划书").active(Boolean.TRUE).source("LOCAL")
                .fileTypeTaxonomyId(8803L).build());
        when(projectCodeMapper.selectById(3000L)).thenReturn(DccProjectCodeDO.builder()
                .id(3000L)
                .projectName("验证项目")
                .projectCode("")
                .status(DccProjectCodeStatusConstants.ENABLE)
                .build());
        when(fileTypeTaxonomyAdminService.resolveActivePath(8803L)).thenReturn(defaultTaxonomyPath());
        when(fileTypeTaxonomyAdminService.listActiveDescendantIds(8803L)).thenReturn(List.of(8803L));
        when(fileTypeTaxonomyAdminService.listActiveDescendantPaths(8803L)).thenReturn(List.of(defaultTaxonomyPath()));
        assertServiceException(() -> workflowService.submitControlledFile(99L, reqVO),
                CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING);

        verify(controlledFileMapper, never()).insert(any(DccControlledFileDO.class));
    }

    @Test
    void submitControlledFileWithoutApproval_success() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("A/1");
        mockCommonSubmitDependencies();
        when(directoryMapper.selectEnabledList()).thenReturn(List.of(
                directory(20L, null, "01.图纸"),
                directory(21L, 20L, "二级目录")));
        doAnswer(invocation -> {
            DccControlledFileMasterDO master = invocation.getArgument(0);
            master.setId(700L);
            return 1;
        }).when(controlledFileMasterMapper).insert(any(DccControlledFileMasterDO.class));
        doAnswer(invocation -> {
            DccControlledFileDO file = invocation.getArgument(0);
            file.setId(901L);
            return 1;
        }).when(controlledFileMapper).insert(any(DccControlledFileDO.class));

        Long fileId = workflowService.submitControlledFileWithoutApproval(99L, reqVO);

        assertEquals(901L, fileId);
        ArgumentCaptor<DccControlledFileDO> fileCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).insert(fileCaptor.capture());
        assertEquals(DccControlledFileStatusEnum.FINALIZING.getStatus(), fileCaptor.getValue().getStatus());
        assertEquals(21L, fileCaptor.getValue().getDirectoryId());
        assertEquals("A/1", fileCaptor.getValue().getVersionNo());
        assertEquals(null, fileCaptor.getValue().getProcessDefinitionKey());
        verify(finalizationService).activateWithoutApproval(901L, true);
        verify(bpmProcessInstanceApi, never()).createProcessInstance(any(Long.class), any());
        verify(routeSnapshotMapper, never()).insert(any(DccControlledFileRouteSnapshotDO.class));
    }

    @Test
    void submitControlledFileWithoutApproval_rawSourceIsCopiedAndClaimedWithoutChangingOriginalHistory() {
        DccControlledFileSubmitReqVO reqVO = buildRawFileIdSubmitReqVO("V1.0");
        mockCommonSubmitDependencies();
        when(directoryMapper.selectEnabledList()).thenReturn(List.of(
                directory(20L, null, "01.图纸"),
                directory(21L, 20L, "01.01.设备图纸")));
        when(sourceOwnershipService.prepareSubmissionSource(100L, true))
                .thenReturn(new DccControlledFilePreparedSource(6100L, 100L, "copied-sha256", true));
        doAnswer(invocation -> {
            DccControlledFileDO file = invocation.getArgument(0);
            file.setId(961L);
            return 1;
        }).when(controlledFileMapper).insert(any(DccControlledFileDO.class));

        Long fileId = workflowService.submitControlledFileWithoutApproval(99L, reqVO);

        assertEquals(961L, fileId);
        ArgumentCaptor<DccControlledFileDO> fileCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).insert(fileCaptor.capture());
        assertEquals(6100L, fileCaptor.getValue().getSourceFileId());
        assertEquals(100L, fileCaptor.getValue().getOriginalFileId());
        verify(sourceOwnershipService).claimSubmissionSource(961L,
                new DccControlledFilePreparedSource(6100L, 100L, "copied-sha256", true), 99L, "SUBMISSION");
    }

    @Test
    void submitControlledFileWithoutApproval_formCenterApprovalRegistersPlatformCandidateBeforeFinalization() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("V1.0");
        mockCommonSubmitDependencies();
        when(directoryMapper.selectEnabledList()).thenReturn(List.of(
                directory(20L, null, "01.图纸"),
                directory(21L, 20L, "二级目录")));
        doAnswer(invocation -> {
            DccControlledFileMasterDO master = invocation.getArgument(0);
            master.setId(700L);
            return 1;
        }).when(controlledFileMasterMapper).insert(any(DccControlledFileMasterDO.class));
        doAnswer(invocation -> {
            DccControlledFileDO file = invocation.getArgument(0);
            file.setId(901L);
            return 1;
        }).when(controlledFileMapper).insert(any(DccControlledFileDO.class));

        Long fileId = workflowService.submitControlledFileWithoutApproval(99L, reqVO,
                "form-process-1", "effect-idem-1");

        assertEquals(901L, fileId);
        ArgumentCaptor<DccControlledFileDO> fileCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(platformAdapter).recordApprovedUploadFinalizationStarted(fileCaptor.capture(), eq(99L),
                eq("form-process-1"), eq("effect-idem-1"));
        assertEquals(901L, fileCaptor.getValue().getId());
        assertEquals("form-process-1", fileCaptor.getValue().getProcessInstanceId());
        verify(finalizationService).activateWithoutApproval(901L, true);
    }

    @Test
    void submitControlledFileWithoutApproval_formCenterRevisionStopsAtReadyToPublish() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("V1.1");
        reqVO.setChangeType(DccControlledFileChangeTypeEnum.REVISION.getCode());
        reqVO.setFileName("SOP-001-修订");
        mockCommonSubmitDependencies();
        DccControlledFileMasterDO master = DccControlledFileMasterDO.builder()
                .id(700L)
                .categoryId(10L)
                .directoryId(21L)
                .fileName("SOP-001")
                .fileNumber("SOP-001")
                .currentActiveControlledFileId(800L)
                .status(DccControlledFileMasterStatusEnum.ACTIVE_CHAIN.getCode())
                .build();
        DccControlledFileDO activeFile = DccControlledFileDO.builder()
                .id(800L)
                .masterId(700L)
                .categoryId(10L)
                .directoryId(21L)
                .fileName("SOP-001")
                .fileNumber("SOP-001")
                .versionNo("V1.0")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .sourceFileId(88L)
                .originalFileId(88L)
                .build();
        when(controlledFileMasterMapper.selectListByFileNumber("SOP-001")).thenReturn(List.of(master));
        when(controlledFileMapper.selectById(800L)).thenReturn(activeFile);
        lenient().when(controlledFileMapper.selectList(
                org.mockito.ArgumentMatchers.<SFunction<DccControlledFileDO, ?>>any(), eq(700L)))
                .thenReturn(List.of(activeFile));
        doAnswer(invocation -> {
            DccControlledFileDO file = invocation.getArgument(0);
            file.setId(902L);
            return 1;
        }).when(controlledFileMapper).insert(any(DccControlledFileDO.class));

        Long fileId = workflowService.submitControlledFileWithoutApproval(99L, reqVO,
                "form-process-revision", "effect-idem-revision");

        assertEquals(902L, fileId);
        ArgumentCaptor<DccControlledFileDO> fileCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).insert(fileCaptor.capture());
        assertEquals(DccControlledFileChangeTypeEnum.REVISION.getCode(), fileCaptor.getValue().getChangeType());
        assertEquals(DccControlledFileStatusEnum.READY_TO_PUBLISH.getStatus(), fileCaptor.getValue().getStatus());
        assertEquals(88L, fileCaptor.getValue().getOriginalFileId());
        assertEquals(100L, fileCaptor.getValue().getSourceFileId());
        verify(platformAdapter).recordApprovedUploadReadyToPublish(fileCaptor.getValue(), 99L,
                "form-process-revision", "effect-idem-revision");
        verify(finalizationService, never()).activateWithoutApproval(any(Long.class), any(Boolean.class));
    }

    @Test
    void submitControlledFileWithoutApproval_allowsEmptyProductBinding() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("A/1");
        reqVO.setProductMasterId(null);
        reqVO.setProductCode(null);
        mockCommonSubmitDependencies();
        when(directoryMapper.selectEnabledList()).thenReturn(List.of(
                directory(20L, null, "01.图纸"),
                directory(21L, 20L, "二级目录")));
        doAnswer(invocation -> {
            DccControlledFileMasterDO master = invocation.getArgument(0);
            master.setId(700L);
            return 1;
        }).when(controlledFileMasterMapper).insert(any(DccControlledFileMasterDO.class));
        doAnswer(invocation -> {
            DccControlledFileDO file = invocation.getArgument(0);
            file.setId(906L);
            return 1;
        }).when(controlledFileMapper).insert(any(DccControlledFileDO.class));

        Long fileId = workflowService.submitControlledFileWithoutApproval(99L, reqVO);

        assertEquals(906L, fileId);
        ArgumentCaptor<DccControlledFileDO> fileCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).insert(fileCaptor.capture());
        assertNull(fileCaptor.getValue().getProductMasterId());
        assertNull(fileCaptor.getValue().getProductCode());
        assertNull(fileCaptor.getValue().getProductName());
        verify(finalizationService).activateWithoutApproval(906L, true);
    }

    @Test
    void submitControlledFileWithoutApproval_externalReviewProcessType_persistsKnownType() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("A/1");
        reqVO.setProcessType(DccControlledFileProcessTypeEnum.EXTERNAL_REVIEW.getCode());
        mockCommonSubmitDependencies();
        when(directoryMapper.selectEnabledList()).thenReturn(List.of(
                directory(20L, null, "01.图纸"),
                directory(21L, 20L, "二级目录")));
        doAnswer(invocation -> {
            DccControlledFileMasterDO master = invocation.getArgument(0);
            master.setId(700L);
            return 1;
        }).when(controlledFileMasterMapper).insert(any(DccControlledFileMasterDO.class));
        doAnswer(invocation -> {
            DccControlledFileDO file = invocation.getArgument(0);
            file.setId(904L);
            return 1;
        }).when(controlledFileMapper).insert(any(DccControlledFileDO.class));

        Long fileId = workflowService.submitControlledFileWithoutApproval(99L, reqVO);

        assertEquals(904L, fileId);
        ArgumentCaptor<DccControlledFileDO> fileCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).insert(fileCaptor.capture());
        assertEquals(DccControlledFileProcessTypeEnum.EXTERNAL_REVIEW.getCode(), fileCaptor.getValue().getProcessType());
        verify(finalizationService).activateWithoutApproval(904L, true);
    }

    @Test
    void submitControlledFile_invalidProcessType_throwsBeforeInsert() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("A/1");
        reqVO.setProcessType("TEMP_FLOW");

        assertServiceException(() -> workflowService.submitControlledFileWithoutApproval(99L, reqVO),
                CONTROLLED_FILE_PROCESS_TYPE_INVALID);
        verify(controlledFileMapper, never()).insert(any(DccControlledFileDO.class));
    }

    @Test
    void submitControlledFileWithoutApproval_allowsNonLeafDirectorySelection() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("A/1");
        reqVO.setDirectoryId(20L);
        mockCommonSubmitDependencies();
        when(directoryMapper.selectEnabledList()).thenReturn(List.of(
                directory(20L, null, "01.图纸"),
                directory(21L, 20L, "二级目录")));
        doAnswer(invocation -> {
            DccControlledFileMasterDO master = invocation.getArgument(0);
            master.setId(700L);
            return 1;
        }).when(controlledFileMasterMapper).insert(any(DccControlledFileMasterDO.class));
        doAnswer(invocation -> {
            DccControlledFileDO file = invocation.getArgument(0);
            file.setId(902L);
            return 1;
        }).when(controlledFileMapper).insert(any(DccControlledFileDO.class));

        Long fileId = workflowService.submitControlledFileWithoutApproval(99L, reqVO);

        assertEquals(902L, fileId);
        ArgumentCaptor<DccControlledFileDO> fileCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).insert(fileCaptor.capture());
        assertEquals(20L, fileCaptor.getValue().getDirectoryId());
        verify(finalizationService).activateWithoutApproval(902L, true);
    }

    @Test
    void submitControlledFileWithoutApproval_existingSingleV1_replacesOldVersion() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("V1.0");
        mockCommonSubmitDependencies();
        DccControlledFileMasterDO master = DccControlledFileMasterDO.builder()
                .id(700L)
                .categoryId(10L)
                .directoryId(21L)
                .fileName("SOP-001")
                .fileNumber("SOP-001")
                .currentActiveControlledFileId(800L)
                .build();
        DccControlledFileDO existingFile = DccControlledFileDO.builder()
                .id(800L)
                .masterId(700L)
                .categoryId(10L)
                .directoryId(21L)
                .fileName("SOP-001")
                .fileNumber("SOP-001")
                .versionNo("V1.0")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build();
        when(controlledFileMasterMapper.selectByCategoryIdAndDirectoryIdAndFileName(10L, 21L, "SOP-001"))
                .thenReturn(master);
        when(controlledFileMasterMapper.selectListByFileNumber("SOP-001")).thenReturn(List.of(master));
        when(controlledFileMapper.selectListByMasterId(700L))
                .thenReturn(List.of(existingFile), List.of());
        doAnswer(invocation -> {
            DccControlledFileDO file = invocation.getArgument(0);
            file.setId(903L);
            return 1;
        }).when(controlledFileMapper).insert(any(DccControlledFileDO.class));

        Long fileId = workflowService.submitControlledFileWithoutApproval(99L, reqVO);

        assertEquals(903L, fileId);
        verify(controlledFileMapper).deleteById(800L);
        verify(controlledFileMasterMapper).update(eq(null), any());
        verify(finalizationService).activateWithoutApproval(903L, true);
    }

    @Test
    void submitControlledFileWithoutApproval_sameFileNameInDifferentDirectoryCreatesSeparateMaster() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("A/1");
        reqVO.setDirectoryId(22L);
        mockCommonSubmitDependencies();
        when(directoryMapper.selectEnabledList()).thenReturn(List.of(
                directory(20L, null, "01.图纸"),
                directory(21L, 20L, "二级目录"),
                directory(22L, 20L, "同名文件目录")));
        doAnswer(invocation -> {
            DccControlledFileMasterDO master = invocation.getArgument(0);
            master.setId(701L);
            return 1;
        }).when(controlledFileMasterMapper).insert(any(DccControlledFileMasterDO.class));
        doAnswer(invocation -> {
            DccControlledFileDO file = invocation.getArgument(0);
            file.setId(904L);
            return 1;
        }).when(controlledFileMapper).insert(any(DccControlledFileDO.class));

        Long fileId = workflowService.submitControlledFileWithoutApproval(99L, reqVO);

        assertEquals(904L, fileId);
        verify(controlledFileMapper, never()).deleteById(800L);
        ArgumentCaptor<DccControlledFileMasterDO> masterCaptor =
                ArgumentCaptor.forClass(DccControlledFileMasterDO.class);
        verify(controlledFileMasterMapper).insert(masterCaptor.capture());
        assertEquals(22L, masterCaptor.getValue().getDirectoryId());
        ArgumentCaptor<DccControlledFileDO> fileCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).insert(fileCaptor.capture());
        assertEquals(701L, fileCaptor.getValue().getMasterId());
        assertEquals(22L, fileCaptor.getValue().getDirectoryId());
        verify(finalizationService).activateWithoutApproval(904L, true);
    }

    @Test
    void submitControlledFileWithoutApproval_deletedNasMaster_restoresMasterBeforeInsert() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("V1.0");
        mockCommonSubmitDependencies();
        DccControlledFileMasterDO deletedMaster = DccControlledFileMasterDO.builder()
                .id(700L)
                .categoryId(10L)
                .directoryId(21L)
                .fileName("SOP-001")
                .fileNumber("SOP-001")
                .status(DccControlledFileMasterStatusEnum.ACTIVE_CHAIN.getCode())
                .build();
        AtomicBoolean restored = new AtomicBoolean(false);
        when(controlledFileMasterMapper.selectDeletedByCategoryIdAndDirectoryIdAndFileName(10L, 21L, "SOP-001"))
                .thenReturn(deletedMaster);
        when(controlledFileMasterMapper.restoreDeletedNasMaster(700L, 10L, 21L, "SOP-001", "SOP-001",
                DccControlledFileMasterStatusEnum.ACTIVE_CHAIN.getCode()))
                .thenAnswer(invocation -> {
                    restored.set(true);
                    return 1;
                });
        when(controlledFileMasterMapper.selectListByFileNumber("SOP-001"))
                .thenAnswer(invocation -> restored.get() ? List.of(deletedMaster) : List.of());
        doAnswer(invocation -> {
            DccControlledFileDO file = invocation.getArgument(0);
            file.setId(905L);
            return 1;
        }).when(controlledFileMapper).insert(any(DccControlledFileDO.class));

        Long fileId = workflowService.submitControlledFileWithoutApproval(99L, reqVO);

        assertEquals(905L, fileId);
        verify(controlledFileMasterMapper).restoreDeletedNasMaster(700L, 10L, 21L, "SOP-001", "SOP-001",
                DccControlledFileMasterStatusEnum.ACTIVE_CHAIN.getCode());
        verify(controlledFileMasterMapper, never()).insert(any(DccControlledFileMasterDO.class));
        ArgumentCaptor<DccControlledFileDO> fileCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).insert(fileCaptor.capture());
        assertEquals(700L, fileCaptor.getValue().getMasterId());
        verify(finalizationService).activateWithoutApproval(905L, true);
    }

    @Test
    void submitControlledFile_bindingDirectoryWithChildren_requiresLeafDirectorySelection() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("A/1");
        reqVO.setDirectoryId(20L);
        mockCommonSubmitDependencies();

        assertServiceException(() -> workflowService.submitControlledFile(99L, reqVO),
                CONTROLLED_FILE_SUBMIT_DIRECTORY_NOT_LEAF);
        verify(bpmProcessInstanceApi, never()).createProcessInstance(any(Long.class), any());
    }

    @Test
    void submitControlledFile_directoryOutsideBindingSubtree_throwsInvalidDirectory() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("A/1");
        reqVO.setDirectoryId(30L);
        mockCommonSubmitDependencies();
        when(directoryMapper.selectEnabledList()).thenReturn(List.of(
                directory(20L, null, "01.图纸"),
                directory(21L, 20L, "二级目录"),
                directory(30L, null, "越界目录")));

        assertServiceException(() -> workflowService.submitControlledFile(99L, reqVO),
                CONTROLLED_FILE_SUBMIT_DIRECTORY_INVALID);
        verify(bpmProcessInstanceApi, never()).createProcessInstance(any(Long.class), any());
    }

    @Test
    void submitControlledFile_bindingDirectoryAlreadyLeaf_allowsSubmitWithBindingDirectory() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("A/1");
        reqVO.setDirectoryId(20L);
        when(categoryMapper.selectById(10L)).thenReturn(DccFileCategoryDO.builder()
                .id(10L).code("SOP").name("SOP").active(Boolean.TRUE).source("LOCAL")
                .fileTypeTaxonomyId(8803L).build());
        when(categoryDirectoryBindingMapper.selectActiveByCategoryId(10L)).thenReturn(
                DccCategoryDirectoryBindingDO.builder().id(1L).categoryId(10L).directoryId(20L).active(Boolean.TRUE).build());
        when(directoryMapper.selectEnabledList()).thenReturn(List.of(directory(20L, null, "叶子目录")));
        lenient().when(routeMapper.selectLatestActiveByCategoryId(10L)).thenReturn(
                DccCategoryApprovalRouteDO.builder().id(30L).categoryId(10L).versionNo(2).active(Boolean.TRUE).effectiveTime(LocalDateTime.now()).build());
        when(routeNodeMapper.selectListByRouteId(30L)).thenReturn(fullApprovalRoute(
                routeNode(1, DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode(), "Doc Control Review", "USER", 200L)));
        when(fileMapper.selectById(100L)).thenReturn(FileDO.builder()
                .id(100L).name("SOP-001.docx")
                .type("application/vnd.openxmlformats-officedocument.wordprocessingml.document").build());
        doAnswer(invocation -> {
            DccControlledFileMasterDO master = invocation.getArgument(0);
            master.setId(700L);
            return 1;
        }).when(controlledFileMasterMapper).insert(any(DccControlledFileMasterDO.class));
        doAnswer(invocation -> {
            DccControlledFileDO file = invocation.getArgument(0);
            file.setId(900L);
            return 1;
        }).when(controlledFileMapper).insert(any(DccControlledFileDO.class));
        when(bpmProcessInstanceApi.createProcessInstance(any(Long.class), any())).thenReturn("proc-1");

        Long fileId = workflowService.submitControlledFile(99L, reqVO);

        assertEquals(900L, fileId);
        ArgumentCaptor<DccControlledFileDO> fileCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).insert(fileCaptor.capture());
        assertEquals(20L, fileCaptor.getValue().getDirectoryId());
    }

    @Test
    void submitControlledFile_categoryWithoutDirectoryBindingUsesUnclassifiedDirectory() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("A/1");
        reqVO.setDirectoryId(900L);
        mockCommonSubmitDependencies();
        when(categoryDirectoryBindingMapper.selectActiveByCategoryId(10L)).thenReturn(null);
        when(directoryMapper.selectEnabledList()).thenReturn(List.of(
                directory(900L, 0L, "未分类", "UNCLASSIFIED"),
                directory(30L, null, "其他根目录")));
        mockFourStageRoute();
        doAnswer(invocation -> {
            DccControlledFileDO file = invocation.getArgument(0);
            file.setId(900L);
            return 1;
        }).when(controlledFileMapper).insert(any(DccControlledFileDO.class));
        when(bpmProcessInstanceApi.createProcessInstance(any(Long.class), any())).thenReturn("proc-1");

        Long fileId = workflowService.submitControlledFile(99L, reqVO);

        assertEquals(900L, fileId);
        ArgumentCaptor<DccControlledFileDO> fileCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).insert(fileCaptor.capture());
        assertEquals(900L, fileCaptor.getValue().getDirectoryId());
    }

    @Test
    void submitControlledFile_categoryMissing_throwsNotExists() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("A/1");
        when(categoryMapper.selectById(10L)).thenReturn(null);

        assertServiceException(() -> workflowService.submitControlledFile(99L, reqVO), FILE_CATEGORY_NOT_EXISTS);
    }

    @Test
    void submitControlledFile_bindingMissingAndUnclassifiedDirectoryMissing_throwsNotExists() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("A/1");
        when(categoryMapper.selectById(10L)).thenReturn(DccFileCategoryDO.builder()
                .id(10L).active(Boolean.TRUE).source("LOCAL").fileTypeTaxonomyId(8803L).build());
        when(categoryDirectoryBindingMapper.selectActiveByCategoryId(10L)).thenReturn(null);
        when(directoryMapper.selectEnabledList()).thenReturn(List.of(directory(30L, null, "其他根目录")));

        assertServiceException(() -> workflowService.submitControlledFile(99L, reqVO),
                FILE_CATEGORY_UNCLASSIFIED_DIRECTORY_NOT_EXISTS);
    }

    @Test
    void submitControlledFile_routeMissing_throwsNotConfigured() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("A/1");
        mockCommonSubmitDependencies();
        when(routeMapper.selectLatestActiveByCategoryId(10L)).thenReturn(null);

        assertServiceException(() -> workflowService.submitControlledFile(99L, reqVO),
                CONTROLLED_FILE_ROUTE_NOT_CONFIGURED);
    }

    @Test
    void submitControlledFile_positionHasNoResolvedUsers_throwsApproverMissing() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("A/1");
        mockCommonSubmitDependencies();
        when(routeNodeMapper.selectListByRouteId(30L)).thenReturn(fullApprovalRoute(
                routeNode(1, DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode(), "Doc Control Review", "POSITION", 50L)));
        when(positionAssignmentMapper.selectActiveListByPositionId(50L)).thenReturn(List.of());

        assertServiceException(() -> workflowService.submitControlledFile(99L, reqVO),
                ROUTE_PREVIEW_APPROVER_NOT_FOUND);
        verify(bpmProcessInstanceApi, never()).createProcessInstance(any(Long.class), any());
    }

    @Test
    void submitControlledFile_userCandidateInvalid_throwsBeforeInsertSnapshotAndBpm() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("A/1");
        mockCommonSubmitDependencies();
        when(routeNodeMapper.selectListByRouteId(30L)).thenReturn(fullApprovalRoute(
                routeNode(1, DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode(),
                        "Doc Control Review", "USER", 200L)));
        doAnswer(invocation -> {
            Collection<Long> userIds = invocation.getArgument(0);
            if (userIds.contains(200L)) {
                throw new ServiceException(1_002_000_004, "user disabled");
            }
            return null;
        }).when(adminUserApi).validateUserList(any());

        assertServiceException(() -> workflowService.submitControlledFile(99L, reqVO),
                ROUTE_PREVIEW_APPROVER_NOT_FOUND);
        verify(controlledFileMapper, never()).insert(any(DccControlledFileDO.class));
        verify(routeSnapshotMapper, never()).insert(any(DccControlledFileRouteSnapshotDO.class));
        verify(bpmProcessInstanceApi, never()).createProcessInstance(any(Long.class), any());
    }

    @Test
    void submitControlledFile_positionDirectUserInvalid_throwsBeforeInsertSnapshotAndBpm() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("A/1");
        mockCommonSubmitDependencies();
        when(routeNodeMapper.selectListByRouteId(30L)).thenReturn(fullApprovalRoute(
                routeNode(1, DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode(),
                        "Doc Control Review", "POSITION", 50L)));
        when(positionAssignmentMapper.selectActiveListByPositionId(50L)).thenReturn(List.of(
                DccPositionAssignmentDO.builder()
                        .id(60L)
                        .positionId(50L)
                        .assignmentType("USER")
                        .userId(200L)
                        .active(Boolean.TRUE)
                        .build()));
        doAnswer(invocation -> {
            Collection<Long> userIds = invocation.getArgument(0);
            if (userIds.contains(200L)) {
                throw new ServiceException(1_002_000_004, "user disabled");
            }
            return null;
        }).when(adminUserApi).validateUserList(any());

        assertServiceException(() -> workflowService.submitControlledFile(99L, reqVO),
                ROUTE_PREVIEW_APPROVER_NOT_FOUND);
        verify(controlledFileMapper, never()).insert(any(DccControlledFileDO.class));
        verify(routeSnapshotMapper, never()).insert(any(DccControlledFileRouteSnapshotDO.class));
        verify(bpmProcessInstanceApi, never()).createProcessInstance(any(Long.class), any());
    }

    @Test
    void submitControlledFile_missingRequiredMetadata_throws() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("A/1");
        reqVO.setFileNumber(null);

        assertServiceException(() -> workflowService.submitControlledFile(99L, reqVO),
                CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING);
    }

    @Test
    void submitControlledFile_fileNumberConflict_throws() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("1.1");
        mockCommonSubmitDependencies();
        lenient().when(routeNodeMapper.selectListByRouteId(30L)).thenReturn(fullApprovalRoute(
                routeNode(1, DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode(), "Doc Control Review", "USER", 200L)));
        when(controlledFileMasterMapper.selectByNewLogicalIdentity(1L, 3000L, 8803L, "SOP-001"))
                .thenReturn(DccControlledFileMasterDO.builder()
                        .id(700L).categoryId(10L).directoryId(21L)
                        .fileName("SOP-001").fileNumber("SOP-001").build());

        assertServiceException(() -> workflowService.submitControlledFile(99L, reqVO),
                CONTROLLED_FILE_FILE_NUMBER_CONFLICT);
    }

    @Test
    void previewRoute_withoutCategoryUploadPermission_success() {
        when(categoryMapper.selectById(10L)).thenReturn(DccFileCategoryDO.builder()
                .id(10L).active(Boolean.TRUE).source("LOCAL").build());
        when(routeMapper.selectLatestActiveByCategoryId(10L)).thenReturn(
                DccCategoryApprovalRouteDO.builder().id(30L).categoryId(10L).versionNo(2).active(Boolean.TRUE).build());
        when(routeNodeMapper.selectListByRouteId(30L)).thenReturn(fullApprovalRoute(
                routeNode(1, DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode(), "Doc Control Review", "USER", 200L)));

        DccControlledFileRouteReadinessRespVO readiness = workflowService.previewRoute(99L, 10L, List.of());
        List<DccControlledFileRoutePreviewRespVO> respVOS = readiness.getNodes();

        assertTrue(readiness.getReady());
        assertEquals(4, respVOS.size());
        assertEquals(1, respVOS.get(0).getStageNo());
        assertEquals(DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode(), respVOS.get(0).getStageCode());
        assertEquals(List.of(200L), respVOS.get(0).getResolvedUserIds());
        verify(permissionSupport, never()).hasCategoryPermission(any(Long.class), any(Long.class),
                any(DccFileCategoryPermissionActionEnum.class));
    }

    @Test
    void previewRoute_positionHasNoResolvedUsers_throwsApproverMissing() {
        when(categoryMapper.selectById(10L)).thenReturn(DccFileCategoryDO.builder()
                .id(10L).active(Boolean.TRUE).source("LOCAL").build());
        when(routeMapper.selectLatestActiveByCategoryId(10L)).thenReturn(
                DccCategoryApprovalRouteDO.builder().id(30L).categoryId(10L).versionNo(2).active(Boolean.TRUE).build());
        when(routeNodeMapper.selectListByRouteId(30L)).thenReturn(fullApprovalRoute(
                routeNode(1, DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode(), "Doc Control Review", "POSITION", 50L)));
        when(positionAssignmentMapper.selectActiveListByPositionId(50L)).thenReturn(List.of());

        assertServiceException(() -> workflowService.previewRoute(99L, 10L, List.of()),
                ROUTE_PREVIEW_APPROVER_NOT_FOUND);
    }

    @Test
    void previewRoute_uploaderDerivedPosition_usesSubmitterContextResolver() {
        when(categoryMapper.selectById(10L)).thenReturn(DccFileCategoryDO.builder()
                .id(10L).active(Boolean.TRUE).source("LOCAL").build());
        when(routeMapper.selectLatestActiveByCategoryId(10L)).thenReturn(
                DccCategoryApprovalRouteDO.builder().id(30L).categoryId(10L).versionNo(2).active(Boolean.TRUE).build());
        when(routeNodeMapper.selectListByRouteId(30L)).thenReturn(fullApprovalRoute(
                routeNode(1, DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode(), "Doc Control Review", "POSITION", 50L)));
        when(positionRuntimeResolver.isUploaderDerivedPosition(50L)).thenReturn(Boolean.TRUE);
        when(positionRuntimeResolver.resolveUserIds(50L, 99L, false)).thenReturn(List.of(300L));

        List<DccControlledFileRoutePreviewRespVO> respVOS = workflowService.previewRoute(99L, 10L, List.of()).getNodes();

        assertEquals(4, respVOS.size());
        assertEquals(List.of(300L), respVOS.get(0).getResolvedUserIds());
    }

    @Test
    void previewRoute_authorizedRepresentative_usesDccAssignmentInsteadOfRuntimeResolver() {
        when(categoryMapper.selectById(10L)).thenReturn(DccFileCategoryDO.builder()
                .id(10L).active(Boolean.TRUE).source("LOCAL").build());
        when(routeMapper.selectLatestActiveByCategoryId(10L)).thenReturn(
                DccCategoryApprovalRouteDO.builder().id(30L).categoryId(10L).versionNo(2).active(Boolean.TRUE).build());
        when(routeNodeMapper.selectListByRouteId(30L)).thenReturn(fullApprovalRoute(
                routeNode(1, DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode(), "Doc Control Review", "POSITION", 900334L)));
        when(positionRuntimeResolver.isUploaderDerivedPosition(900334L)).thenReturn(Boolean.FALSE);
        when(positionAssignmentMapper.selectActiveListByPositionId(900334L)).thenReturn(List.of(
                DccPositionAssignmentDO.builder().id(61L).positionId(900334L)
                        .assignmentType("USER").userId(301L).active(Boolean.TRUE).build()));

        List<DccControlledFileRoutePreviewRespVO> respVOS = workflowService.previewRoute(99L, 10L, List.of()).getNodes();

        assertEquals(4, respVOS.size());
        assertEquals(List.of(301L), respVOS.get(0).getResolvedUserIds());
        verify(positionRuntimeResolver, never()).resolveUserIds(900334L, 99L, false);
    }

    @Test
    void withdrawControlledFile_pendingDocControlReview_allowsWithdraw() {
        assertWithdrawAllowed(DccControlledFileStatusEnum.PENDING_DOC_CONTROL_REVIEW.getStatus());
    }

    @Test
    void withdrawControlledFile_pendingMatrixReview_allowsWithdraw() {
        assertWithdrawAllowed(DccControlledFileStatusEnum.PENDING_MATRIX_REVIEW.getStatus());
    }

    @Test
    void withdrawControlledFile_pendingMatrixApproval_allowsWithdraw() {
        assertWithdrawAllowed(DccControlledFileStatusEnum.PENDING_MATRIX_APPROVAL.getStatus());
    }

    @Test
    void withdrawControlledFile_pendingDocControlApproval_allowsWithdraw() {
        assertWithdrawAllowed(DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL.getStatus());
    }

    @Test
    void withdrawControlledFile_notOwner_throws() {
        when(controlledFileMapper.selectById(900L)).thenReturn(DccControlledFileDO.builder()
                .id(900L).requesterId(100L).processInstanceId("proc-1")
                .status(DccControlledFileStatusEnum.PENDING_DOC_CONTROL_REVIEW.getStatus()).build());
        DccControlledFileWithdrawReqVO reqVO = new DccControlledFileWithdrawReqVO();
        reqVO.setReason("stop");

        assertServiceException(() -> workflowService.withdrawControlledFile(99L, 900L, reqVO),
                CONTROLLED_FILE_WITHDRAW_NOT_ALLOWED);
    }

    @Test
    void withdrawControlledFile_finalizing_throws() {
        when(controlledFileMapper.selectById(900L)).thenReturn(DccControlledFileDO.builder()
                .id(900L).requesterId(99L).processInstanceId("proc-1")
                .status(DccControlledFileStatusEnum.FINALIZING.getStatus()).build());
        DccControlledFileWithdrawReqVO reqVO = new DccControlledFileWithdrawReqVO();
        reqVO.setReason("stop");

        assertServiceException(() -> workflowService.withdrawControlledFile(99L, 900L, reqVO),
                CONTROLLED_FILE_WITHDRAW_NOT_ALLOWED);
    }

    @Test
    void submitControlledFile_externalReviewProcessType_requiresExternalReviewEndpoint() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("A/1");
        reqVO.setProcessType(DccControlledFileProcessTypeEnum.EXTERNAL_REVIEW.getCode());

        assertServiceException(() -> workflowService.submitControlledFile(99L, reqVO),
                EXTERNAL_FILE_REVIEW_ENDPOINT_REQUIRED);

        verify(controlledFileMapper, never()).insert(any(DccControlledFileDO.class));
        verify(bpmProcessInstanceApi, never()).createProcessInstance(any(Long.class), any());
    }

    @Test
    void deleteWithdrawnControlledFile_withdrawnOwner_deletesBusinessRevisionAndRetainsArtifacts() throws Exception {
        when(controlledFileMapper.selectById(900L)).thenReturn(DccControlledFileDO.builder()
                .id(900L)
                .requesterId(99L)
                .processInstanceId("proc-withdrawn")
                .status(DccControlledFileStatusEnum.WITHDRAWN.getStatus())
                .sourceFileId(1000L)
                .originalFileId(1000L)
                .drawingPdfFileId(1001L)
                .build());
        workflowService.deleteWithdrawnControlledFile(99L, 900L);

        verify(controlledFileMapper).deleteById(900L);
        verify(fileService, never()).deleteFileList(any());
        verify(routeSnapshotMapper, never()).delete(
                org.mockito.ArgumentMatchers.<SFunction<DccControlledFileRouteSnapshotDO, ?>>any(), any());
        verify(bpmProcessInstanceService, never()).cancelProcessInstanceByStartUser(any(), any());
    }

    @Test
    void deleteWithdrawnControlledFile_retainsArtifactsRegardlessOfReferenceCount() throws Exception {
        when(controlledFileMapper.selectById(900L)).thenReturn(DccControlledFileDO.builder()
                .id(900L)
                .requesterId(99L)
                .processInstanceId("proc-withdrawn")
                .status(DccControlledFileStatusEnum.WITHDRAWN.getStatus())
                .sourceFileId(1000L)
                .originalFileId(1000L)
                .drawingPdfFileId(1001L)
                .build());
        workflowService.deleteWithdrawnControlledFile(99L, 900L);

        verify(controlledFileMapper).deleteById(900L);
        verify(fileService, never()).deleteFileList(any());
    }

    @Test
    void deleteWithdrawnControlledFile_activeVersion_throwsAndDoesNotDeleteCurrentVersion() {
        when(controlledFileMapper.selectById(900L)).thenReturn(DccControlledFileDO.builder()
                .id(900L)
                .requesterId(99L)
                .processInstanceId("proc-active")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build());

        assertServiceException(() -> workflowService.deleteWithdrawnControlledFile(99L, 900L),
                CONTROLLED_FILE_WITHDRAWN_ACTION_NOT_ALLOWED);

        verify(controlledFileMapper, never()).deleteById(any(Long.class));
    }

    @Test
    void deleteWithdrawnControlledFile_alreadyResubmitted_throwsAndKeepsOldRecord() {
        when(controlledFileMapper.selectById(900L)).thenReturn(DccControlledFileDO.builder()
                .id(900L)
                .requesterId(99L)
                .processInstanceId("proc-old")
                .status(DccControlledFileStatusEnum.WITHDRAWN.getStatus())
                .supersededByFileId(901L)
                .build());

        assertServiceException(() -> workflowService.deleteWithdrawnControlledFile(99L, 900L),
                CONTROLLED_FILE_WITHDRAWN_ACTION_NOT_ALLOWED);

        verify(controlledFileMapper, never()).deleteById(any(Long.class));
    }

    @Test
    void resubmitWithdrawnControlledFile_alreadyResubmitted_throwsAndDoesNotCreateSecondBpm() {
        when(controlledFileMapper.selectById(900L)).thenReturn(DccControlledFileDO.builder()
                .id(900L)
                .requesterId(99L)
                .processInstanceId("proc-old")
                .status(DccControlledFileStatusEnum.WITHDRAWN.getStatus())
                .supersededByFileId(901L)
                .build());

        assertServiceException(() -> workflowService.resubmitWithdrawnControlledFile(99L, 900L),
                CONTROLLED_FILE_WITHDRAWN_ACTION_NOT_ALLOWED);

        verify(controlledFileMapper, never()).insert(any(DccControlledFileDO.class));
        verify(bpmProcessInstanceApi, never()).createProcessInstance(any(Long.class), any());
    }

    @Test
    void resubmitWithdrawnControlledFile_createsNewBpmInstanceAndKeepsOldWithdrawnRecord() {
        DccControlledFileDO withdrawn = DccControlledFileDO.builder()
                .id(900L)
                .masterId(700L)
                .categoryId(10L)
                .directoryId(21L)
                .originalFileId(100L)
                .sourceFileId(100L)
                .fileName("SOP-001")
                .fileNumber("SOP-001")
                .productMasterId(5000L)
                .productCode("PRD20260525001")
                .dccProjectCodeId(3000L)
                .needTraining(Boolean.FALSE)
                .processType("CONTROLLED_FILE")
                .changeType(DccControlledFileChangeTypeEnum.NEW.getCode())
                .versionNo("V1.0")
                .effectiveDate(LocalDate.of(2026, 5, 13))
                .remark("initial release")
                .requesterId(99L)
                .processInstanceId("proc-old")
                .processDefinitionKey(DccControlledFileWorkflowServiceImpl.BPM_PROCESS_DEFINITION_KEY)
                .status(DccControlledFileStatusEnum.WITHDRAWN.getStatus())
                .build();
        when(controlledFileMapper.selectById(900L)).thenReturn(withdrawn);
        mockCommonSubmitDependencies();
        when(controlledFileMasterMapper.selectListByFileNumber("SOP-001"))
                .thenReturn(List.of(DccControlledFileMasterDO.builder()
                        .id(700L)
                        .categoryId(10L)
                        .directoryId(21L)
                        .fileName("SOP-001")
                        .fileNumber("SOP-001")
                        .status(DccControlledFileMasterStatusEnum.ACTIVE_CHAIN.getCode())
                        .build()));
        when(routeNodeMapper.selectListByRouteId(30L)).thenReturn(fullApprovalRoute(
                routeNode(1, DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode(), "Doc Control Review", "POSITION", 50L)));
        when(positionAssignmentMapper.selectActiveListByPositionId(50L)).thenReturn(List.of(
                DccPositionAssignmentDO.builder().id(60L).positionId(50L).assignmentType("POST").systemPostId(500L).active(Boolean.TRUE).build()));
        when(adminUserApi.getUserListByPostIds(List.of(500L))).thenReturn(List.of(new AdminUserRespDTO().setId(200L).setStatus(0)));
        doAnswer(invocation -> {
            DccControlledFileDO file = invocation.getArgument(0);
            file.setId(901L);
            return 1;
        }).when(controlledFileMapper).insert(any(DccControlledFileDO.class));
        when(bpmProcessInstanceApi.createProcessInstance(any(Long.class), any())).thenReturn("proc-new");

        Long newFileId = workflowService.resubmitWithdrawnControlledFile(99L, 900L);

        assertEquals(901L, newFileId);
        ArgumentCaptor<DccControlledFileDO> insertCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).insert(insertCaptor.capture());
        assertEquals(700L, insertCaptor.getValue().getMasterId());
        assertEquals("A/2", insertCaptor.getValue().getVersionNo());
        assertEquals(900L, insertCaptor.getValue().getPredecessorControlledFileId());
        assertNull(insertCaptor.getValue().getProductMasterId());
        assertNull(insertCaptor.getValue().getProductCode());
        assertEquals(DccControlledFileStatusEnum.PENDING_DOC_CONTROL_REVIEW.getStatus(), insertCaptor.getValue().getStatus());
        ArgumentCaptor<DccControlledFileDO> updateCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper, org.mockito.Mockito.times(2)).updateById(updateCaptor.capture());
        assertEquals(901L, updateCaptor.getAllValues().get(0).getId());
        assertEquals("proc-new", updateCaptor.getAllValues().get(0).getProcessInstanceId());
        assertEquals(900L, updateCaptor.getAllValues().get(1).getId());
        assertEquals(901L, updateCaptor.getAllValues().get(1).getSupersededByFileId());
        verify(controlledFileMapper, never()).deleteById(900L);
        verify(platformAdapter).recordResubmitted(withdrawn, 901L);
    }

    @Test
    void getControlledFile_success() {
        DccControlledFileDO file = DccControlledFileDO.builder()
                .id(900L)
                .title("SOP-001")
                .status(DccControlledFileStatusEnum.PENDING_DOC_CONTROL_REVIEW.getStatus())
                .effectiveDate(LocalDate.of(2026, 5, 30))
                .remark("pre-submit review")
                .build();
        when(controlledFileMapper.selectById(900L)).thenReturn(file);
        when(routeSnapshotMapper.selectListByControlledFileId(900L)).thenReturn(List.of(
                DccControlledFileRouteSnapshotDO.builder().id(1L).controlledFileId(900L).stageNo(1).resolvedUserIds("200").build()));

        DccControlledFileRespVO respVO = workflowService.getControlledFile(900L);

        assertEquals(900L, respVO.getId());
        assertEquals(LocalDate.of(2026, 5, 30), respVO.getEffectiveDate());
        assertEquals("pre-submit review", respVO.getRemark());
        assertNotNull(respVO.getRouteSnapshots());
        assertEquals(1, respVO.getRouteSnapshots().size());
    }

    @Test
    void getControlledFilePage_success() {
        DccControlledFilePageReqVO reqVO = new DccControlledFilePageReqVO();
        reqVO.setRequesterId(99L);
        reqVO.setDirectoryId(20L);
        PageResult<DccControlledFileDO> pageResult = new PageResult<>(List.of(
                DccControlledFileDO.builder()
                        .id(1L)
                        .directoryId(20L)
                        .title("A")
                        .status(DccControlledFileStatusEnum.PENDING_DOC_CONTROL_REVIEW.getStatus())
                        .effectiveDate(LocalDate.of(2026, 6, 1))
                        .remark("directory filter")
                        .build()), 1L);
        when(controlledFileMapper.selectWorkflowPage(reqVO)).thenReturn(pageResult);

        PageResult<DccControlledFileRespVO> actual = workflowService.getControlledFilePage(99L, reqVO);

        assertEquals(1L, actual.getTotal());
        assertEquals(1, actual.getList().size());
        assertEquals("A", actual.getList().get(0).getTitle());
        assertEquals(20L, actual.getList().get(0).getDirectoryId());
        assertEquals(LocalDate.of(2026, 6, 1), actual.getList().get(0).getEffectiveDate());
        assertEquals("directory filter", actual.getList().get(0).getRemark());
        verify(controlledFileMapper).selectWorkflowPage(reqVO);
    }

    @Test
    void getControlledFilePage_browserUnauthorizedDirectory_returnsEmpty() {
        DccControlledFilePageReqVO reqVO = new DccControlledFilePageReqVO();
        reqVO.setDirectoryId(30L);
        when(directoryAccessPermissionService.hasDirectoryManagementPermission(99L)).thenReturn(false);
        when(directoryAccessPermissionService.getAuthorizedDirectoryIds(99L, DccAccessTypeEnum.QUERY))
                .thenReturn(java.util.Set.of(20L));

        PageResult<DccControlledFileRespVO> actual = workflowService.getControlledFilePage(99L, reqVO);

        assertEquals(0L, actual.getTotal());
        assertEquals(0, actual.getList().size());
        verify(controlledFileMapper, never()).selectWorkflowPage(reqVO, java.util.Set.of(20L));
    }

    @Test
    void getControlledFilePage_browserAuthorizedDirectories_filtersByPermission() {
        DccControlledFilePageReqVO reqVO = new DccControlledFilePageReqVO();
        java.util.Set<Long> visibleDirectoryIds = java.util.Set.of(20L, 21L);
        PageResult<DccControlledFileDO> pageResult = new PageResult<>(List.of(
                DccControlledFileDO.builder().id(2L).directoryId(20L).title("B").status(DccControlledFileStatusEnum.ACTIVE.getStatus()).build()
        ), 1L);
        when(directoryAccessPermissionService.hasDirectoryManagementPermission(99L)).thenReturn(false);
        when(directoryAccessPermissionService.getAuthorizedDirectoryIds(99L, DccAccessTypeEnum.QUERY))
                .thenReturn(visibleDirectoryIds);
        when(controlledFileMapper.selectWorkflowPage(reqVO, visibleDirectoryIds)).thenReturn(pageResult);

        PageResult<DccControlledFileRespVO> actual = workflowService.getControlledFilePage(99L, reqVO);

        assertEquals(1L, actual.getTotal());
        assertEquals("B", actual.getList().get(0).getTitle());
        verify(controlledFileMapper).selectWorkflowPage(reqVO, visibleDirectoryIds);
    }

    @Test
    void getControlledFile_notExists_throws() {
        when(controlledFileMapper.selectById(900L)).thenReturn(null);

        assertServiceException(() -> workflowService.getControlledFile(900L), CONTROLLED_FILE_NOT_EXISTS);
    }

    @Test
    void getControlledFilePage_returnsSameListWhenEmpty() {
        DccControlledFilePageReqVO reqVO = new DccControlledFilePageReqVO();
        java.util.Set<Long> visibleDirectoryIds = java.util.Set.of(20L);
        PageResult<DccControlledFileDO> pageResult = PageResult.empty(0L);
        when(directoryAccessPermissionService.hasDirectoryManagementPermission(99L)).thenReturn(false);
        when(directoryAccessPermissionService.getAuthorizedDirectoryIds(99L, DccAccessTypeEnum.QUERY))
                .thenReturn(visibleDirectoryIds);
        when(controlledFileMapper.selectWorkflowPage(reqVO, visibleDirectoryIds)).thenReturn(pageResult);

        PageResult<DccControlledFileRespVO> actual = workflowService.getControlledFilePage(99L, reqVO);

        assertEquals(0L, actual.getTotal());
        assertEquals(0, actual.getList().size());
    }

    @Test
    void approveTask_partialSameLayer_keepsCurrentStagePending() {
        when(controlledFileMapper.selectById(900L)).thenReturn(DccControlledFileDO.builder()
                .id(900L)
                .masterId(700L)
                .categoryId(10L)
                .directoryId(21L)
                .fileName("SOP-001")
                .fileNumber("SOP-001")
                .processDefinitionKey(DccControlledFileWorkflowServiceImpl.BPM_PROCESS_DEFINITION_KEY)
                .processInstanceId("proc-1")
                .status(DccControlledFileStatusEnum.PENDING_MATRIX_REVIEW.getStatus())
                .build());
        when(routeSnapshotMapper.selectListByControlledFileId(900L)).thenReturn(List.of(
                DccControlledFileRouteSnapshotDO.builder()
                        .id(1L)
                        .controlledFileId(900L)
                        .stageNo(2)
                        .stageCode(DccControlledFileStageCodeEnum.MATRIX_REVIEW.getCode())
                        .requireAllApprovals(Boolean.TRUE)
                        .resolvedUserIds("99,100")
                        .build()));
        Task currentTask = mockTask("task-1", "proc-1", DccControlledFileStageCodeEnum.MATRIX_REVIEW.getCode());
        Task remainingTask = mockTask("task-2", "proc-1", DccControlledFileStageCodeEnum.MATRIX_REVIEW.getCode());
        when(bpmTaskService.validateTask(99L, "task-1")).thenReturn(currentTask);
        when(bpmTaskService.getRunningTaskListByProcessInstanceId("proc-1", null, null)).thenReturn(List.of(remainingTask));
        DccControlledFileApproveTaskReqVO reqVO = new DccControlledFileApproveTaskReqVO();
        reqVO.setTaskId("task-1");
        reqVO.setPassword("secret");
        reqVO.setReason("approved");
        mockActionSignature(900L, "task-1", 99L, "APPROVE",
                actionSignature(1001L, "task-1", "APPROVE", "APPROVED", "MATRIX_REVIEW_APPROVE"));

        DccSignatureActionRespVO result = workflowService.approveTask(99L, 900L, reqVO);

        assertEquals(1001L, result.getSignatureId());
        assertEquals("APPROVED", result.getTaskActionResult());
        assertEquals("PENDING_MATRIX_REVIEW", result.getNextStatus());
        verify(signatureVerificationService).verifyPasswordAndCreateSignature(99L, 900L, "task-1",
                DccControlledFileStageCodeEnum.MATRIX_REVIEW.getCode(), "APPROVE", "secret", "approved");
        verify(bpmTaskService).approveTask(eq(99L), any(BpmTaskApproveReqVO.class));
        verify(controlledFileMapper, never()).updateById(any(DccControlledFileDO.class));
    }

    @Test
    void approveTask_matrixReviewRequiresReviewPermission() {
        when(controlledFileMapper.selectById(900L)).thenReturn(DccControlledFileDO.builder()
                .id(900L)
                .masterId(700L)
                .categoryId(10L)
                .directoryId(21L)
                .fileName("SOP-001")
                .fileNumber("SOP-001")
                .processDefinitionKey(DccControlledFileWorkflowServiceImpl.BPM_PROCESS_DEFINITION_KEY)
                .processInstanceId("proc-1")
                .status(DccControlledFileStatusEnum.PENDING_MATRIX_REVIEW.getStatus())
                .build());
        when(routeSnapshotMapper.selectListByControlledFileId(900L)).thenReturn(List.of(
                DccControlledFileRouteSnapshotDO.builder()
                        .id(1L)
                        .controlledFileId(900L)
                        .stageNo(2)
                        .stageCode(DccControlledFileStageCodeEnum.MATRIX_REVIEW.getCode())
                        .stageOrder(2)
                        .requireAllApprovals(Boolean.TRUE)
                        .resolvedUserIds("99")
                        .build()));
        Task currentTask = mockTask("task-1", "proc-1", DccControlledFileStageCodeEnum.MATRIX_REVIEW.getCode());
        when(bpmTaskService.validateTask(99L, "task-1")).thenReturn(currentTask);
        when(permissionApi.hasAnyPermissions(99L, "dcc:controlled-file:review")).thenReturn(false);
        DccControlledFileApproveTaskReqVO reqVO = new DccControlledFileApproveTaskReqVO();
        reqVO.setTaskId("task-1");
        reqVO.setPassword("secret");
        reqVO.setReason("approved");

        assertServiceException(() -> workflowService.approveTask(99L, 900L, reqVO),
                CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED);

        verify(signatureVerificationService, never()).verifyPasswordAndCreateSignature(any(), any(), any(), any(), any(), any(), any());
        verify(bpmTaskService, never()).approveTask(eq(99L), any(BpmTaskApproveReqVO.class));
    }

    @Test
    void approveTask_matrixApprovalRequiresApprovePermission() {
        when(controlledFileMapper.selectById(900L)).thenReturn(DccControlledFileDO.builder()
                .id(900L)
                .masterId(700L)
                .categoryId(10L)
                .directoryId(21L)
                .fileName("SOP-001")
                .fileNumber("SOP-001")
                .processDefinitionKey(DccControlledFileWorkflowServiceImpl.BPM_PROCESS_DEFINITION_KEY)
                .processInstanceId("proc-1")
                .status(DccControlledFileStatusEnum.PENDING_MATRIX_APPROVAL.getStatus())
                .build());
        when(routeSnapshotMapper.selectListByControlledFileId(900L)).thenReturn(List.of(
                DccControlledFileRouteSnapshotDO.builder()
                        .id(1L)
                        .controlledFileId(900L)
                        .stageNo(3)
                        .stageCode(DccControlledFileStageCodeEnum.MATRIX_APPROVAL.getCode())
                        .stageOrder(3)
                        .requireAllApprovals(Boolean.FALSE)
                        .resolvedUserIds("99")
                        .build()));
        Task currentTask = mockTask("task-3", "proc-1", DccControlledFileStageCodeEnum.MATRIX_APPROVAL.getCode());
        when(bpmTaskService.validateTask(99L, "task-3")).thenReturn(currentTask);
        when(permissionApi.hasAnyPermissions(99L, "dcc:controlled-file:approve")).thenReturn(false);
        DccControlledFileApproveTaskReqVO reqVO = new DccControlledFileApproveTaskReqVO();
        reqVO.setTaskId("task-3");
        reqVO.setPassword("secret");
        reqVO.setReason("approved");

        assertServiceException(() -> workflowService.approveTask(99L, 900L, reqVO),
                CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED);

        verify(signatureVerificationService, never()).verifyPasswordAndCreateSignature(any(), any(), any(), any(), any(), any(), any());
        verify(bpmTaskService, never()).approveTask(eq(99L), any(BpmTaskApproveReqVO.class));
    }

    @Test
    void approveTask_matrixApprovalPartialSameLayer_keepsCurrentStagePending() {
        when(controlledFileMapper.selectById(900L)).thenReturn(DccControlledFileDO.builder()
                .id(900L)
                .masterId(700L)
                .categoryId(10L)
                .directoryId(21L)
                .fileName("SOP-001")
                .fileNumber("SOP-001")
                .processDefinitionKey(DccControlledFileWorkflowServiceImpl.BPM_PROCESS_DEFINITION_KEY)
                .processInstanceId("proc-1")
                .status(DccControlledFileStatusEnum.PENDING_MATRIX_APPROVAL.getStatus())
                .build());
        when(routeSnapshotMapper.selectListByControlledFileId(900L)).thenReturn(List.of(
                DccControlledFileRouteSnapshotDO.builder()
                        .id(1L)
                        .controlledFileId(900L)
                        .stageNo(3)
                        .stageCode(DccControlledFileStageCodeEnum.MATRIX_APPROVAL.getCode())
                        .stageOrder(3)
                        .requireAllApprovals(Boolean.TRUE)
                        .resolvedUserIds("99,100")
                        .build(),
                DccControlledFileRouteSnapshotDO.builder()
                        .id(2L)
                        .controlledFileId(900L)
                        .stageNo(4)
                        .stageCode(DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL.getCode())
                        .stageOrder(4)
                        .requireAllApprovals(Boolean.FALSE)
                        .resolvedUserIds("101")
                        .build()));
        Task currentTask = mockTask("task-3", "proc-1", DccControlledFileStageCodeEnum.MATRIX_APPROVAL.getCode());
        Task remainingTask = mockTask("task-4", "proc-1", DccControlledFileStageCodeEnum.MATRIX_APPROVAL.getCode());
        when(bpmTaskService.validateTask(99L, "task-3")).thenReturn(currentTask);
        when(bpmTaskService.getRunningTaskListByProcessInstanceId("proc-1", null, null)).thenReturn(List.of(remainingTask));
        DccControlledFileApproveTaskReqVO reqVO = new DccControlledFileApproveTaskReqVO();
        reqVO.setTaskId("task-3");
        reqVO.setPassword("secret");
        reqVO.setReason("approved");
        mockActionSignature(900L, "task-3", 99L, "APPROVE",
                actionSignature(1003L, "task-3", "APPROVE", "APPROVED", "MATRIX_APPROVAL_APPROVE"));

        DccSignatureActionRespVO result = workflowService.approveTask(99L, 900L, reqVO);

        assertEquals(1003L, result.getSignatureId());
        assertEquals("PENDING_MATRIX_APPROVAL", result.getNextStatus());
        verify(signatureVerificationService).verifyPasswordAndCreateSignature(99L, 900L, "task-3",
                DccControlledFileStageCodeEnum.MATRIX_APPROVAL.getCode(), "APPROVE", "secret", "approved");
        verify(bpmTaskService).approveTask(eq(99L), any(BpmTaskApproveReqVO.class));
        verify(controlledFileMapper, never()).updateById(any(DccControlledFileDO.class));
    }

    @Test
    void approveTask_matrixApprovalAnyOneAdvancesToDocControlApproval() {
        when(controlledFileMapper.selectById(900L)).thenReturn(DccControlledFileDO.builder()
                .id(900L)
                .categoryId(10L)
                .processDefinitionKey(DccControlledFileWorkflowServiceImpl.BPM_PROCESS_DEFINITION_KEY)
                .processInstanceId("proc-1")
                .status(DccControlledFileStatusEnum.PENDING_MATRIX_APPROVAL.getStatus())
                .build());
        when(routeSnapshotMapper.selectListByControlledFileId(900L)).thenReturn(List.of(
                DccControlledFileRouteSnapshotDO.builder()
                        .id(1L)
                        .controlledFileId(900L)
                        .stageNo(1)
                        .stageCode(DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode())
                        .stageOrder(1)
                        .candidateSourceIds("98")
                        .requireAllApprovals(Boolean.FALSE)
                        .resolvedUserIds("98")
                        .build(),
                DccControlledFileRouteSnapshotDO.builder()
                        .id(2L)
                        .controlledFileId(900L)
                        .stageNo(2)
                        .stageCode(DccControlledFileStageCodeEnum.MATRIX_REVIEW.getCode())
                        .stageOrder(2)
                        .candidateSourceIds("97,96")
                        .requireAllApprovals(Boolean.TRUE)
                        .resolvedUserIds("97,96")
                        .build(),
                DccControlledFileRouteSnapshotDO.builder()
                        .id(3L)
                        .controlledFileId(900L)
                        .stageNo(3)
                        .stageCode(DccControlledFileStageCodeEnum.MATRIX_APPROVAL.getCode())
                        .stageOrder(3)
                        .candidateSourceIds("99,100")
                        .requireAllApprovals(Boolean.FALSE)
                        .resolvedUserIds("99,100")
                        .build(),
                DccControlledFileRouteSnapshotDO.builder()
                        .id(4L)
                        .controlledFileId(900L)
                        .stageNo(4)
                        .stageCode(DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL.getCode())
                        .stageOrder(4)
                        .candidateSourceIds("101")
                        .requireAllApprovals(Boolean.FALSE)
                        .resolvedUserIds("101")
                        .build()));
        Task currentTask = mockTask("task-3", "proc-1", DccControlledFileStageCodeEnum.MATRIX_APPROVAL.getCode());
        Task nextStageTask = mockTask("task-4", "proc-1", DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL.getCode());
        when(bpmTaskService.validateTask(99L, "task-3")).thenReturn(currentTask);
        when(bpmTaskService.getRunningTaskListByProcessInstanceId("proc-1", null, null))
                .thenReturn(List.of(currentTask))
                .thenReturn(List.of(nextStageTask));
        DccControlledFileApproveTaskReqVO reqVO = new DccControlledFileApproveTaskReqVO();
        reqVO.setTaskId("task-3");
        reqVO.setPassword("secret");
        reqVO.setReason("approved");
        mockActionSignature(900L, "task-3", 99L, "APPROVE",
                actionSignature(1004L, "task-3", "APPROVE", "APPROVED", "MATRIX_APPROVAL_APPROVE"));

        DccSignatureActionRespVO result = workflowService.approveTask(99L, 900L, reqVO);

        assertEquals("PENDING_DOC_CONTROL_APPROVAL", result.getNextStatus());
        ArgumentCaptor<BpmTaskApproveReqVO> approveCaptor = ArgumentCaptor.forClass(BpmTaskApproveReqVO.class);
        verify(bpmTaskService).approveTask(eq(99L), approveCaptor.capture());
        assertEquals(List.of(101L),
                approveCaptor.getValue().getNextAssignees().get(DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL.getCode()));
        ArgumentCaptor<DccControlledFileDO> updateCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).updateById(updateCaptor.capture());
        assertEquals(DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL.getStatus(), updateCaptor.getValue().getStatus());
    }

    @Test
    void approveTask_matrixApprovalNeedTraining_continuesToDocControlWithoutTrainingWait() {
        when(controlledFileMapper.selectById(900L)).thenReturn(DccControlledFileDO.builder()
                .id(900L)
                .categoryId(10L)
                .requesterId(113L)
                .needTraining(Boolean.TRUE)
                .processDefinitionKey(DccControlledFileWorkflowServiceImpl.BPM_PROCESS_DEFINITION_KEY)
                .processInstanceId("proc-1")
                .status(DccControlledFileStatusEnum.PENDING_MATRIX_APPROVAL.getStatus())
                .build());
        when(routeSnapshotMapper.selectListByControlledFileId(900L)).thenReturn(List.of(
                DccControlledFileRouteSnapshotDO.builder()
                        .id(3L)
                        .controlledFileId(900L)
                        .stageNo(3)
                        .stageCode(DccControlledFileStageCodeEnum.MATRIX_APPROVAL.getCode())
                        .stageOrder(3)
                        .candidateSourceIds("99")
                        .requireAllApprovals(Boolean.FALSE)
                        .resolvedUserIds("99")
                        .build(),
                DccControlledFileRouteSnapshotDO.builder()
                        .id(4L)
                        .controlledFileId(900L)
                        .stageNo(4)
                        .stageCode(DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL.getCode())
                        .stageOrder(4)
                        .candidateSourceIds("101")
                        .requireAllApprovals(Boolean.FALSE)
                        .resolvedUserIds("101")
                        .build()));
        Task currentTask = mockTask("task-3", "proc-1", DccControlledFileStageCodeEnum.MATRIX_APPROVAL.getCode());
        Task nextStageTask = mockTask("task-4", "proc-1", DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL.getCode());
        when(bpmTaskService.validateTask(99L, "task-3")).thenReturn(currentTask);
        when(bpmTaskService.getRunningTaskListByProcessInstanceId("proc-1", null, null))
                .thenReturn(List.of(currentTask))
                .thenReturn(List.of(nextStageTask));
        DccControlledFileApproveTaskReqVO reqVO = new DccControlledFileApproveTaskReqVO();
        reqVO.setTaskId("task-3");
        reqVO.setPassword("secret");
        reqVO.setReason("approved");
        mockActionSignature(900L, "task-3", 99L, "APPROVE",
                actionSignature(1007L, "task-3", "APPROVE", "APPROVED", "MATRIX_APPROVAL_APPROVE"));

        workflowService.approveTask(99L, 900L, reqVO);

        ArgumentCaptor<DccControlledFileDO> updateCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).updateById(updateCaptor.capture());
        assertEquals(DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL.getStatus(), updateCaptor.getValue().getStatus());
        assertNull(updateCaptor.getValue().getTrainingRecordFileId());
    }

    @Test
    void uploadTrainingRecord_requesterMovesTrainingGateToDocControlApproval() {
        when(controlledFileMapper.selectById(901L)).thenReturn(DccControlledFileDO.builder()
                .id(901L)
                .categoryId(10L)
                .requesterId(113L)
                .needTraining(Boolean.TRUE)
                .status(DccControlledFileStatusEnum.PENDING_APPLICANT_TRAINING_RECORD.getStatus())
                .processInstanceId("proc-1")
                .processDefinitionKey(DccControlledFileWorkflowServiceImpl.BPM_PROCESS_DEFINITION_KEY)
                .build());
        when(uploadTicketService.resolveForBinding(new DccUploadTicketResolveCommand(
                "UT-TRAINING", 113L, 10L, "session-training", "TRAINING_RECORD")))
                .thenReturn(new DccUploadTicketBoundFile("UT-TRAINING", 810L,
                        "training-record.xlsx",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 128L));
        DccControlledFileTrainingRecordReqVO reqVO = new DccControlledFileTrainingRecordReqVO();
        reqVO.setSessionId("session-training");
        reqVO.setTrainingRecordUploadTicket("UT-TRAINING");

        workflowService.uploadTrainingRecord(113L, 901L, reqVO);

        ArgumentCaptor<DccControlledFileDO> updateCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).updateById(updateCaptor.capture());
        assertEquals(901L, updateCaptor.getValue().getId());
        assertEquals(810L, updateCaptor.getValue().getTrainingRecordFileId());
        assertEquals(DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL.getStatus(), updateCaptor.getValue().getStatus());
        verify(uploadTicketService).markBound(new DccUploadTicketMarkBoundCommand(
                "UT-TRAINING", 113L, 10L, "session-training", "TRAINING_RECORD", 901L));
    }

    @Test
    void uploadTrainingRecord_postFinalizationTrainingStatus_throwsAndDoesNotReuseTrainingInProgress() {
        when(controlledFileMapper.selectById(901L)).thenReturn(DccControlledFileDO.builder()
                .id(901L)
                .categoryId(10L)
                .requesterId(113L)
                .needTraining(Boolean.TRUE)
                .status(DccControlledFileStatusEnum.TRAINING_IN_PROGRESS.getStatus())
                .processInstanceId("proc-1")
                .processDefinitionKey(DccControlledFileWorkflowServiceImpl.BPM_PROCESS_DEFINITION_KEY)
                .build());
        DccControlledFileTrainingRecordReqVO reqVO = new DccControlledFileTrainingRecordReqVO();
        reqVO.setSessionId("session-training");
        reqVO.setTrainingRecordUploadTicket("UT-TRAINING");

        assertServiceException(() -> workflowService.uploadTrainingRecord(113L, 901L, reqVO),
                CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED);

        verify(controlledFileMapper, never()).updateById(any(DccControlledFileDO.class));
    }

    @Test
    void approveTask_bpmSkipsConfiguredNextStage_rejectsTransition() {
        when(controlledFileMapper.selectById(900L)).thenReturn(DccControlledFileDO.builder()
                .id(900L)
                .categoryId(10L)
                .processDefinitionKey(DccControlledFileWorkflowServiceImpl.BPM_PROCESS_DEFINITION_KEY)
                .processInstanceId("proc-1")
                .status(DccControlledFileStatusEnum.PENDING_DOC_CONTROL_REVIEW.getStatus())
                .build());
        when(routeSnapshotMapper.selectListByControlledFileId(900L)).thenReturn(List.of(
                DccControlledFileRouteSnapshotDO.builder()
                        .id(1L)
                        .controlledFileId(900L)
                        .stageNo(1)
                        .stageCode(DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode())
                        .stageOrder(1)
                        .requireAllApprovals(Boolean.FALSE)
                        .resolvedUserIds("99")
                        .build(),
                DccControlledFileRouteSnapshotDO.builder()
                        .id(2L)
                        .controlledFileId(900L)
                        .stageNo(2)
                        .stageCode(DccControlledFileStageCodeEnum.MATRIX_REVIEW.getCode())
                        .stageOrder(2)
                        .requireAllApprovals(Boolean.TRUE)
                        .resolvedUserIds("100,101")
                        .build(),
                DccControlledFileRouteSnapshotDO.builder()
                        .id(3L)
                        .controlledFileId(900L)
                        .stageNo(3)
                        .stageCode(DccControlledFileStageCodeEnum.MATRIX_APPROVAL.getCode())
                        .stageOrder(3)
                        .requireAllApprovals(Boolean.TRUE)
                        .resolvedUserIds("102,103")
                        .build()));
        Task currentTask = mockTask("task-1", "proc-1", DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode());
        Task skippedStageTask = mockTask("task-2", "proc-1", DccControlledFileStageCodeEnum.MATRIX_APPROVAL.getCode());
        when(bpmTaskService.validateTask(99L, "task-1")).thenReturn(currentTask);
        when(bpmTaskService.getRunningTaskListByProcessInstanceId("proc-1", null, null))
                .thenReturn(List.of(currentTask))
                .thenReturn(List.of(skippedStageTask));
        DccControlledFileApproveTaskReqVO reqVO = new DccControlledFileApproveTaskReqVO();
        reqVO.setTaskId("task-1");
        reqVO.setPassword("secret");
        reqVO.setReason("approved");

        assertServiceException(() -> workflowService.approveTask(99L, 900L, reqVO),
                CONTROLLED_FILE_TASK_STAGE_UNSUPPORTED);
        verify(controlledFileMapper, never()).updateById(any(DccControlledFileDO.class));
    }

    @Test
    void approveTask_bpmFinishesBeforeLastConfiguredStage_rejectsTransition() {
        when(controlledFileMapper.selectById(900L)).thenReturn(DccControlledFileDO.builder()
                .id(900L)
                .categoryId(10L)
                .processDefinitionKey(DccControlledFileWorkflowServiceImpl.BPM_PROCESS_DEFINITION_KEY)
                .processInstanceId("proc-1")
                .status(DccControlledFileStatusEnum.PENDING_MATRIX_APPROVAL.getStatus())
                .build());
        when(routeSnapshotMapper.selectListByControlledFileId(900L)).thenReturn(List.of(
                DccControlledFileRouteSnapshotDO.builder()
                        .id(1L)
                        .controlledFileId(900L)
                        .stageNo(1)
                        .stageCode(DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode())
                        .stageOrder(1)
                        .requireAllApprovals(Boolean.FALSE)
                        .resolvedUserIds("98")
                        .build(),
                DccControlledFileRouteSnapshotDO.builder()
                        .id(2L)
                        .controlledFileId(900L)
                        .stageNo(2)
                        .stageCode(DccControlledFileStageCodeEnum.MATRIX_REVIEW.getCode())
                        .stageOrder(2)
                        .requireAllApprovals(Boolean.TRUE)
                        .resolvedUserIds("97,96")
                        .build(),
                DccControlledFileRouteSnapshotDO.builder()
                        .id(3L)
                        .controlledFileId(900L)
                        .stageNo(3)
                        .stageCode(DccControlledFileStageCodeEnum.MATRIX_APPROVAL.getCode())
                        .stageOrder(3)
                        .requireAllApprovals(Boolean.TRUE)
                        .resolvedUserIds("99,100")
                        .build(),
                DccControlledFileRouteSnapshotDO.builder()
                        .id(4L)
                        .controlledFileId(900L)
                        .stageNo(4)
                        .stageCode(DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL.getCode())
                        .stageOrder(4)
                        .requireAllApprovals(Boolean.FALSE)
                        .resolvedUserIds("101")
                        .build()));
        Task currentTask = mockTask("task-3", "proc-1", DccControlledFileStageCodeEnum.MATRIX_APPROVAL.getCode());
        when(bpmTaskService.validateTask(99L, "task-3")).thenReturn(currentTask);
        when(bpmTaskService.getRunningTaskListByProcessInstanceId("proc-1", null, null)).thenReturn(List.of());
        DccControlledFileApproveTaskReqVO reqVO = new DccControlledFileApproveTaskReqVO();
        reqVO.setTaskId("task-3");
        reqVO.setPassword("secret");
        reqVO.setReason("approved");

        assertServiceException(() -> workflowService.approveTask(99L, 900L, reqVO),
                CONTROLLED_FILE_TASK_STAGE_UNSUPPORTED);
        verify(controlledFileMapper, never()).updateById(any(DccControlledFileDO.class));
    }

    @Test
    void approveTask_actorOutsideResolvedStageUsers_rejectsAction() {
        when(controlledFileMapper.selectById(900L)).thenReturn(DccControlledFileDO.builder()
                .id(900L)
                .categoryId(10L)
                .processDefinitionKey(DccControlledFileWorkflowServiceImpl.BPM_PROCESS_DEFINITION_KEY)
                .processInstanceId("proc-1")
                .status(DccControlledFileStatusEnum.PENDING_DOC_CONTROL_REVIEW.getStatus())
                .build());
        when(routeSnapshotMapper.selectListByControlledFileId(900L)).thenReturn(List.of(
                DccControlledFileRouteSnapshotDO.builder()
                        .id(1L)
                        .controlledFileId(900L)
                        .stageNo(1)
                        .stageCode(DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode())
                        .requireAllApprovals(Boolean.FALSE)
                        .resolvedUserIds("100")
                        .build()));
        Task currentTask = mockTask("task-1", "proc-1", DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode());
        when(bpmTaskService.validateTask(99L, "task-1")).thenReturn(currentTask);

        DccControlledFileApproveTaskReqVO reqVO = new DccControlledFileApproveTaskReqVO();
        reqVO.setTaskId("task-1");
        reqVO.setPassword("secret");

        assertServiceException(() -> workflowService.approveTask(99L, 900L, reqVO),
                CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED);
        verify(signatureVerificationService, never()).verifyPasswordAndCreateSignature(any(), any(), any(), any(), any(), any(), any());
        verify(bpmTaskService, never()).approveTask(eq(99L), any(BpmTaskApproveReqVO.class));
    }

    @Test
    void approveTask_genericApproveTaskKey_usesCurrentFileStatusAndAdvancesToNextStage() {
        when(controlledFileMapper.selectById(900L)).thenReturn(DccControlledFileDO.builder()
                .id(900L)
                .categoryId(10L)
                .processDefinitionKey(DccControlledFileWorkflowServiceImpl.BPM_PROCESS_DEFINITION_KEY)
                .processInstanceId("proc-1")
                .status(DccControlledFileStatusEnum.PENDING_DOC_CONTROL_REVIEW.getStatus())
                .build());
        when(routeSnapshotMapper.selectListByControlledFileId(900L)).thenReturn(List.of(
                DccControlledFileRouteSnapshotDO.builder()
                        .id(1L)
                        .controlledFileId(900L)
                        .stageNo(1)
                        .stageCode(DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode())
                        .stageOrder(1)
                        .requireAllApprovals(Boolean.FALSE)
                        .resolvedUserIds("99")
                        .build(),
                DccControlledFileRouteSnapshotDO.builder()
                        .id(2L)
                        .controlledFileId(900L)
                        .stageNo(2)
                        .stageCode(DccControlledFileStageCodeEnum.MATRIX_REVIEW.getCode())
                        .stageOrder(2)
                        .requireAllApprovals(Boolean.TRUE)
                        .resolvedUserIds("100,101")
                        .build()));
        Task currentTask = mockTask("task-1", "proc-1", "approveTask");
        Task nextStageTask = mockTask("task-2", "proc-1", "approveTask");
        when(bpmTaskService.validateTask(99L, "task-1")).thenReturn(currentTask);
        when(bpmTaskService.getRunningTaskListByProcessInstanceId("proc-1", null, null))
                .thenReturn(List.of(currentTask))
                .thenReturn(List.of(nextStageTask));
        DccControlledFileApproveTaskReqVO reqVO = new DccControlledFileApproveTaskReqVO();
        reqVO.setTaskId("task-1");
        reqVO.setPassword("secret");
        reqVO.setReason("approved");
        mockActionSignature(900L, "task-1", 99L, "APPROVE",
                actionSignature(1005L, "task-1", "APPROVE", "APPROVED", "DOC_CONTROL_REVIEW_APPROVE"));

        DccSignatureActionRespVO result = workflowService.approveTask(99L, 900L, reqVO);

        assertEquals("PENDING_MATRIX_REVIEW", result.getNextStatus());
        verify(bpmTaskService).approveTask(eq(99L), any(BpmTaskApproveReqVO.class));
        ArgumentCaptor<DccControlledFileDO> updateCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).updateById(updateCaptor.capture());
        assertEquals(DccControlledFileStatusEnum.PENDING_MATRIX_REVIEW.getStatus(), updateCaptor.getValue().getStatus());
    }

    @Test
    void approveTask_genericApproveTaskKey_snapshotAssigneeDoesNotNeedCategoryReviewApproveRule() {
        when(controlledFileMapper.selectById(900L)).thenReturn(DccControlledFileDO.builder()
                .id(900L)
                .categoryId(10L)
                .processDefinitionKey(DccControlledFileWorkflowServiceImpl.BPM_PROCESS_DEFINITION_KEY)
                .processInstanceId("proc-1")
                .status(DccControlledFileStatusEnum.PENDING_DOC_CONTROL_REVIEW.getStatus())
                .build());
        when(routeSnapshotMapper.selectListByControlledFileId(900L)).thenReturn(List.of(
                DccControlledFileRouteSnapshotDO.builder()
                        .id(1L)
                        .controlledFileId(900L)
                        .stageNo(1)
                        .stageCode(DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode())
                        .stageOrder(1)
                        .requireAllApprovals(Boolean.FALSE)
                        .resolvedUserIds("99")
                        .build(),
                DccControlledFileRouteSnapshotDO.builder()
                        .id(2L)
                        .controlledFileId(900L)
                        .stageNo(2)
                        .stageCode(DccControlledFileStageCodeEnum.MATRIX_REVIEW.getCode())
                        .stageOrder(2)
                        .requireAllApprovals(Boolean.TRUE)
                        .resolvedUserIds("100,101")
                        .build()));
        Task currentTask = mockTask("task-1", "proc-1", "approveTask");
        Task nextStageTask = mockTask("task-2", "proc-1", "approveTask");
        when(bpmTaskService.validateTask(99L, "task-1")).thenReturn(currentTask);
        when(bpmTaskService.getRunningTaskListByProcessInstanceId("proc-1", null, null))
                .thenReturn(List.of(currentTask))
                .thenReturn(List.of(nextStageTask));
        DccControlledFileApproveTaskReqVO reqVO = new DccControlledFileApproveTaskReqVO();
        reqVO.setTaskId("task-1");
        reqVO.setPassword("secret");
        reqVO.setReason("approved");
        mockActionSignature(900L, "task-1", 99L, "APPROVE",
                actionSignature(1005L, "task-1", "APPROVE", "APPROVED", "DOC_CONTROL_REVIEW_APPROVE"));

        DccSignatureActionRespVO result = workflowService.approveTask(99L, 900L, reqVO);

        assertEquals("PENDING_MATRIX_REVIEW", result.getNextStatus());
        verify(bpmTaskService).approveTask(eq(99L), any(BpmTaskApproveReqVO.class));
    }

    @Test
    void approveTask_genericApproveTaskKey_finalStageDoesNotPersistFinalizingStatus() {
        when(controlledFileMapper.selectById(900L)).thenReturn(DccControlledFileDO.builder()
                .id(900L)
                .masterId(700L)
                .categoryId(10L)
                .directoryId(21L)
                .fileName("SOP-001")
                .fileNumber("SOP-001")
                .processDefinitionKey(DccControlledFileWorkflowServiceImpl.BPM_PROCESS_DEFINITION_KEY)
                .processInstanceId("proc-1")
                .status(DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL.getStatus())
                .build());
        when(routeSnapshotMapper.selectListByControlledFileId(900L)).thenReturn(List.of(
                DccControlledFileRouteSnapshotDO.builder()
                        .id(1L)
                        .controlledFileId(900L)
                        .stageNo(1)
                        .stageCode(DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode())
                        .stageOrder(1)
                        .requireAllApprovals(Boolean.FALSE)
                        .resolvedUserIds("98")
                        .build(),
                DccControlledFileRouteSnapshotDO.builder()
                        .id(2L)
                        .controlledFileId(900L)
                        .stageNo(2)
                        .stageCode(DccControlledFileStageCodeEnum.MATRIX_REVIEW.getCode())
                        .stageOrder(2)
                        .requireAllApprovals(Boolean.TRUE)
                        .resolvedUserIds("97,96")
                        .build(),
                DccControlledFileRouteSnapshotDO.builder()
                        .id(3L)
                        .controlledFileId(900L)
                        .stageNo(3)
                        .stageCode(DccControlledFileStageCodeEnum.MATRIX_APPROVAL.getCode())
                        .stageOrder(3)
                        .requireAllApprovals(Boolean.TRUE)
                        .resolvedUserIds("95,94")
                        .build(),
                DccControlledFileRouteSnapshotDO.builder()
                        .id(4L)
                        .controlledFileId(900L)
                        .stageNo(4)
                        .stageCode(DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL.getCode())
                        .stageOrder(4)
                        .requireAllApprovals(Boolean.FALSE)
                        .resolvedUserIds("99")
                        .build()));
        Task currentTask = mockTask("task-4", "proc-1", "approveTask");
        when(bpmTaskService.validateTask(99L, "task-4")).thenReturn(currentTask);
        when(bpmTaskService.getRunningTaskListByProcessInstanceId("proc-1", null, null))
                .thenReturn(List.of(currentTask))
                .thenReturn(List.of());
        DccControlledFileApproveTaskReqVO reqVO = new DccControlledFileApproveTaskReqVO();
        reqVO.setTaskId("task-4");
        reqVO.setPassword("secret");
        reqVO.setReason("approved");
        useStampedPdfTicket(reqVO);
        useSingleDistributionDepartment(reqVO);
        useConfirmedDirectory(reqVO);
        mockActionSignature(900L, "task-4", 99L, "APPROVE",
                actionSignature(1006L, "task-4", "APPROVE", "APPROVED", "DOC_CONTROL_APPROVAL_APPROVE"));

        DccSignatureActionRespVO result = workflowService.approveTask(99L, 900L, reqVO);

        assertEquals(DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL.getStatus(), result.getNextStatus());
        verify(bpmTaskService).approveTask(eq(99L), any(BpmTaskApproveReqVO.class));
        ArgumentCaptor<DccControlledFileDO> updateCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).updateById(updateCaptor.capture());
        assertEquals(800L, updateCaptor.getValue().getPublishedFileId());
        assertEquals(800L, updateCaptor.getValue().getStampedFileId());
        assertNull(updateCaptor.getValue().getStatus());
    }

    @Test
    void approveTask_docControlApprovalRequiresStampedPdf() {
        mockTaskActionContext(900L, 99L, "task-4", "approveTask",
                DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL,
                DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL, Boolean.FALSE);
        DccControlledFileApproveTaskReqVO reqVO = new DccControlledFileApproveTaskReqVO();
        reqVO.setTaskId("task-4");
        reqVO.setPassword("secret");
        reqVO.setReason("approved");
        useSingleDistributionDepartment(reqVO);
        useConfirmedDirectory(reqVO);

        assertFinalApprovalNotReady(() -> workflowService.approveTask(99L, 900L, reqVO), "盖章 PDF");

        verify(signatureVerificationService, never()).verifyPasswordAndCreateSignature(any(), any(), any(), any(), any(), any(), any());
        verify(bpmTaskService, never()).approveTask(eq(99L), any(BpmTaskApproveReqVO.class));
    }

    @Test
    void approveTask_docControlApprovalRequiresDistributionDepartments_newOrdinaryPolicy() {
        assertFinalApprovalIgnoresRetiredInputs(false, null, null);
    }

    @Test
    void approveTask_docControlApprovalRequiresTrainingRecordWhenNeedTraining_newOrdinaryPolicy() {
        assertFinalApprovalIgnoresRetiredInputs(true, null, null);
    }

    @Test
    void approveTask_docControlApprovalPersistsManualStampedPdfAfterTrainingRecordGate() {
        Task currentTask = mockTaskActionContext(900L, 99L, "task-4", "approveTask",
                DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL,
                DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL, Boolean.TRUE);
        when(controlledFileMapper.selectById(900L)).thenReturn(DccControlledFileDO.builder()
                .id(900L)
                .masterId(700L)
                .categoryId(10L)
                .directoryId(21L)
                .fileName("SOP-001")
                .fileNumber("SOP-001")
                .trainingRecordFileId(801L)
                .processDefinitionKey(DccControlledFileWorkflowServiceImpl.BPM_PROCESS_DEFINITION_KEY)
                .processInstanceId("proc-1")
                .status(DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL.getStatus())
                .needTraining(Boolean.TRUE)
                .build());
        when(bpmTaskService.getRunningTaskListByProcessInstanceId("proc-1", null, null))
                .thenReturn(List.of(currentTask))
                .thenReturn(List.of());
        DccControlledFileApproveTaskReqVO reqVO = new DccControlledFileApproveTaskReqVO();
        reqVO.setTaskId("task-4");
        reqVO.setPassword("secret");
        reqVO.setReason("approved");
        useStampedPdfTicket(reqVO);
        useSingleDistributionDepartment(reqVO);
        useConfirmedDirectory(reqVO);
        mockActionSignature(900L, "task-4", 99L, "APPROVE",
                actionSignature(1008L, "task-4", "APPROVE", "APPROVED", "DOC_CONTROL_APPROVAL_APPROVE"));

        workflowService.approveTask(99L, 900L, reqVO);

        ArgumentCaptor<DccControlledFileDO> updateCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).updateById(updateCaptor.capture());
        assertEquals(800L, updateCaptor.getValue().getPublishedFileId());
        assertEquals(800L, updateCaptor.getValue().getStampedFileId());
        assertNull(updateCaptor.getValue().getTrainingRecordFileId());
        assertNotNull(updateCaptor.getValue().getStampedTime());
        verify(signatureVerificationService).verifyPasswordAndCreateSignature(99L, 900L, "task-4",
                DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL.getCode(), "APPROVE", "secret", "approved");
        verify(signatureBindingService).bindPublishedCopy(any(DccControlledFileDO.class), eq(800L), eq(99L),
                eq("dcc-final-approval:900:task-4"));
        verify(bpmTaskService).approveTask(eq(99L), any(BpmTaskApproveReqVO.class));
        verify(uploadTicketService).markBound(new DccUploadTicketMarkBoundCommand(
                "UT-STAMPED", 99L, 10L, "dcc-approval:900:6:task-4:session-stamped", "APPROVAL_PDF", 900L));
    }

    @Test
    void approveTask_docControlApprovalRejectsTrainingRecordPayloadEvenWhenPersisted_newOrdinaryPolicy() {
        assertFinalApprovalIgnoresRetiredInputs(true, List.of(), 801L);
    }

    @Test
    void approveTask_docControlApprovalPersistsStampedPdfAndSingleFileElectronicDepartments_newOrdinaryPolicy() {
        assertFinalApprovalIgnoresRetiredInputs(false, List.of(distributionScope(300L, "PUBLIC_FOLDER")), null);
    }

    @Test
    void approveTask_docControlApprovalPersistsSelectedDistributionDepartments_newOrdinaryPolicy() {
        assertFinalApprovalIgnoresRetiredInputs(false, List.of(distributionScope(301L, "PAPER")), null);
    }

    @Test
    void approveTask_docControlApprovalRequiresConfirmedDirectory() {
        mockTaskActionContext(900L, 99L, "task-4", "approveTask",
                DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL,
                DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL, Boolean.FALSE);
        DccControlledFileApproveTaskReqVO reqVO = new DccControlledFileApproveTaskReqVO();
        reqVO.setTaskId("task-4");
        reqVO.setPassword("secret");
        reqVO.setReason("approved");
        useStampedPdfTicket(reqVO);
        reqVO.setSelectedDistributionScopes(List.of(
                distributionScope(300L, DccDistributionMediumEnum.PUBLIC_FOLDER.getCode())));

        assertFinalApprovalNotReady(() -> workflowService.approveTask(99L, 900L, reqVO), "默认目录");

        verify(signatureVerificationService, never()).verifyPasswordAndCreateSignature(any(), any(), any(), any(), any(), any(), any());
        verify(bpmTaskService, never()).approveTask(eq(99L), any(BpmTaskApproveReqVO.class));
    }

    @Test
    void approveTask_docControlApprovalPersistsConfirmedDirectoryOnFileAndMaster() {
        Task currentTask = mockTaskActionContext(900L, 99L, "task-4", "approveTask",
                DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL,
                DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL, Boolean.FALSE);
        when(bpmTaskService.getRunningTaskListByProcessInstanceId("proc-1", null, null))
                .thenReturn(List.of(currentTask))
                .thenReturn(List.of());
        DccControlledFileApproveTaskReqVO reqVO = new DccControlledFileApproveTaskReqVO();
        reqVO.setTaskId("task-4");
        reqVO.setPassword("secret");
        reqVO.setReason("approved");
        useStampedPdfTicket(reqVO);
        useSingleDistributionDepartment(reqVO);
        useConfirmedDirectory(reqVO, 22L);
        mockActionSignature(900L, "task-4", 99L, "APPROVE",
                actionSignature(1011L, "task-4", "APPROVE", "APPROVED", "DOC_CONTROL_APPROVAL_APPROVE"));

        workflowService.approveTask(99L, 900L, reqVO);

        ArgumentCaptor<DccControlledFileDO> fileCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).updateById(fileCaptor.capture());
        assertEquals(20L, fileCaptor.getValue().getDirectoryId());
        ArgumentCaptor<DccControlledFileMasterDO> masterCaptor = ArgumentCaptor.forClass(DccControlledFileMasterDO.class);
        verify(controlledFileMasterMapper).updateById(masterCaptor.capture());
        assertEquals(700L, masterCaptor.getValue().getId());
        assertEquals(20L, masterCaptor.getValue().getDirectoryId());
    }

    @Test
    void approveTask_docControlApprovalPersistsMixedDistributionScopes_newOrdinaryPolicy() {
        assertFinalApprovalIgnoresRetiredInputs(false, List.of(distributionScope(300L, "PUBLIC_FOLDER"), distributionScope(301L, "PAPER")), null);
    }

    @Test
    void approveTask_docControlApprovalRejectsInvalidDistributionMedium_newOrdinaryPolicy() {
        assertFinalApprovalIgnoresRetiredInputs(false, List.of(distributionScope(300L, "INVALID")), null);
    }

    @Test
    void returnTask_updatesStatusToTargetStageAndDelegatesToBpm_removedByOrdinaryFilePolicy() {
        assertServiceException(() -> workflowService.returnTask(99L, 900L, new DccControlledFileReturnTaskReqVO()),
                CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED);
        org.mockito.Mockito.verifyNoInteractions(controlledFileMapper, routeSnapshotMapper,
                signatureVerificationService, bpmTaskService);
    }

    @Test
    void returnTask_toApplicantReworkKeepsOriginalProcessInstance_removedByOrdinaryFilePolicy() {
        assertServiceException(() -> workflowService.returnTask(99L, 900L, new DccControlledFileReturnTaskReqVO()),
                CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED);
        org.mockito.Mockito.verifyNoInteractions(controlledFileMapper, routeSnapshotMapper,
                signatureVerificationService, bpmTaskService);
    }

    @Test
    void approveTask_applicantReworkContinuesOriginalProcessInstanceToFirstApprovalStage() {
        DccControlledFileDO file = DccControlledFileDO.builder()
                .id(900L)
                .requesterId(77L)
                .masterId(700L)
                .categoryId(10L)
                .directoryId(21L)
                .fileName("SOP-001")
                .fileNumber("SOP-001")
                .processDefinitionKey(DccControlledFileWorkflowServiceImpl.BPM_PROCESS_DEFINITION_KEY)
                .processInstanceId("proc-1")
                .status("PENDING_APPLICANT_REWORK")
                .needTraining(Boolean.FALSE)
                .build();
        when(controlledFileMapper.selectById(900L)).thenReturn(file);
        DccControlledFileRouteSnapshotDO docControlSnapshot = DccControlledFileRouteSnapshotDO.builder()
                .id(8L)
                .controlledFileId(900L)
                .stageNo(1)
                .stageCode(DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode())
                .stageOrder(1)
                .resolvedUserIds("101")
                .build();
        when(routeSnapshotMapper.selectListByControlledFileId(900L)).thenReturn(List.of(docControlSnapshot));
        Task applicantTask = mockTask("task-applicant", "proc-1", "APPLICANT_REWORK");
        Task docControlTask = mockTask("task-doc-control", "proc-1", DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode());
        when(bpmTaskService.validateTask(77L, "task-applicant")).thenReturn(applicantTask);
        when(bpmTaskService.getRunningTaskListByProcessInstanceId("proc-1", null, null))
                .thenReturn(List.of(applicantTask), List.of(docControlTask));
        mockActionSignature(900L, "task-applicant", 77L, "APPROVE",
                actionSignature(1007L, "task-applicant", "APPROVE", "APPROVED", "APPLICANT_REWORK_APPROVE"));
        DccControlledFileApproveTaskReqVO reqVO = new DccControlledFileApproveTaskReqVO();
        reqVO.setTaskId("task-applicant");
        reqVO.setPassword("secret");
        reqVO.setReason("已补充说明，继续原流程");

        DccSignatureActionRespVO result = workflowService.approveTask(77L, 900L, reqVO);

        verify(signatureVerificationService).verifyPasswordAndCreateSignature(77L, 900L, "task-applicant",
                "APPLICANT_REWORK", "APPROVE", "secret", "已补充说明，继续原流程");
        ArgumentCaptor<BpmTaskApproveReqVO> bpmReqCaptor = ArgumentCaptor.forClass(BpmTaskApproveReqVO.class);
        verify(bpmTaskService).approveTask(eq(77L), bpmReqCaptor.capture());
        assertEquals("task-applicant", bpmReqCaptor.getValue().getId());
        assertEquals(List.of(101L), bpmReqCaptor.getValue().getNextAssignees().get(DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode()));
        ArgumentCaptor<DccControlledFileDO> updateCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).updateById(updateCaptor.capture());
        assertEquals(DccControlledFileStatusEnum.PENDING_DOC_CONTROL_REVIEW.getStatus(), updateCaptor.getValue().getStatus());
        assertEquals(DccControlledFileStatusEnum.PENDING_DOC_CONTROL_REVIEW.getStatus(), result.getNextStatus());
        verify(bpmProcessInstanceApi, never()).createProcessInstance(any(), any(BpmProcessInstanceCreateReqDTO.class));
    }

    @Test
    void rejectTask_applicantReworkIsNotAllowed() {
        DccControlledFileDO file = DccControlledFileDO.builder()
                .id(900L)
                .requesterId(77L)
                .processDefinitionKey(DccControlledFileWorkflowServiceImpl.BPM_PROCESS_DEFINITION_KEY)
                .processInstanceId("proc-1")
                .status(DccControlledFileStatusEnum.PENDING_APPLICANT_REWORK.getStatus())
                .build();
        when(controlledFileMapper.selectById(900L)).thenReturn(file);
        Task applicantTask = mockTask("task-applicant", "proc-1", "APPLICANT_REWORK");
        when(bpmTaskService.validateTask(77L, "task-applicant")).thenReturn(applicantTask);
        DccControlledFileRejectTaskReqVO reqVO = new DccControlledFileRejectTaskReqVO();
        reqVO.setTaskId("task-applicant");
        reqVO.setPassword("secret");
        reqVO.setReason("不应在申请人回退节点驳回");

        assertServiceException(() -> workflowService.rejectTask(77L, 900L, reqVO),
                CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED);

        verify(signatureVerificationService, never()).verifyPasswordAndCreateSignature(any(), any(), any(), any(), any(), any(), any());
        verify(bpmTaskService, never()).rejectTask(eq(77L), any(BpmTaskRejectReqVO.class));
        verify(controlledFileMapper, never()).updateById(any(DccControlledFileDO.class));
    }

    @Test
    void transferTask_updatesStageResolvedUsersAndDelegatesToBpm_removedByOrdinaryFilePolicy() {
        assertServiceException(() -> workflowService.transferTask(99L, 900L, new DccControlledFileTransferTaskReqVO()),
                CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED);
        org.mockito.Mockito.verifyNoInteractions(controlledFileMapper, routeSnapshotMapper,
                signatureVerificationService, bpmTaskService);
    }

    @Test
    void createSignTask_appendsResolvedUsersAndDelegatesToBpm_removedByOrdinaryFilePolicy() {
        assertServiceException(() -> workflowService.createSignTask(99L, 900L, new DccControlledFileCreateSignTaskReqVO()),
                CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED);
        org.mockito.Mockito.verifyNoInteractions(controlledFileMapper, routeSnapshotMapper,
                signatureVerificationService, bpmTaskService);
    }

    @Test
    void getTaskActionReadiness_finalApprovalAggregatesAllMissingRequirements() {
        mockTaskActionContext(900L, 99L, "task-4", "approveTask",
                DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL,
                DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL, Boolean.TRUE);
        DccControlledFileTaskReadinessReqVO reqVO = new DccControlledFileTaskReadinessReqVO();
        reqVO.setTaskId("task-4");

        DccControlledFileTaskReadinessRespVO result =
                workflowService.getTaskActionReadiness(99L, 900L, reqVO);

        assertFalse(result.getReady());
        assertTrue(result.getFinalApproval());
        assertEquals(List.of(
                        "STAMPED_PDF_REQUIRED",
                        "DEFAULT_DIRECTORY_INVALID"),
                result.getBlockers().stream().map(blocker -> blocker.getReasonCode()).toList());
    }

    @Test
    void approveTask_finalApprovalMultipleMissingUsesAggregateErrorAndHasNoSideEffects() {
        mockTaskActionContext(900L, 99L, "task-4", "approveTask",
                DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL,
                DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL, Boolean.TRUE);
        DccControlledFileApproveTaskReqVO reqVO = new DccControlledFileApproveTaskReqVO();
        reqVO.setTaskId("task-4");
        reqVO.setPassword("secret");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> workflowService.approveTask(99L, 900L, reqVO));

        assertEquals(CONTROLLED_FILE_FINAL_APPROVAL_NOT_READY.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("盖章 PDF"));
        assertTrue(ex.getMessage().contains("默认目录"));
        verify(signatureVerificationService, never()).verifyPasswordAndCreateSignature(any(), any(), any(), any(), any(), any(), any());
        verify(uploadTicketService, never()).markBound(any(DccUploadTicketMarkBoundCommand.class));
        verify(controlledFileMapper, never()).updateById(any(DccControlledFileDO.class));
        verify(bpmTaskService, never()).approveTask(eq(99L), any(BpmTaskApproveReqVO.class));
    }

    @Test
    void approveTask_snapshotContainsUserButRuntimeTaskAssignedToAnotherUserFailsWithMismatch() {
        when(controlledFileMapper.selectById(900L)).thenReturn(DccControlledFileDO.builder()
                .id(900L)
                .processDefinitionKey(DccControlledFileWorkflowServiceImpl.BPM_PROCESS_DEFINITION_KEY)
                .processInstanceId("proc-1")
                .status(DccControlledFileStatusEnum.PENDING_MATRIX_REVIEW.getStatus())
                .build());
        when(routeSnapshotMapper.selectListByControlledFileId(900L)).thenReturn(List.of(
                DccControlledFileRouteSnapshotDO.builder()
                        .id(2L).controlledFileId(900L).stageNo(2)
                        .stageCode(DccControlledFileStageCodeEnum.MATRIX_REVIEW.getCode())
                        .stageOrder(2).resolvedUserIds("99").build()));
        Task runtimeTask = mockTask("task-2", "proc-1", DccControlledFileStageCodeEnum.MATRIX_REVIEW.getCode());
        when(runtimeTask.getAssignee()).thenReturn("100");
        when(bpmTaskService.validateTask(99L, "task-2"))
                .thenThrow(new ServiceException(1_009_000_005, "该任务的审批人不是你"));
        when(bpmTaskService.getTask("task-2")).thenReturn(runtimeTask);
        DccControlledFileApproveTaskReqVO reqVO = new DccControlledFileApproveTaskReqVO();
        reqVO.setTaskId("task-2");
        reqVO.setPassword("secret");

        assertServiceException(() -> workflowService.approveTask(99L, 900L, reqVO),
                CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH);

        verify(signatureVerificationService, never()).verifyPasswordAndCreateSignature(any(), any(), any(), any(), any(), any(), any());
        verify(bpmTaskService, never()).approveTask(eq(99L), any(BpmTaskApproveReqVO.class));
    }

    @Test
    void transferTask_targetWithoutPostDoesNotSignTransferOrUpdateSnapshot() {
        mockTaskActionContext(900L, 99L, "task-2", "approveTask",
                DccControlledFileStatusEnum.PENDING_MATRIX_REVIEW,
                DccControlledFileStageCodeEnum.MATRIX_REVIEW, Boolean.FALSE);
        DccControlledFileTransferTaskReqVO reqVO = new DccControlledFileTransferTaskReqVO();
        reqVO.setTaskId("task-2");
        reqVO.setPassword("secret");
        reqVO.setAssigneeUserId(101L);
        reqVO.setReason("请代为评审");
        doThrow(exception(CONTROLLED_FILE_APPROVER_POST_REQUIRED))
                .when(approvalParticipantPostValidator).requireConfiguredPosts(List.of(101L));

        assertServiceException(() -> workflowService.transferTask(99L, 900L, reqVO),
                CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED);

        verify(signatureVerificationService, never()).verifyPasswordAndCreateSignature(any(), any(), any(), any(), any(), any(), any());
        verify(bpmTaskService, never()).transferTask(eq(99L), any(BpmTaskTransferReqVO.class));
        verify(routeSnapshotMapper, never()).updateById(any(DccControlledFileRouteSnapshotDO.class));
    }

    @Test
    void createSignTask_targetWithoutPostDoesNotSignCreateTaskOrUpdateSnapshot() {
        mockTaskActionContext(900L, 99L, "task-2", "approveTask",
                DccControlledFileStatusEnum.PENDING_MATRIX_REVIEW,
                DccControlledFileStageCodeEnum.MATRIX_REVIEW, Boolean.FALSE);
        DccControlledFileCreateSignTaskReqVO reqVO = new DccControlledFileCreateSignTaskReqVO();
        reqVO.setTaskId("task-2");
        reqVO.setPassword("secret");
        reqVO.setUserIds(new java.util.LinkedHashSet<>(List.of(101L)));
        reqVO.setType("before");
        reqVO.setReason("增加工艺确认");
        doThrow(exception(CONTROLLED_FILE_APPROVER_POST_REQUIRED))
                .when(approvalParticipantPostValidator).requireConfiguredPosts(List.of(101L));

        assertServiceException(() -> workflowService.createSignTask(99L, 900L, reqVO),
                CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED);

        verify(signatureVerificationService, never()).verifyPasswordAndCreateSignature(any(), any(), any(), any(), any(), any(), any());
        verify(bpmTaskService, never()).createSignTask(eq(99L), any(BpmTaskSignCreateReqVO.class));
        verify(routeSnapshotMapper, never()).updateById(any(DccControlledFileRouteSnapshotDO.class));
    }

    @Test
    void rejectTask_success_updatesRejectedStatusAfterSignature() {
        when(controlledFileMapper.selectById(900L)).thenReturn(DccControlledFileDO.builder()
                .id(900L)
                .categoryId(10L)
                .processDefinitionKey(DccControlledFileWorkflowServiceImpl.BPM_PROCESS_DEFINITION_KEY)
                .processInstanceId("proc-1")
                .status(DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL.getStatus())
                .build());
        when(routeSnapshotMapper.selectListByControlledFileId(900L)).thenReturn(List.of(
                DccControlledFileRouteSnapshotDO.builder()
                        .id(1L)
                        .controlledFileId(900L)
                        .stageNo(4)
                        .stageCode(DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL.getCode())
                        .requireAllApprovals(Boolean.FALSE)
                        .resolvedUserIds("99")
                        .build()));
        Task currentTask = mockTask("task-9", "proc-1", DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL.getCode());
        when(bpmTaskService.validateTask(99L, "task-9")).thenReturn(currentTask);
        DccControlledFileRejectTaskReqVO reqVO = new DccControlledFileRejectTaskReqVO();
        reqVO.setTaskId("task-9");
        reqVO.setPassword("secret");
        reqVO.setReason("need changes");
        mockActionSignature(900L, "task-9", 99L, "REJECT",
                actionSignature(1009L, "task-9", "REJECT", "REJECTED", "DOC_CONTROL_APPROVAL_REJECT"));

        DccSignatureActionRespVO result = workflowService.rejectTask(99L, 900L, reqVO);

        assertEquals(1009L, result.getSignatureId());
        assertEquals("REJECTED", result.getTaskActionResult());
        assertEquals(DccControlledFileStatusEnum.REJECTED.getStatus(), result.getNextStatus());
        verify(signatureVerificationService).verifyPasswordAndCreateSignature(99L, 900L, "task-9",
                DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL.getCode(), "REJECT", "secret", "need changes");
        verify(bpmTaskService).rejectTask(eq(99L), any(BpmTaskRejectReqVO.class));
        ArgumentCaptor<DccControlledFileDO> updateCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).updateById(updateCaptor.capture());
        assertEquals(DccControlledFileStatusEnum.REJECTED.getStatus(), updateCaptor.getValue().getStatus());
        assertEquals("need changes", updateCaptor.getValue().getRejectReason());
    }

    private void mockCommonSubmitDependencies() {
        when(categoryMapper.selectById(10L)).thenReturn(DccFileCategoryDO.builder()
                .id(10L).code("SOP").name("SOP").active(Boolean.TRUE).source("LOCAL")
                .fileTypeTaxonomyId(8803L).build());
        when(categoryDirectoryBindingMapper.selectActiveByCategoryId(10L)).thenReturn(
                DccCategoryDirectoryBindingDO.builder().id(1L).categoryId(10L).directoryId(20L).active(Boolean.TRUE).build());
        when(directoryMapper.selectEnabledList()).thenReturn(List.of(
                directory(20L, null, "01.图纸"),
                directory(21L, 20L, "二级目录")));
        lenient().when(routeMapper.selectLatestActiveByCategoryId(10L)).thenReturn(
                DccCategoryApprovalRouteDO.builder().id(30L).categoryId(10L).versionNo(2).active(Boolean.TRUE).effectiveTime(LocalDateTime.now()).build());
        lenient().when(fileMapper.selectById(100L)).thenReturn(FileDO.builder()
                .id(100L).name("SOP-001.docx")
                .type("application/vnd.openxmlformats-officedocument.wordprocessingml.document").build());
        lenient().when(fileMapper.selectById(101L)).thenReturn(FileDO.builder()
                .id(101L).name("SOP-001.pdf").type("application/pdf").build());
        lenient().when(uploadTicketService.resolveForBinding(any(DccUploadTicketResolveCommand.class)))
                .thenAnswer(invocation -> {
                    DccUploadTicketResolveCommand command = invocation.getArgument(0);
                    if ("DRAWING_PDF".equals(command.purpose())) {
                        return new DccUploadTicketBoundFile(command.uploadTicket(), 101L,
                                "SOP-001.pdf", "application/pdf", 8L);
                    }
                    return new DccUploadTicketBoundFile(command.uploadTicket(), 100L,
                            "SOP-001.docx",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", 4L);
                });
        lenient().doAnswer(invocation -> {
            DccControlledFileMasterDO master = invocation.getArgument(0);
            master.setId(700L);
            return 1;
        }).when(controlledFileMasterMapper).insert(any(DccControlledFileMasterDO.class));
    }

    private List<DccCategoryApprovalRouteNodeDO> fullApprovalRoute(DccCategoryApprovalRouteNodeDO first) {
        return List.of(first,
                routeNode(2, DccControlledFileStageCodeEnum.MATRIX_REVIEW.getCode(), "审核会签", "USER", 200L),
                routeNode(3, DccControlledFileStageCodeEnum.MATRIX_APPROVAL.getCode(), "批准", "USER", 200L),
                routeNode(4, DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL.getCode(), "文控批准", "USER", 200L));
    }

    private void mockFourStageRoute() {
        when(routeNodeMapper.selectListByRouteId(30L)).thenReturn(fullApprovalRoute(
                routeNode(1, DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode(),
                        "Doc Control Review", "POSITION", 50L)));
        when(positionAssignmentMapper.selectActiveListByPositionId(50L)).thenReturn(List.of(
                DccPositionAssignmentDO.builder().id(60L).positionId(50L).assignmentType("POST")
                        .systemPostId(500L).active(Boolean.TRUE).build()));
        when(adminUserApi.getUserListByPostIds(List.of(500L))).thenReturn(List.of(
                new AdminUserRespDTO().setId(200L).setStatus(0)));
    }

    private void useStampedPdfTicket(DccControlledFileApproveTaskReqVO reqVO) {
        reqVO.setSessionId("dcc-approval:900:6:task-4:session-stamped");
        reqVO.setStampedPdfUploadTicket("UT-STAMPED");
        when(uploadTicketService.resolveForBinding(new DccUploadTicketResolveCommand(
                "UT-STAMPED", 99L, 10L, "dcc-approval:900:6:task-4:session-stamped", "APPROVAL_PDF")))
                .thenReturn(new DccUploadTicketBoundFile("UT-STAMPED", 800L,
                        "controlled.pdf", "application/pdf", 128L));
    }

    private void useSingleDistributionDepartment(DccControlledFileApproveTaskReqVO reqVO) {
        reqVO.setSelectedDistributionScopes(List.of(
                distributionScope(300L, DccDistributionMediumEnum.PUBLIC_FOLDER.getCode())));
        when(adminUserApi.getUserListByDeptIds(List.of(300L))).thenReturn(List.of(
                new AdminUserRespDTO().setId(120L).setStatus(0).setDeptId(300L)));
    }

    private void useConfirmedDirectory(DccControlledFileApproveTaskReqVO reqVO) {
        useConfirmedDirectory(reqVO, 21L);
    }

    private void useConfirmedDirectory(DccControlledFileApproveTaskReqVO reqVO, Long directoryId) {
        reqVO.setConfirmedDirectoryId(directoryId);
        when(categoryDirectoryBindingMapper.selectActiveByCategoryId(10L)).thenReturn(
                DccCategoryDirectoryBindingDO.builder().id(1L).categoryId(10L).directoryId(20L).active(Boolean.TRUE).build());
        when(directoryMapper.selectEnabledList()).thenReturn(List.of(
                directory(20L, null, "01.图纸"),
                directory(21L, 20L, "二级目录"),
                directory(22L, 20L, "文控确认目录")));
    }

    private DccControlledFileApproveTaskReqVO.DistributionScope distributionScope(Long departmentId,
                                                                                  String distributionMedium) {
        DccControlledFileApproveTaskReqVO.DistributionScope scope =
                new DccControlledFileApproveTaskReqVO.DistributionScope();
        scope.setDepartmentId(departmentId);
        scope.setDistributionMedium(distributionMedium);
        return scope;
    }

    private DccControlledFileSubmitReqVO buildSubmitReqVO(String versionNo) {
        DccControlledFileSubmitReqVO reqVO = new DccControlledFileSubmitReqVO();
        reqVO.setCategoryId(10L);
        reqVO.setIdempotencyKey("dcc-submit-test-1");
        reqVO.setSessionId("session-1");
        reqVO.setOriginalUploadTicket("UT-ORIGINAL");
        reqVO.setSourceUploadTicket(null);
        reqVO.setSourceFileName("SOP-001.docx");
        reqVO.setDrawingPdfUploadTicket("UT-DRAWING");
        reqVO.setProductMasterId(5000L);
        reqVO.setProductCode("PRD20260525001");
        reqVO.setDccProjectCodeId(3000L);
        reqVO.setFileTypeTaxonomyId(8803L);
        reqVO.setNeedTraining(Boolean.FALSE);
        reqVO.setProcessType("CONTROLLED_FILE");
        reqVO.setChangeType(DccControlledFileChangeTypeEnum.NEW.getCode());
        reqVO.setFileName("SOP-001");
        reqVO.setFileNumber("SOP-001");
        reqVO.setDirectoryId(21L);
        reqVO.setVersionNo(versionNo);
        reqVO.setEffectiveDate(LocalDate.of(2026, 5, 13));
        reqVO.setRemark("initial release");
        return reqVO;
    }

    private DccControlledFileSubmitReqVO buildRawFileIdSubmitReqVO(String versionNo) {
        DccControlledFileSubmitReqVO reqVO = new DccControlledFileSubmitReqVO();
        reqVO.setCategoryId(10L);
        reqVO.setIdempotencyKey("dcc-submit-raw-test-1");
        reqVO.setOriginalFileId(100L);
        reqVO.setSourceFileId(100L);
        reqVO.setSourceFileName("SOP-001.docx");
        reqVO.setDrawingPdfFileId(101L);
        reqVO.setProductMasterId(5000L);
        reqVO.setProductCode("PRD20260525001");
        reqVO.setDccProjectCodeId(3000L);
        reqVO.setFileTypeTaxonomyId(8803L);
        reqVO.setNeedTraining(Boolean.FALSE);
        reqVO.setProcessType("CONTROLLED_FILE");
        reqVO.setChangeType(DccControlledFileChangeTypeEnum.NEW.getCode());
        reqVO.setFileName("SOP-001");
        reqVO.setFileNumber("SOP-001");
        reqVO.setDirectoryId(21L);
        reqVO.setVersionNo(versionNo);
        reqVO.setEffectiveDate(LocalDate.of(2026, 5, 13));
        reqVO.setRemark("initial release");
        return reqVO;
    }

    private DccFileTypeTaxonomyPath defaultTaxonomyPath() {
        return new DccFileTypeTaxonomyPath(8803L, "一级", "二级", "三级", "四级", null);
    }

    private DccFileDirectoryDO directory(Long id, Long parentId, String name) {
        return directory(id, parentId, name, "DIR-" + id);
    }

    private DccFileDirectoryDO directory(Long id, Long parentId, String name, String code) {
        return DccFileDirectoryDO.builder()
                .id(id)
                .parentId(parentId)
                .code(code)
                .name(name)
                .active(Boolean.TRUE)
                .sort(1)
                .build();
    }

    private DccCategoryApprovalRouteNodeDO routeNode(Integer stageNo, String stageCode, String stageName,
                                                     String candidateSourceType, Long candidateSourceId) {
        return DccCategoryApprovalRouteNodeDO.builder()
                .id(40L)
                .routeId(30L)
                .stageNo(stageNo)
                .stageCode(stageCode)
                .stageName(stageName)
                .stageOrder(stageNo)
                .candidateSourceType(candidateSourceType)
                .candidateSourceId(candidateSourceId)
                .candidateSourceIds(String.valueOf(candidateSourceId))
                .approveMethod(stageNo == 2 ? "ALL" : "ANY")
                .approveRatio(stageNo == 2 ? 100 : null)
                .requireAllApprovals(stageNo == 2)
                .required(Boolean.TRUE)
                .sort(stageNo)
                .build();
    }

    private Task mockTask(String taskId, String processInstanceId, String taskDefinitionKey) {
        Task task = mock(Task.class);
        lenient().when(task.getId()).thenReturn(taskId);
        lenient().when(task.getProcessInstanceId()).thenReturn(processInstanceId);
        lenient().when(task.getTaskDefinitionKey()).thenReturn(taskDefinitionKey);
        return task;
    }

    private DccFileCategoryPermissionRuleDO permissionRule(String actionType, String subjectType, Long subjectId) {
        return DccFileCategoryPermissionRuleDO.builder()
                .id(1L)
                .categoryId(10L)
                .actionType(actionType)
                .subjectType(subjectType)
                .subjectId(subjectId)
                .active(Boolean.TRUE)
                .build();
    }

    private Task mockTaskActionContext(Long fileId, Long userId, String taskId, String taskDefinitionKey,
                                       DccControlledFileStatusEnum status,
                                       DccControlledFileStageCodeEnum stageCode,
                                       Boolean needTraining) {
        DccControlledFileDO file = DccControlledFileDO.builder()
                .id(fileId)
                .masterId(700L)
                .categoryId(10L)
                .directoryId(21L)
                .fileName("SOP-001")
                .fileNumber("SOP-001")
                .processDefinitionKey(DccControlledFileWorkflowServiceImpl.BPM_PROCESS_DEFINITION_KEY)
                .processInstanceId("proc-1")
                .status(status.getStatus())
                .needTraining(needTraining)
                .build();
        when(controlledFileMapper.selectById(fileId)).thenReturn(file);
        DccControlledFileRouteSnapshotDO stageSnapshot = DccControlledFileRouteSnapshotDO.builder()
                .id(10L)
                .controlledFileId(fileId)
                .stageNo(stageCode == DccControlledFileStageCodeEnum.MATRIX_REVIEW ? 2
                        : stageCode == DccControlledFileStageCodeEnum.MATRIX_APPROVAL ? 3 : 4)
                .stageCode(stageCode.getCode())
                .stageOrder(stageCode == DccControlledFileStageCodeEnum.MATRIX_REVIEW ? 2
                        : stageCode == DccControlledFileStageCodeEnum.MATRIX_APPROVAL ? 3 : 4)
                .requireAllApprovals(Boolean.FALSE)
                .resolvedUserIds(stageCode == DccControlledFileStageCodeEnum.MATRIX_REVIEW ? "99,100" : "99")
                .build();
        DccControlledFileRouteSnapshotDO matrixReviewSnapshot = DccControlledFileRouteSnapshotDO.builder()
                .id(9L)
                .controlledFileId(fileId)
                .stageNo(2)
                .stageCode(DccControlledFileStageCodeEnum.MATRIX_REVIEW.getCode())
                .stageOrder(2)
                .requireAllApprovals(Boolean.FALSE)
                .resolvedUserIds("99,100")
                .build();
        when(routeSnapshotMapper.selectListByControlledFileId(fileId)).thenReturn(
                stageCode == DccControlledFileStageCodeEnum.MATRIX_REVIEW
                        ? List.of(stageSnapshot)
                        : List.of(matrixReviewSnapshot, stageSnapshot));
        Task currentTask = mockTask(taskId, "proc-1", taskDefinitionKey);
        when(bpmTaskService.validateTask(userId, taskId)).thenReturn(currentTask);
        return currentTask;
    }

    private void mockActionSignature(Long controlledFileId, String taskId, Long actorId, String actionType,
                                     DccControlledFileSignatureDO signature) {
        // approve/reject actions now receive the signature result directly from the unified signature kernel.
    }

    private DccControlledFileSignatureDO actionSignature(Long id, String taskId, String actionType,
                                                        String taskActionResult, String meaningCode) {
        return DccControlledFileSignatureDO.builder()
                .id(id)
                .controlledFileId(900L)
                .revisionId(900L)
                .versionNo("A.1")
                .taskId(taskId)
                .actorId(99L)
                .actionType(actionType)
                .meaningCode(meaningCode)
                .controlledCopyHashStatus("NOT_APPLICABLE")
                .evidenceHash("6f2c91ab03d4aabbcc")
                .evidenceStatus("VALID")
                .signedAt(LocalDateTime.of(2026, 5, 26, 14, 32, 18))
                .build();
    }

    private void assertFinalApprovalNotReady(org.junit.jupiter.api.function.Executable executable,
                                             String expectedMessageFragment) {
        ServiceException ex = assertThrows(ServiceException.class, executable);
        assertEquals(CONTROLLED_FILE_FINAL_APPROVAL_NOT_READY.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains(expectedMessageFragment));
    }

    private void assertWithdrawAllowed(String status) {
        reset(controlledFileMapper, bpmProcessInstanceService, platformAdapter);
        when(controlledFileMapper.selectById(900L)).thenReturn(DccControlledFileDO.builder()
                .id(900L)
                .requesterId(99L)
                .processInstanceId("proc-1")
                .status(status)
                .build());
        DccControlledFileWithdrawReqVO reqVO = new DccControlledFileWithdrawReqVO();
        reqVO.setReason("stop");

        workflowService.withdrawControlledFile(99L, 900L, reqVO);

        verify(bpmProcessInstanceService).cancelProcessInstanceByStartUser(any(Long.class), any(BpmProcessInstanceCancelReqVO.class));
        ArgumentCaptor<DccControlledFileDO> updateCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).updateById(updateCaptor.capture());
        assertEquals(DccControlledFileStatusEnum.WITHDRAWN.getStatus(), updateCaptor.getValue().getStatus());
        assertEquals("stop", updateCaptor.getValue().getRejectReason());
        verify(platformAdapter).recordWithdrawn(
                org.mockito.ArgumentMatchers.argThat(file -> Long.valueOf(900L).equals(file.getId())),
                eq(99L), eq("stop"));
    }
}
