# 生产组长活跃订单路线名称与版本展示

## Task Goal

调整生产组长“活跃订单池”表格：删除“状态”列；将“路线ID”改为“路线名称”并显示正式路线名称；将“路线版本ID”改为“版本号”并显示正式版本数字，不向用户展示这两个内部 ID。

## Milestones

- [x] M1：确认页面入口、共享组件边界和现有活跃订单接口字段
- [x] M2：以 BDD 和失败测试锁定列名、数据字段及禁止显示 ID 的契约
- [ ] M3：补齐正式接口显示字段并实现前端最小改动
- [ ] M4：完成聚焦回归、类型/后端测试和真实页面视觉复核
- [ ] M5：完成证据校验、经验检查与 cleanup

## Expected Verification

- 静态合同先证明旧页面仍显示“路线ID”“路线版本ID”“状态”并缺少正式显示字段，形成 RED。
- 活跃订单列表接口正式返回 `routeName` 和 `routeVersionNo`，不得由前端从 ID 猜测、拼接或降级显示 ID。
- 页面表头显示“路线名称”“版本号”，不显示“路线ID”“路线版本ID”“状态”。
- 现有 5 条活跃订单在真实生产组长页面显示路线名称和纯版本号，且不显示 `980091`、`622` 这两个路线内部 ID。
- 聚焦前后端测试、前端类型检查、真实 Playwright 页面验证和 `frontend-feature-evidence.md` validator 通过。
- task-closeout-cleanup preview/apply 通过；依据当前项目 `AGENTS.md`，本任务默认不执行 Git 暂存、提交或推送。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；接口缺正式路线名称或版本号时不回退显示 ID。
- `是否从根因和长期维护角度解决`：是；由活跃订单正式读接口提供展示字段，页面只消费明确契约。
- `是否存在临时补丁或绕过`：否；不使用前端硬编码、额外 N+1 查询或 ID 文案替代名称。

## Experience Gate

- `docs/experience-index.md` 存在并已读取。
- 适用 `docs/frontend-development.md` 的共享组件边界规则：改动限定在生产组长活跃订单模块，不改变其它生产/PQC 模块的列定义和行为。
- 适用 `docs/e2e-rules.md` 的真实页面门禁：最终通过本机真实生产组长页面验证，不使用 API-only 代替页面结果。

## Current Status

in_progress - 前端静态合同因缺 `routeName/routeVersionNo` 按预期失败；后端聚焦测试因 `MesTeamLeaderActiveOrderRow` 尚未实现按预期编译失败。正在实现正式批量读模型和目标表格列调整。
