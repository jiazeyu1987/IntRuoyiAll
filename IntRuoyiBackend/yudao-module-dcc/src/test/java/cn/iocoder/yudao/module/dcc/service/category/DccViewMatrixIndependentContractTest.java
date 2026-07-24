package cn.iocoder.yudao.module.dcc.service.category;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DccViewMatrixIndependentContractTest extends BaseMockitoUnitTest {

    @Test
    void sourceContracts_requireIndependentViewMatrixEndpointsAndBrowseReason() throws Exception {
        String controller = read("src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/category/DccFileCategoryController.java");
        String queryService = read("src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java");
        String detailPage = read("../../yudao-ui-admin-vue3/src/views/dcc/controlled-file/detail/index.vue");

        assertTrue(controller.contains("@Tag(name = \"管理后台 - DCC 文件类别\")"));
        assertTrue(controller.contains("@GetMapping(\"/view-matrix\")"),
                "controller must expose independent view matrix list endpoint");
        assertTrue(controller.contains("@PostMapping(\"/{id:\\\\d+}/view-matrix/effective-preview\")"),
                "controller must expose independent view matrix preview endpoint");
        assertTrue(controller.contains("@GetMapping(\"/view-matrix/user-lookup\")"),
                "controller must expose independent view matrix reverse lookup endpoint");
        assertTrue(controller.contains("@PutMapping(\"/{id:\\\\d+}/view-matrix\")"),
                "controller must expose independent view matrix save endpoint with numeric id route");
        assertFalse(controller.contains("Mapping(\"/{id}\""),
                "category dynamic id endpoints must be constrained so they cannot swallow /view-matrix");

        assertTrue(queryService.contains("CURRENT_VIEW_MATRIX"),
                "query service must explain normal browsing from the current view matrix");
        assertTrue(queryService.contains("viewMatrixAccessService"),
                "query service must depend on an independent view matrix access service");
        assertTrue(queryService.contains("当前查看矩阵参与人"),
                "query service must describe current view matrix participants");

        assertTrue(detailPage.contains("CURRENT_VIEW_MATRIX: '当前查看矩阵'"),
                "detail page must map CURRENT_VIEW_MATRIX to the new reason label");
    }

    private static String read(String relativePath) throws Exception {
        return Files.readString(Path.of(relativePath), StandardCharsets.UTF_8);
    }
}
