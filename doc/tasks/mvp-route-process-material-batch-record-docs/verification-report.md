# Verification Report

## Scope Verified

- 新 PRD：`docs/product/frontline-process-material-batch-record-mvp-prd.md`
- 新用户操作文档：`docs/product/frontline-process-material-batch-record-mvp-user-operation.md`
- 任务记录：`doc/tasks/mvp-route-process-material-batch-record-docs/task.md`

## Verification Evidence

- `python -X utf8 C:\Users\BJB110\.codex\skills\product-requirements-docs\scripts\validate_product_requirements.py --self-test` -> PASS，PRD 技能校验脚本自身结构检查通过。
- 自定义结构校验 -> PASS，新 PRD 包含 Purpose and Scope、Evidence Reviewed、Product Summary、Target Users、First Version Scope、Non-Goals、Functional Requirements、Business Rules、States and Transitions、Edge Cases、Acceptance Criteria、Open Questions、Product Blockers。
- 自定义结构校验 -> PASS，新用户操作文档包含 Purpose and Scope、Evidence Reviewed、Roles、Primary User Flows、Alternate Flows、Error Flows、Out-of-Scope Flows、Frontend Operation Notes、Admin Configuration Notes、Acceptance Checklist、Open Questions。
- 关键词核对 -> PASS，新文档包含工艺路线工序、灰色、绿色、完成数量、ERP 同步、最小值、快照、审批、删除、用料比例、产品 BOM 等关键约束。
- UTF-8 读取 -> PASS，两份新文档均使用 `python -X utf8` 成功读取。

## Result

PASS。文档按最小 MVP 重写完成，未覆盖原 `docs/product/prd.md`、`docs/product/user-flows.md` 或 `docs/product/acceptance-criteria.md`。

## Closeout

- `task-closeout-cleanup --mode preview` -> PASS，delete=<none>，blocked=<none>，warnings=<none>。
- `task-closeout-cleanup --mode apply` -> PASS，deleted_paths=<none>。
- `scripts\preflight\branch-runtime-port-guard.ps1` -> PASS，提交/合并前端口合同检查通过。
- `git commit -m "docs: add frontline process material batch record MVP"` -> PASS，commit `11c5666ad`。
- `git merge --ff-only codex/20260831-route-process-material-binding` -> PASS，当前 `int_main` 已包含该分支，Already up to date。

## Notes

- 本次为产品文档与操作文档重写任务，未修改生产代码。
- “删除之前已经实现的物料逻辑”已在 PRD 中写成明确交付要求：一线生产批记录物料不得继续由产品 BOM、用料比例或独立工序主数据物料版本驱动；非本功能范围的 ERP 同步、排产和仓储用料能力不在删除范围内。
- 本轮用户授权提交、合并；未执行 push。
