# Execution Log - QA 规程 PDF 字段对齐修复

## User Intent

- 用户要求检查并修复 QA 页面“工序检验方法与抽样方案”列表与指定 PDF 检验规程不一致的问题。

## Preconditions

- 2026-08-06 读取 `docs/frontend-development.md`、`docs/e2e-rules.md`、`docs/task-closeout-rules.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md`。
- 使用 `bug-regression-fix-loop` 技能，已读取 `SKILL.md` 和 `references/bug-contract.md`。
- PDF 原路径缺少 `1` 子目录；实际文件位于 `C:\Users\BJB110\Desktop\文档\1\PQC-IDI-001（B 0）按压式球囊扩充压力泵组装过程检验规程--2026.01.04生效.pdf`。

## BDD

- BDD: 气密性检测方法按 PDF 原文展示 -> Given QA 规程选择压力泵项目，When 查看工序检验方法与抽样方案列表，Then 气密性高压检测和低压检测的“检验方法”应包含 PDF 原文完整操作步骤，而不是仅显示“进行检测”。

## Baseline

- 进入本任务前 `git status --short --branch` 显示大量已暂存、未暂存和未跟踪改动，且分支 `int_main` 落后 `origin/int_main` 16 个提交。
- 本任务文件 `doc/tasks/20260806-qa-regulation-pdf-field-alignment/` 不应进入脏工作区基线提交。

## RED / GREEN

- RED: pending.
- GREEN: pending.

## Milestone Notes

- M1 in_progress：建立任务记录并准备提交进入本任务前的脏工作区基线。

