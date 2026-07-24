# Task: DCC 文件类别列表前端 E2E 验证

## Goal

使用真实前端入口和真实浏览器路径，验证 `http://127.0.0.1:8081` 上的 DCC 文件类别列表页能够成功加载并在前端表格中看到文件类别数据。

## Scope

- 先检查同仓库上一条前端任务状态；若未完成，则先显式阻塞后再启动本任务。
- 在开始验证前创建本任务目录、任务文档、执行日志和验证报告。
- 使用真实前端入口、真实登录路径和真实页面数据打开 DCC 文件类别列表页。
- 使用 Playwright 完成页面访问、列表加载和数据可见性验证。
- 如发现前端无入口、登录失败、接口失败或页面无数据，必须记录明确阻塞与影响。
- 本任务默认只做验证；除非验证中发现必须修复才能继续，否则不改生产代码。

## Previous Task Check

- Previous frontend task: `doc/tasks/20260515-dcc-access-rules-undefined-user-label/task.md`
- Status before this task: blocked
- Impact: the previous access-rules task is explicitly paused by user reprioritization and does not block this file-category list E2E verification.

## Milestones

- [x] M1: Check the previous frontend task state and block it explicitly if needed.
- [x] M2: Create this task document, execution log, and verification report before running verification.
- [x] M3: Reproduce the real frontend path to the DCC file-category list page.
- [x] M4: Verify the page can visibly render file-category list data.
- [x] M5: Record PASS/FAIL evidence and remaining blockers.

## Expected Verification

- A real browser path can log into `http://127.0.0.1:8081`.
- The browser can reach the DCC file-category list page.
- The frontend list visibly shows file-category rows rather than an empty or failed state.
- Verification evidence records the page path, observed rows, and any gaps.

## Current Status

Completed on `2026-05-15`. After replaying the real login path and correcting local runtime prerequisites, the DCC file-category list page visibly rendered real category data in the frontend table.

## Blocker And Impact

- Initial blockers were runtime-only, not product-code regressions:
  - first backend run on `48081` lacked the IntAuth internal token needed by `dcc/approval-positions`
  - early frontend launch was misstarted on port `80` and then had stale connection failures while the backend was restarting
- Impact: once the local frontend and backend runtimes were restarted with the correct parameters, the requested E2E condition passed without needing frontend code edits.

## Final Verification Result

- Real browser login succeeded at `http://127.0.0.1:8081/login?redirect=/index`.
- Real route `http://127.0.0.1:8081/dcc/controlled-file/categories` opened successfully.
- Visible table row confirmed:
  - `DCC_RUNTIME_CATEGORY`
  - `运行时文件类别`
- Backend response evidence:
  - `GET /admin-api/dcc/file-categories` -> `code=0`
  - non-empty `data` array including category `900201 / DCC_RUNTIME_CATEGORY / 运行时文件类别`
