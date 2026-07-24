# 任务：待归属页归属写入口权限收口

- Task ID: `20260701-zhaojie-feedback-attribution-permission-fix`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Created: `2026-07-01`
- Current Status: `completed`

## Task Goal

修复 `生产报工 -> 待归属` 页面把 `选择归属 / 修改归属 / 确认报工 / 补填草稿字段` 暴露给无 `mes:pro-feedback:update` 用户的前端合同问题，确保待归属页所有写入口都与后端 `update` 鉴权一致，并在程序化触发时显式 fail-fast。

## Previous Task Check

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260630-schedule-order-replan-real-e2e-sample-check\task.md`
- 状态：`blocked`
- 处理说明：上一任务已因真实样本不可见阻塞；本次进入独立的权限合同修复，不接管其样本问题。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - 前端任务文档、静态测试与日志统一显式 UTF-8。
- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
  - 本次只做权限入口收口，不改变现有报工页布局密度与视觉方向。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。统一待归属页写入口和后端 `mes:pro-feedback:update` 权限合同，不做 toast 级遮掩。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 缺少 update 权限时待归属页不暴露写入口 -> Given 当前登录用户只有 mes:pro-feedback:query / When 打开待归属页 / Then 页面不显示选择归属、修改归属、确认报工按钮，也不提供草稿补填输入框。`
- `BDD: 程序化触发写入口时前端显式 fail-fast -> Given 用户通过代码路径直接调用归属弹窗或整批确认方法 / When 当前权限缺少 mes:pro-feedback:update / Then 前端立即提示缺少生产报工更新权限，不继续请求后端。`
- `BDD: 有 update 权限时待归属原有闭环保持可用 -> Given 当前登录用户拥有 mes:pro-feedback:update / When 打开待归属页并执行归属或整批确认 / Then 原有补填字段、归属弹窗和确认报工闭环保持不变。`

## Milestones

1. M1：建立前端任务台账并确认现有权限错配。`completed`
2. M2：补 RED 静态合同测试。`completed`
3. M3：实现权限收口并跑到 GREEN。`completed`
4. M4：回填前端证据与结论。`completed`

## Expected Verification

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-feedback-permission-static.spec.js`
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-feedback-tracking-static.spec.js`
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260701-zhaojie-feedback-attribution-permission-fix\bug-regression-evidence.md`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260701-zhaojie-feedback-attribution-permission-fix\frontend-feature-evidence.md`

## Final Verification Result

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-feedback-permission-static.spec.js` -> PASS
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-feedback-tracking-static.spec.js` -> PASS
- 页面当前已对 `确认报工`、`选择归属`、`修改归属` 与可编辑补填字段统一使用 `mes:pro-feedback:update` 权限合同。
- 即使通过程序化路径直接触发归属或整批确认，前端也会先 fail-fast 提示“缺少生产报工更新权限”，不再继续走到后端 403。

## Current Blockers

- 无。
