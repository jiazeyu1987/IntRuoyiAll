package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRelatedFileRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileRelatedFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileRelatedFileMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_RELATED_FILE_DUPLICATE;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_RELATED_FILE_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccControlledFileRelatedFileServiceTest extends BaseMockitoUnitTest {

    @Mock
    private DccControlledFileRelatedFileMapper relatedFileMapper;
    @Mock
    private DccControlledFileMapper controlledFileMapper;
    @InjectMocks
    private DccControlledFileRelatedFileServiceImpl service;

    @Test
    void validateAndBindRelatedFiles_emptySelectionDoesNotCreateRelations() {
        service.validateAndBindRelatedFiles(100L, 20L, List.of());

        verify(relatedFileMapper, never()).insert(any(DccControlledFileRelatedFileDO.class));
    }

    @Test
    void validateAndBindRelatedFiles_multipleSameProjectFilesCreatesExplicitRelations() {
        DccControlledFileDO first = relatedFile(201L, 301L, "DOC-201", "工艺文件", "V1.0");
        DccControlledFileDO second = relatedFile(202L, 302L, "DOC-202", "检验文件", "V2.0");
        when(controlledFileMapper.selectAssociatedFilesByProjectCodeId(20L, List.of(201L, 202L)))
                .thenReturn(List.of(first, second));

        service.validateAndBindRelatedFiles(100L, 20L, List.of(201L, 202L));

        ArgumentCaptor<DccControlledFileRelatedFileDO> captor =
                ArgumentCaptor.forClass(DccControlledFileRelatedFileDO.class);
        verify(relatedFileMapper, org.mockito.Mockito.times(2)).insert(captor.capture());
        assertEquals(List.of(201L, 202L), captor.getAllValues().stream()
                .map(DccControlledFileRelatedFileDO::getRelatedControlledFileId).toList());
        assertEquals("UPLOAD", captor.getAllValues().get(0).getRelationSource());
    }

    @Test
    void validateAndBindRelatedFiles_crossProjectSelectionFailsFast() {
        when(controlledFileMapper.selectAssociatedFilesByProjectCodeId(20L, List.of(201L)))
                .thenReturn(List.of());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.validateAndBindRelatedFiles(100L, 20L, List.of(201L)));

        assertEquals(CONTROLLED_FILE_RELATED_FILE_INVALID.getCode(), exception.getCode());
        verify(relatedFileMapper, never()).insert(any(DccControlledFileRelatedFileDO.class));
    }

    @Test
    void validateAndBindRelatedFiles_duplicateSelectionFailsFast() {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.validateAndBindRelatedFiles(100L, 20L, List.of(201L, 201L)));

        assertEquals(CONTROLLED_FILE_RELATED_FILE_DUPLICATE.getCode(), exception.getCode());
        verify(controlledFileMapper, never()).selectAssociatedFilesByProjectCodeId(any(), any());
    }

    @Test
    void listRelatedFiles_returnsCurrentMetadataAndSnapshotWhenTargetMissing() {
        DccControlledFileRelatedFileDO first = relation(1L, 100L, 201L, "DOC-201", "旧名称", "V1.0");
        DccControlledFileRelatedFileDO second = relation(2L, 100L, 202L, "DOC-202", "快照名称", "V2.0");
        when(relatedFileMapper.selectListByControlledFileId(100L)).thenReturn(List.of(first, second));
        when(controlledFileMapper.selectBatchIds(List.of(201L, 202L)))
                .thenReturn(List.of(relatedFile(201L, 301L, "DOC-201", "当前名称", "V1.1")));

        List<DccControlledFileRelatedFileRespVO> result = service.listRelatedFiles(100L);

        assertEquals(2, result.size());
        assertEquals("当前名称", result.get(0).getFileName());
        assertEquals("V1.1", result.get(0).getVersionNo());
        assertEquals("快照名称", result.get(1).getFileName());
    }

    private DccControlledFileDO relatedFile(Long id, Long masterId, String fileNumber, String fileName,
                                             String versionNo) {
        return DccControlledFileDO.builder().id(id).masterId(masterId).fileNumber(fileNumber)
                .fileName(fileName).versionNo(versionNo).status("ACTIVE").build();
    }

    private DccControlledFileRelatedFileDO relation(Long id, Long controlledFileId, Long relatedFileId,
                                                     String fileNumber, String fileName, String versionNo) {
        return DccControlledFileRelatedFileDO.builder().id(id).controlledFileId(controlledFileId)
                .relatedControlledFileId(relatedFileId).projectCodeId(20L).relatedMasterId(300L)
                .relatedFileNumberSnapshot(fileNumber).relatedFileNameSnapshot(fileName)
                .relatedVersionNoSnapshot(versionNo).relationSource("UPLOAD").build();
    }
}
