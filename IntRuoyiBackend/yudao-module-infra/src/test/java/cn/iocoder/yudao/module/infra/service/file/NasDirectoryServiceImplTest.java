package cn.iocoder.yudao.module.infra.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileNasDirectoryTreeRespVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.FILE_NAS_DIRECTORY_NOT_DIRECTORY;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.FILE_NAS_DIRECTORY_NOT_EXISTS;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.FILE_NAS_DIRECTORY_PATH_BLANK;
import static org.junit.jupiter.api.Assertions.assertEquals;

class NasDirectoryServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private NasDirectoryServiceImpl nasDirectoryService;

    @TempDir
    Path tempDir;

    @Test
    void testGetNasDirectoryTree_success() throws Exception {
        Path root = tempDir.resolve("nas-root");
        Files.createDirectories(root.resolve("Alpha").resolve("Beta"));
        Files.createDirectories(root.resolve("Gamma"));
        Files.writeString(root.resolve("root.txt"), "root");
        Files.writeString(root.resolve("Alpha").resolve("note.txt"), "note");

        FileNasDirectoryTreeRespVO tree = nasDirectoryService.getNasDirectoryTree(root.toString());

        assertEquals(root.toString(), tree.getRootPath());
        assertEquals("nas-root", tree.getRootName());
        assertEquals(4, tree.getDirectoryCount());
        assertEquals(List.of("Alpha", "Gamma"),
                tree.getChildren().stream().map(FileNasDirectoryTreeRespVO.Node::getName).toList());
        assertEquals(List.of("Beta"),
                tree.getChildren().get(0).getChildren().stream().map(FileNasDirectoryTreeRespVO.Node::getName).toList());
        assertEquals(0, tree.getChildren().get(1).getChildren().size());
    }

    @Test
    void testGetNasDirectoryTree_blankPath() {
        assertServiceException(() -> nasDirectoryService.getNasDirectoryTree(" "), FILE_NAS_DIRECTORY_PATH_BLANK);
    }

    @Test
    void testGetNasDirectoryTree_notExists() {
        assertServiceException(() -> nasDirectoryService.getNasDirectoryTree(tempDir.resolve("missing").toString()),
                FILE_NAS_DIRECTORY_NOT_EXISTS);
    }

    @Test
    void testGetNasDirectoryTree_notDirectory() throws Exception {
        Path file = tempDir.resolve("single.txt");
        Files.writeString(file, "content");

        assertServiceException(() -> nasDirectoryService.getNasDirectoryTree(file.toString()),
                FILE_NAS_DIRECTORY_NOT_DIRECTORY);
    }
}
