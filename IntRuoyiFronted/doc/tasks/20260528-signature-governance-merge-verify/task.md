# 任务：电子签名治理支线合并与主租户前端验证

## 目标

配合后端任务 `20260528-signature-governance-merge-verify`，将前端支线 `codex/20260528-signature-governance-docs` 融合进 `int_main`，重启主前端后使用 `芋道源码 / admin / admin123` 走真实电子签名治理路径验证。验证成功后删除支线 worktree。

## Milestones

- [x] M1：创建前端合并验证任务文档。
- [x] M2：前端支线合并进 `int_main`。
- [x] M3：重启主前端并确认加载最新代码。
- [x] M4：Playwright 使用 `芋道源码 / admin / admin123` 验证电子签名治理页面。
- [x] M5：验证成功后删除前端支线 worktree。

## Expected Verification

- 前端 `git status` 与 merge 结果。
- 前端重启记录。
- Playwright 主租户真实路径验证结果。

## Current Status

completed

- 状态：completed
- 当前阶段：主租户验证已通过，前端支线 worktree 已删除，准备精确暂存并提交本任务改动。

## Milestone Evidence

- M2：2026-05-28 前端主 worktree 执行 `git merge --no-ff --no-commit codex/20260528-signature-governance-docs`，无冲突，保持未提交状态等待 runtime 验证。
- M3：Vite dev server 在 Windows 大仓验证时触发 `EMFILE: too many open files`；新增 `scripts/vite-dev-file-handle-contract.test.mjs` 并将 AutoImport d.ts 生成固定关闭，相关静态契约通过。
- M3：由于 dev server 仍受 Vite 文件句柄限制影响，使用 `pnpm build:local` 构建最新主线产物，并以 Vite preview 在 `8081` 运行；产物 API 目标确认包含 `127.0.0.1:48081`。
- M4：先用测试租户 `测试租户 / aoteman / admin123` 跑 `node tests\e2e\signature-governance-policy.e2e.js`，结果 PASS。
- M4：最终用 `芋道源码 / admin / admin123` 登录 `http://127.0.0.1:8081/signature-governance`，页面显示 `电子签名治理` 与 `READY`，策略接口返回四模块 `READY/ready=true`，无失败 `admin-api` 响应。
- M5：前端支线 worktree 已执行 `git worktree remove`；`Test-Path D:\ProjectPackage\Int\IntRuoyi\worktrees\20260528-signature-governance-docs\yudao-ui-admin-vue3` 返回 `False`，`git worktree list` 不再包含该支线。
- M5：`task-closeout-cleanup` 预览通过，后续仅清理本任务临时 `verification-report.md`，保留 `task.md` 与 `execution-log.md`。
