# Execution Log - 20260701-edhr-phase6-module-dedup (Frontend)

BDD: eDHR 主流程入口唯一 -> Given 用户处理一个 eDHR 批次 / When 从菜单或列表进入批次流程 / Then 主流程推进只围绕批次详情页，放行、审计、后台配置不再伪装成并列主流程。

BDD: 删除候选必须可证明安全 -> Given 一个 eDHR 页面疑似重复 / When 检查路由、菜单、调用、详情页入口与真实 E2E 职责 / Then 只有无生产职责且无有效入口的页面才能进入删除清单。

RED: phase6-dedup-matrix-missing -> FAIL，Phase 1-5 完成后尚未形成 eDHR 页面保留/下沉/合并/删除候选矩阵，无法判断哪些模块可删。
GREEN: task-bootstrap -> PASS，已在 `edhr_phase` 前端 worktree 建立 Phase 6 去重任务台账。

GREEN: route-scan -> PASS，已扫描 `src/router/modules/remaining.ts` 中 eDHR 相关路由并生成 `dedup-matrix.md`。
GREEN: dedup-matrix-draft -> PASS，已按 `保留 / 下沉 / 合并候选 / 删除高优先候选` 初步分类；当前未删除任何生产代码。
GREEN: main-list-dedup -> PASS，从批次列表主操作移除复盘/模板直达入口，避免绕开批次详情页形成并行主流程。
GREEN: template-admin-entry -> PASS，在批次详情页管理后台工作区补充批次模板后台入口，模板页下沉但不删除。
GREEN: simulate-delete-proof -> PASS，历史任务与页面引用证明模板模拟填写仍有演练/验证职责，本轮不做删除。
GREEN: pnpm-ts-check -> PASS，`NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_phase\yudao-ui-admin-vue3 ts:check` 通过；首次默认堆大小运行因 `JavaScript heap out of memory` 失败，扩大 Node 堆后通过。
GREEN: list-click-detail-e2e -> PASS，使用真实测试租户 `测试租户/aoteman`、Edge + Playwright 从 `eDHR批次执行` 列表点击首行 `详情`，进入 `detail?id=900000000463`，`/get` 与 `/workbench` 均 200，页面包含 `批次总控 / 阶段摘要 / 放行 / 审计 / 管理后台`，且列表首行操作已不再暴露 `复盘`、`模板` 直达入口。
GREEN: phase6-closeout -> PASS，本轮只移除主流程重复入口并文档化下沉/保留结论，未删除仍有职责证据的页面或后端接口。
BLOCKER: task-closeout-apply -> SKIPPED，`task-closeout-cleanup` preview 保留 `task.md / execution-log.md / dedup-matrix.md` 且无删除项，但因分支无法快进合并到 `int_main` 且脚本误判本轮生产改动为 unrelated pending change，本轮不执行 apply/合并/删除 worktree。
GREEN: detail-review-fusion-static -> PASS，`node tests/e2e/edhr-batch-detail-review-fusion-static.spec.js` 通过；旧复盘路由复用批次详情融合组件，独立复盘组件退役为跳转/兼容壳，避免两套页面逻辑并存。
