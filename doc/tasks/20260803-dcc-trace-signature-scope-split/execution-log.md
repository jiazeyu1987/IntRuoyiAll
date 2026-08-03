# Execution Log

## User Intent

用户反馈受控浏览列表中“追溯”和“签核”点击后跳转页面一样；要求追溯只显示追溯信息，签核只显示签核信息，避免多余内容分散用户注意力。

## Preconditions

- 已读取 `docs/task-closeout-rules.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md`、`docs/frontend-development.md`、`docs/e2e-rules.md`。
- 已使用 `bug-regression-fix-loop` 与 `frontend-feature-delivery` 技能，并读取对应 contract。
- 既有脏工作区基线提交：`6f5f52814 chore: baseline dirty worktree before trace signature split`。
- 基线后仍有并行残余：`IntRuoyiFronted/package.json`、`IntRuoyiFronted/tests/e2e/dcc-upload-browser-tab-cache-static.spec.js`；本任务不修改这些文件。

## BDD Scenarios

- BDD: 追溯入口仅展示追溯信息 -> Given 用户在受控浏览列表点击某文件“追溯”；When 进入 DCC 详情追溯页；Then 页面展示生命周期、版本历史、分发、培训、受控打印等追溯区块，不展示签核追溯和签名留痕区块。
- BDD: 签核入口仅展示签核信息 -> Given 用户在受控浏览列表点击同一文件“签核”；When 进入 DCC 签核页面；Then 页面展示签核追溯和签名留痕区块，不展示项目联动、受控浏览落位、分发、培训、受控打印等非签核区块。

## RED / GREEN

- RED: pending
- GREEN: pending

## Verification Evidence

- Pending.

## Blockers

- Pending.
