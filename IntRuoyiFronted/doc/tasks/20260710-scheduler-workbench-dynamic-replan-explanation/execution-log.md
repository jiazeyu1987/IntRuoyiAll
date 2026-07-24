# Execution Log

## BDD

BDD: 用户进入排产逻辑查看最近重排 -> Given 当前租户已有成功重排 / When 用户进入排产逻辑页签 / Then 页面显示重排时间、来源、操作人、原因和七步实际数值。

BDD: 用户查看完整物料计算 -> Given 本次重排包含充足和短缺物料 / When 用户查看问题步骤 / Then 页面展示每种物料需求、库存、短缺并可展开订单贡献。

BDD: 用户查看工序和产能 -> Given 本次重排包含多道工序 / When 用户查看拆分工序和计算产能步骤 / Then 页面展示班次、工作站、设备、人员、产能、时长和瓶颈。

BDD: 页签激活和窗口聚焦刷新 -> Given 页面已打开 / When 用户切入排产逻辑或窗口重新获得焦点 / Then 页面重新获取最新成功重排且不启动定时轮询。

BDD: 没有成功重排 -> Given 当前租户没有成功重排快照 / When 用户进入页签 / Then 页面显示“暂无已应用的重排记录”且不生成模拟数据。

BDD: 查询失败明确可见 -> Given 查询接口失败 / When 用户进入页签 / Then 页面显示加载失败且不把旧数据标记为最新。

## TDD

- RED: `node tests/e2e/mes-scheduler-workbench-dynamic-replan-explanation-static.spec.js` -> FAIL，前端 API 缺少 `getLatestReplanExplanation`，页面仍为静态说明。
- GREEN: `node tests/e2e/mes-scheduler-workbench-algorithm-guide-tab-static.spec.js` -> PASS，七步通俗说明结构通过。
- GREEN: `node tests/e2e/mes-scheduler-workbench-dynamic-replan-explanation-static.spec.js` -> PASS，动态接口、刷新、空态和错误态契约通过。
- GREEN: `node tests/e2e/mes-pro-schedule-order-apply-replan-toast-static.spec.js` -> PASS，原有重排应用成功提示行为未回归。
- RED: `pnpm ts:check` -> FAIL，Node 默认 4GB 堆内存耗尽，未发现类型错误输出。
- GREEN: `NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check` -> PASS，Vue 和 TypeScript 类型检查通过。
- GREEN: experience-preflight -> PASS，已读取 `docs/powershell-memory.md`、`docs/login-access.md`、`docs/server-access.md` 和 Playwright 执行规范；真实写入验证限定本机测试租户 `tenant_id=122`、账号 `aoteman`，禁止访问远端环境和芋道源码租户写入。
- GREEN: real-e2e-first-display -> PASS，测试租户通过前端应用工单 `TESTERP62AF41D87EFA` 重排后进入“排产逻辑”，显示人工重排时间 `2026-07-10 12:54`、七个步骤、订单和实际数值。
- GREEN: real-e2e-material-contribution -> PASS，展开物料“弯曲连接件”，显示排产订单 `SCH-TESTERP62AF41D87EFA-20260702-0001`、生产工单 `TESTERP62AF41D87EFA`、该订单需要 `685`。
- GREEN: real-e2e-capacity-detail -> PASS，展开工单产能后显示瓶颈工序“外管拉伸2”、白班、工作站“外管拉伸2-工位”、设备 1、人员 0/0、班次产能 25.714286/时、预计时长 26 小时 39 分及实际开始结束时间。
- GREEN: real-e2e-second-refresh -> PASS，第二次真实重排应用后切换进入“排产逻辑”，页面更新时间由 `2026-07-10 12:54` 更新为 `2026-07-10 12:59`；浏览器控制台 error 级别消息为 0。
- GREEN: post-rebase-frontend-verification -> PASS，分支变基到当前 `int_main` 后，三个静态回归测试、`NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check` 和 `git diff int_main...HEAD --check` 通过。
- GREEN: task-closeout-cleanup-apply-off -> PASS，已清理 `.playwright-cli/`、`output/e2e-runtime/`、`output/playwright/` 和临时前端证据文件，保留正式任务记录。
- BLOCKER: frontend-worktree-merge -> 前端主工作区存在其他任务未提交改动，且 `docs/request-command-log.md` 与本任务存在文件级重叠；按混合工作区门禁停止融合，未操作他人改动。
- GREEN: frontend-worktree-merge-resolved -> PASS，用户明确要求融合；任务分支再次变基到最新 `int_main`，仅暂存重叠的请求日志后执行 `git merge --ff-only`，恢复日志冲突时同时保留本任务记录和原有 eDHR 记录。
- GREEN: post-merge-targeted-verification -> PASS，主工作区运行排产逻辑页签、动态说明和重排成功提示三个目标静态测试全部通过。
- REGRESSION BLOCKER: post-merge-main-ts-check -> 主工作区无关未提交文件 `src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue` 缺少 `resolveFormSlotTypeLabel`，导致完整 TypeScript 检查失败；本任务干净分支上的相同 TypeScript 检查已通过。
- GREEN: frontend-worktree-delete -> PASS，Git worktree 注册已删除；残留 `node_modules` 长路径目录完成路径校验后删除，临时 worktree 根目录不存在。

## Current Status

completed
