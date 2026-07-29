# 生产一线报工工序池实现任务

## Task Goal

启动 6 个子 agent，分别在 6 个 `D:\IntRuoyiWorktree\` worktree 中实现并验证 `docs/acceptance/production-line-process-pool/` 下已放行文档对应的 6 个功能点，主线程按 21 条需求门禁 review，全部通过后融合回 `int_main` 并推送。

## Milestones

- [x] 创建实现任务目录并读取触发规则。
- [x] 建立监督式交付任务文档和状态文件。
- [x] 创建 6 个受控 worktree。
- [x] 启动 6 个子 agent 分别实施 F1、F2、F3、F4、F7、F8。
- [ ] 主线程 review 每个 worktree 的实现、测试和证据。
- [ ] 对未通过项退回修复并复验。
- [ ] 将通过的 6 个 worktree 逐个融合进 `int_main`。
- [ ] 在 `int_main` 运行系统级验证。
- [ ] 提交、推送并完成 closeout。

## Expected Verification

- 后端：`mvn -pl yudao-module-mes -am` 定向 JUnit 按 `docs/acceptance/production-line-process-pool/tdd-plan.md` 通过。
- 前端：`pnpm --dir IntRuoyiFronted ts:check` 和任务专用静态/E2E 测试通过或记录正式阻塞。
- 数据库：新增/调整 schema 有 SQL 契约测试、迁移门禁和字段来源证据。
- E2E：真实报工入口、设备账号内切换员工、固定模板提交、PQC 提交、工序池时间轴按计划验证；缺少测试租户/账号/签名/数据时必须记录 blocker，不能用 mock 或 API-only 替代。
- Git/worktree：所有实现分支均可追溯，融合后 `scripts\preflight\branch-runtime-port-guard.ps1` 通过，最终 `git push origin int_main` 后本地不 ahead。

## Applicable Gates

- Worktree 只能位于 `D:\IntRuoyiWorktree\`，目标路径必须先解析并确认是该根目录子路径。
- PowerShell 不使用 `&&`，中文文档按 UTF-8 读写。
- 不新增 fallback、默认模板、默认员工、默认路线、默认成功、静默降级或吞异常。
- 批记录表单、表单槽位 `formBindings`、工序开始配置三条链路不得混用。
- 工序池必须是新正式模型，不能用 `mes_pro_feedback_surplus_pool` 替代。
- 生产工单 FIFO 只按生产工单计划开始时间排序；缺少计划开始时间时阻塞。
- 一线原始提交不按上下限拦截；审核副本上下限修正不是本轮实现的替代逻辑。

## Current Status

ready_for_execution

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是新增正式工序池事件、组合提交、固定模板、设备账号切换、FIFO 和时间轴链路。
- `是否存在临时补丁或绕过`：否。
