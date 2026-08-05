# 生产组长生产人员档案管理

## Task Goal

在独立 worktree 中按 BDD + strict TDD 实现生产组长的生产人员档案管理：只管理与当前生产组长关联的员工，支持正式工搜索选择与临时工手动录入，生产填写选择只显示当前组长关联且未禁用员工，并对新增、禁用、启用、修改显示名、重置临时工签名密码、关联正式工等操作完整留痕。

## Milestones

- [x] M0：创建任务文档、读取项目规则和技能合同，完成 BDD/TDD 设计。
- [x] M1：数据库 schema 与迁移测试，覆盖人员档案、组长关联、唯一显示名、审计日志。
- [x] M2：后端 API 与服务 TDD，覆盖正式工搜索、临时工新增、启停用、改名、密码重置、生产填写候选。
- [x] M3：前端标准列表页签与生产填写员工卡片集成 TDD。
- [x] M4：定向后端、前端、迁移和 evidence validator 验证。
- [x] M5：真实 Playwright E2E 验收，使用 worktree slot 1 的 `8082/48082` 成对运行态通过。
- [ ] M6：收尾、经验沉淀、cleanup、提交并推送当前分支；cleanup 已完成，等待提交/推送，主 worktree 合并与 worktree 删除因外部状态阻塞。

## Expected Verification

- 数据库：schema/migration 静态或单测 RED/GREEN，迁移策略门禁通过。
- 后端：目标 JUnit 覆盖成功、重复名、越权、跨租户、正式工密码不可重置、临时工密码哈希与审计。
- 前端：任务专用静态合同覆盖 `UnifiedListTemplate`、安全搜索下拉、临时工密码表单、审计入口、员工卡片候选接口。
- E2E：Playwright 真实页面路径覆盖新增临时工、选择正式工、重名拦截、禁用后不进新报工选择、历史快照保留、审计记录可见。
- 收尾：`git diff --check`、技能 evidence validator、branch runtime guard、提交和 `git push origin codex/20260805-production-personnel-management`。

## Current Status

ready_for_closeout

实现、后端定向测试、前端静态合同、前端类型检查、脚本语法检查、真实 Playwright E2E、`git diff --check`、四个 evidence validator、经验沉淀和 task-closeout cleanup 已完成。当前仅剩 cleanup 记录提交、分支推送；自动合并/删除 worktree 因主 worktree 外部状态阻塞。

当前 closeout blocker：`E:\IntRuoyi` 主 worktree dirty，且当前分支不能 fast-forward merge 到 `int_main`；未修改主 worktree，未删除当前 worktree。

## Applicable Gates

- BDD/TDD：所有行为变更先写 Given/When/Then，再执行 RED -> GREEN -> REGRESSION。
- Worktree 槽位：附加 worktree 必须使用 `reserve-worktree-slot.ps1` 分配 `slot 1..19`，无槽位时真实 E2E 阻塞。
- PowerShell：不得使用 `&&`；中文文档使用 UTF-8 安全读写。
- 标准列表：员工管理列表必须使用 `UnifiedListTemplate`，不能暴露全系统用户列表。
- Element Plus 下拉：正式工新增通过输入下拉远程搜索，必须等待并选择真实可见选项，不能用数组下标或隐藏值。
- 无 fallback：不得创建临时系统账号、不得返回全量用户再前端过滤、不得用空候选/默认成功掩盖权限或数据缺口。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，建立正式生产人员档案、组长关联、唯一约束和审计链路。
- `是否存在临时补丁或绕过`：否。
