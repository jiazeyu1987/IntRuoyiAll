# 批记录测试 Tab E2E 顺序按钮

## Task Goal

- 在批记录测试页面每个内部 Tab 顶部、现有“测试全部”按钮左侧新增 E2E 按钮。
- 点击 E2E 后，只对当前选中的 Tab 内完整测试行按从前到后的顺序执行 Playwright E2E。
- 后续 E2E 必须携带前一个 E2E 的正式终态结果作为上下文，不得并发启动。

## Milestones

1. 规则与现状定位：completed
2. BDD/RED 静态合同：in_progress
3. 前端实现：pending
4. 定向验证与证据归档：pending
5. 收尾状态更新：pending

## Expected Verification

- node tests/e2e/edhr-batch-record-test-tab-e2e-static.spec.cjs
- node tests/e2e/edhr-batch-record-test-tab-run-all-static.spec.cjs
- node tests/e2e/edhr-batch-record-test-tab-static.spec.cjs
- python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260810-batch-record-tab-e2e-button/frontend-feature-evidence.md

## Experience Gate Summary

- 命中 docs/frontend-development.md#前端行级异步结果归属门禁：批量入口必须对完整 Tab 行集合逐行 await，单行处理函数必须返回正式终态 Promise；业务终态 PASS/FAIL/BLOCKED 与传输异常分开处理，禁止 Promise.all、不可等待定时器、最后一次全局回复或提前启用历史。
- 命中 docs/e2e-rules.md#E2E 脚本入口存在性门禁：本任务只新增页面触发入口和静态合同，不把静态合同冒充真实 Playwright 通过；真实运行态 E2E 若执行，需要另行确认本机服务、登录、Runner 和真实页面路径。

## Current Status

in_progress

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，新增正式 E2E 入口并复用 Codex 测试管理的 Playwright E2E 执行模式。
- 是否存在临时补丁或绕过：否。
