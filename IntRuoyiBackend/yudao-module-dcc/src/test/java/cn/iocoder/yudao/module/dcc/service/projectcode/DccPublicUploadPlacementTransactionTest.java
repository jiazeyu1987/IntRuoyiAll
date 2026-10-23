package cn.iocoder.yudao.module.dcc.service.projectcode;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSubmitReqVO;
import cn.iocoder.yudao.module.dcc.service.file.DccPublicUploadPlacementService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

/** Real project/folder/File/B placement/GXP facts. A's file creation is the separately verified callback boundary. */
@Import(DccPublicUploadPlacementService.class)
class DccPublicUploadPlacementTransactionTest extends DccProjectFormalCombinationTest {
    @Resource DccPublicUploadPlacementService publicUpload;

    DccControlledFileSubmitReqVO request(Long projectId, Long folderId) {
        var request = new DccControlledFileSubmitReqVO();
        request.setDccProjectCodeId(projectId); request.setProjectFolderId(folderId);
        request.setProjectFolderChangeReason("Place this approved draft in its selected project directory");
        request.setDirectoryId(101L); request.setProcessType("CONTROLLED_FILE");
        return request;
    }

    @Test void placementPublicCreationBindsExactLogicalFolderAndIndependentStorage() {
        var project = project("PUBLIC-PLACEMENT", 7L); var folder = folder(project.getId(), "quality");
        directory(101L, 1L, true); policy("dcc.project-file-placement.bind");
        var request = request(project.getId(), folder.getId());
        Long fileId = tx(() -> publicUpload.create(7L, request, () -> file(project.getId(), 101L, "public.pdf").getId()));
        var placement = placements.require(7L, project.getId(), fileId);
        assertEquals(folder.getId(), placement.getProjectFolderId()); assertEquals(101L, placement.getStorageDirectoryId());
        assertEquals(fileId, placement.getControlledFileId()); assertNotEquals(folder.getId(), placement.getStorageDirectoryId());
        assertEquals(1, events.selectList().size());
        Long replay = tx(() -> publicUpload.create(7L, request, () -> fileId));
        assertEquals(fileId, replay); assertEquals(1, events.selectList().size());
    }

    @Test void placementOuterFailureRollsBackActualFilePlacementAndLedger() {
        var project = project("PUBLIC-ROLLBACK", 7L); var folder = folder(project.getId(), "drafts");
        directory(101L, 1L, true); policy("dcc.project-file-placement.bind");
        var created = new AtomicLong();
        assertThrows(IllegalStateException.class, () -> tx(() -> {
            created.set(publicUpload.create(7L, request(project.getId(), folder.getId()),
                    () -> file(project.getId(), 101L, "rollback.pdf").getId()));
            assertNotNull(placements.require(7L, project.getId(), created.get()));
            throw new IllegalStateException("Later application failure");
        }));
        assertTrue(created.get() > 0); assertNull(files.selectById(created.get()));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_file_placement", Integer.class));
        assertEquals(0, events.selectList().size());
    }

    @Test void placementMissingOrForeignFolderRejectsBeforeFileCreation() {
        var project = project("PLACEMENT-IDENTITY", 7L);
        var other = project("PLACEMENT-FOREIGN", 7L);
        var foreign = folder(other.getId(), "foreign");
        var called = new AtomicBoolean();
        for (Long id : new Long[]{null, foreign.getId(), 99999999L}) {
            assertThrows(RuntimeException.class, () -> tx(() -> publicUpload.create(7L, request(project.getId(), id),
                    () -> { called.set(true); return 1L; })));
        }
        assertFalse(called.get());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_file_placement", Integer.class));
    }

    @Test void placementViewOnlyCannotCreateEvenWithAValidFolder() {
        var project = project("PLACEMENT-VIEW", 7L); var folder = folder(project.getId(), "quality");
        jdbc.update("UPDATE dcc_project_access_rule SET access_level='VIEW' WHERE dcc_project_code_id=?", project.getId());
        var called = new AtomicBoolean();
        assertThrows(RuntimeException.class, () -> tx(() -> publicUpload.create(7L, request(project.getId(), folder.getId()),
                () -> { called.set(true); return 1L; })));
        assertFalse(called.get());
    }

    @Test void placementAuditFailureRollsBackCreatedFileAndNewPlacement() {
        var project = project("PLACEMENT-NO-POLICY", 7L); var folder = folder(project.getId(), "quality");
        directory(101L, 1L, true); var created = new AtomicLong();
        assertThrows(RuntimeException.class, () -> tx(() -> publicUpload.create(7L, request(project.getId(), folder.getId()),
                () -> { long id = file(project.getId(), 101L, "no-policy.pdf").getId(); created.set(id); return id; })));
        assertTrue(created.get() > 0); assertNull(files.selectById(created.get()));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_file_placement", Integer.class));
    }

    @Test void placementReplayCannotMoveAnExistingVersionToAnotherFolder() {
        var project = project("PLACEMENT-REPLAY", 7L); var first = folder(project.getId(), "first"); var second = folder(project.getId(), "second");
        directory(101L, 1L, true); policy("dcc.project-file-placement.bind");
        Long fileId = tx(() -> publicUpload.create(7L, request(project.getId(), first.getId()),
                () -> file(project.getId(), 101L, "fixed-location.pdf").getId()));
        assertThrows(RuntimeException.class, () -> tx(() -> publicUpload.create(7L, request(project.getId(), second.getId()), () -> fileId)));
        assertEquals(first.getId(), placements.require(7L, project.getId(), fileId).getProjectFolderId());
        assertEquals(1, events.selectList().size());
    }
}
