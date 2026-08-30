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
