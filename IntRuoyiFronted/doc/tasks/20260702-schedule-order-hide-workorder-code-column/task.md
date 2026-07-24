# 任务：排产工单主列表隐藏工单编码列

- Task ID: 20260702-schedule-order-hide-workorder-code-column
- Created: 2026-07-02
- Current Status: completed

## Current Status

completed

## Task Goal

修复排产工单主列表仍显示“工单编码/工单编号”列的问题：保留查询区的工单编码筛选能力，但主表列头与行内容不再展示工单编码，避免与用户要求的排产工单列表隐藏编码展示冲突。

## Milestones

1. RED：补充静态回归测试，复现主列表仍显示工单编码列。completed
2. GREEN：移除主表工单编码列，保留查询条件。completed
3. REGRESSION：运行排产工单静态契约与证据校验。completed
4. CLOSEOUT：记录验证结果并提交本任务改动。completed

## Expected Verification

- `node tests/e2e/mes-schedule-order-freeze-visibility-static.spec.js`
- `node tests/e2e/mes-pro-schedule-order-pool-static.spec.js`
- `node --check tests/e2e/mes-schedule-order-freeze-visibility-static.spec.js`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260702-schedule-order-hide-workorder-code-column/frontend-feature-evidence.md`

## 经验门禁

- 已读取 `docs/experience-index.md`，本任务命中 PowerShell / Windows shell、前端页面 / 表格 / 样式。
- 已读取 `docs/powershell-memory.md`，PowerShell 命令设置 UTF-8 输入输出，中文文件读写使用显式 UTF-8 或 `apply_patch`。
- 已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`，表格列调整保持现有运营控制台风格，不引入视觉重设计。
- 已读取 `frontend-feature-delivery` 与 `bug-regression-fix-loop` 契约，按 BDD + RED/GREEN 记录证据。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，直接修正排产工单主表列定义与静态契约。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- Given 排产员进入排产工单主列表, When 查看主表列头, Then 主表不显示“工单编码/工单编号”列，首个业务数据列为产品编号。
- Given 排产员需要按工单编码筛选, When 查看查询区, Then 查询条件仍保留“工单编码”输入框。
- Given 排产工单处于冻结状态, When 排产员浏览列表, Then 冻结醒目样式保持不变。

## Current Blockers

- 暂无。

## Cleanup Keep

- `doc/tasks/20260702-schedule-order-hide-workorder-code-column/frontend-feature-evidence.md`

## Final Verification Result

- `node --check tests/e2e/mes-schedule-order-freeze-visibility-static.spec.js` -> PASS。
- `node --check tests/e2e/mes-schedule-order-workorder-link-static.spec.js` -> PASS。
- `node tests/e2e/mes-schedule-order-freeze-visibility-static.spec.js` -> PASS。
- `node tests/e2e/mes-schedule-order-workorder-link-static.spec.js` -> PASS。
- `node tests/e2e/mes-pro-schedule-order-pool-static.spec.js` -> PASS。
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260702-schedule-order-hide-workorder-code-column/frontend-feature-evidence.md` -> PASS。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260702-schedule-order-hide-workorder-code-column --mode preview` -> PASS，无删除项、无阻塞。
