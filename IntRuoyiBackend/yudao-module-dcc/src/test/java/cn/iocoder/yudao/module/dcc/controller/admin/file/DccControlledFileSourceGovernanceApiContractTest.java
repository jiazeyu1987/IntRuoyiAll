package cn.iocoder.yudao.module.dcc.controller.admin.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSourceGovernanceConfirmReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSourceGovernanceExecuteReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSourceGovernancePrepareReqVO;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DccControlledFileSourceGovernanceApiContractTest {

    @Test
    void governanceEndpointsRequireConfirmedManifestFlowAndPermission() throws Exception {
        Method confirm = DccControlledFileController.class.getDeclaredMethod(
                "confirmSourceGovernanceBatch", String.class, DccControlledFileSourceGovernanceConfirmReqVO.class);
        Method execute = DccControlledFileController.class.getDeclaredMethod(
                "executeSourceGovernanceBatch", String.class, DccControlledFileSourceGovernanceExecuteReqVO.class);
        Method blockers = DccControlledFileController.class.getDeclaredMethod(
                "getSourceGovernanceBlockers", String.class);
        Method postflight = DccControlledFileController.class.getDeclaredMethod(
                "getSourceGovernancePostflight", String.class);
        Method prepare = DccControlledFileController.class.getDeclaredMethod(
                "prepareSourceGovernanceBatch", DccControlledFileSourceGovernancePrepareReqVO.class);

        assertTrue(Arrays.asList(confirm.getAnnotation(PostMapping.class).value())
                .contains("/source-governance/batches/{taskKey}/confirm"));
        assertTrue(Arrays.asList(execute.getAnnotation(PostMapping.class).value())
                .contains("/source-governance/batches/{taskKey}/execute"));
        assertTrue(Arrays.asList(blockers.getAnnotation(GetMapping.class).value())
                .contains("/source-governance/batches/{taskKey}/blockers"));
        assertTrue(Arrays.asList(postflight.getAnnotation(GetMapping.class).value())
                .contains("/source-governance/batches/{taskKey}/postflight"));
        assertTrue(Arrays.asList(prepare.getAnnotation(PostMapping.class).value())
                .contains("/source-governance/batches/prepare"));
        assertTrue(confirm.isAnnotationPresent(PreAuthorize.class));
        assertTrue(execute.isAnnotationPresent(PreAuthorize.class));
        assertTrue(blockers.isAnnotationPresent(PreAuthorize.class));
        assertTrue(postflight.isAnnotationPresent(PreAuthorize.class));
        assertTrue(prepare.isAnnotationPresent(PreAuthorize.class));
    }

    @Test
    void legacyMigrationEndpointCannotBypassConfirmedManifest() throws Exception {
        String controller = Files.readString(Path.of("src", "main", "java", "cn", "iocoder", "yudao",
                "module", "dcc", "controller", "admin", "file", "DccControlledFileController.java"));
        assertTrue(controller.contains("CONTROLLED_FILE_SOURCE_GOVERNANCE_LEGACY_ENTRY_DISABLED"));
        assertTrue(!controller.contains("sourceMigrationService.migrateBatch("));
    }
}
