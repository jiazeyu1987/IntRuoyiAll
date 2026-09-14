# 20260727 Form Template Approval Mode Respects Policy

## Task Goal

更正上一轮“表单模板升版/作废强制 BPM”的实现：表单模板升版、作废应严格按业务审批策略执行，配置 `DIRECT` 就直接生效，配置 `BPM_REQUIRED` 才启动 BPM 审批。

## Milestones

- [x] 建立更正需求与 RED 回归
- [x] 修正表单模板升版/作废 direct 与 BPM 行为
- [x] 修正错误的强制 BPM seed / 经验门禁
- [x] 运行目标验证并记录证据
- [x] 完成 cleanup、提交与推送

## Expected Verification

- `mvn -pl yudao-module-bpm "-Dtest=BusinessApprovalPolicyAdministrationServiceTest,FormTemplateUpgradeBusinessApprovalEffectExecutorTest,FormTemplateObsoleteBusinessApprovalEffectExecutorTest" test`
- `mvn -pl yudao-module-bpm "-Dtest=BusinessApprovalOrchestratorBpmRequiredTest" test`
- `python -X utf8 -m pytest script/tests/test_form_template_upgrade_bpm_seed.py script/tests/test_form_template_obsolete_bpm_policy_seed.py`
- `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260727-form-template-approval-mode-respects-policy/bug-regression-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260727-form-template-approval-mode-respects-policy/backend-api-evidence.md`

## 经验门禁

- Trigger: 表单模板升版/作废、`FORM_TEMPLATE_UPGRADE`、`FORM_TEMPLATE_OBSOLETE`、业务审批策略切换 DIRECT/BPM_REQUIRED。
- Preflight check: published 策略模式是权威配置；`DIRECT` 必须直接执行，`BPM_REQUIRED` 必须使用对应流程 key 并启动 BPM。
- Blocker: 代码硬编码强制 BPM、seed 覆盖已发布 DIRECT、或 BPM_REQUIRED 被静默直通时必须阻塞。
- Verification: 目标 Maven、seed 静态合同和 BPM_REQUIRED orchestrator 相邻测试均通过。
- Forbidden action: 禁止把 DIRECT 当降级拦截，禁止用 seed 或手工 SQL 覆盖用户显式配置。
- Evidence: `docs/backend-development.md#业务审批策略按配置执行门禁`。

## Current Status

completed

## Cleanup Keep

- doc/tasks/20260727-form-template-approval-mode-respects-policy/bug-regression-evidence.md
- doc/tasks/20260727-form-template-approval-mode-respects-policy/backend-api-evidence.md

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。DIRECT 是显式业务策略，不是 fallback；BPM_REQUIRED 仍 fail-fast 要求 BPM。
- `是否从根因和长期维护角度解决`：是。修正 executor 与策略管理契约，使配置语义一致。
- `是否存在临时补丁或绕过`：否。
