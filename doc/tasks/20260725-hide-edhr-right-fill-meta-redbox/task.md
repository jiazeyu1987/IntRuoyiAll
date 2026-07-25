# Task: eDHR 右侧填写元信息红框隐藏

## Task Goal

删除 eDHR 批次执行详情页右侧红框中的独立 `填写人 / 提交时间` 元信息块；保留右侧单据卡片、单据卡片内填写人、阻断原因和打开填写入口。

## Milestones

- [x] 建立任务记录并确认截图目标。
- [x] 写入 BDD 场景和 RED 静态契约。
- [x] 移除右侧独立填写元信息块及废弃脚本/CSS。
- [x] 更新相关静态契约并完成目标验证。
- [x] 记录验证、清理状态和未完成 blocker。

## Expected Verification

- `node tests/e2e/edhr-batch-detail-hide-red-box-static.spec.js`
- `node tests/e2e/edhr-review-summary-right-rail-static.spec.js`
- `node tests/e2e/mes-edhr-batch-review-signoff-summary-static.spec.js`
- `node tests/e2e/edhr-batch-admin-filler-visibility-static.spec.js`
- `node tests/e2e/edhr-batch-fill-direct-navigation-static.spec.js`
- `node tests/e2e/edhr-batch-process-form-card-fillers-static.spec.js`

## Current Status

ready_for_closeout

## Experience Gate

- `docs\experience-index.md` 已读取；本任务命中 eDHR 批次详情、填写人显示、前端页面和 PowerShell 编排门禁。
- 适用门禁：不得从当前登录人、创建人、更新人或默认值推断填写人；本次只删除独立摘要块，保留单据卡片内真实填写人展示。

## Verification Summary

- RED: `node tests/e2e/edhr-batch-detail-hide-red-box-static.spec.js` -> FAIL，命中现有 `class="edhr-batch-detail__primary-fill-meta"`。
- GREEN: `node tests/e2e/edhr-batch-detail-hide-red-box-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-review-summary-right-rail-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-edhr-batch-review-signoff-summary-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-batch-admin-filler-visibility-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-batch-fill-direct-navigation-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-batch-process-form-card-fillers-static.spec.js` -> PASS，确认单据卡片填写人保留。
- BLOCKER: `node tests/e2e/edhr-ordinary-process-fill-only-static.spec.js` -> FAIL，既有 `ExecutionPage.vue` 提交处理仍包含“请选择审核/批准人”，与本次红框删除无关。
- BLOCKER: 当前工作区存在其他任务持续写入的后端、E2E 和任务文档改动；本任务未提交、未推送，避免混入非自有文件。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，直接移除不应渲染的右侧独立元信息块及其废弃依赖。
- `是否存在临时补丁或绕过`：否。
