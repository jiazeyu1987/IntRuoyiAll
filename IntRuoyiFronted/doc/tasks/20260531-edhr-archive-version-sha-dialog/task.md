# 20260531-edhr-archive-version-sha-dialog

## Task Goal

修复本地真实 release E2E 暴露的问题：eDHR 执行详情的“归档版本”弹窗已经通过真实 `/mes/pro/batch-record-execution-archive/page` 返回本轮 SEALED 归档，但弹窗表格没有展示 `sha256`，导致用户和 E2E 都无法在版本列表里核对受控归档文件摘要。

本任务只补齐归档版本弹窗的可见证据列，不改变后端接口、不新增测试专用控件、不引入 mock、fallback、静默跳过或默认成功。

## Milestones

1. RED: 记录真实 E2E 失败，并新增静态回归测试证明归档版本弹窗缺少 `sha256` 列。
2. GREEN: 在归档版本弹窗中展示 `sha256`，保持现有归档列表接口、下载动作和权限不变。
3. REGRESSION: 运行目标静态测试、release check gate，并用本地测试租户重新跑真实 release gate。
4. CLOSEOUT: 验证通过后记录证据，按任务收尾策略预览清理，再处理临时 worktree。

## Expected Verification

- `node --test scripts/edhr-archive-export.test.mjs`
- `node scripts/edhr-release-e2e-coverage-gate.mjs --check --report D:\ProjectPackage\Int\IntRuoyi\output\local-edhr-int-main-verify\edhr-release-coverage-check-report.json`
- `node scripts/edhr-release-e2e-coverage-gate.mjs --run-real --report D:\ProjectPackage\Int\IntRuoyi\output\local-edhr-int-main-verify\edhr-release-real-report.json`
- `git diff --check`

## Current Status

- status: completed
- completed work:
  - 归档版本弹窗新增 `SHA-256` 列并绑定 `row.sha256`。
  - 字段审计真实 E2E 定位执行记录步骤改为使用列表 API 返回的 `row.executionCode`，`waitForText` 对空文本 fail fast，避免 undefined selector 内部异常。
  - 本地 MinIO 主文件配置切到已开启 Object Lock/retention 的 `edhr-retention-verifier-20260528` bucket，用于本地真实归档生成与下载验证。
- latest verification:
  - full local release E2E gate: PASS.
- final verification:
  - `node --test scripts\edhr-archive-export.test.mjs` -> PASS, 8 tests.
  - `node --test scripts\edhr-field-audit-e2e-contract.test.mjs` -> PASS, 5 tests.
  - `node scripts\edhr-release-e2e-coverage-gate.mjs --check --report D:\ProjectPackage\Int\IntRuoyi\output\local-edhr-int-main-verify\edhr-release-coverage-check-report.json` -> PASS.
  - `node scripts\edhr-release-e2e-coverage-gate.mjs --run-real --report D:\ProjectPackage\Int\IntRuoyi\output\local-edhr-int-main-verify\edhr-release-real-report.json` -> PASS.
  - `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260531-edhr-archive-version-sha-dialog\execution-log.md` -> PASS.
  - `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260531-edhr-archive-version-sha-dialog\execution-log.md` -> PASS.
  - `git diff --check` -> PASS, only CRLF warnings.
- remaining:
  - 提交当前任务相关改动，预览清理已完成且无删除项；提交后删除已融合临时 worktree。
