# 任务：展厅批量 AI 请求超时修复

## Goal

修复 `展厅 -> 产品管理` 中批量 AI 请求仍使用 axios 默认 `30000ms` 超时的问题，确保用户点击批量封面时不会再出现：

- `接口请求超时,请刷新页面重试!`
- `timeout of 30000ms exceeded`

本次至少覆盖用户已报告的 `一键生成所有封面` 链路，并同步修复同类的批量语音请求超时配置缺口。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\api\showroom-admin\index.ts`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\showroom-admin-ai-request-timeout.test.mjs`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-batch-ai-timeout-fix\**`

## Non-Scope

- 不改批量封面后端处理逻辑、并发策略或模式选择交互。
- 不改单产品 AI 生成链路。
- 不新增 fallback、mock 成功或静默降级。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-batch-cover-mode-parallel-cli\task.md`
- Status before this task: `Completed with commit-boundary blocker on 2026-05-21`
- Impact: 上一任务已完成功能行为和验证；本次只修复同一页面新增暴露出的批量请求超时缺口，不回退模式选择和汇总展示结果。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Current state: `src/api/showroom-admin/index.ts` 当前同时承载批量封面模式与公司字段翻译等在途改动。
- Impact: 本任务只允许在共享 API 文件上叠加最小超时修复和定向测试，不能覆盖无关并行内容。

## Milestones

1. 创建任务文档并确认上一同仓任务状态。
2. 先补 RED，锁定批量封面/批量语音必须显式覆盖默认 30 秒超时。
3. 最小修复批量 AI 请求超时配置。
4. 跑通定向测试、lint、证据校验与 closeout preview。

## Expected Verification

- `node --test scripts/showroom-admin-ai-request-timeout.test.mjs`
- `pnpm exec eslint src/api/showroom-admin/index.ts scripts/showroom-admin-ai-request-timeout.test.mjs --format stylish`
- `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-batch-ai-timeout-fix\bug-regression-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-batch-ai-timeout-fix --mode preview`

## Current Status

- Status: Completed with commit-boundary blocker on 2026-05-21
- Completed work:
  - 已补定向回归，锁定批量语音与批量封面请求必须显式覆盖默认 30 秒超时。
  - 已为两条批量 AI 请求补上 `SHOWROOM_ADMIN_AI_REQUEST_TIMEOUT`。
  - 已完成定向测试与 lint 验证。
- Remaining blockers:
  - `src/api/showroom-admin/index.ts` 当前仍与批量封面模式选择、公司字段翻译等并行任务共享，无法在不混入其他需求内容的前提下安全生成纯本任务提交。

## Final Verification Result

- PASS: `node --test scripts/showroom-admin-ai-request-timeout.test.mjs`
- PASS: `pnpm exec eslint src/api/showroom-admin/index.ts scripts/showroom-admin-ai-request-timeout.test.mjs --format stylish`
