# 20260918 DCC minimal main flow code gap

## Goal

在既有 DCC 最小主流程设计文档基础上，补齐本轮静态审计确认的一个可独立验证的后端缺口：上传源件上下文必须绑定正式项目目录模板位置，并让项目编码单独唯一校验可回归验证。保持最小主流程范围，不改无关链路。

## BDD

- BDD: New upload source context -> Given a user submits a new DCC upload, When project, taxonomy, name, and upload context are checked, Then the backend requires an enabled project, a configured template location, matching taxonomy, and editor/owner access.
- BDD: Project code uniqueness -> Given two project records share one project code, When a project is created or updated, Then the backend rejects the duplicate code while preserving the existing same-project-name fallback only when the code is blank.

## Milestones

1. RED: run focused DCC service tests and capture current failure or missing coverage.
2. GREEN: add the smallest implementation/test patch for the two guards.
3. REGRESSION: rerun focused Maven tests and static checks.
4. Prepare closeout evidence without Git commit/push unless separately authorized.

## Expected Verification

- Focused Maven tests from `IntRuoyiBackend`.
- `git diff --check`.
- No E2E, database writes, service restart, Git commit or push.

## Current Status

blocked — 实现与验证已完成；仓库未提供 task-closeout-cleanup 入口，且当前轮未授权 Git 提交/推送，故保留实现证据但不标记 completed。

## 设计约束检查

- 只覆盖 DCC 上传至受控主流程的项目/目录/编码边界。
- 不引入 fallback、mock 成功、吞异常或兼容绕行。
- 复用现有权限、模板和异常码。
- 当前工作区已有并行 DCC 改动，保留其基线，不回滚无关修改。
