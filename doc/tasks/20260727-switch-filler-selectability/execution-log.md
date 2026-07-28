# Execution Log

## User Intent

- 用户反馈“切换填写人”现在无法切换，弹窗中另外 2 个人无法选择；截图显示当前选中“王歆”，候选人“任丹”“张可莹”呈不可选择状态。

## BDD

- BDD: 金手指可选择其他填写人 -> Given 当前工序详情接口返回多个 `fillableUsers` 且当前账号具备金手指/代填权限 When 用户打开“切换填写人”弹窗 Then 非当前登录用户的可填写候选项也应保持可点击，不应被 `currentAssistUserId` 硬禁用。
- BDD: 普通用户仍保留后端校验 -> Given 当前账号不具备金手指权限 When 用户尝试打开不可处理的填写任务 Then 前端不吞异常，后端 `openTask` 失败应在当前弹窗中显示真实错误。

## RED/GREEN

- RED: `node tests/e2e/edhr-switch-filler-selectability-static.spec.js` -> FAIL, 现有 `isAssistFillerSwitchItemSelectable` 不包含 `hasGoldenFingerPermission.value`，非当前登录用户候选项会被 UI 禁用。
- GREEN: `node --check tests/e2e/edhr-switch-filler-selectability-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-switch-filler-selectability-static.spec.js` -> PASS, `PASS: edhr switch filler selectability static contract`。
- REGRESSION: `pnpm exec eslint src/views/mes/pro/edhr/ExecutionPage.vue tests/e2e/edhr-switch-filler-selectability-static.spec.js --format stylish` -> PASS。
- REGRESSION-BLOCKED: `node tests/e2e/edhr-assist-fill-mode-static.spec.js` -> FAIL, 先失败于既有宽合同 `填写人列表必须区分 MAIN 批处理表单和工艺路线表单槽位`，与本次可选态修复无关。
- GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260727-switch-filler-selectability/bug-regression-evidence.md` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260727-switch-filler-selectability/frontend-feature-evidence.md` -> PASS。
- GREEN: project-experience-consolidation -> PASS, 已有 `docs/frontend-development.md#前端静态契约隔离门禁`、`docs/e2e-rules.md#eDHR 单据填写人显示值门禁` 与 `docs/backend-development.md#eDHR 详情回填门禁` 覆盖本次经验，无需新增长期经验文档。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260727-switch-filler-selectability --mode preview` -> PASS, keep 5 files, delete none, blocked none, warnings none。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260727-switch-filler-selectability --mode apply` -> PASS, deleted_paths none。

## Root Cause

- `ExecutionPage.vue` 的 `isAssistFillerSwitchItemSelectable` 使用 `currentAssistUserId() === item.userId && isAssistBatchTaskOpenable(item.task)` 判断候选项可选态，导致详情接口已经返回的其他 `fillableUsers` 在 UI 层直接被禁用，金手指/代填能力无法进入后端正式校验。

## Milestone Updates

- 建立隔离任务证据：completed。
- 定位候选人禁用根因：completed。
- RED 静态合同：completed。
- 前端最小修复：completed。
- GREEN/回归验证：completed。
- 经验门禁：completed。
- 证据验证：completed。
- cleanup preview/apply：completed。
- final status：completed。

## Verification Evidence

- `node --check tests/e2e/edhr-switch-filler-selectability-static.spec.js` -> PASS。
- `node tests/e2e/edhr-switch-filler-selectability-static.spec.js` -> PASS。
- `pnpm exec eslint src/views/mes/pro/edhr/ExecutionPage.vue tests/e2e/edhr-switch-filler-selectability-static.spec.js --format stylish` -> PASS。
- `node tests/e2e/edhr-assist-fill-mode-static.spec.js` -> FAIL on unrelated existing redbox cleanup/static-contract drift before this task's assertion scope.
- `validate_bug_regression.py` -> PASS。
- `validate_frontend_feature.py` -> PASS。

## Blockers

- 工作区开始时已有未提交改动与本地分支领先 origin 的提交；本任务将避免修改无关文件，提交/推送阶段需按项目规则单独处理。
