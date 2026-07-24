# Task: DCC 目录管理页面乱码修复

## Goal

排查并修复 `http://localhost:8081/dcc/controlled-file/directories` 页面中的乱码问题，确保真实用户路径下的页面标题、筛选区、操作按钮、表格列名与目录弹窗文案均显示为规范简体中文。

## Scope

- 先检查同仓库上一条前端任务状态；若未完成，则显式阻塞后再启动本任务。
- 在生产代码修改前创建本任务目录与任务文档。
- 使用真实本地前端入口与运行中页面复现乱码，而不是只看源码。
- 记录 BDD、RED、GREEN 证据，并补充缺陷回归说明。
- 仅修改与本次乱码问题直接相关的前端壳层、页面文案或展示链路。
- 若缺少登录态、真实路径、依赖接口或其它前置条件，必须记录阻塞与影响后停止。

## Previous Task Check

- Previous frontend task: `doc/tasks/20260515-route-status-toggle-runtime-error/task.md`
- Status before this task: blocked by user priority switch
- Impact: the paused route status-toggle investigation does not block this DCC directory-page garbled-text fix.

## Milestones

- [x] M1: Check the previous frontend task state and block it explicitly.
- [x] M2: Create this task document and execution log before production code changes.
- [ ] M3: Reproduce the directory-page garbled-text symptom on the real running frontend and capture RED evidence.
- [ ] M4: Identify the exact rendering source of the garbled text and apply the minimal fix.
- [ ] M5: Run GREEN verification, update evidence, and prepare a scoped frontend commit.

## Expected Verification

- The running page at `http://localhost:8081/dcc/controlled-file/directories` displays normal simplified Chinese for the page title, toolbar controls, table columns, and directory form dialog.
- `execution-log.md` contains precise BDD / RED / GREEN evidence.
- At least one targeted frontend static check passes after the fix.
- If the real page can be accessed with an existing session, the runtime verification result is recorded.

## Current Status

Completed. The real DCC directory page now renders readable Chinese names again. Investigation confirmed the Vue page copy was already correct; the visible乱码 came from backend runtime seed data, which was repaired in `ruoyi-vue-pro` and verified on the live 8081 page.

## Blocker And Impact

- Blocker: none currently.
- Impact:
  - `DCC目录管理` no longer shows `DCC???????` / `??PDF??` in the directory-name column.
  - No frontend source change was required for the page template itself.

## Root Cause Summary

- The page toolbar, table headers, dialog fields, and browser title were all normal when rendered from the real local frontend.
- The only garbled text on the page came from backend records returned by `/admin-api/dcc/directories/tree`.
- The affected runtime seed rows were repaired in the backend repo and database, so the frontend page recovered without changing `src/views/dcc/controlled-file/directories/**`.

## Final Verification Result

- Real local page capture before fix -> FAIL, `output/playwright/20260515-dcc-directory-page-garbled-copy/dcc-directories-current.png` showed `DCC???????` and `??PDF??`.
- Real local page capture after backend fix -> PASS, `output/playwright/20260515-dcc-directory-page-garbled-copy/dcc-directories-fixed.png` shows `DCC运行时根目录` and `运行时PDF目录`.
- Live browser text extraction after backend fix -> PASS, the directory table text now includes `DCC运行时根目录` and `运行时PDF目录`.
