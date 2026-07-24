package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileMetadataUpdateReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccCategoryDirectoryBindingDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccFileDirectoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccCategoryDirectoryBindingMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.directory.DccFileDirectoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum;
import cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyAdminService;
import cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyPath;
import cn.iocoder.yudao.module.dcc.service.projectcode.assignment.DccProjectCodeAssignmentAuthorization;
import cn.iocoder.yudao.module.dcc.service.projectcode.assignment.DccProjectCodeAssignmentService;
import cn.iocoder.yudao.module.dcc.service.projectcode.assignmentaudit.DccProjectCodeMetadataChangeAuditService;
import cn.iocoder.yudao.module.mdm.api.product.MdmProductApi;
import cn.iocoder.yudao.module.mdm.api.product.dto.MdmProductRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_FILE_NUMBER_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_METADATA_UPDATE_NOT_ALLOWED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PRODUCT_CODE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PRODUCT_MASTER_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SUBMIT_DIRECTORY_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_CODE_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccControlledFileMetadataUpdateServiceTest extends BaseMockitoUnitTest {

    @Mock
    private DccFileCategoryMapper categoryMapper;
    @Mock
    private DccCategoryDirectoryBindingMapper categoryDirectoryBindingMapper;
    @Mock
    private DccFileDirectoryMapper directoryMapper;
    @Mock
    private DccControlledFileMapper controlledFileMapper;
    @Mock
    private DccControlledFileMasterMapper controlledFileMasterMapper;
    @Mock
    private DccProjectCodeMapper projectCodeMapper;
    @Mock
    private PermissionApi permissionApi;
    @Mock
    private MdmProductApi productApi;
    @Mock
    private DccFileTypeTaxonomyAdminService fileTypeTaxonomyAdminService;
    @Mock
    private DccProjectCodeAssignmentService projectCodeAssignmentService;
    @Mock
    private DccProjectCodeMetadataChangeAuditService metadataChangeAuditService;
    @Mock
    private DccControlledFilePendingActionGuard pendingActionGuard;

    @InjectMocks
    private DccControlledFileMetadataUpdateServiceImpl metadataUpdateService;

    @Test
    void updateMetadata_docControlUpdatesFieldsMaintainsMasterAndDoesNotTouchWorkflow() {
        DccControlledFileMetadataUpdateReqVO reqVO = updateReq();
        DccControlledFileDO file = activeFile();
        mockDocControl();
        mockTargetCategoryAndDirectory();
        when(controlledFileMapper.selectById(900L)).thenReturn(file);
        when(controlledFileMasterMapper.selectById(700L)).thenReturn(oldMaster());
        when(controlledFileMapper.selectListByMasterId(700L)).thenReturn(List.of(file));

        metadataUpdateService.updateMetadata(99L, 900L, reqVO);

        ArgumentCaptor<DccControlledFileMasterDO> masterCaptor = ArgumentCaptor.forClass(DccControlledFileMasterDO.class);
        verify(controlledFileMasterMapper).updateById(masterCaptor.capture());
        assertEquals(700L, masterCaptor.getValue().getId());
        assertEquals(11L, masterCaptor.getValue().getCategoryId());
        assertEquals(31L, masterCaptor.getValue().getDirectoryId());
        assertEquals("NEW-SOP", masterCaptor.getValue().getFileName());
        assertEquals("DOC-NEW", masterCaptor.getValue().getFileNumber());
        assertEquals(900L, masterCaptor.getValue().getCurrentActiveControlledFileId());

        ArgumentCaptor<DccControlledFileDO> fileCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).updateById(fileCaptor.capture());
        assertEquals(900L, fileCaptor.getValue().getId());
        assertEquals(700L, fileCaptor.getValue().getMasterId());
        assertEquals(11L, fileCaptor.getValue().getCategoryId());
        assertEquals(31L, fileCaptor.getValue().getDirectoryId());
        assertEquals("NEW-SOP", fileCaptor.getValue().getFileName());
        assertEquals("NEW-SOP", fileCaptor.getValue().getTitle());
        assertEquals("DOC-NEW", fileCaptor.getValue().getFileNumber());
        assertEquals(5000L, fileCaptor.getValue().getProductMasterId());
        assertEquals("PRD20260604001", fileCaptor.getValue().getProductCode());
        assertEquals("离心泵", fileCaptor.getValue().getProductName());
        assertEquals(3000L, fileCaptor.getValue().getDccProjectCodeId());
        assertEquals(Boolean.TRUE, fileCaptor.getValue().getNeedTraining());
        assertEquals("体系文件", fileCaptor.getValue().getFileTypeLevel1());
        assertEquals("技术文件", fileCaptor.getValue().getFileTypeLevel2());
        assertEquals("设计开发", fileCaptor.getValue().getFileTypeLevel3());
        assertEquals("验证资料", fileCaptor.getValue().getFileTypeLevel4());
        assertEquals("归档件", fileCaptor.getValue().getFileTypeLevel5());
        assertEquals(null, fileCaptor.getValue().getStatus());
        assertEquals(null, fileCaptor.getValue().getProcessInstanceId());
        verify(metadataChangeAuditService).recordMetadataChange(any());
    }

    @Test
    void updateMetadata_withConfiguredFileTypeTaxonomyWritesResolvedFiveLevelPath() {
        DccControlledFileMetadataUpdateReqVO reqVO = updateReq();
        reqVO.setFileTypeTaxonomyId(8801L);
        reqVO.setFileTypeLevel1("手工一级");
        reqVO.setFileTypeLevel2("手工二级");
        reqVO.setFileTypeLevel3("手工三级");
        reqVO.setFileTypeLevel4("手工四级");
        reqVO.setFileTypeLevel5("手工五级");
        DccControlledFileDO file = activeFile();
        mockDocControl();
        mockTargetCategoryAndDirectory();
        when(fileTypeTaxonomyAdminService.resolveActivePath(8801L)).thenReturn(new DccFileTypeTaxonomyPath(
                8801L, "技术文档", "设计和开发策划阶段", "项目策划书", "草案", "归档件"));
        when(controlledFileMapper.selectById(900L)).thenReturn(file);
        when(controlledFileMasterMapper.selectById(700L)).thenReturn(oldMaster());
        when(controlledFileMapper.selectListByMasterId(700L)).thenReturn(List.of(file));

        metadataUpdateService.updateMetadata(99L, 900L, reqVO);

        ArgumentCaptor<DccControlledFileDO> fileCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).updateById(fileCaptor.capture());
        assertEquals(8801L, fileCaptor.getValue().getFileTypeTaxonomyId());
        assertEquals("技术文档", fileCaptor.getValue().getFileTypeLevel1());
        assertEquals("设计和开发策划阶段", fileCaptor.getValue().getFileTypeLevel2());
        assertEquals("项目策划书", fileCaptor.getValue().getFileTypeLevel3());
        assertEquals("草案", fileCaptor.getValue().getFileTypeLevel4());
        assertEquals("归档件", fileCaptor.getValue().getFileTypeLevel5());
    }

    @Test
    void updateMetadata_assignedUserUpdatesAssignedFileAndWritesAudit() {
        DccControlledFileMetadataUpdateReqVO reqVO = updateReq();
        reqVO.setAssignmentId(9100L);
        reqVO.setChangeReason("修正 AI 分类和产品参数");
        DccControlledFileDO file = activeFile();
        when(permissionApi.hasAnyRoles(123L, "doc_control"))
                .thenReturn(false);
        when(projectCodeAssignmentService.assertMetadataUpdateAllowed(123L, 900L, 9100L))
                .thenReturn(DccProjectCodeAssignmentAuthorization.assignedUser(9100L, 3000L));
        when(productApi.getEnabledDccProduct(5000L)).thenReturn(defaultProductMaster());
        when(projectCodeMapper.selectById(3000L)).thenReturn(
                cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO.builder()
                        .id(3000L)
                        .projectName("按压式Y型连接器")
                        .projectCode("YCKPR")
                        .status("ENABLE")
                        .build());
        mockTargetCategoryAndDirectory();
        when(controlledFileMapper.selectById(900L)).thenReturn(file);
        when(controlledFileMasterMapper.selectById(700L)).thenReturn(oldMaster());
        when(controlledFileMapper.selectListByMasterId(700L)).thenReturn(List.of(file));

        metadataUpdateService.updateMetadata(123L, 900L, reqVO);

        verify(projectCodeAssignmentService).assertMetadataUpdateAllowed(123L, 900L, 9100L);
        verify(controlledFileMapper).updateById(any(DccControlledFileDO.class));
        verify(metadataChangeAuditService).recordMetadataChange(any());
    }

    @Test
    void updateMetadata_pendingObsoleteActionFailsBeforeWritingMetadata() {
        DccControlledFileMetadataUpdateReqVO reqVO = updateReq();
        DccControlledFileDO file = activeFile();
        mockDocControl();
        when(controlledFileMapper.selectById(900L)).thenReturn(file);
        doThrow(new IllegalStateException("controlled file is locked by active form action"))
                .when(pendingActionGuard).assertNoPendingBusinessAction(file);

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> metadataUpdateService.updateMetadata(99L, 900L, reqVO));

        verify(pendingActionGuard).assertNoPendingBusinessAction(file);
        verify(controlledFileMasterMapper, never()).selectById(700L);
        verify(controlledFileMasterMapper, never()).updateById(any(DccControlledFileMasterDO.class));
        verify(controlledFileMapper, never()).updateById(any(DccControlledFileDO.class));
        verify(metadataChangeAuditService, never()).recordMetadataChange(any());
    }

    @Test
    void updateMetadata_invalidProjectCodeFailsBeforeUpdatingFile() {
        DccControlledFileMetadataUpdateReqVO reqVO = updateReq();
        when(permissionApi.hasAnyRoles(99L, "doc_control"))
                .thenReturn(true);
        when(projectCodeMapper.selectById(3000L)).thenReturn(null);

        assertServiceException(() -> metadataUpdateService.updateMetadata(99L, 900L, reqVO),
                PROJECT_CODE_NOT_EXISTS);

        verify(controlledFileMapper, never()).selectById(900L);
        verify(controlledFileMapper, never()).updateById(any(DccControlledFileDO.class));
    }

    @Test
    void updateMetadata_allowsClearingProductBinding() {
        DccControlledFileMetadataUpdateReqVO reqVO = updateReq();
        reqVO.setProductMasterId(null);
        reqVO.setProductName(null);
        reqVO.setProductCode(null);
        reqVO.setDccProjectCodeId(null);
        DccControlledFileDO file = activeFile();
        when(permissionApi.hasAnyRoles(99L, "doc_control"))
                .thenReturn(true);
        mockTargetCategoryAndDirectory();
        when(controlledFileMapper.selectById(900L)).thenReturn(file);
        when(controlledFileMasterMapper.selectById(700L)).thenReturn(oldMaster());
        when(controlledFileMapper.selectListByMasterId(700L)).thenReturn(List.of(file));

        metadataUpdateService.updateMetadata(99L, 900L, reqVO);

        ArgumentCaptor<DccControlledFileDO> fileCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).updateById(fileCaptor.capture());
        assertNull(fileCaptor.getValue().getProductMasterId());
        assertNull(fileCaptor.getValue().getProductCode());
        assertNull(fileCaptor.getValue().getProductName());
        verify(productApi, never()).getEnabledDccProduct(any());
    }

    @Test
    void updateMetadata_allowsBlankFileNumber() {
        DccControlledFileMetadataUpdateReqVO reqVO = updateReq();
        reqVO.setFileNumber("   ");
        DccControlledFileDO file = activeFile();
        mockDocControl();
        mockTargetCategoryAndDirectory();
        when(controlledFileMapper.selectById(900L)).thenReturn(file);
        when(controlledFileMasterMapper.selectById(700L)).thenReturn(oldMaster());
        when(controlledFileMapper.selectListByMasterId(700L)).thenReturn(List.of(file));

        metadataUpdateService.updateMetadata(99L, 900L, reqVO);

        ArgumentCaptor<DccControlledFileMasterDO> masterCaptor = ArgumentCaptor.forClass(DccControlledFileMasterDO.class);
        verify(controlledFileMasterMapper).updateById(masterCaptor.capture());
        assertEquals("", masterCaptor.getValue().getFileNumber());

        ArgumentCaptor<DccControlledFileDO> fileCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).updateById(fileCaptor.capture());
        assertEquals("", fileCaptor.getValue().getFileNumber());
    }

    @Test
    void updateMetadata_pendingApprovalFileFailsFastWithoutUpdatingMasterOrFile() {
        DccControlledFileDO file = activeFile();
        file.setStatus(DccControlledFileStatusEnum.PENDING_MATRIX_REVIEW.getStatus());
        mockDocControl();
        when(controlledFileMapper.selectById(900L)).thenReturn(file);

        assertServiceException(() -> metadataUpdateService.updateMetadata(99L, 900L, updateReq()),
                CONTROLLED_FILE_METADATA_UPDATE_NOT_ALLOWED);

        verify(controlledFileMasterMapper, never()).updateById(any(DccControlledFileMasterDO.class));
        verify(controlledFileMapper, never()).updateById(any(DccControlledFileDO.class));
        verify(metadataChangeAuditService, never()).recordMetadataChange(any());
    }

    @Test
    void updateMetadata_terminalWithdrawnFileFailsFastWithoutUpdatingMasterOrFile() {
        DccControlledFileDO file = activeFile();
        file.setStatus(DccControlledFileStatusEnum.WITHDRAWN.getStatus());
        mockDocControl();
        when(controlledFileMapper.selectById(900L)).thenReturn(file);

        assertServiceException(() -> metadataUpdateService.updateMetadata(99L, 900L, updateReq()),
                CONTROLLED_FILE_METADATA_UPDATE_NOT_ALLOWED);

        verify(controlledFileMasterMapper, never()).updateById(any(DccControlledFileMasterDO.class));
        verify(controlledFileMapper, never()).updateById(any(DccControlledFileDO.class));
        verify(metadataChangeAuditService, never()).recordMetadataChange(any());
    }

    @Test
    void updateMetadata_superAdminWithoutDocControlFailsBeforeReadingFile() {
        when(permissionApi.hasAnyRoles(99L, "doc_control"))
                .thenReturn(false);

        assertServiceException(() -> metadataUpdateService.updateMetadata(99L, 900L, updateReq()),
                CONTROLLED_FILE_METADATA_UPDATE_NOT_ALLOWED);

        verify(projectCodeAssignmentService, never()).assertMetadataUpdateAllowed(any(), any(), any());
        verify(controlledFileMapper, never()).selectById(900L);
        verify(controlledFileMapper, never()).updateById(any(DccControlledFileDO.class));
    }

    @Test
    void updateMetadata_nonDocControlRoleFailsBeforeReadingFile() {
        when(permissionApi.hasAnyRoles(99L, "doc_control"))
                .thenReturn(false);

        assertServiceException(() -> metadataUpdateService.updateMetadata(99L, 900L, updateReq()),
                CONTROLLED_FILE_METADATA_UPDATE_NOT_ALLOWED);

        verify(projectCodeAssignmentService, never()).assertMetadataUpdateAllowed(any(), any(), any());
        verify(controlledFileMapper, never()).selectById(900L);
        verify(controlledFileMapper, never()).updateById(any(DccControlledFileDO.class));
    }

    @Test
    void updateMetadata_productCodeMustKeepExistingFourteenAlnumRule() {
        DccControlledFileMetadataUpdateReqVO reqVO = updateReq();
        mockDocControl();
        when(productApi.getEnabledDccProduct(5000L)).thenReturn(MdmProductRespDTO.builder()
                .id(5000L)
                .productCode("PMD-001")
                .dccProductCode("bad-code")
                .nameCn("离心泵")
                .status("ENABLE")
                .build());

        assertServiceException(() -> metadataUpdateService.updateMetadata(99L, 900L, reqVO),
                CONTROLLED_FILE_PRODUCT_MASTER_INVALID);

        verify(controlledFileMapper, never()).selectById(900L);
        verify(controlledFileMapper, never()).updateById(any(DccControlledFileDO.class));
    }

    @Test
    void updateMetadata_directoryMustStayInsideCategoryBindingScope() {
        DccControlledFileMetadataUpdateReqVO reqVO = updateReq();
        reqVO.setDirectoryId(99L);
        mockDocControl();
        when(controlledFileMapper.selectById(900L)).thenReturn(activeFile());
        when(categoryMapper.selectById(11L)).thenReturn(DccFileCategoryDO.builder().id(11L).active(Boolean.TRUE).build());
        when(categoryDirectoryBindingMapper.selectActiveByCategoryId(11L)).thenReturn(
                DccCategoryDirectoryBindingDO.builder().id(1L).categoryId(11L).directoryId(30L).active(Boolean.TRUE).build());
        when(directoryMapper.selectEnabledList()).thenReturn(List.of(
                directory(30L, null),
                directory(31L, 30L),
                directory(99L, null)));

        assertServiceException(() -> metadataUpdateService.updateMetadata(99L, 900L, reqVO),
                CONTROLLED_FILE_SUBMIT_DIRECTORY_INVALID);

        verify(controlledFileMapper, never()).updateById(any(DccControlledFileDO.class));
    }

    @Test
    void updateMetadata_conflictingTargetMasterFailsInsteadOfMergingChains() {
        DccControlledFileMetadataUpdateReqVO reqVO = updateReq();
        mockDocControl();
        mockTargetCategoryAndDirectory();
        when(controlledFileMapper.selectById(900L)).thenReturn(activeFile());
        when(controlledFileMasterMapper.selectById(700L)).thenReturn(oldMaster());
        when(controlledFileMapper.selectListByMasterId(700L)).thenReturn(List.of(activeFile()));
        when(controlledFileMasterMapper.selectByCategoryIdAndDirectoryIdAndFileName(11L, 31L, "NEW-SOP")).thenReturn(
                DccControlledFileMasterDO.builder()
                        .id(800L)
                        .categoryId(11L)
                        .directoryId(31L)
                        .fileName("NEW-SOP")
                        .fileNumber("OTHER-NUMBER")
                        .currentActiveControlledFileId(901L)
                        .build());

        assertServiceException(() -> metadataUpdateService.updateMetadata(99L, 900L, reqVO),
                CONTROLLED_FILE_FILE_NUMBER_CONFLICT);

        verify(controlledFileMapper, never()).updateById(any(DccControlledFileDO.class));
    }

    private void mockDocControl() {
        when(permissionApi.hasAnyRoles(99L, "doc_control"))
                .thenReturn(true);
        when(productApi.getEnabledDccProduct(5000L)).thenReturn(defaultProductMaster());
        when(projectCodeMapper.selectById(3000L)).thenReturn(
                cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO.builder()
                        .id(3000L)
                        .projectName("按压式Y型连接器")
                        .projectCode("YCKPR")
                        .status("ENABLE")
                        .build());
    }

    private void mockTargetCategoryAndDirectory() {
        when(categoryMapper.selectById(11L)).thenReturn(DccFileCategoryDO.builder().id(11L).active(Boolean.TRUE).build());
        when(categoryDirectoryBindingMapper.selectActiveByCategoryId(11L)).thenReturn(
                DccCategoryDirectoryBindingDO.builder().id(1L).categoryId(11L).directoryId(30L).active(Boolean.TRUE).build());
        when(directoryMapper.selectEnabledList()).thenReturn(List.of(
                directory(30L, null),
                directory(31L, 30L)));
    }

    private DccControlledFileDO activeFile() {
        return DccControlledFileDO.builder()
                .id(900L)
                .masterId(700L)
                .categoryId(10L)
                .directoryId(21L)
                .fileName("OLD-SOP")
                .title("OLD-SOP")
                .fileNumber("DOC-OLD")
                .productCode("PRD20260525001")
                .versionNo("1.0")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .processInstanceId("proc-1")
                .processDefinitionKey(DccControlledFileWorkflowServiceImpl.BPM_PROCESS_DEFINITION_KEY)
                .build();
    }

    private DccControlledFileMasterDO oldMaster() {
        return DccControlledFileMasterDO.builder()
                .id(700L)
                .categoryId(10L)
                .directoryId(21L)
                .fileName("OLD-SOP")
                .fileNumber("DOC-OLD")
                .currentActiveControlledFileId(900L)
                .build();
    }

    private DccControlledFileMetadataUpdateReqVO updateReq() {
        DccControlledFileMetadataUpdateReqVO reqVO = new DccControlledFileMetadataUpdateReqVO();
        reqVO.setProductMasterId(5000L);
        reqVO.setProductName("离心泵");
        reqVO.setFileName("NEW-SOP");
        reqVO.setProductCode("PRD20260604001");
        reqVO.setFileNumber("DOC-NEW");
        reqVO.setCategoryId(11L);
        reqVO.setDirectoryId(31L);
        reqVO.setDccProjectCodeId(3000L);
        reqVO.setNeedTraining(Boolean.TRUE);
        reqVO.setFileTypeLevel1(" 体系文件 ");
        reqVO.setFileTypeLevel2("技术文件");
        reqVO.setFileTypeLevel3("设计开发");
        reqVO.setFileTypeLevel4("验证资料");
        reqVO.setFileTypeLevel5("归档件");
        return reqVO;
    }

    private MdmProductRespDTO defaultProductMaster() {
        return MdmProductRespDTO.builder()
                .id(5000L)
                .productCode("PMD-001")
                .dccProductCode("PRD20260604001")
                .nameCn("离心泵")
                .status("ENABLE")
                .build();
    }

    private DccFileDirectoryDO directory(Long id, Long parentId) {
        return DccFileDirectoryDO.builder()
                .id(id)
                .parentId(parentId)
                .code("DIR-" + id)
                .name("目录-" + id)
                .active(Boolean.TRUE)
                .sort(1)
                .build();
    }
}
