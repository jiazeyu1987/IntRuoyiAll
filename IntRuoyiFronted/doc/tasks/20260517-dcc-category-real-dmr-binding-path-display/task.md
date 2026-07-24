# Task: DCC 文件类别真实 DMR 绑定与路径显示修正

## Goal

将 `DCC文件类别` 的目录绑定修正为 `3.DMR` 下的真实一级目录数据，并在列表页的 `绑定目录` 列显示完整路径，例如 `3.DMR/01.图纸`。

## Scope

- 先确认上一个前端任务状态，再创建本任务记录。
- 复现当前真实页面和真实运行时接口中的绑定异常。
- 将运行时 `INTAUTH-*` 文件类别重新绑定到 `3.DMR` 下真实一级目录。
- 修改前端 `DCC文件类别` 列表页，仅将 `绑定目录` 列显示为完整路径。
- 不修改现有页面风格、接口契约、审批矩阵、权限、培训或分发逻辑。

## Previous Task Check

- Previous frontend task: `doc/tasks/20260517-dcc-file-category-list-columns-actions/task.md`
- Status before this task: completed.
- Impact: the category list page is available and can be fixed in place for the binding-path regression.

## BDD

BDD: 文件类别绑定必须使用 DMR 真实一级目录 -> Given `3.DMR` 下同时存在空壳重名目录和带编号的真实目录 / When 系统为文件类别建立目录绑定 / Then 应优先绑定到真实目录数据所在的一级目录，例如 `图纸 -> 3.DMR/01.图纸`。

BDD: 文件类别列表显示完整绑定路径 -> Given 文件类别已经绑定到 DMR 一级目录 / When 用户打开 `DCC文件类别` 列表页 / Then `绑定目录` 列应显示完整路径，例如 `3.DMR/01.图纸`，而不是仅显示叶子名或 `-`。

## Milestones

- [x] M1: 记录当前真实页面和真实接口的 RED 证据。
- [x] M2: 将运行时真实绑定修正到 `3.DMR` 下真实一级目录。
- [x] M3: 修改前端列表页显示完整绑定路径。
- [x] M4: 运行真实前端回归验证并记录 GREEN 证据。

## Expected Verification

- 真实接口验证 `GET /admin-api/dcc/file-categories`
- 真实接口验证 `GET /admin-api/dcc/directories/tree`
- 真实接口修正 `PUT /admin-api/dcc/file-categories/{id}/directory-binding`
- 真实前端验证 `http://127.0.0.1:8081/dcc/controlled-file/categories`

## Current Status

Completed. The runtime `INTAUTH-*` categories were rebound to real DMR first-level directories, and the category list now renders full directory paths such as `3.DMR/01.图纸`.

## Final Verification

- Runtime rebinding via real backend API -> PASS
  - updated runtime categories: `48`
  - sample runtime bindings:
    - `INTAUTH-1 -> 10.产品技术要求`
    - `INTAUTH-4 -> 02.说明书`
    - `INTAUTH-7 -> 04.物资采购清单`
    - `INTAUTH-8 -> 05.采购技术要求`
    - `INTAUTH-9 -> 01.图纸`
- `NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check` -> PASS
- `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-category-real-dmr-binding-path-display run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260517-dcc-category-real-dmr-binding-path-display\scripts\verify-dcc-category-real-dmr-binding-path-display.mjs` -> PASS
- Real page result on `http://127.0.0.1:8081/dcc/controlled-file/categories`:
  - `INTAUTH-1 -> 3.DMR/10.产品技术要求`
  - `INTAUTH-2 -> 3.DMR/生产用设备清单`
  - `INTAUTH-3 -> 3.DMR/检验用设备清单`
  - `INTAUTH-4 -> 3.DMR/02.说明书`
  - `INTAUTH-7 -> 3.DMR/04.物资采购清单`
  - `INTAUTH-8 -> 3.DMR/05.采购技术要求`
  - `INTAUTH-9 -> 3.DMR/01.图纸`
  - `INTAUTH-10 -> 3.DMR/工艺流程图`

## Root Cause

- The real runtime dataset rendered by frontend `8081` was the `INTAUTH-*` category set served by backend `48081`, not the parallel dataset inspected earlier.
- In that runtime dataset, several categories were still bound to the DMR root or had no binding at all.
- The frontend list page also displayed only the leaf directory name, not the full DMR path, so even correct runtime bindings would not have met the required display contract.

## Blocker And Impact

- Temporary verification blocker: the backend on `48081` was still starting during part of the runtime inspection and briefly returned connection failures.
- Impact: runtime verification was delayed until `http://127.0.0.1:48081/v3/api-docs` returned `200`.

## Closeout

- PREVIEW: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260517-dcc-category-real-dmr-binding-path-display --mode preview` -> ready, keep `task.md` and `execution-log.md`, delete none, blocked none.
