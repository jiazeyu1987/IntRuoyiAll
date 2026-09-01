# 不合格评审 MVP 验证报告

## Scope

本报告覆盖统一不合格评审 MVP 的实现验证：PQC 提交记录与 PQC 生产放行共用同一不合格评审单、同一 QA 冻结批次列表、同一处置状态机、冻结后三项拦截，以及处置后的追溯展示。

## Verified Commands

- `node src\test\js\mes-edhr-nonconformance-review-mvp-static.spec.cjs` -> PASS
- `node tests\e2e\edhr-nonconformance-review-mvp-static.spec.js` -> PASS
- `mvn -pl yudao-module-mes -am "-DskipTests" compile` -> PASS
- `pnpm install --frozen-lockfile` -> PASS，锁文件未变更
- `pnpm ts:check` -> PASS
- `git diff --check` -> PASS，仅输出 CRLF 提示
- `validate_quality_assurance.py --evidence D:\IntRuoyiWorktree\nonconformance-review-mvp-20260830\doc\tasks\nonconformance-review-mvp-implementation\verification-report.md` -> PASS
- `rg -n "No checked-out worktree for main branch|task-closeout 主线 worktree 可见性门禁" docs\experience-index.md docs\worktree-memory.md` -> PASS
- `E:\IntRuoyi\scripts\runtime\reserve-worktree-slot.ps1 ... -Profile int_batch -AsJson` -> PASS，分配 `slot 1 / 8042 / 48042`
- `task_closeout.py --task-id nonconformance-review-mvp-implementation --mode preview` -> BLOCKED，未找到 `int_main` 的 checked-out worktree
- `mvn.cmd -pl yudao-server -am -DskipTests package` -> PASS，完整运行 Jar 已生成并确认包含新增 Controller
- `run-release-migration-policy-gate.py`（目标迁移 + 依赖迁移）-> PASS
- 本地正式迁移执行及二次幂等执行 -> PASS
- `node --check tests\e2e\edhr-nonconformance-review-mvp-real.e2e.js` -> PASS
- `node tests\e2e\edhr-nonconformance-review-mvp-real.e2e.js`（run `ncr-20260830-04`）-> PASS
- 本地最终只读核验 -> PASS：评审 `4/5/6` 全部 closed，来源覆盖 `PQC_RELEASE/PQC_SUBMISSION`，处置覆盖 `concession_release/rework/void`，批次状态 `60`，待处理评审 `0`

## Matrix

| Requirement | Test / Verification | Result |
| --- | --- | --- |
| 两个不合格来源进入同一个评审入口 | 后端静态契约检查统一 Service/Controller/API；前端静态契约检查 PQC 提交与 PQC 放行均路由到 `MesProFeedbackEdhrNonconformanceReview` | PASS |
| 创建评审单后冻结活跃工单/批次 | 后端静态契约检查 `BATCH_STATUS_FROZEN`；真实 E2E 新建评审后刷新详情响应状态为 `15` | PASS |
| 冻结后禁止报工、PQC提交、PQC放行 | 后端静态契约检查三项拦截调用；真实 E2E 冻结详情显示“冻结后禁止报工、PQC提交、PQC放行” | PASS |
| QA 处置必填评审材料、评审意见、QA签名 | 后端静态契约检查必填校验错误；前端静态契约检查上传组件和输入项 | PASS |
| 让步放行、返工、作废三类最小状态机 | 后端静态契约检查处置常量和状态更新；前端静态契约检查三个处置按钮 | PASS |
| 作废后进入只读追溯 | 后端静态契约检查作废批次纳入 completed trace 查询；前端静态契约检查追溯页展示不合格评审 | PASS |
| 真实前端用户路径 E2E | 测试租户登录、真实菜单、统一入口、上传材料、三类处置、三次差异追溯；run `ncr-20260830-04` | PASS |

## Test Types

- 覆盖统一不合格评审入口、创建冻结、QA 三类处置状态常量、冻结拦截接入点、前端两个来源入口、QA 处置页、追溯页差异展示。
- 后端编译覆盖新增 Controller、VO、DO、Mapper、Service、冻结拦截和追溯读模型的 Java 编译正确性。
- 前端类型检查覆盖新增 API、路由、页面和改动页面的 TypeScript/Vue 类型正确性。
- 未使用 mock 或默认成功路径替代真实运行态；运行态不满足时按 fail-fast 记录 blocker。
- 最终 E2E 使用测试租户已有、无活跃人工任务、无待处理评审的历史 E2E 样本；所有业务写入通过真实页面完成，最终作废收口。

## RED Evidence

- RED: `node src\test\js\mes-edhr-nonconformance-review-mvp-static.spec.cjs` -> FAIL，原因是后端统一不合格评审契约尚未实现。
- RED: `node tests\e2e\edhr-nonconformance-review-mvp-static.spec.js` -> FAIL，原因是前端统一入口契约尚未实现。
- RED: 聚焦静态合同 -> FAIL，`syncBatchStatus` 未保留 `BATCH_STATUS_FROZEN`，详情加载会把冻结批次改回任务计算状态。
- RED: 真实 E2E 处置 -> FAIL，批次被同步回状态 `20` 后 QA 处置返回 `1040750406`。

## GREEN Evidence

- GREEN: `node src\test\js\mes-edhr-nonconformance-review-mvp-static.spec.cjs` -> PASS
- GREEN: `node tests\e2e\edhr-nonconformance-review-mvp-static.spec.js` -> PASS
- GREEN: `mvn -pl yudao-module-mes -am "-DskipTests" compile` -> PASS
- GREEN: `pnpm ts:check` -> PASS
- GREEN: 聚焦静态合同 -> PASS，`syncBatchStatus` 明确保留 `BATCH_STATUS_FROZEN`
- GREEN: `mvn.cmd -pl yudao-server -am -DskipTests package` -> PASS，重新生成运行 Jar
- GREEN: 真实 Playwright run `ncr-20260830-04` -> PASS，评审 ID `4/5/6`

## Verification Notes

- 测试数据与夹具：用户明确允许测试租户模拟数据。最终一次性 run 使用批次 `900000000713`（无活跃人工任务、无既有评审），完成后状态 `60`；分段诊断批次 `900000000710` 也已状态 `60`。
- 一次性恢复：旧运行包导致评审 `id=1` 对应批次状态从冻结错误回到 `20`，在后端停机状态下按精确条件恢复 1 行到 `15`，随后通过真实页面完成处置；该恢复不作为 E2E 通过证据。
- PQC 页面初始化报告“设备账号未绑定启用工艺路线”，但不合格评审按钮按批次上下文正常可用，目标评审创建与返工处置成功；该错误作为非目标初始化异常记录。
- 遗留模拟批次详情不展示“主数据追溯”按钮，因此追溯内容验证通过正式隐藏路由完成；批次详情入口和当前布局由静态契约覆盖。
- CI 影响：新增 Node 静态契约和真实 E2E 脚本可纳入定向流水线；Java 编译、完整打包与前端类型检查已通过。

## Runtime/E2E Result

- 真实 Playwright E2E 最终 PASS，run ID `ncr-20260830-04`。
- 运行态使用已登记的 `int_batch slot 1`：前端 `8042`、后端 `48042`；后端 health 为 `UP`，运行 Jar 来自本 worktree 最新完整打包。
- 用户路径：测试租户登录 -> 批次列表真实筛选 -> 批次详情放行入口发起让步放行评审 -> QA冻结批次列表上传材料并处置 -> PQC填写入口发起返工评审 -> QA处置 -> 放行入口发起作废评审 -> QA处置 -> 追溯页核对三类差异。
- 统一入口：评审 `4/6` 来源 `PQC_RELEASE`，评审 `5` 来源 `PQC_SUBMISSION`，三者进入同一页面和同一后端评审表。
- 状态机：让步放行与返工均从 `15` 恢复冻结前状态 `20`；作废进入 `60`；三个评审均为 `closed`。
- 必填证据：三个评审的材料、评审意见、QA签名、冻结时间、关闭时间均非空；让步/返工有解冻时间，作废有作废时间。
- 追溯证据：最终追溯页同时展示让步放行、返工、作废三行及各自不同结果说明。
- Playwright CLI 在 Windows 触发 `UV_HANDLE_CLOSING`；按项目明确规则使用仓库 Playwright 运行模式。全部可能包含登录态的 trace 已删除，仅保留截图和脱敏 result JSON。
- 结束状态：来源路线 `RT000028` 已恢复停用；两个模拟批次均状态 `60`；待处理评审数 `0`；`8042/48042` 监听数均为 `0`。

## Blockers

- 无功能、验证或主线融合 blocker。`int_main` 已 fast-forward 到 `9c03ce584`。
- 仅剩 cleanup blocker：主工作区存在其它并行任务未提交改动，`task_closeout.py --mode preview` 按规则拒绝 apply；本任务 worktree 尚未自动删除，任务状态保持 `ready_for_closeout`。

## int_main Integration Verification

- 源任务提交：`e5df7e02dd388efa70313a04c8ef44b8c6057262`。
- 融合基线：`int_main@c445dd0f93e099d41e58326dbbf668a4217ad084`。
- 最新 v7 分支运行端口门禁：PASS，融合分支登记 `int_main slot 54`，端口 `8309/48309` 未启动服务。
- 后端：静态合同 PASS；`mvn.cmd -pl yudao-module-mes -am "-DskipTests" compile` PASS，24 个模块全部 SUCCESS。
- 前端：静态合同 PASS；真实 E2E 脚本 `node --check` PASS；`pnpm ts:check` PASS。
- 数据库：目标迁移与依赖迁移闭包门禁 PASS，共 2 个迁移。
- 并行保护：未把主工作区正在修改的 `docs/backend-development.md`、`docs/database-rules.md`、`docs/e2e-rules.md`、`docs/experience-index.md` 纳入最终融合结果；其余任务代码、测试和文档无主工作区 dirty 路径重叠。
- 结论：语义融合验证通过并已 fast-forward 到 `int_main@9c03ce584`；不需要重复写入测试租户数据，原真实 E2E 结果仍作为业务流程验收证据。未 push。

## Release Recommendation

- MVP 代码实现、迁移门禁、完整后端打包、静态/类型验证和真实 E2E 均通过，可进入任务收尾。当前不建议扩大功能范围；先保留最小状态机和现有追溯口径。

## 当前 int_main E2E 复验（2026-08-31）

- 验证提交：`c3a134a797c3aab24feba3aafec32815f8345cf1`。
- 运行态：已登记 `int_main slot 54`，前端 `8309`、后端 `48309`；后端完整构建 30 模块 PASS，health=`UP`，前端 HTTP 200。
- 迁移前置：三层依赖闭包门禁 PASS；本地评审表最新 nullable/index schema 已存在。
- 静态与类型：后端合同 PASS；前端合同、E2E 语法和 `pnpm ts:check` PASS。
- 真实 E2E：`ncr-int-main-20260831-07` PASS。该轮续接同一真实页面已完成的让步评审，继续完成 PQC提交返工、PQC放行作废和三次追溯；E2E 结果覆盖 `concession_release/rework/void`，目标页面和目标控制台错误均为 0。
- 数据库终态：批次 `900000000783` 的实际评审为 `11/12/13`，来源依次 `PQC_RELEASE/PQC_SUBMISSION/PQC_RELEASE`，全部 closed；批次最终 `voided(60)`、待处理评审 `0`。另一完整业务轮次批次 `900000000953` 的评审 `8/9/10` 同样满足三类处置和最终作废。
- 发现并修复：admin 金手指曾绕过前端不合格冻结提示；修复后不合格冻结对任何角色生效，其他锁定仍保持原有高权限语义。
- 非目标遗留：全局审批待办角标接口报 `APPROVAL_ADAPTER_PAGE_INCONSISTENT`，已作为 `nonTargetConsoleErrors` 留证；不合格评审接口和页面未出现目标错误。
- 安全收尾：所有 Playwright trace、任务运行日志和任务专属运行进程均已清理，`8309/48309` 已释放。
- Git 状态：本轮 1 个前端修复、2 个 E2E 文件及任务文档已在 `E:\IntRuoyi` 工作树更新，但未暂存、未提交、未 push。

## PQC提交入口迁移复验（2026-08-31）

- 变更结论：`PQC_SUBMISSION` 的 `不合格审查` 已从一线 PQC 固定模板页移除，新增到 `PQC组长 -> PQC管理` 行操作区。
- 后端数据：`PQC管理` 列表新增 `batchExecutionId` 返回值，按工单、工艺路线、批号解析最新有效 eDHR 批次；旧 `pqc_submission_trace` 连接已移除。
- RED: `node src\test\js\mes-edhr-nonconformance-review-mvp-static.spec.cjs` -> FAIL，原因是读模型仍走旧追溯连接。
- GREEN: `node src\test\js\mes-edhr-nonconformance-review-mvp-static.spec.cjs` -> PASS。
- GREEN: `node tests\e2e\edhr-nonconformance-review-mvp-static.spec.js` -> PASS。
- GREEN: `node --check tests\e2e\edhr-nonconformance-review-mvp-real.e2e.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `mvn.cmd -pl yudao-module-mes -am "-DskipTests" compile` -> PASS，24 个 Maven 模块全部 SUCCESS。
- GREEN: `git diff --check -- <task-owned paths>` -> PASS，仅 CRLF 换行提示。
- 未执行真实 Playwright E2E：当前 `8081` 前端 HTTP 200，但 `48081` 后端无监听，且 `NCR_E2E_PASSWORD` 为空；按 fail-fast 记录前置阻塞。
- Cleanup: `task_closeout.py --mode preview/apply` 均 PASS，delete 为空，blocked 为空；当前为主工作区 `int_main`，未执行 worktree 合并或删除。

## 后端重启后 PQC管理入口复验（2026-09-01）

- 当前代码结论：入口迁移实现仍成立，`PQC_SUBMISSION` 不合格审查按钮位于 `PQC组长 -> PQC管理` 行操作区，一线 PQC 固定模板页不再显示该提交类审查入口。
- 定向验证：前端静态契约 PASS，后端静态契约 PASS，真实 E2E 脚本语法 PASS，后端 `yudao-module-mes` 依赖编译 PASS，scoped diff check PASS。
- 后端修复包核验：标准 backend 重启曾生成 `backend-runtime-control-20260901-022725.jar`，该包内嵌 MES Mapper 包含 `CAST(batch_execution.batch_code AS BINARY) = CAST(work_order.batch_code AS BINARY)`，默认本机租户 `PQC管理` 列表接口业务码为 `0`。
- 当前阻塞：用户本轮重启后，`48081` health=`UP`，但当前运行包为 `backend-runtime-control-20260901-024209-approval-center.jar`；该包仍包含旧 unary `BINARY` 写法，不包含 `CAST(... AS BINARY)` 修复。
- 当前接口结果：默认本机租户登录态请求 `PQC管理` 列表返回业务 `500/系统异常`，因此不能证明红框行操作入口在真实页面可点击可用。
- Playwright 结果：`ncr-int-main-20260901-pqc-entry-04/05/06` 均未到达目标入口闭环；失败原因分别为测试租户业务账号登录失败、QA 列表阶段页面/请求异常、登录页加载超时与 Vite deps cache `EPERM rename` 风险。结果文件均未记录已完成处置。
- Artifact cleanup：已删除本任务 5 个 `failure-trace.zip`，剩余 trace 压缩包 `0`；保留截图和 `result.json` 作为失败证据。
- 放行结论：代码与定向验证可确认修改方向正确；当前共享运行态没有加载后端修复，真实页面验收为 BLOCKED。未用 API-only、旧截图、静态合同或空数据列表冒充真实入口通过。

## 用户授权替换后续复验（2026-09-01）

- 后端状态：用户授权后已替换旧 `approval-center` 运行包；当前 `48081` 运行 `backend-runtime-control-20260901-024209-approval-center-pqc-fixed.jar`，health=`UP`，内嵌 MES Mapper 含 `CAST(... AS BINARY)` 修复。
- 接口状态：默认本机租户 `PQC管理` 列表返回业务码 `0`、总数 `54`；测试租户 `PQC管理` 列表返回业务码 `0`、总数 `0`。
- 定向验证：前端静态合同 PASS，真实 E2E 脚本语法 PASS，scoped diff check PASS；新增只读入口模式的 RED/GREEN 已记录。
- 真实完整 E2E：`ncr-int-main-20260901-pqc-entry-07` FAIL，阻塞在测试租户正式批次创建前置，返回 `1040750403`，表示缺 eDHR 批记录配置或默认批记录；该 run 未完成处置写入。
- 安全状态：路线 `RT000028` 已确认恢复停用；本轮新增 `failure-trace.zip` 已删除。
- 最终检查：前端静态合同、后端静态合同、真实 E2E 脚本语法、scoped diff check 均 PASS；本任务 Playwright trace 压缩包剩余 `0`。
- 当前阻塞：真实红框点击验收还缺一个可登录的真实页面会话或测试租户内可点击的 PQC 提交样本；本轮未猜密码、未复用 token、未用 API-only 替代页面点击。
