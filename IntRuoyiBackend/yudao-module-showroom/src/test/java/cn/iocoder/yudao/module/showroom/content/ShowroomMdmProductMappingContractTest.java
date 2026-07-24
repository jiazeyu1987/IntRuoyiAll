package cn.iocoder.yudao.module.showroom.content;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

class ShowroomMdmProductMappingContractTest {

    @Test
    void showroomMappingFeatureIsRemoved() throws Exception {
        String controller = readRepoFile("yudao-module-showroom/src/main/java/cn/iocoder/yudao/module/showroom/"
                + "controller/admin/ShowroomAdminController.java");
        String mdmApi = readRepoFile("yudao-module-mdm/src/main/java/cn/iocoder/yudao/module/mdm/api/product/"
                + "MdmProductApi.java");
        String migration = readRepoFile("sql/mysql/20260607_product_master_data.sql");

        assertFalse(controller.contains("mdm-mapping-preview"));
        assertFalse(controller.contains("mdm-mapping-confirm"));
        assertFalse(controller.contains("mdm:product:map-showroom"));
        assertFalse(Files.exists(resolveRepoPath("yudao-module-showroom/src/main/java/cn/iocoder/yudao/module/"
                + "showroom/content/service/ShowroomMdmProductMappingService.java")));
        assertFalse(Files.exists(resolveRepoPath("yudao-module-showroom/src/main/java/cn/iocoder/yudao/module/"
                + "showroom/content/model/ShowroomMdmProductMappingPreview.java")));
        assertFalse(mdmApi.contains("getProductByProductCode"));
        assertFalse(mdmApi.contains("saveProductFromShowroom"));
        assertFalse(mdmApi.contains("MdmProductShowroomSyncReqDTO"));
        assertFalse(migration.contains("mdm:product:map-showroom"));
    }

    private String readRepoFile(String relativePath) throws Exception {
        return Files.readString(resolveRepoPath(relativePath), StandardCharsets.UTF_8);
    }

    private Path resolveRepoPath(String relativePath) {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve("pom.xml"))
                    && Files.exists(current.resolve("yudao-module-showroom"))) {
                return current.resolve(relativePath);
            }
            current = current.getParent();
        }
        throw new IllegalStateException("REPO_ROOT_MISSING");
    }

}
