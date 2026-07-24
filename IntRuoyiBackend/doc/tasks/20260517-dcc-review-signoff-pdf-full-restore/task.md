# Task: DCC 审核会签 PDF 47 类全量恢复

## Goal

把当前 live MySQL 中除 `产品技术要求` 之外其余 47 个 DCC 文件类别的审批矩阵
恢复成与 `D:\ocr2\resource\审核会签.pdf` 一致的 DCC 固定四层版本，并记录
PDF 到 DCC 路由的明确映射规则和剩余风险。

## Scope

- 先检查上一条后端任务状态；若未完成，先显式记录阻塞后再开始本任务。
- 先创建当前任务文档和执行日志，再进行 live 数据恢复。
- 只修改 DCC 审批矩阵相关 live 数据：
  - `dcc_category_approval_route`
  - `dcc_category_approval_route_node`
  - 若恢复所需的批准岗位在 live 库中缺失，则允许最小补齐 `dcc_approval_position`
- 不切换数据库，不修改文件类别、目录、权限、培训、分发或受控文件数据。
- 当前已单独恢复的 `category_id=1 / 产品技术要求` 保持不变；本次仅补齐剩余 47 类。

## Previous Task Check

- Previous backend task:
  `doc/tasks/20260517-dcc-review-signoff-pdf-restore/task.md`
- Status before this task: completed.
- Impact: the single-category restore is already closed and can serve as the
  baseline reference for this full recovery.

## Milestones

- [x] M1: Create the task package and capture the current live baseline gap.
- [x] M2: Derive the remaining 47 category matrices from the PDF-derived seed and current DCC runtime constraints.
- [x] M3: Restore the 47 live active routes in MySQL.
- [x] M4: Verify active-route coverage, route content, and any newly added approval positions.

## Expected Verification

- Pre-fix query shows 48 categories but only 1 active approval route.
- Post-fix query shows 48 categories and 48 active approval routes.
- Post-fix route verification proves categories `INTAUTH-2` through `INTAUTH-48`
  each have fixed four stages:
  - stage 1 `文控审核`
  - stage 2 `审核会签`
  - stage 3 `批准`
  - stage 4 `文控批准`
- Task evidence records the PDF-to-DCC mapping rule:
  - `标准化审核 / 文档管理员` -> fixed `文控审核 + 文控批准`
  - `审核会签` dots -> DCC stage-2 signoff positions
  - `批准` text -> DCC stage-3 approval positions

## Current Status

Completed. The remaining 47 categories now have active live approval routes, and
the previously restored `产品技术要求` route has been kept on its earlier
confirmed version.

## Blocker And Impact

- Blocker: none currently.
- Impact: matrix coverage is restored, but several signoff positions and the new
  `研发部门负责人` / `总经理` approval positions still have no live assignee.

## Final Verification Result

- Coverage:
  - `dcc_file_category` active count: `48`
  - `dcc_category_approval_route` active count: `48`
- Representative active routes:
  - `INTAUTH-1 / 产品技术要求` -> `1:31 | 2:2,4,5 | 3:900333,900334 | 4:31`
  - `INTAUTH-2 / 生产用设备清单` -> `1:31 | 2:1,2,4,8 | 3:900335,900336 | 4:31`
  - `INTAUTH-28 / 项目立项书` -> `1:31 | 2:1 | 3:900337,900338 | 4:31`
  - `INTAUTH-39 / 设计转移方案和报告` -> `1:31 | 2:1,2,3,4,7,8,9,10,11 | 3:900335,900336 | 4:31`
  - `INTAUTH-48 / 生产许可/备案资料汇编` -> `1:31 | 2:1,4,6 | 3:900335,900336 | 4:31`
- Added approval positions:
  - `900335 / 编制部门负责人` -> copied 1 active assignment from `900333 / 部门负责人`
  - `900336 / 授权代表` -> copied 1 active assignment from `900334 / 部门授权代表`
  - `900337 / 研发部门负责人` -> `0` active assignment
  - `900338 / 总经理` -> `0` active assignment
- PDF-to-DCC mapping used for this restore:
  - Stage 1 and stage 4 stay fixed as `文控`
  - PDF `标准化审核 / 文档管理员` is represented by the fixed doc-control stages and is therefore excluded from stage-2 signoff candidates
  - Stage-2 signoff candidates come from the bundled PDF-derived matrix seed after removing `文档管理员`
  - Stage-3 approval candidates use split approval roles required by the DCC two-position approval contract
- Drift correction:
  - during this task `category_id=1` briefly drifted to `route_id=66 / version_no=40`
  - final active route was restored to `route_id=65 / version_no=39`
