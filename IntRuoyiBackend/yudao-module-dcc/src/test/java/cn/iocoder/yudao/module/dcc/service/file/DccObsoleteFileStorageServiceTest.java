package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccObsoleteFileStorageServiceTest extends BaseMockitoUnitTest {

    @Mock
    private FileService fileService;
    @InjectMocks
    private DccObsoleteFileStorageService storageService;

    @Test
    void moveControlledFileArtifactsToObsoleteFolder_movesUniqueArtifactFiles() {
        DccControlledFileDO file = DccControlledFileDO.builder()
                .id(900L)
                .sourceFileId(10L)
                .originalFileId(10L)
                .publishedFileId(11L)
                .stampedFileId(11L)
                .drawingPdfFileId(12L)
                .trainingRecordFileId(13L)
                .build();
        when(fileService.getFile(10L)).thenReturn(file(10L, "dcc/original/SOP-001.docx", "SOP-001.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        when(fileService.getFile(11L)).thenReturn(file(11L, "dcc/stamped/SOP-001.pdf", "SOP-001.pdf",
                "application/pdf"));
        when(fileService.getFile(12L)).thenReturn(file(12L, "dcc/drawing/DWG-001.pdf", "DWG-001.pdf",
                "application/pdf"));
        when(fileService.getFile(13L)).thenReturn(file(13L, "dcc/training/TR-001.pdf", "TR-001.pdf",
                "application/pdf"));

        storageService.moveControlledFileArtifactsToObsoleteFolder(file);

        verify(fileService).moveFile(10L, "dcc/original/作废文件/10/SOP-001.docx");
        verify(fileService).moveFile(11L, "dcc/stamped/作废文件/11/SOP-001.pdf");
        verify(fileService).moveFile(12L, "dcc/drawing/作废文件/12/DWG-001.pdf");
        verify(fileService).moveFile(13L, "dcc/training/作废文件/13/TR-001.pdf");
    }

    @Test
    void moveControlledFileArtifactsToObsoleteFolder_missingArtifactFailsBeforeMove() {
        DccControlledFileDO file = DccControlledFileDO.builder()
                .id(901L)
                .sourceFileId(10L)
                .build();
        when(fileService.getFile(10L)).thenReturn(null);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> storageService.moveControlledFileArtifactsToObsoleteFolder(file));

        assertEquals("Controlled file artifact 10 is missing for obsolete move", exception.getMessage());
        verify(fileService, never()).moveFile(org.mockito.Mockito.any(), org.mockito.Mockito.any());
    }

    private FileDO file(Long id, String path, String name, String type) {
        return FileDO.builder()
                .id(id)
                .configId(1L)
                .path(path)
                .name(name)
                .type(type)
                .build();
    }
}
