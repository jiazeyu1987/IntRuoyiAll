# DCC 受控文件下载入口收敛

## Task Goal

将文控受控文件的直接下载入口收敛为受控浏览列表操作列中的“下载”按钮，移除详情/预览态页面内的直接下载按钮，避免用户从多个入口触发同一受控副本下载。

## Milestones

- [x] 定位现有下载入口和受影响前端组件。
- [x] 增加静态契约，先证明详情页仍存在下载入口。
- [x] 移除非列表行操作列的直接下载入口。
- [x] 运行定向静态验证并记录结果。

## Expected Verification

- `node tests/e2e/dcc-download-entry-browser-only-static.spec.js`
- `node tests/e2e/dcc-list-detail-entry-static.spec.js`
- `pnpm ts:check` 如受环境或历史问题阻塞，记录首个 blocker。

## Current Status

blocked

## Verification Summary

- `node tests/e2e/dcc-download-entry-browser-only-static.spec.js`：RED 后 GREEN。
- `pnpm e2e:dcc:download-entry:static`：PASS。
- `node tests/e2e/dcc-list-detail-entry-static.spec.js`：PASS，已同步现有受控浏览操作列 visibility-gated 标记。
- `pnpm ts:check`：PASS。
- `task_closeout.py --mode preview/apply`：PASS，仅删除临时 `frontend-feature-evidence.md`，保留核心任务报告。
- 前端代码已进入提交 `72712e92d chore: baseline concurrent download entry updates`，该提交包含详情页下载入口移除、新增静态契约和相邻 list-detail 契约标记修正。

## Closeout Result

- 前端代码提交：`72712e92d chore: baseline concurrent download entry updates`。
- 收尾文档提交：`fa4ba2225 docs: close DCC download entry task`。
- 推送阻塞：`git push origin int_main` 先因 GitHub 代理 `127.0.0.1:7890` 不可用失败；清空本次命令代理后，GitHub 直连 443 随后也失败/连接重置。当前本地 `int_main` 仍领先 `origin/int_main`，不得标记 completed。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，直接移除多余 UI 入口和未使用的下载状态/处理函数。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- 适用 `docs/frontend-development.md#前端截图按钮统一静态契约门禁`：截图红框命中的按钮入口变更需先写任务专用静态契约，避免扩大改动范围。
- 适用 `docs/frontend-development.md#前端静态契约隔离门禁`：新增最小静态契约覆盖当前入口收敛，若全量检查存在无关历史问题则单独记录。
- 适用 `docs/e2e-rules.md#DCC-受控浏览当前有效版与权限隔离门禁` 的只读边界：本次仅收敛 UI 入口，不改变受控浏览权限、预览、追溯或下载 API 口径。
