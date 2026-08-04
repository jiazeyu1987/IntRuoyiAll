# Verification Report

## Result

PASS，审批中心已增加独立“申请人”列，DCC 业务摘要不再重复展示申请人，四个审批视图已升级用户列配置 key；本任务提交已同步到 `origin/int_main`。

## Scope Verified

- 待办、已办、我发起的、抄送列表均通过 `isApprovalColumnVisible('applicant')` 渲染“申请人”列。
- 申请人列只读取正式 `ApprovalTaskSummaryVO.initiatorUserId`；缺失时沿用审批中心现有空值显示语义。
- “申请人”列位于“业务摘要”之后、“节点”之前。
- DCC `resolveDccKeyFields` 已移除重复的“申请人”摘要项。
- 四个表格 key 已升级为 `approval.center.todo.applicant.v1`、`approval.center.done.applicant.v1`、`approval.center.myInitiated.applicant.v1`、`approval.center.cc.applicant.v1`。

## Commands

- RED: `node tests/e2e/approval-center-applicant-column-static.spec.js` -> FAIL，旧表格没有独立申请人列。
- GREEN: `node tests/e2e/approval-center-applicant-column-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/approval-center-standard-list-template-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/approval-center-chinese-copy-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/approval-center-reviewer-column-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/approval-center-signature-pending-standard-list-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `validate_frontend_feature.py --evidence doc/tasks/20260804-approval-center-applicant-column/frontend-feature-evidence.md` -> PASS。
- GREEN: `git diff --check -- <task-owned records>` -> PASS。
- GREEN: `task-closeout-cleanup --mode preview` -> PASS，计划删除临时 `frontend-feature-evidence.md`，无 blocked/warnings。
- GREEN: `task-closeout-cleanup --mode apply` -> PASS，已删除临时 `frontend-feature-evidence.md`。
- GREEN: `git -c http.https://github.com.proxy=http://127.0.0.1:8902 ls-remote origin HEAD` -> PASS。
- GREEN: `scripts\preflight\branch-runtime-port-guard.ps1` -> PASS。
- GREEN: `git -c http.https://github.com.proxy=http://127.0.0.1:8902 push origin int_main` -> PASS。
- GREEN: `git status --short --branch` -> `## int_main...origin/int_main`，本任务提交不再 ahead。

## Git Notes

- 任务前既有脏工作区基线：`e4495a624 Baseline: preserve existing worktree changes before approval center applicant column`。
- 本任务实现被共享分支并行基线提交 `50bca8e9f` / `7cc9284a1` 吞入；未执行 amend、reset 或历史重写。
- 本地收尾提交：`37cc041af docs: close approval center applicant column task`。
- 合同/阻塞记录提交：`93f935f09 test: align approval center applicant column contracts`。
- GitHub 推送阻塞根因是全局 URL 级代理仍指向未监听的 `127.0.0.1:7890`；本次使用一次性 `-c http.https://github.com.proxy=http://127.0.0.1:8902` 对齐当前系统代理完成远端同步，未修改全局 Git 配置。
