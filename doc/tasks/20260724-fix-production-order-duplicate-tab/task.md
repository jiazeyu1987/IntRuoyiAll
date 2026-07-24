# 修复生产工单重复页签

## Task Goal

生产工单页面在顶部页签中只保留一个 `生产工单` tab，不再出现 `生产工单 (2)` 这类重复页签。

## Milestones

- [x] 记录 BDD 场景并定位重复页签根因。
- [x] 先补充失败回归测试覆盖重复页签去重行为。
- [x] 实现最小前端修复。
- [x] 运行目标测试与相邻页签回归验证。
- [ ] 处理或确认全量 `pnpm ts:check` 既有阻塞后完成最终收尾。

## Expected Verification

- 目标静态/单元测试先失败后通过，证明重复 `生产工单` 页签会被复用为同一个 tab。
- 运行相关前端校验命令，确认没有引入新的类型或行为回归。

## Current Status

blocked

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是修复页签唯一性判定逻辑而不是隐藏重复标题。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- Trigger: 前端页面 / 表格 / 样式任务命中经验索引中的前端质量入口；本次未改页面样式，仅改动态路由元信息。
- Preflight check: 已读取 `docs/experience-index.md`、`docs/e2e-rules.md`、`docs/task-closeout-rules.md` 与 `docs/powershell-encoding.md`。
- Blocker: 高风险发布、服务器、数据库、真实写入 E2E 均不在本次范围；全量 `pnpm ts:check` 当前被既有 DCC 类型错误阻塞。
- Verification: 目标 `workorder-single-tags-view-static` 与相邻 DCC tagsView 静态契约通过。
- Forbidden action: 不通过隐藏 tab 文案、mock、默认成功、fallback、吞异常或修改无关 DCC 文件来绕过。
- Evidence: `doc/tasks/20260724-fix-production-order-duplicate-tab/verification-report.md`。
