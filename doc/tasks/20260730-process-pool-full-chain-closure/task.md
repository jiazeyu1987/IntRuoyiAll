# 生产一线报工与资源池全链路补齐

## Task Goal
补齐“绑定数据源 -> 一线页面真实提交 -> PQC 入池 -> FIFO 编排 -> 审核副本自动规则 -> 班组长工作台 -> 完整 E2E”的正式链路，入口复用当前生产报工页面，一线点击提交后同时产生报工、记录本和资源池事件。

## Milestones
- M1: 建立隔离 worktree、任务文档、BDD/TDD 验收口径。（completed）
- M2: 补齐绑定数据源的生产实现，设备账号只能看到绑定路线/工序/员工/模板。（completed）
- M3: 补齐一线生产和 PQC 页面真实提交，禁止 validate 后假成功。（completed）
- M4: 补齐 PQC 入池、FIFO 按生产工单计划开始时间消费、审核副本自动规则。（completed）
- M5: 补齐班组长工作台和完整真实 E2E。（completed）
- M6: 完成验证、经验沉淀、提交、推送和 worktree 收尾。（blocked）

## Expected Verification
- 后端目标 JUnit 覆盖绑定源、提交授权、PQC 入池、FIFO 生产工单排序、审核副本 clamp 规则。
- 前端静态合同覆盖生产/PQC 页面调用 `frontlineSubmit`、禁止模拟成功、班组长工作台入口和 API。
- Playwright 真实路径 E2E 覆盖一线报工提交、记录本写入、PQC 入池、FIFO 分配、审核副本生成、班组长查看。
- 分支运行端口守卫通过，worktree slot 使用 `int_main slot=1`：前端 `8082`，后端 `48082`。
- 本任务提交并推送当前分支，若无法完成真实 E2E，记录精确前置阻塞。

## Current Status
blocked

## Worktree
- Path: `D:\IntRuoyiWorktree\process-pool-full-chain-closure`
- Branch: `codex/process-pool-full-chain-closure-20260730`
- Runtime profile: `int_main`
- Slot: `1`
- Frontend port: `8082`
- Backend port: `48082`

## Applicable Experience Gates
- Worktree 端口段与原子槽位门禁：已用 `reserve-worktree-slot.ps1` 登记 slot，启动服务前继续核对端口归属。
- E2E 脚本入口存在性门禁：完整 E2E 不允许被静态合同、API-only 或 wrapper 存在替代。
- Worktree / int_main 运行态 URL 门禁：真实 E2E 必须使用同一 slot 的 `8082/48082` 成对 URL。
- Worktree 前端依赖启动门禁：运行前端脚本前检查 worktree 自身依赖，不复制主工作区 `node_modules`。
- 跨分支运行时契约复验门禁：新增生产实现必须有真实 Spring 注入和最小回归，不允许只存在 mock 或测试替身。
- 自动首刷与异步详情精确等待门禁：班组长工作台查询响应按 `submitDate + employeeUserId` 精确匹配，详情抽屉按事件 ID 等待 API 和可见内容。
- E2E 持久化标记与 DECIMAL 数值语义门禁：记录本内嵌 marker 使用包含匹配，FIFO DECIMAL 摘要验证数值语义并要求 API/UI 一致。

## 设计约束检查
- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，补正式数据源、正式提交和正式审核/FIFO链路。
- 是否存在临时补丁或绕过：否。

## Final Verification Summary
- AC-01 至 AC-06：后端目标 JUnit、前端静态合同、TypeScript、迁移策略门禁均通过。
- AC-07：真实 Playwright 全链路 PASS，marker `PPFC-1785436288416-51980`，证据位于 `IntRuoyiFronted/output/playwright/process-pool-full-chain-real-flow/PPFC-1785436288416-51980/`。
- 真实链路结果：生产 `feedbackId=783 / recordbookEntryId=10 / eventId=16`，PQC `feedbackId=784 / recordbookEntryId=11 / eventId=17`，FIFO 先满足工单 `925936=20` 再满足 `925937=30`，审核副本 `reviewCopyId=7` 将 `50` clamp 为 `40`。
- 实现提交：`79aaecd0 feat: close process pool frontline full chain` 已在本地分支生成，任务自有运行态已停止，`8082/48082` 当前无监听。
- 收尾阻塞：`git push -u origin codex/process-pool-full-chain-closure-20260730` 于 2026-07-31 仍因 GitHub HTTPS 连接重置失败；主工作区 `E:\IntRuoyi` 当前无脏文件但 `int_main...origin/int_main [ahead 5]`，远端未同步前不得自动快进融合、cleanup apply、删除 worktree 或标记 completed。
