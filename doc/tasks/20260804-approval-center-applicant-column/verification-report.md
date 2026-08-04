# Verification Report

## Result

功能验证 PASS；Git 远端同步 BLOCKED。审批中心已增加独立“申请人”列，DCC 业务摘要不再重复展示申请人，四个审批视图已升级用户列配置 key。

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

## Git Notes

- 任务前既有脏工作区基线：`e4495a624 Baseline: preserve existing worktree changes before approval center applicant column`。
- 本任务实现被共享分支并行基线提交 `50bca8e9f` / `7cc9284a1` 吞入；未执行 amend、reset 或历史重写。
- 本地收尾提交：`37cc041af docs: close approval center applicant column task`。
- `git push origin int_main` 失败：Git 尝试连接 `github.com:443 via 127.0.0.1`，但 `127.0.0.1:7890` 未监听；GitHub 443 直连测试也失败。
- 续跑 `git ls-remote origin HEAD` 仍失败；当前 `int_main` 仍领先 `origin/int_main`，任务状态保持 blocked，远端同步成功且不再 ahead 前不得标记 completed。
