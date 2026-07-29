# 20260729-codex-test-management-filter-refresh

## Task Goal

在系统管理 > 测试管理页修复两个用户可见问题：

- 红框工具栏区域必须能直接看到“串行路线”下拉框，选择后只显示该串行路线对应节点。
- 运行监控页签不再使用前端定时轮询刷新，改为进入页签、创建执行后和用户点击“刷新”时显式刷新，避免测试期间页面卡顿。

## Milestones

1. 记录 BDD 和当前静态合同 RED 证据。
2. 修复测试管理页筛选栏布局，让串行路线筛选常驻可见。
3. 移除运行监控前端定时轮询，保留显式手动刷新入口。
4. 更新并运行聚焦静态合同，确认筛选和刷新行为。
5. 更新任务证据并准备提交。

## Expected Verification

- `node tests/e2e/system-codex-test-node-chain-static.spec.js`
- `node tests/e2e/system-codex-test-management-static.spec.js`
- `node tests/e2e/system-codex-test-run-monitor-static.spec.js`
- `pnpm ts:check` 如无无关历史阻塞则运行；若失败，记录首个非本任务阻塞。

## Applicable Gate Summary

- 前端静态契约隔离门禁：若宽合同存在无关失败，必须使用聚焦合同证明本任务行为，不得修改无关逻辑绕过。
- 测试管理串行节点串门禁：页面必须能按节点串单独筛选，不得只依赖前端排序或人工勾选。
- Codex Runner 自动测试门禁：执行入口不得因旧 Runner 状态硬阻断，运行监控必须展示真实接口状态。
- PowerShell 编排门禁：测试命令逐条运行并记录退出码，不用 `&&`。

## Current Status

ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；修正正式页面布局和刷新机制，不增加临时脚本或隐藏错误。
- `是否存在临时补丁或绕过`：否。
