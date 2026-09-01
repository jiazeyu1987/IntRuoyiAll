# 不合格评审 MVP 实现执行日志

## User Intent

用户要求在新的 worktree 中实现并验证文档中的最小不合格评审 MVP。两个不合格审批必须使用同一个流程、同一套代码、同一个入口。

2026-08-30 用户追加要求：执行真实 E2E 验证。当前项目端口契约已从旧版 `1..19` 扩展为 `1..100`，因此重新核对并执行 worktree 运行态与 Playwright 真实页面验证。

2026-08-30 用户解除夹具阻塞：明确允许在测试租户模拟数据。授权范围仅用于创建本任务可追踪、可清理的测试夹具；仍禁止直接 SQL/API 绕过真实页面业务链路。

2026-08-30 用户要求先提交当前任务代码，再融合进 `int_main`。当前功能分支基于 `int_batch`，相对当前 BatchRecord clone 的 `origin/int_main` 额外包含 11 个非本任务提交；不得直接 merge 整条分支。融合采用“当前任务提交 -> 最新 `int_main` 干净集成 worktree 仅移植本任务提交 -> `int_main` 快进”的范围隔离方式，不推送远端。

2026-08-31 用户追加要求：`PQC_SUBMISSION` 的 `不合格审查` 入口必须从一线 PQC 填写页移到 `PQC组长 -> PQC管理` 行操作区；一线页不应再直接发起该审查，组长需在提交后的管理列表条目上点击进入统一不合格评审页。

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
- BDD: PQC提交后由组长发起不合格审查 -> Given 一线PQC提交后记录出现在 PQC管理 列表, When PQC组长在该行点击不合格审查, Then 系统进入统一不合格评审页并携带 `sourceType=PQC_SUBMISSION`、提交事件ID和对应 `batchExecutionId`。
- BDD: 一线PQC不显示提交类审查入口 -> Given 用户正在一线PQC填写页, When 页面加载完成, Then 页面不再显示可直接发起 `PQC_SUBMISSION` 不合格审查的按钮。

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
- RED: `node tests\e2e\edhr-nonconformance-review-mvp-static.spec.js` -> FAIL, expected reason: 真实 E2E 脚本仍没有 `PQC_LEADER_PATH`，PQC 提交不合格审查仍未按 `PQC组长 -> PQC管理` 路径验证。

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
- 融合提交：`549b9d1a837e0c6e10812ea56c3ac6e9250d147a`（功能融合）与 `9c03ce584b626f013acbc813ffd412d738744428`（并行经验文档保护）。
- 快进前主工作区交集检查：任务最终 34 个路径与主工作区 70 个 dirty/untracked 路径交集为 `0`；`c445dd0f9` 是 `9c03ce584` 的祖先。
- 首次 `git merge --ff-only` -> BLOCKED，`E:\IntRuoyi\.git\index.lock` 已存在。核验锁文件为 0 字节、创建于 `22:30:25`、超过一小时未更新且可独占打开，证明没有进程持有；只删除该精确陈旧锁后重试。
- `git merge --ff-only codex/nonconformance-review-mvp-int-main-20260830` -> PASS；`int_main` 从 `c445dd0f9` fast-forward 到 `9c03ce584`，post-merge v7 端口门禁 PASS，原并行 dirty/untracked 内容保持未暂存。
- 未执行 push；`int_main` 相对 `origin/int_main` ahead 2。
- Closeout preview after merge: `task_closeout.py --task-id nonconformance-review-mvp-implementation --mode preview` -> BLOCKED，仅因为 `E:\IntRuoyi` 主工作区含其它并行任务 dirty 内容。keep 为三份核心任务文档、delete 为空；未执行 apply 或删除本任务 worktree。

## 当前 int_main E2E 复验（2026-08-31）

- 用户意图：在当前 `int_main` 运行态重新执行不合格评审真实 E2E。
- BDD: 当前主线统一不合格评审闭环 -> Given 测试租户存在可追踪批次且当前运行包来自 `int_main`，When Playwright 从真实页面分别发起 PQC放行/PQC提交不合格审查并由 QA 执行让步放行/返工/作废，Then 批次冻结、三项操作禁止、三类处置和差异追溯均成立，最终无待处理评审。
- 当前状态：in_progress；先核对 `8081/48081` 监听进程、运行包来源、数据库迁移、登录前置和任务脚本入口，任一缺失即 fail fast。
- 运行态隔离：`E:\IntRuoyi` 的 `8081` 正被主工作区前端使用、`48081` 未启动，且主工作区有其它任务 dirty 内容；复用本任务已登记的 `int_main slot 54` 验证 worktree，快进到 `c3a134a797c3aab24feba3aafec32815f8345cf1`，端口为 `8309/48309`。
- RED: 真实 E2E 指定 `8309/48309` -> FAIL，脚本把运行端口写死为旧 `int_batch slot 1`。
- RED: 前端静态合同 -> FAIL，脚本没有读取 `worktree-ports.json` 的正式登记。GREEN: 脚本按当前 repo root 解析基准 `int_main` 或 active worktree 登记，并精确校验成对端口；静态合同与语法检查 PASS。
- 数据库前置：本地 Docker MySQL/Redis 正常；目标评审表 `batch_execution_id` 已可空，来源索引存在；`20260608 -> 20260830 -> 20260831` 三迁移依赖门禁 PASS。
- 前端前置：`pnpm install --frozen-lockfile` PASS；`pnpm ts:check` PASS。
- 后端运行包：`mvn.cmd -pl yudao-server -am "-Dmaven.test.skip=true" package` -> PASS，30 个模块全部 SUCCESS；运行包 SHA-256 `E72727E18DC8F2E2588E166394B2E504688F2FCD6F144508D376A6D8A27AB2C9`，内嵌 MES 包为 STORED，统一评审、评审服务和 PQC 放行 Controller 关键类均存在。
- 运行态：当前提交运行包在 `48309` health=`UP`，Vite 在 `8309` HTTP 200；监听 PID 命令行均属于本任务验证 worktree。
- E2E `ncr-int-main-20260831-01` -> FAIL，进入评审路由时首次组件尚未挂载，脚本过早查找刷新按钮；业务写入 0。修正为等待按钮可见后再监听刷新请求。
- E2E `ncr-int-main-20260831-02/03` -> FAIL，主线列表已从旧 `table-quick-filter` 升级到 `table-multi-filter`，随后泛化 input 同时命中操作符和文本框；业务写入 0。修正为真实“新增筛选条件 -> 批次执行编码 -> 查询”路径及精确文本框。
- E2E `ncr-int-main-20260831-04` -> 目标业务步骤全部完成，批次 `900000000953` 的评审 `8/9/10` 覆盖 `concession_release/rework/void` 并最终作废；总结果因全局审批待办角标接口独立异常判 FAIL。该错误精确归类为 non-target 并保留输出，未删除目标断言。
- RED: E2E `ncr-int-main-20260831-05` -> FAIL；批次已冻结为 `15`，但 admin 金手指绕过导致页面不显示“冻结后禁止报工、PQC提交、PQC放行”。
- RED: 新增静态合同 -> FAIL，`nonconformanceFrozenActionLocked` 位于 `hasGoldenFingerActionBypass` 内。GREEN: 冻结锁移到高权限绕过之外，其他作废/放行锁仍保留原金手指语义；静态合同与 `pnpm ts:check` PASS。
- E2E `ncr-int-main-20260831-06` -> 真实 QA 页面完成评审 `11` 让步放行；随后历史样本无可见工序卡片，旧脚本在正式隐藏追溯路由前超时。修正为有工序卡片时点击，无卡片时直接进入现有正式追溯路由。
- GREEN: E2E `ncr-int-main-20260831-07` -> PASS；同一真实页面链路确认 `PQC_RELEASE/PQC_SUBMISSION` 统一入口、冻结提示、返工、作废和三类差异追溯，`targetPageErrors=0`、`targetConsoleErrors=0`。
- 最终只读核验：批次 `900000000783/900000000953` 均为 `60`；实际评审 `11/12/13` 与 `8/9/10` 均 closed，材料/意见/QA签名/冻结/关闭时间完整，让步与返工有解冻时间、作废有作废时间；目标批次待处理评审数 `0`。
- 安全清理：删除所有 `*trace.zip`，剩余 `0`；停止任务专属 Node/Java 与 launcher，`8309/48309` 均无监听；删除可能含登录请求的四个运行日志和专用 runtime 日志目录。
- 本轮目标源码/测试已用 UTF-8 归一化文本比较同步到 `E:\IntRuoyi`，三个文件与验证 worktree 内容一致；保持未暂存、未提交、未 push。

## PQC提交入口迁移到PQC管理（2026-08-31）

- 用户意图：`PQC_SUBMISSION` 的 `不合格审查` 入口不属于一线 PQC 填写页；应在一线 PQC 提交后，由 `PQC组长 -> PQC管理` 列表对应条目行操作发起。
- BDD: PQC提交后由组长发起不合格审查 -> Given 一线 PQC 已提交并出现在 `PQC组长 -> PQC管理` 列表，When 组长点击该条目行操作的 `不合格审查`，Then 页面进入统一不合格评审入口，来源为 `PQC_SUBMISSION`，来源 ID 为该提交事件 ID，并带出对应 eDHR 批次 ID。
- BDD: 一线PQC不显示提交类审查入口 -> Given 一线 PQC 正在填写或查看 PQC 检验页，When 页面渲染固定模板面板，Then 不显示 `PQC_SUBMISSION` 的 `不合格审查` 按钮，避免一线人员在提交前直接发起审查。
- RED: `node tests\e2e\edhr-nonconformance-review-mvp-static.spec.js` -> FAIL，expected reason: 真实 E2E 脚本仍没有 `PQC_LEADER_PATH`，PQC 提交不合格审查仍未按 `PQC组长 -> PQC管理` 路径验证。
- RED: `node src\test\js\mes-edhr-nonconformance-review-mvp-static.spec.cjs` -> FAIL，expected reason: `PQC管理` 读模型仍通过 `pqc_submission_trace` 读取 `batchExecutionId`，没有从正式 eDHR 批次执行记录解析。
- GREEN: `node src\test\js\mes-edhr-nonconformance-review-mvp-static.spec.cjs` -> PASS，`PQC管理` 读模型按工单、工艺路线、批号解析最新有效 eDHR 批次，并移除旧 `pqc_submission_trace` 连接。
- GREEN: `node tests\e2e\edhr-nonconformance-review-mvp-static.spec.js` -> PASS，前端契约确认一线 PQC 移除旧入口、`PQC组长 -> PQC管理` 行操作新增 `PQC_SUBMISSION` 入口并携带 `batchExecutionId`。
- GREEN: `node --check tests\e2e\edhr-nonconformance-review-mvp-real.e2e.js` -> PASS，真实 E2E 脚本语法正确且路径已指向 `PQC组长 -> PQC管理`。
- RED: `pnpm ts:check` -> FAIL，expected reason: 批次详情页既有归档生成参数把路由解析的字符串 `workTaskId` 直接传给要求数字的接口。
- GREEN: `pnpm ts:check` -> PASS，归档生成前按正整数文本严格转换为安全数字；未引入默认值或降级。
- GREEN: `mvn.cmd -pl yudao-module-mes -am "-DskipTests" compile` -> PASS，24 个 Maven 模块全部 SUCCESS。
- GREEN: `git diff --check -- <task-owned paths>` -> PASS，仅输出 CRLF 换行提示。
- Real E2E blocker: 当前 `8081` 前端 HTTP 200，但 `48081` 后端无监听，且 `NCR_E2E_PASSWORD` 为空；本轮未运行真实 Playwright E2E，未用 API-only 或静态验证冒充真实页面验证。
- GREEN: project-experience-consolidation -> PASS，已将“提交后审查类按钮必须落到提交后的管理列表行操作，并覆盖旧入口负向断言/新入口参数断言”合并到 `docs/frontend-development.md#前端角色内容页签拆分口径门禁`，并更新 `docs/experience-index.md`。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id nonconformance-review-mvp-implementation --mode preview` -> PASS，keep 为三份核心任务文档，delete/blocked/warnings 均为空。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id nonconformance-review-mvp-implementation --mode apply` -> PASS，delete 为空；当前为主工作区 `int_main`，未执行 worktree 合并或删除。
- Current status: completed；本轮未执行 Git commit、push 或 fast-forward。

## 后端重启后真实 E2E 复验（2026-09-01）

- 用户意图：用户已重启后端，要求继续前一轮因 `48081` 后端未监听而阻塞的真实 Playwright E2E。
- Preflight: 已读取 `docs/e2e-rules.md`、`docs/login-access.md`、`docs/local-runtime.md`、`docs/worktree-restrictions.md` 和 Playwright skill。
- Preflight: `8081` 前端 HTTP 200；`48081` 后端 health=`UP`；`npx --version` 可用。
- Preflight: `48081` PID 属于 `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260831-232928.jar`；Jar SHA-256 `3F19749CBE0E462C9A5618FDE11BAA5ED4C0D4786C22782D03D8E09262FF3526`；内嵌 MES 模块包含新的 `PQC管理` 批次执行查询，且不再包含 `pqc_submission_trace`。
- Current status: in_progress；准备使用本机默认登录配置安全注入密码并执行真实页面 E2E。

## QA待审不合格订单测试数据（2026-09-01）

- 用户意图：在本机测试租户加入一条不合格订单，用于 QA 页面直接看到待不合格审批数量和待审列表。
- BDD: QA待审测试订单可见 -> Given 本机测试租户存在可冻结的 eDHR 批次执行且没有待处理不合格评审, When 通过正式不合格评审创建接口发起 `PQC_RELEASE` 不合格评审, Then 该批次进入冻结状态，QA 待审列表新增 1 条 `pending_review` 记录，生产报工、PQC提交、PQC放行被不合格冻结拦截。
- 数据写入约束：只使用本机 `8081/48081` 与测试租户；优先走正式业务接口创建评审单，不手工拼接评审单和批次状态；若找不到可冻结批次或接口前置失败，停止并记录 blocker。
- RED/PRECHECK: `node -e <login + pending-page + batch-page precheck>` -> PASS；测试租户 `122` 登录成功，写入前 QA 待审总数 `0`，选定批次 `900000000980` 状态 `20`，不存在待处理不合格评审。
- GREEN: `node -e <login + create review + pending-page verification>` -> PASS；通过正式接口创建评审 `14`，编号 `EDHR-NCR-20260901011057-900000000980`，来源 `PQC_RELEASE`，批次 `900000000980` 从 `20` 冻结为 `15`，QA 待审总数从 `0` 变为 `1`。
- GREEN: `node -e <login + home summary + pending-page + batch get>` -> PASS；MES 首页不合格评审待处理数 `1`，QA 待审列表 `total=1`，评审 `14` 可见且 `reviewStatus=pending_review`。

## 后端重启后入口迁移复验续跑（2026-09-01）

- 用户意图：用户说明后端已经重启，要求继续验证 `PQC提交 -> PQC组长/PQC管理 -> 不合格审查` 的真实入口迁移。
- BDD: PQC提交后由组长发起不合格审查 -> Given 一线 PQC 已提交并进入 `PQC组长 -> PQC管理` 列表, When 组长点击该条目行操作的 `不合格审查`, Then 跳转统一不合格评审页面，来源为 `PQC_SUBMISSION`，并带出该提交对应的 eDHR 批次 ID。
- RED: 当前 `48081` 运行包检查 -> FAIL，expected reason: 运行包 `backend-runtime-control-20260901-024209-approval-center.jar` 仍包含旧 `BINARY batch_execution.batch_code = BINARY work_order.batch_code`，不包含本任务修复后的 `CAST(... AS BINARY)`。
- RED: 默认本机租户登录态 `PQC管理` 列表接口 -> FAIL，expected reason: 目标接口返回业务 `500/系统异常`，当前共享运行态未加载本任务后端修复。
- GREEN: `backend-runtime-control-20260901-022725.jar` 运行态历史核验 -> PASS；该运行包包含 `CAST(... AS BINARY)`，不含旧 unary `BINARY` 写法，且默认本机租户 `PQC管理` 列表业务码为 `0`。
- GREEN: `node .\IntRuoyiFronted\tests\e2e\edhr-nonconformance-review-mvp-static.spec.js` -> PASS。
- GREEN: `node .\IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-nonconformance-review-mvp-static.spec.cjs` -> PASS。
- GREEN: `node --check .\IntRuoyiFronted\tests\e2e\edhr-nonconformance-review-mvp-real.e2e.js` -> PASS。
- GREEN: `mvn.cmd -pl yudao-module-mes -am "-DskipTests" compile` -> PASS，24 个 Maven 模块全部 SUCCESS。
- GREEN: `git diff --check -- <task-owned paths>` -> PASS，仅 CRLF 换行提示。
- RED/BLOCKED: Playwright `ncr-int-main-20260901-pqc-entry-04` -> FAIL，测试租户目标业务账号密码不匹配，未进入业务写入。
- RED/BLOCKED: Playwright `ncr-int-main-20260901-pqc-entry-05` -> FAIL，登录后进入 QA 列表阶段遇到页面等待超时及本机请求 `502`，结果文件显示 `completedDispositions=[]`。
- RED/BLOCKED: Playwright `ncr-int-main-20260901-pqc-entry-06` -> FAIL，登录页加载超时；前端日志曾出现 Vite deps cache `EPERM rename ... deps_temp_* -> deps`，结果文件显示 `completedDispositions=[]`。
- GREEN: task artifact cleanup -> PASS；已删除 5 个本任务 Playwright `failure-trace.zip`，剩余 trace 压缩包数量为 `0`，保留截图与 `result.json`。
- GREEN: project-experience-consolidation -> PASS；已将 Vite deps cache `EPERM rename` 与 `8081` 监听但页面超时的运行态门禁合并到 `docs/local-runtime.md` 和 `docs/experience-index.md`。
- Current status: blocked；当前共享后端运行包未加载本任务修复，真实页面点击验收不能放行，未强停或覆盖疑似其它任务运行态。

## 用户授权替换后续复验（2026-09-01）

- 用户意图：用户回复“允许”，授权停止当前占用 `48081` 且未加载本任务修复的旧 `approval-center` 后端，并继续验证 `PQC管理` 红框入口。
- BDD: PQC管理红框入口只读验证 -> Given 已存在一线 PQC 提交记录且该记录能解析到 eDHR 批次, When 组长在 `PQC管理` 行操作点击 `不合格审查`, Then 页面进入统一不合格评审页，URL 携带 `sourceType=PQC_SUBMISSION`、提交事件 ID 与 `batchExecutionId`，且入口只读验证不提交评审单。
- GREEN: 用户授权后端替换 -> PASS；停止旧 `approval-center` 运行包后启动 `backend-runtime-control-20260901-022725.jar`，`48081` health=`UP`，默认本机租户 `PQC管理` 列表业务码为 `0`。
- GREEN: 组合验证运行包 -> PASS；基于最新 `approval-center` 运行包生成 `backend-runtime-control-20260901-024209-approval-center-pqc-fixed.jar`，仅替换内嵌 MES Mapper 资源，核验包含 `CAST(batch_execution.batch_code AS BINARY) = CAST(work_order.batch_code AS BINARY)` 且不含旧 unary `BINARY` 写法。
- GREEN: 当前后端运行态 -> PASS；`48081` 运行 `approval-center-pqc-fixed` 验证包，health=`UP`；默认本机租户 `PQC管理` 列表接口业务码 `0`、总数 `54`；测试租户 `PQC管理` 列表接口业务码 `0`、总数 `0`。
- RED/BLOCKED: Playwright `ncr-int-main-20260901-pqc-entry-07` -> FAIL，expected reason: 测试租户创建批次的正式业务前置缺失，`open-or-create-manual` 返回 `1040750403/eDHR 批次执行缺少工艺流程批记录配置流程配置或默认批记录`；该 run 的 `completedDispositions=[]`，未完成处置写入。
- GREEN: 异常恢复核验 -> PASS；失败后只读确认路线 `RT000028` 在测试租户状态为 `0`，未留下启用状态残留。
- 只读数据核验：测试租户当前无可见 `PQC管理` 提交行，候选工单/路线组合数为 `0`；默认本机租户存在历史 PQC 提交行可解析 `batchExecutionId`，但需要真实登录凭据才能走页面只读点击验证。
- RED: `node .\IntRuoyiFronted\tests\e2e\edhr-nonconformance-review-mvp-static.spec.js` -> FAIL，expected reason: 真实 E2E 脚本缺少 `NCR_E2E_PQC_ENTRY_ONLY` 只读入口验证模式。
- GREEN: `node .\IntRuoyiFronted\tests\e2e\edhr-nonconformance-review-mvp-static.spec.js` -> PASS；脚本已支持 `NCR_E2E_PQC_ENTRY_ONLY=1`，在只读模式下验证 `PQC管理` 行按钮跳转并断言不提交评审写请求。
- GREEN: `node --check .\IntRuoyiFronted\tests\e2e\edhr-nonconformance-review-mvp-real.e2e.js` -> PASS。
- RED/BLOCKED: Playwright 只读入口模式 -> FAIL，expected reason: 当前环境缺少 `NCR_E2E_PASSWORD` 且前端默认密码配置不存在；按 fail-fast 未猜密码、未复用 token、未降级 API-only。
- GREEN: E2E 共享路线恢复门禁沉淀 -> PASS；`docs/e2e-rules.md` 明确共享路线/租户开关只有在本脚本实际改变状态时才执行恢复，`docs/experience-index.md` 已加入 `sourceRouteNeedsRestore` 与 `setSourceRouteEnabled` 关键词。
- GREEN: task artifact cleanup -> PASS；已删除本轮新增 `ncr-int-main-20260901-pqc-entry-07\failure-trace.zip`，保留截图和 `result.json` 轻量证据。
- GREEN: final static verification -> PASS；`node .\IntRuoyiFronted\tests\e2e\edhr-nonconformance-review-mvp-static.spec.js`、`node .\IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-nonconformance-review-mvp-static.spec.cjs`、`node --check .\IntRuoyiFronted\tests\e2e\edhr-nonconformance-review-mvp-real.e2e.js` 均通过。
- GREEN: final runtime/artifact check -> PASS；`48081` 监听 `backend-runtime-control-20260901-024209-approval-center-pqc-fixed.jar` 且 health=`UP`，本任务 Playwright trace 压缩包剩余 `0`。
- GREEN: `git diff --check -- <task-owned paths>` -> PASS，仅 CRLF 换行提示。
- Current status: blocked；代码和当前后端接口已满足目标变更，真实红框点击验收还缺正式页面登录凭据或测试租户可点击的提交样本。
