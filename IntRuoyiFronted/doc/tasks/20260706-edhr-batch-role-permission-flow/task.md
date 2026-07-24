# eDHR 批次执行角色化权限与操作体验改造（前端）

## 任务目标
在独立 worktree `edhr_batch_improve` 中完成 eDHR 批次执行前端角色化展示与操作体验改造，使批次详情、任务列表、填写页、审核页、批准页只根据后端能力字段渲染当前登录人的可见内容和可执行动作。

## Milestones
- [x] 建立前端任务记录、经验门禁、BDD 场景和运行态计划
- [ ] 盘点现有批次详情、工作任务、填写/审批入口和权限按钮
- [ ] 编写失败测试覆盖角色化能力字段驱动渲染
- [ ] 实现前端按后端能力字段展示工序、按钮、禁用原因和角色状态
- [ ] 运行前端静态/组件/E2E 回归
- [ ] 支撑真实 E2E 和 5 个角色子 agent 验收
- [x] 提交、融合 `int_main`、合并后回归和 worktree 清理

## Expected Verification
- 批次详情保留完整工序列表，显示工序名称、状态、处理人、阻塞原因和当前用户动作。
- 前端按钮只由后端 `visible / currentUserRole / allowedActions / disabledReason / assignee / reviewer / approver` 等能力字段驱动。
- 填写人、审核人、批准人、生产负责人、无关人员各自看到的内容和操作符合 BDD。
- 红色关闭阻塞项只作为关闭前阻塞原因，不作为填写/审核/批准入口。
- 前端测试、真实 E2E 和 5 个角色验收均通过。

## Current Status
completed

## 当前状态
前端已按后端能力字段完成角色化展示与操作入口收敛：批次详情显示工序名称、角色、允许动作、禁用原因；审核/批准入口按工作任务类型进入审批详情；特殊节点按钮由 CLOSE 动作驱动；审核候选下拉显示用户 ID 以避免误选。路线 900025 / 批次 900000000462 / 执行 778 已通过 worktree 真实数据 E2E；5 个角色独立复核均 PASS。前端提交 `19990ce1f` 已快进融合 `int_main`，合并后脚本语法检查与静态契约检查均 PASS；`edhr_batch_improve` worktree 已删除。本任务完成。

## Previous Task Check
- 前端 worktree 从 `int_main` 创建；仓库内既有任务文档很多，最近文档为 checkout 时间戳，不足以可靠判定唯一“上一任务”。
- 本任务在独立 worktree、独立分支中推进，不接管主工作区未提交改动。

## 经验门禁
- PowerShell：已读取 `docs/powershell-memory.md`，命令使用 UTF-8，禁止 `&&`。
- Worktree：已读取 `docs/worktree-memory.md` 与 worktree skill；开发必须在 `D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_batch_improve`，不复用主工作区 `8081/48081` 做证据。
- 前端特性：已读取 frontend-feature-delivery skill 和 evidence contract；不得 mock 数据、隐藏 API 错误或引入 unrelated redesign。
- BDD/TDD：先记录 Given/When/Then，再 RED -> GREEN -> REGRESSION。
- 真实 E2E：执行前必须读取 `docs/login-access.md` 并跑官方登录预检；写入型 E2E 优先测试租户，芋道源码默认只读。
- 统一样式：涉及 UI 改动前需读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`。
- 禁止 fallback：不得用 mock、静默降级、临时权限放宽或接口绕过掩盖问题。

## 设计约束检查
- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，前端消费后端能力字段，不自行扩大权限。
- 是否存在临时补丁或绕过：否。

## BDD Scenarios
BDD: 填写人只能处理自己的填写任务 -> Given 批次存在分配给填写人的 FILL/REWORK 任务 When 填写人进入批次详情 Then 只显示填写/返工可执行动作，审核、批准、关闭和非授权节点按钮不可用或不可见。

BDD: 审核人只读审核 -> Given 表单已提交并生成审核任务 When 审核人进入审核页 Then 表单值只读，显示审核通过/驳回入口，不显示字段编辑或批准入口。

BDD: 批准人只处理批准阶段 -> Given 审核已完成并生成批准任务 When 批准人进入批准页 Then 可只读查看证据链并批准/驳回；审核未完成时显示明确禁用原因。

BDD: 生产负责人监管但不能代签 -> Given 生产负责人进入批次详情 When 查看批次执行 Then 可见全局进度、阻塞项和人员状态，只显示授权的监管动作，不显示代填、代审、代批入口。

BDD: 无关人员隔离 -> Given 当前用户无该批次执行身份 When 进入详情 Then 页面无执行按钮，或被导航到无权限状态。

## Verification Log
- RED: `node tests\e2e\edhr-batch-pending-form-entry-static.spec.js` -> FAIL，待处理列表未完全由后端 `activeWorkTaskId` / `allowedActions` / `disabledReason` 驱动。
- GREEN: `node tests\e2e\edhr-batch-pending-form-entry-static.spec.js` -> PASS，待处理列表显示角色标签、工序名称、禁用原因，审核/批准动作进入审批页，特殊节点入口由 CLOSE 动作驱动。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS，前端类型检查通过。默认堆内存运行曾 OOM，扩大 Node heap 后通过，未发现类型错误。
- GREEN: `node --check tests\e2e\edhr-batch-role-permission-real-flow.e2e.js` -> PASS，真实 E2E 脚本语法通过。
- REAL E2E: `node tests\e2e\edhr-batch-role-permission-real-flow.e2e.js` -> PASS，baseUrl `http://127.0.0.1:8095`，backend `http://127.0.0.1:48095`，batch `900000000462`，execution `778`，process `吹球囊成型`。
- ROLE VERIFY: 填写人、审核人、批准人、生产负责人、无关人员 5 个角色子任务复核均 PASS。


## 当前状态更新 - 2026-07-06 19:41:30 +08:00
- 已完成真实数据 E2E 与前后端 targeted 回归。
- 后端 targeted 回归：156 个用例通过，0 failure / 0 error。
- 等待五角色独立子任务复核、提交、融合 int_main、清理 worktree。

## 当前状态更新 - 2026-07-06 21:05:00 +08:00
- 最新真实 E2E 已在 worktree 前端 8095 / 后端 48095 重跑通过，证据记录 routeCode=900025、batchExecutionId=900000000462、batchTaskId=2732、executionId=778。
- 前端静态契约、类型检查、真实 E2E 均 PASS。
- 角色复核全部 PASS：填写人关闭态无写入口；审核人只读审核并推动独立批准任务；批准人只处理批准阶段；生产负责人只看/操作 CLOSE 授权动作；无关人员被 403 或只读隔离。
- 当前进入提交、融合 `int_main`、合并后验证和 worktree 清理阶段。

## 合并后验证证据 - 2026-07-06 21:20:00 +08:00
- GREEN: merge-frontend-fast-forward -> PASS，前端 `edhr_batch_improve` 已快进融合到 `int_main`，融合提交 `19990ce1f`。
- GREEN: merge-frontend-real-e2e-script-check -> PASS，command=node --check tests\e2e\edhr-batch-role-permission-real-flow.e2e.js。
- GREEN: merge-frontend-static-contract -> PASS，command=node tests\e2e\edhr-batch-pending-form-entry-static.spec.js，result=edhr batch pending form entry static contract passed。

## 收尾清理证据 - 2026-07-06 21:24:00 +08:00
- GREEN: worktree-runtime-stop -> PASS，已停止本次 worktree 后端 `48095` 与前端 `8095` 运行进程。
- GREEN: worktree-remove -> PASS，已删除 `D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_batch_improve`，`git worktree list` 不再包含 `edhr_batch_improve`。
- GREEN: final-status -> PASS，本任务前端工作完成；主工作区仍保留与本任务无关的既有未提交改动，未纳入本任务提交。
