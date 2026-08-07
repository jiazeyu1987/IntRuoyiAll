package cn.iocoder.yudao.module.dcc.service.file;

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
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileCreateSignTaskReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRejectTaskReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileReturnTaskReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileActionProjectionRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileCurrentVersionRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRoutePreviewRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSubmitReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileTrainingRecordReqVO;
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
import cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyPath;
import cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryAccessPermissionService;
import cn.iocoder.yudao.module.dcc.service.position.DccApprovalPositionRuntimeResolver;
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

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_FILE_NUMBER_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_DRAWING_PDF_FILE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_DRAWING_PDF_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PROCESS_TYPE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ROUTE_NOT_CONFIGURED;
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
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.ROUTE_PREVIEW_APPROVER_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccControlledFileWorkflowServiceImplTest extends BaseMockitoUnitTest {

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
    private BpmProcessInstanceApi bpmProcessInstanceApi;
    @Mock
    private BpmProcessInstanceService bpmProcessInstanceService;
    @Mock
    private BpmTaskService bpmTaskService;
    @Mock
    private AdminUserApi adminUserApi;
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

    private final DccControlledFileApprovalRouteAssigneeResolver approvalRouteAssigneeResolver =
            new DccControlledFileApprovalRouteAssigneeResolver();

    @InjectMocks
    private DccControlledFileWorkflowServiceImpl workflowService;

    @BeforeEach
    void setDefaultUploadTicketBindings() {
        TenantContextHolder.setTenantId(1L);
        ReflectionTestUtils.setField(approvalRouteAssigneeResolver, "routeMapper", routeMapper);
        ReflectionTestUtils.setField(approvalRouteAssigneeResolver, "routeNodeMapper", routeNodeMapper);
        ReflectionTestUtils.setField(approvalRouteAssigneeResolver, "positionAssignmentMapper", positionAssignmentMapper);
        ReflectionTestUtils.setField(approvalRouteAssigneeResolver, "positionRuntimeResolver", positionRuntimeResolver);
        ReflectionTestUtils.setField(approvalRouteAssigneeResolver, "adminUserApi", adminUserApi);
        ReflectionTestUtils.setField(workflowService, "approvalRouteAssigneeResolver", approvalRouteAssigneeResolver);
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
                .thenReturn(List.of(8803L, 8804L));
        lenient().when(fileTypeTaxonomyAdminService.listActiveDescendantPaths(8803L))
                .thenReturn(List.of(
                        new DccFileTypeTaxonomyPath(8803L, "一级", "二级", "三级", null, null),
                        new DccFileTypeTaxonomyPath(8804L, "一级", "二级", "三级", "四级", null)));
    }

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void submitControlledFile_withoutCategoryUploadPermission_success() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("V1.0");
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
        when(adminUserApi.getUserListByPostIds(List.of(500L))).thenReturn(List.of(new AdminUserRespDTO().setId(200L)));
        when(adminUserApi.getUserListByPostIds(List.of(501L))).thenReturn(List.of(new AdminUserRespDTO().setId(201L)));
        when(adminUserApi.getUserListByPostIds(List.of(502L))).thenReturn(List.of(new AdminUserRespDTO().setId(202L)));
        when(adminUserApi.getUserListByPostIds(List.of(503L))).thenReturn(List.of(new AdminUserRespDTO().setId(203L)));
        when(adminUserApi.getUserListByPostIds(List.of(504L))).thenReturn(List.of(new AdminUserRespDTO().setId(204L)));
        when(adminUserApi.getUserListByPostIds(List.of(505L))).thenReturn(List.of(new AdminUserRespDTO().setId(205L)));
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
        ArgumentCaptor<DccControlledFileMasterDO> masterCaptor = ArgumentCaptor.forClass(DccControlledFileMasterDO.class);
        verify(controlledFileMasterMapper).insert(masterCaptor.capture());
        verify(controlledFileMasterMapper).selectByIdForUpdate(700L);
        assertEquals("SOP-001", masterCaptor.getValue().getFileName());
        assertEquals("SOP-001", masterCaptor.getValue().getFileNumber());
        ArgumentCaptor<DccControlledFileDO> fileCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).insert(fileCaptor.capture());
        assertEquals(700L, fileCaptor.getValue().getMasterId());
        assertEquals(21L, fileCaptor.getValue().getDirectoryId());
        assertEquals("SOP-001", fileCaptor.getValue().getFileName());
        assertEquals("SOP-001", fileCaptor.getValue().getFileNumber());
        assertEquals(DccControlledFileChangeTypeEnum.NEW.getCode(), fileCaptor.getValue().getChangeType());
        assertNull(fileCaptor.getValue().getProductMasterId());
        assertEquals(100L, fileCaptor.getValue().getSourceFileId());
        assertEquals(101L, fileCaptor.getValue().getDrawingPdfFileId());
        assertEquals("PRJ-20260719", fileCaptor.getValue().getProductCode());
        assertEquals("验证项目", fileCaptor.getValue().getProductName());
        assertEquals(3000L, fileCaptor.getValue().getDccProjectCodeId());
        assertEquals(8803L, fileCaptor.getValue().getFileTypeTaxonomyId());
        assertEquals("一级", fileCaptor.getValue().getFileTypeLevel1());
        assertEquals("二级", fileCaptor.getValue().getFileTypeLevel2());
        assertEquals("三级", fileCaptor.getValue().getFileTypeLevel3());
        assertEquals("四级", fileCaptor.getValue().getFileTypeLevel4());
        assertNull(fileCaptor.getValue().getFileTypeLevel5());
        assertEquals(Boolean.FALSE, fileCaptor.getValue().getNeedTraining());
        assertEquals("CONTROLLED_FILE", fileCaptor.getValue().getProcessType());
        assertEquals(DccControlledFileStatusEnum.PENDING_DOC_CONTROL_REVIEW.getStatus(), fileCaptor.getValue().getStatus());
        assertEquals(LocalDate.of(2026, 5, 13), fileCaptor.getValue().getEffectiveDate());
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
                "UT-ORIGINAL", 99L, "session-1", "SOURCE", 900L));
        verify(uploadTicketService).markBound(new DccUploadTicketMarkBoundCommand(
                "UT-DRAWING", 99L, "session-1", "DRAWING_PDF", 900L));
        verify(permissionSupport, never()).hasCategoryPermission(any(Long.class), any(Long.class),
                any(DccFileCategoryPermissionActionEnum.class));
    }

    @Test
    void submitControlledFile_missingProjectOrTaxonomy_failsBeforeInsert() {
        DccControlledFileSubmitReqVO missingProjectReqVO = buildSubmitReqVO("V1.0");
        missingProjectReqVO.setDccProjectCodeId(null);

        assertServiceException(() -> workflowService.submitControlledFile(99L, missingProjectReqVO),
                CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING);

        DccControlledFileSubmitReqVO missingTaxonomyReqVO = buildSubmitReqVO("V1.0");
        missingTaxonomyReqVO.setFileTypeTaxonomyId(null);

        assertServiceException(() -> workflowService.submitControlledFile(99L, missingTaxonomyReqVO),
                CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING);
        verify(controlledFileMapper, never()).insert(any(DccControlledFileDO.class));
        verify(bpmProcessInstanceApi, never()).createProcessInstance(any(Long.class), any());
    }

    @Test
    void submitControlledFile_taxonomyMustReachThirdLevel() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("V1.0");
        when(fileTypeTaxonomyAdminService.resolveActivePath(8803L))
                .thenReturn(new DccFileTypeTaxonomyPath(8803L, "一级", "二级", null, null, null));

        assertServiceException(() -> workflowService.submitControlledFile(99L, reqVO),
                FILE_TYPE_TAXONOMY_LEVEL_INVALID);

        verify(controlledFileMapper, never()).insert(any(DccControlledFileDO.class));
        verify(bpmProcessInstanceApi, never()).createProcessInstance(any(Long.class), any());
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
                .versionNo("V1.0")
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
        assertEquals("V1.0", respVO.getCurrentVersionNo());
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
    void submitControlledFile_unfinishedSameNumberWorkflow_throwsInProgress() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("V1.1");
        reqVO.setChangeType(DccControlledFileChangeTypeEnum.REVISION.getCode());
        reqVO.setRevisionTargetControlledFileId(800L);
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
                .versionNo("V1.0")
                .fileNumber("SOP-001")
                .dccProjectCodeId(3000L)
                .fileTypeTaxonomyId(8803L)
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .sourceFileId(88L)
                .originalFileId(88L)
                .build();
        DccControlledFileDO pendingFile = DccControlledFileDO.builder()
                .id(801L)
                .masterId(700L)
                .versionNo("V1.1")
                .status(DccControlledFileStatusEnum.PENDING_MATRIX_REVIEW.getStatus())
                .build();
        when(controlledFileMasterMapper.selectListByFileNumber("SOP-001")).thenReturn(List.of(master));
        when(controlledFileMapper.selectById(800L)).thenReturn(activeFile);
        when(controlledFileMapper.selectList(
                org.mockito.ArgumentMatchers.<SFunction<DccControlledFileDO, ?>>any(), eq(700L)))
                .thenReturn(List.of(activeFile, pendingFile));

        assertServiceException(() -> workflowService.submitControlledFile(99L, reqVO),
                CONTROLLED_FILE_WORKFLOW_IN_PROGRESS);

        verify(controlledFileMapper, never()).insert(any(DccControlledFileDO.class));
        verify(bpmProcessInstanceApi, never()).createProcessInstance(any(Long.class), any());
    }

    @Test
    void submitControlledFile_readyToPublishCandidate_throwsInProgress() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("V1.2");
        reqVO.setChangeType(DccControlledFileChangeTypeEnum.REVISION.getCode());
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
                .versionNo("V1.0")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .sourceFileId(88L)
                .originalFileId(88L)
                .build();
        DccControlledFileDO readyToPublishFile = DccControlledFileDO.builder()
                .id(801L)
                .masterId(700L)
                .versionNo("V1.1")
                .status(DccControlledFileStatusEnum.READY_TO_PUBLISH.getStatus())
                .build();
        when(controlledFileMasterMapper.selectListByFileNumber("SOP-001")).thenReturn(List.of(master));
        when(controlledFileMapper.selectById(800L)).thenReturn(activeFile);
        when(controlledFileMapper.selectList(
                org.mockito.ArgumentMatchers.<SFunction<DccControlledFileDO, ?>>any(), eq(700L)))
                .thenReturn(List.of(activeFile, readyToPublishFile));

        assertServiceException(() -> workflowService.submitControlledFile(99L, reqVO),
                CONTROLLED_FILE_WORKFLOW_IN_PROGRESS);

        verify(controlledFileMapper, never()).insert(any(DccControlledFileDO.class));
        verify(bpmProcessInstanceApi, never()).createProcessInstance(any(Long.class), any());
    }

    @Test
    void submitControlledFile_revisionRequiresUniqueCurrentVersionAndPersistsOriginalPath() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("V1.1");
        reqVO.setChangeType(DccControlledFileChangeTypeEnum.REVISION.getCode());
        reqVO.setRevisionTargetControlledFileId(800L);
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
                .dccProjectCodeId(3000L)
                .fileTypeTaxonomyId(8803L)
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .sourceFileId(88L)
                .originalFileId(88L)
                .build();
        when(controlledFileMasterMapper.selectListByFileNumber("SOP-001")).thenReturn(List.of(master));
        when(controlledFileMapper.selectById(800L)).thenReturn(activeFile);
        when(controlledFileMapper.selectAssociatedFilesByProjectCodeId(3000L, List.of(800L)))
                .thenReturn(List.of(activeFile));
        lenient().when(controlledFileMapper.selectListByMasterId(700L)).thenReturn(List.of(activeFile));
        mockSingleStageRoute();
        doAnswer(invocation -> {
            DccControlledFileDO file = invocation.getArgument(0);
            file.setId(910L);
            return 1;
        }).when(controlledFileMapper).insert(any(DccControlledFileDO.class));
        when(bpmProcessInstanceApi.createProcessInstance(any(Long.class), any())).thenReturn("proc-revision");

        Long fileId = workflowService.submitControlledFile(99L, reqVO);

        assertEquals(910L, fileId);
        ArgumentCaptor<DccControlledFileDO> fileCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).insert(fileCaptor.capture());
        assertEquals(DccControlledFileChangeTypeEnum.REVISION.getCode(), fileCaptor.getValue().getChangeType());
        assertEquals(88L, fileCaptor.getValue().getOriginalFileId());
        assertEquals(100L, fileCaptor.getValue().getSourceFileId());
        assertEquals(3000L, fileCaptor.getValue().getDccProjectCodeId());
        assertEquals(8803L, fileCaptor.getValue().getFileTypeTaxonomyId());
    }

    @Test
    void submitControlledFile_revisionTargetIsRequiredAndMustMatchProjectAndTaxonomyScope() {
        DccControlledFileSubmitReqVO missingTargetReqVO = buildSubmitReqVO("V1.1");
        missingTargetReqVO.setChangeType(DccControlledFileChangeTypeEnum.REVISION.getCode());
        mockCommonSubmitDependencies();
        DccControlledFileMasterDO master = DccControlledFileMasterDO.builder()
                .id(700L)
                .fileName("SOP-001")
                .fileNumber("SOP-001")
                .currentActiveControlledFileId(800L)
                .status(DccControlledFileMasterStatusEnum.ACTIVE_CHAIN.getCode())
                .build();
        DccControlledFileDO activeFile = DccControlledFileDO.builder()
                .id(800L)
                .masterId(700L)
                .fileName("SOP-001")
                .fileNumber("SOP-001")
                .versionNo("V1.0")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .sourceFileId(88L)
                .originalFileId(88L)
                .dccProjectCodeId(3000L)
                .fileTypeTaxonomyId(8803L)
                .build();
        when(controlledFileMasterMapper.selectListByFileNumber("SOP-001")).thenReturn(List.of(master));
        when(controlledFileMapper.selectById(800L)).thenReturn(activeFile);
        lenient().when(controlledFileMapper.selectListByMasterId(700L)).thenReturn(List.of(activeFile));

        assertServiceException(() -> workflowService.submitControlledFile(99L, missingTargetReqVO),
                CONTROLLED_FILE_TASK_TARGET_INVALID);

        DccControlledFileSubmitReqVO mismatchProjectReqVO = buildSubmitReqVO("V1.1");
        mismatchProjectReqVO.setChangeType(DccControlledFileChangeTypeEnum.REVISION.getCode());
        mismatchProjectReqVO.setRevisionTargetControlledFileId(800L);
        when(controlledFileMapper.selectAssociatedFilesByProjectCodeId(3000L, List.of(800L)))
                .thenReturn(List.of());

        assertServiceException(() -> workflowService.submitControlledFile(99L, mismatchProjectReqVO),
                CONTROLLED_FILE_TASK_TARGET_INVALID);

        DccControlledFileSubmitReqVO mismatchTaxonomyReqVO = buildSubmitReqVO("V1.1");
        mismatchTaxonomyReqVO.setChangeType(DccControlledFileChangeTypeEnum.REVISION.getCode());
        mismatchTaxonomyReqVO.setRevisionTargetControlledFileId(800L);
        DccControlledFileDO taxonomyMismatchFile = DccControlledFileDO.builder()
                .id(800L)
                .masterId(700L)
                .fileName("SOP-001")
                .fileNumber("SOP-001")
                .versionNo("V1.0")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .sourceFileId(88L)
                .originalFileId(88L)
                .dccProjectCodeId(3000L)
                .fileTypeTaxonomyId(9900L)
                .build();
        when(controlledFileMapper.selectById(800L)).thenReturn(taxonomyMismatchFile);
        when(controlledFileMapper.selectAssociatedFilesByProjectCodeId(3000L, List.of(800L)))
                .thenReturn(List.of(taxonomyMismatchFile));

        assertServiceException(() -> workflowService.submitControlledFile(99L, mismatchTaxonomyReqVO),
                CONTROLLED_FILE_TASK_TARGET_INVALID);

        verify(controlledFileMapper, never()).insert(any(DccControlledFileDO.class));
        verify(bpmProcessInstanceApi, never()).createProcessInstance(any(Long.class), any());
    }

    @Test
    void getUploadRevisionCandidates_buildsFixedActiveLatestProjectTaxonomyQuery() {
        DccControlledFileRespVO candidate = new DccControlledFileRespVO();
        candidate.setId(800L);
        candidate.setFileName("SOP-001");
        PageResult<DccControlledFileRespVO> pageResult = new PageResult<>(List.of(candidate), 1L);
        when(queryService.getControlledFilePage(eq(99L), any(DccControlledFilePageReqVO.class)))
                .thenReturn(pageResult);

        PageResult<DccControlledFileRespVO> result =
                workflowService.getUploadRevisionCandidates(99L, 3000L, 8803L, " SOP ", 2, 20);

        assertEquals(pageResult, result);
        ArgumentCaptor<DccControlledFilePageReqVO> reqCaptor =
                ArgumentCaptor.forClass(DccControlledFilePageReqVO.class);
        verify(queryService).getControlledFilePage(eq(99L), reqCaptor.capture());
        assertEquals(2, reqCaptor.getValue().getPageNo());
        assertEquals(20, reqCaptor.getValue().getPageSize());
        assertEquals("SOP", reqCaptor.getValue().getKeyword());
        assertEquals(3000L, reqCaptor.getValue().getDccProjectCodeId());
        assertEquals(List.of(8803L, 8804L), reqCaptor.getValue().getFileTypeTaxonomyIds());
        assertEquals(2, reqCaptor.getValue().getFileTypeTaxonomyPaths().size());
        assertEquals("三级", reqCaptor.getValue().getFileTypeTaxonomyPaths().get(0).getLevel3());
        assertEquals("四级", reqCaptor.getValue().getFileTypeTaxonomyPaths().get(1).getLevel4());
        assertEquals(DccControlledFileStatusEnum.ACTIVE.getStatus(), reqCaptor.getValue().getStatus());
        assertEquals(DccControlledFileProcessTypeEnum.CONTROLLED_FILE.getCode(), reqCaptor.getValue().getProcessType());
        assertEquals(Boolean.TRUE, reqCaptor.getValue().getLatestVersionOnly());
    }

    @Test
    void submitControlledFile_revisionTargetMayMatchLegacyFileTypePathWhenIdIsMissing() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("V1.1");
        reqVO.setChangeType(DccControlledFileChangeTypeEnum.REVISION.getCode());
        reqVO.setRevisionTargetControlledFileId(800L);
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
        DccControlledFileDO legacyActiveFile = DccControlledFileDO.builder()
                .id(800L)
                .masterId(700L)
                .categoryId(10L)
                .directoryId(21L)
                .fileName("SOP-001")
                .fileNumber("SOP-001")
                .versionNo("V1.0")
                .dccProjectCodeId(3000L)
                .fileTypeLevel1("一级")
                .fileTypeLevel2("二级")
                .fileTypeLevel3("三级")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .sourceFileId(88L)
                .originalFileId(88L)
                .build();
        when(controlledFileMasterMapper.selectListByFileNumber("SOP-001")).thenReturn(List.of(master));
        when(controlledFileMapper.selectById(800L)).thenReturn(legacyActiveFile);
        when(controlledFileMapper.selectAssociatedFilesByProjectCodeId(3000L, List.of(800L)))
                .thenReturn(List.of(legacyActiveFile));
        when(fileTypeTaxonomyAdminService.resolveActiveIdByPath("一级", "二级", "三级", null, null))
                .thenReturn(8803L);
        lenient().when(controlledFileMapper.selectListByMasterId(700L)).thenReturn(List.of(legacyActiveFile));
        mockSingleStageRoute();
        doAnswer(invocation -> {
            DccControlledFileDO file = invocation.getArgument(0);
            file.setId(911L);
            return 1;
        }).when(controlledFileMapper).insert(any(DccControlledFileDO.class));
        when(bpmProcessInstanceApi.createProcessInstance(any(Long.class), any())).thenReturn("proc-revision-legacy");

        Long fileId = workflowService.submitControlledFile(99L, reqVO);

        assertEquals(911L, fileId);
        ArgumentCaptor<DccControlledFileDO> fileCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).insert(fileCaptor.capture());
        assertEquals(8803L, fileCaptor.getValue().getFileTypeTaxonomyId());
        assertEquals(3000L, fileCaptor.getValue().getDccProjectCodeId());
    }

    @Test
    void submitControlledFile_obsoleteStillRequiresUploadedSourceFileAndPersistsOriginalPath() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("V1.1");
        reqVO.setChangeType(DccControlledFileChangeTypeEnum.OBSOLETE.getCode());
        reqVO.setOriginalUploadTicket(null);
        reqVO.setSourceUploadTicket(null);

        assertServiceException(() -> workflowService.submitControlledFile(99L, reqVO),
                CONTROLLED_FILE_UPLOAD_TICKET_INVALID);
        verify(controlledFileMapper, never()).insert(any(DccControlledFileDO.class));
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
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("V1.0");
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
        when(routeNodeMapper.selectListByRouteId(30L)).thenReturn(List.of(
                routeNode(1, DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode(), "Doc Control Review", "POSITION", 50L)));
        when(positionAssignmentMapper.selectActiveListByPositionId(50L)).thenReturn(List.of(
                DccPositionAssignmentDO.builder().id(60L).positionId(50L).assignmentType("POST").systemPostId(500L).active(Boolean.TRUE).build()));
        when(adminUserApi.getUserListByPostIds(List.of(500L))).thenReturn(List.of(new AdminUserRespDTO().setId(200L)));
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
                "UT-ORIGINAL", 99L, "session-1", "SOURCE", 900L));
        verify(uploadTicketService).markBound(new DccUploadTicketMarkBoundCommand(
                "UT-SOURCE", 99L, "session-1", "SOURCE", 900L));
    }

    @Test
    @SuppressWarnings("unchecked")
    void submitControlledFile_selectedSignoffUsers_overrideMatrixReviewOnlyForInstance() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("V1.0");
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
        when(adminUserApi.getUserListByPostIds(List.of(500L))).thenReturn(List.of(new AdminUserRespDTO().setId(200L)));
        when(adminUserApi.getUserListByPostIds(List.of(501L))).thenReturn(List.of(new AdminUserRespDTO().setId(201L)));
        when(adminUserApi.getUserListByPostIds(List.of(502L))).thenReturn(List.of(new AdminUserRespDTO().setId(203L)));
        when(adminUserApi.getUserListByPostIds(List.of(503L))).thenReturn(List.of(new AdminUserRespDTO().setId(205L)));
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

        workflowService.submitControlledFile(99L, reqVO);

        ArgumentCaptor<DccControlledFileRouteSnapshotDO> snapshotCaptor =
                ArgumentCaptor.forClass(DccControlledFileRouteSnapshotDO.class);
        verify(routeSnapshotMapper, org.mockito.Mockito.times(4)).insert(snapshotCaptor.capture());
        DccControlledFileRouteSnapshotDO signoffSnapshot = snapshotCaptor.getAllValues().stream()
                .filter(snapshot -> DccControlledFileStageCodeEnum.MATRIX_REVIEW.getCode().equals(snapshot.getStageCode()))
                .findFirst()
                .orElseThrow();
        assertEquals("301,302", signoffSnapshot.getResolvedUserIds());
        ArgumentCaptor<BpmProcessInstanceCreateReqDTO> processCaptor = ArgumentCaptor.forClass(BpmProcessInstanceCreateReqDTO.class);
        verify(bpmProcessInstanceApi).createProcessInstance(eq(99L), processCaptor.capture());
        Map<String, List<Long>> nextAssignees = (Map<String, List<Long>>) processCaptor.getValue()
                .getVariables().get(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_APPROVE_USER_SELECT_ASSIGNEES);
        assertEquals(List.of(301L, 302L), nextAssignees.get(DccControlledFileStageCodeEnum.MATRIX_REVIEW.getCode()));
        assertEquals(List.of(203L), nextAssignees.get(DccControlledFileStageCodeEnum.MATRIX_APPROVAL.getCode()));
    }

    @Test
    void submitControlledFile_selectedSignoffUserInvalid_throwsBeforeInsertSnapshotAndBpm() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("V1.0");
        reqVO.setSelectedSignoffUserIds(List.of(301L));
        mockCommonSubmitDependencies();
        when(routeNodeMapper.selectListByRouteId(30L)).thenReturn(List.of(
                routeNode(1, DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode(), "Doc Control Review", "POSITION", 50L),
                routeNode(2, DccControlledFileStageCodeEnum.MATRIX_REVIEW.getCode(), "Matrix Review", "POSITION", 51L)));
        when(positionAssignmentMapper.selectActiveListByPositionId(50L)).thenReturn(List.of(
                DccPositionAssignmentDO.builder().id(60L).positionId(50L).assignmentType("POST").systemPostId(500L).active(Boolean.TRUE).build()));
        when(positionAssignmentMapper.selectActiveListByPositionId(51L)).thenReturn(List.of(
                DccPositionAssignmentDO.builder().id(61L).positionId(51L).assignmentType("POST").systemPostId(501L).active(Boolean.TRUE).build()));
        when(adminUserApi.getUserListByPostIds(List.of(500L))).thenReturn(List.of(new AdminUserRespDTO().setId(200L)));
        when(adminUserApi.getUserListByPostIds(List.of(501L))).thenReturn(List.of(new AdminUserRespDTO().setId(201L)));
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
    void submitControlledFile_rejectsDrawingSourceWithoutPdf() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("V1.0");
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
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("V1.0");
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
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("V1.0");
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
    void submitControlledFile_rejectsInvalidProductCode() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("V1.0");
        mockCommonSubmitDependencies();
        when(categoryMapper.selectById(10L)).thenReturn(DccFileCategoryDO.builder()
                .id(10L).code("SOP").name("SOP").active(Boolean.TRUE).source("LOCAL").build());
        reqVO.setProductMasterId(5000L);
        reqVO.setProductCode("not-authoritative");
        mockSingleStageRoute();
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
        assertEquals("PRJ-20260719", fileCaptor.getValue().getProductCode());
        assertEquals("验证项目", fileCaptor.getValue().getProductName());
    }

    @Test
    void submitControlledFile_dhfCategoryUsesDccProjectCodeAsProductNumber() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("V1.0");
        reqVO.setProductMasterId(null);
        reqVO.setProductCode(null);
        mockCommonSubmitDependencies();
        when(categoryMapper.selectById(10L)).thenReturn(DccFileCategoryDO.builder()
                .id(10L).code("DCC_FVM_DHF_005").name("项目策划书").active(Boolean.TRUE).source("LOCAL").build());
        mockSingleStageRoute();
        doAnswer(invocation -> {
            DccControlledFileDO file = invocation.getArgument(0);
            file.setId(906L);
            return 1;
        }).when(controlledFileMapper).insert(any(DccControlledFileDO.class));

        Long fileId = workflowService.submitControlledFile(99L, reqVO);

        assertEquals(906L, fileId);
        ArgumentCaptor<DccControlledFileDO> fileCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).insert(fileCaptor.capture());
        assertNull(fileCaptor.getValue().getProductMasterId());
        assertEquals("PRJ-20260719", fileCaptor.getValue().getProductCode());
        assertEquals("验证项目", fileCaptor.getValue().getProductName());
    }

    @Test
    void submitControlledFile_projectCodeWithMdmBindingPersistsMdmProduct() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("V1.0");
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
        mockSingleStageRoute();
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
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("V1.0");
        reqVO.setProductMasterId(null);
        reqVO.setProductCode(null);
        when(categoryMapper.selectById(10L)).thenReturn(DccFileCategoryDO.builder()
                .id(10L).code("DCC_FVM_DHF_005").name("项目策划书").active(Boolean.TRUE).source("LOCAL").build());
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

        Long fileId = workflowService.submitControlledFileWithoutApproval(99L, reqVO);

        assertEquals(901L, fileId);
        ArgumentCaptor<DccControlledFileDO> fileCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).insert(fileCaptor.capture());
        assertEquals(DccControlledFileStatusEnum.FINALIZING.getStatus(), fileCaptor.getValue().getStatus());
        assertEquals(21L, fileCaptor.getValue().getDirectoryId());
        assertEquals("V1.0", fileCaptor.getValue().getVersionNo());
        assertEquals(null, fileCaptor.getValue().getProcessDefinitionKey());
        verify(finalizationService).activateWithoutApproval(901L, true);
        verify(bpmProcessInstanceApi, never()).createProcessInstance(any(Long.class), any());
        verify(routeSnapshotMapper, never()).insert(any(DccControlledFileRouteSnapshotDO.class));
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
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("V1.0");
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
        assertEquals("PRJ-20260719", fileCaptor.getValue().getProductCode());
        assertEquals("验证项目", fileCaptor.getValue().getProductName());
        verify(finalizationService).activateWithoutApproval(906L, true);
    }

    @Test
    void submitControlledFileWithoutApproval_externalReviewProcessType_persistsKnownType() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("V1.0");
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
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("V1.0");
        reqVO.setProcessType("TEMP_FLOW");

        assertServiceException(() -> workflowService.submitControlledFileWithoutApproval(99L, reqVO),
                CONTROLLED_FILE_PROCESS_TYPE_INVALID);
        verify(controlledFileMapper, never()).insert(any(DccControlledFileDO.class));
    }

    @Test
    void submitControlledFileWithoutApproval_allowsNonLeafDirectorySelection() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("V1.0");
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
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("V1.0");
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
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("1.0");
        reqVO.setDirectoryId(20L);
        mockCommonSubmitDependencies();

        assertServiceException(() -> workflowService.submitControlledFile(99L, reqVO),
                CONTROLLED_FILE_SUBMIT_DIRECTORY_NOT_LEAF);
        verify(bpmProcessInstanceApi, never()).createProcessInstance(any(Long.class), any());
    }

    @Test
    void submitControlledFile_directoryOutsideBindingSubtree_throwsInvalidDirectory() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("1.0");
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
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("1.0");
        reqVO.setDirectoryId(20L);
        when(categoryMapper.selectById(10L)).thenReturn(DccFileCategoryDO.builder()
                .id(10L).code("SOP").name("SOP").active(Boolean.TRUE).source("LOCAL").build());
        when(categoryDirectoryBindingMapper.selectActiveByCategoryId(10L)).thenReturn(
                DccCategoryDirectoryBindingDO.builder().id(1L).categoryId(10L).directoryId(20L).active(Boolean.TRUE).build());
        when(directoryMapper.selectEnabledList()).thenReturn(List.of(directory(20L, null, "叶子目录")));
        lenient().when(routeMapper.selectLatestActiveByCategoryId(10L)).thenReturn(
                DccCategoryApprovalRouteDO.builder().id(30L).categoryId(10L).versionNo(2).active(Boolean.TRUE).effectiveTime(LocalDateTime.now()).build());
        when(routeNodeMapper.selectListByRouteId(30L)).thenReturn(List.of(
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
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("1.0");
        reqVO.setDirectoryId(900L);
        mockCommonSubmitDependencies();
        when(categoryDirectoryBindingMapper.selectActiveByCategoryId(10L)).thenReturn(null);
        when(directoryMapper.selectEnabledList()).thenReturn(List.of(
                directory(900L, 0L, "未分类", "UNCLASSIFIED"),
                directory(30L, null, "其他根目录")));
        mockSingleStageRoute();
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
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("1.0");
        when(categoryMapper.selectById(10L)).thenReturn(null);

        assertServiceException(() -> workflowService.submitControlledFile(99L, reqVO), FILE_CATEGORY_NOT_EXISTS);
    }

    @Test
    void submitControlledFile_bindingMissingAndUnclassifiedDirectoryMissing_throwsNotExists() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("1.0");
        when(categoryMapper.selectById(10L)).thenReturn(DccFileCategoryDO.builder()
                .id(10L).active(Boolean.TRUE).source("LOCAL").build());
        when(categoryDirectoryBindingMapper.selectActiveByCategoryId(10L)).thenReturn(null);
        when(directoryMapper.selectEnabledList()).thenReturn(List.of(directory(30L, null, "其他根目录")));

        assertServiceException(() -> workflowService.submitControlledFile(99L, reqVO),
                FILE_CATEGORY_UNCLASSIFIED_DIRECTORY_NOT_EXISTS);
    }

    @Test
    void submitControlledFile_routeMissing_throwsNotConfigured() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("1.0");
        mockCommonSubmitDependencies();
        when(routeMapper.selectLatestActiveByCategoryId(10L)).thenReturn(null);

        assertServiceException(() -> workflowService.submitControlledFile(99L, reqVO),
                CONTROLLED_FILE_ROUTE_NOT_CONFIGURED);
    }

    @Test
    void submitControlledFile_positionHasNoResolvedUsers_throwsApproverMissing() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("1.0");
        mockCommonSubmitDependencies();
        when(routeNodeMapper.selectListByRouteId(30L)).thenReturn(List.of(
                routeNode(1, DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode(), "Doc Control Review", "POSITION", 50L)));
        when(positionAssignmentMapper.selectActiveListByPositionId(50L)).thenReturn(List.of());

        assertServiceException(() -> workflowService.submitControlledFile(99L, reqVO),
                ROUTE_PREVIEW_APPROVER_NOT_FOUND);
        verify(bpmProcessInstanceApi, never()).createProcessInstance(any(Long.class), any());
    }

    @Test
    void submitControlledFile_userCandidateInvalid_throwsBeforeInsertSnapshotAndBpm() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("1.0");
        mockCommonSubmitDependencies();
        when(routeNodeMapper.selectListByRouteId(30L)).thenReturn(List.of(
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
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("1.0");
        mockCommonSubmitDependencies();
        when(routeNodeMapper.selectListByRouteId(30L)).thenReturn(List.of(
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
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("1.0");
        reqVO.setFileNumber(null);

        assertServiceException(() -> workflowService.submitControlledFile(99L, reqVO),
                CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING);
    }

    @Test
    void submitControlledFile_invalidVersion_throws() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("V1.A");

        assertServiceException(() -> workflowService.submitControlledFile(99L, reqVO),
                CONTROLLED_FILE_VERSION_INVALID);
    }

    @Test
    void submitControlledFile_nonIncreasingVersion_throws() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("1.0");
        mockCommonSubmitDependencies();
        lenient().when(routeNodeMapper.selectListByRouteId(30L)).thenReturn(List.of(
                routeNode(1, DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode(), "Doc Control Review", "USER", 200L)));
        when(controlledFileMasterMapper.selectListByFileNumber("SOP-001"))
                .thenReturn(List.of(DccControlledFileMasterDO.builder()
                        .id(700L).categoryId(10L).directoryId(21L)
                        .fileName("SOP-001").fileNumber("SOP-001").build()));
        when(controlledFileMapper.selectList(org.mockito.ArgumentMatchers.<SFunction<DccControlledFileDO, ?>>any(), eq(700L)))
                .thenReturn(List.of(DccControlledFileDO.builder().id(800L).masterId(700L).versionNo("1.0").build()));

        assertServiceException(() -> workflowService.submitControlledFile(99L, reqVO),
                CONTROLLED_FILE_VERSION_NOT_GREATER);
    }

    @Test
    void submitControlledFile_fileNumberConflict_throws() {
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("1.1");
        mockCommonSubmitDependencies();
        lenient().when(routeNodeMapper.selectListByRouteId(30L)).thenReturn(List.of(
                routeNode(1, DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode(), "Doc Control Review", "USER", 200L)));
        when(controlledFileMasterMapper.selectListByFileNumber("SOP-001"))
                .thenReturn(List.of(
                        DccControlledFileMasterDO.builder()
                                .id(700L).categoryId(10L).directoryId(21L)
                                .fileName("SOP-001").fileNumber("SOP-001").build(),
                        DccControlledFileMasterDO.builder()
                                .id(701L).categoryId(10L).directoryId(22L)
                                .fileName("SOP-001-副本").fileNumber("SOP-001").build()));

        assertServiceException(() -> workflowService.submitControlledFile(99L, reqVO),
                CONTROLLED_FILE_FILE_NUMBER_CONFLICT);
    }

    @Test
    void previewRoute_withoutCategoryUploadPermission_success() {
        when(categoryMapper.selectById(10L)).thenReturn(DccFileCategoryDO.builder()
                .id(10L).active(Boolean.TRUE).source("LOCAL").build());
        when(routeMapper.selectLatestActiveByCategoryId(10L)).thenReturn(
                DccCategoryApprovalRouteDO.builder().id(30L).categoryId(10L).versionNo(2).active(Boolean.TRUE).build());
        when(routeNodeMapper.selectListByRouteId(30L)).thenReturn(List.of(
                routeNode(1, DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode(), "Doc Control Review", "USER", 200L)));

        List<DccControlledFileRoutePreviewRespVO> respVOS = workflowService.previewRoute(99L, 10L);

        assertEquals(1, respVOS.size());
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
        when(routeNodeMapper.selectListByRouteId(30L)).thenReturn(List.of(
                routeNode(1, DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode(), "Doc Control Review", "POSITION", 50L)));
        when(positionAssignmentMapper.selectActiveListByPositionId(50L)).thenReturn(List.of());

        assertServiceException(() -> workflowService.previewRoute(99L, 10L), ROUTE_PREVIEW_APPROVER_NOT_FOUND);
    }

    @Test
    void previewRoute_uploaderDerivedPosition_usesSubmitterContextResolver() {
        when(categoryMapper.selectById(10L)).thenReturn(DccFileCategoryDO.builder()
                .id(10L).active(Boolean.TRUE).source("LOCAL").build());
        when(routeMapper.selectLatestActiveByCategoryId(10L)).thenReturn(
                DccCategoryApprovalRouteDO.builder().id(30L).categoryId(10L).versionNo(2).active(Boolean.TRUE).build());
        when(routeNodeMapper.selectListByRouteId(30L)).thenReturn(List.of(
                routeNode(1, DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode(), "Doc Control Review", "POSITION", 50L)));
        when(positionRuntimeResolver.isUploaderDerivedPosition(50L)).thenReturn(Boolean.TRUE);
        when(positionRuntimeResolver.resolveUserIds(50L, 99L, false)).thenReturn(List.of(300L));

        List<DccControlledFileRoutePreviewRespVO> respVOS = workflowService.previewRoute(99L, 10L);

        assertEquals(1, respVOS.size());
        assertEquals(List.of(300L), respVOS.get(0).getResolvedUserIds());
    }

    @Test
    void previewRoute_authorizedRepresentative_usesDccAssignmentInsteadOfRuntimeResolver() {
        when(categoryMapper.selectById(10L)).thenReturn(DccFileCategoryDO.builder()
                .id(10L).active(Boolean.TRUE).source("LOCAL").build());
        when(routeMapper.selectLatestActiveByCategoryId(10L)).thenReturn(
                DccCategoryApprovalRouteDO.builder().id(30L).categoryId(10L).versionNo(2).active(Boolean.TRUE).build());
        when(routeNodeMapper.selectListByRouteId(30L)).thenReturn(List.of(
                routeNode(1, DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode(), "Doc Control Review", "POSITION", 900334L)));
        when(positionRuntimeResolver.isUploaderDerivedPosition(900334L)).thenReturn(Boolean.FALSE);
        when(positionAssignmentMapper.selectActiveListByPositionId(900334L)).thenReturn(List.of(
                DccPositionAssignmentDO.builder().id(61L).positionId(900334L)
                        .assignmentType("USER").userId(301L).active(Boolean.TRUE).build()));

        List<DccControlledFileRoutePreviewRespVO> respVOS = workflowService.previewRoute(99L, 10L);

        assertEquals(1, respVOS.size());
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
        DccControlledFileSubmitReqVO reqVO = buildSubmitReqVO("V1.0");
        reqVO.setProcessType(DccControlledFileProcessTypeEnum.EXTERNAL_REVIEW.getCode());

        assertServiceException(() -> workflowService.submitControlledFile(99L, reqVO),
                EXTERNAL_FILE_REVIEW_ENDPOINT_REQUIRED);

        verify(controlledFileMapper, never()).insert(any(DccControlledFileDO.class));
        verify(bpmProcessInstanceApi, never()).createProcessInstance(any(Long.class), any());
    }

    @Test
    void deleteWithdrawnControlledFile_withdrawnOwner_deletesBusinessRevisionAndOrphanedArtifacts() throws Exception {
        when(controlledFileMapper.selectById(900L)).thenReturn(DccControlledFileDO.builder()
                .id(900L)
                .requesterId(99L)
                .processInstanceId("proc-withdrawn")
                .status(DccControlledFileStatusEnum.WITHDRAWN.getStatus())
                .sourceFileId(1000L)
                .originalFileId(1000L)
                .drawingPdfFileId(1001L)
                .build());
        when(controlledFileMapper.selectCountByReferencedFileId(1000L)).thenReturn(0L);
        when(controlledFileMapper.selectCountByReferencedFileId(1001L)).thenReturn(0L);

        workflowService.deleteWithdrawnControlledFile(99L, 900L);

        verify(controlledFileMapper).deleteById(900L);
        verify(fileService).deleteFileList(List.of(1000L, 1001L));
        verify(routeSnapshotMapper, never()).delete(
                org.mockito.ArgumentMatchers.<SFunction<DccControlledFileRouteSnapshotDO, ?>>any(), any());
        verify(bpmProcessInstanceService, never()).cancelProcessInstanceByStartUser(any(), any());
    }

    @Test
    void deleteWithdrawnControlledFile_retainsStillReferencedArtifacts() throws Exception {
        when(controlledFileMapper.selectById(900L)).thenReturn(DccControlledFileDO.builder()
                .id(900L)
                .requesterId(99L)
                .processInstanceId("proc-withdrawn")
                .status(DccControlledFileStatusEnum.WITHDRAWN.getStatus())
                .sourceFileId(1000L)
                .originalFileId(1000L)
                .drawingPdfFileId(1001L)
                .build());
        when(controlledFileMapper.selectCountByReferencedFileId(1000L)).thenReturn(1L);
        when(controlledFileMapper.selectCountByReferencedFileId(1001L)).thenReturn(0L);

        workflowService.deleteWithdrawnControlledFile(99L, 900L);

        verify(controlledFileMapper).deleteById(900L);
        verify(fileService).deleteFileList(List.of(1001L));
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
        when(controlledFileMapper.selectList(
                org.mockito.ArgumentMatchers.<SFunction<DccControlledFileDO, ?>>any(), eq(700L)))
                .thenReturn(List.of(withdrawn));
        when(routeNodeMapper.selectListByRouteId(30L)).thenReturn(List.of(
                routeNode(1, DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode(), "Doc Control Review", "POSITION", 50L)));
        when(positionAssignmentMapper.selectActiveListByPositionId(50L)).thenReturn(List.of(
                DccPositionAssignmentDO.builder().id(60L).positionId(50L).assignmentType("POST").systemPostId(500L).active(Boolean.TRUE).build()));
        when(adminUserApi.getUserListByPostIds(List.of(500L))).thenReturn(List.of(new AdminUserRespDTO().setId(200L)));
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
        assertEquals("V1.0", insertCaptor.getValue().getVersionNo());
        assertNull(insertCaptor.getValue().getProductMasterId());
        assertEquals("PRJ-20260719", insertCaptor.getValue().getProductCode());
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
    void approveTask_matrixApprovalNeedTraining_entersApplicantTrainingRecordUploadBeforeDocControlApproval() {
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
        assertEquals(DccControlledFileStatusEnum.PENDING_APPLICANT_TRAINING_RECORD.getStatus(), updateCaptor.getValue().getStatus());
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
                "UT-TRAINING", 113L, "session-training", "TRAINING_RECORD")))
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
                "UT-TRAINING", 113L, "session-training", "TRAINING_RECORD", 901L));
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

        assertServiceException(() -> workflowService.approveTask(99L, 900L, reqVO),
                CONTROLLED_FILE_STAMPED_PDF_REQUIRED);

        verify(signatureVerificationService, never()).verifyPasswordAndCreateSignature(any(), any(), any(), any(), any(), any(), any());
        verify(bpmTaskService, never()).approveTask(eq(99L), any(BpmTaskApproveReqVO.class));
    }

    @Test
    void approveTask_docControlApprovalRequiresDistributionDepartments() {
        mockTaskActionContext(900L, 99L, "task-4", "approveTask",
                DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL,
                DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL, Boolean.FALSE);
        DccControlledFileApproveTaskReqVO reqVO = new DccControlledFileApproveTaskReqVO();
        reqVO.setTaskId("task-4");
        reqVO.setPassword("secret");
        reqVO.setReason("approved");
        useStampedPdfTicket(reqVO);
        useConfirmedDirectory(reqVO);
        reqVO.setSelectedDistributionScopes(List.of());

        assertServiceException(() -> workflowService.approveTask(99L, 900L, reqVO),
                CONTROLLED_FILE_DISTRIBUTION_DEPARTMENT_REQUIRED);

        verify(signatureVerificationService, never()).verifyPasswordAndCreateSignature(any(), any(), any(), any(), any(), any(), any());
        verify(bpmTaskService, never()).approveTask(eq(99L), any(BpmTaskApproveReqVO.class));
    }

    @Test
    void approveTask_docControlApprovalRequiresTrainingRecordWhenNeedTraining() {
        mockTaskActionContext(900L, 99L, "task-4", "approveTask",
                DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL,
                DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL, Boolean.TRUE);
        DccControlledFileApproveTaskReqVO reqVO = new DccControlledFileApproveTaskReqVO();
        reqVO.setTaskId("task-4");
        reqVO.setPassword("secret");
        reqVO.setReason("approved");
        useStampedPdfTicket(reqVO);

        assertServiceException(() -> workflowService.approveTask(99L, 900L, reqVO),
                CONTROLLED_FILE_TRAINING_RECORD_REQUIRED);

        verify(signatureVerificationService, never()).verifyPasswordAndCreateSignature(any(), any(), any(), any(), any(), any(), any());
        verify(bpmTaskService, never()).approveTask(eq(99L), any(BpmTaskApproveReqVO.class));
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
        verify(bpmTaskService).approveTask(eq(99L), any(BpmTaskApproveReqVO.class));
        verify(uploadTicketService).markBound(new DccUploadTicketMarkBoundCommand(
                "UT-STAMPED", 99L, "session-stamped", "DRAWING_PDF", 900L));
    }

    @Test
    void approveTask_docControlApprovalRejectsTrainingRecordPayloadEvenWhenPersisted() {
        mockTaskActionContext(900L, 99L, "task-4", "approveTask",
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
        DccControlledFileApproveTaskReqVO reqVO = new DccControlledFileApproveTaskReqVO();
        reqVO.setTaskId("task-4");
        reqVO.setPassword("secret");
        reqVO.setReason("approved");
        useStampedPdfTicket(reqVO);
        reqVO.setTrainingRecordFileId(801L);

        assertServiceException(() -> workflowService.approveTask(99L, 900L, reqVO),
                CONTROLLED_FILE_TRAINING_RECORD_REQUIRED);

        verify(signatureVerificationService, never()).verifyPasswordAndCreateSignature(any(), any(), any(), any(), any(), any(), any());
        verify(bpmTaskService, never()).approveTask(eq(99L), any(BpmTaskApproveReqVO.class));
    }

    @Test
    void approveTask_docControlApprovalPersistsStampedPdfAndSingleFileElectronicDepartments() {
        Task currentTask = mockTaskActionContext(900L, 99L, "task-4", "approveTask",
                DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL,
                DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL, Boolean.FALSE);
        when(bpmTaskService.getRunningTaskListByProcessInstanceId("proc-1", null, null))
                .thenReturn(List.of(currentTask))
                .thenReturn(List.of());
        when(adminUserApi.getUserListByDeptIds(List.of(300L))).thenReturn(List.of(
                new AdminUserRespDTO().setId(120L).setDeptId(300L),
                new AdminUserRespDTO().setId(121L).setDeptId(300L)));
        doAnswer(invocation -> {
            DccControlledFileDistributionDO distribution = invocation.getArgument(0);
            distribution.setId(880L);
            return 1;
        }).when(distributionMapper).insert(any(DccControlledFileDistributionDO.class));
        DccControlledFileApproveTaskReqVO reqVO = new DccControlledFileApproveTaskReqVO();
        reqVO.setTaskId("task-4");
        reqVO.setPassword("secret");
        reqVO.setReason("approved");
        useStampedPdfTicket(reqVO);
        reqVO.setSelectedDistributionScopes(List.of(
                distributionScope(300L, DccDistributionMediumEnum.PUBLIC_FOLDER.getCode())));
        useConfirmedDirectory(reqVO);
        mockActionSignature(900L, "task-4", 99L, "APPROVE",
                actionSignature(1010L, "task-4", "APPROVE", "APPROVED", "DOC_CONTROL_APPROVAL_APPROVE"));

        workflowService.approveTask(99L, 900L, reqVO);

        ArgumentCaptor<DccControlledFileDistributionDO> distributionCaptor =
                ArgumentCaptor.forClass(DccControlledFileDistributionDO.class);
        verify(distributionMapper).insert(distributionCaptor.capture());
        assertEquals(900L, distributionCaptor.getValue().getControlledFileId());
        assertEquals(300L, distributionCaptor.getValue().getDepartmentId());
        assertEquals(DccDistributionMediumEnum.PUBLIC_FOLDER.getCode(), distributionCaptor.getValue().getDistributionMedium());

        ArgumentCaptor<DccControlledFileDistributionRecipientDO> recipientCaptor =
                ArgumentCaptor.forClass(DccControlledFileDistributionRecipientDO.class);
        verify(distributionRecipientMapper, org.mockito.Mockito.times(2)).insert(recipientCaptor.capture());
        assertEquals(List.of(120L, 121L),
                recipientCaptor.getAllValues().stream().map(DccControlledFileDistributionRecipientDO::getUserId).toList());
        assertEquals(List.of(distributionCaptor.getValue().getId(), distributionCaptor.getValue().getId()),
                recipientCaptor.getAllValues().stream().map(DccControlledFileDistributionRecipientDO::getDistributionId).toList());
        recipientCaptor.getAllValues().forEach(recipient -> assertNull(recipient.getMessageJobId()));
    }

    @Test
    void approveTask_docControlApprovalPersistsSelectedDistributionDepartments() {
        Task currentTask = mockTaskActionContext(900L, 99L, "task-4", "approveTask",
                DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL,
                DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL, Boolean.FALSE);
        when(bpmTaskService.getRunningTaskListByProcessInstanceId("proc-1", null, null))
                .thenReturn(List.of(currentTask))
                .thenReturn(List.of());
        when(adminUserApi.getUserListByDeptIds(List.of(300L))).thenReturn(List.of(
                new AdminUserRespDTO().setId(120L).setDeptId(300L),
                new AdminUserRespDTO().setId(121L).setDeptId(300L)));
        when(adminUserApi.getUserListByDeptIds(List.of(301L))).thenReturn(List.of(
                new AdminUserRespDTO().setId(122L).setDeptId(301L)));
        doAnswer(invocation -> {
            DccControlledFileDistributionDO distribution = invocation.getArgument(0);
            distribution.setId(distribution.getDepartmentId() + 500L);
            return 1;
        }).when(distributionMapper).insert(any(DccControlledFileDistributionDO.class));
        DccControlledFileApproveTaskReqVO reqVO = new DccControlledFileApproveTaskReqVO();
        reqVO.setTaskId("task-4");
        reqVO.setPassword("secret");
        reqVO.setReason("approved");
        useStampedPdfTicket(reqVO);
        reqVO.setSelectedDistributionScopes(List.of(
                distributionScope(300L, DccDistributionMediumEnum.PUBLIC_FOLDER.getCode()),
                distributionScope(301L, DccDistributionMediumEnum.PUBLIC_FOLDER.getCode())));
        useConfirmedDirectory(reqVO);
        mockActionSignature(900L, "task-4", 99L, "APPROVE",
                actionSignature(1010L, "task-4", "APPROVE", "APPROVED", "DOC_CONTROL_APPROVAL_APPROVE"));

        workflowService.approveTask(99L, 900L, reqVO);

        ArgumentCaptor<DccControlledFileDistributionDO> distributionCaptor =
                ArgumentCaptor.forClass(DccControlledFileDistributionDO.class);
        verify(distributionMapper, org.mockito.Mockito.times(2)).insert(distributionCaptor.capture());
        assertEquals(List.of(300L, 301L),
                distributionCaptor.getAllValues().stream().map(DccControlledFileDistributionDO::getDepartmentId).toList());
        distributionCaptor.getAllValues().forEach(distribution -> {
            assertEquals(900L, distribution.getControlledFileId());
            assertEquals(DccDistributionMediumEnum.PUBLIC_FOLDER.getCode(), distribution.getDistributionMedium());
        });

        ArgumentCaptor<DccControlledFileDistributionRecipientDO> recipientCaptor =
                ArgumentCaptor.forClass(DccControlledFileDistributionRecipientDO.class);
        verify(distributionRecipientMapper, org.mockito.Mockito.times(3)).insert(recipientCaptor.capture());
        assertEquals(List.of(120L, 121L, 122L),
                recipientCaptor.getAllValues().stream().map(DccControlledFileDistributionRecipientDO::getUserId).toList());
        assertEquals(List.of(800L, 800L, 801L),
                recipientCaptor.getAllValues().stream().map(DccControlledFileDistributionRecipientDO::getDistributionId).toList());
        recipientCaptor.getAllValues().forEach(recipient -> assertNull(recipient.getMessageJobId()));
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

        assertServiceException(() -> workflowService.approveTask(99L, 900L, reqVO),
                CONTROLLED_FILE_SUBMIT_DIRECTORY_INVALID);

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
        assertEquals(22L, fileCaptor.getValue().getDirectoryId());
        ArgumentCaptor<DccControlledFileMasterDO> masterCaptor = ArgumentCaptor.forClass(DccControlledFileMasterDO.class);
        verify(controlledFileMasterMapper).updateById(masterCaptor.capture());
        assertEquals(700L, masterCaptor.getValue().getId());
        assertEquals(22L, masterCaptor.getValue().getDirectoryId());
    }

    @Test
    void approveTask_docControlApprovalPersistsMixedDistributionScopes() {
        Task currentTask = mockTaskActionContext(900L, 99L, "task-4", "approveTask",
                DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL,
                DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL, Boolean.FALSE);
        when(bpmTaskService.getRunningTaskListByProcessInstanceId("proc-1", null, null))
                .thenReturn(List.of(currentTask))
                .thenReturn(List.of());
        when(adminUserApi.getUserListByDeptIds(List.of(300L))).thenReturn(List.of(
                new AdminUserRespDTO().setId(120L).setDeptId(300L)));
        doAnswer(invocation -> {
            DccControlledFileDistributionDO distribution = invocation.getArgument(0);
            distribution.setId(distribution.getDepartmentId() + 500L);
            return 1;
        }).when(distributionMapper).insert(any(DccControlledFileDistributionDO.class));
        DccControlledFileApproveTaskReqVO reqVO = new DccControlledFileApproveTaskReqVO();
        reqVO.setTaskId("task-4");
        reqVO.setPassword("secret");
        reqVO.setReason("approved");
        useStampedPdfTicket(reqVO);
        useConfirmedDirectory(reqVO);
        reqVO.setSelectedDistributionScopes(List.of(
                distributionScope(300L, DccDistributionMediumEnum.PUBLIC_FOLDER.getCode()),
                distributionScope(301L, DccDistributionMediumEnum.PAPER.getCode())));
        mockActionSignature(900L, "task-4", 99L, "APPROVE",
                actionSignature(1012L, "task-4", "APPROVE", "APPROVED", "DOC_CONTROL_APPROVAL_APPROVE"));

        workflowService.approveTask(99L, 900L, reqVO);

        ArgumentCaptor<DccControlledFileDistributionDO> distributionCaptor =
                ArgumentCaptor.forClass(DccControlledFileDistributionDO.class);
        verify(distributionMapper, org.mockito.Mockito.times(2)).insert(distributionCaptor.capture());
        assertEquals(List.of(300L, 301L),
                distributionCaptor.getAllValues().stream().map(DccControlledFileDistributionDO::getDepartmentId).toList());
        assertEquals(List.of(DccDistributionMediumEnum.PUBLIC_FOLDER.getCode(), DccDistributionMediumEnum.PAPER.getCode()),
                distributionCaptor.getAllValues().stream().map(DccControlledFileDistributionDO::getDistributionMedium).toList());
        ArgumentCaptor<DccControlledFileDistributionRecipientDO> recipientCaptor =
                ArgumentCaptor.forClass(DccControlledFileDistributionRecipientDO.class);
        verify(distributionRecipientMapper).insert(recipientCaptor.capture());
        assertEquals(120L, recipientCaptor.getValue().getUserId());
        assertEquals(800L, recipientCaptor.getValue().getDistributionId());
        verify(adminUserApi, never()).getUserListByDeptIds(List.of(301L));
    }

    @Test
    void approveTask_docControlApprovalRejectsInvalidDistributionMedium() {
        mockTaskActionContext(900L, 99L, "task-4", "approveTask",
                DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL,
                DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL, Boolean.FALSE);
        DccControlledFileApproveTaskReqVO reqVO = new DccControlledFileApproveTaskReqVO();
        reqVO.setTaskId("task-4");
        reqVO.setPassword("secret");
        reqVO.setReason("approved");
        useStampedPdfTicket(reqVO);
        useConfirmedDirectory(reqVO);
        reqVO.setSelectedDistributionScopes(List.of(distributionScope(300L, "EMAIL")));

        assertServiceException(() -> workflowService.approveTask(99L, 900L, reqVO),
                CONTROLLED_FILE_DISTRIBUTION_MEDIUM_INVALID);

        verify(signatureVerificationService, never()).verifyPasswordAndCreateSignature(any(), any(), any(), any(), any(), any(), any());
        verify(bpmTaskService, never()).approveTask(eq(99L), any(BpmTaskApproveReqVO.class));
    }

    @Test
    void returnTask_updatesStatusToTargetStageAndDelegatesToBpm() {
        mockTaskActionContext(900L, 99L, "task-3", "approveTask",
                DccControlledFileStatusEnum.PENDING_MATRIX_APPROVAL,
                DccControlledFileStageCodeEnum.MATRIX_APPROVAL, Boolean.FALSE);
        DccControlledFileReturnTaskReqVO reqVO = new DccControlledFileReturnTaskReqVO();
        reqVO.setTaskId("task-3");
        reqVO.setPassword("secret");
        reqVO.setTargetTaskDefinitionKey(DccControlledFileStageCodeEnum.MATRIX_REVIEW.getCode());
        reqVO.setReason("缺少会签意见");

        workflowService.returnTask(99L, 900L, reqVO);

        verify(signatureVerificationService).verifyPasswordAndCreateSignature(99L, 900L, "task-3",
                DccControlledFileStageCodeEnum.MATRIX_APPROVAL.getCode(), "RETURN", "secret", "缺少会签意见");
        ArgumentCaptor<BpmTaskReturnReqVO> bpmReqCaptor = ArgumentCaptor.forClass(BpmTaskReturnReqVO.class);
        verify(bpmTaskService).returnTask(eq(99L), bpmReqCaptor.capture());
        assertEquals("task-3", bpmReqCaptor.getValue().getId());
        assertEquals(DccControlledFileStageCodeEnum.MATRIX_REVIEW.getCode(), bpmReqCaptor.getValue().getTargetTaskDefinitionKey());
        ArgumentCaptor<DccControlledFileDO> updateCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).updateById(updateCaptor.capture());
        assertEquals(DccControlledFileStatusEnum.PENDING_MATRIX_REVIEW.getStatus(), updateCaptor.getValue().getStatus());
        assertEquals("有流程回退，需处理：缺少会签意见", updateCaptor.getValue().getRejectReason());
    }

    @Test
    void returnTask_toApplicantReworkKeepsOriginalProcessInstance() {
        mockTaskActionContext(900L, 99L, "task-3", "approveTask",
                DccControlledFileStatusEnum.PENDING_MATRIX_APPROVAL,
                DccControlledFileStageCodeEnum.MATRIX_APPROVAL, Boolean.FALSE);
        DccControlledFileReturnTaskReqVO reqVO = new DccControlledFileReturnTaskReqVO();
        reqVO.setTaskId("task-3");
        reqVO.setPassword("secret");
        reqVO.setTargetTaskDefinitionKey("APPLICANT_REWORK");
        reqVO.setReason("申请人补充源文件说明");

        workflowService.returnTask(99L, 900L, reqVO);

        verify(signatureVerificationService).verifyPasswordAndCreateSignature(99L, 900L, "task-3",
                DccControlledFileStageCodeEnum.MATRIX_APPROVAL.getCode(), "RETURN", "secret", "申请人补充源文件说明");
        ArgumentCaptor<BpmTaskReturnReqVO> bpmReqCaptor = ArgumentCaptor.forClass(BpmTaskReturnReqVO.class);
        verify(bpmTaskService).returnTask(eq(99L), bpmReqCaptor.capture());
        assertEquals("task-3", bpmReqCaptor.getValue().getId());
        assertEquals("APPLICANT_REWORK", bpmReqCaptor.getValue().getTargetTaskDefinitionKey());
        ArgumentCaptor<DccControlledFileDO> updateCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).updateById(updateCaptor.capture());
        assertEquals("PENDING_APPLICANT_REWORK", updateCaptor.getValue().getStatus());
        assertEquals("有流程回退，需处理：申请人补充源文件说明", updateCaptor.getValue().getRejectReason());
        verify(bpmProcessInstanceApi, never()).createProcessInstance(any(), any(BpmProcessInstanceCreateReqDTO.class));
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
    void transferTask_updatesStageResolvedUsersAndDelegatesToBpm() {
        mockTaskActionContext(900L, 99L, "task-2", "approveTask",
                DccControlledFileStatusEnum.PENDING_MATRIX_REVIEW,
                DccControlledFileStageCodeEnum.MATRIX_REVIEW, Boolean.FALSE);
        DccControlledFileTransferTaskReqVO reqVO = new DccControlledFileTransferTaskReqVO();
        reqVO.setTaskId("task-2");
        reqVO.setPassword("secret");
        reqVO.setAssigneeUserId(101L);
        reqVO.setReason("请代为评审");

        workflowService.transferTask(99L, 900L, reqVO);

        verify(signatureVerificationService).verifyPasswordAndCreateSignature(99L, 900L, "task-2",
                DccControlledFileStageCodeEnum.MATRIX_REVIEW.getCode(), "TRANSFER", "secret", "请代为评审");
        ArgumentCaptor<BpmTaskTransferReqVO> bpmReqCaptor = ArgumentCaptor.forClass(BpmTaskTransferReqVO.class);
        verify(bpmTaskService).transferTask(eq(99L), bpmReqCaptor.capture());
        assertEquals("task-2", bpmReqCaptor.getValue().getId());
        assertEquals(101L, bpmReqCaptor.getValue().getAssigneeUserId());
        ArgumentCaptor<DccControlledFileRouteSnapshotDO> snapshotCaptor = ArgumentCaptor.forClass(DccControlledFileRouteSnapshotDO.class);
        verify(routeSnapshotMapper).updateById(snapshotCaptor.capture());
        assertEquals("101,100", snapshotCaptor.getValue().getResolvedUserIds());
    }

    @Test
    void createSignTask_appendsResolvedUsersAndDelegatesToBpm() {
        mockTaskActionContext(900L, 99L, "task-2", "approveTask",
                DccControlledFileStatusEnum.PENDING_MATRIX_REVIEW,
                DccControlledFileStageCodeEnum.MATRIX_REVIEW, Boolean.FALSE);
        DccControlledFileCreateSignTaskReqVO reqVO = new DccControlledFileCreateSignTaskReqVO();
        reqVO.setTaskId("task-2");
        reqVO.setPassword("secret");
        reqVO.setUserIds(new java.util.LinkedHashSet<>(List.of(101L, 102L)));
        reqVO.setType("before");
        reqVO.setReason("增加工艺确认");

        workflowService.createSignTask(99L, 900L, reqVO);

        verify(signatureVerificationService).verifyPasswordAndCreateSignature(99L, 900L, "task-2",
                DccControlledFileStageCodeEnum.MATRIX_REVIEW.getCode(), "ADD_SIGN", "secret", "增加工艺确认");
        ArgumentCaptor<BpmTaskSignCreateReqVO> bpmReqCaptor = ArgumentCaptor.forClass(BpmTaskSignCreateReqVO.class);
        verify(bpmTaskService).createSignTask(eq(99L), bpmReqCaptor.capture());
        assertEquals("task-2", bpmReqCaptor.getValue().getId());
        assertEquals(new java.util.LinkedHashSet<>(List.of(101L, 102L)), bpmReqCaptor.getValue().getUserIds());
        assertEquals("before", bpmReqCaptor.getValue().getType());
        ArgumentCaptor<DccControlledFileRouteSnapshotDO> snapshotCaptor = ArgumentCaptor.forClass(DccControlledFileRouteSnapshotDO.class);
        verify(routeSnapshotMapper).updateById(snapshotCaptor.capture());
        assertEquals("99,100,101,102", snapshotCaptor.getValue().getResolvedUserIds());
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
                .id(10L).code("SOP").name("SOP").active(Boolean.TRUE).source("LOCAL").build());
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

    private void mockSingleStageRoute() {
        when(routeNodeMapper.selectListByRouteId(30L)).thenReturn(List.of(
                routeNode(1, DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode(),
                        "Doc Control Review", "POSITION", 50L)));
        when(positionAssignmentMapper.selectActiveListByPositionId(50L)).thenReturn(List.of(
                DccPositionAssignmentDO.builder().id(60L).positionId(50L).assignmentType("POST")
                        .systemPostId(500L).active(Boolean.TRUE).build()));
        when(adminUserApi.getUserListByPostIds(List.of(500L))).thenReturn(List.of(
                new AdminUserRespDTO().setId(200L)));
    }

    private void useStampedPdfTicket(DccControlledFileApproveTaskReqVO reqVO) {
        reqVO.setSessionId("session-stamped");
        reqVO.setStampedPdfUploadTicket("UT-STAMPED");
        when(uploadTicketService.resolveForBinding(new DccUploadTicketResolveCommand(
                "UT-STAMPED", 99L, "session-stamped", "DRAWING_PDF")))
                .thenReturn(new DccUploadTicketBoundFile("UT-STAMPED", 800L,
                        "controlled.pdf", "application/pdf", 128L));
    }

    private void useSingleDistributionDepartment(DccControlledFileApproveTaskReqVO reqVO) {
        reqVO.setSelectedDistributionScopes(List.of(
                distributionScope(300L, DccDistributionMediumEnum.PUBLIC_FOLDER.getCode())));
        when(adminUserApi.getUserListByDeptIds(List.of(300L))).thenReturn(List.of(
                new AdminUserRespDTO().setId(120L).setDeptId(300L)));
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
                .approveMethod("ALL")
                .approveRatio(100)
                .requireAllApprovals(Boolean.FALSE)
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
        when(signatureMapper.selectActionSignature(controlledFileId, taskId, actorId, actionType))
                .thenReturn(signature);
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
