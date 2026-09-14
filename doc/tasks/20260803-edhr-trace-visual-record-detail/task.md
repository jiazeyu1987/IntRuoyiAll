# eDHR 表单追溯可视化批记录详情

## Task Goal

- 隐藏独立“历史批记录”展示入口。
- 在“表单追溯”详情中提供可点击的批记录详情入口。
- “表单追溯”列表行点击“详情”后，详情弹窗内必须直接显示“批记录表单”页签。
- 详情必须使用类似批次执行填写页的只读可视化表单展示历史快照，不显示纯文字明细。

## Milestones

- [x] 定位表单追溯页面、历史批记录入口和现有只读批记录组件。
- [x] 建立前端静态契约，先证明旧行为不满足需求。
- [x] 复用现有 eDHR 只读执行表单完成页面改造。
- [x] 按用户反馈补正“详情”弹窗内的“批记录表单”页签入口。
- [x] 运行定向验证并记录结果。

## Expected Verification

- `node tests/e2e/edhr-trace-visual-record-detail-static.spec.js`
- 若触及 Vue/TS 逻辑，运行相邻 eDHR 静态契约或 `pnpm ts:check`；若被无关历史问题阻塞，记录首个无关 blocker。

## Current Status

ready_for_closeout

- implementation and targeted verification complete.
- commit/push closeout is pending because the shared worktree has many unrelated dirty files and branch `int_main` is behind `origin/int_main` by 2 commits.

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；表单追溯使用持久化执行快照和现有只读表单组件承载历史详情，不依赖当前活动批记录配置。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- 前端同路由多入口分面门禁：隐藏“历史批记录”入口时，不能只改标题或 CSS；表单追溯详情必须拥有明确可见的批记录详情查看入口。
- 前端静态契约隔离门禁：当前仓库已有大量无关改动和潜在历史失败，本任务使用定向静态契约验证当前行为。
- eDHR 历史只读边界：历史详情必须读取持久化执行快照并按只读展示，不触发活动流转门禁或当前 BATCH 配置推导。

## Verification Summary

- RED: `node tests\e2e\edhr-trace-visual-record-detail-static.spec.js` -> FAIL，旧代码仍展示“历史批记录”页签和独立入口。
- GREEN: `node tests\e2e\edhr-trace-visual-record-detail-static.spec.js` -> PASS。
- RED: `node tests\e2e\edhr-trace-visual-record-detail-static.spec.js` -> FAIL，用户点击“详情”的弹窗没有 `el-tabs` 和“批记录表单”页签。
- GREEN: `node tests\e2e\edhr-trace-visual-record-detail-static.spec.js` -> PASS，详情弹窗内默认显示“批记录表单”页签，并提供可视化只读批记录打开动作。
- GREEN: `node tests\e2e\edhr-batch-history-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\edhr-batch-history-evidence-layout-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\edhr-form-trace-batch-execution-trace-actions-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\edhr-form-trace-tabs-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\edhr-trace-drawer-four-tabs-standard-list-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
