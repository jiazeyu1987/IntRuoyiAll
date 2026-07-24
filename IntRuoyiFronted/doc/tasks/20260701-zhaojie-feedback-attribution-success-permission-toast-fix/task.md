# 任务：待归属确认归属成功后误报无权限（前端）

- Task ID: `20260701-zhaojie-feedback-attribution-success-permission-toast-fix`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Created: `2026-07-01`
- Current Status: `completed`

## Task Goal

修复 `src/views/mes/pro/feedback/index.vue` 在确认归属成功后仍额外刷新正式报工列表、导致 `zhaojie` 账号看到“没有该操作权限” toast 的前端回调问题，同时保持待归属页的当前批次刷新和后续正式报工页签刷新能力。

## Previous Task Check

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260701-srm-nas-locator-blacklist-pattern-search\task.md`
- 状态：`completed`
- 处理说明：上一任务已完成，不阻塞本次待归属误报权限问题修复。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
  - 命中 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
  - 仅修复回调链路和列表刷新，不调整报工页现有运维台风格。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。删除归属成功后的冗余正式报工刷新，保留页签切换时的正式查询入口。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 归属成功后不再额外刷新正式报工列表 -> Given 用户停留在待归属页并完成确认归属 / When handleAttributionSuccess 执行 / Then 只刷新待归属当前上下文，不立即调用 getList。`
- `BDD: 待归属页当前上下文继续保持 -> Given 页面当前锁定导入批次与筛选条件 / When 归属成功 / Then importQueryParams 不被切到其他筛选，且待归属列表会刷新到最新状态。`
- `BDD: 用户切回正式报工页签仍能看到最新数据 -> Given 归属成功后未立即刷新正式报工列表 / When 用户切回正式报工页签 / Then handleTabChange 仍会调用 getList 获取最新正式报工数据。`

## Milestones

1. M1：建立前端任务台账并确认疑似误报链路。`completed`
2. M2：补 RED 静态回归，锁定 handleAttributionSuccess 中的冗余刷新。`completed`
3. M3：实现最小修复并跑到 GREEN。`completed`
4. M4：补 evidence、closeout 预览与结果整理。`completed`

## Expected Verification

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-feedback-attribution-continuation-static.spec.js`
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-feedback-permission-static.spec.js`
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-feedback-tracking-static.spec.js`
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260701-zhaojie-feedback-attribution-success-permission-toast-fix\bug-regression-evidence.md`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260701-zhaojie-feedback-attribution-success-permission-toast-fix\frontend-feature-evidence.md`

## Current Blockers

- 暂无。

## Current Status

completed

## Cleanup Candidates

- `doc/tasks/20260701-zhaojie-feedback-attribution-success-permission-toast-fix/bug-regression-evidence.md`
- `doc/tasks/20260701-zhaojie-feedback-attribution-success-permission-toast-fix/frontend-feature-evidence.md`

## Final Verification Result

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-feedback-attribution-continuation-static.spec.js` -> `PASS`
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-feedback-permission-static.spec.js` -> `PASS`
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-feedback-tracking-static.spec.js` -> `PASS`
- `node D:\ProjectPackage\Int\IntRuoyi\scripts\preflight\login-preflight.mjs --base-url http://localhost:8081 --tenant 芋道源码 --username zhaojie --password 111111 --target-path /mes/pro/feedback --target-text 待归属 --timeout 90000` -> `PASS`
- `node D:\ProjectPackage\Int\IntRuoyi\scripts\preflight\login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username zhaojie --password 111111 --target-path /mes/pro/feedback --target-text 待归属 --timeout 90000` -> `PASS`
- 结论：待归属确认归属成功回调已不再立即刷新正式报工列表，误报权限 toast 的高概率触发链路已被移除；正式报工页签仍通过原有切换链路刷新数据。
