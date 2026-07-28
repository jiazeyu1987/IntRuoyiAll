package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileUploadNameOptionRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileAccessLogMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionRecipientMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileRouteSnapshotMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSignatureMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTrainingAssignmentMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTrainingMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTrainingProgressMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileProcessTypeEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum;
import cn.iocoder.yudao.module.dcc.enums.DccProjectCodeStatusConstants;
import cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyAdminService;
import cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyPath;
import cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryAccessPermissionService;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePageReqVO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_TYPE_TAXONOMY_LEVEL_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_CODE_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccControlledFileUploadNameOptionQueryServiceTest extends BaseMockitoUnitTest {

    @Mock
    private DccControlledFileMapper controlledFileMapper;
    @Mock
    private DccControlledFileMasterMapper controlledFileMasterMapper;
    @Mock
    private DccProjectCodeMapper projectCodeMapper;
    @Mock
    private DccFileCategoryMapper categoryMapper;
    @Mock
    private DccFileTypeTaxonomyAdminService fileTypeTaxonomyAdminService;
    @Mock
    private DccControlledFileRouteSnapshotMapper routeSnapshotMapper;
    @Mock
    private DccControlledFileDistributionMapper distributionMapper;
    @Mock
    private DccControlledFileDistributionRecipientMapper distributionRecipientMapper;
    @Mock
    private DccControlledFileTrainingMapper trainingMapper;
    @Mock
    private DccControlledFileTrainingAssignmentMapper trainingAssignmentMapper;
    @Mock
    private DccControlledFileTrainingProgressMapper trainingProgressMapper;
    @Mock
    private DccControlledFileSignatureMapper signatureMapper;
    @Mock
    private DccControlledFileAccessLogMapper accessLogMapper;
    @Mock
    private DccControlledFileCategoryPermissionSupport permissionSupport;
    @Mock
    private DccDirectoryAccessPermissionService directoryAccessPermissionService;
    @Mock
    private FileMapper fileMapper;
    @Mock
    private FileService fileService;
    @Mock
    private DccControlledPreviewWatermarkService watermarkService;

    @InjectMocks
    private DccControlledFileQueryServiceImpl queryService;

    @Test
    void listUploadNameOptions_returnsProjectTaxonomyActiveFiles() {
        mockEnabledProjectAndTaxonomy();
        when(controlledFileMapper.selectWorkflowList(any(DccControlledFilePageReqVO.class))).thenReturn(List.of(
                DccControlledFileDO.builder()
                        .id(902L)
                        .fileName("WI-002")
                        .fileNumber("WI-002-NO")
                        .versionNo("V2.0")
                        .build(),
                DccControlledFileDO.builder()
                        .id(901L)
                        .fileName("SOP-001")
                        .fileNumber("SOP-001-NO")
                        .versionNo("V1.0")
                        .build()));

        List<DccControlledFileUploadNameOptionRespVO> result = queryService.listUploadNameOptions(124L, 30L);

        assertEquals(2, result.size());
        assertEquals("SOP-001", result.get(0).getFileName());
        assertEquals("V1.0", result.get(0).getCurrentVersionNo());
        assertEquals(901L, result.get(0).getControlledFileId());
        assertEquals("SOP-001-NO", result.get(0).getFileNumber());
        assertEquals("WI-002", result.get(1).getFileName());
        assertEquals("V2.0", result.get(1).getCurrentVersionNo());

        ArgumentCaptor<DccControlledFilePageReqVO> reqCaptor =
                ArgumentCaptor.forClass(DccControlledFilePageReqVO.class);
        verify(controlledFileMapper).selectWorkflowList(reqCaptor.capture());
        DccControlledFilePageReqVO reqVO = reqCaptor.getValue();
        assertEquals(124L, reqVO.getDccProjectCodeId());
        assertEquals(List.of(30L, 31L), reqVO.getFileTypeTaxonomyIds());
        assertEquals(DccControlledFileStatusEnum.ACTIVE.getStatus(), reqVO.getStatus());
        assertEquals(DccControlledFileProcessTypeEnum.CONTROLLED_FILE.getCode(), reqVO.getProcessType());
    }

    @Test
    void listUploadNameOptions_withoutHistory_returnsEmptyList() {
        mockEnabledProjectAndTaxonomy();
        when(controlledFileMapper.selectWorkflowList(any(DccControlledFilePageReqVO.class))).thenReturn(List.of());

        List<DccControlledFileUploadNameOptionRespVO> result = queryService.listUploadNameOptions(124L, 30L);

        assertEquals(0, result.size());
    }

    @Test
    void listUploadNameOptions_projectMissing_throwsNotExists() {
        when(projectCodeMapper.selectById(124L)).thenReturn(null);

        assertServiceException(() -> queryService.listUploadNameOptions(124L, 30L), PROJECT_CODE_NOT_EXISTS);
    }

    @Test
    void listUploadNameOptions_taxonomyLessThanThreeLevels_throwsLevelInvalid() {
        when(projectCodeMapper.selectById(124L)).thenReturn(DccProjectCodeDO.builder()
                .id(124L)
                .status(DccProjectCodeStatusConstants.ENABLE)
                .build());
        when(fileTypeTaxonomyAdminService.resolveActivePath(30L))
                .thenReturn(new DccFileTypeTaxonomyPath(30L, "质量", "SOP", null, null, null));

        assertServiceException(() -> queryService.listUploadNameOptions(124L, 30L), FILE_TYPE_TAXONOMY_LEVEL_INVALID);
    }

    private void mockEnabledProjectAndTaxonomy() {
        when(projectCodeMapper.selectById(124L)).thenReturn(DccProjectCodeDO.builder()
                .id(124L)
                .status(DccProjectCodeStatusConstants.ENABLE)
                .build());
        when(fileTypeTaxonomyAdminService.resolveActivePath(30L))
                .thenReturn(new DccFileTypeTaxonomyPath(30L, "质量", "SOP", "作业指导书", null, null));
        when(fileTypeTaxonomyAdminService.listActiveDescendantIds(30L)).thenReturn(List.of(30L, 31L));
        when(fileTypeTaxonomyAdminService.listActiveDescendantPaths(30L)).thenReturn(List.of(
                new DccFileTypeTaxonomyPath(30L, "质量", "SOP", "作业指导书", null, null),
                new DccFileTypeTaxonomyPath(31L, "质量", "SOP", "作业指导书", "灌装", null)));
    }
}
