# 培训规则页删除权限预检提示

## Task Goal

删除 DCC 文件类别管理“培训规则”页截图红框内的“发布前权限预检”提示区，不改变错误提示、培训规则列表、保存行为、权限数据源或培训任务只读映射页。

## Milestones

- [x] M1：定位截图对应组件和相邻静态契约。
- [ ] M2：先更新静态契约并取得预期 RED。
- [ ] M3：删除目标提示节点并取得 GREEN。
- [ ] M4：完成相邻回归、类型检查、技能证据验证和任务收尾。

## Expected Verification

- `node tests/e2e/dcc-training-ux-prechecks-static.spec.cjs` 先 RED 后 GREEN。
- `pnpm ts:check` 通过，或记录可证明与本任务无关的既有阻塞。
- `git diff --check -- <task-owned-paths>` 通过。
- `frontend-feature-delivery` evidence validator 通过。

## Current Status

in_progress

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；直接移除目标编辑页的展示节点，并用契约区分编辑页与只读页。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- 已读取 `docs/experience-index.md`。
- 适用门禁：`docs/frontend-development.md#前端静态契约隔离门禁`；现有合同必须明确区分目标编辑页和非目标只读页，避免宽泛删除提示能力。
- 截图按钮、填写配置红框及 eDHR 元信息门禁与本任务组件不匹配，不扩展适用范围。
