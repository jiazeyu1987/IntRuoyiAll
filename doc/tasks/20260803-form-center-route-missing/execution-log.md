# Execution Log

## User Intent

- 用户报告：`请求地址不存在:admin-api/form-center/templates/28/versions/V3.0`。
- 期望：运行态打开 FormCenter 动态表单不应触发不存在或无权限的模板管理版本接口。

## Milestone Evidence

- BDD: 运行态 FormCenter 表单使用 openTask 快照 -> Given 用户通过 eDHR/FormCenter 运行态打开模板 28 版本 V3.0 的动态表单, When 前端渲染业务表单面板, Then 前端应优先使用 openTask 返回的 `formTemplateJimuSchemaJson` 与识别字段渲染，不请求 `/form-center/templates/28/versions/V3.0` 管理接口。
- Root Cause: `ActionFormPanel` 在 `resolveEmbeddedTemplateVersionForActionForm()` 返回空时执行 `getTemplateVersion(templateId, versionNo)`，导致运行态表单依赖模板管理接口 `/form-center/templates/{id}/versions/{versionNo}`。普通填写人或运行环境缺少该接口/权限时会出现“请求地址不存在”或 403。
- Fix: `ActionFormPanel` 不再导入或调用 `getTemplateVersion`；有 `formTemplateId + formTemplateVersionNo` 但缺少 `openTask` 嵌入模板快照时，直接可见失败 `动态表单运行态缺少 openTask 模板快照，无法渲染。`，避免 silent downgrade 和错误请求。
- Gate: 已按经验索引命中 `FormCenter 动态表单字段码渲染门禁`、`FormCenter 嵌入模板对象类型契约门禁`、`切换填写人 FormCenter 槽位导航门禁`，并把摘要补入 `task.md`。

## Command Evidence

- READ: `docs/task-closeout-rules.md`、`docs/frontend-development.md`、`docs/backend-development.md`、`docs/powershell-memory.md`、`docs/powershell-encoding.md`、`docs/worktree-restrictions.md` 已读取。
- READ: `bug-regression-fix-loop` 技能及 `references/bug-contract.md` 已读取。
- READ: `frontend-feature-delivery` 技能及 `references/frontend-contract.md` 已读取。
- WORKTREE: 创建隔离分支 `codex/form-center-route-missing-20260803` 于 `D:\IntRuoyiWorktree\form-center-route-missing-20260803`，避免混入 `E:\IntRuoyi` 既有脏改动。
- RED: `node tests/e2e/edhr-switch-filler-formcenter-slot-static.spec.js` -> FAIL, expected reason: 旧实现仍包含 `getTemplateVersion(templateId, versionNo)` 运行态管理接口兜底。
- GREEN: `node tests/e2e/edhr-switch-filler-formcenter-slot-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-dynamic-form-action-panel-prefill-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/form-center-static.spec.js` -> PASS。
- REGRESSION: `node tests/e2e/edhr-work-task-formcenter-navigation-static.spec.js` -> PASS。
- REGRESSION: `node tests/e2e/edhr-loss-form-open-action-static.spec.js` -> PASS。
- REGRESSION: `node tests/e2e/form-center-action-projection-static.spec.js` -> PASS。
- PRECONDITION: First `pnpm ts:check` failed because new worktree lacked `node_modules` and `cross-env` was not installed; ran `pnpm install --frozen-lockfile` successfully with packages reused from local store and no lockfile change expected.
- GREEN: `pnpm ts:check` -> PASS.
- VALIDATOR: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260803-form-center-route-missing\bug-regression-evidence.md` -> PASS.
- VALIDATOR: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260803-form-center-route-missing\frontend-feature-evidence.md` -> PASS.
- EXPERIENCE: 已按 `project-experience-consolidation` 合并关键词 `请求地址不存在` 到 `docs/frontend-development.md#切换填写人-formcenter-槽位导航门禁` 与 `docs/experience-index.md`。
- CHECK: `rg -n "请求地址不存在" docs\experience-index.md docs\frontend-development.md` -> PASS，索引可定位新关键词。
- CHECK: `git diff --check` -> PASS，仅 CRLF 工作区提示，无 whitespace error。
- CLOSEOUT: `task_closeout.py --task-id 20260803-form-center-route-missing --mode preview` -> BLOCKED，原因：主工作区 `E:\IntRuoyi` 脏，且 e2e/经验文档需声明为本任务保留文件。
- CLOSEOUT: 已在 `task.md` 增加 `Cleanup Keep`，保留本任务 e2e 静态契约和经验索引/规则文档。
- CLOSEOUT: `task_closeout.py --task-id 20260803-form-center-route-missing --mode preview --worktree-closeout off` -> READY，仅删除临时 evidence。
- CLOSEOUT: `task_closeout.py --task-id 20260803-form-center-route-missing --mode apply --worktree-closeout off` -> APPLIED，删除 `bug-regression-evidence.md` 与 `frontend-feature-evidence.md`，保留 `task.md`、`execution-log.md`、`verification-report.md`。
- CLOSEOUT: worktree 合并/删除未执行；原因是主工作区 `E:\IntRuoyi` 存在大量非本任务脏改动，不能接收 ff-only merge。
- WORKTREE SLOT: `reserve-worktree-slot.ps1 -Name form-center-route-missing-20260803 -Path D:\IntRuoyiWorktree\form-center-route-missing-20260803 -Branch codex/form-center-route-missing-20260803 -Profile int_main -AsJson` -> PASS，slot `13`，frontend `8094`，backend `48094`。
- GUARD: first `scripts\preflight\branch-runtime-port-guard.ps1` -> FAIL, expected reason: isolated worktree had no port registry entry yet.
- GUARD: after slot reservation, `scripts\preflight\branch-runtime-port-guard.ps1` -> PASS for `codex/form-center-route-missing-20260803/int_main`, frontend `8094`, backend `48094`.

## Current Status

- ready_for_closeout: 实现、定向验证、证据验证器、经验沉淀、diff 检查和 cleanup apply 完成，准备提交并推送分支。
