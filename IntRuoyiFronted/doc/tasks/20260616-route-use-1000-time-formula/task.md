# 工艺排产路线 1000 产品制作时间配置

## 任务目标

- 在工艺排产路线配置表中，用可点击的 `1000产品制作时间(h)` 列替换 `系数`、`固定值` 两列。
- 仅 `无限公式产能` 模式显示可点击时间；`有限小时产能` 显示 `--` 且不可点击。
- 点击时间打开弹框，按小时设置 `a` 和 `b`，展示 `1000 * a + b` 的小时结果和计算说明。
- 保持后端字段分钟口径不变，前端读写时进行小时/分钟换算。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- 适用强制门禁：
  - 前端表格遵循 IntPP 操作台风格，保持紧凑、可扫描、固定列宽。
  - 本次不新增 fallback、降级、静默错误或模拟数据。
  - 后端分钟契约不变，前端只做显式单位换算。

## 上一任务检查

- 前端仓上一相关任务 `20260616-route-use-config-display-tuning` 已标记 `COMPLETED`。
- 仓内仍有未跟踪历史任务目录 `20260615-frontend-build-babel-helper-missing/`，本轮不修改、不提交。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。通过显式小时/分钟转换函数隔离前端展示口径与后端存储口径，避免隐式单位混用。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- BDD: 公式模式显示 1000 产品制作时间 -> Given 某工序产能模式为无限公式产能且已有分钟口径 a/b / When 用户打开排产用途配置 / Then 表格显示按小时换算后的 `1000产品制作时间(h)`，结果保留 2 位小数。
- BDD: 有限小时产能不可配置公式时间 -> Given 某工序产能模式为有限小时产能 / When 用户查看配置表格 / Then `1000产品制作时间(h)` 显示 `--` 且不可点击。
- BDD: 弹框按小时维护 a 和 b -> Given 用户点击 1000 产品制作时间 / When 在弹框填写 `a` 和 `b` 并确认 / Then 当前行本地更新小时口径数据，表格实时显示 `1000 * a + b` 的 2 位小时结果。
- BDD: 保存仍提交分钟口径 -> Given 用户已按小时维护公式参数 / When 点击保存用途配置 / Then 前端提交给后端的 `infiniteDurationQuantityFactor` 和 `infiniteDurationBaseMinutes` 均按分钟口径换算。

## 里程碑

1. M1：建立任务文档、经验门禁和证据草稿。`DONE`
2. M2：RED：新增/更新静态契约测试，确认当前组件仍是系数/固定值两列且缺少弹框。`DONE`
3. M3：GREEN：实现计算列、弹框、小时/分钟换算和校验。`DONE`
4. M4：REGRESSION：运行静态回归、类型检查、证据校验和收尾预览。`DONE`

## 预期验证

- `node tests/e2e/mes-route-use-config-display-static.spec.js`
- `node tests/e2e/mes-process-use-route-tabs-static.spec.js`
- `node tests/e2e/mes-pro-schedule-order-pool-static.spec.js`
- `node tests/e2e/mes-edhr-multi-batch-route-static.spec.js`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260616-route-use-1000-time-formula/frontend-feature-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260616-route-use-1000-time-formula --mode preview`

## 当前状态

- 状态：COMPLETED。
- 恢复说明：用户已明确要求继续实现本计划，本轮从 M3 恢复执行。
- 最终验证：
  - `node tests\e2e\mes-route-use-config-display-static.spec.js` -> PASS。
  - `node tests\e2e\mes-process-use-route-tabs-static.spec.js` -> PASS。
  - `node tests\e2e\mes-pro-schedule-order-pool-static.spec.js` -> PASS。
  - `node tests\e2e\mes-edhr-multi-batch-route-static.spec.js` -> PASS。
  - `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
  - `validate_frontend_feature.py --evidence doc\tasks\20260616-route-use-1000-time-formula\frontend-feature-evidence.md` -> PASS。
  - `task_closeout.py --task-id 20260616-route-use-1000-time-formula --mode preview` -> PASS，delete/blocked/warnings 均为 `<none>`。
- 未完成的环境复验：真实弹框复验因本机测试租户登录失败而受阻；未使用 mock、admin 数据或接口绕过。

## Cleanup Keep

- `doc/tasks/20260616-route-use-1000-time-formula/frontend-feature-evidence.md`
