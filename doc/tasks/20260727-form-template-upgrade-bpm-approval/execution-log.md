# Execution Log

## 2026-07-27

- USER: 点击“导入表单模板”时报错 `Form template upgrade approval cannot be started: Form template upgrade requires BPM approval`，截图显示已有模板“过程检验记录”上传导入升版。
- PRECHECK: 读取 bug-regression-fix-loop、backend-api-delivery 技能及 backend/frontend/database/task-closeout/PowerShell/Git 规则。
- PRECHECK: `git status --short --branch` -> `int_main...origin/int_main [ahead 11]` 且 52 项既有脏改动；本任务实现前需隔离。
- ROOT-CAUSE-CANDIDATE: 报错文本来自 `FormTemplateUpgradeBusinessApprovalEffectExecutor#executeDirect`；说明运行时策略被解析为 `DIRECT`，而表单模板升版 executor 明确要求 BPM。
- BDD: Existing template import starts BPM approval -> Given 已有表单模板上传新 doc/docx 升版 When 导入服务提交 `FORM_TEMPLATE/UPGRADE/DRAFT` 审批 Then 必须启动 `form-template-upgrade-v1` BPM 流程并返回审批申请与流程实例，不允许 DIRECT 生效。
- BDD: Mandatory BPM executor cannot be downgraded -> Given `FORM_TEMPLATE_UPGRADE` 或 `FORM_TEMPLATE_OBSOLETE` 策略已发布 When 管理端尝试切换为 `DIRECT` 或 `SIGNATURE_REQUIRED` Then 发布/切换必须 fail-fast，旧 BPM 策略保持发布状态。
- BDD: Migration preserves BPM-required upgrade policy -> Given 旧环境已有错误的升版 DIRECT 策略 When 执行升级策略 seed Then 必须把目标租户策略校正为 `BPM_REQUIRED/form-template-upgrade-v1/FORM_TEMPLATE_UPGRADE`，且重复发布策略仍 fail-fast。
- BASELINE: `git commit -m "chore: baseline existing dirty IntRuoyi work"` -> `5b0bf00b`，包含本任务开始前 52 个既有脏改动；本任务目录已从 baseline staged set 排除。
- BASELINE-VERIFY: `git show --name-status --oneline -1` -> `5b0bf00b chore: baseline existing dirty IntRuoyi work`；记录文件清单见 Git 输出。
- PARALLEL-DIRTY: baseline 后出现无关 `IntRuoyiFronted/tests/e2e/edhr-cell-control-type-switch-static.spec.js` 与 `doc/tasks/20260727-batch-record-attachment-owner-config/`，不属于本任务且不在当前修复目标范围内。
