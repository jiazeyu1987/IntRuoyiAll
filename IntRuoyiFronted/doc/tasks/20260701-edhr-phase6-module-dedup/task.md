# 任务：eDHR Phase 6 模块去重与后台下沉清理（前端）

- Task ID: `20260701-edhr-phase6-module-dedup`
- Workspace: `D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_phase\yudao-ui-admin-vue3`
- Created: `2026-07-01`
- Current Status: `completed`

## Task Goal

在 Phase 1-5 已把批次详情页确立为唯一主流程页之后，审计当前 eDHR 前端模块是否仍存在主流程重复入口，形成可执行的“保留 / 下沉 / 合并 / 删除候选”矩阵；只删除已经被证据证明无路由、无调用、无业务职责的页面或入口。

## Previous Task Check

- 上一个前端任务：`20260701-edhr-phase5-admin-downscoping`
- 状态：`completed`
- 处理说明：Phase 5 已完成管理后台工作区下沉与补充列表点击详情 E2E；本轮只做 Phase 6 去重，不回滚 Phase 1-5 成果。

## 经验门禁

- 命中 `docs/powershell-memory.md`：PowerShell 使用显式 UTF-8，避免中文路径/文案污染。
- 命中 `docs/worktree-memory.md`：继续限定在 `edhr_phase` worktree 内执行，不回主工作区改动。
- 命中 `simplify-codebase`：先证明冗余与删除安全，再删除；不为历史兼容保留无证据路径，也不做无证据删除。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。以“批次详情页主流程、其他页面后台/专业化”为长期结构，先做菜单和页面职责归位。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: eDHR 主流程入口唯一 -> Given 用户处理一个 eDHR 批次 / When 从菜单或列表进入批次流程 / Then 主流程推进只围绕批次详情页，放行、审计、后台配置不再伪装成并列主流程。`
- `BDD: 删除候选必须可证明安全 -> Given 一个 eDHR 页面疑似重复 / When 检查路由、菜单、调用、详情页入口与真实 E2E 职责 / Then 只有无生产职责且无有效入口的页面才能进入删除清单。`

## Milestones

1. M1：建立 Phase 6 去重任务台账。`completed`
2. M2：扫描 eDHR 路由、页面、入口与调用关系。`completed`
3. M3：形成保留/下沉/合并/删除候选矩阵。`completed`
4. M4：对安全候选执行最小清理并验证。`completed`

## Expected Verification

- `pnpm --dir D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_phase\yudao-ui-admin-vue3 ts:check`
- 真实 E2E：登录测试租户后从 `eDHR批次执行` 列表点击 `详情`，详情页仍可见 `批次总控 / 阶段摘要 / 放行 / 审计 / 管理后台`。

## Current Blockers

- 暂无。当前无已证明可安全删除的页面或后端接口；本轮完成的是主流程重复入口收口与后台下沉。

## Final Verification

- `NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_phase\yudao-ui-admin-vue3 ts:check`：PASS。
- 真实 E2E：`测试租户/aoteman` 登录后从 `eDHR批次执行` 列表点击首行 `详情`，进入批次详情页，`/get` 与 `/workbench` 均返回 200，页面包含 `批次总控 / 阶段摘要 / 放行 / 审计 / 管理后台`，且列表操作仅保留 `详情 / 流程追踪 / 操作轨迹 / UX检查 / 预检 / 查看归档 / 下载打印版PDF`。
- 删除结论：`eDHR模板模拟填写` 有历史任务和页面引用证据，本轮不得删除；`复盘`、`批次模板` 隐藏路由保留，主入口统一下沉到批次详情页。
- 路由收口验证：`node tests/e2e/edhr-batch-detail-review-fusion-static.spec.js` PASS，旧复盘路由已复用批次详情融合组件，独立复盘组件仅保留跳转/兼容职责。

## Cleanup Keep

- `doc/tasks/20260701-edhr-phase6-module-dedup/dedup-matrix.md`
