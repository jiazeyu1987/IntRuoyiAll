# 工艺批记录路线配置隐藏右侧冗余列

## 任务目标

- 工艺批记录路线配置弹窗中不再显示截图红框内的“基础工序默认批记录”和批记录用途行级“备注”列。
- 保持既有批记录表格绑定、记录类型、校验策略、权限范围和保存接口契约不变。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- 适用强制门禁：
  - 前端表格遵循 IntPP 操作台风格，保持紧凑列宽、清晰可扫描结构。
  - 本次仅收敛展示列，不新增 fallback、降级、静默错误或模拟数据。

## 上一任务检查

- 当前前端仓最近相关任务 `20260616-route-use-config-display-tuning` 已标记 `COMPLETED`。
- 当前前端仓最新任务 `20260616-scheduler-workbench-smoke-toggle` 已标记 `COMPLETED`。
- 仓内仍有未跟踪历史任务目录 `20260615-frontend-build-babel-helper-missing/`，本轮不修改该历史任务，不纳入本次提交。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。直接调整工艺批记录路线配置组件的展示契约，移除用户明确不需要看到的冗余列。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- BDD: 工艺批记录路线配置隐藏右侧冗余列 -> Given 用户打开工艺批记录路线配置弹窗 / When 页面渲染批记录用途配置表格 / Then 表格不显示“基础工序默认批记录”和批记录用途行级“备注”列。

## 里程碑

1. M1：建立任务文档、经验门禁和静态契约测试。`DONE`
2. M2：RED：运行静态契约测试，确认当前组件仍显示右侧冗余列。`DONE`
3. M3：GREEN：调整组件展示并保持保存契约不变。`DONE`
4. M4：REGRESSION：运行静态契约、相关回归、类型检查和收尾预览。`DONE`

## 预期验证

- `node tests/e2e/mes-batch-route-config-hide-default-columns-static.spec.js`
- `node tests/e2e/mes-route-use-config-display-static.spec.js`
- `node tests/e2e/mes-edhr-multi-batch-route-static.spec.js`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260616-batch-route-config-hide-default-columns/frontend-feature-evidence.md`
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260616-batch-route-config-hide-default-columns/bug-regression-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260616-batch-route-config-hide-default-columns --mode preview`

## 当前状态

- 状态：COMPLETED。
- 最终验证：
  - `node tests\e2e\mes-batch-route-config-hide-default-columns-static.spec.js` -> PASS。
  - `node tests\e2e\mes-route-use-config-display-static.spec.js` -> PASS。
  - `node tests\e2e\mes-edhr-multi-batch-route-static.spec.js` -> PASS。
  - `node tests\e2e\edhr-tail-four-goals-static.spec.js` -> PASS。
  - `node --check tests\e2e\mes-process-use-route-tabs-real-flow.e2e.js` -> PASS。
  - `node tests\e2e\mes-process-use-route-tabs-static.spec.js` -> PASS。
  - `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
  - `validate_frontend_feature.py --evidence doc\tasks\20260616-batch-route-config-hide-default-columns\frontend-feature-evidence.md` -> PASS。
  - `validate_bug_regression.py --evidence doc\tasks\20260616-batch-route-config-hide-default-columns\bug-regression-evidence.md` -> PASS。
  - `task_closeout.py --task-id 20260616-batch-route-config-hide-default-columns --mode preview` -> PASS，delete/blocked/warnings 均为 `<none>`。
- 未完成的环境复验：内置 Browser 导航工具本轮未暴露可调用接口，因此未做截图复验；未使用 mock、接口绕过或创建数据替代。

## Cleanup Keep

- `doc/tasks/20260616-batch-route-config-hide-default-columns/frontend-feature-evidence.md`
- `doc/tasks/20260616-batch-route-config-hide-default-columns/bug-regression-evidence.md`
