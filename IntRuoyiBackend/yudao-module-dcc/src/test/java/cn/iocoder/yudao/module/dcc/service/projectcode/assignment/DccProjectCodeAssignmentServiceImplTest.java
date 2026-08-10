package cn.iocoder.yudao.module.dcc.service.projectcode.assignment;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.assignment.DccProjectCodeAssignmentCreateReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.assignment.DccProjectCodeAssignmentRevokeReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeAssignmentDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeAssignmentFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeAssignmentFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeAssignmentMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.DccProjectCodeAssignmentConstants.SCOPE_SELECTED_FILES;
import static cn.iocoder.yudao.module.dcc.enums.DccProjectCodeAssignmentConstants.STATUS_ACTIVE;
import static cn.iocoder.yudao.module.dcc.enums.DccProjectCodeAssignmentConstants.STATUS_REVOKED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_METADATA_UPDATE_NOT_ALLOWED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_CODE_ASSIGNMENT_ASSIGNEE_PERMISSION_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_CODE_ASSIGNMENT_FILE_SCOPE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_CODE_ASSIGNMENT_INACTIVE;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_CODE_ASSIGNMENT_REVOKE_NOT_ALLOWED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccProjectCodeAssignmentServiceImplTest extends BaseMockitoUnitTest {

    @Mock
    private DccProjectCodeMapper projectCodeMapper;
    @Mock
    private DccControlledFileMapper controlledFileMapper;
    @Mock
    private DccProjectCodeAssignmentMapper assignmentMapper;
    @Mock
    private DccProjectCodeAssignmentFileMapper assignmentFileMapper;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private PermissionApi permissionApi;

    @InjectMocks
    private DccProjectCodeAssignmentServiceImpl assignmentService;

    @Test
    void createAssignment_selectedFilesCreatesSnapshotForOnlySelectedFiles() {
        when(projectCodeMapper.selectById(3000L)).thenReturn(projectCode());
        when(adminUserApi.getUser(123L)).thenReturn(enabledUser(123L));
        when(permissionApi.hasAnyPermissions(123L, "dcc:project-code-assignment:execute")).thenReturn(true);
        DccControlledFileDO selectedFile = controlledFile(900L);
        when(controlledFileMapper.selectCurrentApprovedFilesByIds(List.of(900L)))
                .thenReturn(List.of(selectedFile));
        doAnswer(invocation -> {
            DccProjectCodeAssignmentDO assignment = invocation.getArgument(0);
            assignment.setId(9100L);
            return 1;
        }).when(assignmentMapper).insert(any(DccProjectCodeAssignmentDO.class));
        when(adminUserApi.getUserList(List.of(123L))).thenReturn(List.of(enabledUser(123L)));

        var resp = assignmentService.createAssignment(99L, 3000L, selectedReq(List.of(900L)));

        assertEquals(9100L, resp.getId());
        assertEquals(1, resp.getFileCount());
        ArgumentCaptor<DccProjectCodeAssignmentDO> assignmentCaptor =
                ArgumentCaptor.forClass(DccProjectCodeAssignmentDO.class);
        verify(assignmentMapper).insert(assignmentCaptor.capture());
        assertEquals(SCOPE_SELECTED_FILES, assignmentCaptor.getValue().getScopeMode());
        ArgumentCaptor<DccProjectCodeAssignmentFileDO> fileCaptor =
                ArgumentCaptor.forClass(DccProjectCodeAssignmentFileDO.class);
        verify(assignmentFileMapper).insert(fileCaptor.capture());
        assertEquals(9100L, fileCaptor.getValue().getAssignmentId());
        assertEquals(900L, fileCaptor.getValue().getControlledFileId());
        assertEquals("DOC-900", fileCaptor.getValue().getFileNumberSnapshot());
    }

    @Test
    void createAssignment_selectedFilesAllowsFileOutsideTargetProjectScope() {
        when(projectCodeMapper.selectById(3000L)).thenReturn(projectCode());
        when(adminUserApi.getUser(123L)).thenReturn(enabledUser(123L));
        when(permissionApi.hasAnyPermissions(123L, "dcc:project-code-assignment:execute")).thenReturn(true);
        DccControlledFileDO externalProjectFile = controlledFile(901L);
        externalProjectFile.setDccProjectCodeId(129L);
        when(controlledFileMapper.selectCurrentApprovedFilesByIds(List.of(901L)))
                .thenReturn(List.of(externalProjectFile));
        doAnswer(invocation -> {
            DccProjectCodeAssignmentDO assignment = invocation.getArgument(0);
            assignment.setId(9101L);
            return 1;
        }).when(assignmentMapper).insert(any(DccProjectCodeAssignmentDO.class));
        when(adminUserApi.getUserList(List.of(123L))).thenReturn(List.of(enabledUser(123L)));

        var resp = assignmentService.createAssignment(99L, 3000L, selectedReq(List.of(901L)));

        assertEquals(9101L, resp.getId());
        ArgumentCaptor<DccProjectCodeAssignmentFileDO> fileCaptor =
                ArgumentCaptor.forClass(DccProjectCodeAssignmentFileDO.class);
        verify(assignmentFileMapper).insert(fileCaptor.capture());
        assertEquals(3000L, fileCaptor.getValue().getProjectCodeId());
        assertEquals(901L, fileCaptor.getValue().getControlledFileId());
    }

    @Test
    void createAssignment_selectedFilesRejectsOutOfScopeFileIds() {
        when(projectCodeMapper.selectById(3000L)).thenReturn(projectCode());
        when(adminUserApi.getUser(123L)).thenReturn(enabledUser(123L));
        when(permissionApi.hasAnyPermissions(123L, "dcc:project-code-assignment:execute")).thenReturn(true);
        when(controlledFileMapper.selectCurrentApprovedFilesByIds(List.of(900L, 901L)))
                .thenReturn(List.of(controlledFile(900L)));

        assertServiceException(() -> assignmentService.createAssignment(99L, 3000L, selectedReq(List.of(900L, 901L))),
                PROJECT_CODE_ASSIGNMENT_FILE_SCOPE_INVALID);

        verify(assignmentMapper, never()).insert(any(DccProjectCodeAssignmentDO.class));
        verify(assignmentFileMapper, never()).insert(any(DccProjectCodeAssignmentFileDO.class));
    }

    @Test
    void createAssignment_rejectsAssigneeWithoutExecutePermission() {
        when(projectCodeMapper.selectById(3000L)).thenReturn(projectCode());
        when(adminUserApi.getUser(123L)).thenReturn(enabledUser(123L));
        when(permissionApi.hasAnyPermissions(123L, "dcc:project-code-assignment:execute")).thenReturn(false);

        assertServiceException(() -> assignmentService.createAssignment(99L, 3000L, selectedReq(List.of(900L))),
                PROJECT_CODE_ASSIGNMENT_ASSIGNEE_PERMISSION_MISSING);

        verify(assignmentMapper, never()).insert(any(DccProjectCodeAssignmentDO.class));
        verify(assignmentFileMapper, never()).insert(any(DccProjectCodeAssignmentFileDO.class));
    }

    @Test
    void assertMetadataUpdateAllowed_rejectsOtherUserExpiredAndOutOfScopeFile() {
        when(assignmentMapper.selectById(9100L)).thenReturn(activeAssignment().build());

        assertServiceException(() -> assignmentService.assertMetadataUpdateAllowed(456L, 900L, 9100L),
                CONTROLLED_FILE_METADATA_UPDATE_NOT_ALLOWED);

        when(assignmentMapper.selectById(9101L)).thenReturn(activeAssignment().expireTime(LocalDateTime.now().minusMinutes(1)).build());
        assertServiceException(() -> assignmentService.assertMetadataUpdateAllowed(123L, 900L, 9101L),
                PROJECT_CODE_ASSIGNMENT_INACTIVE);

        when(assignmentMapper.selectById(9102L)).thenReturn(activeAssignment().id(9102L).build());
        assertServiceException(() -> assignmentService.assertMetadataUpdateAllowed(123L, 901L, 9102L),
                PROJECT_CODE_ASSIGNMENT_FILE_SCOPE_INVALID);
    }

    @Test
    void assertMetadataUpdateAllowed_resolvesLatestApprovedFileByMasterIdentity() {
        DccControlledFileDO latestFile = controlledFile(901L, 700L, "ACTIVE");
        when(assignmentMapper.selectById(9100L)).thenReturn(activeAssignment().build());
        when(controlledFileMapper.selectById(901L)).thenReturn(latestFile);
        when(controlledFileMapper.selectLatestApprovedByMasterId(700L)).thenReturn(latestFile);
        when(assignmentFileMapper.selectByAssignmentIdAndMasterId(9100L, 700L))
                .thenReturn(assignmentFile(8000L, 9100L, 900L, 700L));

        DccProjectCodeAssignmentAuthorization authorization =
                assignmentService.assertMetadataUpdateAllowed(123L, 901L, 9100L);

        assertEquals(9100L, authorization.assignmentId());
        assertEquals(3000L, authorization.projectCodeId());
        verify(assignmentFileMapper).selectByAssignmentIdAndMasterId(9100L, 700L);
    }

    @Test
    void assertMetadataUpdateAllowed_rejectsOldRevisionWhenNewApprovedFileExists() {
        DccControlledFileDO oldFile = controlledFile(900L, 700L, "SUPERSEDED");
        DccControlledFileDO latestFile = controlledFile(901L, 700L, "ACTIVE");
        when(assignmentMapper.selectById(9100L)).thenReturn(activeAssignment().build());
        when(controlledFileMapper.selectById(900L)).thenReturn(oldFile);
        when(controlledFileMapper.selectLatestApprovedByMasterId(700L)).thenReturn(latestFile);

        assertServiceException(() -> assignmentService.assertMetadataUpdateAllowed(123L, 900L, 9100L),
                PROJECT_CODE_ASSIGNMENT_FILE_SCOPE_INVALID);
    }

    @Test
    void markAssignmentFileChanged_updatesAssignmentSnapshotByLatestFileMasterIdentity() {
        DccControlledFileDO latestFile = controlledFile(901L, 700L, "ACTIVE");
        when(controlledFileMapper.selectById(901L)).thenReturn(latestFile);
        when(controlledFileMapper.selectLatestApprovedByMasterId(700L)).thenReturn(latestFile);
        when(assignmentFileMapper.selectByAssignmentIdAndMasterId(9100L, 700L))
                .thenReturn(assignmentFile(8000L, 9100L, 900L, 700L));
        when(assignmentMapper.selectById(9100L)).thenReturn(activeAssignment().build());

        assignmentService.markAssignmentFileChanged(9100L, 901L, 2);

        ArgumentCaptor<DccProjectCodeAssignmentFileDO> fileCaptor =
                ArgumentCaptor.forClass(DccProjectCodeAssignmentFileDO.class);
        verify(assignmentFileMapper).updateById(fileCaptor.capture());
        assertEquals(8000L, fileCaptor.getValue().getId());
        assertEquals(2, fileCaptor.getValue().getChangedFieldCount());
    }

    @Test
    void getAssignmentFilePage_rejectsUnreadableAssignmentForNonDocControlUser() {
        when(assignmentMapper.selectById(9100L)).thenReturn(activeAssignment().assigneeUserId(123L).build());
        when(permissionApi.hasAnyRoles(456L, "doc_control")).thenReturn(false);

        assertServiceException(() -> assignmentService.getAssignmentFilePage(456L, 9100L,
                new cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.assignment.DccProjectCodeAssignmentFilePageReqVO()),
                CONTROLLED_FILE_METADATA_UPDATE_NOT_ALLOWED);
    }

    @Test
    void getAssignmentFilePage_resolvesLatestApprovedFileByMasterIdentityForDisplay() {
        DccControlledFileDO latestFile = controlledFile(901L, 700L, "ACTIVE");
        var reqVO = new cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.assignment.DccProjectCodeAssignmentFilePageReqVO();
        when(assignmentMapper.selectById(9100L)).thenReturn(activeAssignment().build());
        when(permissionApi.hasAnyRoles(99L, "doc_control")).thenReturn(true);
        when(assignmentFileMapper.selectPage(9100L, reqVO)).thenReturn(new PageResult<>(
                List.of(assignmentFile(8000L, 9100L, 900L, 700L)), 1L));
        when(controlledFileMapper.selectLatestApprovedByMasterId(700L)).thenReturn(latestFile);

        var pageResult = assignmentService.getAssignmentFilePage(99L, 9100L, reqVO);

        assertEquals(1, pageResult.getList().size());
        assertEquals(901L, pageResult.getList().get(0).getId());
        assertEquals("DOC-901", pageResult.getList().get(0).getFileNumber());
        assertEquals(9100L, pageResult.getList().get(0).getMetadataEditAssignmentId());
        verify(controlledFileMapper).selectLatestApprovedByMasterId(700L);
    }

    @Test
    void revokeAssignment_rejectsInactiveAssignment() {
        when(assignmentMapper.selectById(9100L)).thenReturn(activeAssignment().status(STATUS_REVOKED).build());
        DccProjectCodeAssignmentRevokeReqVO reqVO = new DccProjectCodeAssignmentRevokeReqVO();
        reqVO.setRevokeReason("重新分配");

        assertServiceException(() -> assignmentService.revokeAssignment(99L, 9100L, reqVO),
                PROJECT_CODE_ASSIGNMENT_REVOKE_NOT_ALLOWED);
    }

    private DccProjectCodeAssignmentCreateReqVO selectedReq(List<Long> fileIds) {
        DccProjectCodeAssignmentCreateReqVO reqVO = new DccProjectCodeAssignmentCreateReqVO();
        reqVO.setAssigneeUserId(123L);
        reqVO.setScopeMode(SCOPE_SELECTED_FILES);
        reqVO.setFileIds(fileIds);
        reqVO.setAssignmentReason("修正分类");
        return reqVO;
    }

    private DccProjectCodeDO projectCode() {
        return DccProjectCodeDO.builder()
                .id(3000L)
                .projectName("PTC")
                .projectCode("PTCABC")
                .build();
    }

    private DccControlledFileDO controlledFile(Long id) {
        return controlledFile(id, 700L, "ACTIVE");
    }

    private DccControlledFileDO controlledFile(Long id, Long masterId, String status) {
        return DccControlledFileDO.builder()
                .id(id)
                .masterId(masterId)
                .fileName("FILE-" + id)
                .fileNumber("DOC-" + id)
                .categoryId(11L)
                .directoryId(31L)
                .status(status)
                .fileTypeLevel1("技术文件")
                .fileTypeLevel2("DMR")
                .fileTypeLevel3("生产记录")
                .build();
    }

    private DccProjectCodeAssignmentFileDO assignmentFile(Long id, Long assignmentId, Long controlledFileId,
                                                          Long masterId) {
        return DccProjectCodeAssignmentFileDO.builder()
                .id(id)
                .assignmentId(assignmentId)
                .projectCodeId(3000L)
                .controlledFileId(controlledFileId)
                .masterId(masterId)
                .changed(Boolean.FALSE)
                .changedFieldCount(0)
                .build();
    }

    private DccProjectCodeAssignmentDO.DccProjectCodeAssignmentDOBuilder activeAssignment() {
        return DccProjectCodeAssignmentDO.builder()
                .id(9100L)
                .projectCodeId(3000L)
                .assigneeUserId(123L)
                .status(STATUS_ACTIVE)
                .expireTime(LocalDateTime.now().plusDays(1));
    }

    private AdminUserRespDTO enabledUser(Long id) {
        return new AdminUserRespDTO()
                .setId(id)
                .setNickname("用户" + id)
                .setStatus(CommonStatusEnum.ENABLE.getStatus());
    }
}
