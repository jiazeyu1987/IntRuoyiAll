# Verification Report

## Summary

表单模板升版/作废审批模式已改为按 published 业务审批策略执行：`DIRECT` 直接生效，`BPM_REQUIRED` 必须启动 BPM。旧的强制 BPM 代码、executor 抛错路径和 seed 覆盖策略已移除或纠正。

## Commands

- `mvn -pl yudao-module-bpm "-Dtest=BusinessApprovalPolicyAdministrationServiceTest,FormTemplateUpgradeBusinessApprovalEffectExecutorTest,FormTemplateObsoleteBusinessApprovalEffectExecutorTest" test` -> PASS，32 tests passed。
- `python -X utf8 -m pytest script/tests/test_form_template_upgrade_bpm_seed.py script/tests/test_form_template_obsolete_bpm_policy_seed.py` -> PASS，7 tests passed。
- `mvn -pl yudao-module-bpm "-Dtest=BusinessApprovalOrchestratorBpmRequiredTest" test` -> PASS，4 tests passed。
- `rg -n "BPM_REQUIRED_EFFECT_EXECUTOR_CODES|Business approval executor requires BPM_REQUIRED|Form template upgrade requires BPM approval|Form template obsolete requires BPM approval" ...` -> PASS，生产/测试代码无旧硬编码错误路径残留。

## Result

- DIRECT 升版：DRAFT 版本直接更新为 `PUBLISHED`。
- DIRECT 作废：当前版本直接更新为 `OBSOLETE`。
- BPM_REQUIRED 升版/作废：仍要求流程 key 与 process instance，流程缺失时 fail fast。
- seed：默认可补 BPM_REQUIRED 策略，但不会覆盖已发布 DIRECT 策略。

## Residual Risk

未执行真实前端导入路径 E2E；本次为后端策略和 executor 层修复，已用单元测试和 SQL 合同锁定核心行为。
