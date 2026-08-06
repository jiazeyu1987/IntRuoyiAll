# Execution Log - QA 规程 PDF 字段对齐修复

## User Intent

- 用户要求检查并修复 QA 页面“工序检验方法与抽样方案”列表与指定 PDF 检验规程不一致的问题。

## Preconditions

- 2026-08-06 读取 `docs/frontend-development.md`、`docs/e2e-rules.md`、`docs/task-closeout-rules.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md`。
- 使用 `bug-regression-fix-loop` 技能，已读取 `SKILL.md` 和 `references/bug-contract.md`。
- PDF 原路径缺少 `1` 子目录；实际文件位于 `C:\Users\BJB110\Desktop\文档\1\PQC-IDI-001（B 0）按压式球囊扩充压力泵组装过程检验规程--2026.01.04生效.pdf`。

## BDD

- BDD: 气密性检测列归属保持一致 -> Given QA 规程选择压力泵项目，When 查看工序检验方法与抽样方案列表，Then 气密性高压/低压检测的“检验方法”保持 PDF 方法栏的简短装配检测句，完整接气源、观察压力表和回零要求保持在“接受标准”栏。

## Baseline

- 进入本任务前工作区持续存在并行任务脏改动和并行提交；过程中出现 `c4675d197 chore: baseline pre-existing dirty worktree`、`2eee67dc1 fix: remember QA project selector` 等并行提交。
- 本任务只拥有 `IntRuoyiFronted/tests/e2e/qa-regulation-pressure-pump-pdf-field-alignment-static.spec.cjs` 与 `doc/tasks/20260806-qa-regulation-pdf-field-alignment/`。

## RED / GREEN

- RED: 不适用。复查 PDF 第 7 页后确认当前 QA 模板的高压/低压“检验方法”已与 PDF 方法栏一致；直接修改生产模板会把“接受标准”误写入“检验方法”。
- GREEN: `node tests/e2e/qa-regulation-pressure-pump-pdf-field-alignment-static.spec.cjs` -> PASS。
- GREEN: `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS。

## Verification Evidence

- 新增静态合同锁定 `PP-020-AIRTIGHT-NEGATIVE`、`PP-021-AIRTIGHT-HIGH`、`PP-022-AIRTIGHT-LOW` 的项目名、检验方法、接受标准、原文摘录、检验设备和抽样方案。
- 合同明确高压/低压完整充气检测步骤属于 `standardText/sourceOriginalExcerpt`，简短“进行检测”句属于 `inspectionMethod/sourceOriginalMethod`。

## Milestone Notes

- M1 completed：建立任务记录，并记录进入本任务前存在并行脏改动。
- M2 completed：复查 PDF 扫描页 7/8，确认第 8 页无 5.1 列表残余，气密性行在第 7 页结束。
- M3 completed：新增 `qa-regulation-pressure-pump-pdf-field-alignment-static.spec.cjs`。
- M4 completed：聚焦 QA 静态合同通过。
- M5 completed：`task_closeout.py --mode preview` 与 `--mode apply` 均通过，无删除项、无阻塞；经验沉淀技能已执行，因既有经验文档存在并行改动，本次仅在任务记录保留经验，避免混入非本任务文件。

## Commits

- Implementation commit: `64d51463c20d89706d0e113977ffb50fa8c3cfd2` (`test: lock QA pressure pump PDF field alignment`)。
