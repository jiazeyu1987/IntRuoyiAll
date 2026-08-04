# Execution Log

## User Intent

- 2026-08-04：用户要求在审批中心单独增加“申请人”列。
- 验收解释：待办、已办、我发起的和抄送列表均显示独立申请人列；DCC 业务摘要不重复显示相同申请人信息。

## Baseline

- 仓库：`E:\IntRuoyi`。
- 分支：`int_main`，任务开始时领先 `origin/int_main` 2 个提交。
- 工作区存在其它任务的 tracked、staged 和 untracked 改动；按项目规则先作为独立基线提交保存，本任务文件不混入基线。
- 申请人正式来源：审批中心响应 `ApprovalTaskSummary.initiatorUserId` / 前端 `ApprovalTaskSummaryVO.initiatorUserId`。
- 当前审批中心表格已使用 `useUserTableColumns`；新增默认列需要升级四个稳定 table key，避免历史列配置继续隐藏新列。

## BDD / TDD

BDD: 审批中心独立显示申请人 -> Given 审批任务返回正式 initiatorUserId When 用户打开待办、已办、我发起的或抄送列表 Then 业务摘要后显示“申请人”独立列并展示对应用户标识。

BDD: DCC 摘要不重复申请人 -> Given DCC 审批任务同时显示业务摘要和申请人列 When 列表完成渲染 Then DCC 关键字段摘要不再重复展示申请人。

BDD: 既有用户获得新默认列 -> Given 用户存在旧审批中心列配置 When 新版本首次加载审批中心列表 Then 四个审批视图使用升级后的 table key 并加载包含申请人的新默认列集合。

## Milestone Updates

### M1 基线与契约

- 状态：completed。
- 分支端口门禁：`scripts/preflight/branch-runtime-port-guard.ps1` -> PASS，`int_main` 使用前端 `8081`、后端 `48081`。
- 既有脏工作区基线提交：`e4495a624 Baseline: preserve existing worktree changes before approval center applicant column`。
- 基线提交包含 24 个既有文件：前端 `package.json`、QA 规程页、排产/班组长/审批中心缓存相关测试、审批中心缓存任务文档、PQC 全屏任务文档、标准列表任务文档、上传审批快捷操作任务文档，以及并发任务已删除的 DCC 审批操作区任务文档。
- 基线提交后 `git status --short --branch` 仅剩本任务 3 个未跟踪文档，未混入审批中心申请人实现。
- 正式数据合同确认：前后端审批任务摘要均已有 `initiatorUserId`，本任务无需新增 API 或后端字段。
- 用户列配置门禁确认：四个审批视图必须升级稳定 table key，确保历史用户加载包含申请人的新默认列集合。

### M2 RED

- 状态：completed。
- RED: `node tests/e2e/approval-center-applicant-column-static.spec.js` -> FAIL，预期原因：旧表格没有 `isApprovalColumnVisible('applicant')` 独立列，申请人仍在 DCC 摘要字段内。

### M3 GREEN

- 状态：completed。
- 实现：在审批中心 Element Plus 表格中将“申请人”作为业务摘要后的独立列渲染，统一通过 `resolveApplicantLabel(row)` 读取正式 `initiatorUserId`。
- 实现：四个审批中心视图的 table key 升级为 `approval.center.*.applicant.v1`，使既有用户加载包含申请人的新默认列集合。
- 实现：DCC 关键字段摘要移除重复的“申请人”项，避免同一信息在业务摘要和独立列中重复。
- 代码归属异常：上述实现与新增聚焦合同已被共享分支并行基线提交 `50bca8e9f` 吞入；标准列表合同 table key 更新被并行基线提交 `7cc9284a1` 吞入。按共享分支并发基线门禁记录，不重写历史、不 amend、不 reset。

### M4 验证与收尾

- 状态：completed。
- GREEN: `node tests/e2e/approval-center-applicant-column-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/approval-center-standard-list-template-static.spec.js` -> PASS，覆盖 TODO/DONE/MY_INITIATED/CC、签名待处理合并和审核人列合同。
- GREEN: `node tests/e2e/approval-center-chinese-copy-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/approval-center-reviewer-column-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/approval-center-signature-pending-standard-list-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260804-approval-center-applicant-column/frontend-feature-evidence.md` -> PASS。
- GREEN: `git diff --check -- <task-owned records>` -> PASS。
- GREEN: `task-closeout-cleanup --mode preview` -> PASS，keep `task.md`、`execution-log.md`、`verification-report.md`，delete `frontend-feature-evidence.md`，blocked/warnings 均为 `<none>`。
- GREEN: `task-closeout-cleanup --mode apply` -> PASS，已删除 `frontend-feature-evidence.md`。
- 本地收尾提交：`37cc041af docs: close approval center applicant column task`。
- BLOCKER: `git push origin int_main` -> FAIL，`Failed to connect to github.com port 443 via 127.0.0.1`。
- 代理复核：第一次窄正则只看到 `http.version=HTTP/1.1`；完整 `git config --show-origin --list` 发现 URL 级代理 `http.https://github.com.proxy=http://127.0.0.1:7890`，但 `127.0.0.1:7890` 未监听，`Test-NetConnection github.com -Port 443` 直连也失败。
- 2026-08-04 续跑复核：`git ls-remote origin HEAD` -> FAIL，仍为 `Failed to connect to github.com port 443 via 127.0.0.1`；`node tests/e2e/approval-center-reviewer-column-static.spec.js` 与 `node tests/e2e/approval-center-signature-pending-standard-list-static.spec.js` -> PASS。
- 本地合同/阻塞记录提交：`93f935f09 test: align approval center applicant column contracts`。
- 2026-08-05 代理恢复路径：Windows 用户代理配置为 `127.0.0.1:8902` 且端口监听，`git -c http.https://github.com.proxy=http://127.0.0.1:8902 ls-remote origin HEAD` -> PASS。
- GREEN: `scripts\preflight\branch-runtime-port-guard.ps1` -> PASS，`int_main` 使用前端 `8081`、后端 `48081`。
- GREEN: `git -c http.https://github.com.proxy=http://127.0.0.1:8902 push origin int_main` -> PASS，`b98d82594..93f935f09  int_main -> int_main`。
- GREEN: `git status --short --branch` -> `## int_main...origin/int_main`，本任务提交已同步远端；工作区仍保留其它并行任务改动，未触碰。

## Blockers

- 当前无产品或接口 blocker。
- 当前无本任务 blocker。
