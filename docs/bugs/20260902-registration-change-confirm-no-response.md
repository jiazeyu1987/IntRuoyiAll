# 注册证变更确认无可见反馈

## Bug Summary And Expected Behavior

- Bug: 注册证变更弹框在批准日期为空时点击“确认”，用户看起来没有反应。
- Summary: 注册证变更弹框填写后点击“确认”看起来没有反应。
- Evidence: 用户提供的截图中“批准日期”为空，确认按钮可见且未显示弹框内错误。
- Expected: 点击确认后，缺少批准日期应在弹框内显示明确提示；其它校验或正式接口失败也应保留弹框并显示错误。

## Reproduction Path

1. 进入注册证管理列表。
2. 点击一条证件的“变更”。
3. 选择变更内容、上传变更批件文件，保持“批准日期”为空。
4. 点击弹框底部“确认”。

## Root Cause

- 当前提交 handler 只通过短暂消息组件呈现错误，弹框内没有稳定错误区域。
- 用户截图中的批准日期为空，提交前校验直接抛出异常，页面缺少可见的内联反馈。

## Regression Test

- `IntRuoyiFronted/tests/registration-certificate-change-confirm-feedback-static.spec.mjs`

## RED/GREEN Evidence

- RED: `node .\tests\registration-certificate-change-confirm-feedback-static.spec.mjs` -> FAIL, 缺少 `registration-certificate-change-action-error` 内联错误区域，且旧弹框仍包含作废入口。
- GREEN: `node .\tests\registration-certificate-change-confirm-feedback-static.spec.mjs` -> PASS。

## Verification

- GREEN: `node .\tests\registration-certificate-change-dialog-static.spec.mjs` -> PASS。
- GREEN: `node .\tests\registration-certificate-operation-panel-width-static.spec.mjs` -> PASS。
- GREEN: `node .\IntRuoyiFronted\tests\registration-certificate-change-approval-upload-static.spec.mjs` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `git diff --check -- IntRuoyiFronted/src/views/dcc/registration-certificate/change/ChangeDialog.vue IntRuoyiFronted/tests/registration-certificate-change-confirm-feedback-static.spec.mjs IntRuoyiFronted/tests/registration-certificate-change-dialog-static.spec.mjs doc/tasks/20260902-registration-change-confirm-no-response docs/bugs/20260902-registration-change-confirm-no-response.md` -> PASS。

## Risk And Scope

- 仅调整前端错误反馈、提交前校验顺序和弹框底部作废入口，不改变后端接口、审批、文件和数据规则。

## Blockers And Follow-up

- Blockers: 无。
- Follow-up: 如需验证真实提交成功，另行使用已授权测试租户和任务自有文件执行写入型 E2E。
