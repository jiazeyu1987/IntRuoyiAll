package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileUploadNameOptionRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO;
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
import cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryAccessPermissionService;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_CATEGORY_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

class DccControlledFileUploadNameOptionQueryServiceTest extends BaseMockitoUnitTest {

    @Mock
    private DccControlledFileMapper controlledFileMapper;
    @Mock
    private DccControlledFileMasterMapper controlledFileMasterMapper;
    @Mock
    private DccFileCategoryMapper categoryMapper;
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
    void listUploadNameOptions_returnsSortedCurrentVersions() {
        when(categoryMapper.selectById(10L)).thenReturn(DccFileCategoryDO.builder()
                .id(10L)
                .active(true)
                .build());
        doReturn(List.of(
                DccControlledFileMasterDO.builder()
                        .id(701L)
                        .categoryId(10L)
                        .fileName("WI-002")
                        .currentActiveControlledFileId(902L)
                        .build(),
                DccControlledFileMasterDO.builder()
                        .id(700L)
                        .categoryId(10L)
                        .fileName("SOP-001")
                        .currentActiveControlledFileId(901L)
                        .build(),
                DccControlledFileMasterDO.builder()
                        .id(702L)
                        .categoryId(10L)
                        .fileName("TMP-003")
                        .currentActiveControlledFileId(null)
                        .build()))
                .when(controlledFileMasterMapper)
                .selectList(org.mockito.ArgumentMatchers.<SFunction<DccControlledFileMasterDO, ?>>any(), eq(10L));
        when(controlledFileMapper.selectBatchIds(List.of(902L, 901L))).thenReturn(List.of(
                DccControlledFileDO.builder().id(901L).versionNo("1.0").build(),
                DccControlledFileDO.builder().id(902L).versionNo("2.5").build()));

        List<DccControlledFileUploadNameOptionRespVO> result = queryService.listUploadNameOptions(10L);

        assertEquals(3, result.size());
        assertEquals("SOP-001", result.get(0).getFileName());
        assertEquals("1.0", result.get(0).getCurrentVersionNo());
        assertEquals("TMP-003", result.get(1).getFileName());
        assertEquals(null, result.get(1).getCurrentVersionNo());
        assertEquals("WI-002", result.get(2).getFileName());
        assertEquals("2.5", result.get(2).getCurrentVersionNo());
    }

    @Test
    void listUploadNameOptions_allCurrentActiveIdsNull_returnsNullVersions() {
        when(categoryMapper.selectById(10L)).thenReturn(DccFileCategoryDO.builder()
                .id(10L)
                .active(true)
                .build());
        doReturn(List.of(
                DccControlledFileMasterDO.builder()
                        .id(700L)
                        .categoryId(10L)
                        .fileName("SOP-001")
                        .currentActiveControlledFileId(null)
                        .build(),
                DccControlledFileMasterDO.builder()
                        .id(701L)
                        .categoryId(10L)
                        .fileName("WI-002")
                        .currentActiveControlledFileId(null)
                        .build()))
                .when(controlledFileMasterMapper)
                .selectList(org.mockito.ArgumentMatchers.<SFunction<DccControlledFileMasterDO, ?>>any(), eq(10L));

        List<DccControlledFileUploadNameOptionRespVO> result = queryService.listUploadNameOptions(10L);

        assertEquals(2, result.size());
        assertEquals("SOP-001", result.get(0).getFileName());
        assertEquals(null, result.get(0).getCurrentVersionNo());
        assertEquals("WI-002", result.get(1).getFileName());
        assertEquals(null, result.get(1).getCurrentVersionNo());
    }

    @Test
    void listUploadNameOptions_withoutHistory_returnsEmptyList() {
        when(categoryMapper.selectById(10L)).thenReturn(DccFileCategoryDO.builder()
                .id(10L)
                .active(true)
                .build());
        doReturn(List.of()).when(controlledFileMasterMapper)
                .selectList(org.mockito.ArgumentMatchers.<SFunction<DccControlledFileMasterDO, ?>>any(), eq(10L));

        List<DccControlledFileUploadNameOptionRespVO> result = queryService.listUploadNameOptions(10L);

        assertEquals(0, result.size());
    }

    @Test
    void listUploadNameOptions_categoryMissing_throwsNotExists() {
        when(categoryMapper.selectById(10L)).thenReturn(null);

        assertServiceException(() -> queryService.listUploadNameOptions(10L), FILE_CATEGORY_NOT_EXISTS);
    }
}
