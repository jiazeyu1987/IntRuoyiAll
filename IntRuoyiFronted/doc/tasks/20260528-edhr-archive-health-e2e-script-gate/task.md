# 20260528-edhr-archive-health-e2e-script-gate

## Task Goal

将现有 `runtime-control-edhr-archive-health.e2e.js` 纳入可复跑的前端 package E2E 门禁，避免 eDHR 归档完整性健康项只有脚本文件、没有统一命令和任务证据。

该门禁必须保持真实用户路径：登录测试租户，打开 `/infra/monitors/runtime-control`，读取 `/admin-api/infra/runtime-control/business-health` 响应，确认 `edhr-archive-integrity` / `eDHR 归档完整性` 可见，并证明运行控制台健康页不发起非 GET 写请求。

## Milestones

- [completed] M1: 任务文档与 BDD/TDD 场景。
- [completed] M2: RED 静态契约证明 package 缺少 `e2e:edhr:archive-health` 和 `e2e:edhr:archive-health:check`。
- [completed] M3: GREEN package scripts 和静态契约。
- [completed] M4: 运行语法、静态契约、真实 E2E；真实 E2E 在当前环境通过。
- [completed] M5: worker 交付后由主 reviewer 复跑静态检查、语法检查、package check 和真实 E2E。

## Expected Verification

- `node tests/e2e/runtime-control-edhr-archive-health-static.spec.js`
- `node --check tests/e2e/runtime-control-edhr-archive-health.e2e.js`
- `pnpm e2e:edhr:archive-health:check`
- `pnpm e2e:edhr:archive-health`

## Current Status

- status: completed
- owner: frontend worker + main reviewer
- production release impact: package 门禁已覆盖 eDHR 归档完整性健康项真实 E2E，可通过 `pnpm e2e:edhr:archive-health:check` 与 `pnpm e2e:edhr:archive-health` 复跑。
- final verification result: PASS。真实 E2E 输出 `PASS: edhr-archive-integrity eDHR 归档完整性 visible with status=BLOCKED, writes=0`。
- closeout preview: `blocked` for linked-worktree fast-forward cleanup only; no delete candidates.
- commit: pending main reviewer commit.

## Remaining Blockers

- None for this package gate. The observed business health item status was `BLOCKED`, which is an accepted real backend health status in the existing E2E contract; the E2E itself passed and confirmed zero runtime-control write requests.

## Main Reviewer Verification

- GREEN: `node tests/e2e/runtime-control-edhr-archive-health-static.spec.js` -> PASS。
- GREEN: `node --check tests/e2e/runtime-control-edhr-archive-health.e2e.js` -> PASS。
- GREEN: `pnpm e2e:edhr:archive-health:check` -> PASS。
- GREEN: `$env:RUNTIME_CONTROL_E2E_BASE_URL='http://localhost:8081'; $env:RUNTIME_CONTROL_E2E_TEST_TENANT='测试租户'; $env:RUNTIME_CONTROL_E2E_TEST_USERNAME='aoteman'; $env:RUNTIME_CONTROL_E2E_TEST_PASSWORD='admin123'; pnpm e2e:edhr:archive-health` -> PASS, `status=BLOCKED`, `writes=0`。
- CHECK: `git diff --check` -> PASS。
- REVIEWER: independent reviewer `Bernoulli` -> `final_decision=pass`, no blocking issues.
- CLOSEOUT PREVIEW: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260528-edhr-archive-health-e2e-script-gate --mode preview` -> `blocked`, delete candidates `<none>`, blocked by linked-worktree fast-forward cleanup state only.
