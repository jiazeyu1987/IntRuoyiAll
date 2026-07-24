package cn.iocoder.yudao.module.bpm.approval.service.provider;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ApprovalTaskProviderGovernanceTest {

    @Test
    void providerContractDocumentsMandatoryUnifiedApprovalPlatformForNewModules() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/cn/iocoder/yudao/module/bpm/approval/service/provider/ApprovalTaskProvider.java"),
                StandardCharsets.UTF_8);

        assertTrue(source.contains("New approval modules must implement this provider"),
                "ApprovalTaskProvider must document that new approval modules use the unified platform");
        assertTrue(source.contains("Do not create a private approval center"),
                "ApprovalTaskProvider must explicitly forbid private module approval centers");
        assertTrue(source.contains("detailRoute"),
                "ApprovalTaskProvider contract must keep module formal page routing as the processing boundary");
    }

    @Test
    void phase4GovernanceDocumentsProvideStandardTemplateAndMigrationInventory() throws Exception {
        String standard = Files.readString(Path.of(
                "../docs/engineering/unified-approval-platform-adapter-standard.md"), StandardCharsets.UTF_8);
        String template = Files.readString(Path.of(
                "../docs/engineering/unified-approval-platform-adapter-template.md"), StandardCharsets.UTF_8);
        String inventory = Files.readString(Path.of(
                "../docs/engineering/unified-approval-platform-migration-inventory.md"), StandardCharsets.UTF_8);

        assertTrue(standard.contains("ApprovalModuleIntegrationGuard"),
                "standard must name the Phase 4 fail-fast integration guard");
        assertTrue(standard.contains("不得再自建审批中心"),
                "standard must keep the no-private-approval-center rule");
        assertTrue(template.contains("ApprovalTaskProvider"),
                "template must include the provider implementation entry point");
        assertTrue(template.contains("RED"),
                "template must require strict RED before GREEN");
        assertTrue(inventory.contains("MES 报工审批"),
                "inventory must include remaining MES feedback approval migration candidate");
        assertTrue(inventory.contains("P0"),
                "inventory must include migration priority");
    }

    @Test
    void phase5RetirementArtifactsKeepUnifiedApprovalCenterAsOnlyFormalEntry() throws Exception {
        Path retirementSql = Path.of("../sql/mysql/20260624_unified_approval_phase5_retire_legacy_menus.sql");
        Path retirementInventory = Path.of("../docs/engineering/unified-approval-platform-retirement-inventory.md");

        assertTrue(Files.exists(retirementSql), "Phase5 must provide an idempotent menu retirement SQL");
        assertTrue(Files.exists(retirementInventory), "Phase5 must provide the post-launch retirement inventory");

        String sql = Files.readString(retirementSql, StandardCharsets.UTF_8);
        String inventory = Files.readString(retirementInventory, StandardCharsets.UTF_8);

        for (String legacyPath : List.of("bpm/task/todo/index", "bpm/task/done/index",
                "bpm/processInstance/index", "controlled-file/approval-tasks",
                "feedback/edhr-approval", "ShowroomAdminApproval")) {
            assertTrue(sql.contains(legacyPath), "retirement SQL must address legacy entry " + legacyPath);
        }
        assertTrue(sql.contains("visible` = b'0'") || sql.contains("visible = b'0'"),
                "retirement SQL must hide legacy menu entries instead of leaving them visible");

        for (String item : List.of("唯一正式入口", "BPM", "DCC", "eDHR", "Showroom",
                "MES 报工审批", "ERP", "CRM", "Phase Final 分类结果")) {
            assertTrue(inventory.contains(item), "retirement inventory must include " + item);
        }
        for (String removed : List.of("阻塞", "半完成", "仍需分类", "产品未分类")) {
            assertTrue(!inventory.contains(removed), "retirement inventory must not contain stale word " + removed);
        }
    }

    @Test
    void phase6OperationalizationArtifactsCoverSrmAndNoMockEvidenceRule() throws Exception {
        String standard = Files.readString(Path.of(
                "../docs/engineering/unified-approval-platform-adapter-standard.md"), StandardCharsets.UTF_8);
        String template = Files.readString(Path.of(
                "../docs/engineering/unified-approval-platform-adapter-template.md"), StandardCharsets.UTF_8);
        Path operationsRunbook = Path.of(
                "../docs/engineering/unified-approval-platform-operations-runbook.md");

        assertTrue(Files.exists(operationsRunbook),
                "Phase6 must provide a unified approval operations runbook");
        String runbook = Files.readString(operationsRunbook, StandardCharsets.UTF_8);

        for (String item : List.of("SRM", "供应商门户审核", "applicationId", "srm:supplier-portal:review")) {
            assertTrue(standard.contains(item), "adapter standard must include SRM requirement " + item);
            assertTrue(template.contains(item), "adapter template must include SRM onboarding example " + item);
        }
        for (String item : List.of("监控", "审计", "告警", "SLA", "超时", "催办", "不得声明 REMINDER",
                "不得 mock 成功")) {
            assertTrue(runbook.contains(item), "operations runbook must include " + item);
        }
    }
}
