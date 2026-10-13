# UM-04 用户编辑保留已有停用岗位

## Task Goal

仅修复 UM-04：编辑用户普通资料时保留已有停用岗位；只有明确移除才删除岗位绑定，禁止新增绑定停用岗位。按已评审方案在独立 worktree 实施，经子 Agent 开发、独立评审、主 Agent 核验及真实前端验收后融合 int_main。

## Scope And Authorization

- 用户目标：修 UM-04；此前明确暂不做 UM-02。沿用本线程独立 worktree、子 Agent 实施、主 Agent 评审、静态验证和真实 E2E 后融合的交付方式。
- 仅处理用户编辑 get/update、用户岗位绑定一致性校验、UserForm.vue 及必要 API 类型与对应测试；CREATE 的岗位可选规则和新增岗位必须启用规则保持。
- 不实施 UM-01/02/03/06/07/08/10，不改导入、岗位启停政策、角色授权、密码/会话行为，不做 schema 迁移。
- 只做任务必需的本地实现/收尾提交及融合；不推送、发布或操作远程服务器。2026-10-08用户明确授权恢复原测试环境，已按正式脚本用原包恢复；后续不再启停共享服务。主仓并行资产保持。
- 真实前端验收只创建、编辑和清理任务自有用户及岗位；全部被验收业务动作由 Playwright 页面完成，API/DB 只能只读核验。保留共享测试租户及管理员。

## Source And Baseline

- 已评审合同：doc/tasks/20261006-user-management-remediation-plan/remediation-plan.md 的 UM-04、AC-04-01/02；对应 review-report.md 和 test-plan.md。
- 初始任务基线：a3b8bedfeece394f286e23a9d5144361b951e8ac。
- 创建 managed worktree 前已检查本线程附件：只有已归档 UM-05 worktree，无可复用活动 worktree。
- 建任务时主仓有六项无关 dirty 文件，属于历史前置。实际融合以a1640b169已提交基线为准，两项MES并行文件零交集且字节完整保留；不提交、stash、reset、restore 或清理并行资产。

## Acceptance Criteria

- AC-04-01：编辑 get 返回同租户正式既有岗位 id/name/status；停用但未删除岗位标注已停用并保持选中。仅修改用户资料时，用户 postIds 与 user_post 绑定均不改变；打开失败或取消零写入。
- AC-04-02：编辑更新 postIds 必须是完整目标集合，缺失/非法集合明确失败，空数组明确移除全部。用户锁内校验原集合 B 与提交 T：保留项允许停用，新增项必须存在且启用；明确删除按差集执行。用户及绑定同事务，错误全部回滚。
- 已删除、跨租户或用户 JSON postIds 与关联表不一致属于正式数据完整性错误；拒绝并报告，不猜名称、不补齐、不静默过滤。
- 既有停用项允许保留或明确移除；本次编辑中移除后不可重新添加。CREATE 保持岗位可选，所有新增项必须启用。

## Milestones

- [x] M0：当前链条与测试/依赖/运行前置核对，建立 managed worktree、分支及预约槽位。
- [x] M1：子 Agent 独立形成现状与实施合同，主 Agent 评审通过并形成后端/前端精确任务单。
- [x] M2：子 Agent 实施最小修复与有意义的后端/真实前端 handler 回归。
- [x] M3：独立三层评审、主 Agent 全链静态审查、定向验证及真实 Playwright E2E。
- [x] M4：记录 ready_for_closeout，清理任务资产、精确本地提交、保护并行修改后融合 int_main、managed 归档和槽位释放，最终审计后 completed。

## Expected Verification

- 后端实际 Controller/service/Mapper 链：已有停用保留、显式单项/全部移除、启用新增、停用/删除/跨租户新增拒绝、完整集合必填、一致性错误、事务回滚、CREATE 可选及租户/权限边界。
- 前端真实生产 SFC/handler/API 行为：停用标记/保留选中、普通资料保存完整集合、移除后不可重选、取消与打开失败零更新、失败不伪成功、创建候选规则。
- 静态逐节点核对用户入口、查询接口、角色权限、正式资料来源、服务锁/事务、两份岗位持久化和异常分支。
- 真实 E2E 从岗位及用户管理页面创建任务自有数据、停用既有岗位、编辑资料验证保留、明确移除验证删除、验证停用岗位不可新增；从页面清理自有数据，只读核验最终结果。
- 测试运行记录精确选择器、退出码、计数、时间与源码指纹；历史测试不冒充本轮结果。全仓既有失败单列，不借此扩大范围。

## 设计约束检查

- 最近 AGENTS.md 优先：BDD/TDD非默认强制，仍必须对应回归和实际验收；引用文档中的全 dirty 提交/推送规则不覆盖当前任务授权及并行资产保护规则。
- 无 fallback、吞异常、模拟成功或兼容成功；缺依赖、权限、正式数据或服务准确阻塞。
- Windows PowerShell 禁用 &&；UTF-8，Maven -D 单个完整参数。只操作任务 worktree 和自有资产，运行前通过官方脚本预约槽位并核对 PID/端口，禁占 48081。
- reviewer 隔离上下文、只评审；worker 只修评审任务单，不自行放行；主 Agent 管理状态和最终证据。

## Current Status

completed

UM-04 实现20项源码/测试已通过全链静态审查、132后端回归、50前端回归、六文件ESLint、真实Playwright八场景及双存储验收；独立三层PASS。实现提交4d1a5041ea810ba7c6117b3a7b0b42d0e3e60ac2已标准快进合入int_main，主线50例复验通过，两个无关并行文件字节保持。cleanup两处preview/apply通过；自有数据已页面清理，runtime关闭、managed worktree实体/Git登记不存在、slot7 inactive及其余73条登记保持。只本地提交，不push或发布；全量类型及非目标MES工作台失败边界保留。最终九项记录单独提交，实际hash由Git log查询。

## Cleanup Keep

- doc/tasks/20261008-user-inactive-post-retention/task-state.json
- doc/tasks/20261008-user-inactive-post-retention/implementation-contract.md
- doc/tasks/20261008-user-inactive-post-retention/review-report.md
- doc/tasks/20261008-user-inactive-post-retention/review-decision.json
- doc/tasks/20261008-user-inactive-post-retention/e2e-summary.json
- doc/tasks/20261008-user-inactive-post-retention/verification-evidence.json
