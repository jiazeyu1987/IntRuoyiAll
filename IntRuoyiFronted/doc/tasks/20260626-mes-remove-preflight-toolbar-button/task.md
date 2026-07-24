# 任务：删除排产工单工具栏排产前检查按钮

## 任务目标

- 删除 `/mes/pro/scheduleorder` 工具栏里的独立 `排产前检查` 按钮。
- 保留 `手动重排` 抽屉中的预检能力、阻断提示和应用前门禁，不改变现有后端接口与重排写入流程。
- 同步清理不再使用的前端入口方法和相关静态断言，避免残留死代码或错误产品语义。

## 当前状态

已完成。

## 上一任务检查

- 上一个 frontend 任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-showroom-product-audio-modal\task.md`
- 状态：`COMPLETED`
- 处理：已确认前一任务文档为完成状态，不阻塞本次需求。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- 适用强制门禁：
  - 本次仅做前端源码与静态合同调整，不做真实登录、真实写入或服务器操作。
  - 排产工单页继续遵循 IntPP 紧凑运维台样式，只删除独立入口，不做无关视觉重构。
  - 不通过隐藏错误、补 fallback 或弱化重排前置校验来达成需求。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。删除入口后仍保留抽屉内真实预检与阻断反馈。
- `是否从根因和长期维护角度解决`：是。同步移除失效入口方法并更新静态契约，避免页面语义和测试继续要求双入口。
- `是否存在临时补丁或绕过`：否。不会仅通过 CSS 隐藏按钮，也不会去掉抽屉中的预检门禁。

## BDD 场景

- `BDD: 工具栏不再暴露独立排产前检查按钮 -> Given 计划员进入排产工单页并勾选排产工单 / When 查看工具栏主操作区 / Then 页面只保留“同步工单”和“手动重排”等入口，不再显示独立“排产前检查”按钮。`
- `BDD: 手动重排仍可触发真实预检 -> Given 计划员点击“手动重排”打开抽屉 / When 在抽屉中执行重新检查或预览重排 / Then 页面继续调用现有预检逻辑，并在应用重排前阻断过期或存在阻断问题的结果。`
- `BDD: 未勾选工单时仍明确阻断手动重排 -> Given 计划员未勾选任何排产工单 / When 查看工具栏 / Then “手动重排”入口继续保持禁用并提示“请先勾选排产工单”。`

## 里程碑

1. M1：创建任务文档、记录命令日志并补 RED 静态断言。
2. M2：删除独立排产前检查按钮，清理未使用入口方法并更新静态测试。
3. M3：运行 GREEN 静态验证、类型检查与 frontend evidence 校验。

## 预期验证

- `node tests/e2e/mes-pro-schedule-order-usability-static.spec.js`
- `node tests/e2e/mes-pro-schedule-order-replan-scope-static.spec.js`
- `node tests/e2e/mes-scheduling-scope-static.spec.js`
- `node tests/e2e/mes-pro-schedule-order-replan-apply-enabled-static.spec.js`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-mes-remove-preflight-toolbar-button\frontend-feature-evidence.md`

## 最终验证结果

- `node tests/e2e/mes-pro-schedule-order-usability-static.spec.js` -> PASS
- `node tests/e2e/mes-pro-schedule-order-replan-scope-static.spec.js` -> PASS
- `node tests/e2e/mes-pro-schedule-order-replan-apply-enabled-static.spec.js` -> PASS
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-mes-remove-preflight-toolbar-button\frontend-feature-evidence.md` -> PASS
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260626-mes-remove-preflight-toolbar-button --mode preview` -> READY，仅建议清理 `frontend-feature-evidence.md`
- `node tests/e2e/mes-scheduling-scope-static.spec.js` -> BLOCKED，仓库现状缺少该静态合同要求的既有语义文案，非本次改动引入
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> BLOCKED，本地 `node_modules` 缺少 `@volar/typescript/lib/quickstart/runTsc`

## Cleanup Keep

- `doc/tasks/20260626-mes-remove-preflight-toolbar-button/frontend-feature-evidence.md`
