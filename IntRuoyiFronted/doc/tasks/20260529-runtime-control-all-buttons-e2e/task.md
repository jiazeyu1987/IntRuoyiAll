# 任务：运行控制台全按钮真实数据 E2E（前端）

## 目标

在本任务前端 worktree 中承接运行控制台全按钮真实数据 E2E 的 Playwright 脚本、页面交互和前端修复。主任务记录见根 worktree `doc/tasks/20260529-runtime-control-all-buttons-e2e/`。

## 里程碑

- [x] 前端 worktree 已创建：`codex/20260529-runtime-control-all-buttons-e2e`。
- [x] 建立前端任务记录。
- [x] 补齐或执行覆盖所有按钮的 Playwright 真实 E2E。
- [x] 如按钮交互有前端缺陷，在测试租户验证修复。
- [x] 支持 `芋道源码/admin` 最终验证。

## 当前状态

completed

## Current Status

completed

## 验证结果

- 新增 `tests/e2e/runtime-control-all-buttons-real.e2e.js`。
- GREEN: `node --check tests\e2e\runtime-control-all-buttons-real.e2e.js` -> PASS。
- GREEN: `node tests\e2e\runtime-control-all-buttons-real.e2e.js` -> PASS，39 checks；2026-05-29 重新执行仍通过。
