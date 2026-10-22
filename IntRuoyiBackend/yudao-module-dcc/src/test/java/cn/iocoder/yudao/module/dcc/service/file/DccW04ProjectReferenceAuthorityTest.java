package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.service.file.relations.DccProjectReferenceAuthorityImpl;
import cn.iocoder.yudao.module.dcc.service.projectcode.folder.DccFolderTemplateService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class DccW04ProjectReferenceAuthorityTest {
    @Test
    void writeAuthorizationUsesCurrentFolderWhileListReadKeepsOrdinaryRead() {
        var folders = mock(DccFolderTemplateService.class);
        var authority = new DccProjectReferenceAuthorityImpl();
        ReflectionTestUtils.setField(authority, "folderService", folders);
        authority.assertFolderBelongsToProjectForUpdate(2L, 21L);
        verify(folders).requireProjectFolderForUpdate(2L, 21L);
        verify(folders, never()).requireProjectFolder(any(), any());
        authority.assertFolderBelongsToProject(2L, 21L);
        verify(folders).requireProjectFolder(2L, 21L);
    }

    @Test
    void currentInvalidFolderDoesNotFallBackToEarlierOrdinarySnapshot() {
        var folders = mock(DccFolderTemplateService.class);
        var authority = new DccProjectReferenceAuthorityImpl();
        ReflectionTestUtils.setField(authority, "folderService", folders);
        doThrow(new IllegalStateException("CURRENT_FOLDER_INVALID"))
                .when(folders).requireProjectFolderForUpdate(2L, 21L);
        assertThrows(IllegalStateException.class,
                () -> authority.assertFolderBelongsToProjectForUpdate(2L, 21L));
        verify(folders, never()).requireProjectFolder(any(), any());
    }
}
