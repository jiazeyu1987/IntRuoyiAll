# 生产组长功能模块 Tab 改造

## Task Goal

将“生产组长”页面中不同功能模块按独立 Tab 展示，至少覆盖人员管理、报工管理、损耗管理等模块，避免多个模块在同一视图中混杂。

## Milestones

- [x] M1: 定位生产组长页面入口、组件、路由和现有模块结构。
- [x] M2: 编写静态合同 RED，证明当前页面未按功能模块 Tab 分组。
- [x] M3: 最小化实现功能模块 Tab，保持现有模块逻辑和接口契约不变。
- [x] M4: 运行静态合同、类型检查或相邻前端验证，并记录 GREEN/回归证据。
- [ ] M5: 更新任务证据、验证报告和收尾状态。

## Expected Verification

- 任务专用静态合同先 RED 后 GREEN。
- 受影响前端检查通过，优先运行目标静态合同；如触及 TypeScript 编译链路，再运行 `pnpm ts:check`。
- 不新增 mock、fallback、吞异常或后端契约变更。

## Current Status

completed

实现、定向验证、经验沉淀和 cleanup apply 已完成；待选择性提交并推送当前任务提交。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按页面功能模块建立正式 Tab 组织方式。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- 已读取 `docs/experience-index.md`。
- 适用 `docs/frontend-development.md#前端静态契约隔离门禁`：本任务使用任务专用静态合同覆盖当前生产组长功能 Tab，不用全量旧合同失败冒充当前行为。
- 适用 `docs/task-closeout-rules.md#技能证据文件清理前归档门禁`：`frontend-feature-evidence.md` validator PASS 后，关键 PASS 结论复制到 `execution-log.md` 和 `verification-report.md`。
