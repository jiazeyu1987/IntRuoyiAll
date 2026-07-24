package cn.iocoder.yudao.module.showroom.controller.admin;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ShowroomHallPreviewAssetAdminContractTest {

    @Test
    void hallPreviewAssetCanBePublishedThroughAdminContract() throws Exception {
        String controller = readRepoFile("yudao-module-showroom/src/main/java/cn/iocoder/yudao/module/showroom/"
                + "controller/admin/ShowroomAdminController.java");
        String runtime = readRepoFile("yudao-module-showroom/src/main/java/cn/iocoder/yudao/module/showroom/"
                + "controller/ShowroomApiRuntime.java");

        assertTrue(controller.contains("@PostMapping(\"/hall/publish-preview-asset\")"));
        assertTrue(controller.contains("HallPreviewAssetPublishReqVO"));
        assertTrue(controller.contains("HallPreviewAssetPublishRespVO"));
        assertTrue(controller.contains("requirePublicityRole(\"发布展柜预览图\")"));
        assertTrue(controller.contains("runtime.publishHallPreviewAsset(reqVO)"));

        assertTrue(runtime.contains("publishHallPreviewAsset("));
        assertTrue(runtime.contains("ShowroomAdminController.HallPreviewAssetPublishReqVO req"));
        assertTrue(runtime.contains("SHOWROOM_PREVIEW_STATIC_ASSET_MISSING: hall preview imageFileId is required"));
        assertTrue(runtime.contains("SHOWROOM_PREVIEW_STATIC_ASSET_MISSING: hall preview file metadata is incomplete"));
        assertTrue(runtime.contains("new ShowroomPreviewAssetDraftCommand("));
        assertTrue(runtime.contains("ShowroomPreviewAssetTargetType.HALL"));
        assertTrue(runtime.contains("new ShowroomPreviewAssetFiles(imageFileId, imageFileId, imageFileId)"));
        assertTrue(runtime.contains("previewAssetService.bindStaticPreviewAssets(command)"));
        assertTrue(runtime.contains("previewAssetService.publishDirectly(draft.id())"));
        assertTrue(runtime.contains("fileUrl(published.files().desktopFileId())"));
    }

    private String readRepoFile(String relativePath) throws Exception {
        return Files.readString(resolveRepoFile(relativePath), StandardCharsets.UTF_8);
    }

    private Path resolveRepoFile(String relativePath) {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(relativePath);
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("REPO_FILE_MISSING: " + relativePath);
    }

}
