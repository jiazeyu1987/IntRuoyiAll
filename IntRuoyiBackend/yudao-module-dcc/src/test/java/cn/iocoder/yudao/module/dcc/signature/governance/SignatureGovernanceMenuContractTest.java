package cn.iocoder.yudao.module.dcc.signature.governance;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignatureGovernanceMenuContractTest {

    @Test
    void unifiedSignatureRecordsMenuMergesLegacyEntriesAndPreservesTenantRoleBindings() throws Exception {
        Path projectDir = findProjectDir();
        String sql = Files.readString(projectDir.resolve("sql/mysql/20260714_unified_signature_records_menu.sql"),
                StandardCharsets.UTF_8);
        String route = Files.readString(projectDir.resolveSibling("IntRuoyiFronted")
                        .resolve("src/router/modules/remaining.ts"),
                StandardCharsets.UTF_8);

        assertTrue(sql.contains("release-migration"), "menu SQL must declare release migration metadata");
        assertTrue(sql.contains("SET NAMES utf8mb4"), "menu SQL must set utf8mb4 explicitly");
        assertTrue(sql.contains("SET @unified_signature_records_menu_id := 900411"));
        assertTrue(sql.contains("SET @legacy_batch_signature_menu_id := 900412"));
        assertTrue(sql.contains("SET `name` = '签名记录'"), "900411 must become unified signature records");
        assertTrue(sql.contains("`path` = 'signature-records'"), "900411 path must match the unified route");
        assertTrue(sql.contains("`component_name` = 'SignatureGovernanceSignatureRecords'"),
                "900411 component name must match route name");
        assertTrue(sql.contains("`always_show` = b'0'"),
                "unified signature records must stay a leaf menu without a submenu arrow");
        assertTrue(sql.contains("WHERE `id` = @legacy_batch_signature_menu_id"),
                "900412 must be explicitly targeted");
        assertTrue(sql.contains("`deleted` = b'1'"), "legacy batch signature page must be soft deleted");
        assertTrue(sql.contains("'dcc:controlled-file:signature:manage'"),
                "DCC file signature permission must remain attached");
        assertTrue(sql.contains("'mes:pro-batch-record-execution:signature-query'"),
                "eDHR batch signature permission must remain attached");
        assertTrue(sql.contains("`id` NOT IN (@unified_signature_records_menu_id, @legacy_batch_signature_menu_id)"),
                "permission re-parenting must not resurrect the hidden legacy batch route");
        assertTrue(sql.contains("`visible` = b'0'"), "permission-only children must not render in sidebar");
        assertTrue(sql.contains("`type` = 3"),
                "permission-only child cleanup must not re-parent sibling page menus such as user authorization");
        assertFalse(sql.contains("`name` IN ('文件签名记录', '批记录签名记录')"),
                "permission-only children must be hidden by permission code, not by mutable display names");
        assertTrue(sql.contains("INSERT INTO `system_role_menu`"));
        assertTrue(sql.contains("`tenant_id`"), "role binding copy must preserve tenant_id");
        assertTrue(sql.contains("src.`tenant_id`"), "role binding copy must copy source tenant_id");
        assertTrue(sql.contains("existing.`tenant_id` = src.`tenant_id`"),
                "duplicate detection must be tenant scoped");
        assertFalse(sql.contains("SCHEDULING"), "menu migration must not create unpersisted scheduling data");
        assertFalse(sql.contains("DOCUMENT_CONTROL"), "menu migration must not create unpersisted document-control data");

        assertTrue(route.contains("redirect: '/signature-governance/signature-records'"));
        assertTrue(route.contains("path: 'signature-records'"));
        assertTrue(route.contains("title: '签名记录'"));
        assertTrue(route.contains("path: 'file-signatures'"));
        assertTrue(route.contains("path: 'batch-signatures'"));
        assertTrue(route.contains("activeMenu: '/signature-governance/signature-records'"));
    }

    private static Path findProjectDir() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve("sql/mysql"))
                    && Files.exists(current.resolve("yudao-module-dcc"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate ruoyi-vue-pro project directory");
    }
}
