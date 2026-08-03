# DCC 受控文件追溯入口收敛

## Task Goal

将文控受控文件的追溯入口收敛为受控浏览列表操作列中、下载按钮旁边的“追溯”按钮，移除文件编号列的可点击追溯入口。

## Milestones

- [x] 定位受控浏览页所有追溯入口。
- [x] 更新静态契约，先证明文件编号列仍是追溯入口。
- [x] 将文件编号列改为纯展示，保留操作列“追溯”按钮。
- [x] 运行定向验证并记录结果。

## Expected Verification

- `node tests/e2e/dcc-browser-file-number-detail-entry-static.spec.js`
- `node tests/e2e/controlled-content-state-view-static.spec.js`
- `pnpm ts:check`

## Current Status

ready_for_closeout

## Verification Summary

- `node tests/e2e/dcc-browser-file-number-detail-entry-static.spec.js`：RED 后 GREEN。
- `pnpm e2e:dcc:download-entry:static`：PASS。
- `pnpm ts:check`：PASS。
- `git diff --check -- <task-owned files>`：PASS，仅有 LF/CRLF 规范化 warning。
- `frontend-feature` evidence validator：PASS，临时 evidence 已在 cleanup apply 中删除。
- `task_closeout.py --mode preview/apply`：PASS，仅删除临时 `frontend-feature-evidence.md`。
- `pnpm e2e:controlled-content:state-view:static`：BLOCKED，先失败在无关历史断言“工艺路线编辑页必须复用受控内容状态条”。

## Closeout Blockers

- `IntRuoyiFronted/src/views/dcc/controlled-file/browser/index.vue` 在本任务前已有并行未提交/暂存改动，当前为混合暂存状态；本任务未提交/推送，避免混入非本任务变更。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，直接收敛入口，保留正式操作列按钮。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- 适用 `docs/frontend-development.md#前端截图按钮统一静态契约门禁`：截图红框命中的按钮入口变更需用任务专用静态契约锁定，不能扩大改路由/权限/API。
- 适用 `docs/frontend-development.md#前端静态契约隔离门禁`：受控浏览页已有并行改动，当前任务用最小静态契约隔离追溯入口收敛。
- 适用 `docs/e2e-rules.md#DCC-受控浏览当前有效版与权限隔离门禁`：本次只调整入口展示，不改变当前有效版、权限隔离、详情追溯 API 或下载链路。
