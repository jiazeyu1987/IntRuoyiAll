# 生产组长功能模块 Tab 改造

## Task Goal

将“生产组长”页面中不同功能模块按独立 Tab 展示，至少覆盖人员管理、报工管理、损耗管理等模块，避免多个模块在同一视图中混杂。

## Milestones

- [ ] M1: 定位生产组长页面入口、组件、路由和现有模块结构。
- [ ] M2: 编写静态合同 RED，证明当前页面未按功能模块 Tab 分组。
- [ ] M3: 最小化实现功能模块 Tab，保持现有模块逻辑和接口契约不变。
- [ ] M4: 运行静态合同、类型检查或相邻前端验证，并记录 GREEN/回归证据。
- [ ] M5: 更新任务证据、验证报告和收尾状态。

## Expected Verification

- 任务专用静态合同先 RED 后 GREEN。
- 受影响前端检查通过，优先运行目标静态合同；如触及 TypeScript 编译链路，再运行 `pnpm ts:check`。
- 不新增 mock、fallback、吞异常或后端契约变更。

## Current Status

in_progress

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按页面功能模块建立正式 Tab 组织方式。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- 待检查 `docs/experience-index.md` 后补充适用经验门禁。
