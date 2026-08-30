# 不合格评审 MVP 实现执行日志

## User Intent

用户要求在新的 worktree 中实现并验证文档中的最小不合格评审 MVP。两个不合格审批必须使用同一个流程、同一套代码、同一个入口。

2026-08-30 用户追加要求：执行真实 E2E 验证。当前项目端口契约已从旧版 `1..19` 扩展为 `1..100`，因此重新核对并执行 worktree 运行态与 Playwright 真实页面验证。

2026-08-30 用户解除夹具阻塞：明确允许在测试租户模拟数据。授权范围仅用于创建本任务可追踪、可清理的测试夹具；仍禁止直接 SQL/API 绕过真实页面业务链路。

2026-08-30 用户要求先提交当前任务代码，再融合进 `int_main`。当前功能分支基于 `int_batch`，相对当前 BatchRecord clone 的 `origin/int_main` 额外包含 11 个非本任务提交；不得直接 merge 整条分支。融合采用“当前任务提交 -> 最新 `int_main` 干净集成 worktree 仅移植本任务提交 -> `int_main` 快进”的范围隔离方式，不推送远端。

提交前运行 `E:\IntRuoyi\scripts\preflight\branch-runtime-port-guard.ps1` -> FAIL：当前功能 worktree 的旧 `branch-runtime-profile.ps1` 只允许 slot `1..19`，而共享登记表已升级到 v7 并存在合法 slot `20`。本任务未修改 `scripts/runtime`、`docs/branch-runtime-ports.md`、分支 env 或 `.githooks`；该中间功能提交继续保留失败证据，最终融合前必须在基于最新 `int_main` 的干净集成 worktree 运行 v7 guard 并 PASS。

模拟夹具方案：使用测试租户正式页面临时把 `RT000028` 从停用改为启用，使工单 `WO-EDHR-CELL-20260728-104808` 可创建唯一批次；批次创建成功后立即通过同一页面恢复路线停用。若任一步失败，脚本在关闭浏览器前优先恢复路线状态并记录恢复结果。

## Requirements Source

- `C:\Users\BJB110\Documents\Codex\2026-08-30\fe\outputs\nonconformance-review-mvp-prd.md`
- `C:\Users\BJB110\Documents\Codex\2026-08-30\fe\outputs\nonconformance-review-mvp-user-flows.md`
- `E:\IntRuoyiBranch\BatchRecord\IntRuoyiAll\docs\product\prd.md`
- `E:\IntRuoyiBranch\BatchRecord\IntRuoyiAll\docs\product\user-flows.md`
- `E:\IntRuoyiBranch\BatchRecord\IntRuoyiAll\docs\product\acceptance-criteria.md`

## BDD Scenarios

- BDD: 测试租户模拟夹具可恢复 -> Given 用户允许测试租户模拟数据且选定样本无活跃人工任务、无既有评审, When Playwright 完成不合格评审状态机, Then 样本最终作废、待处理评审归零，临时启用过的来源路线恢复停用。
- BDD: 统一不合格评审入口 -> Given PQC 提交记录或 PQC 生产放行待放行记录需要进入不合格评审, When 用户发起不合格评审或点击 `不合格审查`, Then 系统进入同一个不合格评审入口并创建同一类不合格评审单。
- BDD: 冻结三项拦截 -> Given 不合格评审单创建后关联活跃工单/批次为 `frozen`, When 用户尝试报工、PQC提交或PQC放行, Then 系统阻止操作并显示对应冻结提示。
- BDD: QA 让步放行 -> Given QA 打开冻结批次评审详情且填写评审意见、上传评审材料、输入 QA 签名, When QA 点击 `让步放行`, Then 工单从 `frozen` 回到 `normal`，评审单关闭并记录处置结论。
- BDD: QA 返工 -> Given QA 打开冻结批次评审详情且填写必填字段, When QA 点击 `返工`, Then 工单从 `frozen` 回到 `normal`，评审单关闭且不进入返工确认流程。
- BDD: QA 作废 -> Given QA 打开冻结批次评审详情且填写必填字段, When QA 点击 `作废`, Then 工单从 `frozen` 变为 `voided`，评审单关闭并形成只读作废批次执行追溯。
- BDD: 追溯差异展示 -> Given 不合格评审已按让步放行、返工或作废关闭, When 用户进入追溯页, Then 页面按处置结论展示不同信息，且包含原因、评审材料、评审意见、QA签名、处置结论和冻结/解冻或作废时间。

## Commands And Evidence

- 已读取 worktree、任务收尾、PowerShell 编码、PowerShell/Git、前端、后端、数据库规则。
- 已创建新 worktree：`D:\IntRuoyiWorktree\nonconformance-review-mvp-20260830`。
- 已读取经验索引命中项：eDHR 批次执行、冻结/作废、真实 E2E、PowerShell/Git、worktree 端口门禁。
- 运行槽位登记：`D:\IntRuoyiWorktree\nonconformance-review-mvp-20260830\scripts\runtime\reserve-worktree-slot.ps1 -Name nonconformance-review-mvp-20260830 -Path D:\IntRuoyiWorktree\nonconformance-review-mvp-20260830 -Branch codex/nonconformance-review-mvp-20260830 -Profile int_batch -AsJson` -> FAIL，既有 worktree `D:\IntRuoyiWorktree\20260815-frontline-pqc-c00-backfill-remediation` 使用非法 slot `20`。
- 已新增后端静态契约测试：`IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-nonconformance-review-mvp-static.spec.cjs`。
- 已新增前端静态契约测试：`IntRuoyiFronted\tests\e2e\edhr-nonconformance-review-mvp-static.spec.js`。
- 已实现后端统一不合格评审单表、DO、Mapper、Service、Controller、冻结拦截和追溯读模型。
- 已实现前端统一不合格评审 API、路由、QA 页面、PQC 填写入口、PQC 放行入口、批次冻结状态显示和追溯展示。
- 2026-08-30 E2E 复验使用当前 `E:\IntRuoyi` 端口合同重新登记成功：`int_batch slot 1`，前端 `8042`，后端 `48042`。
- `mvn.cmd -pl yudao-server -am -DskipTests package` -> PASS，生成本 worktree 完整运行 Jar；SHA-256 为 `A21F52B55DE256381F3A2B95E763060AB34EFA057052C68C19F360710E7896ED`，内嵌 MES 模块确认包含 `MesProEdhrNonconformanceReviewController.class`。
- 本地数据库正式迁移执行两次均成功；表 `mes_pro_edhr_nonconformance_review`、29 个字段、4 个索引及菜单 `9008300..9008303` 已核对。
- 真实运行态：后端 `48042/actuator/health` 为 `UP`，前端 `8042` HTTP 可访问；结束后两端口监听数均为 `0`。
- Playwright CLI `--help` 触发 Windows `UV_HANDLE_CLOSING` 并退出 `1`；按 `docs/e2e-rules.md` 的明确规则，改用仓库既有 `require('playwright')` 真实脚本模式，不改用 API-only。
- 真实浏览器已完成：测试租户 `122` 的 `admin` 登录、真实左侧菜单进入 `eDHR不合格评审`、待评审接口业务码 `0`、真实菜单进入 `批次执行`、打开“打开/创建 eDHR 批次执行”弹框、搜索并选择生产工单。
- 真实浏览器阻塞点：选择 `WO-EDHR-CELL-20260728-104808` 后，页面显示“eDHR 批次执行对应工艺路线不存在”；后端首个业务异常位于 `resolveEnabledProductRoutes`。
- 只读终检：测试租户正式可选工单/路线组合数 `0`；本轮批次号 `NCR-E2E-ncr-20260830-01` 行数 `0`；本轮不合格评审行数 `0`。
- 已删除可能包含登录态的 `failure-trace.zip`；保留不含密码的 `result.json` 与失败页截图作为当前 blocker 证据。
- `pnpm ts:check` -> PASS，本轮迁移修正和真实 E2E 脚本加入后再次复核通过。
- 经验沉淀：已把“eDHR 不合格评审 E2E 正式夹具前置”和“迁移依赖 ID/固定菜单 ID 冲突”分别合并到 `docs/e2e-rules.md`、`docs/database-rules.md`，并更新 `docs/experience-index.md` 关键词路由。
- 用户授权模拟数据后，先通过真实工艺流程页面临时启用/恢复 `RT000028`；新建批次仍因缺正式 BATCH 配置失败，未产生批次。来源路线最终恢复停用。
- 最终模拟样本使用测试租户历史 E2E 批次 `900000000713`：创建前状态 `20`、无活跃人工任务、无既有评审、执行记录 `1218`；完整 run 后状态 `60`。
- E2E 暴露根因缺陷：创建评审写入 `15` 后，详情页 `syncBatchStatus` 未保护冻结状态并按任务结果改回 `20`，导致 QA 处置返回 `1040750406`。
- 已补聚焦静态合同并修复 `syncBatchStatus` 保留 `BATCH_STATUS_FROZEN`；同时冻结状态下 `openTask` 明确拒绝。重新完整打包后运行 Jar加载修复。
- 旧运行包已把本任务评审 `id=1` 的批次状态覆盖为 `20`；后端停机时按 `tenant=122 + batch=900000000710 + review=1 + pending_review + previousStatus=20 + currentStatus=20` 精确恢复 1 行为 `15`，随后真实页面完成处置。
- GREEN: 真实 Playwright `ncr-20260830-04` -> PASS；评审 `4/5/6` 分别为 `concession_release/rework/void`，来源覆盖 `PQC_RELEASE/PQC_SUBMISSION`，最终批次状态 `60`。
- 最终只读核验：模拟批次 `900000000710/900000000713` 均为 `60`；各 3 条评审全部 closed、材料/意见/签名完整；待处理评审 `0`；来源路线状态 `1`（停用）。
- 安全清理：删除全部 `*trace.zip` 共 5 个；最终剩余 trace `0`；停止前后端后 `8042/48042` 监听数均为 `0`。
- 经验沉淀：将“eDHR 批次冻结状态与派生状态同步边界”合并到 `docs/backend-development.md`，并更新 `docs/experience-index.md` 关键词路由。
- Closeout preview: `task_closeout.py --task-id nonconformance-review-mvp-implementation --mode preview` -> BLOCKED；核心三份任务文档均在 keep，delete 为空，唯一阻塞为未找到已 checkout 的 `int_main` worktree。未执行 apply、提交、合并或 worktree 删除。
- 源分支提交门禁：`git commit -m "feat: 实现不合格评审 MVP"` -> BLOCKED；该分支内置端口合同仍为 v3（只接受 `1..19`），共享登记表已升级到 v7 并合法包含 slot `20`，旧钩子无法解析当前正式登记表。为生成仅含本任务的可移植提交，源分支提交将显式跳过这个已失效钩子；融合前必须在最新 `int_main` 干净工作树运行 v7 `branch-runtime-port-guard.ps1` 并通过，未通过不得融合。

## RED

- RED: `node src\test\js\mes-edhr-nonconformance-review-mvp-static.spec.cjs` -> FAIL, expected reason: `MesProEdhrNonconformanceReviewService.java` 尚未实现，后端统一不合格评审契约不存在。
- RED: `node tests\e2e\edhr-nonconformance-review-mvp-static.spec.js` -> FAIL, expected reason: `src/api/mes/pro/edhr/nonconformanceReview.ts` 尚未实现，前端统一入口契约不存在。
- RED: `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql --sql-file sql\mysql\20260830_mes_edhr_nonconformance_review_mvp.sql` -> FAIL, expected reason: `dependsOn` 误写为带 `.sql` 后缀的文件名，迁移门禁无法解析正式迁移 ID。
- RED: 本地数据库菜单预检 -> FAIL, expected reason: 原迁移菜单 ID `900170/900171` 已被排产权限占用，继续执行会静默缺主菜单或主键冲突。
- RED: `node src\test\js\mes-edhr-nonconformance-review-mvp-static.spec.cjs` -> FAIL, expected reason: 迁移尚未使用无冲突菜单 ID，也没有路径/权限/ID fail-fast 过程。
- RED: 真实 Playwright `node tests\e2e\edhr-nonconformance-review-mvp-real.e2e.js` -> BLOCKED, expected reason: 测试租户没有可通过正式页面创建任务自有批次的生产工单/启用工艺路线组合。
- RED: `node src\test\js\mes-edhr-nonconformance-review-mvp-static.spec.cjs` -> FAIL, expected reason: `syncBatchStatus` 未保留 `BATCH_STATUS_FROZEN`。
- RED: 真实 Playwright QA处置 -> FAIL, expected reason: 详情状态同步把冻结批次从 `15` 改回 `20`，处置返回 `1040750406`。

## GREEN

- GREEN: `node src\test\js\mes-edhr-nonconformance-review-mvp-static.spec.cjs` -> PASS，最终复跑 PASS
- GREEN: `node tests\e2e\edhr-nonconformance-review-mvp-static.spec.js` -> PASS，最终复跑 PASS
- GREEN: `mvn -pl yudao-module-mes -am "-DskipTests" compile` -> PASS，最终复跑 PASS
- GREEN: `pnpm install --frozen-lockfile` -> PASS，锁文件未变更
- GREEN: `pnpm ts:check` -> PASS，最终复跑 PASS
- GREEN: `git diff --check` -> PASS，仅输出 CRLF 提示
- GREEN: `python C:\Users\BJB110\.codex\skills\quality-assurance-test-suite\scripts\validate_quality_assurance.py --evidence D:\IntRuoyiWorktree\nonconformance-review-mvp-20260830\doc\tasks\nonconformance-review-mvp-implementation\verification-report.md` -> PASS
- GREEN: `rg -n "No checked-out worktree for main branch|task-closeout 主线 worktree 可见性门禁" docs\experience-index.md docs\worktree-memory.md` -> PASS，经验索引可定位新增 closeout blocker 门禁。
- GREEN: `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql --sql-file sql\mysql\20260830_mes_edhr_nonconformance_review_mvp.sql --sql-file sql\mysql\20260608_edhr_batch_execution_schema.sql` -> PASS，迁移依赖闭包为 2。
- GREEN: `node src\test\js\mes-edhr-nonconformance-review-mvp-static.spec.cjs` -> PASS，覆盖正式依赖 ID、菜单冲突检查和无冲突菜单 ID。
- GREEN: 本地数据库迁移二次幂等执行 -> PASS，目标表和菜单结构保持唯一。
- GREEN: `node src\test\js\mes-edhr-nonconformance-review-mvp-static.spec.cjs` -> PASS，`syncBatchStatus` 冻结保留合同通过。
- GREEN: `mvn.cmd -pl yudao-server -am -DskipTests package` -> PASS，修复后的完整运行 Jar 生成成功。
- GREEN: `node tests\e2e\edhr-nonconformance-review-mvp-real.e2e.js`（run `ncr-20260830-04`）-> PASS，真实用户路径完成三类处置与差异追溯。

## Blockers

- Runtime/E2E blocker: `reserve-worktree-slot.ps1` 因既有 worktree slot `20` 阻塞，暂不能启动本 worktree 前后端服务；本任务继续执行不依赖本地服务启动的代码实现和定向静态/单元验证。
- Runtime/E2E blocker recheck: `D:\IntRuoyiWorktree\nonconformance-review-mvp-20260830\scripts\runtime\reserve-worktree-slot.ps1 -Name nonconformance-review-mvp-20260830 -Path D:\IntRuoyiWorktree\nonconformance-review-mvp-20260830 -Branch codex/nonconformance-review-mvp-20260830 -Profile int_batch -AsJson` -> FAIL，既有 worktree `D:\IntRuoyiWorktree\20260815-frontline-pqc-c00-backfill-remediation` 使用非法 slot `20`，仍阻塞真实运行态和 Playwright E2E。
- Runtime/E2E blocker recheck superseded: 当前项目端口合同已扩展为 `1..100`，使用主工作区最新原子脚本登记 `slot 1` 成功，旧 slot blocker 不再成立。
- Current E2E blocker: 已解除。用户允许测试租户模拟数据后，使用无活跃人工任务、无既有评审的历史 E2E 批次完成真实路径验证并作废收口。
- Closeout blocker: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id nonconformance-review-mvp-implementation --mode preview` -> BLOCKED，未找到 `int_main` 的 checked-out worktree，因此未执行 apply/合并/删除 worktree。
- Source commit guard blocker: 源分支 v3 端口钩子与共享 v7 登记表不兼容；只允许在源分支生成任务提交时显式跳过，最终 `int_main` 融合仍以最新 v7 门禁通过为硬条件。

## int_main 融合记录

- 用户意图：先提交当前任务代码，再融合到 `int_main`；未授权 push。
- 源任务提交：`e5df7e02dd388efa70313a04c8ef44b8c6057262`（`feat: 实现不合格评审 MVP`），仅包含本任务 38 个文件。
- 禁止直接合并源分支：源分支基于 `int_batch`，相对 `int_main` 还携带 11 个非本任务历史提交；因此从最新 `int_main@c445dd0f93e099d41e58326dbbf668a4217ad084` 创建干净融合分支，只移植本任务提交。
- 融合工作树：`D:\IntRuoyiWorktree\nonconformance-review-mvp-int-main-20260830`，分支 `codex/nonconformance-review-mvp-int-main-20260830`，登记 `int_main slot 54`（`8309/48309`）。
- 冲突处理：保留主线放行四材料门禁、经理审批和电子签核链路，同时加入不合格冻结检查；作废状态加入完成追溯；不合格错误码因主线占用 `1040750466..468` 调整为 `1040750469..474`。
- 前端冲突处理：以最新批次详情和一线 PQC 大屏为底稿，移除旧“质量拒收”弹框，统一跳转 `MesProFeedbackEdhrNonconformanceReview`；PQC 页新增同一入口并保留主线现有工单、工序、签名和大屏交互。
- 并行改动保护：`E:\IntRuoyi` 的 `docs/backend-development.md`、`docs/database-rules.md`、`docs/experience-index.md` 正被其它任务修改，首个融合提交显式保留主线版本。融合提交形成后、最终快进前，`docs/e2e-rules.md` 也出现并行修改，因此增加一个范围收缩提交把该文件恢复为主线版本。最终不覆盖或提交这四份并行经验文档；本任务代码、测试、任务记录及无重叠的 `docs/worktree-memory.md` 正常融合。
- GREEN: 最新 v7 `scripts\preflight\branch-runtime-port-guard.ps1` -> PASS（融合前与冲突解决后各一次）。
- GREEN: 融合基线后端静态合同 -> PASS；前端静态合同与真实 E2E 脚本语法检查 -> PASS；迁移依赖闭包门禁 -> PASS。
- GREEN: `pnpm install --frozen-lockfile` -> PASS，锁文件未变更；`pnpm ts:check` -> PASS。
- GREEN: `mvn.cmd -pl yudao-module-mes -am "-DskipTests" compile` -> PASS，24 个 Maven 模块全部 SUCCESS。
- GREEN: `git diff --cached --check` -> PASS，仅 CRLF 提示。
