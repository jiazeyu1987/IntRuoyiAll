package cn.iocoder.yudao.module.dcc.service.file;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DccLegacyPermissionEntryGovernanceTest {

    private static final Path MAIN_JAVA = Path.of("src/main/java");

    @Test
    void controlledFileRuntimeMustNotReintroduceLegacyProductVisibilityEntry() throws IOException {
        assertFalse(Files.exists(MAIN_JAVA.resolve(
                        "cn/iocoder/yudao/module/dcc/service/file/DccControlledFileProductVisibilityService.java")),
                "DCC 浏览/详情/已发布预览已收口到审阅矩阵，不能保留产品可见性旧入口服务。");
        assertFalse(Files.exists(MAIN_JAVA.resolve(
                        "cn/iocoder/yudao/module/dcc/dal/mysql/permission/DccProductVisibilityMapper.java")),
                "旧产品组可见性 Mapper 没有运行时调用方，不能作为隐式权限入口残留。");
    }

    @Test
    void approvalPrintMustUseCurrentBrowseTruthInsteadOfLegacyViewPermission() throws IOException {
        String source = read("cn/iocoder/yudao/module/dcc/service/file/DccApprovalPrintTemplateServiceImpl.java");

        assertFalse(source.contains("DccFileCategoryPermissionActionEnum.VIEW"),
                "审批流程打印属于文件详情读侧能力，不能继续用旧 VIEW 类别权限作为访问真源。");
        assertFalse(source.contains("DccControlledFileCategoryPermissionSupport permissionSupport"),
                "审批流程打印不能注入旧类别权限支持作为浏览兜底。");
        assertTrue(source.contains("DccControlledFileQueryService queryService"),
                "审批流程打印必须复用文件详情读侧门禁，而不是复制第二套查阅真源。");
        assertTrue(source.contains("queryService.getControlledFile(userId, controlledFileId)"),
                "审批流程打印必须先通过详情门禁，再渲染审批记录。");
    }

    @Test
    void controlledFileQueryMustKeepLegacyViewOutOfBrowseDetailAndPublishedPreview() throws IOException {
        String source = read("cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java");

        assertFalse(source.contains("DccControlledFileProductVisibilityService"),
                "普通浏览/详情/已发布预览不得重新注入产品可见性旧服务。");
        assertFalse(source.contains("DccFileCategoryPermissionActionEnum.VIEW"),
                "普通浏览/详情/已发布预览不得回退到旧 VIEW 类别权限。");
    }

    @Test
    void controlledFilePreviewApiMustNotReintroduceStandalonePreviewMenuGate() throws IOException {
        String source = read("cn/iocoder/yudao/module/dcc/controller/admin/file/DccControlledFileController.java");

        assertFalse(source.contains("dcc:controlled-file:preview"),
                "查看与预览已收口为统一读权限，控制器不得重新要求独立 preview 菜单门禁。");
    }

    private String read(String relativePath) throws IOException {
        return Files.readString(MAIN_JAVA.resolve(relativePath), StandardCharsets.UTF_8);
    }
}
