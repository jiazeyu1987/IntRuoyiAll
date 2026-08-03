# 20260803-dcc-viewer-hide-entry-restore-edit

## Task Goal

在 DCC 受控文件 viewer 只读预览页中隐藏截图黄框内的“受控浏览入口”区块，同时恢复基础信息面板右上角“修改”按钮显示。

## Milestones

- [x] 定位 viewer 基础信息面板和“受控浏览入口”区块。
- [x] 更新静态回归契约，先复现当前“受控浏览入口”仍显示且“修改”未恢复的问题。
- [x] 最小化修改 viewer 模板，只隐藏目标区块并恢复修改按钮，不恢复审批/分发/版本/识别基础信息。
- [x] 运行目标和相邻静态契约、TypeScript 检查。
- [x] 记录 cleanup 和提交/推送状态。

## Expected Verification

- `node tests\e2e\dcc-controlled-preview-hide-basic-actions-static.spec.js` 先 RED 后 GREEN。
- `node tests\e2e\dcc-controlled-browser-ux-optimization-static.spec.js` 通过。
- `pnpm ts:check` 通过或记录明确阻塞。

## Experience Gate

- 命中 `docs/frontend-development.md#前端截图按钮统一静态契约门禁`：截图按钮显示/隐藏必须先用聚焦静态契约 RED/GREEN 锁定目标调用方 props，不改路由、权限或共享组件能力。
- 命中 `docs/e2e-rules.md#dcc-受控浏览当前有效版与权限隔离门禁`：viewer 只读路径不能通过 API-only、权限修改或直接详情 URL 冒充验收。

## Current Status

blocked

## Completion Blockers

- Cleanup 已完成：`task-closeout-cleanup --mode preview` 与 `--mode apply` 均通过，保留 `task.md`、`execution-log.md`、`verification-report.md`，无删除项。
- Git closeout 未完成：当前工作区存在大量非本任务并行脏改动，且当前分支已领先 `origin/int_main`；本任务不能将无关改动混入提交。
- 远端推送仍受环境阻塞：上一轮 HTTPS 走 `127.0.0.1:7890` 代理失败，SSH 443 返回 `Permission denied (publickey)`；在凭据/代理恢复前不能满足项目“提交并推送后完成”的门禁。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按 viewer 模板的目标可见性收敛。
- `是否存在临时补丁或绕过`：否。
