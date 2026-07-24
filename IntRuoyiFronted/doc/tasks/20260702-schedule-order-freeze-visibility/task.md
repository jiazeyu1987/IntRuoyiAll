# 任务：排产工单冻结展示优化

- Task ID: 20260702-schedule-order-freeze-visibility
- Created: 2026-07-02
- Current Status: completed

## Current Status

completed

## Task Goal

修复排产工单列表展示问题：主列表不再显示排产编码列，冻结状态必须比普通标签更明显，便于排产员快速识别冻结工单。

## Milestones

1. RED：补充静态回归测试，锁定隐藏排产编码列和冻结态醒目样式要求。completed
2. GREEN：最小修改排产工单页主列表展示，不改后端契约和冻结接口。completed
3. REGRESSION：运行相关排产工单静态测试、语法检查和证据校验。completed
4. CLOSEOUT：记录最终验证结果并只保留本任务相关改动。completed

## Expected Verification

- `node tests/e2e/mes-schedule-order-freeze-visibility-static.spec.js`
- `node tests/e2e/mes-schedule-order-main-table-wrap-static.spec.js`
- `node tests/e2e/mes-pro-schedule-order-pool-static.spec.js`
- `node --check tests/e2e/mes-schedule-order-freeze-visibility-static.spec.js`
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260702-schedule-order-freeze-visibility/bug-regression-evidence.md`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260702-schedule-order-freeze-visibility/frontend-feature-evidence.md`

## 经验门禁

- 已读取 `docs/experience-index.md`，本任务命中 PowerShell / Windows shell、前端页面 / 表格 / 样式。
- 已读取 `docs/powershell-memory.md`，PowerShell 命令设置 UTF-8 输入输出，中文文件读写使用显式 UTF-8 或 `apply_patch`。
- 已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`，列表样式沿用蓝/中性色运营控制台风格，冻结态使用清晰但不引入整页重设计的视觉强调。
- 已读取 `frontend-feature-delivery` 与 `bug-regression-fix-loop` 契约，按 BDD + RED/GREEN 记录证据。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，直接调整主列表列定义与冻结态渲染/样式，避免依赖隐藏文案或临时说明。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- Given 排产员进入排产工单主列表, When 查看列表列头, Then 主列表不显示排产编码列，首个业务识别列为工单编码。
- Given 排产工单处于冻结状态, When 排产员浏览列表, Then 冻结状态以醒目的冻结徽标/锁图标/冻结行样式展示，并继续通过 tooltip 暴露冻结原因。
- Given 排产工单未冻结, When 排产员浏览列表, Then 未冻结状态保持低视觉权重，不干扰异常识别。

## Current Blockers

- 暂无。

## Cleanup Keep

- `doc/tasks/20260702-schedule-order-freeze-visibility/bug-regression-evidence.md`
- `doc/tasks/20260702-schedule-order-freeze-visibility/frontend-feature-evidence.md`

## Final Verification Result

- `node --check tests/e2e/mes-schedule-order-freeze-visibility-static.spec.js` -> PASS。
- `node tests/e2e/mes-schedule-order-freeze-visibility-static.spec.js` -> PASS。
- `node tests/e2e/mes-schedule-order-main-table-wrap-static.spec.js` -> PASS。
- `node tests/e2e/mes-pro-schedule-order-pool-static.spec.js` -> PASS。
- `validate_bug_regression.py` -> PASS。
- `validate_frontend_feature.py` -> PASS。
