# 工艺路线版本弹窗展示进行中草稿

## Task Goal

在工艺路线版本弹窗中可以看到正在进行的草稿版本，同时保留已生效历史版本和现有只读查看能力，不把已取消、已驳回等不应展示的版本重新放回列表。

## Milestones

1. `completed`：定位版本弹窗当前过滤逻辑、草稿数据状态和关联静态合同。
2. `completed`：补充 RED 静态合同，证明进行中 `DRAFT` 草稿应显示但当前被隐藏。
3. `completed`：实现最小前端修复，按正向允许集合展示 `DRAFT`、`ACTIVE`、`SUPERSEDED`。
4. `completed`：运行目标静态合同、相邻历史版本合同和类型检查。
5. `in_progress`：补充 DRAFT 草稿“删除草稿”确认、刷新隐藏、再次编辑基于当前 ACTIVE 新建草稿的前后端回归。
6. `pending`：运行目标静态合同、后端回归、类型检查和证据校验。
7. `pending`：真实 Playwright E2E 和提交收尾若仍受本机浏览器依赖、并行脏改动与远端分叉阻塞，则记录阻塞。

## Expected Verification

- 目标静态合同先 RED 后 GREEN，断言版本弹窗展示进行中 `DRAFT` 草稿。
- 静态合同继续断言已取消、已驳回、审核中或其它非允许状态不得因为本次需求被展示。
- 相邻历史版本只读查看静态合同仍通过。
- 如本地运行态前置完整，再用真实 Playwright 只读路径验证弹窗可见草稿且无 MES 写请求。

## Current Status

in_progress

## Closeout Blocker

- 真实 E2E 阻塞：`node tests/e2e/mes-route-version-list-draft-visible-real.e2e.js` 在启动浏览器前失败，缺少 `E:\Int\DevCache\playwright-browsers\chromium_headless_shell-1223\chrome-headless-shell-win64\chrome-headless-shell.exe`。
- 提交/推送阻塞：当前 `int_main...origin/int_main [ahead 15, behind 8]`，且工作区存在多项非本任务改动，包括调度工作台后端文件、其它任务文档、`docs/experience-index.md` 和 `docs/frontend-development.md`；本任务不得把这些并行改动混入提交。

## 经验门禁

- 命中 `docs/frontend-development.md#前端列表状态口径完整性门禁`：本次必须按用户新口径重建正向允许集合，不能只删除旧过滤或只排除截图中的单一状态。
- 关联历史任务 `doc/tasks/20260727-route-version-list-active-history-only/`：旧需求曾要求版本列表隐藏 `DRAFT`；本次用户明确要求反向展示“正在进行的草稿”，因此需要同步更新测试和展示口径。
- 已执行 `project-experience-consolidation` 判断：本次没有新增长期经验文档；既有“前端列表状态口径完整性门禁”已覆盖正向允许集合建模，本次属于业务口径反向变更，不应把“草稿可见”沉淀为长期固定业务规则。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；计划从版本列表状态集合根因调整展示口径，并用静态合同覆盖允许/禁止状态。
- `是否存在临时补丁或绕过`：否。
