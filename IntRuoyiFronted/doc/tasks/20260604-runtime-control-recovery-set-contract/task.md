# 任务：运行控制前端恢复集与兼容性证据展示

## 任务目标

更新运行控制前端 API 类型与页面展示：恢复候选改为恢复集候选，展示恢复集组成、manifest hash、程序版本和 Redis 策略；回滚候选展示兼容性证据路径、状态、检查时间和摘要；标记发布已测试时必须提交恢复集候选。

## Previous Task Check

- 上一个前端任务：`doc/tasks/20260604-layout-header-remove-redbox-controls/task.md`
- 状态：`completed`
- 处理：上一任务已完成；本任务只修改运行控制 API 类型、页面文案和相关测试。

## BDD 场景

- BDD: 前端展示恢复集候选 -> Given 后端返回完整恢复集候选 / When 操作员打开运行控制台 / Then 页面展示恢复集 ID、状态、程序版本、Redis 策略、配置清单、manifest hash 和组件摘要。
- BDD: 前端标记发布已测试必须绑定恢复集 -> Given 操作员选择发布候选 / When 未选择恢复集候选 / Then 提交被阻断且不会发送降级请求。
- BDD: 前端展示回滚兼容性证据 -> Given 后端返回回滚候选 / When 操作员查看候选 / Then 页面展示兼容性状态、证据路径、检查时间和摘要。
- BDD: 前端文案表达正确操作边界 -> Given 操作员查看恢复与回滚操作 / When 页面渲染 / Then 文案体现恢复同一恢复集、兼容性成立后只回滚程序。

## Milestones

- [x] M1：建立任务文档并确认上一前端任务已完成。
- [x] M2：新增 RED 前端静态/API 契约测试。
- [x] M3：更新 API 类型、请求字段和页面展示。
- [x] M4：运行前端目标验证与 evidence 校验。
- [x] M5：收尾预览并提交前端改动。
- [x] M6：补充本机 `芋道源码/admin` 只读真实 E2E 复核。

## Expected Verification

- RED/GREEN：前端静态/API 契约测试。
- GREEN：frontend feature evidence validator。
- GREEN：`git diff --check`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。缺少恢复集选择时阻断提交；不隐藏候选阻塞原因。
- `是否从根因和长期维护角度解决`：是。把恢复集与兼容性证据直接纳入 API 类型和 UI 操作契约。
- `是否存在临时补丁或绕过`：否。不增加测试专用控件或模拟数据。

## Current Status

completed

## 验证结果

- RED：`node tests/e2e/runtime-control-recovery-set-contract-static.spec.js` -> FAIL，缺少恢复集 API 字段、`selectedRecoverySetCandidateId`、兼容性证据展示与文案。
- GREEN：`node tests/e2e/runtime-control-recovery-set-contract-static.spec.js; node tests/e2e/runtime-control-restore-target-static.spec.js; node tests/e2e/runtime-control-foolproof-static.spec.js; node tests/e2e/runtime-control-ops-static.spec.js` -> PASS。
- GREEN：`pnpm ts:check` -> PASS。
- GREEN：frontend feature evidence validator -> PASS。
- GREEN：UTF-8 readback -> PASS。
- GREEN：`git diff --check` -> PASS。
- GREEN：task-closeout-cleanup preview -> PASS，keep only，无 delete/blocked/warnings。
- GREEN：本机只读真实 E2E `node tests/e2e/runtime-control-yudao-admin-readonly.e2e.js` -> PASS，回滚候选 22 条、恢复集候选 8 条，`YUDAO_ADMIN_READONLY_PASS`，运行控制接口无非 GET 写请求。

## Blockers

- 暂无。用户已明确要求继续实施恢复集计划，本任务恢复为进行中。

## Cleanup Keep

- `doc/tasks/20260604-runtime-control-recovery-set-contract/frontend-feature-evidence.md`
