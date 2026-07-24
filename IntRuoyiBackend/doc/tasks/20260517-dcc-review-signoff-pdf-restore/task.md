# Task: DCC 审核会签 PDF 审批矩阵恢复

## Goal

把 live MySQL 中 `产品技术要求` 类别当前漂移的审批矩阵恢复成与
`D:\ocr2\resource\审核会签.pdf` 一致的版本，并确认最终 active 路线内容严格对应
该 PDF 的 `产品技术要求` 行。

## Scope

- 先检查上一条后端任务状态；若未完成，先明确阻塞原因再开始本任务。
- 先创建当前任务文档和执行日志，再进行 live 数据恢复。
- 仅修正 `dcc_category_approval_route` / `dcc_category_approval_route_node`
  中 `category_id=1` 的 active 审批矩阵内容。
- 不切换数据库，不修改类别、目录、用户、文件或发放矩阵数据。
- 恢复后验证 `审核会签`、`批准` 与 PDF 的 `产品技术要求` 行一致。

## Previous Task Check

- Previous backend task:
  `doc/tasks/20260517-rough-wash-visual-fidelity-phase2/task.md`
- Status before this task: blocked by user priority change.
- Impact: the previous backend task is intentionally paused and does not block
  this DCC live-matrix recovery.

## Milestones

- [x] M1: Create the task package and capture the current live mismatch.
- [x] M2: Restore the active approval matrix content to the PDF-consistent mapping.
- [x] M3: Verify the final active route content against the PDF row and record evidence.

## Expected Verification

- Read-only query shows the pre-fix active route mismatch for `category_id=1`.
- Post-fix query shows:
  - stage 1 `文控审核` -> `31`
  - stage 2 `审核会签` -> `2,4,5`
  - stage 3 `批准` -> `900333,900334`
  - stage 4 `文控批准` -> `31`
- The final evidence explicitly states how those ids map back to the PDF row:
  - `QA` -> `2`
  - `QMS` -> `4`
  - `注册` -> `5`
  - `编制部门负责人或其授权代表` -> `900333,900334`

## Current Status

Completed. The current live route mismatch was corrected in place, and the final
active route content now matches the `产品技术要求` row in `审核会签.pdf`.

## Blocker And Impact

- Blocker: none currently.
- Impact: none currently.

## Final Verification Result

- PDF evidence:
  - `D:\ProjectPackage\Int\IntRuoyi\tmp\pdfs\page1-header-plus-page2-ptjcyq-row.png`
    confirms the `产品技术要求` row marks `QA`、`QMS`、`注册`.
  - `D:\ProjectPackage\Int\IntRuoyi\tmp\pdfs\page1-right-header-plus-page2-ptjcyq-right.png`
    confirms approval responsibility `编制部门负责人或其授权代表`.
- Live route after restore:
  - `route_id=65`
  - `version_no=39`
  - `remark=review-signoff-pdf-baseline-product-tech-requirement`
  - stage 1 `文控审核` -> `31`
  - stage 2 `审核会签` -> `2,4,5`
  - stage 3 `批准` -> `900333,900334`
  - stage 4 `文控批准` -> `31`
- Position mapping:
  - `2 -> QA`
  - `4 -> QMS`
  - `5 -> 注册`
  - `900333,900334 -> 部门负责人, 部门授权代表`
