# Execution Log

## Intent

- 用户要求将业务审批策略列表默认筛选改成 `policyMode = BPM_REQUIRED`。
- 用户进一步澄清：只显示一个 BPM 审批也不对，文控、表单、批记录等审批流程都应出现在默认可开关视图里。

## BDD

- BDD: 默认只看 BPM 审批策略 -> Given 管理员打开业务审批策略页面 / When 页面首次加载策略列表 / Then 请求参数默认带 `policyMode = BPM_REQUIRED`，默认列表只展示 BPM 审批策略。
- BDD: 默认展示可开关审批策略 -> Given 管理员打开业务审批策略页面 / When 页面首次加载策略列表 / Then 请求默认使用可开关审批视图，展示文控、表单、批记录等顶层策略，并排除 eDHR 路线表单明细。

## Milestone Updates

- M1: 已创建任务目录，读取 `docs/frontend-development.md`、`docs/task-closeout-rules.md`、`docs/powershell-encoding.md`、`docs/experience-index.md`、`frontend-feature-delivery` 技能和 `references/frontend-contract.md`。
- M2: 已在 `bpm-business-approval-policy-static.spec.js` 增加默认 `BPM_REQUIRED` 筛选静态契约。
- M3: 已将业务审批策略页 `queryParams.policyMode` 默认值改为 `BPM_REQUIRED`。
- M4: 定向静态契约、frontend evidence validator、UTF-8 读取和限定 diff check 均已通过；任务状态更新为 `ready_for_closeout`。
- Scope correction: 用户澄清默认 `policyMode=BPM_REQUIRED` 过窄，本任务重新进入 `in_progress`，改为新增后端 `approvalSwitchScope` 查询口径并调整前端默认参数。

## Verification Evidence

- RED: `node tests/e2e/bpm-business-approval-policy-static.spec.js` -> FAIL，旧实现 `queryParams.policyMode` 为 `undefined`，默认列表不限制 BPM 审批策略。
- GREEN: `node tests/e2e/bpm-business-approval-policy-static.spec.js` -> PASS，默认筛选契约通过。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260804-bpm-policy-default-bpm-required/frontend-feature-evidence.md` -> PASS。
- GREEN: `git diff --check -- <task-owned paths>` -> PASS，仅提示两个前端文件下次 Git 触碰时 LF 会转 CRLF。
- GREEN: `python -X utf8 -c "...read_text(encoding='utf-8')..."` -> PASS，任务文档 UTF-8 可读。
- PROJECT_EXPERIENCE: 已读取 `project-experience-consolidation`；本次只是默认筛选小改动，既有业务审批策略和前端静态契约门禁已覆盖，无需新增长期经验文档。
- CLOSEOUT_PREVIEW: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260804-bpm-policy-default-bpm-required --mode preview` -> PASS，keep 包含 task.md、execution-log.md、verification-report.md、frontend-feature-evidence.md；delete/blocked/warnings 均为 `<none>`。

## Blockers

- 当前工作区进入本任务前已有大量其他任务的已暂存、未暂存和未跟踪改动，且分支已领先 `origin/int_main`；本任务只触碰本次页面、现有目标静态契约和本任务文档，未执行提交/推送，避免混入无关并行任务改动。
