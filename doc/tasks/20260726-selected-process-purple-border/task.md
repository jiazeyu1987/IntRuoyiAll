# 20260726 Selected Process Purple Border

## Task Goal

将工艺路线图中被选中的工序节点边框改为紫色，未选中节点保持现有绿色边框样式。

## Milestones

- [x] 定位工序节点选中态样式和现有静态契约
- [x] 先补充 RED 静态契约覆盖“选中工序紫色边框”
- [x] 实现最小样式修复
- [x] 运行相关前端验证并记录结果

## Expected Verification

- 相关静态契约先因缺少紫色选中边框断言失败，再在实现后通过。
- 受影响前端测试通过。

## Current Status

ready_for_closeout

## Remaining Closeout Blocker

- Git closeout is blocked by extensive pre-existing unrelated dirty changes in the workspace and mixed staged/unstaged state on shared files. No commit or push was attempted to avoid mixing unrelated task ownership.

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是，直接修正选中态样式契约和实现。
- `是否存在临时补丁或绕过`：否

## 经验门禁

- 已读取 `docs/experience-index.md`。
- 命中 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：默认 active 色为蓝色，但本次用户明确要求选中工序使用紫色，按用户要求做窄范围覆盖。
- 命中 `docs/frontend-development.md#前端静态契约隔离门禁`：本次使用聚焦静态契约完成 RED/GREEN，不扩大到无关大契约。
- 命中 `docs/e2e-rules.md#静态合同与真实-e2e-同步门禁`：修改 `tests/e2e/*static.spec.js` 后重跑同一静态契约。

## Cleanup Keep

- doc/tasks/20260726-selected-process-purple-border/frontend-feature-evidence.md
