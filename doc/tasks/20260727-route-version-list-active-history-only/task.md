# 工艺路线版本列表隐藏取消版本

## Task Goal

版本列表中只显示草稿和已生效/已替代的有效历史版本，隐藏已取消版本，避免用户在版本列表中看到 `已取消` 历史候选版本。

## Milestones

1. `completed`：定位版本列表渲染与数据过滤边界。
2. `completed`：补充 RED 静态合同，复现 `CANCELLED` 版本仍显示的问题。
3. `completed`：实现最小前端过滤，不改变后端只读快照读取能力。
4. `completed`：运行目标静态合同、既有深链合同和类型检查。
5. `in_progress`：记录证据、提交并推送任务分支。

## Expected Verification

- 静态合同证明版本列表过滤 `CANCELLED` 版本。
- 静态合同证明 `DRAFT` 当前草稿、`ACTIVE` 已生效版本和 `SUPERSEDED` 已替代历史版本仍可显示。
- 既有“已取消历史版本只读查看”静态合同仍通过，保证后端/深链查看能力不被误删。

## Current Status

ready_for_closeout

## 经验门禁

- 命中 `docs/frontend-development.md#前端静态契约隔离门禁`：当前是窄范围列表展示缺陷，使用任务专用静态合同做 RED/GREEN。
- 命中 `docs/e2e-rules.md#静态合同与真实-e2e-同步门禁`：修改静态合同需确认真实页面路径仍与当前版本工作区行为一致。
- 命中 `docs/backend-development.md#历史关闭候选版本只读快照边界`：列表隐藏已取消版本不得删除后端按 `routeVersionId` 读取冻结快照的能力，写入仍只允许 `DRAFT`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；在列表展示层明确过滤不应展示的取消候选版本，并保留深链只读读取契约。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

- doc/tasks/20260727-route-version-list-active-history-only/bug-regression-evidence.md
- doc/tasks/20260727-route-version-list-active-history-only/frontend-feature-evidence.md
