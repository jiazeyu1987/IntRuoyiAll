# 工艺排产路线配置显示调整

## 任务目标

- 工艺排产路线配置弹窗中不再显示截图红框内的用途标签、启用工序预览条和日历规则列。
- 小时产能输入改为 2 位小数。
- 公式 a 列描述改为“系数”，公式 b 列描述改为“固定值”。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- 适用强制门禁：
  - 前端页面/表格/样式遵循 IntPP 操作台风格，保持紧凑表格、明确列宽与可扫描结构。
  - 本次仅做展示和文案收敛，不新增 fallback、降级、静默错误或模拟数据。

## 上一任务检查

- 当前前端仓最近任务 `20260616-scheduler-workbench-smoke-toggle` 已标记 `COMPLETED`。
- 仓内仍有未跟踪历史任务目录 `20260615-frontend-build-babel-helper-missing/`，本轮不修改该历史任务，不纳入本次提交。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。直接调整排产用途配置组件展示契约，移除不再需要展示的日历规则选择入口，保留已加载配置数据的保存字段语义。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- BDD: 红框区域不显示 -> Given 用户打开工艺排产路线配置弹窗 / When 页面渲染排产用途配置 / Then 弹窗摘要不显示用途标签、启用工序预览条和日历规则列。
- BDD: 产能与公式列按新文案显示 -> Given 用户查看排产用途配置表格 / When 表格渲染排产字段 / Then 小时产能输入保留 2 位小数，公式 a 列显示“系数”，公式 b 列显示“固定值”。

## 里程碑

1. M1：建立任务文档、经验门禁和静态契约测试。`DONE`
2. M2：RED：运行静态契约测试，确认当前组件不符合新展示要求。`DONE`
3. M3：GREEN：调整组件展示、精度和列名。`DONE`
4. M4：REGRESSION：运行静态契约、类型检查和收尾预览。`DONE`

## 预期验证

- `node tests/e2e/mes-route-use-config-display-static.spec.js`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260616-route-use-config-display-tuning/frontend-feature-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260616-route-use-config-display-tuning --mode preview`

## 当前状态

- 状态：COMPLETED。
- 最终验证：
  - `node tests\e2e\mes-route-use-config-display-static.spec.js` -> PASS。
  - `node tests\e2e\mes-process-use-route-tabs-static.spec.js` -> PASS。
  - `node tests\e2e\mes-pro-schedule-order-pool-static.spec.js` -> PASS。
  - `node tests\e2e\mes-edhr-multi-batch-route-static.spec.js` -> PASS。
  - `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
  - `validate_frontend_feature.py --evidence doc\tasks\20260616-route-use-config-display-tuning\frontend-feature-evidence.md` -> PASS。
  - `task_closeout.py --task-id 20260616-route-use-config-display-tuning --mode preview` -> PASS，delete/blocked/warnings 均为 `<none>`。
- 未完成的环境复验：Browser 目标弹窗视觉复验因本机测试租户登录失败且 admin 租户无路线数据而受阻，已记录为前置条件问题；未使用 mock、接口绕过或创建数据替代。

## Cleanup Keep

- `doc/tasks/20260616-route-use-config-display-tuning/frontend-feature-evidence.md`
