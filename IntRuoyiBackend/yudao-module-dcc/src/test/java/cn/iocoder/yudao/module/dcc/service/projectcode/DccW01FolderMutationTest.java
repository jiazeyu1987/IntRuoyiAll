package cn.iocoder.yudao.module.dcc.service.projectcode;

import cn.iocoder.yudao.module.dcc.service.projectcode.folder.DccProjectFolderMaintenanceService;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Reuses the existing isolated H2 fixture; does not claim a live MySQL isolation test. */
class DccW01FolderMutationTest extends DccProjectFormalCombinationTest {
    @Test void duplicateNameIsRejectedFromCurrentRowsEvenWhenEarlierPlainSnapshotIsEmpty() {
        policy("dcc.project-folder.create");
        var project = project("W01-CURRENT-NAME", 7L);
        var existing = folder(project.getId(), "已提交目录");
        clearInvocations(folders);
        doReturn(List.of()).when(folders).listByProject(project.getId());
        assertThrows(RuntimeException.class, () -> folderMaintenance.save(7L, project.getId(),
                new DccProjectFolderMaintenanceService.Save(null, 0L, existing.getName(), 0, "并发新增同名目录")));
        verify(folders, never()).listByProject(project.getId());
        verify(folders, never()).insert(any(cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectFolderDO.class));
        assertEquals(1, folders.listByProjectForUpdate(project.getId()).size());
        assertTrue(events.selectList().isEmpty());
    }

    @Test void deletedFolderCannotReceivePlacementThroughAStalePlainRead() {
        policy("dcc.project-file-placement.bind");
        var project = project("W01-DELETED-PLACEMENT", 7L);
        var folder = folder(project.getId(), "已删除目录");
        directory(1001L, 1L, true);
        var file = file(project.getId(), 1001L, "w01-placement.pdf");
        jdbc.update("UPDATE dcc_project_folder SET deleted=1,active=0 WHERE id=?", folder.getId());
        doReturn(folder).when(folders).selectById(folder.getId());
        assertThrows(RuntimeException.class, () -> tx(() -> placements.bind(7L, project.getId(),
                folder.getId(), file.getId(), 1001L, "旧快照不能登记文件")));
        verify(folders, never()).selectById(folder.getId());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_file_placement", Integer.class));
        assertTrue(events.selectList().isEmpty());
    }
}
