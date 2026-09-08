package cn.iocoder.yudao.module.signature.gxp;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GxpAuditTrailSchemaContractTest {

    private static final Path MIGRATION = Path.of("../sql/mysql/20260908_gxp_audit_trail_core.sql");
    private static final Path POLICY = Path.of("../../config/gxp-audit-policy.yaml");

    @Test
    void migrationDefinesAppendOnlyEventLedgerAndPolicyRegistry() throws IOException {
        String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT)
                .replace("`", "");

        assertTrue(sql.contains("create table if not exists gxp_audit_event"),
                "必须创建统一 GxP 审计事件账本");
        assertTrue(sql.contains("create table if not exists gxp_audit_policy_version"),
                "必须创建策略版本登记表");
        assertTrue(sql.contains("create table if not exists gxp_audit_coverage_report"),
                "必须创建覆盖登记报告表");
        assertTrue(sql.contains("create table if not exists gxp_audit_daily_manifest"),
                "必须创建审计日清单表");
        assertTrue(sql.contains("create table if not exists gxp_audit_seal_watermark"),
                "必须创建审计封存水位表");
        assertTrue(sql.contains("operation_id"), "审计事件必须保存 operationId");
        assertTrue(sql.contains("actor_id"), "审计事件必须保存操作人");
        assertTrue(sql.contains("server_occurred_at"), "审计事件必须由服务器生成正式时间");
        assertTrue(sql.contains("ledger_sequence"), "审计事件必须保存租户账本序号");
        assertTrue(sql.contains("before_state_json"), "审计事件必须保存前值状态信封");
        assertTrue(sql.contains("after_state_json"), "审计事件必须保存后值状态信封");
        assertTrue(sql.contains("event_hash"), "审计事件必须保存事件 hash");
        assertTrue(sql.contains("previous_event_hash"), "审计事件必须保存前序 hash");
        assertTrue(sql.contains("canonical_event_json"), "审计事件必须保存规范化 JSON");
        assertTrue(sql.contains("unique key uk_gxp_audit_event_idempotency"),
                "审计事件必须具备租户内幂等约束");
        assertTrue(sql.contains("unique key uk_gxp_audit_event_sequence"),
                "审计事件必须具备租户内账本序号唯一约束");
        assertTrue(sql.contains("trg_gxp_audit_event_no_update"),
                "审计事件必须有禁止 UPDATE 的数据库触发器");
        assertTrue(sql.contains("trg_gxp_audit_event_no_delete"),
                "审计事件必须有禁止 DELETE 的数据库触发器");
        assertTrue(sql.contains("trg_gxp_audit_daily_manifest_no_update"),
                "日清单必须有禁止 UPDATE 的数据库触发器");
        assertTrue(sql.contains("trg_gxp_audit_seal_watermark_no_delete"),
                "封存水位必须有禁止 DELETE 的数据库触发器");
        assertTrue(sql.contains("signal sqlstate '45000'"),
                "append-only 约束必须 fail-fast，不能静默忽略");

        String eventTable = sql.substring(sql.indexOf("create table if not exists gxp_audit_event"),
                sql.indexOf("create table if not exists gxp_audit_policy_version"));
        assertFalse(eventTable.contains(" deleted "), "统一审计事件账本不能使用软删除字段");
        assertFalse(eventTable.contains(" updater "), "统一审计事件账本不能包含更新人字段");
        assertFalse(eventTable.contains(" update_time "), "统一审计事件账本不能包含更新时间字段");
    }

    @Test
    void policyRegistryContainsRequiredMachineReadableFields() throws IOException {
        String policy = Files.readString(POLICY, StandardCharsets.UTF_8);

        assertTrue(policy.contains("schemaVersion:"), "策略登记必须声明 schemaVersion");
        assertTrue(policy.contains("policyVersion:"), "策略登记必须声明 policyVersion");
        assertTrue(policy.contains("operationId:"), "策略登记必须声明 operationId");
        assertTrue(policy.contains("sourceLocator:"), "策略登记必须声明 sourceLocator");
        assertTrue(policy.contains("reasonPolicy:"), "策略登记必须声明 reasonPolicy");
        assertTrue(policy.contains("signaturePolicy:"), "策略登记必须声明 signaturePolicy");
        assertTrue(policy.contains("retentionClass:"), "策略登记必须声明 retentionClass");
        assertTrue(policy.contains("testIds:"), "策略登记必须声明 testIds");
        assertTrue(policy.contains("owner:"), "策略登记必须声明 owner");
        assertTrue(policy.contains("applicabilityDecision:"), "策略登记必须声明 applicabilityDecision");
    }

}
