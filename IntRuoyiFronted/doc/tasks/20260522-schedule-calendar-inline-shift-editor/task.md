# Task: 排程日历内联班次编辑替代右侧按日期覆盖班次

## Goal

删除排程日历右侧当前的“按日期覆盖班次”编辑区，改为直接在月历格子上编辑班次，并保持现有 `保存规则` 接口与规则数据结构不变。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\mes\pro\task\calendar\index.vue`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\api\mes\pro\workorder\bom\index.ts`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\schedule-calendar-inline-shift-editor.test.mjs`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-schedule-calendar-inline-shift-editor\**`

## Non-Scope

- 不修改后端规则接口。
- 不改动排程日历正式排程接口。
- 不调整预览问题弹框、工单详情页、工序/工作站设置页。
- 不引入即时保存、fallback 或 mock 数据。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-schedule-calendar-preview-overlay\task.md`
- Status before this task: `Completed`
- Impact: 预览 overlay 修复已完成，不阻塞本次继续在同一页面追加“日历内联班次编辑”交互。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Current state: 仓库存在与本任务无关的在途改动与任务目录。
- Impact: 本任务只允许修改排程日历页、一个直接相关 API 类型文件、专用静态测试和本任务文档；提交时单独暂存本任务文件。

## Milestones

- [x] M1: 建立任务文档并锁定交互方案。
- [x] M2: 先补 RED 测试，锁定“删右侧编辑区、改为日历格子内联班次编辑”的行为。
- [x] M3: 实现日历格子编辑面板、过去日期只读、恢复默认和规则变更提示。
- [x] M4: 跑定向静态回归与 ESLint。
- [x] M5: 回写证据并执行 closeout preview。

## Expected Verification

- `node --test scripts/schedule-calendar-inline-shift-editor.test.mjs scripts/schedule-calendar-preview-overlay.test.mjs scripts/schedule-calendar-issue-tabs-and-jumps.test.mjs`
- `pnpm exec eslint src/views/mes/pro/task/calendar/index.vue src/api/mes/pro/workorder/bom/index.ts scripts/schedule-calendar-inline-shift-editor.test.mjs scripts/schedule-calendar-preview-overlay.test.mjs scripts/schedule-calendar-issue-tabs-and-jumps.test.mjs`
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-schedule-calendar-inline-shift-editor\frontend-feature-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260522-schedule-calendar-inline-shift-editor --mode preview`

## Current Status

Completed.

## Completed Work

- 删除了右侧 `按日期覆盖班次` 的日期选择器、班次下拉、添加按钮和覆盖日期列表。
- 改为在月历格子内直接点击打开小面板，提供 `白班 / 夜班 / 双班 / 休息` 四个固定选项。
- 同一天再次点击已选日期时，可直接收起班次编辑面板。
- 对早于系统今天的日期增加只读限制，tooltip 文案为 `仅可修改今天及未来日期`。
- 为今天及未来日期增加更明确的轻量提示：
  - 可编辑日期显示 `可编辑`
  - 已存在覆盖规则的日期显示 `已覆盖`
  - 可编辑日期 hover 时显示更明显的蓝色提示态
- 为避免功能回退，保留了 `恢复默认` 动作，用来删除该日期的覆盖规则。
- 班次修改只更新本地 `rulesForm.dateShiftModeByDate` 草稿，仍通过现有 `保存规则` 持久化。
- 修改后增加了明确的重排提示：
  - 无预览时：`班次规则已变更，请先保存规则后重新排产`
  - 有预览时：`班次规则已变更，请重新生成预览后重新排产`
  - 保存成功 toast：`排程规则已更新，请重新生成预览后再发布排产`
- 同时保留并兼容现有预览 overlay：预览任务视图和规则草稿不会互相覆盖。

## Verification Result

- PASS: `node --test scripts/schedule-calendar-inline-shift-editor.test.mjs scripts/schedule-calendar-preview-overlay.test.mjs scripts/schedule-calendar-issue-tabs-and-jumps.test.mjs`
- PASS: `pnpm exec eslint src/views/mes/pro/task/calendar/index.vue src/api/mes/pro/workorder/bom/index.ts scripts/schedule-calendar-inline-shift-editor.test.mjs scripts/schedule-calendar-preview-overlay.test.mjs scripts/schedule-calendar-issue-tabs-and-jumps.test.mjs`
- PASS: `git diff --check -- src/views/mes/pro/task/calendar/index.vue src/api/mes/pro/workorder/bom/index.ts scripts/schedule-calendar-inline-shift-editor.test.mjs scripts/schedule-calendar-preview-overlay.test.mjs scripts/schedule-calendar-issue-tabs-and-jumps.test.mjs`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-schedule-calendar-inline-shift-editor\frontend-feature-evidence.md`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260522-schedule-calendar-inline-shift-editor --mode preview`

## Remaining Notes

- 这次没有追加真实浏览器验收截图，因为当前本地浏览器默认登录租户与刚才用于后端预览复现的数据租户不一致，静态回归和 lint 作为主验证证据。
- `frontend-feature-evidence.md` 已生成并通过校验，按当前 closeout 规则属于可清理附属证据文件，不纳入提交。
