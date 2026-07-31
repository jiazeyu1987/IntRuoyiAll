# 20260727 Form Template Upgrade BPM Approval

## Task Goal

修复表单模板导入升版时报错 `Form template upgrade approval cannot be started: Form template upgrade requires BPM approval`，确保已有模板导入升版只能通过 BPM 审批流程启动，不允许被策略配置降级为 DIRECT。

## Milestones

- [x] 建立缺陷复现与 BDD/TDD 证据
- [x] 修复强制 BPM 的表单模板升版审批策略防护
- [x] 补充迁移/静态契约，防止错误 DIRECT 策略残留
- [x] 运行目标验证并记录结果
- [x] 完成 closeout 记录、提交与推送

## Expected Verification

- `mvn -pl yudao-module-bpm "-Dtest=BusinessApprovalPolicyAdministrationServiceTest" test`
- `mvn -pl yudao-module-bpm "-Dtest=FormTemplateUpgradeBusinessApprovalEffectExecutorTest,BusinessApprovalOrchestratorBpmRequiredTest" test`
- `python -X utf8 -m pytest script/tests/test_form_template_upgrade_bpm_seed.py`
- `python -X utf8 -m pytest script/tests/test_form_action_state_machine_release_contract.py script/tests/test_mes_route_version_publish_business_approval_policy_seed.py`
- `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260727-form-template-upgrade-bpm-approval/bug-regression-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260727-form-template-upgrade-bpm-approval/backend-api-evidence.md`

## Current Status

completed

## Cleanup Keep

- doc/tasks/20260727-form-template-upgrade-bpm-approval/bug-regression-evidence.md
- doc/tasks/20260727-form-template-upgrade-bpm-approval/backend-api-evidence.md

## 经验门禁

- 已读取 `docs/task-closeout-rules.md`、`docs/backend-development.md`、`docs/frontend-development.md`、`docs/database-rules.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md`。
- `docs/experience-index.md` 已存在；未命中专门的 `FORM_TEMPLATE_UPGRADE` 运行时错误经验文档，按 backend/database/Git/PowerShell 通用门禁执行。
- 当前工作区已有 52 项既有脏改动且 `int_main...origin/int_main [ahead 11]`，进入实现前需先隔离既有改动，避免本任务提交混入前序任务。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。本任务目标是移除/阻止错误 DIRECT 降级。
- `是否从根因和长期维护角度解决`：是。通过策略发布/切换防护与迁移契约约束强制 BPM executor。
- `是否存在临时补丁或绕过`：否。
