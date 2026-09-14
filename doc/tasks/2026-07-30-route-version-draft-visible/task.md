# 工艺路线版本弹窗展示并删除进行中草稿

## Task Goal

在工艺路线版本弹窗中可以看到并删除正在进行的草稿版本，同时保留已生效历史版本和现有只读查看能力；草稿逻辑删除后，再次点击路线列表“编辑”必须基于当前 `ACTIVE` 版本创建新的 `DRAFT` 草稿。

## Milestones

1. `completed`：定位版本弹窗当前过滤逻辑、草稿数据状态和关联静态合同。
2. `completed`：补充 RED 静态合同，证明进行中 `DRAFT` 草稿应显示但当前被隐藏。
3. `completed`：实现最小前端修复，按正向允许集合展示 `DRAFT`、`ACTIVE`、`SUPERSEDED`。
4. `completed`：运行目标静态合同、相邻历史版本合同和类型检查。
5. `completed`：补充 DRAFT 草稿“删除草稿”确认、刷新隐藏、再次编辑基于当前 ACTIVE 新建草稿的前后端回归。
6. `completed`：运行目标静态合同、后端回归、类型检查和证据校验。
7. `completed`：使用真实 Playwright 页面路径验证复制测试路线、编辑创建草稿、版本弹窗删除草稿、再次编辑重建草稿，并完成任务自有数据清理。
8. `blocked`：提交收尾仍受并行脏改动、混合基线提交与远端分叉阻塞。

## Expected Verification

- 目标静态合同先 RED 后 GREEN，断言版本弹窗展示进行中 `DRAFT` 草稿。
- 版本工作区静态合同断言 `DRAFT` 行显示“删除草稿”、确认框文案正确、取消确认不调用后端、确认后调用取消接口并刷新版本弹窗和路线列表。
- 编辑入口静态合同断言 `CANCELLED` 不属于打开候选，且新草稿使用当前 `activeRouteVersionId` 作为来源版本。
- `MesProRouteVersionWorkflowServiceTest` 覆盖草稿取消为 `CANCELLED` 后重新创建 `DRAFT`，新版本来源为当前 active。
- 静态合同继续断言已取消、已驳回、审核中或其它非允许状态不得因为本次需求被展示。
- 相邻历史版本只读查看静态合同仍通过。
- 真实 Playwright 验证“复制任务自有路线 -> 点击编辑创建草稿 -> 打开版本弹窗 -> 删除草稿 -> 草稿行消失 -> 再次点击编辑 -> 进入基于当前 ACTIVE 的新草稿”，并通过页面清理任务自有路线。

## Current Status

ready_for_closeout

## Closeout Blocker

- 真实 E2E 已完成：`node doc/tasks/2026-07-30-route-version-draft-visible/route-draft-delete-recreate-real.e2e.cjs` 使用本机系统 Chrome `C:\Program Files\Google\Chrome\Application\chrome.exe` 执行，因原配置的 Playwright Chromium 缓存路径仍缺少 `E:\Int\DevCache\playwright-browsers\chromium_headless_shell-1223\chrome-headless-shell-win64\chrome-headless-shell.exe`。
- 独立提交阻塞：并行基线提交 `67282a868c449ee0ea652491cfd45dc448b258e9` 已把本任务实现、测试和任务文档与多项非本任务改动混在同一个“基线”提交中；不得擅自 amend、reset 或重写共享分支历史。
- 提交/推送阻塞：最新快照为 `int_main...origin/int_main [ahead 7]`，工作区仍存在非本任务改动 `doc/tasks/20260730-route-admin-list-layout-unification/*`、`doc/tasks/20260730-standard-template-list-search-alias/*`、`docs/e2e-rules.md`、`docs/experience-index.md`；本任务不得把这些并行改动混入提交。
- 提交保留注意：任务自有 E2E 脚本命中 `.gitignore:99 doc/tasks/**/*.cjs`，后续若要提交本任务收尾记录，必须按 closeout 门禁显式 `git add -f doc/tasks/2026-07-30-route-version-draft-visible/route-draft-delete-recreate-real.e2e.cjs`。

## Cleanup Keep

doc/tasks/2026-07-30-route-version-draft-visible/route-draft-delete-recreate-real.e2e.cjs

## 经验门禁

- 命中 `docs/frontend-development.md#前端列表状态口径完整性门禁`：本次必须按用户新口径重建正向允许集合，不能只删除旧过滤或只排除截图中的单一状态。
- 关联历史任务 `doc/tasks/20260727-route-version-list-active-history-only/`：旧需求曾要求版本列表隐藏 `DRAFT`；本次用户明确要求反向展示“正在进行的草稿”，因此需要同步更新测试和展示口径。
- 已执行 `project-experience-consolidation` 判断：本次没有新增长期经验文档；既有“前端列表状态口径完整性门禁”已覆盖正向允许集合建模，本次属于业务口径反向变更，不应把“草稿可见”沉淀为长期固定业务规则。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；计划从版本列表状态集合根因调整展示口径，并用静态合同覆盖允许/禁止状态。
- `是否存在临时补丁或绕过`：否。
