# DCC 受控浏览体验优化与真实 E2E 验证

## Task Goal

优化 DCC 文控“受控浏览”前端体验，只覆盖本场景：列表信息完整性、预览页业务可读元信息、无权限/无匹配反馈、目录/分类/项目代码定位路径、版本入口文案、发布完成闭环确认、审批前权限范围预检、草稿/历史版隔离提示，并通过真实 Playwright E2E 验证有权限与低权限非 admin 账号路径。

## Scope

- 仅修改 DCC 受控浏览、DCC 受控文件详情/预览、DCC 原版上传/审批闭环中与本需求直接相关的展示、静态契约和 DCC 查询响应投影。
- 不修改数据库状态、权限配置或其它业务场景。
- 不使用 admin 账号绕过权限；API/DB 仅用于最终只读核验。

## Milestones

1. `completed` - 读取项目规则、技能规则、经验索引和适用门禁。
2. `completed` - 建立 BDD/TDD 任务记录并冻结 RED 静态契约。
3. `completed` - 实现受控浏览列表、预览、无权限反馈、路径定位、入口文案和上传/审批闭环提示。
4. `completed` - 运行定向静态合同、类型检查、后端编译和真实 Playwright E2E；授权与低权限账号真实页面路径 PASS。
5. `completed` - 更新验证报告、记录 PASS/BLOCKED 和收尾状态。

## Expected Verification

- `node tests/e2e/dcc-controlled-browser-ux-optimization-static.spec.js`
- 相关既有 DCC 静态契约：受控浏览搜索/版本摘要/上传治理/预览 linkage。
- `pnpm ts:check`，若存在无关历史失败则记录首个 blocker 并以聚焦静态契约作为本任务 GREEN。
- 真实 Playwright 前端 E2E：使用 `wangsiyu` 验证目标 ACTIVE 文件在受控浏览可见、当前有效版本与发布/盖章预览信息正确；使用 `pengyunfeng` 验证同文件不可见或有明确无权限/无匹配提示；记录目标链路错误数和写请求数。

## Applicable Experience Gates

- DCC 文控审批处理入口门禁：viewer 模式必须展示最终目录路径、publishedFileId/stampedFileId 或等价发布文件信息，不得 API-only 替代页面验证。
- DCC 受控浏览当前有效版与权限隔离门禁：必须使用两个非 admin 账号，通过真实受控浏览页面验证当前有效版、发布/盖章预览、目录路径和低权限不可见，不得只看文件名或用 API-only 代替。
- DCC 升版发布 UX 闭环门禁：区分受控浏览 viewer 路径和追溯详情路径，记录目标写请求数、pageErrors 和目标链路错误数。
- 前端静态契约隔离门禁：如既有大契约或 `pnpm ts:check` 先失败于无关历史问题，使用本任务专用最小静态契约完成 RED/GREEN，并记录全量回归 blocker。
- Playwright 浏览器可执行文件门禁：优先使用本机 Chrome 可执行文件并记录路径来源。
- Playwright 目标链路与外部资源异常归因门禁：目标 DCC/本机请求失败即 blocker，外部资源异常需单独归因。
- 真实 E2E 主链路与扩展诊断产物隔离门禁：本任务结果 JSON 使用独立路径，不覆盖原版发布任务证据。
- PowerShell/UTF-8 门禁：中文任务文档和源码用 UTF-8 写入，不记录密码、token 或其它凭据。
- Git 共享分支门禁：当前工作区存在大量非本任务脏改动，本任务只触碰明确范围；提交/推送若无法隔离则记录 blocker。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是把当前有效版、发布/盖章来源、权限反馈和路径定位作为页面显式信息，而不是靠用户猜测文件名。
- `是否存在临时补丁或绕过`：否。

## Current Status

ready_for_closeout

## Closeout Pending

- `2026-08-03 00:21 +08:00`: 实现和验证已完成。当前仓库进入本任务前已有大量非本任务脏改动和 `int_main...origin/int_main [ahead 1]`；后续复扫已无 ahead 标记，但仍有大量非本任务脏改动。本轮未执行基线提交、任务提交或推送，避免混入其它并发任务改动。
- 先前 `48081` 后端运行态 blocker 已解除；最新运行态为 `E:\IntRuoyi\output\runtime\int_main\backend\yudao-server-exec-20260803-001741.jar`，health `UP`，真实 Playwright E2E PASS。
- `2026-08-03`: `verification-report.md` 已更新为最终 PASS；cleanup preview/apply 均已完成，保留任务脚本和结果 JSON，无删除项、无阻塞、无 warning。
- 剩余 blocker：当前仓库仍为共享脏工作区，存在大量非本任务改动；为避免混入其它并行任务，本任务未执行提交和推送，任务状态保持 `ready_for_closeout`。

## Cleanup Keep

- doc/tasks/20260802-dcc-controlled-browser-ux-optimization/dcc-controlled-browser-ux-real-e2e.cjs
- doc/tasks/20260802-dcc-controlled-browser-ux-optimization/dcc-controlled-browser-ux-real-e2e-result.json
